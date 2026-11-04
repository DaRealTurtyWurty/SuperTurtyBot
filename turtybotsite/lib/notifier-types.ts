export interface DashboardNotifierEntry {
    type: string;
    kind: string;
    targetLabel: string;
    targetValue: string | null;
    channelId: string;
    channelName: string | null;
    mention: string;
    details: string[];
    diagnostics: {
        lastCheckAt: number | null;
        lastSuccessfulCheckAt: number | null;
        checkError: string | null;
        checkErrorAt: number | null;
        lastDeliveredAt: number | null;
        lastDeliveredItem: string | null;
        deliveryError: string | null;
        deliveryErrorAt: number | null;
        lastTestAt: number | null;
        testError: string | null;
        channelProblem: string | null;
        eventDriven: boolean;
    };
}

export interface DashboardNotifierSection {
    key: string;
    title: string;
    description: string;
    count: number;
    entries: DashboardNotifierEntry[];
}

export interface DashboardNotifiersResponse {
    guild: {
        id: string;
        name: string;
        iconUrl: string | null;
        memberCount: number;
        connected: boolean;
    };
    totalCount: number;
    sections: DashboardNotifierSection[];
}

export interface DashboardNotifierMutationRequest {
    originalTarget?: string | null;
    target: string | null;
    discordChannelId: string | null;
    mention: string | null;
}
