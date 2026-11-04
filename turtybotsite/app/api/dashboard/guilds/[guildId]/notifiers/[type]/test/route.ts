import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {getPublicOrigin} from "@/lib/discord";
import {testDashboardNotifier, isDashboardApiError} from "@/lib/dashboard-api";

export async function POST(request: NextRequest, {params}: {params: Promise<{guildId: string; type: string}>}) {
    if (request.headers.get("origin") !== getPublicOrigin(request.nextUrl.origin)) {
        return NextResponse.json({message: "This request must come from the dashboard."}, {status: 403});
    }
    try {
        const session = await getCurrentSession();
        if (!session) return NextResponse.json({message: "Sign in to test notifiers."}, {status: 401});
        const {guildId, type} = await params;
        if (!session.guilds.some(guild => guild.id === guildId)) {
            return NextResponse.json({message: "You do not have access to this server."}, {status: 403});
        }
        const body = await request.json().catch(() => null);
        if (!body || typeof body !== "object" || typeof body.discordChannelId !== "string"
            || body.target !== null && typeof body.target !== "string") {
            return NextResponse.json({message: "Select an existing notifier to test."}, {status: 400});
        }
        return NextResponse.json(await testDashboardNotifier(guildId, type, {
            target: body.target, discordChannelId: body.discordChannelId, mention: null
        }));
    } catch (error) {
        return NextResponse.json({message: isDashboardApiError(error) ? error.message : "The test could not complete. Try again shortly."},
            {status: isDashboardApiError(error) ? error.status : 503});
    }
}
