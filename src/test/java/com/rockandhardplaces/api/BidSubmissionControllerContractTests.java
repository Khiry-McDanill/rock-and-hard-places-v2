package com.rockandhardplaces.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@WebMvcTest(TaskController.class)
@Import({BidSubmissionService.class, AccountAuthorizationService.class})
class BidSubmissionControllerContractTests {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveAccountContext account;
    @MockitoBean ApiAccessService access;
    @MockitoBean BidRepository bids;
    @MockitoBean ProjectWorkflowService workflow;
    @MockitoBean TaskProgressService progress;
    @MockitoBean TaskAssignmentService assignmentService;
    @MockitoBean TradespersonRepository tradespeople;

    private User ownerUser;
    private Homeowner owner;
    private Task task;
    private TaskTrade taskTrade;

    @BeforeEach
    void setUp() {
        ownerUser = new User("owner@example.com");
        owner = new Homeowner(ownerUser, "Owner");
        Project project = new Project("Project", "Description", ProjectStatus.PLANNING, "90210", owner);
        task = new Task("Task", "Description", TaskStatus.PLANNING, project, null);
        taskTrade = mock(TaskTrade.class);
        when(taskTrade.getTask()).thenReturn(task);
        when(access.biddingTask(41L)).thenReturn(task);
        when(access.taskTrade(52L, task)).thenReturn(taskTrade);
        when(bids.saveAndFlush(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void verifiedActiveTradespersonCanSubmitBid() throws Exception {
        Tradesperson bidder = bidder(new User("trade@example.com"),
                AccountStatus.ACTIVE, TradespersonVerificationStatus.VERIFIED);
        when(account.activeProfile()).thenReturn(bidder);

        mvc.perform(bidRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.amount").value(125.00));
        verify(bids).saveAndFlush(any(Bid.class));
    }

    @Test
    void unverifiedTradespersonCannotSubmitBid() throws Exception {
        assertRejected(bidder(new User("trade@example.com"), AccountStatus.ACTIVE,
                TradespersonVerificationStatus.NOT_SUBMITTED));
    }

    @Test
    void suspendedTradespersonCannotSubmitBid() throws Exception {
        assertRejected(bidder(new User("trade@example.com"), AccountStatus.SUSPENDED,
                TradespersonVerificationStatus.VERIFIED));
    }

    @Test
    void deactivatedTradespersonCannotSubmitBid() throws Exception {
        assertRejected(bidder(new User("trade@example.com"), AccountStatus.DEACTIVATED,
                TradespersonVerificationStatus.VERIFIED));
    }

    @Test
    void sameUserTradespersonCannotSubmitBid() throws Exception {
        assertRejected(bidder(ownerUser, AccountStatus.ACTIVE, TradespersonVerificationStatus.VERIFIED));
    }

    private void assertRejected(Tradesperson bidder) throws Exception {
        when(account.activeProfile()).thenReturn(bidder);
        mvc.perform(bidRequest()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verify(bids, never()).saveAndFlush(any(Bid.class));
    }

    private Tradesperson bidder(User user, AccountStatus status,
            TradespersonVerificationStatus verification) {
        Tradesperson bidder = new Tradesperson(user, "Tradesperson");
        bidder.setAccountStatus(status); bidder.setVerificationStatus(verification);
        return bidder;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder bidRequest() {
        return post("/api/tasks/41/bids").contentType("application/json")
                .content("{\"taskTradeId\":52,\"amount\":125.00,\"message\":\"Ready to work\"}");
    }
}
