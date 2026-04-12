import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {createDashboardTag, deleteDashboardTag, fetchDashboardTags, isDashboardApiError} from "@/lib/dashboard-api";
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
            message: "You must be signed in to view tags."
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
        return NextResponse.json(await fetchDashboardTags(guildId, page, pageSize));
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
            message: "Failed to load tags."
        }, {status: 500});
    }
}

export async function DELETE(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to delete tags."
        }, {status: 401});
    }

    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild || !isManageableGuild(guild.owner, guild.permissions)) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    const name = request.nextUrl.searchParams.get("name");
    if (!name || name.trim().length === 0) {
        return NextResponse.json({
            message: "The tag name was missing."
        }, {status: 400});
    }

    const page = parsePositiveInt(request.nextUrl.searchParams.get("page"), 1);
    const pageSize = parsePositiveInt(request.nextUrl.searchParams.get("pageSize"), 10);

    try {
        return NextResponse.json(await deleteDashboardTag(guildId, name, page, pageSize));
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
            message: "Failed to delete tag."
        }, {status: 500});
    }
}

export async function POST(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to create tags."
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

    let payload: {name?: unknown; content?: unknown; contentType?: unknown} | null = null;
    try {
        payload = await request.json();
    } catch {
        payload = null;
    }

    if (!payload || typeof payload.name !== "string" || typeof payload.content !== "string" || typeof payload.contentType !== "string") {
        return NextResponse.json({
            message: "The tag payload was missing or invalid."
        }, {status: 400});
    }

    try {
        return NextResponse.json(await createDashboardTag(guildId, {
            name: payload.name,
            content: payload.content,
            contentType: payload.contentType === "embed" ? "embed" : "message",
            actorUserId: session.user.id
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
            message: "Failed to create tag."
        }, {status: 500});
    }
}
