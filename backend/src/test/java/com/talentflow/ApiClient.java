package com.talentflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Petit client MockMvc partagé par les tests d'intégration. */
public class ApiClient {

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;

    public ApiClient(MockMvc mvc, ObjectMapper objectMapper) {
        this.mvc = mvc;
        this.objectMapper = objectMapper;
    }

    public JsonNode send(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(body);
    }

    public MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    public String login(String email, String password) throws Exception {
        return send(json(post("/api/v1/auth/login"), Map.of("email", email, "password", password)), 200)
                .get("accessToken").asText();
    }

    public String register(String email) throws Exception {
        return send(json(post("/api/v1/auth/register"), Map.of(
                "email", email, "password", "Password123!",
                "firstName", "Cand", "lastName", "Idate")), 201).get("accessToken").asText();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static int total(JsonNode page) {
        return page.get("page").get("totalElements").asInt();
    }
}
