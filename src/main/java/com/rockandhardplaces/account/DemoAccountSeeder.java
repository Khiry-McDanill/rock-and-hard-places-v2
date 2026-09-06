package com.rockandhardplaces.account;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoAccountSeeder implements ApplicationRunner {
    private final UserRepository users; private final HomeownerRepository homeowners;
    private final TradespersonRepository tradespeople;
    public DemoAccountSeeder(UserRepository users, HomeownerRepository homeowners, TradespersonRepository tradespeople) {
        this.users = users; this.homeowners = homeowners; this.tradespeople = tradespeople;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        User user = users.findByEmail(DemoActiveAccountContext.DEMO_EMAIL)
                .orElseGet(() -> users.save(new User(DemoActiveAccountContext.DEMO_EMAIL)));
        homeowners.findByUser(user).orElseGet(() -> homeowners.save(new Homeowner(user, "Demo Homeowner")));
        tradespeople.findByUser(user).orElseGet(() -> tradespeople.save(new Tradesperson(user, "Demo Tradesperson")));
    }
}
