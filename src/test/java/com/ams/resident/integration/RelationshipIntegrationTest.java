package com.ams.resident.integration;

import com.ams.resident.client.PropertyClient;
import com.ams.resident.config.AbstractIntegrationTest;
import com.ams.resident.entity.ApartmentRelationship;
import com.ams.resident.entity.RelationshipStatus;
import com.ams.resident.entity.RelationshipType;
import com.ams.resident.repository.ApartmentRelationshipRepository;
import com.ams.resident.util.TestJwtHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class RelationshipIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApartmentRelationshipRepository relationshipRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PropertyClient propertyClient;

    private static final String TEST_USER_ID = "test-user-456";

    @BeforeEach
    void setUp() {
        relationshipRepository.deleteAll();
    }

    @Test
    void shouldCreateRelationshipWhenUnitExists() throws Exception {
        String payload = """
                {
                    "relationshipType": "TENANT_RESIDENT",
                    "unitReference": "UNIT-101",
                    "supportingInfo": "Lease Document attached"
                }
                """;

        mockMvc.perform(post("/api/v1/relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.relationshipType").value("TENANT_RESIDENT"))
                .andExpect(jsonPath("$.unitReference").value("UNIT-101"));

        // Verify Persistence
        assert relationshipRepository.findByUserId(TEST_USER_ID).size() == 1;
        verify(propertyClient, never()).checkUnitExists(anyString());
    }

    @Test
    void shouldCreateRelationshipEvenWhenPropertyServiceIsDown() throws Exception {
        doThrow(new RestClientException("Property Service Offline"))
                .when(propertyClient).checkUnitExists(anyString());

        String payload = """
                {
                    "relationshipType": "TENANT_RESIDENT",
                    "unitReference": "UNIT-999",
                    "supportingInfo": "Lease"
                }
                """;

        mockMvc.perform(post("/api/v1/relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.relationshipType").value("TENANT_RESIDENT"))
                .andExpect(jsonPath("$.unitReference").value("UNIT-999"));

        // Verify Persistence
        assert relationshipRepository.findByUserId(TEST_USER_ID).size() == 1;
        verify(propertyClient, never()).checkUnitExists(anyString());
    }

    @Test
    void shouldReturnOwnRelationships() throws Exception {
        ApartmentRelationship relationship = new ApartmentRelationship();
        relationship.setUserId(TEST_USER_ID);
        relationship.setRelationshipType(RelationshipType.OWNER);
        relationship.setUnitReference("UNIT-202");
        relationship.setStatus(RelationshipStatus.APPROVED);
        relationshipRepository.save(relationship);

        mockMvc.perform(get("/api/v1/relationships/me")
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].unitReference").value("UNIT-202"));
    }

    @Test
    void shouldRejectInvalidRelationshipType() throws Exception {
        String payload = """
                {
                    "relationshipType": "INVALID_TYPE",
                    "unitReference": "UNIT-101"
                }
                """;

        mockMvc.perform(post("/api/v1/relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(TestJwtHelper.userJwt(TEST_USER_ID)))
                .andExpect(status().isBadRequest()); // Enum validation failure -> HttpMessageNotReadableException
    }
}
