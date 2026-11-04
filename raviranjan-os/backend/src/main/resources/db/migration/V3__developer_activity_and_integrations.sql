-- V3__developer_activity_and_integrations.sql
-- Unified Developer Activity and Enhanced Integration Metadata

CREATE TABLE IF NOT EXISTS developer_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_type VARCHAR(50) NOT NULL, -- GITHUB_COMMIT, GITHUB_PR, GITHUB_ISSUE, LEETCODE_SOLVED, GFG_SOLVED, PROJECT_UPDATE, RESEARCH_UPDATE, BLOG_PUBLISHED, MILESTONE_COMPLETED
    source_platform VARCHAR(50) NOT NULL, -- GITHUB, LEETCODE, GFG, SYSTEM, BLOG
    external_id VARCHAR(150),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    external_url VARCHAR(500),
    metadata JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_developer_activities_type ON developer_activities(activity_type);
CREATE INDEX IF NOT EXISTS idx_developer_activities_source ON developer_activities(source_platform);
CREATE INDEX IF NOT EXISTS idx_developer_activities_occurred ON developer_activities(occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_developer_activities_external ON developer_activities(source_platform, external_id);

-- Alter integration_accounts to add sync telemetry columns if missing
ALTER TABLE integration_accounts 
    ADD COLUMN IF NOT EXISTS last_attempt TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS records_processed INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_error_message TEXT,
    ADD COLUMN IF NOT EXISTS secret_token_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS webhook_secret_encrypted TEXT;

-- Seed default integration accounts for GitHub, LeetCode, GFG
INSERT INTO integration_accounts (id, provider, account_identifier, is_enabled, health_status, sync_interval_minutes)
VALUES 
    ('40000000-0000-0000-0000-000000000001', 'GITHUB', 'raviranjan', TRUE, 'HEALTHY', 60),
    ('40000000-0000-0000-0000-000000000002', 'LEETCODE', 'raviranjan', TRUE, 'HEALTHY', 120),
    ('40000000-0000-0000-0000-000000000003', 'GFG', 'raviranjan', TRUE, 'HEALTHY', 180)
ON CONFLICT (provider) DO NOTHING;
