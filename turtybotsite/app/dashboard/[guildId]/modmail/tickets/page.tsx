import ModmailTicketsBrowser from "@/components/ModmailTicketsBrowser";
import {fetchDashboardModmailTickets} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function ModmailTicketsPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const tickets = await fetchDashboardModmailTickets(guildId).catch(handleDashboardPageError);

    if (!tickets) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Modmail tickets could not be loaded from the dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Modmail Tickets</h2>
        </div>

        <ModmailTicketsBrowser guildId={guildId} initialTickets={tickets} />
    </div>;
}
