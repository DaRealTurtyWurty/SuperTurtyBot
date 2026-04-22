"use client";

import {useMemo, useRef, useState} from "react";
import type {ReactElement} from "react";
import Link from "next/link";
import {usePathname, useRouter} from "next/navigation";
import {FaMagnifyingGlass} from "react-icons/fa6";
import {useDashboardNavigationGuard} from "@/components/DashboardNavigationGuard";
import {NOTIFIER_TYPES} from "@/lib/notifiers";

interface GuildDashboardSidebarProps {
    guildId: string;
}

interface SidebarItem {
    label: string;
    href?: (guildId: string) => string;
    fragment?: string | null;
    options?: Array<{label: string; fragment?: string} | string>;
    children?: SidebarItem[];
}

interface SidebarSection {
    label: string;
    items: SidebarItem[];
}

interface SearchMatch {
    href: string;
    label: string;
    fragment?: string | null;
}

function matchesQuery(item: SidebarItem, query: string) {
    if (item.label.toLowerCase().includes(query)) {
        return true;
    }

    return item.options?.some(option => {
        const label = typeof option === "string" ? option : option.label;
        return label.toLowerCase().includes(query);
    }) ?? false;
}

function getMatchedFragment(item: SidebarItem, query: string) {
    const matchedOption = item.options?.find(option => {
        const label = typeof option === "string" ? option : option.label;
        return label.toLowerCase().includes(query);
    });

    if (matchedOption && typeof matchedOption !== "string") {
        return matchedOption.fragment ?? null;
    }

    return null;
}

function getOptionLabel(option: {label: string; fragment?: string} | string) {
    return typeof option === "string" ? option : option.label;
}

function getOptionFragment(option: {label: string; fragment?: string} | string) {
    return typeof option === "string" ? null : option.fragment ?? null;
}

function filterSidebarItems(items: SidebarItem[], query: string): SidebarItem[] {
    if (!query) {
        return items;
    }

    return items.flatMap(item => {
        const childItems = item.children ? filterSidebarItems(item.children, query) : [];
        const itemMatches = matchesQuery(item, query);
        const fragment = itemMatches ? getMatchedFragment(item, query) : null;

        if (!itemMatches && childItems.length === 0) {
            return [];
        }

        return [{
            ...item,
            fragment,
            children: itemMatches ? item.children : childItems
        }];
    });
}

function collectSearchMatches(items: SidebarItem[], guildId: string, query: string): SearchMatch[] {
    const matches: SearchMatch[] = [];

    function walk(item: SidebarItem) {
        const href = item.href?.(guildId) ?? null;
        const normalizedLabel = item.label.toLowerCase();
        const labelMatch = normalizedLabel.includes(query);
        const optionMatch = item.options?.find(option => getOptionLabel(option).toLowerCase().includes(query));
        const exactOption = item.options?.find(option => getOptionLabel(option).toLowerCase() === query);

        if (href && (labelMatch || optionMatch)) {
            const matchedOption = exactOption ?? optionMatch ?? null;
            const fragment = item.fragment ?? (matchedOption ? getOptionFragment(matchedOption) : null) ?? null;
            matches.push({
                href: fragment ? `${href}#${fragment}` : href,
                label: matchedOption ? `${item.label} · ${getOptionLabel(matchedOption)}` : item.label,
                fragment
            });
        }

        item.children?.forEach(walk);
    }

    items.forEach(walk);
    return matches.sort((left, right) => left.label.localeCompare(right.label));
}

const NAV_SECTIONS: SidebarSection[] = [
    {
        label: "General",
        items: [
            {label: "Overview", href: (guildId: string) => `/dashboard/${guildId}`},
            {
                label: "Birthday",
                href: (guildId: string) => `/dashboard/${guildId}/birthday`,
                options: [
                    {label: "Birthday Channel", fragment: "birthday-channel"},
                    {label: "Announce Birthdays", fragment: "announce-birthdays"}
                ]
            },
            {
                label: "Collectables",
                href: (guildId: string) => `/dashboard/${guildId}/collectables`,
                options: [
                    {label: "Collector Channel", fragment: "collector-channel"},
                    {label: "Enable Collectables", fragment: "enable-collectables"},
                    {label: "Restrict Collection Types", fragment: "restrict-collection-types"},
                    {label: "Enabled Collections", fragment: "enabled-collections"},
                    {label: "Minecraft Mobs", fragment: "minecraft-mobs"},
                    {label: "Rainbow Six Operators", fragment: "r6-operators"},
                    {label: "Countries", fragment: "countries"}
                ],
                children: [
                    {label: "Overview", href: (guildId: string) => `/dashboard/${guildId}/collectables`},
                    {label: "Minecraft Mobs", href: (guildId: string) => `/dashboard/${guildId}/collectables/minecraft_mobs`},
                    {label: "Rainbow Six Operators", href: (guildId: string) => `/dashboard/${guildId}/collectables/r6_operators`},
                    {label: "Countries", href: (guildId: string) => `/dashboard/${guildId}/collectables/countries`}
                ]
            },
            {
                label: "Welcome",
                href: (guildId: string) => `/dashboard/${guildId}/welcome`,
                options: [
                    {label: "Welcome Channel", fragment: "welcome-channel"},
                    {label: "Announce Joins", fragment: "announce-joins"},
                    {label: "Announce Leaves", fragment: "announce-leaves"}
                ]
            },
            {
                label: "Suggestions",
                href: (guildId: string) => `/dashboard/${guildId}/suggestions`,
                options: [
                    {label: "Suggestions Channel", fragment: "suggestions-channel"},
                    {label: "Suggestions Status", fragment: "suggestions-status"}
                ]
            },
            {
                label: "Quotes",
                href: (guildId: string) => `/dashboard/${guildId}/quotes`,
                options: [{label: "Quotes", fragment: "quotes"}]
            },
            {
                label: "Tags",
                href: (guildId: string) => `/dashboard/${guildId}/tags`,
                options: [{label: "Tags", fragment: "tags"}]
            },
            {
                label: "Opt-In Channels",
                href: (guildId: string) => `/dashboard/${guildId}/opt-in-channels`,
                options: [
                    {label: "Opt-In Channels", fragment: "opt-in-channels"},
                    {label: "Selected Channels", fragment: "selected-channels"}
                ]
            }
        ]
    },
    {
        label: "Systems",
        items: [
            {
                label: "Notifiers",
                href: (guildId: string) => `/dashboard/${guildId}/notifiers`,
                options: [
                    {label: "Social Notifiers", fragment: "social"},
                    {label: "Game Notifiers", fragment: "games"},
                    {label: "Discord channel", fragment: "add-notifier"},
                    {label: "Who to ping", fragment: "add-notifier"},
                    {label: "Add notifier", fragment: "add-notifier"}
                ],
                children: [
                    {
                        label: "Social Notifiers",
                        children: NOTIFIER_TYPES.filter(type => type.section === "social").map(type => ({
                            label: type.label,
                            options: [{label: type.label, fragment: "add-notifier"}],
                            href: (guildId: string) => `/dashboard/${guildId}/notifiers/${type.type}`
                        }))
                    },
                    {
                        label: "Game Notifiers",
                        children: NOTIFIER_TYPES.filter(type => type.section === "games").map(type => ({
                            label: type.label,
                            options: [{label: type.label, fragment: "add-notifier"}],
                            href: (guildId: string) => `/dashboard/${guildId}/notifiers/${type.type}`
                        }))
                    }
                ]
            },
            {
                label: "Voice Channel Notifiers",
                href: (guildId: string) => `/dashboard/${guildId}/voice-channel-notifiers`,
                options: [
                    {label: "Voice Channel", fragment: "voice-notifier-voice-channel"},
                    {label: "Destination Channel", fragment: "voice-notifier-send-to-channel"},
                    {label: "Mention Roles", fragment: "voice-notifier-mention-roles"},
                    {label: "Cooldown", fragment: "voice-notifier-cooldown"},
                    {label: "Current Voice Channel Notifiers", fragment: "voice-notifier-list"}
                ]
            },
            {
                label: "AI",
                href: (guildId: string) => `/dashboard/${guildId}/ai`,
                options: [
                    {label: "Enable AI", fragment: "enable-ai"},
                    {label: "Channel Whitelist", fragment: "channel-whitelist"},
                    {label: "User Blacklist", fragment: "user-blacklist"}
                ]
            },
            {
                label: "Chat Revival",
                href: (guildId: string) => `/dashboard/${guildId}/chat-revival`,
                options: [
                    {label: "Enable Chat Revival", fragment: "enable-chat-revival"},
                    {label: "Channel", fragment: "chat-revival-channel"},
                    {label: "Interval Hours", fragment: "interval-hours"},
                    {label: "Allow NSFW WYR", fragment: "allow-nsfw-wyr"},
                    {label: "Prompt Types", fragment: "prompt-types"}
                ]
            },
            {
                label: "Threads",
                href: (guildId: string) => `/dashboard/${guildId}/threads`,
                options: [
                    {label: "Moderators Join Threads", fragment: "moderators-join-threads"},
                    {label: "Auto Thread Channels", fragment: "auto-thread-channels"}
                ]
            },
            {
                label: "Misc",
                href: (guildId: string) => `/dashboard/${guildId}/misc`,
                options: [
                    {label: "Patron Role", fragment: "patron-role"},
                    {label: "Create Gists", fragment: "create-gists"},
                    {label: "Startup Message", fragment: "startup-message"},
                    {label: "Send Changelog", fragment: "send-changelog"},
                    {label: "Sticky Roles", fragment: "sticky-roles"}
                ]
            },
            {
                label: "Counting",
                href: (guildId: string) => `/dashboard/${guildId}/counting`,
                options: [
                    {label: "Maximum Succession", fragment: "maximum-succession"},
                    {label: "Add Counting Channel", fragment: "add-counting-channel"},
                    {label: "Mode", fragment: "counting-mode"},
                    {label: "Registered Channels", fragment: "registered-channels"}
                ]
            },
            {
                label: "Economy",
                href: (guildId: string) => `/dashboard/${guildId}/economy`,
                options: [
                    {label: "Enable Economy", fragment: "enable-economy"},
                    {label: "Enable Donations", fragment: "enable-donations"},
                    {label: "Currency", fragment: "currency"},
                    {label: "Default Balance", fragment: "default-balance"},
                    {label: "Income Tax", fragment: "income-tax"}
                ]
            },
            {
                label: "NSFW",
                href: (guildId: string) => `/dashboard/${guildId}/nsfw`,
                options: [
                    {label: "NSFW Channels", fragment: "nsfw-channels"},
                    {label: "Artist NSFW Filter", fragment: "artist-nsfw-filter"}
                ]
            },
            {
                label: "Levelling",
                href: (guildId: string) => `/dashboard/${guildId}/levelling`,
                options: [
                    {label: "Enable Levelling", fragment: "enable-levelling"},
                    {label: "Level Cooldown", fragment: "level-cooldown"},
                    {label: "Minimum XP", fragment: "minimum-xp"},
                    {label: "Maximum XP", fragment: "maximum-xp"},
                    {label: "Item Chance", fragment: "item-chance"},
                    {label: "Disabled Levelling Channels", fragment: "disabled-levelling-channels"},
                    {label: "Level Roles", fragment: "level-roles"},
                    {label: "Disable Level-Up Messages", fragment: "disable-level-up-messages"},
                    {label: "Use Dedicated Level-Up Channel", fragment: "use-dedicated-level-up-channel"},
                    {label: "Level-Up Message Channel", fragment: "level-up-message-channel"},
                    {label: "Embed Level-Up Messages", fragment: "embed-level-up-messages"},
                    {label: "Allow Level Depletion", fragment: "allow-level-depletion"},
                    {label: "XP Boosted Channels", fragment: "xp-boosted-channels"},
                    {label: "XP Boost Percentage", fragment: "xp-boost-percentage"},
                    {label: "XP Boosted Roles", fragment: "xp-boosted-roles"},
                    {label: "Count Server Boosters", fragment: "count-server-boosters"}
                ]
            },
            {
                label: "Starboard",
                href: (guildId: string) => `/dashboard/${guildId}/starboard`,
                options: [
                    {label: "Enable Starboard", fragment: "enable-starboard"},
                    {label: "Starboard Channel", fragment: "starboard-channel"},
                    {label: "Minimum Stars", fragment: "minimum-stars"},
                    {label: "Star Emoji", fragment: "star-emoji"},
                    {label: "Showcase Channels", fragment: "showcase-channels"},
                    {label: "Count Bot Stars", fragment: "count-bot-stars"},
                    {label: "Media Only", fragment: "media-only"}
                ]
            }
        ]
    },
    {
        label: "Moderation",
        items: [
            {
                label: "Reports",
                href: (guildId: string) => `/dashboard/${guildId}/reports`,
                options: [
                    {label: "Reports", fragment: "reports"},
                    {label: "User ID", fragment: "user-id"}
                ]
            },
            {
                label: "Warnings",
                href: (guildId: string) => `/dashboard/${guildId}/warnings`,
                options: [
                    {label: "Moderator Only", fragment: "moderator-only"},
                    {label: "XP Loss Percentage", fragment: "xp-loss-percentage"},
                    {label: "Economy Loss Percentage", fragment: "economy-loss-percentage"},
                    {label: "Recent Warnings", fragment: "recent-warnings"}
                ]
            },
            {
                label: "Automod",
                href: (guildId: string) => `/dashboard/${guildId}/automod`,
                options: [
                    {label: "Invite Guard", fragment: "invite-guard"},
                    {label: "Blocked Channels", fragment: "blocked-channels"},
                    {label: "Scam Detection", fragment: "scam-detection"},
                    {label: "Image Spam AutoBan", fragment: "image-spam-autoban"},
                    {label: "Image Spam Thresholds", fragment: "image-spam-thresholds"},
                    {label: "Window Seconds", fragment: "window-seconds"},
                    {label: "Minimum Images", fragment: "minimum-images"},
                    {label: "New Member Threshold Hours", fragment: "new-member-threshold-hours"}
                ]
            },
            {
                label: "Modmail",
                href: (guildId: string) => `/dashboard/${guildId}/modmail`,
                options: [
                    {label: "Moderator Roles", fragment: "moderator-roles"},
                    {label: "Ticket Created Message", fragment: "ticket-created-message"}
                ],
                children: [
                    {label: "Tickets", href: (guildId: string) => `/dashboard/${guildId}/modmail/tickets`}
                ]
            },
            {
                label: "Sticky Messages",
                href: (guildId: string) => `/dashboard/${guildId}/sticky-messages`,
                options: [
                    {label: "Sticky Channel", fragment: "sticky-editor"},
                    {label: "Sticky Text", fragment: "sticky-editor"},
                    {label: "Current Stickies", fragment: "sticky-list"}
                ]
            },
            {
                label: "Logging",
                href: (guildId: string) => `/dashboard/${guildId}/logging`,
                options: [
                    {label: "Logging Channel", fragment: "logging-channel"},
                    {label: "Moderation Logging Channel", fragment: "moderation-logging-channel"},
                    {label: "Channel Create", fragment: "channel-create"},
                    {label: "Channel Delete", fragment: "channel-delete"},
                    {label: "Channel Update", fragment: "channel-update"},
                    {label: "Guild Update", fragment: "guild-update"},
                    {label: "Role Create", fragment: "role-create"},
                    {label: "Role Delete", fragment: "role-delete"},
                    {label: "Role Update", fragment: "role-update"},
                    {label: "Member Join", fragment: "member-join"},
                    {label: "Member Leave", fragment: "member-leave"},
                    {label: "Ban", fragment: "ban"},
                    {label: "Unban", fragment: "unban"},
                    {label: "Timeout", fragment: "timeout"},
                    {label: "Invite Create", fragment: "invite-create"},
                    {label: "Invite Delete", fragment: "invite-delete"},
                    {label: "Message Delete", fragment: "message-delete"},
                    {label: "Bulk Delete", fragment: "bulk-delete"},
                    {label: "Message Edit", fragment: "message-edit"},
                    {label: "Emoji Added", fragment: "emoji-added"},
                    {label: "Emoji Removed", fragment: "emoji-removed"},
                    {label: "Emoji Update", fragment: "emoji-update"},
                    {label: "Sticker Added", fragment: "sticker-added"},
                    {label: "Sticker Removed", fragment: "sticker-removed"},
                    {label: "Sticker Update", fragment: "sticker-update"},
                    {label: "Forum Tag Update", fragment: "forum-tag-update"}
                ]
            }
        ]
    }
];

export default function GuildDashboardSidebar({guildId}: GuildDashboardSidebarProps) {
    const pathname = usePathname();
    const router = useRouter();
    const {confirmNavigation} = useDashboardNavigationGuard();
    const [searchQuery, setSearchQuery] = useState("");
    const lastNavigatedHrefRef = useRef<string | null>(null);
    const normalizedQuery = searchQuery.trim().toLowerCase();
    const searchMatches = useMemo(
        () => normalizedQuery ? collectSearchMatches(NAV_SECTIONS.flatMap(section => section.items), guildId, normalizedQuery) : [],
        [guildId, normalizedQuery]
    );
    const visibleSections = useMemo(
        () => NAV_SECTIONS.flatMap(section => {
            const filteredItems = filterSidebarItems(section.items, normalizedQuery);

            if (filteredItems.length === 0 && normalizedQuery) {
                return [];
            }

            return [{
                ...section,
                items: filteredItems
            }];
        }),
        [normalizedQuery]
    );

    function navigateTo(href: string) {
        if (lastNavigatedHrefRef.current === href) {
            return;
        }

        if (!confirmNavigation()) {
            return;
        }

        lastNavigatedHrefRef.current = href;
        router.prefetch(href);
        window.location.assign(href);
    }

    function openSearchResults() {
        const query = searchQuery.trim();
        if (!query) {
            return;
        }

        if (!confirmNavigation()) {
            return;
        }

        router.push(`/dashboard/${guildId}/search?q=${encodeURIComponent(query)}`);
    }

    function renderItem(item: SidebarItem, depth: number): ReactElement {
        const href = item.href?.(guildId) ?? null;
        const childActive = item.children?.some(child => isItemActive(child)) ?? false;
        const isActive = href ? pathname === href : false;
        const itemHref = href && item.fragment ? `${href}#${item.fragment}` : href;

        const paddingClass = "px-3 py-2 text-sm";
        const labelClass = depth === 0
            ? ""
            : item.children
                ? "text-slate-400 uppercase tracking-[0.14em] text-[11px]"
                : "text-slate-300";

        return <div key={item.label} className="space-y-1">
            {itemHref ? <Link
                href={itemHref}
                onClick={event => {
                    if (itemHref) {
                        event.preventDefault();
                        navigateTo(itemHref);
                    }
                }}
                className={`block border transition ${
                    isActive || childActive
                        ? "border-sky-400 bg-sky-400/12 text-sky-100"
                        : "border-transparent text-slate-300 hover:border-slate-700 hover:bg-slate-900/80 hover:text-white"
                } ${paddingClass}`}
            >
                <span className={labelClass}>{item.label}</span>
            </Link> : <div className={`border border-transparent ${paddingClass} ${item.children ? "text-slate-500" : "text-slate-600"}`}>
                <span className={labelClass}>{item.label}</span>
            </div>}

            {item.children ? <div className={`space-y-1 border-l border-slate-800 ${depth === 0 ? "pl-4" : "pl-4"}`}>
                {item.children.map(child => renderItem(child, depth + 1))}
            </div> : null}
        </div>;
    }

    function isItemActive(item: SidebarItem): boolean {
        const href = item.href?.(guildId) ?? null;
        if (href && pathname === href) {
            return true;
        }

        return item.children?.some(child => isItemActive(child)) ?? false;
    }

    return <aside className="w-full border border-slate-800/80 bg-slate-950/70 p-4">
        <div className="mb-4 border-b border-slate-800/80 px-1 pb-3">
            <p className="text-sm font-semibold text-slate-100">Guild dashboard</p>
        </div>

        <div className="mb-4">
            <label className="mb-2 block px-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                Search settings
            </label>
            <div className="flex items-stretch gap-2">
                <input
                    value={searchQuery}
                    onChange={event => setSearchQuery(event.target.value)}
                    onKeyDown={event => {
                        if (event.key === "Enter") {
                            event.preventDefault();
                            openSearchResults();
                        }
                    }}
                    placeholder="Search dashboard"
                    className="min-w-0 flex-1 border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-sky-400"
                />
                <button
                    type="button"
                    onClick={openSearchResults}
                    disabled={!searchQuery.trim()}
                    aria-label="Search dashboard"
                    title="Search dashboard"
                    className="flex h-10 w-10 shrink-0 items-center justify-center border border-sky-400 bg-sky-400 text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:border-slate-700 disabled:bg-slate-800 disabled:text-slate-500"
                >
                    <FaMagnifyingGlass className="h-4 w-4" aria-hidden="true" />
                </button>
            </div>
            {searchMatches.length > 0 ? <div className="dashboard-scrollbar mt-2 max-h-56 space-y-1 overflow-y-auto border border-slate-800 bg-slate-950 p-2">
                {searchMatches.slice(0, 8).map(match => <button
                    key={match.href}
                    type="button"
                    onClick={() => navigateTo(match.href)}
                    className="block w-full rounded border border-transparent px-3 py-2 text-left text-sm text-slate-200 transition hover:border-slate-700 hover:bg-slate-900 hover:text-white"
                >
                    <span className="block truncate">{match.label}</span>
                    <span className="block truncate text-xs text-slate-500">{match.href}</span>
                </button>)}
            </div> : null}
        </div>

        <nav className="space-y-4">
            {visibleSections.map(section => <div key={section.label} className="space-y-1">
                <p className="px-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{section.label}</p>
                <div className="space-y-1 border-l border-slate-800 pl-3">
                    {section.items.map(item => renderItem(item, 0))}
                </div>
            </div>)}

            {visibleSections.length === 0 ? (
                <p className="px-1 text-sm text-slate-400">No settings match search.</p>
            ) : null}
        </nav>
    </aside>;
}
