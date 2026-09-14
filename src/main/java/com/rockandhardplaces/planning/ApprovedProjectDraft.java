package com.rockandhardplaces.planning;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Only homeowner-approved domain inputs cross the creation boundary. */
public record ApprovedProjectDraft(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 16000) String description,
        @NotBlank @Size(max = 20) String jobZip,
        @NotNull @Size(max = 50) List<@NotNull @Valid ApprovedTask> tasks) {
    @JsonAnySetter public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unsupported project field");
    }
    public record ApprovedTask(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotNull @Size(min = 1, max = 30) List<@NotNull @Positive Long> requiredTradeIds) {
        @JsonAnySetter public void rejectUnknown(String name, Object value) {
            throw new IllegalArgumentException("Unsupported task field");
        }
    }
}
