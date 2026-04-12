import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {fetchDashboardSuggestionsSettings, isDashboardApiError, updateDashboardSuggestionsSettings} from "@/lib/dashboard-api";
import {isManageableGuild} from "@/lib/discord";

export async function GET(_: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to view suggestions settings."
        }, {status: 401});
    }

    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild || !isManageableGuild(guild.owner, guild.permissions)) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    try {
        return NextResponse.json(await fetchDashboardSuggestionsSettings(guildId));
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
            message: "Failed to load suggestions settings."
        }, {status: 500});
    }
}

export async function PUT(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to update suggestions settings."
        }, {status: 401});
    }

    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild || !isManageableGuild(guild.owner, guild.permissions)) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    const payload = await request.json();

    try {
        return NextResponse.json(await updateDashboardSuggestionsSettings(guildId, payload));
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
            message: "Failed to update suggestions settings."
        }, {status: 500});
    }
}
