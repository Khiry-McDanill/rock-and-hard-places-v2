# RH&P-030 — implementation and review

Status: READY FOR FINAL REVIEW, subject to the documented limitations below. No commit, push, PR, merge, or branch switch.

## Reused domain

User, Tradesperson, active-role context, account status authorization, existing profile image reference, AvailabilityStatus, base ZIP and service radius; PersonTrade qualifications and PersonSpecialty catalog selections; PortfolioItem, PortfolioService, provenance, source project/task relationships and publication approval rules; existing portfolio gallery and delivered-image resolution.

The repository has no standalone Person entity: the responsible human is the Tradesperson backed by User. Existing attachments belong to project conversations, so using them for unrelated profile uploads would incorrectly attach public identity media to project history.

## Additions and endpoints

Four additive, repeat-safe tables in schema.sql: professional_profiles (headline, bio, presentation, qualified primary trade selection), professional_businesses (optional representation), professional_credentials (explicit PERSONAL/BUSINESS scope), and professional_media (owner-associated raster bytes). Existing rows and seeded references are retained. No member/crew/coverage entities were added.

All paths below are under `/api/tradespeople/{id}`:

| Methods | Path | Behavior |
| --- | --- | --- |
| GET, PUT | /professional-profile | Read public/owner projection; save identity, optional business, primary trade, specialties, availability and service area |
| POST | /credentials | Create provided credential |
| PUT, DELETE | /credentials/{credentialId} | Edit/delete owned credential |
| POST | /media | Upload JPEG/PNG; inspect dimensions and re-encode as PNG |
| GET | /media/{key} | Deliver public profile/portfolio image |
| PUT | /photo | Select/remove an owned uploaded portrait |
| POST | /portfolio | Create self-reported external work |
| PUT, DELETE | /portfolio/{itemId} | Edit/delete owned external work only |

Existing team and assignment DTOs now include profileImageReference so uploaded portraits resolve consistently. Existing profile/discovery projections already carry it. Existing messaging UI has no portrait to update.

Mutations verify the active Tradesperson ID and active account state on the server. Homeowners and unrelated tradespeople cannot mutate records. Credential/item IDs are also scoped to their owner. Media selection cannot claim another profile's image. Client-supplied provenance or verification values confer no authority.

## Product behavior

- Owner-only Edit Profile supports display name, headline, bio, and person-first/business-first presentation. Business-first retains the human's name and role.
- Business is optional and distinct from personal identity. Name, description, website, role, years, logo, private phone/address persist. Business removal requires removing its credentials first.
- Qualified trades are visible; an existing qualification can be selected as primary. Additional qualification editing remains outside this UI because PersonTrade directly drives eligibility and has no pending approval state. Specialties are editable catalog selections and do not grant qualification or verification.
- Availability uses all four existing enum values. ZIP/radius persist on Tradesperson. Existing rules use availability for discovery/collaborator filtering, not as a bidding prohibition. Qualification, account verification and project scope eligibility remain intact.
- Existing docs explicitly leave radius units undefined. The UI uses “Service radius”; it does not display “unit unspecified” or infer a conversion. The optional question about establishing miles was unanswered during implementation.
- Personal and business LICENSE, INSURANCE and CERTIFICATION records support title, issuer, number, jurisdiction, issued/expiry dates and notes. Status is always Provided. Numbers and notes are owner-only. No verification workflow or credential inheritance was invented.
- Profile portrait uploads save immediately; business logo and external cover selections save with their forms. Images are limited to JPEG/PNG, 3 MB input and 16 megapixels, re-encoded to strip original payload/metadata. Delivered references take precedence across profile, discovery, team, assignment, account and See It Built surfaces. Removing a reference returns to existing portrait presentation behavior.
- External work supports create/edit/delete, title, description, completion date and one cover image. New/edited work is self-reported. Editing previously externally verified material deliberately downgrades it to self-reported so owner edits cannot retain an unsupported verification claim.
- RH&P work and its project/task provenance remain platform-derived and immutable through these endpoints. Existing homeowner media publication approvals are preserved.
- Account sidebar labels are Dashboard; homeowner eyebrow is Homeowner dashboard. Routes and project Overview labels remain unchanged.
- All eight idea pages share direct category links, one aria-current page, mobile scrolling and previous/all/next links. The See It Built conversion CTA is removed; the final 05 / Start remains.
- RHP-BUSINESS-CREW-FUTURE.md documents future memberships, responsible parties, authorized bidders, proposed workers and jurisdiction-aware credential coverage without implementing them.

## Completed-work audit

Inspected the current rhp.sqlite read-only, and the deterministic scenario builder. Every completed leaf scope below has one accepted bid and matching assignment. The three parent coordination tasks have no required trade, bid or assignment; their completion is derived from child scopes. They are valid aggregate history, not evidence of direct assignment. No deterministic history was missing or inconsistent; no bids or production rules were changed.

| Task ID | Completed task | Finding |
| --- | --- | --- |
| 2 | Fit maple base cabinets | Coherent competitive bid and assignment |
| 6 | Deliver cedar park bathroom renovation | A: valid aggregate; child scopes carry competitive history |
| 7 | Install shower valve and drain | Coherent competitive bid and assignment |
| 8 | Finish moisture-resistant walls | Coherent competitive bid and assignment |
| 9 | Install GFCI protection | Coherent competitive bid and assignment |
| 11 | Replace ledger and joists | Coherent competitive bid and assignment |
| 15 | Frame office partition | Coherent competitive bid and assignment |
| 22 | Deliver cedar park oak floor restoration | A: valid aggregate; child scopes carry competitive history |
| 23 | Replace damaged oak boards | Coherent competitive bid and assignment |
| 24 | Sand and seal first floor | Coherent competitive bid and assignment |
| 25 | Install matching thresholds | Coherent competitive bid and assignment |
| 27 | Move laundry shutoff valves | Coherent competitive bid and assignment |
| 30 | Deliver fishtown exterior water repairs | A: valid aggregate; child scopes carry competitive history |
| 31 | Repoint rear brick joints | Coherent competitive bid and assignment |
| 32 | Restore rear window trim | Coherent competitive bid and assignment |
| 33 | Seal masonry transitions | Coherent competitive bid and assignment |

No B (missing expected history) or C (invalid history) was found. Parent bid panels explain that proposals are recorded under individual scopes. A tradesperson's empty private bid list now says “You have no proposals” rather than implying no one proposed work. Regression coverage checks completed leaf bid/assignment/trade coherence and aggregate completion without fragile global counts.

## Validation and limitations

Fresh databases are exercised by integration tests. schema.sql also applied twice to a backup copy of the existing database, preserving profile names/media and passing foreign_key_check. The user's database was not replaced or edited.

Focused backend tests cover identity, optional business, specialties, unchanged eligibility, credentials, ownership/suspension, profile and business images, external CRUD and provenance protections, alongside the existing profile, portfolio, assignment, frontend-support and demo-history suites. Mockito requires an explicit JVM agent in this sandbox; the normal self-attachment attempt failed before tests could execute. The successful command uses `-DargLine=-javaagent:/Users/khiry/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar`.

Frontend validation: npm run typecheck, npm run build and npm test. Rendered tests cover person/business-first presentation, human identity, public vs owner controls, credential scope, protected project portfolio, Dashboard labels and all eight category navigators/CTAs.

Limitations: one portfolio cover per item (no multiple-image/caption model), no credential document upload, no new qualified-trade approval workflow, and no radius unit decision. Unreferenced uploaded media is retained; storage lifecycle/quotas are release-hardening work. Existing demo session infrastructure is reused. No browser automation tool or installed browser automation package was available, so no interactive browser walkthrough was performed.

Recommended commit: `feat: add professional profile management and final product polish (RHP-030)`

## Final check results

- Focused backend: 79 tests passed, 0 failures/errors across nine suites (including six new profile integration scenarios).
- Frontend: typecheck passed; production build passed; 60 tests passed.
- git diff --check: passed.
- Branch remains rhp-030-professional-profile.
- NO COMMIT / NO PUSH / NO PR / NO MERGE / NO BRANCH SWITCH.

Exact git status --short:

```text
 M frontend/src/api/types.ts
 M frontend/src/app/App.tsx
 M frontend/src/components/AppShell.tsx
 M frontend/src/features/homeowner/HomeOverview.tsx
 M frontend/src/features/homeowner/project/ProjectBids.tsx
 M frontend/src/features/homeowner/project/ProjectTeam.tsx
 M frontend/src/features/people.tsx
 M frontend/src/features/projectWork.tsx
 M frontend/src/features/projects.tsx
 M frontend/src/features/public/InspirationPage.tsx
 M frontend/src/features/public/SeeItBuilt.tsx
 M frontend/src/styles/inspiration.css
 M frontend/src/styles/portfolio.css
 M frontend/tests/inspiration.test.tsx
 M frontend/tests/workspace.test.tsx
 M src/main/java/com/rockandhardplaces/account/Tradesperson.java
 M src/main/java/com/rockandhardplaces/api/ApiDtos.java
 M src/main/java/com/rockandhardplaces/portfolio/PortfolioItem.java
 M src/main/resources/schema.sql
?? docs/RHP-030-IMPLEMENTATION-REPORT.md
?? docs/RHP-BUSINESS-CREW-FUTURE.md
?? frontend/src/features/professionalProfile.tsx
?? frontend/tests/professionalProfile.test.tsx
?? src/main/java/com/rockandhardplaces/api/ProfessionalProfileController.java
?? src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java
```

## Final owner-profile UX fixes (manual-review follow-up)

Root cause of the load blocker: the local process on port 8080 was still the RH&P-029 backend (PID 58495, writing rhp-029-portfolio-default-server.log). The new frontend's GET /api/tradespeople/1/professional-profile returned HTTP 404, while GET /api/account returned valid AVAILABLE_NOW / 19147 / radius 20. Restarting the development backend with the current RH&P-030 code restored the endpoint. The existing startup schema initialization ran against the existing database. No backend implementation or schema file was changed in this follow-up. A backup was made at /tmp/rhp030-before-owner-ux.sqlite before startup.

Missing service values now say not provided and open as blank editable fields (including an unselected availability and blank radius), rather than fabricated defaults. Save failures identify availability/service-area changes and retain the user's entries. Genuine load errors remain visible.

External Portfolio uses one section and the existing PortfolioCard: Add work sits in the header; each owner card has Edit/Delete in its own footer. The separate duplicate management list is removed. Public viewers see the same cards without management actions. PortfolioCard itself suppresses supplied actions for RHP_VERIFIED items. Credential records likewise display their owner actions beside the record, without a duplicate management list. Existing business/trade/media controls stay in their relevant form sections.

Exact files changed in this follow-up:

- frontend/src/components/PortfolioCard.tsx
- frontend/src/features/people.tsx
- frontend/src/features/professionalProfile.tsx
- frontend/src/styles/portfolio.css
- frontend/tests/professionalProfile.test.tsx
- src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java
- docs/RHP-030-IMPLEMENTATION-REPORT.md

Validation: typecheck and build passed; 65 frontend tests passed; all 7 focused ProfessionalProfileIntegrationTests passed; git diff --check passed. Backend regression covers initial GET without optional professional/business records and GET after persisted service-area update.

The earlier browser limitation is superseded: an existing temporary Playwright installation and browser were found. The live browser walkthrough passed owner profile load, visible ZIP/radius, edit/save/reload persistence, restore of original service data, action-specific failed-save recovery, editable missing-service fields, Add work, a single Walnut reading nook card with attached Edit/Delete, and the homeowner public profile without any management actions. Missing-data and failed-save cases used intercepted responses only. No browser page errors occurred. Screenshots: /tmp/rhp030-owner-card.png and /tmp/rhp030-public-card.png. The browser check ended in Homeowner mode, as requested.

NO COMMIT / NO PUSH / NO PR / NO MERGE / NO BRANCH SWITCH.

## Trades editor and See It Built role-context follow-up

Exact collaborator failure: while the demo account's active role was TRADESPERSON after editing My Profile, See It Built called GET /api/discovery/tradespeople unconditionally. The unchanged discovery service requires an active Homeowner and returned HTTP 403 with “The active homeowner profile is required.” The public component treated this expected access restriction as a transient request failure. Jordan's NOT_ACCEPTING_WORK status did not cause that 403: role authorization runs before discovery matching. No profile DTO, availability enum, qualification representation, discovery API shape, or migration regression was found.

See It Built now reads the active account context before requesting discovery. Active Homeowners receive the existing real discovery DTOs; Tradespeople see a clear Homeowner-profile explanation and a workspace link, with no forbidden discovery request. Anonymous and restricted contexts have distinct messages. Inspiration itself remains public. Genuine server/network failures retain retry and never become empty results. Only successful discovery with no matching candidates produces the no-matches state. There is no automatic role switch or authorization relaxation. Existing qualification, availability, specialty relevance, same-user exclusion and profile-link behavior remain intact.

The trades editor shows compact read-only qualified-trade chips. A sole qualified trade is displayed as the primary without a dropdown and saved as a presentation preference. Multiple qualified trades use native radio choices styled as compact selectors. Specialty chips use native checkboxes, visible selected/check states, focus outlines, 44-pixel touch targets and responsive wrapping. All specialty choices come from the existing catalog. Save changes uses the existing owner-authorized profile PUT. No qualification-request workflow exists; this limitation is stated in the editor. No qualification or eligibility is granted by these controls.

Exact files changed in this follow-up:

- frontend/src/features/professionalProfile.tsx
- frontend/src/features/public/SeeItBuilt.tsx
- frontend/src/features/public/seeItBuiltPeople.ts
- frontend/src/styles/portfolio.css
- frontend/tests/professionalProfile.test.tsx
- frontend/tests/seeItBuilt.test.tsx
- docs/RHP-030-IMPLEMENTATION-REPORT.md

No backend implementation or schema changes in this follow-up.

Validation: npm run typecheck and npm run build passed; 72 frontend tests passed. The existing focused backend profile, discovery and demo suites passed all 40 tests (7 ProfessionalProfileIntegrationTests, 19 FrontendSupportIntegrationTests, 14 DemoDataSeederTests). git diff --check passed.

Live browser verification passed single-trade presentation, native keyboard specialty toggling, save/reload persistence, restoration of the original specialty selection, compact desktop/mobile rendering without horizontal overflow, and multiple-primary selection using a read-only response fixture that was never saved. All four requested inspiration categories loaded real qualified and available people with valid profile links in Homeowner context:

| Category | Matched collaborators |
| --- | --- |
| Barns | Caleb Morgan — Carpentry; Marcus Reed — Electrical; Leah Bennett — Exterior Restoration |
| RVs | Owen Price — Carpentry; Marcus Reed — Electrical; Nina Alvarez — Plumbing |
| Containers | Owen Price — Carpentry; Marcus Reed — Electrical; Sofia Nguyen — Drywall |
| And Beyond | Owen Price — Carpentry; Nina Alvarez — Plumbing; Leah Bennett — Exterior Restoration |

Browser checks also passed Tradesperson context without a forbidden discovery request, anonymous public inspiration, failed-request retry distinct from a successful empty result, and no JavaScript page errors. Screenshots: /tmp/rhp030-trades-desktop.png, /tmp/rhp030-trades-mobile.png, /tmp/rhp030-people-beyond.png. The user's existing availability and service area were retained. The browser session ended in Homeowner mode.

NO COMMIT / NO PUSH / NO PR / NO MERGE / NO BRANCH SWITCH.

## Typography and readability follow-up

Targeted CSS adjustments retain the 16px application base, existing large Playfair editorial headings, Inter body/interface copy, and font weights. No global application scaling. Profile body/field text is 16px, helpers and completion dates 15px, qualification/provenance statuses 14px, and selectable specialty text 15px. See It Built collaborator contributions and inspiration reading copy are 16px; subordinate captions, trade labels and links are 15px. Shared account/navigation controls are at least 14px. Small-label letter spacing was reduced where appropriate.

Responsive adjustments support the larger type: collaborator cards use two columns at the intermediate breakpoint, public mobile header navigation wraps onto its own row, and workspace mobile account information wraps with sufficient space for the enlarged wordmark. Fieldsets and inputs can shrink to their containers. There are no placeholder-only label changes.

Intentional sub-14px text on the audited surfaces:

- 13px: editorial eyebrows, uppercase profile fact labels, and sidebar role label. These are short hierarchy markers; the associated values remain 16px. Reduced letter spacing and sufficient contrast preserve readability.
- 12px: bottom line of the compact mobile workspace brand lockup (increased from 8.8px). Its top line is 14.4px. This is logo lettering rather than instructions or account data; the linked logo retains its full accessible name.
- 13.28px: bottom line of the public mobile brand lockup, retained to preserve its logo proportions. The top line is larger than 14px.

No other visible text below 14px was found by the browser audit on the inspected profile and inspiration pages. This was a targeted audit of RH&P-030 surfaces and their shared shell/header/footer components, not a claim that every unrelated page in the site was audited.

Verified at 1440px, 943px and 390px: profile/trades editor, availability controls, portfolio cards, collaborator cards, inspiration supporting copy and navigation. Computed text-size checks passed, as did foreground/background contrast checks for the inspected small text (at least 4.5:1). No horizontal page overflow or JavaScript page errors. Visual screenshots were inspected for trades, portfolio, collaborators and mobile public header. Profile editing also reflowed at a 200% browser-zoom-equivalent viewport (720 CSS pixels at device scale 2 for a 1440px desktop); this was a viewport/device-scale simulation, not a native browser-menu zoom test. No profile changes were saved.

Validation: npm run typecheck passed; npm run build passed; all 72 frontend tests passed; git diff --check passed. Backend implementation/schema unchanged in this typography pass.

Exact files changed in this typography follow-up:

- frontend/src/features/professionalProfile.tsx (scoping class only)
- frontend/src/styles/base.css
- frontend/src/styles/inspiration.css
- frontend/src/styles/portfolio.css
- frontend/src/styles/public-home.css
- frontend/src/styles/see-it-built.css
- frontend/src/styles/shell.css
- docs/RHP-030-IMPLEMENTATION-REPORT.md

Browser audit log: /tmp/rhp030-typography-browser.log. Screenshots: /tmp/rhp030-type-{trades,service,portfolio,people,supporting,header}-{1440,943,390}.png and /tmp/rhp030-type-zoom-200.png.

NO COMMIT / NO PUSH / NO PR / NO MERGE / NO BRANCH SWITCH.

Latest exact git status --short:

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
 M src/main/java/com/rockandhardplaces/api/ApiDtos.java
 M src/main/java/com/rockandhardplaces/portfolio/PortfolioItem.java
 M src/main/resources/schema.sql
?? docs/RHP-030-IMPLEMENTATION-REPORT.md
?? docs/RHP-BUSINESS-CREW-FUTURE.md
?? frontend/src/features/professionalProfile.tsx
?? frontend/tests/professionalProfile.test.tsx
?? src/main/java/com/rockandhardplaces/api/ProfessionalProfileController.java
?? src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java
```

## Credential trust and dual-role follow-up — final validation

Implemented the requested trust semantics and server-backed owner evidence-reference boundary. Full policy and the explicit private-document limitation are in RHP-CREDENTIAL-TRUST.md. The additive schema change in this follow-up is required for the newly requested persisted credential status/reference/assessment; it was not used to address the earlier availability load failure. New/edited credentials remain Provided. Verified relevant evidence supports only its explicitly assessed trade; safety, manufacturer, equipment and continuing education never independently qualify a trade. No credential edit changes PersonTrade. Four small fictional historical examples are seeded once; existing owner credential facts are preserved.

Homeowner discovery now includes Jordan's Tradesperson profile. /people/:personId is available in both active roles; management still requires active Tradesperson plus matching ID. Homeowner self-view has no management controls or private credential fields. Marketplace submission/award/review/provenance guards remain unchanged. Added regression tests cover their same-user protections; owner-performed work remains documented future work.

Browser/source audit of all seven profiles (distinct portraits loaded, no owner controls in public views):

| Person | Trade / specialty | Credential state | Service area / availability | Portfolio provenance |
|---|---|---|---|---|
| Jordan Ellis | Carpentry / Built-in cabinetry | UBC Journeyman: PROVIDED, Union journeyman credential: VERIFIED | 19802 / radius 20 / NOT_ACCEPTING_WORK | Walnut reading nook: SELF_REPORTED |
| Nina Alvarez | Plumbing / Bathroom rough-ins | Plumbing equipment manufacturer training: PENDING_VERIFICATION | 19148 / radius 25 / AVAILABLE_SOON | Cedar Park bathroom renovation: RHP_VERIFIED |
| Marcus Reed | Electrical / Residential panel upgrades | Electrical trade license: VERIFIED | 19104 / radius 25 / AVAILABLE_SOON | Workshop lighting and power upgrade: SELF_REPORTED |
| Sofia Nguyen | Drywall / Plaster repair and finishing | Construction safety training: PROVIDED | 19130 / radius 25 / AVAILABLE_SOON | Small studio plaster and wall finish: SELF_REPORTED |
| Darius Cole | Flooring / Hardwood restoration | None provided | 19143 / radius 25 / AVAILABLE_SOON | Cedar Park oak floor restoration: RHP_VERIFIED |
| Leah Bennett | Exterior Restoration / Masonry and weatherproofing | None provided | 19119 / radius 25 / AVAILABLE_SOON | Fishtown exterior water repairs: RHP_VERIFIED, Germantown garden wall restoration: EXTERNALLY_VERIFIED |
| Owen Price | Carpentry / Finish carpentry | None provided | 19123 / radius 25 / AVAILABLE_SOON | Compact built-in storage: SELF_REPORTED |

Browser credential CRUD: created a temporary safety credential with owner evidence reference, confirmed Provided/non-qualifying status, edited/replaced its reference, switched Homeowner and confirmed the reference was absent, then deleted the temporary record. No private document bytes were uploaded. Jordan's existing UBC Journeyman record remains Provided and unchanged; the historical demo union record is separate. Jordan's service data remains ZIP 19802 / radius 20 / Not accepting work. Owner sees Add work and item-attached Edit/Delete; Walnut reading nook appears once. RH&P Projects have no edit/delete controls.

Final validation: npm run typecheck, npm run build, npm test (80 passed), focused backend (49 passed, zero failures/errors/skips), git diff --check passed. Backend suites: CredentialTrustTests, SelfDealingBoundaryTests, ProfessionalProfileIntegrationTests, FrontendSupportIntegrationTests, Rhp020AccountLifecycleTests, Rhp019PortfolioTests, ReviewControllerContractTests. Mockito agent supplied explicitly for sandbox-compatible tests.

Final browser typography checks passed at 1440/943/390, including profile controls, cards, collaborators and inspiration. No page overflow or small-text contrast failures. The new credential form also reflows at all three widths. Intentional sub-14px text remains the documented 13px hierarchy labels and 12/13.28px mobile logo lettering. Zoom check remains a viewport/device-scale simulation, not native menu zoom.

Artifacts: /tmp/rhp030-credential-browser.log; /tmp/rhp030-audit-{Jordan,Nina,Marcus,Sofia,Darius,Leah,Owen}.png; /tmp/rhp030-credential-editor-{1440,943,390}.png; /tmp/rhp030-typography-browser-final.log. Local database backup before credential migration: /tmp/rhp030-before-credential-trust.sqlite.

Exact files in the cumulative uncommitted RH&P-030 work (including prior authorized passes):

```text
frontend/src/api/types.ts
frontend/src/app/App.tsx
frontend/src/components/AppShell.tsx
frontend/src/components/PortfolioCard.tsx
frontend/src/features/homeowner/HomeOverview.tsx
frontend/src/features/homeowner/project/ProjectBids.tsx
frontend/src/features/homeowner/project/ProjectTeam.tsx
frontend/src/features/people.tsx
frontend/src/features/projectWork.tsx
frontend/src/features/projects.tsx
frontend/src/features/public/InspirationPage.tsx
frontend/src/features/public/SeeItBuilt.tsx
frontend/src/features/public/seeItBuiltPeople.ts
frontend/src/styles/base.css
frontend/src/styles/inspiration.css
frontend/src/styles/portfolio.css
frontend/src/styles/public-home.css
frontend/src/styles/see-it-built.css
frontend/src/styles/shell.css
frontend/tests/inspiration.test.tsx
frontend/tests/seeItBuilt.test.tsx
frontend/tests/workspace.test.tsx
src/main/java/com/rockandhardplaces/account/Tradesperson.java
src/main/java/com/rockandhardplaces/account/TradespersonRepository.java
src/main/java/com/rockandhardplaces/api/ApiDtos.java
src/main/java/com/rockandhardplaces/api/FrontendSupportService.java
src/main/java/com/rockandhardplaces/portfolio/PortfolioItem.java
src/main/resources/schema.sql
src/test/java/com/rockandhardplaces/api/FrontendSupportIntegrationTests.java
docs/RHP-030-IMPLEMENTATION-REPORT.md
docs/RHP-BUSINESS-CREW-FUTURE.md
docs/RHP-CREDENTIAL-TRUST.md
frontend/src/features/professionalProfile.tsx
frontend/tests/professionalProfile.test.tsx
src/main/java/com/rockandhardplaces/api/ProfessionalProfileController.java
src/main/java/com/rockandhardplaces/credential/CredentialSchemaMigration.java
src/main/java/com/rockandhardplaces/credential/CredentialTrust.java
src/main/java/com/rockandhardplaces/demo/DemoCredentialSeeder.java
src/test/java/com/rockandhardplaces/account/SelfDealingBoundaryTests.java
src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java
src/test/java/com/rockandhardplaces/credential/CredentialTrustTests.java
```

Exact final git status --short:

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
?? docs/RHP-BUSINESS-CREW-FUTURE.md
?? docs/RHP-CREDENTIAL-TRUST.md
?? frontend/src/features/professionalProfile.tsx
?? frontend/tests/professionalProfile.test.tsx
?? src/main/java/com/rockandhardplaces/api/ProfessionalProfileController.java
?? src/main/java/com/rockandhardplaces/credential/
?? src/main/java/com/rockandhardplaces/demo/DemoCredentialSeeder.java
?? src/test/java/com/rockandhardplaces/account/SelfDealingBoundaryTests.java
?? src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java
?? src/test/java/com/rockandhardplaces/credential/
```

No commit, push, PR, merge or branch switch. Stopped for review with the documented private-document delivery limitation.

## Final trust badges and public presentation

Six data-backed architectural marks implemented: Verified Trade, Credential Verified, Credential Provided, Pending Verification, Insured, RH&P Project. Exact predicates and documentation-only V3 possibilities are in RHP-CREDENTIAL-TRUST.md. No backend, schema, qualification, eligibility or seed changes in this presentation pass. No automatic credential verification.

Identity now contains the portrait and honors saved business-first/person-first preference. Trust summary follows, then trades/specialties, service area, Work with a story (RH&P Projects and External Portfolio), then Credentials & qualifications. Credentials have concise state marks and native detail disclosures for dates/jurisdiction. Removed public paragraphs about credential mechanics and the account-verification disclaimer block. Owner forms retain review guidance; evidence stays private. Work actions remain on the same external portfolio cards.

Audit: Jordan has Verified Trade Carpentry plus Provided/Verified credential records; Marcus has Verified Trade Electrical and Credential Verified; Nina has Pending Verification and RH&P Project provenance; Sofia has Credential Provided; Darius and Leah have RH&P Project provenance without skill badges; Owen has no trust marks. Leah retains separately labeled externally verified work. No Insured mark appears without verified/current insurance. All seven profiles checked at 1440, 943 and 390px; screenshots for Jordan/Nina/Marcus/Leah visually inspected. Saved presentation variants remain covered by rendering tests. No persisted profile or credential edits during this pass.

Accessibility: badge text 14px with readable text names, decorative SVG aria-hidden, squared borders and varied line treatments supplement color. Palette text contrast ratios: verified 8.62:1, provided 8.96:1, pending 6.12:1, project 10.38:1. No horizontal overflow at tested widths or 720px desktop zoom-equivalent layout. This was viewport reflow, not native browser-menu 200% zoom. No page errors.

Validation: typecheck/build passed; all 84 frontend tests passed; 19 focused backend tests passed (CredentialTrustTests, SelfDealingBoundaryTests, ProfessionalProfileIntegrationTests, Rhp020AccountLifecycleTests); git diff --check passed. Browser log /tmp/rhp030-badges-browser.log; screenshots /tmp/rhp030-badges-{name}-{1440,943,390}.png.

Files touched in this pass: frontend/src/components/TrustBadge.tsx, frontend/src/components/PortfolioCard.tsx, frontend/src/features/professionalProfile.tsx, frontend/src/features/people.tsx, frontend/src/styles/base.css, frontend/tests/professionalProfile.test.tsx, frontend/tests/workspace.test.tsx, docs/RHP-CREDENTIAL-TRUST.md, docs/RHP-030-IMPLEMENTATION-REPORT.md.

Exact final git status --short:

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

Stopped without commit or push.
