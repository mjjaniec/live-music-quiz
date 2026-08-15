# Maestro Account Auth Implementation Plan

## Overview

Add passwordless "magic link" email authentication for the maestro role: a maestro registers/logs in by
requesting a link sent to their email, and maestro-control routes are gated behind that authentication.
Players, the big-screen, and (future) performer surfaces stay fully anonymous with zero added friction.
This is roadmap slice **S-01** (`maestro-account-auth`), implementing PRD FR-001, FR-002, FR-003, and the
precondition half of US-01. It does **not** scope games/set-lists to an owner — that's S-02.

## Current State Analysis

- **No auth exists at all.** `pom.xml` has no `spring-boot-starter-security`; a full grep of
  `src/main/java` for login/security/auth/principal/session returns zero matches. Every `@Route` view is
  reachable by anyone today.
- **`MaestroView.onAttach`** (`views/maestro/MaestroView.java:19-28`) is the layout root for `/maestro` and
  already funnels every *properly-nested* maestro route (`StartGameView` at `start`, `DjView` at `dj`)
  through one place on attach. It is the natural spot to reason about maestro-only access, but Vaadin
  route security is enforced per-view via annotations, not by layout inheritance — each view still needs
  its own annotation (see Phase 3).
- **`FeedbackView`** (`views/maestro/FeedbackView.java:9`) uses the literal route `"maestro/feedback"` with
  no `layout=`, bypassing `MaestroView` entirely. It must be gated explicitly.
- **The player side has an equivalent bypass**: `JoinView` (`views/player/JoinView.java:23`) uses the
  literal route `"player/join"`, bypassing `RootView`/`PlayerView`. It must stay `@AnonymousAllowed`
  explicitly rather than relying on inherited access.
- **Persistence has a clean, repeatable pattern**: `PlayerStore` (plain domain interface) →
  `JpaPlayerStore extends CrudRepository<PlayerDto, String>` (default-method dto↔domain mapping, no
  separate impl class) → `PlayerDto` (`@Data @Entity @Table`). The same triad shape repeats for
  `QuizStore`, `AnswerStore`, `CustomMessageStore`, `FeedbackStore`, `PlayOffStore`, `PlayOffTaskStore`.
- **`RealStageStore`** (`stores/RealStageStore.java`) is the codebase's precedent for a hand-written
  `@Component` that wraps a `CrudRepository` with real branching/mapping logic instead of using simple
  default methods — this is the shape the new magic-link token service follows, because it implements
  Spring Security's own `OneTimeTokenService` interface rather than a plain domain store interface.
- **No shared IT test base class exists.** `SmokeIT`, `GameFlowIT`, `WrapUpGuiVerificationIT`,
  `WrapUpDataInjectionIT` are standalone classes, each with its own Playwright `@BeforeAll`/`@AfterAll`
  lifecycle, navigating to `/`, `/maestro`, `/big-screen` directly via `page.navigate(...)`. There is no
  existing seam to hook a login step into — one must be added per test.
- **Secrets convention**: plain OS env vars via Spring relaxed binding, e.g.
  `spring.datasource.url=${db_url}` in `application.properties`, set directly in Railway's environment
  panel — no Dockerfile/railway.json secret wiring. New SMTP secrets follow the same pattern.
- **CI runs a Trivy dependency vulnerability scan** (`.github/workflows/maven-it.yml`, per AGENTS.md)
  after `spotless:check` — dependency version choices in Phase 2 must keep this green.

## Desired End State

A maestro can open `/login`, submit their email, receive a magic-link email (or see it logged locally in
dev when SMTP isn't configured), click it, and land authenticated on `/maestro` with a session that
persists for 30 days. An unauthenticated visitor hitting any maestro-control route is redirected to
`/login`. The maestro can log out. Every big-screen, player, and join route remains reachable with zero
login friction, exactly as today. Playwright IT tests can authenticate a maestro session without sending
real email.

**Verification:** `./mvnw verify -Pit -Pproduction` passes; manually requesting a link locally (dev mail
fallback) and clicking it lands on `/maestro`; a second, unrelated browser session hitting `/maestro`
directly is redirected to `/login`; big-screen/player QR-join flow is unchanged.

### Key Discoveries

- Vaadin 25 **removed** `VaadinWebSecurity` (deprecated in Vaadin 24) in favor of
  `com.vaadin.flow.spring.security.VaadinSecurityConfigurer`, used as an `HttpSecurity` configurer via
  `http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class))`
  inside a `SecurityFilterChain` `@Bean`. This supersedes the `extends VaadinWebSecurity` pattern this
  project's own `stack-assessment.md` recommends — that recommendation is now stale for this Vaadin
  version.
- Spring Boot 4.0.1 manages **Spring Security 7.0.2**, which is inside the vulnerable range for
  **CVE-2026-22751** (TOCTOU race in `JdbcOneTimeTokenService`, fixed in 7.0.5). This project doesn't use
  `JdbcOneTimeTokenService` (see Phase 2), but the managed version must still be pinned to `>=7.0.5` or
  the CI Trivy scan will flag it.
- Spring Security 7's built-in One-Time Token Login (`http.oneTimeTokenLogin(...)`) does not send email
  itself — the app implements `OneTimeTokenGenerationSuccessHandler` to build the link and dispatch it.
  `JdbcOneTimeTokenService` is `final` and cannot be extended, so a JPA-backed token store means
  implementing the two-method `OneTimeTokenService` interface (`generate`/`consume`) from scratch.
- Vaadin's component event model (Button clicks, Binder-bound fields) communicates over Vaadin's own
  server-push RPC channel, not a real browser form POST. Spring Security's `/ott/generate` endpoint
  requires a genuine full-page `<form method="post">` submission so the browser can follow the resulting
  redirect. `LoginView`'s email-submission control must therefore be raw HTML (`Html` component / direct
  DOM elements), not a Vaadin `TextField`/`Button`/`Binder` — everything else about the view can be normal
  Vaadin.

## What We're NOT Doing

- No tenant scoping of games/set-lists to the maestro account (S-02).
- No "remember me" beyond the chosen 30-day session; no explicit password/local-credential path.
- No display name or profile fields on the maestro account beyond email.
- No admin/multi-role hierarchy — a single `MAESTRO` role.
- No performer view (FR-011, parked).
- No in-app email-template system — a minimal plain-text/HTML email with the link.
- No multi-instance-safe rate limiting — the per-email send cooldown is in-memory, consistent with this
  app's existing single-instance deploy story (`GameServiceImpl` is already a global singleton).

## Implementation Approach

Layer the auth mechanism in independently-testable phases, cheapest/lowest-risk first: get the data model
and email delivery working without any security wiring (Phase 1), then wire Spring Security's One-Time
Token Login end-to-end (Phase 2), then gate the existing routes and add logout (Phase 3), then close the
IT-test gap the new gating opens (Phase 4). This ordering means Phase 3's route annotations are added only
once real auth exists to redirect to, and Phase 4's test seam is added only once there's something to
bypass.

## Critical Implementation Details

**CSRF & the plain-HTML-form seam.** Spring Security enables CSRF protection by default for state-changing
requests. The raw HTML forms this plan introduces (the magic-link request form in `LoginView`, and the
logout control in Phase 3) are real browser POSTs outside Vaadin's own CSRF-exempted internal traffic, so
each must embed a valid CSRF token as a hidden field. Verify the exact token-attribute/access mechanism
against current Spring Security 7 docs when implementing Phase 2 — don't assume an older Spring Security
CSRF API.

**State sequencing — account creation happens at consume-time, not generate-time.** Revised during code
review of Phase 2: `MaestroDto` is created in `OneTimeTokenService.consume(...)`, right after the token is
confirmed valid (found, unexpired) — not in `generate(...)`. This means an unverified email (someone just
typing an address into the request form) never creates a `Maestro` row by itself; a row only appears once
someone actually proves control of that inbox by clicking the link. `generate(...)` only ever creates a
token. The `UserDetailsService` used at token-consumption time still only *finds* the account, never
creates it — by the time `loadUserByUsername(...)` runs (immediately after `consume()` in
`OneTimeTokenAuthenticationProvider.authenticate(...)`), `consume()` has already guaranteed the account
exists. Do not duplicate account-creation logic in the `UserDetailsService`.

**Push/redirect gotcha.** This app runs `@Push`. There's a known Vaadin/Spring Security interaction where
the default saved-request redirect-after-login can resolve to the wrong URL (e.g. a push/heartbeat
endpoint) when push is active. Register a custom `AuthenticationSuccessHandler` that unconditionally
redirects to `/maestro` rather than trusting Spring's saved-request default.

## Phase 1: Maestro Account Persistence & Email Delivery Scaffold

### Overview

Lay the data model and outbound-email groundwork with no security wiring yet, so it's testable in
isolation before the harder auth-mechanism phase.

### Changes Required:

#### 1. Maestro domain type and store

**File**: `src/main/java/com/github/mjjaniec/lmq/model/Maestro.java`

**Intent**: A minimal immutable domain record identifying a maestro account by a stable surrogate id (not
the email itself — per PRD Access Control Changes: "tied to a stable external identity ... not to an email
address alone"), mirroring the existing `Player`-style domain records in `model`.

**Contract**: `record Maestro(UUID id, String email) {}`.

**File**: `src/main/java/com/github/mjjaniec/lmq/stores/MaestroStore.java`,
`src/main/java/com/github/mjjaniec/lmq/stores/JpaMaestroStore.java`,
`src/main/java/com/github/mjjaniec/lmq/stores/MaestroDto.java`

**Intent**: Follow the exact `PlayerStore`/`JpaPlayerStore`/`PlayerDto` triad: a plain interface exposing
domain-typed lookups, a `CrudRepository`-extending interface with default-method dto↔domain mapping, and a
`@Data @Entity` Dto with a generated UUID as `@Id` and email as a unique attribute. The surrogate id (rather
than email) is the account's stable identity, so a future alternate login provider can attach to the same
`Maestro` without a schema migration.

**Contract**: `MaestroStore` exposes `Optional<Maestro> findByEmail(String email)` and
`Maestro createIfAbsent(String email)` (idempotent — returns the existing record if present, generating a
new `UUID` id only on first creation). No password or profile fields on `MaestroDto` beyond
`@Id private UUID id` and `@Column(unique = true) private String email`.

#### 2. Email delivery scaffold

**File**: `pom.xml`

**Intent**: Add `spring-boot-starter-mail` for outbound SMTP.

**Contract**: Dependency addition only in this phase; no security dependency yet (that's Phase 2).

**File**: `src/main/resources/application.properties`, `src/main/resources/application-local.properties.example`

**Intent**: Wire SMTP config through the same `${var}` env-var convention as `db_url`/`db_user`/`db_pass`.

**Contract**: `spring.mail.host=${smtp_host:}`, `spring.mail.port=${smtp_port:587}`,
`spring.mail.username=${smtp_user:}`, `spring.mail.password=${smtp_pass:}`,
`spring.mail.properties.mail.smtp.auth=true`, `spring.mail.properties.mail.smtp.starttls.enable=true`.
Empty-string defaults (`:` with nothing after) let the property resolve to blank when the env var is
unset, which the mail-sending component (Phase 2) uses to detect "SMTP not configured" and fall back to
logging the link instead of sending — this keeps local dev working without real SMTP credentials.

**Provider decision: Gmail SMTP.** `smtp_host=smtp.gmail.com`, `smtp_port=587`, `smtp_user=<dedicated
Gmail address>`, `smtp_pass=<16-character App Password>`. Chosen over a dedicated transactional-mail
service (e.g. Brevo) because it's a provider the team already knows, and the app's volume (a handful of
maestros, occasional login) is nowhere near Gmail's 500/day SMTP send limit.

**Human setup gate (blocks Phase 2 manual verification with real SMTP, not required for Phase 1's own
automated/manual checks — Phase 1 passes fine with SMTP unconfigured):** before Phase 2 can be manually
verified end-to-end with real email delivery, a human must:
1. Create a new Google account **dedicated to this app** — not a personal/daily-use inbox — so a leaked
   credential exposes an empty mailbox, not real correspondence.
2. Enable 2-Step Verification on that account (required before an App Password can be generated).
3. Generate an **App Password** for it at `myaccount.google.com/apppasswords` — this is the value that
   becomes `smtp_pass`; it is not the account's login password.
4. **Disable IMAP and POP access** on that account (Gmail Settings → Forwarding and POP/IMAP). This does
   not affect SMTP sending, but narrows a leaked App Password's blast radius from "read/delete/send on
   this mailbox" down to "send only" — the credential can no longer be used to read anything back, since
   the protocol that would allow it is turned off account-wide.
5. Set `smtp_user`/`smtp_pass` (and `smtp_host`/`smtp_port` if not already defaulted) in Railway's
   Variables panel per the existing `${var}` secrets convention — never commit them to
   `application-local.properties` (gitignored) beyond the `.example` template's blank placeholders.

### Success Criteria:

#### Automated Verification:

- Unit test for `MaestroStore`/`JpaMaestroStore` (create-if-absent idempotency, find-by-email) passes:
  `./mvnw test -Dtest=MaestroStoreTest`
- Formatting passes: `./mvnw spotless:check`
- Full unit test suite passes: `./mvnw test`

#### Manual Verification:

- App still starts locally with `./mvnw spring-boot:run` (new dependency doesn't break startup)

**Implementation Note**: After completing this phase and all automated verification passes, pause here
for manual confirmation before proceeding to Phase 2.

---

## Phase 2: Spring Security One-Time-Token Login Wiring

### Overview

Wire Spring Security's built-in One-Time Token Login feature end-to-end: a JPA-backed token store, the
security filter chain, the email-sending success handler with a send-cooldown, and the `LoginView` request
form.

**Route gating folded in from the original Phase 3 plan.** Discovered during implementation: Vaadin's
`AccessAnnotationChecker` denies access by default to any route lacking a security annotation
(`@AnonymousAllowed`/`@RolesAllowed`/etc.), and enabling `VaadinSecurityConfigurer` (this phase's core task)
also defaults its Spring-Security-level HTTP catch-all rule to `denyAll()`. This means wiring in security at
all — regardless of phase boundaries — immediately denies every existing route the moment this phase lands,
not just the maestro-control routes FR-003 targets. Deferring the `@AnonymousAllowed`/`@RolesAllowed`
annotations to a later phase (as originally planned) would leave the app completely unusable (big-screen,
player join, everything) between this phase and the next. So this phase now also does what was
originally Phase 3 §1 (maestro route gating) and §2 (explicit anonymous surfaces) — every phase boundary
stays a fully working app for real users. Phase 3 is now just logout.

**Known, accepted consequence: the Playwright IT suite goes red until Phase 4.** `SmokeIT`, `GameFlowIT`,
and `WrapUpGuiVerificationIT` all navigate straight to `/maestro*` with no session — once routes are gated,
that's exactly the gap Phase 4's `TestAuthController` backdoor exists to close. Real users aren't affected
(the actual login flow works end-to-end); only headless test runs are, until Phase 4 lands. Do not push/merge
this branch in that interim state. This phase's own success criteria do NOT include "IT suite passes" for
that reason — verified by running the suite: `SmokeIT.maestroPageLoads` fails and all `GameFlowIT`/
`WrapUpGuiVerificationIT` tests time out waiting on `/maestro*`, while `playerPageLoads`/`bigscreenPageLoads`
(anonymous surfaces) still pass.

### Changes Required:

#### 1. Dependency and version pin

**File**: `pom.xml`

**Intent**: Add `spring-boot-starter-security`; pin the managed Spring Security version to avoid
CVE-2026-22751 even though `JdbcOneTimeTokenService` (the affected class) is unused, so the Trivy CI scan
stays green.

**Contract**: Add `<spring-security.version>` (or equivalent Boot BOM override property) pinned to the
latest `7.0.x` patch `>=7.0.5` available at implementation time — verify the current patch release rather
than hardcoding `7.0.5` from this plan.

#### 2. Magic-link token store and `OneTimeTokenService`

**File**: `src/main/java/com/github/mjjaniec/lmq/stores/MagicLinkTokenDto.java`,
`src/main/java/com/github/mjjaniec/lmq/stores/JpaMagicLinkTokenStore.java`

**Intent**: A `@Data @Entity` row per outstanding token (token value as `@Id`, owning email, expiry
instant) and a `CrudRepository<MagicLinkTokenDto, String>` for it — no domain-mapping default methods
needed here since the token itself isn't a domain-facing concept. One extra repository method beyond plain
CRUD: a `@Lock(LockModeType.PESSIMISTIC_WRITE)`-annotated `findByToken(String)`, added during code review
to close a replay race (see `consume(...)` below) — Spring Data JPA supports adding `@Lock` to a derived
query method directly, no custom `@Query` needed.

**Contract**: `MagicLinkTokenDto { @Id String token; String email; Instant expiresAt; }`.
`JpaMagicLinkTokenStore.findByToken(String token) -> Optional<MagicLinkTokenDto>`, holding a
`PESSIMISTIC_WRITE` row lock for the caller's transaction.

**File**: `src/main/java/com/github/mjjaniec/lmq/services/MagicLinkOneTimeTokenService.java`

**Intent**: A hand-written `@Component implements OneTimeTokenService`, following the `RealStageStore`
precedent of wrapping a `CrudRepository` with real logic rather than using default methods.

`generate(...)` first normalizes the requested email (lowercase; strip dots from the local part before
`@`, matching Gmail's dot-insignificance convention — not universally correct for every provider, but an
accepted tradeoff at this app's expected scale, decided during code review) then checks two in-memory
cooldowns keyed by (a) the normalized email and (b) the requesting client IP — both a 60-second
`ConcurrentHashMap<String, Instant>` last-request-time map, same mechanism, different key. The IP is
obtained via `((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
.getRequest().getRemoteAddr()` — safe to call here since `generate()` runs synchronously on the same
request thread as the originating `/ott/generate` POST, so no method-signature change is needed to thread
the request through. If either cooldown is active, `generate()` returns a token result as if it succeeded
(same "check your email" UX, no enumeration signal) but skips the token dispatch — this is what actually
blocks abuse: a flooding IP submitting many distinct fake emails is stopped before any token row is created
or any email is sent, not just before the send. Absent both cooldowns, `generate()` persists a token with a
30-minute expiry — it does **not** touch `MaestroStore` at all (see State sequencing, above: account
creation happens at consume-time, not here).

`consume(...)` extracts the token value from the given `OneTimeTokenAuthenticationToken`, then — inside a
`@Transactional` method — calls `findByToken(...)` (the pessimistic-write-locked lookup above) rather than
plain `findById`. This closes a real race found during code review: two concurrent consumptions of the same
token both calling a plain `findById` before either commits `deleteById` could otherwise both authenticate
successfully, defeating single-use — the same TOCTOU class that CVE-2026-22751 fixed in Spring's own
`JdbcOneTimeTokenService`. With the lock, the second concurrent call blocks until the first transaction
commits (row now deleted), then correctly finds nothing. Once the row is found: delete it unconditionally
(enforcing single-use whether or not it was still valid), return `null` if expired, otherwise call
`MaestroStore.createIfAbsent(...)` for the token's email (the account is created here, now that the token is
confirmed valid) and return the successful token.

**Contract**: Implements `org.springframework.security.authentication.ott.OneTimeTokenService`
(verified against the Spring Security 7.0.5 source): `generate(GenerateOneTimeTokenRequest) -> OneTimeToken`,
`consume(OneTimeTokenAuthenticationToken authenticationToken) -> @Nullable OneTimeToken` — note `consume`
takes the framework's authentication token object (not a raw token string) and returns a nullable
`OneTimeToken` (not `Authentication`); the token value is obtained via the argument.

**File**: `src/main/java/com/github/mjjaniec/lmq/stores/JpaMaestroStore.java`

**Intent**: Revised during code review to close a second race: the original find-then-save
`createIfAbsent` could throw an uncaught constraint violation if two concurrent requests created the same
new email at once. Extends `JpaRepository` (not plain `CrudRepository`) so `saveAndFlush(...)` is
available — forcing the insert (and any constraint violation) to happen synchronously inside
`createIfAbsent`, catchable in the same method, rather than deferred to whenever Hibernate would otherwise
flush. On `DataIntegrityViolationException`, re-reads the row the other concurrent request just committed
instead of propagating the exception.

**Contract**: `createIfAbsent(String email)` — on the losing side of a create race, returns the winning
side's row rather than throwing.

**File**: `src/main/resources/application.properties`

**Intent**: The app runs behind Railway's edge proxy (per `infrastructure.md`), so `request.getRemoteAddr()`
returns the proxy's IP, not the real client's, unless Spring is told to parse `X-Forwarded-For`. Required
for the per-IP cooldown above to key on the actual requester rather than the same proxy IP for every
request. Trusting the header is safe here because Railway's proxy is the sole path into the container — a
client can't reach the app directly to forge it.

**Contract**: Add `server.forward-headers-strategy=framework`.

#### 3. `UserDetailsService`

**File**: `src/main/java/com/github/mjjaniec/lmq/services/MaestroUserDetailsService.java`

**Intent**: Load (not create — see Critical Implementation Details) a `MaestroDto` by email and return a
`UserDetails` with `ROLE_MAESTRO`.

**Contract**: `implements UserDetailsService`; `loadUserByUsername(email)` throws
`UsernameNotFoundException` if `MaestroStore.findByEmail(email)` is absent (should not happen in practice
since `generate()` always creates first).

#### 4. Security filter chain

**File**: `src/main/java/com/github/mjjaniec/lmq/config/SecurityConfig.java`

**Intent**: Compose `VaadinSecurityConfigurer` (Vaadin 25's replacement for the removed
`VaadinWebSecurity`) with `oneTimeTokenLogin(...)`, the custom `OneTimeTokenService`, a custom
`OneTimeTokenGenerationSuccessHandler` (build the link, apply the per-email send cooldown, dispatch via
`JavaMailSender` or the dev-log fallback from Phase 1), and a custom `AuthenticationSuccessHandler` that
unconditionally redirects to `/maestro` (see Critical Implementation Details — push/redirect gotcha).

**Contract**:
```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class))
        .oneTimeTokenLogin(ott -> ott
            .loginProcessingUrl("/login/ott")
            .loginPage("/login")
            .tokenService(magicLinkOneTimeTokenService)
            .tokenGenerationSuccessHandler(magicLinkEmailSuccessHandler)
            .successHandler(redirectToMaestroSuccessHandler))
        .build();
}
```
Discovered during implementation, two chained gotchas — both confirmed against the real
`AbstractAuthenticationFilterConfigurer`/`OneTimeTokenLoginConfigurer` 7.0.6 source, not guessed:

1. `VaadinSecurityConfigurer.loginView(...)` only marks the login page as custom for its own
   `FormLoginConfigurer`. `OneTimeTokenLoginConfigurer` tracks "is this a custom login page" independently —
   without also calling `.loginPage("/login")` on the `oneTimeTokenLogin(...)` DSL itself, Spring's shared
   `DefaultLoginPageGeneratingFilter` stays active and serves its own generic OTT request page at `/login`,
   shadowing `LoginView` entirely (confirmed live: GET `/login` returned Spring's built-in "Request a
   One-Time Token" form, not the app's Vaadin view).
2. Calling `.loginPage("/login")` has a side effect: `AbstractAuthenticationFilterConfigurer.loginPage(...)`
   eagerly runs `updateAuthenticationDefaults()`, which — because this DSL customizer lambda runs *before*
   `OneTimeTokenLoginConfigurer`'s own `init()` has had a chance to default `loginProcessingUrl` to
   `/login/ott` — sees `loginProcessingUrl == null` at that moment and sets it to `this.loginPage` ("/login")
   instead. The confirm-page's form then posts to `/login`, which is silently caught by the *other*
   `FormLoginConfigurer`'s plain `UsernamePasswordAuthenticationFilter` (no username/password present →
   `BadCredentialsException` → redirect to `/login?error`), and `MagicLinkOneTimeTokenService.consume(...)` is
   never even called. Confirmed live via Playwright + a `consume()`-side debug log: authentication failed
   with zero server-side exceptions, and the POST's request URL logged as `/login`, not `/login/ott`.
   Explicitly calling `.loginProcessingUrl("/login/ott")` (either before or after `.loginPage(...)`) fixes it
   by giving that field a non-null value before the eager side effect can clobber it.

Exact method names/overloads must be confirmed against Spring Security 7.0.x docs at implementation time.
Note: `OneTimeTokenLoginConfigurer.authenticationSuccessHandler(...)` still works in 7.0.5 but is deprecated
since 6.5 in favor of the shared `AbstractAuthenticationFilterConfigurer.successHandler(...)` shown above —
prefer `successHandler(...)` unless implementation-time docs say otherwise.

#### 5. Magic-link email content

**File**: `src/main/java/com/github/mjjaniec/lmq/services/MagicLinkOneTimeTokenService.java` (or a small
helper it calls) — the `OneTimeTokenGenerationSuccessHandler`'s email-building step from Changes Required
#4.

**Intent**: A minimal plain-text/HTML email, in Polish (matching this app's existing UI-text convention),
with exactly one job: get the maestro to click the link. No branding/template system per "What We're NOT
Doing."

**Contract**: The email includes:
- A short subject and body naming the app, in Polish
- One clear link: rendered as a clickable button in the HTML part, with the raw URL repeated as
  plain-text for clients that don't render HTML
- An explicit expiry note ("Link jest ważny przez 30 minut")
- A single-use note (the link stops working once clicked)
- A "jeśli to nie Ty, zignoruj tę wiadomość" (if this wasn't you, ignore it) line — standard hygiene;
  no action is needed from a non-requester since nothing happens until the link is clicked
- Nothing else — no name/profile fields exist on `Maestro` to personalize with beyond the email address
  itself

#### 6. Login view

**File**: `src/main/java/com/github/mjjaniec/lmq/views/maestro/LoginView.java`

**Intent**: A `@Route("login") @AnonymousAllowed` Vaadin view whose email-submission control is a raw
HTML form posting to `/ott/generate` (see Key Discoveries — Vaadin's RPC model can't drive a real form
POST), with a normal Vaadin-rendered "check your email" confirmation state around it.

**Contract**: Form field name matches whatever `GenerateOneTimeTokenRequestResolver` expects by default
(confirm exact parameter name, e.g. `username`, against docs at implementation time); includes the CSRF
hidden field per Critical Implementation Details.

#### 7. Maestro route gating

**File**: `src/main/java/com/github/mjjaniec/lmq/views/maestro/MaestroView.java`,
`StartGameView.java`, `DjView.java`, `FeedbackView.java`

**Intent**: Annotate each with `@RolesAllowed("MAESTRO")` individually — Vaadin route security is
per-view, not inherited from a layout, so `MaestroView`'s `onAttach` chokepoint does not implicitly cover
its children, and `FeedbackView`'s literal-path bypass (see Current State Analysis) needs the same
annotation as the properly-nested views. Folded in from the original Phase 3 plan — see the Overview note
on why this can't wait.

**Contract**: `@RolesAllowed("MAESTRO")` on all four classes.

#### 8. Explicit anonymous surfaces

**File**: every `@Route`-annotated class under `views/bigscreen` (`BigScreenView` and its 8 nested views)
and every `@Route`-annotated class under `views/player` (`RootView`, `PlayerView`, `JoinView`, and
`PlayerView`'s 9 nested views)

**Intent**: Once Spring Security is active, unannotated routes default to requiring authentication —
every one of these must be explicitly `@AnonymousAllowed` to preserve today's zero-friction anonymous
access, including the two literal-path routes (`JoinView` at `"player/join"`, and the player package's own
`FeedbackView` if it doesn't already sit under `PlayerView`'s layout). Folded in from the original Phase 3
plan — see the Overview note on why this can't wait.

**Contract**: `@AnonymousAllowed` on every class in both packages; verify none was missed by re-running
the full route inventory from Current State Analysis before closing this phase.

### Success Criteria:

#### Automated Verification:

- Unit tests for `MagicLinkOneTimeTokenService` (generate creates account + token; consume succeeds once
  and fails on replay; expired token rejected; per-email cooldown skips creation/dispatch for a repeat
  email within 60s; per-IP cooldown skips creation/dispatch for a second distinct email from the same IP
  within 60s) pass: `./mvnw test -Dtest=MagicLinkOneTimeTokenServiceTest`
- Formatting passes: `./mvnw spotless:check`
- Full unit test suite passes: `./mvnw test`

#### Manual Verification:

- With SMTP unconfigured locally, requesting a link at `/login` logs the link instead of failing
- Clicking the logged link authenticates the session and lands on `/maestro`
- Re-visiting the same link a second time fails (single-use enforced)
- Waiting past 30 minutes then visiting the link fails (expiry enforced)
- Requesting a second link for the same email within 60 seconds does not send a second email
  (cooldown), while the UI still shows the same "check your email" confirmation either way
- Requesting links for two different emails from the same browser/IP within 60 seconds only creates an
  account/sends for the first; the second is silently no-op'd (same "check your email" confirmation,
  no `Maestro` row or token created for the second email)
- With the dedicated Gmail account's `smtp_user`/`smtp_pass` set (see Human setup gate, Phase 1 §2), a
  real request delivers an actual email to the target inbox — not just the dev-log fallback — with the
  content specified in Changes Required #5
- Unauthenticated visit to `/maestro`, `/maestro/start`, `/maestro/dj`, and `/maestro/feedback` each
  redirect to `/login`
- Authenticated maestro can reach all four maestro routes normally
- Big-screen QR display, player join (`/player/join`), and the full anonymous player flow work with zero
  added steps or visible change

**Implementation Note**: After completing this phase and all automated verification passes, pause here
for manual confirmation before proceeding to Phase 3.

---

## Phase 3: Logout

### Overview

Add a logout affordance so the maestro can end their session. This is the last piece of FR-002; route
gating (originally planned for this phase) already landed in Phase 2 — see that phase's Overview note.

### Changes Required:

#### 1. Logout

**File**: `src/main/java/com/github/mjjaniec/lmq/views/maestro/MaestroView.java` (or a shared toolbar
component under `components/`, if one already wraps the maestro layout's header)

**Intent**: A visible logout control the maestro can use to end their session. Confirm at implementation
time whether Spring Security 7's default logout endpoint accepts the same GET-link pattern this app
otherwise avoids, or requires the same plain-HTML-form-POST treatment as the login request (see Critical
Implementation Details — CSRF).

**Contract**: A control that triggers Spring Security's logout endpoint and results in the maestro landing
back on `/login`, unauthenticated.

### Success Criteria:

#### Automated Verification:

- Formatting passes: `./mvnw spotless:check`
- Full unit test suite passes: `./mvnw test`

Note: the Playwright IT suite is still expected to fail at this point (per Phase 2's Overview note) —
it isn't fixed until Phase 4's backdoor lands. Do not push/merge until then.

#### Manual Verification:

- Logout ends the session and a subsequent visit to `/maestro` redirects to `/login` again

**Implementation Note**: After completing this phase and all automated verification passes, pause here
for manual confirmation before proceeding to Phase 4.

---

## Phase 4: Integration Test Login Seam

### Overview

Close the gap Phase 2 opens for the Playwright/TestBench IT suite: with routes gated, every existing IT
test that navigates to `/maestro` needs an authenticated session first, and there's no way to click a real
emailed link in a headless browser run.

### Changes Required:

#### 1. Test-only login backdoor

**File**: `src/main/java/com/github/mjjaniec/lmq/api/TestAuthController.java`

**Intent**: A `@Profile("integration-test")`-only REST endpoint (mirroring the existing `HintController`
precedent of a plain REST controller alongside the Vaadin views) that authenticates a given email directly
into the current session, bypassing real email entirely. Hard-fenced to the `integration-test` Spring
profile so it can never exist in a production build.

**Contract**: `@Profile("integration-test") @RestController` with an endpoint (e.g.
`POST /test/login?email=...`) that programmatically establishes a `SecurityContext` for that email in the
current `HttpSession`, creating the `Maestro` account via `MaestroStore.createIfAbsent(...)` if absent.

#### 2. Wire the backdoor into existing IT tests

**File**: `src/test/java/.../SmokeIT.java`, `GameFlowIT.java`, `WrapUpGuiVerificationIT.java`

**Intent**: Before each test's maestro `Page` navigates to a maestro route, call the backdoor endpoint
within that same `BrowserContext` (so the resulting session cookie is attached), e.g.
`maestroPage.navigate(BASE_URL + "/test/login?email=maestro@test.local")` immediately before the existing
`maestroPage.navigate(BASE_URL + "/maestro"...)` call. In practice this is exactly 3 call sites: `SmokeIT`'s
direct navigation to `/maestro/start`, `GameFlowIT`'s shared `ensureGameNotStarted(...)` helper (which every
test method routes through, so it's a single edit there, not one per test method), and
`WrapUpGuiVerificationIT`'s direct navigation to `/maestro/dj`. `WrapUpDataInjectionIT` is a pure
`@SpringBootTest` that injects data directly into stores/services — it never opens a Playwright browser or
navigates any route, so it needs no change.

**Contract**: No change to big-screen/player page navigation in these tests — only the maestro-surface
navigation gains a preceding backdoor-login call.

### Success Criteria:

#### Automated Verification:

- Full CI-equivalent suite passes: `./mvnw verify -Pit -Pproduction`
- Formatting passes: `./mvnw spotless:check`

#### Manual Verification:

- Confirm (by code inspection or a deliberate local attempt) that `/test/login` returns 404/is unreachable
  when the app runs under the `local` or default profile, not just `integration-test`

**Implementation Note**: After completing this phase and all automated verification passes, this plan is
complete — no further phase follows.

---

## Testing Strategy

### Unit Tests:

- `MaestroStore`/`JpaMaestroStore`: create-if-absent idempotency, find-by-email
- `MagicLinkOneTimeTokenService`: generate creates account + token with correct expiry; consume succeeds
  once; consume fails on replay; consume fails past expiry
- Per-email and per-IP cooldown logic in `MagicLinkOneTimeTokenService.generate(...)`: a second request
  within 60s for the same email, or for a different email from the same IP, skips account/token creation
  and dispatch (verified via a fake `HttpServletRequest` on `RequestContextHolder`)

### Integration Tests:

- Full existing Playwright suite (`SmokeIT`, `GameFlowIT`, `WrapUpGuiVerificationIT`) continues to pass
  with the Phase 4 backdoor wired in; `WrapUpDataInjectionIT` is unaffected (no browser navigation, no
  route gating to bypass)
- (Optional, if time allows) a new small IT asserting the redirect-to-login behavior for an unauthenticated
  `/maestro` hit

### Manual Testing Steps:

1. Start locally without SMTP env vars set; request a link at `/login`; confirm the link is logged, not
   silently dropped
2. Click the logged link; confirm landing on `/maestro` authenticated
3. Restart the browser (new session/cookie jar) and hit `/maestro` directly; confirm redirect to `/login`
4. Log out; confirm `/maestro` redirects to `/login` again afterward
5. Run the full big-screen QR + player-join + live-round flow end-to-end; confirm no behavior change and
   no perceptible added lag (PRD guardrail)

## Performance Considerations

None beyond the PRD guardrail that authentication must not add perceptible lag to the live real-time
sync — Spring Security's per-request filter chain overhead on Vaadin's push/heartbeat traffic should be
checked informally during Phase 3/4 manual verification, but no specific budget is set (small user/data
scale per PRD `target_scale`).

## Migration Notes

None. Per PRD Constraints & Compatibility, no data migration/backfill is needed — existing global,
unowned data may simply be reset. This change adds new tables (`maestro`, `magic_link_token`) via
Hibernate's `ddl-auto=update`; no existing table is altered.

## References

- PRD: `context/foundation/prd.md` — FR-001, FR-002, FR-003, US-01, Access Control Changes
- Roadmap: `context/foundation/roadmap.md` — slice S-01 (`maestro-account-auth`)
- Stack assessment: `context/foundation/stack-assessment.md` — flags Vaadin route security as the
  agent's thinnest Vaadin training-data surface (superseded in part by the `VaadinSecurityConfigurer`
  discovery above)
- Store pattern precedent: `stores/PlayerStore.java`, `stores/JpaPlayerStore.java`, `stores/PlayerDto.java`
- Hand-written-store-over-CrudRepository precedent: `stores/RealStageStore.java`
- Existing REST controller precedent: `api/HintController.java`
- Vaadin security docs: https://vaadin.com/docs/latest/flow/security/vaadin-security-configurer
- Spring Security One-Time Token Login docs:
  https://docs.spring.io/spring-security/reference/servlet/authentication/onetimetoken.html

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename
> step titles. See `references/progress-format.md`.

### Phase 1: Maestro Account Persistence & Email Delivery Scaffold

#### Automated

- [x] 1.1 Unit test for MaestroStore/JpaMaestroStore passes — 2828d8c
- [x] 1.2 Formatting passes (spotless:check) — 2828d8c
- [x] 1.3 Full unit test suite passes — 2828d8c

#### Manual

- [x] 1.4 App still starts locally with the new dependency — 2828d8c
- [x] 1.5 Human setup gate complete: dedicated Gmail account created, 2-Step Verification enabled, App
      Password generated, IMAP/POP disabled, `smtp_user`/`smtp_pass` set in Railway Variables — 2828d8c

### Phase 2: Spring Security One-Time-Token Login Wiring

#### Automated

- [x] 2.1 Unit tests for MagicLinkOneTimeTokenService pass — 6f5f88e
- [x] 2.2 Formatting passes (spotless:check) — 6f5f88e
- [x] 2.3 Full unit test suite passes — 6f5f88e

#### Manual

- [x] 2.4 Requesting a link with SMTP unconfigured logs the link instead of failing — 6f5f88e
- [x] 2.5 Clicking the logged link authenticates and lands on /maestro — 6f5f88e
- [x] 2.6 Re-visiting the same link a second time fails (single-use) — 6f5f88e
- [x] 2.7 Visiting an expired (30+ min) link fails — 6f5f88e
- [x] 2.8 Requesting a second link within 60s does not send a second email — 6f5f88e
- [x] 2.9 Requesting links for two different emails from the same IP within 60s only creates/sends for
      the first — 6f5f88e
- [x] 2.10 With real Gmail SMTP configured, a request delivers an actual email to the target inbox — 6f5f88e
- [x] 2.11 Unauthenticated visits to all four maestro routes redirect to /login — 6f5f88e
- [x] 2.12 Authenticated maestro can reach all four maestro routes normally — 6f5f88e
- [x] 2.13 Big-screen/player/join flow works with zero added friction — 6f5f88e

### Phase 3: Logout

#### Automated

- [ ] 3.1 Formatting passes (spotless:check)
- [ ] 3.2 Full unit test suite passes

#### Manual

- [ ] 3.3 Logout ends the session and re-gates /maestro

### Phase 4: Integration Test Login Seam

#### Automated

- [ ] 4.1 Full CI-equivalent suite passes (verify -Pit -Pproduction)
- [ ] 4.2 Formatting passes (spotless:check)

#### Manual

- [ ] 4.3 /test/login is unreachable outside the integration-test profile
