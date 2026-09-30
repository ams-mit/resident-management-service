package kln.ams.residentmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kln.ams.residentmanagement.dto.CreateProfileRequest;
import kln.ams.residentmanagement.dto.UpdateResidentRequest;
import kln.ams.residentmanagement.entity.Profile;
import kln.ams.residentmanagement.entity.ProfileStatus;
import kln.ams.residentmanagement.entity.ProfileType;
import kln.ams.residentmanagement.exception.DependencyUnavailableException;
import kln.ams.residentmanagement.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResidentControllerTest {

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
    @DisplayName("RES-001: Admin can list residents with pagination and envelope")
    void testListResidents_Success() throws Exception {
        Profile p1 = profileRepository.save(Profile.builder()
                .userId(UUID.randomUUID().toString())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("0771234567")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id", is(p1.getId())))
                .andExpect(jsonPath("$.data.items[0].firstName", is("John")))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("RES-001: Unauthorized role is forbidden (403 PERMISSION_DENIED)")
    void testListResidents_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(USER_ID, List.of("TENANT_RESIDENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("PERMISSION_DENIED")));
    }

    @Test
    @DisplayName("RES-001: Invalid page size returns 400 VALIDATION_ERROR")
    void testListResidents_InvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER")))
                        .param("size", "150"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("RES-002: Admin can create resident (201 Created)")
    void testCreateResident_Success() throws Exception {
        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .phone("0719876543")
                .build();

        mockMvc.perform(post("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.userId", is(USER_ID)))
                .andExpect(jsonPath("$.data.firstName", is("Alice")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        verify(identityClient, times(1)).validateUser(USER_ID);
    }

    @Test
    @DisplayName("RES-002: Create resident with missing required fields returns 400 VALIDATION_ERROR")
    void testCreateResident_ValidationError() throws Exception {
        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId("not-a-uuid")
                .firstName("")
                .lastName("")
                .email("bad-email")
                .build();

        mockMvc.perform(post("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("RES-002: Create duplicate resident returns 409 RESIDENT_ALREADY_EXISTS")
    void testCreateResident_Duplicate() throws Exception {
        profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("Existing")
                .lastName("Resident")
                .email("existing@example.com")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .build();

        mockMvc.perform(post("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESIDENT_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("RES-002: User not found in Identity Access returns 404 USER_NOT_FOUND")
    void testCreateResident_UserNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("USER_NOT_FOUND", "User not found in Identity Access"))
                .when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .build();

        mockMvc.perform(post("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("USER_NOT_FOUND")));
    }

    @Test
    @DisplayName("RES-002: Identity service down returns 503 DEPENDENCY_UNAVAILABLE")
    void testCreateResident_IdentityUnavailable() throws Exception {
        doThrow(new DependencyUnavailableException("identity-access-service", "Identity service is down"))
                .when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .build();

        mockMvc.perform(post("/api/v1/residents")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DEPENDENCY_UNAVAILABLE")))
                .andExpect(jsonPath("$.error.details.service", is("identity-access-service")));
    }

    @Test
    @DisplayName("RES-003: Get resident by ID (Admin and Owner)")
    void testGetResident_Success() throws Exception {
        Profile p = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        // Admin can view
        mockMvc.perform(get("/api/v1/residents/" + p.getId())
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(p.getId())));

        // Self can view
        mockMvc.perform(get("/api/v1/residents/" + p.getId())
                        .with(TestJwtHelper.userJwt(USER_ID, List.of("TENANT_RESIDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(p.getId())));
    }

    @Test
    @DisplayName("RES-003: Another resident cannot view someone else's profile (403)")
    void testGetResident_OtherUserForbidden() throws Exception {
        Profile p = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        String otherUserId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/residents/" + p.getId())
                        .with(TestJwtHelper.userJwt(otherUserId, List.of("TENANT_RESIDENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("PERMISSION_DENIED")));
    }

    @Test
    @DisplayName("RES-003: Get non-existent resident returns 404 RESIDENT_NOT_FOUND")
    void testGetResident_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/residents/" + UUID.randomUUID())
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESIDENT_NOT_FOUND")));
    }

    @Test
    @DisplayName("RES-004: Update resident profile (200 OK)")
    void testUpdateResident_Success() throws Exception {
        Profile p = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("0771111111")
                .profileType(ProfileType.RESIDENT)
                .status(ProfileStatus.ACTIVE)
                .build());

        UpdateResidentRequest update = UpdateResidentRequest.builder()
                .firstName("Johnny")
                .email("johnny.new@example.com")
                .phone("0772222222")
                .build();

        mockMvc.perform(patch("/api/v1/residents/" + p.getId())
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.firstName", is("Johnny")))
                .andExpect(jsonPath("$.data.lastName", is("Doe")))
                .andExpect(jsonPath("$.data.email", is("johnny.new@example.com")))
                .andExpect(jsonPath("$.data.phone", is("0772222222")));
    }
}
