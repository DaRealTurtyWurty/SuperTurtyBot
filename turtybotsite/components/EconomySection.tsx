import type {DashboardEconomyEntry} from "@/lib/dashboard-api";

function getShortenedMoneyString(money: string): string {
    const suffixes = ["", "K", "M", "B", "T", "q", "Q"];
    let suffixIndex = 0;
    let shortenedMoney = BigInt(money);
    while (shortenedMoney >= BigInt(1000) && suffixIndex < suffixes.length - 1) {
        shortenedMoney /= BigInt(1000);
        suffixIndex++;
    }

    return `${shortenedMoney.toString()}${suffixes[suffixIndex]}`;
}

function convertMoneyToString(money: string) {
    const normalized = BigInt(money).toString();
    return normalized.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

export default function EconomySection({economy}: { economy: DashboardEconomyEntry[] }) {
    if (economy.length === 0) {
        return null;
    }

    return <section className="w-full flex flex-col items-center justify-center space-y-4">
        <p className="text-2xl font-bold mt-5">Economy</p>
        <ul className="w-full bg-slate-800 p-5 rounded-md text-white space-y-2">
            {economy.map((entry) => {
                return <li
                    key={entry.guildId}
                    className="w-full flex flex-col justify-center space-y-2 p-4 border border-gray-300 rounded-md">
                    <h2 className="text-2xl font-bold">Economy data for server: {entry.guildId}</h2>
                    <p className="text-lg">Bank: {entry.currency}{convertMoneyToString(entry.bank)} ({entry.currency}{getShortenedMoneyString(entry.bank)})</p>
                    <p className="text-lg">Wallet: {entry.currency}{convertMoneyToString(entry.wallet)} ({entry.currency}{getShortenedMoneyString(entry.wallet)})</p>
                    <p className="text-lg">Crime Level: {entry.crimeLevel}</p>
                    <p className="text-lg">Heist Level: {entry.heistLevel}</p>
                    <p className="text-lg">Job: {entry.job ?? "None"}</p>
                    <p className="text-lg">Job Level: {entry.jobLevel}</p>
                </li>;
            })}
        </ul>
    </section>;
}
