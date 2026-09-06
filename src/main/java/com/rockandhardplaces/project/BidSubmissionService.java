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
        return bids.saveAndFlush(new Bid(task, taskTrade, bidder, amount, message));
    }
}
