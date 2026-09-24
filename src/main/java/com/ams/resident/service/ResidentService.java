package com.ams.resident.service;

import com.ams.resident.dto.ResidentRequest;
import com.ams.resident.dto.ResidentResponse;
import com.ams.resident.entity.ResidentProfile;
import com.ams.resident.exception.ResourceNotFoundException;
import com.ams.resident.repository.ResidentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentProfileRepository residentRepository;
    private final AuditService auditService;

    @Transactional
    public ResidentResponse createResident(ResidentRequest request) {
        if (residentRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("Profile already exists for user ID: " + request.getUserId());
        }

        // We assume the Identity Boundary requirement means we do not explicitly call the identity service DB here.
        // In a real system, an inter-service API call to check if the user ID exists would happen here.
        // Since we are blocked from faking inter-service validation without the gateway, we skip the API check.
        
        ResidentProfile resident = new ResidentProfile();
        resident.setUserId(request.getUserId());
        resident.setFirstName(request.getFirstName());
        resident.setLastName(request.getLastName());
        resident.setPhone(request.getPhone());
        resident.setEmergencyContact(request.getEmergencyContact());

        resident = residentRepository.save(resident);
        auditService.logEvent("FR-AUD-006", request.getUserId(), "Provisioned resident profile via admin API");

        return mapToResponse(resident);
    }

    public List<ResidentResponse> getAllResidents() {
        return residentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ResidentResponse getResidentById(String id) {
        ResidentProfile resident = residentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resident profile not found with ID: " + id));
        return mapToResponse(resident);
    }

    @Transactional
    public ResidentResponse updateResident(String id, ResidentRequest request) {
        ResidentProfile resident = residentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resident profile not found with ID: " + id));

        resident.setFirstName(request.getFirstName());
        resident.setLastName(request.getLastName());
        resident.setPhone(request.getPhone());
        resident.setEmergencyContact(request.getEmergencyContact());

        resident = residentRepository.save(resident);
        auditService.logEvent("FR-AUD-006", resident.getUserId(), "Updated resident profile");

        return mapToResponse(resident);
    }

    private ResidentResponse mapToResponse(ResidentProfile resident) {
        ResidentResponse response = new ResidentResponse();
        response.setId(resident.getId());
        response.setUserId(resident.getUserId());
        response.setProfileType(resident.getProfileType());
        response.setFirstName(resident.getFirstName());
        response.setLastName(resident.getLastName());
        response.setPhone(resident.getPhone());
        response.setEmergencyContact(resident.getEmergencyContact());
        return response;
    }
}
