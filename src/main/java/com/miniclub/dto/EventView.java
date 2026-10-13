package com.miniclub.dto;

import com.miniclub.model.ClubCategory;
import java.time.Instant;

/**
 * status is one of OPEN, FULL, CLOSED (deadline passed), LIVE (happening now), ENDED, CANCELLED.
 * "registered" is about the logged-in student.
 */
public record EventView(Long id, Long clubId, String clubName, ClubCategory clubCategory, String title,
                        String description, String venue, Instant startsAt, Instant endsAt,
                        Instant registrationDeadline, int capacity, long registeredCount, long seatsLeft,
                        boolean cancelled, boolean registered, String status) {
}
