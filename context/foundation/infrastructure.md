---
project: live-music-quiz
researched_at: 2026-08-12
recommended_platform: Railway
runner_up: Render
context_type: mvp
tech_stack:
  language: Java 25
  framework: Spring Boot 4.0.1 + Vaadin Flow 25.0.2
  runtime: Docker (eclipse-temurin:25), long-running JVM process with persistent server-push connections
---

## Recommendation

**Deploy on Railway.**

Railway scored a clean Pass on all five agent-friendly criteria — GA CLI (`railway up`/`logs`/`redeploy`/`variables`), markdown-native docs with a full-site `llms-full.txt` dump, a scriptable non-interactive deploy flow, and an official MCP server plus Claude Code plugin. Weighted against the interview answers, it also wins on cost: a small always-on service plus co-located Postgres lands around $10–25/mo, well under Fly.io's Postgres-inflated ~$40–44/mo and AWS's ~$45–65/mo (EC2 + RDS + ALB). AWS's existing familiarity and co-location preference didn't overturn its placement — it is simultaneously the most expensive candidate and the weakest on agent-readability (HTML-only docs, interactive-heavy EB CLI, RDS isn't a one-click co-located add-on the way Railway's/Render's/Fly's Postgres templates are).

## Platform Comparison

Hard filters applied first: this app is a long-running JVM monolith (Docker image `FROM eclipse-temurin:25`) requiring a persistent per-session WebSocket connection (Vaadin `@Push`) for real-time sync across three simultaneous browser surfaces (maestro / big-screen / player). Cloudflare Workers/Pages, Vercel, and Netlify were dropped before scoring — see Out of Scope note below the table.

| Platform | CLI-first | Managed/Serverless | Agent-readable docs | Stable deploy API | MCP/Integration | Total |
|---|---|---|---|---|---|---|
| **Railway** | Pass | Pass | Pass | Pass | Pass | 5 Pass |
| **Render** | Partial | Pass | Pass | Partial | Pass | 3 Pass / 2 Partial |
| **Fly.io** | Pass | Partial | Pass | Pass | Partial | 3 Pass / 2 Partial |
| AWS (Elastic Beanstalk / ECS Express Mode) | Partial | Partial | Fail | Partial | Pass | 1 Pass / 3 Partial / 1 Fail |
| Cloudflare Workers/Pages | — | — | — | — | — | Hard-filtered (no JVM runtime) |
| Vercel | — | — | — | — | — | Hard-filtered (function-scoped execution incompatible with always-on `@Push`) |
| Netlify | — | — | — | — | — | Hard-filtered (no container/JVM path; functions cannot hold a WebSocket open) |

**Railway** — `railway up`/`logs`/`redeploy`/`variables` are all GA, non-interactive, and scriptable; docs are markdown-native (`.md` suffix or `llms-full.txt`); Postgres ships as an official co-located template; an official MCP server (local CLI-backed or remote OAuth) plus a Claude Code plugin gives structured agent access. Only real gap: `railway redeploy` restores the prior image+variables but there's no staged/canary rollback, and WebSocket idle-timeout behavior is community-reported (not vendor-documented).

**Render** — Docker web services are first-class GA; docs are markdown-native with `llms.txt`; Postgres is co-located and simple. Loses points on CLI (no dedicated `render rollback` subcommand — dashboard/API only) and has no native JVM buildpack (Docker-only, slightly longer builds). Free tier's 15-minute idle spin-down is incompatible with the persistent-connection requirement, so pricing must start from the paid Starter tier.

**Fly.io** — WebSocket support is the most explicitly documented of the three (dedicated blog post, zero special config, TLS terminated at the edge) and `fly.io/llms.txt` plus Markdown content-negotiation make docs agent-readable. The blocker is Postgres: the original unmanaged offering and the Supabase-managed variant are both deprecated, and the current Fly Managed Postgres has a $38/mo floor with security patching and migration tooling explicitly flagged "under development" — a meaningfully less mature product than Railway's or Render's Postgres offering, and the dominant cost driver.

**AWS (Elastic Beanstalk / ECS Express Mode)** — Note: App Runner, the historical "simple container PaaS" AWS option, stopped accepting new customers as of April 30, 2026 and is not a viable candidate today; AWS itself redirects new users to ECS Express Mode. Elastic Beanstalk supports Docker/JVM natively and provisions an ALB (WebSocket-compatible, though ALB idle-timeout needs raising past 60s and Vaadin push has a documented history of needing manual heartbeat/timeout tuning behind Apache/ALB). RDS Postgres is not "co-located" in the one-click sense the interview favored — it requires manual VPC/security-group wiring. Docs are HTML-only with no markdown/llms.txt path. Realistic all-in cost (EC2 + RDS + ALB) is ~$45–65/mo, the most expensive of all viable candidates.

### Shortlisted Platforms

#### 1. Railway (Recommended)

Wins on every one of the five criteria and is the cheapest reliable total (~$10–25/mo including co-located Postgres). The one thing keeping it from a "no caveats" recommendation is that its WebSocket longevity claims are backed by community reports rather than a documented SLA — addressed in the risk register below with a concrete mitigation (pin the existing Dockerfile; add an app-level push heartbeat).

#### 2. Render

Simpler, flatter pricing (~$13–14/mo) and the same co-located-Postgres convenience, but weaker deploy tooling (rollback is dashboard/API-only, no CLI subcommand) and Docker-only JVM builds (slightly longer build times, no native buildpack shortcut). The safer fallback if Railway's WebSocket-stability risk turns out to be a blocker in practice.

#### 3. Fly.io

Best-documented WebSocket story of the three, but Postgres is the load-bearing risk: product churn (two prior offerings deprecated), a $38/mo floor, and "under development" ops tooling push the realistic total to ~$40–44/mo — the most expensive of the three PaaS candidates, working against the stated cost-minimize priority.

## Anti-Bias Cross-Check: Railway

### Devil's Advocate — Weaknesses

1. **WebSocket stability is community-flagged, not vendor-guaranteed.** Multiple 2026 forum threads report Railway's edge proxy silently dropping WebSocket connections at ~10min/50min/60s intervals despite docs claiming exemption from inactivity timeouts. A live event can run 1-2+ hours with `@Push` held open the whole time — an undocumented proxy-level drop directly threatens the PRD's "no perceptible lag" guardrail, and the standard mitigation (app-level ping every 10-30s) isn't wired into Vaadin's push transport by default.
2. **Usage-based billing is unpredictable for a spiky workload.** This app is quiet most of the month with a burst of concurrent connections during a live event; usage billing (CPU-seconds, RAM-seconds, egress) is a mismatch for that pattern versus a flat-rate always-on tier, and a single well-attended event could spike a bill unpredictably.
3. **No dedicated rollback safety net beyond redeploy-and-hope.** `railway redeploy` restores the prior image+variables but doesn't undo a schema migration a bad deploy already ran (this app uses JPA/Hibernate against Postgres).
4. **Nixpacks/Railpack auto-detection is a black box for this specific build.** Railway's Java auto-detection defaults to JDK 21, not this project's pinned Java 25; the project already has a working multi-stage Dockerfile, and it must be used explicitly or the build silently diverges.
5. **Public domain isn't automatic.** `railway domain` must be run explicitly to expose the big-screen/player-facing routes; forgetting this step breaks the anonymous QR-join flow, a hard PRD guardrail.

### Pre-Mortem — How This Could Fail

The team deployed the Spring Boot + Vaadin app on Railway using auto-detected Railpack instead of the existing Dockerfile, assuming "it just builds Java projects." It picked JDK 21 instead of the pinned Java 25, and a JSpecify-related annotation processor step behaved differently, silently changing null-handling at runtime without failing CI. Nobody noticed for weeks because local testing always used the Dockerfile path. Then, during a real event with 40 players, Railway's edge proxy dropped several players' WebSocket connections around the 50-minute mark mid-round — the team had trusted the docs' "no inactivity timeout" claim and never wired a heartbeat. Big-screen and player views desynced silently; the maestro had no visibility into which players had disconnected. That same month, usage-based billing spiked because three back-to-back events pushed CPU/RAM-seconds far past the team's mental model of "small app, low cost," and the invoice arrived 3x higher than budgeted. The postmortem: they trusted the PaaS's defaults for both build and network layer instead of pinning the Dockerfile and load-testing WebSocket longevity at real event length beforehand.

### Unknown Unknowns

- Railway doesn't publish a documented WebSocket idle-timeout number anywhere — the "gotcha" is entirely from community reports, so there is no SLA to hold them to if it happens during a real event.
- Postgres backups are explicitly labeled "still under development" in Railway's own docs — no confirmed automated backup/restore story today, which matters once games start persisting per the PRD's FR-004 (post-event review data).
- Egress from the public TCP proxy for Postgres is billed separately and isn't obvious from the base pricing page — direct DB connections from tooling outside the app can quietly add cost.
- Railpack's JDK version defaults can silently diverge from the pinned Java 25 unless a Dockerfile is explicitly forced — exactly the kind of drift an agent deploying "the standard way" could introduce without noticing.
- Region choice is effectively permanent (4 regions total, no documented in-place region migration) — only redeploy-to-new-region if the audience's geography shifts later.

**Decision:** proceed with Railway; risks absorbed into the register below with concrete mitigations (pin the existing Dockerfile explicitly, add a Vaadin push heartbeat, monitor first-month usage billing closely).

## Operational Story

- **Preview deploys**: Railway creates a PR-linked environment automatically when "PR environments" are enabled per-service in project settings; each gets its own generated domain. No additional access-control layer is applied by default — treat preview URLs as semi-public unless a variable-gated auth check is added.
- **Secrets**: env vars (`db_url`, `db_user`, `db_pass` — already the exact names `application.properties` expects) live in Railway's project-level Variables store, set via `railway variables --set KEY=VALUE` or the dashboard; readable by anyone with project access. Rotation = update the variable, which triggers a redeploy.
- **Rollback**: `railway redeploy` restores the previous build image and its variable snapshot; typical time-to-revert is a normal deploy cycle (build-cached, so usually under a minute). Caveat: does not reverse a Hibernate/JPA schema migration a bad deploy already applied — a forward-fixing migration is needed in that case, not a rollback.
- **Approval**: an agent may run `railway up`, `railway logs`, `railway variables list`, and `railway redeploy` unattended for routine iteration. Human-only: setting the initial `railway domain`, first-time Postgres provisioning, and any production database credential rotation.
- **Logs**: `railway logs` (add `--build` for build-time logs) tails runtime logs directly in the terminal; the official MCP server also exposes structured log/deploy queries for agents that prefer JSON over CLI text.

## Risk Register

| Risk | Source | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| WebSocket proxy silently drops `@Push` connection mid-event (undocumented idle-timeout) | Devil's advocate | M | H | Add an app-level Vaadin push heartbeat/ping; load-test a held-open connection for a full simulated event length (1-2h) before the first real event |
| Railpack/Nixpacks auto-detects JDK 21 instead of pinned Java 25, silently diverging build | Devil's advocate | M | M | Force the existing multi-stage Dockerfile explicitly in Railway service settings; do not rely on auto-detection |
| Usage-based billing spikes unpredictably on event-heavy months | Devil's advocate / Pre-mortem | M | M | Set a Railway usage alert/budget cap; review the first month's invoice against a dry-run event before scaling to multiple events/month |
| No automated Postgres rollback for a bad schema migration | Devil's advocate | L | H | Take a manual `pg_dump` snapshot before any deploy that includes a schema migration, until Railway's native backup feature exits "under development" |
| Forgetting `railway domain` breaks the public QR-join flow (PRD guardrail) | Devil's advocate | L | H | Add domain verification to the deploy checklist; smoke-test the public join URL after every first-time environment setup |
| Postgres backup/restore feature is explicitly "under development" | Unknown unknowns / Research finding | M | H | Do not treat Railway's backup feature as sufficient alone; keep the manual snapshot habit above until the feature reaches GA |
| Egress cost from direct/public Postgres TCP proxy access | Unknown unknowns | L | L | Always connect through the app in production; if a human needs direct DB access, use `railway connect`/SSH tunnel rather than the public proxy |
| Region choice has no documented in-place migration path | Unknown unknowns | L | M | Confirm target audience geography (PRD says single-region is fine) before first deploy; treat a later region move as a full redeploy, not a config change |

## Getting Started

1. Install the CLI: `npm i -g @railway/cli` (or `brew install railway`), then `railway login`.
2. From the repo root, `railway init` to create a new Railway project (or `railway link` if a project already exists).
3. In the service settings, explicitly select **Dockerfile** as the build method (not Railpack auto-detect) so the pinned Java 25 multi-stage `Dockerfile` at the repo root is used as-is — do not let auto-detection pick a JDK version.
4. Add a Postgres database from the Railway template picker (co-located in the same project); it auto-injects `DATABASE_URL`-style variables — map these to the `db_url`/`db_user`/`db_pass` variables `application.properties` already expects via `railway variables --set`.
5. Run `railway up` to deploy, then `railway domain` to generate the public URL the big-screen QR code and player join links will point to; verify the anonymous join path end-to-end before the first real event.

## Out of Scope

The following were not evaluated in this research:
- Docker image configuration (the project's existing multi-stage `Dockerfile` is reused as-is, not redesigned)
- CI/CD pipeline setup (GitHub Actions workflow already runs `verify -Pit -Pproduction`; wiring it to auto-deploy on Railway is a follow-up, not part of this decision)
- Production-scale architecture (multi-region, HA, DR) — explicitly out of scope per the PRD's single-region, small-scale MVP framing
- Cloudflare Containers (public beta) as a re-opened candidate — WebSocket routing from the front-door Durable Object into the container isn't documented; flagged as unverified rather than researched further, since Railway already cleared the bar without needing a beta product
