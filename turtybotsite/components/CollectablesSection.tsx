import mobCollectables from "@/minecraft_mob_collectables.json";
import Image from "next/image";
import type {DashboardCollectablesProfile} from "@/lib/dashboard-api";

interface Rarity {
    name: string;
    color: string;
}

class Rarities {
    static COMMON: Rarity = {name: "COMMON", color: "#888888"};
    static UNCOMMON: Rarity = {name: "UNCOMMON", color: "#00CC00"};
    static RARE: Rarity = {name: "RARE", color: "#0000DD"};
    static EPIC: Rarity = {name: "EPIC", color: "#DD00DD"};
    static LEGENDARY: Rarity = {name: "LEGENDARY", color: "#EEEE00"};
    static MYTHICAL: Rarity = {name: "MYTHICAL", color: "#FFAA00"};
    static SPECIAL: Rarity = {name: "SPECIAL", color: "#EE0000"};

    static rarities: Rarity[] = [
        Rarities.COMMON,
        Rarities.UNCOMMON,
        Rarities.RARE,
        Rarities.EPIC,
        Rarities.LEGENDARY,
        Rarities.MYTHICAL,
        Rarities.SPECIAL
    ];

    static byName(name: string | undefined): Rarity | undefined {
        return Rarities.rarities.find((rarity) => rarity.name === name);
    }
}

export default function CollectablesSection({collectablesProfile}: { collectablesProfile: DashboardCollectablesProfile | null }) {
    if (!collectablesProfile || collectablesProfile.collectables.length === 0) {
        return null;
    }

    return <section className="w-full flex flex-col items-center justify-center space-y-4">
        <p className="text-2xl font-bold mt-5">Collectables</p>
        <ul className="w-full bg-slate-800 p-5 rounded-md text-white space-y-2">
            {collectablesProfile.collectables.map((collection, index) => {
                return <li key={index} className="space-y-2">
                    <p className="text-xl font-bold text-blue-400">
                        {collection.type.split("_").map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(" ")}
                    </p>
                    <ul className="flex flex-row flex-wrap gap-2 justify-center">
                        {collection.collectables
                            .map(collectable => mobCollectables.find((datum) => datum.name === collectable))
                            .map(collectable => ({
                                name: collectable?.name || "Unknown",
                                emojiId: collectable?.emojiId || "0",
                                rarity: Rarities.byName(collectable?.rarity) || Rarities.COMMON
                            }))
                            .sort((a, b) => {
                                const rarityAIndex = Rarities.rarities.indexOf(a.rarity);
                                const rarityBIndex = Rarities.rarities.indexOf(b.rarity);
                                return rarityBIndex - rarityAIndex;
                            })
                            .map((collectable, collectableIndex) => {
                                const color = collectable.rarity.color;
                                return <li key={collectableIndex} className="w-[128px] h-[128px]">
                                    <div
                                        className="flex flex-col items-center justify-center w-[128px] h-[128px] rounded-md p-2 bg-slate-700 border-2"
                                        style={{borderColor: color}}>
                                        <p className="text-lg text-center">
                                            {collectable.name.split("_").map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(" ")}
                                        </p>
                                        <Image
                                            src={`https://cdn.discordapp.com/emojis/${collectable.emojiId}.webp`}
                                            width={64}
                                            height={64}
                                            alt={collectable.name}
                                        />
                                    </div>
                                </li>;
                            })}
                    </ul>
                </li>;
            })}
        </ul>
    </section>;
}
