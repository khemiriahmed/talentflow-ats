package com.talentflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.ApiClient;
import com.talentflow.TestEmailConfig;
import com.talentflow.interview.Interview;
import com.talentflow.interview.InterviewMode;
import com.talentflow.interview.InterviewRepository;
import com.talentflow.interview.InterviewStatus;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.talentflow.ApiClient.bearer;
import static com.talentflow.ApiClient.total;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import({ApplicationFlowTest.TestStorageConfig.class, TestEmailConfig.class})
class PipelineFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static final String STAFF_PASSWORD = "Staff12345!";
    static final byte[] PDF = "%PDF-1.4\n%fake pdf for tests\n".getBytes(StandardCharsets.UTF_8);

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository users;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    InterviewRepository interviewRepository;

    ApiClient api;

    @BeforeEach
    void init() {
        api = new ApiClient(mvc, objectMapper);
    }

    record Fixture(User hr, User manager, User otherManager,
                   String hrToken, String managerToken, String otherManagerToken,
                   String candidateToken, long jobId, long appId, long candidateId) {
    }

    // ------------------------------------------------------------------ pipeline

    @Test
    void pipelineMovesApplicationsAndRecordsHistory() throws Exception {
        Fixture f = fixture();

        assertThat(stage(f, "SCREENING", null, f.hrToken(), 200).get("currentStage").asText())
                .isEqualTo("SCREENING");
        stage(f, "SCREENING", null, f.hrToken(), 400);                // déjà dans cette étape
        stage(f, "REJECTED", null, f.hrToken(), 400);                 // motif obligatoire
        stage(f, "HIRED", null, f.hrToken(), 400);                    // HIRED seulement depuis OFFER
        stage(f, "SCREENING", null, f.candidateToken(), 403);
        stage(f, "SCREENING", null, f.managerToken(), 403);

        stage(f, "REJECTED", "Profile does not match", f.hrToken(), 200);
        stage(f, "SCREENING", null, f.hrToken(), 200);                // réouverture
        stage(f, "TECH_INTERVIEW", null, f.hrToken(), 200);
        stage(f, "OFFER", null, f.hrToken(), 200);
        stage(f, "HIRED", null, f.hrToken(), 200);
        stage(f, "SCREENING", null, f.hrToken(), 400);                // HIRED est final

        JsonNode history = api.send(get("/api/v1/applications/{id}/history", f.appId())
                .header("Authorization", bearer(f.hrToken())), 200);
        assertThat(history.size()).isEqualTo(7);                       // RECEIVED + 6 changements
        assertThat(history.get(0).get("toStage").asText()).isEqualTo("HIRED");
        boolean rejectionHasReason = false;
        for (JsonNode entry : history) {
            if ("REJECTED".equals(entry.get("toStage").asText())) {
                rejectionHasReason = "Profile does not match".equals(entry.get("reason").asText());
            }
        }
        assertThat(rejectionHasReason).isTrue();

        api.send(get("/api/v1/applications/{id}/history", f.appId())
                .header("Authorization", bearer(f.managerToken())), 200);
        api.send(get("/api/v1/applications/{id}/history", f.appId())
                .header("Authorization", bearer(f.otherManagerToken())), 404);
        api.send(get("/api/v1/applications/{id}/history", f.appId())
                .header("Authorization", bearer(f.candidateToken())), 403);

        JsonNode mine = api.send(get("/api/v1/applications/{id}", f.appId())
                .header("Authorization", bearer(f.candidateToken())), 200);
        assertThat(mine.get("currentStage").asText()).isEqualTo("HIRED");
    }

    // ------------------------------------------------------------------ notes

    @Test
    void notesAreInternalAndOnlyDeletableByTheirAuthor() throws Exception {
        Fixture f = fixture();

        JsonNode hrNote = api.send(api.json(post("/api/v1/applications/{id}/notes", f.appId()),
                Map.of("content", "Strong profile")).header("Authorization", bearer(f.hrToken())), 201);
        api.send(api.json(post("/api/v1/applications/{id}/notes", f.appId()),
                Map.of("content", "Good culture fit")).header("Authorization", bearer(f.managerToken())), 201);
        api.send(api.json(post("/api/v1/applications/{id}/notes", f.appId()),
                Map.of("content", "Not my candidate")).header("Authorization", bearer(f.otherManagerToken())), 404);
        api.send(api.json(post("/api/v1/applications/{id}/notes", f.appId()),
                Map.of("content", " ")).header("Authorization", bearer(f.hrToken())), 400);
        api.send(api.json(post("/api/v1/applications/{id}/notes", f.appId()),
                Map.of("content", "Sneaky")).header("Authorization", bearer(f.candidateToken())), 403);

        JsonNode notes = api.send(get("/api/v1/applications/{id}/notes", f.appId())
                .header("Authorization", bearer(f.hrToken())), 200);
        assertThat(notes.size()).isEqualTo(2);
        api.send(get("/api/v1/applications/{id}/notes", f.appId())
                .header("Authorization", bearer(f.candidateToken())), 403);

        long hrNoteId = hrNote.get("id").asLong();
        api.send(delete("/api/v1/applications/{id}/notes/{noteId}", f.appId(), hrNoteId)
                .header("Authorization", bearer(f.managerToken())), 403);
        api.send(delete("/api/v1/applications/{id}/notes/{noteId}", f.appId(), hrNoteId)
                .header("Authorization", bearer(f.hrToken())), 204);
        assertThat(api.send(get("/api/v1/applications/{id}/notes", f.appId())
                .header("Authorization", bearer(f.hrToken())), 200).size()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ interviews

    @Test
    void interviewsAreScheduledWithoutDoubleBookingAndGetFeedback() throws Exception {
        Fixture f = fixture();
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        // --- planification ---
        JsonNode interview = api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(start, f.manager().getId())).header("Authorization", bearer(f.hrToken())), 201);
        long interviewId = interview.get("id").asLong();
        assertThat(interview.get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(interview.get("participants").size()).isEqualTo(1);

        // chevauchement avec le même participant : refusé ; plus tard : accepté
        api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(start.plus(30, ChronoUnit.MINUTES), f.manager().getId()))
                .header("Authorization", bearer(f.hrToken())), 409);
        api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(start.plus(2, ChronoUnit.HOURS), f.manager().getId()))
                .header("Authorization", bearer(f.hrToken())), 201);

        // règles de validation et de droits
        api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(start.plus(1, ChronoUnit.DAYS), f.candidateId()))
                .header("Authorization", bearer(f.hrToken())), 400);           // le candidat n'est pas staff
        api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(start.plus(1, ChronoUnit.DAYS), 999_999_999L))
                .header("Authorization", bearer(f.hrToken())), 400);           // participant inconnu
        api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(Instant.now().minus(1, ChronoUnit.DAYS), f.manager().getId()))
                .header("Authorization", bearer(f.hrToken())), 400);           // dans le passé
        api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(start.plus(3, ChronoUnit.DAYS), f.manager().getId()))
                .header("Authorization", bearer(f.managerToken())), 403);      // seul RH / Admin planifie

        assertThat(api.send(get("/api/v1/applications/{id}/interviews", f.appId())
                .header("Authorization", bearer(f.hrToken())), 200).size()).isEqualTo(2);

        // --- calendrier ---
        Instant from = Instant.now();
        Instant to = from.plus(7, ChronoUnit.DAYS);
        JsonNode hrCalendar = api.send(get("/api/v1/interviews")
                .param("from", from.toString()).param("to", to.toString())
                .header("Authorization", bearer(f.hrToken())), 200);
        assertThat(containsId(hrCalendar, interviewId)).isTrue();
        assertThat(api.send(get("/api/v1/interviews")
                .param("from", from.toString()).param("to", to.toString())
                .header("Authorization", bearer(f.managerToken())), 200).size()).isEqualTo(2);
        assertThat(api.send(get("/api/v1/interviews")
                .param("from", from.toString()).param("to", to.toString())
                .header("Authorization", bearer(f.otherManagerToken())), 200).size()).isZero();
        api.send(get("/api/v1/interviews")
                .param("from", from.toString()).param("to", from.plus(100, ChronoUnit.DAYS).toString())
                .header("Authorization", bearer(f.hrToken())), 400);
        api.send(get("/api/v1/interviews")
                .param("from", from.toString()).param("to", to.toString())
                .header("Authorization", bearer(f.candidateToken())), 403);

        // --- annulation ---
        api.send(patch("/api/v1/interviews/{id}/cancel", interviewId)
                .header("Authorization", bearer(f.managerToken())), 403);
        JsonNode cancelled = api.send(patch("/api/v1/interviews/{id}/cancel", interviewId)
                .header("Authorization", bearer(f.hrToken())), 200);
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        api.send(patch("/api/v1/interviews/{id}/cancel", interviewId)
                .header("Authorization", bearer(f.hrToken())), 400);

        // --- feedback (entretien déjà commencé, créé directement en base) ---
        Interview past = pastInterview(f);
        api.send(api.json(post("/api/v1/interviews/{id}/feedback", past.getId()),
                feedbackBody(6)).header("Authorization", bearer(f.managerToken())), 400);   // note invalide
        api.send(api.json(post("/api/v1/interviews/{id}/feedback", past.getId()),
                feedbackBody(4)).header("Authorization", bearer(f.otherManagerToken())), 404); // pas participant
        api.send(api.json(post("/api/v1/interviews/{id}/feedback", past.getId()),
                feedbackBody(4)).header("Authorization", bearer(f.candidateToken())), 403);

        JsonNode feedback = api.send(api.json(post("/api/v1/interviews/{id}/feedback", past.getId()),
                feedbackBody(4)).header("Authorization", bearer(f.managerToken())), 201);
        assertThat(feedback.get("rating").asInt()).isEqualTo(4);
        api.send(api.json(post("/api/v1/interviews/{id}/feedback", past.getId()),
                feedbackBody(5)).header("Authorization", bearer(f.managerToken())), 409);   // un seul feedback

        assertThat(interviewRepository.findById(past.getId()).orElseThrow().getStatus())
                .isEqualTo(InterviewStatus.DONE);
        assertThat(api.send(get("/api/v1/interviews/{id}/feedback", past.getId())
                .header("Authorization", bearer(f.hrToken())), 200).get("recommendation").asText())
                .isEqualTo("YES");

        // un entretien qui n'a pas commencé n'accepte pas de feedback
        long upcomingId = api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(start.plus(4, ChronoUnit.DAYS), f.manager().getId()))
                .header("Authorization", bearer(f.hrToken())), 201).get("id").asLong();
        api.send(api.json(post("/api/v1/interviews/{id}/feedback", upcomingId),
                feedbackBody(3)).header("Authorization", bearer(f.managerToken())), 400);
    }

    // ------------------------------------------------------------------ helpers

    private Fixture fixture() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User hr = newStaff(Role.HR);
        User manager = newStaff(Role.MANAGER);
        User otherManager = newStaff(Role.MANAGER);
        String hrToken = api.login(hr.getEmail(), STAFF_PASSWORD);

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("title", "Pipeline job " + tag);
        job.put("description", "Spring Boot");
        job.put("contractType", "CDI");
        job.put("managerId", manager.getId());
        long jobId = api.send(api.json(post("/api/v1/jobs"), job)
                .header("Authorization", bearer(hrToken)), 201).get("id").asLong();
        api.send(api.json(patch("/api/v1/jobs/{id}/status", jobId), Map.of("status", "PUBLISHED"))
                .header("Authorization", bearer(hrToken)), 200);

        String candidateToken = api.register("cand-" + tag + "@test.local");
        JsonNode application = api.send(multipart("/api/v1/jobs/{id}/applications", jobId)
                .file(new MockMultipartFile("cv", "cv.pdf", "application/pdf", PDF))
                .header("Authorization", bearer(candidateToken)), 201);

        return new Fixture(hr, manager, otherManager,
                hrToken,
                api.login(manager.getEmail(), STAFF_PASSWORD),
                api.login(otherManager.getEmail(), STAFF_PASSWORD),
                candidateToken, jobId,
                application.get("id").asLong(),
                application.get("candidateId").asLong());
    }

    private JsonNode stage(Fixture f, String stage, String reason, String token, int expected) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stage", stage);
        if (reason != null) {
            body.put("reason", reason);
        }
        return api.send(api.json(patch("/api/v1/applications/{id}/stage", f.appId()), body)
                .header("Authorization", bearer(token)), expected);
    }

    private Map<String, Object> interviewBody(Instant start, Long participantId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startAt", start.toString());
        body.put("durationMin", 60);
        body.put("mode", "ONLINE");
        body.put("locationOrLink", "https://meet.example.com/room");
        body.put("participantIds", List.of(participantId));
        return body;
    }

    private Map<String, Object> feedbackBody(int rating) {
        return Map.of("rating", rating, "strengths", "Solid fundamentals",
                "weaknesses", "Little cloud experience", "recommendation", "YES");
    }

    private Interview pastInterview(Fixture f) {
        Application application = applicationRepository.findById(f.appId()).orElseThrow();
        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setStartAt(Instant.now().minus(3, ChronoUnit.HOURS));
        interview.setDurationMin(60);
        interview.setMode(InterviewMode.ON_SITE);
        interview.setCreatedBy(f.hr());
        interview.setParticipants(new HashSet<>(Set.of(f.manager())));
        return interviewRepository.saveAndFlush(interview);
    }

    private static boolean containsId(JsonNode array, long id) {
        for (JsonNode node : array) {
            if (node.get("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    private User newStaff(Role role) {
        User user = new User();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local");
        user.setPasswordHash(passwordEncoder.encode(STAFF_PASSWORD));
        user.setFirstName("Staff");
        user.setLastName(role.name());
        user.setRole(role);
        return users.saveAndFlush(user);
    }
}
