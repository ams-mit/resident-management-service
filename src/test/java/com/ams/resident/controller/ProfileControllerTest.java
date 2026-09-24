package com.ams.resident.controller;

import com.ams.resident.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false) // Testing only controller validation, disabling full security filters here for focused unit tests
@ActiveProfiles("test")
public class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @Test
    void shouldRejectInvalidEmailFormat() throws Exception {
        String invalidEmailPayload = """
                {
                    "newEmail": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/v1/profiles/me/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmailPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newEmail").exists());
    }
}
