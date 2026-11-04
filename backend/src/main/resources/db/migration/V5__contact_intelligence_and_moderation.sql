-- V5__contact_intelligence_and_moderation.sql
-- Extends contact_inquiries table with Contact Intelligence, GeoIP, Repeat Abuse Detection, and AI Moderation columns.
-- Note: Hibernate ddl-auto=update also applies these columns automatically in dev/cloud environments.

ALTER TABLE contact_inquiries
    ADD COLUMN IF NOT EXISTS country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS region VARCHAR(100),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS geo_provider VARCHAR(60),
    ADD COLUMN IF NOT EXISTS referrer VARCHAR(512),
    ADD COLUMN IF NOT EXISTS browser_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS message_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS disposable_email BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS repeat_submission BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS repeat_count INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS intent VARCHAR(60) DEFAULT 'GENUINE_INQUIRY',
    ADD COLUMN IF NOT EXISTS is_spam BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_abusive BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_threatening BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS moderation_confidence DOUBLE PRECISION DEFAULT 0.85,
    ADD COLUMN IF NOT EXISTS moderation_reason TEXT,
    ADD COLUMN IF NOT EXISTS recommended_action VARCHAR(100),
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(50) DEFAULT 'APPROVED';

CREATE INDEX IF NOT EXISTS idx_contact_inquiries_message_hash ON contact_inquiries(message_hash);
CREATE INDEX IF NOT EXISTS idx_contact_inquiries_ip_address ON contact_inquiries(ip_address);
CREATE INDEX IF NOT EXISTS idx_contact_inquiries_moderation_status ON contact_inquiries(moderation_status);
CREATE INDEX IF NOT EXISTS idx_contact_inquiries_is_spam ON contact_inquiries(is_spam);
CREATE INDEX IF NOT EXISTS idx_notifications_related_entity_id ON notifications(related_entity_id);
