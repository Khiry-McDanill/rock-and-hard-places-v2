package com.rockandhardplaces.planning;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlanningResponseValidatorTests {
    static final String VALID = """
            {"summary":"Tree house","followUpQuestions":["Freestanding?"],
             "suggestedTrades":[{"trade":"Carpentry","reason":"Frame review","confidence":"HIGH","needsConfirmation":true}],
             "tasks":[{"title":"Review structure","description":"Ask a professional","trade":"Carpentry",
                       "reason":"Adult use","confidence":"MEDIUM","needsConfirmation":false}],
             "assumptions":["Adult use"],"warnings":["Professional review required"]}
            """;
    private jakarta.validation.ValidatorFactory factory;
    private PlanningResponseValidator validator;

    @BeforeEach void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new PlanningResponseValidator(new ObjectMapper(), factory.getValidator());
    }
    @AfterEach void close() { factory.close(); }

    @Test void mapsTypedValues() {
        var result = validator.parse(VALID);
        assertThat(result.tasks().get(0).confidence()).isEqualTo(ProjectPlanningAiResponse.Confidence.MEDIUM);
        assertThat(result.suggestedTrades().get(0).needsConfirmation()).isTrue();
        assertThat(result.tasks().get(0).needsConfirmation()).isFalse();
        assertThat(result.assumptions()).containsExactly("Adult use");
        assertThat(result.warnings()).containsExactly("Professional review required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "null", "{}", "[]", "not JSON", "```json\n{}\n```"})
    void rejectsEmptyMalformedAndWrongShape(String json) { rejects(json); }

    @Test void rejectsSchemaViolationsAndCoercion() {
        rejects(VALID.replace("HIGH", "CERTAIN"));
        rejects(VALID.replace("\"HIGH\"", "0"));
        rejects(VALID.replace("true", "\"true\""));
        rejects(VALID.replace("true", "null"));
        rejects(VALID.replace("\"needsConfirmation\":true", "\"extra\":true"));
        rejects(VALID.replace("\"Tree house\"", "5"));
        rejects(VALID.replace("\"Tree house\"", "\" \""));
        rejects(VALID + " {}");
        rejects(VALID.replace("\"summary\":", "\"summary\":\"duplicate\",\"summary\":"));
        rejects(VALID.replace("\"Tree house\"", "true"));
        rejects(VALID.replace("[\"Adult use\"]", "[null]"));
        rejects(null);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "followUpQuestions,20", "suggestedTrades,30", "tasks,50", "assumptions,20", "warnings,20"
    })
    void preservesExistingArrayLimitsAfterParsing(String field, int limit) throws Exception {
        var mapper = new ObjectMapper();
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(VALID);
        var sample = json.get(field).get(0).deepCopy();
        var items = json.putArray(field);
        for (int i = 0; i < limit; i++) items.add(sample.deepCopy());
        assertThatCode(() -> validator.parse(json.toString())).doesNotThrowAnyException();

        items.add(sample.deepCopy());
        rejects(json.toString());
        var typed = mapper.treeToValue(json, ProjectPlanningAiResponse.class);
        assertThat(factory.getValidator().validate(typed))
                .singleElement().satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo(field);
                    assertThat(violation.getConstraintDescriptor().getAnnotation())
                            .isInstanceOf(jakarta.validation.constraints.Size.class);
                });
    }

    @Test void absentKeyFailsWithoutConnecting() throws Exception {
        var client = new GeminiProjectPlanningAiClient("", "gemini-3.6-flash", 30000,
                validator, new ObjectMapper());
        assertThatThrownBy(() -> client.plan(new ProjectPlanningPrompt("Tree house", java.util.List.of())))
                .isInstanceOfSatisfying(PlanningException.class,
                        e -> assertThat(e.reason()).isEqualTo(PlanningException.Reason.NOT_CONFIGURED));
    }

    private void rejects(String json) {
        assertThatThrownBy(() -> validator.parse(json)).isInstanceOfSatisfying(PlanningException.class,
                e -> assertThat(e.reason()).isEqualTo(PlanningException.Reason.INVALID_RESPONSE));
    }
}
