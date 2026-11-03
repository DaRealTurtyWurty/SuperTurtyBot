# TurtyBot

TurtyBot is a multipurpose Discord bot for servers that want moderation, utilities, games, automation, and community features in one place.

It is built to help a server feel more active, easier to manage, and more fun to use day to day. Instead of stitching together a stack of narrow bots, TurtyBot aims to cover the things most communities actually need.

More information is available at [turtywurty.dev/projects/turtybot](https://turtywurty.dev/projects/turtybot).

## What It Can Do

- Moderate with bans, kicks, timeouts, warnings, reports, modmail, slowmode, purge tools, and automatic protection against invites, scams, and image spam.
- Keep communities active with suggestions, starboard, welcome and goodbye messages, birthdays, reminders, sticky messages, sticky roles, and role selection menus.
- Add progression through levelling, rewards, leaderboards, collectables, and server-wide engagement systems.
- Run a full economy with balances, jobs, deposits, loans, shops, robbery, heists, property, blackjack, poker, slots, crash, and more.
- Give members plenty to do with games like chess, checkers, connect 4, battleships, trivia, hangman, wordle, higher or lower, and other minigames.
- Handle everyday utility with weather, Wikipedia, polls, quotes, code execution, file conversion, GitHub lookups, CurseForge lookups, Steam tools, Minecraft tools, Roblox tools, and more.
- Post outside updates directly into Discord with notifiers for YouTube, Twitch, Reddit, Steam, and several game news feeds.
- Offer optional AI-powered replies for servers that want lightweight chat assistance.

## Examples

- Members can use commands like `/weather`, `/wikipedia`, `/poll`, `/quote`, `/run-code`, `/wordle`, `/trivia`, `/balance`, and `/remindme`.
- Staff can configure things like automod, role menus, logging, suggestions, notifier feeds, and other server features.
- Staff with Manage Server can run `/serveraudit` for a private summary embed linking to the server audit page in the dashboard. The latest completed report is saved per server in MongoDB and includes bot permissions, broken channel/role settings, unusable notifiers, dashboard/API health, repair links, and checks that could not run. Staff can run the audit again from the dashboard after fixing settings. Notifier inactivity alone is not treated as a failure; upstream account existence and delivery age are not verified.
- Communities can use it for recurring engagement through levelling, leaderboards, birthdays, starboard posts, and chat revival prompts.

## Command Metadata

Run `./gradlew generateCommandMetadata` to regenerate [commands.json](commands.json). The normal Gradle `build` task also regenerates it. The JSON is generated from the active `CoreCommand` instances in `CommandHook` and includes command descriptions, categories, access notes, usage, rate limits, scopes, and the Discord registration payloads (including options and subcommands). It does not require a running bot or Discord token.

The `/marketplace` command uses MongoDB transactions to transfer listings, items, and money together. Its MongoDB deployment must support transactions (a replica set or sharded cluster).

## Java quality tools

The build resolves `dev.railroadide.quality` version `1.0.0` from Railroad Maven
at `https://maven.railroadide.dev/releases`. Run `./gradlew format` to apply Railroad's Java style, or
`./gradlew formatCheck` to check it. Run `./gradlew javadocCoverage` to generate
`build/reports/javadoc-coverage/index.html`; `javadocCoverageCheck` enforces complete
public API documentation. Strict coverage is opt-in.

Local and CI builds download the plugin automatically.

## License

TurtyBot is licensed under the GNU Affero General Public License v3.0 or later. See [LICENSE](LICENSE) for the full license text.

## Server audit dashboard links

Set `DASHBOARD_SITE_URL` on the **bot** to the public Next.js site URL (for example, `https://bot.example.com` or `http://localhost:3003` in development). `/serveraudit` links to `/dashboard/{guildId}/audit` on that site. This is separate from `DASHBOARD_PUBLIC_URL`, which points to the bot API for health checks. If the site URL is missing or invalid, the command saves the report and explains that its dashboard link is unavailable.

Audit reads and reruns require a dashboard session and a live Discord Manage Server permission check. Reports are server-scoped; the latest completed audit replaces the previous report and survives bot restarts. A rerun is limited to once every 30 seconds per server, and settings are changed only through their existing dashboard editors. Permission and service health findings include instructions for the server administrator or bot operator.
