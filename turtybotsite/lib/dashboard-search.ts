export interface DashboardSearchEntry {
    label: string;
    href: (guildId: string) => string;
    terms: string[];
}

export const DASHBOARD_SEARCH_ENTRIES: DashboardSearchEntry[] = [
    {
        label: "Overview",
        href: guildId => `/dashboard/${guildId}`,
        terms: ["overview", "dashboard", "bot status", "config record", "starboard", "members"]
    },
    {
        label: "Birthday",
        href: guildId => `/dashboard/${guildId}/birthday`,
        terms: ["birthday channel", "announce birthdays"]
    },
    {
        label: "Collectables",
        href: guildId => `/dashboard/${guildId}/collectables`,
        terms: ["collector channel", "enable collectables", "restrict collection types", "enabled collections", "minecraft mobs", "rainbow six operators", "countries"]
    },
    {
        label: "Welcome",
        href: guildId => `/dashboard/${guildId}/welcome`,
        terms: ["welcome channel", "announce joins", "announce leaves"]
    },
    {
        label: "Suggestions",
        href: guildId => `/dashboard/${guildId}/suggestions`,
        terms: ["suggestions channel", "suggestions status"]
    },
    {
        label: "Quotes",
        href: guildId => `/dashboard/${guildId}/quotes`,
        terms: ["quotes"]
    },
    {
        label: "Tags",
        href: guildId => `/dashboard/${guildId}/tags`,
        terms: ["tags"]
    },
    {
        label: "Opt-In Channels",
        href: guildId => `/dashboard/${guildId}/opt-in-channels`,
        terms: ["opt-in channels", "selected channels"]
    },
    {
        label: "Notifiers",
        href: guildId => `/dashboard/${guildId}/notifiers`,
        terms: ["social notifiers", "game notifiers", "discord channel", "who to ping", "add notifier"]
    },
    {
        label: "AI",
        href: guildId => `/dashboard/${guildId}/ai`,
        terms: ["enable ai", "channel whitelist", "user blacklist"]
    },
    {
        label: "Chat Revival",
        href: guildId => `/dashboard/${guildId}/chat-revival`,
        terms: ["enable chat revival", "channel", "interval hours", "allow nsfw wyr", "prompt types"]
    },
    {
        label: "Threads",
        href: guildId => `/dashboard/${guildId}/threads`,
        terms: ["moderators join threads", "auto thread channels"]
    },
    {
        label: "Misc",
        href: guildId => `/dashboard/${guildId}/misc`,
        terms: ["patron role", "create gists", "startup message", "send changelog", "sticky roles"]
    },
    {
        label: "Counting",
        href: guildId => `/dashboard/${guildId}/counting`,
        terms: ["maximum succession", "add counting channel", "counting mode", "registered channels"]
    },
    {
        label: "Economy",
        href: guildId => `/dashboard/${guildId}/economy`,
        terms: ["enable economy", "enable donations", "currency", "default balance", "income tax"]
    },
    {
        label: "NSFW",
        href: guildId => `/dashboard/${guildId}/nsfw`,
        terms: ["nsfw channels", "artist nsfw filter"]
    },
    {
        label: "Levelling",
        href: guildId => `/dashboard/${guildId}/levelling`,
        terms: [
            "enable levelling",
            "level cooldown",
            "minimum xp",
            "maximum xp",
            "item chance",
            "disabled levelling channels",
            "level roles",
            "disable level-up messages",
            "use dedicated level-up channel",
            "level-up message channel",
            "embed level-up messages",
            "allow level depletion",
            "xp boosted channels",
            "xp boost percentage",
            "xp boosted roles",
            "count server boosters"
        ]
    },
    {
        label: "Starboard",
        href: guildId => `/dashboard/${guildId}/starboard`,
        terms: ["enable starboard", "starboard channel", "minimum stars", "star emoji", "showcase channels", "count bot stars", "media only"]
    },
    {
        label: "Reports",
        href: guildId => `/dashboard/${guildId}/reports`,
        terms: ["reports", "user id"]
    },
    {
        label: "Warnings",
        href: guildId => `/dashboard/${guildId}/warnings`,
        terms: ["moderator only", "xp loss percentage", "economy loss percentage", "recent warnings"]
    },
    {
        label: "Automod",
        href: guildId => `/dashboard/${guildId}/automod`,
        terms: [
            "invite guard",
            "blocked channels",
            "scam detection",
            "image spam autoban",
            "image spam thresholds",
            "window seconds",
            "minimum images",
            "new member threshold hours"
        ]
    },
    {
        label: "Modmail",
        href: guildId => `/dashboard/${guildId}/modmail`,
        terms: ["moderator roles", "ticket created message"]
    },
    {
        label: "Sticky Messages",
        href: guildId => `/dashboard/${guildId}/sticky-messages`,
        terms: ["sticky channel", "sticky text", "current stickies", "sticky messages"]
    },
    {
        label: "Logging",
        href: guildId => `/dashboard/${guildId}/logging`,
        terms: [
            "logging channel",
            "moderation logging channel",
            "channel create",
            "channel delete",
            "channel update",
            "guild update",
            "role create",
            "role delete",
            "role update",
            "member join",
            "member leave",
            "ban",
            "unban",
            "timeout",
            "invite create",
            "invite delete",
            "message delete",
            "bulk delete",
            "message edit",
            "emoji added",
            "emoji removed",
            "emoji update",
            "sticker added",
            "sticker removed",
            "sticker update",
            "forum tag update"
        ]
    }
];

export function searchDashboardEntries(guildId: string, query: string) {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) {
        return [];
    }

    return DASHBOARD_SEARCH_ENTRIES
        .flatMap(entry => {
            const labelMatch = entry.label.toLowerCase().includes(normalizedQuery);
            const termMatch = entry.terms.find(term => term.toLowerCase().includes(normalizedQuery));

            if (!labelMatch && !termMatch) {
                return [];
            }

            const href = entry.href(guildId);
            return [{
                label: termMatch ? `${entry.label} · ${termMatch}` : entry.label,
                href,
                term: termMatch ?? entry.label
            }];
        })
        .slice(0, 50);
}
