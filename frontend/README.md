# Rock & Hard Places frontend (RH&P-025)

The responsive React workspace connects to the existing Spring Boot API and seeded demo account. Homeowner and Tradesperson modes have distinct navigation and server-backed workflows. No business data is stored in the browser.

## Local development

Use Node 22.12+ and npm. From the repository root:

```sh
./mvnw spring-boot:run
```

In another terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open the URL printed by Vite. `/api` proxies to `http://localhost:8080`; the browser uses relative URLs. To verify:

```sh
npm run typecheck
npm run build
npm test
```

The lockfile pins dependencies. Fonts are bundled through Fontsource, so no runtime font service is required.

## Foundation contracts

- `src/api/types.ts`: DTO types for account, summaries, catalog, discovery, opportunities, assignments and the first workflow actions.
- `src/api/client.ts`: JSON fetch, abort signals and shared API errors including field errors. IDs always come from responses.
- `src/app/query.ts`: TanStack Query cache; profile resources include both role and profile ID because IDs may overlap between profile tables. Consume the query signal in every read. Invalidate profile resources after successful mutations with `refreshProfileData()`.
- `src/app/App.tsx`: routing, account bootstrap, server-derived theme and shared shell. Switching hides profile content, cancels requests and removes profile data; the destination theme is driven by the confirmed account response. Even when the switch response is lost, re-bootstrap the server context before resuming. No browser persistence or global business store.
- `src/styles/tokens.css`: sand, terracotta, sage, navy and charcoal; Playfair Display headings and Inter body. The darker homeowner action shade supports readable light button text while preserving the core terracotta accent.

Forms keep drafts in memory. Switching asks before leaving an open form and waits for in-flight mutations. Local presentation portraits and project images are registered in `src/components/seedMedia.ts`; delivered media takes precedence. These removable assets do not change API records or business state. See `../docs/RHP-025-PORTFOLIO-MEDIA.md` for the media handoff.

## Spring Boot production integration plan

Issue #31 requires a documented integration path, not deployment changes. `npm run build` currently writes standalone assets to `frontend/dist/`.

For RH&P-025/release integration:

1. Add an opt-in Maven frontend profile (or a CI build stage) that runs `npm ci` and `npm run build` before resource packaging.
2. Copy the contents of `frontend/dist/` into `${project.build.outputDirectory}/static/`. Do not check generated bundles into source or overwrite backend sources. Ensure clean builds remove old hashed bundles.
3. Serve the root index through Spring Boot static resources. Add explicit UI forwards for `/overview` and future approved UI paths to `/index.html`. Do not add a catch-all forward: `/api/**`, missing assets and backend errors must retain their JSON/error behavior.
4. Verify a packaged JAR serves assets and refreshed deep links, and unknown `/api` routes remain JSON 404s. Keep relative `/api` URLs for same-origin production; Vite proxy is development-only.

The existing singleton demo role remains shared by all browser tabs/clients. Independent sessions and authentication are outside this issue. Do not present this as production session isolation.

References: [Vite guide](https://vite.dev/guide/), [React Router declarative setup](https://reactrouter.com/start/declarative/installation), [TanStack request cancellation](https://tanstack.com/query/latest/docs/framework/react/guides/query-cancellation).

## Implemented journeys

- Homeowner: overview, project list/create/edit, project workspace (Overview, Tasks, Team, Bids, Messages, Completion), task/subtask creation with required trades, assignment, task approval/request changes, task-trade bid acceptance, discovery and two-person comparison, profiles/portfolio, completed-task review creation.
- Tradesperson: operational overview, assigned scopes and project memberships, filtered opportunities and scope detail, bid submission, submitted bids, task start/review submission, project conversations, own profile and portfolio.
- Shared: desktop sidebar, role-labeled account switcher, mobile bottom navigation, server progress, pending/error/empty/inactive-account states, mutation feedback, labeled forms and keyboard focus.

`npm test` runs focused Node tests using esbuild and server rendering, followed by the mocked Project Builder browser suite described below. The Node tests verify navigation by role, inactive-account presentation, progress semantics, API payloads/errors, and profile query isolation/cancellation.

## Current API limits

- API portrait and attachment references remain symbolic. The local demo uses removable presentation media from `public/seed-media`; attachment delivery remains a future integration.
- Profile/qualification editing, membership invitations and role management, project lifecycle changes, portfolio editing/publication approvals, and media upload/download have no current endpoints.
- Homeowner discovery supplies qualifications; the own-profile endpoint does not. No qualifications are inferred from assignments or global catalog specialties.
- Messages compose accessible project conversation reads. Sender account references are shown where names are absent. Thread creation, full inbox/requests, attachments, and realtime delivery are unavailable; use Refresh messages.
- Review writes exist without history/eligibility reads. Completed-task reviews can be submitted against recorded assignments; server rejection preserves the form. Previously saved reviews cannot be retrieved, edited, withdrawn, or replied to from this UI.
- My bids shows server-provided submitted bids; accessible task bid lists show other statuses. There is no complete personal history endpoint or bid edit/withdraw action.
- Bid amounts use the established USD display convention. The API supplies no currency field; multi-currency support remains outside this release. Service-radius units and bid timestamp timezone remain unspecified; radius units are not displayed.
- Production packaging and route forwards remain the integration plan above. Use Vite for the local demo.

## Project Builder (RH&P-031 Phase 3)

`/projects/new` offers guided planning and the existing manual fields, converging on Project Review. Follow-up context is composed into Phase 1's existing `{ idea }` request with its 8,000-character limit. The original idea and answers remain in memory. Planning still uses the Phase 1 endpoint; unsubmitted drafts are not persisted.

Creation occurs only on **Create project**, through `POST /api/project-builder/create`. Both paths send the approved title, description, ZIP and tasks (`title`, `description`, `requiredTradeIds`). Only Keep/Apply items enter the payload; unresolved suggestions must be matched to catalog entries or removed. Kept trade recommendations must belong to a kept task. Review never creates arbitrary catalog trades or persists provider metadata.

The server derives the active homeowner, validates the full draft, and calls `ProjectWorkflowService` inside one transaction. A small `project_creation_receipts` table commits with the Project/Task/TaskTrade writes. The client retains one UUID `Idempotency-Key` across retries. The same owner/key/payload returns the original project; a changed payload after success returns 409. Failed transactions leave no project or receipt. The SQLite workflow serializes requests through commit, and the database owner/key constraint prevents duplicates across competing processes. This is a workflow receipt, not a new project type.

Manual-only creation accepts an empty task list and never requires planning or catalog availability. Planning failure and creation failure retain the review; creation failure supports retry without another conversation. Success opens the normal workspace. Existing project editing still uses the original API.

Install the browser once after `npm ci`:

```sh
npx playwright install chromium
npm run typecheck
npm run build
npm test
```

`npm test` runs the existing Node tests and the Playwright interaction suite; `npm run test:browser` runs only Playwright. The browser suite starts Vite on port 4179 and intercepts every `/api/` request. It never calls Gemini or a backend. The deterministic Hockessin Tree House fixture is test-only. It covers both entry paths, iterative answers, recommendation controls, manual saves, provider failures, timeout, stale responses, and 1440/943/390px layouts. Screenshots are written to ignored `frontend/test-results/` for visual review.


For real browser creation checks, start a separate backend database with Gemini disabled (from the repository root):

```sh
./mvnw spring-boot:run -Dspring-boot.run.arguments='--server.port=8088 --spring.datasource.url=jdbc:sqlite:/tmp/rhp-phase3-browser.sqlite --rhp.planning.gemini.api-key='
RHP_INTEGRATION_URL=http://127.0.0.1:8088 npm test --prefix frontend
```

The opt-in integration suite mocks only `/project-builder/plan`; all creation and workspace reads use that backend. It creates uniquely titled guided and manual projects at 1440/943/390px, verifies persisted tasks/catalog requirements, My Projects and every workspace section. It does not modify the existing manually created Hockessin project. Default `npm test` skips these three real-backend checks and runs the fully mocked browser suite. Install Chromium first as described above.
