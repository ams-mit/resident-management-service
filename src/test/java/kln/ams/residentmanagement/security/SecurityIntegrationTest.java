package kln.ams.residentmanagement.security;

import kln.ams.residentmanagement.util.TestJwtHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated public request returns 401 INVALID_TOKEN")
    void testUnauthenticated_Public() throws Exception {
        mockMvc.perform(get("/api/v1/residents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_TOKEN")))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Unauthenticated internal request returns 401 INVALID_SERVICE_TOKEN")
    void testUnauthenticated_Internal() throws Exception {
        mockMvc.perform(get("/api/v1/internal/residents/" + UUID.randomUUID() + "/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_SERVICE_TOKEN")))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Service token on public endpoint returns 401 INVALID_TOKEN")
    void testServiceTokenOnPublicEndpoint_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .with(TestJwtHelper.serviceJwt("operations-service")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_TOKEN")));
    }

    @Test
    @DisplayName("User token on internal endpoint returns 401 INVALID_SERVICE_TOKEN")
    void testUserTokenOnInternalEndpoint_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/internal/residents/" + UUID.randomUUID() + "/validate")
                        .with(TestJwtHelper.userJwt(UUID.randomUUID().toString(), List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_SERVICE_TOKEN")));
    }
}
