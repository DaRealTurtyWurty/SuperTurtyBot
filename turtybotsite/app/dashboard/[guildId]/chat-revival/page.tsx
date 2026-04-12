import ChatRevivalSettingsForm from "@/components/ChatRevivalSettingsForm";
import {fetchDashboardChatRevivalSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function ChatRevivalPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardChatRevivalSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Chat revival settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Chat Revival</h2>
        </div>

        <ChatRevivalSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
