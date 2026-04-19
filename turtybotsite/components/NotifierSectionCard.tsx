import type {ReactNode} from "react";

export default function NotifierSectionCard({
    id,
    title,
    description,
    count,
    children
}: {
    id?: string;
    title: string;
    description: string;
    count: number;
    children: ReactNode;
}) {
    return <section id={id} className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
        <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
                <h3 className="text-xl font-semibold text-white">{title}</h3>
                <p className="mt-1 text-sm text-slate-400">{description}</p>
            </div>
            <span className="border border-slate-700 bg-slate-900 px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-300">
                {count}
            </span>
        </div>

        <div className="mt-4">
            {children}
        </div>
    </section>;
}
