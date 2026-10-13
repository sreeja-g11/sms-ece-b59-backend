package com.miniclub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** clubId = null posts the announcement to everyone. */
public record AnnouncementRequest(
        Long clubId,

        @NotBlank(message = "Title is required.")
        @Size(max = 140, message = "Title can be at most 140 characters.")
        String title,

        @NotBlank(message = "Message is required.")
        @Size(max = 3000, message = "Message can be at most 3000 characters.")
        String body,

        boolean pinned
) {
}
