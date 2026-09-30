package kln.ams.residentmanagement.controller;

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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResidentInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
    }

    @Test
    @DisplayName("RES-INT-001: Allowed service can validate resident (200 OK)")
    void testValidateResident_Success() throws Exception {
        Profile p = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/internal/residents/" + p.getId() + "/validate")
                        .with(TestJwtHelper.serviceJwt("operations-service")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.residentId", is(p.getId())))
                .andExpect(jsonPath("$.data.userId", is(USER_ID)))
                .andExpect(jsonPath("$.data.exists", is(true)))
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    @DisplayName("RES-INT-001: Unregistered / unauthorized service caller returns 403 CALLER_SERVICE_NOT_ALLOWED")
    void testValidateResident_CallerNotAllowed() throws Exception {
        Profile p = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/internal/residents/" + p.getId() + "/validate")
                        .with(TestJwtHelper.serviceJwt("unregistered-third-party-service")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("CALLER_SERVICE_NOT_ALLOWED")));
    }

    @Test
    @DisplayName("RES-INT-001: User JWT sent to internal endpoint returns 401 INVALID_SERVICE_TOKEN")
    void testValidateResident_UserTokenRejected() throws Exception {
        mockMvc.perform(get("/api/v1/internal/residents/" + UUID.randomUUID() + "/validate")
                        .with(TestJwtHelper.userJwt(USER_ID)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_SERVICE_TOKEN")));
    }

    @Test
    @DisplayName("RES-INT-001: Non-existent resident returns 404 RESIDENT_NOT_FOUND")
    void testValidateResident_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/internal/residents/" + UUID.randomUUID() + "/validate")
                        .with(TestJwtHelper.serviceJwt("billing-payment-service")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESIDENT_NOT_FOUND")));
    }

    @Test
    @DisplayName("RES-INT-002: Allowed service can get user relationships (200 OK)")
    void testGetUserRelationships_Success() throws Exception {
        Profile p1 = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        Profile p2 = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .profileType(ProfileType.OWNER)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/internal/users/" + USER_ID + "/relationships")
                        .with(TestJwtHelper.serviceJwt("operations-service")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(USER_ID)))
                .andExpect(jsonPath("$.data.relationships", hasSize(2)))
                .andExpect(jsonPath("$.data.relationships[0].profileType", anyOf(is("RESIDENT"), is("OWNER"))))
                .andExpect(jsonPath("$.data.relationships[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("RES-INT-002: User not found returns 404 USER_NOT_FOUND")
    void testGetUserRelationships_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + UUID.randomUUID() + "/relationships")
                        .with(TestJwtHelper.serviceJwt("community-service")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("USER_NOT_FOUND")));
    }
}
