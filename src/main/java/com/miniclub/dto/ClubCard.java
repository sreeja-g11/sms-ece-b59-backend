package com.miniclub.dto;

import com.miniclub.model.ClubCategory;

/** What a club looks like in lists. "joined" is about the logged-in student. */
public record ClubCard(Long id, String name, ClubCategory category, String tagline, String clubHead,
                       String meetingInfo, long memberCount, int maxMembers, boolean full, boolean joined,
                       boolean active, long upcomingEvents) {
}
