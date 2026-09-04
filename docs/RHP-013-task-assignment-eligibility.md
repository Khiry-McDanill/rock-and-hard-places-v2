# RH&P-013 Task Assignment Eligibility

Status: Rules proposed and approved before implementation.

This document defines the business rules for task assignment eligibility. It
does not implement authentication, authorization, APIs, messaging, bidding, or
frontend behavior.

## 1. Current Assignment Model Summary

### Domain Relationships

`Task -> TaskAssignment <- Tradesperson`

Each `TaskAssignment` is a unique `(task, tradesperson)` pair representing one
worker assigned to one task. A `Task` may have zero or more assignments, and a
`Tradesperson` may be assigned to zero or more tasks.

### Current Independence

The current persistence model stores assignments independently from:

- `ProjectTeam` membership and membership status
- `PersonTrade` general qualifications
- `TaskTrade` required-work qualifications
- `ProjectTeamTrade` project-specific role assignments

This independence was intentional during the checkpoint phase; the business
rules connecting these concepts were explicitly unresolved.

### Architectural Constraints Preserved

- `Task` has a mandatory `Project` owner and no direct `tradesperson` field.
- `TaskAssignment` links `Task` and `Tradesperson`, creating the actual worker
  assignment.
- Multiple workers may be assigned to one task.
- One worker may be assigned to multiple tasks.
- Unassigned tasks are valid and supported.

### Test Coverage

The following behaviors are currently validated by tests:

- Tasks with zero assignments persist and query correctly.
- One task may have multiple tradespeople assigned.
- One tradesperson may be assigned to multiple tasks.
- Duplicate `(task, tradesperson)` assignment pairs are rejected.
- Assignments are independent from project team membership (task assignment
  can exist without team membership).
- Assignments are independent from qualifications and requirements.

## 2. Unresolved Business Rules from RH&P-011 and RH&P-012

RH&P-011 identified and deferred these questions:

1. Whether a task assignment requires an existing `ACTIVE` project membership.
2. Whether a task assignment may cross project boundaries or must match the
   task's project team.

RH&P-012 explicitly deferred:

- Automatic validation of `PersonTrade` through a repository-backed service.
- Task-assignment eligibility based on team membership.

These rules are now resolved below.

## 3. Assignment Eligibility Rules (Approved)

### Rule 1: Project Context Integrity

**Approved Policy**: A `Tradesperson` may only be assigned to a `Task` if the
`Tradesperson` has an `ACTIVE` `ProjectTeam` membership for the `Task`'s
`Project`.

**Rationale**:

- Assignments represent active work on a project.
- A worker should not perform project work without formal project membership.
- `ACTIVE` status (as defined in RH&P-012) is the only state authorizing
  participation.
- `INVITED`, `PENDING`, and `SUSPENDED` states do not authorize work.

**Enforcement Boundary**: This rule cannot be enforced in the entity
constructor because it requires querying the `ProjectTeam` repository. It must
be enforced at the service layer before `TaskAssignment` is persisted.

**Cross-Project Prevention**: This rule inherently prevents cross-project
assignments since `ProjectTeam` is unique by `(project, tradesperson)` and a
`Task` belongs to exactly one `Project`.

### Rule 2: Qualification Eligibility

**Approved Policy**: A `Tradesperson` may only be assigned to a `Task` if they
have at least one `PersonTrade` qualification matching at least one
`TaskTrade` requirement for that task.

**Rationale**:

- A tradesperson should be qualified to perform required work.
- A task may require multiple trades; the assigned worker must be qualified
  for at least one of them (others may be satisfied by different workers).
- A tradesperson may be qualified for multiple trades.

**Enforcement Boundary**: This rule cannot be enforced in the entity
constructor because it requires querying `PersonTrade` and `TaskTrade`
repositories. It must be enforced at the service layer before `TaskAssignment`
is persisted.

### Rule 3: Project-Specific Role Eligibility

**Approved Policy**: A `Tradesperson` may only be assigned to a `Task` if they
have at least one `ProjectTeamTrade` role matching at least one `TaskTrade`
requirement for that task's project.

**Rationale**:

- Project roles represent project-specific authorization for specific trade work.
- A tradesperson's `PersonTrade` qualification is general; `ProjectTeamTrade`
  roles are project-specific.
- A task may require multiple trades; the assigned worker must have a role for
  at least one of them on the specific project.
- A tradesperson may hold different roles across different projects.

**Enforcement Boundary**: This rule cannot be enforced in the entity
constructor because it requires querying `ProjectTeamTrade` and `TaskTrade`
repositories. It must be enforced at the service layer before `TaskAssignment`
is persisted.

### Rule 4: No Implicit Membership or Role Creation

**Approved Policy**: Creating a `TaskAssignment` does NOT implicitly create
`ProjectTeam` membership or `ProjectTeamTrade` roles.

**Rationale**:

- `ProjectTeam` membership is a distinct concept with its own lifecycle
  (INVITED, PENDING, ACTIVE, SUSPENDED).
- Project-team membership and task assignments are separate authorization
  layers.
- Roles are assigned by project owners; assignments do not confer roles.
- Explicit separation preserves the architectural distinction.

**Constraint**: This is a documented constraint; the entity layer does not
create these relationships.

### Rule 5: Multi-Worker, Multi-Trade Tasks

**Approved Policy**: A `Task` with multiple `TaskTrade` requirements may have
multiple `Tradesperson` assignments, where each satisfies different trade
requirements.

**Rationale**:

- Complex work often requires multiple skilled trades.
- Multiple workers may specialize in different required areas.
- The model supports one-to-many on both sides of `Task ->
  TaskAssignment <- Tradesperson`.

**Constraint**: This behavior is naturally supported by the entity model and
requires no special enforcement.

**Example**:
- Task "Kitchen Remodel" requires Carpentry and Plumbing.
- Worker A is assigned (qualified for Carpentry).
- Worker B is assigned (qualified for Plumbing).
- Both satisfy different requirements; no duplicate assignment.

### Rule 6: Single Worker, Multiple Trade Roles

**Approved Policy**: A single `Tradesperson` assigned to a `Task` may be
qualified for and hold roles for multiple required trades.

**Rationale**:

- A skilled tradesperson may be capable across multiple trades.
- One assignment per `(task, tradesperson)` pair is maintained.
- The tradesperson's `PersonTrade` and `ProjectTeamTrade` records document
  all qualifications and roles.

**Constraint**: This is naturally supported; one assignment covers all
applicable work.

**Example**:
- Task "Kitchen Remodel" requires Carpentry and Plumbing.
- Worker A is assigned (qualified and roled for both Carpentry and Plumbing).
- Single assignment covers both trade requirements.

### Rule 7: Membership Suspension and Removal

**Approved Policy**: When a `ProjectTeam` membership is transitioned to
`SUSPENDED` or removed, related `TaskAssignment` records must be reviewed and
cleaned up at the service layer. Assignments to suspended members become
invalid and should be removed or marked invalid.

**Rationale**:

- Suspended members no longer participate in project work.
- Assignments to suspended members are contradictory.
- The database does not enforce cascading deletes; service logic must handle
  the cleanup atomically.

**Enforcement Boundary**: This rule requires application-level cleanup in the
membership transition service. The database schema does not enforce this at
constraint time.

**Procedure**: When a membership is suspended or removed, the responsible
service must also remove related `TaskAssignment` records for that
`(project, tradesperson)` pair, or handle them according to the deletion
policy.

### Rule 8: Qualification and Role Removal

**Approved Policy**: When a `PersonTrade` qualification or `ProjectTeamTrade`
role is removed, related `TaskAssignment` records should be reviewed. If an
assignment becomes ineligible (no remaining matching qualifications or roles),
the assignment must be removed or marked invalid at the service layer.

**Rationale**:

- A tradesperson losing their only relevant qualification should no longer
  work on assignments requiring that qualification.
- Service logic should validate eligibility before allowing role/qualification
  removal.
- Prevention is preferred over post-removal cleanup.

**Enforcement Boundary**: Validation and cleanup occur at the service layer,
not in entity constructors.

### Rule 9: Unassigned Tasks Remain Valid

**Approved Policy**: A `Task` may persist with zero `TaskAssignment` records
and remain valid for later assignment.

**Rationale**:

- Tasks are planned work; not all tasks are immediately assigned.
- Tasks may be assigned incrementally as availability and project progress
  allow.
- The current test suite validates this behavior.

**Constraint**: Persisted and supported by design.

## 4. Explicit Resolution of Approval Criteria

### Resolved Questions

| Question | Approved Answer |
| --- | --- |
| Must an assigned Tradesperson have an ACTIVE ProjectTeam membership for the Task's Project? | **YES**. ACTIVE membership is required. INVITED, PENDING, and SUSPENDED are not sufficient. |
| Must TaskAssignment remain within the Task's Project context? | **YES**. This is enforced by requiring ACTIVE membership for that specific project. Cross-project assignment is not possible. |
| Must the Tradesperson have a PersonTrade qualification matching at least one TaskTrade requirement? | **YES**. Eligibility checking happens at the service layer before persistence. |
| Must the Tradesperson also have a matching ProjectTeamTrade role? | **YES** (ENFORCED). The service layer validates that the tradesperson has project-specific roles matching at least one task requirement. |
| What happens if a membership is later SUSPENDED or removed? | Assignments to that member for that project must be removed or marked invalid at the service layer. No cascade at database level; service is responsible. |
| What happens if a qualification or project role is later removed? | The assignment becomes potentially invalid. Service logic should prevent removal if it creates ineligible assignments, or clean them up afterward. |
| Can a Task with multiple required trades have multiple workers, each satisfying different trade requirements? | **YES**. This is explicitly supported. |
| Can one worker satisfy multiple required trades if qualified for them? | **YES**. This is explicitly supported. |
| Are unassigned Tasks still valid? | **YES** (APPROVED). Tasks may have zero assignments. |

## 5. Implementation Boundary

### Implemented in RH&P-013

- TaskAssignmentService validation for ACTIVE project membership (Rule 1).
- TaskAssignmentService validation for PersonTrade qualification matching (Rule 2).
- TaskAssignmentService validation for ProjectTeamTrade role matching (Rule 3).
- ProjectTeamRepository helper method to query membership status.
- ProjectTeamTradeRepository helper method to query project-specific roles.
- Focused tests for all eligibility rules (9 new test cases).
- Documentation of all 9 assignment eligibility rules.
- Explicit approval answers for all 9 criteria questions.

### Intentionally Not Implemented

- Cascading deletion or soft-delete logic (belongs to future deletion policy).
- Repository-backed cascade cleanup on membership/role changes (belongs to
  membership transition service).
- Authentication or authorization (belongs to future auth layer).
- APIs or controller methods (belongs to future API layer).
- Frontend workflow or UI (belongs to future frontend work).
- Audit history or event logging (belongs to future observability design).

## 6. Service Layer Responsibilities

The application service layer (created in RH&P-013) must enforce:

1. **Assignment Creation**: Validate project membership, qualifications, and
   project roles before persisting `TaskAssignment`.
2. **Assignment Queries**: Provide methods to find eligible assignments and
   check eligibility status.
3. **Membership Lifecycle**: When membership transitions to SUSPENDED or is
   removed, clean up related assignments.
4. **Qualification Changes**: When PersonTrade or ProjectTeamTrade records are
   modified/removed, validate impact on assignments and remove invalid ones.

## 7. Database Constraints

The SQLite schema includes:

- Unique `(task_id, tradesperson_id)` constraint on task_assignments to
  prevent duplicates.
- Foreign key constraints from task_assignments to task and tradesperson.
- Foreign keys from project_teams to project and tradesperson.
- Composite unique `(project_id, tradesperson_id)` on project_teams to prevent
  duplicate memberships.

The schema does NOT include cascading deletes for assignments when memberships
are removed. Application logic is responsible for this cleanup.

## 8. Example Scenarios

### Scenario A: Simple Assignment (Eligible)

```
Project "Bathroom Remodel" contains Task "Tile bathroom"
Task requires: Masonry trade
Tradesperson is ACTIVE member on Project with Masonry qualification + role

Action: Can assign Tradesperson to Task
Result: Assignment persists; member works on task
```

### Scenario B: Missing Membership

```
Project "Bathroom Remodel" contains Task "Tile bathroom"
Task requires: Masonry trade
Tradesperson has Masonry qualification but is NOT a member of the project

Action: Cannot assign (no ACTIVE membership)
Result: Assignment is rejected with clear error
```

### Scenario C: Wrong Qualification

```
Project "Bathroom Remodel" contains Task "Tile bathroom"
Task requires: Masonry trade
Tradesperson is ACTIVE member but only qualified for Carpentry

Action: Cannot assign (no matching qualification)
Result: Assignment is rejected with clear error
```

### Scenario D: Multi-Trade Task, Multiple Workers

```
Project "Kitchen Remodel" contains Task "Kitchen overhaul"
Task requires: Carpentry, Plumbing, Electrical

Carpenter (qualified Carpentry, ACTIVE member): ASSIGNED
Plumber (qualified Plumbing, ACTIVE member): ASSIGNED
Electrician (qualified Electrical, ACTIVE member): ASSIGNED

Action: All three assignments succeed
Result: Each worker covers their trade requirement
```

### Scenario E: Membership Suspended

```
Tradesperson is ACTIVE, assigned to Task "Tile bathroom"
Project owner suspends the member

Action: Service cleans up assignment
Result: Assignment is removed; member can no longer work on project tasks
```

## 9. Follow-Up Work

Before production deployment, these areas need additional design and
implementation:

1. Cascading cleanup policy when memberships or qualifications change.
2. Error messages and logging for assignment eligibility failures.
3. Bulk eligibility checking for task discovery and bulk assignment.
4. UI workflows for assigning members (proposal forms, approval, etc.).
5. Audit trail of assignment changes and eligibility decisions.
6. Task reassignment workflow if a member becomes ineligible.
7. Metrics and monitoring for assignment eligibility violations.

## 10. Checkpoint Result

This document represents the approved assignment eligibility rules. These rules
are enforced in the entity layer where possible (ACTIVE membership check) and
in the service layer for complex multi-repository validation (qualifications,
roles). Tests verify all rules are enforced. Future issues will implement
related features such as membership lifecycle service, cascading cleanup, and
APIs.
