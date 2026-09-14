package com.rockandhardplaces.planning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class GeminiProjectPlanningAiClient implements ProjectPlanningAiClient {
    private final String apiKey;
    private final String model;
    private final int timeoutMs;
    private final PlanningResponseValidator validator;
    private final ObjectMapper mapper;
    private final Object schema;
    private final String instructions;

    public GeminiProjectPlanningAiClient(
            @Value("${rhp.planning.gemini.api-key:}") String apiKey,
            @Value("${rhp.planning.gemini.model:gemini-3.6-flash}") String model,
            @Value("${rhp.planning.gemini.timeout-ms:30000}") int timeoutMs,
            PlanningResponseValidator validator, ObjectMapper mapper) throws IOException {
        this.apiKey = apiKey; this.model = model; this.timeoutMs = timeoutMs;
        this.validator = validator; this.mapper = mapper;
        this.schema = mapper.readValue(resource("project-plan.schema.json"), Object.class);
        this.instructions = resource("system-instructions.txt");
    }

    @Override
    public ProjectPlanningAiResponse plan(ProjectPlanningPrompt prompt) {
        if (apiKey == null || apiKey.isBlank() || model.isBlank() || timeoutMs <= 0)
            throw new PlanningException(PlanningException.Reason.NOT_CONFIGURED);
        String json;
        // Lazy creation permits startup without credentials. One attempt, bounded timeout, no tools.
        try (Client client = createClient()) {
            var response = generate(client, prompt, schema, "normal");
            if (response == null || response.candidates().isEmpty()
                    || response.candidates().get().size() != 1
                    || !response.candidates().get().get(0).finishReason()
                            .map(reason -> reason.toString().equals("STOP")).orElse(false))
                throw new PlanningException(PlanningException.Reason.INVALID_RESPONSE);
            json = response.text();
        } catch (PlanningException exception) {
            throw exception;
        } catch (Exception exception) {
            // Normal logs retain operational metadata only, never provider messages or causes.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                    "Gemini operation=generateContent model={} exception={} status={}",
                    model, exception.getClass().getName(),
                    exception instanceof com.google.genai.errors.ApiException api ? api.code() : "unknown");
            // Keep the API envelope free of provider details.
            throw new PlanningException(PlanningException.Reason.UNAVAILABLE);
        }
        return validator.parse(json);
    }

    // Shared by normal planning and every diagnostic; only explicit probe inputs differ.
    private GenerateContentConfig configuration(Object responseSchema) {
        return GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(instructions)))
                .responseMimeType("application/json").responseJsonSchema(responseSchema)
                .candidateCount(1).maxOutputTokens(8192).build();
    }

    private GenerateContentResponse generate(Client client, ProjectPlanningPrompt prompt, Object responseSchema,
            String operationLabel) throws IOException {
        var config = configuration(responseSchema);
        var logger = org.slf4j.LoggerFactory.getLogger(GeminiProjectPlanningAiClient.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Gemini request shape operation=generateContent model={} responseMimeType={} schema={}",
                    model, config.responseMimeType().orElse(""), schemaShape(mapper.valueToTree(responseSchema)));
            logger.debug("Gemini safe request comparison {}", mapper.writeValueAsString(safeRequestShape(operationLabel, prompt, responseSchema)));
        }
        return client.models.generateContent(model, mapper.writeValueAsString(prompt), config);
    }

    SchemaDiagnosticResult diagnose(SchemaDiagnosticStage stage) {
        if (apiKey == null || apiKey.isBlank() || model.isBlank() || timeoutMs <= 0)
            return new SchemaDiagnosticResult(stage.name(), "NOT_CONFIGURED", "Gemini is not configured");
        return diagnose(stage.name(), new ProjectPlanningPrompt(SchemaDiagnosticStage.IDEA, java.util.List.of()),
                stage.schema());
    }

    SchemaDiagnosticResult diagnose(RequestDiagnosticStage stage, java.util.List<String> catalogNames) {
        var prompt = new ProjectPlanningPrompt(SchemaDiagnosticStage.IDEA,
                stage == RequestDiagnosticStage.H ? java.util.List.of() : catalogNames);
        return diagnose(stage.name(), prompt, stage.schema(mapper.valueToTree(schema)));
    }

    private SchemaDiagnosticResult diagnose(String stage, ProjectPlanningPrompt prompt, Object probeSchema) {
        if (apiKey == null || apiKey.isBlank() || model.isBlank() || timeoutMs <= 0)
            return new SchemaDiagnosticResult(stage, "NOT_CONFIGURED", "Gemini is not configured");
        try (Client client = createClient()) {
            var response = generate(client, prompt, probeSchema, stage);
            if (response == null || response.candidates().isEmpty()
                    || response.candidates().get().size() != 1
                    || !response.candidates().get().get(0).finishReason()
                            .map(reason -> reason.toString().equals("STOP")).orElse(false))
                return new SchemaDiagnosticResult(stage, "INVALID_RESPONSE", "No complete provider response");
            String json = response.text();
            if (json == null || json.isBlank() || json.length() > 200_000
                    || !mapper.readTree(json).isObject())
                return new SchemaDiagnosticResult(stage, "INVALID_RESPONSE", "No JSON object returned");
            if (stage.equals("L")) {
                validator.parse(json);
                return new SchemaDiagnosticResult(stage, "PASS", "Normal request response passed Java validation");
            }
            return new SchemaDiagnosticResult(stage, "PASS", "Provider accepted schema and returned a JSON object");
        } catch (PlanningException exception) {
            return new SchemaDiagnosticResult(stage, exception.reason().name(), exception.getMessage());
        } catch (com.google.genai.errors.ApiException exception) {
            return new SchemaDiagnosticResult(stage, Integer.toString(exception.code()),
                    safeDiagnosticMessage(exception.message()));
        } catch (Exception exception) {
            return new SchemaDiagnosticResult(stage, "UNAVAILABLE", "Provider request or response could not be completed");
        }
    }

    java.util.Map<String, Object> safeRequestShape(String label, ProjectPlanningPrompt prompt, Object responseSchema)
            throws IOException {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("label", label);
        result.put("model", model);
        result.put("contentsShape", "one user content, one text part containing JSON {idea,catalogTradeNames}");
        result.put("ideaCharacters", prompt.idea().length());
        result.put("catalogCount", prompt.catalogTradeNames().size());
        result.put("userPromptSha256", fingerprint(mapper.writeValueAsString(prompt)));
        result.put("systemInstructionSha256", fingerprint(instructions));
        result.put("systemInstructionShape", "one text part");
        JsonNodeHolder.addConfig(result, com.google.genai.JsonSerializable.toJsonNode(configuration(responseSchema)));
        var schemaNode = mapper.valueToTree(responseSchema);
        result.put("schemaSha256", fingerprint(mapper.writeValueAsString(responseSchema)));
        var properties = new java.util.ArrayList<String>();
        schemaNode.path("properties").fieldNames().forEachRemaining(properties::add);
        result.put("schemaProperties", properties);
        result.put("schemaRequired", schemaNode.path("required"));
        var maxItemsPaths = new java.util.ArrayList<String>();
        findMaxItems(schemaNode, "", maxItemsPaths);
        result.put("maxItemsPaths", maxItemsPaths);
        result.put("httpMode", "Gemini Developer API; SDK default API version");
        result.put("timeoutMs", timeoutMs);
        result.put("attempts", 1);
        return result;
    }

    private static class JsonNodeHolder {
        static void addConfig(java.util.Map<String, Object> result, com.fasterxml.jackson.databind.JsonNode config) {
            for (String field : java.util.List.of("responseMimeType", "responseSchema", "temperature", "topP", "topK",
                    "maxOutputTokens", "candidateCount", "thinkingConfig", "safetySettings", "tools", "toolConfig",
                    "seed", "stopSequences")) {
                var value = config.get(field);
                result.put(field, value == null || value.isNull() ? "UNSET (SDK/provider default)" : value);
            }
            result.put("schemaField", "responseJsonSchema");
        }
    }

    private void findMaxItems(com.fasterxml.jackson.databind.JsonNode node, String path, java.util.List<String> paths) {
        if (node.isObject()) node.fields().forEachRemaining(entry -> {
            String child = path + "/" + entry.getKey();
            if (entry.getKey().equals("maxItems")) paths.add(child);
            findMaxItems(entry.getValue(), child, paths);
        });
        else if (node.isArray()) for (int i = 0; i < node.size(); i++) findMaxItems(node.get(i), path + "/" + i, paths);
    }

    private String fingerprint(String text) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }

    private String safeDiagnosticMessage(String message) {
        if (message == null) return "Provider rejected request";
        String safe = message.replace(apiKey, "[REDACTED]")
                .replace(SchemaDiagnosticStage.IDEA, "[IDEA REDACTED]")
                .replaceAll("AIza[\\w-]+", "[REDACTED]")
                .replaceAll("(?im)(authorization|x-goog-api-key)\\s*[:=][^\\r\\n]*", "$1: [REDACTED]")
                .replaceAll("[\\r\\n\\t]", " ");
        return safe.length() > 2000 ? safe.substring(0, 2000) : safe;
    }

    // Allowlist schema structure only; never log contents, instructions, headers or client config.
    private com.fasterxml.jackson.databind.JsonNode schemaShape(com.fasterxml.jackson.databind.JsonNode source) {
        var shape = mapper.createObjectNode();
        for (String key : java.util.List.of("type", "required", "enum", "additionalProperties", "maxItems")) {
            if (source.has(key)) shape.set(key, source.get(key));
        }
        if (source.has("properties")) {
            var properties = shape.putObject("properties");
            source.get("properties").fields().forEachRemaining(
                    entry -> properties.set(entry.getKey(), schemaShape(entry.getValue())));
        }
        if (source.has("items")) shape.set("items", schemaShape(source.get("items")));
        return shape;
    }

    Client createClient() {
        return Client.builder().apiKey(apiKey).vertexAI(false)
                .httpOptions(HttpOptions.builder().timeout(timeoutMs)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build();
    }

    private String resource(String name) throws IOException {
        try (var input = new ClassPathResource("planning/" + name).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
