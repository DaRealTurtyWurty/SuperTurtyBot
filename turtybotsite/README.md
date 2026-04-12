# TurtyBot Site

This is the Next.js dashboard application for TurtyBot.

## Running Locally

```bash
npm install
npm run dev
```

The site runs on `http://localhost:3003`.

## Required Environment Variables

```env
DISCORD_CLIENT_ID=
DISCORD_CLIENT_SECRET=
DISCORD_REDIRECT_URI=http://localhost:3003/api/auth/discord/callback
DASHBOARD_API_URL=http://127.0.0.1:7070
DASHBOARD_API_KEY=
```

`DASHBOARD_API_URL` should point at the bot's Javalin service, and `DASHBOARD_API_KEY` must match the bot's `DASHBOARD_API_KEY`.

## OAuth Routes

- `/api/auth/discord/login`
- `/api/auth/discord/callback`
- `/api/auth/logout`

## Current Scope

- Discord OAuth sign-in
- Bot API-backed dashboard sessions
- Authenticated guild list and overview stats
- Sidebar-based guild dashboard with dedicated pages
- Editable starboard settings backed by the bot's dashboard API
- Legacy authenticated user profile lookup backed by the bot's dashboard API
