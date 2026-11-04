# TurtyBot Privacy Policy

**Effective date: 4 November 2026**

TurtyBot is a multipurpose Discord bot with an associated web dashboard. This policy explains what personal information we process, why we use it, who can receive it, and how you can request access, correction, or deletion.

TurtyBot is operated by **TurtyWurty, based in the United Kingdom**, referred to here as "we", "us", or "our". For privacy questions or requests about your information, email **[turtybot@gmail.com](mailto:turtybot@gmail.com)**.

This policy covers the TurtyBot service operated by us. If someone else hosts their own copy of the open-source bot, contact that operator about their use of your information. Discord and other external services also process information under their own privacy policies.

## Information we process

The information involved depends on the commands you use, the features enabled in your server, and the permissions granted to TurtyBot. We receive information from Discord, from you, and from other members or moderators who use features involving you, such as reports and quotes.

- **Discord account and server information:** user IDs, usernames, display names, avatars, server IDs and names, channel and role information, membership information, permissions, and relevant event timestamps.
- **Messages and attachments:** command inputs, message text, message IDs and links, reactions, attachment information, and files needed for the feature being used. The bot may process ordinary server messages for moderation, levelling, counting, or other enabled features even if they do not contain a command. Message content can also be stored or reposted through features such as quotes, suggestions, starboard, tags, reminders, saved embeds, sticky messages, and moderation logs.
- **Moderation and support records:** warnings, reports, reasons for moderation actions, temporary bans, blocked-user records, and the users and moderators involved. Modmail records can include ticket details, opening and closing reasons, message text, authors, timestamps, and attachment, embed, or sticker references in transcripts.
- **Game and community activity:** experience points, levels, leaderboards, game results and streaks, quests and related activity records, collectables, inventories, virtual economy balances and transactions, marketplace listings, and feature cooldowns. The bot's virtual economy records describe in-bot activity.
- **Preferences and information you provide:** user and server settings, notification subscriptions, reminder text and schedules, saved roles, custom profile settings, and your day, month, and year of birth if you use the birthday feature.
- **Dashboard sign-in information:** your Discord profile, information about servers you can manage and your permissions, a session identifier, sign-in and expiry times, and cookies used for authentication.
- **Operational information:** errors, diagnostic records, and identifiers or request details needed to investigate problems. Website requests pass through Cloudflare and our nginx web server. Connection and request information, such as IP addresses, browser information, requested URLs, and timestamps, may be processed or logged to deliver the website, maintain security, and investigate errors.

Setting a birthday allows other users to look up your birthday and the age you will turn using the bot. Birthday announcements can also be enabled in individual servers. Turning announcements off does not delete the stored birthday or disable birthday lookups.

## How we use information

We use information to run commands and enabled features, remember settings and progress, deliver reminders and notifications, support server moderation, provide the dashboard, prevent abuse, troubleshoot problems, and respond to privacy requests or legal obligations.

Some features make information visible to other people. For example, leaderboards and marketplace listings show activity to other members; quotes and starboard repost content; and modmail or moderation records are available to staff with access to the relevant channels or dashboard tools. The visibility of Discord posts also depends on the server's channel permissions.

We do not sell personal information, use it for advertising, or use Discord messages to train our own AI models.

## AI replies

When a server enables AI replies and allows them in a channel, mentioning TurtyBot can send your message text and display name to OpenAI to generate a response. The request can include recent AI conversation history from that channel, including exchanges involving other members. The AI can also request the server name, channel name, member count, your display name, and the current date and time. It can use web search when answering questions.

The bot keeps a limited AI conversation cache in memory, which expires after 30 minutes without access and may be evicted sooner. Continued use of the channel's AI conversation can keep that cache active. This is separate from messages stored by Discord and from OpenAI's retention of requests and responses.

We use OpenAI's default API data settings and have not opted into sharing API content for model training. Under those defaults, API content is not used to train OpenAI's models. Abuse-monitoring logs may be retained for up to 30 days, with exceptions for legal or safety needs. The Responses API used by TurtyBot also stores response data by default for at least 30 days, subject to OpenAI's stated exceptions. See [OpenAI's API data controls](https://developers.openai.com/api/docs/guides/your-data) for details. These provider retention periods are separate from the bot's local conversation cache.

Do not send passwords, access tokens, or sensitive personal information in an AI request. Server administrators can disable AI replies, change allowed channels, or block particular users from the feature.

## External services and sharing

We share or transmit information as needed to provide the feature you use, operate the service, or meet legal requirements:

- **Discord** receives bot messages, uploaded results, and other interactions needed to operate the bot and dashboard.
- **OpenAI** receives the information described in the AI replies section when that feature is used.
- **GitHub Gist:** when Gists are enabled in a server, **any user can request a Gist of the supported text attachments on your messages**, provided neither that user nor you has opted out of optional data processing. The requester reacts to the message and confirms an upload prompt naming GitHub and explaining the visibility; your separate approval is not requested. GitHub receives the attachments' filenames and contents. The bot creates an unlisted Gist and posts its link in Discord; unlisted does not mean private, and anyone with the link can access or copy it. Requesters must have permission to share the content, including other people's information it contains. Cancelling or letting the prompt expire sends nothing to GitHub. For new uploads, we retain the Gist and GitHub account IDs, source author and requester IDs, source server/channel/message IDs, creation time, an opaque tracking reference, and cleanup state so we can handle access and deletion requests. These tracking records do not contain filenames or attachment contents. `/privacy delete` attempts to remove tracked bot-created Gists linked to you as either the author or the requester, including when another member requested the upload. Older untracked Gists require manual review. Deleting the original Discord message or using `/privacy opt-out` alone does not delete existing Gists. Contact us about additional copies; forks, downloads and other people's copies may remain beyond our control.
- **Compiler Explorer (godbolt.org)** receives code and execution options through Run Code only after the message author approves the transfer prompt. Other members cannot submit your message through this command. The first supported code block or attachment is used, and the result is posted in the channel. Brainfuck execution runs locally instead of using Compiler Explorer. See [Compiler Explorer's privacy policy](https://github.com/compiler-explorer/compiler-explorer/blob/main/docs/Privacy.md) for its handling of submissions.
- **Lookup and utility providers** receive the inputs needed for their feature. For example, Open-Meteo receives place searches and coordinates for weather lookups. Other commands may query services such as GitHub, Steam, Reddit, Twitch, YouTube, Minecraft services, or our utility API at `api.turtywurty.dev`.
- **Hosting and infrastructure providers** process information needed to host the service. Contabo hosts the bot and website in the European Union, with a data-processing agreement in place for the covered hosting services. MongoDB hosts the database on AWS infrastructure in Ireland, in the `eu-west-1` region.
- **Cloudflare** handles website traffic as a proxy and provides security and delivery services. It processes connection and request information when you visit the website. nginx runs on our website server to handle and forward requests; it is software we operate, rather than a separate service receiving data from us.
- **People with appropriate access** can receive information through feature outputs, moderation channels, support tickets, and dashboard permissions. The operator may access relevant records to maintain the service or handle a request.
- **Authorities or other legally entitled recipients** may receive information where disclosure is required by law.

External services have their own privacy policies, including [Discord](https://discord.com/privacy), [OpenAI](https://openai.com/policies/privacy-policy/), [GitHub](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement), and [Cloudflare](https://www.cloudflare.com/privacypolicy/). Deleting the original Discord message does not automatically delete a Gist, a modmail transcript, or a copy posted elsewhere by the bot.

## Dashboard cookies

The dashboard uses cookies to secure the Discord sign-in process and keep you signed in. Discord sign-in requests the `identify` and `guilds` permissions to retrieve your profile and the server information needed for the dashboard.

The sign-in security cookie lasts up to 10 minutes. Session cookies and cookies containing Discord access and refresh tokens last up to seven days. Tokens have their own validity periods. These cookies are HTTP-only, use SameSite protection, and are marked Secure in production. Signing out removes the current dashboard session and authentication cookies. It does not delete your other bot records.

The dashboard application does not include advertising trackers or visitor analytics integrations. Its own cookies are used for authentication and sign-in security. Cloudflare may also set security or traffic-management cookies, depending on the features active for the website; see [Cloudflare's cookie information](https://developers.cloudflare.com/fundamentals/reference/policies-compliances/cloudflare-cookies/). Cloudflare's processing of website traffic is separate from the application's sign-in cookies.

## How long information is kept

Different features retain information differently:

- Dashboard sessions expire after seven days and are scheduled for automatic database removal after expiry, or are deleted when you sign out.
- If you opt out of optional data processing, we keep only your Discord user ID in a separate opt-out list for as long as the choice applies. This lets us prevent new optional profiles after deletion. `/privacy opt-in` removes that identifier from the list. This marker is separate from existing feature records and privacy requests.
- AI conversation context is kept as described above.
- Message logging uses an in-memory cache limited to the latest 100 cached messages per channel. Copies sent to Discord log channels remain separate from that cache.
- Optional personal profiles and saved feature data are removed after **12 calendar months of inactivity**, using UTC. This includes levelling, personal game and quest progress, economy balances and inventories, birthdays, saved embeds, notification subscriptions, tags, quotes, suggestions, starboard entries, sticky messages and marketplace listings. Inactivity is measured across the bot using messages, reactions, voice activity, feature commands and dashboard use that the bot observes. Background maintenance does not reset the clock. You may lose saved progress, virtual items and content after inactivity. Existing records without a reliable activity timestamp receive a new 12-month window when the cleanup system first observes them; opted-out legacy records use the system's rollout date. Cleanup runs after startup and daily, with a small allowance for activity timestamp batching, and removes eligible data on the next successful run.
- We store your Discord ID and last-observed activity time to apply that profile retention period, for accounts with stored profile data. This activity record contains no message text. It is removed with expired profiles or through `/privacy delete`. After expiry, an in-memory identifier can suppress delayed profile writes until you interact with the bot again or the process restarts.
- Closed modmail tickets and their stored database transcripts are removed **90 days after closure**. Undated closed tickets and transcript chunks without a ticket receive a 90-day period from when cleanup first identifies them. Open tickets are retained until closed or otherwise removed. This database cleanup does not remove separate Discord channel messages or copies controlled by other recipients.
- Warnings with a stored expiry timestamp are removed **90 days after that expiry**. Explicitly resolved reports are removed 90 days after resolution, or after first observing resolution if its date is missing. Active warnings, warnings without a stored expiry, unresolved reports, bans, blocks and pending unban schedules are preserved until resolved, cleared or handled through a request. Pending reminders are preserved for delivery or deletion.
- Sticky-role snapshots expire after six calendar months from when roles were saved on leaving a server, using UTC. Expired snapshots are not restored on rejoining and are included in daily cleanup. Removing deleted or invalid roles from a snapshot does not extend its retention period.
- Gist tracking references are retained while the bot-created Gist exists or its removal remains unresolved. Successful removal deletes the tracking record. Failed or uncertain attempts remain recorded for retry or operator review rather than expiring automatically. A creation attempt can retain an opaque reference if GitHub's response or a database update fails; we use it to investigate whether an upload was made and remove it where needed.
- Server settings and ongoing shared server state, such as the current counting game, remain available while needed for their configured features, until reset, disabled and removed, or handled through a deletion request. The personal-profile inactivity period does not reset shared server progress. Leaving a server or removing the bot does not automatically erase every stored record. You can request deletion by emailing us as described below.
- nginx access and error logs are rotated daily and retained for 90 days. This server log policy is separate from records retained by Cloudflare and other service providers under their own terms and service settings.
- File conversions and generated GIFs normally remove their managed working files after delivery or a handled failure. The daily cleanup removes abandoned files in the bot's dedicated temporary directory once they have been unused for **24 hours**, while preserving active work and recently modified files. Older temporary files outside that managed directory still require manual cleanup. Approved Gist uploads read up to 8 MiB of text attachments into memory without writing new working files to disk.
- We do not currently create or retain our own backups of the service's data. Discord and other external services may retain their own copies under their terms and privacy policies.

Deletion requests can start automatic cleanup through `/privacy delete`, with remaining records handled through operator review. We will promptly delete information when required by a valid request, Discord's developer terms, or applicable law, except where retention is required by law. A deletion request retains your Discord user ID, the request status, and references needed to locate remaining copies while it is being resolved. After the operator marks it resolved, daily cleanup removes personal details and deletes the remaining minimal request record **30 days after resolution**. A resolved request without a resolution date starts that period when first processed by cleanup. Unresolved requests do not expire automatically.

## Security and international processing

Dashboard access uses authenticated sessions and permission checks. The bot and its service providers process information on the infrastructure used to deliver the service. No online service can guarantee complete security.

Administrative access to the hosting accounts and database is limited to the operator, TurtyWurty. We use access controls and security measures to protect the service. This administrative access is separate from server staff's access to records through bot features, Discord channels, and dashboard permissions.

The operator is based in the United Kingdom, the bot and website are hosted in the European Union, and the database is hosted in Ireland. External services, including Discord, OpenAI, and Cloudflare, may process information in other countries. We use providers' standard service arrangements and have not arranged custom data-residency controls.

The terms applicable to each provider determine its processing locations and any international-transfer safeguards. For example, [MongoDB's data-processing agreement](https://www.mongodb.com/legal/data-processing-agreement) and [Cloudflare's data-processing addendum](https://www.cloudflare.com/cloudflare-customer-dpa/) describe safeguards such as standard contractual clauses and UK transfer terms where applicable. You can contact us for information about the provider terms and safeguards relevant to your data.

## Legal bases and automated moderation

Where UK or EU data protection law applies, we rely on legitimate interests to process information needed to respond to commands, provide enabled community features, maintain user settings and progress, administer servers, and protect the service against abuse. These interests are ours and those of the server communities using the bot. Processing on this basis must be necessary and must not be overridden by your interests, rights, and freedoms. You can object to processing on this basis by contacting us.

Where we must process information to meet a legal requirement, including responding to statutory data-protection requests, we rely on compliance with that legal obligation. Choosing to use a feature does not waive your data-protection rights.

Enabled moderation features can automatically remove messages or ban accounts based on rules such as blocked invites, known scam domains, or repeated image spam. These actions can affect your ability to participate in a server. Contact that server's moderators to ask for a human review of a moderation action, and contact us about the personal information used by the bot.

## Your choices and requests

Use `/privacy info` to read a summary and find this policy. `/privacy export` privately provides a ZIP of your structured database records across servers. The export excludes session identifiers, other people's message entries and identities, and staff-only moderation text; shared records are provided as account-related summaries. Large exports and requests for additional records can be handled by email.

`/privacy opt-out` disables most commands and optional background features for your account across all servers, including levelling, games, AI replies and dashboard sign-in. Commands that are unavailable explain that you have opted out. Privacy commands and basic help and status commands remain available. Existing reminders are paused, dashboard sessions are revoked, and affected AI and message caches are cleared. Opting out does not erase existing feature records. We retain only your Discord user ID in the separate opt-out list to honour your choice.

`/privacy opt-in` removes your identifier from the opt-out list and enables optional features again. Future reminders resume, while overdue reminders are discarded. Using features again may create or update records; deleted records and progress are not restored.

`/privacy delete` asks for confirmation before permanently removing supported database records, deleting tracked bot-created Gists linked to you as source author or requester, cleaning supported references in shared records, revoking dashboard sessions, cancelling reminders, and clearing affected AI and message caches. It also opts you out before cleanup to prevent optional features from recreating your profile, retaining the minimal opt-out identifier described above. You do not need a server administrator's permission. Failed Gist removals keep their tracking references for retry or review; the result reports partial failure when this happens. The bot saves a request for operator review of remaining copies, including older untracked Gists, active restrictions, free-text references, or steps that fail. Known references to Gists, Discord posts and ticket channels are retained with that request so the remaining copies can be located. The result explains that automatic cleanup is not confirmation that every copy has been erased. Deletion does not itself remove Discord bans.

The optional-data opt-out does not disable server safety moderation, handling of privacy requests, or necessary security and operational processing described in this policy. Actions or requests already sent to Discord or another provider may finish. Contact us if you want to object to other processing or discuss remaining records or copies.

You can choose whether to use optional commands, sign out of the dashboard, and revoke the dashboard's authorization in Discord. Server administrators can adjust enabled features and the bot's permissions or remove the bot. Disabling a feature or revoking access does not itself erase information already stored.

To request access to, correction of, or deletion of your information, email **[turtybot@gmail.com](mailto:turtybot@gmail.com)**. Include your Discord user ID, relevant server IDs if known, and what you want us to do. We may need to verify that the request relates to you or that you are authorized to act for a server. Do not send your Discord password or authentication token.

Depending on the law that applies to you, you may also have rights to restrict processing, object to processing, obtain a portable copy of information, withdraw consent where processing relies on consent, or request human review of certain automated decisions. We will respond within the applicable legal time limits and explain any lawful limits on a request. Where Discord's developer terms require deletion of API data, we will follow those requirements.

Our handling of a request covers the records and copies we control. Discord, server administrators, other members, and external services may control additional copies; we can explain which recipients are involved where relevant. You can also complain to your local data protection authority. In the UK, this is the [Information Commissioner's Office](https://ico.org.uk/make-a-complaint/).

## Children

TurtyBot is intended for people who meet Discord's minimum age requirements for their country. If you believe we hold personal information about a child who does not meet those requirements, contact us so we can investigate and delete it where appropriate.

## Changes to this policy

We may update this policy when features, service providers, or data practices change. We will update the effective date and make the revised policy available at the same location. Where a change materially affects how personal information is used, we will provide an appropriate notice before the change takes effect where required.
