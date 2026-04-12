"use client";
import React, { useState } from "react";
import Link from "next/link";

export default function UserIdInput() {
    const [userId, setUserId] = useState("");
    return <div className="flex flex-col items-center justify-center space-y-4">
        <input
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            onChange={e => {
                let value = e.target.value.replace(/[^0-9]/g, "");
                value = value.substring(0, Math.min(value.length, 18));
                setUserId(value);
            }}
            value={userId}
            onKeyPress={(e: React.KeyboardEvent<HTMLInputElement>) => {
                const key = e.key;
                if (key < "0" || key > "9") {
                    e.preventDefault();
                }
            }}
            placeholder="User ID"
            className="border border-gray-300 rounded-md p-2 text-lg text-gray-700 text-center focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
        />
        <Link href={`/user/${userId}`}>
            <button
                className="mt-2 bg-blue-500 text-white rounded-md p-2">
                View Profile
            </button>
        </Link>
    </div>
}