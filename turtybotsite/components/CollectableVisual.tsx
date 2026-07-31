"use client";

import Image from "next/image";
import CollectableEmoji from "@/components/CollectableEmoji";
import type {DashboardCollectableItem} from "@/lib/dashboard-api";

interface CollectableVisualProps {
    guildId: string;
    collectionType: string;
    presentation: "emoji" | "image";
    collectable: DashboardCollectableItem;
    large?: boolean;
}

export default function CollectableVisual({
    guildId,
    collectionType,
    presentation,
    collectable,
    large = false
}: CollectableVisualProps) {
    if (presentation === "image") {
        const size = large ? 64 : 40;
        return <span className={`inline-flex shrink-0 items-center justify-center overflow-hidden bg-slate-900 ${
            large ? "h-16 w-16" : "h-10 w-10"
        }`}>
            <Image
                src={`/api/dashboard/guilds/${encodeURIComponent(guildId)}/collectables/${encodeURIComponent(collectionType)}/${encodeURIComponent(collectable.name)}/image`}
                alt={collectable.richName}
                width={size}
                height={size}
                unoptimized
                className="h-full w-full object-cover"
            />
        </span>;
    }

    return <CollectableEmoji emoji={collectable.emoji ?? "❓"} label={collectable.richName} />;
}
