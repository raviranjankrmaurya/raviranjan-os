-- V7__seed_initial_projects.sql
-- Seed production projects for Raviranjan.OS

INSERT INTO projects (id, slug, title, short_summary, full_description, architecture_style, status, progress, is_featured, display_order, live_demo_url, github_repo_url)
VALUES 
    ('40000000-0000-0000-0000-000000000001',
     'rakshagrid',
     'RakshaGrid: Resilient Grid Protection Architecture',
     'Decentralized microgrid fault isolation and load balancing system with sub-millisecond edge telemetry and Byzantine fault tolerance.',
     'RakshaGrid is a decentralized microgrid fault isolation and load balancing system architected by Raviranjan. It utilizes Rust and C++20 for sub-millisecond edge telemetry, combined with a Spring Boot control plane and PostgreSQL event store. Key innovations include Byzantine fault-tolerant consensus for distributed circuit isolation during blackout scenarios.',
     'Event-Driven Microservices & Edge BFT',
     'PUBLISHED',
     95,
     TRUE,
     1,
     'https://rakshagrid.dev',
     'https://github.com/raviranjankrmaurya/rakshagrid'),

    ('40000000-0000-0000-0000-000000000002',
     'raviranjan-os-core',
     'Raviranjan.OS: Developer Operating System',
     'Production-grade personal developer operating system combining portfolio, headless CMS, real-time telemetry, and RAG assistant.',
     'Raviranjan.OS is a production-grade personal developer operating system engineered with Java 21, Spring Boot 3.3, PostgreSQL 16, Redis 7, and Next.js. It enforces strict separation between manual CMS content and automated external data pipelines (GitHub, LeetCode, GFG), featuring real-time SSE telemetry, and an automated RAG assistant.',
     'Modular Monolith with Hexagonal Ports & Adapters',
     'PUBLISHED',
     100,
     TRUE,
     2,
     'http://localhost:3000',
     'https://github.com/raviranjankrmaurya/raviranjan-os'),

    ('40000000-0000-0000-0000-000000000003',
     'distributed-kafka-ingestion',
     'Distributed Kafka Ingestion Engine',
     'High-throughput distributed ingestion engine handling 250,000 events/second using Java 21 Virtual Threads, Kafka, and ClickHouse.',
     'High-throughput distributed ingestion engine handling 250,000 events/second. Built using Java 21 Virtual Threads, Apache Kafka, Apache Flink for stream windowing, and ClickHouse for analytical queries. Implemented exactly-once semantics with transactional outbox patterns.',
     'Reactive Distributed Stream Processing',
     'PUBLISHED',
     90,
     TRUE,
     3,
     NULL,
     'https://github.com/raviranjankrmaurya/distributed-kafka-ingestion')
ON CONFLICT (slug) DO NOTHING;

-- Link technologies to projects
INSERT INTO project_technologies (project_id, skill_id)
SELECT p.id, s.id 
FROM projects p, skills s 
WHERE p.slug = 'raviranjan-os-core' AND s.name IN ('Java 21 / Spring Boot', 'React & TypeScript', 'PostgreSQL & PostGIS', 'Docker & Git')
ON CONFLICT DO NOTHING;

INSERT INTO project_technologies (project_id, skill_id)
SELECT p.id, s.id 
FROM projects p, skills s 
WHERE p.slug = 'rakshagrid' AND s.name IN ('Java 21 / Spring Boot', 'PostgreSQL & PostGIS', 'AI & RAG Applications')
ON CONFLICT DO NOTHING;

INSERT INTO project_technologies (project_id, skill_id)
SELECT p.id, s.id 
FROM projects p, skills s 
WHERE p.slug = 'distributed-kafka-ingestion' AND s.name IN ('Java 21 / Spring Boot', 'Docker & Git')
ON CONFLICT DO NOTHING;

-- Update official profile social links with Raviranjan's real platform usernames
UPDATE social_links SET url = 'https://github.com/raviranjankrmaurya', handle = 'raviranjankrmaurya' WHERE platform = 'GitHub';
UPDATE social_links SET url = 'https://leetcode.com/raviranjanmaurya', handle = 'raviranjanmaurya' WHERE platform = 'LeetCode';
UPDATE social_links SET url = 'https://auth.geeksforgeeks.org/user/raviranjankrmaurya', handle = 'raviranjankrmaurya' WHERE platform = 'GeeksforGeeks';

-- Update integration accounts
UPDATE integration_accounts SET account_identifier = 'raviranjankrmaurya' WHERE provider = 'GITHUB';
UPDATE integration_accounts SET account_identifier = 'raviranjanmaurya' WHERE provider = 'LEETCODE';
UPDATE integration_accounts SET account_identifier = 'raviranjankrmaurya' WHERE provider = 'GFG';
