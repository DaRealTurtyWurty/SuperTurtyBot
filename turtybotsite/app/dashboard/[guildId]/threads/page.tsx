import ThreadSettingsForm from "@/components/ThreadSettingsForm";
import {fetchDashboardThreadSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function ThreadsPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardThreadSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Thread settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Threads</h2>
        </div>

        <ThreadSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
