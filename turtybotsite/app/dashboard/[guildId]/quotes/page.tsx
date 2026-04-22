import QuotesDashboardPanel from "@/components/QuotesDashboardPanel";
import {fetchDashboardQuotes} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

function parsePage(value: string | string[] | undefined, fallback: number) {
    const raw = Array.isArray(value) ? value[0] : value;
    if (!raw || raw.trim().length === 0) {
        return fallback;
    }

    const parsed = Number.parseInt(raw, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export default async function QuotesPage({
    params,
    searchParams
}: {
    params: Promise<{ guildId: string }>;
    searchParams: Promise<{ page?: string; pageSize?: string }>;
}) {
    const guildId = (await params).guildId;
    const query = await searchParams;
    const page = parsePage(query.page, 1);
    const pageSize = parsePage(query.pageSize, 10);

    const quotes = await fetchDashboardQuotes(guildId, page, pageSize).catch(handleDashboardPageError);

    if (!quotes) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Quotes could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Quotes</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Browse every stored quote in this server and remove entries you no longer want saved.
            </p>
        </div>

        <QuotesDashboardPanel guildId={guildId} initialState={quotes} />
    </div>;
}
