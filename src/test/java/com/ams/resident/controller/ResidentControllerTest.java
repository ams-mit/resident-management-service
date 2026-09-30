package com.ams.resident.controller;

import com.ams.resident.dto.ResidentResponse;
import com.ams.resident.service.ResidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResidentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ResidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResidentService residentService;

    @Test
    void shouldReturnDefaultPagingWhenNoParametersProvided() throws Exception {
        ResidentResponse resident = new ResidentResponse();
        resident.setId("res-1");
        resident.setUserId("user-1");

        when(residentService.getAllResidents(eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(resident), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/residents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("res-1"))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void shouldReturnCustomPageAndSizeWhenSpecified() throws Exception {
        ResidentResponse resident = new ResidentResponse();
        resident.setId("res-2");
        resident.setUserId("user-2");

        when(residentService.getAllResidents(eq(PageRequest.of(2, 10))))
                .thenReturn(new PageImpl<>(List.of(resident, resident, resident, resident, resident), PageRequest.of(2, 10), 25));

        mockMvc.perform(get("/api/v1/residents")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("res-2"))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.size").value(10))
                .andExpect(jsonPath("$.meta.totalElements").value(25));
    }

    @Test
    void shouldReturnBadRequestWhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSizeIsGreaterThan100() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSizeIsZeroOrNegative() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}
