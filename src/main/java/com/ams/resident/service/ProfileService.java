package com.ams.resident.service;

import com.ams.resident.client.IdentityClient;
import com.ams.resident.dto.EmailChangeRequest;
import com.ams.resident.dto.ProfileRequest;
import com.ams.resident.dto.ProfileResponse;
import com.ams.resident.entity.EmailChangeRequestEntity;
import com.ams.resident.entity.Profile;
import com.ams.resident.entity.ProfileType;
import com.ams.resident.entity.ResidentProfile;
import com.ams.resident.exception.ResourceNotFoundException;
import com.ams.resident.repository.EmailChangeRequestRepository;
import com.ams.resident.repository.ProfileRepository;
import com.ams.resident.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final AuditService auditService;
    private final IdentityClient identityClient;

    @Value("${app.email-change.expiry-hours:24}")
    private long expiryHours = 24;

    public ProfileResponse getOwnProfile() {
        String userId = getAuthenticatedUserId();
        Profile profile = findOrCreateProfile(userId);

        return mapToResponse(profile);
    }

    @Transactional
    public ProfileResponse editOwnProfile(ProfileRequest request) {
        String userId = getAuthenticatedUserId();
        Profile profile = findOrCreateProfile(userId);

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        // Handle fullName mapping if applicable

        profile = profileRepository.save(profile);
        auditService.logEvent("FR-AUD-006", "PROFILE", profile.getId(), userId, "Updated own profile");

        return mapToResponse(profile);
    }

    public Profile findOrCreateProfile(String userId) {
        Optional<Profile> existing = profileRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ResidentProfile newProfile = new ResidentProfile();
        newProfile.setUserId(userId);
        try {
            return profileRepository.saveAndFlush(newProfile);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent creation: on duplicate, re-read and return the existing row
            return profileRepository.findByUserId(userId)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    public void requestEmailChange(EmailChangeRequest request) {
        String userId = getAuthenticatedUserId();
        LocalDateTime now = LocalDateTime.now();

        // Any older unused request for the same user is invalidated
        emailChangeRequestRepository.invalidateUnusedRequestsForUser(userId, now);

        // Generate a random secure token (at least 32 bytes, URL-safe). Save only its hash.
        String rawToken = TokenUtils.generateSecureToken();
        String tokenHash = TokenUtils.hashToken(rawToken);

        EmailChangeRequestEntity entity = EmailChangeRequestEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .newEmail(request.getNewEmail())
                .tokenHash(tokenHash)
                .expiresAt(now.plusHours(expiryHours))
                .createdAt(now)
                .build();

        emailChangeRequestRepository.save(entity);

        // Real email sending is out of scope (Case Scope §13). Log the raw token ONCE with prefix [DEV-ONLY] at INFO
        log.info("[DEV-ONLY] Email change verification token for user {}: {}", userId, rawToken);

        // Audit event: EMAIL_CHANGE_REQUESTED (no token in details)
        auditService.logEvent("EMAIL_CHANGE_REQUESTED", userId, "Requested email change to: " + request.getNewEmail());
    }

    private String getAuthenticatedUserId() {
        return com.ams.resident.security.SecurityUtils.getCurrentUserId();
    }
    
    private ProfileResponse mapToResponse(Profile profile) {
        ProfileResponse response = new ProfileResponse();
        response.setUserId(profile.getUserId());
        response.setProfileType(profile.getProfileType());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setPhone(profile.getPhone());
        response.setStatusInfo("ACTIVE");
        return response;
    }
}
