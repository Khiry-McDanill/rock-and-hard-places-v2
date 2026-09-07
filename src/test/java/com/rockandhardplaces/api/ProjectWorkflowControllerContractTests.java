package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@WebMvcTest(ProjectController.class)
class ProjectWorkflowControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ApiAccessService access;
    @MockitoBean ProjectWorkflowService workflow;
    @MockitoBean TaskProgressService progress;
    @MockitoBean ProjectTeamRepository teams;

    @Test
    void createsProjectForActiveHomeownerAndCalculatesProgress() throws Exception {
        Homeowner owner = mock(Homeowner.class);
        Project project = mock(Project.class);
        when(account.activeProfile()).thenReturn(owner);
        when(workflow.create(owner, "Kitchen", "Renovate", "90210")).thenReturn(project);
        when(project.getStatus()).thenReturn(ProjectStatus.PLANNING);
        when(progress.progressPercentage(project)).thenReturn(40);

        mvc.perform(post("/api/projects").contentType("application/json")
                        .content("{\"title\":\"Kitchen\",\"description\":\"Renovate\",\"jobZip\":\"90210\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.progressPercentage").value(40));
        verify(workflow).create(owner, "Kitchen", "Renovate", "90210");
    }

    @Test
    void wrongRoleCannotCreateProject() throws Exception {
        when(account.activeProfile()).thenReturn(mock(Tradesperson.class));
        mvc.perform(post("/api/projects").contentType("application/json")
                        .content("{\"title\":\"Kitchen\",\"description\":\"Renovate\",\"jobZip\":\"90210\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(workflow);
    }

    @Test
    void ignoresClientOwnedWorkflowFieldsAndReturnsCalculatedValues() throws Exception {
        Homeowner owner = mock(Homeowner.class); Project project = mock(Project.class);
        when(account.activeProfile()).thenReturn(owner);
        when(workflow.create(owner, "Kitchen", "Renovate", "90210")).thenReturn(project);
        when(project.getStatus()).thenReturn(ProjectStatus.PLANNING);
        when(progress.progressPercentage(project)).thenReturn(40);
        mvc.perform(post("/api/projects").contentType("application/json")
                        .content("{\"title\":\"Kitchen\",\"description\":\"Renovate\",\"jobZip\":\"90210\",\"status\":\"COMPLETED\",\"progressPercentage\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.progressPercentage").value(40));
    }
}
