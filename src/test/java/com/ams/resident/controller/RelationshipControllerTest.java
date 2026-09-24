package com.ams.resident.controller;

import com.ams.resident.service.RelationshipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@WebMvcTest(RelationshipController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class RelationshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RelationshipService relationshipService;

    @Test
    void shouldRejectMissingRelationshipType() throws Exception {
        String invalidPayload = """
                {
                    "unitReference": "UNIT-101"
                }
                """;

        mockMvc.perform(post("/api/v1/relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
