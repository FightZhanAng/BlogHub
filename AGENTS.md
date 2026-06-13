# AGENTS.md

## Rules

When you need to search docs, use `context7` tools.

## Project

BlogHub — full-stack Chinese-language tech blog. Monorepo, two independent sub-projects:

- `blog-backend/` — Spring Boot 2.7.18 + MyBatis-Plus + MySQL 8 (Java 8 source level)
- `blog-frontend/` — Vue 3 + Vite + Element Plus + Pinia

Root `docker-compose.yml` orchestrates MySQL, backend, frontend/nginx.

## Commands

### Backend (from `blog-backend/`)
```
mvn spring-boot:run              # Start on :8081
mvn package -DskipTests          # Build JAR
mvn test                         # Run tests
mvn test -Dtest=ClassName        # Run single test class
```

### Frontend (from `blog-frontend/`)
```
npm install
npm run dev        # Dev server on :3000 (proxies /api + /uploads to :8081)
npm run build      # Production build → dist/
npm run lint       # ESLint with auto-fix
```

### Docker (from project root)
```
docker-compose up -d        # MySQL :3307, Backend :8081, Frontend :80
docker-compose down
```

### Database
```
mysql -u root -p < blog-backend/sql/init.sql
```
MySQL client: `D:\MySQL\MySQL Server 5.7\bin\mysql.exe` (not in PATH).

## Architecture

### Backend (package `com.blog`)

```
controller/  → REST endpoints, all under /api
service/     → Business logic (interfaces in service/, impls in service/impl/)
mapper/      → MyBatis-Plus mapper interfaces (extend BaseMapper<Entity>)
entity/      → DB entities with Lombok @Data, extend BaseEntity (auto fill timestamps)
dto/         → Request/response DTOs
config/      → Auth, CORS, JWT, MyBatis-Plus, WebMvc
common/      → Result wrapper, enums, rate limiting, audit logging, HTML sanitizer
aspect/      → AOP aspects (RateLimitAspect, AuditLogAspect, LoggingAspect)
exception/   → GlobalExceptionHandler
```

All endpoints return `Result<T>`: `{ code, message, data }`. Use static factories: `Result.success(data)`, `Result.created(data)`, `Result.notFound(msg)`, etc.

Auth: JWT (24h access + 7d refresh), `AuthInterceptor` on `/api/**` except public reads. Roles: `admin` / `user`.

### Frontend (src/)

```
api/          → Axios modules per domain + shared request.js (baseURL: /api, auto Bearer token)
views/        → 26 page components
components/   → 18 shared UI components
composables/  → useApi, useInteraction, useMarkdown, usePosts, useAiChat
stores/       → Pinia: auth (token/user/role, persisted to localStorage), menu (dynamic tree)
router/       → 24 routes, MainLayout.vue shell, beforeEach guard (auth + SEO meta)
layouts/      → MainLayout.vue
styles/       → global.css
utils/        → visitorId, logger, tracker
```

Response interceptor auto-unwraps `Result.data`. 401 → clear auth → redirect `/login`.

### Database

- MySQL 8, database `blog_db`, 30 tables
- Single init script: `blog-backend/sql/init.sql` (all tables + seed data)
- All tables MUST have `created_at` + `updated_at`; entities extend `BaseEntity`
- MyBatis-Plus: auto-increment IDs, underscore-to-camelCase, SQL logged to stdout in dev

## Conventions

- Every new table needs `created_at` + `updated_at` columns
- All DB schema changes must sync to `init.sql`; remove redundant migration files
- All feature pushes must update `README.md`
- When `CLAUDE.md` needs updating, remind the user (don't auto-update)
- Language: Chinese (zh-CN) for UI and docs, English for code
- Upload directory: `./uploads` relative to backend working dir
- API docs: Knife4j at `http://localhost:8081/doc.html`

## Environment

- Backend config: `blog-backend/src/main/resources/application.yml`
- Frontend dev server: port 3000, proxies `/api` and `/uploads` to `localhost:8081`
- DB: `localhost:3306/blog_db` (root/root), Docker maps to `:3307`
- Admin creds: `admin` / `admin123`
