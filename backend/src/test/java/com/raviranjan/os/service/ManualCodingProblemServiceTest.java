package com.raviranjan.os.service;

import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.DsaNote;
import com.raviranjan.os.domain.entity.DsaProblem;
import com.raviranjan.os.dto.dsa.ManualProblemCreateRequest;
import com.raviranjan.os.dto.dsa.ManualProblemResponse;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.DsaNoteRepository;
import com.raviranjan.os.repository.DsaProblemRepository;
import com.raviranjan.os.service.impl.ManualCodingProblemServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualCodingProblemServiceTest {

    @Mock
    private DsaProblemRepository problemRepository;
    @Mock
    private DsaNoteRepository noteRepository;
    @Mock
    private DeveloperActivityRepository activityRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ManualCodingProblemServiceImpl manualCodingProblemService;

    @Test
    @DisplayName("Should manually author coding problem and dispatch unified activity")
    void testCreateManualProblem() {
        ManualProblemCreateRequest req = new ManualProblemCreateRequest();
        req.setPlatform("LEETCODE");
        req.setTitle("Median of Two Sorted Arrays");
        req.setSlug("median-of-two-sorted-arrays");
        req.setDifficulty("HARD");
        req.setTopic("Binary Search");
        req.setApproach("Partition binary search with O(log(min(N, M))) complexity");
        req.setTimeComplexity("O(log(min(N,M)))");
        req.setSpaceComplexity("O(1)");
        req.setWhatILearned("Partition invariant balance check");
        req.setSolvedDate(LocalDate.now());

        DsaProblem savedProblem = new DsaProblem();
        savedProblem.setId(UUID.randomUUID());
        savedProblem.setPlatform("LEETCODE");
        savedProblem.setTitle(req.getTitle());
        savedProblem.setSlug(req.getSlug());
        savedProblem.setDifficulty("HARD");

        when(problemRepository.findByPlatformAndSlug("LEETCODE", "median-of-two-sorted-arrays"))
                .thenReturn(Optional.empty());
        when(problemRepository.save(any(DsaProblem.class))).thenReturn(savedProblem);
        when(noteRepository.save(any(DsaNote.class))).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.save(any(DeveloperActivity.class))).thenAnswer(i -> i.getArgument(0));

        ManualProblemResponse res = manualCodingProblemService.createManualProblem(req);

        assertNotNull(res);
        assertEquals("LEETCODE", res.getPlatform());
        assertEquals("Median of Two Sorted Arrays", res.getTitle());
        assertEquals("HARD", res.getDifficulty());

        verify(activityRepository, times(1)).save(any(DeveloperActivity.class));
        verify(eventPublisher, times(1)).publishEvent(any());
    }
}
