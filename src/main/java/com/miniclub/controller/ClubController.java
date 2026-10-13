package com.miniclub.controller;

import com.miniclub.dto.ClubActiveRequest;
import com.miniclub.dto.ClubCard;
import com.miniclub.dto.ClubDetail;
import com.miniclub.dto.ClubRequest;
import com.miniclub.dto.MemberView;
import com.miniclub.model.ClubCategory;
import com.miniclub.model.User;
import com.miniclub.service.ClubService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    public List<ClubCard> list(@AuthenticationPrincipal User user,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) ClubCategory category) {
        return clubService.list(user, search, category);
    }

    @GetMapping("/{id}")
    public ClubDetail get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return clubService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClubCard create(@AuthenticationPrincipal User user, @Valid @RequestBody ClubRequest req) {
        return clubService.create(user, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClubCard update(@AuthenticationPrincipal User user, @PathVariable Long id,
                           @Valid @RequestBody ClubRequest req) {
        return clubService.update(user, id, req);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ClubCard setActive(@AuthenticationPrincipal User user, @PathVariable Long id,
                              @Valid @RequestBody ClubActiveRequest req) {
        return clubService.setActive(user, id, req.active());
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public ClubCard join(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return clubService.join(user, id);
    }

    @DeleteMapping("/{id}/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ClubCard leave(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return clubService.leave(user, id);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberView> members(@PathVariable Long id) {
        return clubService.members(id);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        clubService.removeMember(id, userId);
    }
}
