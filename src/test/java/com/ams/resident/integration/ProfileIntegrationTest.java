package com.ams.resident.integration;

import com.ams.resident.client.IdentityClient;
import com.ams.resident.config.AbstractIntegrationTest;
import com.ams.resident.entity.ProfileType;
import com.ams.resident.entity.ResidentProfile;
import com.ams.resident.repository.ProfileRepository;
import com.ams.resident.service.AuditService;
import com.ams.resident.service.ProfileService;
import com.ams.resident.util.TestJwtHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private com.ams.resident.repository.AuditEventRepository auditEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityClient identityClient;

    private static final String TEST_USER_ID = "test-user-123";

    @Autowired
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        profileRepository.deleteAll();

        ResidentProfile profile = new ResidentProfile();
        profile.setUserId(TEST_USER_ID);
        profile.setProfileType(ProfileType.RESIDENT);
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setPhone("1234567890");
        profileRepository.save(profile);
    }

    @Test
    void shouldReturnProfileForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"));
    }

    @Test
    void shouldCreateBaselineProfileOnFirstAccess() throws Exception {
        String newUserId = "brand-new-user-123";

        mockMvc.perform(get("/api/v1/profiles/me")
                        .with(TestJwtHelper.userJwt(newUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(newUserId))
                .andExpect(jsonPath("$.data.statusInfo").value("ACTIVE"));

        // Verify Database Persistence
        var created = profileRepository.findByUserId(newUserId);
        org.junit.jupiter.api.Assertions.assertTrue(created.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(newUserId, created.get().getUserId());
    }

    @Test
    void shouldReturnSameProfileOnSecondAccess() throws Exception {
        String newUserId = "brand-new-user-456";

        // First call creates profile
        mockMvc.perform(get("/api/v1/profiles/me")
                        .with(TestJwtHelper.userJwt(newUserId)))
                .andExpect(status().isOk());

        var firstCreated = profileRepository.findByUserId(newUserId).orElseThrow();
        String initialId = firstCreated.getId();

        // Second call returns same profile (same id)
        mockMvc.perform(get("/api/v1/profiles/me")
                        .with(TestJwtHelper.userJwt(newUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(newUserId));

        var secondCreated = profileRepository.findByUserId(newUserId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(initialId, secondCreated.getId());
    }

    @Test
    void shouldCreateAndUpdateProfileOnPutWhenProfileDoesNotExist() throws Exception {
        String newUserId = "brand-new-user-789";
        String updatePayload = """
                {
                    "firstName": "Alice",
                    "lastName": "Smith",
                    "phone": "5551234567"
                }
                """;

        mockMvc.perform(put("/api/v1/profiles/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload)
                        .with(TestJwtHelper.userJwt(newUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(newUserId))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.phone").value("5551234567"));

        var profile = profileRepository.findByUserId(newUserId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Alice", profile.getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("Smith", profile.getLastName());
    }

    @Test
    void shouldHandleConcurrentDuplicateKeyOnCreation() {
        ProfileRepository mockRepo = org.mockito.Mockito.mock(ProfileRepository.class);
        AuditService mockAudit = org.mockito.Mockito.mock(AuditService.class);
        IdentityClient mockIdentity = org.mockito.Mockito.mock(IdentityClient.class);
        com.ams.resident.repository.EmailChangeRequestRepository mockEmailRepo = org.mockito.Mockito.mock(com.ams.resident.repository.EmailChangeRequestRepository.class);
        ProfileService service = new ProfileService(mockRepo, mockEmailRepo, mockAudit, mockIdentity);

        String userId = "concurrent-user";
        ResidentProfile existingProfile = new ResidentProfile();
        existingProfile.setId("existing-id-123");
        existingProfile.setUserId(userId);

        org.mockito.Mockito.when(mockRepo.findByUserId(userId))
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.of(existingProfile));

        org.mockito.Mockito.when(mockRepo.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry"));

        com.ams.resident.entity.Profile result = service.findOrCreateProfile(userId);
        org.junit.jupiter.api.Assertions.assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertEquals("existing-id-123", result.getId());
        org.junit.jupiter.api.Assertions.assertEquals(userId, result.getUserId());
    }

    @Test
    void shouldUpdatePermittedFieldsSuccessfully() throws Exception {
        String updatePayload = """
                {
                    "firstName": "Johnny",
                    "lastName": "Doeson",
                    "phone": "9876543210"
                }
                """;

        mockMvc.perform(put("/api/v1/profiles/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Johnny"))
                .andExpect(jsonPath("$.data.lastName").value("Doeson"))
                .andExpect(jsonPath("$.data.phone").value("9876543210"));

        // Verify Database Persistence
        ResidentProfile updated = (ResidentProfile) profileRepository.findByUserId(TEST_USER_ID).orElseThrow();
        assert updated.getFirstName().equals("Johnny");
        assert updated.getPhone().equals("9876543210");
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getCreatedAt());
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getUpdatedAt());

        // Verify Audit Event
        var audits = auditEventRepository.findByActorUserId(TEST_USER_ID);
        org.junit.jupiter.api.Assertions.assertEquals(1, audits.size());
        org.junit.jupiter.api.Assertions.assertEquals("FR-AUD-006", audits.get(0).getAction());
        org.junit.jupiter.api.Assertions.assertEquals(TEST_USER_ID, audits.get(0).getActorUserId());
        org.junit.jupiter.api.Assertions.assertEquals("PROFILE", audits.get(0).getEntityType());
        org.junit.jupiter.api.Assertions.assertEquals(updated.getId(), audits.get(0).getEntityId());
        org.junit.jupiter.api.Assertions.assertNotNull(audits.get(0).getCreatedAt());
    }

    @Test
    void shouldRejectProfileUpdateIfFieldsAreBlank() throws Exception {
        String updatePayload = """
                {
                    "firstName": "",
                    "lastName": ""
                }
                """;

        mockMvc.perform(put("/api/v1/profiles/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Autowired
    private com.ams.resident.repository.EmailChangeRequestRepository emailChangeRequestRepository;

    @Test
    void shouldStorePendingEmailChangeRequestWithHashedToken() throws Exception {
        emailChangeRequestRepository.deleteAll();

        String emailPayload = """
                {
                    "newEmail": "new@example.com"
                }
                """;

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/api/v1/profiles/me/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailPayload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.message").value("Verification required"))
                .andExpect(jsonPath("$.meta").isMap())
                .andReturn();

        // Verify raw token never appears in response body
        String responseContent = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(responseContent.contains("token"));

        // Verify row saved in DB with hash
        java.util.List<com.ams.resident.entity.EmailChangeRequestEntity> requests =
                emailChangeRequestRepository.findAll().stream()
                        .filter(r -> TEST_USER_ID.equals(r.getUserId()))
                        .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, requests.size());
        com.ams.resident.entity.EmailChangeRequestEntity saved = requests.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("new@example.com", saved.getNewEmail());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getTokenHash());
        org.junit.jupiter.api.Assertions.assertEquals(64, saved.getTokenHash().length());
        org.junit.jupiter.api.Assertions.assertNull(saved.getUsedAt());
        org.junit.jupiter.api.Assertions.assertTrue(saved.getExpiresAt().isAfter(java.time.LocalDateTime.now()));

        // Invalidate older unused request when a new one is requested
        String secondPayload = """
                {
                    "newEmail": "second@example.com"
                }
                """;
        mockMvc.perform(post("/api/v1/profiles/me/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPayload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isAccepted());

        com.ams.resident.entity.EmailChangeRequestEntity firstRequest =
                emailChangeRequestRepository.findById(saved.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(firstRequest.getExpiresAt().isBefore(java.time.LocalDateTime.now().plusSeconds(1)));
    }
}
