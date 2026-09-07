package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@WebMvcTest({TaskController.class, ProjectController.class})
@Import(TaskProgressService.class)
class TaskProgressControllerContractTests {
    @Autowired MockMvc mvc;
    @Autowired TaskProgressService progress;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ApiAccessService access;
    @MockitoBean BidSubmissionService bidSubmission;
    @MockitoBean ProjectWorkflowService workflow;
    @MockitoBean TaskAssignmentService assignments;
    @MockitoBean TradespersonRepository tradespeople;
    @MockitoBean ProjectTeamRepository teams;
    @MockitoBean TaskRepository tasks;
    @MockitoBean TaskAssignmentRepository taskAssignments;

    private final Homeowner owner = mock(Homeowner.class);
    private final Project project = new Project("Kitchen", "Renovate", ProjectStatus.IN_PROGRESS,
            "90210", owner);

    @BeforeEach
    void activeOwner() {
        when(account.activeProfile()).thenReturn(owner);
        when(access.project(1L, owner)).thenReturn(project);
    }

    @Test
    void creationIgnoresClientProgressAndReturnsServerCalculatedValue() throws Exception {
        Task task = new Task("Paint", "Walls", TaskStatus.PLANNING, project, null);
        when(workflow.createTask(owner, project, "Paint", "Walls", null, List.of())).thenReturn(task);
        when(access.task(2L, owner)).thenReturn(task);

        mvc.perform(post("/api/projects/1/tasks").contentType("application/json")
                        .content("""
                                {"title":"Paint","description":"Walls","requiredTradeIds":[],
                                 "progressPercentage":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.progressPercentage").value(0));
        verify(workflow).createTask(owner, project, "Paint", "Walls", null, List.of());
        mvc.perform(get("/api/tasks/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(0));
    }

    @Test
    void taskAndProjectTaskListReturnCalculatedLeafProgress() throws Exception {
        Task parent = new Task("Paint", "Walls", TaskStatus.IN_PROGRESS, project, null);
        new Task("First", "Room", TaskStatus.COMPLETED, project, parent);
        new Task("Second", "Room", TaskStatus.IN_PROGRESS, project, parent);
        when(access.task(2L, owner)).thenReturn(parent);

        mvc.perform(get("/api/tasks/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(50));
        mvc.perform(get("/api/projects/1/tasks")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].progressPercentage").value(50))
                .andExpect(jsonPath("$[1].progressPercentage").value(100))
                .andExpect(jsonPath("$[2].progressPercentage").value(0));
    }

    @Test
    void workflowResponsesCalculateProgressAfterTransition() throws Exception {
        Task task = new Task("Paint", "Walls", TaskStatus.IN_PROGRESS, project, null);
        Tradesperson worker = mock(Tradesperson.class);
        when(access.biddingTask(2L)).thenReturn(task);
        when(access.task(2L, owner)).thenReturn(task);
        doAnswer(invocation -> { progress.submitForReview(task); return null; })
                .when(workflow).readyForReview(worker, task);
        doAnswer(invocation -> { progress.approve(task); return null; })
                .when(workflow).approve(owner, task);
        doAnswer(invocation -> { progress.reject(task); return null; })
                .when(workflow).reject(owner, task);

        when(account.activeProfile()).thenReturn(worker);
        mvc.perform(post("/api/tasks/2/ready-for-review")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_REVIEW"))
                .andExpect(jsonPath("$.progressPercentage").value(0));
        when(account.activeProfile()).thenReturn(owner);
        mvc.perform(post("/api/tasks/2/reject")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progressPercentage").value(0));
        progress.submitForReview(task);
        mvc.perform(post("/api/tasks/2/approve")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercentage").value(100));
    }
}
