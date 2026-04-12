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

async function proxyNotifierMutation(guildId: string, type: string, method: "POST" | "PUT" | "DELETE", body: unknown) {
    const apiUrl = new URL(`/api/guilds/${guildId}/notifiers/${encodeURIComponent(type)}`, getRequiredEnv("DASHBOARD_API_URL").replace(/\/?$/, "/"));
    const response = await fetch(apiUrl, {
        method,
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "X-TurtyBot-Api-Key": getRequiredEnv("DASHBOARD_API_KEY")
        },
        body: JSON.stringify(body),
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

async function handleMutation(
    request: NextRequest,
    method: "POST" | "PUT" | "DELETE",
    params: Promise<{guildId: string; type: string}>,
    unauthorizedMessage: string,
) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({
            message: unauthorizedMessage
        }, {status: 401});
    }

    const {guildId, type} = await params;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        return NextResponse.json({
            message: "You do not have access to that guild."
        }, {status: 403});
    }

    const body = await request.json().catch(() => ({}));

    try {
        return NextResponse.json(await proxyNotifierMutation(guildId, type, method, body));
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
            message: "Failed to mutate notifier."
        }, {status: 500});
    }
}

export async function POST(request: NextRequest, {params}: {params: Promise<{guildId: string; type: string}>}) {
    return handleMutation(request, "POST", params, "You must be signed in to create notifiers.");
}

export async function PUT(request: NextRequest, {params}: {params: Promise<{guildId: string; type: string}>}) {
    return handleMutation(request, "PUT", params, "You must be signed in to edit notifiers.");
}

export async function DELETE(request: NextRequest, {params}: {params: Promise<{guildId: string; type: string}>}) {
    return handleMutation(request, "DELETE", params, "You must be signed in to remove notifiers.");
}
