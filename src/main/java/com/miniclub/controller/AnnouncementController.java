package com.miniclub.controller;

import com.miniclub.dto.AnnouncementRequest;
import com.miniclub.dto.AnnouncementView;
import com.miniclub.model.User;
import com.miniclub.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public List<AnnouncementView> list(@AuthenticationPrincipal User user,
                                       @RequestParam(required = false) Long clubId,
                                       @RequestParam(defaultValue = "false") boolean mine) {
        return announcementService.list(user, clubId, mine);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementView create(@AuthenticationPrincipal User user, @Valid @RequestBody AnnouncementRequest req) {
        return announcementService.create(user, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementView update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest req) {
        return announcementService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        announcementService.delete(id);
    }
}
