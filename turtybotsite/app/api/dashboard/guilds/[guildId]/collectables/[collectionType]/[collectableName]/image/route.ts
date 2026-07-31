import {NextResponse} from "next/server";
import {getCurrentSession} from "@/lib/auth";
import {fetchDashboardCollectableImage, isDashboardApiError} from "@/lib/dashboard-api";

export async function GET(
    _request: Request,
    {params}: {params: Promise<{guildId: string; collectionType: string; collectableName: string}>}
) {
    const session = await getCurrentSession();
    if (!session) {
        return NextResponse.json({message: "You must be signed in to view collectable images."}, {status: 401});
    }

    const {guildId, collectionType, collectableName} = await params;
    if (!session.guilds.some(entry => entry.id === guildId)) {
        return NextResponse.json({message: "You do not have access to that guild."}, {status: 403});
    }

    try {
        const response = await fetchDashboardCollectableImage(collectionType, collectableName);
        if (!response.ok) {
            return NextResponse.json({message: "Collectable image not found."}, {status: response.status});
        }

        return new NextResponse(response.body, {
            status: 200,
            headers: {
                "Content-Type": response.headers.get("Content-Type") ?? "application/octet-stream",
                "Cache-Control": "private, max-age=86400"
            }
        });
    } catch (error) {
        if (isDashboardApiError(error)) {
            return NextResponse.json({message: error.message}, {status: error.status});
        }
        return NextResponse.json({message: "Failed to load the collectable image."}, {status: 500});
    }
}
