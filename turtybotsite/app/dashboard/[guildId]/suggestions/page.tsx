import Link from "next/link";
import SuggestionsSettingsForm from "@/components/SuggestionsSettingsForm";
import {fetchDashboardSuggestionsSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function SuggestionsPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardSuggestionsSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    return <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
                <h2 className="text-3xl font-bold tracking-tight">Suggestions</h2>
                <p className="mt-2 max-w-3xl text-sm text-slate-400">
                    Configure suggestion channel and manage suggestion list on dedicated sub-page.
                </p>
            </div>

            <Link
                href={`/dashboard/${guildId}/suggestions/list`}
                className="border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
            >
                View Suggestions
            </Link>
        </div>

        {settings ? <SuggestionsSettingsForm guildId={guildId} initialSettings={settings} /> : <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Suggestions settings could not be loaded from the dashboard API.
        </div>}
    </div>;
}
