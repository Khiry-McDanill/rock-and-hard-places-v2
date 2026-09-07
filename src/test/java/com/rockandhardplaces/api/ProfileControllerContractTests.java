package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rockandhardplaces.account.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean HomeownerRepository homeowners;
    @MockitoBean TradespersonRepository tradespeople;

    @Test
    void returnsHomeownerProfileAsDto() throws Exception {
        Homeowner homeowner = mock(Homeowner.class);
        when(homeowner.getId()).thenReturn(31L);
        when(homeowner.getDisplayName()).thenReturn("Owner");
        when(homeowner.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(homeowner.getProfileImageReference()).thenReturn("owner.jpg");
        when(homeowners.findById(31L)).thenReturn(Optional.of(homeowner));

        mvc.perform(get("/api/homeowners/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.role").value("HOMEOWNER"))
                .andExpect(jsonPath("$.displayName").value("Owner"))
                .andExpect(jsonPath("$.profileImageReference").value("owner.jpg"));
    }

    @Test
    void returnsTradespersonProfileAsDto() throws Exception {
        Tradesperson tradesperson = mock(Tradesperson.class);
        when(tradesperson.getId()).thenReturn(44L);
        when(tradesperson.getDisplayName()).thenReturn("Builder");
        when(tradesperson.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(tradesperson.getVerificationStatus()).thenReturn(TradespersonVerificationStatus.VERIFIED);
        when(tradesperson.getBaseZip()).thenReturn("19801");
        when(tradesperson.getServiceRadius()).thenReturn(25);
        when(tradesperson.getAvailabilityStatus()).thenReturn(AvailabilityStatus.AVAILABLE_NOW);
        when(tradesperson.getProfileImageReference()).thenReturn("builder.jpg");
        when(tradespeople.findById(44L)).thenReturn(Optional.of(tradesperson));

        mvc.perform(get("/api/tradespeople/44"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(44))
                .andExpect(jsonPath("$.role").value("TRADESPERSON"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.baseZip").value("19801"))
                .andExpect(jsonPath("$.profileImageReference").value("builder.jpg"));
    }

    @Test
    void missingProfileUsesSharedNotFoundHandling() throws Exception {
        when(homeowners.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/homeowners/999"))
                .andExpect(status().isNotFound());
    }
}
