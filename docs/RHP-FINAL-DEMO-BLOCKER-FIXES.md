# Final demo blocker fix pass

2026-09-09 · `rhp-030-professional-profile` · base `3f05e663ec75522e659a4b1d320d132673e44ae4`

**READY FOR DEMO within the existing single-presenter demo scope. Remaining BLOCKER count: 0.** The original audit's three POLISH findings and four V3 limitations remain deferred. This is not a production authentication or full multi-user deployment certification.

## Four-blocker re-audit

| Finding | Before | Root cause | Fix | After |
| --- | --- | --- | --- | --- |
| B-01 | Nina's public project story led to a forbidden private project | Portfolio dialog unconditionally linked to `/projects/:id` without a public destination or access context | Removed that misleading continuation; existing public card still opens its published story and stage gallery | Public gallery and stage navigation work; no private-workspace link in dialog; Jordan's own project and member project remain accessible, unauthorized project 2 stays 403 |
| B-02 | Saved feedback vanished after reload; blank publish form returned | Creation persisted correctly, but no task-review read endpoint/query existed and rendering used only mutation state | Added owner-authorized task readback, relationship/name/timestamp DTO fields, server-loaded rendering, and excluded already-reviewed assignees from creation | Existing seeded review loads; newly saved review survives repeated reloads with correct author, target, task/project, rating, text and timestamp; one record remains; self/unauthorized review rejected |
| B-03 | Existing bid coexisted with submit form; duplicate returned 500 | No duplicate precheck in submission service; UI ignored ownBids | Explicit domain conflict plus database-failure conflict handling; retain unique constraint; existing scopes render reusable proposal card with valid state/actions | Duplicate submitted and withdrawn attempts return 409; no extra record; first creation succeeds in integration coverage; edit/withdraw persist and history remains; accepted/rejected states cannot resubmit |
| B-04 | Name or portfolio title edit removed the visible legacy cover | Two covers existed only in a presentation registry keyed by editable name/title; update inputs also failed to distinguish omitted media from explicit removal | One-time stable-reference migration for those two covers; removed external text-based media lookup; presence-aware media inputs preserve omission and honor null removal; portfolio metadata merges omissions before validation; identity edits leave portrait unchanged | Name/title changes preserve covers; title-only request preserves description/date/media; uploaded portrait, logo and cover survive saves; explicit remove/replace persists; migration does not resurrect removed cover |

The B-04 repair changes only the two relevant legacy media references and adds its migration marker. It does not change credentials, badges, other seed content, schema or private-workspace permissions. Public portrait rendering also respects an explicitly absent photo rather than restoring a name-based image after removal.

## Validation

- `npm run typecheck --prefix frontend`: PASS.
- `npm run build --prefix frontend`: PASS.
- `npm test --prefix frontend`: **97 passed**, zero failures.
- Full backend suite: **245 passed**, zero failures/errors/skips. Command: `./mvnw -q -DargLine=-javaagent:/Users/khiry/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar test`.
- Relevant backend suites rerun after final metadata/read-authorization adjustments: FrontendSupportIntegrationTests, ProfessionalProfileIntegrationTests, ReviewControllerContractTests, SelfDealingBoundaryTests: PASS.
- `git diff --check`: PASS.

New regression coverage checks first/duplicate proposals and all history states; persisted review readback after clearing the persistence context, repeat reads, relationships and unauthorized actions; legacy migration idempotence; omission versus explicit removal for portrait/logo/portfolio; title-only preservation; and frontend existing-proposal/review/media presentation. Existing self-award, self-review and verified-provenance restrictions remain tested.

## Browser verification

Browser writes used `/tmp/rhp-blocker-fix.sqlite`, copied from the current original database. No review, bid, message, uploaded test image or profile-edit test data was retained in the normal demo database. The normal backend was restored afterward; Homeowner Jordan is active. The required two-cover migration runs on the original database.

Passed:

- Public Nina story opens, stage selector changes image, Escape returns focus; dialog has no `/projects/` link. Private project 2 returns 403, homeowner project 1 returns 200 and tradesperson-member project 8 returns 200.
- Existing cabinet review loads. Newly created plumbing review remains after two refreshes; API returns one correct record. No duplicate publish form for the reviewed person. Self-review returns 400.
- Existing Jordan proposal has no submit button. Edit to $1,400, reload, withdraw, reload all persist. Duplicate submitted and withdrawn attempts return 409. History remains.
- Owner name and legacy title edits retain work image. New external work add/view/edit/delete, personal/business credential CRUD, private-field filtering, media propagation and protected RH&P work all pass.
- Final title-only HTTP update is visible after browser reload and retains description/date/image. Cover, portrait and logo explicit removal/replacement pass with server responses awaited before reload.
- Self-bid returns 403. Three dual-role roundtrips retain correct owner/public controls. Homes, Barns and Outdoor Spaces collaborator links retain matching identity and back navigation.
- Owner management checks at 1440, 943, 390 and 720 CSS px pass layout/focus checks. No styling or trust-mark redesign performed.

A textarea locator mismatch and a verification script that refreshed before save completion were corrected in the temporary browser harness; neither was treated as a product failure. Final reruns pass.

The original fixed-Jordan limitations remain: no fresh eligible unbid scope or completed homeowner-owned Jordan fixture. First-bid success and accepted/rejected restrictions are verified in integration tests; the browser checks the existing submitted/withdrawn flow. No claim of a newly implemented multi-user or project-closing workflow.

## Files changed in this pass

- `frontend/src/api/workspace.ts`
- `frontend/src/components/PortfolioCard.tsx`
- `frontend/src/components/seedMedia.ts`
- `frontend/src/features/reviews.tsx`
- `frontend/src/features/work.tsx`
- `frontend/tests/demoBlockers.test.tsx` (new)
- `frontend/tests/workspace.test.tsx`
- `src/main/java/com/rockandhardplaces/api/ApiDtos.java`
- `src/main/java/com/rockandhardplaces/api/ProfessionalProfileController.java`
- `src/main/java/com/rockandhardplaces/api/ReviewController.java`
- `src/main/java/com/rockandhardplaces/demo/DemoPortfolioMediaMigration.java` (new)
- `src/main/java/com/rockandhardplaces/project/BidRepository.java`
- `src/main/java/com/rockandhardplaces/project/BidSubmissionService.java`
- `src/main/java/com/rockandhardplaces/review/ReviewRepository.java`
- `src/test/java/com/rockandhardplaces/api/FrontendSupportIntegrationTests.java`
- `src/test/java/com/rockandhardplaces/api/ProfessionalProfileIntegrationTests.java`
- `docs/RHP-FINAL-DEMO-AUDIT.md` (re-audit status notice; historical audit retained)
- `docs/RHP-FINAL-DEMO-BLOCKER-FIXES.md` (this report)

All other pre-existing RH&P-030 changes remain in the working tree. No POLISH or V3 implementation, commit, push, PR, merge, branch switch, reset or stash.
