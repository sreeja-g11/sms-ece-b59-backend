package com.miniclub;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** End-to-end API tests against an in-memory H2 database filled by DataSeeder. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MiniClubPortalApiTest {

    private static final String ADMIN = "admin@clubportal.edu";
    private static final String STUDENT = "student@clubportal.edu";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ---- helpers ---------------------------------------------------------------------------

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private String admin() throws Exception {
        return login(ADMIN, "Admin@123");
    }

    private String student() throws Exception {
        return login(STUDENT, "Student@123");
    }

    private String newStudent(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Student\",\"email\":\"" + email + "\",\"password\":\"Hello1234\",\"year\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String body = mvc.perform(get(url).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private long clubId(String token, String name) throws Exception {
        for (JsonNode c : getJson("/api/clubs", token)) {
            if (c.get("name").asText().equals(name)) {
                return c.get("id").asLong();
            }
        }
        throw new IllegalStateException("club not found: " + name);
    }

    private long eventId(String token, String title) throws Exception {
        for (JsonNode e : getJson("/api/events", token)) {
            if (e.get("title").asText().equals(title)) {
                return e.get("id").asLong();
            }
        }
        throw new IllegalStateException("event not found: " + title);
    }

    private String eventJson(long clubId, Instant start, Instant end, Instant deadline, int capacity) {
        return "{\"clubId\":" + clubId + ",\"title\":\"Test Meetup\",\"description\":\"Testing\","
                + "\"venue\":\"Room 1\",\"startsAt\":\"" + start + "\",\"endsAt\":\"" + end + "\","
                + "\"registrationDeadline\":\"" + deadline + "\",\"capacity\":" + capacity + "}";
    }

    private ResultActions createEvent(String token, String body) throws Exception {
        return mvc.perform(post("/api/events").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ---- auth ------------------------------------------------------------------------------

    @Test
    void healthIsPublicButEverythingElseNeedsLogin() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/clubs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mvc.perform(get("/api/clubs").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() throws Exception {
        for (String email : new String[]{STUDENT, "nobody@clubportal.edu"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"Wrong@1234\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void registerValidatesInputAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"short\",\"year\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasSize(4)));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Copy\",\"email\":\"STUDENT@clubportal.edu\",\"password\":\"Hello1234\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    // ---- clubs -----------------------------------------------------------------------------

    @Test
    void studentsCanSearchAndFilterClubs() throws Exception {
        String token = student();
        mvc.perform(get("/api/clubs?category=SPORTS").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].category", everyItem(is("SPORTS"))));
        mvc.perform(get("/api/clubs?search=robot").header("Authorization", token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("RoboNexus"));
        mvc.perform(get("/api/clubs?category=PAINTING").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void studentJoinsAndLeavesAClub() throws Exception {
        String token = newStudent("joiner@clubportal.edu");
        long robo = clubId(token, "RoboNexus");

        mvc.perform(post("/api/clubs/" + robo + "/join").header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.memberCount").value(2));
        mvc.perform(post("/api/clubs/" + robo + "/join").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_MEMBER"));

        JsonNode activity = getJson("/api/me/activity", token);
        assertEquals(1, activity.get("clubs").size());

        mvc.perform(delete("/api/clubs/" + robo + "/join").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(false))
                .andExpect(jsonPath("$.memberCount").value(1));
        mvc.perform(delete("/api/clubs/" + robo + "/join").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_MEMBER"));
    }

    @Test
    void aFullClubRejectsNewMembers() throws Exception {
        String token = student();
        long chess = clubId(token, "Chess Circle");
        mvc.perform(post("/api/clubs/" + chess + "/join").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CLUB_FULL"));
    }

    @Test
    void onlyAdminManagesClubs() throws Exception {
        String clubBody = "{\"name\":\"Astronomy Club\",\"category\":\"TECHNICAL\",\"tagline\":\"Look up\","
                + "\"description\":\"Star gazing nights.\",\"maxMembers\":20}";
        mvc.perform(post("/api/clubs").header("Authorization", student())
                        .contentType(MediaType.APPLICATION_JSON).content(clubBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        String admin = admin();
        String created = mvc.perform(post("/api/clubs").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(clubBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberCount").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(post("/api/clubs").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(clubBody.replace("Astronomy", "astronomy")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CLUB_NAME_TAKEN"));

        // Archived clubs disappear for students.
        mvc.perform(patch("/api/clubs/" + id + "/active").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/clubs/" + id).header("Authorization", student()))
                .andExpect(status().isNotFound());
    }

    @Test
    void clubLimitCannotGoBelowCurrentMembers() throws Exception {
        String admin = admin();
        long code = clubId(admin, "Code Crafters"); // 2 members
        String body = "{\"name\":\"Code Crafters\",\"category\":\"TECHNICAL\",\"tagline\":\"x\","
                + "\"description\":\"y\",\"maxMembers\":1}";
        mvc.perform(put("/api/clubs/" + code).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CAPACITY_TOO_LOW"));
    }

    // ---- events ----------------------------------------------------------------------------

    @Test
    void studentRegistersAndCancelsForAnEvent() throws Exception {
        String token = newStudent("eventgoer@clubportal.edu");
        long debate = eventId(token, "Debate: AI in Classrooms");

        mvc.perform(post("/api/events/" + debate + "/register").header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(post("/api/events/" + debate + "/register").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_REGISTERED"));

        JsonNode mine = getJson("/api/events?mine=true", token);
        assertEquals(1, mine.size());

        mvc.perform(get("/api/events/" + debate + "/attendees").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("eventgoer@clubportal.edu"));

        mvc.perform(delete("/api/events/" + debate + "/register").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.registeredCount").value(0));
    }

    @Test
    void fullClosedAndCancelledEventsRejectRegistrations() throws Exception {
        String token = student();
        mvc.perform(post("/api/events/" + eventId(token, "Simul vs the Club Captain") + "/register")
                        .header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EVENT_FULL"));
        mvc.perform(post("/api/events/" + eventId(token, "Campus Photo Walk") + "/register")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REGISTRATION_CLOSED"));
        mvc.perform(post("/api/events/" + eventId(token, "Drone Assembly Session") + "/register")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EVENT_CANCELLED"));
    }

    @Test
    void eventStatusesAndPastEvents() throws Exception {
        String token = student();
        for (JsonNode e : getJson("/api/events", token)) {
            String title = e.get("title").asText();
            String expected = switch (title) {
                case "Simul vs the Club Captain" -> "FULL";
                case "Campus Photo Walk" -> "CLOSED";
                case "Drone Assembly Session" -> "CANCELLED";
                default -> "OPEN";
            };
            assertEquals(expected, e.get("status").asText(), title);
        }
        JsonNode past = getJson("/api/events?when=past", token);
        assertEquals(2, past.size());
        assertTrue(past.get(0).get("status").asText().equals("ENDED"));
        mvc.perform(get("/api/events?when=later").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCreatesEventsWithValidTimes() throws Exception {
        String admin = admin();
        long lit = clubId(admin, "Inkwell Literary Society");
        Instant start = Instant.now().plus(Duration.ofDays(3));
        Instant end = start.plus(Duration.ofHours(2));

        createEvent(admin, eventJson(lit, start, start.minusSeconds(60), start.minus(Duration.ofHours(1)), 10))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_TIMES"));
        createEvent(admin, eventJson(lit, start, end, start.plus(Duration.ofHours(1)), 10))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_DEADLINE"));
        Instant past = Instant.now().minus(Duration.ofDays(1));
        createEvent(admin, eventJson(lit, past, past.plus(Duration.ofHours(1)), past.minus(Duration.ofHours(1)), 10))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("EVENT_IN_PAST"));
        createEvent(student(), eventJson(lit, start, end, start.minus(Duration.ofHours(1)), 10))
                .andExpect(status().isForbidden());

        String created = createEvent(admin, eventJson(lit, start, end, start.minus(Duration.ofHours(1)), 10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.clubName").value("Inkwell Literary Society"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(post("/api/events/" + id + "/cancel").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/events/" + id + "/cancel").header("Authorization", admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ALREADY_CANCELLED"));
    }

    @Test
    void eventCapacityCannotGoBelowRegistrations() throws Exception {
        String admin = admin();
        JsonNode blood = null;
        for (JsonNode e : getJson("/api/events", admin)) {
            if (e.get("title").asText().equals("Blood Donation Camp")) {
                blood = e;
            }
        }
        assertEquals(2, blood.get("registeredCount").asInt());
        String body = eventJson(blood.get("clubId").asLong(), Instant.parse(blood.get("startsAt").asText()),
                Instant.parse(blood.get("endsAt").asText()), Instant.parse(blood.get("registrationDeadline").asText()), 1);
        mvc.perform(put("/api/events/" + blood.get("id").asLong()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CAPACITY_TOO_LOW"));
    }

    // ---- announcements & dashboards ----------------------------------------------------------

    @Test
    void announcementsArePinnedFirstAndFilterable() throws Exception {
        String token = student(); // member of Code Crafters and Swara Music Club
        JsonNode board = getJson("/api/announcements", token);
        assertEquals(6, board.size());
        assertTrue(board.get(0).get("pinned").asBoolean());
        assertTrue(board.get(1).get("pinned").asBoolean());

        JsonNode mine = getJson("/api/announcements?mine=true", token);
        assertEquals(4, mine.size()); // 2 college-wide + Code Crafters + Swara

        String admin = admin();
        long code = clubId(admin, "Code Crafters");
        mvc.perform(post("/api/announcements").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Hi\",\"body\":\"There\"}"))
                .andExpect(status().isForbidden());
        String created = mvc.perform(post("/api/announcements").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clubId\":" + code + ",\"title\":\"Lab closed\",\"body\":\"No session today.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clubName").value("Code Crafters"))
                .andExpect(jsonPath("$.postedBy").value("Club Office Admin"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        assertEquals(2, getJson("/api/announcements?clubId=" + code, token).size());

        mvc.perform(delete("/api/announcements/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/announcements/" + id).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardsDependOnRole() throws Exception {
        mvc.perform(get("/api/dashboard").header("Authorization", student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.clubs", hasSize(2)))
                .andExpect(jsonPath("$.upcomingEvents", hasSize(2)))
                .andExpect(jsonPath("$.stats", hasSize(4)));
        mvc.perform(get("/api/dashboard").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.stats", hasSize(6)))
                .andExpect(jsonPath("$.clubs[0].name").value("Chess Circle"));
        mvc.perform(get("/api/students").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/students").header("Authorization", student()))
                .andExpect(status().isForbidden());
    }
}
