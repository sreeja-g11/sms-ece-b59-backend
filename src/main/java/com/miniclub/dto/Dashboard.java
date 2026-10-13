package com.miniclub.dto;

import com.miniclub.model.Role;
import java.util.List;

/**
 * Students: clubs = their clubs, upcomingEvents = events they registered for, suggestedEvents = open events.
 * Admin: clubs = most popular clubs, upcomingEvents = next events, suggestedEvents is empty.
 */
public record Dashboard(Role role, List<StatItem> stats, List<ClubCard> clubs, List<EventView> upcomingEvents,
                        List<EventView> suggestedEvents, List<AnnouncementView> announcements) {
}
