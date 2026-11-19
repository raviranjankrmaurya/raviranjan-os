package com.raviranjan.os.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides what a visitor message is about BEFORE any retrieval or database access happens.
 * Pure Java (no Spring / JPA) so it can be unit tested in isolation.
 *
 * Greetings and acknowledgements never reach retrieval; requests for private data are answered with the
 * standard "not in the portfolio" reply without searching anything.
 */
public final class IntentClassifier {

    public enum Intent { GREETING, ACKNOWLEDGEMENT, PORTFOLIO, TECHNICAL, UNKNOWN }

    public enum Topic { NONE, PROJECTS, TECH_STACK, DATABASE, EDUCATION, EXPERIENCE, ACHIEVEMENTS, CODING, CONTRIBUTIONS, ABOUT, CONTACT }

    public enum Lang { ENGLISH, HINGLISH, HINDI }

    /**
     * @param intent       what kind of message this is
     * @param topic        which part of the portfolio it asks about (NONE = unclear, retrieval decides)
     * @param detailed     the visitor explicitly asked for a long / detailed answer
     * @param lang         language to answer in
     * @param content      the message with greetings and the language tag removed (used for matching names)
     */
    public record Classification(Intent intent, Topic topic, boolean detailed, Lang lang, String content) { }

    private static final Pattern LANG_TAG = Pattern.compile("\\[preferred language:\\s*([a-z]+)\\]", Pattern.CASE_INSENSITIVE);

    private static final List<String> GREETING_PHRASES = List.of(
            "good morning", "good afternoon", "good evening", "good night", "hey there", "hi there", "hello there",
            "how are you", "how r u", "how are u", "what s up", "whats up", "kaise ho", "kaise hain", "kya haal", "kya hal",
            "who are you", "aap kaun ho", "aap kaun hain", "tum kaun ho", "kaun ho aap",
            "\u0906\u092a \u0915\u094c\u0928 \u0939\u0948\u0902", "\u0906\u092a \u0915\u0948\u0938\u0947 \u0939\u0948\u0902");

    private static final Set<String> GREETING_TOKENS = Set.of(
            "hello", "helo", "hlo", "hi", "hii", "hiii", "hey", "heya", "heyy", "hola", "yo", "sup", "namaste", "namaskar", "pranam",
            "greetings", "\u0928\u092e\u0938\u094d\u0924\u0947", "\u0928\u092e\u0938\u094d\u0915\u093e\u0930", "\u0939\u0947\u0932\u094b", "\u0939\u093e\u092f");

    private static final Set<String> FILLERS = Set.of(
            "there", "ji", "bhai", "sir", "mam", "madam", "dear", "assistant", "bot", "ai", "again", "all", "everyone", "team",
            "please", "pls", "plz", "so", "very", "much", "a", "lot", "the", "for", "your", "you", "u", "that", "this", "it", "is", "was");

    private static final Set<String> ACK_STRONG = Set.of(
            "thanks", "thank", "thankyou", "thx", "ty", "tysm", "ok", "okay", "okk", "kk", "cool", "great", "nice", "awesome", "perfect",
            "understood", "noted", "alright", "bye", "goodbye", "shukriya", "dhanyavad", "dhanyawad", "dhanyvaad", "theek", "thik", "accha",
            "achha", "badhiya", "\u0927\u0928\u094d\u092f\u0935\u093e\u0926", "\u0936\u0941\u0915\u094d\u0930\u093f\u092f\u093e", "\u0920\u0940\u0915");

    private static final Set<String> ACK_WEAK = Set.of(
            "got", "get", "good", "fine", "sure", "yes", "yep", "yeah", "help", "info", "information", "hai", "ho", "gaya", "samajh", "samjha", "\u0939\u0948");

    /** Questions the portfolio can never answer (private data). */
    private static final List<String> PRIVATE_PHRASES = List.of(
            "salary", "salaries", "ctc", "lpa", "stipend", "income", "how much does he earn", "how much he earns", "expected pay",
            "phone number", "mobile number", "contact number", "whatsapp number", "home address", "his address", "where does he live exactly",
            "birthday", "birth date", "date of birth", "dob", "how old", "his age", "marital", "married", "girlfriend", "boyfriend", "his wife", "his husband",
            "religion", "caste", "aadhaar", "aadhar", "passport", "password", "otp", "bank account");

    private static final Set<String> DETAIL_TOKENS = Set.of("detailed", "elaborate", "vistar", "vistaar", "thoroughly", "comprehensive");
    private static final List<String> DETAIL_PHRASES = List.of("in detail", "in depth", "step by step", "deep dive", "full explanation", "poori detail", "puri detail", "detail me");

    private static final Set<String> HINGLISH_TOKENS = Set.of(
            "kya", "kaun", "batao", "bataiye", "bataye", "samjhao", "kaise", "kaha", "kahan", "kare", "karna", "baare", "banaya", "banaye", "chahiye",
            "nahi", "karo", "dikhao", "padhai", "kitne", "kaunsa", "kaunse", "mujhe", "unka", "unke", "unki", "hain", "hoon", "kaam", "naam", "kyun", "kyu",
            "shukriya", "dhanyavad", "namaste", "aap", "tum", "hinglish");

    private static final Set<String> DB_TOKENS = Set.of("database", "databases", "db", "sql", "nosql", "postgres", "postgresql", "mysql", "mongodb", "mongo", "redis", "datastore", "dbms");
    private static final Set<String> PROJECT_TOKENS = Set.of("project", "projects", "built", "build", "made", "apps", "app", "applications", "application", "banaya", "banaye", "prajekt", "\u092a\u094d\u0930\u094b\u091c\u0947\u0915\u094d\u091f", "\u092a\u094d\u0930\u094b\u091c\u0947\u0915\u094d\u091f\u094d\u0938");
    private static final Set<String> STACK_TOKENS = Set.of("stack", "technologies", "technology", "tech", "skills", "skill", "tools", "tool", "frameworks", "framework", "languages", "language", "proficient", "expertise", "\u0938\u094d\u0915\u093f\u0932\u094d\u0938");
    private static final Set<String> EDU_TOKENS = Set.of("education", "college", "university", "degree", "study", "studying", "studies", "bca", "btech", "graduation", "school", "qualification", "qualifications", "padhai", "\u0936\u093f\u0915\u094d\u0937\u093e");
    private static final Set<String> EXP_TOKENS = Set.of("experience", "experiences", "intern", "internship", "internships", "job", "jobs", "company", "companies", "career", "employment", "worked", "anubhav", "\u0905\u0928\u0941\u092d\u0935");
    private static final Set<String> ACH_TOKENS = Set.of("achievement", "achievements", "award", "awards", "certification", "certifications", "certificate", "certificates", "certified", "hackathon", "winner");
    private static final Set<String> CODING_TOKENS = Set.of("dsa", "leetcode", "gfg", "geeksforgeeks", "algorithm", "algorithms", "competitive", "codeforces", "solved");
    private static final Set<String> CONTRIB_TOKENS = Set.of("contribution", "contributions", "contribute", "contributed", "opensource", "pr", "prs");
    private static final Set<String> CONTACT_TOKENS = Set.of("contact", "reach", "hire", "email", "mail", "connect", "linkedin");
    private static final Set<String> TECHNICAL_WORDS = Set.of("architecture", "backend", "frontend", "api", "microservice", "microservices", "programming", "code", "coding", "framework", "docker", "kubernetes", "java", "python", "react", "spring", "fastapi", "deploy", "deployment", "cache", "caching");

    private IntentClassifier() { }

    public static Classification classify(String raw) {
        String text = raw == null ? "" : raw;
        Lang preferred = null;
        Matcher m = LANG_TAG.matcher(text);
        if (m.find()) {
            String p = m.group(1).toLowerCase(Locale.ROOT);
            preferred = p.equals("hindi") ? Lang.HINDI : p.equals("hinglish") ? Lang.HINGLISH : p.equals("english") ? Lang.ENGLISH : null;
            text = LANG_TAG.matcher(text).replaceAll(" ");
        }
        String norm = normalize(text);
        Lang lang = preferred != null ? preferred : detectLang(text, norm);

        if (norm.isEmpty()) {
            return new Classification(Intent.UNKNOWN, Topic.NONE, false, lang, "");
        }
        if (containsAnyPhrase(norm, PRIVATE_PHRASES)) {
            return new Classification(Intent.UNKNOWN, Topic.NONE, false, lang, norm);
        }

        // --- greeting: nothing but greeting words / phrases
        String rest = " " + norm + " ";
        boolean greeted = false;
        for (String phrase : GREETING_PHRASES) {
            String needle = " " + phrase + " ";
            while (rest.contains(needle)) { rest = rest.replace(needle, " "); greeted = true; }
        }
        List<String> leftover = new ArrayList<>();
        for (String tok : rest.trim().split(" ")) {
            if (tok.isEmpty()) continue;
            if (GREETING_TOKENS.contains(tok)) { greeted = true; continue; }
            if (FILLERS.contains(tok)) continue;
            leftover.add(tok);
        }
        if (greeted && leftover.isEmpty()) {
            return new Classification(Intent.GREETING, Topic.NONE, false, lang, "");
        }

        // --- acknowledgement: thanks / ok / got it ...
        boolean strongAck = false, allAck = true;
        for (String tok : norm.split(" ")) {
            if (ACK_STRONG.contains(tok)) strongAck = true;
            else if (!(ACK_WEAK.contains(tok) || FILLERS.contains(tok) || GREETING_TOKENS.contains(tok))) allAck = false;
        }
        if (strongAck && allAck) {
            return new Classification(Intent.ACKNOWLEDGEMENT, Topic.NONE, false, lang, "");
        }

        // --- portfolio / technical (greeting words in front of a real question are ignored)
        String content = greeted ? String.join(" ", leftover) : norm;
        Set<String> tokens = Set.copyOf(Arrays.asList(content.split(" ")));
        boolean detailed = !java.util.Collections.disjoint(tokens, DETAIL_TOKENS) || containsAnyPhrase(content, DETAIL_PHRASES);

        Topic topic = Topic.NONE;
        if (hasAny(tokens, DB_TOKENS)) topic = Topic.DATABASE;
        else if (hasAny(tokens, PROJECT_TOKENS)) topic = Topic.PROJECTS;
        else if (hasAny(tokens, STACK_TOKENS)) topic = Topic.TECH_STACK;
        else if (hasAny(tokens, EDU_TOKENS)) topic = Topic.EDUCATION;
        else if (hasAny(tokens, EXP_TOKENS)) topic = Topic.EXPERIENCE;
        else if (hasAny(tokens, ACH_TOKENS)) topic = Topic.ACHIEVEMENTS;
        else if (hasAny(tokens, CODING_TOKENS)) topic = Topic.CODING;
        else if (hasAny(tokens, CONTRIB_TOKENS) || content.contains("open source")) topic = Topic.CONTRIBUTIONS;
        else if (hasAny(tokens, CONTACT_TOKENS)) topic = Topic.CONTACT;
        else if (content.contains("who is") || content.contains("about him") || content.contains("about raviranjan") || tokens.contains("introduce") || tokens.contains("bio")) topic = Topic.ABOUT;

        boolean technical = topic == Topic.TECH_STACK || topic == Topic.DATABASE || hasAny(tokens, TECHNICAL_WORDS);
        return new Classification(technical ? Intent.TECHNICAL : Intent.PORTFOLIO, topic, detailed, lang, content);
    }

    /** lower-case, strip punctuation and emoji, keep letters (incl. Devanagari marks) and digits. */
    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\p{M}\\s]+", " ");
        return t.replaceAll("\\s+", " ").trim();
    }

    private static Lang detectLang(String original, String norm) {
        for (int i = 0; i < original.length(); i++) {
            if (Character.UnicodeBlock.of(original.charAt(i)) == Character.UnicodeBlock.DEVANAGARI) return Lang.HINDI;
        }
        for (String tok : norm.split(" ")) {
            if (HINGLISH_TOKENS.contains(tok)) return Lang.HINGLISH;
        }
        return Lang.ENGLISH;
    }

    private static boolean hasAny(Set<String> tokens, Set<String> vocabulary) {
        return !java.util.Collections.disjoint(tokens, vocabulary);
    }

    private static boolean containsAnyPhrase(String norm, List<String> phrases) {
        String padded = " " + norm + " ";
        for (String p : phrases) {
            if (padded.contains(" " + p + " ")) return true;
        }
        return false;
    }
}
