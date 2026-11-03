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

## Server audits

The authenticated `/dashboard/{guildId}/audit` page shows the latest report generated in Discord or from the dashboard. Findings link to the relevant settings editors; run the audit again after making changes. Sign-in and current Discord Manage Server permission are required. Reports are stored by the bot in MongoDB.

Configure `DASHBOARD_SITE_URL` on the bot to this site's public URL so the Discord summary embed can link here. `DASHBOARD_API_URL` on this site still points to the Javalin API.

Run the audit route authorization checks with `node --experimental-vm-modules --test tests/server-audit.test.mjs`.
