# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned (Day 3-14)
- Session module + sidebar UI
- Model router (OpenAI-compatible)
- Streaming chat (SSE) ⭐
- Short/long-term memory
- RAG knowledge base
- Function calling
- Admin dashboard
- Multimodal upload
- Monitoring
- Production hardening

## [0.2.0] - 2026-06-16

### Day 2 - User System & JWT Auth
- Added: SQL schema (5 tables: sys_user/sys_role/sys_user_role/auth_refresh_token/auth_login_log)
- Added: User/Role/UserRole/RefreshToken/LoginLog entities
- Added: MyBatis-Plus mappers (5) + XML mappings (2)
- Added: JWT dual-token (access 30min + refresh 7d) with SHA-256 hashed refresh
- Added: Spring Security 6 stateless config + JWT filter + JSON entry points
- Added: AuthService (register/login/refresh/logout/me)
- Added: AuthController 5 REST endpoints
- Added: AuthApplication (standalone runnable)
- Added: Frontend real login page (login/register tabs)
- Added: Pinia user store with dual-token persistence
- Added: Axios 401 auto-refresh and retry
- Added: Full router guards
- Added: Vite proxy `/api/v1/auth` -> :8081
- Added: Unit tests (4 cases)
- Added: Self-check scripts (daily-build.sh, java-static-check.sh)
- Fixed: Element Plus Memory icon deprecated -> Cpu
- Fixed: Result.toJsonString() missing
- Fixed: UA length truncation

## [0.1.0] - 2026-06-15

### Day 1 - Project Skeleton
- Added: Spring Boot 3 multi-module Maven structure (7 modules)
- Added: Unified Result wrapper + global exception handler
- Added: Vue 3 + Vite + Element Plus + Pinia skeleton
- Added: Frontend routes (login/chat/knowledge/memory/admin/about)
- Added: Layout (sidebar + topbar + user dropdown)
- Added: docker-compose for MySQL 8 + Redis 7 + ES 8 + MinIO
- Added: Gateway health check + platform intro API
- Added: Daily build script
- Added: Cron setup script
