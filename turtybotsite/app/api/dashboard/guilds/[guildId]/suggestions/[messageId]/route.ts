import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {deleteDashboardSuggestion, isDashboardApiError, moderateDashboardSuggestion} from "@/lib/dashboard-api";
import {isManageableGuild} from "@/lib/discord";

function parsePositiveInt(value: string | null, fallback: number) {
    if (!value || value.trim().length === 0) {
        return fallback;
    }

    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export async function PATCH(request: NextRequest, {params}: {params: Promise<{guildId: string; messageId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to update suggestions."
        }, {status: 401});
    }

    const {guildId, messageId} = await params;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild || !isManageableGuild(guild.owner, guild.permissions)) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    const payload = await request.json();
    const page = parsePositiveInt(request.nextUrl.searchParams.get("page"), 1);
    const pageSize = parsePositiveInt(request.nextUrl.searchParams.get("pageSize"), 10);

    try {
        return NextResponse.json(await moderateDashboardSuggestion(guildId, messageId, payload.action, {
            actorUserId: session.user.id,
            reason: payload.reason ?? "Unspecified"
        }, page, pageSize));
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
            message: "Failed to update suggestion."
        }, {status: 500});
    }
}

export async function DELETE(request: NextRequest, {params}: {params: Promise<{guildId: string; messageId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to delete suggestions."
        }, {status: 401});
    }

    const {guildId, messageId} = await params;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild || !isManageableGuild(guild.owner, guild.permissions)) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    const page = parsePositiveInt(request.nextUrl.searchParams.get("page"), 1);
    const pageSize = parsePositiveInt(request.nextUrl.searchParams.get("pageSize"), 10);

    try {
        return NextResponse.json(await deleteDashboardSuggestion(guildId, messageId, {
            actorUserId: session.user.id,
            reason: "Deleted from dashboard"
        }, page, pageSize));
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
            message: "Failed to delete suggestion."
        }, {status: 500});
    }
}
