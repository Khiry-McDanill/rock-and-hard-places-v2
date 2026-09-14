package com.rockandhardplaces.planning;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class PlanningResponseValidator {
    private final ObjectMapper mapper;
    private final Validator validator;

    public PlanningResponseValidator(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        this.mapper.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Textual)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
        this.mapper.enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.validator = validator;
    }

    public ProjectPlanningAiResponse parse(String json) {
        if (json == null || json.isBlank() || json.length() > 200_000) throw invalid();
        try {
            return validate(mapper.readValue(json, ProjectPlanningAiResponse.class));
        } catch (JsonProcessingException exception) {
            throw invalid();
        }
    }

    public ProjectPlanningAiResponse validate(ProjectPlanningAiResponse response) {
        if (response == null || !validator.validate(response).isEmpty()) throw invalid();
        return response;
    }

    private PlanningException invalid() {
        return new PlanningException(PlanningException.Reason.INVALID_RESPONSE);
    }
}
