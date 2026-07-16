# Multi-tenant Notification Service — CLAUDE.md

Context for AI-assisted development on this project.

## Stack
- Java 17, Spring Boot 3.5.16, Maven
- Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring `@Scheduled` (poller-driven dispatch, no external job store needed)
- JJWT for token auth
- H2 (file mode) as default DB profile — zero-setup run. PostgreSQL as an optional `postgres` profile for production-like persistence.
- JUnit 5 + Spring Boot Test for unit/integration tests

## Domain model
- `Tenant` — id, name, status, global rate-limit override
- `AppUser` — id, tenant_id (nullable for platform admin), username, password hash, role (PLATFORM_ADMIN / TENANT_ADMIN)
- `ChannelConfig` — per-tenant channel (EMAIL/SMS/PUSH/INAPP) settings, enabled flag
- `Template` — per-tenant, per-channel, variable placeholders (`{{var}}`), versioned
- `NotificationRequest` — the send intent: tenant, template, channel, recipient, variables (JSON), scheduled_at, idempotency_key, status, priority
- `DeliveryAttempt` — one row per dispatch attempt: attempt number, status, error, timestamps, worker id — audit trail
- `RateLimitPolicy` — per-tenant per-channel throughput limits

Status flow: `PENDING -> SENDING -> SENT/DELIVERED`, or `FAILED -> RETRY_SCHEDULED -> PENDING` (loop until max attempts), or `DEAD_LETTER`.

## Concurrency & correctness rules
- Claim pattern for dispatch: `SELECT ... FOR UPDATE SKIP LOCKED` — never claim a row two workers might both process.
- One bounded `ThreadPoolExecutor` per channel type; sizes configurable via properties.
- Idempotency: unique constraint `(tenant_id, idempotency_key)` prevents duplicate creation; status check before dispatch prevents duplicate send on retry.
- Rate limiting: in-memory token bucket per (tenant, channel). Over-limit requests stay PENDING for next poll cycle — never block a worker thread waiting on a limiter.
- Fairness: poller pulls top-N pending per tenant round-robin, not global FIFO, so one tenant's backlog can't starve others.
- Retry: exponential backoff + jitter, `next_attempt_at` column drives re-pickup, max attempts configurable per tenant/channel.

## Conventions
- Package layout: `com.notifsvc.{tenant,user,channel,template,notification,delivery,auth,config}` — one package per bounded concept, not per layer.
- No comments unless explaining a non-obvious WHY (a race condition avoided, a workaround for a specific constraint).
- Controllers thin; business logic in services; no logic in entities beyond simple invariants.
- Bean Validation (`@Valid` + JSR-303 annotations) at controller boundary; don't re-validate deeper in the call stack.
- Channel senders are simulated (`NotificationSender` interface, one impl per channel) — no real SendGrid/Twilio integration in scope. Configurable random latency/failure rate so retry/backoff is actually exercisable in tests and demo.
- Tests: unit tests for template substitution, rate limiter, backoff calculation, claim logic. Integration tests via `@SpringBootTest` + MockMvc against the H2 profile (grader has no Postgres — don't make tests depend on it).

## Out of scope (per assignment)
UI/frontend, deployment/containerization/CI, microservices/distributed systems, OAuth/SSO/MFA, production observability tooling.
