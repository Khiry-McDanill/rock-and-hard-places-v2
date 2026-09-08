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

`npm test` runs focused Node tests using esbuild and server rendering. They verify navigation by role, inactive-account presentation, progress semantics, API payloads/errors, and profile query isolation/cancellation. They are not a replacement for browser interaction tests.

## Current API limits

- API portrait and attachment references remain symbolic. The local demo uses removable presentation media from `public/seed-media`; attachment delivery remains a future integration.
- Profile/qualification editing, membership invitations and role management, project lifecycle changes, portfolio editing/publication approvals, and media upload/download have no current endpoints.
- Homeowner discovery supplies qualifications; the own-profile endpoint does not. No qualifications are inferred from assignments or global catalog specialties.
- Messages compose accessible project conversation reads. Sender account references are shown where names are absent. Thread creation, full inbox/requests, attachments, and realtime delivery are unavailable; use Refresh messages.
- Review writes exist without history/eligibility reads. Completed-task reviews can be submitted against recorded assignments; server rejection preserves the form. Previously saved reviews cannot be retrieved, edited, withdrawn, or replied to from this UI.
- My bids shows server-provided submitted bids; accessible task bid lists show other statuses. There is no complete personal history endpoint or bid edit/withdraw action.
- Bid amounts use the established USD display convention. The API supplies no currency field; multi-currency support remains outside this release. Service-radius units and bid timestamp timezone remain unspecified; radius units are not displayed.
- Production packaging and route forwards remain the integration plan above. Use Vite for the local demo.
