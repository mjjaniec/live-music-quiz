<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Maestro Account Auth

- **Plan**: context/changes/maestro-account-auth/plan.md
- **Scope**: Phase 3-4 of 4
- **Date**: 2026-08-16
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical, 2 warnings, 3 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | WARNING |
| Success Criteria | PASS |

## Findings

### F1 — Residual multi-profile risk in TestAuthController's zero-credential backdoor

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/github/mjjaniec/lmq/api/TestAuthController.java:18
- **Detail**: `@Profile("integration-test")` matches if that string appears anywhere in a comma-separated active-profiles list — Spring does not treat multiple active profiles as mutually exclusive. If a deployment ever sets `spring.profiles.active=integration-test,production` (typo, copy-paste, CI/CD env-var concatenation bug), this endpoint becomes live in production and lets anyone authenticate as any email with zero credential check. Profile activation here is a plain string env var, same convention as the rest of this app's config.
- **Fix A ⭐ Recommended**: Document the constraint explicitly (e.g. in AGENTS.md or deploy notes): "`integration-test` must never appear in any production profile list."
  - Strength: Zero code change; matches this app's existing plain-env-var secrets/config convention and its documented-risk posture elsewhere (e.g. the PRD's accepted single-instance rate-limiting tradeoff).
  - Tradeoff: Doesn't technically prevent misconfiguration — relies on the deploy operator reading docs.
  - Confidence: HIGH — this app's deploy story is small-scale, single-operator; documentation matches existing risk posture elsewhere.
  - Blind spot: Haven't checked whether Railway's env panel makes this kind of typo more or less likely than other platforms.
- **Fix B**: Add a second independent guard, e.g. `@ConditionalOnProperty(name = "test-auth.enabled", havingValue = "true")` requiring an explicit additional property only set in `application-integration-test.properties`.
  - Strength: Defense-in-depth — requires two independent misconfigurations to expose the backdoor, not one.
  - Tradeoff: Adds a new property and slightly more moving parts for a test-only seam.
  - Confidence: MEDIUM — effective but somewhat heavy for a CI-only test helper.
  - Blind spot: Haven't verified there's an existing project convention for this kind of double-guard elsewhere in the codebase.
- **Decision**: FIXED — implemented a modified Fix B: instead of a new `test-auth.enabled` property, added `NotOnRailwayCondition` (`src/main/java/com/github/mjjaniec/lmq/api/NotOnRailwayCondition.java`), a `Condition` matching only when `RAILWAY_SERVICE_NAME` is absent, and applied `@Conditional(NotOnRailwayCondition.class)` alongside `@Profile("integration-test")` on `TestAuthController`. Rejected convention/documentation-only (Fix A) as insufficient. Railway sets `RAILWAY_SERVICE_NAME` in every deployed service's environment, so this is an independent, env-based kill switch that holds even if `integration-test` leaks into a production profile list.

### F2 — DjView's join-race fix not applied to slackers/play listeners

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/github/mjjaniec/lmq/views/maestro/DjView.java:444-452
- **Detail**: The new resync-after-register fix (calling `refreshPlayers()` right after `attachPlayerList` registers) was applied only to the player-list listener. `attachSlackersList`/`refreshSlackers` and `attachPlay`/`refreshPlay` register the exact same way, immediately after construction, with no resync — the same race (an update landing in the construct/attach gap gets silently dropped until a later, unrelated broadcast happens to catch it up) plausibly applies to them too. Not reproduced/confirmed for these two — only the player-list race was demonstrated via `bumpOutPlayerFlow`.
- **Fix A ⭐ Recommended**: Apply the identical resync-after-register call to `refreshSlackers()`/`refreshPlay()` in `onAttach()`, mirroring the fix just made for players.
  - Strength: Directly closes the demonstrated bug class everywhere the same construct-then-attach pattern repeats; small, mechanical, low-risk change.
  - Tradeoff: Adds 2 more redundant calls per attach; unverified whether the race is actually observable for these two — their state may already always resync via some other event before it matters.
  - Confidence: MEDIUM — the pattern match is strong, but no concrete failure has been reproduced for slackers/play the way `bumpOutPlayerFlow` demonstrated it for players.
  - Blind spot: Haven't checked whether `refreshSlackers`/`refreshPlay`'s underlying state is inherently less exposed to a "single event right at attach time" scenario (e.g. slackers only matters mid-round, well after DjView attaches).
- **Fix B**: Leave as-is; only fix player-list resync since it's the one with confirmed, reproduced failing-test evidence — treat the others as a documented open question for a follow-up.
  - Strength: Doesn't speculatively change code paths with no demonstrated bug or test coverage backing the change.
  - Tradeoff: Leaves a plausible, unverified latent bug in place.
  - Confidence: MEDIUM — reasonable "don't fix what isn't proven broken," but leaves an inconsistency between the three listeners.
  - Blind spot: No test currently exists that would catch this for slackers/play even if it does happen.
- **Decision**: FIXED — root-caused instead of patching each listener. Moved the dynamic UI construction (the `if (gameService.isGameStarted()) { ... }` accordion-building block) out of the constructor and into `onAttach()`, placed after the three `broadcastAttach.attachXxx(...)` registrations. Since the panels built there (`playersList()`, `refreshPieceContent()`, `refreshPlayOffContent()`) read `gameService`'s current state at build time, and that build now always happens after listeners are registered, the previously-added `refreshPlayers()` catch-up call became redundant and was removed — no separate re-sync call is needed for players, slackers, or play. Also removed the constructor's `else { resetAction.click(); }` branch per user request: it was dead/coincidental (its own `getUI().ifPresent(...)` navigate call no-ops during construction, since the component isn't attached yet; the actual redirect-when-not-started behavior is produced independently by the parent `MaestroView.onAttach`, which re-checks `isGameStarted()`), untested, and only reachable via direct URL navigation to `/maestro/dj`. Net behavior change: hitting `/maestro/dj` directly while no game is running no longer silently resets game state before redirecting away — it now just redirects, since `MaestroView.onAttach` already handles that case. Verified: `spotless:check`/`compile` clean; `GameFlowIT`, `WrapUpGuiVerificationIT`, `SmokeIT` all pass, including the player join/bump-out race scenario that originally demonstrated this bug class.

### F3 — permitAll ordering is correct today but implicit/unasserted

- **Severity**: OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Architecture
- **Location**: src/main/java/com/github/mjjaniec/lmq/config/SecurityConfig.java:22-26
- **Detail**: `authorizeHttpRequests()`'s explicit `permitAll()` for `/test/login` and `/api/v1/hint/**` correctly takes priority over `VaadinSecurityConfigurer`'s deferred `anyRequest().denyAll()` (verified: the explicit rule runs synchronously in the bean method, before the configurer's own `configure()` phase adds its catch-all at `.build()` time). But this correctness depends on builder-chain ordering and isn't asserted by any test — a future refactor could silently break it.
- **Fix**: Add a lightweight test (e.g. `MockMvc` or a minimal Spring context test) asserting `GET /test/login` and `GET /api/v1/hint/artist` are reachable without a session, to guard against a silent regression.
- **Decision**: FIXED — added `src/test/java/com/github/mjjaniec/lmq/SecurityConfigIT.java`, a new IT (not a `MockMvc`/`@SpringBootTest` unit test, to stay consistent with this project's existing convention that only `*IT` tests boot the app/DB — see AGENTS.md) using a plain `java.net.http.HttpClient` with redirects disabled, following `SmokeIT`'s `BASE_URL`/`PORT` pattern. Asserts `GET /test/login` and both `GET /api/v1/hint/{artist,title}` return 200 with no session/cookies sent. Verified the test actually guards the regression: temporarily replaced the `permitAll` matcher with a bogus path and confirmed both assertions failed with the expected 302 (redirect to login), then reverted. Passes against the real `integration-test` profile (H2 in-memory DB).

### F4 — No session-fixation protection in TestAuthController

- **Severity**: OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/github/mjjaniec/lmq/api/TestAuthController.java:34-36
- **Detail**: Unlike the real `oneTimeTokenLogin` flow, this endpoint never calls `request.changeSessionId()`/applies a `SessionAuthenticationStrategy`. Acceptable for a profile-gated, test-only endpoint with no real users; worth a caveat if this pattern is ever copied outside a test-only context.
- **Fix**: No action needed now — note as a caveat if this pattern is ever reused outside a test-only context.
- **Decision**: ACKNOWLEDGED — no action; accepted as-is for this test-only, profile/env-gated endpoint.

### F5 — Hardcoded test email duplicated across 3 test files

- **Severity**: OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/test/java/com/github/mjjaniec/lmq/GameFlowIT.java:865, SmokeIT.java:56, WrapUpGuiVerificationIT.java:173
- **Detail**: The literal `"maestro@test.local"` is duplicated identically across 3 test files with no shared constant/helper. Consistent (no drift), just duplicated.
- **Fix**: Extract a shared constant (e.g. a small test-auth helper or a static final String in a shared test base) so a future convention change is a one-line edit.
- **Decision**: FIXED — added `src/test/java/com/github/mjjaniec/lmq/TestAuth.java` with `static final String MAESTRO_EMAIL = "maestro@test.local"`, and updated `GameFlowIT`, `SmokeIT`, `WrapUpGuiVerificationIT` to reference it instead of the duplicated literal. Also updated `SecurityConfigIT` (added while fixing F3, which had its own distinct literal) to use the same shared constant, so all four call sites now share one source of truth. Verified: full `verify -Pit -Pproduction` suite passes.
