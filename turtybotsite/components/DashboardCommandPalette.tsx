"use client";

import {useEffect, useMemo, useRef, useState} from "react";
import {createPortal} from "react-dom";
import {FaMagnifyingGlass} from "react-icons/fa6";
import {searchDashboardEntries} from "@/lib/dashboard-search";

interface DashboardCommandPaletteProps {
    guildId: string;
}

interface CommandAction {
    label: string;
    href: string;
    description: string;
}

const COMMON_ACTIONS: CommandAction[] = [
    {label: "Overview", href: "overview", description: "Jump to the guild overview."},
    {label: "Search dashboard", href: "search", description: "Open the dashboard search page."},
    {label: "Sticky Messages", href: "sticky-messages", description: "Manage sticky messages."},
    {label: "Notifiers", href: "notifiers", description: "Manage notifier channels."},
    {label: "Counting", href: "counting", description: "Manage counting channels."},
    {label: "Logging", href: "logging", description: "Configure logging settings."},
    {label: "Levelling", href: "levelling", description: "Configure levelling and XP settings."},
    {label: "Modmail", href: "modmail", description: "Configure modmail settings."}
];

export default function DashboardCommandPalette({guildId}: DashboardCommandPaletteProps) {
    const [isOpen, setIsOpen] = useState(false);
    const [query, setQuery] = useState("");
    const [activeIndex, setActiveIndex] = useState(0);
    const inputRef = useRef<HTMLInputElement | null>(null);

    const results = useMemo(() => {
        const normalizedQuery = query.trim();
        if (!normalizedQuery) {
            return COMMON_ACTIONS.map(action => ({
                label: action.label,
                href: `/dashboard/${guildId}${action.href === "overview" ? "" : `/${action.href}`}`,
                description: action.description
            }));
        }

        return searchDashboardEntries(guildId, normalizedQuery)
            .map(result => ({
                label: result.label,
                href: result.href,
                description: result.term
            }))
            .slice(0, 12);
    }, [guildId, query]);

    useEffect(() => {
        function onKeyDown(event: KeyboardEvent) {
            if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
                event.preventDefault();
                setIsOpen(current => !current);
            }

            if (event.key === "Escape") {
                setIsOpen(false);
            }
        }

        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);
    }, []);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        setQuery("");
        setActiveIndex(0);
        queueMicrotask(() => inputRef.current?.focus());
    }, [isOpen]);

    useEffect(() => {
        setActiveIndex(0);
    }, [query]);

    function close() {
        setIsOpen(false);
    }

    function navigate(href: string) {
        window.location.assign(href);
    }

    function submitCurrent() {
        const selected = results[activeIndex];
        if (!selected) {
            return;
        }

        close();
        navigate(selected.href);
    }

    return <>
        <button
            type="button"
            onClick={() => setIsOpen(true)}
            className="inline-flex items-center gap-2 border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-800"
        >
            <FaMagnifyingGlass className="h-3.5 w-3.5" aria-hidden="true" />
            Command Palette
            <span className="ml-1 hidden rounded border border-slate-700 px-1.5 py-0.5 text-[10px] uppercase tracking-[0.18em] text-slate-500 md:inline">
                Ctrl K
            </span>
        </button>

        {isOpen && typeof document !== "undefined" ? createPortal(
            <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm" onClick={close}>
                <div
                    className="absolute left-1/2 top-20 w-[min(92vw,720px)] -translate-x-1/2 border border-slate-800 bg-slate-950 shadow-2xl"
                    onClick={event => event.stopPropagation()}
                >
                    <div className="border-b border-slate-800 p-4">
                        <div className="flex items-center gap-3 border border-slate-700 bg-slate-900 px-3 py-2">
                            <FaMagnifyingGlass className="h-4 w-4 shrink-0 text-slate-500" aria-hidden="true" />
                            <input
                                ref={inputRef}
                                value={query}
                                onChange={event => setQuery(event.target.value)}
                                onKeyDown={event => {
                                    if (event.key === "Enter") {
                                        event.preventDefault();
                                        submitCurrent();
                                    }

                                    if (event.key === "ArrowDown") {
                                        event.preventDefault();
                                        setActiveIndex(current => Math.min(current + 1, Math.max(results.length - 1, 0)));
                                    }

                                    if (event.key === "ArrowUp") {
                                        event.preventDefault();
                                        setActiveIndex(current => Math.max(current - 1, 0));
                                    }
                                }}
                                placeholder="Search pages, settings, and fields..."
                                className="w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
                            />
                        </div>
                        <p className="mt-2 text-xs text-slate-500">
                            Type to filter dashboard pages. Enter opens the highlighted result.
                        </p>
                    </div>

                    <div className="dashboard-scrollbar max-h-[60vh] overflow-y-auto p-2">
                        {results.length > 0 ? results.map((result, index) => <button
                            key={`${result.href}:${result.label}`}
                            type="button"
                            onMouseEnter={() => setActiveIndex(index)}
                            onClick={() => {
                                close();
                                navigate(result.href);
                            }}
                            className={`flex w-full items-start gap-3 border px-4 py-3 text-left transition ${
                                index === activeIndex
                                    ? "border-sky-400/40 bg-sky-400/10"
                                    : "border-transparent hover:border-slate-700 hover:bg-slate-900"
                            }`}
                        >
                            <span className="mt-0.5 rounded border border-slate-700 bg-slate-900 px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.16em] text-slate-400">
                                {index + 1}
                            </span>
                            <span className="min-w-0 flex-1">
                                <span className="block text-sm font-semibold text-white">{result.label}</span>
                                <span className="mt-0.5 block truncate text-sm text-slate-400">{result.description}</span>
                            </span>
                        </button>) : <div className="px-4 py-10 text-sm text-slate-400">
                            No matches.
                        </div>}
                    </div>
                </div>
            </div>,
            document.body
        ) : null}
    </>;
}
