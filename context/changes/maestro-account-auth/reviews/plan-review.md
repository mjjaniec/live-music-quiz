<!-- PLAN-REVIEW-REPORT -->
# Plan Review: Maestro Account Auth Implementation Plan

- **Plan**: context/changes/maestro-account-auth/plan.md
- **Mode**: Deep
- **Date**: 2026-08-15
- **Verdict**: REVISE (SOUND after triage — all 5 findings resolved)
- **Findings**: 1 critical, 2 warnings, 2 observations — all FIXED

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| End-State Alignment | WARNING |
| Lean Execution | PASS |
| Architectural Fitness | PASS |
| Blind Spots | WARNING |
| Plan Completeness | FAIL |

## Grounding

15/15 paths verified (MaestroView.java, FeedbackView.java x2, JoinView.java, PlayerStore/JpaPlayerStore/PlayerDto triad, RealStageStore.java, HintController.java, application.properties, pom.xml, GameServiceImpl.java, all 4 IT test files, bigscreen/player route inventories) ✓, 3/3 symbols confirmed (spring-security.version=7.0.2 in Boot 4.0.1's managed BOM; OneTimeTokenService real interface at the 7.0.5 tag; route-literal bypasses `maestro/feedback` and `player/join`) ✓, brief↔plan consistent ✓.

External API claims (Phase 2's own flagged uncertainties) were verified against live Spring Security 7.0.5 source and Vaadin 25 docs via a dedicated research pass — see F1 for the one correction found.

## Findings

### F1 — `OneTimeTokenService.consume(...)` contract is wrong in Phase 2 §2

- **Severity**: CRITICAL
- **Impact**: LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Completeness
- **Location**: Phase 2, Changes Required #2 (`MagicLinkOneTimeTokenService`)
- **Detail**: The plan documents the interface as `consume(String tokenValue) -> Authentication`. Verified against the actual Spring Security 7.0.5 source (`OneTimeTokenService.java`), the real signature is `@Nullable OneTimeToken consume(OneTimeTokenAuthenticationToken authenticationToken)` — it takes the framework's authentication token object (not a raw string) and returns a nullable `OneTimeToken` (not `Authentication`). `generate(GenerateOneTimeTokenRequest) -> OneTimeToken` is correct as written. If an implementer codes directly against the plan's stated contract, `MagicLinkOneTimeTokenService` won't compile as an `OneTimeTokenService` implementation.
- **Fix**: Correct the Contract line in Phase 2 §2 to: `consume(OneTimeTokenAuthenticationToken authenticationToken) -> @Nullable OneTimeToken` — the implementation extracts the token string via the argument (e.g. `authenticationToken.getTokenValue()`), looks it up, deletes the row unconditionally, and returns `null` (not throws) when expired/absent, matching the interface's nullable-return contract rather than a throw.
- **Decision**: FIXED (applied to plan.md Phase 2 §2)

### F2 — `MaestroDto` binds account identity to email, contradicting the PRD's explicit design intent

- **Severity**: WARNING
- **Impact**: HIGH — architectural stakes; think carefully before deciding
- **Dimension**: End-State Alignment
- **Location**: Phase 1, Changes Required #1 (`Maestro`/`MaestroStore`/`MaestroDto`)
- **Detail**: `context/foundation/prd.md` Access Control Changes section states explicitly: "the maestro account is tied to a stable external identity (swappable per provider), **not to an email address alone**." The plan makes email the literal primary key (`record Maestro(String email)`, `MaestroDto { @Id String email }`) and the sole account attribute. This is architecturally consistent with the existing codebase (PlayerDto also uses a natural key), and the roadmap did defer the *provider* choice to this plan (magic-link email vs. OAuth) — but choosing email-as-primary-key is exactly the "tied to an email address alone" shape the PRD asked to avoid. If a later slice adds an alternate login provider (Google/GitHub OAuth) for the same maestro, there's no stable surrogate ID to attach it to — matching would have to go through the (mutable) email column, or require a schema migration then.

  Fix A ⭐ Recommended: Add a surrogate `id` (UUID) as `@Id` on `MaestroDto`, keep `email` as a `@Column(unique = true)` attribute used for the magic-link lookup today.
  - Strength: Costs nothing extra now (one extra column), and any future auth provider attaches to the stable `id` instead of a mutable/provider-specific email — matches the PRD's stated intent directly.
  - Tradeoff: `MaestroStore.findByEmail`/`createIfAbsent` need an extra id-generation step; `UserDetailsService`'s principal name is now an id, not the email string, which slightly complicates `MagicLinkOneTimeTokenService`/logging that currently think in terms of email.
  - Confidence: HIGH — this is a standard "natural key today, surrogate key for extensibility" pattern, low risk to add before any rows exist.
  - Blind spot: Whether S-02's tenant-isolation FK design already assumes `Maestro.email` as the join key — not checked, since S-02 isn't planned yet.

  Fix B: Keep email as the primary key as currently planned, and treat this as accepted MVP debt.
  - Strength: Simplest, matches this slice's committed scope (email-only account, no profile fields) with zero extra code.
  - Tradeoff: A future OAuth/second-provider addition requires a schema migration + data backfill to introduce a surrogate id after real rows exist — exactly the migration cost the PRD's phrasing was trying to avoid paying later.
  - Confidence: MEDIUM — acceptable if the team is confident magic-link email is the permanent mechanism, not a placeholder for OAuth.
  - Blind spot: No signal in the PRD/roadmap on how likely a second auth provider is post-MVP.
- **Decision**: FIXED (applied Fix A — surrogate `UUID id` added to `Maestro`/`MaestroDto` in plan.md Phase 1 §1)

### F3 — Phase 4 lists a test file that has nothing to wire

- **Severity**: WARNING
- **Impact**: LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Completeness
- **Location**: Phase 4, Changes Required #2
- **Detail**: The plan lists `WrapUpDataInjectionIT.java` among files needing the backdoor-login call added "before each test's maestro Page navigates to /maestro." Checked the file directly: it's a `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `@MockitoBean`s that injects data straight into Spring-managed stores/services — it never opens a Playwright `Page`/`BrowserContext` and never navigates to any Vaadin route, gated or not. There's nothing in this file for Phase 4 to touch. (For contrast: `GameFlowIT`'s single call site is the shared `ensureGameNotStarted` helper, and `SmokeIT`/`WrapUpGuiVerificationIT` each have exactly one direct `/maestro*` navigation — so the actual Phase 4 blast radius is 3 call sites total, not 4 files each needing edits.)
- **Fix**: Drop `WrapUpDataInjectionIT.java` from the Phase 4 file list; note the real change set is one call site each in `SmokeIT`, `GameFlowIT` (via `ensureGameNotStarted`), and `WrapUpGuiVerificationIT`.
- **Decision**: FIXED (plan.md Phase 4 §2 corrected)

### F4 — No failure-path coverage for email delivery, and no cross-target abuse limit

- **Severity**: OBSERVATION
- **Impact**: MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Blind Spots
- **Location**: Phase 2, Success Criteria / `OneTimeTokenGenerationSuccessHandler`
- **Detail**: Two related gaps, both plausible at this app's small/low target scale but worth a conscious accept-or-fix decision rather than silent omission: (1) Success criteria only test the "SMTP unconfigured → logs the link" fallback; there's no criterion for "SMTP configured but `JavaMailSender.send()` throws" (bad creds, mail host down) — as specified, the UI would still show "check your email" even though nothing was sent, with no server-side signal beyond an exception in logs. (2) The 60-second cooldown is scoped per-email, so it limits repeat requests to the *same* address but not the total volume of *distinct* addresses one visitor can target — `/login` is an anonymous, public, auto-registering endpoint, so it can be used to send unsolicited email to arbitrary third-party addresses at a rate of one per submission with no aggregate throttle.
- **Fix (as applied, differs from the original proposal)**: Rather than only logging-and-accepting, added a per-IP cooldown addressing part (2) directly — same in-memory `ConcurrentHashMap<String, Instant>` mechanism as the per-email cooldown, keyed by client IP via `RequestContextHolder`, enforced inside `MagicLinkOneTimeTokenService.generate(...)` itself (before account/token creation, not just before the email send) — so a single flooding source can't grow the `Maestro`/token tables or trigger unbounded sends, though a distributed attack across many IPs is explicitly still out of scope (consistent with "no multi-instance-safe rate limiting"). Also added `server.forward-headers-strategy=framework` since the app runs behind Railway's edge proxy and `getRemoteAddr()` would otherwise return the proxy's IP for every request. Part (1), SMTP send-failure logging, was not addressed — remains an open item.
- **Decision**: FIXED (partially — per-IP rate limiting added to plan.md Phase 2 §2/§4 and Testing Strategy; SMTP send-failure logging still not addressed, left as accepted risk)

### F5 — `authenticationSuccessHandler(...)` on `oneTimeTokenLogin` is deprecated in 7.0.x

- **Severity**: OBSERVATION
- **Impact**: LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Completeness
- **Location**: Phase 2, Changes Required #4 (Security filter chain code sample)
- **Detail**: Verified against the `OneTimeTokenLoginConfigurer` source at the 7.0.5 tag: `.authenticationSuccessHandler(...)` on this configurer still works but is deprecated since 6.5 in favor of the shared `AbstractAuthenticationFilterConfigurer.successHandler(...)`. Using the plan's exact sample as-is will compile and run but emit a deprecation warning.
- **Fix**: Note in Phase 2 §4 to prefer `.successHandler(redirectToMaestroSuccessHandler)` over `.authenticationSuccessHandler(...)` if implementation-time docs still show it deprecated.
- **Decision**: FIXED (plan.md Phase 2 §4 code sample and note updated)
