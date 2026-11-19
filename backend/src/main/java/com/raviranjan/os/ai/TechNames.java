package com.raviranjan.os.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Splits, canonicalises and de-duplicates technology names ("Postgres" == "PostgreSQL", "Java 21 / Spring Boot" -> two items). */
public final class TechNames {

    private static final Map<String, String> CANONICAL = Map.ofEntries(
            Map.entry("postgres", "PostgreSQL"), Map.entry("postgresql", "PostgreSQL"), Map.entry("psql", "PostgreSQL"),
            Map.entry("mongo", "MongoDB"), Map.entry("mongodb", "MongoDB"),
            Map.entry("js", "JavaScript"), Map.entry("javascript", "JavaScript"), Map.entry("ts", "TypeScript"), Map.entry("typescript", "TypeScript"),
            Map.entry("reactjs", "React"), Map.entry("react", "React"), Map.entry("nodejs", "Node.js"), Map.entry("node", "Node.js"),
            Map.entry("springboot", "Spring Boot"), Map.entry("fastapi", "FastAPI"), Map.entry("redis", "Redis"), Map.entry("docker", "Docker"),
            Map.entry("git", "Git"), Map.entry("mysql", "MySQL"), Map.entry("sql", "SQL"));

    private static final Set<String> DATABASES = Set.of(
            "postgresql", "mysql", "mariadb", "sqlite", "mongodb", "redis", "cassandra", "dynamodb", "oracle", "sql", "nosql",
            "neo4j", "elasticsearch", "postgis", "h2", "firebase", "supabase");

    private TechNames() { }

    /** Dedup key: lower-case letters only, version numbers dropped ("Java 21" and "java" collapse). */
    public static String key(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", "");
    }

    public static String canonical(String name) {
        String trimmed = name == null ? "" : name.trim();
        String canon = CANONICAL.get(key(trimmed));
        return canon != null ? canon : trimmed;
    }

    /** Splits combined labels such as "Python / FastAPI" or "Docker & Git" into separate technologies. */
    public static List<String> split(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String part : raw.split("\\s*(?:/|&|\\+|,|;|\\band\\b)\\s*")) {
            String p = part.trim();
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    /** Splits, canonicalises and de-duplicates (first spelling wins), keeping order. */
    public static List<String> unique(Iterable<String> names) {
        Map<String, String> seen = new LinkedHashMap<>();
        for (String raw : names) {
            for (String part : split(raw)) {
                String canon = canonical(part);
                String k = key(canon);          // key of the canonical spelling, so "Postgres" == "PostgreSQL"
                if (k.isEmpty()) continue;
                seen.putIfAbsent(k, canon);
            }
        }
        return new ArrayList<>(seen.values());
    }

    public static boolean isDatabase(String name) {
        return DATABASES.contains(key(canonical(name)));
    }
}
