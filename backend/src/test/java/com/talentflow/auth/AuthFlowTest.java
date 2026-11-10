package com.talentflow.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowTest {

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
    PasswordEncoder passwordEncoder;

    @Test
    void registerLoginAndReadProfile() throws Exception {
        String email = uniqueEmail();
        JsonNode registered = send(jsonPost("/api/v1/auth/register", registerBody(email)), 201);
        assertThat(registered.get("accessToken").asText()).isNotBlank();
        assertThat(registered.get("user").get("role").asText()).isEqualTo("CANDIDATE");

        JsonNode login = send(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", "Password123!")), 200);
        String access = login.get("accessToken").asText();

        JsonNode me = send(get("/api/v1/auth/me").header("Authorization", "Bearer " + access), 200);
        assertThat(me.get("email").asText()).isEqualTo(email);
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        send(jsonPost("/api/v1/auth/register", registerBody(email)), 201);
        send(jsonPost("/api/v1/auth/register", registerBody(email)), 409);
    }

    @Test
    void invalidInputAndBadCredentialsAreRejected() throws Exception {
        JsonNode invalid = send(jsonPost("/api/v1/auth/register",
                Map.of("email", "not-an-email", "password", "short",
                        "firstName", "A", "lastName", "B")), 400);
        assertThat(invalid.get("fieldErrors").has("email")).isTrue();

        String email = uniqueEmail();
        send(jsonPost("/api/v1/auth/register", registerBody(email)), 201);
        send(jsonPost("/api/v1/auth/login", Map.of("email", email, "password", "wrong-password")), 401);
    }

    @Test
    void protectedEndpointsRequireAValidToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenFlow() throws Exception {
        String email = uniqueEmail();
        JsonNode auth = send(jsonPost("/api/v1/auth/register", registerBody(email)), 201);

        // un access token ne doit pas être accepté comme refresh token
        send(jsonPost("/api/v1/auth/refresh",
                Map.of("refreshToken", auth.get("accessToken").asText())), 401);

        JsonNode refreshed = send(jsonPost("/api/v1/auth/refresh",
                Map.of("refreshToken", auth.get("refreshToken").asText())), 200);
        assertThat(refreshed.get("accessToken").asText()).isNotBlank();
    }

    @Test
    void adminEndpointsAreRestrictedToAdmins() throws Exception {
        String candidateToken = send(jsonPost("/api/v1/auth/register", registerBody(uniqueEmail())), 201)
                .get("accessToken").asText();
        send(get("/api/v1/admin/users").header("Authorization", "Bearer " + candidateToken), 403);

        String adminEmail = uniqueEmail();
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode("Admin12345!"));
        admin.setFirstName("Admin");
        admin.setLastName("Test");
        admin.setRole(Role.ADMIN);
        users.saveAndFlush(admin);

        String adminToken = send(jsonPost("/api/v1/auth/login",
                Map.of("email", adminEmail, "password", "Admin12345!")), 200)
                .get("accessToken").asText();

        JsonNode page = send(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken), 200);
        assertThat(page.get("content").isArray()).isTrue();

        JsonNode created = send(jsonPost("/api/v1/admin/users", Map.of(
                        "email", uniqueEmail(), "password", "Password123!",
                        "firstName", "Hana", "lastName", "HR", "role", "HR"))
                .header("Authorization", "Bearer " + adminToken), 201);
        assertThat(created.get("role").asText()).isEqualTo("HR");
    }

    // ---- helpers ----

    private MockHttpServletRequestBuilder jsonPost(String url, Object body) throws Exception {
        return post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private JsonNode send(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(body);
    }

    private static Map<String, String> registerBody(String email) {
        return Map.of("email", email, "password", "Password123!",
                "firstName", "Test", "lastName", "User");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }
}
