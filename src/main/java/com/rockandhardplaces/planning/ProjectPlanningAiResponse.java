package com.rockandhardplaces.planning;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ProjectPlanningAiResponse(
        @NotBlank @Size(max = 4000) String summary,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 2000) String> followUpQuestions,
        @NotNull @Size(max = 30) List<@NotNull @Valid SuggestedTrade> suggestedTrades,
        @NotNull @Size(max = 50) List<@NotNull @Valid SuggestedTask> tasks,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 2000) String> assumptions,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 2000) String> warnings) {
    public enum Confidence { HIGH, MEDIUM, LOW }

    public record SuggestedTrade(
            @NotBlank @Size(max = 200) String trade,
            @NotBlank @Size(max = 2000) String reason,
            @NotNull Confidence confidence,
            @NotNull Boolean needsConfirmation) {}

    public record SuggestedTask(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotBlank @Size(max = 200) String trade,
            @NotBlank @Size(max = 2000) String reason,
            @NotNull Confidence confidence,
            @NotNull Boolean needsConfirmation) {}
}
