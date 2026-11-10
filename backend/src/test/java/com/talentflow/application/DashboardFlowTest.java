package com.talentflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.ApiClient;
import com.talentflow.TestEmailConfig;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.talentflow.ApiClient.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Une seule classe de test = une base vierge : les totaux du dashboard sont donc exacts. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import({ApplicationFlowTest.TestStorageConfig.class, TestEmailConfig.class})
class DashboardFlowTest {

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

    ApiClient api;

    @BeforeEach
    void init() {
        api = new ApiClient(mvc, objectMapper);
    }

    @Test
    void statsReflectTheRecruitmentFunnel() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User hr = newStaff(Role.HR);
        String hrToken = api.login(hr.getEmail(), STAFF_PASSWORD);

        long jobA = publishedJob(hrToken, "Job A " + tag);
        long jobB = publishedJob(hrToken, "Job B " + tag);

        String t1 = api.register("c1-" + tag + "@test.local");
        String t2 = api.register("c2-" + tag + "@test.local");
        String t3 = api.register("c3-" + tag + "@test.local");
        String t4 = api.register("c4-" + tag + "@test.local");
        long a1 = apply(jobA, t1, "linkedin");
        long a2 = apply(jobA, t2, "linkedin");
        apply(jobA, t3, null);
        long a4 = apply(jobB, t4, "referral");

        // a1 : parcourt tout le pipeline jusqu'à l'embauche
        for (String stage : new String[]{"SCREENING", "HR_INTERVIEW", "TECH_INTERVIEW", "OFFER", "HIRED"}) {
            move(a1, hrToken, stage, null);
        }
        // a2 : présélectionné puis refusé ; a4 : présélectionné ; a3 : reste "reçue"
        move(a2, hrToken, "SCREENING", null);
        move(a2, hrToken, "REJECTED", "Profile does not match");
        move(a4, hrToken, "SCREENING", null);

        // ---------- vue globale (paramètres par défaut) ----------
        JsonNode stats = api.send(get("/api/v1/dashboard/stats")
                .header("Authorization", bearer(hrToken)), 200);

        JsonNode summary = stats.get("summary");
        assertThat(summary.get("totalApplications").asInt()).isEqualTo(4);
        assertThat(summary.get("hired").asInt()).isEqualTo(1);
        assertThat(summary.get("rejected").asInt()).isEqualTo(1);
        assertThat(summary.get("inProgress").asInt()).isEqualTo(2);
        assertThat(summary.get("openJobs").asInt()).isEqualTo(2);

        assertThat(stageCount(stats.get("currentStages"), "RECEIVED")).isEqualTo(1);
        assertThat(stageCount(stats.get("currentStages"), "SCREENING")).isEqualTo(1);
        assertThat(stageCount(stats.get("currentStages"), "REJECTED")).isEqualTo(1);
        assertThat(stageCount(stats.get("currentStages"), "HIRED")).isEqualTo(1);
        assertThat(stageCount(stats.get("currentStages"), "OFFER")).isZero();

        // entonnoir : nombre de candidatures ayant ATTEINT chaque étape
        JsonNode funnel = stats.get("funnel");
        assertThat(stageCount(funnel, "RECEIVED")).isEqualTo(4);
        assertThat(stageCount(funnel, "SCREENING")).isEqualTo(3);
        assertThat(stageCount(funnel, "HR_INTERVIEW")).isEqualTo(1);
        assertThat(stageCount(funnel, "TECH_INTERVIEW")).isEqualTo(1);
        assertThat(stageCount(funnel, "OFFER")).isEqualTo(1);
        assertThat(stageCount(funnel, "HIRED")).isEqualTo(1);
        JsonNode screening = funnelStep(funnel, "SCREENING");
        assertThat(screening.get("percentOfReceived").asDouble()).isEqualTo(75.0);
        assertThat(screening.get("percentOfPrevious").asDouble()).isEqualTo(75.0);
        assertThat(funnelStep(funnel, "HR_INTERVIEW").get("percentOfPrevious").asDouble()).isEqualTo(33.3);
        assertThat(funnelStep(funnel, "RECEIVED").get("percentOfPrevious").isNull()).isTrue();

        // sources et offres
        Map<String, Integer> sources = new HashMap<>();
        for (JsonNode s : stats.get("bySource")) {
            sources.put(s.get("label").asText(), s.get("count").asInt());
        }
        assertThat(sources).containsEntry("linkedin", 2).containsEntry("referral", 1).containsEntry("unknown", 1);
        assertThat(stats.get("topJobs").get(0).get("title").asText()).isEqualTo("Job A " + tag);
        assertThat(stats.get("topJobs").get(0).get("count").asInt()).isEqualTo(3);

        // candidatures dans le temps : la somme des points = le total
        int sum = 0;
        for (JsonNode point : stats.get("applicationsOverTime")) {
            sum += point.get("count").asInt();
        }
        assertThat(sum).isEqualTo(4);

        // délai d'embauche : une embauche, quasi immédiate dans ce test
        JsonNode timeToHire = stats.get("timeToHire");
        assertThat(timeToHire.get("hiredCount").asInt()).isEqualTo(1);
        assertThat(timeToHire.get("averageDays").asDouble()).isBetween(0.0, 1.0);

        // ---------- filtre par offre ----------
        JsonNode onlyB = api.send(get("/api/v1/dashboard/stats").param("jobOfferId", String.valueOf(jobB))
                .header("Authorization", bearer(hrToken)), 200);
        assertThat(onlyB.get("summary").get("totalApplications").asInt()).isEqualTo(1);
        assertThat(stageCount(onlyB.get("funnel"), "SCREENING")).isEqualTo(1);
        assertThat(onlyB.get("bySource").get(0).get("label").asText()).isEqualTo("referral");

        // ---------- période sans candidature ----------
        Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
        JsonNode empty = api.send(get("/api/v1/dashboard/stats")
                .param("from", future.toString())
                .param("to", future.plus(1, ChronoUnit.DAYS).toString())
                .header("Authorization", bearer(hrToken)), 200);
        assertThat(empty.get("summary").get("totalApplications").asInt()).isZero();
        assertThat(stageCount(empty.get("funnel"), "RECEIVED")).isZero();
        assertThat(funnelStep(empty.get("funnel"), "SCREENING").get("percentOfReceived").asDouble()).isZero();
        assertThat(empty.get("timeToHire").get("averageDays").isNull()).isTrue();

        // ---------- droits et validation ----------
        String managerToken = api.login(newStaff(Role.MANAGER).getEmail(), STAFF_PASSWORD);
        api.send(get("/api/v1/dashboard/stats").header("Authorization", bearer(managerToken)), 403);
        api.send(get("/api/v1/dashboard/stats").header("Authorization", bearer(t1)), 403);
        api.send(get("/api/v1/dashboard/stats"), 401);
        api.send(get("/api/v1/dashboard/stats")
                .param("from", Instant.now().toString())
                .param("to", Instant.now().minus(1, ChronoUnit.DAYS).toString())
                .header("Authorization", bearer(hrToken)), 400);
        api.send(get("/api/v1/dashboard/stats")
                .param("from", Instant.now().minus(400, ChronoUnit.DAYS).toString())
                .param("to", Instant.now().toString())
                .header("Authorization", bearer(hrToken)), 400);
    }

    // ------------------------------------------------------------------ helpers

    private long publishedJob(String hrToken, String title) throws Exception {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("title", title);
        job.put("description", "Spring Boot");
        job.put("contractType", "CDI");
        long id = api.send(api.json(post("/api/v1/jobs"), job)
                .header("Authorization", bearer(hrToken)), 201).get("id").asLong();
        api.send(api.json(patch("/api/v1/jobs/{id}/status", id), Map.of("status", "PUBLISHED"))
                .header("Authorization", bearer(hrToken)), 200);
        return id;
    }

    private long apply(long jobId, String candidateToken, String source) throws Exception {
        MockHttpServletRequestBuilder request = multipart("/api/v1/jobs/{id}/applications", jobId)
                .file(new MockMultipartFile("cv", "cv.pdf", "application/pdf", PDF))
                .header("Authorization", bearer(candidateToken));
        if (source != null) {
            request.param("source", source);
        }
        return api.send(request, 201).get("id").asLong();
    }

    private void move(long appId, String hrToken, String stage, String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stage", stage);
        if (reason != null) {
            body.put("reason", reason);
        }
        api.send(api.json(patch("/api/v1/applications/{id}/stage", appId), body)
                .header("Authorization", bearer(hrToken)), 200);
    }

    private static int stageCount(JsonNode array, String stage) {
        for (JsonNode node : array) {
            if (stage.equals(node.get("stage").asText())) {
                return node.get("count").asInt();
            }
        }
        throw new AssertionError("Stage not found: " + stage);
    }

    private static JsonNode funnelStep(JsonNode funnel, String stage) {
        for (JsonNode node : funnel) {
            if (stage.equals(node.get("stage").asText())) {
                return node;
            }
        }
        throw new AssertionError("Funnel step not found: " + stage);
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
