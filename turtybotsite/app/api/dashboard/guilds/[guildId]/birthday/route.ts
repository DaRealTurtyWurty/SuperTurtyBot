import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {isDashboardApiError, updateDashboardBirthdaySettings} from "@/lib/dashboard-api";

export async function PUT(request: NextRequest, {params}: {params: Promise<{guildId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to update birthday settings."
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
        const updated = await updateDashboardBirthdaySettings(guildId, payload);
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
            message: "Failed to update birthday settings."
        }, {status: 500});
    }
}
