-- V1__init_schema.sql: Initial Normalized Schema for Raviranjan.OS
-- PostgreSQL 16 compatible DDL

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========================================================
-- 1. SECURITY & IDENTITY
-- ========================================================

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    last_login_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_username ON admin_users(username);

CREATE TABLE admin_user_roles (
    user_id UUID NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    principal VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    old_state JSONB,
    new_state JSONB,
    client_ip VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_principal ON audit_logs(principal);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- ========================================================
-- 2. PROFILE & SITE CONFIGURATION
-- ========================================================

CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    headline VARCHAR(255) NOT NULL,
    bio TEXT NOT NULL,
    avatar_url VARCHAR(500),
    location VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    available_for_hire BOOLEAN NOT NULL DEFAULT TRUE,
    current_focus TEXT,
    resume_url VARCHAR(500),
    system_status JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE social_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    handle VARCHAR(100),
    icon_key VARCHAR(50),
    display_order INT NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE site_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    setting_group VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_site_settings_key ON site_settings(setting_key);

CREATE TABLE homepage_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_key VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    subtitle TEXT,
    display_order INT NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================================
-- 3. SKILLS DOMAIN
-- ========================================================

CREATE TABLE skill_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES skill_categories(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(100),
    proficiency INT NOT NULL CHECK (proficiency >= 0 AND proficiency <= 100),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    years_experience NUMERIC(4, 1) DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skills_category_id ON skills(category_id);
CREATE INDEX idx_skills_featured ON skills(featured);

-- ========================================================
-- 4. PROJECTS DOMAIN
-- ========================================================

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(150) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    short_summary TEXT NOT NULL,
    full_description TEXT NOT NULL,
    featured_image_url VARCHAR(500),
    live_demo_url VARCHAR(500),
    github_repo_url VARCHAR(500),
    architecture_style VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    progress INT NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    started_at DATE,
    completed_at DATE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_slug ON projects(slug);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_is_featured ON projects(is_featured);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);

CREATE TABLE project_technologies (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, skill_id)
);

CREATE TABLE project_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    version_tag VARCHAR(50),
    description TEXT,
    target_date DATE,
    completion_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_milestones_project ON project_milestones(project_id);

CREATE TABLE project_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    milestone_id UUID REFERENCES project_milestones(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'TODO', -- TODO, IN_PROGRESS, DONE
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_tasks_project ON project_tasks(project_id);
CREATE INDEX idx_project_tasks_status ON project_tasks(status);

CREATE TABLE project_changelogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version_tag VARCHAR(50) NOT NULL,
    release_summary TEXT NOT NULL,
    commit_hash VARCHAR(100),
    released_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_changelogs_project ON project_changelogs(project_id);

CREATE TABLE project_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    decision_title VARCHAR(255) NOT NULL,
    problem_context TEXT NOT NULL,
    decision_taken TEXT NOT NULL,
    trade_offs_considered TEXT NOT NULL,
    consequences TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACCEPTED', -- PROPOSED, ACCEPTED, SUPERSEDED, REJECTED
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_decisions_project ON project_decisions(project_id);

-- ========================================================
-- 5. CAREER & EDUCATION DOMAIN
-- ========================================================

CREATE TABLE experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company VARCHAR(150) NOT NULL,
    role VARCHAR(150) NOT NULL,
    employment_type VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
    location VARCHAR(100),
    start_date DATE NOT NULL,
    end_date DATE,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    responsibilities TEXT,
    technologies VARCHAR(500),
    achievements TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_experiences_start_date ON experiences(start_date DESC);

CREATE TABLE education (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution VARCHAR(200) NOT NULL,
    degree VARCHAR(150) NOT NULL,
    field VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    grade VARCHAR(50),
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    organization VARCHAR(150) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    proof_url VARCHAR(500),
    certificate VARCHAR(500),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE certifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    issuer VARCHAR(150) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE,
    credential_id VARCHAR(150),
    credential_url VARCHAR(500),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================================
-- 6. RESEARCH & LAB EXPERIMENTS
-- ========================================================

CREATE TABLE research (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(150) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    abstract TEXT NOT NULL,
    authors VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, SUBMITTED, PUBLISHED, ARCHIVED
    publication VARCHAR(200),
    doi VARCHAR(150),
    pdf_url VARCHAR(500),
    github_url VARCHAR(500),
    dataset_url VARCHAR(500),
    methodology TEXT,
    results TEXT,
    lessons_learned TEXT,
    published_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_research_slug ON research(slug);
CREATE INDEX idx_research_status ON research(status);

CREATE TABLE research_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE research_tag_mappings (
    research_id UUID NOT NULL REFERENCES research(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES research_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (research_id, tag_id)
);

CREATE TABLE lab_experiments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(150) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    objective TEXT NOT NULL,
    description TEXT NOT NULL,
    technologies VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, SHELVED
    progress INT NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    results TEXT,
    github_url VARCHAR(500),
    demo_url VARCHAR(500),
    screenshots JSONB,
    lessons_learned TEXT,
    future_work TEXT,
    conducted_at DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lab_experiments_slug ON lab_experiments(slug);
CREATE INDEX idx_lab_experiments_status ON lab_experiments(status);

-- ========================================================
-- 7. WRITING & ARTICLES
-- ========================================================

CREATE TABLE articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(150) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content TEXT NOT NULL,
    cover_image_url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    published_at TIMESTAMP WITH TIME ZONE,
    seo_title VARCHAR(200),
    seo_description VARCHAR(300),
    canonical_url VARCHAR(500),
    reading_time_minutes INT DEFAULT 5,
    related_project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_articles_slug ON articles(slug);
CREATE INDEX idx_articles_status ON articles(status);
CREATE INDEX idx_articles_published_at ON articles(published_at DESC);

CREATE TABLE article_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE article_tag_mappings (
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES article_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);

-- ========================================================
-- 8. CONTRIBUTION STORIES & DSA
-- ========================================================

CREATE TABLE contribution_stories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository VARCHAR(255) NOT NULL,
    external_contribution_id VARCHAR(150),
    title VARCHAR(255) NOT NULL,
    what_i_contributed TEXT NOT NULL,
    why_i_contributed TEXT,
    problem TEXT NOT NULL,
    what_i_learned TEXT,
    impact TEXT,
    technologies VARCHAR(500),
    contribution_date DATE NOT NULL,
    related_project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    pr_url VARCHAR(500),
    issue_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contribution_stories_repo ON contribution_stories(repository);
CREATE INDEX idx_contribution_stories_date ON contribution_stories(contribution_date DESC);

CREATE TABLE dsa_problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform VARCHAR(50) NOT NULL, -- LEETCODE, GFG, CODEFORCES
    external_id VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) NOT NULL, -- EASY, MEDIUM, HARD
    problem_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (platform, slug)
);

CREATE TABLE dsa_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID NOT NULL REFERENCES dsa_problems(id) ON DELETE CASCADE,
    pattern VARCHAR(150),
    time_complexity VARCHAR(50),
    space_complexity VARCHAR(50),
    solution_code TEXT,
    explanation TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dsa_notes_problem ON dsa_notes(problem_id);

-- ========================================================
-- 9. GITHUB INTEGRATION TABLES
-- ========================================================

CREATE TABLE github_repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    full_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    html_url VARCHAR(500) NOT NULL,
    is_fork BOOLEAN DEFAULT FALSE,
    is_private BOOLEAN DEFAULT FALSE,
    stargazers_count INT DEFAULT 0,
    forks_count INT DEFAULT 0,
    primary_language VARCHAR(50),
    topics JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE github_commits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID REFERENCES github_repositories(id) ON DELETE CASCADE,
    sha VARCHAR(100) NOT NULL UNIQUE,
    message TEXT NOT NULL,
    author_name VARCHAR(150),
    author_date TIMESTAMP WITH TIME ZONE NOT NULL,
    html_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_github_commits_sha ON github_commits(sha);
CREATE INDEX idx_github_commits_date ON github_commits(author_date DESC);

CREATE TABLE github_contributions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contribution_date DATE NOT NULL UNIQUE,
    contribution_count INT NOT NULL DEFAULT 0,
    intensity_level INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE github_pull_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID REFERENCES github_repositories(id) ON DELETE CASCADE,
    pr_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    state VARCHAR(50) NOT NULL,
    merged_at TIMESTAMP WITH TIME ZONE,
    html_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (repo_id, pr_number)
);

CREATE TABLE github_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID REFERENCES github_repositories(id) ON DELETE CASCADE,
    issue_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    state VARCHAR(50) NOT NULL,
    html_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (repo_id, issue_number)
);

CREATE TABLE github_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID REFERENCES github_repositories(id) ON DELETE CASCADE,
    tag_name VARCHAR(50) NOT NULL,
    name VARCHAR(255),
    body TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    html_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================================
-- 10. LEETCODE INTEGRATION TABLES
-- ========================================================

CREATE TABLE leetcode_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    ranking INT,
    reputation INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE leetcode_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES leetcode_profiles(id) ON DELETE CASCADE,
    total_solved INT NOT NULL DEFAULT 0,
    easy_solved INT NOT NULL DEFAULT 0,
    medium_solved INT NOT NULL DEFAULT 0,
    hard_solved INT NOT NULL DEFAULT 0,
    acceptance_rate NUMERIC(5, 2) DEFAULT 0.0,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE leetcode_problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    title_slug VARCHAR(255) NOT NULL UNIQUE,
    difficulty VARCHAR(50) NOT NULL,
    paid_only BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE leetcode_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES leetcode_profiles(id) ON DELETE CASCADE,
    problem_slug VARCHAR(255) NOT NULL,
    submission_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACCEPTED, etc.
    language VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================================
-- 11. GEEKSFORGEEKS INTEGRATION TABLES
-- ========================================================

CREATE TABLE gfg_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    institute_rank INT,
    current_coding_score INT,
    monthly_score INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE gfg_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES gfg_profiles(id) ON DELETE CASCADE,
    total_problems_solved INT NOT NULL DEFAULT 0,
    school_solved INT DEFAULT 0,
    basic_solved INT DEFAULT 0,
    easy_solved INT DEFAULT 0,
    medium_solved INT DEFAULT 0,
    hard_solved INT DEFAULT 0,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE gfg_problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_name VARCHAR(255) NOT NULL,
    problem_slug VARCHAR(255) NOT NULL UNIQUE,
    difficulty VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE gfg_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES gfg_profiles(id) ON DELETE CASCADE,
    problem_slug VARCHAR(255) NOT NULL,
    solved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================================
-- 12. CRM, OPERATIONS & INTEGRATION INFRASTRUCTURE
-- ========================================================

CREATE TABLE contact_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    company VARCHAR(150),
    subject VARCHAR(255),
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW', -- NEW, CONTACTED, INTERVIEW, NEGOTIATION, COMPLETED, REJECTED
    notes TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contact_inquiries_email ON contact_inquiries(email);
CREATE INDEX idx_contact_inquiries_status ON contact_inquiries(status);
CREATE INDEX idx_contact_inquiries_created_at ON contact_inquiries(created_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    level VARCHAR(50) NOT NULL DEFAULT 'INFO', -- INFO, WARNING, ALERT
    action_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_is_read ON notifications(is_read);

CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    page_slug VARCHAR(255),
    referrer VARCHAR(500),
    ip_hash VARCHAR(64) NOT NULL,
    country_code VARCHAR(10),
    device_type VARCHAR(50),
    browser VARCHAR(50),
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_created_at ON analytics_events(created_at DESC);
CREATE INDEX idx_analytics_page_slug ON analytics_events(page_slug);

CREATE TABLE integration_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(50) NOT NULL UNIQUE, -- GITHUB, LEETCODE, GFG, WHATSAPP, EMAIL, ANALYTICS, AI
    account_identifier VARCHAR(150) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    api_endpoint VARCHAR(500),
    credentials_encrypted TEXT,
    sync_interval_minutes INT NOT NULL DEFAULT 60,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    health_status VARCHAR(50) NOT NULL DEFAULT 'HEALTHY', -- HEALTHY, DEGRADED, ERROR, DISABLED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE integration_sync_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES integration_accounts(id) ON DELETE CASCADE,
    job_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE integration_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES integration_sync_jobs(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES integration_accounts(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL, -- SUCCESS, WARNING, FAILURE
    records_synced INT NOT NULL DEFAULT 0,
    execution_time_ms INT NOT NULL DEFAULT 0,
    error_message TEXT,
    log_details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_integration_sync_logs_acc ON integration_sync_logs(account_id);
CREATE INDEX idx_integration_sync_logs_created ON integration_sync_logs(created_at DESC);
