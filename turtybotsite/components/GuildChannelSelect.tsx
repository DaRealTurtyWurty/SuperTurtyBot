"use client";

import {useEffect, useMemo, useRef, useState} from "react";
import {
    FaBullhorn,
    FaCameraRetro,
    FaComments,
    FaFolderTree,
    FaHashtag,
    FaMicrophoneLines,
    FaPhotoFilm,
    FaVolumeHigh
} from "react-icons/fa6";
import type {IconType} from "react-icons";

export interface DashboardGuildChannelOption {
    id: string;
    name: string;
    type: string;
    parentCategoryId: string | null;
    position: number;
}

interface GuildChannelSelectProps {
    guildId: string;
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    label: string;
    description?: string;
    allowTypes?: string[];
    placeholder?: string;
    multiple?: boolean;
    values?: string[];
    onValuesChange?: (values: string[]) => void;
}

interface ChannelGroup {
    label: string;
    channels: DashboardGuildChannelOption[];
}

interface SummaryState {
    label: string;
    iconType: string | null;
}

function createChannelGroups(channels: DashboardGuildChannelOption[]) {
    const categories = channels.filter(channel => channel.type === "category");
    const categorizedIds = new Set(categories.map(channel => channel.id));

    const groups: ChannelGroup[] = categories.map(category => ({
        label: category.name,
        channels: channels.filter(channel => channel.parentCategoryId === category.id && channel.type !== "category")
    }));

    groups.push({
        label: "No Category",
        channels: channels.filter(channel => channel.type !== "category" && (!channel.parentCategoryId || !categorizedIds.has(channel.parentCategoryId)))
    });

    return groups.filter(group => group.channels.length > 0);
}

const channelTypeIcons: Record<string, IconType> = {
    announcement: FaBullhorn,
    category: FaFolderTree,
    forum: FaComments,
    media: FaPhotoFilm,
    stage: FaMicrophoneLines,
    text: FaHashtag,
    thread: FaCameraRetro,
    voice: FaVolumeHigh
};

function ChannelTypeIcon({type}: {type: string}) {
    const Icon = channelTypeIcons[type] ?? FaComments;
    return <Icon
        title={`${type} channel`}
        className="h-3.5 w-3.5 shrink-0 text-slate-400"
        aria-hidden="true"
    />;
}

function formatChannelLabel(channel: DashboardGuildChannelOption) {
    switch (channel.type) {
        case "announcement":
            return `Announcement: ${channel.name}`;
        case "forum":
            return `Forum: ${channel.name}`;
        case "stage":
            return `Stage: ${channel.name}`;
        case "media":
            return `Media: ${channel.name}`;
        case "thread":
            return `Thread: ${channel.name}`;
        case "voice":
            return `Voice: ${channel.name}`;
        default:
            return channel.name;
    }
}

function getSummaryState(
    selectedChannels: DashboardGuildChannelOption[],
    placeholder: string,
    multiple: boolean
): SummaryState {
    if (selectedChannels.length === 0) {
        return {
            label: placeholder,
            iconType: null
        };
    }

    if (!multiple) {
        return {
            label: formatChannelLabel(selectedChannels[0]),
            iconType: selectedChannels[0].type
        };
    }

    if (selectedChannels.length === 1) {
        return {
            label: formatChannelLabel(selectedChannels[0]),
            iconType: selectedChannels[0].type
        };
    }

    return {
        label: `${selectedChannels.length} channels selected`,
        iconType: "category"
    };
}

export function useGuildChannels(guildId: string) {
    const [channels, setChannels] = useState<DashboardGuildChannelOption[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let isCancelled = false;

        setIsLoading(true);
        setError(null);

        fetch(`/api/discord/guilds/${guildId}/channels`, {
            cache: "no-store"
        })
            .then(async response => {
                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    throw new Error(payload?.message ?? "Failed to load guild channels.");
                }

                return response.json() as Promise<{channels: DashboardGuildChannelOption[]}>;
            })
            .then(payload => {
                if (!isCancelled) {
                    setChannels(payload.channels);
                    setIsLoading(false);
                }
            })
            .catch(fetchError => {
                if (!isCancelled) {
                    setError(fetchError instanceof Error ? fetchError.message : "Failed to load guild channels.");
                    setIsLoading(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [guildId]);

    return {channels, isLoading, error};
}

export default function GuildChannelSelect({
    guildId,
    value,
    onChange,
    disabled,
    label,
    description,
    allowTypes = ["text"],
    placeholder = "Select a channel",
    multiple = false,
    values = [],
    onValuesChange
}: GuildChannelSelectProps) {
    const {channels, isLoading, error} = useGuildChannels(guildId);
    const groups = useMemo(() => createChannelGroups(channels), [channels]);
    const allowedTypes = useMemo(() => new Set(allowTypes), [allowTypes]);
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement | null>(null);

    const selectedIds = multiple ? values : value ? [value] : [];
    const selectedChannels = channels.filter(channel => selectedIds.includes(channel.id));
    const summary = getSummaryState(selectedChannels, placeholder, multiple);

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

    function toggleValue(channelId: string) {
        if (multiple) {
            const nextValues = values.includes(channelId)
                ? values.filter(valueEntry => valueEntry !== channelId)
                : [...values, channelId];
            onValuesChange?.(nextValues);
            return;
        }

        onChange(channelId);
        setIsOpen(false);
    }

    return <div ref={containerRef} className="block border border-slate-800/80 bg-slate-950/60 p-5">
        <p className="text-sm font-semibold text-white">{label}</p>
        {description ? <p className="mt-1 text-sm text-slate-400">{description}</p> : null}

        <div className="relative mt-4">
            <button
                type="button"
                onClick={() => setIsOpen(current => !current)}
                disabled={disabled || isLoading}
                className="flex min-h-12 w-full items-center justify-between gap-3 border border-slate-700 bg-slate-900 px-4 py-3 text-left text-sm text-white outline-none transition hover:border-slate-600 focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
                <span className="flex min-w-0 items-center gap-2 text-slate-100">
                    {!isLoading && summary.iconType ? <ChannelTypeIcon type={summary.iconType} /> : null}
                    <span className="truncate">
                        {isLoading ? "Loading channels..." : summary.label}
                    </span>
                </span>
                <svg
                    className={`h-4 w-4 shrink-0 text-slate-400 transition ${isOpen ? "rotate-180" : ""}`}
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                >
                    <path d="m6 9 6 6 6-6" />
                </svg>
            </button>

            {isOpen && !isLoading ? <div className="dashboard-scrollbar absolute left-0 right-0 top-full z-30 mt-1 max-h-96 overflow-y-auto border border-slate-700 bg-slate-900 shadow-2xl">
                <div className="space-y-4 p-3">
                {groups.map(group => <div key={group.label} className="space-y-1">
                    <div className="flex items-center gap-2 px-2 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                        <ChannelTypeIcon type="category" />
                        <span>{group.label}</span>
                    </div>
                    <div className="border-l border-slate-800 pl-3">
                        {group.channels.map(channel => {
                            const isAllowed = allowedTypes.has(channel.type);
                            const isSelected = selectedIds.includes(channel.id);

                            return <button
                                key={channel.id}
                                type="button"
                                onClick={() => isAllowed ? toggleValue(channel.id) : undefined}
                                disabled={!isAllowed}
                                className={`flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm transition ${
                                    isAllowed
                                        ? isSelected
                                            ? "bg-sky-400/12 text-sky-100"
                                            : "text-slate-200 hover:bg-slate-800 hover:text-white"
                                        : "cursor-not-allowed text-slate-600"
                                }`}
                            >
                                    <span className="flex min-w-0 items-center gap-2">
                                        <ChannelTypeIcon type={channel.type} />
                                        <span className="truncate">{formatChannelLabel(channel)}</span>
                                    </span>
                                {isSelected ? <svg
                                    className="h-4 w-4 shrink-0 text-sky-300"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    aria-hidden="true"
                                >
                                    <path d="M20 6 9 17l-5-5" />
                                </svg> : null}
                            </button>;
                        })}
                    </div>
                </div>)}
                </div>
            </div> : null}
        </div>

        {error ? <p className="mt-3 text-sm text-red-300">{error}</p> : null}
        {!error && !isLoading ? <p className="mt-3 text-xs text-slate-500">Only supported channel types can be selected. Other channels are shown for context.</p> : null}
    </div>;
}
