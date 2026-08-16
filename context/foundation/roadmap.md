---
project: "Live Music Quiz"
version: 1
status: draft
created: 2026-08-13
updated: 2026-08-16
prd_version: 1
main_goal: quality
top_blocker: decisions
---

# Roadmap: Live Music Quiz

> Derived from `context/foundation/prd.md` (v1) + auto-researched codebase baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Vision recap

Live Music Quiz runs an in-person, real-time music-guessing game across three synchronized screens (a maestro control
panel, a shared big-screen, and each player's phone). It works today for a single maestro against one shared, global
game with no accounts. Moving toward a hosted, multi-maestro deployment means wrapping that same live engine in
per-maestro ownership and isolation — without touching the game mechanics or the players' zero-friction anonymous join.

## North star

**S-03: Maestro runs the owned game live, isolation holds through the event** — this is the fullest proof that the
existing live engine can be wrapped in per-maestro ownership without breaking real-time sync or leaking one maestro's
game to another, including mid-quiz.

> "North star" here means the smallest end-to-end slice whose successful delivery proves the
> whole change is safe to ship. It is sequenced last only because it structurally needs S-01 and
> S-02 first (there's no live game to run without a logged-in maestro who owns one) — not
> because it's deprioritized.

## At a glance

| ID    | Change ID              | Outcome (user can …)                                                                      | Prerequisites | PRD refs                              | Status   |
|-------|------------------------|-------------------------------------------------------------------------------------------|---------------|---------------------------------------|----------|
| S-01  | maestro-account-auth   | Register, log in/out; unauthenticated maestro-route hits redirect to sign-in              | —             | FR-001, FR-002, FR-003, US-01         | done |
| S-01b | http-email-integration | Maestro's magic-link sign-in email delivers reliably in production                        | S-01          | — (production incident; see below)    | proposed |
| S-02  | owned-game-and-setlist | Import a set-list from Sheets and create an account-scoped game no other maestro can see  | S-01          | FR-004, FR-005, FR-006, FR-010, US-01 | proposed |
| S-03  | isolated-live-game-run | Run the owned game live end-to-end; players join by QR; isolation holds through the event | S-02          | FR-006, FR-007, FR-008, FR-009, US-01 | proposed |

## Baseline

What's already in place in the codebase as of `2026-08-13` (auto-researched + user-confirmed). Foundations/slices below
assume these are present and do NOT re-scaffold what's already there.

- **Frontend:** present — Vaadin Flow 25.0.2 + Vite/npm, `@Route` views split into
  `views/maestro`, `views/bigscreen`, `views/player` packages, Lumo theme (`src/main/frontend/themes/live-music-quiz`).
- **Backend / API:** present — Spring Boot 4.0.1 + Vaadin; game logic in
  `GameService`/`GameServiceImpl`, currently a single global `@Component` singleton holding one shared game's state (no
  per-account scoping yet). One REST endpoint (`HintController`).
- **Data:** present but unscoped — Spring Data JPA + PostgreSQL/H2, Hibernate
  `ddl-auto=update` (no Flyway/Liquibase). JPA entities (`QuizDto`, `PlayerDto`, `AnswerDto`,
  `PlayOffDto`, `StageDto`, `FeedbackDto`, `CustomMessageDto`, …) exist but none has an owner/account/maestro field —
  everything is global today.
- **Auth:** absent — no Spring Security, no `VaadinWebSecurity`, no
  `@AnonymousAllowed`/`@RolesAllowed` anywhere. This is exactly the gap S-01 closes.
- **Deploy / infra:** partial — `Dockerfile` + `railway.json` exist (Railway is the selected target per
  `infrastructure.md`), but CI doesn't trigger a deploy and there's no
  `context/deployment/deploy-plan.md` yet; a separate legacy `deploy.sh` (manual `scp` to an EC2/systemd host) also
  exists. Not touched by this roadmap — deploy automation is out of this skill's scope.
- **Observability:** absent — default Logback only; no Actuator/Sentry/OpenTelemetry/Micrometer. No PRD requirement
  forces this in for the MVP, so no foundation is proposed for it.

## Foundations

None needed for this MVP. The two candidate cross-cutting elements — an auth scaffold and a centralized
ownership/isolation check — each have a direct, user-visible outcome of their own (registering/logging in; creating a
game no other maestro can see) once built, so they are scoped as vertical slices (S-01, S-02) rather than horizontal
foundations. No PRD signal (NFR, Access Control, or baseline gap) implies a prerequisite that isn't already covered by
S-01/S-02 themselves.

## Slices

### S-01: Maestro can register and sign in to a personal account

- **Outcome:** user can register an account, log in and log out; an unauthenticated visitor hitting a maestro-gated
  route is redirected to sign-in.
- **Change ID:** `maestro-account-auth`
- **PRD refs:** FR-001, FR-002, FR-003, US-01 (precondition: "a maestro who has registered and logged in")
- **Prerequisites:** —
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:**
    - Concrete auth mechanism (OAuth/Google vs. local email+password vs. passwordless) — direction locked (stable
      external identity, no-new-password preferred) but the specific provider is deliberately deferred. Owner: user,
      during `/10x-plan`. Block: no.
    - If OAuth is chosen, the Playwright e2e story needs a dev-profile login bypass or a stubbed provider (a real
      provider login can't be scripted). Owner: user, downstream. Block: no.
- **Risk:** Vaadin route-security is the training-data gap the stack assessment flags as the agent's weakest surface on
  this stack. Sequenced first anyway, because every later slice needs a logged-in maestro to scope work to — deferring
  it would leave S-02/S-03 unplannable.
- **Status:** done

### S-01b: Maestro's magic-link email delivers reliably in production

- **Outcome:** the magic-link sign-in email that S-01 sends actually reaches the maestro's inbox when the app is
  running on Railway in production, not just locally.
- **Change ID:** `http-email-integration`
- **PRD refs:** — not sourced from the PRD; discovered as a production incident after the S-01 deploy. In practice it
  blocks US-01's login precondition ("a maestro who has registered and logged in") whenever magic-link delivery fails.
- **Prerequisites:** S-01
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:**
    - Which HTTP-based transactional email provider to use (Resend, SendGrid, Mailgun, Postmark) — Railway's own docs
      point at Resend, but the choice is deliberately deferred. Owner: user, during `/10x-plan`. Block: no — any of the
      four works.
- **Risk:** Root cause confirmed: Railway blocks all outbound SMTP (ports 25/465/587/2525) on the Free/Trial/Hobby
  plans; `MagicLinkEmailSuccessHandler`'s direct-SMTP `JavaMailSender` config (`smtp.gmail.com:587`) times out at the
  network level in production. Not a code bug in S-01 itself. Fix is to replace the SMTP transport with an HTTPS email
  API, which Railway recommends over SMTP even on paid plans. Sequenced directly after S-01 (ahead of S-02) because it
  blocks S-01's actual production usability, even though S-02/S-03 don't structurally depend on it.
- **Status:** proposed

### S-02: Maestro can create an owned, isolated game with an imported set-list

- **Outcome:** user can import a set-list from their Google Sheet and create an account-scoped game referencing it; no
  other maestro can see or list it.
- **Change ID:** `owned-game-and-setlist`
- **PRD refs:** FR-004, FR-005, FR-006, FR-010, US-01 (ownership/isolation acceptance criteria)
- **Prerequisites:** S-01
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:**
    - Unguessable game/set-list identifier scheme for URLs (feeds the QR code in S-03). Direction locked (use
      unguessable ids so anonymous join can't be used to browse other maestros' games). Owner: user, downstream design.
      Block: no.
- **Risk:** Carries FR-006, the PRD's explicitly named primary success criterion (centralized, no-IDOR enforcement, not
  "light scoping"). Sequenced right after auth exists, so isolation is provably correct at rest before the higher-risk
  live-flow code (S-03) is touched.
- **Status:** proposed

### S-03: Maestro runs the owned game live end-to-end; isolation holds through the event

- **Outcome:** user can run their game live (all 3 round modes, reveals, rankings, play-off, final results) exactly as
  before; players join anonymously via the big-screen QR into the correct game; a second maestro's concurrent game is
  neither visible nor affected, including mid-quiz.
- **Change ID:** `isolated-live-game-run`
- **PRD refs:** FR-006 (verified under live conditions), FR-007, FR-008, FR-009, US-01 (full acceptance criteria)
- **Prerequisites:** S-02
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:**
    - QR encoding scheme for the game identifier on the big-screen (same identifier-scheme question as S-02's Unknown,
      resolved once and shared). Owner: user, downstream. Block: no.
    - Retention period for persisted player answers, and whether a privacy notice is needed. Owner:
      user, downstream. Block: no — the user has already judged the live experience unchanged and flagged this for
      completeness, not as a blocker.
- **Risk:** Highest regression risk in the roadmap — it touches the real-time push/threading model the stack assessment
  flags as the weakest-covered Vaadin surface, under the guardrail that scoping must not add lag or break the
  active-game lookup mid-quiz. This is the north star:
  the full validation that ownership/isolation can wrap the live engine without breaking it.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID                | Suggested issue title                                                                    | Ready for `/10x-plan` | Notes                                |
|------------|--------------------------|------------------------------------------------------------------------------------------|-----------------------|--------------------------------------|
| S-01       | `maestro-account-auth`   | Add maestro account registration, login/logout, and maestro-route gating                 | yes                   | Done — see `## Done`                 |
| S-01b      | `http-email-integration` | Replace SMTP magic-link email delivery with an HTTPS email API (Railway blocks SMTP)     | yes                   | Run `/10x-plan http-email-integration`; pick provider first |
| S-02       | `owned-game-and-setlist` | Scope games and set-lists to the owning maestro; import set-list from Sheets per maestro | no                    | Blocked until S-01 lands             |
| S-03       | `isolated-live-game-run` | Run the owned game live end-to-end with isolation holding through the event              | no                    | Blocked until S-02 lands             |

## Open Roadmap Questions

1. **Concrete auth mechanism for the maestro** (OAuth/Google vs. local email+password vs. passwordless) — Owner: user,
   during `/10x-plan` on `maestro-account-auth`. Block:
   `maestro-account-auth` (S-01), and indirectly S-02/S-03 which build on whatever user model S-01 lands.
2. **How the join URL/QR targets a specific game while staying zero-friction and hard to enumerate** — direction locked
   (QR encodes the game identifier; use unguessable ids). Owner:
   user, downstream design. Block: `owned-game-and-setlist` (S-02), `isolated-live-game-run`
   (S-03) — the exact identifier scheme.
3. **Retention of anonymous players' answers** — how long persisted answers are kept and whether a privacy notice is
   needed. Owner: user, downstream. Block: `isolated-live-game-run` (S-03) — flagged for completeness, not treated as a
   hard blocker per the user's own read of the PRD.

## Parked

- **In-app set-list management** — Why parked: PRD Non-Goals; set-list content stays in Google Sheets for this MVP, a
  deliberate scope limit for the 3-week after-hours budget, not a design conviction.
- **Post-event analytics UI** — Why parked: PRD Non-Goals; S-03 captures player answers/feedback under the owned game,
  but no dashboard or difficulty-analysis view ships in this MVP.
- **Player accounts / cross-event identity** — Why parked: PRD Non-Goals; players stay anonymous per game, with no login
  and no cross-event tracking.
- **Admin / multi-role hierarchy** — Why parked: PRD Non-Goals; flat single-Maestro-role model only, no admin or
  shared/team ownership.
- **Performer view (FR-011)** — Why parked: nice-to-have per its own PRD resolution ("fully parked — attempted only if
  time remains after accounts + scoping + isolation land"); not a committed slice.
- **Zero-downtime / live-migration guarantee** — Why parked: PRD non-functional non-goal; updates land between events,
  so in-flight upgrade safety is out of scope.

## Done

- **S-01 — Maestro can register and sign in to a personal account** (`maestro-account-auth`). Shipped and
  impl-reviewed (`context/changes/maestro-account-auth/reviews/impl-review-phase-3-4.md`), then deployed to
  production. Note: deploying it surfaced S-01b (Railway blocks outbound SMTP, breaking magic-link delivery) — the
  auth mechanism itself is not at fault.

