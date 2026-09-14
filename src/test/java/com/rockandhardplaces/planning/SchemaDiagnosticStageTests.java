package com.rockandhardplaces.planning;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SchemaDiagnosticStageTests {
    ObjectMapper mapper = new ObjectMapper();

    @Test void stagesAddExactlyTheRequestedFieldsAndConstraints() {
        String[][] fields = {{"summary"}, {"summary", "followUpQuestions"},
                {"summary", "followUpQuestions", "suggestedTrades"},
                {"summary", "followUpQuestions", "suggestedTrades", "tasks"},
                {"summary", "followUpQuestions", "suggestedTrades", "tasks", "assumptions", "warnings"}};
        for (var stage : SchemaDiagnosticStage.values()) {
            JsonNode schema = mapper.valueToTree(stage.schema());
            assertThat(schema.path("required")).isEqualTo(mapper.valueToTree(fields[Math.min(stage.ordinal(), 4)]));
            assertThat(schema.path("properties").size()).isEqualTo(fields[Math.min(stage.ordinal(), 4)].length);
            inspect(schema, stage);
        }
        JsonNode c = mapper.valueToTree(SchemaDiagnosticStage.C.schema());
        assertThat(c.at("/properties/suggestedTrades/items/required"))
                .isEqualTo(mapper.valueToTree(new String[]{"trade", "reason", "confidence", "needsConfirmation"}));
        assertThat(c.at("/properties/suggestedTrades/items/properties/confidence/enum"))
                .isEqualTo(mapper.valueToTree(new String[]{"HIGH", "MEDIUM", "LOW"}));
        assertThat(c.at("/properties/suggestedTrades/items/properties/needsConfirmation/type").asText()).isEqualTo("boolean");
        JsonNode g = mapper.valueToTree(SchemaDiagnosticStage.G.schema());
        assertThat(g.at("/properties/tasks/maxItems").asInt()).isEqualTo(50);
        assertThat(g.at("/properties/suggestedTrades/maxItems").asInt()).isEqualTo(30);
        for (String field : new String[]{"followUpQuestions", "assumptions", "warnings"})
            assertThat(g.at("/properties/" + field + "/maxItems").asInt()).isEqualTo(20);
    }

    private void inspect(JsonNode node, SchemaDiagnosticStage stage) {
        if (node.path("type").asText().equals("object")) {
            assertThat(node.has("additionalProperties")).isEqualTo(stage.ordinal() >= 5);
            if (node.has("additionalProperties")) assertThat(node.get("additionalProperties").asBoolean()).isFalse();
            node.path("properties").forEach(child -> inspect(child, stage));
        }
        if (node.path("type").asText().equals("array")) {
            assertThat(node.has("maxItems")).isEqualTo(stage == SchemaDiagnosticStage.G);
            inspect(node.path("items"), stage);
        }
    }

    @Test void listenerRequiresBothExplicitProfileAndEnableFlag() {
        var runner = new ApplicationContextRunner().withUserConfiguration(LocalGeminiSchemaDiagnostics.class);
        runner.run(context -> assertThat(context).doesNotHaveBean(LocalGeminiSchemaDiagnostics.class));
        runner.withPropertyValues("rhp.planning.diagnostics.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(LocalGeminiSchemaDiagnostics.class));
        runner.withPropertyValues("spring.profiles.active=local-gemini-diagnostics")
                .run(context -> assertThat(context).doesNotHaveBean(LocalGeminiSchemaDiagnostics.class));
    }
}
