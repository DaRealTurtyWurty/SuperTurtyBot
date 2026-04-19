import Link from "next/link";
import type {ReactNode} from "react";
import {
    FaArrowRight,
    FaBell,
    FaHashtag,
    FaMessage,
    FaTriangleExclamation,
} from "react-icons/fa6";
import {
    fetchDashboardAutomodSettings,
    fetchDashboardCountingSettings,
    fetchDashboardGuildConfig,
    fetchDashboardModmailSettings,
    fetchDashboardModmailTickets,
    fetchDashboardNotifiers,
    fetchDashboardOptInChannelsSettings,
    fetchDashboardStickyMessages,
    fetchDashboardSuggestions,
    fetchDashboardWarnings,
    isDashboardApiError
} from "@/lib/dashboard-api";

function readBoolean(config: Record<string, unknown> | undefined, key: string) {
    const value = config?.[key];
    return typeof value === "boolean" ? value : undefined;
}

function readText(config: Record<string, unknown> | undefined, key: string) {
    const value = config?.[key];
    return typeof value === "string" || typeof value === "number" ? value.toString() : undefined;
}

function readList(config: Record<string, unknown> | undefined, key: string): string[] {
    const value = config?.[key];
    return Array.isArray(value) ? value.map(entry => String(entry)) : [];
}

function formatStatus(value: boolean | undefined) {
    if (value === undefined) {
        return "Unknown";
    }

    return value ? "Enabled" : "Disabled";
}

function formatCount(value: number) {
    return value.toLocaleString();
}

function MetricCard({
    icon,
    label,
    value,
    detail,
    href,
    tone
}: {
    icon: ReactNode;
    label: string;
    value: string;
    detail: string;
    href: string;
    tone: "sky" | "emerald" | "amber" | "violet";
}) {
    const toneClasses = {
        sky: "border-sky-400/20 bg-sky-400/10 text-sky-200",
        emerald: "border-emerald-400/20 bg-emerald-400/10 text-emerald-200",
        amber: "border-amber-400/20 bg-amber-400/10 text-amber-200",
        violet: "border-violet-400/20 bg-violet-400/10 text-violet-200"
    }[tone];

    return <Link
        href={href}
        className="group border border-slate-800/80 bg-slate-950/60 p-5 transition hover:border-slate-600 hover:bg-slate-900/70"
    >
        <div className="flex items-start justify-between gap-4">
            <div className={`flex h-10 w-10 items-center justify-center border ${toneClasses}`}>
                {icon}
            </div>
            <FaArrowRight className="mt-1 h-4 w-4 text-slate-600 transition group-hover:translate-x-0.5 group-hover:text-slate-300" />
        </div>
        <p className="mt-4 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{label}</p>
        <p className="mt-2 text-2xl font-bold text-white">{value}</p>
        <p className="mt-2 text-sm text-slate-400">{detail}</p>
    </Link>;
}

function ModuleCard({
    title,
    href,
    status,
    detail,
    ready
}: {
    title: string;
    href: string;
    status: string;
    detail: string;
    ready: boolean;
}) {
    return <Link
        href={href}
        className={`block border bg-slate-950/60 p-5 transition hover:bg-slate-900/70 ${
            ready ? "border-emerald-400/20 hover:border-emerald-400/40" : "border-slate-800/80 hover:border-slate-600"
        }`}
    >
        <div className="flex items-start justify-between gap-4">
            <div>
                <p className="text-sm font-semibold text-white">{title}</p>
                <p className="mt-1 text-sm text-slate-400">{detail}</p>
            </div>
            <span className={`border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] ${
                ready
                    ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-200"
                    : "border-amber-400/30 bg-amber-400/10 text-amber-200"
            }`}>
                {status}
            </span>
        </div>
    </Link>;
}

function AttentionItem({
    title,
    detail,
    href
}: {
    title: string;
    detail: string;
    href: string;
}) {
    return <Link
        href={href}
        className="flex items-start justify-between gap-3 border border-slate-800/80 bg-slate-950/60 px-4 py-3 transition hover:border-slate-600 hover:bg-slate-900/70"
    >
        <span>
            <span className="block text-sm font-semibold text-white">{title}</span>
            <span className="mt-1 block text-sm text-slate-400">{detail}</span>
        </span>
        <FaArrowRight className="mt-1 h-4 w-4 shrink-0 text-slate-500" />
    </Link>;
}

export default async function GuildOverviewPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;

    const [
        snapshot,
        notifiers,
        warnings,
        counting,
        stickyMessages,
        modmailTickets,
        suggestions,
        optInChannels,
        automod,
        modmailSettings
    ] = await Promise.all([
        fetchDashboardGuildConfig(guildId).catch(error => {
            if (isDashboardApiError(error)) {
                return null;
            }

            throw error;
        }),
        fetchDashboardNotifiers(guildId).catch(() => null),
        fetchDashboardWarnings(guildId).catch(() => null),
        fetchDashboardCountingSettings(guildId).catch(() => null),
        fetchDashboardStickyMessages(guildId).catch(() => null),
        fetchDashboardModmailTickets(guildId).catch(() => null),
        fetchDashboardSuggestions(guildId).catch(() => null),
        fetchDashboardOptInChannelsSettings(guildId).catch(() => null),
        fetchDashboardAutomodSettings(guildId).catch(() => null),
        fetchDashboardModmailSettings(guildId).catch(() => null)
    ]);

    if (!snapshot) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            The guild overview could not be loaded from the dashboard API.
        </div>;
    }

    const config = snapshot.config;
    const allWarnings = warnings?.warnings.length ?? 0;
    const openTickets = modmailTickets?.tickets.filter(ticket => ticket.open).length ?? 0;
    const totalTickets = modmailTickets?.tickets.length ?? 0;
    const notifierCount = notifiers?.totalCount ?? 0;
    const countingChannels = counting?.channels.length ?? 0;
    const stickyCount = stickyMessages?.stickyMessages.length ?? 0;
    const suggestionCount = suggestions?.totalCount ?? 0;
    const optInCount = optInChannels?.optInChannelIds.length ?? 0;
    const modmailRoleCount = modmailSettings?.moderatorRoleIds.length ?? readList(config, "modmail_moderator_roles").length;
    const aiWhitelistCount = readList(config, "ai_channel_whitelist").length;
    const aiBlacklistCount = readList(config, "ai_user_blacklist").length;
    const threadChannelCount = readList(config, "auto_thread_channels").length;
    const disabledLevellingCount = readList(config, "disabled_levelling_channels").length;
    const levellingRoleCount = readList(config, "level_roles").length;
    const xpBoostChannelCount = readList(config, "xp_boosted_channels").length;
    const xpBoostRoleCount = readList(config, "xp_boosted_roles").length;
    const showcaseChannelCount = readList(config, "showcase_channels").length;
    const inviteWhitelistCount = readList(config, "discord_invite_whitelist_channels").length;
    const nsfwChannelCount = readList(config, "nsfw_channels").length;
    const welcomeChannel = readText(config, "welcome_channel");
    const birthdayChannel = readText(config, "birthday_channel");
    const suggestionsChannel = readText(config, "suggestions");
    const loggingChannel = readText(config, "logging_channel");
    const modLoggingChannel = readText(config, "mod_logging");
    const starboardChannel = readText(config, "starboard");
    const chatRevivalChannel = readText(config, "chat_revival_channel");
    const countSummary = counting?.maxCountingSuccession ?? Number(readText(config, "max_counting_succession") ?? 0);

    const moduleCards = [
        {
            title: "Starboard",
            href: `/dashboard/${guildId}/starboard`,
            status: readBoolean(config, "starboard_enabled") ? "Ready" : "Disabled",
            ready: Boolean(readBoolean(config, "starboard_enabled")),
            detail: starboardChannel
                ? `Channel ${starboardChannel} · minimum ${readText(config, "minimum_stars") ?? "?"} stars · ${showcaseChannelCount} showcase channels`
                : "No starboard channel selected."
        },
        {
            title: "Welcome",
            href: `/dashboard/${guildId}/welcome`,
            status: welcomeChannel ? "Ready" : "Missing",
            ready: Boolean(welcomeChannel),
            detail: `${formatStatus(readBoolean(config, "should_announce_joins"))} joins, ${formatStatus(readBoolean(config, "should_announce_leaves"))} leaves in ${welcomeChannel ?? "no channel"}`
        },
        {
            title: "Suggestions",
            href: `/dashboard/${guildId}/suggestions`,
            status: suggestionsChannel ? "Ready" : "Missing",
            ready: Boolean(suggestionsChannel),
            detail: suggestionsChannel ? `Channel ${suggestionsChannel} · ${suggestionCount.toLocaleString()} suggestions` : "No suggestions channel set."
        },
        {
            title: "Logging",
            href: `/dashboard/${guildId}/logging`,
            status: loggingChannel || modLoggingChannel ? "Ready" : "Missing",
            ready: Boolean(loggingChannel || modLoggingChannel),
            detail: [
                loggingChannel ? `Log channel ${loggingChannel}` : null,
                modLoggingChannel ? `Mod log ${modLoggingChannel}` : null
            ].filter(Boolean).join(" · ") || "No logging channels configured."
        },
        {
            title: "AI",
            href: `/dashboard/${guildId}/ai`,
            status: readBoolean(config, "ai_enabled") ? "Ready" : "Disabled",
            ready: Boolean(readBoolean(config, "ai_enabled")),
            detail: `${aiWhitelistCount} whitelisted channels · ${aiBlacklistCount} blocked users`
        },
        {
            title: "Chat Revival",
            href: `/dashboard/${guildId}/chat-revival`,
            status: readBoolean(config, "chat_revival_enabled") ? "Ready" : "Disabled",
            ready: Boolean(readBoolean(config, "chat_revival_enabled")),
            detail: chatRevivalChannel ? `Channel ${chatRevivalChannel} · every ${readText(config, "chat_revival_time") ?? "?"} hours` : "No revival channel selected."
        },
        {
            title: "Threads",
            href: `/dashboard/${guildId}/threads`,
            status: threadChannelCount > 0 || Boolean(readBoolean(config, "should_moderators_join_threads")) ? "Ready" : "Missing",
            ready: threadChannelCount > 0 || Boolean(readBoolean(config, "should_moderators_join_threads")),
            detail: `${threadChannelCount} auto-thread channels · moderators ${formatStatus(readBoolean(config, "should_moderators_join_threads"))}`
        },
        {
            title: "Modmail",
            href: `/dashboard/${guildId}/modmail`,
            status: modmailRoleCount > 0 ? "Ready" : "Missing",
            ready: modmailRoleCount > 0,
            detail: `${modmailRoleCount} moderator roles · ${modmailTickets?.tickets.length ?? 0} tickets`
        },
        {
            title: "Counting",
            href: `/dashboard/${guildId}/counting`,
            status: countingChannels > 0 ? "Ready" : "Missing",
            ready: countingChannels > 0,
            detail: `${countingChannels} channel${countingChannels === 1 ? "" : "s"} · max succession ${countSummary || "?"}`
        },
        {
            title: "Sticky Messages",
            href: `/dashboard/${guildId}/sticky-messages`,
            status: stickyCount > 0 ? "Ready" : "Missing",
            ready: stickyCount > 0,
            detail: `${stickyCount} sticky message${stickyCount === 1 ? "" : "s"}`
        },
        {
            title: "Warnings",
            href: `/dashboard/${guildId}/warnings`,
            status: warnings ? "Ready" : "Unknown",
            ready: Boolean(warnings),
            detail: warnings ? `${allWarnings.toLocaleString()} warnings recorded` : "Warning history could not be loaded."
        },
        {
            title: "Economy",
            href: `/dashboard/${guildId}/economy`,
            status: readBoolean(config, "economy_enabled") ? "Ready" : "Disabled",
            ready: Boolean(readBoolean(config, "economy_enabled")),
            detail: `Currency ${readText(config, "economy_currency") ?? "unset"} · donations ${formatStatus(readBoolean(config, "donate_enabled"))}`
        },
        {
            title: "Levelling",
            href: `/dashboard/${guildId}/levelling`,
            status: readBoolean(config, "levelling_enabled") ? "Ready" : "Disabled",
            ready: Boolean(readBoolean(config, "levelling_enabled")),
            detail: `${levellingRoleCount} level roles · ${disabledLevellingCount} disabled channels · boosts ${xpBoostChannelCount}/${xpBoostRoleCount}`
        },
        {
            title: "NSFW",
            href: `/dashboard/${guildId}/nsfw`,
            status: nsfwChannelCount > 0 || Boolean(readBoolean(config, "artist_nsfw_filter_enabled")) ? "Ready" : "Missing",
            ready: nsfwChannelCount > 0 || Boolean(readBoolean(config, "artist_nsfw_filter_enabled")),
            detail: `${nsfwChannelCount} NSFW channels · artist filter ${formatStatus(readBoolean(config, "artist_nsfw_filter_enabled"))}`
        },
        {
            title: "Automod",
            href: `/dashboard/${guildId}/automod`,
            status: automod?.inviteGuardEnabled || automod?.scamDetectionEnabled || automod?.imageSpamAutoBanEnabled ? "Ready" : "Disabled",
            ready: Boolean(automod?.inviteGuardEnabled || automod?.scamDetectionEnabled || automod?.imageSpamAutoBanEnabled),
            detail: `${inviteWhitelistCount} invite whitelist channels · image spam ${formatStatus(automod?.imageSpamAutoBanEnabled)}`
        },
        {
            title: "Opt-In Channels",
            href: `/dashboard/${guildId}/opt-in-channels`,
            status: optInCount > 0 ? "Ready" : "Missing",
            ready: optInCount > 0,
            detail: `${optInCount} opt-in channel${optInCount === 1 ? "" : "s"}`
        },
        {
            title: "Birthday",
            href: `/dashboard/${guildId}/birthday`,
            status: birthdayChannel ? "Ready" : "Missing",
            ready: Boolean(birthdayChannel),
            detail: `${birthdayChannel ? `Channel ${birthdayChannel}` : "No channel set"} · announcements ${formatStatus(readBoolean(config, "announce_birthdays"))}`
        }
    ];

    const attentionItems = [
        !starboardChannel ? {title: "Starboard channel missing", detail: "Set the starboard channel so starred messages have somewhere to go.", href: `/dashboard/${guildId}/starboard`} : null,
        !welcomeChannel ? {title: "Welcome channel missing", detail: "Join and leave announcements are configured but no channel is selected.", href: `/dashboard/${guildId}/welcome`} : null,
        !suggestionsChannel ? {title: "Suggestions channel missing", detail: "Suggestion settings are live, but the channel is not chosen yet.", href: `/dashboard/${guildId}/suggestions`} : null,
        !loggingChannel && !modLoggingChannel ? {title: "Logging channels missing", detail: "Log channels are not configured.", href: `/dashboard/${guildId}/logging`} : null,
        countingChannels === 0 ? {title: "Counting not configured", detail: "No counting channels are registered yet.", href: `/dashboard/${guildId}/counting`} : null,
        stickyCount === 0 ? {title: "No sticky messages", detail: "You have sticky messages enabled in the bot, but no dashboard entries yet.", href: `/dashboard/${guildId}/sticky-messages`} : null,
        modmailRoleCount === 0 ? {title: "Modmail roles missing", detail: "Modmail needs at least one moderator role.", href: `/dashboard/${guildId}/modmail`} : null,
        notifierCount === 0 ? {title: "No notifiers configured", detail: "Notifier sections are empty right now.", href: `/dashboard/${guildId}/notifiers`} : null
    ].filter(Boolean) as Array<{title: string; detail: string; href: string}>;

    const setupScore = moduleCards.filter(card => card.ready).length;
    const totalModules = moduleCards.length;
    const hasAttentionItems = attentionItems.length > 0;

    return <div className="space-y-8 pl-1 md:pl-2">
        <section className="relative overflow-hidden border border-slate-800/80 bg-slate-950/60 p-6">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(56,189,248,0.16),transparent_35%),radial-gradient(circle_at_bottom_left,rgba(236,72,153,0.10),transparent_30%)]" />
            <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
                <div className="max-w-3xl space-y-4">
                    <p className="inline-flex items-center gap-2 border border-sky-400/20 bg-sky-400/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-sky-200">
                        Overview
                    </p>
                    <div className="space-y-2">
                        <h2 className="text-4xl font-black tracking-tight text-white">{snapshot.guild.name}</h2>
                        <p className="text-sm text-slate-300">
                            Use this page to see what is configured, what is missing, and where the busy parts of the guild live right now.
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-3 text-sm text-slate-300">
                        <span className={`border px-3 py-1 ${
                            snapshot.guild.connected
                                ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-200"
                                : "border-amber-400/30 bg-amber-400/10 text-amber-200"
                        }`}>
                            {snapshot.guild.connected ? "Bot connected" : "Bot offline"}
                        </span>
                        <span className="border border-slate-700 bg-slate-900 px-3 py-1 text-slate-200">
                            {snapshot.persisted ? "Config saved" : "Using defaults"}
                        </span>
                        <span className="border border-slate-700 bg-slate-900 px-3 py-1 text-slate-200">
                            {formatCount(snapshot.guild.memberCount)} members
                        </span>
                        <span className="border border-slate-700 bg-slate-900 px-3 py-1 text-slate-200">
                            {setupScore}/{totalModules} modules ready
                        </span>
                    </div>
                </div>

                <div className="relative grid gap-3 sm:grid-cols-2 xl:min-w-[360px] xl:grid-cols-1">
                    <div className="border border-slate-800 bg-slate-950/80 p-4">
                        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Guild ID</p>
                        <p className="mt-2 font-mono text-sm text-slate-200">{snapshot.guild.id}</p>
                    </div>
                    <div className="border border-slate-800 bg-slate-950/80 p-4">
                        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Configured items</p>
                        <p className="mt-2 text-sm text-slate-200">{snapshot.persisted ? "Guild config exists" : "No saved config yet"}</p>
                    </div>
                </div>
            </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard
                icon={<FaBell className="h-4 w-4" />}
                label="Notifiers"
                value={formatCount(notifierCount)}
                detail="Automatic social and game subscriptions."
                href={`/dashboard/${guildId}/notifiers`}
                tone="violet"
            />
            <MetricCard
                icon={<FaTriangleExclamation className="h-4 w-4" />}
                label="Warnings"
                value={formatCount(allWarnings)}
                detail="Recorded warning history in this guild."
                href={`/dashboard/${guildId}/warnings`}
                tone="amber"
            />
            <MetricCard
                icon={<FaHashtag className="h-4 w-4" />}
                label="Counting channels"
                value={formatCount(countingChannels)}
                detail={`Max succession ${countSummary || "unset"}.`}
                href={`/dashboard/${guildId}/counting`}
                tone="sky"
            />
            <MetricCard
                icon={<FaMessage className="h-4 w-4" />}
                label="Sticky messages"
                value={formatCount(stickyCount)}
                detail="Pinned helper messages that re-post to the bottom."
                href={`/dashboard/${guildId}/sticky-messages`}
                tone="emerald"
            />
        </section>

        <section className={`grid gap-4 ${hasAttentionItems ? "lg:grid-cols-[minmax(0,1.4fr)_minmax(0,0.9fr)]" : ""}`}>
            <div className="space-y-4">
                <div className="flex items-end justify-between gap-4">
                    <div>
                        <h3 className="text-xl font-bold text-white">Module health</h3>
                        <p className="mt-1 text-sm text-slate-400">What is set, what is enabled, and what needs a pass.</p>
                    </div>
                    <Link href={`/dashboard/${guildId}/search`} className="text-sm font-semibold text-sky-300 hover:text-sky-200">
                        Full search
                    </Link>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                    {moduleCards.map(card => <ModuleCard key={card.title} {...card} />)}
                </div>
            </div>

            {hasAttentionItems ? <div className="space-y-4">
                <div>
                    <h3 className="text-xl font-bold text-white">Needs attention</h3>
                    <p className="mt-1 text-sm text-slate-400">The shortest list of stuff you probably want to fix first.</p>
                </div>

                <div className="space-y-3">
                    {attentionItems.slice(0, 8).map(item => <AttentionItem key={item.title} {...item} />)}
                </div>
            </div> : null}
        </section>

        <section className="space-y-4">
            <div>
                <h3 className="text-xl font-bold text-white">Quick actions</h3>
                <p className="mt-1 text-sm text-slate-400">Jump straight to the pages you open most often.</p>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Link
                    href={`/dashboard/${guildId}/notifiers`}
                    className="border border-slate-800/80 bg-slate-950/60 p-5 transition hover:border-slate-600 hover:bg-slate-900/70"
                >
                    <p className="text-sm font-semibold text-white">Open Notifiers</p>
                    <p className="mt-2 text-sm text-slate-400">Inspect social and game notifiers, then add or edit a specific type.</p>
                </Link>
                <Link
                    href={`/dashboard/${guildId}/suggestions/list`}
                    className="border border-slate-800/80 bg-slate-950/60 p-5 transition hover:border-slate-600 hover:bg-slate-900/70"
                >
                    <p className="text-sm font-semibold text-white">Open Suggestions</p>
                    <p className="mt-2 text-sm text-slate-400">Review the live suggestion queue and moderate entries.</p>
                </Link>
                <Link
                    href={`/dashboard/${guildId}/sticky-messages`}
                    className="border border-slate-800/80 bg-slate-950/60 p-5 transition hover:border-slate-600 hover:bg-slate-900/70"
                >
                    <p className="text-sm font-semibold text-white">Open Sticky Messages</p>
                    <p className="mt-2 text-sm text-slate-400">Manage sticky text per channel and clean up old entries.</p>
                </Link>
            </div>
        </section>
    </div>;
}
