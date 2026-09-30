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
class StaffControllerTest {

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
    @DisplayName("STF-001: List staff profiles")
    void testListStaff_Success() throws Exception {
        profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("David")
                .lastName("Staff")
                .email("david@example.com")
                .profileType(ProfileType.STAFF)
                .status(ProfileStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/staff")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].firstName", is("David")));
    }

    @Test
    @DisplayName("STF-002: Create staff profile (201 Created)")
    void testCreateStaff_Success() throws Exception {
        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("David")
                .lastName("Staff")
                .email("david@example.com")
                .phone("0778889999")
                .build();

        mockMvc.perform(post("/api/v1/staff")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("APARTMENT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(USER_ID)))
                .andExpect(jsonPath("$.data.firstName", is("David")));

        verify(identityClient, times(1)).validateUser(USER_ID);
    }

    @Test
    @DisplayName("STF-002: Duplicate staff returns 409 STAFF_ALREADY_EXISTS")
    void testCreateStaff_Duplicate() throws Exception {
        profileRepository.save(Profile.builder()
                .userId(USER_ID)
                .firstName("David")
                .lastName("Staff")
                .email("david@example.com")
                .profileType(ProfileType.STAFF)
                .status(ProfileStatus.ACTIVE)
                .build());

        doNothing().when(identityClient).validateUser(USER_ID);

        CreateProfileRequest request = CreateProfileRequest.builder()
                .userId(USER_ID)
                .firstName("David")
                .lastName("Staff")
                .email("david@example.com")
                .build();

        mockMvc.perform(post("/api/v1/staff")
                        .with(TestJwtHelper.userJwt(ADMIN_ID, List.of("SYSTEM_ADMINISTRATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("STAFF_ALREADY_EXISTS")));
    }
}
