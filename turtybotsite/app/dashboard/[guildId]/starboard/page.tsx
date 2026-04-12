import StarboardSettingsForm from "@/components/StarboardSettingsForm";
import {fetchDashboardStarboardSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function StarboardPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardStarboardSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Starboard settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Starboard</h2>
        </div>

        <StarboardSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
