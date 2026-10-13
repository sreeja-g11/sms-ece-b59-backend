package com.miniclub.dto;

import jakarta.validation.constraints.NotNull;

public record ClubActiveRequest(@NotNull(message = "active is required.") Boolean active) {
}
