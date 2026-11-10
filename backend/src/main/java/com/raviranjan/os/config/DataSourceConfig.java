package com.raviranjan.os.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    private static final String DEFAULT_FALLBACK_URL =
            "jdbc:postgresql://postgres:5432/raviranjan_os?stringtype=unspecified";

    @Value("${NEON_DB_URL:}")
    private String neonDbUrl;

    @Value("${NEON_DATABASE_URL:}")
    private String neonDatabaseUrl;

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${SPRING_DATASOURCE_URL:}")
    private String springDatasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:}")
    private String defaultUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        Map<String, String> fileEnv = loadEnvFilesFromDisk();

        String[] candidates = new String[]{
                System.getenv("NEON_DB_URL"),
                System.getenv("NEON_DATABASE_URL"),
                System.getenv("DATABASE_URL"),
                System.getenv("SPRING_DATASOURCE_URL"),
                fileEnv.get("NEON_DB_URL"),
                fileEnv.get("NEON_DATABASE_URL"),
                fileEnv.get("DATABASE_URL"),
                fileEnv.get("SPRING_DATASOURCE_URL"),
                neonDbUrl,
                neonDatabaseUrl,
                databaseUrl,
                springDatasourceUrl,
                DEFAULT_FALLBACK_URL
        };

        String externalCandidate = "";
        for (String c : candidates) {
            if (c != null && !c.isBlank() && !c.contains("//postgres:") && !c.contains("//localhost:")) {
                externalCandidate = stripQuotes(c.trim());
                break;
            }
        }

        String rawUrl = !externalCandidate.isEmpty()
                ? externalCandidate
                : DEFAULT_FALLBACK_URL;

        String explicitUser = firstNonBlank(
                System.getenv("NEON_DB_USERNAME"),
                System.getenv("SPRING_DATASOURCE_USERNAME"),
                fileEnv.get("NEON_DB_USERNAME"),
                fileEnv.get("SPRING_DATASOURCE_USERNAME"),
                defaultUsername
        ).trim();

        String explicitPass = firstNonBlank(
                System.getenv("NEON_DB_PASSWORD"),
                System.getenv("SPRING_DATASOURCE_PASSWORD"),
                fileEnv.get("NEON_DB_PASSWORD"),
                fileEnv.get("SPRING_DATASOURCE_PASSWORD"),
                defaultPassword
        ).trim();

        ParsedJdbc parsed = parseJdbcUrlAndCredentials(rawUrl, explicitUser, explicitPass);

        // Test connection to Neon DB first (with 8s timeout) so backend never crashes on startup
        if (canConnect(parsed.jdbcUrl, parsed.username, parsed.password)) {
            String masked = parsed.jdbcUrl.replaceAll("(?i)(password=)[^&]+", "$1********");
            log.info("==================================================================");
            log.info("[Raviranjan.OS DB] Connected to Neon Cloud PostgreSQL");
            log.info("[Raviranjan.OS DB] Target JDBC URL : {}", masked);
            log.info("[Raviranjan.OS DB] Database User   : {}", parsed.username);
            log.info("==================================================================");
            return buildHikari(parsed.jdbcUrl, parsed.username, parsed.password);
        }

        // If custom URL failed, try DEFAULT_FALLBACK_URL
        if (!rawUrl.equals(DEFAULT_FALLBACK_URL)) {
            ParsedJdbc fallbackNeon = parseJdbcUrlAndCredentials(DEFAULT_FALLBACK_URL, "", "");
            if (canConnect(fallbackNeon.jdbcUrl, fallbackNeon.username, fallbackNeon.password)) {
                log.info("[Raviranjan.OS DB] Connected to primary Neon Cloud PostgreSQL instance.");
                return buildHikari(fallbackNeon.jdbcUrl, fallbackNeon.username, fallbackNeon.password);
            }
        }

        // Final safety fallback to local Docker PostgreSQL so container never crashes
        String localUrl = "jdbc:postgresql://postgres:5432/raviranjan_os?stringtype=unspecified";
        log.warn("[Raviranjan.OS DB] Neon DB unreachable, falling back to local PostgreSQL container: {}", localUrl);
        return buildHikari(localUrl, "postgres", "postgrespassword");
    }

    private HikariDataSource buildHikari(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(900000);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private boolean canConnect(String jdbcUrl, String username, String password) {
        try {
            DriverManager.setLoginTimeout(8);
            Properties props = new Properties();
            if (username != null && !username.isBlank()) props.setProperty("user", username);
            if (password != null && !password.isBlank()) props.setProperty("password", password);
            try (Connection conn = DriverManager.getConnection(jdbcUrl, props)) {
                return conn != null && conn.isValid(5);
            }
        } catch (Exception e) {
            log.warn("[Raviranjan.OS DB] Connection check note: {}", e.getMessage());
            return false;
        }
    }

    private ParsedJdbc parseJdbcUrlAndCredentials(String rawUrl, String explicitUser, String explicitPass) {
        String finalUser = (explicitUser != null && !explicitUser.isBlank() && !"postgres".equalsIgnoreCase(explicitUser))
                ? explicitUser : "";
        String finalPass = (explicitPass != null && !explicitPass.isBlank() && !"postgrespassword".equalsIgnoreCase(explicitPass))
                ? explicitPass : "";
        String cleanUrl = rawUrl;

        try {
            String uriCandidate = rawUrl;
            if (uriCandidate.startsWith("jdbc:")) {
                uriCandidate = uriCandidate.substring(5);
            }
            if (uriCandidate.startsWith("postgres://")) {
                uriCandidate = "postgresql://" + uriCandidate.substring("postgres://".length());
            }

            if (uriCandidate.startsWith("postgresql://")) {
                URI uri = URI.create(uriCandidate);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    if (finalUser.isEmpty()) {
                        finalUser = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    }
                    if (parts.length > 1 && finalPass.isEmpty()) {
                        finalPass = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getPath();
                String query = uri.getRawQuery();

                StringBuilder qBuilder = new StringBuilder();
                if (query != null && !query.isBlank()) {
                    for (String param : query.split("&")) {
                        if (param.isBlank()) continue;
                        String[] kv = param.split("=", 2);
                        String k = kv[0].trim();
                        String v = kv.length > 1 ? URLDecoder.decode(kv[1].trim(), StandardCharsets.UTF_8) : "";
                        if ("user".equalsIgnoreCase(k) || "username".equalsIgnoreCase(k)) {
                            if (finalUser.isEmpty()) finalUser = v;
                            continue;
                        }
                        if ("password".equalsIgnoreCase(k)) {
                            if (finalPass.isEmpty()) finalPass = v;
                            continue;
                        }
                        // Strip channelBinding=require because Java 21 PgJDBC fails on Neon pooler with channelBinding=require
                        if ("channelBinding".equalsIgnoreCase(k)) {
                            continue;
                        }
                        if (qBuilder.length() > 0) qBuilder.append("&");
                        qBuilder.append(param);
                    }
                }

                if (!qBuilder.toString().contains("stringtype=")) {
                    if (qBuilder.length() > 0) qBuilder.append("&");
                    qBuilder.append("stringtype=unspecified");
                }
                if (host != null && (host.contains("neon.tech") || host.contains("aws") || host.contains("supabase"))
                        && !qBuilder.toString().contains("sslmode=")) {
                    if (qBuilder.length() > 0) qBuilder.append("&");
                    qBuilder.append("sslmode=require");
                }

                StringBuilder sb = new StringBuilder("jdbc:postgresql://");
                sb.append(host);
                if (port > 0) {
                    sb.append(":").append(port);
                }
                sb.append(path != null && !path.isBlank() ? path : "/neondb");
                if (qBuilder.length() > 0) {
                    sb.append("?").append(qBuilder);
                }
                cleanUrl = sb.toString();
            }
        } catch (Exception e) {
            log.warn("URI parse fallback: {}", e.getMessage());
            cleanUrl = rawUrl.replaceAll("(?i)&?channelBinding=require", "");
            if (!cleanUrl.startsWith("jdbc:")) {
                cleanUrl = "jdbc:" + cleanUrl;
            }
            if (!cleanUrl.contains("stringtype=")) {
                cleanUrl += (cleanUrl.contains("?") ? "&" : "?") + "stringtype=unspecified";
            }
        }

        if (finalUser.isEmpty()) {
            finalUser = !explicitUser.isEmpty() ? explicitUser : "postgres";
        }
        if (finalPass.isEmpty()) {
            finalPass = !explicitPass.isEmpty() ? explicitPass : "postgrespassword";
        }
        return new ParsedJdbc(cleanUrl, finalUser, finalPass);
    }

    private Map<String, String> loadEnvFilesFromDisk() {
        Map<String, String> map = new LinkedHashMap<>();
        String[] paths = new String[]{
                "/workspace_env/config.env",
                "/workspace_env/.env",
                "config.env",
                ".env",
                "../config.env",
                "../.env"
        };
        for (String p : paths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                        int idx = line.indexOf('=');
                        String k = line.substring(0, idx).trim();
                        String v = stripQuotes(line.substring(idx + 1).trim());
                        if (!v.isEmpty() && !map.containsKey(k)) {
                            map.put(k, v);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private String stripQuotes(String s) {
        if (s == null) return "";
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static class ParsedJdbc {
        final String jdbcUrl;
        final String username;
        final String password;

        ParsedJdbc(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}
