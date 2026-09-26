package com.ams.resident.integration;

import com.ams.resident.client.IdentityClient;
import com.ams.resident.config.AbstractIntegrationTest;
import com.ams.resident.entity.EmailChangeRequestEntity;
import com.ams.resident.exception.ConflictException;
import com.ams.resident.exception.DependencyUnavailableException;
import com.ams.resident.repository.EmailChangeRequestRepository;
import com.ams.resident.util.TestJwtHelper;
import com.ams.resident.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class EmailChangeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailChangeRequestRepository emailChangeRequestRepository;

    @MockBean
    private IdentityClient identityClient;

    private static final String TEST_USER_ID = "test-user-confirm-123";
    private static final String RAW_TOKEN = "test-raw-token-abcdef1234567890-secure";

    @BeforeEach
    void setUp() {
        emailChangeRequestRepository.deleteAll();
    }

    private EmailChangeRequestEntity createPendingRequest(String userId, String token, LocalDateTime expiresAt, LocalDateTime usedAt) {
        EmailChangeRequestEntity entity = EmailChangeRequestEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .newEmail("confirmed@example.com")
                .tokenHash(TokenUtils.hashToken(token))
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        return emailChangeRequestRepository.save(entity);
    }

    @Test
    void shouldSuccessfullyConfirmEmailChangeWhenTokenIsValid() throws Exception {
        EmailChangeRequestEntity saved = createPendingRequest(
                TEST_USER_ID, RAW_TOKEN, LocalDateTime.now().plusHours(24), null);

        doNothing().when(identityClient).updateUserEmail(TEST_USER_ID, "confirmed@example.com");

        String payload = String.format("""
                {
                    "verificationToken": "%s"
                }
                """, RAW_TOKEN);

        MvcResult result = mockMvc.perform(put("/api/v1/profiles/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Email updated"))
                .andExpect(jsonPath("$.meta").isMap())
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains(RAW_TOKEN));

        verify(identityClient, times(1)).updateUserEmail(TEST_USER_ID, "confirmed@example.com");

        EmailChangeRequestEntity updated = emailChangeRequestRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(updated.getUsedAt());
    }

    @Test
    void shouldReturn400WhenTokenIsWrong() throws Exception {
        createPendingRequest(TEST_USER_ID, RAW_TOKEN, LocalDateTime.now().plusHours(24), null);

        String payload = """
                {
                    "verificationToken": "completely-wrong-token"
                }
                """;

        mockMvc.perform(put("/api/v1/profiles/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_TOKEN"))
                .andExpect(jsonPath("$.error.message").value("Invalid or expired verification token"));

        verify(identityClient, never()).updateUserEmail(anyString(), anyString());
    }

    @Test
    void shouldReturn400WhenTokenIsExpired() throws Exception {
        createPendingRequest(TEST_USER_ID, RAW_TOKEN, LocalDateTime.now().minusMinutes(10), null);

        String payload = String.format("""
                {
                    "verificationToken": "%s"
                }
                """, RAW_TOKEN);

        mockMvc.perform(put("/api/v1/profiles/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_TOKEN"))
                .andExpect(jsonPath("$.error.message").value("Invalid or expired verification token"));

        verify(identityClient, never()).updateUserEmail(anyString(), anyString());
    }

    @Test
    void shouldReturn400WhenTokenIsAlreadyUsed() throws Exception {
        createPendingRequest(TEST_USER_ID, RAW_TOKEN, LocalDateTime.now().plusHours(24), LocalDateTime.now().minusMinutes(1));

        String payload = String.format("""
                {
                    "verificationToken": "%s"
                }
                """, RAW_TOKEN);

        mockMvc.perform(put("/api/v1/profiles/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_TOKEN"))
                .andExpect(jsonPath("$.error.message").value("Invalid or expired verification token"));

        verify(identityClient, never()).updateUserEmail(anyString(), anyString());
    }

    @Test
    void shouldReturn400WhenTokenBelongsToAnotherUser() throws Exception {
        createPendingRequest("other-user-999", RAW_TOKEN, LocalDateTime.now().plusHours(24), null);

        String payload = String.format("""
                {
                    "verificationToken": "%s"
                }
                """, RAW_TOKEN);

        mockMvc.perform(put("/api/v1/profiles/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_TOKEN"))
                .andExpect(jsonPath("$.error.message").value("Invalid or expired verification token"));

        verify(identityClient, never()).updateUserEmail(anyString(), anyString());
    }

    @Test
    void shouldReturn409AndKeepRequestUnusedWhenIdentityReturnsConflict() throws Exception {
        EmailChangeRequestEntity saved = createPendingRequest(
                TEST_USER_ID, RAW_TOKEN, LocalDateTime.now().plusHours(24), null);

        doThrow(new ConflictException("EMAIL_ALREADY_IN_USE", "Email already in use"))
                .when(identityClient).updateUserEmail(TEST_USER_ID, "confirmed@example.com");

        String payload = String.format("""
                {
                    "verificationToken": "%s"
                }
                """, RAW_TOKEN);

        mockMvc.perform(put("/api/v1/profiles/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_IN_USE"));

        EmailChangeRequestEntity reloaded = emailChangeRequestRepository.findById(saved.getId()).orElseThrow();
        assertNull(reloaded.getUsedAt(), "Request must stay unused when Identity returns conflict");
    }

    @Test
    void shouldReturn503AndKeepRequestUnusedWhenIdentityIsDown() throws Exception {
        EmailChangeRequestEntity saved = createPendingRequest(
                TEST_USER_ID, RAW_TOKEN, LocalDateTime.now().plusHours(24), null);

        doThrow(new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "Identity service unreachable"))
                .when(identityClient).updateUserEmail(TEST_USER_ID, "confirmed@example.com");

        String payload = String.format("""
                {
                    "verificationToken": "%s"
                }
                """, RAW_TOKEN);

        mockMvc.perform(put("/api/v1/profiles/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("DEPENDENCY_UNAVAILABLE"));

        EmailChangeRequestEntity reloaded = emailChangeRequestRepository.findById(saved.getId()).orElseThrow();
        assertNull(reloaded.getUsedAt(), "Request must stay unused when Identity is unavailable");
    }
}
