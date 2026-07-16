# Multi-tenant Notification Service

A Spring Boot service that dispatches notifications across multiple channels (email, SMS,
push, in-app) for multiple tenants, with tenant-defined templates, scheduled and immediate
sends, per-tenant rate limiting and fairness, retry with backoff on transient failures, and
a persisted delivery audit trail.

## Tech stack and reasoning

- **Java 17 + Spring Boot 3.5.16** — the assignment specifies Spring Boot; 3.5.x is the
  latest stable line on a well-understood, widely-documented API surface (Spring Boot 4 had
  just landed and renames several starter artifacts — not worth the unfamiliarity risk on a
  48-hour build).
- **Spring Web, Spring Data JPA, Spring Security, Bean Validation** — standard REST +
  persistence + auth stack.
- **Spring `@Scheduled`** for the dispatch poller instead of Quartz — the design is a
  DB-row-driven poller (`scheduled_at` / `next_attempt_at` columns, `SELECT ... FOR UPDATE
  SKIP LOCKED` claim), which doesn't need a distributed job store. Quartz would add
  complexity with no benefit here.
- **JJWT** for stateless JWT auth — simple, no OAuth/SSO (explicitly out of scope).
- **H2 (file-based) as the default profile, PostgreSQL as an optional `postgres` profile** —
  the service runs out of the box with zero setup; anyone grading this doesn't need Postgres
  installed. The `postgres` profile is there to show the persistence layer isn't
  H2-specific, and is what SKIP LOCKED behavior is validated against for real-world use.
- **JUnit 5 + Spring Boot Test + MockMvc** — unit tests for pure logic (template
  substitution, backoff, rate limiter), integration tests against the H2 profile for the
  full HTTP + auth + dispatch flow, so tests never depend on an external database.

## Domain model

- **Tenant** — id, name, status (ACTIVE/SUSPENDED), optional global rate limit override.
- **AppUser** — username, password hash, role (`PLATFORM_ADMIN` / `TENANT_ADMIN`), tenant
  (null for platform admins).
- **ChannelConfig** — per-tenant, per-channel enabled flag and a sender identity (e.g. a
  from-address). One row per (tenant, channel).
- **Template** — per-tenant, per-channel, with `{{variable}}` placeholders. Lightweight
  immutable versioning: creating a template starts at version 1; "revising" it creates a new
  version row and deactivates the old one, so history is preserved rather than mutated.
- **NotificationRequest** — the send intent: tenant, template, channel (derived from the
  template), recipient, variables, `scheduledAt`, `idempotencyKey` (unique per tenant),
  status, attempt count/max, `nextAttemptAt`.
- **DeliveryAttempt** — one immutable row per dispatch attempt (success/failure, error,
  worker id, timestamps) — the audit trail.
- **RateLimitPolicy** — optional per-tenant per-channel override of the default rate limit.

Status flow: `PENDING → SENDING → DELIVERED`, or `... → FAILED → RETRY_SCHEDULED → SENDING`
(looping until `maxAttempts`), or `→ DEAD_LETTER`. `CANCELLED` is a terminal state reachable
from `PENDING`/`RETRY_SCHEDULED` only.

## Concurrency, fairness, retry design

- **Claim pattern**: the poller claims ready rows with a native
  `SELECT ... FOR UPDATE SKIP LOCKED` query (`NotificationRequestRepository.claimReadyForTenant`).
  Verified working against both H2 and PostgreSQL. Two workers can never process the same
  row — a busy row is simply skipped, not blocked on.
- **Bounded worker pools**: one fixed-size `ExecutorService` per channel type
  (`ChannelWorkerPools`), so a slow/overloaded channel can't starve the others.
- **Fairness**: the poller iterates tenants with ready work and claims a bounded batch
  (`notifsvc.dispatch.batch-size-per-tenant`) per tenant per cycle, rather than draining one
  tenant's entire backlog first — over several poll cycles this round-robins fairly across
  tenants instead of letting one noisy tenant monopolize the workers.
- **Rate limiting**: an in-memory token bucket per (tenant, channel)
  (`RateLimiterRegistry`/`TokenBucket`), refilling continuously based on elapsed time. If a
  claimed row is over its tenant's limit, it's left untouched (still `PENDING`) for the next
  poll cycle rather than blocking a worker thread on it.
- **Retry/backoff**: on failure, `BackoffCalculator` computes an exponential delay (capped,
  with up to 20% jitter) and sets `nextAttemptAt`; the row goes back to `RETRY_SCHEDULED` and
  is picked up again once ready. After `maxAttempts`, it's marked `DEAD_LETTER`.
- **Idempotency**: a unique `(tenant_id, idempotency_key)` constraint plus an upfront lookup
  in `NotificationService.create` — resubmitting the same key returns the existing
  notification (HTTP 200) instead of creating a duplicate (HTTP 201 on first creation). This
  guards against duplicate *creation* from client-side retries; duplicate *dispatch* is
  separately guarded by the SKIP LOCKED claim (a row can only be SENDING once).
- **Channel senders are simulated** — no real email/SMS/push provider integration is in
  scope. Each channel has a `NotificationSender` impl with a configurable random
  latency/failure rate (`notifsvc.senders.*`), so the retry/backoff path is actually
  exercisable end-to-end (see tests and the manual verification below).

## Notable assumptions

- **Template → channel binding**: a template belongs to exactly one channel; a notification
  request references a template, and the channel is derived from it (not passed separately).
- **Bootstrap platform admin**: a fresh database has no users, so nobody could create the
  first tenant admin. `BootstrapDataInitializer` seeds one platform admin
  (`notifsvc.bootstrap.platform-admin-username`/`-password`, default `admin` /
  `ChangeMe123!`) on first startup if none exists. Change the password after first login in
  any real deployment.
- **JWT claims, not per-request DB lookups**: after login, the JWT carries role and tenantId
  claims and `JwtAuthFilter` trusts them directly rather than re-querying the user on every
  request. Trade-off: disabling a user or changing their role only takes effect once their
  existing tokens expire (`notifsvc.security.jwt-expiration-minutes`, default 60). Acceptable
  given the assignment's scope (no OAuth/SSO, no production auth hardening expected).
- **Template variable validation happens at creation time**: `NotificationService.create`
  renders the template against the supplied variables before persisting, so a request with
  a missing variable fails fast with 400 instead of silently failing at dispatch time later.
- **Rate limit resolution order**: a channel-specific `RateLimitPolicy` if one exists,
  otherwise the tenant's `globalRateLimitPerMinute`, otherwise a default of 60/minute.
- **Cancellation** is only allowed from `PENDING`/`RETRY_SCHEDULED`; cancelling anything else
  (already sending, delivered, dead-lettered, or already cancelled) is a 409 conflict rather
  than a silent no-op.
- **Single-instance scope**: per the assignment's explicit exclusion of distributed systems,
  the worker pools and rate limiter are in-process/in-memory. The SKIP LOCKED claim pattern
  and DB-persisted `next_attempt_at`/status would still make this correct if scaled to
  multiple instances sharing one Postgres database — the in-memory pieces (rate limiter,
  worker pool sizing) would need to move to a shared store for true multi-instance fairness,
  which is out of scope here.

## Running it

Requires JDK 17 and Maven (or use the included `mvnw`/`mvnw.cmd` wrapper).

```
mvnw.cmd spring-boot:run
```

Runs on `http://localhost:8080` against a file-based H2 database under `./data` — no setup
required. A platform admin is seeded automatically (`admin` / `ChangeMe123!`).

To run against PostgreSQL instead (requires a local Postgres with a `notification_service`
database and a `notifsvc` role already created — see `application-postgres.properties`):

```
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

### Example flow

```
POST /api/auth/login                        {"username":"admin","password":"ChangeMe123!"}
POST /api/tenants                           (platform admin) create a tenant
POST /api/tenants/{id}/admins               (platform admin) create its tenant admin
POST /api/auth/login                        (as the tenant admin)
PUT  /api/channels                          configure a channel
POST /api/templates                         create a template
POST /api/notifications                     send (immediate or scheduled, idempotency key required)
GET  /api/notifications/{id}                check status
GET  /api/notifications/{id}/attempts       audit trail of dispatch attempts
GET  /api/reports/delivery                  per-tenant delivery aggregates
```

## Testing

```
mvnw.cmd test
```

22 tests: unit tests for template substitution, backoff growth/capping, and token-bucket
capacity/refill; integration tests (MockMvc + H2) covering auth/RBAC, the full tenant-admin
management flow with template versioning, and the notification dispatch flow — including
waiting for the async poller to actually deliver a notification, idempotent resubmission,
cancellation conflict handling, and upfront template-variable validation.

Every core flow (login, RBAC rejection, tenant/template/channel CRUD, immediate dispatch to
DELIVERED, forced-failure retry walking `RETRY_SCHEDULED` → `DEAD_LETTER`, delivery
reporting) was also exercised manually end-to-end against a running instance during
development, not just under tests.

## AI workflow

Built with Claude Code. `CLAUDE.md` at the repo root carries the project-specific context
(stack, domain model, concurrency rules, conventions) so the agent's decisions stayed
consistent across the session rather than being re-derived each time.

No custom Claude Code skills were authored for this project — development used direct
agent instructions via `CLAUDE.md` plus the harness's built-in tools (file edits, running
Maven, starting/curling the app to verify behavior). The `/caveman` skill was used partway
through purely for terser chat responses (a communication-style preference), not for any
part of the actual implementation or review process.

## Out of scope

Per the assignment: no UI/frontend, no deployment/containerization/CI, no distributed
systems/microservices, no OAuth/SSO/MFA, no production-grade observability tooling.
