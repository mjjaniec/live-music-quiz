# Maestro Account Auth — Plan Brief

> Full plan: `context/changes/maestro-account-auth/plan.md`

## What & Why

Add passwordless "magic link" email login for the maestro role, and gate maestro-control routes behind it. This is
roadmap slice S-01, the first step toward a hosted, multi-maestro deployment — today the app has no accounts at all, so
any visitor reaches the single shared game and set-list.

## Starting Point

No auth exists anywhere in the codebase (zero Spring Security, zero login code). `MaestroView.onAttach`
already funnels the two properly-nested maestro routes (`StartGameView`, `DjView`), but `FeedbackView`
bypasses it via a literal path and needs separate gating. The `stores` package has a clean, repeated interface→
`CrudRepository`→`@Entity` triad pattern to follow for the new `Maestro` account.

## Desired End State

A maestro requests a link at `/login`, clicks it, and lands authenticated on `/maestro` for up to 30 days. Anyone
hitting a maestro route unauthenticated is redirected to `/login`. Players, big-screen, and join routes are unaffected —
zero added friction. Playwright IT tests can authenticate without real email.

## Key Decisions Made

| Decision                    | Choice                                                                          | Why (1 sentence)                                                                                                                                                                  | Source |
|-----------------------------|---------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| Auth mechanism              | Passwordless magic-link email                                                   | Matches PRD's "stable external identity, no-new-password" direction without an external OAuth dependency                                                                          | Plan   |
| Registration                | Auto-register on first link request                                             | One form serves both register and log in; the emailed link is the real credential either way                                                                                      | Plan   |
| Auth building block         | Spring Security 7's built-in One-Time Token Login, custom `OneTimeTokenService` | Built-in feature avoids hand-rolled token logic; custom (not `JdbcOneTimeTokenService`) needed for a JPA-backed store following this repo's pattern, and sidesteps CVE-2026-22751 | Plan   |
| Vaadin security integration | `VaadinSecurityConfigurer` (not `VaadinWebSecurity`)                            | `VaadinWebSecurity` was removed in Vaadin 25 — confirmed via docs, corrects a stale recommendation in this repo's own stack-assessment.md                                         | Plan   |
| Email delivery              | SMTP via Spring Mail, env-var configured; logs the link when unconfigured       | Fits the existing `${var}` secrets convention; keeps local dev working without real SMTP                                                                                          | Plan   |
| Link policy                 | 30-minute expiry, single-use                                                    | User-specified value (default recommendation was 15 min)                                                                                                                          | Plan   |
| Session length              | 30-day persistent cookie                                                        | Matches occasional-use pattern; avoids re-login friction mid-event                                                                                                                | Plan   |
| Rate limiting               | In-memory per-email 60s send cooldown                                           | Cheap abuse guard consistent with the app's existing single-instance deploy story                                                                                                 | Plan   |
| Account fields              | Email only, no display name                                                     | Matches this slice's committed scope (FR-001/002/003); avoids inventing a profile-collection flow                                                                                 | Plan   |
| IT test login               | `@Profile("integration-test")` backdoor endpoint                                | No shared IT base class exists to hook a real-email flow into; mirrors existing direct-injection test patterns                                                                    | Plan   |

## Scope

**In scope:** maestro registration/login/logout via magic link; route gating on `MaestroView`,
`StartGameView`, `DjView`, `FeedbackView`; explicit `@AnonymousAllowed` audit of every big-screen/player route; IT test
login seam.

**Out of scope:** tenant-scoping games/set-lists to an owner (S-02); performer view (FR-011); any profile fields beyond
email; multi-role/admin model; multi-instance-safe rate limiting.

## Architecture / Approach

Spring Security 7's `oneTimeTokenLogin(...)` DSL handles the login flow; a custom `OneTimeTokenService`
(JPA-backed, following the `RealStageStore` wrap-a-repository precedent) replaces the built-in
`JdbcOneTimeTokenService` for both persistence-pattern consistency and to avoid a known CVE in that class.
`VaadinSecurityConfigurer` composes in the same filter chain to gate Vaadin routes via
`@RolesAllowed`/`@AnonymousAllowed`. The one non-standard piece: `LoginView`'s email-submission control must be a raw
HTML `<form>` (not a Vaadin `Button`), because Vaadin's RPC-based component model can't drive the real browser POST
Spring Security's endpoint requires.

## Phases at a Glance

| Phase                                   | What it delivers                                                  | Key risk                                                                                            |
|-----------------------------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| 1. Maestro persistence + email scaffold | `Maestro`/`MaestroStore` triad, SMTP config with dev-log fallback | Low — no security wiring yet                                                                        |
| 2. One-Time-Token login wiring          | Working login end-to-end (not yet gating anything)                | Highest — new Spring Security 7 API, CSRF on the raw HTML form, push/redirect gotcha                |
| 3. Route gating + logout                | FR-003 actually enforced; anonymous surfaces audited              | Missing an `@AnonymousAllowed` on a bypass route (`JoinView`, `FeedbackView`) breaks anonymous flow |
| 4. IT test login seam                   | Full IT suite passes with routes gated                            | Backdoor endpoint accidentally reachable outside `integration-test` profile                         |

**Prerequisites:** none (S-01 has no upstream slice dependency). **Estimated effort:** ~3-4 sessions across 4 phases,
fits within the PRD's 3-week after-hours budget.

## Open Risks & Assumptions

- Exact Spring Security 7.0.x API shapes (`OneTimeTokenService`, DSL method names, CSRF token access) were verified
  against current docs but not against the literal Spring Boot 4.0.1/Vaadin 25.0.2 pairing — flagged as extrapolation in
  the plan; confirm exact signatures during Phase 2 implementation.
- No worked example combining `VaadinSecurityConfigurer` + `oneTimeTokenLogin(...)` was found in official docs — the
  composition is inferred from both being ordinary `HttpSecurity` configurers.
- Whether Spring Security's default logout endpoint needs the same raw-HTML-form treatment as login is left for Phase 3
  to confirm.

## Success Criteria (Summary)

- A maestro can register/log in via magic link and log out; an unauthenticated maestro-route hit redirects to sign-in
  (FR-001/002/003, US-01 precondition)
- The full Playwright IT suite passes with routes gated
- Big-screen/player/join flow is unchanged, with no added friction or perceptible lag
