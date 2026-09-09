package com.rockandhardplaces.credential;
import static org.assertj.core.api.Assertions.*;
import static com.rockandhardplaces.credential.CredentialTrust.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
class CredentialTrustTests {
 @Test void onlyReviewedRelevantPersonalEvidenceSupportsAnAssessedTrade() {
  for(var kind:EvidenceKind.values()) {
   String type=kind==EvidenceKind.TRADE_LICENSE?"LICENSE":"CERTIFICATION";
   boolean strong=java.util.Set.of(EvidenceKind.TRADE_LICENSE,EvidenceKind.UNION_TRADE_CREDENTIAL,EvidenceKind.APPRENTICESHIP_COMPLETION,EvidenceKind.TRADE_SCHOOL_COMPLETION,EvidenceKind.INDUSTRY_CERTIFICATION).contains(kind);
   assertThat(supportsQualification(Status.VERIFIED,"PERSONAL",type,kind,1L)).isEqualTo(strong);
   assertThat(supportsQualification(Status.PROVIDED,"PERSONAL",type,kind,1L)).isFalse();
   assertThat(supportsQualification(Status.PENDING_VERIFICATION,"PERSONAL",type,kind,1L)).isFalse();
   assertThat(supportsQualification(Status.VERIFIED,"BUSINESS",type,kind,1L)).isFalse();
   assertThat(supportsQualification(Status.VERIFIED,"PERSONAL",type,kind,null)).isFalse();
  }
 }
 @Test void legacyMigrationPreservesFactsAndDoesNotVerifyExistingCredentials() {
  var ds=new SingleConnectionDataSource("jdbc:sqlite::memory:",true);
  try {
   var jdbc=new JdbcTemplate(ds);jdbc.execute("CREATE TABLE professional_credentials(id INTEGER PRIMARY KEY,name TEXT)");
   jdbc.update("INSERT INTO professional_credentials(name) VALUES('Existing safety certificate')");
   var migration=new CredentialSchemaMigration(jdbc);migration.migrate();migration.migrate();
   assertThat(jdbc.queryForMap("SELECT name,verification_status,evidence_kind FROM professional_credentials")).containsEntry("name","Existing safety certificate").containsEntry("verification_status","PROVIDED").containsEntry("evidence_kind","OTHER");
  } finally {ds.destroy();}
 }
}
