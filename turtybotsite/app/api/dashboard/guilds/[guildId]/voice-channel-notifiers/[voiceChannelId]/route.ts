import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {deleteDashboardVoiceChannelNotifier, isDashboardApiError} from "@/lib/dashboard-api";

export async function DELETE(_: NextRequest, {params}: {params: Promise<{guildId: string; voiceChannelId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to update voice channel notifiers."
        }, {status: 401});
    }

    const {guildId, voiceChannelId} = await params;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    try {
        return NextResponse.json(await deleteDashboardVoiceChannelNotifier(guildId, voiceChannelId));
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
            message: "Failed to delete voice channel notifier."
        }, {status: 500});
    }
}
