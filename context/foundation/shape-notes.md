---
project: "Live Music Quiz"
context_type: brownfield
created: 2026-07-11
updated: 2026-07-11
product_type: web-app
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  delivery_weeks: 3
  hard_deadline: null
  after_hours_only: true
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: "must-preserve behavior"
      decision: "anonymous player join, three-view model, live in-event flow must not regress"
    - topic: "change type"
      decision: "architectural improvement — auth + account/game scoping wrapping the existing engine"
    - topic: "why now"
      decision: "moving toward a hosted/public deployment; global shared state is now a liability"
    - topic: "role model"
      decision: "flat — one authenticated Maestro; Player and Performer stay anonymous via game-scoped links"
    - topic: "performer view"
      decision: "nice-to-have; anonymous access via privately-shared link; in scope only if time remains"
    - topic: "maestro auth method"
      decision: "provider-agnostic; stable external identity, no-new-password preferred; concrete provider deferred downstream"
    - topic: "MVP slice"
      decision: "accounts + game/set-list scoping + tenant isolation; performer view excluded (nice-to-have)"
    - topic: "DB backward compatibility"
      decision: "NOT a concern — occasional use; update between quizzes; existing global rows can be reset (no migration/backfill/rollback)"
    - topic: "delivery timeline"
      decision: "fits ~3 weeks after-hours"
    - topic: "set-list ownership"
      decision: "reusable owned library asset; a game references a set-list; ownership sits on the set-list"
    - topic: "isolation strength"
      decision: "strict/centralized enforcement; no guessable-id (IDOR) leakage"
    - topic: "game persistence"
      decision: "games are persisted owned entities; retain player answers/feedback for post-event set-list refinement (analysis UI out of MVP)"
    - topic: "google sheets set-list"
      decision: "(re)imported before game start, editable in sheet anytime; content stays external only to limit scope"
  frs_drafted: 11
  quality_check_status: accepted
---

# Live Music Quiz — Shape Notes (brownfield)

<!-- Sections are written as discovery phases complete. Body order anticipates
     the 11 brownfield PRD sections defined in references/prd-schema.md. -->

## Current System

**Purpose (one sentence):** A web service that runs an in-person live-music quiz — a maestro (host) leads a game while a performer plays music excerpts live and players guess the artist and title from their phones.

**Key architecture:** Single-deployable server-rendered monolith. Three synchronized real-time views driven by one backend:
- **maestro-screen** — private control surface for the host to run the game.
- **big-screen** — public display (QR join code, per-question "who answered", answer reveals, round rankings, final results).
- **players' phones** — join via QR, submit answers, see their own points/feedback.

**Tech stack:** Java 25, Spring Boot, Vaadin (Flow) UI, Spring Data JPA, PostgreSQL (prod) + H2 (dev/test), Playwright (e2e tests). Set-lists are read from a Google spreadsheet (no in-app content management). Containerized (Dockerfile) with GitHub Actions CI.

**Current user base:** Single-maestro / personal use. Small in-event scale (one maestro + one performer + a room of players per event).

**Core functionality (today):**
- Maestro prepares a **set-list** (3–6 rounds, each 4–12 music pieces) read from a Google spreadsheet; optional short **test-setlist** to teach players the mechanics.
- Live event flow: players scan QR on big-screen to join → maestro runs rounds → per question the performer plays an excerpt and players answer → maestro reveals answers → rankings shown per round → **play-off** tie-breaker (players estimate the number of notes played) → final results & awards.
- **Three round modes:** *everybody* (fuzzy-hinted Artist/Title form, per-field scoring), *onion* (same, but earlier correct answers score higher as musical layers are added), *first* (buzzer — first player to buzz answers aloud; maestro judges; wrong answers increase the point pool for next players to buzz).

**Current limitation driving this change:** Everything is **global** — one shared, unscoped state for all games and set-lists. There is no user account and no access control.

## Problem Statement & Motivation

The system was built for a single maestro running events locally, so global shared state was acceptable. The trigger for change is **moving toward a hosted/public deployment**: once the service is reachable by more than one host, global state becomes a liability — any user can see and clobber the single shared game and set-list. There is no ownership boundary.

**The gap:** no concept of "this game / this set-list belongs to this maestro." To be safely hosted, the app needs maestro accounts and state scoped to those accounts.

**Why non-obvious:** the quiz engine and its real-time three-view sync are the hard, working part of the app; the change is not about the game mechanics but about wrapping the existing engine in ownership/isolation boundaries without disturbing the live event flow or the frictionless anonymous player experience.

## User & Persona

**Primary persona — the Maestro (host).** The person who prepares the set-list, organizes the event, and runs the game from the maestro-screen. They are the only actor who will get an account (registration + login). The change exists to serve them: to let them own and isolate their games and set-lists in a hosted environment.

**Secondary persona — the Performer.** Plays the music live; may be the same person as the maestro. Today they read the maestro-screen to know what to play. This change *optionally* gives them a dedicated performer view.

**Secondary persona — the Player.** A guest at the event who joins anonymously by scanning a QR code on their phone. **Players remain account-free** — no login, no install. Their experience must not gain any friction from this change.

## Access Control Changes

**Current model:** none. Everything is global and unauthenticated — any visitor reaches the single shared game and set-list.

**Change:** introduce a single authenticated role — the **Maestro** — and keep everyone else anonymous.

- **Maestro** — the only account. Registers and logs in, then owns and sees only their own games and set-lists. An unauthenticated visitor hitting a maestro-gated route is sent to sign-in.
- **Player** — remains anonymous. Joins a specific game via the public QR code shown on the big-screen. No account, no login, no install. (Preserved behavior.)
- **Performer** — remains anonymous. Reaches the (nice-to-have) performer view via a link the maestro shares **privately** — same anonymous-link mechanism as players, but the link is not shown publicly on the big-screen.

**Auth method — product intent (provider-agnostic):** the maestro authenticates via a **stable external identity with no new password preferred** ("don't make the host create yet-another-account"). The concrete mechanism (OAuth/Google vs. local email+password vs. passwordless) is deliberately deferred to downstream stack/implementation planning. To keep that open, the Maestro account record should be keyed on a **stable external identifier** (swappable per provider), not on email alone.

**Role model:** flat — one authenticated role (Maestro) plus two anonymous, link-scoped guest surfaces (Player, Performer). No admin/multi-role hierarchy in scope.

## Success Criteria

### Primary
- A maestro can register/log in, create a game that belongs to their account, attach a set-list to it, and run that game end-to-end (rounds → reveals → rankings → play-off → final results) exactly as today.
- **Tenant isolation holds:** a second maestro logging in sees only their own games and set-lists, never another maestro's. This is the proof that the hosted deployment is safe.

### Secondary
- (Nice-to-have, only if time remains) A dedicated performer view, reached anonymously via a link the maestro shares privately.

### Guardrails (must not regress)
- **Live in-event flow & real-time sync** — the maestro-screen ↔ big-screen ↔ player-phone synchronization and in-flight game state must keep working through a live event; scoping must not break the active-game lookup mid-quiz.
- **Anonymous player join** — players still join a game by scanning the public QR on the big-screen, with no account, login, or install. Player join UX gains zero friction.
- **QR join / player entry routing** — once games are per-account, the join link / game-code scheme must still land each player in the correct game.
- **Set-list loading (Google Sheets)** — reading a set-list from a Google spreadsheet keeps working when set-lists become game/account-scoped (exact behavior confirmed in Scope of Change).
- **Three-view model** — maestro-screen / big-screen / players remain distinct synchronized surfaces; the change does not collapse or restructure them.

## Functional Requirements

<!-- Brownfield: each FR carries a Change tag (new | modified | preserved).
     /10x-prd maps these into the PRD's ## Scope of Change section. -->

### Accounts & Access
- FR-001: Maestro can register an account. Priority: must-have. Change: new
  > Socrates: Considered invite-only / pre-provisioned accounts (smaller surface). Resolution: kept open self-service registration — the point of going hosted is to let arbitrary maestros use it.
- FR-002: Maestro can log in and log out. Priority: must-have. Change: new
  > Socrates: Considered dropping explicit logout for MVP. Resolution: kept — small feature, makes the app feel complete and significantly simplifies user testing (switching maestros).
- FR-003: An unauthenticated visitor to a **maestro-gated route** is redirected to sign-in; player and performer join links stay public. Priority: must-have. Change: new
  > Socrates: Risk that too-broad gating blocks anonymous QR join. Resolution: gate is scoped to maestro control routes only; player/performer surfaces remain public.

### Scoping & Isolation
- FR-004: Maestro can create a game that belongs to their account; games are persisted and reviewable after the event. Priority: must-have. Change: modified
  > Socrates: Considered games as ephemeral runs of a set-list. Resolution: games are persisted, owned entities — they retain player answers and feedback so the maestro can refine future set-lists and see which pieces were too hard/too easy. (Post-event analysis UI itself is out of MVP scope — see Non-Goals — but the data is captured.)
- FR-005: Maestro can attach a set-list to a game; set-lists are reusable library assets owned by the maestro and can be attached to multiple games over time. Priority: must-have. Change: modified
  > Socrates: Considered set-list-per-game (literal "scope set-list to a game"). Resolution: set-list is a reusable owned asset; a game references one. Ownership sits on the set-list.
- FR-006: Maestro sees and can operate only their own games and set-lists (tenant isolation), enforced centrally so it cannot be bypassed by a guessable id. Priority: must-have. Change: new
  > Socrates: Considered logical scoping with light per-query enforcement. Resolution: strict/centralized enforcement — owner boundary applied in one place and re-checked on every id-access path (no IDOR). This is the primary success criterion; light scoping would leave the very leak the change exists to close.

### Preserved live-event flow
- FR-007: Maestro can run a game end-to-end (rounds in all 3 modes → reveals → per-round rankings → play-off → final results), behaving identically to today once inside a game. Priority: must-have. Change: preserved
  > Socrates: Must it be byte-for-byte identical? Resolution: the in-event experience is preserved, but a "pick which game to run" step before the live flow starts is acceptable (games are now owned entities).
- FR-008: Player can join a game anonymously by scanning the public QR on the big-screen, landing directly in the correct game. Priority: must-have. Change: preserved
  > Socrates: Per-account games risk requiring players to identify a game. Resolution: the big-screen QR encodes the game identifier, so players land directly in the right game with zero added steps.
- FR-009: Player can submit answers and see their points on their phone. Priority: must-have. Change: preserved
  > Socrates: Games now persist, so player answers are retained. Resolution: the live player experience is unchanged; retention is backend-only (answers stored under the owning game for the maestro's later review).
- FR-010: Maestro can (re)import a set-list from a Google spreadsheet; the set-list is editable in the spreadsheet at any time and re-imported before a game starts. Priority: must-have. Change: modified
  > Socrates: Live sheet read vs one-time import, given set-lists are now owned assets. Resolution: re-tagged from preserved to **modified** — set-list is (re)imported from the maestro's spreadsheet before a game; editable anytime in the sheet. Keeping content in Google Sheets (rather than building in-app set-list management) is a deliberate scope limit for the tight timeline, not a design conviction — see Non-Goals and Forward: technical-roadmap.

### Performer view (nice-to-have)
- FR-011: Performer can open a read-only "what to play" view via a link the maestro shares privately. Priority: nice-to-have. Change: new
  > Socrates: Confirm out of committed scope. Resolution: fully parked — attempted only if time remains after accounts + scoping + isolation land.

## User Stories

### US-01: Maestro runs an isolated, account-scoped game

- **Given** a maestro who has registered and logged in
- **When** they create a game under their account, attach a set-list to it, and run it live
- **Then** the game runs end-to-end exactly as before, and no other maestro can see or affect it — while players still join anonymously by QR

#### Acceptance Criteria
- A newly created game is owned by the creating maestro and appears only in that maestro's list.
- A second, distinct maestro logging in sees none of the first maestro's games or set-lists.
- The full live flow (all three round modes, reveals, rankings, play-off, final results) behaves identically to the pre-change behavior.
- Players join the correct game via the big-screen QR with no account and no added steps.

## Business Logic Changes

**No domain logic change. This is an architectural/technical change.** The existing scoring rules are preserved exactly:
- *everybody* mode — Artist and Title checked as separate fields; points added per correct field.
- *onion* mode — same per-field checking, but earlier correct answers score more and later ones score progressively less (mirrors the layered performance).
- *first* mode — buzzer-based; each incorrect answer increase the point pool available for the next player on that piece, but only first correct answer gives points.
- *play-off* — estimation tie-breaker (players guess the number of notes played).

The change adds ownership and isolation around games and set-lists; it introduces no new scoring rule and modifies none. Note: persisted games now retain player answers and feedback, but this is **data capture**, not a new domain rule — any post-event difficulty analysis is out of MVP scope (see Non-Goals).

## Constraints & Compatibility

- **No data migration / backfill / rollback needed.** The app is used occasionally and updates are applied when no quiz is running. Existing global, unowned games/set-lists may be reset rather than preserved and assigned owners.
- **Deployment window:** updates land between events — no live-migration or zero-downtime requirement for the change itself.
- **Preserved integration:** Google Sheets set-list import continues to work (now as a per-maestro (re)import before a game — see FR-010).
- **Preserved contracts:** the anonymous player join path (QR → correct game) and the three-view synchronized model remain intact.

## Non-Functional Requirements

- **Live-sync responsiveness does not regress.** Real-time updates across maestro-screen, big-screen, and player phones remain as responsive through a live event as before the change; adding authentication and scoping introduces no perceptible lag in the in-event flow.
- **Maestro data confidentiality.** A maestro's games and set-lists are never observable by another maestro or by an anonymous visitor — not through the UI, a shared/guessable URL, or an enumerable identifier.
- **Anonymous join stays frictionless.** Scanning the big-screen QR places a player into the correct game with no account, no install, and no additional steps beyond today's flow.
- **Credential-handling hygiene (baseline).** Whatever authentication mechanism is chosen downstream, secrets are never stored or transmitted in the clear and sessions expire — a sensible security baseline, without specifying mechanism here.

## Non-Goals

Functional non-goals:
- **No in-app set-list management.** Set-list content stays in Google Sheets; no in-app editor/CRUD for pieces and rounds. Deliberate scope limit for the timeline — technically could live in-app later.
- **No post-event analytics UI.** Player answers and feedback are captured and stored under the owned game, but no dashboards or difficulty-analysis views ship in this MVP.
- **No player accounts / cross-event identity.** Players stay anonymous per game; no player login and no tracking a player across events.
- **No admin / multi-role hierarchy.** Flat model — one maestro role. No admin, no shared/team ownership of games or set-lists.
- **Performer view is not in committed scope.** It is a nice-to-have (FR-011), attempted only if time remains.

Non-functional non-goals:
- **No zero-downtime / live-migration guarantee.** Updates land between events; the change does not aim for in-flight upgrade safety.

## Forward: tech-stack (informational — not part of the PRD schema)

For the downstream stack-assessment / implementation step:
- **Auth mechanism is undecided.** Direction: stable external identity, no-new-password preferred; provider-agnostic. Candidates: OAuth/Google vs. local email+password vs. passwordless. See Open Questions #1–2 for the OAuth integration risks (Google Cloud consent verification, redirect URIs, adding Spring Security + `VaadinWebSecurity`, and the harder Playwright e2e story needing a dev-profile login bypass or stubbed provider).
- Existing stack (for the assessor): Java 25, Spring Boot, Vaadin (Flow), Spring Data JPA, PostgreSQL + H2, Playwright, Docker, GitHub Actions.

## Forward: technical-roadmap (informational — not part of the PRD schema)

Deferred beyond this MVP, in rough priority order:
1. Performer view (nice-to-have this cycle; else next).
2. In-app set-list management (replace/augment Google Sheets).
3. Post-event analytics — surface which pieces were too hard/easy from captured answers to refine future set-lists.

## Quality cross-check

Run at Phase 7. Result: **accepted** — all elements present, no gaps.

- Access Control: present.
- Business Logic: present (architectural-only change; no domain rule change — valid for brownfield).
- Project artifacts: present.
- Timeline-cost acknowledged: present (delivery_weeks 3 ≤ 3; no override needed).
- Non-Goals: present (6 entries).
- Preserved behavior: present (Constraints & Compatibility + preserved FRs 007–010).

## Open Questions

<!-- Running list; /10x-prd mirrors these into the PRD's ## Open Questions verbatim. -->
1. **Concrete auth mechanism for the maestro** — OAuth/Google vs. local email+password vs. passwordless. Owner: user (during stack/implementation planning). Direction locked: stable external identity, no-new-password preferred; provider deferred.
2. **OAuth integration risks (if OAuth is chosen)** — Google Cloud consent-screen publishing/verification for a public app; exact per-environment redirect URIs; adding Spring Security + `VaadinWebSecurity` to a Vaadin app; and a harder Playwright e2e story (real provider login can't be scripted → needs a dev-profile login bypass or stubbed provider). Owner: user (downstream).
3. ~~Does Google Sheets set-list import stay as-is once set-lists are scoped to a game/account?~~ **Resolved (Phase 4):** set-list is (re)imported from the maestro's Google spreadsheet before a game starts and is editable in the sheet anytime; content stays in Sheets deliberately, only to limit scope. See FR-010.
4. **How is the join URL / QR made to target a specific game while staying zero-friction and hard to enumerate?** — direction: QR encodes the game identifier. Owner: user (downstream design). Consider unguessable game identifiers so anonymous join can't be used to browse other maestros' games.
5. **Retention of anonymous players' answers.** — Games now persist player answers for post-event analysis. Confirm how long they are kept and whether any privacy notice is needed. Owner: user (downstream). (User judged the live experience unchanged; flagged here for completeness, not as a blocker.)
