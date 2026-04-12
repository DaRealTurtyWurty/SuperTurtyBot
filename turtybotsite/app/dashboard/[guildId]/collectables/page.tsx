import CollectablesSettingsForm from "@/components/CollectablesSettingsForm";
import {fetchDashboardCollectablesSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function CollectablesPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardCollectablesSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Collectables settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Collectables</h2>
            <p className="mt-2 max-w-3xl text-sm text-slate-400">
                Configure the collector channel and which collections can spawn. Open a collection page to manage its disabled items.
            </p>
        </div>

        <CollectablesSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
