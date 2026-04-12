import NsfwSettingsForm from "@/components/NsfwSettingsForm";
import {fetchDashboardNsfwSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function NsfwPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardNsfwSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            NSFW settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">NSFW</h2>
        </div>

        <NsfwSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
