import CountingSettingsForm from "@/components/CountingSettingsForm";
import {fetchDashboardCountingSettings} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function CountingPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardCountingSettings(guildId).catch(handleDashboardPageError);

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Counting settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Counting</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Manage the channels the bot uses for counting and choose the counting mode for each one.
            </p>
        </div>

        <CountingSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
