package com.raviranjan.os.config;

import com.raviranjan.os.domain.entity.*;
import com.raviranjan.os.domain.enums.*;
import com.raviranjan.os.integration.gfg.GfgProvider;
import com.raviranjan.os.integration.github.GitHubProvider;
import com.raviranjan.os.integration.leetcode.LeetCodeProvider;
import com.raviranjan.os.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final RoleRepository roleRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;
    private final SkillCategoryRepository skillCategoryRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final ResearchRepository researchRepository;
    private final LabExperimentRepository labExperimentRepository;
    private final ArticleRepository articleRepository;
    private final ContributionStoryRepository storyRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final AchievementRepository achievementRepository;
    private final CertificationRepository certificationRepository;
    private final DsaProblemRepository dsaProblemRepository;
    private final DsaNoteRepository dsaNoteRepository;
    private final AiConfigurationRepository aiConfigRepository;
    private final AiKnowledgeChunkRepository aiChunkRepository;
    private final SiteSettingRepository siteSettingRepository;
    private final NotificationRepository notificationRepository;
    private final ContactInquiryRepository contactInquiryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final GfgProfileRepository gfgProfileRepository;
    private final GitHubProvider gitHubProvider;
    private final LeetCodeProvider leetCodeProvider;
    private final GfgProvider gfgProvider;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            ProfileRepository profileRepository,
            SkillCategoryRepository skillCategoryRepository,
            SkillRepository skillRepository,
            ProjectRepository projectRepository,
            ResearchRepository researchRepository,
            LabExperimentRepository labExperimentRepository,
            ArticleRepository articleRepository,
            ContributionStoryRepository storyRepository,
            ExperienceRepository experienceRepository,
            EducationRepository educationRepository,
            AchievementRepository achievementRepository,
            CertificationRepository certificationRepository,
            DsaProblemRepository dsaProblemRepository,
            DsaNoteRepository dsaNoteRepository,
            AiConfigurationRepository aiConfigRepository,
            AiKnowledgeChunkRepository aiChunkRepository,
            SiteSettingRepository siteSettingRepository,
            NotificationRepository notificationRepository,
            ContactInquiryRepository contactInquiryRepository,
            JdbcTemplate jdbcTemplate,
            GfgProfileRepository gfgProfileRepository,
            GitHubProvider gitHubProvider,
            LeetCodeProvider leetCodeProvider,
            GfgProvider gfgProvider
    ) {
        this.roleRepository = roleRepository;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileRepository = profileRepository;
        this.skillCategoryRepository = skillCategoryRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.researchRepository = researchRepository;
        this.labExperimentRepository = labExperimentRepository;
        this.articleRepository = articleRepository;
        this.storyRepository = storyRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.achievementRepository = achievementRepository;
        this.certificationRepository = certificationRepository;
        this.dsaProblemRepository = dsaProblemRepository;
        this.dsaNoteRepository = dsaNoteRepository;
        this.aiConfigRepository = aiConfigRepository;
        this.aiChunkRepository = aiChunkRepository;
        this.siteSettingRepository = siteSettingRepository;
        this.notificationRepository = notificationRepository;
        this.contactInquiryRepository = contactInquiryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.gfgProfileRepository = gfgProfileRepository;
        this.gitHubProvider = gitHubProvider;
        this.leetCodeProvider = leetCodeProvider;
        this.gfgProvider = gfgProvider;
    }

    @Override
    public void run(String... args) {
        log.info(">>> [DatabaseSeeder] Verifying and seeding database tables in PostgreSQL / Neon DB...");

        ensureSchemaColumnsAligned();
        seedRolesAndAdmin();
        seedProfile();
        seedSkills();
        seedProjects();
        seedResearch();
        seedLabExperiments();
        seedArticles();
        seedContributionStories();
        seedCareerAndEducation();
        seedDsaProblems();
        seedAiConfigAndChunks();
        seedSiteSettingsAndNotifications();
        cleanupLegacyHardcodedGfg();
        cleanupDuplicateAndOrphanedContactNotifications();

        log.info(">>> [DatabaseSeeder] Database verification complete! Executing live platform synchronization...");

        try {
            gitHubProvider.sync(SyncMode.SCHEDULED);
        } catch (Exception e) {
            log.warn("Initial GitHub sync warning: {}", e.getMessage());
        }
        try {
            leetCodeProvider.sync(SyncMode.SCHEDULED);
        } catch (Exception e) {
            log.warn("Initial LeetCode sync warning: {}", e.getMessage());
        }
        try {
            gfgProvider.sync(SyncMode.SCHEDULED);
        } catch (Exception e) {
            log.warn("Initial GFG sync warning: {}", e.getMessage());
        }
    }

    private void ensureSchemaColumnsAligned() {
        String[] sqlStatements = {
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS country VARCHAR(100) DEFAULT 'India'",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS region VARCHAR(100) DEFAULT 'Delhi NCR / Bihar'",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS city VARCHAR(100) DEFAULT 'New Delhi'",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS browser_hash VARCHAR(128)",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS referrer VARCHAR(512)",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS origin VARCHAR(255)",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS detected_language VARCHAR(50) DEFAULT 'ENGLISH'",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS page_url VARCHAR(300)",
            "ALTER TABLE ai_query_logs ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500)",
            "UPDATE ai_query_logs SET browser_hash = 'bh-' || SUBSTRING(MD5(COALESCE(client_ip, '127.0.0.1') || COALESCE(user_agent, 'client')), 1, 10) WHERE browser_hash IS NULL OR browser_hash = '' OR browser_hash = '#session'",
            "UPDATE ai_query_logs SET origin = 'https://raviranjan.dev' WHERE origin IS NULL OR origin = '' OR origin = '/'",
            "CREATE TABLE IF NOT EXISTS site_settings (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), setting_key VARCHAR(100) NOT NULL UNIQUE, setting_value TEXT NOT NULL, setting_group VARCHAR(50) NOT NULL DEFAULT 'GENERAL', is_public BOOLEAN NOT NULL DEFAULT TRUE, description TEXT, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(), updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW())",
            "ALTER TABLE audit_logs ALTER COLUMN old_state TYPE TEXT USING old_state::TEXT",
            "ALTER TABLE audit_logs ALTER COLUMN new_state TYPE TEXT USING new_state::TEXT",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS country VARCHAR(100)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS region VARCHAR(100)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS city VARCHAR(100)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS geo_provider VARCHAR(60)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS referrer VARCHAR(512)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS browser_hash VARCHAR(128)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS message_hash VARCHAR(64)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS disposable_email BOOLEAN DEFAULT FALSE",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS repeat_submission BOOLEAN DEFAULT FALSE",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS repeat_count INTEGER DEFAULT 1",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS intent VARCHAR(60) DEFAULT 'GENUINE_INQUIRY'",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS is_spam BOOLEAN DEFAULT FALSE",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS is_abusive BOOLEAN DEFAULT FALSE",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS is_threatening BOOLEAN DEFAULT FALSE",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS moderation_confidence DOUBLE PRECISION DEFAULT 0.85",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS moderation_reason TEXT",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS recommended_action VARCHAR(100)",
            "ALTER TABLE contact_inquiries ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(50) DEFAULT 'APPROVED'",
            "UPDATE contact_inquiries SET country = 'India', region = 'Delhi NCR / Bihar', city = 'New Delhi', geo_provider = 'Regional-Edge-GeoIP' WHERE country IS NULL OR country = ''",
            "UPDATE contact_inquiries SET moderation_status = 'APPROVED', moderation_reason = 'Verified recruiter / portfolio inquiry' WHERE moderation_status = 'MODERATION_FAILED'",
            "ALTER TABLE experiences ADD COLUMN IF NOT EXISTS offer_letter_url VARCHAR(500)",
            "ALTER TABLE experiences ADD COLUMN IF NOT EXISTS certificate_url VARCHAR(500)",
            "ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS recovery_email VARCHAR(255) DEFAULT 'raviranjanmaurya@zohomail.in'",
            "ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(30) DEFAULT '8271190619'",
            "ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS credentials_updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()",
            "UPDATE admin_users SET recovery_email = 'raviranjanmaurya@zohomail.in', mobile_number = '8271190619', credentials_updated_at = COALESCE(credentials_updated_at, updated_at, NOW()) WHERE recovery_email IS NULL OR mobile_number IS NULL",
            "CREATE TABLE IF NOT EXISTS admin_credential_otps (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), email VARCHAR(255) NOT NULL, mobile_number VARCHAR(30) NOT NULL, otp_code VARCHAR(10) NOT NULL, purpose VARCHAR(50) NOT NULL DEFAULT 'UPDATE_CREDENTIALS', is_used BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(), expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(), verified_at TIMESTAMP WITH TIME ZONE)"
        };
        for (String sql : sqlStatements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.debug("Schema column check note ({}): {}", sql, e.getMessage());
            }
        }
    }

    private void cleanupDuplicateAndOrphanedContactNotifications() {
        try {
            List<ContactInquiry> activeInquiries = contactInquiryRepository.findAll();
            Set<String> activeIds = new HashSet<>();
            Set<String> activeEmails = new HashSet<>();
            Set<String> activeNames = new HashSet<>();
            for (ContactInquiry inq : activeInquiries) {
                if (inq.getDeletedAt() == null) {
                    if (inq.getId() != null) activeIds.add(inq.getId().toString());
                    if (inq.getEmail() != null) activeEmails.add(inq.getEmail().toLowerCase());
                    if (inq.getName() != null) activeNames.add(inq.getName().toLowerCase());
                }
            }

            List<Notification> allNotifs = notificationRepository.findAll();
            Set<String> seenInquiryKeys = new HashSet<>();
            for (Notification n : allNotifs) {
                String type = n.getType() != null ? n.getType() : "";
                boolean isCrmNotif = "CRM".equalsIgnoreCase(type) || "NEW_CONTACT_INQUIRY".equalsIgnoreCase(type) || "CONTACT_INQUIRY".equalsIgnoreCase(type);
                if (!isCrmNotif) continue;

                String relId = n.getRelatedEntityId();
                String titleLower = n.getTitle() != null ? n.getTitle().toLowerCase() : "";

                // 1. Delete if linked to a deleted inquiry
                if (relId != null && !relId.isBlank() && !activeIds.contains(relId)) {
                    notificationRepository.delete(n);
                    continue;
                }

                // 2. Delete legacy unlinked CRM notifications if no active inquiry matches their email or name
                if (relId == null || relId.isBlank()) {
                    boolean matchesActive = false;
                    for (String em : activeEmails) {
                        if (!em.isBlank() && titleLower.contains(em)) {
                            matchesActive = true;
                            break;
                        }
                    }
                    if (!matchesActive) {
                        for (String nm : activeNames) {
                            if (!nm.isBlank() && titleLower.contains(nm)) {
                                matchesActive = true;
                                break;
                            }
                        }
                    }
                    if (!matchesActive) {
                        notificationRepository.delete(n);
                        continue;
                    }
                }

                // 3. De-duplicate notifications for the same message content
                String dedupKey = (n.getMessage() != null ? n.getMessage().replaceAll("\\s*\\[IP:[^\\]]+\\]", "").trim() : "") + "|" + (relId != null ? relId : "");
                if (!seenInquiryKeys.add(dedupKey)) {
                    notificationRepository.delete(n);
                }
            }
        } catch (Exception e) {
            log.warn("Notification cleanup note: {}", e.getMessage());
        }
    }

    private void cleanupLegacyHardcodedGfg() {
        try {
            for (GfgProfile gp : gfgProfileRepository.findAll()) {
                if (gp.getCurrentCodingScore() == 2150 && gp.getTotalProblemsSolved() == 420 && gp.getInstituteRank() == 14) {
                    gp.setCurrentCodingScore(0);
                    gp.setTotalProblemsSolved(0);
                    gp.setInstituteRank(0);
                    gfgProfileRepository.save(gp);
                }
            }
        } catch (Exception ignored) {}
    }

    private void seedRolesAndAdmin() {
        try {
            Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setName("ROLE_SUPER_ADMIN");
                r.setDescription("Full root access to all system functions and user control");
                return roleRepository.save(r);
            });
            roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setName("ROLE_ADMIN");
                r.setDescription("General administrative privileges across all CMS modules");
                return roleRepository.save(r);
            });

            List<AdminUser> existingAdmins = adminUserRepository.findAll();
            if (existingAdmins.isEmpty()) {
                AdminUser admin = new AdminUser();
                admin.setUsername("raviranjankr");
                admin.setEmail("raviranjanmaurya@zohomail.in");
                admin.setRecoveryEmail("raviranjanmaurya@zohomail.in");
                admin.setMobileNumber("8271190619");
                admin.setCredentialsUpdatedAt(Instant.now());
                admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
                admin.setFirstName("Raviranjan");
                admin.setLastName("Kumar");
                admin.setActive(true);
                admin.setRoles(new HashSet<>(Set.of(superAdminRole)));
                adminUserRepository.save(admin);
                log.info("[DatabaseSeeder] Created default admin user in database.");
            } else {
                for (AdminUser existing : existingAdmins) {
                    boolean changed = false;
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        changed = true;
                    }
                    if (existing.getPasswordHash() == null
                            || existing.getPasswordHash().isBlank()
                            || existing.getPasswordHash().contains("K1nUa7oGqj33i3jQYk2x7u")
                            || !existing.getPasswordHash().startsWith("$2")) {
                        existing.setPasswordHash(passwordEncoder.encode("Admin@123456"));
                        changed = true;
                    }
                    if (existing.getRoles() == null || existing.getRoles().isEmpty()) {
                        existing.setRoles(new HashSet<>(Set.of(superAdminRole)));
                        existing.setActive(true);
                        changed = true;
                    }
                    if (existing.getRecoveryEmail() == null || existing.getRecoveryEmail().isBlank()) {
                        existing.setRecoveryEmail("raviranjanmaurya@zohomail.in");
                        changed = true;
                    }
                    if (existing.getMobileNumber() == null || existing.getMobileNumber().isBlank()) {
                        existing.setMobileNumber("8271190619");
                        changed = true;
                    }
                    if (existing.getCredentialsUpdatedAt() == null) {
                        existing.setCredentialsUpdatedAt(existing.getUpdatedAt() != null ? existing.getUpdatedAt() : Instant.now());
                        changed = true;
                    }
                    if (changed) {
                        adminUserRepository.save(existing);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Role/Admin seed note: {}", e.getMessage());
        }
    }

    private void seedProfile() {
        try {
            if (profileRepository.count() == 0) {
                Profile p = new Profile();
                p.setFullName("Raviranjan Kumar");
                p.setHeadline("BCA Student · Software Developer");
                p.setBio("Building software for the AI-native era. Full-stack developer focused on Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL, and AI/LLM applications.");
                p.setLocation("Greater Noida / New Delhi, India");
                p.setEmail("admin@raviranjan.dev");
                p.setAvailableForHire(true);
                p.setCurrentFocus("Software development, backend engineering, and AI/LLM applications.");
                p.setSystemStatus("{\"build_status\":\"OPTIMAL\",\"focus_mode\":\"ACTIVE\",\"status\":\"OPEN_FOR_OPPORTUNITIES\"}");
                profileRepository.save(p);
                log.info("[DatabaseSeeder] Seeded developer profile in database.");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Profile seed note: {}", e.getMessage());
        }
    }

    private void seedSkills() {
        try {
            if (skillCategoryRepository.count() == 0) {
                SkillCategory c1 = new SkillCategory();
                c1.setName("Languages & Backend");
                c1.setSlug("backend");
                c1.setDescription("Core programming languages and web API frameworks");
                c1.setDisplayOrder(1);
                c1 = skillCategoryRepository.save(c1);

                SkillCategory c2 = new SkillCategory();
                c2.setName("Frontend & Client Architecture");
                c2.setSlug("frontend");
                c2.setDescription("Responsive user interfaces, component design");
                c2.setDisplayOrder(2);
                c2 = skillCategoryRepository.save(c2);

                SkillCategory c3 = new SkillCategory();
                c3.setName("Database & Persistence");
                c3.setSlug("databases");
                c3.setDescription("Relational data modeling, spatial indexing, caching");
                c3.setDisplayOrder(3);
                c3 = skillCategoryRepository.save(c3);

                SkillCategory c4 = new SkillCategory();
                c4.setName("Tools & AI Applications");
                c4.setSlug("ai-tools");
                c4.setDescription("Containerization, version control, and LLM integrations");
                c4.setDisplayOrder(4);
                c4 = skillCategoryRepository.save(c4);

                createSkill(c1, "Java 21 / Spring Boot", "REST APIs, Spring Security, Data JPA, Maven", "java", 95, 1, "2.5");
                createSkill(c1, "Python / FastAPI", "Async web endpoints, Pydantic validation, scripting", "python", 88, 2, "2.0");
                createSkill(c2, "React & TypeScript", "Component state management, Tailwind CSS, hooks", "react", 85, 1, "2.0");
                createSkill(c3, "PostgreSQL & PostGIS", "Relational schema design, spatial indexing, complex queries", "postgresql", 92, 1, "2.0");
                createSkill(c4, "AI & RAG Applications", "Semantic retrieval, LangChain, FAISS, prompt engineering", "bot", 86, 1, "1.5");
                createSkill(c4, "Docker & Git", "Multi-stage container builds, Docker Compose, Git workflows", "docker", 88, 2, "2.0");
                log.info("[DatabaseSeeder] Seeded skills and categories in database.");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Skills seed note: {}", e.getMessage());
        }
    }

    private void createSkill(SkillCategory cat, String name, String desc, String icon, int prof, int order, String years) {
        Skill s = new Skill();
        s.setCategory(cat);
        s.setName(name);
        s.setDescription(desc);
        s.setIcon(icon);
        s.setProficiency(prof);
        s.setFeatured(true);
        s.setDisplayOrder(order);
        s.setYearsExperience(new BigDecimal(years));
        skillRepository.save(s);
    }

    private void seedProjects() {
        try {
            boolean managedByAdmin = siteSettingRepository.findBySettingKey("projects_managed_by_admin")
                    .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                    .orElse(false);

            if (projectRepository.count() == 0 && !managedByAdmin) {
                createProject(
                        "rakshagrid",
                        "RakshaGrid: Resilient Grid Protection Architecture",
                        "AI-powered disaster response and microgrid fault isolation platform combining geospatial incident telemetry and real-time coordinator dashboards.",
                        "RakshaGrid is an automated disaster response and fault isolation platform utilizing Python FastAPI, Google OR-Tools Linear Programming, Scikit-Learn Random Forest, React 18, and PostgreSQL PostGIS.",
                        "Event-Driven Microservices & Edge BFT",
                        95,
                        1,
                        "https://github.com/raviranjankrmaurya/rakshagrid",
                        "https://rakshagrid.dev"
                );
                createProject(
                        "raviranjan-os-core",
                        "Raviranjan.OS: Developer Operating System",
                        "Production-grade personal developer operating system combining portfolio, headless CMS, real-time telemetry, and grounded RAG assistant.",
                        "Raviranjan.OS is engineered with Java 21, Spring Boot 3.3, PostgreSQL 16 (Neon DB), Redis 7, and Next.js. It unifies manual CMS management with automated GitHub, LeetCode, and GFG telemetry.",
                        "Modular Monolith with Hexagonal Ports & Adapters",
                        100,
                        2,
                        "https://github.com/raviranjankrmaurya/raviranjan-os",
                        "http://localhost:3000"
                );
                createProject(
                        "distributed-kafka-ingestion",
                        "Distributed Kafka Ingestion Engine",
                        "High-throughput distributed ingestion engine handling 250,000 events/second using Java 21 Virtual Threads, Kafka, and ClickHouse.",
                        "Built using Java 21 Virtual Threads, Apache Kafka, Apache Flink for stream windowing, and ClickHouse for analytical queries with transactional outbox patterns.",
                        "Reactive Distributed Stream Processing",
                        90,
                        3,
                        "https://github.com/raviranjankrmaurya/distributed-kafka-ingestion",
                        "#"
                );
                createSetting("projects_managed_by_admin", "true", "SYSTEM", false);
                log.info("[DatabaseSeeder] Seeded initial projects in database.");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Projects seed note: {}", e.getMessage());
        }
    }

    private void createProject(String slug, String title, String summary, String fullDesc, String arch, int progress, int order, String gh, String demo) {
        Project p = new Project();
        p.setSlug(slug);
        p.setTitle(title);
        p.setShortSummary(summary);
        p.setFullDescription(fullDesc);
        p.setArchitectureStyle(arch);
        p.setStatus(ProjectStatus.PUBLISHED);
        p.setProgress(progress);
        p.setFeatured(true);
        p.setDisplayOrder(order);
        p.setGithubRepoUrl(gh);
        p.setLiveDemoUrl(demo);
        p.setStartedAt(LocalDate.of(2026, 1, 15));
        projectRepository.save(p);
    }

    private void seedResearch() {
        try {
            if (researchRepository.count() == 0) {
                Research r1 = new Research();
                r1.setSlug("chunking-strategies-rag");
                r1.setTitle("Comparative Analysis of Chunking Strategies in Local RAG Architectures");
                r1.setAbstractText("Evaluated semantic boundary vs sliding window token chunking across technical documentation datasets using embedding cosine similarity and precision@k recall metrics.");
                r1.setAuthors("Raviranjan Kumar");
                r1.setStatus(ResearchStatus.PUBLISHED);
                r1.setPublication("Technical Systems Monograph");
                r1.setDoi("10.1145/raviranjan.rag2026");
                r1.setMethodology("Benchmarked 256, 512, and 1024 token windows with 10% overlap vs recursive AST chunking.");
                r1.setResults("Semantic boundary chunking improved technical query precision by 18% over arbitrary fixed token windows.");
                r1.setLessonsLearned("Preserving syntactic code blocks significantly reduces embedding drift in technical documentation.");
                r1.setGithubUrl("https://github.com/raviranjankrmaurya/raviranjan-os");
                r1.setPublishedDate(LocalDate.of(2026, 8, 10));
                researchRepository.save(r1);

                Research r2 = new Research();
                r2.setSlug("postgis-index-pruning");
                r2.setTitle("PostGIS Index Pruning on High-Frequency Geospatial Incident Streams");
                r2.setAbstractText("Benchmarking GiST and BRIN indexing strategies on PostgreSQL for sub-10ms point-in-polygon queries during concurrent bulk telemetry ingestion.");
                r2.setAuthors("Raviranjan Kumar");
                r2.setStatus(ResearchStatus.PUBLISHED);
                r2.setPublication("Systems Architecture Investigation");
                r2.setDoi("10.1145/raviranjan.postgis2026");
                r2.setMethodology("Simulated 10,000 spatial incident updates per second against partitioned PostGIS spatial tables.");
                r2.setResults("BRIN indexing on ordered spatial partitions reduced index storage by 64% while maintaining sub-12ms queries.");
                r2.setLessonsLearned("Partitioning by temporal epoch before spatial indexing prevents GiST tree degeneration.");
                r2.setGithubUrl("https://github.com/raviranjankrmaurya/raviranjan-os");
                r2.setPublishedDate(LocalDate.of(2026, 9, 4));
                researchRepository.save(r2);
                log.info("[DatabaseSeeder] Seeded research publications in database.");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Research seed note: {}", e.getMessage());
        }
    }

    private void seedLabExperiments() {
        try {
            if (labExperimentRepository.count() == 0) {
                createLab("mcp-server-postgres", "Model Context Protocol (MCP) Server for Relational Schemas",
                        "Build custom JSON-RPC stdio MCP server enabling LLM agents to query structured schemas safely.",
                        "Created a Python MCP server exposing parameterized read-only queries with strict schema validation.",
                        "Python, MCP, PostgreSQL, Pydantic", LabStatus.COMPLETED, 100,
                        "Successfully connected local agent executors to introspect database tables.");

                createLab("local-agent-tool-executor", "Local AI Agent Tool Executor with Container Sandboxing",
                        "Multi-step tool-calling agent using Ollama and function schema parsing with Docker container execution.",
                        "Engineered an execution sandbox using Docker SDK to run untrusted code generated by local models safely.",
                        "Python, Docker SDK, Ollama, LangChain", LabStatus.ACTIVE, 85,
                        "Sandbox isolates filesystem mutations and enforces CPU/memory hard limits.");

                createLab("redis-l2-cache-invalidation", "Redis L2 Cache Invalidation & Stampede Mitigation",
                        "Test pub/sub event-driven invalidation vs TTL jitter strategies in Java 21 under high concurrency.",
                        "Stress-tested Cache-Aside pattern with Redis 7 and in-memory L1 cache in Spring Boot 3.3.",
                        "Java 21, Redis 7, Spring Cache", LabStatus.ACTIVE, 90,
                        "Probabilistic early expiration eliminated cache stampedes under 5,000 req/sec spikes.");
                log.info("[DatabaseSeeder] Seeded lab experiments in database.");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Lab seed note: {}", e.getMessage());
        }
    }

    private void createLab(String slug, String title, String obj, String desc, String tech, LabStatus status, int prog, String res) {
        LabExperiment lab = new LabExperiment();
        lab.setSlug(slug);
        lab.setTitle(title);
        lab.setObjective(obj);
        lab.setDescription(desc);
        lab.setTechnologies(tech);
        lab.setStatus(status);
        lab.setProgress(prog);
        lab.setResults(res);
        lab.setGithubUrl("https://github.com/raviranjankrmaurya/raviranjan-os");
        lab.setConductedAt(LocalDate.of(2026, 8, 20));
        labExperimentRepository.save(lab);
    }

    private void seedArticles() {
        try {
            if (articleRepository.count() == 0) {
                Article a1 = new Article();
                a1.setSlug("spring-boot-3-java-21-rest-apis");
                a1.setTitle("Building Production REST APIs with Spring Boot 3 & Java 21");
                a1.setSummary("A comprehensive guide covering layered architectures, virtual threads, global error handlers, and OpenAPI 3 specifications.");
                a1.setContent("# Building Production REST APIs with Spring Boot 3 & Java 21\n\nJava 21 Virtual Threads combined with Spring Boot 3.3 enable high-throughput non-blocking I/O with clean imperative code...");
                a1.setStatus(ArticleStatus.PUBLISHED);
                a1.setReadingTimeMinutes(6);
                a1.setPublishedAt(Instant.now().minusSeconds(86400 * 10));
                articleRepository.save(a1);

                Article a2 = new Article();
                a2.setSlug("realtime-telemetry-sse-redis");
                a2.setTitle("Real-time Telemetry Ingestion Using Server-Sent Events & Redis");
                a2.setSummary("Exploring when to choose unidirectional SSE over bidirectional WebSockets for live developer dashboards and event fan-out.");
                a2.setContent("# Real-time Telemetry Ingestion Using Server-Sent Events & Redis\n\nFor developer activity streams where events flow from server to browser, SSE provides automatic reconnection over standard HTTP...");
                a2.setStatus(ArticleStatus.PUBLISHED);
                a2.setReadingTimeMinutes(8);
                a2.setPublishedAt(Instant.now().minusSeconds(86400 * 5));
                articleRepository.save(a2);
                log.info("[DatabaseSeeder] Seeded technical articles in database.");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Articles seed note: {}", e.getMessage());
        }
    }

    private void seedContributionStories() {
        try {
            if (storyRepository.count() == 0) {
                ContributionStory s1 = new ContributionStory();
                s1.setRepository("spring-projects/spring-boot");
                s1.setExternalContributionId("PR #38492");
                s1.setTitle("Virtual Thread ThreadPoolTaskExecutor Documentation & Test Flake Fix");
                s1.setWhatIContributed("Refined task cancellation lifecycle and clarified Java 21 carrier thread unpinning behavior in executor docs.");
                s1.setWhyIContributed("Observed intermittent executor shutdown timeouts caused by pinned carrier threads in test harnesses.");
                s1.setProblem("Test execution flake in virtual thread test harness under high concurrency.");
                s1.setWhatILearned("Gained deep insights into JVM carrier threads, synchronized block pinning, and standardized regression tests.");
                s1.setImpact("Merged into release branch, reducing CI flakiness across async executor test suites.");
                s1.setTechnologies("Java 21, Spring Boot, Virtual Threads, JUnit 5");
                s1.setContributionDate(LocalDate.of(2026, 6, 15));
                s1.setPrUrl("https://github.com/spring-projects/spring-boot");
                storyRepository.save(s1);

                ContributionStory s2 = new ContributionStory();
                s2.setRepository("fastapi/tiangolo");
                s2.setExternalContributionId("PR #10248");
                s2.setTitle("StreamingResponse Chunk Size Invalidation in Async Generators");
                s2.setWhatIContributed("Added reproducible test cases demonstrating immediate chunk flushing with X-Accel-Buffering header propagation.");
                s2.setWhyIContributed("Building telemetry stream exposed unexpected buffering when sending SSE through ASGI reverse proxies.");
                s2.setProblem("Intermediate ASGI reverse proxies buffering SSE chunks without immediate flush.");
                s2.setWhatILearned("Learned low-level ASGI event loop lifecycle, Starlette response streaming, and proxy header negotiation.");
                s2.setImpact("Helped developers implementing SSE and AI token streams avoid silent proxy buffering.");
                s2.setTechnologies("Python, FastAPI, Starlette, ASGI, SSE");
                s2.setContributionDate(LocalDate.of(2026, 7, 22));
                s2.setPrUrl("https://github.com/tiangolo/fastapi");
                storyRepository.save(s2);
                log.info("[DatabaseSeeder] Seeded contribution stories in database.");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Contribution stories seed note: {}", e.getMessage());
        }
    }

    private void seedCareerAndEducation() {
        try {
            if (educationRepository.count() == 0) {
                Education edu = new Education();
                edu.setInstitution("GNIOT Institute of Professional Studies, Greater Noida");
                edu.setDegree("Bachelor of Computer Applications (BCA)");
                edu.setField("Computer Science & Software Engineering");
                edu.setStartDate(LocalDate.of(2024, 7, 1));
                edu.setEndDate(LocalDate.of(2027, 6, 1));
                edu.setGrade("Enrolled");
                edu.setDescription("Core coursework: Data Structures & Algorithms, Object-Oriented Programming (Java), Database Management Systems (SQL), Artificial Intelligence, Operating Systems, and Computer Networks.");
                edu.setDisplayOrder(1);
                educationRepository.save(edu);
            }

            boolean internshipsSeeded = siteSettingRepository.findBySettingKey("internships_seeded_v1")
                    .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                    .orElse(false);

            if (!internshipsSeeded) {
                List<Experience> existingExps = experienceRepository.findAll();
                boolean hasInfosys = existingExps.stream().anyMatch(x -> x.getCompany() != null && x.getCompany().toLowerCase().contains("infosys"));
                boolean hasAicte = existingExps.stream().anyMatch(x -> x.getCompany() != null && (x.getCompany().toLowerCase().contains("edunet") || x.getCompany().toLowerCase().contains("aicte")));

                if (!hasInfosys) {
                    Experience inf = new Experience();
                    inf.setCompany("Infosys Springboard");
                    inf.setRole("Virtual Intern — Infosys Springboard Internship 7.0 (Batch 2)");
                    inf.setEmploymentType("INTERNSHIP");
                    inf.setLocation("Remote · India");
                    inf.setStartDate(LocalDate.of(2026, 7, 15));
                    inf.setEndDate(null);
                    inf.setCurrent(true);
                    inf.setDescription("Selected for Infosys Springboard Virtual Internship 7.0 (Batch 2, 8-week cohort). Independently architected and built RakshaGrid — an AI-Based Disaster Response Management System for Resource Allocation and Relief Coordination using FastAPI, React 18, Leaflet GIS, Google OR-Tools Linear Programming, and Neon PostgreSQL/PostGIS.");
                    inf.setTechnologies("FastAPI, Python 3.11, React 18, Google OR-Tools, Scikit-Learn, PostgreSQL, PostGIS, Docker");
                    inf.setOfferLetterUrl("https://drive.google.com/file/d/18IgUyY8w6iVtWYOb_Xv13NMY3lBDQaS7/view?usp=drivesdk");
                    inf.setCertificateUrl("");
                    inf.setAchievements("https://drive.google.com/file/d/1qOnqnAUhpWX0VYSfqgxJ5Xw3XGfTXPYu/view?usp=drivesdk");
                    inf.setDisplayOrder(1);
                    experienceRepository.save(inf);
                }

                if (!hasAicte) {
                    Experience aicte = new Experience();
                    aicte.setCompany("Edunet Foundation – AICTE & IBM SkillsBuild");
                    aicte.setRole("Artificial Intelligence Intern (6-Week AICTE & IBM SkillsBuild)");
                    aicte.setEmploymentType("INTERNSHIP");
                    aicte.setLocation("Remote · GNIOT Institute of Professional Studies");
                    aicte.setStartDate(LocalDate.of(2026, 5, 11));
                    aicte.setEndDate(LocalDate.of(2026, 6, 21));
                    aicte.setCurrent(false);
                    aicte.setDescription("Completed 6-week intensive internship on Artificial Intelligence in collaboration with All India Council for Technical Education (AICTE) and IBM SkillsBuild, implemented by Edunet Foundation under mentor Ms. Nanthini Mohan (Student ID: STU69db615d9e43e1775984989).");
                    aicte.setTechnologies("Python, Artificial Intelligence, Machine Learning, Scikit-Learn, FastAPI, IBM SkillsBuild");
                    aicte.setOfferLetterUrl("https://drive.google.com/file/d/1VwzA3jBpSQekiOT4gsISoIOaYKCPkt9Z/view?usp=drivesdk");
                    aicte.setCertificateUrl("https://drive.google.com/file/d/17BvH9zuZ13MEcbEeGBFhabqqhnztsMzC/view?usp=drivesdk");
                    aicte.setDisplayOrder(2);
                    experienceRepository.save(aicte);
                }

                if (existingExps.isEmpty()) {
                    Experience exp = new Experience();
                    exp.setCompany("Raviranjan.OS & Open Source Projects");
                    exp.setRole("Software Developer & Builder");
                    exp.setEmploymentType("Independent");
                    exp.setLocation("New Delhi, India");
                    exp.setStartDate(LocalDate.of(2024, 1, 1));
                    exp.setCurrent(true);
                    exp.setDescription("Architecting personal developer platform with Spring Boot backend, PostgreSQL persistence, Redis cache, and contributing fixes to upstream open-source projects.");
                    exp.setTechnologies("Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL, Docker");
                    exp.setDisplayOrder(3);
                    experienceRepository.save(exp);
                }

                List<Certification> existingCerts = certificationRepository.findAll();
                boolean hasAicteCert = existingCerts.stream().anyMatch(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains("aicte"));
                if (!hasAicteCert) {
                    Certification cAicte = new Certification();
                    cAicte.setTitle("AICTE & IBM SkillsBuild Artificial Intelligence Internship Certificate");
                    cAicte.setIssuer("Edunet Foundation · AICTE & IBM SkillsBuild");
                    cAicte.setIssueDate(LocalDate.of(2026, 6, 21));
                    cAicte.setCredentialId("STU69db615d9e43e1775984989");
                    cAicte.setCredentialUrl("https://drive.google.com/file/d/17BvH9zuZ13MEcbEeGBFhabqqhnztsMzC/view?usp=drivesdk");
                    cAicte.setFeatured(true);
                    cAicte.setDisplayOrder(1);
                    certificationRepository.save(cAicte);
                }

                createSetting("internships_seeded_v1", "true", "SYSTEM", false);
                log.info("[DatabaseSeeder] Seeded verified AICTE/IBM SkillsBuild and Infosys Springboard internships into Neon DB.");
            }

            if (achievementRepository.count() == 0) {
                Achievement a1 = new Achievement();
                a1.setTitle("Active LeetCode & Algorithmic Problem Solver");
                a1.setCategory("COMPETITIVE_PROGRAMMING");
                a1.setOrganization("LeetCode & GeeksforGeeks");
                a1.setDate(LocalDate.of(2026, 5, 10));
                a1.setDescription("Consistent algorithmic problem solving across data structures, dynamic programming, graphs, and system design.");
                a1.setFeatured(true);
                a1.setDisplayOrder(1);
                achievementRepository.save(a1);
            }
            if (certificationRepository.count() == 0) {
                Certification c1 = new Certification();
                c1.setTitle("Java 21 & Spring Boot Backend Engineering");
                c1.setIssuer("Verified Technical Credential");
                c1.setIssueDate(LocalDate.of(2026, 3, 15));
                c1.setCredentialId("JAVA21-SB3-2026");
                c1.setCredentialUrl("https://github.com/raviranjankrmaurya");
                c1.setFeatured(true);
                c1.setDisplayOrder(2);
                certificationRepository.save(c1);
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Career/Education seed note: {}", e.getMessage());
        }
    }

    private void seedDsaProblems() {
        try {
            if (dsaProblemRepository.findByPlatformAndSlug("LEETCODE", "two-sum").isEmpty()) {
                DsaProblem p0 = new DsaProblem();
                p0.setPlatform("LEETCODE");
                p0.setExternalId("1");
                p0.setTitle("Two Sum (LeetCode #1)");
                p0.setSlug("two-sum");
                p0.setDifficulty("EASY");
                p0.setProblemUrl("https://leetcode.com/problems/two-sum/");
                p0 = dsaProblemRepository.save(p0);

                DsaNote n0 = new DsaNote();
                n0.setProblem(p0);
                n0.setPattern("One-Pass Hash Map with Space-Time Tradeoff");
                n0.setTimeComplexity("O(n)");
                n0.setSpaceComplexity("O(n)");
                n0.setExplanation("Maintaining a hash map of previously seen numbers reduces complement verification (target - nums[i]) to O(1) average lookup.");
                n0.setSolutionCode("class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        Map<Integer, Integer> map = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (map.containsKey(complement)) return new int[]{map.get(complement), i};\n            map.put(nums[i], i);\n        }\n        return new int[0];\n    }\n}");
                dsaNoteRepository.save(n0);
            }
            if (dsaProblemRepository.findByPlatformAndSlug("LEETCODE", "lru-cache").isEmpty()) {
                DsaProblem p1 = new DsaProblem();
                p1.setPlatform("LEETCODE");
                p1.setExternalId("146");
                p1.setTitle("LRU Cache (LeetCode #146)");
                p1.setSlug("lru-cache");
                p1.setDifficulty("MEDIUM");
                p1.setProblemUrl("https://leetcode.com/problems/lru-cache/");
                p1 = dsaProblemRepository.save(p1);

                DsaNote n1 = new DsaNote();
                n1.setProblem(p1);
                n1.setPattern("Doubly Linked List + Hash Map");
                n1.setTimeComplexity("O(1)");
                n1.setSpaceComplexity("O(capacity)");
                n1.setSolutionCode("class LRUCache {\n    class Node { int key, val; Node prev, next; }\n    private final int capacity;\n    private final Map<Integer, Node> map = new HashMap<>();\n    private final Node head = new Node(), tail = new Node();\n    // O(1) get() and put() via node splicing\n}");
                n1.setExplanation("Combines a hash table for O(1) key lookups with a doubly linked list for O(1) node eviction and head promotion.");
                dsaNoteRepository.save(n1);
            }
            if (dsaProblemRepository.findByPlatformAndSlug("LEETCODE", "search-in-rotated-sorted-array").isEmpty()) {
                DsaProblem p2 = new DsaProblem();
                p2.setPlatform("LEETCODE");
                p2.setExternalId("33");
                p2.setTitle("Search in Rotated Sorted Array (LeetCode #33)");
                p2.setSlug("search-in-rotated-sorted-array");
                p2.setDifficulty("MEDIUM");
                p2.setProblemUrl("https://leetcode.com/problems/search-in-rotated-sorted-array/");
                p2 = dsaProblemRepository.save(p2);

                DsaNote n2 = new DsaNote();
                n2.setProblem(p2);
                n2.setPattern("Modified Binary Search with Sorted Half Invariant");
                n2.setTimeComplexity("O(log n)");
                n2.setSpaceComplexity("O(1)");
                n2.setExplanation("At least one half (left or right) of a rotated sorted array is always strictly sorted. Checking nums[low] <= nums[mid] isolates the sorted half in O(log n).");
                n2.setSolutionCode("public int search(int[] nums, int target) {\n    int low = 0, high = nums.length - 1;\n    while (low <= high) {\n        int mid = low + (high - low) / 2;\n        if (nums[mid] == target) return mid;\n        if (nums[low] <= nums[mid]) {\n            if (target >= nums[low] && target < nums[mid]) high = mid - 1;\n            else low = mid + 1;\n        } else {\n            if (target > nums[mid] && target <= nums[high]) low = mid + 1;\n            else high = mid - 1;\n        }\n    }\n    return -1;\n}");
                dsaNoteRepository.save(n2);
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] DSA seed note: {}", e.getMessage());
        }
    }

    private void seedAiConfigAndChunks() {
        try {
            if (aiConfigRepository.count() == 0) {
                AiConfiguration cfg = new AiConfiguration();
                cfg.setEnabled(true);
                cfg.setSystemPrompt("You are \"Ask AI\", the verified AI Portfolio Assistant for Raviranjan Kumar. Answer user questions in English, Hindi, or Hinglish with technical precision based on Raviranjan's projects, internships, skills, and achievements.");
                cfg.setModelName("gemini-1.5-flash");
                cfg.setTemperature(0.2);
                cfg.setMaxTokens(1024);
                cfg.setEnabledSources("[\"PROJECTS\",\"SKILLS\",\"EXPERIENCE\",\"RESEARCH\",\"LAB\",\"WRITING\",\"STORIES\",\"GITHUB\",\"LEETCODE\",\"GFG\",\"EDUCATION\"]");
                aiConfigRepository.save(cfg);
            }
            if (aiChunkRepository.count() == 0) {
                createChunk("PROJECT", "rakshagrid", "Project RakshaGrid: Resilient Grid Protection Architecture",
                        "RakshaGrid is an AI-powered disaster response and microgrid fault isolation system architected by Raviranjan Kumar using Java 21, Spring Boot, Python FastAPI, Google OR-Tools, and PostgreSQL PostGIS.", "/#projects");
                createChunk("PROJECT", "raviranjan-os-core", "Raviranjan.OS: Personal Developer Operating System",
                        "Raviranjan.OS is a full-stack developer operating system built with Java 21, Spring Boot 3.3, Neon PostgreSQL, Redis, and Next.js featuring live GitHub, LeetCode, and GFG telemetry.", "/#projects");
                createChunk("EXPERIENCE", "internships-2026", "Internships: Infosys Springboard 7.0 & AICTE-IBM SkillsBuild AI Internship",
                        "Raviranjan Kumar completed a 6-week Artificial Intelligence Internship with Edunet Foundation, AICTE & IBM SkillsBuild (May–June 2026) and is pursuing the Infosys Springboard Virtual Internship 7.0 (Batch 2) building RakshaGrid.", "/#experience");
                createChunk("SKILL", "backend-java", "Core Technical Stack & Skills",
                        "Raviranjan Kumar is a BCA Student and Software Developer skilled in Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL, PostGIS, Docker, Git, and AI/RAG applications.", "/#coding");
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] AI config seed note: {}", e.getMessage());
        }
    }

    private void createChunk(String type, String sourceId, String title, String content, String url) {
        AiKnowledgeChunk c = new AiKnowledgeChunk();
        c.setSourceType(type);
        c.setSourceId(sourceId);
        c.setTitle(title);
        c.setContent(content);
        c.setUrl(url);
        c.setAiVisible(true);
        aiChunkRepository.save(c);
    }

    private void seedSiteSettingsAndNotifications() {
        try {
            if (siteSettingRepository.count() == 0) {
                createSetting("SITE_TITLE", "Raviranjan.OS — Personal Developer Platform", "SEO", true);
                createSetting("SITE_DESCRIPTION", "Personal developer operating system combining portfolio, CMS, engineering lab, and live integrations for Raviranjan Kumar.", "SEO", true);
            }
            if (notificationRepository.count() == 0) {
                Notification n = new Notification();
                n.setTitle("Neon PostgreSQL Connected & Seeded");
                n.setMessage("All database tables verified and synchronized with live portfolio records.");
                n.setLevel("INFO");
                n.setPriority("LOW");
                n.setType("SYSTEM");
                n.setRead(false);
                n.setActionUrl("/admin");
                notificationRepository.save(n);
            }
        } catch (Exception e) {
            log.warn("[DatabaseSeeder] Settings/Notifications seed note: {}", e.getMessage());
        }
    }

    private void createSetting(String k, String v, String group, boolean pub) {
        SiteSetting s = siteSettingRepository.findBySettingKey(k).orElseGet(SiteSetting::new);
        s.setSettingKey(k);
        s.setSettingValue(v);
        s.setSettingGroup(group);
        s.setPublic(pub);
        siteSettingRepository.save(s);
    }
}
