# Rock & Hard Places — GitHub Copilot Instructions

## Project

Rock & Hard Places (RH&P) is a platform connecting homeowners and skilled
tradespeople around construction, renovation, and remodeling projects.

This repository is a clean rebuild of the original RH&P prototype.

Do not copy architecture or code from the original RH&P project unless the
developer explicitly asks for a specific piece of old code to be reviewed
and migrated.

## Development Philosophy

Build RH&P incrementally from GitHub Issues.

Before making changes:

1. Read the entire assigned GitHub Issue.
2. Inspect the existing codebase.
3. Understand the dependencies and acceptance criteria.
4. Implement only the scope of the current issue.
5. Do not build future features prematurely.
6. Run tests after implementation.

Prefer simple, readable Java over clever or unnecessarily abstract code.

This project is also being used as a learning project, so generated code
should remain understandable to a junior Java developer.

## Technology

Backend:
- Java
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- Jakarta Validation
- SQLite

Frontend technology will be decided later.

Do not introduce a frontend framework unless explicitly requested.

## Source of Truth

The backend and database are the authoritative source of application state.

Do NOT:

- use localStorage as authoritative business state
- hardcode database IDs
- hardcode project IDs
- hardcode user IDs
- hardcode trades as application logic
- duplicate persistent business data in the frontend

Frontend clients should request state from backend APIs.

## Account Model

RH&P has separate account/profile experiences for:

- Homeowner
- Tradesperson

A shared User identity may hold common account/authentication information.

Homeowner and Tradesperson must remain distinct profile/domain concepts.

Do not merge them into a single profile entity.

## Trade Model

Trade and Specialty are separate entities.

Relationship:

Trade 1 -> many Specialty

Examples:

Trade: Carpentry

Specialties:
- Framing
- Finish Carpentry
- Cabinetry

Trades must be stored as database records rather than hardcoded application
branches.

## Tradesperson Qualifications

A Tradesperson may perform multiple trades.

Do NOT place a single trade_id on Tradesperson.

Use:

Tradesperson -> PersonTrade <- Trade

PersonTrade represents what a tradesperson is qualified to do.

The Tradesperson + Trade pair must be unique.

## Projects

A Homeowner may own multiple Projects.

Each Project belongs to a Homeowner.

Project contains job location information that will eventually support
distance/radius-based trade discovery.

Project lifecycle status is separate from ProjectTeam membership status.

## Tasks

A Project contains Tasks.

Tasks may exist without a tradesperson assignment.

Tasks may have subtasks using a parent-task relationship.

Complex work may therefore be represented through:

- multiple trades on one task
- multiple workers on one task
- subtasks when decomposition provides better clarity

Do NOT store one tradesperson directly on Task.

## Required Task Trades

Use:

Task -> TaskTrade <- Trade

TaskTrade describes which trades are required to perform a task.

It does NOT represent which worker is assigned.

## Task Assignments

Use:

Task -> TaskAssignment <- Tradesperson

TaskAssignment represents the tradespeople actually assigned to work on a
task.

A task may have zero, one, or multiple assignments.

## Project Team

Use:

Project -> ProjectTeam <- Tradesperson

ProjectTeam represents membership in a specific project.

Use:

ProjectTeam -> ProjectTeamTrade <- Trade

ProjectTeamTrade represents the trade role or roles that a tradesperson
performs on that particular project.

Do not confuse these concepts:

PersonTrade = general qualification

TaskTrade = trade required by work

TaskAssignment = worker assigned to task

ProjectTeam = project membership

ProjectTeamTrade = person's role on that project

## Bidding

Bids belong to Tasks, not Projects.

Relationship concept:

Task -> Bid <- Tradesperson

A task may receive multiple bids.

A tradesperson may bid on multiple tasks within the same project.

A task may exist without bids.

Only one bid may ultimately be accepted for a bidding decision.

Bid data may include:

- amount
- message
- status
- timestamps

Bid negotiation history will be preserved through the communication system
rather than prematurely creating a BidRevision system.

Accepting a bid should eventually result in the appropriate TaskAssignment
and ProjectTeam membership.

Do not implement this behavior until its GitHub Issue is active.

## Communication

RH&P will support communication before and after bid acceptance.

First-contact communication may require MessageRequest approval to reduce
spam.

Accepted bidders and members of the same project team may communicate
without repeating the first-contact process.

Communication records will eventually use a unified Post-style model with
project, bid, sender, recipient, reply/thread, and forum/type context where
appropriate.

Do not implement messaging prematurely.

## Reviews

Reviews are about Tradespeople, not Bids.

A unified Post model may eventually represent reviews using a REVIEW type
and rating.

RH&P should distinguish:

- RH&P verified work
- external client verified work
- unverified portfolio work

## Portfolio

Tradespeople will eventually have portfolio items.

Portfolio content may represent:

- RH&P completed work
- external completed work

Completed RH&P tasks may later prompt tradespeople to add work photos to
their portfolio.

## Location

Location search should not permanently restrict users geographically.

Expected behavior will eventually include:

- project ZIP/job location
- tradesperson base location
- tradesperson service radius
- homeowner radius filters such as 25 / 50 / 100 miles / anywhere

Do not implement location search until its issue is active.

## Availability

Tradesperson availability may eventually include states such as:

- AVAILABLE_NOW
- AVAILABLE_SOON
- BUSY
- NOT_ACCEPTING_WORK

An optional available-start date may also be supported.

## Project Progress

RH&P will include an overall project progress indicator.

Tasks will contribute to project completion and should eventually be
visually represented within the overall progress experience.

The final weighting/calculation method has not yet been decided.

Do not invent one until that design decision is made.

## AI Project Planning

The future workflow is:

Homeowner describes project
-> AI asks project-specific questions
-> AI proposes structured tasks
-> Homeowner reviews/edits tasks
-> Homeowner approves project plan
-> Tasks become available for bidding

The AI should assist the homeowner rather than silently control project
scope.

Do not implement AI planning until its issue is active.

## Pricing

Future preliminary estimates will use structured tasks, quantities,
location, and regional pricing information.

Preliminary estimates are planning ranges, not contractor quotes.

Tradesperson bids represent actual marketplace offers.

## UI / Frontend

The RH&P user interface has NOT been finalized.

Do not treat the original RH&P frontend as the final design.

When frontend development begins, the interface will be intentionally
redesigned around the finalized workflows.

Prioritize:

- simplicity
- strong visual hierarchy
- mobile usability
- homeowner accessibility
- clear navigation
- trade discovery
- project/team visibility
- task progress
- bidding clarity
- trust and verification

Do not copy the original frontend wholesale.

## Original RH&P Code

The original application is reference material only.

When enough of the new architecture exists, selected old code may be audited
for reusable pieces.

Before migrating old code:

1. Identify exactly what problem the old code solves.
2. Verify it matches the new architecture.
3. Remove hardcoded IDs and localStorage business state.
4. Simplify unnecessary code.
5. Rewrite rather than copy when rewriting produces a cleaner result.
6. Add tests around migrated behavior.

Never migrate old code merely because it already exists.

## Testing

Every development issue should include appropriate tests.

After making changes:

- run relevant focused tests
- run the complete Maven test suite when practical
- fix failures caused by the implementation
- report tests executed and results

Do not remove or weaken tests just to make a build pass.

## Scope Control

The active GitHub Issue defines the implementation scope.

If a requirement belongs to a future issue, do not implement it early.

If the issue conflicts with these repository instructions, point out the
conflict before changing the architecture.

When uncertain about an RH&P business rule, ask rather than inventing a
major architectural decision.