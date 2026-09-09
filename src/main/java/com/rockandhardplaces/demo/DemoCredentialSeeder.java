package com.rockandhardplaces.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fictional historical review outcomes, not a runtime verification mechanism. */
@Component
@Order(110)
@DependsOn("credentialSchemaMigration")
@ConditionalOnProperty(name="rhp.demo.enabled",havingValue="true",matchIfMissing=true)
public class DemoCredentialSeeder implements ApplicationRunner {
    private static final String VERSION="rhp-030-credential-history-v1";
    private static final String VARIETY_VERSION="rhp-030-credential-variety-v2";
    private final JdbcTemplate jdbc;
    public DemoCredentialSeeder(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_versions WHERE version=?",Integer.class,VERSION)==0) {
        add("demo@rockandhardplaces.local","Union journeyman credential","CERTIFICATION","Carpenters union training program","UNION_TRADE_CREDENTIAL","VERIFIED","Carpentry",
                "Historical RH&P review confirmed the issuer record and its Carpentry scope.");
        add("marcus-reed@demo.rockandhardplaces.local","Electrical trade license","LICENSE","State licensing authority","TRADE_LICENSE","VERIFIED","Electrical",
                "Historical RH&P review confirmed the license record and its Electrical scope; no jurisdictional requirement is inferred.");
        add("nina-alvarez@demo.rockandhardplaces.local","Plumbing equipment manufacturer training","CERTIFICATION","Equipment manufacturer training program","MANUFACTURER_CERTIFICATION","PENDING_VERIFICATION",null,
                "Historical submission awaiting verification; manufacturer training alone does not establish Plumbing qualification.");
        add("sofia-nguyen@demo.rockandhardplaces.local","Construction safety training","CERTIFICATION","Safety training provider","SAFETY_CERTIFICATION","PROVIDED",null,
                "Owner-provided safety training; not verification of Drywall qualification.");
        jdbc.update("INSERT INTO demo_seed_versions(version) VALUES(?)",VERSION);
        }
        if(jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_versions WHERE version=?",Integer.class,VARIETY_VERSION)>0) return;
        add("nina-alvarez@demo.rockandhardplaces.local","Plumbing trade license","LICENSE","State licensing authority","TRADE_LICENSE","VERIFIED","Plumbing",
                "Fictional historical RH&P review confirmed the license record and Plumbing scope; no jurisdictional licensing requirement is inferred.");
        add("leah-bennett@demo.rockandhardplaces.local","Exterior restoration liability insurance","INSURANCE","Demo commercial insurance carrier","INSURANCE","VERIFIED",null,
                "Fictional historical RH&P review confirmed the personal policy for exterior restoration work and its coverage dates; insurance does not establish trade qualification.","2026-01-01","2027-01-01");
        add("darius-cole@demo.rockandhardplaces.local","Hardwood flooring installation training","CERTIFICATION","Flooring manufacturer training program","MANUFACTURER_CERTIFICATION","VERIFIED",null,
                "Fictional historical RH&P review confirmed the manufacturer's installation training record; this credential does not independently establish Flooring qualification.");
        jdbc.update("INSERT INTO demo_seed_versions(version) VALUES(?)",VARIETY_VERSION);
    }
    private void add(String email,String name,String type,String issuer,String kind,String status,String trade,String basis) {
        add(email,name,type,issuer,kind,status,trade,basis,null,null);
    }
    private void add(String email,String name,String type,String issuer,String kind,String status,String trade,String basis,String issuedDate,String expirationDate) {
        var owners=jdbc.queryForList("SELECT t.id FROM tradespeople t JOIN users u ON u.id=t.user_id WHERE u.email=?",Long.class,email);
        if(owners.isEmpty()) return;
        Long owner=owners.get(0);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM professional_credentials WHERE tradesperson_id=? AND name=?",Integer.class,owner,name)>0) return;
        Long tradeId=trade==null ? null : jdbc.queryForObject("SELECT pt.trade_id FROM person_trades pt JOIN trades t ON t.id=pt.trade_id WHERE pt.tradesperson_id=? AND t.name=?",Long.class,owner,trade);
        jdbc.update("""
            INSERT INTO professional_credentials(tradesperson_id,scope,type,name,issuer,number,jurisdiction,notes,
                evidence_kind,verification_status,confirmed_trade_id,verified_at,verification_basis,issued_date,expiration_date)
            VALUES(?,'PERSONAL',?,?,?,?,?,'Fictional demo credential history; no real document is stored.',?,?,?,?,?,?,?)
            """,owner,type,name,issuer,"DEMO-HISTORY","",kind,status,tradeId,
                status.equals("VERIFIED") ? "2026-01-07" : null,basis,issuedDate,expirationDate);
    }
}
