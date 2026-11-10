package com.talentflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.ApiClient;
import com.talentflow.TestEmailConfig;
import com.talentflow.audit.AuditLogRepository;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import({ApplicationFlowTest.TestStorageConfig.class, TestEmailConfig.class})
class TagsExportFlowTest {

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
    AuditLogRepository auditLogRepository;

    ApiClient api;

    @BeforeEach
    void init() {
        api = new ApiClient(mvc, objectMapper);
    }

    @Test
    void tagsFiltersAndCsvExport() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String hrToken = api.login(newStaff(Role.HR).getEmail(), STAFF_PASSWORD);
        String managerToken = api.login(newStaff(Role.MANAGER).getEmail(), STAFF_PASSWORD);
        long jobId = publishedJob(hrToken, "Tagged job " + tag);

        String c1Email = "ana-" + tag + "@test.local";
        String c2Email = "evil-" + tag + "@test.local";
        String c1Token = register(c1Email, "Ana", "Zed");
        String c2Token = register(c2Email, "=SUM(1+1)", "Tester");   // tentative d'injection de formule
        long app1 = apply(jobId, c1Token, "linkedin");
        long app2 = apply(jobId, c2Token, null);

        // ---------- tags : normalisation, droits, validation ----------
        JsonNode tagged = api.send(api.json(put("/api/v1/applications/{id}/tags", app1),
                Map.of("tags", List.of("Java", "  Spring   Boot ", "java")))
                .header("Authorization", bearer(hrToken)), 200);
        Set<String> tags = new HashSet<>();
        tagged.get("tags").forEach(t -> tags.add(t.asText()));
        assertThat(tags).containsExactlyInAnyOrder("java", "spring boot");

        api.send(api.json(put("/api/v1/applications/{id}/tags", app1), Map.of("tags", List.of("<script>")))
                .header("Authorization", bearer(hrToken)), 400);
        api.send(api.json(put("/api/v1/applications/{id}/tags", app1),
                Map.of("tags", List.of("t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10", "t11")))
                .header("Authorization", bearer(hrToken)), 400);
        api.send(api.json(put("/api/v1/applications/{id}/tags", app1), Map.of("tags", List.of("x")))
                .header("Authorization", bearer(managerToken)), 403);
        api.send(api.json(put("/api/v1/applications/{id}/tags", app1), Map.of("tags", List.of("x")))
                .header("Authorization", bearer(c1Token)), 403);

        // les tags sont internes : invisibles pour le candidat, visibles pour la RH
        assertThat(api.send(get("/api/v1/applications/{id}", app1)
                .header("Authorization", bearer(c1Token)), 200).has("tags")).isFalse();
        assertThat(api.send(get("/api/v1/applications/{id}", app1)
                .header("Authorization", bearer(hrToken)), 200).get("tags").size()).isEqualTo(2);

        // ---------- filtres de la liste ----------
        assertThat(listTotal(hrToken, jobId, "tag", "JAVA")).isEqualTo(1);          // insensible à la casse
        assertThat(listTotal(hrToken, jobId, "tag", "cobol")).isZero();
        assertThat(listTotal(hrToken, jobId, "q", "ana-" + tag)).isEqualTo(1);
        assertThat(listTotal(hrToken, jobId, "from", Instant.now().minus(1, ChronoUnit.DAYS).toString()))
                .isEqualTo(2);
        assertThat(listTotal(hrToken, jobId, "from", Instant.now().plus(1, ChronoUnit.DAYS).toString())).isZero();
        assertThat(listTotal(hrToken, jobId, "to", Instant.now().minus(1, ChronoUnit.DAYS).toString())).isZero();

        // ---------- export CSV ----------
        String csv = export(hrToken, jobId, null, null);
        assertThat(csv).startsWith("\uFEFF");                                      // BOM pour Excel
        assertThat(csv).contains("id;jobOfferId;jobTitle;candidateName;candidateEmail;stage;source;tags;createdAt");
        assertThat(csv).contains(c1Email).contains(c2Email);
        assertThat(csv).contains("java, spring boot");
        assertThat(csv).contains("'=SUM(1+1)");                                    // formule neutralisée
        assertThat(csv).doesNotContain(";=SUM(1+1)");
        assertThat(csv.strip().split("\r\n")).hasSize(3);                           // en-tête + 2 lignes

        String filtered = export(hrToken, jobId, "tag", "java");
        assertThat(filtered.strip().split("\r\n")).hasSize(2);
        assertThat(filtered).contains(c1Email).doesNotContain(c2Email);

        api.send(get("/api/v1/applications/export").header("Authorization", bearer(managerToken)), 403);
        api.send(get("/api/v1/applications/export").header("Authorization", bearer(c1Token)), 403);
        api.send(get("/api/v1/applications/export"), 401);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "APPLICATIONS_EXPORTED".equals(log.getAction()));

        // l'application n°2 n'a pas de tags : colonne vide, pas d'erreur
        assertThat(app2).isNotEqualTo(app1);
    }

    // ------------------------------------------------------------------ helpers

    private int listTotal(String hrToken, long jobId, String param, String value) throws Exception {
        return total(api.send(get("/api/v1/applications")
                .param("jobOfferId", String.valueOf(jobId)).param(param, value)
                .header("Authorization", bearer(hrToken)), 200));
    }

    private String export(String hrToken, long jobId, String param, String value) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/applications/export")
                .param("jobOfferId", String.valueOf(jobId))
                .header("Authorization", bearer(hrToken));
        if (param != null) {
            request.param(param, value);
        }
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        assertThat(MediaType.parseMediaType(result.getResponse().getContentType()).getSubtype()).isEqualTo("csv");
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

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

    private String register(String email, String firstName, String lastName) throws Exception {
        return api.send(api.json(post("/api/v1/auth/register"), Map.of(
                "email", email, "password", "Password123!",
                "firstName", firstName, "lastName", lastName)), 201).get("accessToken").asText();
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
