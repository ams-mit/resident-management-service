package com.ams.resident.integration;

import com.ams.resident.config.AbstractIntegrationTest;
import com.ams.resident.util.TestJwtHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200ForLivenessWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldReturn200ForReadinessWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldNotExposeEnvEndpoint() throws Exception {
        // Unauthenticated access is rejected (401)
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());

        // Even when authenticated, endpoint is not exposed by actuator (404)
        mockMvc.perform(get("/actuator/env")
                        .with(TestJwtHelper.userJwt("user123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotExposeBeansEndpoint() throws Exception {
        // Unauthenticated access is rejected (401)
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());

        // Even when authenticated, endpoint is not exposed by actuator (404)
        mockMvc.perform(get("/actuator/beans")
                        .with(TestJwtHelper.userJwt("user123")))
                .andExpect(status().isNotFound());
    }
}
