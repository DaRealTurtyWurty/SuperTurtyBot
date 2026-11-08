# TurtyBot

TurtyBot is a multipurpose Discord bot for servers that want moderation, utilities, games, automation, and community features in one place.

It is built to help a server feel more active, easier to manage, and more fun to use day to day. Instead of stitching together a stack of narrow bots, TurtyBot aims to cover the things most communities actually need.

More information is available at [turtywurty.dev/projects/turtybot](https://turtywurty.dev/projects/turtybot).

See the [Terms of Service](TERMS.md) and [Privacy Policy](PRIVACY.md) for the hosted service.

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

## Privacy commands

`/privacy info`, `/privacy export`, `/privacy delete`, `/privacy opt-out`, and
`/privacy opt-in` are available to users
without server administrator permission. Discord requires a subcommand when a
command has children, so the information command is `/privacy info`.

All replies are ephemeral. Export prepares a ZIP in memory containing
`turtybot-data.json` and a `README.txt` guide to its structure, scope, redactions,
and support options. The JSON holds structured database records across servers;
it excludes authentication/session identifiers,
other people's message entries, and staff-only moderation text. Shared records
are exported as account-related summaries. Exports have a one-minute cooldown,
two concurrent slots, a 5,000-record limit per query, a 16 MiB JSON limit, and a
7 MiB attachment limit. Larger or wider access requests go to `turtybot@gmail.com`.

Deletion requires a five-minute, single-use confirmation bound to the requesting
Discord account. It removes supported owned records, redacts supported references
in shared records, revokes database dashboard sessions, cancels reminders, and
clears affected AI/message caches. It also saves the optional-data opt-out before
cleanup, preventing optional profiles from being recreated. It does not change
Discord bans. Age-based retention is handled by the daily cleanup described below. In-progress
games and concurrent feature activity can require follow-up review.

Deletion also removes tracked bot-created Gists from GitHub if the account was
the source author or the requester, including uploads requested by another member.
Successful removals delete the tracking record. Failed or uncertain removals keep
their records in `gistUploads` and mark the privacy request `partial_failure`;
repeat `/privacy delete` or complete operator review to retry. A failed Gist step
does not prevent other supported data cleanup. Older, untracked Gists still need
manual review.

`/privacy opt-out` disables most commands, interactions and background features
for the account across all servers. Privacy commands and basic help/status remain
available; blocked commands explain the choice. Optional account collections reject
new writes and exclude opted-out owners from ordinary queries. Existing records
remain until deleted. Reminders are paused, AI/message caches are cleared, and
dashboard sessions are revoked; dashboard sign-in is disabled while opted out.
Server safety moderation, privacy requests and necessary operational processing
still apply. Requests already sent to Discord or another provider may finish.

The `privacyOptOuts` collection stores only `_id: <Discord user ID>` for each
opted-out account. This marker survives deletion and restarts, has no automatic
expiry, and is removed by `/privacy opt-in`. Opting in enables optional features
again and resumes future reminders; overdue reminders are discarded. Deleted
records are not restored. Preferences are loaded before connecting to Discord;
a loading failure stops startup. Run a single bot process: preference changes
and write coordination are local to that process, with durable MongoDB storage.

Set `DASHBOARD_SITE_URL` on the bot to the public website origin for the policy
link. Set `OWNER_ID` to receive Discord DMs about deletion requests. Requests remain
durably recorded even if DM delivery fails; monitor the `privacyRequests` MongoDB
collection for `processing`, `partial_failure`, and `manual_review` records.

The operator must promptly finish each request: investigate `failedSteps`, use
`externalReferences` to locate known Discord copies and ticket channels, review
active temporary-ban schedules, modmail blocks and AI restrictions, and handle
Gists, downloaded files, provider-held data, free-text references and live activity.
Active schedules are left for review so erasure does not cancel an eventual unban.
Only retain data where legally required; server staff do not have a general veto
over a user's API-data deletion request. Use the request reference to communicate
the outcome, including any justified limits, to the user.

Once fully resolved, mark the request `resolved` and set `resolvedAt` to its actual
completion time as a BSON Date. Remove personal details immediately where possible;
the daily cleanup also strips the user ID, external references and other personal
fields, retaining only the reference, status, dates and numeric counts. It clamps
`deleteAfter` to no later than 30 days after resolution and removes due records.
The TTL index applies only to resolved requests. Pending requests have no automatic
expiry, and the daily job removes accidental deadlines from unresolved records.
The request reference, status, timestamps, counts and step names are sufficient
for a minimal completed-request record. Never store transcripts or exported ZIPs
in the request record or application logs.

Deletion is idempotent and may complete some steps before another fails. Retry
failed steps and complete manual review rather than reporting full erasure based
only on the automatic cleanup result. Exports and deletion are serialized per
account within this bot instance; multiple bot processes need a distributed lock.

## Daily retention cleanup

`PrivacyRetentionManager` starts one cleanup pass after startup and repeats daily.
Each step reports a count and failures without recording message contents or user
IDs in cleanup logs. Failures do not cancel later steps or future runs.

- Optional profiles and saved personal feature records: 12 UTC calendar months
  without observed messages, reactions, voice activity, commands or dashboard use
  across the bot. Balances, inventories and saved content can be lost after expiry.
- Closed modmail database tickets and transcripts: 90 days after closure.
  Undated closures and orphaned transcript chunks get 90 days from first observation.
- Warnings with an explicit stored expiry: 90 days after expiry. Reports explicitly
  marked resolved: 90 days after `resolvedAt`, or first observing resolution.
- Resolved privacy requests: sanitize personal details on the next run and remove
  the minimal receipt within 30 days of resolution.
- Sticky roles: six UTC calendar months after `savedAt`; sessions: their existing expiry.
- Abandoned conversion/GIF work in the dedicated `superturtybot` temporary directory:
  24 hours since modification, with active work and recent descendants protected.

Active/no-expiry warnings, unresolved reports, bans/blocks, pending unbans, open
modmail, future reminders, opt-out identifiers and unresolved Gist receipts are
preserved. Shared server settings and counting state are not reset by profile
expiry. Discord-hosted copies, legacy Gists and old unmanaged temporary files still
need their existing separate cleanup/review.

`privacyActivity` stores only the account ID and `lastActiveAt` for accounts with
profile data. Profile writes enroll an account once; background maintenance does
not refresh it. Activity is cached and persisted at most hourly, with an extra
hour of expiry grace so a restart cannot cause early cleanup. Existing data without
activity timestamps gets a full new window when observed. `privacyRetentionConfig`
stores the rollout date used for untimestamped opted-out records; those accounts do
not acquire new activity profiles. Profile expiry removes the activity metadata and
keeps only a transient ID guard against delayed writes until a new user interaction
or restart. `/privacy export` includes activity timestamps; `/privacy delete` clears them.

The startup index migration replaces the old unrestricted `privacyRequests.deleteAfter`
TTL index with one filtered to `status: resolved`, so a reopened/pending request cannot
be deleted by TTL. The cleanup also enforces that status when deleting requests.
Run a single bot process; activity caching, expiry guards and file leases are local
to that process. Keep cleanup error logs and pending deletion requests under review.

## External sharing review

Any user can request a Gist of another member's supported text attachments by
reacting, provided neither the requester nor the message author has opted out.
The requester then confirms the upload after seeing GitHub, the files involved,
and the fact that an unlisted Gist is readable by anyone with the link. The message
author does not approve individual uploads. Requesters must have permission to
share the content; a privacy notice and an absence of opt-out do not establish that
permission. Run Code accepts only the source message's author and asks before
submitting its first supported code block or file
to Compiler Explorer. Brainfuck remains local. Execution output is no longer copied
to debug logs.

Approval buttons are bound to the requester, single-use, and expire after five minutes.
Cancel sends nothing. The source is checked again before submission; an edit or
deletion requires a fresh request. Opt-out and deletion invalidate pending approvals
involving either the requester or the source author, and both accounts' opt-outs
are checked again before the external request. Pending approval data
is held in a bounded in-memory cache, not a persistent consent/profile collection.
Approval is for that submission only. Provider-held copies can outlive it, and an
already-started upload cannot be recalled. Gists are read in memory with an 8 MiB
combined limit and no new temporary disk files.

New uploads save a durable `gistUploads` intent before sending any files to GitHub.
It holds an opaque tracking ID, source author and requester IDs, source server,
channel and message IDs, GitHub account/Gist IDs, creation time and cleanup state.
It contains no attachment filenames or contents. These records stay available for
access and deletion handling even after an opt-out. Exports include the account's
Gist references with the other participant's identity omitted.

If saving the returned Gist ID fails, the bot attempts to remove the upload before
reporting failure. Uncertain creation attempts retain their intent for review; the
Gist description includes `TurtyBot upload <tracking ID>` so an operator can locate
it without publishing Discord identifiers. Retained records have no automatic TTL
while a copy or unresolved cleanup remains. Do not erase these records before the
GitHub copy is confirmed removed. A mismatched GitHub account, missing credentials,
permission error, rate limit or ambiguous 404 leaves the record for retry/review.
Once GitHub confirms deletion, the receipt records that confirmation before its
own removal, so a later retry can finish removing metadata without re-deleting it.
The token needs access to the GitHub account that created the Gist and permission
to delete Gists. Previously made forks, downloads and other people's copies cannot
be removed by this bot's GitHub deletion call.

The source review also found that file conversion uses local FFmpeg and scam
matching uses a downloaded domain list locally; ordinary message text is not sent
to an external conversion or scam-check API. Social feeds fetch configured source
IDs and public posts. Lookup commands forward the query supplied for that command;
they do not forward ambient chat history. Cataas receives `/catsays` text, StrawPoll
receives poll inputs, and Open-Meteo receives weather locations and coordinates.
The operator's utility API receives inputs such as avatar URLs for image commands.
AI behavior and shared conversation context are unchanged and outside this review.

This review covers source-level submission controls. Review historical Gists,
old temporary files, provider agreements and deployed settings separately; current
controls do not establish authority for past uploads or for third-party private
information copied into a user's submission. See the
[Discord Developer Policy](https://support-dev.discord.com/hc/en-us/articles/8563934450327-Discord-Developer-Policy)
and [ICO consent guidance](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/lawful-basis/consent/what-is-valid-consent/)
for the guidance used in this review.

## Command Metadata

Run `./gradlew generateCommandMetadata` to regenerate [commands.json](commands.json). The normal Gradle `build` task also regenerates it. The JSON is generated from the active `CoreCommand` instances in `CommandHook` and includes command descriptions, categories, access notes, usage, rate limits, scopes, and the Discord registration payloads (including options and subcommands). It does not require a running bot or Discord token.

Each run replaces the entire metadata file, so commands removed from
`CommandHook.createCommands()` disappear on regeneration.

The dashboard's **Command explorer** (`/dashboard/{guildId}/commands`) uses that same
metadata generator against the running registry. Staff with current Manage Server
permission can search and filter commands, inspect their full definitions, and
enable or disable each command for their server. Availability is saved separately
in `guildData.disabledCommands`, recorded in settings history, and checked before
command execution, cooldowns, or quest activity. Disabling a command blocks its
slash, prefix, context-menu, and subcommand entry points in that server; Discord
still lists registered commands. Direct messages and background features are
unaffected. Enabling preserves existing permissions and module settings.

The `/marketplace` command uses MongoDB transactions to transfer listings, items, and money together. Its MongoDB deployment must support transactions (a replica set or sharded cluster).

## Notifier diagnostics and settings history

Steam news snapshots accept both BSON 32-bit and 64-bit integer dates. New writes
use 64-bit integers; existing snapshots do not need a database migration.

Notifier cards in the dashboard show the last attempted/successful check, last
confirmed delivery and item, and separate feed, delivery and manual-test errors.
Refresh diagnostics to load current state and destination permission checks.
Successful checks with no new posts are recorded. Twitch is event-driven: its
timestamps describe received live events, rather than a successful upstream poll.
Diagnostics start when the updated bot observes activity and persist across restarts.

**Send test (no pings)** sends a fixed Discord embed to the notifier's saved
destination. It requires current Manage Server permission, uses no configured
mentions, and is limited to once every 30 seconds per server. Tests check Discord
delivery only; they do not contact the upstream provider or change deduplication
caches, real delivery timestamps, or successful-check timestamps. A timed-out send
can still arrive. Inactive diagnostic records expire after 90 days.

The **Settings history** dashboard page lists before/after values, the Discord
actor (linked to their profile), source, and time for server-wide settings,
counting registrations/modes, and external notifier configuration. It covers
dashboard and command writes through the shared collections, including automatic
configuration changes. Runtime caches, counting progress, birthday membership and
tax bookkeeping are excluded. Saved content and moderation actions are separate
from settings history. Entries start with this release and expire after 90 days;
there is no rollback control yet.

History reads and server mutations require current Manage Server permission.
The Next.js server forwards the actor from the signed-in session using
`X-TurtyBot-Actor-Id`; direct bot-API mutation clients must supply that header as
well as the API key. Deploy the bot and dashboard together. Never expose the API
key to browser code or accept actor IDs from browser inputs.

Run one bot process. Configuration snapshots/writes are serialized per collection
within that process; history is written after the settings write and is not a
MongoDB transaction. A history-write failure reports that the settings were saved
but history could not be recorded, and logs an operator error. A process failure
between the writes can leave a gap; this is operational history, not a tamper-proof
ledger. `/privacy export` includes the actor's history metadata; deletion removes
their actor references. References embedded in settings values need operator review.

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
