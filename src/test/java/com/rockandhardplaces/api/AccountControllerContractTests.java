package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.NoSuchElementException;
import java.util.List;

import com.rockandhardplaces.account.*;

@WebMvcTest(AccountController.class)
class AccountControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext accountContext;

    @Test
    void returnsCurrentAccountAsDto() throws Exception {
        User user = mock(User.class);
        Homeowner homeowner = mock(Homeowner.class);
        Tradesperson tradesperson = mock(Tradesperson.class);
        when(user.getId()).thenReturn(17L);
        when(user.getEmail()).thenReturn("owner@example.com");
        when(homeowner.getId()).thenReturn(31L);
        when(homeowner.getDisplayName()).thenReturn("Owner");
        when(homeowner.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(homeowner.getProfileImageReference()).thenReturn("owner.jpg");
        when(tradesperson.getId()).thenReturn(32L);
        when(tradesperson.getDisplayName()).thenReturn("Builder");
        when(tradesperson.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(accountContext.currentUser()).thenReturn(user);
        when(accountContext.activeRole()).thenReturn(AccountRole.HOMEOWNER);
        when(accountContext.activeProfile()).thenReturn(homeowner);
        when(accountContext.availableProfiles()).thenReturn(List.of(homeowner, tradesperson));

        mvc.perform(get("/api/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(17))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.activeRole").value("HOMEOWNER"))
                .andExpect(jsonPath("$.profile.id").value(31))
                .andExpect(jsonPath("$.profile.role").value("HOMEOWNER"))
                .andExpect(jsonPath("$.profile.profileImageReference").value("owner.jpg"))
                .andExpect(jsonPath("$.profiles.length()").value(2))
                .andExpect(jsonPath("$.profiles[0].role").value("HOMEOWNER"))
                .andExpect(jsonPath("$.profiles[1].role").value("TRADESPERSON"));
    }

    @Test
    void switchesRoleThroughActiveAccountContext() throws Exception {
        User user = mock(User.class);
        Tradesperson profile = mock(Tradesperson.class);
        when(accountContext.currentUser()).thenReturn(user);
        when(accountContext.activeRole()).thenReturn(AccountRole.TRADESPERSON);
        when(accountContext.activeProfile()).thenReturn(profile);
        when(accountContext.availableProfiles()).thenReturn(List.of(profile));

        mvc.perform(post("/api/account/switch")
                        .contentType("application/json")
                        .content("{\"role\":\"TRADESPERSON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("TRADESPERSON"));

        verify(accountContext).switchTo(AccountRole.TRADESPERSON);
    }

    @Test
    void validationErrorsUseSharedShape() throws Exception {
        mvc.perform(post("/api/account/switch")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/account/switch"))
                .andExpect(jsonPath("$.fieldErrors.role").exists());
        verify(accountContext, never()).switchTo(any());
    }

    @Test
    void rejectsSwitchToProfileCurrentUserDoesNotOwn() throws Exception {
        doThrow(new NoSuchElementException("Profile is not owned by current user"))
                .when(accountContext).switchTo(AccountRole.TRADESPERSON);

        mvc.perform(post("/api/account/switch")
                        .contentType("application/json")
                        .content("{\"role\":\"TRADESPERSON\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/account/switch"));
    }
}
