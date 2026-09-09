package com.rockandhardplaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.*;
import com.rockandhardplaces.project.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FrontendSupportIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired TaskProgressService progress;
    @MockitoBean ActiveAccountContext account;
    Homeowner owner;
    Tradesperson worker;
    Project project;
    Task task;
    Trade trade;
    TaskTrade requirement;

    @BeforeEach
    void fixture() {
        owner = save(new Homeowner(save(new User(UUID.randomUUID() + "@example.com")), "Owner"));
        worker = save(new Tradesperson(save(new User(UUID.randomUUID() + "@example.com")), "Builder"));
        worker.setVerificationStatus(TradespersonVerificationStatus.VERIFIED);
        trade = save(new Trade("Fixture trade " + UUID.randomUUID()));
        save(new PersonTrade(worker, trade));
        project = save(new Project("Bus conversion", "Living space", ProjectStatus.PLANNING, "10001", owner));
        task = save(new Task("Build cabinets", "Cabinet scope", TaskStatus.PLANNING, project, null));
        requirement = save(new TaskTrade(task, trade));
        reload();
        asOwner();
    }

    @Test void proposalCreationConflictsWithoutDuplicatingHistory() throws Exception {
        asWorker();
        String body="{\"taskTradeId\":"+requirement.getId()+",\"amount\":1200,\"message\":\"Approach\"}";
        mvc.perform(post("/api/tasks/{id}/bids",task.getId()).contentType("application/json").content(body)).andExpect(status().isCreated());
        for(String state: new String[]{"SUBMITTED","ACCEPTED","REJECTED","WITHDRAWN"}) {
            em.createNativeQuery("UPDATE bids SET status=:state WHERE task_id=:task").setParameter("state",state).setParameter("task",task.getId()).executeUpdate();em.clear();
            mvc.perform(post("/api/tasks/{id}/bids",task.getId()).contentType("application/json").content(body)).andExpect(status().isConflict());
            assertThat(((Number)em.createNativeQuery("SELECT COUNT(*) FROM bids WHERE task_id=:task").setParameter("task",task.getId()).getSingleResult()).intValue()).isEqualTo(1);
        }
    }

    @Test void reviewIsReadBackAfterClearingPersistenceContextWithItsRelationships() throws Exception {
        assign();progress.start(task);progress.submitForReview(task);progress.approve(task);reload();asOwner();
        String body="{\"tradespersonId\":"+worker.getId()+",\"projectId\":"+project.getId()+",\"taskId\":"+task.getId()+",\"level\":\"TASK\",\"overallRating\":5,\"body\":\"Lasting review\"}";
        mvc.perform(post("/api/reviews").contentType("application/json").content(body)).andExpect(status().isCreated());
        reload();asOwner();
        for(int i=0;i<2;i++) mvc.perform(get("/api/reviews").param("taskId",task.getId().toString())).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].body").value("Lasting review"))
            .andExpect(jsonPath("$[0].overallRating").value(5)).andExpect(jsonPath("$[0].homeownerId").value(owner.getId()))
            .andExpect(jsonPath("$[0].tradespersonId").value(worker.getId())).andExpect(jsonPath("$[0].projectId").value(project.getId()))
            .andExpect(jsonPath("$[0].taskId").value(task.getId())).andExpect(jsonPath("$[0].createdAt").isNotEmpty())
            .andExpect(jsonPath("$[0].authorDisplayName").value("Owner")).andExpect(jsonPath("$[0].tradespersonDisplayName").value("Builder"))
            .andExpect(jsonPath("$[0].withdrawn").value(false));
        mvc.perform(post("/api/reviews").contentType("application/json").content(body)).andExpect(status().isConflict());
        asWorker();mvc.perform(get("/api/reviews").param("taskId",task.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/api/reviews").contentType("application/json").content(body)).andExpect(status().isForbidden());
        var stranger=save(new Homeowner(save(new User(UUID.randomUUID()+"@example.com")),"Other owner"));
        when(account.activeProfile()).thenReturn(stranger);
        mvc.perform(get("/api/reviews").param("taskId",task.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/api/reviews").contentType("application/json").content(body)).andExpect(status().isForbidden());
    }

    @Test
    void bidderEditsAndWithdrawsSameProposalWhileHistoryAndScopeStayIntact() throws Exception {
        Bid bid=save(new Bid(task,requirement,worker,BigDecimal.TEN,"Original"));em.flush();asWorker();
        Long id=bid.getId();
        mvc.perform(put("/api/bids/{id}",id).contentType("application/json").content("{\"amount\":125.50,\"message\":\"Revised approach\",\"taskTradeId\":99999,\"status\":\"ACCEPTED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.amount").value(125.50))
            .andExpect(jsonPath("$.taskTradeId").value(requirement.getId())).andExpect(jsonPath("$.status").value("SUBMITTED"));
        em.clear();
        mvc.perform(get("/api/bids")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].bid.message").value("Revised approach"));
        mvc.perform(post("/api/bids/{id}/withdraw",id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WITHDRAWN"));
        em.clear();mvc.perform(get("/api/bids")).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].bid.id").value(id)).andExpect(jsonPath("$[0].bid.status").value("WITHDRAWN"));
        mvc.perform(put("/api/bids/{id}",id).contentType("application/json").content("{\"amount\":150}")).andExpect(status().isConflict());
        mvc.perform(post("/api/bids/{id}/withdraw",id)).andExpect(status().isConflict());
    }
    @Test
    void proposalMutationsRejectOtherActorsInvalidAmountsAndDecidedBids() throws Exception {
        Bid bid=save(new Bid(task,requirement,worker,BigDecimal.TEN,"Original"));em.flush();
        mvc.perform(put("/api/bids/{id}",bid.getId()).contentType("application/json").content("{\"amount\":20}")).andExpect(status().isForbidden());
        Tradesperson other=save(new Tradesperson(save(new User(UUID.randomUUID()+"@test.local")),"Other"));em.flush();when(account.activeProfile()).thenReturn(other);
        mvc.perform(put("/api/bids/{id}",bid.getId()).contentType("application/json").content("{\"amount\":20}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/bids/{id}/withdraw",bid.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/api/bids")).andExpect(jsonPath("$.length()").value(0));asWorker();
        mvc.perform(put("/api/bids/{id}",bid.getId()).contentType("application/json").content("{\"amount\":0}")).andExpect(status().isBadRequest());
        for(boolean accepted:java.util.List.of(true,false)) {
            Task decisionTask=save(new Task("Decision scope","Description",TaskStatus.PLANNING,project,null));
            TaskTrade decisionRequirement=save(new TaskTrade(decisionTask,trade));
            Bid decided=save(new Bid(decisionTask,decisionRequirement,worker,BigDecimal.TEN,"Fixed"));if(accepted)decided.accept();else decided.reject();em.flush();
            mvc.perform(put("/api/bids/{id}",decided.getId()).contentType("application/json").content("{\"amount\":20}")).andExpect(status().isConflict());
            mvc.perform(post("/api/bids/{id}/withdraw",decided.getId())).andExpect(status().isConflict());
            assertThat(decided.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
        }
    }
    @Test
    void zipSearchReturnsOnlyEligibleOpenScopesIncludingNoTaskAndSelfExclusions() throws Exception {
        save(new Project("No open needs","Description",ProjectStatus.PLANNING,"10001",owner));
        Homeowner self=save(new Homeowner(worker.getUser(),"Same person"));
        Project own=save(new Project("Hockessin Tree House","Description",ProjectStatus.PLANNING,"19807",self));
        Task ownTask=save(new Task("Own scope","Description",TaskStatus.PLANNING,own,null));save(new TaskTrade(ownTask,trade));
        Project otherZip=save(new Project("Open work elsewhere","Description",ProjectStatus.PLANNING,"19807",owner));
        Task otherTask=save(new Task("Eligible scope","Description",TaskStatus.PLANNING,otherZip,null));save(new TaskTrade(otherTask,trade));
        reload();asWorker();
        mvc.perform(get("/api/opportunities").param("jobZip","10001")).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].project.title").value("Bus conversion"));
        mvc.perform(get("/api/opportunities").param("jobZip","19807")).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].project.title").value("Open work elsewhere"));
        mvc.perform(get("/api/opportunities").param("jobZip","00000")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void homeownerSummaryUsesServerCountsAndUnfilledRequirements() throws Exception {
        save(new Task("Review scope", "Inspect", TaskStatus.READY_FOR_REVIEW, project, null));
        save(new Task("Finished scope", "Done", TaskStatus.COMPLETED, project, null));
        save(new Task("Cancelled scope", "Cancelled", TaskStatus.CANCELLED, project, null));
        reload(); asOwner();
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects[0].project.title").value("Bus conversion"))
            .andExpect(jsonPath("$.projects[0].project.status").value("PLANNING"))
            .andExpect(jsonPath("$.projects[0].project.progressPercentage").value(33))
            .andExpect(jsonPath("$.projects[0].totalTasks").value(3))
            .andExpect(jsonPath("$.projects[0].completedTasks").value(1))
            .andExpect(jsonPath("$.projects[0].awaitingReview.length()").value(1))
            .andExpect(jsonPath("$.projects[0].tradesNeeded[0].requiredTrade.id").value(requirement.getId()))
            .andExpect(jsonPath("$.nextAction").value("REVIEW_WORK"))
            .andExpect(jsonPath("$.projects[0].project.homeowner").doesNotExist());
    }

    @Test
    void catalogAndPeopleSearchReturnQualificationsIncludingSelfPublicProfile() throws Exception {
        Specialty specialty = save(new Specialty("Cabinetry", trade));
        save(new PersonSpecialty(worker, specialty));
        Tradesperson self = save(new Tradesperson(owner.getUser(), "Self builder"));
        save(new PersonTrade(self, trade));
        Tradesperson suspended = save(new Tradesperson(save(new User(UUID.randomUUID() + "@example.com")), "Suspended builder"));
        suspended.setAccountStatus(AccountStatus.SUSPENDED);
        save(new PersonTrade(suspended, trade));
        em.flush();
        mvc.perform(get("/api/catalog/trades")).andExpect(status().isOk());
        mvc.perform(get("/api/discovery/tradespeople").param("tradeId", trade.getId().toString()).param("q", "BUILD"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].profile.id").value(self.getId()))
            .andExpect(jsonPath("$[0].profile.id").value(worker.getId()))
            .andExpect(jsonPath("$[0].qualifications[0].specialties[0].name").value("Cabinetry"))
            .andExpect(jsonPath("$[0].specialties[0].name").value("Cabinetry"))
            .andExpect(jsonPath("$[0].profile.user").doesNotExist());
        mvc.perform(get("/api/discovery/tradespeople").param("tradeId", trade.getId().toString()).param("availability", "BUSY"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/discovery/tradespeople").param("tradeId", "invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void opportunitiesPermitScopeReadWithoutMembershipButNeverPrivateProjectRead() throws Exception {
        asWorker();
        mvc.perform(get("/api/opportunities").param("tradeId", trade.getId().toString()).param("jobZip", "10001"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].requiredTrade.id").value(requirement.getId()))
            .andExpect(jsonPath("$[0].bidding.allowed").value(true));
        mvc.perform(get("/api/opportunities/{id}", requirement.getId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.task.description").value("Cabinet scope"));
        mvc.perform(get("/api/projects/{id}", project.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks/{id}/assignments", task.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/api/opportunities").param("jobZip", "00000"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void opportunitiesHideOwnClosedCancelledUnqualifiedAndFilledScopes() throws Exception {
        Homeowner self = save(new Homeowner(worker.getUser(), "Worker owner"));
        scope(self, ProjectStatus.PLANNING, TaskStatus.PLANNING, trade);
        scope(owner, ProjectStatus.COMPLETED, TaskStatus.PLANNING, trade);
        scope(owner, ProjectStatus.PLANNING, TaskStatus.READY_FOR_REVIEW, trade);
        scope(owner, ProjectStatus.PLANNING, TaskStatus.CANCELLED, trade);
        scope(owner, ProjectStatus.PLANNING, TaskStatus.PLANNING, save(new Trade("Other " + UUID.randomUUID())));
        Bid accepted = save(new Bid(task, requirement, worker, BigDecimal.TEN, "Accepted"));
        accepted.accept();
        reload(); asWorker();
        mvc.perform(get("/api/opportunities")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/opportunities/{id}", requirement.getId())).andExpect(status().isNotFound());
    }

    @Test
    void verificationEligibilityMatchesSubmitAndOnlyOwnBidsAreReturned() throws Exception {
        save(new Bid(task, requirement, worker, BigDecimal.TEN, "Mine"));
        Tradesperson other = save(new Tradesperson(save(new User(UUID.randomUUID() + "@example.com")), "Other"));
        save(new Bid(task, requirement, other, BigDecimal.ONE, "Private proposal"));
        worker.setVerificationStatus(TradespersonVerificationStatus.PENDING);
        em.flush(); asWorker();
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk())
            .andExpect(jsonPath("$.bidding.allowed").value(false))
            .andExpect(jsonPath("$.bidding.reason").isNotEmpty())
            .andExpect(jsonPath("$.activeBids.length()").value(1))
            .andExpect(jsonPath("$.opportunities[0].ownBids.length()").value(1))
            .andExpect(jsonPath("$.opportunities[0].ownBids[0].message").value("Mine"));
        mvc.perform(post("/api/tasks/{id}/bids", task.getId()).contentType("application/json")
            .content("{\"taskTradeId\":" + requirement.getId() + ",\"amount\":10}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void assignmentStartReviewApproveTraversesTaskLifecycleWithoutCompletingProject() throws Exception {
        assign(); asWorker();
        mvc.perform(get("/api/assignments")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/tasks/{id}/assignments", task.getId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].tradespersonId").value(worker.getId()));
        mvc.perform(post("/api/tasks/{id}/start", task.getId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(post("/api/tasks/{id}/start", task.getId())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/tasks/{id}/ready-for-review", task.getId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.progressPercentage").value(0));
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk())
            .andExpect(jsonPath("$.completedWorkCount").value(0));
        asOwner();
        mvc.perform(post("/api/tasks/{id}/approve", task.getId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.progressPercentage").value(100));
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects[0].project.status").value("PLANNING"))
            .andExpect(jsonPath("$.projects[0].project.progressPercentage").value(100));
        asWorker();
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk())
            .andExpect(jsonPath("$.activeWork.length()").value(0))
            .andExpect(jsonPath("$.completedWorkCount").value(1))
            .andExpect(jsonPath("$.completedProjectCount").value(0));
    }

    @Test
    void assignmentCoverageRemovesTradeNeedAndOpportunity() throws Exception {
        assign(); asOwner();
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects[0].tradesNeeded.length()").value(0))
            .andExpect(jsonPath("$.projects[0].team[0].trades[0]").value(trade.getName()));
        asWorker();
        mvc.perform(get("/api/opportunities")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void suspendedMembershipHidesAssignmentsAndCannotStart() throws Exception {
        assign();
        project.getProjectTeams().get(0).setStatus(ProjectTeamStatus.SUSPENDED);
        em.flush(); asWorker();
        mvc.perform(get("/api/assignments")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/tasks/{id}/start", task.getId())).andExpect(status().isForbidden());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PLANNING);
    }

    @Test
    void wrongRoleInactiveAndUnassignedActorsAreRejected() throws Exception {
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isForbidden());
        mvc.perform(get("/api/opportunities")).andExpect(status().isForbidden());
        mvc.perform(post("/api/tasks/{id}/start", task.getId())).andExpect(status().isForbidden());
        asWorker();
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isForbidden());
        mvc.perform(get("/api/discovery/tradespeople")).andExpect(status().isForbidden());
        save(new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE)); reload(); asWorker();
        mvc.perform(post("/api/tasks/{id}/start", task.getId())).andExpect(status().isForbidden());
        worker.setAccountStatus(AccountStatus.SUSPENDED);
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isForbidden());
    }

    @Test
    void emptyHomeownerHasExplicitNextAction() throws Exception {
        Homeowner empty = save(new Homeowner(save(new User(UUID.randomUUID() + "@example.com")), "New owner"));
        when(account.activeProfile()).thenReturn(empty);
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects.length()").value(0)).andExpect(jsonPath("$.nextAction").value("CREATE_PROJECT"));
    }

    @Test
    void sameUserAssignmentCannotStartWork() throws Exception {
        Tradesperson self = save(new Tradesperson(owner.getUser(), "Own builder"));
        save(new ProjectTeam(project, self, ProjectTeamStatus.ACTIVE));
        save(new TaskAssignment(task, self));
        reload();
        when(account.activeProfile()).thenReturn(self);
        mvc.perform(post("/api/tasks/{id}/start", task.getId())).andExpect(status().isForbidden());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PLANNING);
    }

    @Test
    void closedProjectAndCancelledParentCannotStartWork() throws Exception {
        Project closed = save(new Project("Closed", "Closed", ProjectStatus.COMPLETED, "10001", owner));
        Task closedTask = save(new Task("Task", "Task", TaskStatus.PLANNING, closed, null));
        save(new ProjectTeam(closed, worker, ProjectTeamStatus.ACTIVE));
        save(new TaskAssignment(closedTask, worker));
        Task parent = save(new Task("Cancelled", "Cancelled", TaskStatus.CANCELLED, project, null));
        Task child = save(new Task("Child", "Child", TaskStatus.PLANNING, project, parent));
        save(new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        save(new TaskAssignment(child, worker));
        reload(); asWorker();
        mvc.perform(post("/api/tasks/{id}/start", closedTask.getId())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/tasks/{id}/start", child.getId())).andExpect(status().isBadRequest());
    }

    @Test
    void submittedBidRetainsHistoryButLosesClosedScope() throws Exception {
        Bid bid = save(new Bid(task, requirement, worker, BigDecimal.TEN, "Mine"));
        asWorker();
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk())
            .andExpect(jsonPath("$.activeBids[0].task.id").value(task.getId()));
        progress.cancel(task); em.flush();
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk())
            .andExpect(jsonPath("$.activeBids[0].bid.id").value(bid.getId()))
            .andExpect(jsonPath("$.activeBids[0].bid.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.activeBids[0].task").isEmpty())
            .andExpect(jsonPath("$.activeBids[0].project").isEmpty());
    }

    @Test
    void suspendedBidderCannotRediscoverScopeButKeepsBid() throws Exception {
        save(new Bid(task, requirement, worker, BigDecimal.TEN, "Mine"));
        save(new ProjectTeam(project, worker, ProjectTeamStatus.SUSPENDED));
        asWorker();
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk())
            .andExpect(jsonPath("$.activeBids[0].bid.message").value("Mine"))
            .andExpect(jsonPath("$.activeBids[0].project").isEmpty())
            .andExpect(jsonPath("$.activeBids[0].task").isEmpty());
        mvc.perform(get("/api/opportunities/{id}", requirement.getId())).andExpect(status().isNotFound());
    }

    @Test
    void sameUserBidDoesNotGrantScopeEvenWithMembership() throws Exception {
        Tradesperson self = save(new Tradesperson(owner.getUser(), "Self"));
        save(new PersonTrade(self, trade));
        save(new ProjectTeam(project, self, ProjectTeamStatus.ACTIVE));
        save(new Bid(task, requirement, self, BigDecimal.TEN, "Own history"));
        when(account.activeProfile()).thenReturn(self);
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk())
            .andExpect(jsonPath("$.activeBids[0].bid.message").value("Own history"))
            .andExpect(jsonPath("$.activeBids[0].task").isEmpty());
        mvc.perform(get("/api/opportunities/{id}", requirement.getId())).andExpect(status().isNotFound());
    }

    @Test
    void unrelatedHomeownerCannotReadAssignmentsOrStartTask() throws Exception {
        Homeowner unrelated = save(new Homeowner(save(new User(UUID.randomUUID() + "@example.com")), "Unrelated"));
        when(account.activeProfile()).thenReturn(unrelated);
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects").isEmpty());
        mvc.perform(get("/api/tasks/{id}/assignments", task.getId())).andExpect(status().isForbidden());
        mvc.perform(post("/api/tasks/{id}/start", task.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/api/assignments")).andExpect(status().isForbidden());
        mvc.perform(get("/api/opportunities/{id}", requirement.getId())).andExpect(status().isForbidden());
    }

    @Test
    void assignmentFillsOnlyTheAssignedProjectTrade() throws Exception {
        Trade second = save(new Trade("Second " + UUID.randomUUID()));
        save(new PersonTrade(worker, second));
        TaskTrade secondRequirement = save(new TaskTrade(task, second));
        assign(); asOwner();
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects[0].tradesNeeded.length()").value(1))
            .andExpect(jsonPath("$.projects[0].tradesNeeded[0].requiredTrade.id").value(secondRequirement.getId()));
        asWorker();
        mvc.perform(get("/api/opportunities").param("tradeId", second.getId().toString())).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].requiredTrade.id").value(secondRequirement.getId()));
        mvc.perform(get("/api/opportunities/{id}", requirement.getId())).andExpect(status().isNotFound());
    }

    @Test
    void startingNestedChildReconcilesAllCompletedAncestorsAndSummary() throws Exception {
        progress.completeUnassigned(task);
        Task middle = save(new Task("Middle", "Middle", TaskStatus.COMPLETED, project, task));
        Task child = save(new Task("Child", "Child", TaskStatus.PLANNING, project, middle));
        save(new Task("Done", "Done", TaskStatus.COMPLETED, project, middle));
        save(new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        save(new TaskAssignment(child, worker));
        reload(); asWorker();
        mvc.perform(post("/api/tasks/{id}/start", child.getId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.progressPercentage").value(0));
        reload(); asOwner();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(em.find(Task.class, middle.getId()).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects[0].totalTasks").value(1))
            .andExpect(jsonPath("$.projects[0].completedTasks").value(0))
            .andExpect(jsonPath("$.projects[0].totalSubtasks").value(3))
            .andExpect(jsonPath("$.projects[0].completedSubtasks").value(1))
            .andExpect(jsonPath("$.projects[0].project.progressPercentage").value(50))
            .andExpect(jsonPath("$.projects[0].project.status").value("PLANNING"));
    }

    @Test
    void cancelledOnlyProjectSuggestsPlanningAndMalformedFiltersUseSharedErrors() throws Exception {
        progress.cancel(task); em.flush();
        mvc.perform(get("/api/dashboard/homeowner")).andExpect(status().isOk())
            .andExpect(jsonPath("$.projects[0].nextAction").value("PLAN_TASKS"));
        for (String parameter : new String[] {"tradeId", "availability"}) {
            mvc.perform(get("/api/discovery/tradespeople").param(parameter, "invalid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/discovery/tradespeople"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
        }
    }

    private void assign() {
        ProjectTeam team = save(new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        save(new ProjectTeamTrade(team, trade));
        save(new TaskAssignment(task, worker));
        reload();
    }
    private void scope(Homeowner h, ProjectStatus ps, TaskStatus ts, Trade tr) {
        Project p = save(new Project("Scope", "Scope", ps, "10001", h));
        Task t = save(new Task("Task", "Task", ts, p, null));
        save(new TaskTrade(t, tr));
    }
    private <T> T save(T entity) { em.persist(entity); em.flush(); return entity; }
    private void reload() {
        em.flush(); em.clear();
        owner = em.find(Homeowner.class, owner.getId()); worker = em.find(Tradesperson.class, worker.getId());
        project = em.find(Project.class, project.getId()); task = em.find(Task.class, task.getId());
        requirement = em.find(TaskTrade.class, requirement.getId()); trade = em.find(Trade.class, trade.getId());
    }
    private void asOwner() { when(account.activeProfile()).thenReturn(owner); }
    private void asWorker() { when(account.activeProfile()).thenReturn(worker); }
}
