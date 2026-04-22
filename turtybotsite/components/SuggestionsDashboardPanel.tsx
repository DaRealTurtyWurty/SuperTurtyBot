"use client";

import type {ChangeEvent} from "react";
import Image from "next/image";
import {useState, useTransition} from "react";
import type {DashboardSuggestionsPageResponse} from "@/lib/dashboard-api";

interface SuggestionsDashboardPanelProps {
    guildId: string;
    actorUserId: string;
    currentPage: number;
    pageSize: number;
    initialState: DashboardSuggestionsPageResponse;
}

const ACTION_LABELS: Record<string, string> = {
    PENDING: "Pending",
    APPROVED: "Approved",
    DENIED: "Denied",
    CONSIDERED: "Considered"
};

function formatDate(timestamp: number) {
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(timestamp));
}

function statusClass(status: string) {
    switch (status) {
        case "APPROVED":
            return "border-emerald-500/30 bg-emerald-500/10 text-emerald-100";
        case "DENIED":
            return "border-rose-500/30 bg-rose-500/10 text-rose-100";
        case "CONSIDERED":
            return "border-amber-500/30 bg-amber-500/10 text-amber-100";
        default:
            return "border-slate-700 bg-slate-900 text-slate-200";
    }
}

function isRenderableMediaUrl(value: string) {
    try {
        const url = new URL(value);
        return /\.(png|jpe?g|gif|webp|bmp|avif)$/i.test(url.pathname);
    } catch {
        return false;
    }
}

function getPreviewUrl(preview: NonNullable<DashboardSuggestionsPageResponse["suggestions"][number]["mediaPreview"]>) {
    return preview.url || preview.imageUrl || "";
}

function SuggestionMediaPreviewCard({preview}: {preview: NonNullable<DashboardSuggestionsPageResponse["suggestions"][number]["mediaPreview"]>}) {
    const previewUrl = getPreviewUrl(preview);
    const isImage = preview.type === "image" || (preview.imageUrl && isRenderableMediaUrl(preview.imageUrl));
    const label = preview.siteName?.trim() || (() => {
        try {
            return new URL(previewUrl).hostname.replace(/^www\./, "");
        } catch {
            return "Link";
        }
    })();

    if (isImage && preview.imageUrl) {
        return <a
            href={previewUrl}
            target="_blank"
            rel="noreferrer"
            className="group block overflow-hidden border border-slate-700 bg-slate-950/70 transition hover:border-sky-400/40 hover:bg-slate-900/80"
        >
            <Image
                src={preview.imageUrl}
                alt={preview.title || label}
                width={800}
                height={420}
                className="max-h-[420px] w-full object-contain"
                loading="lazy"
            />
            <div className="space-y-1 p-3">
                <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                    {label}
                </p>
                {preview.title ? <p className="text-sm font-semibold text-white">{preview.title}</p> : null}
                {preview.description ? <p className="text-sm text-slate-300">{preview.description}</p> : null}
            </div>
        </a>;
    }

    return <a
        href={previewUrl}
        target="_blank"
        rel="noreferrer"
        className="group block overflow-hidden border border-slate-700 bg-slate-950/70 transition hover:border-sky-400/40 hover:bg-slate-900/80"
    >
        <div className="grid gap-3 p-3 md:grid-cols-[minmax(0,1fr)_160px]">
            <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                    {label}
                </p>
                {preview.title ? <p className="mt-1 text-sm font-semibold text-white">{preview.title}</p> : null}
                {preview.description ? <p className="mt-1 text-sm text-slate-300">{preview.description}</p> : null}
                <p className="mt-2 truncate text-xs text-sky-300 group-hover:text-sky-200">{previewUrl}</p>
            </div>
            {preview.imageUrl ? <Image
                src={preview.imageUrl}
                alt={preview.title || label}
                width={160}
                height={112}
                className="h-28 w-full rounded-md border border-slate-700 object-cover md:h-full"
                loading="lazy"
            /> : null}
        </div>
    </a>;
}

export default function SuggestionsDashboardPanel({
    guildId,
    actorUserId,
    currentPage,
    pageSize,
    initialState
}: SuggestionsDashboardPanelProps) {
    const [state, setState] = useState(initialState);
    const [reasons, setReasons] = useState<Record<string, string>>({});
    const [busyMessageId, setBusyMessageId] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateReason(messageId: string, event: ChangeEvent<HTMLTextAreaElement>) {
        const value = event.target.value;
        setReasons(current => ({
            ...current,
            [messageId]: value
        }));
    }

    function performAction(messageId: string, action: "APPROVED" | "DENIED" | "CONSIDERED" | "DELETE") {
        const reason = reasons[messageId]?.trim() || "Unspecified";
        setError(null);
        setBusyMessageId(messageId);

        startTransition(async () => {
            try {
                const response = action === "DELETE"
                    ? await fetch(`/api/dashboard/guilds/${guildId}/suggestions/${encodeURIComponent(messageId)}?page=${currentPage}&pageSize=${pageSize}`, {
                        method: "DELETE",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            actorUserId,
                            reason
                        })
                    })
                    : await fetch(`/api/dashboard/guilds/${guildId}/suggestions/${encodeURIComponent(messageId)}?page=${currentPage}&pageSize=${pageSize}`, {
                        method: "PATCH",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            actorUserId,
                            reason,
                            action
                        })
                    });

                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    setError(payload?.message ?? "Failed to update suggestion.");
                    return;
                }

                const updated = await response.json() as DashboardSuggestionsPageResponse;
                setState(updated);
            } finally {
                setBusyMessageId(null);
            }
        });
    }

    return <section className="space-y-4">
        <div className="flex items-end justify-between gap-4">
            <div>
                <h3 className="text-2xl font-semibold text-white">Suggestions</h3>
                <p className="mt-1 text-sm text-slate-400">
                    {state.suggestions.length} loaded. Use buttons to approve, deny, consider, or delete.
                </p>
            </div>
            {state.suggestionsChannelId ? <a
                href={`https://discord.com/channels/${guildId}/${state.suggestionsChannelId}`}
                target="_blank"
                rel="noreferrer"
                className="border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
            >
                Open Channel
            </a> : null}
        </div>

        {error ? <p className="border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
            {error}
        </p> : null}

        <div className="space-y-4">
            {state.suggestions.length === 0 ? <div className="border border-slate-800/80 bg-slate-950/60 p-6 text-sm text-slate-400">
                No suggestions found.
            </div> : state.suggestions.map(suggestion => <article
                key={suggestion.messageId}
                className="border border-slate-800/80 bg-slate-950/60 p-5"
            >
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        <div className="flex flex-wrap items-center gap-3">
                            <h4 className="text-lg font-semibold text-white">Suggestion #{suggestion.number + 1}</h4>
                            <span className={`border px-2 py-0.5 text-xs font-semibold uppercase tracking-[0.16em] ${statusClass(suggestion.status)}`}>
                                {ACTION_LABELS[suggestion.status] ?? suggestion.status}
                            </span>
                        </div>
                        <p className="mt-2 text-sm text-slate-400">
                            By {suggestion.userDisplayName} at {formatDate(suggestion.createdAt)}
                        </p>
                    </div>

                    {suggestion.messageUrl ? <a
                        href={suggestion.messageUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="border border-slate-700 px-3 py-2 text-sm text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
                    >
                        Open Message
                    </a> : null}
                </div>

                <div className="mt-4 rounded border border-slate-800 bg-slate-900/70 p-4">
                    <p className="whitespace-pre-wrap text-sm text-slate-100">{suggestion.content}</p>
                    {suggestion.mediaPreview ? <div className="mt-4">
                        <SuggestionMediaPreviewCard preview={suggestion.mediaPreview} />
                    </div> : null}
                </div>

                {suggestion.responses.length > 0 ? <div className="mt-4 space-y-2">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">History</p>
                    {suggestion.responses.map(response => <div key={`${suggestion.messageId}:${response.respondedAt}:${response.type}`} className="border border-slate-800 bg-slate-900/70 p-3 text-sm text-slate-300">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                            <p>
                                <span className="font-semibold text-slate-100">{response.type}</span>
                                {" "}by {response.responderDisplayName}
                            </p>
                            <p className="text-xs text-slate-500">{formatDate(response.respondedAt)}</p>
                        </div>
                        <p className="mt-2 whitespace-pre-wrap">{response.content}</p>
                    </div>)}
                </div> : null}

                <div className="mt-4 grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
                    <label className="block border border-slate-800 bg-slate-900/70 p-4">
                        <p className="text-sm font-semibold text-white">Reason / note</p>
                        <textarea
                            value={reasons[suggestion.messageId] ?? ""}
                            onChange={event => updateReason(suggestion.messageId, event)}
                            rows={3}
                            className="mt-3 w-full border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition focus:border-sky-400"
                            placeholder="Optional reason shown in response"
                        />
                    </label>

                    <div className="grid gap-2 self-start sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
                        <button
                            type="button"
                            disabled={isPending && busyMessageId === suggestion.messageId}
                            onClick={() => performAction(suggestion.messageId, "APPROVED")}
                            className="border border-emerald-400 bg-emerald-400 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-emerald-300 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            Approve
                        </button>
                        <button
                            type="button"
                            disabled={isPending && busyMessageId === suggestion.messageId}
                            onClick={() => performAction(suggestion.messageId, "DENIED")}
                            className="border border-rose-400 bg-rose-400 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-rose-300 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            Deny
                        </button>
                        <button
                            type="button"
                            disabled={isPending && busyMessageId === suggestion.messageId}
                            onClick={() => performAction(suggestion.messageId, "CONSIDERED")}
                            className="border border-amber-400 bg-amber-400 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            Consider
                        </button>
                        <button
                            type="button"
                            disabled={isPending && busyMessageId === suggestion.messageId}
                            onClick={() => performAction(suggestion.messageId, "DELETE")}
                            className="border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-500 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            Delete
                        </button>
                    </div>
                </div>
            </article>)}
        </div>
    </section>;
}
