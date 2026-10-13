package com.miniclub.config;

import com.miniclub.model.Announcement;
import com.miniclub.model.Club;
import com.miniclub.model.ClubCategory;
import com.miniclub.model.ClubMember;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with demo data so the portal can be shown right away.
 * Event dates are relative to today, so the demo always has upcoming events.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final UserRepository users;
    private final ClubRepository clubs;
    private final ClubMemberRepository members;
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final AnnouncementRepository announcements;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DataSeeder(UserRepository users, ClubRepository clubs, ClubMemberRepository members,
                      EventRepository events, EventRegistrationRepository registrations,
                      AnnouncementRepository announcements, PasswordEncoder encoder,
                      @Value("${app.seed.enabled:true}") boolean enabled) {
        this.users = users;
        this.clubs = clubs;
        this.members = members;
        this.events = events;
        this.registrations = registrations;
        this.announcements = announcements;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || users.count() > 0) {
            return;
        }
        log.info("Empty database - inserting demo data for Mini College Club Portal.");

        User admin = user("Club Office Admin", "admin@clubportal.edu", "Admin@123", Role.ADMIN, "Student Activities", null);
        User ananya = user("Ananya Reddy", "student@clubportal.edu", "Student@123", Role.STUDENT, "CSE", 3);
        User rahul = user("Rahul Varma", "rahul@clubportal.edu", "Student@123", Role.STUDENT, "ECE", 2);
        User fatima = user("Fatima Shaik", "fatima@clubportal.edu", "Student@123", Role.STUDENT, "IT", 1);
        User karthik = user("Karthik Rao", "karthik@clubportal.edu", "Student@123", Role.STUDENT, "Mechanical", 4);

        Club code = club("Code Crafters", ClubCategory.TECHNICAL, "Code, compete and build real projects together",
                "Weekly coding sessions, contest practice, hackathons and open-source sprints. Beginners are "
                        + "welcome - seniors pair with first-years on every project.",
                "Varun Teja (III CSE)", "Dr. P. Lakshmi", "Wednesdays, 4:00 - 5:30 PM, Lab 204", "codecrafters@clubportal.edu", 60);
        Club robo = club("RoboNexus", ClubCategory.TECHNICAL, "Robots, drones and IoT tinkering",
                "Hands-on electronics and robotics: line followers, drones, sensor projects and the annual "
                        + "robo-race. Components are provided by the department.",
                "Sanjana M (IV ECE)", "Prof. R. Srinivas", "Saturdays, 10:00 AM - 1:00 PM, Innovation Lab", "robonexus@clubportal.edu", 30);
        Club music = club("Swara Music Club", ClubCategory.CULTURAL, "Jam sessions, open mics and the fest band",
                "Vocals, guitar, keys, percussion - every style is welcome. We perform at college fests and "
                        + "run open-mic evenings every month.",
                "Harsha Vardhan (III IT)", "Ms. K. Deepthi", "Tuesdays & Thursdays, 5:00 - 6:30 PM, Music Room", null, 40);
        Club dance = club("Nritya Dance Crew", ClubCategory.CULTURAL, "Classical, hip-hop and everything in between",
                "Learn choreography, compete in inter-college dance battles and perform at the annual day.",
                "Meghana S (II CSE)", "Ms. A. Priya", "Mondays & Fridays, 5:30 - 7:00 PM, Auditorium Stage", null, 35);
        Club photo = club("Lens & Frames", ClubCategory.ARTS, "Photography and short film club",
                "Photo walks, editing workshops and short-film projects. Bring a phone or a camera - both work.",
                "Arjun K (III MECH)", "Mr. S. Naveen", "Sundays, 7:00 - 9:00 AM, Main Gate", "lensandframes@clubportal.edu", 25);
        Club sports = club("Sports Arena", ClubCategory.SPORTS, "Cricket, football, badminton and more",
                "Daily practice slots, inter-department tournaments and selection trials for university teams.",
                "Vikram Goud (IV CIVIL)", "Mr. D. Ramesh (Physical Director)", "Mon - Sat, 6:00 - 7:30 AM, Main Ground", null, 80);
        Club chess = club("Chess Circle", ClubCategory.SPORTS, "Think three moves ahead",
                "Casual games, puzzle nights and rated rapid tournaments. Small group, big brains.",
                "Rahul Varma (II ECE)", "Dr. M. Kiran", "Fridays, 3:00 - 5:00 PM, Library Hall", null, 3);
        Club lit = club("Inkwell Literary Society", ClubCategory.LITERARY, "Debates, quizzes, poetry and the college magazine",
                "Sharpen your words: weekly debates, quiz nights, creative writing and editing the college magazine.",
                "Nikhita R (III CSE)", "Dr. V. Sharada", "Wednesdays, 3:00 - 4:30 PM, Seminar Hall B", null, 30);
        Club nss = club("Seva NSS Unit", ClubCategory.COMMUNITY, "Serve the community, one drive at a time",
                "Blood donation camps, village visits, clean-up drives and awareness rallies. Volunteering hours count "
                        + "towards your NSS certificate.",
                "Pranav Reddy (III IT)", "Dr. G. Suresh (NSS Officer)", "Sundays, 8:00 AM - 12:00 PM", "nss@clubportal.edu", 100);

        join(code, ananya);
        join(music, ananya);
        join(code, rahul);
        join(robo, rahul);
        join(chess, rahul);
        join(photo, fatima);
        join(nss, fatima);
        join(chess, fatima);
        join(sports, karthik);
        join(chess, karthik); // Chess Circle is now full (3 of 3) - shows the "club full" rule in the demo

        Event hack = event(code, "HackNight 2.0", "An overnight build sprint in teams of up to four. Mentors, snacks "
                + "and prizes for the top three teams.", "Seminar Hall A", 5, "18:00", 5, 80);
        Event git = event(code, "Git & GitHub Bootcamp", "Branches, pull requests and resolving merge conflicts - "
                + "bring your laptop.", "Lab 204", 2, "15:00", 2, 40);
        event(robo, "Line Follower Challenge", "Build and race a line-following robot. Kits available on request.",
                "Innovation Lab", 9, "10:00", 4, 24);
        Event drone = event(robo, "Drone Assembly Session", "Assemble and test a quadcopter frame.",
                "Innovation Lab", 5, "10:00", 3, 20);
        drone.setCancelled(true);
        Event openMic = event(music, "Open Mic Evening", "Sing, play or recite - everyone gets five minutes on stage.",
                "Open Air Theatre", 3, "17:30", 2, 120);
        event(dance, "Freestyle Workshop", "A beginner-friendly session on freestyle and footwork basics.",
                "Auditorium Stage", 6, "16:00", 2, 30);
        Event walk = event(photo, "Campus Photo Walk", "Golden-hour photo walk around the campus lake.",
                "Main Gate", 1, "06:30", 2, 25);
        // Registration closed an hour ago, but the walk hasn't started yet - shows the "Closed" state.
        walk.setRegistrationDeadline(Instant.now().minus(Duration.ofHours(1)));
        event(sports, "Inter-Department Cricket Trials", "Selection trials for the inter-department T20 league.",
                "Main Ground", 4, "06:30", 3, 44);
        Event simul = event(chess, "Simul vs the Club Captain", "Our captain plays three opponents at once.",
                "Library Hall", 8, "15:00", 2, 3);
        event(lit, "Debate: AI in Classrooms", "Parliamentary-style debate. Teams of two, topics shared on the day.",
                "Seminar Hall B", 7, "15:00", 2, 60);
        Event blood = event(nss, "Blood Donation Camp", "In association with the city blood bank. Donors get a "
                + "certificate and refreshments.", "Health Centre", 11, "09:00", 6, 150);
        Event cpIntro = event(code, "Intro to Competitive Programming", "Time complexity, STL and your first contest.",
                "Lab 204", -6, "16:00", 2, 50);
        Event jam = event(music, "Freshers' Jam", "Welcome jam for first-years.", "Music Room", -12, "17:00", 2, 40);

        register(hack, ananya);
        register(hack, rahul);
        register(git, rahul);
        register(openMic, ananya);
        register(openMic, fatima);
        register(blood, fatima);
        register(blood, karthik);
        register(simul, rahul);
        register(simul, fatima);
        register(simul, karthik); // full: 3 of 3 seats
        register(cpIntro, ananya);
        register(cpIntro, rahul);
        register(jam, ananya);
        register(jam, fatima);

        notice(admin, null, "Club registrations are open for 2026-27",
                "Explore the clubs on this portal and join the ones you like. You can be part of as many clubs as "
                        + "you can actively attend. Club heads will add you to their WhatsApp groups after you join.",
                true, 9);
        notice(admin, null, "Annual Club Fest - call for stalls",
                "Every club gets a stall at this year's Club Fest. Club heads, send your stall ideas and power "
                        + "requirements to the Student Activities office by Friday.",
                false, 4);
        notice(admin, code, "HackNight 2.0 - team formation",
                "Register on the portal first, then form teams of up to four in the Lab 204 meet-up on Monday. "
                        + "Solo participants will be matched with a team.",
                false, 2);
        notice(admin, music, "Auditions for the fest band",
                "We need two vocalists, a drummer and a keyboard player for the fest band. Auditions after the "
                        + "Open Mic Evening.",
                false, 3);
        notice(admin, nss, "Volunteers needed for the Blood Donation Camp",
                "We need 20 volunteers for registration, refreshments and donor care. Volunteers must report at "
                        + "8:00 AM at the Health Centre.",
                true, 1);
        notice(admin, sports, "Ground timings changed this week",
                "Morning practice moves to 6:30 - 8:00 AM this week because of the cricket trials.",
                false, 0);

        log.info("Demo data ready. Admin: admin@clubportal.edu / Admin@123 - Student: student@clubportal.edu / Student@123");
    }

    // ---- helpers ---------------------------------------------------------------------------

    private User user(String name, String email, String password, Role role, String dept, Integer year) {
        return users.save(new User(name, email, encoder.encode(password), role, dept, year));
    }

    private Club club(String name, ClubCategory category, String tagline, String description, String head,
                      String advisor, String meeting, String email, int maxMembers) {
        Club c = new Club();
        c.setName(name);
        c.setCategory(category);
        c.setTagline(tagline);
        c.setDescription(description);
        c.setClubHead(head);
        c.setFacultyAdvisor(advisor);
        c.setMeetingInfo(meeting);
        c.setContactEmail(email);
        c.setMaxMembers(maxMembers);
        return clubs.save(c);
    }

    private void join(Club club, User student) {
        members.save(new ClubMember(club, student));
    }

    /** dayOffset is relative to today (negative = in the past); time is local college time (IST). */
    private Event event(Club club, String title, String description, String venue, int dayOffset, String time,
                        int hours, int capacity) {
        Instant start = LocalDate.now(IST).plusDays(dayOffset).atTime(LocalTime.parse(time)).atZone(IST).toInstant();
        Event e = new Event();
        e.setClub(club);
        e.setTitle(title);
        e.setDescription(description);
        e.setVenue(venue);
        e.setStartsAt(start);
        e.setEndsAt(start.plus(Duration.ofHours(hours)));
        e.setRegistrationDeadline(start.minus(Duration.ofHours(3)));
        e.setCapacity(capacity);
        return events.save(e);
    }

    private void register(Event event, User student) {
        registrations.save(new EventRegistration(event, student));
    }

    private void notice(User admin, Club club, String title, String body, boolean pinned, int daysAgo) {
        Announcement a = new Announcement();
        a.setPostedBy(admin);
        a.setClub(club);
        a.setTitle(title);
        a.setBody(body);
        a.setPinned(pinned);
        a.setCreatedAt(Instant.now().minus(Duration.ofDays(daysAgo)).minus(Duration.ofMinutes(37L * daysAgo + 5)));
        announcements.save(a);
    }
}
