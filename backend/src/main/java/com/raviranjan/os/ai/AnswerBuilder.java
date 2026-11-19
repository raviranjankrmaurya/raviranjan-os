package com.raviranjan.os.ai;

import com.raviranjan.os.ai.IntentClassifier.Classification;
import com.raviranjan.os.ai.IntentClassifier.Lang;
import com.raviranjan.os.ai.IntentClassifier.Topic;
import com.raviranjan.os.ai.PortfolioFacts.AchievementFact;
import com.raviranjan.os.ai.PortfolioFacts.CertificationFact;
import com.raviranjan.os.ai.PortfolioFacts.ChunkFact;
import com.raviranjan.os.ai.PortfolioFacts.DsaFact;
import com.raviranjan.os.ai.PortfolioFacts.EducationFact;
import com.raviranjan.os.ai.PortfolioFacts.ExperienceFact;
import com.raviranjan.os.ai.PortfolioFacts.ProjectFact;
import com.raviranjan.os.ai.PortfolioFacts.SkillFact;
import com.raviranjan.os.ai.PortfolioFacts.StoryFact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds SHORT, grounded answers from portfolio facts. Nothing here is invented: if the facts for a topic are
 * missing the standard "not in the portfolio" reply is returned, never a guess.
 */
public final class AnswerBuilder {

    private static final int MAX_LIST = 6;

    private AnswerBuilder() { }

    // ------------------------------------------------------------------ canned conversational replies

    public static String greeting(Lang lang) {
        return switch (lang) {
            case HINDI -> "\u0928\u092e\u0938\u094d\u0924\u0947! \u092e\u0948\u0902 " + AssistantIdentity.NAME + " \u0939\u0942\u0901\u0964 \u0906\u092a \u0909\u0928\u0915\u0947 \u092a\u094d\u0930\u094b\u091c\u0947\u0915\u094d\u091f\u094d\u0938, \u0938\u094d\u0915\u093f\u0932\u094d\u0938, \u0936\u093f\u0915\u094d\u0937\u093e \u092f\u093e \u0905\u0928\u0941\u092d\u0935 \u0915\u0947 \u092c\u093e\u0930\u0947 \u092e\u0947\u0902 \u092a\u0942\u091b \u0938\u0915\u0924\u0947 \u0939\u0948\u0902\u0964";
            case HINGLISH -> "Namaste! Main " + AssistantIdentity.NAME + " hoon. Aap unke projects, skills, education ya experience ke baare me pooch sakte hain.";
            default -> "Hello! I'm " + AssistantIdentity.NAME + ". Ask me about his projects, skills, education or experience.";
        };
    }

    public static String acknowledgement(Lang lang) {
        return switch (lang) {
            case HINDI -> "\u0906\u092a\u0915\u093e \u0938\u094d\u0935\u093e\u0917\u0924 \u0939\u0948! \u0915\u0941\u091b \u0914\u0930 \u091c\u093e\u0928\u0928\u093e \u0939\u094b \u0924\u094b \u092c\u0924\u093e\u0907\u090f\u0964";
            case HINGLISH -> "Welcome! Kuch aur jaanna ho to bata dijiye.";
            default -> "You're welcome! Let me know if you'd like to know anything else about Raviranjan's work.";
        };
    }

    // ------------------------------------------------------------------ structured answers

    /**
     * @return the answer, {@link AssistantIdentity#UNKNOWN_ANSWER} when the topic is clear but the data is missing,
     *         or {@code null} when the topic is unclear (caller then tries retrieved chunks).
     */
    public static String build(Classification c, PortfolioFacts f) {
        Lang lang = c.lang();
        ProjectFact named = findNamedProject(c.content(), f.projects());

        if (named != null) {
            if (c.topic() == Topic.DATABASE) {
                List<String> dbs = TechNames.unique(named.technologies()).stream().filter(TechNames::isDatabase).toList();
                return dbs.isEmpty() ? AssistantIdentity.UNKNOWN_ANSWER
                        : named.title() + " " + String.format(p(lang, "uses %s.", "%s use karta hai.", "%s का उपयोग करता है।"), joinAnd(dbs, lang));
            }
            if (c.topic() == Topic.TECH_STACK) {
                List<String> techs = TechNames.unique(named.technologies());
                return techs.isEmpty() ? AssistantIdentity.UNKNOWN_ANSWER : builtWith(named, techs, lang);
            }
            return projectDetail(named, c.detailed(), lang);
        }

        return switch (c.topic()) {
            case PROJECTS -> projectList(f.projects(), lang);
            case TECH_STACK -> techStack(f, c.detailed(), lang);
            case DATABASE -> databases(f, lang);
            case EDUCATION -> education(f.education(), lang);
            case EXPERIENCE -> experience(f.experience(), lang);
            case ACHIEVEMENTS -> achievements(f.achievements(), f.certifications(), lang);
            case CODING -> coding(f.dsa(), lang);
            case CONTRIBUTIONS -> contributions(f.stories(), lang);
            case ABOUT -> about(f, lang);
            case CONTACT -> p(lang, "You can reach him through the Contact section of this website.",
                    "Aap is website ke Contact section se unse sampark kar sakte hain.",
                    "आप इस वेबसाइट के Contact सेक्शन से उनसे संपर्क कर सकते हैं।");
            case NONE -> null;
        };
    }

    /** Short answer from retrieved chunks (used when the question names no specific topic). */
    public static String fromChunks(List<ChunkFact> chunks, boolean detailed) {
        if (chunks == null || chunks.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        List<String> sentences = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String link = null;
        int limit = detailed ? 5 : 2;
        for (ChunkFact ch : chunks) {
            for (String s : ResponsePolicy.splitSentences(clean(ch.content()))) {
                String k = IntentClassifier.normalize(s);
                if (k.length() < 12 || !seen.add(k)) continue;
                sentences.add(s.trim());
                if (link == null && isHttp(ch.url())) link = ch.url();
                if (sentences.size() >= limit) break;
            }
            if (sentences.size() >= limit) break;
        }
        if (sentences.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        String text = String.join(" ", sentences);
        return link != null ? text + " [More](" + link + ")" : text;
    }

    // ------------------------------------------------------------------ topics

    private static String projectList(List<ProjectFact> projects, Lang lang) {
        if (projects.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(p(lang, "He has built %d project%s:", "Unhone %d project%s banaye hain:", "उन्होंने %d प्रोजेक्ट%s बनाए हैं:"),
                projects.size(), lang == Lang.ENGLISH && projects.size() != 1 ? "s" : (lang == Lang.HINGLISH && projects.size() != 1 ? "s" : "")));
        int shown = 0;
        for (ProjectFact pr : projects) {
            if (shown++ >= MAX_LIST) break;
            sb.append("\n- ").append(titleLink(pr));
            String s = clip(firstSentence(pr.summary()), 90);
            if (!s.isEmpty()) sb.append(" \u2013 ").append(s);
        }
        if (projects.size() > MAX_LIST) {
            sb.append("\n").append(String.format(p(lang, "...and %d more.", "...aur %d aur.", "...और %d।"), projects.size() - MAX_LIST));
        }
        return sb.toString();
    }

    private static String projectDetail(ProjectFact pr, boolean detailed, Lang lang) {
        List<String> parts = new ArrayList<>();
        String summary = firstSentence(pr.summary());
        if (summary.isEmpty()) summary = firstSentence(pr.description());
        parts.add(titleLink(pr) + (summary.isEmpty() ? "" : " \u2013 " + endSentence(summary)));
        List<String> techs = TechNames.unique(pr.technologies());
        if (!techs.isEmpty()) parts.add(builtWith(pr, techs, lang));
        if (detailed) {
            if (notBlank(pr.architecture())) parts.add(p(lang, "Architecture: ", "Architecture: ", "आर्किटेक्चर: ") + endSentence(clip(pr.architecture(), 200)));
            String more = firstSentence(pr.description());
            if (!more.isEmpty() && !more.equalsIgnoreCase(summary)) parts.add(endSentence(clip(more, 220)));
            if (notBlank(pr.status())) parts.add(p(lang, "Status: ", "Status: ", "स्थिति: ") + pr.status().toLowerCase(Locale.ROOT) + " (" + pr.progress() + "%).");
        }
        if (isHttp(pr.liveUrl()) && detailed) parts.add(p(lang, "Live: ", "Live: ", "लाइव: ") + "[demo](" + pr.liveUrl() + ").");
        return String.join(" ", parts);
    }

    private static String techStack(PortfolioFacts f, boolean detailed, Lang lang) {
        List<String> names = new ArrayList<>();
        f.skills().stream()
                .sorted(Comparator.comparing(SkillFact::featured).reversed().thenComparing(Comparator.comparingInt(SkillFact::proficiency).reversed()))
                .forEach(s -> names.add(s.name()));
        if (names.isEmpty()) f.projects().forEach(pr -> names.addAll(pr.technologies()));
        List<String> unique = TechNames.unique(names);
        if (unique.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        int cap = detailed ? 20 : 12;
        List<String> shown = unique.subList(0, Math.min(cap, unique.size()));
        return String.format(p(lang, "His main technologies include %s.", "Unki main technologies hain: %s.", "उनकी मुख्य तकनीकें हैं: %s।"), joinAnd(shown, lang));
    }

    private static String databases(PortfolioFacts f, Lang lang) {
        List<String> all = new ArrayList<>();
        f.skills().forEach(s -> all.add(s.name()));
        f.projects().forEach(pr -> all.addAll(pr.technologies()));
        List<String> dbs = TechNames.unique(all).stream().filter(TechNames::isDatabase).toList();
        if (dbs.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        return String.format(p(lang, "He works with %s.", "Wo %s ke saath kaam karte hain.", "वे %s के साथ काम करते हैं।"), joinAnd(dbs, lang));
    }

    private static String education(List<EducationFact> list, Lang lang) {
        if (list.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        List<String> lines = list.stream().limit(3).map(e -> {
            String what = notBlank(e.field()) ? e.degree() + " (" + e.field() + ")" : e.degree();
            String when = period(e.start(), e.end(), lang);
            return what + (notBlank(e.institution()) ? ", " + e.institution() : "") + (when.isEmpty() ? "" : " " + when);
        }).toList();
        return list(lines, p(lang, "Education:", "Education:", "शिक्षा:"));
    }

    private static String experience(List<ExperienceFact> list, Lang lang) {
        if (list.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        List<String> lines = list.stream().limit(3).map(e -> {
            String when = e.current() ? p(lang, "(current)", "(current)", "(वर्तमान)") : period(e.start(), e.end(), lang);
            return e.role() + (notBlank(e.company()) ? " " + p(lang, "at", "at", "—") + " " + e.company() : "") + (when.isEmpty() ? "" : " " + when);
        }).toList();
        return list(lines, p(lang, "Experience:", "Experience:", "अनुभव:"));
    }

    private static String achievements(List<AchievementFact> ach, List<CertificationFact> certs, Lang lang) {
        if (ach.isEmpty() && certs.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        List<String> parts = new ArrayList<>();
        if (!ach.isEmpty()) {
            parts.add(p(lang, "Achievements: ", "Achievements: ", "उपलब्धियाँ: ") + joinAnd(ach.stream().limit(5)
                    .map(a -> notBlank(a.organization()) ? a.title() + " (" + a.organization() + ")" : a.title()).toList(), lang) + ".");
        }
        if (!certs.isEmpty()) {
            parts.add(p(lang, "Certifications: ", "Certifications: ", "प्रमाणपत्र: ") + joinAnd(certs.stream().limit(5)
                    .map(cf -> notBlank(cf.issuer()) ? cf.title() + " (" + cf.issuer() + ")" : cf.title()).toList(), lang) + ".");
        }
        return String.join(" ", parts);
    }

    private static String coding(List<DsaFact> dsa, Lang lang) {
        if (dsa.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        List<String> titles = dsa.stream().map(DsaFact::title).distinct().limit(4).toList();
        return String.format(p(lang, "He has documented %d DSA problem%s, including %s.", "Unhone %d DSA problem%s document kiye hain, jaise %s.", "उन्होंने %d DSA समस्या%s दर्ज की हैं, जैसे %s।"),
                dsa.size(), lang == Lang.ENGLISH && dsa.size() != 1 ? "s" : "", joinAnd(titles, lang));
    }

    private static String contributions(List<StoryFact> stories, Lang lang) {
        if (stories.isEmpty()) return AssistantIdentity.UNKNOWN_ANSWER;
        List<String> lines = stories.stream().limit(3).map(s -> {
            String label = notBlank(s.repository()) ? s.repository() + " \u2013 " + s.title() : s.title();
            return isHttp(s.prUrl()) ? "[" + label + "](" + s.prUrl() + ")" : label;
        }).toList();
        return list(lines, p(lang, "Open-source contributions:", "Open-source contributions:", "ओपन-सोर्स योगदान:"));
    }

    private static String about(PortfolioFacts f, Lang lang) {
        PortfolioFacts.ProfileFact pf = f.profile();
        if (pf == null || (!notBlank(pf.headline()) && !notBlank(pf.bio()))) return AssistantIdentity.UNKNOWN_ANSWER;
        String name = notBlank(pf.fullName()) ? pf.fullName() : "Raviranjan";
        String headline = notBlank(pf.headline()) ? pf.headline().trim() : "";
        String bio = firstSentence(pf.bio());
        StringBuilder sb = new StringBuilder(name);
        if (!headline.isEmpty()) sb.append(" \u2013 ").append(headline).append(".");
        else sb.append(".");
        if (!bio.isEmpty() && !bio.equalsIgnoreCase(headline)) sb.append(" ").append(endSentence(bio));
        return sb.toString();
    }

    // ------------------------------------------------------------------ helpers

    static ProjectFact findNamedProject(String normalizedQuery, List<ProjectFact> projects) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) return null;
        String squashed = normalizedQuery.replaceAll("[^a-z0-9]", "");
        ProjectFact best = null;
        int bestLen = 0;
        for (ProjectFact pr : projects) {
            for (String candidate : new String[] { pr.title(), pr.slug() }) {
                if (candidate == null) continue;
                String key = IntentClassifier.normalize(candidate).replaceAll("[^a-z0-9]", "");
                if (key.length() >= 4 && squashed.contains(key) && key.length() > bestLen) { best = pr; bestLen = key.length(); }
            }
        }
        return best;
    }

    private static String builtWith(ProjectFact pr, List<String> techs, Lang lang) {
        return p(lang, "Built with ", "Isme ", "इसमें ") + joinAnd(techs.subList(0, Math.min(8, techs.size())), lang) + p(lang, ".", " use hua hai.", " का उपयोग हुआ है।");
    }

    private static String titleLink(ProjectFact pr) {
        return isHttp(pr.githubUrl()) ? "[" + pr.title() + "](" + pr.githubUrl() + ")" : pr.title();
    }

    private static String list(List<String> lines, String heading) {
        if (lines.size() == 1) return heading + " " + endSentence(lines.get(0));
        return heading + lines.stream().map(l -> "\n- " + l).collect(Collectors.joining());
    }

    private static String period(String start, String end, Lang lang) {
        String s = year(start), e = year(end);
        if (s.isEmpty() && e.isEmpty()) return "";
        return "(" + (s.isEmpty() ? "?" : s) + "\u2013" + (e.isEmpty() ? p(lang, "present", "present", "वर्तमान") : e) + ")";
    }

    private static String year(String date) {
        return date != null && date.length() >= 4 ? date.substring(0, 4) : "";
    }

    static String joinAnd(List<String> items, Lang lang) {
        String and = p(lang, "and", "aur", "और");
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        return String.join(", ", items.subList(0, items.size() - 1)) + " " + and + " " + items.get(items.size() - 1);
    }

    static String firstSentence(String text) {
        String t = clean(text);
        if (t.isEmpty()) return "";
        List<String> sentences = ResponsePolicy.splitSentences(t);
        return sentences.isEmpty() ? t : sentences.get(0).trim();
    }

    static String clip(String text, int max) {
        if (text == null) return "";
        String t = text.trim();
        if (t.length() <= max) return t;
        int cut = t.lastIndexOf(' ', max);
        return t.substring(0, cut > max / 2 ? cut : max).trim() + "...";
    }

    private static String endSentence(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) return t;
        char last = t.charAt(t.length() - 1);
        return (last == '.' || last == '!' || last == '?' || last == '\u0964' || last == ')') ? t : t + ".";
    }

    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("[*_`#]+", "").replaceAll("\\s+", " ").trim();
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static boolean isHttp(String url) { return url != null && (url.startsWith("https://") || url.startsWith("http://")); }

    private static String p(Lang lang, String en, String hinglish, String hindi) {
        return switch (lang) { case HINDI -> hindi; case HINGLISH -> hinglish; default -> en; };
    }
}
