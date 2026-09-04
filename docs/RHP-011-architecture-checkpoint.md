# RH&P-011 Core Use-Case and Architecture Checkpoint

Status: checkpoint complete after RH&P-010.

This document audits the core domain model and use cases before bidding,
messaging, AI planning, estimates, authentication, APIs, or frontend work.
No new product feature is implemented by this checkpoint.

## 1. Current Architecture Summary

The backend and SQLite database are the authoritative source of persistent
business state. Spring Data JPA repositories provide persistence access, and
`schema.sql` defines the SQLite tables and database constraints. There are no
REST controllers or frontend clients in the current rebuild.

The model has four layers:

- Identity and profiles: `User`, `Homeowner`, and `Tradesperson`.
- Trade catalog: `Trade` and `Specialty`.
- Project planning: `Project`, `Task`, `TaskTrade`, and `TaskAssignment`.
- Project participation: `ProjectTeam` and `ProjectTeamTrade`.

Important cardinalities:

- One `User` may have a homeowner or tradesperson profile through separate
  one-to-one profile relationships.
- One `Homeowner` owns many `Project` records.
- One `Project` contains many `Task` records.
- One `Task` may have a parent task, zero or more required `TaskTrade` rows,
  and zero or more `TaskAssignment` rows.
- One `Tradesperson` may have many `PersonTrade` qualifications, many
  `TaskAssignment` rows, and many `ProjectTeam` memberships.
- One `ProjectTeam` membership may have many `ProjectTeamTrade` role rows.
- One `Trade` may be referenced by specialties, qualifications, task
  requirements, and project-specific roles.

The junction entities use mandatory foreign keys and database-level composite
unique constraints. Existing persistence tests cover the relationships and
reject duplicate pairs.

## 2. Homeowner Use Cases

### Manage homeowner profile

A homeowner owns a separate `Homeowner` profile linked to shared `User`
identity data. Profile management will eventually update profile information
without merging the homeowner concept with `Tradesperson`.

Current support: `User` and `Homeowner` entities and repositories.

Future work: authentication, authorization, profile APIs, and a profile UI.

### Create and manage projects

A homeowner creates projects. Each `Project` has exactly one homeowner, title,
description, job ZIP, and a `ProjectStatus` lifecycle value. A homeowner may
own multiple projects.

Current support: `Homeowner -> Project`, project persistence, lifecycle enum,
and job location field.

### Plan project tasks

A project contains multiple `Task` records. Tasks support controlled lifecycle
status and an optional parent task for subtasks.

Current support: `Project -> Task`, `Task.parentTask`, `TaskStatus`, and task
persistence tests.

### Identify required trades

A homeowner or future planning workflow identifies the trades needed by work.
`TaskTrade` records the trades required for a task; they do not identify a
worker.

Current support: `Task -> TaskTrade <- Trade`, including multiple trades per
task and duplicate prevention.

### Find and select tradespeople

The intended workflow will discover tradespeople using trade qualifications,
project work requirements, and eventually location, availability, trust, and
other marketplace data. A homeowner can then choose a person for project work.

Current support: the domain stores `PersonTrade` qualifications and project
roles, but no discovery query, location search, availability, ranking, API, or
UI exists yet.

### Build and manage project teams

A homeowner adds a tradesperson to a project through `ProjectTeam`. The
membership has its own status and is distinct from the project's lifecycle.
The person's role on that project is recorded with one or more
`ProjectTeamTrade` rows.

Current support: project membership, membership status, project-specific
multiple roles, inverse mappings, and duplicate constraints.

### Assign workers to tasks

A homeowner or later project workflow assigns one or more tradespeople to work
on a task using `TaskAssignment`. A task may remain unassigned.

Current support: `Task -> TaskAssignment <- Tradesperson`, including multiple
workers per task and multiple tasks per worker.

Open rule: whether every assigned worker must already be an active project team
member is not decided. The model deliberately does not infer membership from
assignment.

### Monitor task and project progress

Task status currently represents the lifecycle of an individual task. Project
status currently represents the project lifecycle.

Current support: stored `TaskStatus` and `ProjectStatus` values.

Missing by design: an overall project progress calculation, task weighting,
completion evidence, approval workflow, and progress history. These require a
business decision before implementation.

### Communicate during the project lifecycle

Homeowners will eventually communicate with tradespeople and project teammates
before and after work begins.

Current support: none. Communication is explicitly future scope and is not
represented by `ProjectTeam`, `TaskAssignment`, or a new checkpoint entity.

### Complete projects and review tradespeople

A homeowner can mark a project complete using project lifecycle status in the
current model. Reviews and trust history are future concepts about
tradespeople, not bids.

Current support: `ProjectStatus.COMPLETED` only.

Missing by design: review authorization, rating rules, review provenance,
verified RH&P work, external work, and unverified portfolio work.

## 3. Tradesperson Use Cases

### Manage tradesperson profile

A tradesperson has a separate `Tradesperson` profile linked to `User` and a
display name. It is not merged with `Homeowner`.

Current support: profile entity and persistence repository.

### Maintain multiple qualifications

A tradesperson may hold many general qualifications through `PersonTrade`.
The `(tradesperson, trade)` pair is unique.

Current support: `Tradesperson -> PersonTrade <- Trade`.

### Discover appropriate work

A tradesperson will eventually discover tasks and projects that match
qualifications, location, availability, and other marketplace rules.

Current support: the persisted qualification and project/task requirement
records that future discovery can query. No discovery feature is implemented.

### Participate in multiple projects

A tradesperson may belong to many projects through separate `ProjectTeam`
rows. The same person may have a different status and role set per project.

Current support: unique `(project, tradesperson)` membership and inverse
repositories.

### Join project teams and hold project-specific roles

`ProjectTeam` records membership. `ProjectTeamTrade` records the trade role or
roles performed by that member on that specific project. Multiple roles are
allowed for one membership, and duplicate role pairs are rejected.

A general `PersonTrade` qualification is not automatically copied into a
project role.

### Receive task assignments

A tradesperson receives work through `TaskAssignment`, independent of the
qualification and team-role junctions.

Current support: assignments can span many tasks, and a task can have many
workers.

### Update task and work progress

The current `TaskStatus` supports a basic task lifecycle, but there is no
tradesperson-facing update workflow, progress history, evidence, approval, or
completion note.

These rules remain future work.

### Communicate with homeowners and teammates

Communication is future scope. Project membership may later participate in
access rules, but it is not itself a message model.

### Build trust and portfolio history

The model has no review or portfolio entities yet. Future design must
separate RH&P-verified work, externally verified work, and unverified
portfolio work as described by the project architecture guidance.

## 4. System Responsibilities

- Enforce separate `Homeowner` and `Tradesperson` profile concepts under
  shared `User` identity.
- Preserve project ownership through the mandatory `Project.homeowner`
  relationship.
- Store trades and specialties as database records rather than hardcoded
  branches.
- Match general qualifications (`PersonTrade`) to required work (`TaskTrade`)
  without treating either as an assignment.
- Store actual workers on tasks through `TaskAssignment`.
- Store project membership through `ProjectTeam`.
- Store project-specific roles through `ProjectTeamTrade`.
- Keep `ProjectStatus`, `TaskStatus`, and `ProjectTeamStatus` separate.
- Enforce non-null foreign keys and duplicate-prevention constraints in the
  database as well as JPA mappings.
- Keep the backend and database authoritative; clients must not use local
  storage or hardcoded identifiers as business state.

## 5. Use-Case to Domain-Model Mapping

| Use case | Entities and relationships | Current coverage |
| --- | --- | --- |
| Manage homeowner profile | `User -> Homeowner` | Persistence model only |
| Manage tradesperson profile | `User -> Tradesperson` | Persistence model only |
| Create projects | `Homeowner -> Project` | Implemented and tested |
| Manage project lifecycle | `Project.status`, `ProjectStatus` | Implemented and tested |
| Plan tasks and subtasks | `Project -> Task`, `Task.parentTask` | Implemented and tested |
| Track task lifecycle | `Task.status`, `TaskStatus` | Implemented and tested |
| Identify required trades | `Task -> TaskTrade <- Trade` | Implemented and tested |
| Maintain qualifications | `Tradesperson -> PersonTrade <- Trade` | Implemented and tested |
| Find/select workers | `PersonTrade`, `TaskTrade`, future queries | Data foundation only |
| Build project team | `Project -> ProjectTeam <- Tradesperson` | Implemented and tested |
| Set project-specific roles | `ProjectTeam -> ProjectTeamTrade <- Trade` | Implemented and tested |
| Assign task workers | `Task -> TaskAssignment <- Tradesperson` | Implemented and tested |
| Monitor task progress | `TaskStatus` | Basic lifecycle only |
| Calculate project progress | `Project`, `Task` | Calculation not decided |
| Communicate | Future communication model linked to users/projects/tasks | Not implemented by design |
| Complete project | `ProjectStatus.COMPLETED` | Basic lifecycle only |
| Review tradespeople | Future review/post model and `Tradesperson` | Not implemented by design |
| Build portfolio history | Future portfolio/work verification model | Not implemented by design |

## 6. Entity Audit

### User

Shared identity/account anchor with unique email. Authentication and account
lifecycle are not implemented. The future authorization model must decide
whether one user may hold both profile types and how profile ownership is
managed.

### Homeowner

Distinct homeowner profile linked one-to-one to `User`; owns many projects.
The current display name is minimal but the ownership boundary is clear.

### Tradesperson

Distinct tradesperson profile linked one-to-one to `User`; inverse collections
expose qualifications, task assignments, and project memberships. It correctly
has no direct `trade_id`.

### Trade

Catalog record with unique name. It is reused by specialties,
qualifications, task requirements, and project roles through explicit
junctions.

### Specialty

One specialty belongs to one trade, with unique `(trade, name)`. The current
model does not yet connect specialties to person qualifications, task
requirements, or project roles. Whether specialties are needed in those
relationships is unresolved.

### PersonTrade

Explicit general qualification junction. Unique `(tradesperson, trade)`.
It must not be used as proof of a project role or task assignment.

### Project

Owned by one homeowner and containing tasks and team memberships. Its
`ProjectStatus` is the project lifecycle only. Job ZIP is a location seed for
future discovery, not a completed location-search model.

### Task

Belongs to one project, may have a parent task, and has independent task
status. It has collections for required trades and actual assignments. It has
no direct worker or trade field, which preserves the approved architecture.

### TaskTrade

Explicit required-trade junction for work. Unique `(task, trade)`. It does not
represent a person or role.

### TaskAssignment

Explicit actual-worker junction for work. Unique `(task, tradesperson)`. It
is independent of `TaskTrade` and `ProjectTeam`; the future business rule
linking assignments to team membership is unresolved.

### ProjectTeam

Explicit project membership junction. Unique `(project, tradesperson)` and a
separate `ProjectTeamStatus` enum. It does not store a direct trade role.

### ProjectTeamTrade

Explicit project-role junction between one membership and one trade. Unique
`(project_team, trade)`, allowing multiple roles on one membership. It does
not replace `PersonTrade`.

## 7. Explicit Distinction Validation

The model and tests validate these meanings:

- `PersonTrade` = what a tradesperson is generally qualified to do.
- `TaskTrade` = what trade capability the task requires.
- `TaskAssignment` = which tradesperson is actually assigned to the task.
- `ProjectTeam` = the tradesperson's membership in a specific project.
- `ProjectTeamTrade` = the trade role that member performs on that project.

No entity collapses these concepts, and no current relationship infers one from
another.

## 8. Lifecycle Separation Validation

`Project.status` uses `ProjectStatus` values such as `PLANNING`,
`IN_PROGRESS`, `COMPLETED`, and `CANCELLED`.

`ProjectTeam.status` uses the independent `ProjectTeamStatus` values
`INVITED`, `PENDING`, `ACTIVE`, and `SUSPENDED`.

They are separate Java enums, columns, constraints, and persistence tests. A
project may be `IN_PROGRESS` while a member is `PENDING`, and a member may be
`ACTIVE` while the project has another lifecycle state. No project status is
stored on a team membership, and no membership status is stored on a project.

## 9. Unresolved Business Rules

These rules were unresolved at the RH&P-011 checkpoint. Membership rules are
now defined in `docs/RHP-012-project-team-membership-workflow.md`.

- Whether a task assignment requires an existing `ACTIVE` project membership.
- Whether a task assignment may cross project boundaries or must match the
  task's project team.
- How project progress is calculated from tasks, subtasks, weights, and
  cancellations.
- Whether task status changes require homeowner approval or evidence.
- Which location fields, service-radius rules, and availability rules drive
  discovery.
- Whether specialties participate in qualifications, requirements, or roles.
- How authentication, authorization, account verification, and dual-profile
  users work.
- How bids become assignments or team membership when bidding is introduced.
- Communication permissions, first-contact approval, thread structure, and
  retention.
- Review eligibility, rating semantics, moderation, and verified-work rules.
- Portfolio ownership, evidence, and the distinction between external and
  RH&P-completed work.
- Project/team/task deletion and archival behavior.

## 10. Original RH&P Audit

The original implementation at `/Users/khiry/Projects/rock-and-hard-places`
was inspected as reference material only. No code was copied.

| Original concept | Decision | Audit result |
| --- | --- | --- |
| Project and task management workflow | REWRITE | The old UI communicates useful create/manage/view-task goals, but its model uses a looser project shape and fields that do not match the new owner, lifecycle, hierarchy, and junction design. Rebuild against the new backend model. |
| Project/task progress presentation | REWRITE | Dashboard and project pages show task counts/status and completion-oriented UI. Keep the user goal and information hierarchy, but define progress semantics first and calculate from authoritative task data. |
| Project team workflow | REWRITE | The old `ProjectTeam` and controller are a useful confirmation of the membership problem, but use a string status, lack the new project-role junction, and do not show the new constraints. Rebuild using `ProjectTeam`, `ProjectTeamStatus`, and `ProjectTeamTrade`. |
| Trade discovery and filters | REWRITE | The old `trades.js` hardcodes people, trades, specialties, ratings, and icons. Keep search/filter as a future UX goal; source results from backend entities and explicit rules. |
| Trade profile presentation | REWRITE | The profile page communicates useful trust and skill information, but hardcoded profiles and invitation behavior cannot be migrated as-is. Rebuild around persisted profiles, qualifications, project roles, reviews, and portfolio decisions. |
| Homeowner dashboard/workflow | REWRITE | The old homeowner page has useful project/team-request workflow concepts, but it relies on old endpoints and mixed assumptions. Preserve goals, redesign flow around the new domain and authoritative APIs. |
| Project dashboard/detail page | REWRITE | The old project page presents tasks and status, but uses fields/status values not aligned with the current model. Retain the inspection workflow, rewrite data and interactions. |
| Communication concepts | DISCARD AS-IS | The old implementation does not provide an approved authoritative communication model. Do not migrate UI assumptions before message/request/thread rules are decided. |
| Hardcoded IDs and sample records | DISCARD | Hardcoded or demo identifiers and records are not business state and violate the new source-of-truth rules. |
| `localStorage` team requests | DISCARD AS-IS | The old trade profile creates invitation objects with `Date.now()` and stores them in `localStorage`. This is explicitly non-authoritative and must not be migrated. |
| Old frontend structure and styling | DISCARD AS-IS | The old static page structure is reference material, not the finalized UI. Reuse no wholesale layout or frontend architecture. |
| Old API/controller code | DISCARD AS-IS | Controllers bind to the old model and endpoint assumptions. Reimplement future APIs against the new entities and authorization rules. |
| Existing imagery and branding assets | KEEP AS REFERENCE ONLY | Assets may inform future visual direction if licensing and product fit are confirmed, but they are not domain or UI architecture to migrate. |

## 11. Future UI/UX Journeys

These journeys should drive a later desktop and mobile redesign. They are
workflow descriptions only; no frontend is built here.

### Homeowner journeys

1. Create or edit a homeowner profile.
2. Create a project, set job location, and view project lifecycle.
3. Describe and organize work into tasks and subtasks.
4. Mark required trades for each task.
5. Search, filter, and inspect tradespeople by relevant persisted data.
6. Invite or add a tradesperson to a project and manage membership status.
7. Assign one or more active workers to a task while seeing task requirements.
8. Review task status and project progress without confusing the two lifecycles.
9. Communicate with project participants when communication rules exist.
10. Complete a project, verify work history, and submit an eligible review.

### Tradesperson journeys

1. Create or edit a tradesperson profile.
2. Add, remove, and review multiple general trade qualifications.
3. Discover suitable tasks/projects using approved matching rules.
4. Review an invitation or membership request and its project role.
5. Participate in multiple projects with separate membership statuses and roles.
6. View assigned tasks and update work progress when that workflow is defined.
7. Coordinate with homeowners and teammates through approved communication.
8. Present verified work, external work, portfolio items, reviews, and trust
   signals according to future provenance rules.

### Cross-cutting UI requirements

- Desktop views should support comparison, project navigation, team/task
  context, and dense status review.
- Mobile views should prioritize one workflow at a time, clear status labels,
  touch-friendly actions, and easy return to the active project or task.
- All views should read authoritative backend state and never treat client
  storage as business state.
- Invitations, roles, assignments, and requirements must be visually
  distinguishable so users do not confuse a qualification with an assignment
  or membership.

## 12. Checkpoint Result

### Validated

- Core identity/profile separation is present.
- Homeowner ownership of projects is present.
- Trades and specialties are persisted as catalog records.
- General qualifications, task requirements, task assignments, project
  membership, and project-specific roles are separate explicit concepts.
- Project and task lifecycles are represented with enums.
- Project lifecycle and project-team membership status are separate.
- Junction foreign keys and duplicate constraints are present in JPA and
  SQLite schema.
- Existing Issues #1-#10 persistence tests exercise the implemented core
  relationships, inverse mappings, and prohibited direct fields.
- The original implementation was audited without migrating code.

### Needs correction

RH&P-012 identified and corrected one concrete follow-up from this checkpoint:
the original status set could not represent suspension and reactivation. The
model now includes `ProjectTeamStatus.SUSPENDED`, with migration support for
existing SQLite databases and role-timing enforcement. The remaining absent
application workflows are intentionally future scope, not defects in this
checkpoint.

### Unresolved

Progress semantics, discovery rules, membership/assignment authorization,
communication, authentication, bidding transitions, reviews, portfolio
provenance, specialties in matching, and deletion/archival rules remain open.
They are recorded above rather than guessed.

### Recommended future GitHub Issues

- Define authentication, authorization, and profile/account lifecycle.
- Define project progress calculation, task completion evidence, and approval.
- Define location, radius, availability, and qualification matching.
- Define team invitation, approval, suspension, and membership permissions.
- Define task assignment eligibility and membership enforcement.
- Define bidding and its transitions into assignments and team membership.
- Define communication, message requests, threads, and permissions.
- Define reviews, verification, moderation, and portfolio provenance.
- Add APIs and contract tests after the domain rules are approved.
- Design and implement the new responsive homeowner/tradesperson UI.

This checkpoint does not authorize any of those features. They should be
specified and implemented only when their issues become active.
