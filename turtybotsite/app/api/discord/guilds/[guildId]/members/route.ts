import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {fetchDashboardGuildMembers, isDashboardApiError} from "@/lib/dashboard-api";

export async function GET(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({message: "You must be signed in to view guild members."}, {status: 401});
    }

    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({message: "You do not have access to that guild."}, {status: 403});
    }

    const query = request.nextUrl.searchParams.get("query") ?? "";

    try {
        const members = await fetchDashboardGuildMembers(guildId, session.user.id, query);
        return NextResponse.json(members);
    } catch (error) {
        if (isDashboardApiError(error)) {
            return NextResponse.json({message: error.message}, {status: error.status});
        }

        if (error instanceof Error) {
            return NextResponse.json({message: error.message}, {status: 500});
        }

        return NextResponse.json({message: "Failed to load guild members."}, {status: 500});
    }
}
