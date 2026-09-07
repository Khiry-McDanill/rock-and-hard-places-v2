package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@WebMvcTest(TaskController.class)
class TaskAssignmentControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ApiAccessService access;
    @MockitoBean BidSubmissionService bidSubmission;
    @MockitoBean ProjectWorkflowService workflow;
    @MockitoBean TaskProgressService progress;
    @MockitoBean TaskAssignmentService assignments;
    @MockitoBean TradespersonRepository tradespeople;

    @Test
    void eligibleAssignmentIsCreatedThroughEligibilityService() throws Exception {
        Homeowner owner = mock(Homeowner.class); Task task = mock(Task.class);
        Tradesperson worker = mock(Tradesperson.class); TaskAssignment assignment = mock(TaskAssignment.class);
        when(account.activeProfile()).thenReturn(owner); when(access.task(41L, owner)).thenReturn(task);
        when(tradespeople.findById(52L)).thenReturn(Optional.of(worker));
        when(assignments.createEligibleAssignment(task, worker)).thenReturn(assignment);
        when(assignment.getId()).thenReturn(63L); when(assignment.getTask()).thenReturn(task);
        when(task.getId()).thenReturn(41L); when(assignment.getTradesperson()).thenReturn(worker);
        when(worker.getId()).thenReturn(52L); when(worker.getDisplayName()).thenReturn("Worker");

        mvc.perform(request()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(63))
                .andExpect(jsonPath("$.tradespersonId").value(52));
        verify(assignments).createEligibleAssignment(task, worker);
    }

    @Test
    void ineligibleAssignmentIsRejectedWithoutBypassingEligibilityService() throws Exception {
        Homeowner owner = mock(Homeowner.class); Task task = mock(Task.class);
        Tradesperson worker = mock(Tradesperson.class);
        when(account.activeProfile()).thenReturn(owner); when(access.task(41L, owner)).thenReturn(task);
        when(tradespeople.findById(52L)).thenReturn(Optional.of(worker));
        when(assignments.createEligibleAssignment(task, worker))
                .thenThrow(new IllegalArgumentException("no matching qualifications"));

        mvc.perform(request()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        verify(assignments).createEligibleAssignment(task, worker);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request() {
        return post("/api/tasks/41/assignments").contentType("application/json")
                .content("{\"tradespersonId\":52}");
    }
}
