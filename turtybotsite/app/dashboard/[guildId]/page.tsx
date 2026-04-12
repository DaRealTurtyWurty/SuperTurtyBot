import {fetchDashboardGuildConfig, isDashboardApiError} from "@/lib/dashboard-api";

function readBoolean(config: Record<string, unknown> | undefined, key: string) {
    const value = config?.[key];
    return typeof value === "boolean" ? value : undefined;
}

function readText(config: Record<string, unknown> | undefined, key: string) {
    const value = config?.[key];
    return typeof value === "string" || typeof value === "number" ? value.toString() : undefined;
}

function formatStatus(value: boolean | undefined) {
    return value ? "Enabled" : "Disabled";
}

export default async function GuildOverviewPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
    const snapshot = await fetchDashboardGuildConfig(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!snapshot) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            The guild overview could not be loaded from the dashboard API.
        </div>;
    }

    const config = snapshot.config;
    const showcaseChannels = readText(config, "showcase_channels");
    const showcaseChannelCount = showcaseChannels ? showcaseChannels.split(/[ ;]+/).filter(Boolean).length : 0;

    return <div className="space-y-6">
        <div>
            <h2 className="text-3xl font-bold tracking-tight">Overview</h2>
        </div>

        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <div className="border border-slate-800/80 bg-slate-950/60 p-5">
                <p className="text-sm text-slate-500">Bot status</p>
                <p className="mt-3 text-2xl font-semibold text-white">{snapshot.guild.connected ? "In Server" : "Not Connected"}</p>
            </div>
            <div className="border border-slate-800/80 bg-slate-950/60 p-5">
                <p className="text-sm text-slate-500">Config record</p>
                <p className="mt-3 text-2xl font-semibold text-white">{snapshot.persisted ? "Saved" : "Defaulted"}</p>
            </div>
            <div className="border border-slate-800/80 bg-slate-950/60 p-5">
                <p className="text-sm text-slate-500">Starboard</p>
                <p className="mt-3 text-2xl font-semibold text-white">{formatStatus(readBoolean(config, "starboard_enabled"))}</p>
            </div>
            <div className="border border-slate-800/80 bg-slate-950/60 p-5">
                <p className="text-sm text-slate-500">Members</p>
                <p className="mt-3 text-2xl font-semibold text-white">{snapshot.guild.memberCount.toLocaleString()}</p>
            </div>
        </div>

        <div className="grid gap-4 lg:grid-cols-2">
            <div className="border border-slate-800/80 bg-slate-950/60 p-6">
                <h3 className="text-lg font-semibold text-white">Starboard</h3>
                <div className="mt-4 space-y-3 text-sm text-slate-300">
                    <p>Channel: {readText(config, "starboard") ?? "Not set"}</p>
                    <p>Minimum Stars: {readText(config, "minimum_stars") ?? "Unknown"}</p>
                    <p>Bot Stars Count: {formatStatus(readBoolean(config, "bot_stars_count"))}</p>
                    <p>Media Only: {formatStatus(readBoolean(config, "is_starboard_media_only"))}</p>
                    <p>Showcase Channels: {showcaseChannelCount}</p>
                </div>
            </div>

            <div className="border border-slate-800/80 bg-slate-950/60 p-6">
                <h3 className="text-lg font-semibold text-white">Systems</h3>
                <div className="mt-4 grid gap-3 sm:grid-cols-2 text-sm text-slate-300">
                    <p>Levelling: {formatStatus(readBoolean(config, "levelling_enabled"))}</p>
                    <p>Economy: {formatStatus(readBoolean(config, "economy_enabled"))}</p>
                    <p>AI: {formatStatus(readBoolean(config, "ai_enabled"))}</p>
                    <p>Chat Revival: {formatStatus(readBoolean(config, "chat_revival_enabled"))}</p>
                    <p>Threads: {formatStatus(readBoolean(config, "should_moderators_join_threads"))}</p>
                    <p>Misc: {formatStatus(readBoolean(config, "should_create_gists"))}</p>
                    <p>NSFW Filter: {formatStatus(readBoolean(config, "artist_nsfw_filter_enabled"))}</p>
                    <p>Sticky Roles: {formatStatus(readBoolean(config, "sticky_roles_enabled"))}</p>
                    <p>Invite Guard: {formatStatus(readBoolean(config, "discord_invite_guard_enabled"))}</p>
                </div>
            </div>
        </div>
    </div>;
}
