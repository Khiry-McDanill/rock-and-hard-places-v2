# RH&P UI/UX blueprint — THE HANDSHAKE

Recommendation: one React + TypeScript frontend, built with Vite and served by the existing Spring Boot application. Homeowner and Tradesperson modes share components and styling, with distinct navigation, priorities, and persistent role identification.

The main implementation dependency is API coverage. The domain supports substantially more than the current REST surface exposes. Several journeys need endpoint additions before they can work end to end.

This was a read-only inspection. Branch remains `rhp-022-ui`; no files were changed, and no commits, pushes, or PRs were created. Tests were inspected, not executed.

## 1. Repository findings

- Application: Java 17, Spring Boot 3.5.5, Maven
- Backend: Spring Web, Spring Data JPA, Jakarta Validation, SQLite
- Domain packages: `account`, `catalog`, `project`, `communication`, `review`, `portfolio`, `demo`
- REST layer: eight controllers and shared DTOs under `api`
- Frontend: no existing static UI, templates, JavaScript package, or frontend build configuration
- Account context: server-controlled demo identity with Homeowner/Tradesperson switching
- Errors: shared JSON response containing status, error, message, path, and field errors
- Tests: controller contracts, persistence tests, domain workflow tests, demo-data tests
- Demo data: rich persisted scenarios; runtime IDs must be obtained through APIs

Some architecture documentation describes earlier checkpoints. This blueprint uses current controllers and services for endpoint availability and workflow behavior.

## 2. Smallest clean frontend architecture

### Technology choice

Static HTML + vanilla JavaScript:
Lowest initial setup, but role switching, nested tasks, comparison, messaging, and coordinated refreshes would require substantial custom UI infrastructure.

Thymeleaf + progressive enhancement:
Reasonable for primarily page/form interactions, but this repo has no templates and already exposes a REST delivery layer.

React + TypeScript + Vite:
Best fit for shared components, role-specific screens, nested task interactions, comparisons, and API-driven state. Adds a frontend build step.

### Proposed structure

- `frontend/` — frontend source and build configuration
- App shell — routing, account bootstrap, role switcher, navigation, global errors
- Feature folders — profiles, projects/tasks, discovery, bids, messages, reviews/portfolio
- Shared components — forms, people cards, status labels, task lists, comparison layouts
- API layer — typed DTO contracts, fetch wrapper, consistent error handling
- Styles — CSS variables, shared base styles, component styles
- Build integration — package generated assets into the Spring Boot artifact

Use one client router and a small server-data query/cache layer. Keep form drafts and presentation state in component memory. Avoid an additional global business-state store.

Development would use Vite with `/api` proxied to Spring Boot. Production would use a single Spring Boot deployment, with frontend route fallback restricted to UI routes so API errors remain JSON.

### State rules

- Bootstrap from `GET /api/account`
- Obtain actor identity exclusively from the server account response
- Switch through `POST /api/account/switch`; update the UI only after success
- On switching, cancel or discard old requests, clear profile-scoped cached data, and load the destination mode
- Warn about unsaved forms before switching
- Store filters and selected comparison IDs in URLs where useful; retrieve records from the API
- Keep unsaved drafts in memory. Do not imply persistent drafts exist
- After mutations, refetch affected server resources
- Display returned progress percentages; do not implement another progress formula
- Treat frontend action visibility as guidance; server authorization remains decisive

## 3. Visual direction

THE HANDSHAKE

“Built on trust. Backed by skill.”

The interface should resemble a welcoming, well-organized project workspace.

- Cream/sand: page backgrounds and warm surfaces
- Charcoal: primary text and strong structure
- Terracotta/copper: homeowner emphasis and primary actions
- Navy: tradesperson emphasis and primary actions
- Sage: supporting surfaces and successful outcomes, with explicit labels
- Construction references: measured spacing, ruled dividers, restrained plan-grid details, clear work sections
- People: names and portraits lead team, bid, message, and discovery content
- Typography: readable sans-serif for working screens; a restrained characterful heading face for welcome and editorial moments

Homeowner screens emphasize “What needs my attention?” and guided next steps.

Tradesperson screens emphasize scope, location, availability, bids, and assigned work.

Use labeled line icons. Qualifications and provenance appear as factual information, without trophies, achievement scores, or emoji badges.

## 4. Global navigation and information architecture

### Desktop

Persistent sidebar with a top account bar showing portrait, name, active role, and “Switch profile.”

Homeowner:
- Overview
- My projects
- Find tradespeople
- Messages
- My profile

Tradesperson:
- Overview
- My work
- Find work
- My bids
- Messages
- My profile

Tradesperson profile contains:
- Qualifications
- Availability & service area
- Portfolio
- Reviews

Inside a project:

Overview · Tasks · Team · Bids · Messages · Completion

Tabs share layout across modes; actions and content visibility follow server permissions. Tradespeople see their own bids.

Desktop uses available space for:
- Project list alongside project summary
- Task list alongside selected task details
- Two or three tradespeople or bids compared across aligned attributes
- Conversation list alongside the active thread

### Mobile

Persistent top bar keeps the active role visible.

Homeowner bottom navigation:
- Overview
- Projects
- Find people
- Messages
- Profile

Tradesperson bottom navigation:
- Overview
- My work
- Find work
- Messages
- Profile

“My bids” appears prominently within Tradesperson Overview and My work.

Project sections become a compact labeled section selector. Task details, forms, and conversations open as full screens. Filters use a sheet; comparisons show two selected candidates with attributes grouped consistently.

Use one prominent contextual action near the bottom, respecting safe areas and the keyboard. Preserve browser Back behavior and list position.

Navigation targets needing missing APIs remain part of the blueprint, not simulated working features.

## 5. Homeowner journey, screen by screen

1. Overview
- Active homeowner identity
- Project summaries
- Work awaiting review
- “Create project”
- API: `GET /api/account`, `GET /api/projects`
- No aggregate attention endpoint

2. My profile
- Name, image, account status
- Edit profile
- API: `GET /api/homeowners/{id}`
- Missing: profile update and image management

3. My projects
- Project cards with title, ZIP, lifecycle, progress
- Filter by state
- API: `GET /api/projects`

4. Create project
- Name, description, job ZIP
- Save then organize work
- API: `POST /api/projects`
- Creation returns `PLANNING`
- No persistent draft workflow

5. Project overview/edit
- Project description
- Progress
- Next actions
- People involved
- API: `GET` and `PATCH /api/projects/{projectId}`
- Task/team reads
- PATCH expects title, description, ZIP

6. Plan tasks and subtasks
- Expandable work list
- Break into smaller tasks
- Required trades shown separately
- API:
  - `GET /api/projects/{projectId}/tasks`
  - `POST /api/projects/{projectId}/tasks`
  - `GET /api/tasks/{taskId}`
- Missing:
  - trade catalog
  - task editing/removal
  - requirement editing

7. Find tradespeople
- Search by required trade
- Inspect availability/service area
- Compare people
- Missing:
  - discovery/search endpoint
  - qualification reads

8. Tradesperson profile/compare
- Person first
- Qualifications
- Verification
- Availability
- Portfolio provenance
- Reviews
- API:
  - `GET /api/tradespeople/{id}`
  - `GET /api/tradespeople/{id}/portfolio`
- Missing:
  - qualifications
  - review reads
  - media delivery

9. Project team
- People
- Membership status
- Project roles
- Invitation/membership actions
- API: `GET /api/projects/{projectId}/team`
- Missing:
  - invitation
  - membership decision
  - suspension
  - role management

10. Compare bids
- Select task and required trade
- Compare person, amount, proposal, status
- Explain acceptance consequences
- API:
  - `GET /api/tasks/{taskId}/bids`
  - `POST /api/bids/{bidId}/accept`

11. Assign and monitor work
- Separate “Trades needed” from “People assigned”
- Choose eligible team members
- Inspect task status and progress
- API:
  - `POST /api/tasks/{taskId}/assignments`
  - task/project reads
- Missing:
  - assignment reads
  - eligibility information

12. Project conversation
- Team/private thread
- Participant identities
- Chronological messages
- Composer
- API:
  - `GET /api/projects/{projectId}/conversations`
  - `GET /api/conversations/{conversationId}/messages`
  - `POST /api/conversations/{conversationId}/messages`
- Missing:
  - participant details
  - new-thread/contact-request flows
  - attachments

13. Review completed work
- Approve work or request changes
- Keep discussion nearby
- API:
  - `POST /api/tasks/{taskId}/approve`
  - `POST /api/tasks/{taskId}/reject`
- Rejection currently accepts no reason payload

14. Completion and reviews
- Completed-work summary
- Review eligible tradesperson and task/project
- Later edit or withdraw
- API:
  - `POST /api/reviews`
  - `PATCH /api/reviews/{reviewId}`
  - `POST /api/reviews/{reviewId}/withdraw`
- Missing:
  - review reads
  - eligibility reads
  - explicit project completion action

15. Portfolio photo approval
- Inspect publication request
- Approve specific project photos
- Missing:
  - publication-request listing/decision
  - media endpoints

Bid selection must explain the actual transaction:
acceptance activates or creates membership, adds the required project trade role, creates the task assignment, and rejects other submitted bids for the same task-trade requirement. It does not select one person for the whole project.

## 6. Tradesperson journey, screen by screen

1. Overview
- Active tradesperson identity
- Availability
- Work needing attention
- Bids
- Project participation
- API:
  - `GET /api/account`
  - `GET /api/projects`
- Missing:
  - assignment listing
  - personal bid listing

2. Profile and qualifications
- Name
- Image
- Verification status
- Multiple trades and specialties
- Separate qualifications from verification
- API: `GET /api/tradespeople/{id}`
- Missing:
  - edits
  - qualifications/specialties reads and writes
  - verification submission

3. Availability and service area
- Availability
- Base ZIP
- Service radius
- Explain effect on discovery
- Current profile GET exposes values
- Missing:
  - updates
  - radius unit contract
  - search behavior

4. Find work
- Browse available task scopes
- Required trade
- Job location
- Filters
- Missing:
  - work discovery endpoint

5. Opportunity detail
- Scope
- Task/subtask context
- Required trade
- Homeowner context
- Own bid
- Contact action
- Missing:
  - opportunity read access for nonmembers

6. Submit bid
- Select specific required trade
- Amount
- Proposal
- Review submission
- API:
  - `POST /api/tasks/{taskId}/bids`
- Uses server-supplied `taskTradeId`

7. My bids
- Submitted
- Accepted
- Rejected
- Withdrawn
- API:
  - `GET /api/tasks/{taskId}/bids`
- Missing:
  - cross-project personal listing
  - withdrawal/edit endpoints

8. Memberships and project roles
- Active projects
- Invitations
- Membership status
- Project roles
- API:
  - `GET /api/projects`
  - `GET /api/projects/{projectId}/team`
- Missing:
  - invited membership listing
  - decisions

9. My work
- Assigned tasks grouped by project
- Open task
- Start work
- Submit for homeowner review
- API:
  - task/project reads
  - `POST /api/tasks/{taskId}/ready-for-review`
- Missing:
  - assignment listing
  - start-work transition

10. Messages
- Project/team/private conversations and requests
- Existing endpoints support participating threads
- Missing:
  - complete inbox
  - participant identities
  - requests
  - creation
  - media

11. Portfolio
- Published work with provenance
- Add completed work
- Request photo publication
- API:
  - `GET /api/tradespeople/{id}/portfolio`
- Missing:
  - owner management
  - unpublished items
  - publication workflow
  - media

12. Reviews
- Read feedback
- Show task/project context
- Post response
- API:
  - `POST /api/reviews/{reviewId}/response`
- Missing:
  - review/reply listing
  - response editing

A verification warning can explain why bidding is unavailable, but the UI must not offer a verification submission flow until its API exists.

## 7. Visually distinguish domain concepts

Active profile role:
- persistent portrait/name
- “Homeowner mode” or “Tradesperson mode”
- copper/navy shell accent
- always labeled in text

Qualification:
- section titled “Qualified trades”
- trade names and specialties beneath

Required trade:
- task section titled “Trades needed”
- outlined trade labels

Team membership:
- person row showing:
  “Membership: Active / Invited / Pending / Suspended”

Task assignment:
- section “Assigned to this task”
- explicit “Unassigned” empty state

Project role:
- separate field:
  “Roles on this project”

Bid status:
- label attached to proposal and amount:
  Submitted / Accepted / Rejected / Withdrawn

Task progress:
- status label
- returned percentage
- “Awaiting your review” where appropriate

Project progress:
- project lifecycle shown separately from percentage

Verification/provenance:
- explicit factual labels
- account verification and portfolio provenance stay separate

Example task:

- Trades needed: Plumbing
- Assigned to this task: Nina Alvarez
- Membership: Active
- Roles on this project: Plumbing
- Task status: Ready for review

These fields remain visually distinct even when values align.

Progress represents completed included leaf tasks, not elapsed time, money spent, or estimated effort.

“Ready for review” does not count as completed.

Use the server’s percentage.

Portfolio labels:
- RH&P verified
- Externally verified
- Self-reported

Do not translate account verification into claims about licenses or insurance.

## 8. Reusable UI components

Shell:
- account/role switcher
- desktop sidebar
- mobile navigation
- page heading
- project section navigation

People:
- portrait/name block
- profile card
- comparison column
- availability display

Domain distinctions:
- qualification list
- required-trade list
- membership row
- project-role list
- assignment list

Projects/tasks:
- project card
- progress indicator
- expandable task list
- task detail panel

Decisions:
- bid card
- comparison table
- acceptance confirmation
- work-review action panel

Forms:
- labeled inputs
- trade selector
- ZIP field
- amount input
- rating control
- field-error summary

Communication:
- thread list
- participant header
- message
- composer
- attachment placeholder/state

Trust:
- review card
- response block
- portfolio item
- provenance explanation
- publication request

Feedback:
- loading state
- empty state
- retry panel
- permission message
- stale-data notice
- mutation result

Shared components should accept explicit domain data.

Avoid a universal “badge” component that obscures domain distinctions.

## 9. API/UI blockers and mismatches

### Required for complete core journeys

1. Demo account context is application-wide
- Current demo context stores mutable role in a singleton
- Acceptable for controlled local demo
- Not suitable for independent production browser sessions
- Switching roles does not switch among all seeded people

2. Discovery and catalog APIs are absent
Need:
- trade/specialty reads
- people search
- restricted work discovery/detail contracts

3. New work cannot traverse the whole lifecycle
- Created tasks start in `PLANNING`
- Review submission requires `IN_PROGRESS`
- No exposed start action
- Bid acceptance and assignment do not start tasks
- Project lifecycle transitions also lack REST actions

4. Assignments can be created but not retrieved
Need:
- assignment reads
- eligibility/actionable reasons

5. Profile and qualification management lack REST support
Need:
- profile reads/writes
- qualifications/specialties reads/writes

6. Membership and project-role management are read-only through REST
Need:
- stable IDs
- authorized mutations

### Required for communication and trust journeys

7. Messaging lacks people and inbox context
- messages contain user IDs
- conversation DTOs omit participants/names
Need:
- participant details
- inbox support
- requests/creation
- media

8. Review writes exist without review reads
Need:
- listing/detail contracts
- eligibility data

9. Portfolio reads cannot power a complete gallery/editor
Need:
- media delivery
- unpublished owner-visible records
- publication requests and decisions

### Semantics to settle before wiring actions

10. Bid eligibility differs across operations
Prefer explicit server action eligibility.

11. Progress and project lifecycle can diverge
Do not infer COMPLETED from 100% progress.

12. Several modeled states have no exposed action
Examples:
- bid withdrawal
- task reopen
- task cancel
- start work
- some completion transitions

13. Display contracts need small additions
Clarify:
- bid currency
- service-radius units
- timestamp timezone
- safe image references

Do not invent:
- precise schedules
- distances
- credential details
- budget totals

14. List APIs lack pagination and summary context
Small demo screens can compose current reads, but production-scale discovery, inboxes, and dashboards need bounded list contracts.

## 10. Responsive and interaction acceptance criteria

- Active role remains visible on every screen, including mobile forms and conversations
- Narrow phone widths do not require page-level horizontal scrolling
- Desktop comparisons align the same facts across people or bids
- Keyboard users can navigate lists, dialogs, task expansion, and comparison controls
- Text labels accompany every status color
- Contrast and focus states are verified during implementation
- Forms have persistent labels
- Forms have inline errors and an error summary
- Forms show pending/success states
- Empty states explain the next available action without implying missing APIs work
- Failed mutations preserve entered text where practical
- 400 errors explain input issues
- 403 explains access
- 404 offers a route back
- 409 prompts refresh and reconsideration
- Role switching cannot allow late responses from the previous profile to populate the new screen
- Bid acceptance, task approval, and membership changes display server-confirmed outcomes
- Completed work, canceled work, missing images, suspended membership, and unavailable actions have explicit designs

## 11. Implementation Sequence

1. RH&P-022 — approved UI/UX blueprint
2. RH&P-024 — minimum frontend-support API gaps
3. RH&P-025 — responsive frontend implementation
4. demo hardening
5. dev → main release PR
