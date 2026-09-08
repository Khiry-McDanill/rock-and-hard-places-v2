# RH&P-029 collaborator portfolio consistency

## Selection audit before changing portfolio data

Audited the running `/api/discovery/tradespeople` response with the actual `selectCollaborators` function and all eight `inspirationCategories` stories. Used concept.key as the direction, exactly as SeeItBuilt does. Seven unique people currently appear; discovery excludes the active account's Jordan Ellis profile. All seven have portraits, active accounts, verified account status, and Available soon status. Account verification is independent of portfolio provenance.

| Story | Actual collaborators |
|---|---|
| Homes | Owen Price, Nina Alvarez, Darius Cole |
| Barns | Caleb Morgan, Marcus Reed, Leah Bennett |
| RVs | Owen Price, Marcus Reed, Nina Alvarez |
| Buses / Skoolies | Caleb Morgan, Marcus Reed, Darius Cole |
| Tiny homes | Caleb Morgan, Nina Alvarez, Sofia Nguyen |
| Containers | Owen Price, Marcus Reed, Sofia Nguyen |
| Outdoor spaces | Caleb Morgan, Leah Bennett, Marcus Reed |
| Beyond / Greenhouse | Owen Price, Nina Alvarez, Leah Bennett |

| Person | Qualified trade; specialty | Suggested contribution | Existing published evidence before this pass | Gap / action |
|---|---|---|---|---|
| Owen Price | Carpentry; Finish carpentry | Cabinets, secure storage, desks, benches, planting-table frames | No RH&P or external portfolio, no work media. Existing bids do not establish completed work. | Add compact fitted cabinetry example. |
| Caleb Morgan | Carpentry; Decks and structural framing | Framing, benches, fitted furniture, timber members/connections | No published portfolio. Actual kitchen/deck/attic assignments exist, but active projects are not presented as completed portfolio history. | Add timber workbench/storage example. |
| Marcus Reed | Electrical; Residential panel upgrades | Lighting, power, accessible circuits | No published portfolio. Actual kitchen, bathroom and utility-room assignments exist; another person's bathroom portfolio does not supply Marcus's evidence. | Add workshop lighting/power example. |
| Nina Alvarez | Plumbing; Bathroom rough-ins | Sink/water connections, compact services, drainage | Cedar Park bathroom renovation — RH&P verified, actual completed assigned shower valve/drain task; registered coherent four-stage bathroom imagery. No external portfolio. | Relevant existing evidence retained. |
| Sofia Nguyen | Drywall; Plaster repair and finishing | Interior lining and finish | No published portfolio. Actual bathroom/attic/utility work and planning bids do not automatically publish her work. | Add studio plaster/wall-finish example. |
| Darius Cole | Flooring; Hardwood restoration | Floor repairs, durable living-zone finishes | Cedar Park oak floor restoration — RH&P verified, actual completed assigned floor task; four-stage floor imagery. No external portfolio. | Relevant existing evidence retained. |
| Leah Bennett | Exterior Restoration; Masonry and weatherproofing | Siding/exterior surfaces, weatherproofing, glazing edges | Fishtown exterior water repairs — RH&P verified, completed assigned masonry task, four-stage imagery. Germantown garden wall restoration — existing Externally verified item, checked-client-reference seed description, one wall image. | Relevant existing exterior/water-repair evidence retained; no new verification claims. |
| Jordan Ellis (additional network audit) | Carpentry; Built-in cabinetry | Not currently selected from this active account's discovery response | Walnut reading nook — Self-reported, one fitted shelving/window-seat image. Actual exterior trim assignment remains intact. | Already has relevant independent evidence; no addition. |

The pre-change matrix was established from the live discovery selection, portfolio database rows, deterministic seed source and media registry before adding records. All eight seeded tradespeople have useful evidence after this pass. Seven profiles remain intentionally modest with one item each; Leah has two. None is a portfolio dead end. We did not expand to 2–3 each just to meet a visual count.

## Exact new items and provenance

| Person | Title | Completion date | Provenance | Image |
|---|---|---|---|---|
| Owen Price | Compact built-in storage | 2025-09-18 | Self-reported | compact-built-in-storage.png |
| Caleb Morgan | Timber workbench and storage wall | 2025-10-09 | Self-reported | timber-workbench-storage.png |
| Marcus Reed | Workshop lighting and power upgrade | 2025-11-06 | Self-reported | workshop-lighting-power.png |
| Sofia Nguyen | Small studio plaster and wall finish | 2025-12-04 | Self-reported | studio-plaster-finish.png |

All four descriptions explicitly identify outside-RH&P, fictional demo history with illustrative imagery. Every project_id and task_id is null. No See It Built story is claimed as completed work. No concept-to-item relationship was added. Existing account verification, RH&P relationships, approved media/publication requests and external verification support remain unchanged.

## Media

Four independent images generated with the built-in image_gen tool, one per new item, saved in `frontend/public/seed-media/portfolios/rhp-029/`. Exact prompts are recorded in that directory's `prompts.json`. Visual inspection confirmed fitted cabinetry, timber bench joinery, electrical lighting/conduit/outlets, and plaster finishing respectively. No See It Built image or unrelated fallback was reused. Existing Nina, Darius, Leah and Jordan portfolio imagery remains in use.

## Implementation

Backend/seed files:
- `src/main/java/com/rockandhardplaces/demo/DemoDataSeeder.java`: additive versioned `rhp-029-collaborator-portfolio-v1` upgrade, existing demo identities/qualifications only, fixed dates, normal sequential database IDs, no random identities. One transaction covers records and marker. Existing title matches, removed qualifications, inactive accounts and subsequent edits are preserved.
- `src/main/java/com/rockandhardplaces/portfolio/PortfolioItem.java`: nullable stored media reference.
- `src/main/java/com/rockandhardplaces/portfolio/PortfolioSchemaMigration.java`: repeat-safe nullable-column upgrade for existing SQLite databases.
- `src/main/resources/schema.sql`: matching fresh-schema column.
- `src/main/java/com/rockandhardplaces/api/ApiDtos.java`: deliver media reference with real portfolio record.
- `src/test/java/com/rockandhardplaces/demo/DemoDataSeederTests.java`: outside-work provenance, media delivery, additive upgrade and repeat/edit preservation checks; retain authoritative RH&P completion/consent assertions.

Frontend files changed by this consistency pass:
- `frontend/src/api/workspace.ts`: optional portfolio media reference.
- `frontend/src/components/seedMedia.ts`: consume external record media reference without name matching; maintain RH&P consent/media path.
- `frontend/tests/collaboratorPortfolio.test.tsx`: independent names, no fake title/name fallback, no external image promoted into RH&P history.
- `frontend/tests/collaboratorPortfolio.browser.mjs`: live selection across every story, relevant evidence per trade, links, profile facts, provenance, dialogs, media responses/rendering and deliberate image-failure check.
- `frontend/tests/workspace.test.tsx`: include focused test.
- Four new PNGs and their prompt manifest.

The previously uncommitted See It Built UI, selection rules, story text and imagery were preserved.

## Final validation — READY FOR VISUAL REVIEW

- `npm run typecheck`: PASS.
- `npm run build`: PASS.
- `npm test`: PASS, 47 tests.
- `./mvnw -Dtest=DemoDataSeederTests,PortfolioControllerContractTests,Rhp019PortfolioTests test`: PASS, 23 tests. Initial sandbox execution could not attach Mockito; authorized execution outside the sandbox passed.
- Live Chromium audit: PASS across all eight story selections and all seven collaborator profiles. Owen, Marcus, Nina, Leah and Caleb screenshots were also visually inspected. Every portfolio detail opened, every normal image loaded, provenance matched the API, and intentionally blocked external media displayed no unrelated fallback.
- `git diff --check`: PASS.

Browser command from `frontend/` (uses the existing local Playwright installation):

```sh
PLAYWRIGHT_BROWSERS_PATH=/private/tmp/rhp-025-browsers PLAYWRIGHT_MODULE=/private/tmp/rhp-025-browser/node_modules/playwright/index.mjs node tests/collaboratorPortfolio.browser.mjs
```

Review screenshots and machine-readable story/contribution/item matrix: `/var/folders/xc/wv_35jxj6z309cf9n0nwwh7m0000gp/T/rhp-029-portfolio-review/`.
The normal `http://localhost:5173` preview serves the updated portfolio API through the restarted backend on port 8080. New records were applied through the application seeder, including the existing local database upgrade.

No commit, push or merge performed.

## Exact final git status --short

```text
 M frontend/src/api/workspace.ts
 M frontend/src/components/seedMedia.ts
 M frontend/src/features/public/InspirationPage.tsx
 M frontend/src/features/public/inspirationData.ts
 M frontend/src/main.tsx
 M frontend/tests/inspiration.browser.mjs
 M frontend/tests/inspiration.test.tsx
 M frontend/tests/workspace.test.tsx
 M src/main/java/com/rockandhardplaces/api/ApiDtos.java
 M src/main/java/com/rockandhardplaces/demo/DemoDataSeeder.java
 M src/main/java/com/rockandhardplaces/portfolio/PortfolioItem.java
 M src/main/resources/schema.sql
 M src/test/java/com/rockandhardplaces/demo/DemoDataSeederTests.java
?? docs/RHP-029-COLLABORATOR-PORTFOLIO-AUDIT.md
?? frontend/public/images/see-it-built/
?? frontend/public/seed-media/portfolios/rhp-029/
?? frontend/src/features/public/SeeItBuilt.tsx
?? frontend/src/features/public/seeItBuiltPeople.ts
?? frontend/src/styles/see-it-built.css
?? frontend/tests/collaboratorPortfolio.browser.mjs
?? frontend/tests/collaboratorPortfolio.test.tsx
?? frontend/tests/seeItBuilt.browser.mjs
?? frontend/tests/seeItBuilt.test.tsx
?? src/main/java/com/rockandhardplaces/portfolio/PortfolioSchemaMigration.java
```
