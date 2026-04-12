"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import {useRouter} from "next/navigation";
import type {DashboardTagsPageResponse, DashboardTagCreateRequest} from "@/lib/dashboard-api";

interface TagsDashboardPanelProps {
    guildId: string;
    initialState: DashboardTagsPageResponse;
}

export default function TagsDashboardPanel({guildId, initialState}: TagsDashboardPanelProps) {
    const [state, setState] = useState(initialState);
    const [name, setName] = useState("");
    const [contentType, setContentType] = useState<DashboardTagCreateRequest["contentType"]>("message");
    const [content, setContent] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [pendingTagName, setPendingTagName] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();
    const router = useRouter();

    function goToPage(page: number) {
        const searchParams = new URLSearchParams({
            page: page.toString(),
            pageSize: state.pageSize.toString()
        });

        router.push(`/dashboard/${guildId}/tags?${searchParams.toString()}`);
    }

    function handleCreate(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        const trimmedName = name.trim();
        if (!trimmedName || !content.trim()) {
            setError("Tag name and content are required.");
            return;
        }

        startTransition(async () => {
            try {
                const response = await fetch(`/api/dashboard/guilds/${guildId}/tags?page=${state.page}&pageSize=${state.pageSize}`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        name: trimmedName,
                        contentType,
                        content
                    })
                });

                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    throw new Error(payload?.message ?? "Failed to create tag.");
                }

                const updated = await response.json() as DashboardTagsPageResponse;
                setState(updated);
                setName("");
                setContent("");
                setContentType("message");
                setSuccess(`Tag ${trimmedName} created.`);
                router.replace(`/dashboard/${guildId}/tags?page=${updated.page}&pageSize=${updated.pageSize}`);
            } catch (createError) {
                setError(createError instanceof Error ? createError.message : "Failed to create tag.");
            }
        });
    }

    function handleDelete(tagName: string) {
        if (!window.confirm(`Remove tag ${tagName}?`)) {
            return;
        }

        setError(null);
        setSuccess(null);
        setPendingTagName(tagName);

        startTransition(async () => {
            try {
                const response = await fetch(`/api/dashboard/guilds/${guildId}/tags?name=${encodeURIComponent(tagName)}&page=${state.page}&pageSize=${state.pageSize}`, {
                    method: "DELETE"
                });

                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    throw new Error(payload?.message ?? "Failed to remove tag.");
                }

                const updated = await response.json() as DashboardTagsPageResponse;
                setState(updated);
                setSuccess(`Tag ${tagName} removed.`);
                router.replace(`/dashboard/${guildId}/tags?page=${updated.page}&pageSize=${updated.pageSize}`);
            } catch (saveError) {
                setError(saveError instanceof Error ? saveError.message : "Failed to remove tag.");
            } finally {
                setPendingTagName(null);
            }
        });
    }

    return <div className="space-y-5">
        <form onSubmit={handleCreate} className="space-y-4 border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="space-y-2">
                <h3 className="text-lg font-semibold text-white">Create Tag</h3>
                <p className="text-sm text-slate-400">
                    Tags created here use the same payload format as the bot command.
                </p>
            </div>

            <div className="grid gap-4 lg:grid-cols-[1fr_220px]">
                <label className="space-y-2">
                    <span className="text-sm font-semibold text-slate-200">Tag Name</span>
                    <input
                        type="text"
                        value={name}
                        onChange={event => setName(event.target.value)}
                        placeholder="Helpful name"
                        maxLength={64}
                        className="w-full border border-slate-800 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-sky-500"
                    />
                </label>

                <label className="space-y-2">
                    <span className="text-sm font-semibold text-slate-200">Content Type</span>
                    <select
                        value={contentType}
                        onChange={event => setContentType(event.target.value === "embed" ? "embed" : "message")}
                        className="w-full border border-slate-800 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition focus:border-sky-500"
                    >
                        <option value="message">Message</option>
                        <option value="embed">Embed</option>
                    </select>
                </label>
            </div>

            <label className="space-y-2 block">
                <span className="text-sm font-semibold text-slate-200">
                    {contentType === "embed" ? "Embed Name" : "Message Content"}
                </span>
                <textarea
                    value={content}
                    onChange={event => setContent(event.target.value)}
                    placeholder={contentType === "embed" ? "Your saved embed name" : "Tag text"}
                    maxLength={2000}
                    rows={6}
                    className="w-full border border-slate-800 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-sky-500"
                />
            </label>

            <p className="text-xs text-slate-500">
                {contentType === "embed"
                    ? "The embed name must already exist for the dashboard account."
                    : "Plain message tags are sent as simple text."}
            </p>

            <div className="flex justify-end">
                <button
                    type="submit"
                    disabled={isPending}
                    className="border border-sky-400 bg-sky-400 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isPending ? "Creating..." : "Create Tag"}
                </button>
            </div>
        </form>

        <div className="flex flex-wrap items-center justify-between gap-4 border border-slate-800/80 bg-slate-950/60 p-4 text-sm text-slate-300">
            <p>
                Page {state.page} of {state.totalPages} {" "}
                ({state.totalCount.toLocaleString()} total tags)
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

        {state.tags.length === 0 ? <div className="border border-slate-800/80 bg-slate-950/60 p-6 text-sm text-slate-400">
            No tags saved yet.
        </div> : <div className="space-y-4">
            {state.tags.map(tag => <article key={tag.name} className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 flex-1 space-y-3">
                        <div className="flex flex-wrap items-center gap-2">
                            <h3 className="text-lg font-semibold text-white">{tag.name}</h3>
                            <span className="rounded-full border border-slate-700 px-2 py-1 text-[11px] uppercase tracking-[0.14em] text-slate-300">
                                {tag.contentType}
                            </span>
                        </div>

                        <div className="grid gap-2 text-sm text-slate-300 md:grid-cols-2">
                            <p>Created by: {tag.userDisplayName}</p>
                            <p>User ID: <span className="font-mono text-slate-400">{tag.userId}</span></p>
                        </div>

                        <div className="space-y-2">
                            <p className="text-sm font-semibold text-slate-200">Content</p>
                            <div className="border border-slate-800/80 bg-slate-950/70 px-4 py-3 text-sm leading-6 text-slate-200 whitespace-pre-wrap">
                                {tag.content || tag.rawData}
                            </div>
                        </div>

                        {tag.rawData && tag.rawData !== tag.content ? <details className="border border-slate-800/80 bg-slate-950/50 px-4 py-3">
                            <summary className="cursor-pointer text-sm font-semibold text-slate-200">Raw Data</summary>
                            <pre className="mt-3 overflow-x-auto whitespace-pre-wrap text-xs leading-5 text-slate-400">{tag.rawData}</pre>
                        </details> : null}
                    </div>

                    <button
                        type="button"
                        onClick={() => void handleDelete(tag.name)}
                        disabled={pendingTagName === tag.name || isPending}
                        className="border border-red-500/50 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-100 transition hover:border-red-400 hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {pendingTagName === tag.name ? "Removing..." : "Remove"}
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
