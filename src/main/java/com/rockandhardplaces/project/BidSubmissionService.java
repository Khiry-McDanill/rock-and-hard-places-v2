package com.rockandhardplaces.project;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rockandhardplaces.account.AccountAuthorizationService;
import com.rockandhardplaces.account.Tradesperson;

@Service
public class BidSubmissionService {
    private final BidRepository bids;
    private final AccountAuthorizationService authorization;

    public BidSubmissionService(BidRepository bids, AccountAuthorizationService authorization) {
        this.bids = bids;
        this.authorization = authorization;
    }

    @Transactional
    public Bid submit(Task task, TaskTrade taskTrade, Tradesperson bidder,
            BigDecimal amount, String message) {
        authorization.requireVerifiedBidder(bidder);
        authorization.requireDifferentUsers(bidder.getUser(), task.getProject().getHomeowner().getUser());
        if (bids.existsByTaskTradeAndTradesperson(taskTrade, bidder))
            throw new IllegalStateException("You already have a proposal for this scope. View your existing proposal.");
        try {
            return bids.saveAndFlush(new Bid(task, taskTrade, bidder, amount, message));
        } catch (org.springframework.dao.DataIntegrityViolationException conflict) {
            // The database remains the final guard when two submissions race.
            throw new IllegalStateException("The proposal could not be submitted because this scope or proposal changed. Refresh and view your proposals.", conflict);
        }
    }
}
