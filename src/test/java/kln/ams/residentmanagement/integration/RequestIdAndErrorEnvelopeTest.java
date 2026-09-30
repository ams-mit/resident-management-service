package kln.ams.residentmanagement.integration;

import kln.ams.residentmanagement.entity.Profile;
import kln.ams.residentmanagement.entity.ProfileStatus;
import kln.ams.residentmanagement.entity.ProfileType;
import kln.ams.residentmanagement.repository.ProfileRepository;
import kln.ams.residentmanagement.util.TestJwtHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestIdAndErrorEnvelopeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    private static final String ADMIN_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
    }

    @Test
    @DisplayName("Propagates supplied X-Request-ID to response header and response envelope")
    void testSuppliedRequestId() throws Exception {
        String customRequestId = "test-request-id-" + UUID.randomUUID();

        Profile p = profileRepository.save(Profile.builder()
                .userId(UUID.randomUUID().toString())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/residents/" + p.getId())
                        .header("X-Request-ID", customRequestId)
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", customRequestId))
                .andExpect(jsonPath("$.requestId", is(customRequestId)))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Generates new UUID X-Request-ID when missing from request and sets in response header and envelope")
    void testGeneratedRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andReturn();

        String generatedHeader = result.getResponse().getHeader("X-Request-ID");
        assertNotNull(generatedHeader);
        // Verify valid UUID format
        UUID.fromString(generatedHeader);
    }

    @Test
    @DisplayName("Exception responses preserve X-Request-ID in header and error envelope")
    void testExceptionResponseContainsRequestId() throws Exception {
        String customRequestId = "err-request-id-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/residents/00000000-0000-0000-0000-000000000000")
                        .header("X-Request-ID", customRequestId)
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-ID", customRequestId))
                .andExpect(jsonPath("$.requestId", is(customRequestId)))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESIDENT_NOT_FOUND")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("401 error envelope conforms to global API standard")
    void test401ErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/residents"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_TOKEN")))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("403 error envelope conforms to global API standard")
    void test403ErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(UUID.randomUUID().toString(), List.of("TENANT_RESIDENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("PERMISSION_DENIED")))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
