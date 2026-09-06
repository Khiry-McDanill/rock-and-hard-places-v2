package com.rockandhardplaces.account;

import org.springframework.stereotype.Component;

/** Replaceable stand-in for credential-backed session context. */
@Component
public class DemoActiveAccountContext implements ActiveAccountContext {
    public static final String DEMO_EMAIL = "demo@rockandhardplaces.local";

    private final UserRepository users;
    private final HomeownerRepository homeowners;
    private final TradespersonRepository tradespeople;
    private AccountRole activeRole = AccountRole.HOMEOWNER;

    public DemoActiveAccountContext(UserRepository users, HomeownerRepository homeowners,
            TradespersonRepository tradespeople) {
        this.users = users;
        this.homeowners = homeowners;
        this.tradespeople = tradespeople;
    }

    @Override public User currentUser() {
        return users.findByEmail(DEMO_EMAIL).orElseThrow(() -> new IllegalStateException("Demo user is not seeded"));
    }

    @Override public AccountRole activeRole() { return activeRole; }

    @Override public Object activeProfile() {
        User user = currentUser();
        return activeRole == AccountRole.HOMEOWNER
                ? homeowners.findByUser(user).orElseThrow()
                : tradespeople.findByUser(user).orElseThrow();
    }

    @Override public void switchTo(AccountRole role) {
        if (role == null) throw new IllegalArgumentException("Account role is required");
        User user = currentUser();
        if (role == AccountRole.HOMEOWNER) homeowners.findByUser(user).orElseThrow();
        else tradespeople.findByUser(user).orElseThrow();
        this.activeRole = role;
    }
}
