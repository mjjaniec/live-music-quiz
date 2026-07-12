---
project: live-music-quiz
assessed_at: 2026-07-12T08:37:33Z
agent_readiness: ready-with-compensation
context_type: brownfield
stack_components:
  language: Java 25
  framework: Spring Boot 4.0.1 + Vaadin Flow 25.0.2
  build_tool: Maven (mvnw wrapper) + Spotless
  test_runner: JUnit 5 + Playwright + Vaadin TestBench
  package_manager: Maven
  ci_provider: GitHub Actions
  deployment_target: Docker (Temurin 25) + manual scp/systemd to EC2
gates_passed: 11
gates_failed: 1
---

## Stack Components

**Language — Java 25.** Statically typed by the language. The project goes further than the baseline: it opts into JSpecify null-safety with `@NullMarked` `package-info.java` files across `config`, `services`, `model`, and `views/player` packages (JSpecify 1.0.0 in `pom.xml`), plus Lombok for boilerplate reduction. An agent can reason about input/output shapes and nullability from the source without running the program.

**Backend framework — Spring Boot 4.0.1.** Inherited via `spring-boot-starter-parent`. Provides autoconfiguration, Spring Data JPA (`spring-boot-starter-data-jpa`), and bean-validation (`spring-boot-starter-validation`). Persistence is PostgreSQL in production (`postgresql` 42.7.8) and H2 at runtime for dev/test. This is the dominant Java web framework and the part of the stack an agent will handle most fluently.

**UI framework — Vaadin Flow 25.0.2.** Server-side, component-based UI (`vaadin-spring-boot-starter`, `vaadin-core`). Views live under `com.github.mjjaniec.lmq.views` split into `bigscreen`, `maestro`, and `player` — mirroring the PRD's three-view synchronized model. Vaadin drives the real-time surfaces server-side rather than through a separate JS/TS frontend. This is the one component that carries an agent-friendliness gap (see below).

**Build tool — Maven** with the `mvnw` wrapper, plus Spotless (Eclipse formatter) for Java formatting and the `vaadin-maven-plugin` for frontend build. A `production` profile enables Vaadin production mode; an `it` profile wires Failsafe integration tests around a started Spring Boot instance.

**Test runner — JUnit 5 + Playwright + Vaadin TestBench.** Unit tests (`MainSetParserTest`, `PointsCounterTest`) run under `spring-boot-starter-test`; browser-level integration tests (`SmokeIT`, `GameFlowIT`, `WrapUpGuiVerificationIT`, `WrapUpDataInjectionIT`) run via Playwright + Vaadin TestBench through the `it` profile. Strong existing coverage of the live game flow — valuable ballast for the "must not regress" guardrails in the PRD.

**CI/CD — GitHub Actions** (`.github/workflows/maven-it.yml`): builds on JDK 25, installs Playwright browsers, and runs `mvn verify -Pit -Pproduction` on every push/PR to `main`.

**Deployment — Docker + manual script.** Multi-stage `Dockerfile` on `eclipse-temurin:25`. `deploy.sh` builds the production jar and pushes it via `scp` to a systemd-managed service on an EC2 host — a manual, single-target deploy rather than an automated pipeline.

## Quality Gate Assessment

| Component        | Typed | Convention | Training Data | Documented | Verdict              |
|------------------|-------|------------|---------------|------------|----------------------|
| Language (Java)  | ✓     | —          | —             | —          | pass                 |
| Spring Boot      | —     | ✓          | ✓             | ✓          | pass                 |
| Vaadin Flow      | —     | ✓          | ✗             | ✓          | pass-with-gap        |
| Maven            | —     | ✓          | ✓             | ✓          | pass                 |
| Test runner      | —     | —          | ~             | ✓          | pass                 |

Legend: ✓ = pass, ✗ = fail, ~ = partial, — = not applicable

### Gate Details

**Type safety — pass.** Evidence: `pom.xml` sets `java.version` 25 and compiles with `-source/-target 25`; the language is statically typed. Beyond that, `jspecify` 1.0.0 is a compile dependency and four `package-info.java` files carry `@NullMarked`, so nullability is an explicit, checkable contract rather than convention. This is stronger than a typical Java baseline.

**Convention adherence — pass across the board.** Spring Boot ships autoconfiguration and a well-known layout; Vaadin Flow is strongly convention-based too (`@Route`-registered views, a server-side component tree, themes under `frontend/themes/`). The project's own package layout (`api`, `components`, `config`, `model`, `services`, `stores`, `util`, `views/{bigscreen,maestro,player}`) is predictable and role-segmented. Maven follows the standard lifecycle. An agent can predict where things live.

**Training-data density — the one real gap, on Vaadin Flow.** Assessed *within the Java family*: Spring Boot, Maven, and JUnit 5 are all top-tier and heavily represented. Vaadin Flow is a genuine, established framework but sits well behind Spring MVC / Thymeleaf / REST controllers in Java training-data volume — an agent has internalized far fewer Vaadin idioms than Spring ones. Compounding this: **Java 25, Spring Boot 4.0.1, and Vaadin 25.0.2 are all very recent releases** that postdate most training corpora, so even the well-known parts carry version-specific API drift (Spring Boot 4 / Spring Framework 7 baseline; Vaadin 25 component APIs). The Playwright + TestBench test layer is marked partial (~) for the same recency reason, not for being niche.

**Documentation — pass.** Vaadin (vaadin.com/docs), Spring Boot (versioned reference), Maven, JUnit, and Playwright all publish current, version-pinned, link-able official docs. The gap on Vaadin is training-data density, *not* documentation quality — which is precisely why the compensation path (point the agent at the docs, pin versions) works well here.

## Gaps & Compensation

The stack is strongly typed, consistently convention-based, well-documented, and already carries a real integration-test suite. The single meaningful agent-friendliness gap is **Vaadin Flow's lower training-data density within the Java family, amplified by bleeding-edge versions (Java 25 / Spring Boot 4 / Vaadin 25)**.

Why this matters *for this specific change*: the PRD's committed work is maestro accounts + tenant isolation (FR-001…FR-006) layered onto a Vaadin app, plus a hard guardrail that the real-time three-view sync must not regress. Both of those land squarely in the two areas where an agent has the least internalized Vaadin knowledge — **Vaadin's route-level security model** and **Vaadin's server-push / `UI.access()` threading model**. Left uncompensated, the agent is most likely to confabulate exactly on the auth wiring and the live-sync code that the PRD flags as highest-risk. The compensation below front-loads instruction-file context for precisely those surfaces.

### Recommended Instruction File Additions

Paste these into `CLAUDE.md` (this project already has one). They are written against the code and the PRD's committed scope.

```markdown
## Stack versions (newer than typical training data — verify against docs, don't assume)

- Java 25, Spring Boot 4.0.1 (Spring Framework 7 baseline), Vaadin Flow 25.0.2, Maven.
- These postdate most training data. When an API looks unfamiliar or you're unsure of a
  signature, check the official docs rather than pattern-matching an older version:
  - Vaadin Flow 25: https://vaadin.com/docs/latest
  - Spring Boot 4.0: https://docs.spring.io/spring-boot/index.html
- Do NOT introduce `javax.*` imports — this codebase is on `jakarta.*`.
```

```markdown
## Vaadin Flow conventions (this is a server-side Vaadin app, not a REST/JS-frontend app)

- UI is built in Java. Views are classes under `com.github.mjjaniec.lmq.views`, registered
  with `@Route`. There is no separate REST controller layer or JS frontend for these screens.
- The three synchronized surfaces map to packages: `views/bigscreen`, `views/maestro`,
  `views/player`. Keep new views in the package that matches their surface.
- Real-time cross-view updates use Vaadin server push. To update a UI from a background /
  non-UI thread, wrap the mutation in `ui.access(() -> ...)` — never touch a component from
  outside its UI's lock. This is load-bearing for the live-sync guardrail; breaking it
  introduces lag or lost updates the PRD explicitly forbids.
- Prefer Vaadin's own components (`com.vaadin.flow.component.*`) and layouts over hand-rolled
  HTML. Themes live under `src/main/frontend/themes/live-music-quiz`.
```

```markdown
## Auth & route security (the FR-001…FR-003 work — Vaadin-specific, not plain Spring MVC)

- Secure routes with Vaadin's security model, not URL-pattern matching. Extend
  `VaadinWebSecurity` and use view-level annotations:
  - `@AnonymousAllowed` on the public surfaces: big-screen, player join, and (if built) the
    performer view. Anonymous QR join MUST stay frictionless (PRD guardrail).
  - `@PermitAll` / `@RolesAllowed("MAESTRO")` on maestro control views only.
  - Configure the login view via `setLoginView(...)`; unauthenticated hits on a maestro-gated
    route redirect to sign-in (FR-003).
- Auth intent (PRD): stable external identity, no-new-password preferred; the account is keyed
  to an external identity, not an email alone. Keep the concrete provider swappable.
- Secrets are never stored or transmitted in the clear; sessions expire (PRD baseline).
```

```markdown
## Tenant isolation (FR-006 — the primary success criterion)

- Every game and set-list is owned by a maestro. Enforce the owner boundary CENTRALLY, in one
  place, and re-check it on every id-access path — do not rely on per-query filtering scattered
  across views. A guessable/enumerable id must never expose another maestro's data (no IDOR).
- Use unguessable identifiers for game/set-list ids that appear in URLs or QR codes, so
  anonymous join links can't be used to browse other maestros' games.
```

```markdown
## Build & formatting

- Format with Spotless before committing: `./mvnw spotless:apply`. NOTE: `pom.xml` points
  Spotless at `eclipse-formatter.xml`, which is not currently present in the repo root —
  restore or regenerate it, otherwise `spotless:check` (and CI) will fail.
- Run the full check the way CI does: `./mvnw verify -Pit -Pproduction`.
- Lombok is an annotation processor — regenerated getters/setters/builders won't appear in
  source; don't hand-write what an annotation already provides.
```

## Summary

**Overall: ready-with-compensation.** This is a well-built, agent-friendly Java stack. It passes on type safety (statically typed *plus* JSpecify null-marking), convention adherence (Spring Boot + Vaadin + a clean role-segmented package layout), documentation, and it already carries a Playwright/TestBench integration suite that directly protects the PRD's "must not regress" guardrails.

**Key strengths:** explicit types and nullability contracts; strong conventions an agent can navigate; excellent existing test coverage of the live game flow; Spring Boot fluency for the bulk of the new backend work.

**Key gap:** Vaadin Flow's thinner training-data density within the Java family, amplified by the very recent Java 25 / Spring Boot 4 / Vaadin 25 versions. This concentrates risk on exactly the surfaces the change touches — Vaadin route security (auth) and Vaadin server-push threading (live sync). The gap is *documentation-rich and therefore compensable*: the instruction-file entries above pin versions and encode the Vaadin auth + push idioms so the agent stops confabulating where it's weakest. Also flagged: the missing `eclipse-formatter.xml` referenced by Spotless.

**Recommended next step:** run `/10x-health-check` to focus a deeper pass on the identified gaps (Vaadin auth wiring, tenant-scoping enforcement, the Spotless config gap).
