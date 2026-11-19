package com.raviranjan.os.ai;

import com.raviranjan.os.domain.entity.AiKnowledgeChunk;
import com.raviranjan.os.domain.entity.Project;
import com.raviranjan.os.domain.enums.ProjectStatus;
import com.raviranjan.os.repository.AiKnowledgeChunkRepository;
import com.raviranjan.os.repository.ProjectRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Keyword retriever over the visible knowledge chunks.
 *
 * Root-cause fix: the old version returned the FIRST {@code topK} chunks whenever nothing matched (or the query had no
 * usable words), which is why "hello" produced a dump of the portfolio. This version only returns chunks that really
 * match the question and returns an EMPTY list otherwise, so callers can answer "not in the portfolio".
 */
@Component
public class PortfolioRagRetriever {

    private static final int TITLE_HIT = 5;
    private static final int CONTENT_HIT = 2;
    private static final int MIN_SCORE = 4;

    private static final Set<String> STOP_WORDS = Set.of(
            "what", "does", "with", "have", "from", "that", "this", "tell", "show", "explain", "about", "which", "where", "when", "who", "whom",
            "how", "his", "her", "him", "has", "had", "are", "was", "were", "the", "and", "for", "you", "your", "can", "could", "would", "should",
            "please", "give", "list", "any", "some", "did", "raviranjan", "kumar", "portfolio", "there", "their", "they", "them", "into", "also",
            "kya", "hai", "hain", "aur", "batao", "bataiye", "samjhao", "mujhe", "unka", "unke", "unki", "iska", "uska", "mein", "karta", "karte");

    private final AiKnowledgeChunkRepository chunkRepository;
    private final ProjectRepository projectRepository;

    public PortfolioRagRetriever(AiKnowledgeChunkRepository chunkRepository, ProjectRepository projectRepository) {
        this.chunkRepository = chunkRepository;
        this.projectRepository = projectRepository;
    }

    public List<AiKnowledgeChunk> retrieveRelevantChunks(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        Set<String> tokens = significantTokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<AiKnowledgeChunk> candidates = new ArrayList<>(chunkRepository.findByAiVisibleTrue());
        if (candidates.isEmpty()) {
            candidates = chunksFromPublishedProjects();
        }

        record Scored(AiKnowledgeChunk chunk, int score) { }
        List<Scored> scored = new ArrayList<>();
        for (AiKnowledgeChunk chunk : candidates) {
            int score = score(chunk, tokens);
            if (score >= MIN_SCORE) scored.add(new Scored(chunk, score));
        }
        if (scored.isEmpty()) {
            return List.of();                       // nothing relevant -> never pad with unrelated chunks
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());

        int best = scored.get(0).score();
        int cutoff = Math.max(MIN_SCORE, (int) Math.ceil(best * 0.4));
        Set<String> seenTitles = new LinkedHashSet<>();
        List<AiKnowledgeChunk> result = new ArrayList<>();
        for (Scored s : scored) {
            if (s.score() < cutoff) break;
            String key = IntentClassifier.normalize(s.chunk().getTitle());
            if (!seenTitles.add(key)) continue;       // one chunk per title
            result.add(s.chunk());
            if (result.size() >= topK) break;
        }
        return result;
    }

    static int score(AiKnowledgeChunk chunk, Set<String> tokens) {
        Set<String> titleWords = words(chunk.getTitle());
        Set<String> contentWords = words(chunk.getContent());
        int score = 0;
        for (String t : tokens) {
            if (titleWords.contains(t)) score += TITLE_HIT;
            else if (contentWords.contains(t)) score += CONTENT_HIT;
        }
        return score;
    }

    static Set<String> significantTokens(String query) {
        Set<String> out = new LinkedHashSet<>();
        for (String w : IntentClassifier.normalize(query).split(" ")) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) out.add(stem(w));
        }
        return out;
    }

    private static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        for (String w : IntentClassifier.normalize(text).split(" ")) {
            if (!w.isEmpty()) out.add(stem(w));
        }
        return out;
    }

    private static String stem(String w) {
        return w.length() > 4 && w.endsWith("s") && !w.endsWith("ss") ? w.substring(0, w.length() - 1) : w;
    }

    /** Fallback when no curated chunks exist: PUBLISHED projects only (drafts and archived work are never exposed). */
    private List<AiKnowledgeChunk> chunksFromPublishedProjects() {
        List<AiKnowledgeChunk> out = new ArrayList<>();
        for (Project p : projectRepository.findByStatusAndDeletedAtIsNull(ProjectStatus.PUBLISHED, Pageable.unpaged()).getContent()) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setTitle(p.getTitle());
            chunk.setSourceType("PROJECT");
            chunk.setSourceId(p.getSlug());
            chunk.setUrl(p.getGithubRepoUrl());
            StringBuilder content = new StringBuilder(p.getTitle()).append(". ");
            if (p.getShortSummary() != null && !p.getShortSummary().isBlank()) content.append(p.getShortSummary().trim()).append(' ');
            if (p.getArchitectureStyle() != null && !p.getArchitectureStyle().isBlank()) content.append("Architecture: ").append(p.getArchitectureStyle().trim()).append('.');
            chunk.setContent(content.toString().trim());
            out.add(chunk);
        }
        return out;
    }
}
