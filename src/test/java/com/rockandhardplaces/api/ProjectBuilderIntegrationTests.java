package com.rockandhardplaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.Trade;
import com.rockandhardplaces.planning.*;
import com.rockandhardplaces.planning.ProjectPlanningAiResponse.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectBuilderIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ProjectPlanningAiClient ai;
    Homeowner owner;
    Trade trade;
    String suggestion;

    @BeforeEach void fixture() {
        var user = new User(UUID.randomUUID() + "@example.com"); em.persist(user);
        owner = new Homeowner(user, "Planner"); em.persist(owner);
        trade = new Trade("Planning Carpentry " + UUID.randomUUID()); em.persist(trade); em.flush();
        suggestion = "  " + trade.getName().toUpperCase(Locale.ROOT).replace(" ", "  ") + "  ";
        when(account.activeProfile()).thenReturn(owner);
        when(ai.plan(any())).thenReturn(valid());
    }

    @Test void homeownerGetsValidatedPlanWithoutAnyAuthoritativeWrites() throws Exception {
        var before = counts();
        mvc.perform(post("/api/project-builder/plan").contentType("application/json")
                .content("{\"idea\":\"Hockessin Tree House with deck and lighting\",\"userId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.summary").value("Hockessin Tree House"))
                .andExpect(jsonPath("$.plan.suggestedTrades[0].needsConfirmation").value(true))
                .andExpect(jsonPath("$.plan.tasks[0].needsConfirmation").value(false))
                .andExpect(jsonPath("$.plan.assumptions[0]").value("Adult use"))
                .andExpect(jsonPath("$.plan.warnings[0]").value("Professional review required"))
                .andExpect(jsonPath("$.recognizedTrades[0].tradeId").value(trade.getId()))
                .andExpect(jsonPath("$.recognizedTrades[0].tradeName").value(trade.getName()))
                .andExpect(jsonPath("$.unresolvedTrades[0]").value("Unknown tree specialist"))
                .andExpect(jsonPath("$.unresolvedTrades[1]").value("Unknown task-only trade"));
        em.flush(); em.clear();
        assertThat(counts()).isEqualTo(before);
        verify(ai).plan(argThat(prompt -> prompt.catalogTradeNames().contains(trade.getName())
                && prompt.idea().equals("Hockessin Tree House with deck and lighting")));
    }

    @Test void wrongRoleCannotCallProvider() throws Exception {
        when(account.activeProfile()).thenReturn(mock(Tradesperson.class));
        request().andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(ai);
    }

    @ParameterizedTest @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "DEACTIVATED"})
    void inactiveHomeownerCannotCallProvider(AccountStatus status) throws Exception {
        owner.setAccountStatus(status);
        request().andExpect(status().isForbidden());
        verifyNoInteractions(ai);
    }

    @ParameterizedTest @EnumSource(PlanningException.Reason.class)
    void providerFailureUsesSafeApiEnvelope(PlanningException.Reason reason) throws Exception {
        var before = counts();
        when(ai.plan(any())).thenThrow(new PlanningException(reason));
        request().andExpect(status().is(reason == PlanningException.Reason.INVALID_RESPONSE ? 502 : 503))
                .andExpect(jsonPath("$.error").value("PLANNING_" + reason.name()))
                .andExpect(jsonPath("$.path").value("/api/project-builder/plan"))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.trace").doesNotExist());
        assertThat(counts()).isEqualTo(before);
    }

    @Test void invalidTypedProviderResponseIsRevalidated() throws Exception {
        when(ai.plan(any())).thenReturn(new ProjectPlanningAiResponse(" ", List.of(), List.of(),
                List.of(), List.of(), List.of()));
        request().andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("PLANNING_INVALID_RESPONSE"));
    }

    @Test void invalidIdeasNeverCallProvider() throws Exception {
        for (String idea : List.of("", " ", "x".repeat(8001)))
            mvc.perform(post("/api/project-builder/plan").contentType("application/json")
                    .content("{\"idea\":\"" + idea + "\"}"))
                    .andExpect(status().isBadRequest());
        verifyNoInteractions(ai);
    }

    private org.springframework.test.web.servlet.ResultActions request() throws Exception {
        return mvc.perform(post("/api/project-builder/plan").contentType("application/json")
                .content("{\"idea\":\"Hockessin Tree House\"}"));
    }

    private ProjectPlanningAiResponse valid() {
        return new ProjectPlanningAiResponse("Hockessin Tree House", List.of("Freestanding or tree-supported?"),
                List.of(new SuggestedTrade(suggestion, "Framing review", Confidence.HIGH, true),
                        new SuggestedTrade("Unknown tree specialist", "Tree review", Confidence.LOW, true)),
                List.of(new SuggestedTask("Clarify use", "Discuss intended use", suggestion,
                        "Scope", Confidence.MEDIUM, false),
                        new SuggestedTask("Review tree", "Request professional assessment", "Unknown task-only trade",
                                "Safety", Confidence.LOW, true)),
                List.of("Adult use"), List.of("Professional review required"));
    }

    private Map<String, Long> counts() {
        var counts = new LinkedHashMap<String, Long>();
        for (String table : List.of("projects", "tasks", "task_trades", "trades", "bids",
                "task_assignments", "project_teams", "project_team_trades"))
            counts.put(table, ((Number) em.createNativeQuery("SELECT COUNT(*) FROM " + table)
                    .getSingleResult()).longValue());
        return counts;
    }
}
