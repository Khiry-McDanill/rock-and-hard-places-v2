# Rock & Hard Places frontend foundation (RH&P-024)

This is the shell for CRAFT × TECH × IMAGINATION. RH&P-025 owns the full responsive screens. The shell bootstraps a real server profile, switches profiles and checks the corresponding summary endpoint; it deliberately renders no dashboard cards or fake business data.

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
```

The lockfile pins dependencies. Fonts are bundled through Fontsource, so no runtime font service is required.

## Foundation contracts

- `src/api/types.ts`: DTO types for account, summaries, catalog, discovery, opportunities, assignments and the first workflow actions.
- `src/api/client.ts`: JSON fetch, abort signals and shared API errors including field errors. IDs always come from responses.
- `src/app/query.ts`: TanStack Query cache; profile resources include both role and profile ID because IDs may overlap between profile tables. Consume the query signal in every read. Invalidate profile resources after successful mutations with `refreshProfileData()`.
- `src/app/App.tsx`: routing, account bootstrap, server-derived theme and shared shell. Switching hides profile content, cancels requests and removes profile data; the destination theme is driven by the confirmed account response. Even when the switch response is lost, re-bootstrap the server context before resuming. No browser persistence or global business store.
- `src/styles/tokens.css`: sand, terracotta, sage, navy and charcoal; Playfair Display headings and Inter body. The darker homeowner action shade supports readable light button text while preserving the core terracotta accent.

Future forms must keep drafts in memory and warn before switching when dirty. There are no draft forms yet. A profile image reference is not assumed to be a deliverable URL; RH&P-025 should use a named fallback until media delivery is defined.

## Spring Boot production integration plan

Issue #31 requires a documented integration path, not deployment changes. `npm run build` currently writes standalone assets to `frontend/dist/`.

For RH&P-025/release integration:

1. Add an opt-in Maven frontend profile (or a CI build stage) that runs `npm ci` and `npm run build` before resource packaging.
2. Copy the contents of `frontend/dist/` into `${project.build.outputDirectory}/static/`. Do not check generated bundles into source or overwrite backend sources. Ensure clean builds remove old hashed bundles.
3. Serve the root index through Spring Boot static resources. Add explicit UI forwards for `/overview` and future approved UI paths to `/index.html`. Do not add a catch-all forward: `/api/**`, missing assets and backend errors must retain their JSON/error behavior.
4. Verify a packaged JAR serves assets and refreshed deep links, and unknown `/api` routes remain JSON 404s. Keep relative `/api` URLs for same-origin production; Vite proxy is development-only.

The existing singleton demo role remains shared by all browser tabs/clients. Independent sessions and authentication are outside this issue. Do not present this as production session isolation.

References: [Vite guide](https://vite.dev/guide/), [React Router declarative setup](https://reactrouter.com/start/declarative/installation), [TanStack request cancellation](https://tanstack.com/query/latest/docs/framework/react/guides/query-cancellation).
