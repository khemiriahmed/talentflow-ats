package com.talentflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.ApiClient;
import com.talentflow.RecordingEmailService;
import com.talentflow.RecordingEmailService.SentEmail;
import com.talentflow.TestEmailConfig;
import com.talentflow.interview.Interview;
import com.talentflow.interview.InterviewMode;
import com.talentflow.interview.InterviewRepository;
import com.talentflow.notification.NotificationDispatcher;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static com.talentflow.ApiClient.bearer;
import static com.talentflow.ApiClient.total;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = "app.reminders.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
@Import({ApplicationFlowTest.TestStorageConfig.class, TestEmailConfig.class})
class NotificationFlowTest {

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
    @Autowired
    RecordingEmailService emails;
    @Autowired
    NotificationDispatcher dispatcher;

    ApiClient api;

    @BeforeEach
    void init() {
        api = new ApiClient(mvc, objectMapper);
    }

    record Fixture(User hr, User manager, String hrToken, String managerToken,
                   String candidateToken, String candidateEmail, long jobId, long appId) {
    }

    // ------------------------------------------------------------------ emails + in-app

    @Test
    void emailsAndNotificationsFollowTheRecruitmentFlow() throws Exception {
        Fixture f = fixture();

        // --- candidature reçue ---
        awaitEmail(f.candidateEmail(), m -> m.subject().contains("bien été reçue"));
        awaitNotification(f.managerToken(), "APPLICATION_RECEIVED");
        awaitNotification(f.hrToken(), "APPLICATION_RECEIVED");      // la RH est la créatrice de l'offre

        // --- changement d'étape : email au candidat, notification au manager (pas à l'auteur) ---
        stage(f, "SCREENING", null);
        SentEmail stageMail = awaitEmail(f.candidateEmail(), m -> m.html().contains("Présélection"));
        assertThat(stageMail.subject()).contains("Mise à jour");
        awaitNotification(f.managerToken(), "STAGE_CHANGED");
        assertThat(types(notifications(f.hrToken()))).doesNotContain("STAGE_CHANGED");

        // --- refus : le motif interne ne doit JAMAIS apparaître dans l'email ---
        stage(f, "REJECTED", "Secret internal reason");
        SentEmail rejection = awaitEmail(f.candidateEmail(), m -> m.html().contains("regret"));
        assertThat(rejection.html()).doesNotContain("Secret internal reason");
        stage(f, "SCREENING", null);   // réouverture pour planifier un entretien

        // --- entretien planifié : invitations + notification aux participants (pas à l'auteur) ---
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        long interviewId = api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(f, start)).header("Authorization", bearer(f.hrToken())), 201).get("id").asLong();

        SentEmail invitation = awaitEmail(f.candidateEmail(), m -> m.subject().contains("Invitation"));
        assertThat(invitation.html()).contains("https://meet.example.com/room");
        SentEmail staffInvitation = awaitEmail(f.manager().getEmail(), m -> m.subject().contains("Invitation"));
        assertThat(staffInvitation.html()).contains("Cand Idate");
        awaitNotification(f.managerToken(), "INTERVIEW_SCHEDULED");
        assertThat(types(notifications(f.hrToken()))).doesNotContain("INTERVIEW_SCHEDULED");

        // --- annulation ---
        api.send(patch("/api/v1/interviews/{id}/cancel", interviewId)
                .header("Authorization", bearer(f.hrToken())), 200);
        awaitEmail(f.candidateEmail(), m -> m.subject().contains("annulé"));
        awaitEmail(f.manager().getEmail(), m -> m.subject().contains("annulé"));
        awaitNotification(f.managerToken(), "INTERVIEW_CANCELLED");

        // --- feedback : la RH qui a planifié est prévenue ---
        Interview past = interview(f, Instant.now().minus(3, ChronoUnit.HOURS));
        api.send(api.json(post("/api/v1/interviews/{id}/feedback", past.getId()),
                Map.of("rating", 4, "recommendation", "YES"))
                .header("Authorization", bearer(f.managerToken())), 201);
        awaitNotification(f.hrToken(), "FEEDBACK_SUBMITTED");
    }

    // ------------------------------------------------------------------ rappels

    @Test
    void remindersAreSentOnceAndOnlyForInterviewsStartingWithin24Hours() throws Exception {
        Fixture f = fixture();

        // planifié via l'API moins de 24 h à l'avance : l'invitation suffit, pas de rappel
        long soonId = api.send(api.json(post("/api/v1/applications/{id}/interviews", f.appId()),
                interviewBody(f, Instant.now().plus(10, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS)))
                .header("Authorization", bearer(f.hrToken())), 201).get("id").asLong();
        assertThat(interviewRepository.findById(soonId).orElseThrow().getReminderSentAt()).isNotNull();

        // entretien dans 13 h, rappel pas encore envoyé (créé directement en base)
        Interview due = interview(f, Instant.now().plus(13, ChronoUnit.HOURS));
        // entretien dans 5 jours : trop tôt pour un rappel
        interview(f, Instant.now().plus(5, ChronoUnit.DAYS));

        assertThat(dispatcher.sendDueReminders(Instant.now())).isEqualTo(1);
        assertThat(emails.to(f.candidateEmail()))
                .anyMatch(m -> m.subject().contains("Rappel"));
        assertThat(emails.to(f.manager().getEmail()))
                .anyMatch(m -> m.subject().contains("Rappel"));
        assertThat(interviewRepository.findById(due.getId()).orElseThrow().getReminderSentAt()).isNotNull();
        awaitNotification(f.managerToken(), "INTERVIEW_REMINDER");

        // deuxième passage : rien à renvoyer
        assertThat(dispatcher.sendDueReminders(Instant.now())).isZero();
    }

    // ------------------------------------------------------------------ API in-app

    @Test
    void inAppNotificationsArePersonalAndCanBeMarkedAsRead() throws Exception {
        Fixture f = fixture();
        awaitNotification(f.managerToken(), "APPLICATION_RECEIVED");

        assertThat(unreadCount(f.managerToken())).isEqualTo(1);
        long notificationId = notifications(f.managerToken()).get("content").get(0).get("id").asLong();

        // un autre utilisateur ne peut pas marquer cette notification comme lue
        api.send(patch("/api/v1/notifications/{id}/read", notificationId)
                .header("Authorization", bearer(f.hrToken())), 404);
        JsonNode read = api.send(patch("/api/v1/notifications/{id}/read", notificationId)
                .header("Authorization", bearer(f.managerToken())), 200);
        assertThat(read.get("read").asBoolean()).isTrue();
        assertThat(unreadCount(f.managerToken())).isZero();

        // nouvelle notification, puis "tout marquer comme lu"
        stage(f, "SCREENING", null);
        awaitNotification(f.managerToken(), "STAGE_CHANGED");
        assertThat(unreadCount(f.managerToken())).isEqualTo(1);
        assertThat(total(api.send(get("/api/v1/notifications").param("unreadOnly", "true")
                .header("Authorization", bearer(f.managerToken())), 200))).isEqualTo(1);

        JsonNode result = api.send(post("/api/v1/notifications/read-all")
                .header("Authorization", bearer(f.managerToken())), 200);
        assertThat(result.get("updated").asInt()).isEqualTo(1);
        assertThat(unreadCount(f.managerToken())).isZero();

        // le candidat reçoit des emails mais aucune notification in-app
        awaitEmail(f.candidateEmail(), m -> m.html().contains("Présélection"));
        assertThat(total(notifications(f.candidateToken()))).isZero();

        api.send(get("/api/v1/notifications"), 401);
    }

    // ------------------------------------------------------------------ helpers

    private Fixture fixture() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User hr = newStaff(Role.HR);
        User manager = newStaff(Role.MANAGER);
        String hrToken = api.login(hr.getEmail(), STAFF_PASSWORD);

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("title", "Notif job " + tag);
        job.put("description", "Spring Boot");
        job.put("contractType", "CDI");
        job.put("managerId", manager.getId());
        long jobId = api.send(api.json(post("/api/v1/jobs"), job)
                .header("Authorization", bearer(hrToken)), 201).get("id").asLong();
        api.send(api.json(patch("/api/v1/jobs/{id}/status", jobId), Map.of("status", "PUBLISHED"))
                .header("Authorization", bearer(hrToken)), 200);

        String candidateEmail = "cand-" + tag + "@test.local";
        String candidateToken = api.register(candidateEmail);
        long appId = api.send(multipart("/api/v1/jobs/{id}/applications", jobId)
                .file(new MockMultipartFile("cv", "cv.pdf", "application/pdf", PDF))
                .header("Authorization", bearer(candidateToken)), 201).get("id").asLong();

        return new Fixture(hr, manager, hrToken, api.login(manager.getEmail(), STAFF_PASSWORD),
                candidateToken, candidateEmail, jobId, appId);
    }

    private void stage(Fixture f, String stage, String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stage", stage);
        if (reason != null) {
            body.put("reason", reason);
        }
        api.send(api.json(patch("/api/v1/applications/{id}/stage", f.appId()), body)
                .header("Authorization", bearer(f.hrToken())), 200);
    }

    private Map<String, Object> interviewBody(Fixture f, Instant start) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startAt", start.toString());
        body.put("durationMin", 60);
        body.put("mode", "ONLINE");
        body.put("locationOrLink", "https://meet.example.com/room");
        body.put("participantIds", List.of(f.manager().getId()));
        return body;
    }

    private Interview interview(Fixture f, Instant start) {
        Application application = applicationRepository.findById(f.appId()).orElseThrow();
        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setStartAt(start);
        interview.setDurationMin(60);
        interview.setMode(InterviewMode.ON_SITE);
        interview.setCreatedBy(f.hr());
        interview.setParticipants(new HashSet<>(Set.of(f.manager())));
        return interviewRepository.saveAndFlush(interview);
    }

    private SentEmail awaitEmail(String to, Predicate<SentEmail> condition) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(emails.to(to)).anyMatch(condition));
        return emails.to(to).stream().filter(condition).findFirst().orElseThrow();
    }

    private void awaitNotification(String token, String type) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(types(notifications(token))).contains(type));
    }

    private JsonNode notifications(String token) throws Exception {
        return api.send(get("/api/v1/notifications").param("size", "50")
                .header("Authorization", bearer(token)), 200);
    }

    private long unreadCount(String token) throws Exception {
        return api.send(get("/api/v1/notifications/unread-count")
                .header("Authorization", bearer(token)), 200).get("count").asLong();
    }

    private static List<String> types(JsonNode page) {
        List<String> types = new java.util.ArrayList<>();
        for (JsonNode node : page.get("content")) {
            types.add(node.get("type").asText());
        }
        return types;
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
