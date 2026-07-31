# TurtyBot Features

This file is an inventory of the commands, feature modules, dashboard areas, and likely next additions found by scanning the repository.

Notes:

- Scanned command count: 126 active registered commands and 4 implemented-but-disabled command classes.
- Active commands are the commands currently added in `CommandHook.createCommands()`.
- Prefix commands use the configured bot prefix at runtime. This file writes them as `prefix:<name>`.
- Context commands are Discord message or user context menu commands.
- A few implemented commands are intentionally listed as disabled because their registration line is commented out.

## Active Commands

### Core

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/ping` | Slash | Global | Shows REST and gateway ping. |
| `/help` | Slash | Global | Shows bot help or help for a specific command. |
| `/commands` | Slash | Global | Lists available commands. |
| `/tag` | Slash, message context | Server | Creates, edits, deletes, lists, and fetches reusable server tags. Subcommands: `get`, `create`, `edit`, `delete`, `list`. |
| `prefix:shutdown` | Prefix | Global | Owner/admin shutdown command. |
| `prefix:restart` | Prefix | Global | Owner/admin restart command. |
| `/serverconfig` | Slash | Server | Gets and sets guild configuration. Subcommands: `get`, `set`. |
| `/userconfig` | Slash | Server | Gets and sets per-user configuration. Subcommands: `get`, `set`. |
| `/opt` | Slash | Server | Opts users in or out of configured channels. Subcommands: `in`, `out`, `list`. |
| `/uptime` | Slash | Global | Shows bot uptime. |
| `prefix:eval` | Prefix | Global | Evaluates code for bot administration. |
| `prefix:speak` | Prefix | Global | Makes the bot send a supplied message. |
| `prefix:announce` | Prefix | Global | Sends an announcement to all servers the bot is in. |
| `/test` | Slash | Global | Test/development command. |
| `/tokens` | Slash | Global | Shows remaining AI token allowance. |
| `/transactionhistory` | Slash | Server | Shows transaction history. Subcommand: `economy`. |

### Utility And Information

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/botinfo` | Slash | Global | Shows information about the bot. |
| `/userinfo` | Slash, user context | Server | Shows information about a user. |
| `/serverinfo` | Slash | Server | Shows information about the current server. |
| `/poll` | Slash | Global | Creates a Discord poll. |
| `/strawpoll` | Slash | Global | Creates a Strawpoll. |
| `/strawpollresults` | Slash | Global | Retrieves Strawpoll results. |
| `/roles` | Slash | Server | Displays server roles. |
| `/github` | Slash | Global | Looks up a GitHub repository. |
| `/curseforge` | Slash | Global | Looks up a CurseForge project. |
| `/highlight` | Slash | Server | Notifies users when configured text appears. Subcommands: `create`, `list`, `delete`. |
| `/periodic-table` | Slash | Global | Looks up periodic table elements. |
| `/fact` | Slash | Global | Returns a random fact. |
| `/quote` | Slash, message context | Server | Saves and retrieves quotes. Subcommands: `add`, `remove`, `list`, `list_compact`, `get`, `random`. |
| `/latest` | Slash | Global | Checks latest Minecraft/tooling versions. Subcommands: `minecraft`, `forge`, `fabric`, `quilt`, `parchment`, `neoforge`, `all`. |
| `Analyze Log` | Message context | Global | Analyzes a log message/file for warnings and errors. |
| `/embed` | Slash | Global | Creates and manages saved embeds. Subcommands: `create`, `edit`, `add_field`, `delete_field`, `edit_field`, `delete`, `view`, `list`. |
| `/remindme` | Slash | Server | Creates and manages reminders. Subcommands: `create`, `list`, `delete`, `clear`. |
| `/r6status` | Slash | Global | Shows Rainbow Six Siege server status. |
| `/weather` | Slash | Global | Gets weather for a location. |
| `/steam` | Slash | Global | Steam user/game lookup. Subcommands: `userid`, `usergames`, `game-details`. |
| `/roblox` | Slash | Global | Roblox user lookup. Subcommands: `username`, `avatar`, `friends`, `favourite-games`. |
| `/minecraft` | Slash | Global | Minecraft username, UUID, and skin lookup. Subcommands: `username`, `uuid`, `skin`. |
| `/wikipedia` | Slash | Global | Looks up a Wikipedia page. |
| `/topic` | Slash | Global | Returns a random conversation starter topic. |
| `/birthday` | Slash | Global | Sets/views birthdays and toggles birthday announcements. Subcommands: `set`, `view`, `announced`. |
| `/latex` | Slash | Global | Renders LaTeX as an image. |
| `Run Code` | Message context | Global | Executes code snippets in supported languages via the configured runner. |

### Community And Server Workflow

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/suggest` | Slash | Server | Creates and moderates server suggestions. Subcommands: `add`, `approve`, `deny`, `consider`, `delete`. |
| `/role-selection` | Slash | Server | Creates and edits role-selection menus. Subcommands: `create`, `add`, `delete`, `remove`. |
| `/notifier` | Slash | Server | Configures external update notifiers. Subcommands: `youtube`, `twitch`, `steam`, `steamsales`, `reddit`, `minecraft`, `siege`, `rocketleague`, `league`, `valorant`. |
| `/addroletothread` | Slash | Server | Adds a role's members to a thread. |

### Moderation

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/ban` | Slash | Server | Bans a user. |
| `/tempban` | Slash | Server | Temporarily bans a user. |
| `/unban` | Slash | Server | Unbans a user. |
| `/timeout` | Slash | Server | Times out a member. |
| `/removetimeout` | Slash | Server | Removes a member timeout. |
| `/kick` | Slash | Server | Kicks a member. |
| `/purge` | Slash | Server | Bulk-deletes messages. |
| `/warn` | Slash | Server | Warns a user. |
| `/removewarn` | Slash | Server | Removes a warning. |
| `/clearwarns` | Slash | Server | Clears all warnings for a user. |
| `/warnings` | Slash | Server | Lists warnings for a user. |
| `/slowmode` | Slash | Server | Sets channel slowmode. |
| `/automod` | Slash | Server | Configures invite guard, scam detection, and image-spam autoban. Subcommands: `status`, `invite_guard`, `scam_detection`, `image_spam`. |
| `/sticky` | Slash | Server | Creates sticky text or embed messages. Subcommands: `text`, `embed`, `view`, `clear`. |
| `/modmail` | Slash | Server | Creates and manages private modmail tickets. Subcommands: `create`, `close`, `block`, `unblock`. |
| `/register-counting` | Slash | Server | Registers or unregisters counting channels. |
| `/report` | Slash | Server | Reports a user. |
| `/reports` | Slash | Server | Shows report history for a user. |

### Image And Media

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/httpcat` | Slash | Global | Shows an HTTP status image. |
| `/httpdog` | Slash | Global | Shows an HTTP status image. |
| `/meme` | Slash | Global | Fetches memes. |
| `/programmingmeme` | Slash | Global | Fetches programming memes. |
| `/inspirobot` | Slash | Global | Fetches a generated inspirational image. |
| `/image` | Slash | Global | Fetches registered image types from APIs and Pexels, including nature/space/food/random animal categories, plus NASA. |
| `/deepfry` | Slash, message context, user context | Global | Applies a deep-fried image effect. |
| `/catsays` | Slash | Global | Generates a captioned image. |
| `Flagify` | User context | Server | Applies a flag overlay to a user's avatar. |
| `LGBTify` | User context | Global | Applies an LGBT flag overlay to a user's avatar. |

### Fun

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/advice` | Slash | Global | Returns advice. |
| `/coinflip` | Slash | Global | Flips a coin. |
| `/eightball` | Slash | Global | Answers a question. |
| `/internetrule` | Slash | Global | Returns a rule from the bundled Rules of the Internet resource. |
| `/reversetext` | Slash | Global | Reverses text. |
| `/upsidedowntext` | Slash | Global | Converts text to upside-down characters. |
| `/urban` | Slash | Global | Searches Urban Dictionary. |
| `/petpetgif` | Slash, message context, user context | Global | Creates a petpet GIF from a user avatar or image. Subcommands: `user`, `image`. |
| `/love` | Slash, user context | Global | Calculates a love score between users. |
| `/smashorpass` | Slash | Global | Celebrity smash-or-pass game. |
| `/wouldyourather` | Slash | Global | Returns a would-you-rather prompt. |
| `/collectables` | Slash | Server | Shows the collectables a user has obtained. |

### Levelling

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/rank` | Slash | Server | Shows a member's rank card. |
| `/leaderboard` | Slash | Server | Shows levelling or economy leaderboards. Subcommands: `levels`, `economy`. |
| `prefix:setxp` | Prefix | Server | Server-owner command to add, remove, or set user XP. |

### Minigames

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/trivia` | Slash | Server | Starts trivia. |
| `/guess` | Slash | Server | Guessing game group. Subcommands: `geoguesser`, `combinedflags`, `border`. |
| `/higherlower` | Slash | Server | Higher/lower games. Subcommands: `population`, `area`, `word_frequency`. |
| `/wordle` | Slash | Global | Daily Wordle-style game. |
| `/hangman` | Slash | Server | Hangman game. |
| `/tictactoe` | Slash | Server | Tic-tac-toe against a user or bot. |
| `/wordsearch` | Slash | Server | Word search game. |
| `/crossword` | Slash | Server | Clue-based crossword. |
| `/connect4` | Slash | Server | Connect 4 against a user or bot. |
| `/checkers` | Slash | Server | Checkers against a user or bot. |
| `/2048` | Slash | Global | 2048 game. |
| `/chess` | Slash, user context | Server | Chess game. |
| `/battleships` | Slash | Global | Battleships game. |

### Economy

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/balance` | Slash | Server | Shows wallet, bank, total balance, and bet totals. |
| `/crime` | Slash | Server | Risk/reward crimes and crime progression. Subcommands: `beginner`, `intermediate`, `advanced`, `expert`, `master`, `profile`. |
| `/deposit` | Slash | Server | Moves wallet money into the bank. |
| `/job` | Slash | Server | Job system. Subcommands: `work`, `register`, `quit`, `profile`, `promote`, `info`. |
| `/reward` | Slash | Server | Claims rewards. Subcommand: `claimall`. |
| `/rob` | Slash | Server | Attempts to rob another user's wallet. |
| `/shop` | Slash | Server | Views, buys, and sells shop items. Subcommands: `view`, `buy`, `sell`. |
| `/withdraw` | Slash | Server | Moves bank money into the wallet. |
| `/slots` | Slash | Server | Slot machine betting. |
| `prefix:setmoney` | Prefix | Server | Admin command to set user money. |
| `/crash` | Slash | Server | Crash betting game. |
| `/loan` | Slash | Server | Loan system. Subcommands: `request`, `pay`, `list`, `info`. |
| `/donate` | Slash | Server | Donates money to another user when enabled. |
| `/property` | Slash | Server | Property ownership, upgrades, trading, and renting. Subcommands: `buy`, `sell`, `list`, `info`, `upgrade`, `trade`, `rent`, `rent-choose`, `rent-reroll`, `stop-rent`, `pause-rent`, `resume-rent`. |
| `/boost` | Slash | Server | Buys convenience boosts. Subcommands: `list`, `status`, `buy`. |
| `prefix:economyreset` | Prefix | Server | Server-owner economy reset. |
| `/heist` | Slash | Server | Heist actions and profile. Subcommands: `start`, `profile`. |
| `/blackjack` | Slash | Server | Blackjack against the bot. Subcommands: `play`, `hit`, `stand`, `howtoplay`. |
| `/poker` | Slash | Server | Simplified Texas Hold'em against the dealer. Subcommands: `play`, `bet`, `fold`, `howtoplay`. |
| `/gofish` | Slash | Server | Multiplayer Go Fish. Subcommands: `create`, `howtoplay`, `ask`, `hand`, `status`, `leave`. |

### NSFW

| Command | Type | Scope | What it does |
| --- | --- | --- | --- |
| `/nsfw` | Slash | Global | Runs the configured NSFW command group. |
| `/guesssexposition` | Slash | Global | Tile-reveal guessing game. |
| `/nsfwsmashorpass` | Slash | Global | NSFW smash-or-pass game. |

## Implemented But Disabled Commands

These command classes exist, but their `commands.add(...)` lines are commented out in `CommandHook`.

| Command | Type | Scope | Notes |
| --- | --- | --- | --- |
| `/systemstats` | Slash | Global | Shows bot host system stats. |
| `/convert` | Slash | Global | Unit conversion command with `list`, `info`, and `all` subcommands. |
| `/xpinventory` | Slash | Server | Renders a levelling inventory image; item acquisition is also currently commented out in `LevellingManager`. |
| `/mappings` | Slash | Global | Minecraft mapping translation/search command. |

## Feature Modules

### Bot Runtime

- JDA 6 bot runtime with slash commands, prefix commands, message context commands, user context commands, per-command rate limits, global and server-only command registration, and startup command-list publishing.
- Environment-driven configuration via `.env`, command-line paths for `.env`, emojis, server icons, log file, AI image dataset, snippets DB, and YouTube video ID DB.
- MongoDB-backed persistence through Morphia/driver collections, plus custom codecs and startup index creation.
- Registry loading for rank card items, collectables, image command types, shop items, and properties.
- Discord logback webhook appender and console tee to a latest log file.

### Moderation And Safety

- Automoderator pipeline for Discord invite filtering, scam-domain detection, and new-member image spam autobans.
- Standard moderation actions: ban, tempban, unban, timeout, remove timeout, kick, purge, slowmode, warnings, warning removal, warning clearing, reports, and report lookup.
- Warning system with expiry, optional moderator-only visibility, XP/economy penalties, and configurable sanctions such as timeout/kick/tempban/ban.
- Temp-ban scheduler that unbans expired temporary bans and logs the event.
- Modmail ticket system that creates private ticket channels, assigns moderator roles, records open/closed status, archives transcripts in chunks, supports blocking/unblocking users, and exposes ticket browsing through the dashboard.
- Sticky messages for support channels, backed by saved text or saved user embeds, with debounced reposting at the bottom of the channel.
- Sticky roles that save member roles when they leave, restore roles on rejoin, clear roles on bans, remove deleted roles, and periodically clean old records.

### Community Automation

- Suggestions workflow with submit, approve, deny, consider, delete, dashboard moderation, and media previews.
- Starboard/showcase system that auto-reacts to configured channels, tracks star counts, supports media-only mode, and reposts to a starboard channel after threshold.
- Welcome/goodbye messages with configurable channel and join/leave toggles.
- Birthday storage and daily birthday announcements for users who opt in per server.
- Reminders with persistent scheduling, guild-channel delivery, thread support, and DM fallback.
- Chat revival scheduler that posts configured prompt types: discussion topics, drawing prompts, and would-you-rather prompts with optional NSFW handling.
- Auto-thread system for configured channels plus moderator-role auto-join behavior for threads.
- Opt-in channel system for user-accessible optional channels.
- Voice channel notifier system with configurable destination channel, role mentions, join/leave notifications, per-join behavior, and cooldowns.
- Hello responder for lightweight conversational trigger replies.

### Levelling

- XP gain from messages with per-user cooldowns.
- Configurable XP range, cooldown, disabled channels, level-up messages, dedicated level-up channel, embedded level-up messages, and level roles.
- XP boosts from specific channels, roles, and server boosting.
- Optional weekly level depletion for inactive users.
- Rank cards and rank card item registries for avatar outlines, backgrounds, XP bar fills, XP bar empties, outlines, and XP outlines.

### Counting

- Per-channel counting games with persistent state, leader data, highest count, and max same-user succession controls.
- Supported counting modes include normal, reverse, decimal, maths expressions, bases from binary through hexadecimal, base36, squares, triangular, pentagonal, hexagonal, cubes, primes, abundant, composite, odd, even, Fibonacci, Lucas, Golomb, happy, and lucky numbers.
- Wrong counts reset the channel and announce the expected value and next target.

### Economy

- Per-server economy accounts with wallet, bank, transaction history, configurable currency, configurable default balance, and enable/disable switches.
- Daily end-of-day income tax processing with optional user DM notifications.
- Jobs with work cooldowns, pay calculation, promotion minigames, and work boosts.
- Crimes with tiered risk/reward progression and imprisonment/fine handling.
- Bank deposits/withdrawals, donations, robbery, rewards, loans, shops, convenience boosts, and admin reset/set-money tools.
- Public shop/item registry currently includes apple, banana, cherry, and orange items.
- Property system currently includes Starter Flat, Suburban Home, and City Penthouse templates with purchase price, estate tax, rent, upgrades, trades, and renter offers.
- Gambling/game economy surfaces include slots, crash, blackjack, poker, Go Fish, and heists.

### Minigames

- Board/table games: chess, checkers, connect 4, tic-tac-toe, battleships, 2048.
- Word/question games: trivia, Wordle, hangman, word search, crossword.
- Guessing games: GeoGuesser, combined flags, region border, and higher/lower for population, area, and word frequency.
- Wordle reminder manager tracks streak reminders and sends scheduled channel/thread/DM reminders.

### Collectables

- Scheduled collectable events in a configured collector channel.
- Users collect items by replying to generated question prompts before the event expires.
- Collection types: Minecraft mobs, Rainbow Six operators, and countries.
- Collectables use rarity weights and can be enabled/disabled by type or individual item.

### Image, Media, And Text Utilities

- `/image` registry covers API-based image fetches and Pexels categories, including nature, space, food, NASA, and several animal categories.
- Avatar/image transforms: deepfry, flag overlay, LGBT overlay, captioned cat image, and petpet GIF generation.
- HTTP status image commands.
- FFmpeg-backed reaction conversion for supported video, audio, and image attachments: video to MP4, audio to MP3, image to PNG.
- GitHub Gist reaction workflow for text/code/log attachments when enabled.
- LaTeX image rendering.
- Log analysis and message-context code execution helpers.

### External Notifiers

- YouTube upload notifier using channel RSS feeds and video details.
- Twitch go-live notifier using Twitch4J.
- Reddit post notifier.
- Steam update notifier and Steam store sale/fest notifier.
- Scraped game-news notifiers for Minecraft, Rainbow Six Siege, Rocket League, League of Legends, and VALORANT.
- Shared dashboard and command configuration for target Discord channel and mention behavior.

### AI And NSFW Classification

- OpenAI-backed message responder, gated by server enable switch, channel whitelist, and user blacklist.
- Local Discord context tool support in the AI responder.
- ONNX-based NSFW classifiers and artist NSFW cache using the configured dataset root.
- Per-user opt-in for artist NSFW filtering.

### Dashboard

The repository includes a Java Javalin backend and a Next.js dashboard frontend.

Backend API areas:

- Health, sessions, user profile, guild config catalog, guild channels, roles, and member search.
- Settings APIs for starboard, levelling, logging, warnings, economy, welcome, birthday, collectables, opt-in channels, suggestions, AI, chat revival, NSFW, threads, misc, automod, modmail, sticky messages, counting, and voice channel notifiers.
- Management APIs for notifiers, warning records, report history, suggestions, quotes, tags, modmail tickets, sticky messages, counting channels, and voice channel notifiers.

Frontend dashboard pages:

- Overview/search, birthday, collectables, welcome, suggestions, quotes, tags, opt-in channels, notifiers, voice channel notifiers, AI, chat revival, threads, misc, counting, economy, NSFW, levelling, starboard, reports, warnings, automod, modmail, sticky messages, and logging.

## Data And Assets

- Bundled resources include cards, chess pieces, Battleships ship art, Wordle image, leaderboard/rank-card assets, petpet frames, economy item art, fingerprints, fonts, topics, work/crime/rob responses, country land area data, rules of the internet, objects for drawing prompts, Piston metadata, and collectable JSON data.
- The Next.js site includes dashboard UI components, auth routes, Discord proxy routes, notifier UI, settings forms, and offline/search helpers.

## Suggested Additions

### Moderation And Trust

- Case management system: unify bans, kicks, warnings, reports, timeouts, tempbans, and modmail into a numbered moderation case ledger with evidence, staff notes, attachments, expiry, appeal status, and dashboard search.
- Appeal portal: let users submit ban/warn appeals through Discord buttons or the web dashboard, then allow moderators to vote, discuss, and resolve the appeal.
- Raid mode: emergency lockdown command/dashboard toggle that raises verification requirements, slows channels, disables invites, watches join velocity, and posts a live incident summary.
- Anti-alt risk signals: score new accounts using account age, join bursts, avatar/name similarity, invite source, mutual servers, and recent automod hits.
- Message quarantine: hide suspected scam/invite/image-spam messages into a staff review queue before deleting or sanctioning.
- Moderator action review: weekly digest of high-impact staff actions, reversed actions, sanction rates, and unresolved reports.

### Utility And Admin

- `/schedule`: recurring event/reminder system with calendar-style schedules, time zone support, RSVP buttons, and automatic reminder threads.
- `/translate`: translate text or replied messages with language detection and per-server preferred languages.
- `/timezone`: save user time zones and convert times mentioned in chat.
- `/serveraudit`: checks bot permissions, broken config references, deleted channels/roles in settings, stale notifiers, and dashboard/API health.
- `/snapshot`: export selected guild configuration to JSON and restore it later, with dry-run validation.
- `/cleanup`: find stale roles, empty channels, dead threads, expired invites, inactive voice notifiers, and unused bot-created categories.
- `/docs`: searchable command/config documentation generated from command metadata and dashboard config descriptors.

### Community Engagement

- Quest system: weekly server quests such as answer trivia, win a game, post in showcase, finish a counting streak, or help in support channels.
- Achievements and badges: cross-feature badges for games, economy milestones, moderation participation, birthdays, collectables, and streaks.
- Server seasons: resettable seasonal leaderboards for XP, economy, minigames, counting, and collectables with rewards at season end.
- Reputation/kudos: peer recognition with cooldowns, leaderboards, anti-farming checks, and optional role rewards.
- Event brackets: tournament manager for chess, checkers, Connect 4, trivia, or custom event signups.
- Birthday/event cards: generated celebratory images using existing font/image infrastructure.

### Economy And Games

- Marketplace: user-to-user shop for collectables, rank-card items, property rental contracts, and economy items.
- Crafting/upgrades: combine collectables or shop items into cosmetics, boosts, or limited badges.
- Daily/weekly quests for money and XP, integrated with the reward command.
- Stock/crypto parody market: simulated market with server-local prices, news events, and risk controls.
- Fishing/mining/farming idle loops: low-pressure economy activities with rare drops and collectable tie-ins.
- Cooperative boss/heist events: multi-user timed events using buttons, roles, skills, and pooled risk/reward.
- Game lobbies: shared lobby system for chess/checkers/connect4/battleships/trivia with open invites and rematches.

### Dashboard And Operations

- Full command explorer page: command list, permissions, cooldowns, scopes, subcommands, and enable/disable switches per command.
- Config diff and audit log: show who changed settings, when, and what changed, with rollback.
- Dashboard onboarding wizard: first-run setup for logging, moderation, starboard, suggestions, economy, levelling, and modmail.
- Real-time bot status panel: gateway ping, shard/session state, scheduler status, queue sizes, failing notifiers, and recent exceptions.
- Notifier diagnostics: last poll time, last delivered item, failures, channel permission checks, and manual test-send.
- Data retention controls: dashboard UI for pruning old logs, warnings, reports, transcripts, reminders, and stale economy accounts.

### AI Features

- Staff summarizer: summarize reports, modmail tickets, warning history, and long message threads for moderators.
- AI-assisted automod drafts: suggest sanction reason text, but keep human approval for actions.
- Server knowledge bot: configurable FAQ/knowledge base built from pinned messages, tags, docs, and selected channels.
- AI image alt-text: generate accessible descriptions for images in showcase/starboard channels.
- Toxicity trend summaries: aggregate non-punitive trend reports for staff without exposing raw private user profiling.

### Developer Quality

- Generate command metadata automatically into JSON/Markdown from `CoreCommand` instances to prevent docs drift.
- Add integration tests for command registration, dashboard route coverage, and config option descriptors.
- Add smoke tests for every dashboard API route with fake guild/config data.
- Add health checks for external API keys and optional dependencies like FFmpeg, OpenAI, Twitch, Steam, GitHub, and Pexels.
- Add migration/versioning support for MongoDB schema changes and config defaults.
