export type NotifierSectionKey = "social" | "games";
export type NotifierIconKey =
    "youtube"
    | "twitch"
    | "reddit"
    | "steam"
    | "store"
    | "minecraft"
    | "siege"
    | "rocket-league"
    | "league"
    | "valorant";

export interface NotifierTypeDefinition {
    type: string;
    label: string;
    description: string;
    details: string[];
    targetLabel: string | null;
    targetPlaceholder: string | null;
    targetHelp: string | null;
    requiresTarget: boolean;
    section: NotifierSectionKey;
    command: string;
    icon: NotifierIconKey;
}

export const NOTIFIER_TYPES: NotifierTypeDefinition[] = [
    {
        type: "youtube",
        label: "YouTube",
        description: "Tracks a specific YouTube channel and pushes new uploads into the chosen Discord channel.",
        details: [
            "Best for creators that publish videos on a regular schedule.",
            "The bot remembers already announced uploads so the same video is not posted twice.",
            "Each notifier is tied to one Discord channel, one target channel, and an optional mention."
        ],
        targetLabel: "YouTube channel ID",
        targetPlaceholder: "UC...",
        targetHelp: "Paste the YouTube channel ID, not the channel name.",
        requiresTarget: true,
        section: "social",
        command: "/notifier youtube",
        icon: "youtube"
    },
    {
        type: "twitch",
        label: "Twitch",
        description: "Watches a Twitch channel and alerts the configured Discord channel when the stream goes live.",
        details: [
            "Useful for live creators who want immediate Discord alerts when a stream starts.",
            "The target is a Twitch channel name, while delivery happens in the selected Discord channel.",
            "Mentions are optional and can be used to ping a role or a moderator group."
        ],
        targetLabel: "Twitch channel",
        targetPlaceholder: "streamername",
        targetHelp: "Use the Twitch channel name exactly as it appears on Twitch.",
        requiresTarget: true,
        section: "social",
        command: "/notifier twitch",
        icon: "twitch"
    },
    {
        type: "reddit",
        label: "Reddit",
        description: "Monitors a subreddit and posts new Reddit submissions to the selected Discord channel.",
        details: [
            "Handy for community updates, patch discussions, and feed-style subreddit alerts.",
            "The notifier key is the subreddit name, without the leading r/ in the command.",
            "Posts are de-duplicated so the same submission is not repeated."
        ],
        targetLabel: "Subreddit",
        targetPlaceholder: "communityname",
        targetHelp: "Enter the subreddit name without the leading r/.",
        requiresTarget: true,
        section: "social",
        command: "/notifier reddit",
        icon: "reddit"
    },
    {
        type: "steam",
        label: "Steam",
        description: "Tracks Steam news for one app ID and forwards new article posts into Discord.",
        details: [
            "Use this for game-specific update feeds that are published through Steam news.",
            "The target is a numeric Steam app ID, not the game title itself.",
            "The notifier stores previously seen news items so only fresh posts are announced."
        ],
        targetLabel: "Steam app ID",
        targetPlaceholder: "570",
        targetHelp: "Enter the numeric Steam app ID for the game.",
        requiresTarget: true,
        section: "games",
        command: "/notifier steam",
        icon: "steam"
    },
    {
        type: "steam-store",
        label: "Steam Store",
        description: "Follows Steam sales and festival articles and sends those announcements to Discord.",
        details: [
            "Best for guilds that want to know about Steam-wide sales, festivals, and storefront announcements.",
            "This notifier is not tied to a single game app ID.",
            "It keeps an internal article list to avoid reposting the same sale notice."
        ],
        targetLabel: null,
        targetPlaceholder: null,
        targetHelp: null,
        requiresTarget: false,
        section: "games",
        command: "/notifier steamstore",
        icon: "store"
    },
    {
        type: "minecraft",
        label: "Minecraft",
        description: "Watches Minecraft news posts and forwards Mojang updates to the selected Discord channel.",
        details: [
            "Suited to servers that care about official Minecraft blog or update announcements.",
            "The notifier keeps track of the articles it has already sent.",
            "Creation time is shown because this notifier stores its own setup timestamp."
        ],
        targetLabel: null,
        targetPlaceholder: null,
        targetHelp: null,
        requiresTarget: false,
        section: "games",
        command: "/notifier minecraft",
        icon: "minecraft"
    },
    {
        type: "siege",
        label: "Rainbow Six Siege",
        description: "Tracks Rainbow Six Siege news and patch-style article updates for the selected channel.",
        details: [
            "Useful for balance updates, seasonal announcements, and official Siege news posts.",
            "The notifier stores article IDs so it only announces each update once.",
            "Mentions can be used to ping a role whenever a new post lands."
        ],
        targetLabel: null,
        targetPlaceholder: null,
        targetHelp: null,
        requiresTarget: false,
        section: "games",
        command: "/notifier siege",
        icon: "siege"
    },
    {
        type: "rocket-league",
        label: "Rocket League",
        description: "Forwards Rocket League news and official update posts into Discord.",
        details: [
            "Good for patch notes, roadmap posts, and event announcements.",
            "This notifier tracks already posted article slugs to avoid duplicates.",
            "It behaves like a feed listener rather than a manual announcement tool."
        ],
        targetLabel: null,
        targetPlaceholder: null,
        targetHelp: null,
        requiresTarget: false,
        section: "games",
        command: "/notifier rocketleague",
        icon: "rocket-league"
    },
    {
        type: "league",
        label: "League of Legends",
        description: "Monitors official League of Legends news and patch articles for the channel you choose.",
        details: [
            "Useful for patch cadence, event news, and Riot announcement coverage.",
            "The stored URL list keeps the notifier from reposting old articles.",
            "You can pair it with a role mention if a whole player group should be alerted."
        ],
        targetLabel: null,
        targetPlaceholder: null,
        targetHelp: null,
        requiresTarget: false,
        section: "games",
        command: "/notifier league",
        icon: "league"
    },
    {
        type: "valorant",
        label: "VALORANT",
        description: "Watches VALORANT news and patch articles and posts them in Discord.",
        details: [
            "Best for patch notes, agent updates, and official Riot news posts.",
            "Like the other feed-style notifiers, it stores URLs to avoid duplicate announcements.",
            "This notifier is intended for structured news posts, not live gameplay alerts."
        ],
        targetLabel: null,
        targetPlaceholder: null,
        targetHelp: null,
        requiresTarget: false,
        section: "games",
        command: "/notifier valorant",
        icon: "valorant"
    }
];

export const NOTIFIER_SECTIONS: Record<NotifierSectionKey, {label: string; description: string}> = {
    social: {
        label: "Social Notifiers",
        description: "YouTube, Twitch, and Reddit feeds."
    },
    games: {
        label: "Game Notifiers",
        description: "Game, store, and patch feed notifiers."
    }
};

export function getNotifierTypeDefinition(type: string) {
    return NOTIFIER_TYPES.find(entry => entry.type === type) ?? null;
}

export function getNotifierSectionDefinition(section: NotifierSectionKey) {
    return NOTIFIER_SECTIONS[section];
}
