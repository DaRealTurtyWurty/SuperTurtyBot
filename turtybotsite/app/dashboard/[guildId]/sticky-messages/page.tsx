import StickyMessagesSettingsForm from "@/components/StickyMessagesSettingsForm";
import {fetchDashboardStickyMessages, isDashboardApiError} from "@/lib/dashboard-api";

export default async function StickyMessagesPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardStickyMessages(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Sticky messages could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Sticky Messages</h2>
            <p className="mt-2 max-w-3xl text-sm text-slate-400">
                Manage sticky messages that stay at the bottom of active channels.
            </p>
        </div>

        <StickyMessagesSettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
