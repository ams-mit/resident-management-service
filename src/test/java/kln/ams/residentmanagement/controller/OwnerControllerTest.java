package kln.ams.residentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kln.ams.residentmanagement.dto.CreateProfileRequest;
import kln.ams.residentmanagement.entity.Profile;
import kln.ams.residentmanagement.entity.ProfileStatus;
import kln.ams.residentmanagement.entity.ProfileType;
import kln.ams.residentmanagement.repository.ProfileRepository;
import kln.ams.residentmanagement.service.IdentityClient;
import kln.ams.residentmanagement.util.TestJwtHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityClient identityClient;

    private static final String ADMIN_ID = UUID.randomUUID().toString();
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        reset(identityClient);
    }

    @Test
    @DisplayName("OWN-001: List owners with pagination")
    void testListOwners_Success() throws Exception {
        profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("Bob")
                .lastName("Owner")
                .email("bob@example.com")
                .profileType(ProfileType.OWNER)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/owners")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].firstName", is("Bob")));
    }

    @Test
    @DisplayName("OWN-002: Create owner profile (201 Created)")
    void testCreateOwner_Success() throws Exception {
        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Bob")
                .lastName("Owner")
                .email("bob@example.com")
                .phone("0775556666")
                .build();

        mockMvc.perform(post("/api/v1/owners")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(USER_ID)))
                .andExpect(jsonPath("$.data.firstName", is("Bob")));

        verify(identityClient, times(1)).validateUser(USER_ID);
    }

    @Test
    @DisplayName("OWN-002: Duplicate owner returns 409 OWNER_ALREADY_EXISTS")
    void testCreateOwner_Duplicate() throws Exception {
        profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("Bob")
                .lastName("Owner")
                .email("bob@example.com")
                .profileType(ProfileType.OWNER)
                .status(ProfileStatus.ACTIVE)
                .build());

        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Bob")
                .lastName("Owner")
                .email("bob@example.com")
                .build();

        mockMvc.perform(post("/api/v1/owners")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("OWNER_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("OWN-003: Get owner profile by ID")
    void testGetOwner_Success() throws Exception {
        Profile p = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("Bob")
                .lastName("Owner")
                .email("bob@example.com")
                .profileType(ProfileType.OWNER)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/owners/" + p.getId())
                        .with(TestJwtHelper.userJwt(USER_ID, List.of("OWNER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(p.getId())))
                .andExpect(jsonPath("$.data.firstName", is("Bob")));
    }

    @Test
    @DisplayName("OWN-003: Get non-existent owner returns 404 OWNER_NOT_FOUND")
    void testGetOwner_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/owners/" + UUID.randomUUID())
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("OWNER_NOT_FOUND")));
    }
}
