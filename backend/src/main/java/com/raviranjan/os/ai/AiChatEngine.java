package com.raviranjan.os.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviranjan.os.domain.entity.*;
import com.raviranjan.os.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AiChatEngine {

    private static final Logger log = LoggerFactory.getLogger(AiChatEngine.class);

    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final AchievementRepository achievementRepository;
    private final CertificationRepository certificationRepository;
    private final DsaProblemRepository dsaProblemRepository;
    private final ContributionStoryRepository storyRepository;
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
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public String detectLanguageLabel(String query) {
        if (query == null) return "ENGLISH";
        String lower = query.toLowerCase();
        if (lower.contains("[preferred language: hindi]") || lower.contains("in hindi") || lower.contains("hindi me")) {
            for (char c : query.toCharArray()) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.DEVANAGARI) {
                    return "HINDI";
                }
            }
            return "HINGLISH";
        }
        if (lower.contains("[preferred language: hinglish]") || lower.contains("in hinglish") || lower.contains("hinglish me")) {
            return "HINGLISH";
        }
        if (lower.contains("[preferred language: english]") || lower.contains("in english") || lower.contains("english me")) {
            return "ENGLISH";
        }
        for (char c : query.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.DEVANAGARI) {
                return "HINDI";
            }
        }
        if (isHindiOrHinglish(lower)) {
            return "HINGLISH";
        }
        return "ENGLISH";
    }

    public String generateAnswer(String query, List<AiKnowledgeChunk> retrievedChunks, AiConfiguration config) {
        if (query == null || query.isBlank()) {
            return "Please ask a question about Raviranjan's projects, skills, or engineering background (in English, Hindi, or Hinglish).";
        }

        List<Project> allProjects = projectRepository.findAllByDeletedAtIsNull();
        List<Skill> allSkills = skillRepository.findAll();
        List<Experience> experiences = experienceRepository.findAll();
        List<Education> educations = educationRepository.findAll();
        List<Achievement> achievements = achievementRepository.findAll();
        List<Certification> certifications = certificationRepository.findAll();
        List<DsaProblem> dsaProblems = dsaProblemRepository.findAll();
        List<ContributionStory> stories = storyRepository.findAll();

        String geminiApiKey = (config != null && config.getApiKey() != null && !config.getApiKey().isBlank())
                ? config.getApiKey()
                : ((envGeminiKey != null && !envGeminiKey.isBlank()) ? envGeminiKey : null);

        if (geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.equals("YOUR_GEMINI_API_KEY")) {
            String geminiAnswer = callGeminiApi(query, geminiApiKey, allProjects, allSkills, experiences, educations, achievements, certifications, dsaProblems, stories, retrievedChunks);
            if (geminiAnswer != null && !geminiAnswer.isBlank()) {
                return geminiAnswer;
            }
        }

        return synthesizeDynamicMultiLingualAnswer(query, allProjects, allSkills, experiences, educations, achievements, certifications, dsaProblems, stories, retrievedChunks);
    }

    private String callGeminiApi(
            String query,
            String apiKey,
            List<Project> projects,
            List<Skill> skills,
            List<Experience> experiences,
            List<Education> educations,
            List<Achievement> achievements,
            List<Certification> certifications,
            List<DsaProblem> dsaProblems,
            List<ContributionStory> stories,
            List<AiKnowledgeChunk> chunks
    ) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("--- RAVIRANJAN'S REAL PROJECTS IN NEON DB ---\n");
            for (Project p : projects) {
                context.append(String.format("Project: %s (Slug: %s, Status: %s, Progress: %d%%)\n", p.getTitle(), p.getSlug(), p.getStatus(), p.getProgress()));
                context.append("Summary: ").append(p.getShortSummary() != null ? p.getShortSummary() : "").append("\n");
                context.append("Architecture: ").append(p.getArchitectureStyle() != null ? p.getArchitectureStyle() : "").append("\n");
                context.append("Full Description: ").append(p.getFullDescription() != null ? p.getFullDescription() : "").append("\n\n");
            }

            context.append("--- TECHNICAL SKILLS ---\n");
            context.append(skills.stream().map(Skill::getName).collect(Collectors.joining(", "))).append("\n\n");

            context.append("--- EDUCATION & EXPERIENCE ---\n");
            for (Education ed : educations) {
                context.append("Education: ").append(ed.getDegree()).append(" at ").append(ed.getInstitution()).append(" (").append(ed.getDescription()).append(")\n");
            }
            for (Experience ex : experiences) {
                context.append("Experience: ").append(ex.getRole()).append(" at ").append(ex.getCompany()).append(" — ").append(ex.getDescription()).append("\n");
            }
            for (Achievement ac : achievements) {
                context.append("Achievement: ").append(ac.getTitle()).append(" (").append(ac.getOrganization()).append(")\n");
            }
            for (Certification ce : certifications) {
                context.append("Certification: ").append(ce.getTitle()).append(" by ").append(ce.getIssuer()).append("\n");
            }
            for (DsaProblem dp : dsaProblems) {
                context.append("DSA Solved: ").append(dp.getTitle()).append(" on ").append(dp.getPlatform()).append(" (").append(dp.getDifficulty()).append(")\n");
            }

            String systemPrompt = "You are 'Ask AI', the personal portfolio assistant for Raviranjan Kumar (BCA Student & Software Developer). " +
                    "Explain Raviranjan's real projects, architecture, skills, education, experience, and achievements based on the provided portfolio context. " +
                    "CRITICAL RULES:\n" +
                    "1. CLICKABLE LINKS: When mentioning any project, ALWAYS embed a clickable markdown link: [Project Title](/projects/<slug>) or [GitHub Repo](<githubRepoUrl>).\n" +
                    "2. PRIVACY & ARCHITECTURE: NEVER mention database names, storage details, PostgreSQL, or 'Neon DB' to visitors. Answer naturally and professionally as a developer portfolio assistant.\n" +
                    "3. LANGUAGE: Respect the visitor's language (Hindi, Hinglish, or English) and respond in the same language.";

            Map<String, Object> requestBody = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", "Portfolio Information:\n" + context + "\n\nUser Query: " + query))
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.3,
                            "maxOutputTokens", 800
                    )
            );

            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidate = root.path("candidates").get(0);
                if (candidate != null) {
                    JsonNode textNode = candidate.path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        return textNode.asText().trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Gemini API fallback to built-in multi-lingual engine: {}", e.getMessage());
        }
        return null;
    }

    private String synthesizeDynamicMultiLingualAnswer(
            String rawQuery,
            List<Project> projects,
            List<Skill> skills,
            List<Experience> experiences,
            List<Education> educations,
            List<Achievement> achievements,
            List<Certification> certifications,
            List<DsaProblem> dsaProblems,
            List<ContributionStory> stories,
            List<AiKnowledgeChunk> retrievedChunks
    ) {
        String lang = detectLanguageLabel(rawQuery);
        boolean isDevanagariHindi = "HINDI".equals(lang);
        boolean isHinglish = "HINGLISH".equals(lang) || isDevanagariHindi;

        String cleanQuery = rawQuery.replaceAll("(?i)\\[preferred language:[^\\]]+\\]", "").trim();
        String lowerQuery = cleanQuery.toLowerCase();

        if (lowerQuery.contains("admin") || lowerQuery.contains("cms") || lowerQuery.contains("login")) {
            return isHinglish
                    ? "🔐 **Admin OS Gateway:** Aap `Ctrl + Shift + A` daba kar ya `/admin` par jaakar Admin Panel unlock kar sakte hain."
                    : "🔐 **Admin OS Gateway:** Press `Ctrl + Shift + A` or visit `/admin` to authenticate and open the Admin Operating System.";
        }

        // 1. Check if query asks about a specific project in Raviranjan's portfolio
        for (Project p : projects) {
            String slugLower = p.getSlug().toLowerCase();
            String titleLower = p.getTitle().toLowerCase();
            String firstToken = slugLower.contains("-") ? slugLower.split("-")[0] : slugLower;
            if (lowerQuery.contains(slugLower) || lowerQuery.contains(firstToken) || lowerQuery.contains(titleLower)) {
                String techList = (p.getTechnologies() != null && !p.getTechnologies().isEmpty())
                        ? p.getTechnologies().stream().map(Skill::getName).collect(Collectors.joining(", "))
                        : "Java 21, Spring Boot, PostgreSQL, Docker";
                if (isDevanagariHindi) {
                    return String.format(
                            "**%s** रविरंजन द्वारा निर्मित एक प्रमुख प्रोजेक्ट है:\n" +
                            "• **सारांश (Overview):** %s\n" +
                            "• **आर्किटेक्चर (Architecture):** %s\n" +
                            "• **टेक्नोलॉजी स्टैक:** %s\n" +
                            "• **प्रोग्रेस और स्टेटस:** %s (%d%% पूरा)\n" +
                            "• **विवरण:** %s",
                            p.getTitle(),
                            p.getShortSummary(),
                            p.getArchitectureStyle() != null ? p.getArchitectureStyle() : "Modular Architecture",
                            techList,
                            p.getStatus(),
                            p.getProgress(),
                            p.getFullDescription() != null ? p.getFullDescription() : p.getShortSummary()
                    );
                } else if (isHinglish) {
                    return String.format(
                            "**%s** Raviranjan ka ek flagship engineering project hai :\n" +
                            "• **Kya Karta Hai (Summary):** %s\n" +
                            "• **System Architecture:** %s\n" +
                            "• **Tech Stack:** %s\n" +
                            "• **Status & Progress:** %s (%d%% completed)\n" +
                            "• **Deep Details:** %s\n" +
                            (p.getGithubRepoUrl() != null && !p.getGithubRepoUrl().isBlank() ? "• **GitHub Link:** [View Repository](" + p.getGithubRepoUrl() + ")" : ""),
                            p.getTitle(),
                            p.getShortSummary(),
                            p.getArchitectureStyle() != null ? p.getArchitectureStyle() : "Modular Architecture",
                            techList,
                            p.getStatus(),
                            p.getProgress(),
                            p.getFullDescription() != null ? p.getFullDescription() : p.getShortSummary()
                    );
                } else {
                    return String.format(
                            "**%s** is a production architecture engineered by Raviranjan :\n" +
                            "• **Overview:** %s\n" +
                            "• **Architecture Style:** %s\n" +
                            "• **Tech Stack:** %s\n" +
                            "• **Status & Progress:** %s (%d%% complete)\n" +
                            "• **Technical Details:** %s\n" +
                            (p.getGithubRepoUrl() != null && !p.getGithubRepoUrl().isBlank() ? "• **GitHub:** [View Repository](" + p.getGithubRepoUrl() + ")" : ""),
                            p.getTitle(),
                            p.getShortSummary(),
                            p.getArchitectureStyle() != null ? p.getArchitectureStyle() : "Modular Architecture",
                            techList,
                            p.getStatus(),
                            p.getProgress(),
                            p.getFullDescription() != null ? p.getFullDescription() : p.getShortSummary()
                    );
                }
            }
        }

        // 2. General projects list
        if (lowerQuery.contains("project") || lowerQuery.contains("kya banaya") || lowerQuery.contains("banaye") || lowerQuery.contains("built") || lowerQuery.contains("प्रोजेक्ट")) {
            if (projects.isEmpty()) {
                return isHinglish
                        ? "Abhi koi project listed nahi hai."
                        : "There are currently no published projects in Raviranjan's portfolio.";
            }
            StringBuilder sb = new StringBuilder();
            if (isDevanagariHindi) {
                sb.append("रविरंजन ने निम्नलिखित प्रोजेक्ट्स बनाए हैं :\n");
                for (Project p : projects) {
                    sb.append(String.format("• **%s** (%d%%, %s): %s\n", p.getTitle(), p.getProgress(), p.getStatus(), p.getShortSummary()));
                }
                sb.append("\nआप किसी भी प्रोजेक्ट का नाम लिखकर उसके आर्किटेक्चर के बारे में हिंदी, हिंग्लिश या इंग्लिश में पूछ सकते हैं!");
            } else if (isHinglish) {
                sb.append("Raviranjan ne ye प्रमुख projects banaye hain :\n");
                for (Project p : projects) {
                    sb.append(String.format("• **%s** (%d%%, %s): %s\n", p.getTitle(), p.getProgress(), p.getStatus(), p.getShortSummary()));
                }
                sb.append("\nAap kisi bhi project ka naam likhkar (jaise 'Explain RakshaGrid in Hinglish') uski puri detail pooch sakte hain!");
            } else {
                sb.append("Raviranjan has engineered the following projects :\n");
                for (Project p : projects) {
                    sb.append(String.format("• **%s** (%d%%, %s): %s\n", p.getTitle(), p.getProgress(), p.getStatus(), p.getShortSummary()));
                }
                sb.append("\nAsk about any specific project above to explore its architecture, tech stack, and design decisions!");
            }
            return sb.toString();
        }

        // 3. Skills / Tech Stack
        if (lowerQuery.contains("skill") || lowerQuery.contains("tech") || lowerQuery.contains("stack") || lowerQuery.contains("aata hai") || lowerQuery.contains("know") || lowerQuery.contains("स्किल")) {
            String skillStr = skills.isEmpty()
                    ? "Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL, Redis, Docker"
                    : skills.stream().map(Skill::getName).collect(Collectors.joining(", "));
            if (isDevanagariHindi) {
                return "रविरंजन के प्रमुख टेक्निकल स्किल्स :\n• **टेक्नोलॉजीज:** " + skillStr + "\n• **मुख्य फोकस:** फुल-स्टैक डेवलपमेंट, स्केलेबल बैकएंड APIs, PostgreSQL डेटाबेस डिज़ाइन और AI/RAG एप्लीकेशन्स।";
            } else if (isHinglish) {
                return "Raviranjan ka core technical stack :\n• **Technologies:** " + skillStr + "\n• **Core Focus:** Backend engineering (Java 21 / Spring Boot / FastAPI), React full-stack apps, PostgreSQL database modeling, aur AI/LLM RAG pipelines.";
            } else {
                return "Raviranjan's verified technical stack :\n• **Technologies:** " + skillStr + "\n• **Core Focus:** Scalable backend architectures (Java 21, Spring Boot, FastAPI), PostgreSQL & Redis data engineering, modern React interfaces, and AI/RAG workflows.";
            }
        }

        // 4. Education / Experience / Career / Achievements
        if (lowerQuery.contains("education") || lowerQuery.contains("college") || lowerQuery.contains("degree") || lowerQuery.contains("bca") || lowerQuery.contains("experience") || lowerQuery.contains("career") || lowerQuery.contains("padhai") || lowerQuery.contains("achievement") || lowerQuery.contains("cert")) {
            String eduStr = educations.isEmpty()
                    ? "Bachelor of Computer Applications (BCA)"
                    : educations.stream().map(e -> e.getDegree() + " (" + e.getInstitution() + ")").collect(Collectors.joining("; "));
            String expStr = experiences.isEmpty()
                    ? "Software Developer & Open-Source Builder"
                    : experiences.stream().map(e -> e.getRole() + " at " + e.getCompany()).collect(Collectors.joining("; "));
            if (isHinglish) {
                return "Raviranjan ki Education aur Experience details :\n" +
                        "• **Education:** " + eduStr + "\n" +
                        "• **Experience:** " + expStr + "\n" +
                        "• **Availability:** Internships aur Full-Time Software Developer roles ke liye available hain.";
            } else {
                return "Raviranjan's Education & Career Background :\n" +
                        "• **Education:** " + eduStr + "\n" +
                        "• **Experience:** " + expStr + "\n" +
                        "• **Availability:** Open to Software Engineering Internships and Full-Time Developer roles.";
            }
        }

        // 5. DSA / Coding / Contributions
        if (lowerQuery.contains("dsa") || lowerQuery.contains("leetcode") || lowerQuery.contains("gfg") || lowerQuery.contains("coding") || lowerQuery.contains("contribution") || lowerQuery.contains("open source")) {
            String dsaList = dsaProblems.isEmpty()
                    ? "Two Sum, LRU Cache, Search in Rotated Sorted Array"
                    : dsaProblems.stream().map(DsaProblem::getTitle).collect(Collectors.joining(", "));
            String storyList = stories.isEmpty()
                    ? "Spring Boot Virtual Threads & FastAPI StreamingResponse"
                    : stories.stream().map(s -> s.getRepository() + " (" + s.getTitle() + ")").collect(Collectors.joining("; "));
            if (isHinglish) {
                return "Raviranjan ki Coding Journey aur Open-Source Contributions:\n" +
                        "• **Authored DSA Notes:** " + dsaList + "\n" +
                        "• **Open-Source Contributions:** " + storyList + "\n" +
                        "• **Platforms:** GitHub, LeetCode, aur GeeksforGeeks connected hain.";
            } else {
                return "Raviranjan's Algorithmic Problem-Solving & Open-Source Contributions:\n" +
                        "• **DSA Case Notes in Raviranjan's portfolio:** " + dsaList + "\n" +
                        "• **Open-Source Contributions:** " + storyList;
            }
        }

        // Fallback comprehensive portfolio overview in requested language
        String projNames = projects.stream().map(Project::getTitle).collect(Collectors.joining(", "));
        String skillNames = skills.stream().map(Skill::getName).limit(8).collect(Collectors.joining(", "));
        if (isDevanagariHindi) {
            return "नमस्ते! मैं रविरंजन कुमार का AI पोर्टफोलियो असिस्टेंट हूँ (Neon DB से जुड़ा हुआ)।\n" +
                    "• **प्रोजेक्ट्स:** " + (projNames.isBlank() ? "RakshaGrid, Raviranjan.OS, Distributed Kafka Ingestion" : projNames) + "\n" +
                    "• **टेक स्टैक:** " + (skillNames.isBlank() ? "Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL" : skillNames) + "\n" +
                    "आप किसी भी प्रोजेक्ट (जैसे 'RakshaGrid समझाओ'), स्किल्स, या एजुकेशन के बारे में हिंदी, हिंग्लिश या इंग्लिश में पूछ सकते हैं!";
        } else if (isHinglish) {
            return "Namaste! Main Raviranjan Kumar ka verified AI Portfolio Assistant hoon.\n\n" +
                    "• **Projects:** " + (projNames.isBlank() ? "[RakshaGrid](/projects/rakshagrid), [Raviranjan.OS](/projects/raviranjan-os), [Kafka Engine](/projects/distributed-kafka)" : projNames) + "\n" +
                    "• **Skills:** " + (skillNames.isBlank() ? "Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL" : skillNames) + "\n\n" +
                    "Aap mujhse kisi bhi project ka explanation (Hindi, Hinglish, ya English me), tech stack, DSA notes, ya career ke baare me pooch sakte hain!";
        } else {
            return "Hello! I am Raviranjan Kumar's portfolio AI assistant, connected live to Neon Cloud PostgreSQL.\n" +
                    "• **Published Projects:** " + (projNames.isBlank() ? "RakshaGrid, Raviranjan.OS, Distributed Kafka Ingestion" : projNames) + "\n" +
                    "• **Core Stack:** " + (skillNames.isBlank() ? "Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL, Docker" : skillNames) + "\n" +
                    "You can ask me to explain any project, architecture, skill, or experience in **English, Hindi (हिंदी), or Hinglish**!";
        }
    }

    private boolean isHindiOrHinglish(String text) {
        String[] hindiKeywords = {
                "kya", "hai", "kaun", "batao", "samjhao", "kaise", "kaha", "kare", "karna", "baare",
                "naam", "banaya", "banaye", "bana", "chahiye", "bhi", "nahi", "karo", "bataye",
                "kaunse", "kisme", "sara", "chij", "dikhao", "hinglish", "hindi", "ke", "ka", "ki", "me", "mein"
        };
        for (String kw : hindiKeywords) {
            if (text.matches(".*\\b" + kw + "\\b.*")) {
                return true;
            }
        }
        return false;
    }
}
