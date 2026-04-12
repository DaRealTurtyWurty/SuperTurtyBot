import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";

function getRequiredEnv(name: string) {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Missing required environment variable: ${name}`);
    }

    return value;
}

async function proxyModmailTicket(guildId: string, ticketNumber: string) {
    const apiUrl = new URL(`/api/guilds/${guildId}/modmail/tickets/${ticketNumber}`, getRequiredEnv("DASHBOARD_API_URL").replace(/\/?$/, "/"));
    const response = await fetch(apiUrl, {
        method: "GET",
        headers: {
            "Accept": "application/json",
            "X-TurtyBot-Api-Key": getRequiredEnv("DASHBOARD_API_KEY")
        },
        cache: "no-store"
    });

    if (!response.ok) {
        const payload = await response.json().catch(() => null) as {message?: string} | null;
        throw new Error(payload?.message ?? `Dashboard API request failed with status ${response.status}.`);
    }

    return response.json();
}

export async function GET(_: NextRequest, {params}: {params: Promise<{guildId: string; ticketNumber: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to view modmail tickets."
        }, {status: 401});
    }

    const {guildId, ticketNumber} = await params;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    try {
        return NextResponse.json(await proxyModmailTicket(guildId, ticketNumber));
    } catch (error) {
        if (error instanceof Error) {
            return NextResponse.json({
                message: error.message
            }, {status: 500});
        }

        return NextResponse.json({
            message: "Failed to load modmail ticket."
        }, {status: 500});
    }
}
