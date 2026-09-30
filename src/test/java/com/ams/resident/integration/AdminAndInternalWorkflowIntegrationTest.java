package com.ams.resident.integration;

import com.ams.resident.config.AbstractIntegrationTest;
import com.ams.resident.entity.ApartmentRelationship;
import com.ams.resident.entity.Profile;
import com.ams.resident.entity.RelationshipStatus;
import com.ams.resident.entity.RelationshipType;
import com.ams.resident.entity.ResidentProfile;
import com.ams.resident.repository.ApartmentRelationshipRepository;
import com.ams.resident.repository.ProfileRepository;
import com.ams.resident.util.TestJwtHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AdminAndInternalWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ApartmentRelationshipRepository relationshipRepository;

    @BeforeEach
    void setUp() {
        relationshipRepository.deleteAll();
        profileRepository.deleteAll();
    }

    // a) GET /api/v1/profiles/me with FINANCE_OFFICER token -> 200; no token -> 401 with { error: { code, message } }
    @Test
    void testProfilesMeWithFinanceOfficerAndUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")
                        .with(TestJwtHelper.userJwt("finance-user-1", List.of("FINANCE_OFFICER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("finance-user-1"))
                .andExpect(jsonPath("$.meta").isMap());

        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    // b) GET /api/v1/profiles/{userId}: SYSTEM_ADMINISTRATOR -> 200; TENANT_RESIDENT -> 403; unknown user -> 404
    @Test
    void testAdminProfileView() throws Exception {
        Profile profile = new ResidentProfile();
        profile.setId("prof-target-1");
        profile.setUserId("target-user-1");
        profile.setFirstName("Target");
        profile.setLastName("User");
        profileRepository.save(profile);

        // SYSTEM_ADMINISTRATOR -> 200
        mockMvc.perform(get("/api/v1/profiles/target-user-1")
                        .with(TestJwtHelper.userJwt("admin-user-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("target-user-1"))
                .andExpect(jsonPath("$.data.firstName").value("Target"))
                .andExpect(jsonPath("$.meta").isMap());

        // TENANT_RESIDENT -> 403
        mockMvc.perform(get("/api/v1/profiles/target-user-1")
                        .with(TestJwtHelper.userJwt("tenant-user-1", List.of("TENANT_RESIDENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").exists());

        // unknown user -> 404
        mockMvc.perform(get("/api/v1/profiles/non-existent-user-999")
                        .with(TestJwtHelper.userJwt("admin-user-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    // c) Relationship state machine: submit -> PENDING; approve -> APPROVED with decidedBy/decidedAt; approve again -> 409; reject without reason -> 400; reject PENDING -> REJECTED with decisionReason; GET /relationships/me shows decisionReason
    @Test
    void testRelationshipStateMachine() throws Exception {
        String submitPayload = """
                {
                    "relationshipType": "TENANT_RESIDENT",
                    "unitReference": "UNIT-201",
                    "supportingInfo": "Lease Document"
                }
                """;

        // submit -> PENDING
        MvcResult submitResult = mockMvc.perform(post("/api/v1/relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload)
                        .with(TestJwtHelper.userJwt("tenant-submitter-1", List.of("TENANT_RESIDENT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.relationshipId").exists())
                .andReturn();

        String responseBody = submitResult.getResponse().getContentAsString();
        String relationshipId = com.jayway.jsonpath.JsonPath.read(responseBody, "$.data.relationshipId");

        // approve -> APPROVED with decidedBy/decidedAt
        mockMvc.perform(patch("/api/v1/relationships/" + relationshipId + "/approve")
                        .with(TestJwtHelper.userJwt("admin-approver-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.decidedBy").value("admin-approver-1"))
                .andExpect(jsonPath("$.data.decidedAt").exists());

        // approve again -> 409
        mockMvc.perform(patch("/api/v1/relationships/" + relationshipId + "/approve")
                        .with(TestJwtHelper.userJwt("admin-approver-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RELATIONSHIP_ALREADY_DECIDED"))
                .andExpect(jsonPath("$.error.message").exists());

        // Second relationship to test reject
        String submitPayload2 = """
                {
                    "relationshipType": "TENANT_RESIDENT",
                    "unitReference": "UNIT-202",
                    "supportingInfo": "Second Lease"
                }
                """;
        MvcResult submitResult2 = mockMvc.perform(post("/api/v1/relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload2)
                        .with(TestJwtHelper.userJwt("tenant-submitter-1", List.of("TENANT_RESIDENT"))))
                .andExpect(status().isCreated())
                .andReturn();
        String relationshipId2 = com.jayway.jsonpath.JsonPath.read(submitResult2.getResponse().getContentAsString(), "$.data.relationshipId");

        // reject without reason -> 400
        mockMvc.perform(patch("/api/v1/relationships/" + relationshipId2 + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"\"}")
                        .with(TestJwtHelper.userJwt("admin-rejector-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // reject PENDING -> REJECTED with decisionReason
        mockMvc.perform(patch("/api/v1/relationships/" + relationshipId2 + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Document forged\"}")
                        .with(TestJwtHelper.userJwt("admin-rejector-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.decisionReason").value("Document forged"))
                .andExpect(jsonPath("$.data.decidedBy").value("admin-rejector-1"))
                .andExpect(jsonPath("$.data.decidedAt").exists());

        // GET /relationships/me shows decisionReason
        mockMvc.perform(get("/api/v1/relationships/me")
                        .with(TestJwtHelper.userJwt("tenant-submitter-1", List.of("TENANT_RESIDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.relationshipId == '" + relationshipId2 + "')].decisionReason").value("Document forged"));
    }

    // d) GET /api/v1/relationships?status=PENDING&page=0&size=10 -> meta.page, meta.size, meta.totalElements; invalid status -> 400; size=500 -> 400
    @Test
    void testAdminRelationshipsListingAndPagination() throws Exception {
        mockMvc.perform(get("/api/v1/relationships?status=PENDING&page=0&size=10")
                        .with(TestJwtHelper.userJwt("admin-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(10))
                .andExpect(jsonPath("$.meta.totalElements").isNumber());

        mockMvc.perform(get("/api/v1/relationships?status=INVALID_STATUS&page=0&size=10")
                        .with(TestJwtHelper.userJwt("admin-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/relationships?page=0&size=500")
                        .with(TestJwtHelper.userJwt("admin-1", List.of("SYSTEM_ADMINISTRATOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // e) GET /internal/v1/relationships/validate: allowed service token + APPROVED -> verified true; PENDING -> verified false; no match -> verified false, status NONE; user token -> 401; service not in allow-list -> 403; missing param -> 400
    @Test
    void testInternalRelationshipValidation() throws Exception {
        ApartmentRelationship approvedRel = new ApartmentRelationship();
        approvedRel.setUserId("user-approved-1");
        approvedRel.setUnitReference("UNIT-301");
        approvedRel.setRelationshipType(RelationshipType.TENANT_RESIDENT);
        approvedRel.setStatus(RelationshipStatus.APPROVED);
        relationshipRepository.save(approvedRel);

        ApartmentRelationship pendingRel = new ApartmentRelationship();
        pendingRel.setUserId("user-pending-1");
        pendingRel.setUnitReference("UNIT-302");
        pendingRel.setRelationshipType(RelationshipType.TENANT_RESIDENT);
        pendingRel.setStatus(RelationshipStatus.PENDING);
        relationshipRepository.save(pendingRel);

        // allowed service token + APPROVED -> verified true
        mockMvc.perform(get("/internal/v1/relationships/validate")
                        .param("userId", "user-approved-1")
                        .param("unitReference", "UNIT-301")
                        .param("relationshipType", "TENANT_RESIDENT")
                        .with(TestJwtHelper.serviceJwt("property-unit-service")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.relationshipType").value("TENANT_RESIDENT"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // PENDING -> verified false
        mockMvc.perform(get("/internal/v1/relationships/validate")
                        .param("userId", "user-pending-1")
                        .param("unitReference", "UNIT-302")
                        .param("relationshipType", "TENANT_RESIDENT")
                        .with(TestJwtHelper.serviceJwt("property-unit-service")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(false))
                .andExpect(jsonPath("$.data.relationshipType").value("TENANT_RESIDENT"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // no match -> verified false, status NONE
        mockMvc.perform(get("/internal/v1/relationships/validate")
                        .param("userId", "user-nonexistent")
                        .param("unitReference", "UNIT-999")
                        .param("relationshipType", "TENANT_RESIDENT")
                        .with(TestJwtHelper.serviceJwt("property-unit-service")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(false))
                .andExpect(jsonPath("$.data.relationshipType").value("TENANT_RESIDENT"))
                .andExpect(jsonPath("$.data.status").value("NONE"));

        // user token -> 401
        mockMvc.perform(get("/internal/v1/relationships/validate")
                        .param("userId", "user-approved-1")
                        .param("unitReference", "UNIT-301")
                        .param("relationshipType", "TENANT_RESIDENT")
                        .with(TestJwtHelper.userJwt("user-approved-1", List.of("TENANT_RESIDENT"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        // service not in allow-list -> 403
        mockMvc.perform(get("/internal/v1/relationships/validate")
                        .param("userId", "user-approved-1")
                        .param("unitReference", "UNIT-301")
                        .param("relationshipType", "TENANT_RESIDENT")
                        .with(TestJwtHelper.serviceJwt("unauthorized-rogue-service")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        // missing param -> 400
        mockMvc.perform(get("/internal/v1/relationships/validate")
                        .param("userId", "user-approved-1")
                        .param("unitReference", "UNIT-301")
                        .with(TestJwtHelper.serviceJwt("property-unit-service")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
