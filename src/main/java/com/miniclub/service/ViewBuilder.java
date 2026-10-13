package com.miniclub.service;

import com.miniclub.dto.ClubCard;
import com.miniclub.dto.EventView;
import com.miniclub.model.Club;
import com.miniclub.model.Event;
import com.miniclub.model.User;
import com.miniclub.repository.ClubMemberRepository;
import com.miniclub.repository.EventRegistrationRepository;
import com.miniclub.repository.EventRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Turns entities into API views, loading member / registration counts in a few grouped queries. */
@Component
public class ViewBuilder {

    private final ClubMemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;

    public ViewBuilder(ClubMemberRepository memberRepository, EventRepository eventRepository,
                       EventRegistrationRepository registrationRepository) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    // ---- clubs ---------------------------------------------------------------------------

    public List<ClubCard> clubCards(List<Club> clubs, User viewer) {
        Map<Long, Long> members = toMap(memberRepository.countPerClub());
        Map<Long, Long> upcoming = toMap(eventRepository.countUpcomingPerClub(Instant.now()));
        Set<Long> joined = joinedClubIds(viewer);
        return clubs.stream().map(c -> card(c, members.getOrDefault(c.getId(), 0L),
                upcoming.getOrDefault(c.getId(), 0L), joined.contains(c.getId()))).toList();
    }

    public ClubCard clubCard(Club club, User viewer) {
        return clubCards(List.of(club), viewer).get(0);
    }

    public Set<Long> joinedClubIds(User viewer) {
        return viewer.isAdmin() ? Set.of() : new HashSet<>(memberRepository.clubIdsOf(viewer.getId()));
    }

    private static ClubCard card(Club c, long memberCount, long upcomingEvents, boolean joined) {
        return new ClubCard(c.getId(), c.getName(), c.getCategory(), c.getTagline(), c.getClubHead(),
                c.getMeetingInfo(), memberCount, c.getMaxMembers(), memberCount >= c.getMaxMembers(), joined,
                c.isActive(), upcomingEvents);
    }

    // ---- events --------------------------------------------------------------------------

    public List<EventView> events(List<Event> events, User viewer) {
        if (events.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = toMap(registrationRepository.countFor(events.stream().map(Event::getId).toList()));
        Set<Long> mine = viewer.isAdmin() ? Set.of() : new HashSet<>(registrationRepository.eventIdsOf(viewer.getId()));
        Instant now = Instant.now();
        return events.stream()
                .map(e -> view(e, counts.getOrDefault(e.getId(), 0L), mine.contains(e.getId()), now))
                .toList();
    }

    public EventView event(Event event, User viewer) {
        return events(List.of(event), viewer).get(0);
    }

    private static EventView view(Event e, long registered, boolean mine, Instant now) {
        Club c = e.getClub();
        return new EventView(e.getId(), c.getId(), c.getName(), c.getCategory(), e.getTitle(), e.getDescription(),
                e.getVenue(), e.getStartsAt(), e.getEndsAt(), e.getRegistrationDeadline(), e.getCapacity(),
                registered, Math.max(0, e.getCapacity() - registered), e.isCancelled(), mine,
                status(e, registered, now));
    }

    /** OPEN, FULL, CLOSED (deadline passed), LIVE (happening now), ENDED or CANCELLED. */
    public static String status(Event e, long registered, Instant now) {
        if (e.isCancelled()) {
            return "CANCELLED";
        }
        if (!now.isBefore(e.getEndsAt())) {
            return "ENDED";
        }
        if (!now.isBefore(e.getStartsAt())) {
            return "LIVE";
        }
        if (registered >= e.getCapacity()) {
            return "FULL";
        }
        if (now.isAfter(e.getRegistrationDeadline())) {
            return "CLOSED";
        }
        return "OPEN";
    }

    // ---- helpers -------------------------------------------------------------------------

    static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
