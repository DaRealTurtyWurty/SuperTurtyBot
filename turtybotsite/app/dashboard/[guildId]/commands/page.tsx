import {redirect} from "next/navigation";
import CommandExplorer from "@/components/CommandExplorer";
import {requireCurrentSession} from "@/lib/auth";
import {fetchDashboardCommandCatalog, fetchDashboardCommandSettings, isDashboardApiError} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function CommandsPage({params}: {params: Promise<{guildId: string}>}) {
    const {guildId} = await params;
    const session = await requireCurrentSession();
    if (!session.guilds.some(guild => guild.id === guildId)) redirect("/dashboard");
    const data = await Promise.all([
        fetchDashboardCommandCatalog(), fetchDashboardCommandSettings(guildId, session.user.id)
    ]).catch(error => {
        if (isDashboardApiError(error) && error.status === 403) return null;
        return handleDashboardPageError(error);
    });
    if (!data) return <p role="alert" className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
        Commands could not be loaded. You need current Manage Server permission. Try signing in again if your access changed.
    </p>;
    return <CommandExplorer guildId={guildId} commands={data[0].commands} initialSettings={data[1]} />;
}
