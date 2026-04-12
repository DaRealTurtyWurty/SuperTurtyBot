"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardChatRevivalSettings} from "@/lib/dashboard-api";
import DashboardNumberInput from "@/components/DashboardNumberInput";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface ChatRevivalSettingsFormProps {
    guildId: string;
    initialSettings: DashboardChatRevivalSettings;
}

const CHAT_REVIVAL_OPTIONS = [
    {value: "drawing", label: "Drawing"},
    {value: "topic", label: "Topic"},
    {value: "would_you_rather", label: "Would You Rather"}
] as const;

export default function ChatRevivalSettingsForm({guildId, initialSettings}: ChatRevivalSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        chatRevivalChannelId: initialSettings.chatRevivalChannelId ?? ""
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const revivalDisabled = !settings.chatRevivalEnabled;

    function updateBoolean(key: "chatRevivalEnabled" | "chatRevivalAllowNsfw", value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateChannel(value: string) {
        setSettings(current => ({
            ...current,
            chatRevivalChannelId: value
        }));
    }

    function updateTime(value: string) {
        setSettings(current => ({
            ...current,
            chatRevivalTime: Number.parseInt(value, 10) || 0
        }));
    }

    function toggleType(value: string, checked: boolean) {
        setSettings(current => ({
            ...current,
            chatRevivalTypes: checked
                ? [...current.chatRevivalTypes, value]
                : current.chatRevivalTypes.filter(entry => entry !== value)
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/chat-revival`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    chatRevivalEnabled: settings.chatRevivalEnabled,
                    chatRevivalChannelId: settings.chatRevivalChannelId.trim() || null,
                    chatRevivalTime: settings.chatRevivalTime,
                    chatRevivalTypes: settings.chatRevivalTypes,
                    chatRevivalAllowNsfw: settings.chatRevivalAllowNsfw
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save chat revival settings.");
                return;
            }

            const updated = await response.json() as DashboardChatRevivalSettings;
            setSettings({
                ...updated,
                chatRevivalChannelId: updated.chatRevivalChannelId ?? ""
            });
            setSuccess("Chat revival settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <div className="grid gap-4 md:grid-cols-2">
            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Enable Chat Revival</p>
                        <p className="mt-1 text-sm text-slate-400">Automatically post conversation starters on a schedule.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.chatRevivalEnabled}
                        onChange={event => updateBoolean("chatRevivalEnabled", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <GuildChannelSelect
                guildId={guildId}
                value={settings.chatRevivalChannelId}
                onChange={updateChannel}
                disabled={revivalDisabled}
                label="Channel"
                description="The text channel where revival prompts should be sent."
                placeholder="Select a channel"
            />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <p className="text-sm font-semibold text-white">Interval Hours</p>
                <p className="mt-1 text-sm text-slate-400">How often the revival prompt should run.</p>
                <DashboardNumberInput
                    min={1}
                    value={settings.chatRevivalTime}
                    onChange={updateTime}
                    disabled={revivalDisabled}
                    placeholder="24"
                />
            </label>

            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Allow NSFW WYR</p>
                        <p className="mt-1 text-sm text-slate-400">Only applies when the prompt lands in an NSFW text channel.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.chatRevivalAllowNsfw}
                        onChange={event => updateBoolean("chatRevivalAllowNsfw", event.target.checked)}
                        disabled={revivalDisabled}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>
        </div>

        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <h3 className="text-lg font-semibold text-white">Prompt Types</h3>
            <div className="mt-4 grid gap-3 md:grid-cols-3">
                {CHAT_REVIVAL_OPTIONS.map(option => <label key={option.value} className="flex items-center justify-between gap-4 border border-slate-800/80 bg-slate-950/50 px-4 py-3">
                    <span className="text-sm font-semibold text-white">{option.label}</span>
                    <input
                        type="checkbox"
                        checked={settings.chatRevivalTypes.includes(option.value)}
                        onChange={event => toggleType(option.value, event.target.checked)}
                        disabled={revivalDisabled}
                        className="h-5 w-5 accent-sky-400"
                    />
                </label>)}
            </div>
        </section>

        {error ? <p className="border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
            {error}
        </p> : null}

        {success ? <p className="border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100">
            {success}
        </p> : null}

        <div className="flex items-center justify-between gap-4">
            <button
                type="submit"
                disabled={isPending}
                className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60">
                {isPending ? "Saving..." : "Save Chat Revival"}
            </button>
        </div>
    </form>;
}
