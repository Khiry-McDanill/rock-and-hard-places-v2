package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rockandhardplaces.account.*;

@WebMvcTest(AccountController.class)
class AccountControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext accountContext;

    @Test
    void returnsCurrentAccountAsDto() throws Exception {
        User user = mock(User.class);
        Homeowner homeowner = mock(Homeowner.class);
        when(user.getId()).thenReturn(17L);
        when(user.getEmail()).thenReturn("owner@example.com");
        when(homeowner.getId()).thenReturn(31L);
        when(homeowner.getDisplayName()).thenReturn("Owner");
        when(homeowner.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(accountContext.currentUser()).thenReturn(user);
        when(accountContext.activeRole()).thenReturn(AccountRole.HOMEOWNER);
        when(accountContext.activeProfile()).thenReturn(homeowner);

        mvc.perform(get("/api/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(17))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.activeRole").value("HOMEOWNER"))
                .andExpect(jsonPath("$.profile.id").value(31))
                .andExpect(jsonPath("$.profile.role").value("HOMEOWNER"));
    }

    @Test
    void switchesRoleThroughActiveAccountContext() throws Exception {
        User user = mock(User.class);
        Tradesperson profile = mock(Tradesperson.class);
        when(accountContext.currentUser()).thenReturn(user);
        when(accountContext.activeRole()).thenReturn(AccountRole.TRADESPERSON);
        when(accountContext.activeProfile()).thenReturn(profile);

        mvc.perform(post("/api/account/active-role")
                        .contentType("application/json")
                        .content("{\"role\":\"TRADESPERSON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("TRADESPERSON"));

        verify(accountContext).switchTo(AccountRole.TRADESPERSON);
    }

    @Test
    void validationErrorsUseSharedShape() throws Exception {
        mvc.perform(post("/api/account/active-role")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
        verify(accountContext, never()).switchTo(any());
    }
}
