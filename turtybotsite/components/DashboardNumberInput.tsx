"use client";

import {FaChevronDown, FaChevronUp} from "react-icons/fa6";

interface DashboardNumberInputProps {
    value: string | number;
    onChange: (value: string) => void;
    disabled?: boolean;
    min?: number;
    max?: number;
    step?: number;
    placeholder?: string;
    className?: string;
}

function clamp(value: number, min?: number, max?: number) {
    let next = value;

    if (typeof min === "number") {
        next = Math.max(next, min);
    }

    if (typeof max === "number") {
        next = Math.min(next, max);
    }

    return next;
}

function getPrecision(value: number) {
    const valueText = value.toString().toLowerCase();
    if (valueText.includes("e-")) {
        const [base, exponentText] = valueText.split("e-");
        const exponent = Number(exponentText);
        const fractional = base.includes(".") ? base.split(".")[1].length : 0;
        return fractional + exponent;
    }

    const fractional = valueText.split(".")[1];
    return fractional ? fractional.length : 0;
}

function formatNumber(value: number, precision: number) {
    if (precision <= 0) {
        return String(Math.round(value));
    }

    return value.toFixed(precision).replace(/\.0+$/, "").replace(/(\.\d*?)0+$/, "$1");
}

export default function DashboardNumberInput({
    value,
    onChange,
    disabled,
    min,
    max,
    step = 1,
    placeholder,
    className = ""
}: DashboardNumberInputProps) {
    function updateByDelta(delta: number) {
        const current = typeof value === "number" ? value : Number.parseFloat(value);
        const fallback = typeof min === "number" ? min : 0;
        const precision = Math.max(getPrecision(step), typeof value === "number" ? getPrecision(value) : getPrecision(Number.isFinite(current) ? current : fallback), typeof min === "number" ? getPrecision(min) : 0, typeof max === "number" ? getPrecision(max) : 0);
        const scale = 10 ** precision;
        const stepUnits = Math.round(step * scale);
        const baseValue = Number.isFinite(current) ? current : fallback;
        const nextUnits = Math.round(baseValue * scale) + (delta * stepUnits);
        const minUnits = typeof min === "number" ? Math.round(min * scale) : undefined;
        const maxUnits = typeof max === "number" ? Math.round(max * scale) : undefined;
        const clampedUnits = clamp(nextUnits, minUnits, maxUnits);
        onChange(formatNumber(clampedUnits / scale, precision));
    }

    return <div className={`relative mt-4 h-12 ${className}`}>
        <input
            type="number"
            min={min}
            max={max}
            step={step}
            value={value}
            onChange={event => onChange(event.target.value)}
            disabled={disabled}
            placeholder={placeholder}
            className="dashboard-number-input h-full w-full border border-slate-700 bg-slate-900 px-4 pr-12 text-sm text-white outline-none transition focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
        />
        <div className="absolute inset-y-0 right-0 flex h-full w-9 flex-col border-l border-slate-700 bg-slate-900">
            <button
                type="button"
                onClick={() => updateByDelta(1)}
                disabled={disabled}
                className="flex h-1/2 items-center justify-center border-b border-slate-700 text-slate-300 transition hover:bg-slate-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
                aria-label="Increase value"
            >
                <FaChevronUp className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
            <button
                type="button"
                onClick={() => updateByDelta(-1)}
                disabled={disabled}
                className="flex h-1/2 items-center justify-center text-slate-300 transition hover:bg-slate-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
                aria-label="Decrease value"
            >
                <FaChevronDown className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
        </div>
    </div>;
}
