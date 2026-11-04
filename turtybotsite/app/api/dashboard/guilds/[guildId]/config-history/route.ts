import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {fetchDashboardConfigHistory, isDashboardApiError} from "@/lib/dashboard-api";

export async function GET(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    try {
        const session = await getCurrentSession();
        if (!session) return NextResponse.json({message: "Sign in to view settings history."}, {status: 401});
        const {guildId} = await params;
        if (!session.guilds.some(guild => guild.id === guildId)) {
            return NextResponse.json({message: "You do not have access to this server."}, {status: 403});
        }
        const page = Number(request.nextUrl.searchParams.get("page") ?? "1");
        if (!Number.isInteger(page) || page < 1 || page > 10000) {
            return NextResponse.json({message: "Invalid history page."}, {status: 400});
        }
        return NextResponse.json(await fetchDashboardConfigHistory(guildId, session.user.id, page));
    } catch (error) {
        return NextResponse.json({message: isDashboardApiError(error) ? error.message : "Settings history could not be loaded."},
            {status: isDashboardApiError(error) ? error.status : 503});
    }
}
