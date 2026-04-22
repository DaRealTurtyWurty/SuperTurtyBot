import BirthdaySettingsForm from "@/components/BirthdaySettingsForm";
import {fetchDashboardBirthdaySettings} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function BirthdayPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const settings = await fetchDashboardBirthdaySettings(guildId).catch(handleDashboardPageError);

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Birthday settings could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Birthday</h2>
            <p className="mt-2 max-w-3xl text-sm text-slate-400">
                Choose the channel for birthday announcements and toggle whether the server announces them.
            </p>
        </div>

        <BirthdaySettingsForm guildId={guildId} initialSettings={settings} />
    </div>;
}
