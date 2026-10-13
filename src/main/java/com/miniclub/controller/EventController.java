package com.miniclub.controller;

import com.miniclub.dto.AttendeeView;
import com.miniclub.dto.EventRequest;
import com.miniclub.dto.EventView;
import com.miniclub.model.User;
import com.miniclub.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventView> list(@AuthenticationPrincipal User user,
                                @RequestParam(required = false) Long clubId,
                                @RequestParam(defaultValue = "upcoming") String when,
                                @RequestParam(required = false) String search,
                                @RequestParam(defaultValue = "false") boolean mine) {
        return eventService.list(user, clubId, when, search, mine);
    }

    @GetMapping("/{id}")
    public EventView get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return eventService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EventView create(@AuthenticationPrincipal User user, @Valid @RequestBody EventRequest req) {
        return eventService.create(user, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventView update(@AuthenticationPrincipal User user, @PathVariable Long id,
                            @Valid @RequestBody EventRequest req) {
        return eventService.update(user, id, req);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public EventView cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return eventService.cancel(user, id);
    }

    @PostMapping("/{id}/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public EventView register(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return eventService.register(user, id);
    }

    @DeleteMapping("/{id}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public EventView unregister(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return eventService.unregister(user, id);
    }

    @GetMapping("/{id}/attendees")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AttendeeView> attendees(@PathVariable Long id) {
        return eventService.attendees(id);
    }
}
