package com.raviranjan.os.service.impl;

import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.DsaNote;
import com.raviranjan.os.domain.entity.DsaProblem;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.dto.dsa.ManualProblemCreateRequest;
import com.raviranjan.os.dto.dsa.ManualProblemResponse;
import com.raviranjan.os.exception.BadRequestException;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.realtime.ActivitySynchronizedEvent;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.DsaNoteRepository;
import com.raviranjan.os.repository.DsaProblemRepository;
import com.raviranjan.os.service.ManualCodingProblemService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ManualCodingProblemServiceImpl implements ManualCodingProblemService {

    private final DsaProblemRepository problemRepository;
    private final DsaNoteRepository noteRepository;
    private final DeveloperActivityRepository activityRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ManualCodingProblemServiceImpl(
            DsaProblemRepository problemRepository,
            DsaNoteRepository noteRepository,
            DeveloperActivityRepository activityRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.problemRepository = problemRepository;
        this.noteRepository = noteRepository;
        this.activityRepository = activityRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ManualProblemResponse createManualProblem(ManualProblemCreateRequest request) {
        String rawTitle = request.getTitle();
        if (rawTitle == null || rawTitle.isBlank()) {
            throw new BadRequestException("Problem title is required");
        }
        String cleanTitle = rawTitle.trim();
        String plat = (request.getPlatform() != null && !request.getPlatform().isBlank())
                ? request.getPlatform().trim().toUpperCase()
                : "LEETCODE";
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug().trim()
                : cleanTitle.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "problem-" + System.currentTimeMillis();
        }
        final String finalSlug = slug;

        DsaProblem problem = problemRepository.findByPlatformAndSlug(plat, finalSlug)
                .orElseGet(() -> {
                    DsaProblem p = new DsaProblem();
                    p.setPlatform(plat);
                    p.setExternalId(finalSlug);
                    p.setTitle(cleanTitle);
                    p.setSlug(finalSlug);
                    p.setDifficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM");
                    p.setProblemUrl(request.getProblemUrl() != null && !request.getProblemUrl().isBlank()
                            ? request.getProblemUrl()
                            : (plat.equals("LEETCODE") ? "https://leetcode.com/problems/" + finalSlug + "/" : "https://www.geeksforgeeks.org/problems/" + finalSlug + "/1"));
                    return problemRepository.save(p);
                });
        problem.setTitle(cleanTitle);
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            problem.setDifficulty(request.getDifficulty().toUpperCase());
        }
        if (request.getProblemUrl() != null && !request.getProblemUrl().isBlank()) {
            problem.setProblemUrl(request.getProblemUrl());
        }
        problem = problemRepository.save(problem);

        final UUID probId = problem.getId();
        DsaNote note = noteRepository.findAll().stream()
                .filter(n -> n.getProblem() != null && probId.equals(n.getProblem().getId()))
                .findFirst()
                .orElseGet(DsaNote::new);

        note.setProblem(problem);
        note.setPattern(request.getTopic() != null && !request.getTopic().isBlank() ? request.getTopic() : "Algorithmic Optimization");
        note.setTimeComplexity(request.getTimeComplexity() != null && !request.getTimeComplexity().isBlank() ? request.getTimeComplexity() : "O(n)");
        note.setSpaceComplexity(request.getSpaceComplexity() != null && !request.getSpaceComplexity().isBlank() ? request.getSpaceComplexity() : "O(1)");
        note.setSolutionCode(request.getSolutionCode() != null ? request.getSolutionCode() : "");
        note.setExplanation(request.getApproach() != null && !request.getApproach().isBlank()
                ? request.getApproach()
                : (request.getWhatILearned() != null ? request.getWhatILearned() : "Optimal solution note"));
        noteRepository.save(note);

        try {
            ActivityType type = plat.equalsIgnoreCase("LEETCODE")
                    ? ActivityType.LEETCODE_SOLVED
                    : ActivityType.GFG_SOLVED;

            String extKey = "dsa-" + problem.getId();
            DeveloperActivity activity = activityRepository.findBySourcePlatformAndExternalId(plat, extKey)
                    .orElseGet(DeveloperActivity::new);
            activity.setActivityType(type);
            activity.setSourcePlatform(plat);
            activity.setExternalId(extKey);
            activity.setTitle("Solved " + problem.getTitle() + " (" + problem.getDifficulty() + ")");
            activity.setDescription(note.getExplanation());
            activity.setExternalUrl(problem.getProblemUrl());
            Instant solveInstant = request.getSolvedDate() != null
                    ? request.getSolvedDate().atStartOfDay().toInstant(ZoneOffset.UTC)
                    : Instant.now();
            activity.setOccurredAt(solveInstant);
            DeveloperActivity savedActivity = activityRepository.save(activity);
            eventPublisher.publishEvent(new ActivitySynchronizedEvent(this, List.of(savedActivity)));
        } catch (Exception ignored) {}

        return mapProblemWithNote(problem, note);
    }

    @Override
    public ManualProblemResponse updateManualProblem(UUID id, ManualProblemCreateRequest request) {
        DsaProblem problem = problemRepository.findById(id)
                .orElseGet(() -> noteRepository.findById(id).map(DsaNote::getProblem)
                        .orElseThrow(() -> new ResourceNotFoundException("DsaProblem", "id", id)));

        if (request.getTitle() != null && !request.getTitle().isBlank()) problem.setTitle(request.getTitle().trim());
        if (request.getPlatform() != null && !request.getPlatform().isBlank()) problem.setPlatform(request.getPlatform().toUpperCase());
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) problem.setDifficulty(request.getDifficulty().toUpperCase());
        if (request.getProblemUrl() != null && !request.getProblemUrl().isBlank()) problem.setProblemUrl(request.getProblemUrl());
        problem = problemRepository.save(problem);

        final UUID probId = problem.getId();
        DsaNote note = noteRepository.findAll().stream()
                .filter(n -> n.getProblem() != null && probId.equals(n.getProblem().getId()))
                .findFirst()
                .orElseGet(DsaNote::new);
        note.setProblem(problem);
        if (request.getTopic() != null) note.setPattern(request.getTopic());
        if (request.getTimeComplexity() != null) note.setTimeComplexity(request.getTimeComplexity());
        if (request.getSpaceComplexity() != null) note.setSpaceComplexity(request.getSpaceComplexity());
        if (request.getSolutionCode() != null) note.setSolutionCode(request.getSolutionCode());
        if (request.getApproach() != null) note.setExplanation(request.getApproach());
        noteRepository.save(note);

        try {
            String extKey = "dsa-" + problem.getId();
            final String pTitle = problem.getTitle();
            DeveloperActivity act = activityRepository.findAll().stream()
                    .filter(a -> extKey.equals(a.getExternalId()) || (pTitle != null && a.getTitle() != null && a.getTitle().contains(pTitle)))
                    .findFirst()
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType("LEETCODE".equalsIgnoreCase(problem.getPlatform()) ? ActivityType.LEETCODE_SOLVED : ActivityType.GFG_SOLVED);
            act.setSourcePlatform(problem.getPlatform() != null ? problem.getPlatform() : "LEETCODE");
            act.setExternalId(extKey);
            act.setTitle("Solved " + problem.getTitle() + " (" + problem.getDifficulty() + ")");
            act.setDescription(note.getExplanation());
            act.setExternalUrl(problem.getProblemUrl());
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}

        return mapProblemWithNote(problem, note);
    }

    @Override
    public void deleteManualProblem(UUID id) {
        try {
            Optional<DsaProblem> pCheck = problemRepository.findById(id);
            String titleMatch = pCheck.map(DsaProblem::getTitle).orElse(null);
            String extKey = "dsa-" + id;
            List<DeveloperActivity> acts = activityRepository.findAll().stream()
                    .filter(a -> extKey.equals(a.getExternalId()) || (titleMatch != null && a.getTitle() != null && a.getTitle().contains(titleMatch)))
                    .collect(Collectors.toList());
            if (!acts.isEmpty()) activityRepository.deleteAll(acts);
        } catch (Exception ignored) {}
        Optional<DsaProblem> probOpt = problemRepository.findById(id);
        if (probOpt.isPresent()) {
            DsaProblem problem = probOpt.get();
            List<DsaNote> notes = noteRepository.findAll().stream()
                    .filter(n -> n.getProblem() != null && id.equals(n.getProblem().getId()))
                    .collect(Collectors.toList());
            noteRepository.deleteAll(notes);
            problemRepository.delete(problem);
            return;
        }
        Optional<DsaNote> noteOpt = noteRepository.findById(id);
        if (noteOpt.isPresent()) {
            DsaNote note = noteOpt.get();
            DsaProblem problem = note.getProblem();
            noteRepository.delete(note);
            if (problem != null) {
                problemRepository.delete(problem);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualProblemResponse> getAllManualProblems() {
        List<DsaNote> allNotes = noteRepository.findAll();
        return problemRepository.findAll().stream().map(p -> {
            Optional<DsaNote> noteOpt = allNotes.stream()
                    .filter(n -> n.getProblem() != null && p.getId().equals(n.getProblem().getId()))
                    .findFirst();
            return mapProblemWithNote(p, noteOpt.orElse(null));
        }).collect(Collectors.toList());
    }

    private ManualProblemResponse mapProblemWithNote(DsaProblem p, DsaNote note) {
        ManualProblemResponse res = new ManualProblemResponse();
        res.setId(p.getId());
        res.setPlatform(p.getPlatform());
        res.setTitle(p.getTitle());
        res.setSlug(p.getSlug());
        res.setDifficulty(p.getDifficulty());
        res.setProblemUrl(p.getProblemUrl());
        if (note != null) {
            res.setTopic(note.getPattern());
            res.setApproach(note.getExplanation());
            res.setTimeComplexity(note.getTimeComplexity());
            res.setSpaceComplexity(note.getSpaceComplexity());
            res.setSolutionCode(note.getSolutionCode());
            res.setWhatILearned(note.getExplanation());
        }
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }
}
