import CollectablesSection from "@/components/CollectablesSection";
import BirthdaySection from "@/components/BirthdaySection";
import WordleProfileSection from "@/components/WordleProfileSection";
import EconomySection from "@/components/EconomySection";
import {requireCurrentSession} from "@/lib/auth";
import {fetchDashboardUserProfile, isDashboardApiError} from "@/lib/dashboard-api";

export default async function UserProfile({params}: { params: Promise<{ userId: string }> }) {
    await requireCurrentSession();
    const userId = (await params).userId;
    const profile = await fetchDashboardUserProfile(userId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    return <div className="w-full h-full flex flex-col items-center justify-center p-5">
        <h1 className="text-4xl font-bold">User Profile</h1>
        <p className="text-lg">User ID: {userId}</p>
        {!profile ? <section className="w-full bg-red-500/10 border border-red-500/30 rounded-md p-5 text-red-100 mt-5">
            The dashboard API could not load this user profile. Check that the Java dashboard service is running and
            that <code className="rounded bg-slate-900/80 px-1.5 py-0.5 font-mono text-slate-100">DASHBOARD_API_URL</code> and{" "}
            <code className="rounded bg-slate-900/80 px-1.5 py-0.5 font-mono text-slate-100">DASHBOARD_API_KEY</code> match.
        </section> : <div className="w-full flex flex-col items-center justify-center space-y-4">
            <BirthdaySection birthday={profile.birthday} />
            <WordleProfileSection wordle={profile.wordle} />
            <CollectablesSection collectablesProfile={profile.collectables} />
            <EconomySection economy={profile.economy} />
        </div>
        }
    </div>
}
