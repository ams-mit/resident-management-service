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
class TenantControllerTest {

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
    @DisplayName("TEN-001: List tenants with pagination")
    void testListTenants_Success() throws Exception {
        profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("Charlie")
                .lastName("Tenant")
                .email("charlie@example.com")
                .profileType(ProfileType.TENANT)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/tenants")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].firstName", is("Charlie")));
    }

    @Test
    @DisplayName("TEN-002: Create tenant profile (201 Created)")
    void testCreateTenant_Success() throws Exception {
        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Charlie")
                .lastName("Tenant")
                .email("charlie@example.com")
                .phone("0773334444")
                .build();

        mockMvc.perform(post("/api/v1/tenants")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(USER_ID)))
                .andExpect(jsonPath("$.data.firstName", is("Charlie")));

        verify(identityClient, times(1)).validateUser(USER_ID);
    }

    @Test
    @DisplayName("TEN-002: Duplicate tenant returns 409 TENANT_ALREADY_EXISTS")
    void testCreateTenant_Duplicate() throws Exception {
        profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("Charlie")
                .lastName("Tenant")
                .email("charlie@example.com")
                .profileType(ProfileType.TENANT)
                .status(ProfileStatus.ACTIVE)
                .build());

        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("Charlie")
                .lastName("Tenant")
                .email("charlie@example.com")
                .build();

        mockMvc.perform(post("/api/v1/tenants")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("TENANT_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("TEN-003: Get tenant profile by ID")
    void testGetTenant_Success() throws Exception {
        Profile p = profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("Charlie")
                .lastName("Tenant")
                .email("charlie@example.com")
                .profileType(ProfileType.TENANT)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/tenants/" + p.getId())
                        .with(TestJwtHelper.userJwt(USER_ID, List.of("TENANT_RESIDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(p.getId())))
                .andExpect(jsonPath("$.data.firstName", is("Charlie")));
    }

    @Test
    @DisplayName("TEN-003: Get non-existent tenant returns 404 TENANT_NOT_FOUND")
    void testGetTenant_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + UUID.randomUUID())
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("TENANT_NOT_FOUND")));
    }
}
