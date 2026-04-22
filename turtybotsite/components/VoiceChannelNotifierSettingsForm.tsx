"use client";

import {useMemo, useState, useTransition} from "react";
import type {FormEvent} from "react";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import GuildRoleSelect from "@/components/GuildRoleSelect";
import type {
    DashboardVoiceChannelNotifierEntry,
    DashboardVoiceChannelNotifierResponse
} from "@/lib/dashboard-api";

interface VoiceChannelNotifierSettingsFormProps {
    guildId: string;
    initialSettings: DashboardVoiceChannelNotifierResponse;
}

interface FormState {
    voiceChannelId: string;
    sendToChannelId: string;
    mentionRoleIds: string[];
    message: string;
    enabled: boolean;
    announcePerJoin: boolean;
    cooldownSeconds: string;
}

const DEFAULT_MESSAGE = "{mentions} {user} joined {channel}";

function createEmptyForm(): FormState {
    return {
        voiceChannelId: "",
        sendToChannelId: "",
        mentionRoleIds: [],
        message: DEFAULT_MESSAGE,
        enabled: true,
        announcePerJoin: false,
        cooldownSeconds: "0"
    };
}

function createFormFromEntry(entry: DashboardVoiceChannelNotifierEntry): FormState {
    return {
        voiceChannelId: entry.voiceChannelId,
        sendToChannelId: entry.sendToChannelId,
        mentionRoleIds: entry.mentionRoleIds,
        message: entry.message,
        enabled: entry.enabled,
        announcePerJoin: entry.announcePerJoin,
        cooldownSeconds: Math.floor(entry.cooldownMs / 1000).toString()
    };
}

function formatCooldown(cooldownMs: number) {
    if (cooldownMs <= 0) {
        return "No delay";
    }

    const totalSeconds = Math.floor(cooldownMs / 1000);
    if (totalSeconds < 60) {
        return `${totalSeconds}s`;
    }

    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return seconds === 0 ? `${minutes}m` : `${minutes}m ${seconds}s`;
}

export default function VoiceChannelNotifierSettingsForm({
    guildId,
    initialSettings
}: VoiceChannelNotifierSettingsFormProps) {
    const [entries, setEntries] = useState(initialSettings.entries);
    const [editingVoiceChannelId, setEditingVoiceChannelId] = useState<string | null>(null);
    const [form, setForm] = useState<FormState>(createEmptyForm);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const editingEntry = useMemo(
        () => entries.find(entry => entry.voiceChannelId === editingVoiceChannelId) ?? null,
        [editingVoiceChannelId, entries]
    );

    function resetForm() {
        setEditingVoiceChannelId(null);
        setForm(createEmptyForm());
    }

    function setField<K extends keyof FormState>(key: K, value: FormState[K]) {
        setForm(current => ({
            ...current,
            [key]: value
        }));
    }

    function startEdit(entry: DashboardVoiceChannelNotifierEntry) {
        setEditingVoiceChannelId(entry.voiceChannelId);
        setForm(createFormFromEntry(entry));
        setError(null);
        setSuccess(null);
    }

    function cancelEdit() {
        resetForm();
        setError(null);
        setSuccess(null);
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!form.voiceChannelId.trim()) {
            setError("Pick a voice channel.");
            return;
        }

        if (!form.sendToChannelId.trim()) {
            setError("Pick a destination channel.");
            return;
        }

        if (!form.message.trim()) {
            setError("Notifier message cannot be blank.");
            return;
        }

        const cooldownSeconds = Number.parseInt(form.cooldownSeconds.trim() || "0", 10);
        if (Number.isNaN(cooldownSeconds) || cooldownSeconds < 0) {
            setError("Cooldown must be zero or a positive number of seconds.");
            return;
        }

        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/voice-channel-notifiers`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    originalVoiceChannelId: editingEntry?.voiceChannelId ?? null,
                    voiceChannelId: form.voiceChannelId,
                    sendToChannelId: form.sendToChannelId,
                    mentionRoleIds: form.mentionRoleIds,
                    message: form.message,
                    enabled: form.enabled,
                    announcePerJoin: form.announcePerJoin,
                    cooldownMs: cooldownSeconds * 1000
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save voice channel notifier.");
                return;
            }

            const updated = await response.json() as DashboardVoiceChannelNotifierResponse;
            setEntries(updated.entries);
            setSuccess(editingEntry ? "Voice channel notifier updated." : "Voice channel notifier created.");
            resetForm();
        });
    }

    function removeEntry(entry: DashboardVoiceChannelNotifierEntry) {
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(
                `/api/dashboard/guilds/${guildId}/voice-channel-notifiers/${encodeURIComponent(entry.voiceChannelId)}`,
                {method: "DELETE"}
            );

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to delete voice channel notifier.");
                return;
            }

            const updated = await response.json() as DashboardVoiceChannelNotifierResponse;
            setEntries(updated.entries);
            if (editingVoiceChannelId === entry.voiceChannelId) {
                resetForm();
            }
            setSuccess("Voice channel notifier deleted.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-6">
        <section id="voice-notifier-editor" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <div className="space-y-5">
                <div>
                    <p className="text-sm font-semibold text-white">
                        {editingEntry ? "Edit Voice Channel Notifier" : "Add Voice Channel Notifier"}
                    </p>
                    <p className="mt-1 text-sm text-slate-400">
                        Supported placeholders: <code className="font-mono text-slate-300">{"{user}"}</code>,{" "}
                        <code className="font-mono text-slate-300">{"{channel}"}</code>,{" "}
                        <code className="font-mono text-slate-300">{"{mentions}"}</code>.
                    </p>
                </div>

                <GuildChannelSelect
                    id="voice-notifier-voice-channel"
                    guildId={guildId}
                    value={form.voiceChannelId}
                    onChange={value => setField("voiceChannelId", value)}
                    disabled={isPending || Boolean(editingEntry)}
                    label="Voice Channel"
                    description={editingEntry
                        ? "Voice channel cannot be changed while editing because the current backend update route keys entries by voice channel ID. Delete and recreate to move it."
                        : "Choose the voice channel that triggers this notifier."}
                    allowTypes={["voice"]}
                    placeholder="Select a voice channel"
                />

                <GuildChannelSelect
                    id="voice-notifier-send-to-channel"
                    guildId={guildId}
                    value={form.sendToChannelId}
                    onChange={value => setField("sendToChannelId", value)}
                    disabled={isPending}
                    label="Destination Channel"
                    description="Notifications will be posted here."
                    allowTypes={["text", "announcement"]}
                    placeholder="Select a destination channel"
                />

                <GuildRoleSelect
                    id="voice-notifier-mention-roles"
                    guildId={guildId}
                    multiple
                    values={form.mentionRoleIds}
                    onValuesChange={values => setField("mentionRoleIds", values)}
                    disabled={isPending}
                    label="Mention Roles"
                    description="Optional. If your message uses {mentions}, these roles will be pinged."
                    placeholder="Select roles"
                />

                <label className="block">
                    <div className="flex items-center justify-between gap-4">
                        <div>
                            <p className="text-sm font-semibold text-white">Message</p>
                            <p className="mt-1 text-sm text-slate-400">Custom notification content shown in the destination channel.</p>
                        </div>
                        <span className="text-xs text-slate-500">{form.message.length}/2000</span>
                    </div>
                    <textarea
                        value={form.message}
                        onChange={event => setField("message", event.target.value)}
                        disabled={isPending}
                        rows={5}
                        className="mt-4 w-full resize-y border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
                        placeholder={DEFAULT_MESSAGE}
                    />
                </label>

                <div className="grid gap-4 lg:grid-cols-2">
                    <label id="voice-notifier-enabled" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                        <div className="flex items-center justify-between gap-4">
                            <div>
                                <p className="text-sm font-semibold text-white">Enabled</p>
                                <p className="mt-1 text-sm text-slate-400">Turn this notifier on or off without deleting it.</p>
                            </div>
                            <input
                                type="checkbox"
                                checked={form.enabled}
                                onChange={event => setField("enabled", event.target.checked)}
                                disabled={isPending}
                                className="h-5 w-5 accent-sky-400"
                            />
                        </div>
                    </label>

                    <label id="voice-notifier-announce-per-join" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                        <div className="flex items-center justify-between gap-4">
                            <div>
                                <p className="text-sm font-semibold text-white">Announce Per Join</p>
                                <p className="mt-1 text-sm text-slate-400">When enabled, each qualifying join can trigger its own announcement.</p>
                            </div>
                            <input
                                type="checkbox"
                                checked={form.announcePerJoin}
                                onChange={event => setField("announcePerJoin", event.target.checked)}
                                disabled={isPending}
                                className="h-5 w-5 accent-sky-400"
                            />
                        </div>
                    </label>
                </div>

                <label id="voice-notifier-cooldown" className="block scroll-mt-24">
                    <span className="text-sm font-semibold text-white">Cooldown</span>
                    <p className="mt-1 text-sm text-slate-400">Delay before the notifier is allowed to send, in seconds.</p>
                    <input
                        type="number"
                        min="0"
                        step="1"
                        value={form.cooldownSeconds}
                        onChange={event => setField("cooldownSeconds", event.target.value)}
                        disabled={isPending}
                        className="mt-3 w-full border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
                        placeholder="0"
                    />
                </label>

                <div className="flex flex-wrap gap-3">
                    <button
                        type="submit"
                        disabled={isPending}
                        className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {isPending ? "Saving..." : editingEntry ? "Save Voice Notifier" : "Add Voice Notifier"}
                    </button>
                    {editingEntry ? <button
                        type="button"
                        onClick={cancelEdit}
                        disabled={isPending}
                        className="border border-slate-700 bg-slate-900 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        Cancel Edit
                    </button> : null}
                </div>
            </div>
        </section>

        <section id="voice-notifier-list" className="space-y-4 scroll-mt-24">
            <div>
                <p className="text-sm font-semibold text-white">Current Voice Channel Notifiers</p>
                <p className="mt-1 text-sm text-slate-400">Each entry maps one voice channel to one destination channel.</p>
            </div>

            {entries.length === 0 ? <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                No voice channel notifiers configured yet.
            </div> : <div className="grid gap-3">
                {entries.map(entry => <div
                    key={entry.voiceChannelId}
                    className="border border-slate-800/80 bg-slate-950/60 p-5"
                >
                    <div className="flex flex-wrap items-start justify-between gap-4">
                        <div className="min-w-0 space-y-3">
                            <div className="flex flex-wrap items-center gap-2">
                                <p className="text-sm font-semibold text-white">
                                    {entry.voiceChannelName} → #{entry.sendToChannelName}
                                </p>
                                <span className={`border px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.14em] ${
                                    entry.enabled
                                        ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-200"
                                        : "border-slate-600/60 bg-slate-800/80 text-slate-300"
                                }`}>
                                    {entry.enabled ? "Enabled" : "Disabled"}
                                </span>
                                <span className="border border-slate-700 bg-slate-900 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-300">
                                    {entry.announcePerJoin ? "Per Join" : "Single Join"}
                                </span>
                            </div>

                            <div className="flex flex-wrap gap-4 text-xs text-slate-500">
                                <span>Voice ID {entry.voiceChannelId}</span>
                                <span>Destination ID {entry.sendToChannelId}</span>
                                <span>Cooldown {formatCooldown(entry.cooldownMs)}</span>
                                <span>{entry.mentionRoleIds.length} mention role{entry.mentionRoleIds.length === 1 ? "" : "s"}</span>
                            </div>

                            <p className="text-sm text-slate-300">{entry.message}</p>
                        </div>

                        <div className="flex flex-wrap gap-2">
                            <button
                                type="button"
                                onClick={() => startEdit(entry)}
                                className="border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800"
                            >
                                Edit
                            </button>
                            <button
                                type="button"
                                onClick={() => removeEntry(entry)}
                                disabled={isPending}
                                className="border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                </div>)}
            </div>}
        </section>

        {error ? <p className="border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-100">{error}</p> : null}
        {success ? <p className="border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-100">{success}</p> : null}
    </form>;
}
