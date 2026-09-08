# RH&P-025 product-copy audit

Copy audit complete; final visual review recorded below.

Scope: all frontend routes, shared components, loading/empty/error states, image alt/title text, HTML entry point, and CSS-generated text. Public homepage, homeowner overview, project creation/editing, projects, tasks, teams, bids, messages, discovery, profiles, portfolios, reviews, tradesperson overview/work/opportunities/bids, and profile switching were reviewed. No layouts, images, registrations, provenance, relationships, permissions, or business transitions were changed. No unsupported actions were added.

## Exact production files changed

- frontend/src/app/App.tsx
- frontend/src/components/ui.tsx
- frontend/src/components/PortfolioCard.tsx
- frontend/src/components/ProjectImage.tsx
- frontend/src/components/seedMedia.ts
- frontend/src/features/people.tsx
- frontend/src/features/messages.tsx
- frontend/src/features/reviews.tsx
- frontend/src/features/work.tsx
- frontend/src/features/projects.tsx
- frontend/src/features/homeowner/project/ProjectMediaGallery.tsx
- frontend/src/features/homeowner/project/ProjectTeam.tsx
- frontend/src/features/homeowner/project/HomeProjectWorkspace.tsx

Supporting files: frontend/tests/workspace.test.tsx (updated copy assertions and added error-display coverage), and this report. The homepage required no changes.

## Removed without replacement

| Exact previous copy | Location / treatment |
| --- | --- |
| Concept image · not an uploaded work photo | Portfolio image caption removed. |
| Illustrative stage views. Stage labels describe images, not recorded milestones. | Portfolio gallery disclosure removed. |
| Concept image — project photo not available | Project image tooltip removed. |
| Illustrative views of the build process. Project photos are not available yet. | Project gallery disclosure removed. |
| Individual qualifications are not available for this profile through the current profile endpoint. | Profile notice removed. |
| Profile editing, qualification changes, and image uploads are not available yet. | Unsupported profile-details notice removed. |
| Review history is not available in this view yet. | Review notice removed; no claim that an unloaded review history is empty. |
| Review history and response management require review listing support. | Unsupported review-management notice removed. |
| Radius {serviceRadius} (unit unspecified) | Radius suffix omitted from discovery/comparison service area; base ZIP remains. |
| Service radius: {serviceRadius or Not provided} (unit unspecified) | Profile radius paragraph omitted; base ZIP remains. Documentation does not establish miles. |
| Membership and project roles are shown here. Invitations and role editing are not available in this workspace. | Team limitation paragraph removed; current team data remains. |
| Team membership is managed outside this view. | Legacy team notice heading removed. |
| Invitations and project-role changes are not available yet. | Legacy team notice body removed. |
| New conversations and attachments are not available yet. | Removed from conversation guidance. |
| Participant names are not supplied for this conversation. Other senders are shown by their account reference. | Message-thread footnote removed. Existing Participant labels remain. |
| Verification submission is not available in this workspace. | Removed from bidding notice; factual bidding eligibility reason remains. |
| The project API does not specify currency. Confirm it with the homeowner. | Removed from bid form; amount label now explicitly uses the established USD presentation. |

The unsupported profile/review notice blocks and their headings (Your profile details, Reviews, Reviews & responses) were omitted; actual profile information and supported task-review forms remain.

## Rewritten copy

| Previous copy | Replacement |
| --- | --- |
| Concept image: {subject}; not an uploaded project photo | {subject} in image alt text |
| Build concept | Progress |
| Preparation concept | Early work |
| Finish concept | Detail |
| Work concept | Work photo |
| Concept views | Progress photos |
| Project photos not available yet | No project photos yet |
| Work photos not available yet | No work photos yet |
| Project photo unavailable | No project photos yet |
| {name} — portrait unavailable | {name} in the portrait accessibility label |
| Qualifications are not available in discovery. | No qualification details to show. |
| Project closure, review history, and portfolio photo approvals are not available in this workspace yet. Use Tasks to approve work awaiting your review. | Follow task progress and review completed work in Tasks. (Both project workspaces.) |
| Keep access details, decisions, and handoffs together. New conversations and attachments are not available yet. | Keep access details, decisions, and handoffs together. |
| Your review concerns this task and the person who performed it. The server checks eligibility and existing reviews. | Share your experience with this task and the person who performed it. |
| Proposal amount | Proposal amount (USD) |
| This view lists submitted bids. Accepted, rejected, and withdrawn proposals remain visible in accessible project task bids; a complete bid-history endpoint is not available. | Your submitted bids appear here. To see accepted, rejected, or withdrawn proposals, open the bids for the project task. |

Raw error.message and fieldErrors output was replaced in shared read errors, action errors, form notices, account loading errors, and profile-switch errors. Raw errors remain internal; error handling, requests, retry behavior, and business rules are unchanged. Product copy is selected by the existing response status:

| Existing status | Displayed copy |
| --- | --- |
| 400 / 422 | Check your entries and try again. |
| 401 | Your session could not be confirmed. Refresh and try again. |
| 403 | You don’t have access to this information or action. |
| 404 | We couldn’t find what you’re looking for. |
| 409 | Something changed. Refresh and try again. |
| 429 | Please wait a moment and try again. |
| Other errors | Something went wrong. Please try again. |

These numbers and internal field names are not rendered. The existing prefix “Profile switch could not be confirmed.” remains.

## Source-level audit classifications

The post-validation search covered forbidden terms and equivalents across frontend/src and frontend/index.html. Each match was inspected in context.

A — Internal, retained: api/workspaceApi imports and calls; ApiError and its raw internal message/details; seedMedia, seedPortrait, seedProjectMedia, projectPresentation, availableStoryAssets and related identifiers; presentation/delivered source metadata; fallback variables; /seed-media and demo storage references; the existing display-name normalization regex; CSS class names containing placeholder; HTML placeholder attributes with normal input hints; comments about APIs, seeds, server ownership and the concept board. toLocaleString/toLocaleDateString are formatting calls, not “local” product copy. README/docs/tests remain internal.

B — Legitimate product language, retained:

- RH&P verified, Externally verified, Self-reported: unchanged factual portfolio provenance.
- Account verification and “This is not verification of licenses or insurance.” / “Account verification does not verify licenses or insurance.”: factual boundaries of account verification, not implementation commentary.
- “Bidding unavailable” / “Bidding is unavailable”: actual eligibility restrictions. Their supplied reasons were checked against the existing authorization messages: “An active tradesperson profile is required” and “A verified tradesperson profile is required to submit bids.”
- “Assignment unavailable”: temporary assignment-load error, with the existing retry action.
- “Unavailable” for an inaccessible required-trade detail and “Scope no longer accessible”: product data/access states, not missing-feature explanations.
- Pending, suspended, rejected, withdrawn, unassigned, completed, availability statuses, and Awaiting your review: unchanged business states.
- “Not provided,” “None recorded,” “No trade specified,” and “No project photos yet”: intentional data/empty states.
- Participant {id}: existing anonymous participant labels; no invented names.
- Workspace, project membership, task-trade scope, and bid-acceptance consequences: existing product navigation and business concepts.
- Homepage “Inspiration” and “Barn restoration inspiration”: ordinary project inspiration, not disclaimers about implementation or data origin.

C — Rendered implementation language: removed or rewritten as listed above. No unresolved matches remain in frontend-owned rendered copy. User-authored project descriptions, messages, and other business content were not censored or rewritten.

## Confirmations

- No user-facing demo terminology in audited frontend-owned copy.
- No concept-image disclaimers, including alt text and tooltips.
- No illustrative/fallback terminology in product copy.
- No API/backend limitation commentary or raw service errors in rendered error components.
- No “unit unspecified”; no invented radius unit.
- No “currency not specified”; bid form matches existing USD bid cards.
- All three portfolio provenance labels preserved.
- Legitimate business states, eligibility, relationships, dates, and actions preserved.
- Nina's modal retains Project story, registered bathroom media, Before / Early work / Progress / Completed controls, RH&P verified, title, supplied completion date, and description. The two reported disclaimers are absent from its source. Layout/order was not redesigned; live browser rendering was not checked.

## Validation

- npm run typecheck: passed from frontend/. An initial invocation at the repository root failed because that directory has no package.json; corrected without code changes.
- npm run build: passed.
- npm test: 13 passed, 0 failed.
- Post-validation rendered-string source audit: complete; classifications above.
- Browser pass: not available. No browser tool or installed Playwright/@playwright/test/Puppeteer runtime was found. No browser visual or interaction success is claimed.

No commit, push, merge, branch switch, reset, or stash was performed. Exact final git status is supplied with the task response.

## Final finishing-pass review — 2026-09-08

The earlier validation above is historical. Conversation display labels now use the available project title plus “Project Team,” or “Direct conversation” when participant identity is absent. No database IDs are appended. The conversation guidance is now “Keep project questions, decisions, and updates in one place.” Factual opened dates remain unchanged.

Live Chromium checks used one desktop size (1440 × 1000) and one mobile size (390 × 844), against an isolated temporary SQLite database. Public navigation, homeowner project sections, messages and refresh, discovery, all three RH&P portfolio stories, both external portfolios, tradesperson work/bids/profile, and both profile switches were exercised. Mobile task navigation, modal Close/Escape with restored focus, and the home logo link were checked. The mobile account header intercepted logo taps; raising the existing mobile logo above that header fixed the navigation blocker. No page-level horizontal overflow or broken registered media was found. All 31 seed-media assets decoded, and image subjects/stages were visually reviewed. The shared brand geometry and wordmark were refined against the approved board; homepage structure, categories, and barn carousel behavior were preserved.

At that stage, one demo-data gap remained (resolved by the follow-up below): the seeded Jordan Tradesperson profile has no eligible open opportunities. All eligible carpentry requirements are already filled, completed, cancelled, or on Jordan’s own project. The planning project’s open scopes require Drywall. Find Work correctly shows its empty state; Opportunity Detail could not be reached through this intended demo journey. Eligibility rules, qualifications, and project scopes were not changed to bypass this gap. That pass reported NOT READY pending a decision about an appropriate opportunity fixture or an adjusted walkthrough.

The frontend-owned rendered-copy search found only internal identifiers/comments/storage references and normal input placeholder attributes. Portfolio provenance labels were preserved. Browser validation scripts, screenshots, service logs, and the isolated database remain under `/private/tmp`, outside the commit candidate. No business writes were performed during the walkthrough other than profile switching; the isolated account was returned to Homeowner.

Cleanup review: retain the concept board as design support, this audit and the portfolio handoff as useful documentation, and the seed inventory as an explicitly internal/removable fixture reference. Retain the four existing modified Java files: their changes repair exact legacy fixture copy, preserve customized content and seed markers, and test those repairs. Remove the unused `frontend/public/images/barn-before-after.png` composite before commit; keep the separate before/after images. Move `frontend/public/images/README.md` into documentation before release packaging so its useful provenance notes are not published as a static asset. These cleanup recommendations were reported before any deletion; no files were deleted or moved.


## Final blocker and cleanup follow-up — 2026-09-08

The authorized seed-only follow-up adds Carpentry alongside Drywall on Ruth Chen’s existing planning task “Finish window returns” in “Fairmount plaster and drywall repairs.” Jordan already has Carpentry / Built-in cabinetry qualifications and completed window-trim work. No qualification, existing bid, assignment, membership, progress value, portfolio provenance, review, or historical timestamp is changed. The separate `rhp-025-window-returns-carpentry-v1` seed marker applies the adjustment to fresh and existing fixtures once, without restoring a later user removal. Production discovery, same-user restrictions, verification requirements, and frontend business logic remain unchanged.

The unused `frontend/public/images/barn-before-after.png` was deleted after confirming it had no runtime references. The carousel still uses the separate before/after JPEGs. Developer provenance notes moved from `frontend/public/images/README.md` to [RH&P-025 media assets](RHP-025-MEDIA-ASSETS.md); no developer README remains in public assets. The earlier cleanup recommendations above are historical and are now completed.

Follow-up validation: `DemoDataSeederTests` 12 passed; `FrontendSupportIntegrationTests` 19 passed; frontend typecheck/build passed and 20 frontend tests passed. Chromium verified Jordan → Find Work → Opportunity Detail with the enabled existing bid form against the previously seeded isolated database after upgrade. Bid submission returned HTTP 201 in the rollback test; no browser bid was submitted. The isolated account returned to Homeowner. Cleanup was verified in both public sources and the built assets. The prior opportunity blocker is resolved; READY TO COMMIT, with no commit or release action performed.
