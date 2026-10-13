package com.miniclub.service;

import com.miniclub.dto.AnnouncementView;
import com.miniclub.dto.ClubCard;
import com.miniclub.dto.ClubDetail;
import com.miniclub.dto.ClubRequest;
import com.miniclub.dto.EventView;
import com.miniclub.dto.MemberView;
import com.miniclub.exception.ApiException;
import com.miniclub.model.Club;
import com.miniclub.model.ClubCategory;
import com.miniclub.model.ClubMember;
import com.miniclub.model.User;
import com.miniclub.repository.AnnouncementRepository;
import com.miniclub.repository.ClubMemberRepository;
import com.miniclub.repository.ClubRepository;
import com.miniclub.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final AnnouncementRepository announcementRepository;
    private final ViewBuilder views;

    public ClubService(ClubRepository clubRepository, ClubMemberRepository memberRepository,
                       EventRepository eventRepository, AnnouncementRepository announcementRepository,
                       ViewBuilder views) {
        this.clubRepository = clubRepository;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.announcementRepository = announcementRepository;
        this.views = views;
    }

    /** Students see active clubs only; the admin also sees archived ones. */
    @Transactional(readOnly = true)
    public List<ClubCard> list(User viewer, String search, ClubCategory category) {
        String q = Text.query(search);
        List<Club> clubs = (viewer.isAdmin() ? clubRepository.findAllByOrderByNameAsc()
                : clubRepository.findByActiveTrueOrderByNameAsc()).stream()
                .filter(c -> category == null || c.getCategory() == category)
                .filter(c -> Text.matches(q, c.getName(), c.getTagline(), c.getDescription(), c.getClubHead()))
                .toList();
        return views.clubCards(clubs, viewer);
    }

    @Transactional(readOnly = true)
    public ClubDetail get(User viewer, Long id) {
        Club c = visibleClub(viewer, id);
        ClubCard card = views.clubCard(c, viewer);
        List<EventView> events = views.events(eventRepository.findUpcoming(Instant.now(), id).stream()
                .filter(e -> !e.isCancelled()).limit(6).toList(), viewer);
        List<AnnouncementView> notices = announcementRepository.findBoard().stream()
                .filter(a -> a.getClub() != null && a.getClub().getId().equals(id))
                .limit(5).map(AnnouncementView::from).toList();
        return new ClubDetail(c.getId(), c.getName(), c.getCategory(), c.getTagline(), c.getDescription(),
                c.getClubHead(), c.getFacultyAdvisor(), c.getMeetingInfo(), c.getContactEmail(),
                card.memberCount(), c.getMaxMembers(), card.full(), card.joined(), c.isActive(), c.getCreatedAt(),
                events, notices);
    }

    @Transactional
    public ClubCard create(User admin, ClubRequest req) {
        String name = req.name().trim();
        if (clubRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("CLUB_NAME_TAKEN", "A club named \"" + name + "\" already exists.");
        }
        Club club = new Club();
        apply(club, req, name);
        clubRepository.save(club);
        return views.clubCard(club, admin);
    }

    @Transactional
    public ClubCard update(User admin, Long id, ClubRequest req) {
        // Lock first so the member count can't change while we check the new limit.
        Club club = clubRepository.lockById(id).orElseThrow(() -> ApiException.notFound("Club"));
        String name = req.name().trim();
        if (clubRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict("CLUB_NAME_TAKEN", "A club named \"" + name + "\" already exists.");
        }
        long members = memberRepository.countByClubId(id);
        if (req.maxMembers() < members) {
            throw ApiException.badRequest("CAPACITY_TOO_LOW",
                    "This club already has " + members + " members, so the limit can't be lower than that.");
        }
        apply(club, req, name);
        return views.clubCard(club, admin);
    }

    /** Archive (active = false) hides the club from students; restore brings it back. */
    @Transactional
    public ClubCard setActive(User admin, Long id, boolean active) {
        Club club = clubRepository.findById(id).orElseThrow(() -> ApiException.notFound("Club"));
        club.setActive(active);
        return views.clubCard(club, admin);
    }

    @Transactional
    public ClubCard join(User student, Long id) {
        // Lock the club row: two students can't both take the last seat.
        Club club = clubRepository.lockById(id).orElseThrow(() -> ApiException.notFound("Club"));
        if (!club.isActive()) {
            throw ApiException.notFound("Club");
        }
        if (memberRepository.existsByClubIdAndUserId(id, student.getId())) {
            throw ApiException.conflict("ALREADY_MEMBER", "You are already a member of " + club.getName() + ".");
        }
        if (memberRepository.countByClubId(id) >= club.getMaxMembers()) {
            throw ApiException.conflict("CLUB_FULL", club.getName() + " is full right now.");
        }
        memberRepository.save(new ClubMember(club, student));
        return views.clubCard(club, student);
    }

    @Transactional
    public ClubCard leave(User student, Long id) {
        ClubMember member = memberRepository.findByClubIdAndUserId(id, student.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_MEMBER",
                        "You are not a member of this club."));
        Club club = member.getClub();
        memberRepository.delete(member);
        memberRepository.flush();
        return views.clubCard(club, student);
    }

    @Transactional(readOnly = true)
    public List<MemberView> members(Long id) {
        if (!clubRepository.existsById(id)) {
            throw ApiException.notFound("Club");
        }
        return memberRepository.findMembers(id).stream().map(m -> {
            User u = m.getUser();
            return new MemberView(u.getId(), u.getName(), u.getEmail(), u.getDepartment(), u.getYear(),
                    m.getJoinedAt());
        }).toList();
    }

    @Transactional
    public void removeMember(Long clubId, Long userId) {
        ClubMember member = memberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> ApiException.notFound("Member"));
        memberRepository.delete(member);
    }

    // ---- helpers -------------------------------------------------------------------------

    private Club visibleClub(User viewer, Long id) {
        Club club = clubRepository.findById(id).orElseThrow(() -> ApiException.notFound("Club"));
        if (!club.isActive() && !viewer.isAdmin()) {
            throw ApiException.notFound("Club");
        }
        return club;
    }

    private static void apply(Club club, ClubRequest req, String name) {
        club.setName(name);
        club.setCategory(req.category());
        club.setTagline(req.tagline().trim());
        club.setDescription(req.description().trim());
        club.setClubHead(Text.clean(req.clubHead()));
        club.setFacultyAdvisor(Text.clean(req.facultyAdvisor()));
        club.setMeetingInfo(Text.clean(req.meetingInfo()));
        String email = Text.clean(req.contactEmail());
        club.setContactEmail(email == null ? null : email.toLowerCase());
        club.setMaxMembers(Objects.requireNonNull(req.maxMembers()));
    }
}
