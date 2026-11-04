-- V4__ai_assistant_and_rag.sql
-- AI Assistant Configuration, RAG Knowledge Chunks, and Query Logs

-- 1. AI Configuration (Admin controlled)
CREATE TABLE IF NOT EXISTS ai_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    system_prompt TEXT NOT NULL,
    model_name VARCHAR(100) NOT NULL DEFAULT 'gemini-1.5-pro',
    temperature NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    max_tokens INT NOT NULL DEFAULT 1024,
    enabled_sources JSONB NOT NULL DEFAULT '["PROJECTS", "SKILLS", "EXPERIENCE", "RESEARCH", "LAB", "WRITING", "STORIES", "GITHUB", "LEETCODE", "GFG", "EDUCATION"]'::jsonb,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. AI Knowledge Chunks (RAG Index)
CREATE TABLE IF NOT EXISTS ai_knowledge_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type VARCHAR(50) NOT NULL, -- PROJECT, SKILL, EXPERIENCE, RESEARCH, LAB, WRITING, STORY, GITHUB, CODING, EDUCATION
    source_id VARCHAR(150),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    url VARCHAR(500),
    is_ai_visible BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_chunks_source ON ai_knowledge_chunks(source_type);
CREATE INDEX IF NOT EXISTS idx_ai_chunks_visibility ON ai_knowledge_chunks(is_ai_visible);

-- 3. AI Query Logs & Telemetry
CREATE TABLE IF NOT EXISTS ai_query_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100),
    user_query TEXT NOT NULL,
    retrieved_chunks_count INT NOT NULL DEFAULT 0,
    answer TEXT NOT NULL,
    latency_ms INT NOT NULL DEFAULT 0,
    is_error BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    client_ip VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_query_logs_created_at ON ai_query_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_query_logs_error ON ai_query_logs(is_error);

-- 4. Add ai_visible flag across content tables
ALTER TABLE projects ADD COLUMN IF NOT EXISTS ai_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS ai_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE research ADD COLUMN IF NOT EXISTS ai_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE lab_experiments ADD COLUMN IF NOT EXISTS ai_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE contribution_stories ADD COLUMN IF NOT EXISTS ai_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE skills ADD COLUMN IF NOT EXISTS ai_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE experiences ADD COLUMN IF NOT EXISTS ai_visible BOOLEAN NOT NULL DEFAULT TRUE;

-- 5. Seed default AI configuration
INSERT INTO ai_configurations (id, is_enabled, system_prompt, model_name, temperature, max_tokens)
VALUES (
    '50000000-0000-0000-0000-000000000001',
    TRUE,
    'You are "Ask Raviranjan", the verified AI Portfolio Assistant for Raviranjan. Answer user questions with technical precision and factual honesty based strictly on the retrieved portfolio knowledge chunks. Never invent experience, projects, skills, statistics, companies, or achievements. If the user asks for information not present in the provided portfolio chunks, reply exactly with: "I don''t have that information in Raviranjan''s portfolio." When relevant, embed clickable markdown links to projects, GitHub repositories, research papers, or articles.',
    'gemini-1.5-pro',
    0.00,
    1024
) ON CONFLICT DO NOTHING;

-- 6. Seed core knowledge chunks for instantaneous RAG retrieval
INSERT INTO ai_knowledge_chunks (source_type, source_id, title, content, url, is_ai_visible)
VALUES 
    ('PROJECT', 'rakshagrid', 'Project RakshaGrid: Resilient Grid Protection Architecture', 
     'RakshaGrid is a decentralized microgrid fault isolation and load balancing system architected by Raviranjan. It utilizes Rust and C++20 for sub-millisecond edge telemetry, combined with a Spring Boot control plane and PostgreSQL event store. Key innovations include Byzantine fault-tolerant consensus for distributed circuit isolation during blackout scenarios.', 
     '/projects/rakshagrid', TRUE),
    
    ('PROJECT', 'raviranjan-os-core', 'Raviranjan.OS: Developer Operating System', 
     'Raviranjan.OS is a production-grade personal developer operating system engineered with Java 21, Spring Boot 3.3, PostgreSQL 16, Redis 7, and Next.js 14. It enforces strict separation between manual CMS content and automated external data pipelines (GitHub, LeetCode, GFG), featuring real-time SSE telemetry, and an automated RAG assistant.', 
     '/projects/raviranjan-os-core', TRUE),
    
    ('PROJECT', 'distributed-kafka-ingestion', 'Distributed Kafka Ingestion Engine', 
     'High-throughput distributed ingestion engine handling 250,000 events/second. Built using Java 21 Virtual Threads, Apache Kafka, Apache Flink for stream windowing, and ClickHouse for analytical queries. Implemented exactly-once semantics with transactional outbox patterns.', 
     '/projects/distributed-kafka-ingestion', TRUE),
    
    ('SKILL', 'backend-java', 'Primary Backend Expertise: Java 21 & Spring Boot', 
     'Raviranjan possesses expert-level mastery in Java 21 and Spring Boot, specializing in Virtual Threads (Project Loom), Spring Security 6, Spring Data JPA, Spring AI, and high-concurrency microservices with over 6 years of production experience.', 
     '/skills', TRUE),
    
    ('SKILL', 'database-pg', 'Database Specialization: PostgreSQL & Redis', 
     'Raviranjan is deeply experienced with PostgreSQL (query optimization, JSONB indexing, connection pooling with HikariCP, and schema normalization) and Redis (L2 distributed caching, rate-limiting token buckets, and distributed pub/sub buses).', 
     '/skills', TRUE),
    
    ('RESEARCH', 'distributed-consensus-p2p', 'Research Paper: High-Throughput BFT Consensus in Heterogeneous Edge Networks', 
     'Published research paper authored by Raviranjan exploring optimistic Byzantine fault tolerance algorithms under adversarial network partitions. Evaluated across 100 geographically distributed nodes with 4x throughput improvements over classic PBFT. DOI: 10.1145/3456789.3456790.', 
     '/research/distributed-consensus-p2p', TRUE),
     
    ('EXPERIENCE', 'lead-architect', 'Lead Systems Architect & Distributed Systems Engineer', 
     'Lead Systems Architect specializing in cloud-native infrastructure, high-throughput distributed pipelines, event-driven architectures, and developer tooling. Extensive experience leading technical decisions, authoring architectural RFCs, and mentoring engineering teams.', 
     '/experience', TRUE),

    ('CODING', 'leetcode-stats', 'LeetCode Competitive Programming Statistics', 
     'Raviranjan has solved 550+ verified algorithmic problems on LeetCode (180 Easy, 290 Medium, 80 Hard), ranking in the top 4.2% globally with an acceptance rate of 68.4%. Specializes in Dynamic Programming, Graph Algorithms, and Advanced Concurrency.', 
     '/coding', TRUE),

    ('STORY', 'open-source-kafka-pr', 'Open Source Contribution: Apache Kafka Consumer Rebalance Optimization', 
     'Contributed a performance optimization to Apache Kafka consumer group partition rebalancing under network flaps, reducing partition lock contention by 34% through adaptive backoff timers.', 
     '/contributions', TRUE)
ON CONFLICT DO NOTHING;
