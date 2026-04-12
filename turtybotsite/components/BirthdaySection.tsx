import type {DashboardBirthday} from "@/lib/dashboard-api";

export default function BirthdaySection({birthday}: { birthday: DashboardBirthday | null }) {
    if (!birthday) {
        return null;
    }

    return <section className="w-full flex flex-col items-center justify-center space-y-4">
        <p className="text-2xl font-bold mt-5">Birthday</p>
        <ul className="w-full bg-slate-800 p-5 rounded-md text-white space-y-2">
            <li className="space-y-2">
                <p className="text-xl font-bold text-blue-400">
                    Birthday
                </p>
                <div className="text-lg">
                    <p>EU {birthday.day}/{birthday.month}/{birthday.year}</p>
                    <p>US {birthday.month}/{birthday.day}/{birthday.year}</p>
                    <p>JP {birthday.year}/{birthday.month}/{birthday.day}</p>
                </div>
            </li>
        </ul>
    </section>;
}
