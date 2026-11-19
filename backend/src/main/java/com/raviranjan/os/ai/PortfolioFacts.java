package com.raviranjan.os.ai;

import java.util.List;

/**
 * Plain, read-only snapshot of the PUBLIC portfolio data needed to answer one question.
 * Only the lists relevant to the question are filled; the rest stay empty.
 */
public record PortfolioFacts(
        ProfileFact profile,
        List<ProjectFact> projects,
        List<SkillFact> skills,
        List<EducationFact> education,
        List<ExperienceFact> experience,
        List<AchievementFact> achievements,
        List<CertificationFact> certifications,
        List<DsaFact> dsa,
        List<StoryFact> stories) {

    public PortfolioFacts {
        projects = projects == null ? List.of() : projects;
        skills = skills == null ? List.of() : skills;
        education = education == null ? List.of() : education;
        experience = experience == null ? List.of() : experience;
        achievements = achievements == null ? List.of() : achievements;
        certifications = certifications == null ? List.of() : certifications;
        dsa = dsa == null ? List.of() : dsa;
        stories = stories == null ? List.of() : stories;
    }

    public static PortfolioFacts empty() {
        return new PortfolioFacts(null, null, null, null, null, null, null, null, null);
    }

    public record ProfileFact(String fullName, String headline, String bio) { }

    public record ProjectFact(String title, String slug, String summary, String description, String architecture,
                              String status, int progress, String githubUrl, String liveUrl, List<String> technologies) {
        public ProjectFact {
            technologies = technologies == null ? List.of() : technologies;
        }
    }

    public record SkillFact(String name, String category, boolean featured, int proficiency) { }

    public record EducationFact(String degree, String field, String institution, String start, String end) { }

    public record ExperienceFact(String role, String company, boolean current, String start, String end) { }

    public record AchievementFact(String title, String organization) { }

    public record CertificationFact(String title, String issuer) { }

    public record DsaFact(String title, String platform, String difficulty) { }

    public record StoryFact(String repository, String title, String prUrl) { }

    /** A retrieved knowledge chunk, reduced to what an answer needs. */
    public record ChunkFact(String title, String url, String content) { }
}
