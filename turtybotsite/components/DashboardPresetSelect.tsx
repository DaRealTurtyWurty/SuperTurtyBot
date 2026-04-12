"use client";

import {useEffect, useMemo, useRef, useState} from "react";
import {FaChevronDown} from "react-icons/fa6";

interface DashboardPresetSelectOption {
    label: string;
    value: string;
}

interface DashboardPresetSelectProps {
    label: string;
    description?: string;
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    options: DashboardPresetSelectOption[];
    customValue?: string;
    onCustomValueChange?: (value: string) => void;
    customPlaceholder?: string;
    allowCustom?: boolean;
}

function getSummaryLabel(value: string, options: DashboardPresetSelectOption[]) {
    return options.find(option => option.value === value)?.label ?? "Custom";
}

export default function DashboardPresetSelect({
    label,
    description,
    value,
    onChange,
    disabled,
    options,
    customValue = "",
    onCustomValueChange,
    customPlaceholder = "Custom value",
    allowCustom = true
}: DashboardPresetSelectProps) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement | null>(null);
    const summary = useMemo(() => getSummaryLabel(value, options), [options, value]);

    useEffect(() => {
        function onDocumentClick(event: MouseEvent) {
            if (!containerRef.current?.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }

        if (isOpen) {
            document.addEventListener("mousedown", onDocumentClick);
        }

        return () => {
            document.removeEventListener("mousedown", onDocumentClick);
        };
    }, [isOpen]);

    return <div ref={containerRef} className="block border border-slate-800/80 bg-slate-950/60 p-5">
        <p className="text-sm font-semibold text-white">{label}</p>
        {description ? <p className="mt-1 text-sm text-slate-400">{description}</p> : null}

        <div className="relative mt-4">
            <button
                type="button"
                onClick={() => setIsOpen(current => !current)}
                disabled={disabled}
                className="flex min-h-12 w-full items-center justify-between gap-3 border border-slate-700 bg-slate-900 px-4 py-3 text-left text-sm text-white outline-none transition hover:border-slate-600 focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
                <span className="truncate">
                    {summary}
                </span>
                <FaChevronDown className={`h-4 w-4 shrink-0 text-slate-400 transition ${isOpen ? "rotate-180" : ""}`} aria-hidden="true" />
            </button>

            {isOpen ? <div className="dashboard-scrollbar absolute left-0 right-0 top-full z-30 mt-1 max-h-96 overflow-y-auto border border-slate-700 bg-slate-900 shadow-2xl">
                <div className="p-2">
                    {options.map(option => {
                        const isSelected = option.value === value;

                        return <button
                            key={option.value}
                            type="button"
                            onClick={() => {
                                onChange(option.value);
                                setIsOpen(false);
                            }}
                            className={`flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm transition ${
                                isSelected
                                    ? "bg-sky-400/12 text-sky-100"
                                    : "text-slate-200 hover:bg-slate-800 hover:text-white"
                            }`}
                        >
                            <span className="truncate">{option.label}</span>
                            {isSelected ? <span className="text-xs font-semibold uppercase tracking-[0.14em] text-sky-300">Selected</span> : null}
                        </button>;
                    })}

                    {allowCustom ? <button
                        type="button"
                        onClick={() => {
                            onChange("__custom__");
                            setIsOpen(false);
                        }}
                        className={`mt-2 flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm transition ${
                            value === "__custom__"
                                ? "bg-sky-400/12 text-sky-100"
                                : "text-slate-200 hover:bg-slate-800 hover:text-white"
                        }`}
                    >
                        <span className="truncate">Custom</span>
                        {value === "__custom__" ? <span className="text-xs font-semibold uppercase tracking-[0.14em] text-sky-300">Selected</span> : null}
                    </button> : null}
                </div>
            </div> : null}
        </div>

        {value === "__custom__" ? <input
            value={customValue}
            onChange={event => onCustomValueChange?.(event.target.value)}
            disabled={disabled}
            placeholder={customPlaceholder}
            className="mt-3 w-full border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
        /> : null}
    </div>;
}
