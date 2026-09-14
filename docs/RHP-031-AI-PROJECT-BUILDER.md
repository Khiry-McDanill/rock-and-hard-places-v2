# RH&P-031 — Phase 1 AI Project Builder

Backend planning spike only. No React changes, project creation, task creation, bidding,
assignment, team membership, autonomous agents, search, pricing, scheduling, or contractor selection.

## Phase 1 closeout — READY FOR PHASE 2

Live evidence below was reported by the developer from the local VS Code environment;
Codex did not execute these live requests. The original structured-output incompatibility
is fixed. No further request/schema changes are required for Phase 1.

| Live stage/request | Result |
| --- | --- |
| A | PASS |
| B | PASS |
| C | PASS |
| D | PASS |
| E | PASS |
| F | PASS |
| G (first addition of `maxItems`) | 400 `INVALID_ARGUMENT` |
| H (normal schema key ordering) | PASS |
| I (real catalog names) | PASS |
| J (root string annotations) | PASS |
| K (nested annotations; normal request configuration) | PASS |
| L (normal request control and Java validation) | Provider 429 `QUOTA_EXCEEDED` |
| Normal `POST /api/project-builder/plan` | Provider 429, replacing the prior provider 400 |

**Root cause and compatibility decision:** `gemini-3.6-flash` rejected the provider-side
`maxItems` constraints in this responseJsonSchema. The normal schema omits those five
constraints while retaining object/array structure, required fields, closed objects,
confidence enums, booleans and annotations. Diagnostic G intentionally retains the
incompatible constraints as a local reproduction case.

**Application limits remain unchanged:** followUpQuestions ≤ 20, suggestedTrades ≤ 30,
tasks ≤ 50, assumptions ≤ 20, warnings ≤ 20. Strict Java parsing/DTO validation enforces
these after generation. Automated tests accept the exact limits and reject limit + 1.
Trade validation and the no-persistence rule remain unchanged.

**Current limitation:** Gemini free-tier quota, evidenced by provider 429. Quota exhaustion
is an availability condition, not an RH&P schema-validation failure, and must not prompt
further schema relaxation. The existing normal API envelope remains HTTP 503
`PLANNING_UNAVAILABLE`; upstream 429 is retained in safe server log metadata and in
local diagnostic status responses. No retries or extra live requests were added.

**Pending verification:** a complete live Tree House response through normal Java
validation and RH&P trade resolution remains pending until quota is available. K's PASS
confirms provider acceptance; L's quota response cannot establish successful full-flow
completion. Automated tests cover mapping, strict validation, trade resolution and
unchanged business-table counts. Phase 2 may proceed with this live verification item
explicitly outstanding.

**Diagnostic review:** retained. The listener requires the `local-gemini-diagnostics`
profile AND `rhp.planning.diagnostics.enabled=true`, binds only to hardcoded loopback,
and has no route on the normal MVC server. Without both opt-ins it does not exist.
Host/origin checks remain in place. Tests cover the gates, absent MVC route and no
business writes. Start normal application runtime without the diagnostic profile/flag.

**Logging review:** normal WARN logs include only operation, model, exception class and
upstream status. Provider messages and exception causes/stack traces are omitted.
Explicit DEBUG logging exposes only safe request shapes/fingerprints. Bounded, redacted
provider messages remain available only through the opt-in local diagnostic response.

The commands below are retained as manual reference; do not run additional live
requests during this closeout. The full smoke test is deferred until quota availability.

## Architecture and provider boundary

`POST /api/project-builder/plan` → existing `ActiveAccountContext` → active Homeowner
check through `AccountAuthorizationService` → `ProjectPlanningService` →
`ProjectPlanningAiClient.plan(ProjectPlanningPrompt)` → `ProjectPlanningAiResponse` →
Java validation → existing RH&P trade catalog resolution → `ValidatedProjectPlan`.

The provider interface, prompt and response are plain Java types. Only
`GeminiProjectPlanningAiClient` imports Google classes. Tests can replace the interface
with `@MockitoBean`. The service revalidates typed results even from replacement providers.

Google's supported [Google Gen AI Java SDK](https://github.com/googleapis/java-genai)
is pinned to `com.google.genai:google-genai:1.70.0`, compatible with this application's
Java 17. It uses the Gemini Developer API `models.generateContent` with
`responseMimeType=application/json` and `responseJsonSchema`. No tool configuration is sent.
The default model is `gemini-3.6-flash`; override with `GEMINI_MODEL` as needed.
See Google's [structured output documentation](https://ai.google.dev/gemini-api/docs/generate-content/structured-output)
and [model lifecycle](https://ai.google.dev/gemini-api/docs/deprecations).

## Server configuration

- `GEMINI_API_KEY` → `rhp.planning.gemini.api-key` (empty by default).
- `GEMINI_MODEL` → `rhp.planning.gemini.model` (`gemini-3.6-flash` by default).
- `rhp.planning.gemini.timeout-ms` defaults to 30000 milliseconds.

The key is explicitly supplied to the SDK, with the Developer API selected. Client
construction is lazy, so missing credentials do not prevent application startup.
Each request closes its client, uses a timeout and allows one attempt (no retry).
Normal WARN logging contains only operation, model, exception class and upstream status;
no provider message, stack trace, key, raw prompt or response is logged. Do not
enable HTTP wire logging when using credentials. `.env` files are ignored; Spring
Boot does not automatically load them. Provide secrets through the server environment.
Never use a React/Vite variable for this key.

## API and structured response

Request (the only input field is `idea`; account identity comes from the existing context):

```json
{"idea":"I want to build a tree house in Hockessin. I want it to be more than a kids' playhouse — something adults can use too, with a small deck, lighting, maybe power, and weather protection."}
```

Blank ideas and ideas over 8000 characters fail validation. As with existing project
APIs, unrelated request fields are ignored; a supplied user ID cannot select an actor.
The existing context is a demo account selector, not production credential authentication.

Response shape:

```json
{
  "plan": {
    "summary": "Advisory scope summary",
    "followUpQuestions": ["Freestanding or tree-supported?"],
    "suggestedTrades": [
      {"trade":"Carpentry","reason":"Framing review","confidence":"HIGH","needsConfirmation":true}
    ],
    "tasks": [
      {"title":"Review support approach","description":"Discuss options with a qualified professional","trade":"Carpentry","reason":"Adult use and deck loads require assessment","confidence":"MEDIUM","needsConfirmation":true}
    ],
    "assumptions": ["Adult use is intended"],
    "warnings": ["Professional structural and permit review is required"]
  },
  "recognizedTrades": [{"suggestion":"Carpentry","tradeId":123,"tradeName":"Carpentry"}],
  "unresolvedTrades": []
}
```

The example trade ID is illustrative. Actual IDs and names come only from the database.
All six plan fields and every nested field are required. Confidence is `HIGH`, `MEDIUM`
or `LOW`; `needsConfirmation` must be a JSON boolean. Arrays may be empty.

`src/main/resources/planning/project-plan.schema.json` constrains provider types,
required fields, confidence values and additional properties. Array limits, text size
and nonblank rules are enforced by Jakarta Validation. Provider-side `maxItems` is
omitted because local live bisection confirmed it causes `400 INVALID_ARGUMENT`
with `gemini-3.6-flash`. The isolated Jackson mapper rejects unknown properties,
duplicate keys, trailing content, invalid enums and scalar coercion. Null, empty,
malformed, oversized or invalid nested responses fail before catalog mapping.
Markdown fences are not stripped. Incomplete or blocked SDK candidates are rejected.

## Trade validation and safety

The service snapshots existing `TradeRepository.findAll()` scalar values before the
provider call. Names from both `suggestedTrades` and `tasks` are matched using only
case folding, trimming and whitespace collapse. Exactly one match yields the original
suggestion plus the authoritative catalog ID/name. Missing or ambiguous matches appear
in `unresolvedTrades`. There are no fuzzy matches, aliases, new Trade entities or saves.
The original plan preserves reasons, confidence, confirmation flags, assumptions and
warnings. Recognition means catalog membership only, not approval of the suggested work.

System instructions make the output advisory and treat homeowner text as untrusted data.
They request useful questions, actionable planning tasks, uncertainty and professional
confirmation. Tree house considerations include support approach, dimensions, access,
deck/railing, enclosure, weather protection, power/lighting, use, insulation/climate
control and finishes. These are contextual considerations, not a fixed questionnaire.
The instructions prohibit final engineering, approval claims, invented capacity/load/member
specifications, licensing guarantees, prices, contractor selection and scheduling.
Schema and catalog validation cannot prove the truth or safety of free text; suggestions
remain advisory and require human/professional review before any future project creation.

## No persistence

Planning has no project/task/bid/assignment/team repositories or workflow services and
opens no write transaction. It reads the catalog and returns DTOs only.
`ProjectBuilderIntegrationTests` flushes and clears the persistence context and compares
counts before/after planning for projects, tasks, task trades, trades, bids, assignments,
project teams and team trades. Unknown suggestions do not increase trade counts.
Existing application startup seeders are separate from the planning request.

## Failures

The existing API error envelope is reused: `status`, `error`, `message`, `path`, `fieldErrors`.

| Condition | HTTP | Error |
| --- | --- | --- |
| Wrong role, suspended/deactivated Homeowner | 403 | `FORBIDDEN` |
| Blank/oversized idea | 400 | `VALIDATION_ERROR` |
| Missing key or invalid provider configuration | 503 | `PLANNING_NOT_CONFIGURED` |
| Timeout, transport failure, provider error (including upstream 429 quota exhaustion) | 503 | `PLANNING_UNAVAILABLE` |
| Empty/malformed/invalid/blocked/truncated response | 502 | `PLANNING_INVALID_RESPONSE` |
| Unknown or ambiguous trade | 200 | Listed in `unresolvedTrades` |

Provider failures use fixed messages with no SDK cause or stack trace in the response.

## Automated validation

```bash
./mvnw -q test
git diff --check
```

Normal tests never call live Gemini. Endpoint integration tests mock
`ProjectPlanningAiClient`; parser tests cover malformed data and strict validation;
SDK adapter tests use a loopback HTTP stub to verify real SDK serialization and error handling.
The missing-key adapter test and existing full Spring contexts exercise no-key operation.
Frontend is untouched and does not need rebuilding.

## Optional manual Gemini smoke test

This is manual only; it is never part of `./mvnw -q test`. Set `GEMINI_API_KEY` in the
server environment using your secret manager or a silent shell prompt. Do not put
its value in a command argument, source file or shared transcript. Confirm the active
demo profile is an ACTIVE Homeowner (`GET /api/account`, and use the existing role switch
if needed). Start the backend from that environment:

```bash
./mvnw spring-boot:run
```

Send exactly one planning request:

```bash
curl --noproxy '*' --fail-with-body -sS http://127.0.0.1:8080/api/project-builder/plan \
  -H 'Content-Type: application/json' \
  --data-binary @- <<'JSON'
{"idea":"I want to build a tree house in Hockessin. I want it to be more than a kids' playhouse — something adults can use too, with a small deck, lighting, maybe power, and weather protection."}
JSON
```

Inspect the six typed plan fields, recognized/unresolved trades, questions, uncertainty,
and professional-confirmation flags. Verify there are no final engineering specifications,
price estimates or approval claims. Compare authoritative table counts before and after
the request if manually checking persistence. Never turn this request into project creation.

Local live bisection (reported by the developer): **A–F PASS; G returns 400
INVALID_ARGUMENT**. G first adds `maxItems`, confirming the provider incompatibility
in this request. The normal provider schema now omits only those five constraints.
Java limits remain: followUpQuestions 20, suggestedTrades 30, tasks 50, assumptions 20,
warnings 20. Automated tests accept each exact limit and reject limit + 1 after parsing.
After this fix H–K passed live. L and normal planning reached provider 429 quota
exhaustion. The full Tree House smoke test remains pending until quota availability;
Codex does not execute live requests.

## Local-only schema diagnostic listener (manual stages A–G)

Live Gemini calls are run manually from the developer's local environment. Codex does
not need, request or retrieve the key. Automated tests use fake credentials and a local
provider stub only.

Start Spring Boot from the VS Code terminal that already has your server environment
configured. Both the profile and enable flag are required:

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local-gemini-diagnostics \
  -Dspring-boot.run.arguments='--rhp.planning.diagnostics.enabled=true'
```

This adds a separate listener bound strictly to `127.0.0.1:8091`. It does not add any
route to the normal application server (usually port 8080), and it does not change
`POST /api/project-builder/plan`. Without both opt-ins the diagnostic listener does
not exist. Stop the application and restart without those opt-ins to disable it.
If 8091 is occupied, add `--rhp.planning.diagnostics.port=8092` to the arguments and
adjust the curl URLs. The bind address cannot be changed to a public interface.
Use the literal `127.0.0.1` URLs below: other Host values, browser Origin headers,
non-POST methods, request bodies and query parameters are rejected. No CORS is enabled.
Do not publish or reverse-proxy this local development listener.

Each request makes one provider attempt through the existing Gemini adapter, reusing
its configured model/key/timeout, system instructions, MIME type, candidate count,
output-token limit and SDK client construction. The diagnostic prompt is fixed to:

> I want to build a tree house in Hockessin. I want it to be more than a kids' playhouse — something adults can use too, with a small deck, lighting, maybe power, and weather protection.

The prompt's catalog-name list is consistently empty for all stages: schema probes
have no catalog or business repository dependency. They do not invoke the planning
service, trade resolution or domain workflows. This keeps the prompt identical across
stages and avoids database access on the diagnostic path.

| Stage | Addition |
| --- | --- |
| A | Required `summary: string` in an object |
| B | Required `followUpQuestions: string[]` |
| C | Required `suggestedTrades: object[]`; required trade/reason strings, HIGH/MEDIUM/LOW string enum, needsConfirmation boolean |
| D | Required `tasks: object[]`; required title/description/trade/reason strings, confidence enum, needsConfirmation boolean |
| E | Required `assumptions: string[]` and `warnings: string[]` |
| F | `additionalProperties: false` on the root and both nested object types |
| G | `maxItems`: questions/assumptions/warnings 20; trades 30; tasks 50 |

Stages intentionally omit descriptive annotations. The normal provider schema now
matches F structurally; only diagnostic G deliberately retains `maxItems` to reproduce
the confirmed provider failure. Diagnostics remain gated by both explicit opt-ins and
the separate loopback-only listener, unavailable in normal application runtime.
Strict Java DTO validation and safe redacted provider error logging are preserved. Early stages cannot satisfy the full planning DTO;
diagnostics check provider acceptance and a complete JSON object without exposing that
object. `PASS` is a schema acceptance signal, not proof of a validated RH&P plan.

Run these **one at a time in order**, stopping at the first `status: "400"`. The
mechanism never runs another stage automatically:

```bash
# A
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/A
# B
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/B
# C
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/C
# D
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/D
# E
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/E
# F
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/F
# G
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/G
```

Valid diagnostic requests return HTTP 200 with exactly three fields, for example:

```json
{"stage":"C","status":"400","message":"Request contains an invalid argument."}
```

Inspect the JSON `status`, not curl's exit status: it is `PASS`, a provider HTTP code
such as `400`/`429`/`503`, or `NOT_CONFIGURED`, `UNAVAILABLE`, `INVALID_RESPONSE`.
Do not interpret a timeout, rate limit or incomplete response as a schema rejection.
A success message reports acceptance only. Provider messages are bounded, flattened,
and redact the configured key, Google-key patterns, sensitive auth headers and the
fixed homeowner idea. No response content, client configuration or credential is returned.

`LocalGeminiSchemaDiagnosticsTests` sends all seven requests over the real loopback
listener through the real adapter to a local HTTP provider stub. It compares counts
for projects, tasks, task trades, trades, bids, assignments and teams before/after each
stage and provider failure. It also tests rejection of browser/invalid requests and
absence of the route from the normal MVC server. Separate tests prove both opt-ins
are required and check each stage's schema structure. Normal application startup
seeders remain independent of diagnostics.

## Stage F versus normal request: local H–L comparison

The request builder is shared. F already uses the exact fixed Tree House idea, normal
system instructions, `responseMimeType=application/json`, `candidateCount=1` and
`maxOutputTokens=8192`. Adding those again would not isolate a difference.

| Field | Stage F versus normal planning |
| --- | --- |
| Model | Same configured model (`gemini-3.6-flash` default) |
| Contents shape | Same single user content and text part containing serialized `ProjectPlanningPrompt` JSON |
| Idea | Identical when normal planning receives the exact Tree House example; normal service strips surrounding whitespace |
| Catalog in user prompt | F has `catalogTradeNames: []`; normal uses names from `TradeRepository.findAll()` in returned order |
| System instruction | Identical resource and SDK construction |
| MIME/schema option | Both use `application/json` and `responseJsonSchema`; neither supplies `responseSchema` |
| Schema structure | Same types, required fields, enum values and closed objects; neither includes `maxItems` |
| Schema annotations | Normal adds descriptive annotations on summary, string-array items, and nested trade/task string fields |
| JSON schema key order | The F builder and loaded normal resource serialize object members in different orders; semantically equal after removing annotations |
| Explicit generation settings | Both set candidate count 1 and output-token limit 8192 |
| Temperature, topP, topK, thinking, safety settings, tools/toolConfig, seed, stop sequences | All unset by both paths; SDK/provider defaults apply |
| HTTP/API mode | Both explicitly select Gemini Developer API; same timeout and one attempt, same default API version (SDK stub tests observe `/v1beta/models/gemini-3.6-flash:generateContent`) |
| Post-response behavior | Normal applies strict Java DTO validation and the planning service resolves catalog trades; F reports provider acceptance only |

These source and SDK serialization findings were subsequently checked live: H–K passed.
Catalog names and schema annotations/order did not reproduce the earlier 400. L and
normal planning now encounter provider quota exhaustion (429). See the closeout evidence
above; no remaining non-schema request defect was established.

The local listener now accepts H–L on the same route, with no body or query:

| Stage | One change from the preceding control |
| --- | --- |
| H | From F: use normal schema object-member ordering, still without descriptions and with an empty catalog |
| I | From H: add the actual RH&P catalog-name list to the fixed prompt, using the same query/order as the normal service |
| J | From I: add normal description annotations on summary and the three root string-array item schemas |
| K | From J: add normal description annotations inside suggestedTrades/tasks; request now matches normal planning |
| L | Exact normal-request repeat/control, including strict Java response validation; deliberately introduces no new provider settings |

All stages retain the existing model, system instructions, generation settings and
client configuration. Do not modify the catalog between stages. The catalog is read
only for H–L; no diagnostic calls a domain workflow or saves entities. L uses the shared
normal provider request construction and validator without creating a project or returning
provider content. Live provider errors remain the three-field stage/status/safe-message
response; schema acceptance does not prove trade validity. Stage L reports Java validation
failures separately as `INVALID_RESPONSE`.

Start/restart your local application with diagnostic logging enabled. This compiles the
current resources/code and makes runtime schema fingerprints visible:

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local-gemini-diagnostics \
  -Dspring-boot.run.arguments='--rhp.planning.diagnostics.enabled=true --logging.level.com.rockandhardplaces.planning.GeminiProjectPlanningAiClient=DEBUG'
```

Run these individually; stop at the first provider `400`:

```bash
# H: F structure in normal schema key order
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/H
# I: actual catalog names in the user prompt
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/I
# J: top-level string description annotations
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/J
# K: nested trade/task description annotations
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/K
# L: exact normal request control plus Java validation
curl --noproxy '*' -sS -X POST http://127.0.0.1:8091/diagnostics/gemini/schema/L
```

DEBUG logs include `Gemini safe request comparison` followed by JSON with a label,
model, content shape, idea length, catalog count, prompt/system/schema SHA-256 fingerprints,
schema property and required names, explicit settings or `UNSET`, timeout/attempts/API
mode, and `maxItemsPaths`. No credential, header, raw prompt/catalog or raw system
instruction is included. The existing schema-shape DEBUG line remains available.
For the normal endpoint the label is `normal`; verify `maxItemsPaths: []`. A nonempty
list identifies exactly where an older runtime schema still includes `maxItems`.
Fingerprints reflect object-member ordering as well as values. Compare the normal
request's prompt/schema fingerprints against K/L in the same running process.

The offline integration test now captures the real SDK request bodies for F and H–L
and for an actual MVC call to `/api/project-builder/plan`, using a local provider stub.
It proves K/L and normal serialize identically, and that normal's serialized schema
contains no `maxItems` anywhere. A safe comparison artifact is written to
`target/rhp031-safe-request-comparison.json` during tests. It contains no raw captured
request body or authentication headers. Tests also verify H–L provider errors remain
redacted, count all authoritative business tables before/after, and keep the local-only
listener gates. Normal DTO validation, trade resolution and API error handling are unchanged.
