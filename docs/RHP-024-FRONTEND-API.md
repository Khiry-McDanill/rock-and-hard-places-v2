# RH&P-024 frontend support contracts

The existing Spring Boot domain/services and demo singleton identity are retained. `/api/account` and `POST /api/account/switch` already return the active profile and all available profiles with IDs, role, display name, image reference and status, so their contract is unchanged. Never choose actors from seed IDs.

## New endpoints

| Method/path | Active profile | Response / behavior |
| --- | --- | --- |
| `GET /api/dashboard/homeowner` | Active Homeowner | Owned project summaries, top-level/subtask counts, team roles and memberships, review queue, unfilled requirements and next-action codes |
| `GET /api/dashboard/tradesperson` | Active Tradesperson | Profile and bidding eligibility, active assignments, submitted bids with scope, completed work/project counts and matching opportunities |
| `GET /api/catalog/trades` | Either active profile | Trade IDs/names and nested specialty IDs/names |
| `GET /api/discovery/tradespeople` | Active Homeowner | Active other-user profiles, qualifications and selected specialties; optional `tradeId`, case-insensitive display-name `q`, exact `availability` enum |
| `GET /api/opportunities` | Active Tradesperson | Suitable open task-trade requirements; optional `tradeId` and exact `jobZip` |
| `GET /api/opportunities/{taskTradeId}` | Active Tradesperson | One currently discoverable requirement; otherwise 404 |
| `GET /api/assignments` | Active Tradesperson | Own assignments within currently accessible active memberships, including completed work |
| `GET /api/tasks/{taskId}/assignments` | Authorized active project profile | Assignment DTOs for the task; project access rules apply |
| `POST /api/tasks/{taskId}/start` | Assigned active Tradesperson | PLANNING → IN_PROGRESS, requires active membership and different user from homeowner; rejects closed projects and cancelled ancestry |

Wrong roles/inactive profiles return 403. Invalid filters or transitions return 400. Missing/inaccessible opportunity detail returns 404. Shared API error DTOs remain unchanged. Existing ready-for-review, approve, reject, submit-bid and accept-bid actions remain the domain authorities.

## Summary semantics

- `totalTasks` / `completedTasks` count included top-level tasks; separate subtask counts use the existing progress service. Cancelled tasks and descendants are excluded. `READY_FOR_REVIEW` is not completed.
- Percentages come exclusively from `TaskProgressService`. Lifecycle is returned separately; neither starting a task nor reaching 100% changes project lifecycle.
- Homeowner review queue contains included READY_FOR_REVIEW tasks. Project next actions: `PROJECT_CLOSED`, `REVIEW_WORK`, `PLAN_TASKS`, `FIND_TRADESPEOPLE`, `MONITOR_WORK`. Account next actions: `CREATE_PROJECT`, `REVIEW_WORK`, `VIEW_PROJECTS`. These are navigation guidance, not mutation authorization.
- A needed trade is an open requirement without an accepted bid or an active assigned member holding both that trade qualification and project role. Assignment is task-wide, so it alone cannot prove every required trade is covered.
- Active work excludes completed/cancelled tasks, cancelled ancestry and closed projects. Submitted bids are the domain's active bids, regardless of project lifecycle. Completed work counts completed included assigned tasks in currently accessible memberships; completed-project count uses explicit COMPLETED lifecycle among those memberships. Counts do not claim a lifetime history after membership loss.
- Discovery excludes the current user's opposite profile. Opportunity matching requires a recorded PersonTrade for the requirement, an active other-user homeowner, an open project and a PLANNING/IN_PROGRESS task with no cancelled ancestor. Filled requirements are excluded. Exact ZIP filtering is not distance matching.
- Opportunity detail grants no project membership, team/message access or visibility into other bidders. It includes only the requesting tradesperson's bids for that requirement. Existing protected project/task reads remain protected.
- `bidding` reports the existing submit service's account/verification prerequisite. Discovery suitability is separate from submission authorization: the current submit service does not require qualification, availability, open lifecycle or lack of an earlier bid. This issue does not silently tighten those existing domain rules. Acceptance still performs its existing stronger eligibility checks.
- Account verification is not a claim about licenses or insurance. No budget, schedule, currency, distance or radius unit is inferred.

## Deliberate limits and RH&P-025 follow-up

The read models use deterministic demo-scale lists and repository reads, without pagination or a search engine. Add database-level pagination and batched aggregate queries before scaling beyond the controlled demo. Catalog and opportunity lists sort by persisted IDs; no arbitrary actor IDs are embedded.

RH&P-025 can now build first dashboard/discovery/work screens against server DTOs. Full journey gaps from the blueprint remain outside RH&P-024: profile editing/media delivery, membership management, full inbox, review reads, portfolio editing/publication, explicit project lifecycle mutations and production sessions. Currency, service-radius units and bid timestamp timezone remain unspecified domain contracts; do not invent labels for them. See `frontend/README.md` for the production packaging and restricted UI route fallback plan.

## Verification

Run the full suite with `./mvnw test`. On a local runtime that prevents Mockito self-attachment, preload the resolved Byte Buddy agent in the test JVM using Maven `-DargLine=-javaagent:/absolute/path/to/byte-buddy-agent.jar`; this changes no application or test behavior. Then run the frontend typecheck/build and `git diff --check`.

### RH&P-024 review clarifications

Submitted bid summaries always retain the actor's own bid DTO. Live `project` and `task` are nullable: access requires current active project membership or current opportunity discoverability, with same-user and suspended-membership exclusions. Closed or filled opportunities do not independently grant scope access; an active project member retains their separately authorized project access.

`TradeSummary.specialties` contains global catalog options. `PersonSummary.specialties` contains only that person's selected specialties. Starting a planning child uses the existing progress reconciliation to reopen completed ancestors, without changing project lifecycle status. Projects with only cancelled task scope suggest `PLAN_TASKS`.
