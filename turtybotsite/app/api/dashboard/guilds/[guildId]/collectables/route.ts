import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {
    fetchDashboardCollectablesPage,
    isDashboardApiError,
    updateDashboardCollectablesSettings
} from "@/lib/dashboard-api";

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
        return NextResponse.json({message: "You must be signed in to view collectables."}, {status: 401});
    }

    const guildId = (await params).guildId;
    if (!session.guilds.some(entry => entry.id === guildId)) {
        return NextResponse.json({message: "You do not have access to that guild."}, {status: 403});
    }

    const collectionType = request.nextUrl.searchParams.get("collectionType");
    if (!collectionType) {
        return NextResponse.json({message: "A collection type is required."}, {status: 400});
    }

    try {
        const result = await fetchDashboardCollectablesPage(
            guildId,
            collectionType,
            parsePositiveInt(request.nextUrl.searchParams.get("page"), 1),
            parsePositiveInt(request.nextUrl.searchParams.get("pageSize"), 60),
            request.nextUrl.searchParams.get("query") ?? ""
        );
        return NextResponse.json(result);
    } catch (error) {
        if (isDashboardApiError(error)) {
            return NextResponse.json({message: error.message}, {status: error.status});
        }
        return NextResponse.json({message: error instanceof Error ? error.message : "Failed to load collectables."}, {status: 500});
    }
}

export async function PUT(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to update collectables settings."
        }, {status: 401});
    }

    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    const payload = await request.json();

    try {
        const updated = await updateDashboardCollectablesSettings(guildId, payload);
        return NextResponse.json(updated);
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
            message: "Failed to update collectables settings."
        }, {status: 500});
    }
}
