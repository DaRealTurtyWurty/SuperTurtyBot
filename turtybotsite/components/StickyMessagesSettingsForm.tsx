"use client";

import {useMemo, useState, useTransition} from "react";
import type {FormEvent} from "react";
import type {DashboardStickyMessagesResponse} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface StickyMessagesSettingsFormProps {
    guildId: string;
    initialSettings: DashboardStickyMessagesResponse;
}

export default function StickyMessagesSettingsForm({guildId, initialSettings}: StickyMessagesSettingsFormProps) {
    const [stickyMessages, setStickyMessages] = useState(initialSettings.stickyMessages);
    const [selectedChannelId, setSelectedChannelId] = useState(initialSettings.stickyMessages[0]?.channelId ?? "");
    const [content, setContent] = useState(initialSettings.stickyMessages[0]?.hasEmbed ? "" : initialSettings.stickyMessages[0]?.content ?? "");
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const selectedSticky = useMemo(
        () => stickyMessages.find(entry => entry.channelId === selectedChannelId) ?? null,
        [selectedChannelId, stickyMessages]
    );

    function syncSelection(channelId: string) {
        setSelectedChannelId(channelId);
        const sticky = stickyMessages.find(entry => entry.channelId === channelId) ?? null;
        setContent(sticky?.hasEmbed ? "" : sticky?.content ?? "");
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!selectedChannelId.trim()) {
            setError("Pick a channel first.");
            return;
        }

        if (!content.trim()) {
            setError("Sticky content cannot be blank.");
            return;
        }

        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/sticky-messages`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    channelId: selectedChannelId,
                    content
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save sticky message.");
                return;
            }

            const updated = await response.json() as DashboardStickyMessagesResponse;
            setStickyMessages(updated.stickyMessages);
            setSuccess("Sticky message saved.");
        });
    }

    function clearSticky(channelId: string) {
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/sticky-messages/${encodeURIComponent(channelId)}`, {
                method: "DELETE"
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to clear sticky message.");
                return;
            }

            const updated = await response.json() as DashboardStickyMessagesResponse;
            setStickyMessages(updated.stickyMessages);
            if (selectedChannelId === channelId) {
                setContent("");
            }
            setSuccess("Sticky message cleared.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-6">
        <section id="sticky-editor" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <div className="space-y-5">
                <div>
                    <p className="text-sm font-semibold text-white">Sticky Message Editor</p>
                    <p className="mt-1 text-sm text-slate-400">Set a sticky text message for a message-capable channel. Existing embed stickies can be replaced with text here.</p>
                </div>

                <GuildChannelSelect
                    id="sticky-channel"
                    guildId={guildId}
                    value={selectedChannelId}
                    onChange={syncSelection}
                    label="Sticky Channel"
                    description="Only message channels can use sticky messages."
                    allowTypes={["text", "announcement", "forum", "media", "thread"]}
                    placeholder="Select a channel"
                />

                <label className="block">
                    <div className="flex items-center justify-between gap-4">
                        <div>
                            <p className="text-sm font-semibold text-white">Sticky Text</p>
                            <p className="mt-1 text-sm text-slate-400">This message will be reposted to the bottom of the channel after new messages.</p>
                        </div>
                        <span className="text-xs text-slate-500">{content.length}/2000</span>
                    </div>
                    <textarea
                        value={content}
                        onChange={event => setContent(event.target.value)}
                        rows={7}
                        className="mt-4 w-full resize-y border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-sky-400"
                        placeholder="Write the sticky message here..."
                    />
                </label>

                {selectedSticky?.hasEmbed ? <p className="text-sm text-amber-200">
                    This channel currently uses a saved embed sticky. Saving text here will replace it.
                </p> : null}

                <div className="flex flex-wrap items-center justify-between gap-3">
                    <button
                        type="submit"
                        disabled={isPending}
                        className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {isPending ? "Saving..." : selectedSticky ? "Update Sticky" : "Save Sticky"}
                    </button>

                    {selectedSticky ? <button
                        type="button"
                        onClick={() => clearSticky(selectedSticky.channelId)}
                        disabled={isPending}
                        className="border border-slate-700 bg-slate-900 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        Clear Sticky
                    </button> : null}
                </div>
            </div>
        </section>

        <section id="sticky-list" className="space-y-4 scroll-mt-24">
            <div>
                <p className="text-sm font-semibold text-white">Current Stickies</p>
                <p className="mt-1 text-sm text-slate-400">These channels already have a sticky message configured.</p>
            </div>

            {stickyMessages.length === 0 ? <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                No sticky messages configured yet.
            </div> : <div className="grid gap-3">
                {stickyMessages.map(sticky => <div
                    key={sticky.channelId}
                    className="border border-slate-800/80 bg-slate-950/60 p-5"
                >
                    <div className="flex flex-wrap items-start justify-between gap-4">
                        <div className="min-w-0 space-y-2">
                            <div className="flex flex-wrap items-center gap-2">
                                <p className="text-sm font-semibold text-white">
                                    {sticky.connected ? `#${sticky.channelName}` : "Unknown Channel"}
                                </p>
                                <span className={`border px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.14em] ${
                                    sticky.hasEmbed
                                        ? "border-amber-400/30 bg-amber-400/10 text-amber-200"
                                        : "border-sky-400/30 bg-sky-400/10 text-sky-200"
                                }`}>
                                    {sticky.hasEmbed ? "Embed" : "Text"}
                                </span>
                                {!sticky.connected ? <span className="text-xs text-slate-500">Disconnected</span> : null}
                            </div>
                            <p className="text-xs text-slate-500 font-mono">{sticky.channelId}</p>
                            <p className="text-sm text-slate-300">
                                {sticky.hasEmbed ? "Saved embed sticky." : sticky.content || "No text content."}
                            </p>
                            <p className="text-xs text-slate-500">
                                Updated {new Date(sticky.updatedAt).toLocaleString()} · Owner {sticky.ownerDisplayName}
                                {sticky.ownerId === "0" ? "" : ` (${sticky.ownerId})`}
                            </p>
                        </div>

                        <div className="flex flex-wrap gap-2">
                            <button
                                type="button"
                                onClick={() => syncSelection(sticky.channelId)}
                                className="border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800"
                            >
                                Edit
                            </button>
                            <button
                                type="button"
                                onClick={() => clearSticky(sticky.channelId)}
                                disabled={isPending}
                                className="border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                Clear
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
