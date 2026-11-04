package com.raviranjan.os.service;

import com.raviranjan.os.domain.entity.Project;
import com.raviranjan.os.domain.enums.ProjectStatus;
import com.raviranjan.os.dto.project.ProjectCreateRequest;
import com.raviranjan.os.dto.project.ProjectResponse;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ProjectMapper;
import com.raviranjan.os.repository.*;
import com.raviranjan.os.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private SkillCategoryRepository skillCategoryRepository;
    @Mock
    private ProjectMilestoneRepository milestoneRepository;
    @Mock
    private ProjectTaskRepository taskRepository;
    @Mock
    private ProjectChangelogRepository changelogRepository;
    @Mock
    private ProjectDecisionRepository decisionRepository;
    @Mock
    private DeveloperActivityRepository activityRepository;
    @Mock
    private SiteSettingRepository siteSettingRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project sampleProject;
    private ProjectCreateRequest createRequest;
    private ProjectResponse sampleResponse;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        sampleProject = new Project();
        sampleProject.setId(id);
        sampleProject.setTitle("Raviranjan.OS Core");
        sampleProject.setSlug("raviranjan-os-core");
        sampleProject.setShortSummary("Personal developer platform");
        sampleProject.setFullDescription("A deep architectural description");
        sampleProject.setStatus(ProjectStatus.PUBLISHED);

        createRequest = new ProjectCreateRequest();
        createRequest.setTitle("Raviranjan.OS Core");
        createRequest.setSlug("raviranjan-os-core");
        createRequest.setShortSummary("Personal developer platform");
        createRequest.setFullDescription("A deep architectural description");
        createRequest.setStatus(ProjectStatus.PUBLISHED);

        sampleResponse = new ProjectResponse();
        sampleResponse.setId(id);
        sampleResponse.setTitle("Raviranjan.OS Core");
        sampleResponse.setSlug("raviranjan-os-core");
    }

    @Test
    @DisplayName("Should successfully create project when slug is unique")
    void testCreateProjectSuccess() {
        when(projectRepository.findBySlug("raviranjan-os-core")).thenReturn(Optional.empty());
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);
        when(projectMapper.toResponse(sampleProject)).thenReturn(sampleResponse);

        ProjectResponse response = projectService.createProject(createRequest);

        assertNotNull(response);
        assertEquals("raviranjan-os-core", response.getSlug());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("Should retrieve project by slug")
    void testGetProjectBySlug() {
        when(projectRepository.findBySlugAndDeletedAtIsNull("raviranjan-os-core")).thenReturn(Optional.of(sampleProject));
        when(projectMapper.toResponse(sampleProject)).thenReturn(sampleResponse);

        ProjectResponse response = projectService.getProjectBySlug("raviranjan-os-core");

        assertNotNull(response);
        assertEquals("raviranjan-os-core", response.getSlug());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent slug")
    void testGetProjectBySlugNotFound() {
        when(projectRepository.findBySlugAndDeletedAtIsNull("unknown-slug")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectBySlug("unknown-slug"));
    }

    @Test
    @DisplayName("Should hard delete an existing project")
    void testDeleteProject() {
        UUID id = sampleProject.getId();
        when(projectRepository.findById(id)).thenReturn(Optional.of(sampleProject));

        projectService.deleteProject(id);

        verify(projectRepository, times(1)).delete(sampleProject);
    }
}
