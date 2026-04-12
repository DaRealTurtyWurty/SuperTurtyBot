import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {fetchDashboardGuildChannels, isDashboardApiError} from "@/lib/dashboard-api";

export async function GET(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({message: "You must be signed in to view guild channels."}, {status: 401});
    }

    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({message: "You do not have access to that guild."}, {status: 403});
    }

    try {
        const channels = await fetchDashboardGuildChannels(guildId, session.user.id);
        return NextResponse.json(channels);
    } catch (error) {
        if (isDashboardApiError(error)) {
            return NextResponse.json({message: error.message}, {status: error.status});
        }

        if (error instanceof Error) {
            return NextResponse.json({message: error.message}, {status: 500});
        }

        return NextResponse.json({message: "Failed to load guild channels."}, {status: 500});
    }
}
