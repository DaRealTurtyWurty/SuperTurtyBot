"use client";

import Link from "next/link";
import {useState, useTransition} from "react";
import {useRouter} from "next/navigation";
import type {DashboardQuotesPageResponse} from "@/lib/dashboard-api";

interface QuotesDashboardPanelProps {
    guildId: string;
    initialState: DashboardQuotesPageResponse;
}

function formatDate(timestamp: number) {
    return new Date(timestamp).toLocaleString();
}

export default function QuotesDashboardPanel({guildId, initialState}: QuotesDashboardPanelProps) {
    const [state, setState] = useState(initialState);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [pendingQuoteNumber, setPendingQuoteNumber] = useState<number | null>(null);
    const [isPending, startTransition] = useTransition();
    const router = useRouter();

    function goToPage(page: number) {
        const searchParams = new URLSearchParams({
            page: page.toString(),
            pageSize: state.pageSize.toString()
        });

        router.push(`/dashboard/${guildId}/quotes?${searchParams.toString()}`);
    }

    function handleDelete(quoteNumber: number) {
        if (!window.confirm(`Remove quote #${quoteNumber}?`)) {
            return;
        }

        setError(null);
        setSuccess(null);
        setPendingQuoteNumber(quoteNumber);

        startTransition(async () => {
            try {
                const response = await fetch(`/api/dashboard/guilds/${guildId}/quotes/${quoteNumber}?page=${state.page}&pageSize=${state.pageSize}`, {
                    method: "DELETE"
                });

                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    throw new Error(payload?.message ?? "Failed to remove quote.");
                }

                const updated = await response.json() as DashboardQuotesPageResponse;
                setState(updated);
                setSuccess(`Quote #${quoteNumber} removed.`);
                router.replace(`/dashboard/${guildId}/quotes?page=${updated.page}&pageSize=${updated.pageSize}`);
            } catch (saveError) {
                setError(saveError instanceof Error ? saveError.message : "Failed to remove quote.");
            } finally {
                setPendingQuoteNumber(null);
            }
        });
    }

    return <div className="space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-4 border border-slate-800/80 bg-slate-950/60 p-4 text-sm text-slate-300">
            <p>
                Page {state.page} of {state.totalPages} {" "}
                ({state.totalCount.toLocaleString()} total quotes)
            </p>
            <div className="flex gap-2">
                <button
                    type="button"
                    onClick={() => goToPage(Math.max(1, state.page - 1))}
                    disabled={state.page <= 1 || isPending}
                    className={`border px-3 py-2 transition ${
                        state.page <= 1 || isPending
                            ? "pointer-events-none border-slate-800 text-slate-600"
                            : "border-slate-700 text-slate-200 hover:border-slate-500 hover:bg-slate-900"
                    }`}
                >
                    Previous
                </button>
                <button
                    type="button"
                    onClick={() => goToPage(Math.min(state.totalPages, state.page + 1))}
                    disabled={state.page >= state.totalPages || isPending}
                    className={`border px-3 py-2 transition ${
                        state.page >= state.totalPages || isPending
                            ? "pointer-events-none border-slate-800 text-slate-600"
                            : "border-slate-700 text-slate-200 hover:border-slate-500 hover:bg-slate-900"
                    }`}
                >
                    Next
                </button>
            </div>
        </div>

        {state.quotes.length === 0 ? <div className="border border-slate-800/80 bg-slate-950/60 p-6 text-sm text-slate-400">
            No quotes saved yet.
        </div> : <div className="space-y-4">
            {state.quotes.map(quote => <article key={quote.number} className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 flex-1 space-y-3">
                        <div className="flex flex-wrap items-center gap-2">
                            <h3 className="text-lg font-semibold text-white">Quote #{quote.number}</h3>
                            <span className="text-xs text-slate-500">{formatDate(quote.timestamp)}</span>
                        </div>

                        <blockquote className="border-l-4 border-slate-700 bg-slate-950/70 px-4 py-3 text-sm leading-6 text-slate-200 whitespace-pre-wrap">
                            {quote.text}
                        </blockquote>

                        <div className="grid gap-2 text-sm text-slate-300 md:grid-cols-2">
                            <p> Said by: {quote.userDisplayName}</p>
                            <p> Added by: {quote.addedByDisplayName}</p>
                            <p> User ID: <span className="font-mono text-slate-400">{quote.userId}</span></p>
                            <p> Added By ID: <span className="font-mono text-slate-400">{quote.addedById}</span></p>
                        </div>

                        <div className="flex flex-wrap gap-3 text-sm">
                            {quote.messageUrl ? <Link
                                href={quote.messageUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="border border-slate-700 px-3 py-2 text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
                            >
                                Open Message
                            </Link> : null}
                            {quote.channelId ? <p className="px-3 py-2 text-slate-400">
                                Channel ID: <span className="font-mono">{quote.channelId}</span>
                            </p> : null}
                            {quote.messageId ? <p className="px-3 py-2 text-slate-400">
                                Message ID: <span className="font-mono">{quote.messageId}</span>
                            </p> : null}
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={() => void handleDelete(quote.number)}
                        disabled={pendingQuoteNumber === quote.number || isPending}
                        className="border border-red-500/50 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-100 transition hover:border-red-400 hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {pendingQuoteNumber === quote.number ? "Removing..." : "Remove"}
                    </button>
                </div>
            </article>)}
        </div>}

        {error ? <p className="border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
            {error}
        </p> : null}

        {success ? <p className="border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100">
            {success}
        </p> : null}
    </div>;
}
