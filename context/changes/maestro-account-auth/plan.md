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

**State sequencing — account creation must precede authentication lookup.** The custom
`OneTimeTokenService.generate(...)` (Phase 2) is where an unknown email causes `MaestroDto` to be created
(auto-register-on-request). The `UserDetailsService` used at token-consumption time only needs to *find*
the account, not create it — by the time a token is consumed, `generate()` has already guaranteed the
account exists. Do not duplicate account-creation logic in the `UserDetailsService`.

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
form. After this phase, a maestro can request a link and authenticate — but no existing route is gated
yet (that's Phase 3), so this phase is testable purely as "can I log in," independent of route access.

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
instant) and a plain `CrudRepository<MagicLinkTokenDto, String>` for it — no domain-mapping default
methods needed here since the token itself isn't a domain-facing concept.

**Contract**: `MagicLinkTokenDto { @Id String token; String email; Instant expiresAt; }`.

**File**: `src/main/java/com/github/mjjaniec/lmq/services/MagicLinkOneTimeTokenService.java`

**Intent**: A hand-written `@Component implements OneTimeTokenService`, following the `RealStageStore`
precedent of wrapping a `CrudRepository` with real logic rather than using default methods. `generate(...)`
first checks two in-memory cooldowns keyed by (a) the requested email and (b) the requesting client IP —
both a 60-second `ConcurrentHashMap<String, Instant>` last-request-time map, same mechanism, different key.
The IP is obtained via `((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
.getRequest().getRemoteAddr()` — safe to call here since `generate()` runs synchronously on the same
request thread as the originating `/ott/generate` POST, so no method-signature change is needed to thread
the request through. If either cooldown is active, `generate()` returns a token result as if it succeeded
(same "check your email" UX, no enumeration signal) but skips both `MaestroStore.createIfAbsent(...)` and
the token/email dispatch — this is what actually blocks abuse: a flooding IP submitting many distinct fake
emails is stopped before any `Maestro`/token row is created or any email is sent, not just before the send
(enforcing the cooldown only in the success handler, as the per-email cooldown alone would, still lets an
attacker grow the `maestro`/`magic_link_token` tables unbounded). Absent both cooldowns, `generate()`
resolves or auto-creates the `Maestro` via `MaestroStore.createIfAbsent(...)` and persists a token with a
30-minute expiry. `consume(...)` extracts the token value from the given `OneTimeTokenAuthenticationToken`,
looks up the token, deletes the row unconditionally (enforcing single-use whether or not it was still
valid), and returns `null` when expired or absent.

**Contract**: Implements `org.springframework.security.authentication.ott.OneTimeTokenService`
(verified against the Spring Security 7.0.5 source): `generate(GenerateOneTimeTokenRequest) -> OneTimeToken`,
`consume(OneTimeTokenAuthenticationToken authenticationToken) -> @Nullable OneTimeToken` — note `consume`
takes the framework's authentication token object (not a raw token string) and returns a nullable
`OneTimeToken` (not `Authentication`); the token value is obtained via the argument.

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
            .tokenService(magicLinkOneTimeTokenService)
            .tokenGenerationSuccessHandler(magicLinkEmailSuccessHandler)
            .successHandler(redirectToMaestroSuccessHandler))
        .build();
}
```
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

**Implementation Note**: After completing this phase and all automated verification passes, pause here
for manual confirmation before proceeding to Phase 3.

---

## Phase 3: Route Gating & Logout

### Overview

Gate every maestro-control view behind the `MAESTRO` role, explicitly mark every anonymous surface, and
add a logout affordance. This is the phase that actually enforces FR-003.

### Changes Required:

#### 1. Maestro route gating

**File**: `src/main/java/com/github/mjjaniec/lmq/views/maestro/MaestroView.java`,
`StartGameView.java`, `DjView.java`, `FeedbackView.java`

**Intent**: Annotate each with `@RolesAllowed("MAESTRO")` individually — Vaadin route security is
per-view, not inherited from a layout, so `MaestroView`'s `onAttach` chokepoint does not implicitly cover
its children, and `FeedbackView`'s literal-path bypass (see Current State Analysis) needs the same
annotation as the properly-nested views.

**Contract**: `@RolesAllowed("MAESTRO")` on all four classes.

#### 2. Explicit anonymous surfaces

**File**: every `@Route`-annotated class under `views/bigscreen` (`BigScreenView` and its 8 nested views)
and every `@Route`-annotated class under `views/player` (`RootView`, `PlayerView`, `JoinView`, and
`PlayerView`'s 9 nested views)

**Intent**: Once Spring Security is active, unannotated routes default to requiring authentication —
every one of these must be explicitly `@AnonymousAllowed` to preserve today's zero-friction anonymous
access, including the two literal-path routes (`JoinView` at `"player/join"`, and the player package's own
`FeedbackView` if it doesn't already sit under `PlayerView`'s layout).

**Contract**: `@AnonymousAllowed` on every class in both packages; verify none was missed by re-running
the full route inventory from Current State Analysis before closing this phase.

#### 3. Logout

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

- Existing Playwright IT suite still passes with routes gated: `./mvnw verify -Pit -Pproduction`
- Formatting passes: `./mvnw spotless:check`

#### Manual Verification:

- Unauthenticated visit to `/maestro`, `/maestro/start`, `/maestro/dj`, and `/maestro/feedback` each
  redirect to `/login`
- Authenticated maestro can reach all four maestro routes normally
- Big-screen QR display, player join (`/player/join`), and the full anonymous player flow work with zero
  added steps or visible change
- Logout ends the session and a subsequent visit to `/maestro` redirects to `/login` again

**Implementation Note**: After completing this phase and all automated verification passes, pause here
for manual confirmation before proceeding to Phase 4.

---

## Phase 4: Integration Test Login Seam

### Overview

Close the gap Phase 3 opens for the Playwright/TestBench IT suite: with routes gated, every existing IT
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

- [x] 1.1 Unit test for MaestroStore/JpaMaestroStore passes
- [x] 1.2 Formatting passes (spotless:check)
- [x] 1.3 Full unit test suite passes

#### Manual

- [x] 1.4 App still starts locally with the new dependency
- [x] 1.5 Human setup gate complete: dedicated Gmail account created, 2-Step Verification enabled, App
      Password generated, IMAP/POP disabled, `smtp_user`/`smtp_pass` set in Railway Variables

### Phase 2: Spring Security One-Time-Token Login Wiring

#### Automated

- [ ] 2.1 Unit tests for MagicLinkOneTimeTokenService pass
- [ ] 2.2 Formatting passes (spotless:check)
- [ ] 2.3 Full unit test suite passes

#### Manual

- [ ] 2.4 Requesting a link with SMTP unconfigured logs the link instead of failing
- [ ] 2.5 Clicking the logged link authenticates and lands on /maestro
- [ ] 2.6 Re-visiting the same link a second time fails (single-use)
- [ ] 2.7 Visiting an expired (30+ min) link fails
- [ ] 2.8 Requesting a second link within 60s does not send a second email
- [ ] 2.9 Requesting links for two different emails from the same IP within 60s only creates/sends for
      the first
- [ ] 2.10 With real Gmail SMTP configured, a request delivers an actual email to the target inbox

### Phase 3: Route Gating & Logout

#### Automated

- [ ] 3.1 Existing Playwright IT suite still passes with routes gated
- [ ] 3.2 Formatting passes (spotless:check)

#### Manual

- [ ] 3.3 Unauthenticated visits to all four maestro routes redirect to /login
- [ ] 3.4 Authenticated maestro can reach all four maestro routes normally
- [ ] 3.5 Big-screen/player/join flow works with zero added friction
- [ ] 3.6 Logout ends the session and re-gates /maestro

### Phase 4: Integration Test Login Seam

#### Automated

- [ ] 4.1 Full CI-equivalent suite passes (verify -Pit -Pproduction)
- [ ] 4.2 Formatting passes (spotless:check)

#### Manual

- [ ] 4.3 /test/login is unreachable outside the integration-test profile
