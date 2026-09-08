package com.rockandhardplaces.account;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.core.annotation.Order(0)
@Component
public class DemoAccountSeeder implements ApplicationRunner {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final UserRepository users; private final HomeownerRepository homeowners;
    private final TradespersonRepository tradespeople;
    public DemoAccountSeeder(UserRepository users, HomeownerRepository homeowners, TradespersonRepository tradespeople, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.users = users; this.homeowners = homeowners; this.tradespeople = tradespeople;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        User user = users.findByEmail(DemoActiveAccountContext.DEMO_EMAIL)
                .orElseGet(() -> users.save(new User(DemoActiveAccountContext.DEMO_EMAIL)));
        // Repair exact legacy fixture labels; retain user edits and internal seed identity.
        jdbc.update("UPDATE homeowners SET display_name = 'Jordan Ellis' WHERE user_id = ? AND display_name = 'Demo Homeowner'", user.getId());
        jdbc.update("UPDATE tradespeople SET display_name = 'Jordan Ellis' WHERE user_id = ? AND display_name = 'Demo Tradesperson'", user.getId());
        homeowners.findByUser(user).orElseGet(() -> homeowners.save(new Homeowner(user, "Jordan Ellis")));
        tradespeople.findByUser(user).orElseGet(() -> tradespeople.save(new Tradesperson(user, "Jordan Ellis")));
    }
}
