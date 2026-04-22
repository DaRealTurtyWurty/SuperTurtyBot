import VoiceChannelNotifierSettingsForm from "@/components/VoiceChannelNotifierSettingsForm";
import {fetchDashboardVoiceChannelNotifiers} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function VoiceChannelNotifiersPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardVoiceChannelNotifiers(guildId).catch(handleDashboardPageError);

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Voice channel notifiers could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Voice Channel Notifiers</h2>
            <p className="mt-2 max-w-3xl text-sm text-slate-400">
                Configure delayed announcements for specific voice channels. Each voice channel can post a custom
                message into one Discord text channel and optionally ping selected roles.
            </p>
        </div>

        <VoiceChannelNotifierSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
