package com.miniclub.service;

import com.miniclub.dto.AnnouncementView;
import com.miniclub.dto.ClubCard;
import com.miniclub.dto.Dashboard;
import com.miniclub.dto.EventView;
import com.miniclub.dto.MyActivity;
import com.miniclub.dto.StatItem;
import com.miniclub.dto.StudentView;
import com.miniclub.model.Event;
import com.miniclub.model.EventRegistration;
import com.miniclub.model.Role;
import com.miniclub.model.User;
import com.miniclub.repository.AnnouncementRepository;
import com.miniclub.repository.ClubMemberRepository;
import com.miniclub.repository.ClubRepository;
import com.miniclub.repository.EventRegistrationRepository;
import com.miniclub.repository.EventRepository;
import com.miniclub.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final ViewBuilder views;

    public DashboardService(ClubRepository clubRepository, ClubMemberRepository memberRepository,
                            EventRepository eventRepository, EventRegistrationRepository registrationRepository,
                            AnnouncementRepository announcementRepository, UserRepository userRepository,
                            ViewBuilder views) {
        this.clubRepository = clubRepository;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.views = views;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(User user) {
        return user.isAdmin() ? adminDashboard(user) : studentDashboard(user);
    }

    /** Everything the student has signed up for: clubs plus upcoming and past event registrations. */
    @Transactional(readOnly = true)
    public MyActivity activity(User student) {
        Instant now = Instant.now();
        List<Event> mine = registrationRepository.findByUserWithEvent(student.getId()).stream()
                .map(EventRegistration::getEvent)
                .filter(e -> e.getClub().isActive())
                .toList();
        List<Event> upcoming = mine.stream().filter(e -> e.getEndsAt().isAfter(now)).toList();
        List<Event> past = mine.stream().filter(e -> !e.getEndsAt().isAfter(now))
                .sorted(Comparator.comparing(Event::getStartsAt).reversed()).toList();
        return new MyActivity(myClubs(student), views.events(upcoming, student), views.events(past, student));
    }

    @Transactional(readOnly = true)
    public List<StudentView> students() {
        Map<Long, Long> clubs = ViewBuilder.toMap(memberRepository.countPerUser());
        Map<Long, Long> events = ViewBuilder.toMap(registrationRepository.countPerUser());
        return userRepository.findByRoleOrderByNameAsc(Role.STUDENT).stream()
                .map(u -> new StudentView(u.getId(), u.getName(), u.getEmail(), u.getDepartment(), u.getYear(),
                        clubs.getOrDefault(u.getId(), 0L), events.getOrDefault(u.getId(), 0L), u.getCreatedAt()))
                .toList();
    }

    // ---- student ---------------------------------------------------------------------------

    private Dashboard studentDashboard(User student) {
        Instant now = Instant.now();
        List<ClubCard> clubs = myClubs(student);
        Set<Long> clubIds = views.joinedClubIds(student);

        MyActivity activity = activity(student);
        List<EventView> myUpcoming = activity.upcomingEvents().stream().filter(e -> !e.cancelled()).toList();

        // Open events the student hasn't registered for yet, events from their own clubs first.
        List<EventView> open = views.events(eventRepository.findUpcoming(now, null).stream()
                .filter(e -> e.getClub().isActive() && !e.isCancelled()).toList(), student).stream()
                .filter(e -> !e.registered() && "OPEN".equals(e.status()))
                .toList();
        List<EventView> suggested = open.stream()
                .sorted(Comparator.comparing((EventView e) -> !clubIds.contains(e.clubId()))
                        .thenComparing(EventView::startsAt))
                .limit(4).toList();

        List<AnnouncementView> notices = announcementRepository.findBoard().stream()
                .filter(a -> a.getClub() == null
                        || (a.getClub().isActive() && clubIds.contains(a.getClub().getId())))
                .map(AnnouncementView::from).toList();
        Instant weekAgo = now.minus(Duration.ofDays(7));
        long fresh = notices.stream().filter(a -> a.createdAt().isAfter(weekAgo)).count();

        List<StatItem> stats = List.of(
                new StatItem("Clubs joined", clubs.size(), "Clubs you are a member of"),
                new StatItem("My upcoming events", myUpcoming.size(), "Events you registered for"),
                new StatItem("Open events", open.size(), "Still taking registrations"),
                new StatItem("New notices", fresh, "Posted in the last 7 days"));
        return new Dashboard(Role.STUDENT, stats, clubs, myUpcoming.stream().limit(5).toList(), suggested,
                notices.stream().limit(4).toList());
    }

    private List<ClubCard> myClubs(User student) {
        Set<Long> ids = views.joinedClubIds(student);
        return views.clubCards(clubRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(c -> ids.contains(c.getId())).toList(), student);
    }

    // ---- admin -----------------------------------------------------------------------------

    private Dashboard adminDashboard(User admin) {
        Instant now = Instant.now();
        List<ClubCard> popular = views.clubCards(clubRepository.findByActiveTrueOrderByNameAsc(), admin).stream()
                .sorted(Comparator.comparingLong(ClubCard::memberCount).reversed())
                .limit(5).toList();
        List<EventView> upcoming = views.events(eventRepository.findUpcoming(now, null).stream()
                .filter(e -> e.getClub().isActive() && !e.isCancelled()).limit(6).toList(), admin);
        List<AnnouncementView> notices = announcementRepository.findBoard().stream()
                .limit(4).map(AnnouncementView::from).toList();

        List<StatItem> stats = List.of(
                new StatItem("Active clubs", clubRepository.countByActiveTrue(), "Visible to students"),
                new StatItem("Students", userRepository.countByRole(Role.STUDENT), "Registered accounts"),
                new StatItem("Memberships", memberRepository.countInActiveClubs(), "Across all active clubs"),
                new StatItem("Upcoming events", eventRepository.countUpcoming(now), "Not finished or cancelled"),
                new StatItem("Event registrations", registrationRepository.count(), "All time"),
                new StatItem("Announcements", announcementRepository.count(), "On the notice board"));
        return new Dashboard(Role.ADMIN, stats, popular, upcoming, List.of(), notices);
    }
}
