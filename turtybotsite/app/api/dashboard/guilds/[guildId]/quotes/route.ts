import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {fetchDashboardQuotes, isDashboardApiError} from "@/lib/dashboard-api";
import {isManageableGuild} from "@/lib/discord";

function parsePositiveInt(value: string | null, fallback: number) {
    if (!value || value.trim().length === 0) {
        return fallback;
    }

    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export async function GET(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to view quotes."
        }, {status: 401});
    }

    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild || !isManageableGuild(guild.owner, guild.permissions)) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    const page = parsePositiveInt(request.nextUrl.searchParams.get("page"), 1);
    const pageSize = parsePositiveInt(request.nextUrl.searchParams.get("pageSize"), 10);

    try {
        return NextResponse.json(await fetchDashboardQuotes(guildId, page, pageSize));
    } catch (error) {
        if (isDashboardApiError(error)) {
            return NextResponse.json({
                message: error.message
            }, {status: error.status});
        }

        if (error instanceof Error) {
            return NextResponse.json({
                message: error.message
            }, {status: 500});
        }

        return NextResponse.json({
            message: "Failed to load quotes."
        }, {status: 500});
    }
}
