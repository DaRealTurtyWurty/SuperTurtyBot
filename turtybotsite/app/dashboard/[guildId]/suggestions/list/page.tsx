import Link from "next/link";
import SuggestionsDashboardPanel from "@/components/SuggestionsDashboardPanel";
import {
    fetchDashboardSuggestions,
    fetchDashboardSuggestionsSettings
} from "@/lib/dashboard-api";
import {requireCurrentSession} from "@/lib/auth";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

function parsePage(value: string | string[] | undefined, fallback: number) {
    const raw = Array.isArray(value) ? value[0] : value;
    if (!raw || raw.trim().length === 0) {
        return fallback;
    }

    const parsed = Number.parseInt(raw, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export default async function SuggestionsListPage({
    params,
    searchParams
}: {
    params: Promise<{ guildId: string }>;
    searchParams: Promise<{ page?: string; pageSize?: string }>;
}) {
    const session = await requireCurrentSession();
    const guildId = (await params).guildId;
    const query = await searchParams;
    const page = parsePage(query.page, 1);
    const pageSize = parsePage(query.pageSize, 10);

    const settings = await fetchDashboardSuggestionsSettings(guildId).catch(handleDashboardPageError);
    const suggestions = await fetchDashboardSuggestions(guildId, page, pageSize).catch(handleDashboardPageError);

    return <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
                <h2 className="text-3xl font-bold tracking-tight">Suggestion List</h2>
                <p className="mt-2 max-w-3xl text-sm text-slate-400">
                    Paginated moderation view for guild suggestions.
                </p>
            </div>

            <div className="flex flex-wrap gap-3">
                <Link
                    href={`/dashboard/${guildId}/suggestions`}
                    className="border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
                >
                    Back To Settings
                </Link>
                {settings?.suggestionsChannelId ? <a
                    href={`https://discord.com/channels/${guildId}/${settings.suggestionsChannelId}`}
                    target="_blank"
                    rel="noreferrer"
                    className="border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
                >
                    Open Channel
                </a> : null}
            </div>
        </div>

        {suggestions ? <SuggestionsDashboardPanel
            guildId={guildId}
            actorUserId={session.user.id}
            currentPage={suggestions.page}
            pageSize={suggestions.pageSize}
            initialState={suggestions}
        /> : <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Suggestions list could not be loaded from the dashboard API.
        </div>}

        {suggestions ? <div className="flex flex-wrap items-center justify-between gap-4 border border-slate-800/80 bg-slate-950/60 p-4 text-sm text-slate-300">
            <p>
                Page {suggestions.page} of {suggestions.totalPages} {" "}
                ({suggestions.totalCount.toLocaleString()} total suggestions)
            </p>
            <div className="flex gap-2">
                <Link
                    href={`/dashboard/${guildId}/suggestions/list?page=${Math.max(1, suggestions.page - 1)}&pageSize=${suggestions.pageSize}`}
                    aria-disabled={suggestions.page <= 1}
                    className={`border px-3 py-2 transition ${
                        suggestions.page <= 1
                            ? "pointer-events-none border-slate-800 text-slate-600"
                            : "border-slate-700 text-slate-200 hover:border-slate-500 hover:bg-slate-900"
                    }`}
                >
                    Previous
                </Link>
                <Link
                    href={`/dashboard/${guildId}/suggestions/list?page=${Math.min(suggestions.totalPages, suggestions.page + 1)}&pageSize=${suggestions.pageSize}`}
                    aria-disabled={suggestions.page >= suggestions.totalPages}
                    className={`border px-3 py-2 transition ${
                        suggestions.page >= suggestions.totalPages
                            ? "pointer-events-none border-slate-800 text-slate-600"
                            : "border-slate-700 text-slate-200 hover:border-slate-500 hover:bg-slate-900"
                    }`}
                >
                    Next
                </Link>
            </div>
        </div> : null}
    </div>;
}
