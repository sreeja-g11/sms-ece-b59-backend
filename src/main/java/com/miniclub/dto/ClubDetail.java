package com.miniclub.dto;

import com.miniclub.model.ClubCategory;
import java.time.Instant;
import java.util.List;

public record ClubDetail(Long id, String name, ClubCategory category, String tagline, String description,
                         String clubHead, String facultyAdvisor, String meetingInfo, String contactEmail,
                         long memberCount, int maxMembers, boolean full, boolean joined, boolean active,
                         Instant createdAt, List<EventView> upcomingEvents, List<AnnouncementView> announcements) {
}
