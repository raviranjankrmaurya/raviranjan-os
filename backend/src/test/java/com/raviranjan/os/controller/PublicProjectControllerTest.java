package com.raviranjan.os.controller;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ProjectStatus;
import com.raviranjan.os.dto.project.ProjectResponse;
import com.raviranjan.os.dto.project.ProjectSummaryResponse;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.security.CustomUserDetailsService;
import com.raviranjan.os.security.JwtTokenProvider;
import com.raviranjan.os.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/projects should return paginated project list")
    void testGetPublishedProjects() throws Exception {
        ProjectSummaryResponse summary = new ProjectSummaryResponse();
        summary.setId(UUID.randomUUID());
        summary.setTitle("Distributed Ingestion Engine");
        summary.setSlug("distributed-ingestion-engine");
        summary.setStatus(ProjectStatus.PUBLISHED);

        PageResponse<ProjectSummaryResponse> pageResponse = new PageResponse<>(
                List.of(summary), 0, 10, 1, 1, true
        );

        when(projectService.getProjects(eq(ProjectStatus.PUBLISHED), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].slug").value("distributed-ingestion-engine"));
    }

    @Test
    @DisplayName("GET /api/projects/{slug} should return 200 with full architecture detail")
    void testGetProjectBySlug() throws Exception {
        ProjectResponse response = new ProjectResponse();
        response.setId(UUID.randomUUID());
        response.setTitle("Distributed Ingestion Engine");
        response.setSlug("distributed-ingestion-engine");
        response.setStatus(ProjectStatus.PUBLISHED);

        when(projectService.getProjectBySlug("distributed-ingestion-engine"))
                .thenReturn(response);

        mockMvc.perform(get("/api/projects/distributed-ingestion-engine")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Distributed Ingestion Engine"));
    }

    @Test
    @DisplayName("GET /api/projects/{slug} should return 404 when project not found")
    void testGetProjectBySlugNotFound() throws Exception {
        when(projectService.getProjectBySlug("non-existent"))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        mockMvc.perform(get("/api/projects/non-existent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
