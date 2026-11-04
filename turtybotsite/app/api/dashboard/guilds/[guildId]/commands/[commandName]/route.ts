import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {getPublicOrigin} from "@/lib/discord";
import {isDashboardApiError, updateDashboardCommand} from "@/lib/dashboard-api";

export async function PATCH(request: NextRequest, {params}: {params: Promise<{guildId: string; commandName: string}>}) {
    if (request.headers.get("origin") !== getPublicOrigin(request.nextUrl.origin)) {
        return NextResponse.json({message: "This request must come from the dashboard."}, {status: 403});
    }
    try {
        const session = await getCurrentSession();
        if (!session) return NextResponse.json({message: "Sign in to change commands."}, {status: 401});
        const {guildId, commandName} = await params;
        if (!session.guilds.some(guild => guild.id === guildId)) {
            return NextResponse.json({message: "You do not have access to this server."}, {status: 403});
        }
        let body: unknown;
        try {
            body = await request.json();
        } catch {
            return NextResponse.json({message: "The command settings payload was invalid."}, {status: 400});
        }
        if (!body || typeof body !== "object" || !("enabled" in body) || typeof body.enabled !== "boolean") {
            return NextResponse.json({message: "Choose whether to enable this command."}, {status: 400});
        }
        return NextResponse.json(await updateDashboardCommand(guildId, commandName, body.enabled));
    } catch (error) {
        return NextResponse.json({message: isDashboardApiError(error) ? error.message : "The command could not be saved. Try again shortly."},
            {status: isDashboardApiError(error) ? error.status : 503});
    }
}
