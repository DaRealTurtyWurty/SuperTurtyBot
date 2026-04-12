"use client";

import type {ReactElement} from "react";
import Link from "next/link";
import {usePathname} from "next/navigation";
import {NOTIFIER_TYPES} from "@/lib/notifiers";

interface GuildDashboardSidebarProps {
    guildId: string;
}

interface SidebarItem {
    label: string;
    href?: (guildId: string) => string;
    children?: SidebarItem[];
}

interface SidebarSection {
    label: string;
    items: SidebarItem[];
}

const NAV_SECTIONS: SidebarSection[] = [
    {
        label: "General",
        items: [
            {label: "Overview", href: (guildId: string) => `/dashboard/${guildId}`},
            {label: "Birthday", href: (guildId: string) => `/dashboard/${guildId}/birthday`},
            {
                label: "Collectables",
                href: (guildId: string) => `/dashboard/${guildId}/collectables`,
                children: [
                    {label: "Overview", href: (guildId: string) => `/dashboard/${guildId}/collectables`},
                    {label: "Minecraft Mobs", href: (guildId: string) => `/dashboard/${guildId}/collectables/minecraft_mobs`},
                    {label: "Rainbow Six Operators", href: (guildId: string) => `/dashboard/${guildId}/collectables/r6_operators`},
                    {label: "Countries", href: (guildId: string) => `/dashboard/${guildId}/collectables/countries`}
                ]
            },
            {label: "Welcome", href: (guildId: string) => `/dashboard/${guildId}/welcome`},
            {label: "Suggestions", href: (guildId: string) => `/dashboard/${guildId}/suggestions`},
            {label: "Quotes", href: (guildId: string) => `/dashboard/${guildId}/quotes`},
            {label: "Tags", href: (guildId: string) => `/dashboard/${guildId}/tags`},
            {label: "Opt-In Channels", href: (guildId: string) => `/dashboard/${guildId}/opt-in-channels`}
        ]
    },
    {
        label: "Systems",
        items: [
            {
                label: "Notifiers",
                href: (guildId: string) => `/dashboard/${guildId}/notifiers`,
                children: [
                    {
                        label: "Social Notifiers",
                        children: NOTIFIER_TYPES.filter(type => type.section === "social").map(type => ({
                            label: type.label,
                            href: (guildId: string) => `/dashboard/${guildId}/notifiers/${type.type}`
                        }))
                    },
                    {
                        label: "Game Notifiers",
                        children: NOTIFIER_TYPES.filter(type => type.section === "games").map(type => ({
                            label: type.label,
                            href: (guildId: string) => `/dashboard/${guildId}/notifiers/${type.type}`
                        }))
                    }
                ]
            },
            {label: "AI", href: (guildId: string) => `/dashboard/${guildId}/ai`},
            {label: "Chat Revival", href: (guildId: string) => `/dashboard/${guildId}/chat-revival`},
            {label: "Threads", href: (guildId: string) => `/dashboard/${guildId}/threads`},
            {label: "Misc", href: (guildId: string) => `/dashboard/${guildId}/misc`},
            {label: "Counting", href: (guildId: string) => `/dashboard/${guildId}/counting`},
            {label: "Economy", href: (guildId: string) => `/dashboard/${guildId}/economy`},
            {label: "NSFW", href: (guildId: string) => `/dashboard/${guildId}/nsfw`},
            {label: "Levelling", href: (guildId: string) => `/dashboard/${guildId}/levelling`},
            {label: "Starboard", href: (guildId: string) => `/dashboard/${guildId}/starboard`}
        ]
    },
    {
        label: "Moderation",
        items: [
            {label: "Reports", href: (guildId: string) => `/dashboard/${guildId}/reports`},
            {label: "Warnings", href: (guildId: string) => `/dashboard/${guildId}/warnings`},
            {label: "Automod", href: (guildId: string) => `/dashboard/${guildId}/automod`},
            {
                label: "Modmail",
                href: (guildId: string) => `/dashboard/${guildId}/modmail`,
                children: [
                    {label: "Tickets", href: (guildId: string) => `/dashboard/${guildId}/modmail/tickets`}
                ]
            },
            {label: "Logging", href: (guildId: string) => `/dashboard/${guildId}/logging`}
        ]
    }
];

export default function GuildDashboardSidebar({guildId}: GuildDashboardSidebarProps) {
    const pathname = usePathname();

    function renderItem(item: SidebarItem, depth: number): ReactElement {
        const href = item.href?.(guildId) ?? null;
        const childActive = item.children?.some(child => isItemActive(child)) ?? false;
        const isActive = href ? pathname === href : false;

        const paddingClass = "px-3 py-2 text-sm";
        const labelClass = depth === 0
            ? ""
            : item.children
                ? "text-slate-400 uppercase tracking-[0.14em] text-[11px]"
                : "text-slate-300";

        return <div key={item.label} className="space-y-1">
            {href ? <Link
                href={href}
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

        <nav className="space-y-4">
            {NAV_SECTIONS.map(section => <div key={section.label} className="space-y-1">
                <p className="px-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{section.label}</p>
                <div className="space-y-1 border-l border-slate-800 pl-3">
                    {section.items.map(item => renderItem(item, 0))}
                </div>
            </div>)}
        </nav>
    </aside>;
}
