-- V5__enhanced_notifications_and_events.sql
-- Real-time Event System & Notification Center Enhancements

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS related_entity_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS related_entity_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS email_dispatched BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS whatsapp_dispatched BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_notifications_priority ON notifications(priority);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at DESC);

-- Seed initial admin notifications
INSERT INTO notifications (id, title, message, level, priority, type, is_read, action_url)
VALUES 
    ('60000000-0000-0000-0000-000000000001', 'GitHub Webhook Verified', 'Commit 4c89fb2 was ingested into raviranjan/raviranjan-os', 'INFO', 'LOW', 'GITHUB', TRUE, '/admin/github'),
    ('60000000-0000-0000-0000-000000000002', 'New Contact Inquiry: Sarah Jenkins', 'Principal Engineering leadership opportunity at FinTech Global', 'ALERT', 'HIGH', 'CRM', FALSE, '/admin/crm'),
    ('60000000-0000-0000-0000-000000000003', 'Milestone Completed: Core Architecture', 'Raviranjan.OS Phase 1 & 2 baseline completed with 100% test coverage', 'INFO', 'MEDIUM', 'MILESTONE', FALSE, '/admin/projects')
ON CONFLICT DO NOTHING;
