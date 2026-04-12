import WarningsSettingsForm from "@/components/WarningsSettingsForm";
import {fetchDashboardWarnings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function WarningsPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const data = await fetchDashboardWarnings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!data) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Warnings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Warnings</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Review current warning policy and recent warning records for this guild.
            </p>
        </div>

        <WarningsSettingsForm guildId={guildId} initialData={data} />
    </div>;
}
