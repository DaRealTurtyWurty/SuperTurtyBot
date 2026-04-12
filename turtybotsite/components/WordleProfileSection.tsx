import type {DashboardWordleProfile} from "@/lib/dashboard-api";

export default function WordleProfileSection({wordle}: { wordle: DashboardWordleProfile | null }) {
    if (!wordle || wordle.streaks.length === 0) {
        return null;
    }

    return <section className="w-full flex flex-col items-center justify-center space-y-4">
        <p className="text-2xl font-bold mt-5">Wordle</p>
        <ul className="w-full bg-slate-800 p-5 rounded-md text-white space-y-2">
            <li className="space-y-2">
                <p className="text-xl font-bold text-blue-400">
                    Wordle
                </p>
                <div className="text-lg w-full flex flex-col space-y-4 bg-slate-700 p-5 rounded-md">
                    {wordle.streaks.map((streak, index) =>
                        <div key={index}>
                            <h2 className="text-xl font-bold">
                                {streak.guildId !== "0" ? `Guild ID: ${streak.guildId}` : "Global"}
                            </h2>
                            <p>Best Streak: {streak.bestStreak}</p>
                            <p>Current Streak: {streak.streak}</p>
                            <p>Has Played Today: {streak.hasPlayedToday ? "Yes" : "No"}</p>
                        </div>
                    )}
                </div>
            </li>
        </ul>
    </section>;
}
