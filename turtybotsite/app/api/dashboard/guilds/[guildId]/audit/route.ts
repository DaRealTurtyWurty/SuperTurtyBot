import {NextRequest, NextResponse} from "next/server";
import {getPublicOrigin} from "@/lib/discord";
import {getCurrentSession} from "@/lib/auth";
import {fetchDashboardAudit, isDashboardApiError} from "@/lib/dashboard-api";

async function handleAudit(request: NextRequest, params: Promise<{guildId: string}>, method: "GET" | "POST") {
    // Only accept changes from this site. The user ID always comes from the signed-in session.
    if (method === "POST" && request.headers.get("origin") !== getPublicOrigin(request.nextUrl.origin)) {
        return NextResponse.json({message: "This request must come from the dashboard."}, {status: 403});
    }
    try {
        const session = await getCurrentSession();
        if (!session) {
            return NextResponse.json({message: "Sign in to view server audits."}, {status: 401});
        }
        const {guildId} = await params;
        if (!session.guilds.some(guild => guild.id === guildId)) {
            return NextResponse.json({message: "You do not have access to this server."}, {status: 403});
        }
        return NextResponse.json(await fetchDashboardAudit(guildId, session.user.id, method));
    } catch (error) {
        return NextResponse.json({
            message: isDashboardApiError(error) ? error.message : "The audit could not be loaded. Try again shortly."
        }, {status: isDashboardApiError(error) ? error.status : 503});
    }
}

export async function GET(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    return handleAudit(request, params, "GET");
}

export async function POST(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    return handleAudit(request, params, "POST");
}
