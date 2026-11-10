package com.raviranjan.os.ai;

import com.raviranjan.os.domain.entity.AiKnowledgeChunk;
import com.raviranjan.os.domain.entity.Project;
import com.raviranjan.os.repository.AiKnowledgeChunkRepository;
import com.raviranjan.os.repository.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PortfolioRagRetriever {
    private final AiKnowledgeChunkRepository chunkRepository;
    private final ProjectRepository projectRepository;

    public PortfolioRagRetriever(AiKnowledgeChunkRepository chunkRepository, ProjectRepository projectRepository) {
        this.chunkRepository = chunkRepository;
        this.projectRepository = projectRepository;
    }

    public List<AiKnowledgeChunk> retrieveRelevantChunks(String query, int topK) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<AiKnowledgeChunk> allVisibleChunks = new ArrayList<>(chunkRepository.findByAiVisibleTrue());

        // Dynamic fallback: build chunks directly from active projects in Neon DB
        if (allVisibleChunks.isEmpty()) {
            List<Project> projects = projectRepository.findAllByDeletedAtIsNull();
            for (Project p : projects) {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk();
                chunk.setTitle(p.getTitle());
                chunk.setSourceType("PROJECT");
                chunk.setSourceId(p.getSlug());
                chunk.setUrl("/projects/" + p.getSlug());
                String content = String.format("%s. Architecture: %s. %s",
                        p.getTitle(),
                        p.getArchitectureStyle() != null ? p.getArchitectureStyle() : "Modular Architecture",
                        p.getShortSummary() != null ? p.getShortSummary() : "");
                chunk.setContent(content);
                chunk.setAiVisible(true);
                allVisibleChunks.add(chunk);
            }
        }

        if (allVisibleChunks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> queryTokens = Arrays.stream(query.toLowerCase().split("[^a-zA-Z0-9]+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !Set.of("what", "does", "with", "have", "from", "that", "this", "tell", "show", "explain", "about").contains(w))
                .collect(Collectors.toSet());

        if (queryTokens.isEmpty()) {
            return allVisibleChunks.stream().limit(topK).collect(Collectors.toList());
        }

        Map<AiKnowledgeChunk, Integer> scoredChunks = new HashMap<>();
        for (AiKnowledgeChunk chunk : allVisibleChunks) {
            int score = 0;
            String lowerTitle = chunk.getTitle().toLowerCase();
            String lowerContent = chunk.getContent().toLowerCase();

            for (String token : queryTokens) {
                if (lowerTitle.contains(token)) score += 5;
                if (lowerContent.contains(token)) score += 2;
            }

            if (score > 0) {
                scoredChunks.put(chunk, score);
            }
        }

        if (scoredChunks.isEmpty()) {
            return allVisibleChunks.stream().limit(topK).collect(Collectors.toList());
        }

        return scoredChunks.entrySet().stream()
                .sorted(Map.Entry.<AiKnowledgeChunk, Integer>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
