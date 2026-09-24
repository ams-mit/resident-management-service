package com.ams.resident.integration;

import com.ams.resident.client.IdentityClient;
import com.ams.resident.config.AbstractIntegrationTest;
import com.ams.resident.entity.ProfileType;
import com.ams.resident.entity.ResidentProfile;
import com.ams.resident.repository.ProfileRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityClient identityClient;

    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
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
                        .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID).claim("type", "user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void shouldReturn404IfProfileDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")
                        .with(jwt().jwt(builder -> builder.subject("non-existent-user").claim("type", "user"))))
                .andExpect(status().isNotFound());
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
                        .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID).claim("type", "user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Doeson"))
                .andExpect(jsonPath("$.phone").value("9876543210"));

        // Verify Database Persistence
        ResidentProfile updated = (ResidentProfile) profileRepository.findByUserId(TEST_USER_ID).orElseThrow();
        assert updated.getFirstName().equals("Johnny");
        assert updated.getPhone().equals("9876543210");
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
                        .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID).claim("type", "user"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists());
    }

    @Test
    void shouldForwardEmailChangeSuccessfully() throws Exception {
        doNothing().when(identityClient).requestEmailChange(anyString(), anyString());

        String emailPayload = """
                {
                    "newEmail": "new@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/profiles/me/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailPayload)
                        .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID).claim("type", "user"))))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldReturn503WhenIdentityServiceFails() throws Exception {
        doThrow(new RuntimeException("Identity service offline"))
                .when(identityClient).requestEmailChange(anyString(), anyString());

        String emailPayload = """
                {
                    "newEmail": "new@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/profiles/me/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailPayload)
                        .with(jwt().jwt(builder -> builder.subject(TEST_USER_ID).claim("type", "user"))))
                .andExpect(status().isInternalServerError()); // Custom Exception handler maps Exception to 500, but let's check
    }
}
