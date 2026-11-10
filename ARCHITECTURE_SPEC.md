# Architecture Specification — Raviranjan.OS

## Modules
1. **Identity & Auth**: BCrypt hashing, JWT tokens, 2-step database OTP reset, failed login rate-limiting (5 attempts / 15 min lockout).
2. **Telemetry Engine**: 100% real live sync with GitHub (144 contributions in the last year), LeetCode (31 solved, 11-day streak), and GeeksforGeeks (234 score, 68 solved).
3. **Projects Showcase**: Architectural cards with category filters (AI/RAG, Backend, Full-Stack) and technical diff modals.
4. **Engineering Lab & Research**: Empirical benchmarks and peer-reviewed system publications.
5. **Command Palette**: Raycast/Linear-style quick jump menu (`Cmd+K`).
