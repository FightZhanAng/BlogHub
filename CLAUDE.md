# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BlogHub is a full-stack Chinese-language tech blog system. Monorepo with two independent sub-projects:

- **`blog-backend/`** — Spring Boot 2.7.18 + MyBatis-Plus + MySQL 8 (Java 8 source level)
- **`vue3-elementplus-starter/`** — Vue 3 + Vite + Element Plus + Pinia

Root-level `docker-compose.yml` orchestrates all three services (MySQL, backend, frontend/nginx).

## Build & Run Commands

### Backend (from `blog-backend/`)
```bash
mvn spring-boot:run              # Start on :8080
mvn package -DskipTests          # Build JAR
mvn test                         # Run tests
mvn test -Dtest=ClassName        # Run single test class
```

### Frontend (from `vue3-elementplus-starter/`)
```bash
npm install
npm run dev        # Dev server on :3000 (proxies /uploads to :8080)
npm run build      # Production build → dist/
npm run lint       # ESLint with auto-fix
```

### Docker (from project root)
```bash
docker-compose up -d        # MySQL :3307, Backend :8080, Frontend :80
docker-compose down
```

### Database initialization
```bash
mysql -u root -p < blog-backend/sql/init.sql
```

## Architecture

### Backend Layering (package `com.blog`)

```
controller/  → REST endpoints, all under /api
service/     → Business logic (interfaces in service/, impls in service/impl/)
mapper/      → MyBatis-Plus mapper interfaces (extend BaseMapper<Entity>)
entity/      → DB entities with Lombok @Data, extend BaseEntity (auto fill create/update time)
dto/         → Request/response DTOs
config/      → Auth, CORS, JWT, MyBatis-Plus, WebMvc
common/      → Result wrapper, enums, rate limiting, audit logging, HTML sanitizer
exception/   → GlobalExceptionHandler → catches BusinessException/ResourceNotFoundException
```

### API Response Contract

All endpoints return `Result<T>` with shape `{ code, message, data }`. Use static factories:
- `Result.success(data)`, `Result.created(data)`, `Result.noContent()`
- `Result.badRequest(msg)`, `Result.notFound(msg)`, `Result.error(ResultCode, msg)`

Codes: 200 success, 201 created, 204 no content, 400 bad request, 401 unauthorized, 403 forbidden, 404 not found.

### Authentication

- JWT with Access Token (24h) + Refresh Token (7d), stored client-side in `localStorage`
- `AuthInterceptor` runs on all `/api/**` except: `/api/auth/**`, public post reads (`GET /api/posts`), user profiles, stats, uploads, hot topics
- Authenticated requests inject `userId`, `username`, `role` as request attributes
- Role-based: `admin` vs `user` — admin routes require `role === 'admin'`
- Rate limiting via `@RateLimit` annotation + AOP aspect (login: 5/60s, register: 3/60s)

### Frontend Architecture

```
src/
  api/          → Axios modules per domain (postApi, authApi, commentApi, etc.)
  api/request.js → Shared Axios instance (baseURL: :8080/api), auto-injects Bearer token + X-Visitor-Id
  views/        → Page components (Home, BlogList, BlogPost, Dashboard, etc.)
  components/   → Shared UI (BlogCard, BlogComments, BlogToc, ImageLightbox, etc.)
  composables/  → useApi, useInteraction, useMarkdown, usePosts
  stores/       → Pinia stores (auth.js — token/user/role state)
  router/       → Vue Router with beforeEach guard (auth + admin checks + SEO meta)
  layouts/      → MainLayout.vue (shell with navbar/sidebar)
```

Key conventions:
- API modules return `request.get/post/put/delete(...)` — response interceptor auto-unwraps `Result.data`
- 401 responses auto-clear auth state and redirect to `/login`
- Element Plus icons globally registered, locale set to `zh-cn`
- Bootstrap Icons loaded via CSS

### Database

- MySQL 8.0, database name `blog_db`
- MyBatis-Plus: auto-increment IDs, underscore-to-camelCase mapping, SQL logged to stdout in dev
- Migration scripts in `blog-backend/sql/` (init.sql) and `blog-backend/src/main/resources/db/`
- Entities use `@TableField(fill = FieldFill.INSERT)` for auto-filled timestamps via `MyMetaObjectHandler`

### Key Features to Know

- **Posts**: Markdown content, custom slug, cover image, draft/scheduled publish, version history (auto-snapshot per edit, max 50)
- **Tags**: Normalized `tag` + `post_tag` join table, `findOrCreate` pattern
- **Comments**: Nested via `parentId`, supports anonymous + authenticated, XSS-sanitized with jsoup
- **Social**: Likes (IP-based for guests, user-based for logged-in), bookmarks, follows, notifications
- **Albums**: Baby photo/video albums with waterfall layout and timeline view
- **Hot Topics**: Aggregated trending from Weibo/Zhihu/Douyin/Bilibili/Toutiao/GitHub (refreshes every 2h)
- **Admin**: Dashboard with ECharts stats, user/comment/image/log management, CSV export
- **SEO**: Per-page `<title>` + `<meta description>` set in router guard
- **RSS/Sitemap**: `/feed.xml` and `/sitemap.xml` served by `FeedController`

### MCP Servers

Configured in `.mcp.json`: codegraph, fetch, memory, sequential-thinking, playwright (Edge browser).

## Environment

- Backend config: `blog-backend/src/main/resources/application.yml`
- Frontend env: `.env` / `.env.development` / `.env.production` in `vue3-elementplus-starter/`
- DB connection: `localhost:3306/blog_db` (root/root), Docker maps to `:3307`
- Upload directory: `../uploads` relative to backend (mapped to `/app/uploads` in Docker)
