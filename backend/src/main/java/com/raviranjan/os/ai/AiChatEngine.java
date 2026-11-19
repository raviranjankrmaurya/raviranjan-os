package com.raviranjan.os.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviranjan.os.ai.IntentClassifier.Classification;
import com.raviranjan.os.ai.IntentClassifier.Topic;
import com.raviranjan.os.ai.PortfolioFacts.ChunkFact;
import com.raviranjan.os.domain.entity.AiConfiguration;
import com.raviranjan.os.domain.entity.AiKnowledgeChunk;
import com.raviranjan.os.domain.entity.Profile;
import com.raviranjan.os.domain.entity.Project;
import com.raviranjan.os.domain.entity.Skill;
import com.raviranjan.os.domain.enums.ProjectStatus;
import com.raviranjan.os.repository.AchievementRepository;
import com.raviranjan.os.repository.CertificationRepository;
import com.raviranjan.os.repository.ContributionStoryRepository;
import com.raviranjan.os.repository.DsaProblemRepository;
import com.raviranjan.os.repository.EducationRepository;
import com.raviranjan.os.repository.ExperienceRepository;
import com.raviranjan.os.repository.ProfileRepository;
import com.raviranjan.os.repository.ProjectRepository;
import com.raviranjan.os.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Produces the final answer for one visitor message.
 *
 * Pipeline: classify intent -> load ONLY the facts that topic needs -> build a short grounded answer
 * (optionally rephrased by Gemini for open questions / explicit "in detail" requests) -> clean up.
 * Nothing is answered from memory: if the portfolio has no data the standard "not in the portfolio" reply is returned.
 */
@Component
public class AiChatEngine {

    private static final Logger log = LoggerFactory.getLogger(AiChatEngine.class);
    private static final int MAX_CHUNK_CHARS = 600;

    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final AchievementRepository achievementRepository;
    private final CertificationRepository certificationRepository;
    private final DsaProblemRepository dsaProblemRepository;
    private final ContributionStoryRepository storyRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.integrations.ai.gemini-api-key:}")
    private String envGeminiKey;

    public AiChatEngine(
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ExperienceRepository experienceRepository,
            EducationRepository educationRepository,
            AchievementRepository achievementRepository,
            CertificationRepository certificationRepository,
            DsaProblemRepository dsaProblemRepository,
            ContributionStoryRepository storyRepository,
            ProfileRepository profileRepository,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.achievementRepository = achievementRepository;
        this.certificationRepository = certificationRepository;
        this.dsaProblemRepository = dsaProblemRepository;
        this.storyRepository = storyRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    }

    /** Label stored in the query log. */
    public String detectLanguageLabel(String query) {
        return switch (IntentClassifier.classify(query).lang()) {
            case HINDI -> "Hindi";
            case HINGLISH -> "Hinglish";
            default -> "English";
        };
    }

    public String generateAnswer(String query, List<AiKnowledgeChunk> chunks, AiConfiguration config) {
        Classification c = IntentClassifier.classify(query);

        // Safety net: the service already routes these without touching the engine.
        switch (c.intent()) {
            case GREETING: return AnswerBuilder.greeting(c.lang());
            case ACKNOWLEDGEMENT: return AnswerBuilder.acknowledgement(c.lang());
            case UNKNOWN: return AssistantIdentity.UNKNOWN_ANSWER;
            default: break;
        }

        List<ChunkFact> chunkFacts = toChunkFacts(chunks);
        PortfolioFacts facts = loadFacts(c.topic());
        String local = AnswerBuilder.build(c, facts);
        String apiKey = resolveApiKey(config);

        if (local != null && !AssistantIdentity.UNKNOWN_ANSWER.equals(local)) {
            // Structured topics (projects, stack, database, education ...) are answered deterministically; Gemini is
            // only used to elaborate when the visitor explicitly asked for detail.
            if (c.detailed() && apiKey != null) {
                String rephrased = callGemini(apiKey, config, c, query, facts, chunkFacts);
                if (rephrased != null) return rephrased;
            }
            return local;
        }

        if (chunkFacts.isEmpty()) {
            return AssistantIdentity.UNKNOWN_ANSWER;      // clear topic but no data, or nothing relevant retrieved
        }
        if (apiKey != null) {
            String generated = callGemini(apiKey, config, c, query, facts, chunkFacts);
            if (generated != null) return generated;
        }
        return AnswerBuilder.fromChunks(chunkFacts, c.detailed());
    }

    // ------------------------------------------------------------------ facts (only what the topic needs)

    private PortfolioFacts loadFacts(Topic topic) {
        List<PortfolioFacts.ProjectFact> projects = List.of();
        List<PortfolioFacts.SkillFact> skills = List.of();
        List<PortfolioFacts.EducationFact> education = List.of();
        List<PortfolioFacts.ExperienceFact> experience = List.of();
        List<PortfolioFacts.AchievementFact> achievements = List.of();
        List<PortfolioFacts.CertificationFact> certifications = List.of();
        List<PortfolioFacts.DsaFact> dsa = List.of();
        List<PortfolioFacts.StoryFact> stories = List.of();
        PortfolioFacts.ProfileFact profile = null;

        switch (topic) {
            case PROJECTS, NONE -> projects = publishedProjects();
            case TECH_STACK, DATABASE -> { skills = skills(); projects = publishedProjects(); }
            case EDUCATION -> education = education();
            case EXPERIENCE -> experience = experience();
            case ACHIEVEMENTS -> { achievements = achievements(); certifications = certifications(); }
            case CODING -> dsa = dsa();
            case CONTRIBUTIONS -> stories = stories();
            case ABOUT -> profile = profile();
            default -> { }
        }
        return new PortfolioFacts(profile, projects, skills, education, experience, achievements, certifications, dsa, stories);
    }

    private List<PortfolioFacts.ProjectFact> publishedProjects() {
        List<PortfolioFacts.ProjectFact> out = new ArrayList<>();
        for (Project p : projectRepository.findByStatusAndDeletedAtIsNull(ProjectStatus.PUBLISHED, Pageable.unpaged()).getContent()) {
            List<String> techs = p.getTechnologies() == null ? List.of()
                    : p.getTechnologies().stream().map(Skill::getName).collect(Collectors.toList());
            out.add(new PortfolioFacts.ProjectFact(p.getTitle(), p.getSlug(), p.getShortSummary(), p.getFullDescription(),
                    notBlank(p.getSystemArchitecture()) ? p.getSystemArchitecture() : p.getArchitectureStyle(),
                    p.getStatus() == null ? null : p.getStatus().name(), p.getProgress(), p.getGithubRepoUrl(), p.getLiveDemoUrl(), techs));
        }
        return out;
    }

    private List<PortfolioFacts.SkillFact> skills() {
        return skillRepository.findAll().stream()
                .map(s -> new PortfolioFacts.SkillFact(s.getName(), s.getCategory() == null ? null : s.getCategory().getName(), s.isFeatured(), s.getProficiency()))
                .collect(Collectors.toList());
    }

    private List<PortfolioFacts.EducationFact> education() {
        return educationRepository.findAllByOrderByStartDateDesc().stream()
                .map(e -> new PortfolioFacts.EducationFact(e.getDegree(), e.getField(), e.getInstitution(), date(e.getStartDate()), date(e.getEndDate())))
                .collect(Collectors.toList());
    }

    private List<PortfolioFacts.ExperienceFact> experience() {
        return experienceRepository.findAllByOrderByStartDateDesc().stream()
                .map(e -> new PortfolioFacts.ExperienceFact(e.getRole(), e.getCompany(), e.isCurrent(), date(e.getStartDate()), date(e.getEndDate())))
                .collect(Collectors.toList());
    }

    private List<PortfolioFacts.AchievementFact> achievements() {
        return achievementRepository.findAllByOrderByDateDesc().stream()
                .map(a -> new PortfolioFacts.AchievementFact(a.getTitle(), a.getOrganization()))
                .collect(Collectors.toList());
    }

    private List<PortfolioFacts.CertificationFact> certifications() {
        return certificationRepository.findAllByOrderByIssueDateDesc().stream()
                .map(c -> new PortfolioFacts.CertificationFact(c.getTitle(), c.getIssuer()))
                .collect(Collectors.toList());
    }

    private List<PortfolioFacts.DsaFact> dsa() {
        return dsaProblemRepository.findAll().stream()
                .map(d -> new PortfolioFacts.DsaFact(d.getTitle(), d.getPlatform(), d.getDifficulty()))
                .collect(Collectors.toList());
    }

    private List<PortfolioFacts.StoryFact> stories() {
        return storyRepository.findAllByOrderByContributionDateDesc(PageRequest.of(0, 20)).getContent().stream()
                .map(s -> new PortfolioFacts.StoryFact(s.getRepository(), s.getTitle(), s.getPrUrl()))
                .collect(Collectors.toList());
    }

    private PortfolioFacts.ProfileFact profile() {
        Profile p = profileRepository.findAll().stream().findFirst().orElse(null);
        return p == null ? null : new PortfolioFacts.ProfileFact(p.getFullName(), p.getHeadline(), p.getBio());
    }

    // ------------------------------------------------------------------ Gemini (optional)

    private String resolveApiKey(AiConfiguration config) {
        if (config != null && notBlank(config.getApiKey())) return config.getApiKey().trim();
        return notBlank(envGeminiKey) ? envGeminiKey.trim() : null;
    }

    private String callGemini(String apiKey, AiConfiguration config, Classification c, String query, PortfolioFacts facts, List<ChunkFact> chunks) {
        try {
            String model = config != null && notBlank(config.getModelName()) ? config.getModelName().trim() : "gemini-1.5-flash";
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", buildPrompt(config, c, query, facts, chunks))))),
                    "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 350));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)             // header, not ?key= (keeps the key out of URLs and logs)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Gemini responded with HTTP {}", response.statusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            return ResponsePolicy.apply(text, c.detailed());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(AiConfiguration config, Classification c, String query, PortfolioFacts facts, List<ChunkFact> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are \"").append(AssistantIdentity.NAME).append("\", the assistant on Raviranjan's portfolio website. ")
          .append("You are NOT Raviranjan; refer to him as \"he\" or \"Raviranjan\". This name overrides any other name you were given.\n\n")
          .append("RULES\n")
          .append("1. Answer ONLY from the CONTEXT below. Never guess.\n")
          .append("2. Give the exact answer first, in 1-3 short sentences").append(c.detailed() ? " (the visitor asked for detail: up to 8 sentences)" : "").append(". No greeting, no introduction, no closing line.\n")
          .append("3. Use bullet points only for lists of three or more items.\n")
          .append("4. Never repeat a fact or mention the same technology twice. Never summarise the whole portfolio.\n")
          .append("5. Add a markdown link such as [GitHub](url) only when a URL appears in the CONTEXT and is relevant.\n")
          .append("6. If the CONTEXT does not contain the answer, reply exactly: ").append(AssistantIdentity.UNKNOWN_ANSWER).append('\n')
          .append("7. Never mention servers, databases or infrastructure of this website.\n")
          .append("8. Reply in the visitor's language (English, Hindi or Hinglish).\n");
        if (config != null && notBlank(config.getSystemPrompt())) {
            String extra = config.getSystemPrompt().trim();
            sb.append("\nOWNER NOTES (lower priority than the rules above): ").append(extra.length() > 800 ? extra.substring(0, 800) : extra).append('\n');
        }
        sb.append("\nCONTEXT\n");
        for (ChunkFact ch : chunks) {
            String content = ch.content() == null ? "" : ch.content();
            sb.append("- ").append(ch.title()).append(": ").append(content.length() > MAX_CHUNK_CHARS ? content.substring(0, MAX_CHUNK_CHARS) : content);
            if (ch.url() != null && ch.url().startsWith("http")) sb.append(" (").append(ch.url()).append(')');
            sb.append('\n');
        }
        for (PortfolioFacts.ProjectFact p : facts.projects()) {
            sb.append("- Project ").append(p.title()).append(": ").append(p.summary() == null ? "" : p.summary());
            if (!p.technologies().isEmpty()) sb.append(" Technologies: ").append(String.join(", ", TechNames.unique(p.technologies()))).append('.');
            if (p.githubUrl() != null) sb.append(" (").append(p.githubUrl()).append(')');
            sb.append('\n');
        }
        sb.append("\nVISITOR QUESTION: ").append(query).append("\nANSWER:");
        return sb.toString();
    }

    // ------------------------------------------------------------------ small helpers

    private static List<ChunkFact> toChunkFacts(List<AiKnowledgeChunk> chunks) {
        List<ChunkFact> out = new ArrayList<>();
        if (chunks == null) return out;
        for (AiKnowledgeChunk ch : chunks) out.add(new ChunkFact(ch.getTitle(), ch.getUrl(), ch.getContent()));
        return out;
    }

    private static String date(LocalDate d) { return d == null ? null : d.toString(); }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
