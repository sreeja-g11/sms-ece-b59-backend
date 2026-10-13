package com.miniclub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventRequest(
        @NotNull(message = "Pick the club that runs this event.")
        Long clubId,

        @NotBlank(message = "Title is required.")
        @Size(max = 120, message = "Title can be at most 120 characters.")
        String title,

        @Size(max = 2000, message = "Description can be at most 2000 characters.")
        String description,

        @NotBlank(message = "Venue is required.")
        @Size(max = 120, message = "Venue can be at most 120 characters.")
        String venue,

        @NotNull(message = "Start time is required.")
        Instant startsAt,

        @NotNull(message = "End time is required.")
        Instant endsAt,

        @NotNull(message = "Registration deadline is required.")
        Instant registrationDeadline,

        @NotNull(message = "Capacity is required.")
        @Min(value = 1, message = "Capacity must be at least 1.")
        @Max(value = 2000, message = "Capacity can be at most 2000.")
        Integer capacity
) {
}
