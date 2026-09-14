package com.rockandhardplaces.planning;

import java.util.*;

/** Fixed development probes. No model-generated value can select or alter a schema. */
enum SchemaDiagnosticStage {
    A, B, C, D, E, F, G;

    static final String IDEA = "I want to build a tree house in Hockessin. I want it to be more than a kids' "
            + "playhouse — something adults can use too, with a small deck, lighting, maybe power, and weather protection.";

    Map<String, Object> schema() {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("summary", text());
        if (ordinal() >= B.ordinal()) fields.put("followUpQuestions", array(text(), 20));
        if (ordinal() >= C.ordinal()) fields.put("suggestedTrades", array(object(tradeFields()), 30));
        if (ordinal() >= D.ordinal()) {
            var task = new LinkedHashMap<String, Object>();
            task.put("title", text()); task.put("description", text()); task.putAll(tradeFields());
            fields.put("tasks", array(object(task), 50));
        }
        if (ordinal() >= E.ordinal()) {
            fields.put("assumptions", array(text(), 20));
            fields.put("warnings", array(text(), 20));
        }
        return object(fields);
    }

    private Map<String, Object> tradeFields() {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("trade", text()); fields.put("reason", text());
        fields.put("confidence", Map.of("type", "string", "enum", List.of("HIGH", "MEDIUM", "LOW")));
        fields.put("needsConfirmation", Map.of("type", "boolean"));
        return fields;
    }

    private Map<String, Object> object(Map<String, Object> fields) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object"); schema.put("required", List.copyOf(fields.keySet()));
        schema.put("properties", fields);
        if (ordinal() >= F.ordinal()) schema.put("additionalProperties", false);
        return schema;
    }

    private Map<String, Object> array(Map<String, Object> items, int max) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "array"); schema.put("items", items);
        if (this == G) schema.put("maxItems", max);
        return schema;
    }

    private Map<String, Object> text() { return Map.of("type", "string"); }
}
