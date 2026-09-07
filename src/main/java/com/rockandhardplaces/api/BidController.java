package com.rockandhardplaces.api;

import org.springframework.web.bind.annotation.*;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@RestController
@RequestMapping("/api/bids")
class BidController {
    private final ActiveAccountContext accountContext;
    private final ApiAccessService access;
    private final BidAcceptanceService acceptance;

    BidController(ActiveAccountContext accountContext, ApiAccessService access,
            BidAcceptanceService acceptance) {
        this.accountContext = accountContext;
        this.access = access;
        this.acceptance = acceptance;
    }

    @PostMapping("/{bidId}/accept")
    ApiDtos.BidResponse accept(@PathVariable Long bidId) {
        Object profile = accountContext.activeProfile();
        if (!(profile instanceof Homeowner homeowner)) {
            throw new SecurityException("The active homeowner profile is required");
        }
        return ApiDtos.BidResponse.from(acceptance.acceptBid(access.bid(bidId), homeowner));
    }
}
