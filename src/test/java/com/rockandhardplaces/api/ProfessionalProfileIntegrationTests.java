package com.rockandhardplaces.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockandhardplaces.account.*;

@SpringBootTest(properties="rhp.demo.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ProfessionalProfileIntegrationTests {
    static final String DB=UUID.randomUUID().toString();
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {p.add("spring.datasource.url",()->"jdbc:sqlite:./target/profile-"+DB+".sqlite");}
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired DemoActiveAccountContext context;
    @Autowired com.rockandhardplaces.demo.DemoCredentialSeeder credentialSeeder;
    @Autowired com.rockandhardplaces.demo.DemoPortfolioMediaMigration mediaMigration;
    Long id;
    @BeforeEach void setup(){context.switchTo(AccountRole.TRADESPERSON);id=((Tradesperson)context.activeProfile()).getId();jdbc.update("DELETE FROM professional_credentials WHERE tradesperson_id=?",id);}
    @AfterEach void reset(){context.switchTo(AccountRole.HOMEOWNER);}
    String path(){return "/api/tradespeople/"+id;}
    Map<String,Object> identity(){
        Map<String,Object> v=new LinkedHashMap<>();
        v.put("displayName","Responsible Human");v.put("headline","Carpenter");v.put("bio","Careful work");v.put("presentation","PERSON_FIRST");v.put("baseZip","19801");v.put("serviceRadius",35);v.put("availabilityStatus","NOT_ACCEPTING_WORK");v.put("specialtyIds",List.of());return v;
    }
    Map<String,Object> business(){return Map.of("name","Independent Business","description","Restoration","website","https://example.com","phone","private phone","address","private address","yearsInBusiness",5,"role","Owner");}
    Map<String,Object> credential(String scope){return Map.of("scope",scope,"type","LICENSE","name","Contractor registration","issuer","Provider","number","private-number","jurisdiction","DE","issuedDate","2024-01-01","expirationDate","2027-01-01","notes","private notes");}
    void putIdentity(Map<String,Object> input) throws Exception {mvc.perform(put(path()+"/professional-profile").contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isOk());}
    @Test void legacyCoverIsStableAcrossTextEditsAndMigrationDoesNotUndoRemoval() throws Exception {
        Long item=jdbc.queryForObject("SELECT id FROM portfolio_items WHERE tradesperson_id=? AND title='Walnut reading nook'",Long.class,id);
        String reference="/seed-media/portfolios/jordan-ellis/walnut-reading-nook.jpg";
        assertThat(jdbc.queryForObject("SELECT media_reference FROM portfolio_items WHERE id=?",String.class,item)).isEqualTo(reference);
        putIdentity(identity());
        mvc.perform(put(path()+"/portfolio/"+item).contentType("application/json").content("{\"title\":\"New work title\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mediaReference").value(reference)).andExpect(jsonPath("$.completionDate").value("2025-11-14"));
        em.flush();em.clear();
        assertThat(jdbc.queryForObject("SELECT media_reference FROM portfolio_items WHERE id=?",String.class,item)).isEqualTo(reference);
        mvc.perform(put(path()+"/portfolio/"+item).contentType("application/json").content("{\"title\":\"Walnut reading nook\",\"description\":\"Same work\",\"mediaReference\":null}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mediaReference").isEmpty());
        em.flush();em.clear();mediaMigration.run(null);
        assertThat(jdbc.queryForObject("SELECT media_reference FROM portfolio_items WHERE id=?",String.class,item)).isNull();
    }

    @Test void ownerProfileLoadsBeforeOptionalProfessionalOrBusinessRecordsExist() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_profiles WHERE tradesperson_id=?",Integer.class,id)).isZero();
        mvc.perform(get(path()+"/professional-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.availabilityStatus").value("AVAILABLE_NOW"))
                .andExpect(jsonPath("$.profile.baseZip").value("19147"))
                .andExpect(jsonPath("$.profile.serviceRadius").value(20))
                .andExpect(jsonPath("$.identity.presentation").value("PERSON_FIRST"))
                .andExpect(jsonPath("$.identity.headline").value(""))
                .andExpect(jsonPath("$.business").isEmpty());
        putIdentity(identity());
        mvc.perform(get(path()+"/professional-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.baseZip").value("19801"))
                .andExpect(jsonPath("$.profile.serviceRadius").value(35))
                .andExpect(jsonPath("$.profile.availabilityStatus").value("NOT_ACCEPTING_WORK"));
    }
    @Test void identityBusinessServiceAreaAndSpecialtiesPersistWithoutQualificationEscalation() throws Exception {
        int qualifications=jdbc.queryForObject("SELECT COUNT(*) FROM person_trades WHERE tradesperson_id=?",Integer.class,id);
        var input=identity();input.put("business",business());input.put("presentation","BUSINESS_FIRST");
        Long specialty=jdbc.queryForObject("SELECT id FROM specialties LIMIT 1",Long.class);input.put("specialtyIds",List.of(specialty));putIdentity(input);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.identity.presentation").value("BUSINESS_FIRST")).andExpect(jsonPath("$.business.name").value("Independent Business")).andExpect(jsonPath("$.profile.displayName").value("Responsible Human")).andExpect(jsonPath("$.profile.baseZip").value("19801")).andExpect(jsonPath("$.profile.serviceRadius").value(35)).andExpect(jsonPath("$.profile.availabilityStatus").value("NOT_ACCEPTING_WORK")).andExpect(jsonPath("$.specialties[0].id").value(specialty));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM person_trades WHERE tradesperson_id=?",Integer.class,id)).isEqualTo(qualifications);
        // Existing eligibility is based on active/verified status and scope, not availability.
        mvc.perform(get("/api/dashboard/tradesperson")).andExpect(status().isOk()).andExpect(jsonPath("$.bidding.allowed").value(true));
        context.switchTo(AccountRole.HOMEOWNER);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.business.phone").doesNotExist()).andExpect(jsonPath("$.business.address").doesNotExist());
        context.switchTo(AccountRole.TRADESPERSON);input.remove("business");input.put("presentation","PERSON_FIRST");putIdentity(input);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.business").isEmpty());
    }
    @Test void ownerAndActiveStatusRequiredAndInputValidated() throws Exception {
        var input=identity();
        mvc.perform(put("/api/tradespeople/999999/professional-profile").contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isForbidden());
        context.switchTo(AccountRole.HOMEOWNER);
        mvc.perform(put(path()+"/professional-profile").contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isForbidden());
        context.switchTo(AccountRole.TRADESPERSON);input.put("baseZip","bad");
        mvc.perform(put(path()+"/professional-profile").contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
        ((Tradesperson)context.activeProfile()).setAccountStatus(AccountStatus.SUSPENDED);
        mvc.perform(post(path()+"/credentials").contentType("application/json").content(json.writeValueAsString(credential("PERSONAL")))).andExpect(status().isForbidden());
    }
    @Test void credentialsHaveSeparateScopeCrudAndProvidedTrust() throws Exception {
        mvc.perform(post(path()+"/credentials").contentType("application/json").content(json.writeValueAsString(credential("BUSINESS")))).andExpect(status().isBadRequest());
        var input=identity();input.put("business",business());putIdentity(input);
        for(String scope:List.of("PERSONAL","BUSINESS"))mvc.perform(post(path()+"/credentials").contentType("application/json").content(json.writeValueAsString(credential(scope)))).andExpect(status().isOk());
        Long credentialId=jdbc.queryForObject("SELECT id FROM professional_credentials WHERE tradesperson_id=? AND scope='BUSINESS'",Long.class,id);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.credentials[0].verificationStatus").value("PROVIDED")).andExpect(jsonPath("$.credentials[1].scope").value("BUSINESS"));
        var changed=new LinkedHashMap<String,Object>(credential("BUSINESS"));changed.put("name","Updated insurance");changed.put("type","INSURANCE");changed.put("verificationStatus","VERIFIED");
        mvc.perform(put(path()+"/credentials/"+credentialId).contentType("application/json").content(json.writeValueAsString(changed))).andExpect(status().isOk()).andExpect(jsonPath("$.credentials[1].verificationStatus").value("PROVIDED"));
        context.switchTo(AccountRole.HOMEOWNER);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.credentials[0].number").doesNotExist());
        mvc.perform(delete(path()+"/credentials/"+credentialId)).andExpect(status().isForbidden());context.switchTo(AccountRole.TRADESPERSON);
        mvc.perform(delete(path()+"/credentials/"+credentialId)).andExpect(status().isOk());
        mvc.perform(delete(path()+"/credentials/"+credentialId)).andExpect(status().isNotFound());
    }
    @Test void imagesAndExternalWorkAreServerBackedAndPlatformHistoryProtected() throws Exception {
        var raster=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(raster,"png",bytes);
        var uploaded=mvc.perform(post(path()+"/media").contentType("application/json").content(json.writeValueAsString(Map.of("base64",Base64.getEncoder().encodeToString(bytes.toByteArray()))))).andExpect(status().isOk()).andReturn();
        String reference=json.readTree(uploaded.getResponse().getContentAsString()).get("reference").asText();
        mvc.perform(get(reference)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(put(path()+"/photo").contentType("application/json").content(json.writeValueAsString(Map.of("reference",reference)))).andExpect(status().isOk()).andExpect(jsonPath("$.profileImageReference").value(reference));
        var input=identity();var b=new LinkedHashMap<String,Object>(business());b.put("logoReference",reference);input.put("business",b);putIdentity(input);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.business.logoReference").value(reference));
        var work=new LinkedHashMap<String,Object>(Map.of("title","External work","description","Built elsewhere","completionDate","2025-01-01","mediaReference",reference,"provenance","RHP_VERIFIED"));
        var created=mvc.perform(post(path()+"/portfolio").contentType("application/json").content(json.writeValueAsString(work))).andExpect(status().isOk()).andExpect(jsonPath("$.provenance").value("SELF_REPORTED")).andExpect(jsonPath("$.projectId").isEmpty()).andReturn();
        long itemId=json.readTree(created.getResponse().getContentAsString()).get("id").asLong();work.put("title","Updated external work");
        mvc.perform(put(path()+"/portfolio/"+itemId).contentType("application/json").content(json.writeValueAsString(work))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated external work"));
        work.remove("mediaReference");work.put("title","Metadata only");
        mvc.perform(put(path()+"/portfolio/"+itemId).contentType("application/json").content(json.writeValueAsString(work))).andExpect(status().isOk()).andExpect(jsonPath("$.mediaReference").value(reference));
        em.flush();em.clear();
        assertThat(jdbc.queryForObject("SELECT media_reference FROM portfolio_items WHERE id=?",String.class,itemId)).isEqualTo(reference);
        b.remove("logoReference");input.put("displayName","Renamed person");putIdentity(input);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.profile.profileImageReference").value(reference)).andExpect(jsonPath("$.business.logoReference").value(reference));
        mvc.perform(put(path()+"/photo").contentType("application/json").content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.profileImageReference").value(reference));
        work.put("mediaReference",null);
        mvc.perform(put(path()+"/portfolio/"+itemId).contentType("application/json").content(json.writeValueAsString(work))).andExpect(status().isOk()).andExpect(jsonPath("$.mediaReference").isEmpty());
        work.put("mediaReference",reference);
        mvc.perform(put(path()+"/portfolio/"+itemId).contentType("application/json").content(json.writeValueAsString(work))).andExpect(status().isOk()).andExpect(jsonPath("$.mediaReference").value(reference));
        b.put("logoReference",null);putIdentity(input);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.business.logoReference").isEmpty());
        b.put("logoReference",reference);putIdentity(input);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.business.logoReference").value(reference));
        long rhp=jdbc.queryForObject("SELECT id FROM portfolio_items WHERE provenance='RHP_VERIFIED' LIMIT 1",Long.class);
        mvc.perform(put(path()+"/portfolio/"+rhp).contentType("application/json").content(json.writeValueAsString(work))).andExpect(status().isForbidden());
        mvc.perform(delete(path()+"/portfolio/"+rhp)).andExpect(status().isForbidden());
        context.switchTo(AccountRole.HOMEOWNER);mvc.perform(delete(path()+"/portfolio/"+itemId)).andExpect(status().isForbidden());context.switchTo(AccountRole.TRADESPERSON);
        mvc.perform(delete(path()+"/portfolio/"+itemId)).andExpect(status().isOk());
        mvc.perform(put(path()+"/photo").contentType("application/json").content("{\"reference\":null}")).andExpect(status().isOk()).andExpect(jsonPath("$.profileImageReference").isEmpty());
    }
    @Test void unrelatedResourcesAndUnqualifiedPrimaryTradeCannotBeClaimed() throws Exception {
        Long other=jdbc.queryForObject("SELECT id FROM tradespeople WHERE id<>? LIMIT 1",Long.class,id);
        var input=identity();input.put("primaryTradeId",999999);
        mvc.perform(put(path()+"/professional-profile").contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
        jdbc.update("INSERT INTO professional_credentials(tradesperson_id,scope,type,name,issuer,number,jurisdiction,notes) VALUES(?,'PERSONAL','LICENSE','Other license','','','','')",other);
        Long credentialId=jdbc.queryForObject("SELECT id FROM professional_credentials WHERE tradesperson_id=?",Long.class,other);
        mvc.perform(put(path()+"/credentials/"+credentialId).contentType("application/json").content(json.writeValueAsString(credential("PERSONAL")))).andExpect(status().isNotFound());
        mvc.perform(delete(path()+"/credentials/"+credentialId)).andExpect(status().isNotFound());
        Long external=jdbc.queryForObject("SELECT id FROM portfolio_items WHERE tradesperson_id<>? AND provenance='SELF_REPORTED' LIMIT 1",Long.class,id);
        mvc.perform(put(path()+"/portfolio/"+external).contentType("application/json").content("{\"title\":\"Changed\",\"description\":\"Other work\"}")).andExpect(status().isForbidden());
        mvc.perform(delete(path()+"/portfolio/"+external)).andExpect(status().isForbidden());
        mvc.perform(put(path()+"/photo").contentType("application/json").content("{\"reference\":\"/api/tradespeople/999/media/other\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(path()+"/media").contentType("application/json").content("{\"base64\":\"bm90LWFuLWltYWdl\"}")).andExpect(status().isBadRequest());
    }
    @Test void fictionalReviewSeedIsVariedAndDoesNotRestoreOwnerDeletedCredentials() throws Exception {
        var statuses=jdbc.queryForList("SELECT verification_status FROM professional_credentials ORDER BY id",String.class);
        assertThat(statuses).contains("VERIFIED","PENDING_VERIFICATION","PROVIDED");
        int count=jdbc.queryForObject("SELECT COUNT(*) FROM professional_credentials",Integer.class);
        credentialSeeder.run(null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_credentials",Integer.class)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_credentials WHERE tradesperson_id=?",Integer.class,id)).isZero();
    }
    @Test void varietySeedHasTradeSpecificReviewInsuranceDatesAndNonQualifyingManufacturerEvidence() throws Exception {
        context.switchTo(AccountRole.HOMEOWNER);
        for(String email:List.of("nina-alvarez","leah-bennett","darius-cole","owen-price")) {
            Long person=jdbc.queryForObject("SELECT t.id FROM tradespeople t JOIN users u ON u.id=t.user_id WHERE u.email=?",Long.class,email+"@demo.rockandhardplaces.local");
            var response=mvc.perform(get("/api/tradespeople/"+person+"/professional-profile")).andExpect(status().isOk()).andReturn();
            var credentials=json.readTree(response.getResponse().getContentAsString()).get("credentials");
            if(email.equals("nina-alvarez")) {
                assertThat(credentials.toString()).contains("PENDING_VERIFICATION","Plumbing trade license");
                var license=java.util.stream.StreamSupport.stream(credentials.spliterator(),false).filter(c->c.get("type").asText().equals("LICENSE")).findFirst().orElseThrow();
                assertThat(license.get("supportsQualification").asBoolean()).isTrue();assertThat(license.get("confirmedTradeName").asText()).isEqualTo("Plumbing");
            } else if(email.equals("owen-price")) assertThat(credentials.isEmpty()).isTrue();
            else {
                assertThat(credentials.size()).isEqualTo(1);var c=credentials.get(0);
                assertThat(c.get("verificationStatus").asText()).isEqualTo("VERIFIED");assertThat(c.get("supportsQualification").asBoolean()).isFalse();
                if(email.equals("leah-bennett")) {assertThat(c.get("type").asText()).isEqualTo("INSURANCE");assertThat(c.get("issuedDate").asText()).isEqualTo("2026-01-01");assertThat(c.get("expirationDate").asText()).isEqualTo("2027-01-01");}
                else assertThat(c.get("evidenceKind").asText()).isEqualTo("MANUFACTURER_CERTIFICATION");
            }
        }
        jdbc.update("DELETE FROM professional_credentials WHERE name='Hardwood flooring installation training'");
        credentialSeeder.run(null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_credentials WHERE name='Hardwood flooring installation training'",Integer.class)).isZero();
    }
    @Test void credentialEvidenceIsOwnerOnlyAndEditsClearReviewedTradeAssessment() throws Exception {
        var input=new LinkedHashMap<String,Object>(credential("PERSONAL"));
        input.put("evidenceKind","TRADE_LICENSE");input.put("evidenceReference","owner-document-1");
        input.put("verificationStatus","VERIFIED");input.put("confirmedTradeId",1);
        mvc.perform(post(path()+"/credentials").contentType("application/json").content(json.writeValueAsString(input)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.credentials[0].verificationStatus").value("PROVIDED"))
            .andExpect(jsonPath("$.credentials[0].supportsQualification").value(false));
        Long cid=jdbc.queryForObject("SELECT id FROM professional_credentials WHERE tradesperson_id=?",Long.class,id);
        Long trade=jdbc.queryForObject("SELECT trade_id FROM person_trades WHERE tradesperson_id=? LIMIT 1",Long.class,id);
        jdbc.update("UPDATE professional_credentials SET verification_status='VERIFIED',confirmed_trade_id=?,verified_at='2026-01-07',verification_basis='Test authorized review' WHERE id=?",trade,cid);
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.credentials[0].supportsQualification").value(true));
        context.switchTo(AccountRole.HOMEOWNER);
        mvc.perform(get("/api/discovery/tradespeople")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.profile.id == "+id+")]").isNotEmpty());
        mvc.perform(get(path()+"/professional-profile")).andExpect(status().isOk())
            .andExpect(jsonPath("$.credentials[0].evidenceReference").doesNotExist())
            .andExpect(jsonPath("$.credentials[0].verificationBasis").doesNotExist());
        mvc.perform(put(path()+"/credentials/"+cid).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isForbidden());
        context.switchTo(AccountRole.TRADESPERSON);input.put("evidenceReference","replacement-document");
        mvc.perform(put(path()+"/credentials/"+cid).contentType("application/json").content(json.writeValueAsString(input)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.credentials[0].verificationStatus").value("PROVIDED"))
            .andExpect(jsonPath("$.credentials[0].supportsQualification").value(false))
            .andExpect(jsonPath("$.credentials[0].evidenceReference").value("replacement-document"));
        mvc.perform(get(path()+"/professional-profile")).andExpect(jsonPath("$.credentials[0].evidenceReference").value("replacement-document"));
        mvc.perform(delete(path()+"/credentials/"+cid)).andExpect(status().isOk());
    }
    @Test void completedDemoLeafScopesHaveCoherentBidsAssignmentsAndTradeCoverage() {
        var completed=jdbc.queryForList("SELECT t.id FROM tasks t JOIN projects p ON p.id=t.project_id JOIN homeowners h ON h.id=p.homeowner_id JOIN users u ON u.id=h.user_id WHERE t.status='COMPLETED' AND u.email LIKE '%@demo.rockandhardplaces.local' AND NOT EXISTS(SELECT 1 FROM tasks c WHERE c.parent_task_id=t.id)");
        assertThat(completed).isNotEmpty();
        for(var task:completed) {
            Long taskId=((Number)task.get("id")).longValue();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bids b JOIN task_assignments a ON a.task_id=b.task_id AND a.tradesperson_id=b.tradesperson_id JOIN task_trades tt ON tt.id=b.task_trade_id AND tt.task_id=b.task_id JOIN person_trades pt ON pt.tradesperson_id=a.tradesperson_id AND pt.trade_id=tt.trade_id WHERE b.task_id=? AND b.status='ACCEPTED'",Integer.class,taskId)).isPositive();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tasks t WHERE t.status='COMPLETED' AND EXISTS(SELECT 1 FROM tasks c WHERE c.parent_task_id=t.id AND c.status NOT IN ('COMPLETED','CANCELLED'))",Integer.class)).isZero();
    }
}
