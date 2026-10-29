---
name: java-springboot-backend
description: Java 17 Spring Boot backend engineering guidance for the media-gateway project. Use when implementing, reviewing, or refactoring Java backend APIs, controllers, services, MyBatis-Plus persistence, PostgreSQL 14 schema/migrations, Redis session/idempotency logic, REST endpoints, or external HTTP/webhook integrations. Apply the project BaseResponse response style, snake_case API contracts, session-token auth with role checks, and the pagination/error conventions.
---

# Java Spring Boot Backend（media-gateway）

## Core Role

Act as a senior Java backend engineer for this project. Prefer Java 17, Spring Boot 3.3.5, MyBatis-Plus, PostgreSQL 14,
Redis, HTTP and WebSocket patterns that fit the existing codebase before introducing new abstractions.

When API responses, pagination, or error payloads are involved, read `references/api-response-style.md` before editing or reviewing code.
When designing tables or migrations, read `references/pq-schema-style.md`.

## Backend Workflow

1. Inspect nearby controllers, services, mappers, entities, DTOs, and tests before changing behavior.
2. Keep controller methods thin: validate/request-map at the edge, delegate business rules to services, and return the unified response wrapper.
3. Keep service methods transaction-aware and explicit about failure behavior. Use `@Transactional` for multi-write operations and avoid broad transactions around slow network calls.
4. Keep mapper/entity changes compatible with MyBatis-Plus conventions already used in the repository; entities extend `persistence/BaseEntity`.
5. Keep API contracts in `snake_case` on the wire; query parameters are bound explicitly with snake_case names.
6. Run the narrowest meaningful tests or compile checks available for the touched module.

## API Style

- Return all business APIs through the project `BaseResponse` shape; success message is `ok`.
- Put normal business data under `data`.
- Put paginated `items`, `page`, `page_size`, and `total` together under `data` (use `PageResult`).
- Reserve `extra` for non-standard metadata such as debug information.
- Keep error `code` aligned with the HTTP status; business extension codes start from 10000.
- For errors, return a stable `code`, readable `message`, and only include actionable structured detail in `data` when clients can use it.

## Authentication And Roles

- Protected endpoints require `Authorization: Bearer <sessionToken>`; sessions are stored in Redis by `SessionService`.
- Public endpoints are configured in `mgw.security.public-paths`; do not hardcode authentication bypasses.
- Role checks use `@RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})`; method annotation wins over class annotation.
- Read the current user through `security/AuthContext`, never parse the Authorization header in business code.
- Never log tokens, passwords, secrets or full session identifiers.

## Spring Boot Guidelines

- Prefer constructor injection (`@RequiredArgsConstructor`) and the repository's established style.
- Keep request DTOs separate from persistence entities; return VOs, never entities.
- Use Bean Validation annotations on request DTOs; validation failures are handled globally.
- Keep configuration in typed properties classes under `config/` (`mgw.*` prefix).
- Do not leak internal exceptions, SQL details, stack traces, or Redis keys through API responses.

## MyBatis-Plus Guidelines

- Prefer built-in CRUD, `LambdaQueryWrapper`, and typed column references over string column names.
- Keep query conditions explicit about tenant/user/permission scope.
- Use `Page<T>` + `PageResult.of(...)` consistently for list endpoints.
- Avoid N+1 queries in list endpoints. Batch related lookups or use well-scoped joins where appropriate.
- For updates, protect against accidental full-table changes; require clear `where` conditions.
- `delete_flag` is maintained by `@TableLogic`; never write physical `DELETE` for business data.

## PostgreSQL 14 Guidelines

- Use PostgreSQL-compatible SQL and data types; do not assume MySQL-only syntax.
- Prefer `timestamptz` for time data; status uses `SMALLINT`; business keys use `VARCHAR(64)` (UUIDv7 hex).
- Dual key design: `BIGINT GENERATED ALWAYS AS IDENTITY` physical primary key + business key `xxx_id`.
- No physical foreign keys; create indexes for logical foreign keys and frequent filters.
- Keep JSON/JSONB use deliberate: useful for flexible metadata, not a substitute for frequently filtered relational columns.
- Use migrations in `migrations/versions/` instead of ad hoc DDL; include table/column/constraint comments.

## Redis Guidelines

- Build keys through `common/util/RedisKey`; keep the `mgw:` prefix and module segment.
- Always set TTLs for session, idempotency, and temporary keys unless persistence is intentional.
- Use atomic operations (`SET NX PX`, Lua) for lock/counter/idempotency workflows that must be race-safe.
- Treat Redis as a cache or coordination layer unless the existing system explicitly uses it as durable state.

## HTTP / Webhook Guidelines

- For HTTP APIs, keep methods semantically correct: `GET` for reads, `POST` for creation/actions, `PUT/PATCH` for updates, `DELETE` for deletions.
- Handle idempotency for retry-prone actions with `@Idempotent` + `Idempotency-Key`.
- Call upstream systems (e.g. ZLMediaKit REST API) through `integration/ExternalHttpClient` to inherit timeouts, trace-id propagation and error mapping.
- For inbound webhooks: verify HMAC signature, enforce event-id idempotency with a unique index, persist the raw payload, return quickly, and process asynchronously with retry.
- For outbound webhooks: sign payloads, retry with backoff, record delivery state, and document multi-instance locking requirements.
