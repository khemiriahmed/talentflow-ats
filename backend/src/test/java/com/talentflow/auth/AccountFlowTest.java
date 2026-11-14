package com.talentflow.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.RecordingEmailService;
import com.talentflow.RecordingEmailService.SentEmail;
import com.talentflow.TestEmailConfig;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.require-email-verification=false")
@AutoConfigureMockMvc
@Testcontainers
@Import(TestEmailConfig.class)
class AccountFlowTest {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository users;
    @Autowired
    RecordingEmailService emails;

    @Test
    void profileCanBeUpdated() throws Exception {
        String email = uniqueEmail();
        String access = register(email);

        JsonNode updated = send(patch("/api/v1/auth/me")
                .header("Authorization", bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "firstName", "Camille",
                        "lastName", "Martin",
                        "phone", "+33 6 12 34 56 78",
                        "linkedinUrl", "https://www.linkedin.com/in/camille",
                        "photoUrl", "https://example.com/photo.jpg"))), 200);

        assertThat(updated.get("firstName").asText()).isEqualTo("Camille");
        assertThat(updated.get("phone").asText()).isEqualTo("+33 6 12 34 56 78");
        assertThat(updated.get("linkedinUrl").asText()).contains("linkedin.com");
        assertThat(updated.get("emailVerified").asBoolean()).isFalse();
    }

    @Test
    void emailVerificationLinkMarksAccountAsVerified() throws Exception {
        String email = uniqueEmail();
        register(email);

        String token = awaitToken(email, m -> m.html().contains("/verifier-email"));
        JsonNode verified = send(jsonPost("/api/v1/auth/verify-email", Map.of("token", token)), 200);
        assertThat(verified.get("emailVerified").asBoolean()).isTrue();

        send(jsonPost("/api/v1/auth/verify-email", Map.of("token", token)), 400);
    }

    @Test
    void passwordResetReplacesTheOldPassword() throws Exception {
        String email = uniqueEmail();
        register(email);

        send(jsonPost("/api/v1/auth/forgot-password", Map.of("email", email)), 202);
        String token = awaitToken(email, m -> m.html().contains("/reinitialiser-mot-de-passe"));

        send(jsonPost("/api/v1/auth/reset-password",
                Map.of("token", token, "password", "NewPassword456!")), 204);

        send(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", "Password123!")), 401);
        send(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", "NewPassword456!")), 200);

        send(jsonPost("/api/v1/auth/reset-password",
                Map.of("token", token, "password", "AnotherPassword789!")), 400);
    }

    @Test
    void unknownForgotPasswordEmailGetsTheSameNeutralResponse() throws Exception {
        send(jsonPost("/api/v1/auth/forgot-password", Map.of("email", uniqueEmail())), 202);
    }

    @Test
    void candidateCanDeleteTheirAccount() throws Exception {
        String email = uniqueEmail();
        String access = register(email);

        send(delete("/api/v1/auth/me").header("Authorization", bearer(access)), 204);

        send(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", "Password123!")), 401);

        User anonymized = users.findAll().stream()
                .filter(user -> user.getEmail().startsWith("deleted-"))
                .filter(user -> !user.isEnabled())
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(anonymized.getFirstName()).isEqualTo("Compte");
        assertThat(anonymized.getLastName()).isEqualTo("supprimé");
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        String email = uniqueEmail();
        register(email);
        User user = users.findByEmail(email).orElseThrow();
        user.setEnabled(false);
        users.save(user);

        send(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", "Password123!")), 403);
    }

    @Test
    void staffDirectoryAndAuditAreRestricted() throws Exception {
        String candidateAccess = register(uniqueEmail());
        send(get("/api/v1/staff").header("Authorization", bearer(candidateAccess)), 403);
        send(get("/api/v1/admin/audit").header("Authorization", bearer(candidateAccess)), 403);
    }

    // ---- helpers ----

    private String register(String email) throws Exception {
        return send(jsonPost("/api/v1/auth/register", registerBody(email)), 201)
                .get("accessToken").asText();
    }

    private String awaitToken(String email, Predicate<SentEmail> condition) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(emails.to(email)).anyMatch(condition));
        SentEmail mail = emails.to(email).stream().filter(condition).findFirst().orElseThrow();
        Matcher matcher = TOKEN.matcher(mail.html());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private MockHttpServletRequestBuilder jsonPost(String url, Object body) throws Exception {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    private JsonNode send(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(body);
    }

    private static Map<String, String> registerBody(String email) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", "Password123!");
        body.put("firstName", "Test");
        body.put("lastName", "User");
        return body;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "account-" + UUID.randomUUID() + "@test.local";
    }
}
