"use client";

import Image from "next/image";
import {useEffect, useMemo, useRef, useState} from "react";
import type {DashboardGuildMemberInfo} from "@/lib/dashboard-api";

interface GuildMemberMultiSelectProps {
    id?: string;
    guildId: string;
    values: string[];
    onValuesChange: (values: string[]) => void;
    onResolvedValuesChange?: (values: DashboardGuildMemberInfo[]) => void;
    disabled?: boolean;
    label: string;
    description?: string;
    placeholder?: string;
}

function MemberChip({
    member,
    onRemove,
    disabled
}: {
    member: DashboardGuildMemberInfo;
    onRemove: () => void;
    disabled?: boolean;
}) {
    return <span className="inline-flex items-center gap-2 border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-sm text-slate-100">
        <MemberAvatar member={member} />
        <span className="flex min-w-0 flex-col">
            <span className="truncate font-medium">{member.displayName}</span>
            {member.username ? <span className="truncate text-xs text-slate-400">@{member.username}</span> : null}
        </span>
        <button
            type="button"
            onClick={onRemove}
            disabled={disabled}
            className="text-slate-400 transition hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
            aria-label={`Remove ${member.displayName}`}
        >
            <svg
                className="h-4 w-4"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
            >
                <path d="M18 6 6 18" />
                <path d="M6 6 18 18" />
            </svg>
        </button>
    </span>;
}

function MemberAvatar({member}: {member: DashboardGuildMemberInfo}) {
    const fallback = member.displayName.trim().slice(0, 1).toUpperCase() || "?";

    if (member.avatarUrl) {
        return <Image
            src={member.avatarUrl}
            alt=""
            width={32}
            height={32}
            className="h-8 w-8 shrink-0 rounded-full object-cover"
        />;
    }

    return <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-800 text-xs font-semibold uppercase text-slate-200">
        {fallback}
    </span>;
}

function normalizeSnowflake(value: string) {
    const trimmed = value.trim();
    const mentionMatch = trimmed.match(/^<@!?(\d{17,20})>$/);
    if (mentionMatch) {
        return mentionMatch[1];
    }

    if (/^\d{17,20}$/.test(trimmed)) {
        return trimmed;
    }

    return null;
}

function extractSnowflakes(value: string) {
    return value
        .split(/[\s,]+/)
        .map(normalizeSnowflake)
        .filter((entry): entry is string => Boolean(entry));
}

export default function GuildMemberMultiSelect({
    id,
    guildId,
    values,
    onValuesChange,
    onResolvedValuesChange,
    disabled,
    label,
    description,
    placeholder = "Search members"
}: GuildMemberMultiSelectProps) {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState<DashboardGuildMemberInfo[]>([]);
    const [memberCache, setMemberCache] = useState<Record<string, DashboardGuildMemberInfo>>({});
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isOpen, setIsOpen] = useState(false);
    const [unresolvedIds, setUnresolvedIds] = useState<string[]>([]);
    const containerRef = useRef<HTMLDivElement | null>(null);
    const [backspaceArmed, setBackspaceArmed] = useState(false);

    const selectedMembers = useMemo(() => values.map(id => {
        return memberCache[id] ?? {
            id,
            username: "",
            displayName: "Unknown User",
            avatarUrl: null
        };
    }), [memberCache, values]);
    const selectedSet = useMemo(() => new Set(values), [values]);
    const normalizedQuery = useMemo(() => normalizeSnowflake(query), [query]);
    const missingIds = useMemo(
        () => values.filter(id => !memberCache[id] && !unresolvedIds.includes(id)),
        [memberCache, unresolvedIds, values]
    );

    useEffect(() => {
        onResolvedValuesChange?.(selectedMembers);
    }, [onResolvedValuesChange, selectedMembers]);

    useEffect(() => {
        function onDocumentClick(event: MouseEvent) {
            if (!containerRef.current?.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }

        if (isOpen) {
            document.addEventListener("mousedown", onDocumentClick);
        }

        return () => {
            document.removeEventListener("mousedown", onDocumentClick);
        };
    }, [isOpen]);

    useEffect(() => {
        let isCancelled = false;
        const controller = new AbortController();
        const timeout = window.setTimeout(() => {
            setIsLoading(true);
            setError(null);

            fetch(`/api/discord/guilds/${guildId}/members?query=${encodeURIComponent(query)}`, {
                cache: "no-store",
                signal: controller.signal
            })
                .then(async response => {
                    if (!response.ok) {
                        const payload = await response.json().catch(() => null) as {message?: string} | null;
                        throw new Error(payload?.message ?? "Failed to load guild members.");
                    }

                    return response.json() as Promise<{members: DashboardGuildMemberInfo[]}>;
                })
                .then(payload => {
                    if (isCancelled) {
                        return;
                    }

                    setResults(payload.members);
                    setMemberCache(current => {
                        const next = {...current};
                        for (const member of payload.members) {
                            next[member.id] = member;
                        }

                        return next;
                    });
                    setIsLoading(false);
                })
                .catch(fetchError => {
                    if (isCancelled || controller.signal.aborted) {
                        return;
                    }

                    setError(fetchError instanceof Error ? fetchError.message : "Failed to load guild members.");
                    setIsLoading(false);
                });
        }, 200);

        return () => {
            isCancelled = true;
            controller.abort();
            window.clearTimeout(timeout);
        };
    }, [guildId, query]);

    useEffect(() => {
        let isCancelled = false;

        if (missingIds.length === 0) {
            return;
        }

        Promise.all(missingIds.map(async id => {
            const response = await fetch(`/api/discord/guilds/${guildId}/members?query=${encodeURIComponent(id)}`, {
                cache: "no-store"
            });

            if (!response.ok) {
                return null;
            }

            const payload = await response.json() as {members?: DashboardGuildMemberInfo[]};
            return payload.members?.find(member => member.id === id) ?? null;
        }))
            .then(members => {
                if (isCancelled) {
                    return;
                }

                const resolvedIds = new Set(members.filter(Boolean).map(member => member!.id));
                const unresolved = missingIds.filter(id => !resolvedIds.has(id));

                setMemberCache(current => {
                    const next = {...current};
                    for (const member of members) {
                        if (member) {
                            next[member.id] = member;
                        }
                    }

                    return next;
                });
                if (unresolved.length > 0) {
                    setUnresolvedIds(current => Array.from(new Set([...current, ...unresolved])));
                }
            })
            .catch(() => null);

        return () => {
            isCancelled = true;
        };
    }, [guildId, missingIds]);

    function toggleValue(memberId: string) {
        if (selectedSet.has(memberId)) {
            onValuesChange(values.filter(value => value !== memberId));
            return;
        }

        onValuesChange([...values, memberId]);
        setQuery("");
    }

    function removeValue(memberId: string) {
        onValuesChange(values.filter(value => value !== memberId));
    }

    function removeLastValue() {
        const lastValue = values.at(-1);
        if (!lastValue) {
            return;
        }

        removeValue(lastValue);
    }

    function addRawValue(memberId: string) {
        const normalized = normalizeSnowflake(memberId);
        if (!normalized || selectedSet.has(normalized)) {
            return;
        }

        onValuesChange([...values, normalized]);
        setUnresolvedIds(current => current.includes(normalized) ? current : [...current, normalized]);
        setQuery("");
        setIsOpen(false);
    }

    return <div id={id} ref={containerRef} className="block border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
        <p className="text-sm font-semibold text-white">{label}</p>
        {description ? <p className="mt-1 text-sm text-slate-400">{description}</p> : null}

        <div className="relative mt-4">
            <div
                className="flex min-h-12 w-full flex-wrap gap-2 border border-slate-700 bg-slate-900 px-3 py-3 text-sm text-white transition focus-within:border-sky-400"
                onClick={() => {
                    if (!disabled) {
                        setIsOpen(true);
                    }
                }}
            >
                {selectedMembers.length > 0 ? selectedMembers.map(member => (
                    <MemberChip
                        key={member.id}
                        member={member}
                        onRemove={() => removeValue(member.id)}
                        disabled={disabled}
                    />
                )) : null}
                <input
                    value={query}
                    onChange={event => {
                        setQuery(event.target.value);
                        if (backspaceArmed) {
                            setBackspaceArmed(false);
                        }
                    }}
                    onFocus={() => setIsOpen(true)}
                    onKeyDown={event => {
                        if (event.key !== "Backspace" && backspaceArmed) {
                            setBackspaceArmed(false);
                        }

                        if (event.key === "Backspace") {
                            const target = event.currentTarget;
                            const start = target.selectionStart ?? 0;
                            const end = target.selectionEnd ?? 0;

                            if ((query.length === 0 || (start === 0 && end === 0)) && values.length > 0) {
                                event.preventDefault();

                                if (backspaceArmed) {
                                    removeLastValue();
                                    setBackspaceArmed(false);
                                } else {
                                    setBackspaceArmed(true);
                                }

                                return;
                            }
                        }

                        if (event.key === "Enter" || event.key === "Tab" || event.key === ",") {
                            if (normalizedQuery) {
                                event.preventDefault();
                                addRawValue(normalizedQuery);
                                setBackspaceArmed(false);
                            }
                        }
                    }}
                    onClick={() => setBackspaceArmed(false)}
                    onBlur={() => setBackspaceArmed(false)}
                    onPaste={event => {
                        const pasted = event.clipboardData.getData("text");
                        const valuesToAdd = extractSnowflakes(pasted);
                        if (valuesToAdd.length === 0) {
                            return;
                        }

                        event.preventDefault();
                        onValuesChange(Array.from(new Set([...values, ...valuesToAdd])));
                        setQuery("");
                        setBackspaceArmed(false);
                    }}
                    disabled={disabled}
                    placeholder={placeholder}
                    className="min-w-36 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500 disabled:cursor-not-allowed"
                />
            </div>

            {isOpen ? <div className="dashboard-scrollbar absolute left-0 right-0 top-full z-30 mt-1 max-h-96 overflow-y-auto border border-slate-700 bg-slate-900 shadow-2xl">
                <div className="p-2">
                    {isLoading ? <p className="px-3 py-2 text-sm text-slate-400">Searching members...</p> : null}
                    {!isLoading && error ? <p className="px-3 py-2 text-sm text-red-300">{error}</p> : null}
                    {!isLoading && !error && query.trim().length > 0 && results.length === 0 ? <p className="px-3 py-2 text-sm text-slate-400">No matching members found.</p> : null}
                    {!isLoading && !error && normalizedQuery && !selectedSet.has(normalizedQuery) ? <button
                        type="button"
                        onClick={() => addRawValue(normalizedQuery)}
                        className="mb-2 flex w-full items-center justify-between gap-3 border border-slate-700 bg-slate-900 px-3 py-2 text-left text-sm text-slate-200 transition hover:bg-slate-800 hover:text-white"
                    >
                        <span className="min-w-0">
                            <span className="block truncate font-medium">Add raw ID</span>
                            <span className="block truncate text-xs text-slate-500">{normalizedQuery}</span>
                        </span>
                        <span className="text-xs font-semibold uppercase tracking-[0.14em] text-sky-300">Add</span>
                    </button> : null}
                    {!isLoading && !error ? results.map(member => {
                        const isSelected = selectedSet.has(member.id);

                        return <button
                            key={member.id}
                            type="button"
                            onClick={() => toggleValue(member.id)}
                            className={`flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm transition ${
                                isSelected
                                    ? "bg-sky-400/12 text-sky-100"
                                    : "text-slate-200 hover:bg-slate-800 hover:text-white"
                            }`}
                        >
                            <span className="flex min-w-0 items-center gap-2">
                                <MemberAvatar member={member} />
                                <span className="min-w-0">
                                    <span className="block truncate font-medium">{member.displayName}</span>
                                    <span className="block truncate text-xs text-slate-500">@{member.username} · {member.id}</span>
                                </span>
                            </span>
                            {isSelected ? <span className="text-xs font-semibold uppercase tracking-[0.14em] text-sky-300">Selected</span> : null}
                        </button>;
                    }) : null}
                </div>
            </div> : null}
        </div>

        {!error ? <p className="mt-3 text-xs text-slate-500">Paste comma-separated IDs or start typing a name. Press Enter or comma to add a valid ID, including mentions like <code className="rounded bg-slate-900 px-1 py-0.5 font-mono text-slate-300">&lt;@123...&gt;</code>. When the field is empty, press Backspace twice to remove the last chip.</p> : null}
    </div>;
}
