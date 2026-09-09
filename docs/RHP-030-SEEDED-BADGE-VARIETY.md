# Seeded badge variety pass

The one-time `rhp-030-credential-variety-v2` seed adds only three fictional credential histories. The original seed marker is preserved. Existing records and owner edits are not overwritten; deleted seeded credentials are not recreated on restart. No badge design, qualification policy, schema or eligibility change.

| Person | Seed addition in this pass | Result and justification |
|---|---|---|
| Jordan Ellis — Carpentry | None; retain verified union journeyman history | Verified Trade · Carpentry and Credential Verified from the existing explicit Carpentry assessment. Existing owner-provided credential remains Provided. |
| Marcus Reed — Electrical | None; retain verified state license history | Verified Trade · Electrical and Credential Verified from the existing explicit Electrical assessment. |
| Nina Alvarez — Plumbing | Verified Plumbing trade license | Verified Trade · Plumbing and Credential Verified; historical issuer/scope review links to her existing Plumbing qualification. Existing manufacturer training remains Pending Verification. |
| Leah Bennett — Exterior Restoration | Verified personal exterior-restoration liability insurance, issued 2026-01-01, expires 2027-01-01 | Insured while current, plus Credential Verified. No trade assessment or Verified Trade is added. |
| Sofia Nguyen — Drywall | None; retain provided safety training | Credential Provided; safety training does not establish a verified trade. |
| Darius Cole — Flooring | Verified hardwood installation manufacturer training | Credential Verified only; manufacturer training does not independently verify Flooring. |
| Owen Price — Carpentry | None | No credential/trade/insurance badge, retaining a lighter profile. |

All new verified records explicitly represent fictional prior RH&P review, dated 2026-01-07, with an internal review basis. No real licensing requirements, insurer coverage or credentials are asserted. No private documents are seeded. Leah's Insured badge naturally stops displaying after the fixed expiration date; the seed never renews it automatically. Existing RH&P Project and external-work provenance remain unchanged.

Files changed in this pass:

- `src/main/java/com/rockandhardplaces/demo/DemoCredentialSeeder.java`
- `src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java`
- `docs/RHP-030-SEEDED-BADGE-VARIETY.md`

Validation: 12 focused backend tests passed (10 ProfessionalProfileIntegrationTests + 2 CredentialTrustTests), including trade-specific assessment, insurance dates, non-qualifying manufacturer evidence, and no restoration after deletion. All 84 frontend tests passed. git diff --check passed. No frontend implementation or build configuration changed.

All seven public profiles audited at 1440/943/390px; no false badges, public owner controls, horizontal overflow or page errors. Screenshots for the three changed profiles were visually inspected. Owner controls still open correctly; portfolio provenance is unchanged. Browser log: /tmp/rhp030-variety-browser.log. Screenshots: /tmp/rhp030-variety-{name}-{1440,943,390}.png.

The existing local database was backed up to /tmp/rhp030-before-badge-variety.sqlite before restarting the development backend. The v2 seed successfully applied to this existing v1 database. No user profile fields were edited.

Exact cumulative git status --short (includes pre-existing uncommitted RH&P-030 work):


```text
 M frontend/src/api/types.ts
 M frontend/src/app/App.tsx
 M frontend/src/components/AppShell.tsx
 M frontend/src/components/PortfolioCard.tsx
 M frontend/src/features/homeowner/HomeOverview.tsx
 M frontend/src/features/homeowner/project/ProjectBids.tsx
 M frontend/src/features/homeowner/project/ProjectTeam.tsx
 M frontend/src/features/people.tsx
 M frontend/src/features/projectWork.tsx
 M frontend/src/features/projects.tsx
 M frontend/src/features/public/InspirationPage.tsx
 M frontend/src/features/public/SeeItBuilt.tsx
 M frontend/src/features/public/seeItBuiltPeople.ts
 M frontend/src/styles/base.css
 M frontend/src/styles/inspiration.css
 M frontend/src/styles/portfolio.css
 M frontend/src/styles/public-home.css
 M frontend/src/styles/see-it-built.css
 M frontend/src/styles/shell.css
 M frontend/tests/inspiration.test.tsx
 M frontend/tests/seeItBuilt.test.tsx
 M frontend/tests/workspace.test.tsx
 M src/main/java/com/rockandhardplaces/account/Tradesperson.java
 M src/main/java/com/rockandhardplaces/account/TradespersonRepository.java
 M src/main/java/com/rockandhardplaces/api/ApiDtos.java
 M src/main/java/com/rockandhardplaces/api/FrontendSupportService.java
 M src/main/java/com/rockandhardplaces/portfolio/PortfolioItem.java
 M src/main/resources/schema.sql
 M src/test/java/com/rockandhardplaces/api/FrontendSupportIntegrationTests.java
?? docs/RHP-030-IMPLEMENTATION-REPORT.md
?? docs/RHP-030-SEEDED-BADGE-VARIETY.md
?? docs/RHP-BUSINESS-CREW-FUTURE.md
?? docs/RHP-CREDENTIAL-TRUST.md
?? frontend/src/components/TrustBadge.tsx
?? frontend/src/features/professionalProfile.tsx
?? frontend/tests/professionalProfile.test.tsx
?? src/main/java/com/rockandhardplaces/api/ProfessionalProfileController.java
?? src/main/java/com/rockandhardplaces/credential/
?? src/main/java/com/rockandhardplaces/demo/DemoCredentialSeeder.java
?? src/test/java/com/rockandhardplaces/account/SelfDealingBoundaryTests.java
?? src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java
?? src/test/java/com/rockandhardplaces/credential/
```

No commit or push.
