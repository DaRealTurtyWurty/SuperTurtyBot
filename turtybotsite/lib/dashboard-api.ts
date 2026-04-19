import "server-only";

export interface DashboardHealthResponse {
    status: string;
    environment: string;
    botStatus: string;
    startedAt: number;
    configOptionCount: number;
    publicUrl: string | null;
}

export interface DashboardSessionUserSummary {
    id: string;
    username: string;
    globalName: string | null;
    avatar: string | null;
}

export interface DashboardSessionGuildSummary {
    id: string;
    name: string;
    icon: string | null;
    owner: boolean;
    permissions: string;
}

export interface DashboardSessionRecord {
    sessionId: string;
    user: DashboardSessionUserSummary;
    guilds: DashboardSessionGuildSummary[];
    createdAtMs: number;
    expiresAtMs: number;
}

export interface DashboardGuildInfo {
    id: string;
    name: string;
    iconUrl: string | null;
    memberCount: number;
    connected: boolean;
}

export interface DashboardGuildConfigSnapshot {
    guild: DashboardGuildInfo;
    persisted: boolean;
    config: Record<string, unknown>;
}

export interface DashboardGuildChannelInfo {
    id: string;
    name: string;
    type: string;
    parentCategoryId: string | null;
    position: number;
}

export interface DashboardGuildRoleInfo {
    id: string;
    name: string;
    color: number;
    position: number;
}

export interface DashboardGuildMemberInfo {
    id: string;
    username: string;
    displayName: string;
    avatarUrl: string | null;
}

export interface DashboardStarboardSettings {
    starboardEnabled: boolean;
    starboardChannelId: string | null;
    minimumStars: number;
    botStarsCount: boolean;
    showcaseChannelIds: string[];
    starboardMediaOnly: boolean;
    starEmoji: string;
}

export interface DashboardLevellingSettings {
    levellingEnabled: boolean;
    levelCooldown: number;
    minXp: number;
    maxXp: number;
    levellingItemChance: number;
    disabledLevellingChannelIds: string[];
    disableLevelUpMessages: boolean;
    hasLevelUpChannel: boolean;
    levelUpMessageChannelId: string | null;
    shouldEmbedLevelUpMessage: boolean;
    levelDepletionEnabled: boolean;
    levelRoleMappings: string[];
    xpBoostedChannelIds: string[];
    xpBoostedRoleIds: string[];
    xpBoostPercentage: number;
    doServerBoostsAffectXP: boolean;
}

export interface DashboardLoggingSettings {
    loggingChannelId: string | null;
    modLoggingChannelId: string | null;
    logChannelCreate: boolean;
    logChannelDelete: boolean;
    logChannelUpdate: boolean;
    logEmojiAdded: boolean;
    logEmojiRemoved: boolean;
    logEmojiUpdate: boolean;
    logForumTagUpdate: boolean;
    logStickerUpdate: boolean;
    logGuildUpdate: boolean;
    logRoleUpdate: boolean;
    logBan: boolean;
    logUnban: boolean;
    logInviteCreate: boolean;
    logInviteDelete: boolean;
    logMemberJoin: boolean;
    logMemberRemove: boolean;
    logStickerAdded: boolean;
    logStickerRemove: boolean;
    logTimeout: boolean;
    logMessageBulkDelete: boolean;
    logMessageDelete: boolean;
    logMessageUpdate: boolean;
    logRoleCreate: boolean;
    logRoleDelete: boolean;
}

export interface DashboardOptInChannelsSettings {
    optInChannelIds: string[];
}

export interface DashboardWarningsSettings {
    warningsModeratorOnly: boolean;
    warningXpPercentage: number;
    warningEconomyPercentage: number;
}

export interface DashboardWarningRecord {
    uuid: string;
    userId: string;
    userDisplayName: string;
    userAvatarUrl: string | null;
    warnerId: string;
    warnerDisplayName: string;
    warnerAvatarUrl: string | null;
    reason: string;
    warnedAt: number;
}

export interface DashboardWarningUserSummary {
    id: string;
    displayName: string;
    avatarUrl: string | null;
}

export interface DashboardWarningsResponse {
    settings: DashboardWarningsSettings;
    warnings: DashboardWarningRecord[];
}

export interface DashboardWarningDetailResponse {
    warning: DashboardWarningRecord;
    user: DashboardWarningUserSummary;
    relatedWarnings: DashboardWarningRecord[];
}

export interface DashboardWarningHistoryResponse {
    user: DashboardWarningUserSummary;
    warnings: DashboardWarningRecord[];
}

export interface DashboardNotifierEntry {
    type: string;
    kind: string;
    targetLabel: string;
    targetValue: string | null;
    channelId: string;
    channelName: string | null;
    mention: string;
    details: string[];
}

export interface DashboardNotifierSection {
    key: string;
    title: string;
    description: string;
    count: number;
    entries: DashboardNotifierEntry[];
}

export interface DashboardNotifiersResponse {
    guild: DashboardGuildInfo;
    totalCount: number;
    sections: DashboardNotifierSection[];
}

export interface DashboardNotifierMutationRequest {
    originalTarget?: string | null;
    target: string | null;
    discordChannelId: number | null;
    mention: string | null;
}

export interface DashboardReportUserSummary {
    id: string;
    displayName: string;
    avatarUrl: string | null;
}

export interface DashboardReportRecord {
    reporterId: string;
    reporterDisplayName: string;
    reporterAvatarUrl: string | null;
    reason: string;
    reportedAt: number;
}

export interface DashboardReportHistoryResponse {
    user: DashboardReportUserSummary;
    reports: DashboardReportRecord[];
}

export interface DashboardEconomySettings {
    economyCurrency: string;
    economyEnabled: boolean;
    donateEnabled: boolean;
    defaultEconomyBalance: string;
    incomeTax: number;
}

export interface DashboardWelcomeSettings {
    welcomeChannelId: string | null;
    shouldAnnounceJoins: boolean;
    shouldAnnounceLeaves: boolean;
}

export interface DashboardBirthdaySettings {
    birthdayChannelId: string | null;
    announceBirthdays: boolean;
}

export interface DashboardCollectableItem {
    name: string;
    richName: string;
    emoji: string;
    rarity: string;
    note: string | null;
}

export interface DashboardCollectableCollection {
    type: string;
    displayName: string;
    disabledCollectables: string[];
    collectables: DashboardCollectableItem[];
}

export interface DashboardCollectablesSettings {
    collectorChannelId: string | null;
    collectingEnabled: boolean;
    collectableTypesRestricted: boolean;
    enabledCollectableTypeIds: string[];
    collections: DashboardCollectableCollection[];
}

export interface DashboardSuggestionsSettings {
    suggestionsChannelId: string | null;
}

export interface DashboardQuoteRecord {
    number: number;
    text: string;
    userId: string;
    userDisplayName: string;
    userAvatarUrl: string | null;
    addedById: string;
    addedByDisplayName: string;
    addedByAvatarUrl: string | null;
    channelId: string | null;
    messageId: string | null;
    messageUrl: string | null;
    timestamp: number;
}

export interface DashboardQuotesPageResponse {
    page: number;
    pageSize: number;
    totalCount: number;
    totalPages: number;
    quotes: DashboardQuoteRecord[];
}

export interface DashboardTagRecord {
    name: string;
    userId: string;
    userDisplayName: string;
    userAvatarUrl: string | null;
    contentType: string;
    content: string;
    rawData: string;
}

export interface DashboardTagsPageResponse {
    page: number;
    pageSize: number;
    totalCount: number;
    totalPages: number;
    tags: DashboardTagRecord[];
}

export interface DashboardTagCreateRequest {
    name: string;
    contentType: "message" | "embed";
    content: string;
    actorUserId: string;
}

export interface DashboardSuggestionResponseEntry {
    type: string;
    content: string;
    responderId: string;
    responderDisplayName: string;
    responderAvatarUrl: string | null;
    respondedAt: number;
}

export interface DashboardSuggestionRecord {
    number: number;
    messageId: string;
    messageUrl: string | null;
    userId: string;
    userDisplayName: string;
    userAvatarUrl: string | null;
    content: string;
    mediaUrl: string | null;
    mediaPreview: DashboardSuggestionMediaPreview | null;
    createdAt: number;
    status: string;
    responses: DashboardSuggestionResponseEntry[];
}

export interface DashboardSuggestionMediaPreview {
    url: string;
    title: string | null;
    description: string | null;
    siteName: string | null;
    imageUrl: string | null;
    type: string;
}

export interface DashboardSuggestionsPageResponse {
    suggestionsChannelId: string | null;
    page: number;
    pageSize: number;
    totalCount: number;
    totalPages: number;
    suggestions: DashboardSuggestionRecord[];
}

export interface DashboardSuggestionActionRequest {
    actorUserId: string;
    action?: string;
    reason: string;
}

export interface DashboardAiSettings {
    aiEnabled: boolean;
    aiChannelWhitelist: string[];
    aiUserBlacklist: string[];
}

export interface DashboardChatRevivalSettings {
    chatRevivalEnabled: boolean;
    chatRevivalChannelId: string | null;
    chatRevivalTime: number;
    chatRevivalTypes: string[];
    chatRevivalAllowNsfw: boolean;
}

export interface DashboardNsfwSettings {
    nsfwChannelIds: string[];
    artistNsfwFilterEnabled: boolean;
}

export interface DashboardThreadSettings {
    shouldModeratorsJoinThreads: boolean;
    autoThreadChannelIds: string[];
}

export interface DashboardMiscSettings {
    shouldCreateGists: boolean;
    shouldSendStartupMessage: boolean;
    shouldSendChangelog: boolean;
    stickyRolesEnabled: boolean;
    patronRoleId: string | null;
}

export interface DashboardAutomodSettings {
    inviteGuardEnabled: boolean;
    inviteGuardWhitelistChannelIds: string[];
    scamDetectionEnabled: boolean;
    imageSpamAutoBanEnabled: boolean;
    imageSpamWindowSeconds: number;
    imageSpamMinImages: number;
    imageSpamNewMemberThresholdHours: number;
}

export interface DashboardModmailSettings {
    moderatorRoleIds: string[];
    ticketCreatedMessage: string;
}

export interface DashboardStickyMessageInfo {
    channelId: string;
    channelName: string;
    connected: boolean;
    content: string;
    hasEmbed: boolean;
    ownerDisplayName: string;
    ownerId: string;
    postedMessage: number;
    updatedAt: number;
}

export interface DashboardStickyMessagesResponse {
    stickyMessages: DashboardStickyMessageInfo[];
}

export interface DashboardStickyMessageRequest {
    channelId: string;
    content: string;
}

export interface DashboardModmailTicketSummary {
    ticketNumber: number;
    userId: string;
    userDisplayName: string;
    userAvatarUrl: string | null;
    channelId: string;
    channelName: string;
    categoryId: string;
    categoryName: string;
    open: boolean;
    source: string;
    openedAt: number;
    closedAt: number;
    closedById: string;
    closedByName: string;
    closeReason: string;
    transcriptChunkCount: number;
    transcriptMessageCount: number;
}

export interface DashboardModmailTranscriptEntry {
    messageId: string;
    authorId: string;
    authorTag: string;
    authorAvatarUrl: string | null;
    bot: boolean;
    content: string;
    previews: DashboardModmailLinkPreview[];
    attachments: string[];
    embeds: string[];
    stickers: string[];
    createdAt: number;
    editedAt: number;
}

export interface DashboardModmailLinkPreview {
    url: string;
    title: string | null;
    description: string | null;
    siteName: string | null;
    imageUrl: string | null;
    type: string;
}

export interface DashboardModmailTicketDetail {
    ticket: DashboardModmailTicketSummary;
    openerMessage: string;
    transcript: DashboardModmailTranscriptEntry[];
}

export interface DashboardModmailTicketsResponse {
    tickets: DashboardModmailTicketSummary[];
}

export interface DashboardCountingChannelInfo {
    channelId: string;
    channelName: string;
    mode: string;
    connected: boolean;
    currentCount: number;
    highestCount: number;
}

export interface DashboardCountingSettings {
    maxCountingSuccession: number;
    channels: DashboardCountingChannelInfo[];
    availableModes: DashboardCountingModeInfo[];
}

export interface DashboardCountingModeInfo {
    mode: string;
    label: string;
    description: string;
}

export interface DashboardCountingChannelUpsertRequest {
    channelId: string;
    mode: string;
    maxCountingSuccession?: number;
}

export interface DashboardBirthday {
    day: number;
    month: number;
    year: number;
}

export interface DashboardWordleStreak {
    guildId: string;
    streak: number;
    bestStreak: number;
    hasPlayedToday: boolean;
}

export interface DashboardWordleProfile {
    streaks: DashboardWordleStreak[];
}

export interface DashboardCollectablesGroup {
    type: string;
    collectables: string[];
}

export interface DashboardCollectablesProfile {
    collectables: DashboardCollectablesGroup[];
}

export interface DashboardEconomyEntry {
    guildId: string;
    currency: string;
    bank: string;
    wallet: string;
    crimeLevel: number;
    heistLevel: number;
    job: string | null;
    jobLevel: number;
}

export interface DashboardUserProfile {
    userId: string;
    birthday: DashboardBirthday | null;
    wordle: DashboardWordleProfile | null;
    collectables: DashboardCollectablesProfile | null;
    economy: DashboardEconomyEntry[];
}

interface DashboardApiErrorPayload {
    error?: string;
    message?: string;
}

class DashboardApiError extends Error {
    readonly status: number;
    readonly code: string | null;

    constructor(message: string, status: number, code: string | null = null) {
        super(message);
        this.name = "DashboardApiError";
        this.status = status;
        this.code = code;
    }
}

function getRequiredEnv(name: string) {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Missing required environment variable: ${name}`);
    }

    return value;
}

function getDashboardApiBaseUrl() {
    const value = getRequiredEnv("DASHBOARD_API_URL").trim();
    return value.endsWith("/") ? value : `${value}/`;
}

function createDashboardApiUrl(path: string) {
    const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
    return new URL(normalizedPath, getDashboardApiBaseUrl());
}

function createDashboardHeaders() {
    return {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-TurtyBot-Api-Key": getRequiredEnv("DASHBOARD_API_KEY")
    };
}

async function readErrorPayload(response: Response) {
    try {
        return await response.json() as DashboardApiErrorPayload;
    } catch {
        return null;
    }
}

async function dashboardFetch<T>(path: string, init?: RequestInit) {
    const response = await fetch(createDashboardApiUrl(path), {
        ...init,
        headers: {
            ...createDashboardHeaders(),
            ...(init?.headers ?? {})
        },
        cache: "no-store"
    });

    if (!response.ok) {
        const payload = await readErrorPayload(response);
        throw new DashboardApiError(
            payload?.message ?? `Dashboard API request failed with status ${response.status}.`,
            response.status,
            payload?.error ?? null
        );
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}

export function isDashboardApiError(error: unknown): error is DashboardApiError {
    return error instanceof DashboardApiError;
}

export async function fetchDashboardHealth() {
    return dashboardFetch<DashboardHealthResponse>("/api/health", {
        method: "GET"
    });
}

export async function fetchDashboardGuildConfig(guildId: string) {
    return dashboardFetch<DashboardGuildConfigSnapshot>(`/api/guilds/${guildId}/config`, {
        method: "GET"
    });
}

export async function fetchDashboardGuildChannels(guildId: string, userId: string) {
    return dashboardFetch<{channels: DashboardGuildChannelInfo[]}>(`/api/guilds/${guildId}/channels?userId=${encodeURIComponent(userId)}`, {
        method: "GET"
    });
}

export async function fetchDashboardGuildRoles(guildId: string, userId: string) {
    return dashboardFetch<{roles: DashboardGuildRoleInfo[]}>(`/api/guilds/${guildId}/roles?userId=${encodeURIComponent(userId)}`, {
        method: "GET"
    });
}

export async function fetchDashboardGuildMembers(guildId: string, userId: string, query: string) {
    const searchParams = new URLSearchParams({
        userId,
        query
    });

    return dashboardFetch<{members: DashboardGuildMemberInfo[]}>(`/api/guilds/${guildId}/members?${searchParams.toString()}`, {
        method: "GET"
    });
}

export async function fetchDashboardStarboardSettings(guildId: string) {
    return dashboardFetch<DashboardStarboardSettings>(`/api/guilds/${guildId}/config/starboard`, {
        method: "GET"
    });
}

export async function fetchDashboardLevellingSettings(guildId: string) {
    return dashboardFetch<DashboardLevellingSettings>(`/api/guilds/${guildId}/config/levelling`, {
        method: "GET"
    });
}

export async function fetchDashboardLoggingSettings(guildId: string) {
    return dashboardFetch<DashboardLoggingSettings>(`/api/guilds/${guildId}/config/logging`, {
        method: "GET"
    });
}

export async function fetchDashboardOptInChannelsSettings(guildId: string) {
    return dashboardFetch<DashboardOptInChannelsSettings>(`/api/guilds/${guildId}/config/opt-in-channels`, {
        method: "GET"
    });
}

export async function updateDashboardOptInChannelsSettings(guildId: string, body: DashboardOptInChannelsSettings) {
    return dashboardFetch<DashboardOptInChannelsSettings>(`/api/guilds/${guildId}/config/opt-in-channels`, {
        method: "PUT",
        body: JSON.stringify(body)
    });
}

export async function fetchDashboardWarnings(guildId: string) {
    return dashboardFetch<DashboardWarningsResponse>(`/api/guilds/${guildId}/warnings`, {
        method: "GET"
    });
}

export async function fetchDashboardNotifiers(guildId: string) {
    return dashboardFetch<DashboardNotifiersResponse>(`/api/guilds/${guildId}/notifiers`, {
        method: "GET"
    });
}

export async function createDashboardNotifier(guildId: string, type: string, body: DashboardNotifierMutationRequest) {
    return dashboardFetch<DashboardNotifiersResponse>(`/api/guilds/${guildId}/notifiers/${encodeURIComponent(type)}`, {
        method: "POST",
        body: JSON.stringify(body)
    });
}

export async function updateDashboardNotifier(guildId: string, type: string, body: DashboardNotifierMutationRequest) {
    return dashboardFetch<DashboardNotifiersResponse>(`/api/guilds/${guildId}/notifiers/${encodeURIComponent(type)}`, {
        method: "PUT",
        body: JSON.stringify(body)
    });
}

export async function deleteDashboardNotifier(guildId: string, type: string, body: DashboardNotifierMutationRequest) {
    return dashboardFetch<DashboardNotifiersResponse>(`/api/guilds/${guildId}/notifiers/${encodeURIComponent(type)}`, {
        method: "DELETE",
        body: JSON.stringify(body)
    });
}

export async function fetchDashboardWarningDetail(guildId: string, warningUuid: string) {
    return dashboardFetch<DashboardWarningDetailResponse>(`/api/guilds/${guildId}/warnings/${encodeURIComponent(warningUuid)}`, {
        method: "GET"
    });
}

export async function fetchDashboardUserWarnings(guildId: string, userId: string) {
    return dashboardFetch<DashboardWarningHistoryResponse>(`/api/guilds/${guildId}/warnings/users/${encodeURIComponent(userId)}`, {
        method: "GET"
    });
}

export async function fetchDashboardUserReports(guildId: string, userId: string) {
    return dashboardFetch<DashboardReportHistoryResponse>(`/api/guilds/${guildId}/reports/${encodeURIComponent(userId)}`, {
        method: "GET"
    });
}

export async function fetchDashboardEconomySettings(guildId: string) {
    return dashboardFetch<DashboardEconomySettings>(`/api/guilds/${guildId}/config/economy`, {
        method: "GET"
    });
}

export async function fetchDashboardWelcomeSettings(guildId: string) {
    return dashboardFetch<DashboardWelcomeSettings>(`/api/guilds/${guildId}/config/welcome`, {
        method: "GET"
    });
}

export async function fetchDashboardBirthdaySettings(guildId: string) {
    return dashboardFetch<DashboardBirthdaySettings>(`/api/guilds/${guildId}/config/birthday`, {
        method: "GET"
    });
}

export async function fetchDashboardCollectablesSettings(guildId: string) {
    return dashboardFetch<DashboardCollectablesSettings>(`/api/guilds/${guildId}/config/collectables`, {
        method: "GET"
    });
}

export async function fetchDashboardSuggestionsSettings(guildId: string) {
    return dashboardFetch<DashboardSuggestionsSettings>(`/api/guilds/${guildId}/config/suggestions`, {
        method: "GET"
    });
}

export async function fetchDashboardQuotes(guildId: string, page = 1, pageSize = 10) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardQuotesPageResponse>(`/api/guilds/${guildId}/quotes?${searchParams.toString()}`, {
        method: "GET"
    });
}

export async function fetchDashboardTags(guildId: string, page = 1, pageSize = 10) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardTagsPageResponse>(`/api/guilds/${guildId}/tags?${searchParams.toString()}`, {
        method: "GET"
    });
}

export async function createDashboardTag(guildId: string, request: DashboardTagCreateRequest, page = 1, pageSize = 10) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardTagsPageResponse>(`/api/guilds/${guildId}/tags?${searchParams.toString()}`, {
        method: "POST",
        body: JSON.stringify(request)
    });
}

export async function fetchDashboardSuggestions(guildId: string, page = 1, pageSize = 10) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardSuggestionsPageResponse>(`/api/guilds/${guildId}/suggestions?${searchParams.toString()}`, {
        method: "GET"
    });
}

export async function fetchDashboardAiSettings(guildId: string) {
    return dashboardFetch<DashboardAiSettings>(`/api/guilds/${guildId}/config/ai`, {
        method: "GET"
    });
}

export async function fetchDashboardChatRevivalSettings(guildId: string) {
    return dashboardFetch<DashboardChatRevivalSettings>(`/api/guilds/${guildId}/config/chat-revival`, {
        method: "GET"
    });
}

export async function fetchDashboardNsfwSettings(guildId: string) {
    return dashboardFetch<DashboardNsfwSettings>(`/api/guilds/${guildId}/config/nsfw`, {
        method: "GET"
    });
}

export async function fetchDashboardThreadSettings(guildId: string) {
    return dashboardFetch<DashboardThreadSettings>(`/api/guilds/${guildId}/config/threads`, {
        method: "GET"
    });
}

export async function fetchDashboardMiscSettings(guildId: string) {
    return dashboardFetch<DashboardMiscSettings>(`/api/guilds/${guildId}/config/misc`, {
        method: "GET"
    });
}

export async function fetchDashboardAutomodSettings(guildId: string) {
    return dashboardFetch<DashboardAutomodSettings>(`/api/guilds/${guildId}/config/automod`, {
        method: "GET"
    });
}

export async function fetchDashboardModmailSettings(guildId: string) {
    return dashboardFetch<DashboardModmailSettings>(`/api/guilds/${guildId}/config/modmail`, {
        method: "GET"
    });
}

export async function fetchDashboardStickyMessages(guildId: string) {
    return dashboardFetch<DashboardStickyMessagesResponse>(`/api/guilds/${guildId}/sticky-messages`, {
        method: "GET"
    });
}

export async function fetchDashboardModmailTickets(guildId: string, status: string = "all") {
    const searchParams = new URLSearchParams({status});
    return dashboardFetch<DashboardModmailTicketsResponse>(`/api/guilds/${guildId}/modmail/tickets?${searchParams.toString()}`, {
        method: "GET"
    });
}

export async function fetchDashboardModmailTicket(guildId: string, ticketNumber: string) {
    return dashboardFetch<DashboardModmailTicketDetail>(`/api/guilds/${guildId}/modmail/tickets/${ticketNumber}`, {
        method: "GET"
    });
}

export async function fetchDashboardCountingSettings(guildId: string) {
    return dashboardFetch<DashboardCountingSettings>(`/api/guilds/${guildId}/counting`, {
        method: "GET"
    });
}

export async function fetchDashboardUserProfile(userId: string) {
    return dashboardFetch<DashboardUserProfile>(`/api/users/${userId}/profile`, {
        method: "GET"
    });
}

export async function createDashboardSession(session: DashboardSessionRecord) {
    return dashboardFetch<DashboardSessionRecord>("/api/sessions", {
        method: "POST",
        body: JSON.stringify(session)
    });
}

export async function getDashboardSession(sessionId: string) {
    return dashboardFetch<DashboardSessionRecord>(`/api/sessions/${sessionId}`, {
        method: "GET"
    });
}

export async function deleteDashboardSession(sessionId: string) {
    return dashboardFetch<void>(`/api/sessions/${sessionId}`, {
        method: "DELETE"
    });
}

export async function updateDashboardStarboardSettings(guildId: string, settings: DashboardStarboardSettings) {
    return dashboardFetch<DashboardStarboardSettings>(`/api/guilds/${guildId}/config/starboard`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardLevellingSettings(guildId: string, settings: DashboardLevellingSettings) {
    return dashboardFetch<DashboardLevellingSettings>(`/api/guilds/${guildId}/config/levelling`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardLoggingSettings(guildId: string, settings: DashboardLoggingSettings) {
    return dashboardFetch<DashboardLoggingSettings>(`/api/guilds/${guildId}/config/logging`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardWarnings(guildId: string, settings: DashboardWarningsSettings) {
    return dashboardFetch<DashboardWarningsResponse>(`/api/guilds/${guildId}/warnings`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function deleteDashboardWarning(guildId: string, warningUuid: string) {
    return dashboardFetch<DashboardWarningsResponse>(`/api/guilds/${guildId}/warnings/${encodeURIComponent(warningUuid)}`, {
        method: "DELETE"
    });
}

export async function updateDashboardEconomySettings(guildId: string, settings: DashboardEconomySettings) {
    return dashboardFetch<DashboardEconomySettings>(`/api/guilds/${guildId}/config/economy`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardWelcomeSettings(guildId: string, settings: DashboardWelcomeSettings) {
    return dashboardFetch<DashboardWelcomeSettings>(`/api/guilds/${guildId}/config/welcome`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardBirthdaySettings(guildId: string, settings: DashboardBirthdaySettings) {
    return dashboardFetch<DashboardBirthdaySettings>(`/api/guilds/${guildId}/config/birthday`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardCollectablesSettings(guildId: string, settings: DashboardCollectablesSettings) {
    return dashboardFetch<DashboardCollectablesSettings>(`/api/guilds/${guildId}/config/collectables`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardSuggestionsSettings(guildId: string, settings: DashboardSuggestionsSettings) {
    return dashboardFetch<DashboardSuggestionsSettings>(`/api/guilds/${guildId}/config/suggestions`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function deleteDashboardQuote(guildId: string, quoteNumber: number, page = 1, pageSize = 10) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardQuotesPageResponse>(`/api/guilds/${guildId}/quotes/${quoteNumber}?${searchParams.toString()}`, {
        method: "DELETE"
    });
}

export async function deleteDashboardTag(guildId: string, tagName: string, page = 1, pageSize = 10) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardTagsPageResponse>(`/api/guilds/${guildId}/tags/${encodeURIComponent(tagName)}?${searchParams.toString()}`, {
        method: "DELETE"
    });
}

export async function moderateDashboardSuggestion(
    guildId: string,
    messageId: string,
    action: "APPROVED" | "DENIED" | "CONSIDERED",
    body: Omit<DashboardSuggestionActionRequest, "action">,
    page = 1,
    pageSize = 10
) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardSuggestionsPageResponse>(`/api/guilds/${guildId}/suggestions/${encodeURIComponent(messageId)}?${searchParams.toString()}`, {
        method: "PATCH",
        body: JSON.stringify({
            ...body,
            action
        })
    });
}

export async function deleteDashboardSuggestion(
    guildId: string,
    messageId: string,
    body: Omit<DashboardSuggestionActionRequest, "action">,
    page = 1,
    pageSize = 10
) {
    const searchParams = new URLSearchParams({
        page: page.toString(),
        pageSize: pageSize.toString()
    });

    return dashboardFetch<DashboardSuggestionsPageResponse>(`/api/guilds/${guildId}/suggestions/${encodeURIComponent(messageId)}?${searchParams.toString()}`, {
        method: "DELETE",
        body: JSON.stringify(body)
    });
}

export async function updateDashboardAiSettings(guildId: string, settings: DashboardAiSettings) {
    return dashboardFetch<DashboardAiSettings>(`/api/guilds/${guildId}/config/ai`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardChatRevivalSettings(guildId: string, settings: DashboardChatRevivalSettings) {
    return dashboardFetch<DashboardChatRevivalSettings>(`/api/guilds/${guildId}/config/chat-revival`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardNsfwSettings(guildId: string, settings: DashboardNsfwSettings) {
    return dashboardFetch<DashboardNsfwSettings>(`/api/guilds/${guildId}/config/nsfw`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardThreadSettings(guildId: string, settings: DashboardThreadSettings) {
    return dashboardFetch<DashboardThreadSettings>(`/api/guilds/${guildId}/config/threads`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardMiscSettings(guildId: string, settings: DashboardMiscSettings) {
    return dashboardFetch<DashboardMiscSettings>(`/api/guilds/${guildId}/config/misc`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardAutomodSettings(guildId: string, settings: DashboardAutomodSettings) {
    return dashboardFetch<DashboardAutomodSettings>(`/api/guilds/${guildId}/config/automod`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function updateDashboardModmailSettings(guildId: string, settings: DashboardModmailSettings) {
    return dashboardFetch<DashboardModmailSettings>(`/api/guilds/${guildId}/config/modmail`, {
        method: "PUT",
        body: JSON.stringify(settings)
    });
}

export async function upsertDashboardStickyMessage(guildId: string, payload: DashboardStickyMessageRequest) {
    return dashboardFetch<DashboardStickyMessagesResponse>(`/api/guilds/${guildId}/sticky-messages`, {
        method: "PUT",
        body: JSON.stringify(payload)
    });
}

export async function deleteDashboardStickyMessage(guildId: string, channelId: string) {
    return dashboardFetch<DashboardStickyMessagesResponse>(`/api/guilds/${guildId}/sticky-messages/${encodeURIComponent(channelId)}`, {
        method: "DELETE"
    });
}

export async function upsertDashboardCountingChannel(guildId: string, payload: DashboardCountingChannelUpsertRequest) {
    return dashboardFetch<DashboardCountingSettings>(`/api/guilds/${guildId}/counting`, {
        method: "PUT",
        body: JSON.stringify(payload)
    });
}

export async function deleteDashboardCountingChannel(guildId: string, channelId: string) {
    return dashboardFetch<DashboardCountingSettings>(`/api/guilds/${guildId}/counting/${channelId}`, {
        method: "DELETE"
    });
}
