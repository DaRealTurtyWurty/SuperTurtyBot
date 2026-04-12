import {NextRequest, NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";

function getRequiredEnv(name: string) {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Missing required environment variable: ${name}`);
    }

    return value;
}

class DashboardProxyError extends Error {
    constructor(message: string, readonly status: number) {
        super(message);
        this.name = "DashboardProxyError";
    }
}

async function proxyReportHistory(guildId: string, userId: string) {
    const apiUrl = new URL(`/api/guilds/${guildId}/reports/${userId}`, getRequiredEnv("DASHBOARD_API_URL").replace(/\/?$/, "/"));
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
        throw new DashboardProxyError(
            payload?.message ?? `Dashboard API request failed with status ${response.status}.`,
            response.status
        );
    }

    return response.json();
}

export async function GET(_: NextRequest, {params}: {params: Promise<{guildId: string; userId: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to view reports."
        }, {status: 401});
    }

    const {guildId, userId} = await params;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    try {
        return NextResponse.json(await proxyReportHistory(guildId, userId));
    } catch (error) {
        if (error instanceof DashboardProxyError) {
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
            message: "Failed to load reports."
        }, {status: 500});
    }
}
