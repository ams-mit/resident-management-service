package com.ams.resident.service;

import com.ams.resident.client.IdentityClient;
import com.ams.resident.dto.EmailChangeRequest;
import com.ams.resident.dto.ProfileRequest;
import com.ams.resident.dto.ProfileResponse;
import com.ams.resident.entity.Profile;
import com.ams.resident.entity.ProfileType;
import com.ams.resident.entity.ResidentProfile;
import com.ams.resident.exception.ResourceNotFoundException;
import com.ams.resident.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final AuditService auditService;
    private final IdentityClient identityClient;

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
        auditService.logEvent("FR-AUD-006", userId, "Updated own profile");

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

    public void requestEmailChange(EmailChangeRequest request) {
        String userId = getAuthenticatedUserId();
        // Forward email change request to Identity Service
        identityClient.requestEmailChange(userId, request.getNewEmail());
        auditService.logEvent("FR-AUD-006", userId, "Requested email change to: " + request.getNewEmail());
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
