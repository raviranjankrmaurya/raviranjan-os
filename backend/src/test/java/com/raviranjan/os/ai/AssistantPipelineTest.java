package com.raviranjan.os.ai;

import com.raviranjan.os.ai.IntentClassifier.Classification;
import com.raviranjan.os.ai.IntentClassifier.Intent;
import com.raviranjan.os.ai.IntentClassifier.Topic;
import com.raviranjan.os.ai.PortfolioFacts.ProjectFact;
import com.raviranjan.os.ai.PortfolioFacts.SkillFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests (no Spring context, no database) for intent routing, answer building and clean-up. */
class AssistantPipelineTest {

    private static PortfolioFacts facts() {
        List<ProjectFact> projects = List.of(
                new ProjectFact("RakshaGrid", "rakshagrid", "AI-powered disaster response platform. It predicts risk zones.", "", "Event-driven", "PUBLISHED", 80,
                        "https://github.com/raviranjankrmaurya/disaster", null, List.of("Java 21 / Spring Boot", "PostgreSQL", "Docker", "Postgres", "Redis")),
                new ProjectFact("Raviranjan.OS", "raviranjan-os", "AI-native developer portfolio.", "", "", "PUBLISHED", 90,
                        "https://github.com/raviranjankrmaurya/raviranjan-os", null, List.of("Java", "PostgreSQL")));
        List<SkillFact> skills = List.of(new SkillFact("Java", "BACKEND", true, 90), new SkillFact("PostgreSQL", "DATABASE", true, 80),
                new SkillFact("Postgres", "DATABASE", false, 70), new SkillFact("Docker", "DEVOPS", false, 60), new SkillFact("Docker", "DEVOPS", false, 50), new SkillFact("Redis", "DATABASE", false, 40));
        return new PortfolioFacts(null, projects, skills, null, null, null, null, null, null);
    }

    private static String answer(String question) {
        Classification c = IntentClassifier.classify(question);
        switch (c.intent()) {
            case GREETING: return AnswerBuilder.greeting(c.lang());
            case ACKNOWLEDGEMENT: return AnswerBuilder.acknowledgement(c.lang());
            case UNKNOWN: return AssistantIdentity.UNKNOWN_ANSWER;
            default:
                String built = AnswerBuilder.build(c, facts());
                return built == null ? AssistantIdentity.UNKNOWN_ANSWER : built;
        }
    }

    @Test
    void greetingsAreClassifiedWithoutRetrieval() {
        for (String g : new String[] {"Hello", "Hi", "hey!!", "Good morning", "Namaste"}) {
            assertEquals(Intent.GREETING, IntentClassifier.classify(g).intent(), g);
            assertTrue(answer(g).contains(AssistantIdentity.NAME), g);
            assertFalse(answer(g).contains("RakshaGrid"), g);
        }
    }

    @Test
    void acknowledgementsAreShort() {
        for (String g : new String[] {"Thank you", "thanks!", "ok", "got it, thanks"}) {
            assertEquals(Intent.ACKNOWLEDGEMENT, IntentClassifier.classify(g).intent(), g);
            assertTrue(answer(g).length() < 130, g);
        }
    }

    @Test
    void greetingFollowedByAQuestionIsTreatedAsAQuestion() {
        Classification c = IntentClassifier.classify("Hello, what projects has he built?");
        assertEquals(Intent.PORTFOLIO, c.intent());
        assertEquals(Topic.PROJECTS, c.topic());
    }

    @Test
    void privateQuestionsReturnTheExactFallback() {
        for (String q : new String[] {"What is his salary?", "what is his phone number", "How old is he?"}) {
            assertEquals("I don't have that information in Raviranjan's portfolio.", answer(q), q);
        }
    }

    @Test
    void projectListShowsNamesOnly() {
        String a = answer("What projects has he built?");
        assertTrue(a.contains("RakshaGrid") && a.contains("Raviranjan.OS"));
        assertFalse(a.contains("Docker"));
    }

    @Test
    void projectExplanationIsFocusedAndLinked() {
        String a = answer("Explain RakshaGrid");
        assertTrue(a.contains("github.com/raviranjankrmaurya/disaster"));
        assertEquals(a.indexOf("PostgreSQL"), a.lastIndexOf("PostgreSQL"));
        assertFalse(a.contains("Raviranjan.OS"));
    }

    @Test
    void techStackAndDatabaseAnswersHaveNoDuplicates() {
        String stack = answer("What is his tech stack?");
        assertEquals(stack.indexOf("PostgreSQL"), stack.lastIndexOf("PostgreSQL"));
        assertEquals(stack.indexOf("Docker"), stack.lastIndexOf("Docker"));
        String db = answer("Which database does he use?");
        assertTrue(db.contains("PostgreSQL") && db.contains("Redis"));
        assertFalse(db.contains("Java"));
    }

    @Test
    void missingDataIsNeverInvented() {
        Classification c = IntentClassifier.classify("What is his education?");
        assertEquals(AssistantIdentity.UNKNOWN_ANSWER, AnswerBuilder.build(c, PortfolioFacts.empty()));
        assertEquals(AssistantIdentity.UNKNOWN_ANSWER, AnswerBuilder.fromChunks(List.of(), false));
    }

    @Test
    void responsePolicyRemovesRepetitionAndNormalisesIdentity() {
        String messy = "Sure! I am Ask AI, Raviranjan Kumar's portfolio AI assistant. He uses Java, Docker, PostgreSQL, Docker, Postgres. Data is stored in Neon DB.";
        String clean = ResponsePolicy.apply(messy, false);
        assertFalse(clean.contains("Ask AI"));
        assertFalse(clean.toLowerCase().contains("neon"));
        assertEquals(clean.indexOf("Docker"), clean.lastIndexOf("Docker"));
        assertEquals(AssistantIdentity.UNKNOWN_ANSWER, ResponsePolicy.apply("Sorry, I do not have any information about that in the context.", false));
        assertNull(ResponsePolicy.apply("   ", false));
    }
}
