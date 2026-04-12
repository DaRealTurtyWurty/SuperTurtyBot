"use client";

import {useEffect, useMemo, useRef, useState} from "react";
import {FaChevronDown} from "react-icons/fa6";
import type {
    DashboardCountingChannelInfo,
    DashboardCountingModeInfo,
    DashboardCountingSettings
} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface CountingSettingsFormProps {
    guildId: string;
    initialSettings: DashboardCountingSettings;
}

function formatCountingModeLabel(mode: string) {
    return mode
        .toLowerCase()
        .replace(/_/g, " ")
        .replace(/\b\w/g, value => value.toUpperCase());
}

function CountingModeSelect({
    value,
    onChange,
    options,
    disabled
}: {
    value: string;
    onChange: (value: string) => void;
    options: DashboardCountingModeInfo[];
    disabled?: boolean;
}) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement | null>(null);
    const selectedOption = options.find(option => option.mode === value) ?? null;

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

    return <div ref={containerRef} className="relative">
        <button
            type="button"
            onClick={() => setIsOpen(current => !current)}
            disabled={disabled}
            className="flex min-h-12 w-full items-center justify-between gap-3 border border-slate-700 bg-slate-900 px-4 py-3 text-left text-sm text-white outline-none transition hover:border-slate-600 focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
        >
            <span className="min-w-0">
                <span className="block truncate font-medium">
                    {selectedOption?.label ?? formatCountingModeLabel(value)}
                </span>
                {selectedOption ? <span className="block truncate text-xs text-slate-500">{selectedOption.description}</span> : null}
            </span>
            <FaChevronDown className={`h-4 w-4 shrink-0 text-slate-400 transition ${isOpen ? "rotate-180" : ""}`} aria-hidden="true" />
        </button>

        {isOpen ? <div className="dashboard-scrollbar absolute left-0 right-0 top-full z-30 mt-1 max-h-80 overflow-y-auto border border-slate-700 bg-slate-900 shadow-2xl">
            <div className="p-2">
                {options.map(option => {
                    const isSelected = option.mode === value;

                    return <button
                        key={option.mode}
                        type="button"
                        onClick={() => {
                            onChange(option.mode);
                            setIsOpen(false);
                        }}
                        className={`flex w-full items-start gap-3 px-3 py-2.5 text-left transition ${
                            isSelected
                                ? "bg-sky-400/12 text-sky-100"
                                : "text-slate-200 hover:bg-slate-800 hover:text-white"
                        }`}
                    >
                        <span className="min-w-0 flex-1">
                            <span className="block truncate text-sm font-medium">{option.label}</span>
                            <span className="mt-0.5 block text-xs leading-5 text-slate-500">{option.description}</span>
                        </span>
                        {isSelected ? <span className="pt-0.5 text-xs font-semibold uppercase tracking-[0.14em] text-sky-300">Selected</span> : null}
                    </button>;
                })}
            </div>
        </div> : null}
    </div>;
}

export default function CountingSettingsForm({guildId, initialSettings}: CountingSettingsFormProps) {
    const [channels, setChannels] = useState<DashboardCountingChannelInfo[]>(initialSettings.channels);
    const [availableModes] = useState(initialSettings.availableModes);
    const [maxCountingSuccession, setMaxCountingSuccession] = useState(initialSettings.maxCountingSuccession.toString());
    const [selectedChannelId, setSelectedChannelId] = useState("");
    const [selectedMode, setSelectedMode] = useState(initialSettings.availableModes[0]?.mode ?? "NORMAL");
    const [pendingChannelId, setPendingChannelId] = useState<string | null>(null);
    const [isAdding, setIsAdding] = useState(false);
    const [isSavingSuccession, setIsSavingSuccession] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const modeOptions = useMemo(() => availableModes, [availableModes]);

    function getModeLabel(mode: string) {
        return availableModes.find(option => option.mode === mode)?.label ?? formatCountingModeLabel(mode);
    }

    function applySettings(next: DashboardCountingSettings) {
        setChannels(next.channels);
        setMaxCountingSuccession(next.maxCountingSuccession.toString());
        setSuccess("Counting settings saved.");
    }

    async function readApiMessage(response: Response) {
        const payload = await response.json().catch(() => null) as {message?: string} | null;
        return payload?.message ?? "Unexpected dashboard response.";
    }

    async function handleAddChannel() {
        if (!selectedChannelId) {
            return;
        }

        setError(null);
        setSuccess(null);
        setPendingChannelId(selectedChannelId);
        setIsAdding(true);

        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/counting`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    channelId: selectedChannelId,
                    mode: selectedMode
                })
            });

            if (!response.ok) {
                throw new Error(await readApiMessage(response));
            }

            const updated = await response.json() as DashboardCountingSettings;
            applySettings(updated);
            setSelectedChannelId("");
            setSelectedMode(updated.availableModes[0]?.mode ?? "NORMAL");
        } catch (saveError) {
            setError(saveError instanceof Error ? saveError.message : "Failed to save counting channel.");
        } finally {
            setPendingChannelId(null);
            setIsAdding(false);
        }
    }

    async function handleModeChange(channelId: string, mode: string) {
        const previousChannel = channels.find(channel => channel.channelId === channelId);
        if (!previousChannel || previousChannel.mode === mode) {
            return;
        }

        setError(null);
        setSuccess(null);
        setPendingChannelId(channelId);
        setChannels(current => current.map(channel => channel.channelId === channelId ? {
            ...channel,
            mode
        } : channel));

        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/counting`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    channelId,
                    mode
                })
            });

            if (!response.ok) {
                throw new Error(await readApiMessage(response));
            }

            const updated = await response.json() as DashboardCountingSettings;
            applySettings(updated);
        } catch (saveError) {
            setChannels(current => current.map(channel => channel.channelId === channelId ? previousChannel : channel));
            setError(saveError instanceof Error ? saveError.message : "Failed to save counting channel.");
        } finally {
            setPendingChannelId(null);
        }
    }

    async function handleRemoveChannel(channelId: string) {
        setError(null);
        setSuccess(null);
        setPendingChannelId(channelId);

        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/counting/${channelId}`, {
                method: "DELETE"
            });

            if (!response.ok) {
                throw new Error(await readApiMessage(response));
            }

            const updated = await response.json() as DashboardCountingSettings;
            applySettings(updated);
        } catch (removeError) {
            setError(removeError instanceof Error ? removeError.message : "Failed to remove counting channel.");
        } finally {
            setPendingChannelId(null);
        }
    }

    async function handleSaveSuccession() {
        const parsed = Number.parseInt(maxCountingSuccession, 10);
        if (!Number.isFinite(parsed) || parsed < 1) {
            setError("Maximum counting succession must be at least 1.");
            return;
        }

        setError(null);
        setSuccess(null);
        setIsSavingSuccession(true);

        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/counting`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    maxCountingSuccession: parsed
                })
            });

            if (!response.ok) {
                throw new Error(await readApiMessage(response));
            }

            const updated = await response.json() as DashboardCountingSettings;
            applySettings(updated);
        } catch (saveError) {
            setError(saveError instanceof Error ? saveError.message : "Failed to save counting settings.");
        } finally {
            setIsSavingSuccession(false);
        }
    }

    return <div className="space-y-6">
        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="flex flex-col gap-4 md:flex-row md:items-end">
                <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-white">Maximum Succession</p>
                    <p className="mt-1 text-sm text-slate-400">How many repeated counts the bot should tolerate before it stops a streak.</p>
                    <div className="mt-4 max-w-xs">
                        <input
                            type="number"
                            min={1}
                            value={maxCountingSuccession}
                            onChange={event => setMaxCountingSuccession(event.target.value)}
                            disabled={isSavingSuccession || pendingChannelId !== null || isAdding}
                            className="min-h-12 w-full border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition hover:border-slate-600 focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
                        />
                    </div>
                </div>

                <button
                    type="button"
                    onClick={() => void handleSaveSuccession()}
                    disabled={isSavingSuccession || pendingChannelId !== null || isAdding}
                    className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSavingSuccession ? "Saving..." : "Save Succession"}
                </button>
            </div>
        </section>

        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="flex flex-col gap-3 md:flex-row md:items-end">
                <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-white">Add Counting Channel</p>
                    <p className="mt-1 text-sm text-slate-400">Pick a text channel and counting mode. Selecting an existing channel updates its mode.</p>
                    <div className="mt-4">
                        <GuildChannelSelect
                            guildId={guildId}
                            value={selectedChannelId}
                            onChange={setSelectedChannelId}
                            disabled={pendingChannelId !== null || isAdding || isSavingSuccession}
                            label="Channel"
                            description="Only text channels that the dashboard user can access are shown."
                            placeholder="Select a channel"
                        />
                    </div>
                </div>

                <div className="min-w-0 md:w-72">
                    <p className="text-sm font-semibold text-white">Mode</p>
                    <p className="mt-1 text-sm text-slate-400">Choose how counting should progress.</p>
                    <div className="mt-4">
                        <CountingModeSelect
                            value={selectedMode}
                            onChange={setSelectedMode}
                            options={modeOptions}
                            disabled={pendingChannelId !== null || isAdding || isSavingSuccession}
                        />
                    </div>
                </div>

                <button
                    type="button"
                    onClick={() => void handleAddChannel()}
                    disabled={!selectedChannelId || pendingChannelId !== null || isAdding || isSavingSuccession}
                    className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isAdding && pendingChannelId === selectedChannelId ? "Saving..." : "Save Channel"}
                </button>
            </div>
        </section>

        <section className="space-y-3">
            <div>
                <p className="text-sm font-semibold text-white">Registered Channels</p>
                <p className="mt-1 text-sm text-slate-400">These channels are currently registered for counting.</p>
            </div>

            {channels.length === 0 ? <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                No counting channels are registered yet.
            </div> : <div className="space-y-3">
                {channels.map(channel => {
                    const isBusy = pendingChannelId === channel.channelId;

                    return <div key={channel.channelId} className="border border-slate-800/80 bg-slate-950/60 p-5">
                        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                            <div className="min-w-0">
                                <p className="truncate text-sm font-semibold text-white">{channel.connected ? `#${channel.channelName}` : "Unknown Channel"}</p>
                                <p className="mt-1 text-xs text-slate-500 font-mono">{channel.channelId}</p>
                                <p className="mt-2 text-xs text-slate-400">
                                    Current count {channel.currentCount} · Highest count {channel.highestCount}
                                </p>
                                <p className="mt-2 text-xs text-slate-500">
                                    Mode: {getModeLabel(channel.mode)}
                                </p>
                            </div>

                            <div className="grid gap-3 md:grid-cols-[minmax(0,240px)_auto] md:items-end">
                                <label className="block">
                                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Mode</p>
                                    <div className="mt-2">
                                        <CountingModeSelect
                                            value={channel.mode}
                                            onChange={mode => void handleModeChange(channel.channelId, mode)}
                                            options={modeOptions}
                                            disabled={isBusy || !channel.connected}
                                        />
                                    </div>
                                    {!channel.connected ? <p className="mt-2 text-xs text-slate-500">This channel no longer exists in Discord. Remove it to clear the entry.</p> : null}
                                </label>

                                <button
                                    type="button"
                                    onClick={() => void handleRemoveChannel(channel.channelId)}
                                    disabled={isBusy}
                                    className="border border-slate-700 bg-slate-900 px-4 py-3 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    {isBusy ? "Working..." : "Remove"}
                                </button>
                            </div>
                        </div>
                    </div>;
                })}
            </div>}
        </section>

        {error ? <p className="border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
            {error}
        </p> : null}

        {success ? <p className="border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100">
            {success}
        </p> : null}
    </div>;
}
