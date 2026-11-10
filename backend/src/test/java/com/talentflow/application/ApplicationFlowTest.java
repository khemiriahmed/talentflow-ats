package com.talentflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.common.ResourceNotFoundException;
import com.talentflow.storage.StorageService;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(ApplicationFlowTest.TestStorageConfig.class)
class ApplicationFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static final byte[] PDF = "%PDF-1.4\n%fake pdf for tests\n".getBytes(StandardCharsets.UTF_8);
    static final String STAFF_PASSWORD = "Staff12345!";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository users;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    InMemoryStorageService storage;

    @Test
    void jobsAndApplicationsFlow() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User hr = newStaff(Role.HR);
        User manager = newStaff(Role.MANAGER);
        User otherManager = newStaff(Role.MANAGER);
        String hrToken = login(hr.getEmail(), STAFF_PASSWORD);

        // --- création d'une offre (brouillon, invisible publiquement) ---
        Map<String, Object> jobBody = new LinkedHashMap<>();
        jobBody.put("title", "Backend Java " + tag);
        jobBody.put("description", "Spring Boot, PostgreSQL, Docker");
        jobBody.put("location", "Tunis");
        jobBody.put("contractType", "CDI");
        jobBody.put("managerId", manager.getId());
        JsonNode created = send(json(post("/api/v1/jobs"), jobBody).header("Authorization", bearer(hrToken)), 201);
        long jobId = created.get("id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");

        send(get("/api/v1/jobs/" + jobId), 404);
        assertThat(total(send(get("/api/v1/jobs").param("keyword", tag), 200))).isZero();

        // --- transition de statut invalide puis publication ---
        send(json(patch("/api/v1/jobs/{id}/status", jobId), Map.of("status", "CLOSED"))
                .header("Authorization", bearer(hrToken)), 400);
        send(json(patch("/api/v1/jobs/{id}/status", jobId), Map.of("status", "PUBLISHED"))
                .header("Authorization", bearer(hrToken)), 200);

        // --- vue publique (sans token), sans infos internes ---
        assertThat(total(send(get("/api/v1/jobs").param("keyword", tag), 200))).isEqualTo(1);
        JsonNode publicJob = send(get("/api/v1/jobs/" + jobId), 200);
        assertThat(publicJob.has("managerId")).isFalse();
        assertThat(publicJob.has("status")).isFalse();

        // --- mise à jour par la RH ---
        jobBody.put("title", "Backend Java " + tag + " v2");
        JsonNode updated = send(json(put("/api/v1/jobs/{id}", jobId), jobBody)
                .header("Authorization", bearer(hrToken)), 200);
        assertThat(updated.get("title").asText()).endsWith("v2");

        // --- candidatures ---
        String c1Token = register("c1-" + tag + "@test.local");
        String c2Token = register("c2-" + tag + "@test.local");
        int filesBefore = storage.size();

        send(cvUpload(jobId, PDF).header("Authorization", bearer(hrToken)), 403);
        send(cvUpload(jobId, "not a pdf".getBytes(StandardCharsets.UTF_8))
                .header("Authorization", bearer(c1Token)), 400);

        JsonNode application = send(cvUpload(jobId, PDF).param("coverLetter", "Motivated")
                .header("Authorization", bearer(c1Token)), 201);
        long appId = application.get("id").asLong();
        assertThat(application.get("currentStage").asText()).isEqualTo("RECEIVED");
        assertThat(application.has("cvFileKey")).isFalse();
        assertThat(storage.size()).isEqualTo(filesBefore + 1);

        send(cvUpload(jobId, PDF).header("Authorization", bearer(c1Token)), 409);

        assertThat(total(send(get("/api/v1/applications/mine")
                .header("Authorization", bearer(c1Token)), 200))).isEqualTo(1);

        // --- contrôle d'accès ---
        send(get("/api/v1/applications/{id}", appId).header("Authorization", bearer(c1Token)), 200);
        send(get("/api/v1/applications/{id}", appId).header("Authorization", bearer(c2Token)), 404);
        send(get("/api/v1/applications").header("Authorization", bearer(c1Token)), 403);

        String managerToken = login(manager.getEmail(), STAFF_PASSWORD);
        String otherManagerToken = login(otherManager.getEmail(), STAFF_PASSWORD);
        assertThat(total(send(get("/api/v1/applications").param("jobOfferId", String.valueOf(jobId))
                .header("Authorization", bearer(hrToken)), 200))).isEqualTo(1);
        assertThat(total(send(get("/api/v1/applications").param("jobOfferId", String.valueOf(jobId))
                .header("Authorization", bearer(managerToken)), 200))).isEqualTo(1);
        assertThat(total(send(get("/api/v1/applications").param("jobOfferId", String.valueOf(jobId))
                .header("Authorization", bearer(otherManagerToken)), 200))).isZero();
        assertThat(total(send(get("/api/v1/applications").param("q", "c1-" + tag)
                .header("Authorization", bearer(hrToken)), 200))).isEqualTo(1);

        // --- téléchargement du CV ---
        MvcResult cv = mvc.perform(get("/api/v1/applications/{id}/cv", appId)
                        .header("Authorization", bearer(hrToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        assertThat(cv.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        send(get("/api/v1/applications/{id}/cv", appId).header("Authorization", bearer(c2Token)), 404);

        // --- suppression d'offre : refusée tant qu'il y a des candidatures ---
        send(delete("/api/v1/jobs/{id}", jobId).header("Authorization", bearer(hrToken)), 400);

        // --- retrait de la candidature : supprime aussi le CV ---
        send(delete("/api/v1/applications/{id}", appId).header("Authorization", bearer(c2Token)), 404);
        send(delete("/api/v1/applications/{id}", appId).header("Authorization", bearer(c1Token)), 204);
        assertThat(total(send(get("/api/v1/applications/mine")
                .header("Authorization", bearer(c1Token)), 200))).isZero();
        assertThat(storage.size()).isEqualTo(filesBefore);

        send(delete("/api/v1/jobs/{id}", jobId).header("Authorization", bearer(hrToken)), 204);
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private MockHttpServletRequestBuilder cvUpload(long jobId, byte[] content) {
        return multipart("/api/v1/jobs/{id}/applications", jobId)
                .file(new MockMultipartFile("cv", "cv.pdf", "application/pdf", content));
    }

    private JsonNode send(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(body);
    }

    private static int total(JsonNode page) {
        return page.get("page").get("totalElements").asInt();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
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

    private String login(String email, String password) throws Exception {
        return send(json(post("/api/v1/auth/login"), Map.of("email", email, "password", password)), 200)
                .get("accessToken").asText();
    }

    private String register(String email) throws Exception {
        return send(json(post("/api/v1/auth/register"), Map.of(
                "email", email, "password", "Password123!",
                "firstName", "Cand", "lastName", "Idate")), 201).get("accessToken").asText();
    }

    // ---------- stockage en mémoire pour les tests ----------

    static class InMemoryStorageService implements StorageService {
        private final Map<String, byte[]> files = new ConcurrentHashMap<>();

        @Override
        public void upload(String key, byte[] content, String contentType) {
            files.put(key, content);
        }

        @Override
        public byte[] download(String key) {
            byte[] data = files.get(key);
            if (data == null) {
                throw new ResourceNotFoundException("File not found");
            }
            return data;
        }

        @Override
        public void delete(String key) {
            files.remove(key);
        }

        int size() {
            return files.size();
        }
    }

    @TestConfiguration
    static class TestStorageConfig {
        @Bean
        @Primary
        InMemoryStorageService inMemoryStorageService() {
            return new InMemoryStorageService();
        }
    }
}
