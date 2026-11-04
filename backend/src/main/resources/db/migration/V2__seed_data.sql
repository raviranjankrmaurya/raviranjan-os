-- V2__seed_data.sql: Baseline Reference Data & Default Admin Setup

-- 1. Roles & Permissions
INSERT INTO roles (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ROLE_SUPER_ADMIN', 'Full root access to all system functions and user control'),
    ('00000000-0000-0000-0000-000000000002', 'ROLE_ADMIN', 'General administrative privileges across all CMS modules'),
    ('00000000-0000-0000-0000-000000000003', 'ROLE_EDITOR', 'Content editing access for articles, projects, and lab experiments'),
    ('00000000-0000-0000-0000-000000000004', 'ROLE_ANALYST', 'Read-only access to analytics, audit logs, and performance metrics');

INSERT INTO permissions (name, description) VALUES
    ('PROJECT_READ', 'View project details'),
    ('PROJECT_WRITE', 'Create and edit projects'),
    ('PROJECT_DELETE', 'Delete projects'),
    ('CONTENT_PUBLISH', 'Publish articles and projects'),
    ('INTEGRATION_MANAGE', 'Configure external integrations and run syncs'),
    ('USER_MANAGE', 'Manage administrative users and roles'),
    ('CRM_MANAGE', 'Triage and manage contact inquiries');

-- Default Super Admin (Initial password: Admin@123456 - bcrypt hashed)
INSERT INTO admin_users (id, username, email, password_hash, first_name, last_name, is_active) VALUES
    ('10000000-0000-0000-0000-000000000001', 'raviranjan', 'admin@raviranjan.dev', '$2a$12$K1nUa7oGqj33i3jQYk2x7uF0PjK9.p1F7m0B4a1m2h7N8q0g2p4qC', 'Raviranjan', 'Kumar', TRUE);

INSERT INTO admin_user_roles (user_id, role_id) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');

-- 2. Core Profile Setup (Grounded in Raviranjan Kumar: BCA Student & Software Developer)
INSERT INTO profiles (id, full_name, headline, bio, location, email, available_for_hire, current_focus, system_status) VALUES
    ('20000000-0000-0000-0000-000000000001', 
     'Raviranjan Kumar', 
     'BCA Student · Software Developer', 
     'Building software for the AI-native era. Full-stack developer focused on Java, Spring Boot, Python, FastAPI, React, PostgreSQL, and AI/LLM applications.', 
     'New Delhi, Delhi, India', 
     'admin@raviranjan.dev', 
     TRUE, 
     'Software development, backend engineering, and AI/LLM applications.',
     '{"build_status": "OPTIMAL", "focus_mode": "ACTIVE", "status": "OPEN_FOR_OPPORTUNITIES"}'::jsonb);

-- 3. Social Links
INSERT INTO social_links (profile_id, platform, url, handle, icon_key, display_order) VALUES
    ('20000000-0000-0000-0000-000000000001', 'GitHub', 'https://github.com/raviranjan', 'raviranjan', 'github', 1),
    ('20000000-0000-0000-0000-000000000001', 'LinkedIn', 'https://linkedin.com/in/raviranjan', 'raviranjan', 'linkedin', 2),
    ('20000000-0000-0000-0000-000000000001', 'LeetCode', 'https://leetcode.com/raviranjan', 'raviranjan', 'code', 3),
    ('20000000-0000-0000-0000-000000000001', 'GeeksforGeeks', 'https://auth.geeksforgeeks.org/user/raviranjan', 'raviranjan', 'terminal', 4);

-- 4. Skill Categories & Skills
INSERT INTO skill_categories (id, name, slug, description, display_order) VALUES
    ('30000000-0000-0000-0000-000000000001', 'Languages & Backend', 'backend', 'Core programming languages and web API frameworks', 1),
    ('30000000-0000-0000-0000-000000000002', 'Frontend & Client Architecture', 'frontend', 'Responsive user interfaces, component design', 2),
    ('30000000-0000-0000-0000-000000000003', 'Database & Persistence', 'databases', 'Relational data modeling, spatial indexing, caching', 3),
    ('30000000-0000-0000-0000-000000000004', 'Tools & AI Applications', 'ai-tools', 'Containerization, version control, and LLM integrations', 4);

INSERT INTO skills (category_id, name, description, icon, proficiency, featured, display_order, years_experience) VALUES
    ('30000000-0000-0000-0000-000000000001', 'Java 21 / Spring Boot', 'REST APIs, Spring Security, Data JPA, Maven', 'java', 90, TRUE, 1, 2.0),
    ('30000000-0000-0000-0000-000000000001', 'Python / FastAPI', 'Async web endpoints, Pydantic validation, scripting', 'python', 88, TRUE, 2, 2.0),
    ('30000000-0000-0000-0000-000000000002', 'React & TypeScript', 'Component state management, Tailwind CSS, hooks', 'react', 85, TRUE, 1, 2.0),
    ('30000000-0000-0000-0000-000000000003', 'PostgreSQL & PostGIS', 'Relational schema design, spatial indexing, complex queries', 'postgresql', 88, TRUE, 1, 2.0),
    ('30000000-0000-0000-0000-000000000004', 'AI & RAG Applications', 'Semantic retrieval, LangChain, FAISS, prompt engineering', 'bot', 82, TRUE, 1, 1.5),
    ('30000000-0000-0000-0000-000000000004', 'Docker & Git', 'Multi-stage container builds, Docker Compose, Git workflows', 'docker', 85, TRUE, 2, 2.0);

-- 5. Baseline Site Settings
INSERT INTO site_settings (setting_key, setting_value, setting_group, is_public, description) VALUES
    ('SITE_TITLE', 'Raviranjan.OS — Personal Developer Platform', 'SEO', TRUE, 'Global page title'),
    ('SITE_DESCRIPTION', 'Personal developer operating system combining portfolio, CMS, engineering lab, and live integrations for Raviranjan Kumar.', 'SEO', TRUE, 'Global meta description'),
    ('MAINTENANCE_MODE', 'false', 'SYSTEM', FALSE, 'Global maintenance toggle'),
    ('FEATURE_AI_ASSISTANT', 'true', 'FEATURES', TRUE, 'Toggle RAG AI Portfolio Assistant'),
    ('FEATURE_LIVE_TELEMETRY', 'true', 'FEATURES', TRUE, 'Toggle real-time SSE telemetry stream');

-- 6. Baseline Homepage Sections
INSERT INTO homepage_sections (section_key, title, subtitle, display_order, is_visible) VALUES
    ('HERO', 'Raviranjan.OS', 'Building software for the AI-native era', 1, TRUE),
    ('PROFILE', 'Developer Profile', 'BCA Student & Software Developer focus', 2, TRUE),
    ('PROJECTS', 'Featured Projects', 'Production builds and real-world architectures', 3, TRUE),
    ('ACTIVITY', 'Developer Activity', 'Live platform telemetry and contribution heatmap', 4, TRUE),
    ('CODING', 'Coding Journey', 'LeetCode, GFG, and authored DSA problem notes', 5, TRUE),
    ('CONTRIBUTIONS', 'Contribution Stories', 'Why I contributed and engineering takeaways', 6, TRUE),
    ('RESEARCH', 'Research Hub', 'Technical papers and architectural investigations', 7, TRUE),
    ('LAB', 'Engineering Lab', 'Prototypes and experimental builds', 8, TRUE),
    ('WRITING', 'Technical Writing', 'Engineering notes and system design breakdowns', 9, TRUE),
    ('CONTACT', 'Get in Touch', 'Direct inquiry submission to CRM', 10, TRUE);
