package com.rockandhardplaces.planning;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"rhp.planning.diagnostics.enabled=true", "rhp.planning.diagnostics.port=0",
        "rhp.planning.gemini.api-key=diagnostic-test-key"})
@ActiveProfiles("local-gemini-diagnostics")
@AutoConfigureMockMvc
class LocalGeminiSchemaDiagnosticsTests {
    @Autowired LocalGeminiSchemaDiagnostics diagnostics;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.rockandhardplaces.catalog.TradeRepository trades;
    Long fixtureTradeId;
    @Autowired MockMvc mvc;
    @MockitoSpyBean GeminiProjectPlanningAiClient client;
    HttpServer provider;
    AtomicReference<JsonNode> captured = new AtomicReference<>();
    int providerStatus = 200;
    AtomicReference<String> capturedPath = new AtomicReference<>();

    @BeforeEach void setup() throws Exception {
        fixtureTradeId = trades.saveAndFlush(new com.rockandhardplaces.catalog.Trade(
                "Diagnostic comparison trade " + UUID.randomUUID())).getId();
        provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/", exchange -> {
            captured.set(mapper.readTree(exchange.getRequestBody()));
            capturedPath.set(exchange.getRequestURI().getPath());
            String body = providerStatus == 200 ? mapper.writeValueAsString(Map.of("candidates", List.of(
                    Map.of("finishReason", "STOP", "content", Map.of("parts", List.of(
                            Map.of("text", PlanningResponseValidatorTests.VALID)))))))
                    : mapper.writeValueAsString(Map.of("error", Map.of("code", 400, "status", "INVALID_ARGUMENT",
                            "message", "Invalid argument diagnostic-test-key\nAuthorization: Bearer hidden")));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(providerStatus, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        provider.start();
        doAnswer(invocation -> Client.builder().apiKey("diagnostic-test-key").vertexAI(false)
                .httpOptions(HttpOptions.builder().baseUrl("http://127.0.0.1:" + provider.getAddress().getPort())
                        .timeout(2000).retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build())
                .when(client).createClient();
    }

    @AfterEach void close() {
        if (provider != null) provider.stop(0);
        if (fixtureTradeId != null) trades.deleteById(fixtureTradeId);
    }

    @Test void everyStageUsesRealAdapterAndCreatesNoBusinessRecords() throws Exception {
        var before = counts();
        JsonNode previousPrompt = null;
        for (var stage : SchemaDiagnosticStage.values()) {
            var response = send(stage.name(), null, "POST");
            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode result = mapper.readTree(response.body());
            assertThat(result.size()).isEqualTo(3);
            assertThat(result.path("stage").asText()).isEqualTo(stage.name());
            assertThat(result.path("status").asText()).isEqualTo("PASS");
            assertThat(response.body()).doesNotContain("diagnostic-test-key", "Tree house", "candidates");
            var config = captured.get().path("generationConfig");
            assertThat(config.path("responseMimeType").asText()).isEqualTo("application/json");
            assertThat(config.path("responseJsonSchema")).isEqualTo(mapper.valueToTree(stage.schema()));
            assertThat(config.path("maxOutputTokens").asInt()).isEqualTo(8192);
            assertThat(config.path("candidateCount").asInt()).isEqualTo(1);
            JsonNode prompt = captured.get().path("contents");
            assertThat(prompt.toString()).contains("Hockessin", "weather protection");
            if (previousPrompt != null) assertThat(prompt).isEqualTo(previousPrompt);
            previousPrompt = prompt;
            assertThat(counts()).isEqualTo(before);
        }
    }

    @Test void comparesActualSdkRequestsFromFThroughNormalEndpointWithoutPersistence() throws Exception {
        var before = counts();
        var requests = new LinkedHashMap<String, JsonNode>();
        for (String stage : List.of("F", "H", "I", "J", "K", "L")) {
            var response = send(stage, null, "POST");
            assertThat(mapper.readTree(response.body()).path("status").asText()).isEqualTo("PASS");
            requests.put(stage, captured.get().deepCopy());
            assertThat(counts()).isEqualTo(before);
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/project-builder/plan").contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("idea", SchemaDiagnosticStage.IDEA))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        requests.put("normal", captured.get().deepCopy());
        assertThat(capturedPath.get()).isEqualTo("/v1beta/models/gemini-3.6-flash:generateContent");
        assertThat(counts()).isEqualTo(before);
        // H changes only schema key ordering; no semantic JSON differences from F.
        assertThat(requests.get("H")).isEqualTo(requests.get("F"));
        assertThat(requests.get("H").toString()).isNotEqualTo(requests.get("F").toString());
        var normal = requests.get("normal");
        assertThat(requests.get("K").toString()).isEqualTo(normal.toString());
        assertThat(requests.get("L").toString()).isEqualTo(normal.toString());
        assertThat(normal.path("generationConfig").path("responseJsonSchema").findValues("maxItems")).isEmpty();
        assertThat(requests.get("H").path("contents")).isEqualTo(requests.get("F").path("contents"));
        assertThat(requests.get("I").path("contents")).isEqualTo(normal.path("contents"));
        assertThat(requests.get("I").path("contents")).isNotEqualTo(requests.get("H").path("contents"));
        for (String field : List.of("responseSchema", "temperature", "topP", "topK", "thinkingConfig",
                "seed", "stopSequences"))
            assertThat(normal.path("generationConfig").has(field)).isFalse();
        for (String field : List.of("safetySettings", "tools", "toolConfig"))
            assertThat(normal.has(field)).isFalse();
        var prompt = mapper.readTree(normal.path("contents").get(0).path("parts").get(0).path("text").asText());
        assertThat(prompt.path("catalogTradeNames").size()).isEqualTo(jdbc.queryForObject("SELECT COUNT(*) FROM trades", Integer.class));
        assertThat(prompt.path("catalogTradeNames").size()).isPositive();
        for (String stage : List.of("F", "H", "I", "J", "K", "L")) {
            var comparable = (com.fasterxml.jackson.databind.node.ObjectNode) requests.get(stage).deepCopy();
            comparable.set("contents", normal.get("contents"));
            ((com.fasterxml.jackson.databind.node.ObjectNode) comparable.get("generationConfig"))
                    .set("responseJsonSchema", normal.path("generationConfig").get("responseJsonSchema"));
            assertThat(comparable).isEqualTo(normal); // No hidden non-schema settings differ.
        }
        assertThat(requests.get("I").path("generationConfig")).isEqualTo(requests.get("H").path("generationConfig"));
        assertThat(requests.get("J").at("/generationConfig/responseJsonSchema/properties/summary/description").isTextual()).isTrue();
        assertThat(requests.get("J").at("/generationConfig/responseJsonSchema/properties/tasks/items/properties/title/description").isMissingNode()).isTrue();
        assertThat(requests.get("K").at("/generationConfig/responseJsonSchema/properties/tasks/items/properties/title/description").isTextual()).isTrue();
        // Save a safe representation derived from the captured wire bodies; no headers/raw text.
        var reports = new LinkedHashMap<String, Object>();
        for (var entry : requests.entrySet()) {
            var wire = entry.getValue();
            var wirePrompt = mapper.readValue(wire.path("contents").get(0).path("parts").get(0).path("text").asText(), ProjectPlanningPrompt.class);
            reports.put(entry.getKey(), client.safeRequestShape(entry.getKey(), wirePrompt,
                    wire.path("generationConfig").get("responseJsonSchema")));
        }
        String safe = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reports);
        assertThat(safe).doesNotContain("diagnostic-test-key", "Hockessin", "Carpentry", "Authorization", "apiKey");
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/rhp031-safe-request-comparison.json"), safe);
    }

    @Test void requestBisectionReportsProvider400AndDoesNotPersist() throws Exception {
        var before = counts(); providerStatus = 400;
        for (String stage : List.of("H", "I", "J", "K", "L")) {
            var response = send(stage, null, "POST");
            assertThat(mapper.readTree(response.body()).path("status").asText()).isEqualTo("400");
            assertThat(response.body()).doesNotContain("diagnostic-test-key", "hidden");
        }
        assertThat(counts()).isEqualTo(before);
    }

    @Test void providerFailureReturnsOnlySafeStatusAndCannotPersist() throws Exception {
        var before = counts(); providerStatus = 400;
        var response = send("C", null, "POST");
        var result = mapper.readTree(response.body());
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.path("status").asText()).isEqualTo("400");
        assertThat(result.path("message").asText()).contains("Invalid argument", "[REDACTED]");
        assertThat(response.body()).doesNotContain("diagnostic-test-key", "hidden");
        assertThat(counts()).isEqualTo(before);
    }

    @Test void rejectsBrowserInvalidStageAndGetWithoutCallingProvider() throws Exception {
        assertThat(send("A", "https://example.com", "POST").statusCode()).isEqualTo(403);
        assertThat(send("Z", null, "POST").statusCode()).isEqualTo(400);
        assertThat(send("A?idea=override", null, "POST").statusCode()).isEqualTo(400);
        assertThat(send("A", null, "GET").statusCode()).isEqualTo(405);
        assertThat(captured.get()).isNull();
        verify(client, never()).createClient();
    }

    @Test void diagnosticPathIsAbsentFromNormalApplicationEndpoint() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/diagnostics/gemini/schema/A"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        assertThat(captured.get()).isNull();
    }

    private HttpResponse<String> send(String stage, String origin, String method) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + diagnostics.port()
                + "/diagnostics/gemini/schema/" + stage)).method(method, HttpRequest.BodyPublishers.noBody());
        if (origin != null) request.header("Origin", origin);
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, Long> counts() {
        var result = new LinkedHashMap<String, Long>();
        for (String table : List.of("projects", "tasks", "task_trades", "trades", "bids",
                "task_assignments", "project_teams", "project_team_trades"))
            result.put(table, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class));
        return result;
    }
}
