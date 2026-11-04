# Project guidance

## Repository overview

TurtyBot is a multipurpose Discord bot with a separate web dashboard. Keep changes consistent across the bot API and dashboard when a feature spans both.

- The bot uses Java 25, JDA 6, Gradle, MongoDB, and Javalin. Its entry point is `src/main/java/dev/darealturtywurty/superturtybot/TurtyBot.java`.
- `turtybotsite/` is a Next.js 15 App Router application using React 19, strict TypeScript, Tailwind CSS, and npm with `package-lock.json`. Local development uses port 3003.
- Read `README.md` for feature behavior, privacy/retention requirements, and operational constraints. Read `turtybotsite/README.md` for dashboard setup.
- `TERMS.md` and `PRIVACY.md` describe the hosted service. The website renders its policy pages from `turtybotsite/content/terms.md` and `turtybotsite/content/privacy.md`; keep corresponding documents consistent when changing them.

## Code map

Java paths below are relative to `src/main/java/dev/darealturtywurty/superturtybot/`:

- `commands/`: commands organized by feature/category.
- `core/command/`: shared command behavior, registration, categories, and metadata generation. `CoreCommand` implements common interaction handling; `CommandHook.createCommands()` supplies the active command set.
- `modules/`: feature managers and shared business logic, including economy, privacy, and auditing.
- `database/Database.java`: MongoDB collections, codecs, indexes, and collection wrappers. Persisted models live in `database/pojos/`.
- `dashboard/http/`: Javalin routes and HTTP request handling. `dashboard/service/` contains feature services and API payload types; `DashboardServer` wires services together.
- `weblisteners/`: external feeds and notifiers.
- `Environment.java`: bot environment configuration. Inspect configuration declarations rather than opening local secret files.

Dashboard paths are relative to `turtybotsite/`:

- `app/dashboard/[guildId]/`: guild dashboard pages.
- `app/api/`: server-side route handlers, including Discord OAuth and dashboard API proxies.
- `components/`: reusable UI and feature panels.
- `lib/dashboard-api.ts`: server-only bot API client and shared response/request types.
- `lib/auth.ts` and `lib/discord.ts`: sessions and Discord authentication helpers.
- `tests/`: Node test runner tests for authentication, audit routes, and audit rendering.

## Build and verification

Use the checked-in Gradle wrapper. The configured toolchain is Java 25 and the wrapper uses Gradle 9.4.1. On PowerShell, run these from the repository root; on Unix use `./gradlew` instead of `.\gradlew.bat`.

| Command | Purpose |
| --- | --- |
| `.\gradlew.bat compileJava` | Compile Java changes without starting the bot. |
| `.\gradlew.bat formatCheck` | Check Railroad semantic and Spotless formatting. |
| `.\gradlew.bat format` | Apply configured formatting for Java, CSS, Groovy, and Gradle files; review the diff. |
| `.\gradlew.bat generateCommandMetadata` | Regenerate root `commands.json` from registered commands without a running bot or Discord token. |
| `.\gradlew.bat build` | Build/check the Java project and regenerate command metadata. |
| `.\gradlew.bat shadowJar` | Produce `build/libs/SuperTurtyBot-all.jar`, used by the root Dockerfile. |
| `.\gradlew.bat javadocCoverage` | Generate the public API documentation report under `build/reports/javadoc-coverage/`. |

`javadocCoverageCheck` enforces complete public API documentation and is opt-in. Do not introduce it as a mandatory check without a task requiring that change. The quality plugin is resolved through Railroad Maven, configured in `settings.gradle`.

Run dashboard commands from `turtybotsite/`:

```text
npm ci
npm run dev
npm run lint
npm run build
node --experimental-vm-modules --test tests/discord-auth.test.mjs tests/server-audit.test.mjs tests/server-audit-panel.test.mjs
```

- Install dependencies with `npm ci` when needed; preserve the npm lockfile and avoid introducing another package manager.
- Choose checks relevant to the change. For Java behavior changes, compile and check formatting; for dashboard changes, run lint, build, and relevant existing tests.
- Do not leave newly written unit tests or other automated tests in the project unless the user explicitly asks to keep them. You may create and run temporary tests to verify your work, but remove those tests before finishing.
- Remove any fixtures, helper scripts, test-only dependencies, and build/configuration changes you introduced solely for those temporary tests. Preserve tests and test infrastructure that existed before your task, including uncommitted user work; this cleanup rule applies only to your own temporary additions.
- Report which temporary checks ran and their results, even though their files were removed. Review the final diff and untracked files to confirm no temporary testing artifacts remain.
- The Java build currently declares no test framework dependencies. Do not report `gradlew test` as meaningful behavioral coverage without verifying that tests actually ran.
- Documentation-only changes need content/path review, the mandatory formatter pass below, and `git diff --check`; an application build is unnecessary.
- Report what was checked and any failures or unavailable prerequisites accurately. Do not silently weaken checks to get a passing result.

## Formatting rules and mandatory completion checks

- Always run `.\gradlew.bat format` from the repository root before finishing a task, including documentation-only or dashboard-only tasks. On Unix, use `./gradlew format`. Run it after the final edits; `formatCheck` alone does not replace the formatter pass.
- Then run `.\gradlew.bat formatCheck` and `git diff --check`. Resolve formatting or whitespace issues introduced by your changes before reporting completion. If you make further edits, rerun the formatter and formatting check.
- The Railroad quality plugin configured in `build.gradle` is the authority for Java formatting. Preserve its indentation, brace placement, wrapping, import ordering, and whitespace rules; do not substitute another formatter or change/disable formatting rules to make a check pass.
- The current generated Java formatter profile uses four spaces for indentation, a 120-column wrapping target, and opening braces on the same line for types, methods, and blocks. The generated profile is `build/railroad-quality/config/railroad-eclipse-formatter.xml`; inspect it when needed, but change configuration through the build/plugin rather than editing generated output.
- Java source encoding is UTF-8. Preserve existing line-ending conventions, use final newlines, and avoid trailing whitespace. Match surrounding TypeScript/TSX, CSS, JSON, Gradle, and Markdown style. The dashboard currently has ESLint but no dedicated formatter script; use `npm run lint` for dashboard edits and do not introduce a formatter dependency unless requested.
- Review the diff after formatting. Keep formatter-required changes distinct from unrelated manual cleanup, and preserve any pre-existing user edits. If formatting or its checks fail or cannot run, report the command and reason explicitly; never claim they passed or silently skip them.

## Implementation conventions

- Follow nearby feature implementations and existing naming/style. Keep unrelated refactors, dependency upgrades, and manual style cleanup out of focused changes; always perform the required formatter pass above.
- Use the existing command base classes and registration path. Preserve rate limits, privacy checks, guild/permission restrictions, and correct interaction acknowledgement. Defer Discord replies before slow work and follow the existing asynchronous JDA patterns.
- When adding or changing command definitions, regenerate `commands.json`; do not hand-edit the generated metadata.
- Keep HTTP handling in routes and feature logic in services/managers. For API changes, update Java payload types, TypeScript types/client functions, Next.js proxy routes, and UI callers together as applicable.
- TypeScript uses the `@/*` alias rooted at `turtybotsite/`. Preserve the server/client component boundary and keep the server-only API client out of browser imports.
- Preserve persisted field names and compatibility with existing MongoDB documents. Handle missing fields/defaults deliberately; review indexes and migration behavior for schema changes.
- Reuse the existing collection wrappers. Bypassing `OptionalDataCollection` or `AuditedConfigCollection` can bypass privacy guards or settings history.

## Authentication, privacy, and runtime constraints

- Keep tokens, OAuth secrets, session identifiers, and `DASHBOARD_API_KEY` out of committed files, browser code, logs, and responses. Do not print local `.env` files or runtime data while investigating configuration.
- Derive dashboard actor IDs from the signed-in server session. Preserve API-key authentication, current Discord permission checks, and origin checks for mutations. Forward `X-TurtyBot-Actor-Id` through the server-side client; never trust an actor ID supplied by browser inputs.
- Before adding personal-data persistence or external submissions, inspect `modules/privacy/` and the README. Integrate applicable opt-out, export, deletion, retention, and confirmation handling, including caches and background jobs. Preserve the necessary operational/safety exceptions documented there.
- Do not log message contents, exported data, or sensitive moderation details. Preserve requester-bound, expiring, single-use confirmations for external sharing.
- Privacy coordination and settings-write serialization assume one bot process. Do not introduce multiple-instance assumptions without addressing distributed coordination.
- Marketplace transfers require MongoDB transaction support through a replica set or sharded cluster; preserve atomic money/item/listing updates.
- Bot startup connects to real services and starts background work. Use compilation and targeted checks for routine verification; use a deliberately configured development bot/database for runtime testing.
- The bot defaults to a root `.env`; the website has its own environment configuration. Website `DASHBOARD_API_URL` points to the Javalin API, bot `DASHBOARD_SITE_URL` points to the public website, and bot `DASHBOARD_PUBLIC_URL` is the public API health-check address. Do not interchange them.
- Treat logs, `data/`, `state/`, `versions/`, local databases, and runtime JSON files as operational data. Do not delete, rewrite, or commit them as incidental cleanup. Build output, caches, `.next/`, and `node_modules/` are generated artifacts.

## Completing work

Remove your temporary tests and their supporting changes unless the user explicitly requested keeping them. Run the formatter and formatting checks described above after the final edits, then review the final diff and untracked files for leftover temporary testing artifacts, unrelated edits, generated-file churn, and secrets. Update the relevant README when changing configuration or user-visible behavior. Summarize the resulting behavior, verification performed (including formatter/check results), and any remaining limitations.
