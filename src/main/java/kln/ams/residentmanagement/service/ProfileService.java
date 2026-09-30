package kln.ams.residentmanagement.service;

import jakarta.persistence.criteria.Predicate;
import kln.ams.residentmanagement.dto.*;
import kln.ams.residentmanagement.entity.Profile;
import kln.ams.residentmanagement.entity.ProfileStatus;
import kln.ams.residentmanagement.entity.ProfileType;
import kln.ams.residentmanagement.exception.BadRequestException;
import kln.ams.residentmanagement.exception.ConflictException;
import kln.ams.residentmanagement.exception.ResourceNotFoundException;
import kln.ams.residentmanagement.repository.ProfileRepository;
import kln.ams.residentmanagement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final IdentityClient identityClient;
    private final AuditService auditService;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Transactional(readOnly = true)
    public PagedData<ProfileResponse> listProfiles(ProfileType profileType, ProfileStatus status, String search, String userId, Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException("VALIDATION_ERROR", "Page index must not be negative");
        }
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new BadRequestException("VALIDATION_ERROR", "Page size must be between 1 and 100");
        }

        Specification<Profile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("profileType"), profileType));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(userId)) {
                predicates.add(cb.equal(root.get("userId"), userId.trim()));
            }

            if (StringUtils.hasText(search)) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("firstName")), term),
                        cb.like(cb.lower(root.get("lastName")), term),
                        cb.like(cb.lower(root.get("email")), term)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Profile> page = profileRepository.findAll(spec, pageable);
        return PagedData.of(page.map(ProfileResponse::fromEntity));
    }

    @Transactional
    public ProfileResponse createProfile(ProfileType profileType, CreateProfileRequest request) {
        // Validate user in Identity Access
        identityClient.validateUser(request.getUserId());

        // Check if profile of this type already exists for the user
        if (profileRepository.existsByUserIdAndProfileType(request.getUserId(), profileType)) {
            String errorCode = profileType.name() + "_ALREADY_EXISTS";
            throw new ConflictException(errorCode, profileType.name() + " profile already exists for user: " + request.getUserId());
        }

        Profile profile = Profile.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .profileType(profileType)
                .status(ProfileStatus.ACTIVE)
                .build();

        Profile saved = profileRepository.save(profile);

        auditService.logEvent(profileType.name() + "_CREATED", profileType.name(), saved.getId(),
                SecurityUtils.getAuthenticatedUserId(), "Created " + profileType.name().toLowerCase() + " profile");

        return ProfileResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(String id, ProfileType expectedType) {
        Profile profile = profileRepository.findByIdAndProfileType(id, expectedType)
                .orElseThrow(() -> new ResourceNotFoundException(expectedType.name() + "_NOT_FOUND",
                        expectedType.name() + " profile not found for id: " + id));

        // Self-service access check: if not admin/manager, must be the owner of the profile
        if (!SecurityUtils.isManagerOrAdmin()) {
            String currentUserId = SecurityUtils.getAuthenticatedUserId();
            if (currentUserId == null || !currentUserId.equals(profile.getUserId())) {
                throw new AccessDeniedException("Access is denied to this profile");
            }
        }

        return ProfileResponse.fromEntity(profile);
    }

    @Transactional
    public ProfileResponse updateResident(String residentId, UpdateResidentRequest request) {
        Profile profile = profileRepository.findByIdAndProfileType(residentId, ProfileType.RESIDENT)
                .orElseThrow(() -> new ResourceNotFoundException("RESIDENT_NOT_FOUND",
                        "Resident profile not found for id: " + residentId));

        if (!SecurityUtils.isManagerOrAdmin()) {
            String currentUserId = SecurityUtils.getAuthenticatedUserId();
            if (currentUserId == null || !currentUserId.equals(profile.getUserId())) {
                throw new AccessDeniedException("Access is denied to update this profile");
            }
        }

        if (StringUtils.hasText(request.getFirstName())) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (StringUtils.hasText(request.getLastName())) {
            profile.setLastName(request.getLastName().trim());
        }
        if (StringUtils.hasText(request.getEmail())) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }

        Profile updated = profileRepository.save(profile);

        auditService.logEvent("RESIDENT_UPDATED", "RESIDENT", updated.getId(),
                SecurityUtils.getAuthenticatedUserId(), "Updated resident profile");

        return ProfileResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public ResidentValidationResponse validateResident(String residentId) {
        Profile profile = profileRepository.findByIdAndProfileType(residentId, ProfileType.RESIDENT)
                .orElseThrow(() -> new ResourceNotFoundException("RESIDENT_NOT_FOUND",
                        "Resident profile does not exist"));

        auditService.logEvent("RESIDENT_VALIDATION_REQUEST", "RESIDENT", profile.getId(),
                SecurityUtils.getAuthenticatedService(), "Validated resident profile");

        return ResidentValidationResponse.builder()
                .residentId(profile.getId())
                .userId(profile.getUserId())
                .exists(true)
                .active(profile.getStatus() == ProfileStatus.ACTIVE)
                .build();
    }

    @Transactional(readOnly = true)
    public UserRelationshipsResponse getUserRelationships(String userId) {
        if (!StringUtils.hasText(userId) || !UUID_PATTERN.matcher(userId).matches()) {
            throw new BadRequestException("VALIDATION_ERROR", "userId must be a valid UUID");
        }

        List<Profile> profiles = profileRepository.findByUserId(userId);
        if (profiles.isEmpty()) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "No relationships found for user: " + userId);
        }

        List<UserRelationshipsResponse.RelationshipItem> items = profiles.stream()
                .map(p -> UserRelationshipsResponse.RelationshipItem.builder()
                        .profileId(p.getId())
                        .profileType(p.getProfileType())
                        .status(p.getStatus())
                        .build())
                .toList();

        auditService.logEvent("USER_RELATIONSHIPS_REQUEST", "USER", userId,
                SecurityUtils.getAuthenticatedService(), "Retrieved user relationships");

        return UserRelationshipsResponse.builder()
                .userId(userId)
                .relationships(items)
                .build();
    }
}
