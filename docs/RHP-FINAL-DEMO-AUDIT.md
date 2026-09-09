# RH&P — Final functional demo audit

> Re-audit, 2026-09-09: B-01–B-04 are resolved. **READY FOR DEMO within the existing single-presenter scope; 0 remaining blockers.** See [blocker fix evidence](RHP-FINAL-DEMO-BLOCKER-FIXES.md). The original audit below is retained as historical evidence; its POLISH and V3 findings remain deferred.

## 1. Audit date and scope

2026-09-09, America/New_York. **AUDIT ONLY.** Verdict: **NOT READY FOR DEMO**. Findings: **4 BLOCKER, 3 POLISH, 4 V3**.

The current uncommitted implementation was exercised through Chromium, HTTP requests, database reads, source inspection, and the existing test suites. PASS refers to the stated action and actor, not every possible state. PARTIAL identifies fixture or environment limits. No findings were repaired.

To preserve the existing demo data, SQLite's backup API copied the current database to `/tmp/rhp-final-audit.sqlite`. The unchanged backend was temporarily started against that copy. All audit writes—messages, profile edits, uploads, credentials, portfolios, bids, reviews, project and task creation—went to the copy through existing application flows. No seed records were patched or reseeded. The normal backend and original database were restored afterward, with Homeowner Jordan active. Original bid 27 remains $1,200, SUBMITTED; original database has 10 projects and no audit messages.

## 2. Branch and commit base

Branch: `rhp-030-professional-profile`.
Base: `3f05e663ec75522e659a4b1d320d132673e44ae4`.
All pre-existing tracked and untracked file hashes match the initial audit snapshot. The only workspace file added by this audit is this document. Existing uncommitted RH&P-030 work remains intact.

## 3. Baseline validation

| Check | Result |
| --- | --- |
| Frontend startup | Already running on localhost:5173; remained available |
| Backend startup | Already running on :8080; isolated copy and original-database restart both succeeded |
| Database | SQLite `rhp.sqlite`; integrity_check = ok; foreign_key_check = empty |
| Schema/migrations | SQL initialization enabled; Hibernate ddl-auto none; current credential migration/seed markers present; no audit schema changes |
| `npm run typecheck` (frontend) | PASS |
| `npm run build` (frontend) | PASS |
| `npm test` (frontend) | PASS: 90 tests, zero failures/skips |
| Full backend Maven test suite | PASS: 242 tests across 36 reports, zero failures/errors/skips |
| `git diff --check` | PASS |

Frontend commands ran using `--prefix frontend`, equivalent to running the requested scripts in that package. Backend command: `./mvnw -q -DargLine=-javaagent:/Users/khiry/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar test`; exit 0. The explicit Mockito agent supports this local JVM; no test or build configuration was changed.

Present data markers: rhp-023-v1; rhp-025-window-returns-carpentry-v1; rhp-029-collaborator-portfolio-v1; rhp-030-credential-history-v1; rhp-030-credential-variety-v2.

## 4. Public journey matrix

| Journey/action | Result | Evidence/limit |
| --- | --- | --- |
| Homepage initial state and timer | PASS | AFTER first; BEFORE after 7.3 seconds |
| Manual previous/next | PASS | Both change the image/state |
| Reduced motion | PASS | AFTER remains after 7.3 seconds |
| Touch swipe | PASS | Corrected Touch-event harness changes AFTER to BEFORE at 390px; initial missing Touch identifier was a harness error, not a product defect |
| Logo, Start Project, Find Work | PASS | Logo returns home; project form opens; wrong-role Find Work explains required workspace |
| Create project and task | PASS | Browser-created project and Carpentry scope persist through reload in isolated database |
| Public navigation/media/layout | PASS with P-02 | No broken requested images or document overflow at tested widths; small mobile copy remains |
| Collaborator → person → portfolio → back | PASS | Homes, Barns, Outdoor Spaces; correct identity, dialog opens, Escape restores card focus, browser back returns |
| Public portfolio → RH&P workspace link | FAIL B-01 | Nina's published work links into private project 2 and returns access-denied dead section |
| People API error | PASS | Injected HTTP 500 displays a specific suggestions error, no false zero-match claim; retry recovers |
| Guest error presentation | PARTIAL | Injected account 401 shows sign-in requirement; real anonymous session is unavailable in this demo account model |

## 5. Eight-category inspiration matrix

API people in every successful request: **Jordan Ellis, Caleb Morgan, Nina Alvarez, Marcus Reed, Sofia Nguyen, Darius Cole, Leah Bennett, Owen Price** (HTTP 200). The API list is broader than the category-eligible list. Jordan's initial NOT_ACCEPTING_WORK state excludes him from suggestions. Selection is evaluated against required trades and availability, not an arbitrary expectation that all eight render.

| Category | Expected trades | Eligible people | API people | Rendered people | Profile links | Result |
| --- | --- | --- | --- | --- | --- | --- |
| homes | Carpentry, Plumbing, Flooring | Caleb Morgan, Nina Alvarez, Darius Cole, Owen Price | 8 named above; 200 | Owen Price, Nina Alvarez, Darius Cole | /people/8, /people/3, /people/6 | PASS |
| barns | Carpentry, Electrical, Exterior Restoration | Caleb Morgan, Marcus Reed, Leah Bennett, Owen Price | 8 named above; 200 | Caleb Morgan, Marcus Reed, Leah Bennett | /people/2, /people/4, /people/7 | PASS |
| rvs | Carpentry, Electrical, Plumbing | Caleb Morgan, Nina Alvarez, Marcus Reed, Owen Price | 8 named above; 200 | Owen Price, Marcus Reed, Nina Alvarez | /people/8, /people/4, /people/3 | PASS |
| buses | Carpentry, Electrical, Flooring | Caleb Morgan, Marcus Reed, Darius Cole, Owen Price | 8 named above; 200 | Caleb Morgan, Marcus Reed, Darius Cole | /people/2, /people/4, /people/6 | PASS |
| tiny-homes | Carpentry, Plumbing, Drywall | Caleb Morgan, Nina Alvarez, Sofia Nguyen, Owen Price | 8 named above; 200 | Caleb Morgan, Nina Alvarez, Sofia Nguyen | /people/2, /people/3, /people/5 | PASS |
| containers | Carpentry, Electrical, Drywall | Caleb Morgan, Marcus Reed, Sofia Nguyen, Owen Price | 8 named above; 200 | Owen Price, Marcus Reed, Sofia Nguyen | /people/8, /people/4, /people/5 | PASS |
| outdoor-spaces | Carpentry, Exterior Restoration, Electrical | Caleb Morgan, Marcus Reed, Leah Bennett, Owen Price | 8 named above; 200 | Caleb Morgan, Leah Bennett, Marcus Reed | /people/2, /people/7, /people/4 | PASS |
| beyond | Carpentry, Plumbing, Exterior Restoration | Caleb Morgan, Nina Alvarez, Leah Bennett, Owen Price | 8 named above; 200 | Owen Price, Nina Alvarez, Leah Bennett | /people/8, /people/3, /people/7 | PASS |

Every category has its hero, concept directions, build sequence, planning content, See It Built, work/trade explanations, portraits, contribution copy, correct active category, navigation targets and exactly one closing Start Project conversion CTA. Each concept was opened/closed with focus returned; Idea/Build/Result controls were clicked and their selected state checked. No duplicate conversion CTA inside See It Built. Previous/next and All Ideas were followed through Homes/Barns; other category targets were inspected and all eight destination pages exercised. Responsive checks included Barns, Containers and Beyond at 943/390px.

## 6. Homeowner journey and work consistency

| Surface / action | Result | Evidence |
| --- | --- | --- |
| Dashboard, My Projects, Find tradespeople, Messages, My profile | PASS | Natural navigation; account navigation says Dashboard, project navigation Overview |
| Passyunk kitchen remodel, project 1 | PASS except reviews B-02 | Overview, Tasks, Team, Bids, Messages, Completion inspected |
| Hockessin Tree House, project 10 | PASS | PLANNING, ZIP 19807, no tasks/bids/assignments; empty state coherent |
| Completed project | PARTIAL | Jordan has no completed homeowner-owned fixture. Project 8's six sections exercised as authorized Tradesperson Jordan; projects 2/6/8 histories traced in database |
| Work approval | PASS | Task 3 READY_FOR_REVIEW → COMPLETED persisted; dashboard progress changed 33% → 67% |
| Completion feedback | FAIL B-02 | Existing review missing; newly published review persists in database but disappears from UI on refresh |
| Other-owner workspace | PASS authorization | Jordan denied private project 2 (403); this correctly enforced denial exposes bad public link B-01 |
| New project + trade task | PASS | Created via forms, reloaded, correct owner and Carpentry requirement retained |

Initial Passyunk has one parent coordination task and four child scopes: completed cabinet fitting (Caleb, accepted bid 2; Owen bid 1 rejected), plumbing ready for review (Nina, accepted bid 3), electrical in progress (Marcus, accepted bid 4), cancelled pantry niche without an award. The displayed 33% counts the three non-cancelled child scopes. Dashboard parent count 0/1 and child count 1/3 are distinguished.

All 37 initial task records were traced below. There are 13 completed trade-bearing leaf tasks with accepted bids and matching named assignments. Three completed coordination parents intentionally have no trade bid/assignment. Task nesting identifies coordination versus child scopes; tradesperson bid views say “You have no proposals for this task,” not that the entire project has no proposals. No missing accepted work-history record was found in this trace.

| Project / task | State | Required trade | Bids (ID:state:person) | Assignment |
| --- | --- | --- | --- | --- |
| 1 / 1: Deliver passyunk kitchen remodel | IN_PROGRESS | Coordination | None | None |
| 1 / 2: Fit maple base cabinets | COMPLETED | Carpentry | 2:ACCEPTED:Caleb Morgan,1:REJECTED:Owen Price | Caleb Morgan |
| 1 / 3: Relocate sink supply and waste | READY_FOR_REVIEW | Plumbing | 3:ACCEPTED:Nina Alvarez | Nina Alvarez |
| 1 / 4: Wire island outlets | IN_PROGRESS | Electrical | 4:ACCEPTED:Marcus Reed | Marcus Reed |
| 1 / 5: Add pantry niche | CANCELLED | Carpentry | None | None |
| 2 / 6: Deliver cedar park bathroom renovation | COMPLETED | Coordination | None | None |
| 2 / 7: Install shower valve and drain | COMPLETED | Plumbing | 5:ACCEPTED:Nina Alvarez | Nina Alvarez |
| 2 / 8: Finish moisture-resistant walls | COMPLETED | Drywall | 6:ACCEPTED:Sofia Nguyen | Sofia Nguyen |
| 2 / 9: Install GFCI protection | COMPLETED | Electrical | 7:ACCEPTED:Marcus Reed | Marcus Reed |
| 3 / 10: Deliver fishtown cedar deck | IN_PROGRESS | Coordination | None | None |
| 3 / 11: Replace ledger and joists | COMPLETED | Carpentry | 9:ACCEPTED:Caleb Morgan,8:REJECTED:Owen Price | Caleb Morgan |
| 3 / 12: Lay cedar decking | READY_FOR_REVIEW | Carpentry | 10:ACCEPTED:Caleb Morgan | Caleb Morgan |
| 3 / 13: Fit stair guards | IN_PROGRESS | Carpentry | 11:ACCEPTED:Caleb Morgan | Caleb Morgan |
| 4 / 14: Deliver mount airy attic framing | IN_PROGRESS | Coordination | None | None |
| 4 / 15: Frame office partition | COMPLETED | Carpentry | 12:ACCEPTED:Caleb Morgan | Caleb Morgan |
| 4 / 16: Build storage alcove | READY_FOR_REVIEW | Carpentry | 13:ACCEPTED:Caleb Morgan | Caleb Morgan |
| 4 / 17: Hang attic drywall | IN_PROGRESS | Drywall | 14:ACCEPTED:Sofia Nguyen | Sofia Nguyen |
| 5 / 18: Deliver fairmount plaster and drywall repairs | PLANNING | Coordination | None | None |
| 5 / 19: Patch stairwell ceiling | PLANNING | Drywall | 15:SUBMITTED:Sofia Nguyen | None |
| 5 / 20: Skim living room walls | PLANNING | Drywall | 16:SUBMITTED:Sofia Nguyen | None |
| 5 / 21: Finish window returns | PLANNING | Carpentry,Drywall | 27:SUBMITTED:Jordan Ellis,17:SUBMITTED:Sofia Nguyen | None |
| 6 / 22: Deliver cedar park oak floor restoration | COMPLETED | Coordination | None | None |
| 6 / 23: Replace damaged oak boards | COMPLETED | Flooring | 18:ACCEPTED:Darius Cole | Darius Cole |
| 6 / 24: Sand and seal first floor | COMPLETED | Flooring | 19:ACCEPTED:Darius Cole | Darius Cole |
| 6 / 25: Install matching thresholds | COMPLETED | Flooring | 20:ACCEPTED:Darius Cole | Darius Cole |
| 7 / 26: Deliver fairmount utility room coordination | IN_PROGRESS | Coordination | None | None |
| 7 / 27: Move laundry shutoff valves | COMPLETED | Plumbing | 21:ACCEPTED:Nina Alvarez | Nina Alvarez |
| 7 / 28: Run dedicated laundry circuit | READY_FOR_REVIEW | Electrical | 22:ACCEPTED:Marcus Reed | Marcus Reed |
| 7 / 29: Close utility wall | IN_PROGRESS | Drywall | 23:ACCEPTED:Sofia Nguyen | Sofia Nguyen |
| 8 / 30: Deliver fishtown exterior water repairs | COMPLETED | Coordination | None | None |
| 8 / 31: Repoint rear brick joints | COMPLETED | Exterior Restoration | 24:ACCEPTED:Leah Bennett | Leah Bennett |
| 8 / 32: Restore rear window trim | COMPLETED | Carpentry | 25:ACCEPTED:Jordan Ellis | Jordan Ellis |
| 8 / 33: Seal masonry transitions | COMPLETED | Exterior Restoration | 26:ACCEPTED:Leah Bennett | Leah Bennett |
| 9 / 34: Deliver mount airy side porch replacement | CANCELLED | Coordination | None | None |
| 9 / 35: Survey porch framing | CANCELLED | Carpentry | None | None |
| 9 / 36: Replace porch boards | CANCELLED | Carpentry | None | None |
| 9 / 37: Repair porch foundation | CANCELLED | Exterior Restoration | None | None |

## 7. Tradesperson journey, Find Work, messages and self-dealing

| Surface / action | Result | Evidence |
| --- | --- | --- |
| Dashboard, My Work, Find Work, My Bids, Messages, My Profile | PASS | Followed normal navigation |
| Opportunity detail | PASS display; FAIL submit B-03 | Correct task, homeowner, ZIP, required trade and existing proposal |
| ZIP 19130 + Carpentry | PASS | Positive scope: task 21, Finish window returns, Fairmount plaster and drywall repairs, homeowner Ruth Chen |
| Same ZIP + Plumbing | PASS | No eligible wrong-trade scope |
| ZIP 19807 | PASS | Empty; Jordan's Hockessin project has no open trade needs and is not an own-user opportunity |
| Change ZIP/filter | PASS | Results update; explicit empty state |
| Project team and direct messages | PASS | Sent in conversation 1 (Passyunk team) and 2 (Caleb direct); reload retains messages and real names; own sender shows You |
| Private conversation 4 | PASS | Both read and send denied 403 for non-participant Jordan |
| Self bid | PASS | Own freshly created Carpentry task rejected 403: “A user cannot interact with their own opposite profile” |
| Self review | PASS | Task review POST rejected 400: “Self-review is not allowed” |
| Self award / manufactured RH&P history | PASS, service-test coverage | Existing SelfDealingBoundaryTests deny award and RHP_VERIFIED creation before writes; runtime verified portfolio edit/delete denied 403 |

Underlying user identity, not different profile IDs, enforces self-dealing. No owner-performed-work feature was implemented. Message membership is user-based, so Jordan's permitted conversations across his two roles are not evidence of private data leakage.

## 8. Dual-role matrix

| Role / target | Identity | Management actions | Result |
| --- | --- | --- | --- |
| Homeowner Jordan → discovery Jordan | Same professional Jordan | No Edit Profile, Add work, Edit/Delete | PASS |
| Tradesperson Jordan → My Profile | Same professional Jordan | Profile editor, credential controls, Add work, own-card Edit/Delete | PASS |
| Switch back to Homeowner | Same saved public identity | Management actions disappear | PASS |
| Repeat three roundtrips | Correct role-dependent navigation and data | No contamination in sequential single-session testing | PASS |

This is not a multi-session authentication certification: active role is shared backend demo state (V3-01).

## 9. Bid lifecycle matrix

| State/action | Result | Evidence |
| --- | --- | --- |
| Create a fresh eligible bid | PARTIAL / blocked fixture | Only available Jordan-matching open scope already has bid 27; current enabled submit instead produces B-03. Positive creation covered by existing backend suite, not a fresh browser success |
| View submitted | PASS | Bid 27, task 21, $1,200.00 USD, own message |
| Edit submitted | PASS | Same ID changed to $1,325.50 and edited message; reload retained both |
| Withdraw submitted | PASS | Same ID remains in history as Withdrawn; not deleted |
| Accepted | PASS | Bid 25, task 32, $3,000.00 USD; edit request rejected 409 |
| Rejected | PARTIAL | Rejected seeded bids traced; current Jordan has no own rejected fixture. Existing tests exercise decided-state locking |
| Other bidder mutation | PASS | PUT bid 15 rejected 403 |
| Duplicate create | FAIL B-03 | Existing bid visible alongside enabled submit; POST /api/tasks/21/bids returns 500 |

## 10. Professional profile and external portfolio matrix

| Area/action | Result | Persistence, privacy and boundary evidence |
| --- | --- | --- |
| Name/headline/about | PASS with B-04 side effect | Saved/reloaded and reflected on public profile; editable-name image lookup breaks seeded work cover |
| Person/business-first | PASS | Business-first public heading then person-first restored; same identity |
| Business details | PASS | Name, role, optional details and logo persist; private phone/address excluded from public API and page |
| Trades/specialties | PASS within fixture | Carpentry clearly qualified; no useless single-trade radio; Finish carpentry selection persists; qualifications unchanged by ordinary edits |
| Multiple qualified trades | PARTIAL | No multi-trade owner fixture; source/test coverage only |
| Availability / ZIP / radius | PASS with P-03 | Initial Not accepting work / 19802 / 20 loads; changed to Available soon / 19803 / 35, saved and reloaded |
| Missing service values | PASS, existing focused tests | Null optional availability/ZIP/radius render editable empty values, not generic dead-section state |
| Personal and business credential CRUD | PASS | Add Provided safety credential, edit reference, reload, delete, reload; no trade qualification granted |
| Credential private evidence | PASS | Owner reference remains private; public omits number, notes, evidence reference and review basis |
| Credential evidence upload | Deferred V3-02 | Disabled, explanation present; no false upload success |
| Portrait and logo uploads | PASS | Existing valid JPEG uploaded through supported flow; persisted media URL propagated to account header and discovery; business logo saved |
| Wrong-role/other-owner edit | PASS | Homeowner profile PUT and other tradesperson photo PUT denied 403 |
| External work add/view/edit/delete | PASS with B-04 | New uploaded-image item appears once, Self-reported, completion date retained, dialog/Escape/focus return, edit/delete persist |
| Existing Walnut title edit | FAIL B-04 | Cover disappears after persisted title change without any image edit |
| Owner card layout | PASS | Add work at section header; Edit/Delete attached to actual card; no separate duplicate management list |
| Homeowner/public controls | PASS | No Add/Edit/Delete; same portfolio card presentation |
| RH&P Projects | PASS protection | No owner editing controls; attempts to PUT/DELETE protected item rejected 403 |

## 11. Seven-profile trust / badge matrix

All seven were opened through homeowner discovery; initial service values, specialties, identity, portrait, credentials and provenance were inspected. Six trade mark SVG shapes are distinct; verified status has explicit text and is not conveyed only through shape/color. No “Uninsured” inference found.

| Person | Primary trade / specialty | Rendered trust and provenance | Evidence / why justified |
| --- | --- | --- | --- |
| Jordan Ellis | Carpentry / Built-in cabinetry | Verified Trade Carpentry; Credential Verified; Credential Provided; external Self-reported | Union journeyman credential explicitly VERIFIED and supports Carpentry; separate UBC Journeyman remains PROVIDED |
| Marcus Reed | Electrical / Residential panel upgrades | Verified Trade Electrical; Credential Verified; external Self-reported | VERIFIED trade license with explicit Electrical qualification assessment |
| Nina Alvarez | Plumbing / Bathroom rough-ins | Verified Trade Plumbing; Credential Verified; Pending Verification; RH&P Project | VERIFIED plumbing license supports Plumbing; separate manufacturer equipment training remains pending |
| Leah Bennett | Exterior Restoration / Masonry and weatherproofing | Neutral Trade; Insured; Credential Verified; RH&P Project; external Externally verified | Insurance VERIFIED, issued 2026-01-01, expires 2027-01-01, current on audit date; insurance does not establish trade qualification |
| Sofia Nguyen | Drywall / Plaster repair and finishing | Neutral Trade Drywall; Credential Provided; external Self-reported | Provided safety training has supportsQualification=false; no automatic Verified Trade |
| Darius Cole | Flooring / Hardwood restoration | Neutral Trade Flooring; Credential Verified; RH&P Project | Verified manufacturer installation training, but no explicit trade qualification assessment; correctly not Verified Trade |
| Owen Price | Carpentry / Finish carpentry | Neutral Trade Carpentry; external Self-reported; no credentials | No verified evidence; intentionally lighter profile |

These are fictional local fixtures, not independent verification of real licenses or insurers. No jurisdictional legal requirement is inferred. Platform work provenance is distinct from credential verification. Leah's literal “Demo” issuer wording is P-01.

## 12. Responsive and accessibility matrix

| Surface | 1440 | 943 | 390 | Reflow / interaction |
| --- | --- | --- | --- | --- |
| Homepage | PASS layout | PASS layout | PASS layout; P-02 typography | Carousel manual/reduced motion/touch checked |
| Inspiration / collaborators | All 8 inspected | Barns/Containers/Beyond PASS | Same PASS | Concept open/close and focus return; category nav doesn't overflow document |
| Work dashboard, bids, opportunities, messages, profile | Desktop journeys PASS | PASS | PASS | Also checked at 720 CSS px |
| Owner editor | PASS | PASS | PASS | 720px reflow PASS; Tab from ZIP to radius has visible outline |
| Portfolio dialogs | PASS | Representative layout checked | Representative layout checked | Escape and focus return PASS |
| Trade marks | Six distinct symbols with labels | Wrap within layout | Labels retained | No symbol-only verified assertion |

720 CSS px approximates the available layout width of a 1440px browser at 200% zoom; it is a reflow test, not a claim that native browser zoom or every assistive technology was certified. No document horizontal overflow found. Playfair editorial headings and Inter interface text remain intact. Existing 13px uppercase section eyebrows are a reasonable hierarchy exception; 8.8–10.4px homepage steps and 9.6px category descriptions are not (P-02). No typography was changed. Exact contrast ratios, complete touch-target inventory, full dialog focus-trap traversal, screen-reader operation and physical-device swipes were not comprehensively certified.

## 13. Media audit

All eight See It Built result images were visually inspected: kitchen, timber workshop, trailer galley, bus interior, woodland tiny home, container studio, pergola and greenhouse studio correspond to their named directions and trades. All three stage controls work; stage resources and captions were checked, but this is not a forensic assessment that every pictured construction sequence is physically identical. No broken image request was found in initial homepage/category/profile scans. Portrait/name/trade mappings match the source people list. Repeated portraits across categories intentionally identify the same worker; no accidental cross-person substitution found.

Verified project galleries and external portfolio dialogs open with their corresponding work. New uploaded media persists and takes precedence over presentation media. Existing seed-only portfolio covers depend on editable names/titles and disappear after ordinary edits (B-04). Public project access is separately blocked by B-01. Full-page screenshots can contain offscreen lazy-image placeholders; these alone were not counted as broken images.

## 14. Product-language audit

Rendered homepage, inspiration pages, workspaces and public profiles were searched for demo, seed, fallback, generated, concept image, illustrative, API, backend, unit unspecified, Participant and Unknown sender. Developer documents/tests were excluded.

P-01: Marcus, Sofia and Owen external work descriptions contain “fictional demo history with illustrative imagery”; Marcus's full portfolio dialog reproduces it. Leah's credential issuer reads “Demo commercial insurance carrier.” These are visible customer copy, not internal source comments. No generic numeric participant or Unknown sender leakage found in exercised conversations. No “unit unspecified” phrase found, but radius itself lacks a unit (P-03). One closing inspiration conversion CTA per category; no duplicate portfolio-management list. Opportunity text “Submit task-trade bid” and membership explanations remain somewhat technical; recorded here as context, not separate counted defects.

## 15. Findings

### B-01 — Public project story links into a forbidden workspace

- **Severity:** BLOCKER. **Area:** Public portfolio journey.
- **Steps:** Homeowner Jordan → Find tradespeople → Nina Alvarez → Cedar Park bathroom portfolio → View RH&P project.
- **Expected:** Continue through publicly published work, or offer a workspace action only when authorized.
- **Actual:** `/projects/2` renders “This view couldn’t be loaded” with access denied. Authorization correctly protects the private project, but the public journey ends in a dead section.
- **Recommended correction:** Make the public continuation resolve to permitted published story content, or conditionally offer private workspace navigation. Do not weaken private project authorization.
- **Source:** `frontend/src/components/PortfolioCard.tsx` public dialog project link and project access enforcement.

### B-02 — Saved task reviews disappear after refresh

- **Severity:** BLOCKER. **Area:** Completion and review history.
- **Steps:** Open Passyunk completed cabinet task (existing review absent). Separately approve plumbing task 3, publish a review, then refresh.
- **Expected:** Persisted review stays visible as published feedback.
- **Actual:** POST succeeds with 201 and database retains review, but reload returns a blank publish form. Existing review is also not read back.
- **Recommended correction:** Load and render persisted review state and prevent misleading duplicate publish presentation.
- **Source:** `frontend/src/features/reviews.tsx` renders mutation result without existing-review readback.

### B-03 — Existing proposal still offers duplicate submission and returns 500

- **Severity:** BLOCKER. **Area:** Find Work / proposal creation.
- **Steps:** Tradesperson Jordan → Find Work → ZIP 19130 / Carpentry → Finish window returns → submit while existing bid 27 is displayed.
- **Expected:** Existing proposal management, or a clear domain-level duplicate restriction.
- **Actual:** Enabled submit sends POST `/api/tasks/21/bids`; HTTP 500. Database uniqueness prevents a second bid for the same bidder/task-trade, but UI eligibility ignores existing proposal state.
- **Recommended correction:** Align submit availability with existing bid state and map duplicate attempts to a specific product error. Preserve bid history.
- **Source:** `frontend/src/features/work.tsx`, task bid submission flow and bid uniqueness constraint.

### B-04 — Editing identity or portfolio title removes existing work image

- **Severity:** BLOCKER. **Area:** Profile / external portfolio media.
- **Steps:** As Jordan, change display name and save; inspect Walnut reading nook. Restore name, edit that item's title only, save and refresh.
- **Expected:** An unchanged work image survives unrelated text edits.
- **Actual:** Both paths replace its displayed image with “No work photos yet.” Newly uploaded items are unaffected.
- **Recommended correction:** Give existing work media a stable persisted association independent of editable display name and title.
- **Source:** `frontend/src/components/seedMedia.ts` exact person/title portfolio registry matching when mediaReference is absent.

### P-01 — Demo implementation wording is visible in public profiles

- **Severity:** POLISH. **Area:** Public work descriptions / credentials.
- **Steps:** Open Marcus, Sofia or Owen's external work description; open Leah's credential section.
- **Expected:** Natural product copy while retaining truthful provenance.
- **Actual:** “fictional demo history with illustrative imagery” and “Demo commercial insurance carrier.”
- **Recommended correction:** Remove implementation-oriented wording from customer presentation while retaining explicit fictional-fixture disclosure in appropriate demo documentation and preserving provenance truthfulness.

### P-02 — Homepage mobile supporting copy is too small

- **Severity:** POLISH. **Area:** Homepage readability.
- **Steps:** Open `/` at 390px and inspect process steps/category descriptions.
- **Expected:** Comfortable supporting copy and readable step labels.
- **Actual:** Step eyebrows 8.8px; step explanations 10.4px; category descriptions 9.6px; other supporting lines 12–13.6px.
- **Recommended correction:** Adjust these scoped mobile rules without scaling the entire application or changing editorial hierarchy.

### P-03 — Service radius has no visible unit

- **Severity:** POLISH. **Area:** Professional profile/service area.
- **Steps:** Open any reviewed profile or owner radius input.
- **Expected:** A radius value with a clearly defined distance unit.
- **Actual:** “Service radius 20” / “Service radius 25”; editable numeric input likewise has no unit.
- **Recommended correction:** Confirm the existing distance contract and communicate its unit in the label and public summary; do not guess miles or kilometres.

### V3-01 — Demo account and active role are shared server state

- **Severity:** V3. **Area:** Authentication / multi-session use.
- **Steps:** Inspect DemoActiveAccountContext; use the account endpoint and role switch.
- **Expected:** For future multi-user use, independently authenticated users and session-scoped role selection.
- **Actual:** Fixed underlying Jordan user and mutable singleton role; sequential one-presenter demo works, genuine anonymous and other-account journeys cannot be exercised through sign-in.
- **Recommended correction:** Add real per-session authentication before multi-user deployment. Do not present this audit as production authorization certification.

### V3-02 — Private credential document uploads are deferred

- **Severity:** V3. **Area:** Credential evidence.
- **Steps:** Add/edit credential and inspect Upload evidence.
- **Expected:** Future private evidence storage and controlled retrieval.
- **Actual:** Upload disabled with explanation; owner reference metadata works and stays private.
- **Recommended correction:** Implement secure private document storage when this capability enters scope; do not reuse public profile-media delivery for private credentials.

### V3-03 — No owner-driven qualification verification workflow

- **Severity:** V3. **Area:** Credential review / additional qualifications.
- **Steps:** Open Trades & specialties or add a credential.
- **Expected:** Future authorized review process for qualification changes.
- **Actual:** Owner edits cannot grant qualifications; adding a credential produces Provided rather than Verified. No complete reviewer UI is available here.
- **Recommended correction:** Add an authorized evidence-review workflow in its own scope; preserve current owner restrictions.

### V3-04 — Project lifecycle closure is separate and not actionable here

- **Severity:** V3. **Area:** Project lifecycle.
- **Steps:** Open Completion; inspect task approval and project status flows.
- **Expected:** Future explicit project start/close workflow if included in product scope.
- **Actual:** Task completion works; project lifecycle is displayed separately. No complete owner project-close action was found; completed project lifecycle states are seeded.
- **Recommended correction:** Define and implement lifecycle transitions separately if an end-to-end project-closing demo is required. Do not claim task approval itself closes the project.

## 16. Explicit deferred limitations and audit boundaries

- Owner-performed work, self-generated verified provenance and reputation badges remain outside current scope; no self-dealing bypass was introduced.
- V3-01 through V3-04 above are deliberate future capability boundaries, not successful end-to-end demonstrations.
- Completed Homeowner Jordan project, fresh eligible Jordan bid and multiple-qualified-trade owner states are absent from current fixtures; these are marked PARTIAL rather than manufactured through seed edits.
- Bid award, rejected-own-bid locking, missing optional service values and several trust edge cases rely on the existing backend/frontend suites in addition to source inspection. Not every such state was recreated in the browser.
- Guest 401 and collaborator API 500 were browser response interceptions to verify presentation, not real identity switching or backend outage injection.
- Single Chromium environment, representative widths and 720px reflow were covered; this is not full browser/device/assistive-technology certification.
- Evidence logs, JSON observations and screenshots are temporary local files `/tmp/rhp-final-*`; reusable audit scripts are `/tmp/rhp-025-browser/rhp-final-*.mjs`. They were not added as product tests.

## 17. Final verdict and repository state

**NOT READY FOR DEMO.** Four reproducible functional blockers remain despite all existing automated suites passing. They affect public story navigation, persistent review presentation, proposal submission and preservation of portfolio media after editing. Three polish findings and four V3 limitations are separately classified above.

**NO PRODUCT FIXES. NO COMMIT. NO PUSH. NO PR. NO MERGE. NO BRANCH SWITCH.** No reset or stash. Original database connection restored and source hashes unchanged. Only this audit document was created.

Exact final `git status --short`:

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
?? docs/RHP-FINAL-DEMO-AUDIT.md
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
