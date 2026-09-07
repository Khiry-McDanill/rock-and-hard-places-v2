package com.rockandhardplaces.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@WebMvcTest(BidController.class)
class BidControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext accountContext;
    @MockitoBean ApiAccessService access;
    @MockitoBean BidAcceptanceService acceptance;

    @Test
    void acceptsBidThroughDomainServiceUsingActiveHomeowner() throws Exception {
        Homeowner homeowner = mock(Homeowner.class);
        Bid bid = bid(44L);
        when(accountContext.activeProfile()).thenReturn(homeowner);
        when(access.bid(44L)).thenReturn(bid);
        when(acceptance.acceptBid(bid, homeowner)).thenReturn(bid);

        mvc.perform(post("/api/bids/44/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(44))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(acceptance).acceptBid(bid, homeowner);
    }

    @Test
    void rejectsAcceptanceFromTradespersonProfile() throws Exception {
        when(accountContext.activeProfile()).thenReturn(mock(Tradesperson.class));

        mvc.perform(post("/api/bids/44/accept"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verifyNoInteractions(acceptance);
    }

    private Bid bid(Long id) {
        Bid bid = mock(Bid.class);
        Task task = mock(Task.class);
        TaskTrade taskTrade = mock(TaskTrade.class);
        Tradesperson tradesperson = mock(Tradesperson.class);
        when(bid.getId()).thenReturn(id);
        when(bid.getTask()).thenReturn(task);
        when(task.getId()).thenReturn(8L);
        when(bid.getTaskTrade()).thenReturn(taskTrade);
        when(taskTrade.getId()).thenReturn(9L);
        when(bid.getTradesperson()).thenReturn(tradesperson);
        when(tradesperson.getId()).thenReturn(10L);
        when(bid.getAmount()).thenReturn(new BigDecimal("125.00"));
        when(bid.getStatus()).thenReturn(BidStatus.ACCEPTED);
        return bid;
    }
}
