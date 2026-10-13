package com.miniclub.service;

import com.miniclub.dto.AttendeeView;
import com.miniclub.dto.EventRequest;
import com.miniclub.dto.EventView;
import com.miniclub.exception.ApiException;
import com.miniclub.model.Club;
import com.miniclub.model.Event;
import com.miniclub.model.EventRegistration;
import com.miniclub.model.User;
import com.miniclub.repository.ClubRepository;
import com.miniclub.repository.EventRegistrationRepository;
import com.miniclub.repository.EventRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final ClubRepository clubRepository;
    private final ViewBuilder views;

    public EventService(EventRepository eventRepository, EventRegistrationRepository registrationRepository,
                        ClubRepository clubRepository, ViewBuilder views) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.clubRepository = clubRepository;
        this.views = views;
    }

    /**
     * when = "upcoming" (not finished yet, soonest first) or "past" (finished, latest first).
     * mine = only events the student registered for.
     */
    @Transactional(readOnly = true)
    public List<EventView> list(User viewer, Long clubId, String when, String search, boolean mine) {
        Instant now = Instant.now();
        List<Event> events = switch (when == null ? "upcoming" : when.trim().toLowerCase()) {
            case "upcoming" -> eventRepository.findUpcoming(now, clubId);
            case "past" -> eventRepository.findPast(now, clubId);
            default -> throw ApiException.badRequest("INVALID_PARAMETER", "'when' must be 'upcoming' or 'past'.");
        };
        String q = Text.query(search);
        Set<Long> registered = mine && !viewer.isAdmin()
                ? new HashSet<>(registrationRepository.eventIdsOf(viewer.getId())) : null;
        List<Event> visible = events.stream()
                .filter(e -> viewer.isAdmin() || e.getClub().isActive())
                .filter(e -> registered == null || registered.contains(e.getId()))
                .filter(e -> Text.matches(q, e.getTitle(), e.getVenue(), e.getDescription(), e.getClub().getName()))
                .toList();
        return views.events(visible, viewer);
    }

    @Transactional(readOnly = true)
    public EventView get(User viewer, Long id) {
        Event event = eventRepository.findWithClub(id).orElseThrow(() -> ApiException.notFound("Event"));
        if (!viewer.isAdmin() && !event.getClub().isActive()) {
            throw ApiException.notFound("Event");
        }
        return views.event(event, viewer);
    }

    @Transactional
    public EventView create(User admin, EventRequest req) {
        Club club = activeClub(req.clubId());
        Instant now = Instant.now();
        checkTimes(req);
        if (!req.startsAt().isAfter(now)) {
            throw ApiException.badRequest("EVENT_IN_PAST", "The start time must be in the future.");
        }
        if (!req.registrationDeadline().isAfter(now)) {
            throw ApiException.badRequest("INVALID_DEADLINE", "The registration deadline must be in the future.");
        }
        Event event = new Event();
        event.setClub(club);
        apply(event, req);
        eventRepository.save(event);
        return views.event(event, admin);
    }

    @Transactional
    public EventView update(User admin, Long id, EventRequest req) {
        // Lock first so registrations can't change while we check the new capacity.
        Event event = eventRepository.lockById(id).orElseThrow(() -> ApiException.notFound("Event"));
        Instant now = Instant.now();
        if (event.isCancelled()) {
            throw ApiException.badRequest("EVENT_CANCELLED", "A cancelled event can't be edited.");
        }
        if (!now.isBefore(event.getEndsAt())) {
            throw ApiException.badRequest("EVENT_ENDED", "This event has already ended.");
        }
        checkTimes(req);
        if (!req.startsAt().equals(event.getStartsAt()) && !req.startsAt().isAfter(now)) {
            throw ApiException.badRequest("EVENT_IN_PAST", "The new start time must be in the future.");
        }
        long registered = registrationRepository.countByEventId(id);
        if (req.capacity() < registered) {
            throw ApiException.badRequest("CAPACITY_TOO_LOW",
                    registered + " students have already registered, so capacity can't be lower than that.");
        }
        if (!req.clubId().equals(event.getClub().getId())) {
            event.setClub(activeClub(req.clubId()));
        }
        apply(event, req);
        return views.event(event, admin);
    }

    @Transactional
    public EventView cancel(User admin, Long id) {
        Event event = eventRepository.lockById(id).orElseThrow(() -> ApiException.notFound("Event"));
        if (event.isCancelled()) {
            throw ApiException.conflict("ALREADY_CANCELLED", "This event is already cancelled.");
        }
        if (!Instant.now().isBefore(event.getEndsAt())) {
            throw ApiException.badRequest("EVENT_ENDED", "This event has already ended.");
        }
        event.setCancelled(true);
        return views.event(event, admin);
    }

    @Transactional
    public EventView register(User student, Long id) {
        // Lock the event row: two students can't both take the last seat.
        Event event = eventRepository.lockById(id).orElseThrow(() -> ApiException.notFound("Event"));
        if (!event.getClub().isActive()) {
            throw ApiException.notFound("Event");
        }
        Instant now = Instant.now();
        if (event.isCancelled()) {
            throw ApiException.badRequest("EVENT_CANCELLED", "This event has been cancelled.");
        }
        if (!now.isBefore(event.getStartsAt())) {
            throw ApiException.badRequest("EVENT_STARTED", "This event has already started.");
        }
        if (now.isAfter(event.getRegistrationDeadline())) {
            throw ApiException.badRequest("REGISTRATION_CLOSED", "Registration for this event has closed.");
        }
        if (registrationRepository.existsByEventIdAndUserId(id, student.getId())) {
            throw ApiException.conflict("ALREADY_REGISTERED", "You have already registered for this event.");
        }
        if (registrationRepository.countByEventId(id) >= event.getCapacity()) {
            throw ApiException.conflict("EVENT_FULL", "Sorry, all seats for this event are taken.");
        }
        registrationRepository.save(new EventRegistration(event, student));
        return views.event(event, student);
    }

    @Transactional
    public EventView unregister(User student, Long id) {
        Event event = eventRepository.findWithClub(id).orElseThrow(() -> ApiException.notFound("Event"));
        EventRegistration registration = registrationRepository.findByEventIdAndUserId(id, student.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_REGISTERED",
                        "You are not registered for this event."));
        if (!Instant.now().isBefore(event.getStartsAt())) {
            throw ApiException.badRequest("EVENT_STARTED", "This event has already started, so you can't cancel now.");
        }
        registrationRepository.delete(registration);
        registrationRepository.flush();
        return views.event(event, student);
    }

    @Transactional(readOnly = true)
    public List<AttendeeView> attendees(Long id) {
        if (!eventRepository.existsById(id)) {
            throw ApiException.notFound("Event");
        }
        return registrationRepository.findAttendees(id).stream().map(r -> {
            User u = r.getUser();
            return new AttendeeView(u.getId(), u.getName(), u.getEmail(), u.getDepartment(), u.getYear(),
                    r.getRegisteredAt());
        }).toList();
    }

    // ---- helpers -------------------------------------------------------------------------

    private Club activeClub(Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> ApiException.notFound("Club"));
        if (!club.isActive()) {
            throw ApiException.badRequest("CLUB_ARCHIVED", club.getName() + " is archived. Restore it first.");
        }
        return club;
    }

    private static void checkTimes(EventRequest req) {
        if (!req.endsAt().isAfter(req.startsAt())) {
            throw ApiException.badRequest("INVALID_TIMES", "The end time must be after the start time.");
        }
        if (req.registrationDeadline().isAfter(req.startsAt())) {
            throw ApiException.badRequest("INVALID_DEADLINE",
                    "The registration deadline can't be after the event starts.");
        }
    }

    private static void apply(Event event, EventRequest req) {
        event.setTitle(req.title().trim());
        event.setDescription(Text.clean(req.description()));
        event.setVenue(req.venue().trim());
        event.setStartsAt(req.startsAt());
        event.setEndsAt(req.endsAt());
        event.setRegistrationDeadline(req.registrationDeadline());
        event.setCapacity(req.capacity());
    }
}
