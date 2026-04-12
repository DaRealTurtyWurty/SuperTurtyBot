import ModmailSettingsForm from "@/components/ModmailSettingsForm";
import {fetchDashboardModmailSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function ModmailPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardModmailSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Modmail settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Modmail</h2>
        </div>

        <ModmailSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
