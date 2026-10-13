package com.miniclub.dto;

import com.miniclub.model.ClubCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubRequest(
        @NotBlank(message = "Club name is required.")
        @Size(max = 80, message = "Club name can be at most 80 characters.")
        String name,

        @NotNull(message = "Pick a category.")
        ClubCategory category,

        @NotBlank(message = "Tagline is required.")
        @Size(max = 160, message = "Tagline can be at most 160 characters.")
        String tagline,

        @NotBlank(message = "Description is required.")
        @Size(max = 2000, message = "Description can be at most 2000 characters.")
        String description,

        @Size(max = 100, message = "Club head can be at most 100 characters.")
        String clubHead,

        @Size(max = 100, message = "Faculty advisor can be at most 100 characters.")
        String facultyAdvisor,

        @Size(max = 120, message = "Meeting info can be at most 120 characters.")
        String meetingInfo,

        @Email(message = "Enter a valid contact email.")
        @Size(max = 150, message = "Contact email can be at most 150 characters.")
        String contactEmail,

        @NotNull(message = "Maximum members is required.")
        @Min(value = 1, message = "Maximum members must be at least 1.")
        @Max(value = 500, message = "Maximum members can be at most 500.")
        Integer maxMembers
) {
}
