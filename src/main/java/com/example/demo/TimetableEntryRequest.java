package com.example.demo;

import jakarta.validation.constraints.NotNull;

public record TimetableEntryRequest(
        @NotNull Long classGroupId,
        @NotNull Long subjectId,
        @NotNull Long roomId,
        @NotNull Long timeSlotId) {
}
