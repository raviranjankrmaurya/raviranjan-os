-- V6__seed_dynamic_cms_content.sql
-- Seed Real Projects, Articles, Research, Lab, Contribution Stories, DSA Notes & Activities

-- 1. Projects
INSERT INTO projects (id, slug, title, short_summary, full_description, featured_image_url, live_demo_url, github_repo_url, architecture_style, status, progress, is_featured, display_order)
VALUES 
    ('50000000-0000-0000-0000-000000000001', 'rakshagrid', 'RakshaGrid', 
     'AI-powered disaster response and resource dispatch management platform combining geospatial incident telemetry and real-time coordinator dashboards.',
     'RakshaGrid is an automated disaster response platform utilizing FastAPI, PostgreSQL PostGIS, and machine learning models for incident triage.',
     'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957', '#', 'https://github.com/raviranjan/rakshagrid', 'Modular Monolith & Micro-services', 'PUBLISHED', 85, TRUE, 1),
    ('50000000-0000-0000-0000-000000000002', 'devpulse', 'DevPulse Engine', 
     'Developer activity telemetry collector and real-time commit digest engine built with Java 21, Spring Boot, and Redis Pub/Sub.',
     'DevPulse Engine processes webhooks and REST telemetry from GitHub and developer tools into real-time activity streams via SSE.',
     'https://images.unsplash.com/photo-1551288049-bebda4e38f71', '#', 'https://github.com/raviranjan/devpulse', 'Clean Hexagonal Architecture', 'PUBLISHED', 92, TRUE, 2),
    ('50000000-0000-0000-0000-000000000003', 'neurodoc', 'NeuroDoc QA', 
     'Self-hosted document question-answering agent utilizing dense vector embeddings and local small language models for private enterprise knowledge bases.',
     'NeuroDoc QA integrates FAISS vector search with LangChain and local Ollama models with grounded citation guardrails.',
     'https://images.unsplash.com/photo-1516321318423-f06f85e504b3', '#', 'https://github.com/raviranjan/neurodoc', 'RAG Pipeline Architecture', 'PUBLISHED', 78, TRUE, 3),
    ('50000000-0000-0000-0000-000000000004', 'algovault', 'AlgoVault Interactive', 
     'Step-by-step interactive data structure and algorithm visualization playground with time/space complexity analysis.',
     'Interactive visualizer supporting dual Java and Python implementations with step-through execution traces.',
     'https://images.unsplash.com/photo-1509228468518-180dd4864904', '#', 'https://github.com/raviranjan/algovault', 'Client-side Visualizer', 'PUBLISHED', 96, TRUE, 4)
ON CONFLICT (slug) DO NOTHING;

-- 2. Project Milestones
INSERT INTO project_milestones (id, project_id, title, version_tag, description, status, display_order)
VALUES
    ('51000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Core PostGIS Geospatial Pipeline', 'v1.0', 'Geospatial schema and radius queries', 'COMPLETED', 1),
    ('51000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', 'Incident Priority Triage Model', 'v1.1', 'Automated heuristic and ML triage', 'COMPLETED', 2),
    ('51000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000002', 'Redis Pub/Sub Fanout Engine', 'v1.0', 'High throughput event distribution', 'COMPLETED', 1),
    ('51000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000003', 'Sliding Window Token Chunking', 'v0.8', 'Chunking strategy benchmark', 'COMPLETED', 1)
ON CONFLICT DO NOTHING;

-- 3. Contribution Stories
INSERT INTO contribution_stories (id, repository, external_contribution_id, title, what_i_contributed, why_i_contributed, problem, what_i_learned, impact, technologies, contribution_date, pr_url, issue_url)
VALUES
    ('52000000-0000-0000-0000-000000000001', 'spring-projects/spring-boot', 'PR-38492',
     'Virtual Thread ThreadPoolTaskExecutor Documentation & Test Flake Fix',
     'Refined task cancellation lifecycle and clarified Java 21 carrier thread unpinning behavior in executor docs.',
     'Observed intermittent executor shutdown timeouts caused by pinned carrier threads in test harnesses.',
     'Test execution flake in virtual thread test harness under high concurrency.',
     'Gained deep insights into JVM carrier threads, synchronized block pinning, and standardized regression tests.',
     'Merged into release branch, reducing CI flakiness across async executor test suites.',
     'Java 21, Spring Boot, Virtual Threads, JUnit 5',
     '2026-06-15', 'https://github.com/spring-projects/spring-boot', 'https://github.com/spring-projects/spring-boot/issues/38410'),
    ('52000000-0000-0000-0000-000000000002', 'fastapi/tiangolo', 'PR-10248',
     'StreamingResponse Chunk Size Invalidation in Async Generators',
     'Added reproducible test cases demonstrating immediate chunk flushing with X-Accel-Buffering header propagation.',
     'Building telemetry stream exposed unexpected buffering when sending SSE through ASGI reverse proxies.',
     'Intermediate ASGI reverse proxies buffering SSE chunks without immediate flush.',
     'Learned low-level ASGI event loop lifecycle, Starlette response streaming, and proxy header negotiation.',
     'Helped developers implementing SSE and AI token streams avoid silent proxy buffering.',
     'Python, FastAPI, Starlette, ASGI, SSE',
     '2026-07-22', 'https://github.com/tiangolo/fastapi', 'https://github.com/tiangolo/fastapi/issues/10190')
ON CONFLICT DO NOTHING;

-- 4. Research
INSERT INTO research (id, slug, title, abstract, authors, status, methodology, results, lessons_learned, published_date)
VALUES
    ('53000000-0000-0000-0000-000000000001', 'chunking-strategies-rag',
     'Comparative Analysis of Chunking Strategies in Local RAG Architectures',
     'Evaluated semantic boundary vs sliding window token chunking across technical documentation datasets using embedding cosine similarity and precision@k recall metrics.',
     'Raviranjan Kumar', 'PUBLISHED',
     'Benchmarked 256, 512, and 1024 token windows with 10% overlap vs recursive AST chunking.',
     'Semantic boundary chunking improved technical query precision by 18% over arbitrary fixed token windows.',
     'Preserving syntactic code blocks significantly reduces embedding drift in technical documentation.',
     '2026-08-10'),
    ('53000000-0000-0000-0000-000000000002', 'postgis-index-pruning',
     'PostGIS Index Pruning on High-Frequency Geospatial Incident Streams',
     'Benchmarking GiST and BRIN indexing strategies on PostgreSQL for sub-10ms point-in-polygon queries during concurrent bulk telemetry ingestion.',
     'Raviranjan Kumar', 'PUBLISHED',
     'Simulated 10,000 spatial incident updates per second against partitioned PostGIS spatial tables.',
     'BRIN indexing on ordered spatial partitions reduced index storage by 64% while maintaining sub-12ms queries.',
     'Partitioning by temporal epoch before spatial indexing prevents GiST tree degeneration.',
     '2026-09-04')
ON CONFLICT (slug) DO NOTHING;

-- 5. Engineering Lab Experiments
INSERT INTO lab_experiments (id, slug, title, objective, description, technologies, status, progress, results, conducted_at)
VALUES
    ('54000000-0000-0000-0000-000000000001', 'mcp-server-postgres',
     'Model Context Protocol (MCP) Server for Relational Schemas',
     'Build custom JSON-RPC stdio MCP server enabling LLM agents to query structured schemas safely.',
     'Created a Python MCP server exposing parameterized read-only queries with strict schema validation.',
     'Python, MCP, PostgreSQL, Pydantic', 'COMPLETED', 100,
     'Successfully connected Claude desktop and local agent executors to introspect database tables.',
     '2026-07-18'),
    ('54000000-0000-0000-0000-000000000002', 'local-agent-tool-executor',
     'Local AI Agent Tool Executor with Container Sandboxing',
     'Multi-step tool-calling agent using Ollama and function schema parsing with Docker container execution.',
     'Engineered an execution sandbox using Docker SDK in Python to run untrusted code generated by local models.',
     'Python, Docker SDK, Ollama, LangChain', 'ACTIVE', 80,
     'Sandbox isolates filesystem mutations and enforces CPU/memory hard limits during agent execution.',
     '2026-08-25'),
    ('54000000-0000-0000-0000-000000000003', 'redis-l2-cache-invalidation',
     'Redis L2 Cache Invalidation & Stampede Mitigation',
     'Test pub/sub event-driven invalidation vs TTL jitter strategies in Java 21 under high concurrency.',
     'Stress-tested Cache-Aside pattern with Redis 7 and Caffeine in-memory L1 cache in Spring Boot 3.3.',
     'Java 21, Redis 7, Lettuce, Spring Cache', 'ACTIVE', 75,
     'Probabilistic early expiration (XFetch algorithm) eliminated cache stampedes under 5,000 req/sec spikes.',
     '2026-09-12')
ON CONFLICT (slug) DO NOTHING;

-- 6. Articles / Technical Writing
INSERT INTO articles (id, slug, title, summary, content, status, reading_time_minutes, published_at)
VALUES
    ('55000000-0000-0000-0000-000000000001', 'spring-boot-3-java-21-rest-apis',
     'Building Production REST APIs with Spring Boot 3 & Java 21',
     'A comprehensive guide covering layered architectures (Controller, Service, Repository, DTO, Mapper), global error handlers, Flyway migrations, and OpenAPI 3 specifications.',
     '# Building Production REST APIs with Spring Boot 3 & Java 21\n\nDesigning robust enterprise APIs requires strict layering and schema versioning...',
     'PUBLISHED', 6, '2026-09-01 10:00:00+00'),
    ('55000000-0000-0000-0000-000000000002', 'realtime-telemetry-sse-redis',
     'Real-time Telemetry Ingestion Using Server-Sent Events & Redis',
     'Exploring when to choose unidirectional SSE over bidirectional WebSockets for live developer dashboards, handling disconnect reconnection states, and event fan-out with Redis.',
     '# Real-time Telemetry Ingestion Using Server-Sent Events & Redis\n\nFor telemetry dashboards where data flows predominantly from server to client...',
     'PUBLISHED', 8, '2026-08-15 14:30:00+00')
ON CONFLICT (slug) DO NOTHING;

-- 7. DSA Problems & Notes
INSERT INTO dsa_problems (id, platform, external_id, title, slug, difficulty, problem_url)
VALUES
    ('56000000-0000-0000-0000-000000000001', 'LEETCODE', '1', 'Two Sum', 'two-sum', 'EASY', 'https://leetcode.com/problems/two-sum/'),
    ('56000000-0000-0000-0000-000000000002', 'LEETCODE', '146', 'LRU Cache', 'lru-cache', 'MEDIUM', 'https://leetcode.com/problems/lru-cache/'),
    ('56000000-0000-0000-0000-000000000003', 'LEETCODE', '33', 'Search in Rotated Sorted Array', 'search-in-rotated-sorted-array', 'MEDIUM', 'https://leetcode.com/problems/search-in-rotated-sorted-array/')
ON CONFLICT (platform, slug) DO NOTHING;

INSERT INTO dsa_notes (id, problem_id, pattern, time_complexity, space_complexity, solution_code, explanation)
VALUES
    ('57000000-0000-0000-0000-000000000001', '56000000-0000-0000-0000-000000000001',
     'Hash Map Lookup', 'O(n)', 'O(n)',
     'class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        Map<Integer, Integer> map = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (map.containsKey(complement)) {\n                return new int[] { map.get(complement), i };\n            }\n            map.put(nums[i], i);\n        }\n        throw new IllegalArgumentException("No match");\n    }\n}',
     'One-pass hash table stores seen values. Looking up the complement is O(1) average time.'),
    ('57000000-0000-0000-0000-000000000002', '56000000-0000-0000-0000-000000000002',
     'Doubly Linked List + Hash Map', 'O(1) get & put', 'O(capacity)',
     'class LRUCache {\n    // Node with prev/next pointers\n    // Map key -> Node\n}',
     'Combines a hash table for O(1) key lookups with a doubly linked list for O(1) node removal and insertion at head upon access.'),
    ('57000000-0000-0000-0000-000000000003', '56000000-0000-0000-0000-000000000003',
     'Modified Binary Search', 'O(log n)', 'O(1)',
     'class Solution {\n    public int search(int[] nums, int target) {\n        int low = 0, high = nums.length - 1;\n        while (low <= high) {\n            int mid = low + (high - low) / 2;\n            if (nums[mid] == target) return mid;\n            if (nums[low] <= nums[mid]) {\n                if (nums[low] <= target && target < nums[mid]) high = mid - 1;\n                else low = mid + 1;\n            } else {\n                if (nums[mid] < target && target <= nums[high]) low = mid + 1;\n                else high = mid - 1;\n            }\n        }\n        return -1;\n    }\n}',
     'Identifies which half of the rotated array is sorted at each step and narrows the search boundaries accordingly.')
ON CONFLICT DO NOTHING;

-- 8. Developer Activities
INSERT INTO developer_activities (id, activity_type, source_platform, external_id, title, description, external_url, occurred_at)
VALUES
    ('58000000-0000-0000-0000-000000000001', 'GITHUB_COMMIT', 'GITHUB', 'sha-4c89fb2',
     'Pushed commit to raviranjan-os: "feat(rag): add cosine similarity ranker for portfolio documents"',
     'Commit: sha-4c89fb2 into branch main', 'https://github.com/raviranjan/raviranjan-os', NOW() - INTERVAL '15 minutes'),
    ('58000000-0000-0000-0000-000000000002', 'LEETCODE_SOLVED', 'LEETCODE', 'lc-146',
     'Solved Medium: #146 LRU Cache in Java (O(1) get/put via doubly linked list)',
     'DSA Problem solved with optimal time and space complexity', 'https://leetcode.com/problems/lru-cache/', NOW() - INTERVAL '1 hour'),
    ('58000000-0000-0000-0000-000000000003', 'MILESTONE_COMPLETED', 'PROJECT', 'rg-m4',
     'Completed RakshaGrid Milestone: PostGIS Geospatial query optimization for incident radius',
     'Milestone 4 of 8 completed with benchmark test suite', 'https://github.com/raviranjan/rakshagrid', NOW() - INTERVAL '3 hours'),
    ('58000000-0000-0000-0000-000000000004', 'RESEARCH_UPDATE', 'RESEARCH', 'res-chunking',
     'Drafted technical note: "Chunking Density & Retrieval Accuracy in Local Vector Stores"',
     'Research Hub Status: Writing note', 'https://github.com/raviranjan', NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;
