---
project: live-music-quiz
checked_at: 2026-07-12T09:52:00Z
health_status: needs-attention
context_type: brownfield
language_family: java
stack_assessment_available: true
checks_run:
  - lockfile
  - dependency_audit
  - outdated_deps
  - test_runner
  - ci_cd
  - configuration
audit_findings:
  critical: 0
  high: 0
  moderate: 0
  low: 0
test_runner_detected: true
ci_provider: GitHub Actions
recommended_fixes: 6
---

## Dependency Health

### Lockfile

```
Status: missing (expected — Maven has no native lockfile mechanism)
Package manager: maven (mvnw wrapper, Maven 3.9.9)
```

Maven does not use a lockfile in the way npm/Cargo/pip do. Dependency versions here are
pinned three ways, which gives most of the reproducibility a lockfile would: the
`spring-boot-starter-parent` 4.0.1 BOM, the imported `vaadin-bom` 25.0.2, and explicit
`<version>` properties for the direct deps that sit outside those BOMs (postgresql 42.7.8,
lombok 1.18.42, jackson 2.20.1, guava 33.3.1-jre, playwright 1.49.0, jspecify 1.0.0).
Transitive versions are still resolved by Maven at build time rather than frozen. If you want
a true lock, the `io.github.chains-project:maven-lockfile` plugin or `dependency:tree` output
committed to the repo can pin the full transitive graph — optional, low priority.

### Security Audit

```
Tool: skipped — no built-in audit tool for java
Recommended external tool: OWASP Dependency-Check Maven plugin
  (`./mvnw org.owasp:dependency-check-maven:check`) and/or GitHub Dependabot alerts.
```

Java has no built-in equivalent of `npm audit` / `cargo audit`, so no vulnerability scan was
run against the dependency tree. This is a real blind spot for a project on very recent
releases (Spring Boot 4.0.1 / Vaadin 25.0.2) — no automated signal exists today for a
disclosed CVE in a transitive dependency. See Recommended Fixes #2 for the setup.

### Outdated Dependencies

```
Packages with major version gaps: not assessed
```

No staleness check was run (no built-in Maven equivalent). Observationally, the stack is on
bleeding-edge versions (Java 25, Spring Boot 4.0.1, Vaadin 25.0.2) rather than behind — the
risk here is *newness* (version-specific API drift the agent may not know), not staleness.
That risk is a training-data concern already documented in the stack assessment, not a
dependency-freshness problem.

## Test Suite

```
Test runner: JUnit 5 + Playwright + Vaadin TestBench
Tests found: 6 test classes (2 unit, 4 integration)
Test execution: not attempted
```

```
Configuration: pom.xml (spring-boot-starter-test, playwright 1.49.0, vaadin-testbench-junit5);
               `it` Maven profile wires Failsafe around a started Spring Boot instance
Framework: JUnit 5 (via spring-boot-starter-test), Vaadin TestBench, Microsoft Playwright 1.49.0
```

Detected classes:
- Unit: `MainSetParserTest`, `PointsCounterTest`
- Integration (`*IT`, browser-level): `SmokeIT`, `GameFlowIT`, `WrapUpGuiVerificationIT`,
  `WrapUpDataInjectionIT`

This is a genuine strength. A live-flow integration suite directly protects the PRD's
"must not regress" guardrail (FR-007…FR-009). A dry run was **not** executed on purpose:
`./mvnw verify -Pit` triggers a full dependency resolution plus a Playwright browser download
(multi-minute, network-heavy) and health-check is read-only. Runner detection is
high-confidence from `pom.xml` plus the present test sources. Verify locally with
`./mvnw verify -Pit -Pproduction` (the same command CI runs).

## CI/CD

```
Provider: GitHub Actions
Configuration: .github/workflows/maven-it.yml
```

| Stage      | Status | Notes                                                                 |
|------------|--------|-----------------------------------------------------------------------|
| Lint       | ✗      | no Spotless/format check step (`spotless:check` is not invoked)        |
| Test       | ✓      | `mvn verify -Pit -Pproduction` runs unit + integration tests          |
| Build      | ✓      | `-Pproduction` builds the Vaadin frontend and the production jar      |
| Type check | ✓      | covered by the Java compiler during the build (statically typed)      |
| Security   | ✗      | no dependency scan, no CodeQL, no Dependabot config                   |

The pipeline covers the load-bearing stages (build + full test) well. Two gaps: no
format/lint gate and no security scanning. One minor reproducibility note: the workflow calls
`mvn` directly rather than the pinned `./mvnw` wrapper, so CI uses whatever Maven `setup-java`
ships instead of the repo-pinned 3.9.9.

## Configuration

### Medium severity

- **eclipse-formatter.xml** — `pom.xml` points the Spotless plugin at
  `${project.basedir}/eclipse-formatter.xml`, but the file is absent from the repo root. Any
  invocation of Spotless (`./mvnw spotless:apply` or `spotless:check`) fails immediately with a
  missing-file error. CLAUDE.md/stack-assessment document `./mvnw spotless:apply` as the
  format command, so the documented workflow is broken. Note: Spotless has no `<execution>`
  binding in `pom.xml`, so `mvn verify` does *not* run it — CI is not currently red because of
  this, but the moment anyone (agent or human) runs the format command, it breaks.
  Fix: restore/regenerate the Eclipse formatter config at the repo root, or point Spotless at a
  bundled profile (e.g. `<eclipse><version>4.33</version></eclipse>` without `<file>`, or switch
  to `googleJavaFormat`/`palantirJavaFormat`).

### Low severity

- **.editorconfig** — no editor-agnostic formatting baseline. With Spotless currently
  non-functional (above), there is effectively no enforced whitespace/indent contract, so
  agent-generated code style may drift. Fix: add a minimal `.editorconfig` (Java: 4-space
  indent, LF, trailing-newline).
- **application-local.properties.example** — README documents that a local
  `application-local.properties` with PostgreSQL datasource credentials is required to run, but
  no committed template exists. Fix: commit an `application-local.properties.example` with
  placeholder `spring.datasource.*` keys (the `.env.example` equivalent for a Spring project).

Present and healthy: `.gitignore` (comprehensive, incl. Vaadin generated artifacts and
`error-screenshots/`), `.prettierrc.json` (frontend theme formatting), `CLAUDE.md`,
JSpecify `@NullMarked` `package-info.java` in `config`, `model`, `services`, `views/player`.

## Stack Assessment Cross-Reference

```
Stack assessment: context/foundation/stack-assessment.md
Agent readiness (from stack-assess): ready-with-compensation
```

| Quality Gate Gap                                   | Health-Check Finding                                                                 | Status      |
|----------------------------------------------------|----------------------------------------------------------------------------------------|-------------|
| Vaadin training-data density (recent versions)     | No security scan on a bleeding-edge Spring Boot 4 / Vaadin 25 tree                    | Reinforced  |
| Compensation = CLAUDE.md instruction-file entries  | Project CLAUDE.md holds only the 10xDevs lesson router; **none** of the recommended Vaadin/auth/tenant/Spotless entries are present | Reinforced (gap) |
| Spotless config gap (flagged in stack-assess)      | Confirmed — `eclipse-formatter.xml` missing; Spotless fails on invocation             | Reinforced  |
| Test coverage protects "must not regress"          | 4 browser-level `*IT` classes present and wired via the `it` profile                  | Mitigated   |
| Type/nullability contract (JSpecify)               | `@NullMarked` covers 4 packages but **not** `views/maestro` — where the FR-001…006 auth/tenant work lands | Partially reinforced |

The stack assessment gave `ready-with-compensation` on the condition that its instruction-file
entries (Vaadin push/`ui.access` threading, `VaadinWebSecurity` route security, tenant/IDOR
rules, version-pinning) get pasted into CLAUDE.md. **That compensation is not yet applied** —
the current CLAUDE.md contains only the bootstrapper lesson content. Building that content is
exactly what agent onboarding (M1L4) covers, so it is tracked as a Category B item below rather
than flagged as an alarm.

Also confirmed: no Spring Security wiring exists yet (`no VaadinWebSecurity / spring-boot-starter-security`),
which is expected — that is the FR-001…FR-003 work the PRD scopes, not a health defect.

## Recommended Fixes

### Fix before agent work (Category A)

### 1. Restore the missing Spotless formatter config

**Impact**: The documented format command (`./mvnw spotless:apply`) fails on a missing file, so
the agent has no working way to normalize the style of code it writes — output style will drift
and any Spotless call errors out.
**Severity**: medium
**Effort**: moderate (15–30 min)
**Fix**:

```bash
# Option A — bundle a standard profile, drop the external file reference in pom.xml:
#   <eclipse><version>4.33</version></eclipse>   (remove the <file> line)
# Option B — switch to a self-contained formatter (no external config):
#   replace the <eclipse>…</eclipse> block with <googleJavaFormat/> or <palantirJavaFormat/>
# Option C — regenerate/commit eclipse-formatter.xml at the repo root, then:
./mvnw spotless:apply
```

### 2. Add dependency vulnerability scanning

**Impact**: On very recent Spring Boot 4 / Vaadin 25 releases there is currently zero automated
signal for a disclosed CVE in a transitive dependency. An agent making dependency changes has no
guardrail against pulling in a vulnerable version.
**Severity**: medium
**Effort**: moderate (15–30 min)
**Fix**:

```bash
# One-off local scan:
./mvnw org.owasp:dependency-check-maven:check
# Ongoing: add .github/dependabot.yml with a maven ecosystem entry for weekly PRs.
```

### 3. Extend JSpecify @NullMarked to the change-surface packages

**Impact**: `@NullMarked` currently covers `config`, `model`, `services`, `views/player` — but
the committed work (maestro accounts + tenant isolation, FR-001…006) lands in `views/maestro`,
`api`, and `stores`, which have no nullability contract. The agent loses the null-safety signal
on exactly the code it is about to write.
**Severity**: low
**Effort**: moderate (15–30 min)
**Fix**: add `@NullMarked` `package-info.java` files to `views/maestro`, `api`, `stores`
(and `components`, `util`) mirroring the existing four.

### 4. Add an .editorconfig

**Impact**: With Spotless non-functional, there is no enforced style baseline; agent output
whitespace/indent may be inconsistent.
**Severity**: low
**Effort**: quick (< 5 min)
**Fix**: add a repo-root `.editorconfig` (Java: `indent_style=space`, `indent_size=4`,
`end_of_line=lf`, `insert_final_newline=true`).

### 5. Commit an application-local.properties.example

**Impact**: A required-to-run config (PostgreSQL datasource) is documented in prose but has no
template, so an agent (or new contributor) can't scaffold a working local config from the repo.
**Severity**: low
**Effort**: quick (< 5 min)
**Fix**: commit `src/main/resources/application-local.properties.example` with placeholder
`spring.datasource.url/username/password` keys.

### 6. Use the Maven wrapper in CI

**Impact**: CI runs `mvn` (runner-provided) instead of the pinned `./mvnw` (3.9.9), so local and
CI builds can use different Maven versions — a subtle reproducibility gap.
**Severity**: low
**Effort**: quick (< 5 min)
**Fix**: in `.github/workflows/maven-it.yml`, replace `mvn` with `./mvnw` in both run steps.

### Addressed in upcoming lessons (Category B)

### Missing agent instruction content (AGENTS.md + CLAUDE.md compensation)

**Lesson**: [Agent Onboarding: Agents.md, AI Rules i feedback loops (M1L4)](https://platforma.przeprogramowani.pl/external/10xdevs-3/m1-l4)
**What you'll do there**: Build the real agent instruction files — including the Vaadin
push/threading, `VaadinWebSecurity` route-security, and tenant/IDOR rules the stack assessment
recommended. There's no `AGENTS.md`, and CLAUDE.md currently holds only the lesson router;
generating stubs now would be premature.

### No lint/format and no security stage in CI

**Lesson**: [Sprint Zero z Agentem: infrastruktura, walking skeleton i pierwszy deploy (M1L5)](https://platforma.przeprogramowani.pl/external/10xdevs-3/m1-l5)
**What you'll do there**: Round out the CI pipeline — add the Spotless/format gate and a
security-scan step alongside the existing build+test job. For now, the working local test runner
is what matters for agent collaboration.

### Manual, single-target deployment

**Lesson**: [Sprint Zero z Agentem: infrastruktura, walking skeleton i pierwszy deploy (M1L5)](https://platforma.przeprogramowani.pl/external/10xdevs-3/m1-l5)
**What you'll do there**: `deploy.sh` currently builds the jar and `scp`s it to a systemd
service on a single EC2 host — a manual deploy, not an automated pipeline. The infrastructure
lesson covers automating this. Acknowledge, don't prioritize.

## Summary

```
Health status: needs-attention
```

This is a well-built, agent-friendly Java project: statically typed with JSpecify null-marking,
strong Spring Boot + Vaadin conventions, and — most valuable for agent work — a real
browser-level integration suite that directly guards the PRD's "must not regress" live-flow
requirement. It lands at `needs-attention` rather than `healthy` for a small set of addressable
Category A items: the Spotless formatter config is missing (the documented format command is
broken), there is no dependency vulnerability scanning on a bleeding-edge dependency tree, and
the JSpecify null-safety contract doesn't yet cover the packages the upcoming auth/tenant work
touches. None of these block progress and none are security-critical.

Next step: knock out the Category A fixes above — at minimum #1 (Spotless) and #2 (dependency
scanning) — then proceed to agent onboarding (M1L4), where you'll build the CLAUDE.md/AGENTS.md
compensation content the stack assessment recommended.
