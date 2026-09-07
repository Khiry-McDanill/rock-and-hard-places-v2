package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import com.rockandhardplaces.review.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ApiAccessService access;
    @MockitoBean TradespersonRepository tradespeople;
    @MockitoBean ReviewRepository reviews;
    @MockitoBean ReviewService service;

    private Homeowner owner;
    private Tradesperson subject;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        owner = mock(Homeowner.class); subject = mock(Tradesperson.class);
        project = mock(Project.class); task = mock(Task.class);
        when(account.activeProfile()).thenReturn(owner);
        when(tradespeople.findById(52L)).thenReturn(Optional.of(subject));
        when(access.project(31L, owner)).thenReturn(project);
        when(access.task(41L, owner)).thenReturn(task);
        when(project.getId()).thenReturn(31L); when(task.getProject()).thenReturn(project);
    }

    @Test
    void selfReviewIsRejected() throws Exception {
        rejection(new IllegalArgumentException("Self-review is not allowed"));
    }

    @Test
    void reviewWithoutEligibleCompletedWorkIsRejected() throws Exception {
        rejection(new IllegalArgumentException("Task must be completed through the RH&P workflow"));
    }

    @Test
    void duplicateReviewIsRejected() throws Exception {
        rejection(new IllegalStateException("This task review already exists"));
    }

    private void rejection(RuntimeException failure) throws Exception {
        when(service.createTaskReview(owner, subject, task, 5, null, null, null, null, "Great"))
                .thenThrow(failure);
        var result = mvc.perform(post("/api/reviews").contentType("application/json").content("""
                {"tradespersonId":52,"projectId":31,"taskId":41,"level":"TASK",
                 "overallRating":5,"body":"Great"}
                """));
        if (failure instanceof IllegalStateException) {
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("CONFLICT"));
        } else {
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }
        result.andExpect(jsonPath("$.message").value(failure.getMessage()))
                .andExpect(jsonPath("$.path").value("/api/reviews"));
        verify(service).createTaskReview(owner, subject, task, 5, null, null, null, null, "Great");
    }
}
