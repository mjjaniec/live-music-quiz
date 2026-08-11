# AGENTS.md

This file provides guidance to Coding agents like Claude Code when working with code in this repository.

## Project overview

LMQ (Live Music Quiz) is a Spring Boot + Vaadin Flow application for running a live "name that tune" quiz across three synchronized surfaces, each a separate browser session hitting the same running server:

- **maestro** (`views/maestro`, routes under `/maestro`) — the quiz host's control panel: picks the set, advances stages, resolves answers.
- **big-screen** (`views/bigscreen`, routes under `/big-screen`) — the shared screen all players watch (invite QR, listening, reveal, results).
- **player** (`views/player`, routes under `/player`) — each player's phone view (join, answer, wait).

## Common commands

```shell
./mvnw spring-boot:run                           # run locally (default goal)
./mvnw clean package -Pproduction                # production build (builds Vaadin frontend too)
java -Dspring.profiles.active=local -jar target/live-music-quiz-1.0-SNAPSHOT.jar 

./mvnw spotless:check                            # verify formatting (palantir-java-format)
./mvnw spotless:apply                            # auto-format

./mvnw test                                      # unit tests only (MainSetParserTest, PointsCounterTest)
./mvnw test -Dtest=PointsCounterTest             # single unit test class
./mvnw verify -Pit -Pproduction                  # full CI-equivalent: starts the app, runs Playwright/TestBench *IT tests, stops it
```

- Requires a local PostgreSQL database. Copy `src/main/resources/application-local.properties.example` to `application-local.properties` (gitignored) and fill in `spring.datasource.*`, or set `db_url`/`db_user`/`db_pass` env vars consumed by `application.properties`.
- CI (`.github/workflows/maven-it.yml`) runs, in order: `spotless:check`, a Trivy dependency vulnerability scan, Playwright browser install, then `verify -Pit -Pproduction`. Match this locally before pushing.
- Integration tests (`*IT` suffix) are real Playwright browser sessions against a running server on the `it` profile — expect them to be slower and to open multiple browser contexts (one per simulated maestro/big-screen/player) per test.

## Architecture

**Package layout is role-segmented and mirrors the three surfaces:**
- `views/{maestro,bigscreen,player}` — Vaadin `@Route` views, one class per screen. Nested routes attach to a parent layout view (`layout = MaestroView.class`, etc.) via Vaadin's `@Route(value=..., layout=...)`.
- `services` — game logic and cross-view coordination (`GameService`/`MaestroInterface`, `Navigator`, `BroadcastAttach`).
- `stores` — persistence, split into a plain interface (e.g. `PlayerStore`) plus a Spring Data `JpaXxxStore extends CrudRepository<XxxDto, ID>` implementation and a `XxxDto` JPA entity. Not every store is JPA-backed (`RealStageStore` is a hand-written `@Component`).
- `model` — domain types, mostly immutable records (`MainSet`, `Answer`, `Player`) plus `MainSetParser`/`SpreadsheetLoader` for importing quiz content from a Google Sheets CSV export.
- `components` — reusable Vaadin UI building blocks shared across views.
- `util` — `TestId.testId(component, id)` sets `data-testid` for Playwright selectors (`page.getByTestId(...)`); `LocalStorage` persists the player's identity across page loads via browser localStorage.

**Game state machine — `GameStage` (`model/GameStage.java`):** a sealed interface (`Invite`, `RoundInit`, `RoundPiece`, `RoundSummary`, `PlayOff`, `WrapUp`) is the single source of truth for "what's happening right now." Each variant declares its own `playerView()`/`bigScreenView()` Vaadin route classes — that mapping is *how* the maestro's stage transitions drive navigation on the other two surfaces. When adding a new stage or sub-stage, add the routing decision inside the `GameStage` variant, not in the views.

**Cross-view live sync — server push, not polling:** all three surfaces are kept in sync via Vaadin server push (`@Push` on `LiveMusicQuizApp`). Views register themselves with `BroadcastAttach` on attach and deregister on detach; `Navigator`/`BroadcastAttachImpl` then fan out `ui.access(() -> ...)` calls to every registered UI to navigate or re-run a `refresh` callback. Any state change that should be visible elsewhere must go through this attach/broadcast path — mutating shared state without calling the matching `Navigator`/`BroadcastAttach` method will leave other surfaces stale. Never touch a `UI`/component from outside `ui.access(...)`.

**No auth/route security is wired yet.** Routes are plain `@Route`, with no `@AnonymousAllowed`/`@RolesAllowed`/`VaadinWebSecurity`. This is in-progress brownfield work (see `context/foundation/prd.md`); don't assume any tenant/session isolation exists between maestro instances today.

**Content import:** `MainSetParser` + `SpreadsheetLoader` turn a Google Sheets CSV export into a `MainSet` (rounds/pieces with artist/title/alternatives/tempo/hints). `tools/Hot100Parser` is a standalone offline data-prep utility, unrelated to the running app.

**Null-safety:** every package has a `package-info.java` with `@org.jspecify.annotations.NullMarked`

**UI text is Polish** (player-facing strings, e.g. "grają z nami") — match existing copy's language and tone when adding UI text, don't default to English.
