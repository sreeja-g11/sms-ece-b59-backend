package com.miniclub.dto;

import java.util.List;

public record MyActivity(List<ClubCard> clubs, List<EventView> upcomingEvents, List<EventView> pastEvents) {
}
