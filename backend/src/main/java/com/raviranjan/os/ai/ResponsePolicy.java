package com.raviranjan.os.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Final clean-up applied to every answer that comes from a language model: one identity, no filler, no repeated
 * sentences / list items / technologies, no infrastructure leaks, and a short default length.
 */
public final class ResponsePolicy {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?\u0964])\\s+(?=[\\p{Lu}\\p{Lo}\\[\\(\"'*\u2022\\-])");
    private static final Pattern FILLER_OPENING = Pattern.compile("^(?:(?:sure|certainly|of course|absolutely|great question|good question|here(?:'s| is) (?:the )?(?:answer|information)|as an ai[^.]*)[!,.:\\-\\s]+)+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNKNOWN_LIKE = Pattern.compile("(?i)(?:don't|do not|doesn't|does not|cannot|can't|no)\\s+(?:have|contain|find|know|see)[^.!?]{0,60}(?:information|info|details|data)|not\\s+(?:mentioned|available|listed|provided|found)\\s+in");
    private static final Pattern IDENTITY_VARIANTS = Pattern.compile("(?i)\\bask\\s+ai\\b|\\bask\\s+assistant\\b|raviranjan(?:\\s+kumar)?'s\\s+(?:verified\\s+)?(?:portfolio\\s+)?(?:ai\\s+)?assistant|(?:portfolio\\s+)?ai\\s+assistant\\s+(?:for|of)\\s+raviranjan(?:\\s+kumar)?");
    private static final Pattern IMPERSONATION = Pattern.compile("(?i)\\b(?:i\\s+am|i'm)\\s+raviranjan(?:\\s+kumar)?\\b");
    private static final Pattern INFRA_LEAK = Pattern.compile("(?i)\\bneon\\b|connected live to|my (?:own )?(?:database|backend)");

    private ResponsePolicy() { }

    public static List<String> splitSentences(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String s : SENTENCE_SPLIT.split(text.trim())) {
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    /** @return the cleaned answer, the standard unknown reply, or {@code null} if the input was empty. */
    public static String apply(String raw, boolean detailed) {
        if (raw == null || raw.isBlank()) return null;
        String text = raw.replace("\r", "").trim();

        if (text.length() < 220 && UNKNOWN_LIKE.matcher(text).find()) return AssistantIdentity.UNKNOWN_ANSWER;

        text = IMPERSONATION.matcher(text).replaceAll(java.util.regex.Matcher.quoteReplacement("I'm " + AssistantIdentity.NAME));
        text = IDENTITY_VARIANTS.matcher(text).replaceAll(java.util.regex.Matcher.quoteReplacement(AssistantIdentity.NAME));
        // two variants next to each other ("Ask AI, Raviranjan's assistant") must collapse into ONE identity
        String id = Pattern.quote(AssistantIdentity.NAME);
        text = text.replaceAll("(?i)" + id + "(?:\\s*(?:,|and|\\(|\\)|-|\u2013)\\s*" + id + ")+", java.util.regex.Matcher.quoteReplacement(AssistantIdentity.NAME));
        text = FILLER_OPENING.matcher(text).replaceFirst("").trim();

        List<String> out = new ArrayList<>();
        Set<String> seenLines = new LinkedHashSet<>();
        int proseBudget = detailed ? 10 : 3;
        int bulletBudget = detailed ? 12 : 6;
        int bullets = 0;

        for (String line : text.split("\n")) {
            String l = line.trim();
            if (l.isEmpty()) continue;
            boolean bullet = l.startsWith("- ") || l.startsWith("* ") || l.startsWith("\u2022");
            if (bullet) {
                if (bullets >= bulletBudget) continue;
                if (INFRA_LEAK.matcher(l).find()) continue;
                String key = IntentClassifier.normalize(l);
                if (!seenLines.add(key)) continue;
                out.add(dedupeCommaList(l));
                bullets++;
                continue;
            }
            List<String> kept = new ArrayList<>();
            for (String s : splitSentences(l)) {
                if (INFRA_LEAK.matcher(s).find()) continue;
                String key = IntentClassifier.normalize(s);
                if (key.isEmpty() || !seenLines.add(key)) continue;
                if (proseBudget <= 0) break;
                kept.add(dedupeCommaList(s));
                proseBudget--;
            }
            if (!kept.isEmpty()) out.add(String.join(" ", kept));
        }
        String joined = String.join("\n", out).trim();
        return joined.isEmpty() ? null : joined;
    }

    /** "Java, Docker, PostgreSQL, Docker, Postgres" -> "Java, Docker, PostgreSQL" (only for list-like sentences). */
    static String dedupeCommaList(String sentence) {
        String[] parts = sentence.split(",\\s*");
        if (parts.length < 4) return sentence;
        Map<String, String> seen = new LinkedHashMap<>();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            boolean middle = i > 0 && i < parts.length - 1;
            if (middle && part.split("\\s+").length > 4) return sentence;     // prose, not a list
            String key = TechNames.key(part.replaceFirst("(?i)^and\\s+", ""));
            if (key.isEmpty()) { seen.put("#" + i, part); continue; }
            String canon = TechNames.canonical(part.replaceFirst("(?i)^and\\s+", ""));
            if (!middle) { seen.putIfAbsent(i == 0 ? "#0" : key, part); continue; }
            seen.putIfAbsent(TechNames.key(canon), part);
        }
        // a trailing "and X" that repeats an earlier item is dropped
        List<String> items = new ArrayList<>(seen.values());
        if (items.size() >= 2) {
            String last = items.get(items.size() - 1);
            String lastKey = TechNames.key(last.replaceFirst("(?i)^and\\s+", "").replaceFirst("[.!?]+$", ""));
            for (int i = 0; i < items.size() - 1; i++) {
                if (!lastKey.isEmpty() && TechNames.key(items.get(i).replaceFirst("[.!?]+$", "")).equals(lastKey)) {
                    items.remove(items.size() - 1);
                    String tail = items.get(items.size() - 1);
                    if (!tail.matches(".*[.!?]$")) items.set(items.size() - 1, tail + ".");
                    break;
                }
            }
        }
        return String.join(", ", items);
    }
}
