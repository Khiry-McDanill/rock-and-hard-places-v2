package com.rockandhardplaces.planning;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Real SDK serialization and parsing against loopback only; never contacts Google. */
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class GeminiProjectPlanningAiClientTests {
    HttpServer server;
    jakarta.validation.ValidatorFactory factory;
    GeminiProjectPlanningAiClient adapter;
    ObjectMapper mapper = new ObjectMapper();
    AtomicReference<JsonNode> request = new AtomicReference<>();
    String responseText = PlanningResponseValidatorTests.VALID;
    String finish = "STOP";
    int status = 200;

    @BeforeEach void setup() throws Exception {
        factory = Validation.buildDefaultValidatorFactory();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            request.set(mapper.readTree(exchange.getRequestBody()));
            String body = status == 200 ? mapper.writeValueAsString(java.util.Map.of("candidates", List.of(
                    java.util.Map.of("finishReason", finish, "content", java.util.Map.of("parts", List.of(
                            java.util.Map.of("text", responseText)))))))
                    : "{\"error\":{\"code\":503,\"message\":\"sensitive provider details\",\"status\":\"UNAVAILABLE\"}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        adapter = spy(new GeminiProjectPlanningAiClient("test-key", "gemini-3.6-flash", 30000,
                new PlanningResponseValidator(mapper, factory.getValidator()), mapper));
        Client client = Client.builder().apiKey("test-key").vertexAI(false)
                .httpOptions(HttpOptions.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                        .timeout(1000).retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build();
        doReturn(client).when(adapter).createClient();
    }

    @AfterEach void close() { server.stop(0); factory.close(); }

    @Test void sendsSchemaAndInstructionsAndParsesStructuredOutput() {
        var result = plan();
        assertThat(result.summary()).isEqualTo("Tree house");
        JsonNode config = request.get().path("generationConfig");
        assertThat(config.path("responseMimeType").asText()).isEqualTo("application/json");
        assertThat(config.path("responseJsonSchema").path("additionalProperties").asBoolean(true)).isFalse();
        assertThat(config.path("responseJsonSchema").path("required").size()).isEqualTo(6);
        assertThat(request.get().path("systemInstruction").toString()).contains("advisory", "structural member sizing");
        assertThat(request.get().path("contents").toString()).contains("Hockessin", "Carpentry");
        assertThat(request.get().has("tools")).isFalse();
    }

    @Test void serializedSchemaPreservesAllConstraints() throws Exception {
        plan();
        try (var input = new org.springframework.core.io.ClassPathResource(
                "planning/project-plan.schema.json").getInputStream()) {
            assertThat(request.get().path("generationConfig").path("responseJsonSchema"))
                    .isEqualTo(mapper.readTree(input));
        }
        assertThat(request.get().path("generationConfig").has("responseSchema")).isFalse();
    }

    @Test void providerSchemaOmitsMaxItemsAndKeepsAllOtherStructuralConstraints() {
        plan();
        JsonNode providerSchema = request.get().path("generationConfig").path("responseJsonSchema");
        assertThat(providerSchema.findValues("maxItems")).isEmpty();
        // Stage F is the independently defined, live-accepted structure: required fields,
        // closed objects, string arrays, nested object arrays, enums and booleans.
        JsonNode structuralSchema = providerSchema.deepCopy();
        removeSchemaDescriptions(structuralSchema);
        assertThat(structuralSchema).isEqualTo(mapper.valueToTree(SchemaDiagnosticStage.F.schema()));
    }

    private void removeSchemaDescriptions(JsonNode schema) {
        ((com.fasterxml.jackson.databind.node.ObjectNode) schema).remove("description");
        schema.path("properties").forEach(this::removeSchemaDescriptions);
        if (schema.has("items")) removeSchemaDescriptions(schema.get("items"));
    }

    @Test void debugLogsOnlySchemaShape(org.springframework.boot.test.system.CapturedOutput output) {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(GeminiProjectPlanningAiClient.class);
        var previous = logger.getLevel();
        try {
            logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
            plan();
            String shape = output.getOut().lines().filter(line -> line.contains("Gemini request shape"))
                    .findFirst().orElseThrow();
            assertThat(shape).contains("model=gemini-3.6-flash", "responseMimeType=application/json",
                    "summary", "followUpQuestions", "suggestedTrades", "tasks", "assumptions", "warnings",
                    "required", "HIGH", "MEDIUM", "LOW", "needsConfirmation", "additionalProperties")
                    .doesNotContain("test-key", "Hockessin", "Carpentry", "Nonblank text", "systemInstruction",
                            "Authorization", "x-goog-api-key", "catalogTradeNames");
        } finally {
            logger.setLevel(previous);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"", "not JSON", "{}"})
    void invalidProviderPayloadFailsSafely(String text) {
        responseText = text;
        assertThatThrownBy(this::plan).isInstanceOfSatisfying(PlanningException.class,
                e -> assertThat(e.reason()).isEqualTo(PlanningException.Reason.INVALID_RESPONSE));
    }

    @ParameterizedTest @ValueSource(strings = {"MAX_TOKENS", "SAFETY"})
    void rejectsIncompleteOrBlockedResponseEvenWithValidJson(String reason) {
        finish = reason;
        assertThatThrownBy(this::plan).isInstanceOfSatisfying(PlanningException.class,
                e -> assertThat(e.reason()).isEqualTo(PlanningException.Reason.INVALID_RESPONSE));
    }

    @Test void unavailableDoesNotLeakProviderDetails() {
        status = 503;
        assertThatThrownBy(this::plan).isInstanceOfSatisfying(PlanningException.class, e -> {
            assertThat(e.reason()).isEqualTo(PlanningException.Reason.UNAVAILABLE);
            assertThat(e.getMessage()).doesNotContain("sensitive", "test-key");
            assertThat(e.getCause()).isNull();
        });
    }

    @ParameterizedTest @ValueSource(ints = {400, 429})
    void normalProviderLoggingOmitsDetailsAndKeepsApiClean(int providerStatus,
            org.springframework.boot.test.system.CapturedOutput output) {
        doThrow(new com.google.genai.errors.ApiException(providerStatus, "PROVIDER_ERROR",
                "Unsupported schema test-key Hockessin Tree House\nAuthorization: Bearer private-token"))
                .when(adapter).createClient();
        assertThatThrownBy(this::plan).isInstanceOfSatisfying(PlanningException.class, e -> {
            assertThat(e.reason()).isEqualTo(PlanningException.Reason.UNAVAILABLE);
            assertThat(e.getMessage()).doesNotContain("Unsupported", "test-key", "private-token");
            assertThat(e.getCause()).isNull();
        });
        assertThat(output.getOut()).contains("operation=generateContent", "model=gemini-3.6-flash",
                "exception=com.google.genai.errors.ApiException", "status=" + providerStatus)
                .doesNotContain("Unsupported schema", "test-key", "private-token", "Hockessin Tree House",
                        "Authorization", "PROVIDER_ERROR");
    }

    private ProjectPlanningAiResponse plan() {
        return adapter.plan(new ProjectPlanningPrompt("Hockessin Tree House", List.of("Carpentry")));
    }
}
