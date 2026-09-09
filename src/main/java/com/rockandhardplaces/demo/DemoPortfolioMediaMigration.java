package com.rockandhardplaces.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** One-time persistence of the two legacy external covers formerly resolved by editable text. */
@Component
@Order(120)
@ConditionalOnProperty(name="rhp.demo.enabled",havingValue="true",matchIfMissing=true)
public class DemoPortfolioMediaMigration implements ApplicationRunner {
    private static final String VERSION="rhp-030-stable-portfolio-media-v1";
    private final JdbcTemplate jdbc;
    public DemoPortfolioMediaMigration(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_versions WHERE version=?",Integer.class,VERSION)>0) return;
        cover("demo@rockandhardplaces.local","Walnut reading nook","jordan-ellis/walnut-reading-nook");
        cover("leah-bennett@demo.rockandhardplaces.local","Germantown garden wall restoration","leah-bennett/germantown-garden-wall-restoration");
        jdbc.update("INSERT INTO demo_seed_versions(version) VALUES(?)",VERSION);
    }
    private void cover(String email,String title,String file) {
        jdbc.update("UPDATE portfolio_items SET media_reference=? WHERE title=? AND provenance<>'RHP_VERIFIED' AND media_reference IS NULL AND tradesperson_id IN (SELECT t.id FROM tradespeople t JOIN users u ON u.id=t.user_id WHERE u.email=?)",
                "/seed-media/portfolios/"+file+".jpg",title,email);
    }
}
