# RH&P-031 final AI Project Builder audit

## 1. Audit date and scope

September 14, 2026. Original audit for the September 24 demo. Original verdict: **NOT READY FOR DEMO**.

**Current verdict after the authorized B01–B04 fix pass: READY FOR DEMO. Zero remaining blockers; see section 15.** Sections 1–14 retain the original audit evidence and procedural disclosure.

Original findings: **4 BLOCKER, 4 POLISH, 2 V3**. No product, schema, test, or seed-source fixes were made during the original audit. This document was the only new repository change made by the original audit; the later authorized blocker fixes are recorded in section 15. Build/test outputs and temporary browser probes were generated outside tracked source. Existing uncommitted Phase 3 work was preserved.

Evidence combines source inspection, the required automated suites, additional deterministic browser reproductions, and real creation against the isolated `/tmp/rhp-phase3-browser.sqlite` database on port 8088. Existing repository `rhp.sqlite` and its manual Hockessin project were not used for audit creation. Browser projects received unique AI-assisted/manual audit titles. No special AI project entity was introduced.

**Procedural disclosure:** routine planning/browser tests used mocks, and the isolated backend had its Gemini key explicitly disabled. However, an audit availability probe mistakenly sent one POST to `/diagnostics/gemini/schema/A` on an already-running, separate port-8091 listener. This may have invoked Gemini and consumed quota; the response was not retained, so quota use cannot be determined. No further diagnostic POST was made. This was an audit execution error, not evidence that the normal application enables diagnostics. See section 11.

## 2. Branch and baseline state

Branch: `rhp-031-ai-project-builder`.

Backend baseline: Phase 1 planning adapter and validation; Phase 3 approved-draft endpoint, transaction orchestration, catalog validation, and durable submission receipts. Frontend baseline: two entry paths, iterative questions, shared review, catalog correction, explicit Keep/Apply controls, and creation into the ordinary workspace.

Initial `git status --short`:

```text
 M frontend/README.md
 M frontend/src/api/projectBuilder.ts
 M frontend/src/features/homeowner/project/HomeProjectWorkspace.tsx
 M frontend/src/features/projectForm.tsx
 M frontend/tests/browser/projectBuilder.spec.ts
 M src/main/java/com/rockandhardplaces/api/ApiExceptionHandler.java
 M src/main/java/com/rockandhardplaces/api/ProjectBuilderController.java
 M src/main/resources/schema.sql
?? frontend/tests/browser/projectCreation.integration.spec.ts
?? src/main/java/com/rockandhardplaces/planning/ApprovedProjectCreationService.java
?? src/main/java/com/rockandhardplaces/planning/ApprovedProjectDraft.java
?? src/test/java/com/rockandhardplaces/api/ApprovedProjectCreationTests.java
```

## 3. Baseline validation

| Check | Result | Evidence/qualification |
| --- | --- | --- |
| `./mvnw -q test` | PASS | 296 tests, zero failures/errors/skips. |
| `npm run typecheck` in frontend | PASS | Exit 0. |
| `npm run build` in frontend | PASS | Production bundle generated, exit 0. |
| `npm test` in frontend | PASS | 97 Node tests; 14 mocked browser tests pass. Three opt-in real-backend browser tests skipped by default. |
| `git diff --check` | PASS | No whitespace errors. |
| Additional real-backend suite | QUALIFIED | 1440/943px passed. Initial 390px run failed during route teardown after assertions; isolated 390px rerun passed. See P04. |
| Additional audit probes | FINDINGS | Reproduced all four blockers independently of the baseline tests. |

Logs: `/tmp/rhp-final-audit-backend.log`, `/tmp/rhp-final-audit-build.log`, `/tmp/rhp-final-audit-frontend.log`, `/tmp/rhp-final-audit-real-browser.log`, `/tmp/rhp-final-audit-real-recheck.log`. Backend counts were read from Surefire XML reports. The passing baseline does not cover the newly reproduced failing state transitions.

## 4. Entry and guided-flow matrix

| Area | Result | Observation |
| --- | --- | --- |
| Both `/projects/new` paths | PASS | Visible side-by-side desktop cards, stacked on mobile; manual path has an explicit, usable action. RH&P typography/colors remain coherent. |
| Back navigation | PARTIAL | Back buttons exist, but starting a guided flow retains unrelated manual draft fields (B01). |
| Idea submission/loading | PASS | Single planning request; no Project/Task writes during questioning. |
| Initial idea stays visible | FAIL / P01 | Original idea is retained in memory/request context but disappears from the displayed questions/review. |
| Follow-up association | PASS within one flow | Question strings and answers remain paired; repeated identical questions retain the answer. |
| New summary versus final project description | FAIL / B01 | Updated RH&P understanding and the saved description can contradict each other. |
| Trades/tasks grouping | PASS | Separate sections and labeled catalog selectors. |
| Reason/confidence/confirmation truthfulness after edit | FAIL / B03 | Changed work retains the original recommendation's evidence and confirmation state. |
| Assumptions/warnings | PASS | Visible and not automatically persisted. |
| Normal product terminology | PASS with polish | Confidence enum values are translated. Targeted source scan found no Gemini/API/provider/quota/schema/JSON/backend/diagnostic labels in the normal builder UI. Duplicate fallback actions are P02. |

## 5. Manual-review matrix

| Area | Result | Observation |
| --- | --- | --- |
| Original manual fields | PASS | Name, description, ZIP; edits preserved during ordinary form navigation. |
| Optional RH&P review | PASS | Manual creation does not require a planning call. |
| Apply/Edit/Ignore | PASS for ordinary flow | Applied task payload contains final task text/catalog ID; ignored tasks are excluded. |
| Advisory recommendations | PASS | No auto-apply; zero approved tasks is supported. |
| Review outage before recommendations | PASS | Calm error, retry/manual actions, zero-task project creation works. |
| Outage after previously approved recommendations | FAIL / B02 | Returning through the manual form clears previously applied tasks and answers. Guided fallback also overwrites edited project details. |

## 6. Provider-failure matrix

| Condition | Backend behavior | Homeowner behavior | Verdict |
| --- | --- | --- | --- |
| Provider 429 | Existing adapter logs status only and returns safe `PLANNING_UNAVAILABLE`/503; direct frontend 429 is also mocked | Calm error, Retry/Continue manually | PASS on initial draft; B02 on fallback after review |
| Provider 503 | Safe planning exception/envelope | Same fallback; no provider detail exposed | PASS with same B02 qualification |
| Timeout | Provider call bounded; frontend planning aborts at 45 seconds | Draft retained at error; fallback exists | PASS at error, FAIL if fallback discards reviewed state |
| Invalid provider response | Strict Java response validation; 502 | Safe error, retry/manual route | PASS with same fallback qualification |
| Empty/null/blank provider response | Validator rejects null/blank/non-object/invalid response as `INVALID_RESPONSE` | Same 502 handling | PASS by adapter/validator tests and shared browser 502 handling |

An empty *provider* response is not treated as a valid HTTP-200 project plan. Arbitrary corruption of the application's own HTTP-200 envelope was not used as a proxy for provider failure. Errors do not independently create business records. Matching authoritative retries are tested separately below.

## 7. Approved-plan integrity

`POST /api/project-builder/create` accepts title, description, ZIP, and approved tasks containing title, description, and existing catalog IDs. Identity comes from `ActiveAccountContext`; unknown ownership/status/provider fields are rejected. `ProjectWorkflowService` creates ordinary PLANNING Project/Task records and TaskTrade requirements. No confidence, reason, needs-confirmation or provider-specific entity fields are written.

| Check | Result |
| --- | --- |
| Removed tasks excluded; ignored manual recommendations excluded | PASS |
| Edited task title/scope submitted | PASS |
| Unknown catalog ID rejected without new Trade | PASS; audit request returned 400 |
| Unresolved suggestions block UI until correction/removal | PASS |
| Kept standalone trades require a kept task | PASS |
| Removed trade and kept-task conflict | FAIL / B04 for case variants |
| Current planning understanding supplies coherent approved fields | FAIL / B01 |
| No model metadata becomes domain state | PASS |

The backend correctly persists its approved-draft input. B01/B04 concern incorrect or inconsistent frontend review state reaching that boundary; they are not claims that Gemini bypasses the endpoint.

## 8. Transaction/retry matrix

| Check | Result | Evidence |
| --- | --- | --- |
| Failure after first task | PASS | Injected second-task persistence failure leaves Project/Task/TaskTrade/receipt counts unchanged; retry succeeds. |
| Matching replay | PASS | Same owner/key/payload returns original project. |
| Simultaneous matching requests | PASS | Audit sent two concurrent requests: both 201, both project ID 28; one task and one requirement persisted. |
| Changed payload with successful key | PASS | Conflict returned rather than a second project. |
| UI disables Create while pending | PASS | Browser assertion verifies disabled button. |
| Creation failure retains review and retry key | PASS | Mocked failure/retry submits identical approved payload/key. |
| Receipt transaction | PASS by inspection/tests | Receipt and domain writes share the transaction. SQLite workflow serialization covers commit; unique owner/key is the database guard. |

Cross-process contention was not load-tested. A unique-constraint/lock failure may require another retry, but the transaction and receipt prevent a second committed project for the same owner/key. No heavyweight distributed guarantee is claimed.

## 9. Persistence, discovery and self-dealing matrix

| Area | Result | Evidence |
| --- | --- | --- |
| Guided Hockessin project | PASS | Separate unique title, description, approved task and catalog requirement persisted in isolated database. |
| Manual/no AI/zero tasks | PASS | Real-backend browser creation; empty task list verified. |
| My Projects and workspace | PASS | Listing plus Overview/Tasks/Team/Bids/Messages/Completion inspected through real backend. |
| Post-creation refresh/direct navigation | PASS | Audit project reopened and refreshed; authoritative state unchanged. |
| Eligible tradesperson discovery/bid | PASS | `ApprovedProjectCreationTests` creates requirement, discovers it and submits normal bid. |
| Wrong trade / no open requirement | PASS | `FrontendSupportIntegrationTests` plus discoverable/qualifiedTrades/openTask inspection. |
| Duplicate proposal restrictions | PASS | Existing bid uniqueness/workflow tests and database constraint remain unchanged. |
| Jordan Homeowner → Tradesperson | PASS | Browser role switch; created project absent from own opportunities; direct self-bid returns 403. |
| Wrong-role project creation | PASS | Audit request returned 403. |
| Switch back to Homeowner | PASS | Project title/tasks remain correct. |
| Self-assignment/review/self-verification | PASS by service inspection and existing tests | Assignment checks different underlying Users; review rejects self; credential writes cannot assert VERIFIED status. No new bypass in builder. |

The demo uses one server-global role context; this remains a controlled-demo limitation (V02), not production authentication.

## 10. Responsive/accessibility/navigation

1440, 943 and 390px entry/questions/review/fallback and resulting workspace were exercised. No horizontal overflow was observed. At 390px the existing workspace uses its Project section select instead of desktop links. Screenshot review confirmed readable cards and ordinary task/trade presentation.

A 720-CSS-pixel viewport tested the reflow equivalent of a 1440px viewport at 200% zoom; no horizontal overflow occurred in entry/review. This is reflow evidence, not a claim of testing every browser's native zoom implementation. Keyboard Enter activated guided entry, planning, review, and Keep at 390px; focused buttons had a visible 3px solid outline. Controls use native buttons/selects and labeled fields. No full screen-reader audit was performed.

Pre-creation reload returns to the entry screen and loses the client draft as designed. The repository documents in-memory drafts, but the current screen does not warn the homeowner (P03). Ordinary post-save refresh and navigation preserve authoritative data.

## 11. Diagnostic/security audit

| Condition | Result | Evidence |
| --- | --- | --- |
| Local profile only | PASS | `@Profile("local-gemini-diagnostics")`. |
| Explicit enable flag | PASS | `@ConditionalOnProperty(... havingValue="true")`, no match-if-missing. |
| Loopback only | PASS | Listener binds literal `127.0.0.1`; remote loopback and exact Host checked; Origin rejected. |
| Inaccessible on normal MVC runtime | PASS | Normal audit process PID 53683 on 8088 returned 404 for diagnostic path. No active diagnostic profile. Existing test also asserts MVC 404. |
| Separate pre-existing listener | EXPECTED OPT-IN | Port 8091 was PID 45198, not the audit backend. A redacted argument inspection confirmed both required profile and explicit flag; listener bound loopback. It was not stopped or reconfigured. |
| Secrets/output | PASS within inspected scope | Adapter logs allowlisted metadata/status, not exception bodies; diagnostics redact configured key, key patterns, auth headers, and sample idea. Existing redaction tests pass. |
| No business persistence | PASS | Diagnostic code only calls provider/read-only catalog query; diagnostic tests assert unchanged business-table counts. |
| Safe to retain in repository | PASS | Required gates and read-only business behavior hold. Live invocation remains an explicit quota-consuming operation. |

A redacted scan covered 382 repository/document/test/log files, the current diff, audit/Phase 3 logs and Surefire outputs: no Google API-key-pattern match found. No `GEMINI_API_KEY` value was available to perform an exact-value scan. Configuration uses environment substitution; `.env`/`.env.*`, build outputs and SQLite files are ignored. Documented variable names and dummy test keys are not live credentials. The pre-existing diagnostic process writes to a terminal; its prior terminal history and arbitrary external logs were not audited. No secret was printed.

The accidental single diagnostic POST described in section 1 prevents claiming zero possible live quota consumption for this audit. That procedural error does not change the verified profile/flag/loopback gates.

## 12. Findings

### B01 — Stale or unrelated draft content reaches the creation payload

**Re-audit status: RESOLVED in the authorized blocker-fix pass; historical reproduction below.**

- **Severity:** BLOCKER
- **Area:** Guided state, path switching, approved-plan integrity.
- **Steps:** Submit a tree-supported idea; answer “Freestanding only”; return a revised freestanding summary; open review and create. Separately, enter a manual garage draft, return to start options, choose guided tree-house planning, keep a task and create.
- **Expected:** Current project intent and review fields stay coherent. Unedited generated description follows the latest understanding; unrelated prior-path fields cannot silently attach to a new plan.
- **Actual:** Latest understanding says “Freestanding tree house with revised scope.” but the payload description remains “Tree-supported tree house.” The cross-path reproduction submits title “Old garage draft” and description “Garage only. No tree house.” with a tree-house deck task. The stale fields are visible if the homeowner notices the contradiction; there is no reconciliation warning.
- **Evidence:** `frontend/src/features/projectForm.tsx:112` only fills description when empty; guided entry at line 231 clears recommendations but retains the project draft. Payloads captured in `/tmp/rhp-final-audit-probes.json`.
- **Recommendation:** Define explicit ownership/dirty state for generated versus homeowner-edited summary and project identity; reconcile updates and path changes without overwriting deliberate edits or retaining unrelated fields.

### B02 — Manual fallback discards reviewed tasks and answers

**Re-audit status: RESOLVED in the authorized blocker-fix pass; historical reproduction below.**

- **Severity:** BLOCKER
- **Area:** Provider failure, manual fallback, draft preservation.
- **Steps:** Keep/Apply a task, edit the project description, answer a question, then make an updated planning request fail with 503. Choose Continue manually and Continue to project review.
- **Expected:** Fallback preserves approved tasks, edits and answers while removing only the dependency on another successful planning call.
- **Actual:** Guided fallback resets the edited description to the original idea and removes tasks/answers. Manual fallback retains the description, but submitting the manual form clears previously applied tasks and answers. Both reproductions end with zero review tasks and zero answers, without a discard confirmation.
- **Evidence:** `frontend/src/features/projectForm.tsx:86` and `:299`; temporary probes `guided-fallback-loss` and `manual-fallback-loss`.
- **Recommendation:** Preserve one shared draft through fallback; only an explicit discard/new-plan action should remove reviewed work.

### B03 — Edited work retains misleading RH&P recommendation evidence

**Re-audit status: RESOLVED in the authorized blocker-fix pass; historical reproduction below.**

- **Severity:** BLOCKER
- **Area:** Reason/confidence/needs-confirmation truthfulness.
- **Steps:** Return a HIGH-confidence Carpentry task with framing rationale and no confirmation flag. Change its required trade to Electrical using the catalog selector.
- **Expected:** Original model evidence is clearly scoped to the original suggestion or invalidated/marked homeowner-edited. It must not imply RH&P assessed the changed trade.
- **Actual:** Card now says “Trade: Electrical” and still says “Carpentry fits the timber framing” / “Confidence: Strong fit”, with no Needs confirmation badge. The original metadata survives text/scope changes too.
- **Evidence:** `frontend/src/features/projectForm.tsx:493` and `:518`; `edited-evidence` probe.
- **Recommendation:** Track edited provenance and stop presenting stale recommendation metadata as support for changed work. A new assessment must be explicit, not an automatic provider call.

### B04 — Case variants bypass the removed-trade conflict check

**Re-audit status: RESOLVED in the authorized blocker-fix pass; historical reproduction below.**

- **Severity:** BLOCKER
- **Area:** Approved task/trade relationship integrity.
- **Steps:** Return suggested trade `CARPENTRY` and task trade `Carpentry`, both resolving to catalog ID 1. Remove the trade suggestion; Keep the task; Create Project.
- **Expected:** The existing removed-trade/kept-task conflict is detected by catalog identity regardless of capitalization; homeowner must resolve it before creation.
- **Actual:** Create remains enabled and submits `requiredTradeIds: [1]`. Exact-case variants would be blocked, but removed-trade comparison uses raw strings while catalog mapping is case-insensitive.
- **Evidence:** `frontend/src/features/projectForm.tsx:152` versus `:168`; `removed-trade-case-mismatch` probe captured the submitted payload.
- **Recommendation:** Use canonical catalog IDs consistently when evaluating approved/removed trade relationships.

### P01 — Original idea disappears after intake

- **Severity:** POLISH
- **Area:** Guided context.
- **Steps:** Submit an identifiable original idea and proceed to questions/review.
- **Expected:** Homeowner can still see the original idea alongside the developing understanding.
- **Actual:** It remains in memory and request context but is absent from rendered questions/review. Going back to restart is the available route to the input.
- **Recommendation:** Display the original idea in a compact, accessible review section.

### P02 — Duplicate Continue manually actions on guided error

- **Severity:** POLISH
- **Area:** Entry/fallback CTAs.
- **Steps:** Submit a guided idea; return 429/503.
- **Expected:** One clear fallback action in the error state.
- **Actual:** Error panel and footer both offer Continue manually. Both lead to the same handler; they are duplicates rather than different recovery options.
- **Recommendation:** Present one unambiguous fallback action for that state.

### P03 — Draft-reset behavior is documented only outside the user flow

- **Severity:** POLISH
- **Area:** Refresh/navigation language.
- **Steps:** Reach review, then reload before creating.
- **Expected:** User understands that the unsubmitted draft is temporary.
- **Actual:** Reload resets the draft to entry. README describes in-memory drafts, but no refresh/reset/unsaved notice appears in current review UI.
- **Recommendation:** Add concise temporary-draft wording; persistence itself can remain deferred.

### P04 — Real-backend browser suite has an in-flight route teardown race

- **Severity:** POLISH
- **Area:** Audit/test reliability.
- **Steps:** Run the opt-in real-backend browser suite; final 390px test reaches post-creation checks while dashboard request remains in flight.
- **Expected:** Completed product assertions produce a stable result and routes settle before test teardown.
- **Actual:** Initial audit run failed with `route.fetch: Test ended` for `/api/dashboard/homeowner`. Isolated 390px rerun passed. No corresponding product assertion failed.
- **Evidence:** `frontend/tests/browser/projectCreation.integration.spec.ts:52`; initial and rerun logs listed in section 3.
- **Recommendation:** Await/clean up outstanding routes before closing the test. No test fix was applied here.

### V01 — Resume drafts across refresh/devices

- **Severity:** V3
- **Area:** Draft persistence.
- **Steps:** Leave or reload an unsubmitted plan, or open it on another device.
- **Expected:** Future saved-draft feature can resume the planning conversation and approval state.
- **Actual:** State is client-memory-only, an explicitly accepted Phase 2 limitation. P03 concerns warning text, not a requirement to build persistence now.
- **Recommendation:** Defer a server-backed, non-authoritative draft design until after the demo.

### V02 — Replace global demo identity with per-session authentication

- **Severity:** V3
- **Area:** Multi-user deployment.
- **Steps:** Use two clients against the demo server and switch a profile in one.
- **Expected:** Production clients have independently authenticated identities and role contexts.
- **Actual:** `DemoActiveAccountContext` uses a singleton active role and demo user. This is documented existing demo architecture, not a newly introduced builder authorization bypass. Same-user restrictions still operate.
- **Recommendation:** Keep the demo controlled to one operator; implement session authentication before multi-user deployment. Do not redesign it during this audit.

## 13. Explicit deferred limitations

- No image/multimodal input, pricing, scheduling, contractor selection, autonomous actions, or web search.
- No authoritative persistence before Create Project; no server draft resume.
- Local single-process SQLite orchestration, with durable owner/key receipts; no distributed load-test claim.
- No production per-session identity or independent simultaneous demo role contexts.
- No full screen-reader or cross-browser audit; Chromium plus CSS reflow/keyboard checks only.
- Deterministic planning validates product state/contract behavior, not the quality or availability of live Gemini output on demo day.
- Historical external process logs/terminal output and unknown-format credentials cannot be comprehensively certified by the performed scan.

## 14. Original audit verdict (superseded by section 15)

**NOT READY FOR DEMO.** Backend authorization, catalog restrictions, atomic persistence, matching retry protection, and normal workspace integration are sound in the exercised scenarios. Four reproducible frontend integrity/truthfulness failures remain: B01–B04. Passing baseline suites do not cover these sequences. Resolve and re-audit those blockers before the September 24 demo.

Audit-only delivery: no product fixes, no commit, no push, no merge, no branch switch/reset/stash.

## 15. Authorized B01–B04 fix pass and re-audit — September 14, 2026

**READY FOR DEMO. Remaining blockers: 0.** Original POLISH P01–P04 and V3 V01–V02 remain deferred and were not implemented. This verdict applies to the controlled demo and the existing limitations recorded above.

### Scope and root-cause corrections

Only `frontend/src/features/projectForm.tsx`, the new `frontend/src/features/projectBuilderDraft.ts`, the new `frontend/tests/browser/projectBuilderBlockers.spec.ts`, and this audit document were changed by this fix pass. Other Git changes predate this pass. No backend/domain/schema/seed changes, diagnostic requests, live Gemini calls, commits, pushes, branch changes, or UI redesign were performed.

| ID | Root cause | Correction | Re-audit |
| --- | --- | --- | --- |
| B01 | Path entry independently cleared some state while retaining project fields; generated description was initialized only once. | One draft-reset boundary clears incompatible fields, idea, answers, recommendations, decisions, review/error state and submission identity. Choosing the other entry path or changing a reviewed guided idea starts fresh. Returning to the same path without changing intent retains its current work. A description ownership flag lets new planning summaries update untouched generated descriptions without overwriting homeowner edits. | PASS: both cross-path directions at 1440/943/390px, same-path navigation, edited idea reset, iterative summary updates, explicit homeowner summary preservation, and exact approved creation payload. |
| B02 | Fallback cleared guided state; submitting the manual form cleared accepted tasks and answers; successful planning replaced every recommendation. | Continue manually changes editing mode within the same draft, retains project fields/answers/reviewed work and the retry key, and invalidates late results. Manual form submission preserves review state. Planning refresh retains kept/removed items, authored additions and edited scope; original suggestion identities suppress reissued copies after edits. Only unreviewed suggestions are refreshed. | PASS: guided and manual 429/503/45-second timeout at each width, successful retry, no duplicate retained tasks, subsequent failure/manual continuation, and creation with exactly the retained accepted scope. |
| B03 | Title/scope/trade updates retained model evidence; new manual tasks received invented confidence. | Draft-only provenance distinguishes suggested, edited and added work. Any actual title or description change—even minor punctuation—clears original reason/confidence/confirmation metadata. A different normalized trade also clears it. Catalog-equivalent spelling alone does not change the assessment. Edited/added cards say Edited by you / Added by you; manual additions have no model confidence. Explicit payload mapping excludes all provenance/model fields. | PASS: title-only policy, independent material description and trade changes, added task display, and provider/provenance-free creation payload. |
| B04 | Removed-trade comparisons used raw strings while matching used normalized names. | Exact catalog matching trims, collapses whitespace and normalizes case; only one unambiguous catalog match is accepted. Relationship conflicts use catalog IDs. Unknown names remain unresolved; no fuzzy matching or catalog writes. | PASS: removed `  CARPENTRY  ` versus task `carpentry`, ` Exterior   Work ` matching, unknown `Tree Wizard` blocking, and explicit catalog correction before creation at all widths. |

### Validation and browser evidence

- `./mvnw -q test`: **296 passed**, zero failures/errors/skips. Existing rollback, approved payload, duplicate submission and same-user self-dealing tests remain passing.
- `npm run typecheck`: **PASS**.
- `npm run build`: **PASS**.
- `npm test`: **97 Node tests and 47 browser tests passed**. The three pre-existing opt-in real-backend scenarios remain skipped in the default run; they were not changed to fix POLISH P04.
- **33 new blocker browser regressions** cover the state boundaries and rationale/conflict fixes; 30 exercise the 1440/943/390px matrix and three isolate same-path intent changes and independent edits.
- `git diff --check`: **PASS**.
- Screenshots `frontend/test-results/blockers-edited-1440.png`, `blockers-edited-943.png`, and `blockers-edited-390.png` support visual review of homeowner-edited cards. Overflow assertions pass across the fallback/review matrix.
- All planning and create requests in the new browser regressions are intercepted deterministic fixtures. No live provider or diagnostic listener was contacted. Backend tests use their existing isolated/mock fixtures. The existing manual Hockessin project was not modified.
- One new regression initially used an exact label-text locator that could not find a populated textarea; it was corrected to the native textbox accessible-name locator. The subsequent full suite passed. No product workaround or unrelated POLISH test-teardown fix was introduced.

Re-audited original areas C/D/E/F/G/H/I/N now satisfy the four blocker requirements: current draft integrity, graceful fallback, truthful edited-item presentation, canonical trade relationships, and ordinary manual/guided creation controls. Backend authorization/persistence invariants continue to pass. The earlier live-diagnostic-probe disclosure belongs to the original audit only; no such request occurred in this fix pass.

Logs: `/tmp/rhp-blocker-fix-backend.log`, `/tmp/rhp-blocker-fix-final-build.log`, `/tmp/rhp-blocker-fix-final-frontend.log`.
