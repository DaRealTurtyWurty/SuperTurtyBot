import Image from "next/image";
import type {NotifierIconKey} from "@/lib/notifiers";

const ICON_PATHS: Record<NotifierIconKey, string> = {
    youtube: "/logos/youtube.webp",
    twitch: "/logos/twitch.png",
    reddit: "/logos/reddit.jpeg",
    steam: "/logos/steam.png",
    store: "/logos/steam.png",
    minecraft: "/logos/minecraft.png",
    siege: "/logos/rainbow-six-siege.png",
    "rocket-league": "/logos/rocket-league.png",
    league: "/logos/league-of-legends.png",
    valorant: "/logos/valorant.png"
};

export default function NotifierIcon({
    icon,
    className = "h-4 w-4"
}: {
    icon: NotifierIconKey;
    className?: string;
}) {
    const src = ICON_PATHS[icon];

    return <Image
        src={src}
        alt=""
        aria-hidden="true"
        width={16}
        height={16}
        className={`${className} object-contain`}
    />;
}
