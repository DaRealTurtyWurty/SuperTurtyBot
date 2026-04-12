import TagsDashboardPanel from "@/components/TagsDashboardPanel";
import {fetchDashboardTags, isDashboardApiError} from "@/lib/dashboard-api";

function parsePage(value: string | string[] | undefined, fallback: number) {
    const raw = Array.isArray(value) ? value[0] : value;
    if (!raw || raw.trim().length === 0) {
        return fallback;
    }

    const parsed = Number.parseInt(raw, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export default async function TagsPage({
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

    const tags = await fetchDashboardTags(guildId, page, pageSize).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!tags) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Tags could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Tags</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Create, browse, and remove every tag stored for this server.
            </p>
        </div>

        <TagsDashboardPanel guildId={guildId} initialState={tags} />
    </div>;
}
