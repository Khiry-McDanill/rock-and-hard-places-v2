# Final bid actions, Find Work clarity and trade marks

My Bids now loads the active bidder's complete proposal history from GET /api/bids. Submitted cards have View proposal, Edit proposal and Withdraw proposal. View reveals the stored message alongside amount, task/project context and status. Editing uses PUT /api/bids/{id} and permits only positive amount (up to ten integer/two fractional digits) and message (up to 2,000 characters). The same record, bidder and task-trade relationship remain intact. POST /api/bids/{id}/withdraw reuses Bid.withdraw and persists WITHDRAWN; no deletion occurs. Both writes check active bidder ownership, same-user exclusions and SUBMITTED status, including a conditional status check at persistence. Accepted/rejected/withdrawn records cannot be edited or withdrawn. History retains the existing project/task visibility restrictions and can display unavailable scope context without breaking the card. Dashboard submitted counts are unchanged.

Find Work now says Work ZIP and “Find open project scopes matching your qualified trades.” Its empty state says “No matching open scopes right now” and explains that projects without an open trade requirement will not appear. Opportunity filtering is unchanged. Tests prove matching eligible scopes appear, no-task projects do not, self-owned scopes remain excluded and ZIP filters select only matching scopes. The original Hockessin Tree House at 19807 still has zero tasks and does not appear for Jordan.

Trade linework follows the six existing catalog names: Carpentry framing/joinery, Electrical circuit/conduit, Plumbing pipe/elbow, Drywall panel/seam, Flooring planks, Exterior Restoration facade/flashing. All use the existing compact mark styling, consistent strokes and readable text. A neutral Trade mark identifies a recorded qualification without asserting verification; Verified Trade still requires the exact matching verified assessment. Unknown future catalog trades retain a neutral frame. Insurance rules are unchanged: only current VERIFIED insurance produces Insured, never Uninsured from absent evidence. Credential details remain below work.

Seven-profile public browser audit at 1440/943/390:

| Person | Top trust marks |
|---|---|
| Jordan | Carpentry framing mark, Verified Trade |
| Marcus | Electrical circuit mark, Verified Trade |
| Nina | Plumbing pipe mark, Verified Trade |
| Sofia | Drywall panel mark, neutral Trade |
| Darius | Flooring plank mark, neutral Trade |
| Leah | Exterior Restoration facade mark, neutral Trade; Insured |
| Owen | Carpentry framing mark, neutral Trade |

All seven trust-summary screenshots visually inspected. Credential/provenance states retained; no false verification, no page errors, no horizontal overflow. Text remains 14px minimum in marks and uses explicit labels rather than color alone. The 720px viewport check is zoom-equivalent reflow, not native browser-menu zoom.

Real edit/withdraw browser tests ran against /tmp/rhp030-bid-browser.sqlite, an isolated copy. Edit persisted after reload with the same ID/count; withdrawal persisted and remained in history; accepted and withdrawn cards lacked modification controls. Find Work 19807 empty state and eligible alternate ZIP were checked. Screenshots: /tmp/rhp030-my-bids-{submitted,withdrawn,943,390}.png and /tmp/rhp030-find-work-empty.png. Logs: /tmp/rhp030-actions-browser.log and /tmp/rhp030-trade-marks-browser.log. The local backend was restored to the original rhp.sqlite afterward. Original proposal 27 remains SUBMITTED at 1200 with its original message. No user proposal was withdrawn or edited in the original database.

Validation: npm run typecheck passed; npm run build passed; npm test passed (90 tests). Focused backend passed (32 tests: FrontendSupportIntegrationTests 22, SelfDealingBoundaryTests 3, Rhp020AccountLifecycleTests 5, CredentialTrustTests 2). git diff --check passed. Tests cover authorization, editable/decided states, immutable relationship, preserved history, ZIP exclusions, six distinct accessible marks and status-specific card actions.

Files touched in this pass:

- frontend/src/features/work.tsx
- frontend/src/components/TrustBadge.tsx
- frontend/src/features/professionalProfile.tsx
- frontend/src/styles/base.css
- frontend/tests/workActions.test.tsx
- frontend/tests/workspace.test.tsx
- src/main/java/com/rockandhardplaces/api/ProposalController.java
- src/main/java/com/rockandhardplaces/api/FrontendSupportService.java
- src/test/java/com/rockandhardplaces/api/FrontendSupportIntegrationTests.java
- docs/RHP-030-BID-ACTIONS-AND-TRADE-MARKS.md

No schema, qualification policy, opportunity eligibility or seed changes in this pass. No commit or push.

Exact cumulative git status --short:

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
 M frontend/src/features/work.tsx
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
?? docs/RHP-030-BID-ACTIONS-AND-TRADE-MARKS.md
?? docs/RHP-030-IMPLEMENTATION-REPORT.md
?? docs/RHP-030-SEEDED-BADGE-VARIETY.md
?? docs/RHP-BUSINESS-CREW-FUTURE.md
?? docs/RHP-CREDENTIAL-TRUST.md
?? frontend/src/components/TrustBadge.tsx
?? frontend/src/features/professionalProfile.tsx
?? frontend/tests/professionalProfile.test.tsx
?? frontend/tests/workActions.test.tsx
?? src/main/java/com/rockandhardplaces/api/ProfessionalProfileController.java
?? src/main/java/com/rockandhardplaces/api/ProposalController.java
?? src/main/java/com/rockandhardplaces/credential/
?? src/main/java/com/rockandhardplaces/demo/DemoCredentialSeeder.java
?? src/test/java/com/rockandhardplaces/account/SelfDealingBoundaryTests.java
?? src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java
?? src/test/java/com/rockandhardplaces/credential/
```
