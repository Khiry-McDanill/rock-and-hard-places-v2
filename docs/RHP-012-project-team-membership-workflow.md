# RH&P-012 ProjectTeam Membership Workflow

Status: approved domain workflow after RH&P-011.

This document defines the business rules for project-team membership. It does
not implement authentication, authorization infrastructure, APIs, messaging,
bidding, assignments, or frontend behavior.

## 1. Concepts

- `ProjectTeam` is one tradesperson's membership in one project.
- `ProjectTeamTrade` is a trade role held by that membership on that project.
- `PersonTrade` is a tradesperson's general qualification.
- `ProjectStatus` is the lifecycle of the project and is unrelated to team
  membership status.
- `ProjectTeamStatus` is the lifecycle of one membership.

A membership is unique by `(project, tradesperson)`. A membership may have
multiple project-specific trade roles, each unique by `(projectTeam, trade)`.

## 2. Actors and Permissions

The project owner is the homeowner who owns the `Project`. Until
authentication and authorization are implemented, these are domain-level
permission rules for future application services.

### Project owner / homeowner

The project owner may:

- invite a tradesperson to the project;
- approve or reject a tradesperson's membership request;
- withdraw an unanswered invitation;
- suspend an active member;
- remove a member from any membership state;
- reactivate a suspended member;
- manage the member's project-specific roles after the membership is active.

A homeowner who does not own the project may not perform these actions.

### Tradesperson

A tradesperson may:

- request membership in a project;
- accept or decline an invitation addressed to that tradesperson;
- withdraw their own pending membership request;
- leave their own active membership.

A tradesperson may not approve, suspend, reactivate, or change another
tradesperson's membership. A tradesperson may not assign project roles to
another member.

### System invariants

The future application service must enforce actor ownership and identity. A
client must never be able to select a different actor by supplying an ID, and
client storage is never authoritative membership state.

## 3. Status Meanings

### `INVITED`

The project owner initiated an invitation to a tradesperson. The invitation is
waiting for that tradesperson's response. It is not membership approval and
does not authorize project work or project roles.

### `PENDING`

The tradesperson initiated a request to join a project. The request is waiting
for the project owner's decision. It is not active membership and does not
authorize project work or project roles.

### `ACTIVE`

The membership was accepted by the invited tradesperson or approved by the
project owner. The member may participate in the project, and project-specific
roles may exist for this membership.

### `SUSPENDED`

The project owner temporarily disabled an active membership. The membership
record and its project roles remain for history, but the member may not
participate in project work while suspended. Reactivation returns the same
membership to `ACTIVE`; it does not create a duplicate membership.

Suspension is different from removal. Removal deletes the membership and its
role records according to the future deletion policy. The current persistence
model does not yet implement deletion cascades or audit history.

## 4. Creation and Valid Transitions

A new `(project, tradesperson)` pair may be created only through one of these
initiating actions:

| Action | Result |
| --- | --- |
| Project owner sends invitation | `null -> INVITED` |
| Tradesperson requests membership | `null -> PENDING` |

The valid transitions are:

| Current status | Actor and action | Result |
| --- | --- | --- |
| `INVITED` | Invited tradesperson accepts | `ACTIVE` |
| `INVITED` | Invited tradesperson declines | Remove membership |
| `INVITED` | Project owner withdraws invitation | Remove membership |
| `PENDING` | Project owner approves request | `ACTIVE` |
| `PENDING` | Project owner rejects request | Remove membership |
| `PENDING` | Requesting tradesperson withdraws request | Remove membership |
| `ACTIVE` | Project owner suspends member | `SUSPENDED` |
| `ACTIVE` | Project owner removes member | Remove membership |
| `ACTIVE` | Member leaves project | Remove membership |
| `SUSPENDED` | Project owner reactivates member | `ACTIVE` |
| `SUSPENDED` | Project owner removes member | Remove membership |

`INVITED` and `PENDING` are different initiators, not interchangeable approval
stages. There is no automatic transition between them.

## 5. Invalid Transitions

The following transitions are invalid:

- `INVITED -> PENDING` or `PENDING -> INVITED`;
- `INVITED -> SUSPENDED`;
- `PENDING -> SUSPENDED`;
- `SUSPENDED -> INVITED` or `SUSPENDED -> PENDING`;
- any transition performed by an actor without the permission listed above;
- any new membership when the `(project, tradesperson)` pair already exists;
- any reactivation that creates a second membership instead of reusing the
  suspended row;
- any direct status update that bypasses the actor and transition rules.

The current codebase has no application service or authorization layer, so the
full actor transition matrix is documented rather than exposed as an API.
`ProjectTeamStatus` persistence accepts the defined enum values, and the
`ProjectTeamTrade` constructor enforces the role-timing rule below.

## 6. Project-Team Role Timing

A `ProjectTeamTrade` may be created only when its `ProjectTeam` status is
`ACTIVE`. Roles may not be created for `INVITED`, `PENDING`, or `SUSPENDED`
memberships.

When an active membership is suspended, existing role rows remain associated
with the membership for history but are not active work authorization. If the
membership is reactivated, those roles become available again unless a future
workflow explicitly removes or changes them.

The current model enforces the creation-time rule in `ProjectTeamTrade` and
covers rejection of non-active memberships with a focused test. Future status
transition services must also prevent adding roles after a membership becomes
non-active.

## 7. PersonTrade Qualification Rule

Every `ProjectTeamTrade` role must be backed by a matching `PersonTrade` row
for the same tradesperson and trade:

`ProjectTeamTrade.projectTeam.tradesperson + ProjectTeamTrade.trade`
`requires PersonTrade.tradesperson + PersonTrade.trade`.

This is an eligibility invariant, not a merge of the two concepts:

- `PersonTrade` remains the general qualification record.
- `ProjectTeamTrade` remains the project-specific role record.
- A qualification does not automatically create a project role.
- A project role does not create or modify a general qualification.

The current entity constructor cannot query a repository, and the repository
layer has no application service, so this cross-entity invariant is documented
for the future membership/role service. It must be validated before a role is
created or changed; no API should bypass that service.

## 8. Project Lifecycle Separation

`ProjectStatus` describes the project: `PLANNING`, `IN_PROGRESS`,
`COMPLETED`, or `CANCELLED`.

`ProjectTeamStatus` describes one membership: `INVITED`, `PENDING`, `ACTIVE`,
or `SUSPENDED`.

Changing one does not implicitly change the other. For example, an
`IN_PROGRESS` project may have a `PENDING` or `SUSPENDED` member, and a
`COMPLETED` project may retain historical memberships. Any rules restricting
new membership activity based on project lifecycle require a separate future
business decision.

## 9. Implementation Boundary

Implemented in RH&P-012:

- added `SUSPENDED` to `ProjectTeamStatus`;
- expanded the SQLite status constraint;
- added an idempotent migration for existing local SQLite databases;
- rejected project roles for non-active memberships;
- added focused persistence coverage.

Intentionally not implemented:

- authentication or authorization;
- controller/service/API workflow methods;
- invitation or request entities;
- status transition audit history;
- deletion cascades or soft-delete policy;
- automatic validation of `PersonTrade` through a repository-backed service;
- task-assignment eligibility based on team membership.

## 10. Follow-Up Issues

Before APIs or UI are built, define and implement:

1. Authentication and authorization for project-owner and tradesperson actors.
2. A membership service that enforces the transition matrix atomically.
3. Repository-backed validation that project roles have matching
   `PersonTrade` qualifications.
4. Invitation/request expiry, notifications, audit history, and reason data.
5. Removal versus soft deletion and role-history retention.
6. Whether project lifecycle states restrict new invitations, requests, or
   reactivation.
7. Whether `TaskAssignment` requires active project membership.
