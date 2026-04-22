import OptInChannelsSettingsForm from "@/components/OptInChannelsSettingsForm";
import {fetchDashboardOptInChannelsSettings} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function OptInChannelsPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardOptInChannelsSettings(guildId).catch(handleDashboardPageError);

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Opt-in channels settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Opt-In Channels</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Pick channels members can unlock with /opt in. The bot keeps those channels hidden until a user opts in.
            </p>
        </div>

        <OptInChannelsSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
