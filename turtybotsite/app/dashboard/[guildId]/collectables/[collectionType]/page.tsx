import Link from "next/link";
import {notFound} from "next/navigation";
import CollectablesSettingsForm from "@/components/CollectablesSettingsForm";
import {fetchDashboardCollectablesSettings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function CollectablesCollectionPage({
    params
}: {
    params: Promise<{ guildId: string; collectionType: string }>;
}) {
    const {guildId, collectionType} = await params;
    const settings = await fetchDashboardCollectablesSettings(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!settings) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Collectables settings could not be loaded from the dashboard API.
        </div>;
    }

    const collection = settings.collections.find(entry => entry.type === collectionType);
    if (!collection) {
        notFound();
    }

    return <div className="space-y-6">
        <div className="space-y-3">
            <Link href={`/dashboard/${guildId}/collectables`} className="text-sm font-semibold text-sky-300 hover:text-sky-200">
                Back to collectables overview
            </Link>
            <div>
                <h2 className="text-3xl font-bold tracking-tight">{collection.displayName}</h2>
                <p className="mt-2 max-w-3xl text-sm text-slate-400">
                    Enable or disable individual collectables for this collection.
                </p>
            </div>
        </div>

        <CollectablesSettingsForm guildId={guildId} initialSettings={settings} collectionType={collectionType} />
    </div>;
}
