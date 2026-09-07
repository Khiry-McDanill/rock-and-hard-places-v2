# RH&P-023 demo dataset

The existing application-runner initialization now adds `rhp-023-v1` on normal startup.
Run `./mvnw spring-boot:run` with the existing local SQLite configuration. No credentials,
upload service, frontend code, or external network requests are involved.
Use `--rhp.demo.enabled=false` as an application argument to skip rich demo initialization;
the original active-context account initializer still runs. Ordinary tests disable rich
seeding, while `DemoDataSeederTests` enables it against a unique database under `target/`.

The rich graph and its `demo_seed_versions` marker commit in one transaction. Repeated
startup leaves existing rows, IDs, timestamps, homeowner edits, account states, and new
activity untouched. A failed initialization rolls back the graph and marker. Existing
catalog names are reused. A demo email collision without the marker fails explicitly
instead of adopting an unrelated account. Do not delete the marker to refresh an existing
database: use a new disposable database. This is a versioned initial snapshot, not a repair
or synchronization service; it intentionally does not recreate rows later removed manually.

All dates are fixed: activity runs January–August 2026, with older outside-work portfolio
completion dates. It will not drift with the wall clock. Numeric IDs are allocated normally
and may differ on databases containing other activity. Timestamp backdating is confined
to newly created seed rows after the ORM flush; production constructors and APIs retain
their existing behavior. Projects and tasks have no historical date fields in the model.

## People

All five homeowner profiles and eight tradesperson profiles are ACTIVE; all eight
tradespeople are VERIFIED in this fictional demo snapshot. No actual verification or
credential claims are made about real people.

| Person | Profile / qualification | Email |
| --- | --- | --- |
| Jordan Ellis | Homeowner + carpentry / built-in cabinetry | demo@rockandhardplaces.local |
| Maya Patel | Homeowner | maya-patel@demo.rockandhardplaces.local |
| Andre Brooks | Homeowner | andre-brooks@demo.rockandhardplaces.local |
| Elena Rivera | Homeowner | elena-rivera@demo.rockandhardplaces.local |
| Ruth Chen | Homeowner | ruth-chen@demo.rockandhardplaces.local |
| Caleb Morgan | Carpentry / decks and structural framing | caleb-morgan@demo.rockandhardplaces.local |
| Nina Alvarez | Plumbing / bathroom rough-ins | nina-alvarez@demo.rockandhardplaces.local |
| Marcus Reed | Electrical / residential panel upgrades | marcus-reed@demo.rockandhardplaces.local |
| Sofia Nguyen | Drywall / plaster repair and finishing | sofia-nguyen@demo.rockandhardplaces.local |
| Darius Cole | Flooring / hardwood restoration | darius-cole@demo.rockandhardplaces.local |
| Leah Bennett | Exterior restoration / masonry and weatherproofing | leah-bennett@demo.rockandhardplaces.local |
| Owen Price | Carpentry / finish carpentry | owen-price@demo.rockandhardplaces.local |

Jordan retains the RH&P-020 active-context email. Role switching reveals Jordan's kitchen
as homeowner and completed trim work on Andre's exterior project as tradesperson.
No service authorizes actions using a seed ID, and Jordan never bids on Jordan's kitchen.
An existing original demo profile retains its display name; fresh initialization uses Jordan Ellis.

## Projects and task scopes

Each project has one coordinating parent task and three trade-qualified subtasks.
Kitchen also has a cancelled pantry-niche subtask: **9 parents + 28 subtasks = 37 tasks**,
with **28 TaskTrade relationships**. The coordinating parents are not separately assigned
or counted as additional work. Completed children roll their parent up through RH&P-017.

| Project | Owner / ZIP | State / progress | Subtask scopes (in order) |
| --- | --- | --- | --- |
| Passyunk kitchen remodel | Jordan / 19147 | IN_PROGRESS / 33% | Maple base cabinets; sink supply and waste; island outlets; cancelled pantry niche |
| Cedar Park bathroom renovation | Maya / 19143 | COMPLETED / 100% | Shower valve and drain; moisture-resistant walls; GFCI protection |
| Fishtown cedar deck | Andre / 19125 | IN_PROGRESS / 33% | Ledger and joists; cedar decking; stair guards |
| Mount Airy attic framing | Elena / 19119 | IN_PROGRESS / 33% | Office partition; storage alcove; attic drywall |
| Fairmount plaster and drywall repairs | Ruth / 19130 | PLANNING / 0% | Stairwell ceiling; living room skim coat; window returns |
| Cedar Park oak floor restoration | Maya / 19143 | COMPLETED / 100% | Damaged oak boards; sanding and sealing; thresholds |
| Fairmount utility room coordination | Ruth / 19130 | IN_PROGRESS / 33% | Laundry shutoff valves; dedicated circuit; utility wall |
| Fishtown exterior water repairs | Andre / 19125 | COMPLETED / 100% | Brick repointing; window trim (Jordan); masonry transitions |
| Mount Airy side porch replacement | Elena / 19119 | CANCELLED / 0% | Porch survey; porch boards; foundation repairs (all cancelled before commissioning) |

Active projects have a completed first scope, a READY_FOR_REVIEW second scope, and an
IN_PROGRESS third scope. Assigned work goes through tradesperson submission and homeowner
approval. Cancelled work remains stored and excluded according to existing progress rules.

## Connected activity

- 26 bids: 21 ACCEPTED, 3 SUBMITTED on Fairmount drywall, 2 retained REJECTED competing
  carpentry bids on the kitchen and deck. Amounts range from $2,350 to $5,775.
- 21 task assignments created by bid acceptance, 15 ACTIVE project memberships, and 15
  corresponding project trade roles. Multi-trade jobs include several tradespeople;
  repeated work by the same person reuses their membership.
- 15 conversations: 7 PROJECT_TEAM and 8 PRIVATE. The planning project's private chat
  follows an accepted message request; commissioned project chats use active relationships.
- 52 ordered messages, spanning months, cover access, materials, handoffs, walkthroughs,
  and progress photos. Participants are synchronized through RH&P-018 services.
- 7 reviews: 3 completed-project reviews and 4 completed-task reviews on ongoing projects;
  overall scores of 4 or 5 with quality, communication, reliability, and professionalism
  ratings. Three public tradesperson responses. No same-user or stacked project/task reviews.
- 5 portfolio items: 3 RHP_VERIFIED entries validated against assigned completed tasks,
  Leah's EXTERNALLY_VERIFIED pre-platform garden wall, and Jordan's SELF_REPORTED walnut nook.
  Outside-work entries have no RH&P project/task links.
- 6 RH&P photo publication requests: 3 APPROVED by source homeowners, 2 PENDING, 1 DECLINED.
  Only the three approved photos qualify for public portfolio display.

## Media references and model limits

Profile image references use `demo/rhp-023/profiles/<person-slug>.jpg` (13 profile references,
12 distinct people; Jordan shares one image). Each commissioned project's project-title
slug has two image attachment references:

- `demo/rhp-023/<project-title-slug>/work.jpg`
- `demo/rhp-023/<project-title-slug>/detail.jpg`

These 14 JPEG metadata records have original filenames, content type, size, sender/uploader,
and historical timestamps. They are **symbolic demo references, not bundled image bytes
or working download URLs**. This follows the current model's reference-only storage
boundary. No upload/download infrastructure has been added. Future media delivery should
resolve these keys while retaining conversation access and publication approval checks.
Portfolio items have no general external-image field; only RH&P publication requests link
to authoritative message media. The external and self-reported examples therefore remain
textual portfolio entries rather than inventing unsupported image/provenance fields.

The legacy SQLite task constraint omitted READY_FOR_REVIEW despite the Java enum and
workflow supporting it. `TaskStatusSchemaMigration` repairs only that constraint, retaining
rows, IDs, hierarchy, indexes/triggers, and incoming relationships. Fresh schema initialization
includes the existing state directly. No new task state or business rule is introduced.

Focused verification: `./mvnw -q -Dtest=DemoDataSeederTests,TaskStatusSchemaMigrationTests test`.
The tests cover row-preserving reinitialization, transactional rollback, active-context
switching, qualification/assignment rules, cancellation and leaf progress, message access
and historical order, reviews, portfolio consent, and a populated legacy schema migration.
