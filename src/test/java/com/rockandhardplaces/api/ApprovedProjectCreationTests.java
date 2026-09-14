package com.rockandhardplaces.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.Trade;
import com.rockandhardplaces.planning.ProjectPlanningAiClient;
import com.rockandhardplaces.project.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:./target/approved-project-test.sqlite")
@AutoConfigureMockMvc
class ApprovedProjectCreationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ProjectPlanningAiClient ai;
    @MockitoSpyBean ProjectWorkflowService workflow;
    Homeowner owner;
    Trade trade;
    String key;
    @BeforeEach void fixture() {
        key = UUID.randomUUID().toString();
        new TransactionTemplate(manager).execute(status -> {
            var user = new User(key + "@example.com"); em.persist(user);
            owner = new Homeowner(user, "Phase 3 owner"); em.persist(owner);
            trade = new Trade("Phase 3 carpentry " + key); em.persist(trade);
            return null;
        });
        when(account.activeProfile()).thenReturn(owner);
    }
    @AfterEach void neverCallsAi() { verifyNoInteractions(ai); }
    Map<String, Object> draft() {
        return new LinkedHashMap<>(Map.of("title", "AI-assisted Hockessin " + key, "description", "Approved adult tree house scope", "jobZip", "19707",
                "tasks", List.of(Map.of("title", "Approved deck", "description", "Edited deck and railing scope", "requiredTradeIds", List.of(trade.getId())))));
    }
    org.springframework.test.web.servlet.ResultActions create(Map<String, Object> draft) throws Exception {
        return mvc.perform(post("/api/project-builder/create").header("Idempotency-Key", key).contentType("application/json").content(mapper.writeValueAsString(draft)));
    }
    long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }

    @Test void createsNormalOwnedProjectTasksAndRequirementsAndReplaysWithoutDuplicates() throws Exception {
        long before = count("projects");
        var result = create(draft()).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PLANNING")).andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        create(draft()).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        assertThat(count("projects")).isEqualTo(before + 1);
        assertThat(jdbc.queryForObject("SELECT homeowner_id FROM projects WHERE id = ?", Long.class, id)).isEqualTo(owner.getId());
        mvc.perform(get("/api/projects/" + id + "/tasks")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].title").value("Approved deck"))
            .andExpect(jsonPath("$[0].description").value("Edited deck and railing scope"))
            .andExpect(jsonPath("$[0].status").value("PLANNING"))
            .andExpect(jsonPath("$[0].requiredTrades[0].tradeId").value(trade.getId()))
            .andExpect(jsonPath("$[0].confidence").doesNotExist()).andExpect(jsonPath("$[0].reason").doesNotExist());
        mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty());
        // Omitted/removed/ignored recommendations are absent: exactly one submitted task exists.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE project_id = ?", Long.class, id)).isEqualTo(1);
        var changed = draft(); changed.put("title", "Changed after creation");
        create(changed).andExpect(status().isConflict());
    }
    @Test void manualWithoutAiOrTasksWorks() throws Exception {
        var draft = draft(); draft.put("tasks", List.of());
        create(draft).andExpect(status().isCreated());
    }
    @Test void wrongRoleAndInactiveOwnerAreRejected() throws Exception {
        when(account.activeProfile()).thenReturn(mock(Tradesperson.class));
        create(draft()).andExpect(status().isForbidden());
        when(account.activeProfile()).thenReturn(owner);
        owner.setAccountStatus(AccountStatus.SUSPENDED);
        create(draft()).andExpect(status().isForbidden());
    }
    @Test void unresolvedOrMissingTradeRollsBackAndCreatesNoCatalogEntries() throws Exception {
        long before = count("projects"), catalog = count("trades");
        var draft = draft();
        for (var ids : List.of(List.of(999999999L), List.of())) {
            draft.put("tasks", List.of(Map.of("title", "Unresolved work", "description", "Keep this scope", "requiredTradeIds", ids)));
            create(draft).andExpect(status().isBadRequest());
        }
        assertThat(count("projects")).isEqualTo(before); assertThat(count("trades")).isEqualTo(catalog);
    }
    @Test void rejectsIdentityStatusAndProviderMetadata() throws Exception {
        long before = count("projects");
        for (String field : List.of("ownerId", "userId", "status", "confidence", "reason", "recognizedTrades")) {
            var draft = draft(); draft.put(field, "not accepted"); create(draft).andExpect(status().isBadRequest());
        }
        var draft = draft(); draft.put("tasks", List.of(Map.of("title", "Scope", "description", "Scope", "requiredTradeIds", List.of(trade.getId()), "needsConfirmation", true)));
        create(draft).andExpect(status().isBadRequest());
        assertThat(count("projects")).isEqualTo(before);
    }
    @Test void failureAfterFirstTaskRollsBackEverythingAndRetrySucceeds() throws Exception {
        long projects = count("projects"), tasks = count("tasks"), requirements = count("task_trades"), receipts = count("project_creation_receipts");
        var draft = draft(); draft.put("tasks", List.of(
                Map.of("title", "First task", "description", "First", "requiredTradeIds", List.of(trade.getId())),
                Map.of("title", "Fail second", "description", "Second", "requiredTradeIds", List.of(trade.getId()))));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("PRIVATE DATABASE DETAIL"))
                .when(workflow).createTask(any(), any(), eq("Fail second"), any(), isNull(), any());
        create(draft).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("We could not save your changes. Please try again."));
        assertThat(count("projects")).isEqualTo(projects); assertThat(count("tasks")).isEqualTo(tasks);
        assertThat(count("task_trades")).isEqualTo(requirements); assertThat(count("project_creation_receipts")).isEqualTo(receipts);
        reset(workflow); create(draft).andExpect(status().isCreated());
        assertThat(count("projects")).isEqualTo(projects + 1); assertThat(count("tasks")).isEqualTo(tasks + 2);
    }
    @Test void createdRequirementsUseNormalDiscoveryBiddingAndSelfBidRules() throws Exception {
        var result = create(draft()).andExpect(status().isCreated()).andReturn();
        long projectId = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        long taskId = jdbc.queryForObject("SELECT id FROM tasks WHERE project_id = ?", Long.class, projectId);
        long requirementId = jdbc.queryForObject("SELECT id FROM task_trades WHERE task_id = ?", Long.class, taskId);
        var workers = new TransactionTemplate(manager).execute(status -> {
            var otherUser = new User(UUID.randomUUID() + "@example.com"); em.persist(otherUser);
            var other = new Tradesperson(otherUser, "Independent builder");
            var own = new Tradesperson(owner.getUser(), "Owner builder");
            for (var worker : List.of(other, own)) {
                worker.setVerificationStatus(TradespersonVerificationStatus.VERIFIED); em.persist(worker);
                em.persist(new PersonTrade(worker, trade));
            }
            return List.of(other, own);
        });
        when(account.activeProfile()).thenReturn(workers.get(1));
        mvc.perform(get("/api/opportunities").param("tradeId", trade.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        String bid = mapper.writeValueAsString(Map.of("taskTradeId", requirementId, "amount", 100, "message", "Test scope"));
        mvc.perform(post("/api/tasks/" + taskId + "/bids").contentType("application/json").content(bid)).andExpect(status().isForbidden());
        when(account.activeProfile()).thenReturn(workers.get(0));
        mvc.perform(get("/api/opportunities").param("tradeId", trade.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].requiredTrade.id").value(requirementId));
        mvc.perform(post("/api/tasks/" + taskId + "/bids").contentType("application/json").content(bid)).andExpect(status().isCreated());
    }

}
