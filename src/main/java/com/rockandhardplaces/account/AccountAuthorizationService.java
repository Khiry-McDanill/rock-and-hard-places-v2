package com.rockandhardplaces.account;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AccountAuthorizationService {

    public void requireActive(Homeowner homeowner) {
        if (homeowner == null || homeowner.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new SecurityException("An active homeowner profile is required");
        }
    }

    public void requireActive(Tradesperson tradesperson) {
        if (tradesperson == null || tradesperson.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new SecurityException("An active tradesperson profile is required");
        }
    }

    public void requireVerifiedBidder(Tradesperson tradesperson) {
        requireActive(tradesperson);
        if (tradesperson.getVerificationStatus() != TradespersonVerificationStatus.VERIFIED) {
            throw new SecurityException("A verified tradesperson profile is required to submit bids");
        }
    }

    public void requireDifferentUsers(User first, User second) {
        if (first == second
                || (first != null
                && second != null
                && first.getId() != null
                && second.getId() != null
                && Objects.equals(first.getId(), second.getId()))) {
            throw new SecurityException("A user cannot interact with their own opposite profile");
        }
    }
}