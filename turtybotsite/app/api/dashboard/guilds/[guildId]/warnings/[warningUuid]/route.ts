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

async function proxyDeleteWarning(guildId: string, warningUuid: string) {
    const apiUrl = new URL(`/api/guilds/${guildId}/warnings/${warningUuid}`, getRequiredEnv("DASHBOARD_API_URL").replace(/\/?$/, "/"));
    const response = await fetch(apiUrl, {
        method: "DELETE",
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

export async function DELETE(_: NextRequest, {params}: {params: Promise<{guildId: string; warningUuid: string}>}) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: "You must be signed in to update warnings settings."
        }, {status: 401});
    }

    const {guildId, warningUuid} = await params;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    try {
        return NextResponse.json(await proxyDeleteWarning(guildId, warningUuid));
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
            message: "Failed to remove warning."
        }, {status: 500});
    }
}
