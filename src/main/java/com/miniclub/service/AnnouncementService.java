package com.miniclub.service;

import com.miniclub.dto.AnnouncementRequest;
import com.miniclub.dto.AnnouncementView;
import com.miniclub.exception.ApiException;
import com.miniclub.model.Announcement;
import com.miniclub.model.Club;
import com.miniclub.model.User;
import com.miniclub.repository.AnnouncementRepository;
import com.miniclub.repository.ClubRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ClubRepository clubRepository;
    private final ViewBuilder views;

    public AnnouncementService(AnnouncementRepository announcementRepository, ClubRepository clubRepository,
                               ViewBuilder views) {
        this.announcementRepository = announcementRepository;
        this.clubRepository = clubRepository;
        this.views = views;
    }

    /**
     * The notice board, pinned first then newest first.
     * clubId = only that club's notices. mine = college-wide notices plus notices from the student's clubs.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementView> list(User viewer, Long clubId, boolean mine) {
        Set<Long> myClubs = mine ? views.joinedClubIds(viewer) : null;
        return announcementRepository.findBoard().stream()
                .filter(a -> a.getClub() == null || a.getClub().isActive() || viewer.isAdmin())
                .filter(a -> clubId == null || (a.getClub() != null && a.getClub().getId().equals(clubId)))
                .filter(a -> myClubs == null || viewer.isAdmin() || a.getClub() == null
                        || myClubs.contains(a.getClub().getId()))
                .map(AnnouncementView::from)
                .toList();
    }

    @Transactional
    public AnnouncementView create(User admin, AnnouncementRequest req) {
        Announcement a = new Announcement();
        a.setPostedBy(admin);
        a.setClub(targetClub(req.clubId()));
        apply(a, req);
        announcementRepository.save(a);
        return AnnouncementView.from(a);
    }

    @Transactional
    public AnnouncementView update(Long id, AnnouncementRequest req) {
        Announcement a = announcementRepository.findWithDetails(id)
                .orElseThrow(() -> ApiException.notFound("Announcement"));
        Long currentClubId = a.getClub() == null ? null : a.getClub().getId();
        if (!Objects.equals(currentClubId, req.clubId())) {
            a.setClub(targetClub(req.clubId()));
        }
        apply(a, req);
        announcementRepository.flush(); // runs @PreUpdate so updatedAt is filled in the response
        return AnnouncementView.from(a);
    }

    @Transactional
    public void delete(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Announcement"));
        announcementRepository.delete(a);
    }

    private Club targetClub(Long clubId) {
        if (clubId == null) {
            return null;
        }
        Club club = clubRepository.findById(clubId).orElseThrow(() -> ApiException.notFound("Club"));
        if (!club.isActive()) {
            throw ApiException.badRequest("CLUB_ARCHIVED", club.getName() + " is archived. Restore it first.");
        }
        return club;
    }

    private static void apply(Announcement a, AnnouncementRequest req) {
        a.setTitle(req.title().trim());
        a.setBody(req.body().trim());
        a.setPinned(req.pinned());
    }
}
