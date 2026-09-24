package com.ams.resident.security;

import com.ams.resident.controller.ProfileController;
import com.ams.resident.controller.RelationshipController;
import com.ams.resident.controller.ResidentController;
import com.ams.resident.service.ProfileService;
import com.ams.resident.service.RelationshipService;
import com.ams.resident.service.ResidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.springframework.test.context.ActiveProfiles;

@WebMvcTest({ProfileController.class, ResidentController.class, RelationshipController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class SecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private ResidentService residentService;

    @MockBean
    private RelationshipService relationshipService;

    @Test
    void shouldReturnUnauthorizedWhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .with(jwt().jwt(builder -> builder.subject("user123").claim("type", "user"))))
                // The Resident GET API requires APARTMENT_MANAGER or SYSTEM_ADMIN. A plain token without roles will be forbidden.
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundOrUnauthorizedForOldTestTokenRoute() throws Exception {
        // The route /api/v1/auth/test-token must NOT be available.
        mockMvc.perform(get("/api/v1/auth/test-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccessWhenAuthorized() throws Exception {
        // We test an endpoint that is allowed by the role.
        mockMvc.perform(get("/api/v1/residents")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_APARTMENT_MANAGER")).jwt(builder -> builder.subject("admin").claim("roles", List.of("APARTMENT_MANAGER")).claim("type", "user"))))
                .andExpect(status().isOk());
    }
}
