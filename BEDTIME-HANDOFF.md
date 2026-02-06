# PolyInsights Bedtime Handoff

## Done
- Supabase SQL schema created: `supabase/schema.sql`
- Main n8n workflow JSON created: `n8n/polyinsights-workflow.json`
- Public preview n8n workflow JSON created: `n8n/polyinsights-preview-workflow.json`
- Next.js App Router pages implemented:
  - Public landing with partial preview
  - Google sign-in + auth callback
  - Protected dashboard with full summary + report generation
  - History page
  - Unique report detail page (`/dashboard/reports/[id]`)

## Copy This Env File For Next.js
- Template file: `frontend/.env.local.template`
- Copy command:
  - `cp frontend/.env.local.template .env.local`
- Then replace `YOUR_N8N_DOMAIN` values.

## n8n Setup (required)
- Import workflows:
  - `n8n/polyinsights-preview-workflow.json`
  - `n8n/polyinsights-workflow.json`
- Set n8n environment variables:
  - `OPENAI_API_KEY`
  - `SUPABASE_URL=https://mfqbccfgzvsumewkedmy.supabase.co`
  - `SUPABASE_SERVICE_ROLE_KEY=<service_role_key>`
- Activate both workflows after testing.

## Supabase Setup (required)
- Confirm Google OAuth provider stays enabled.
- URL config (already done for local):
  - `http://localhost:3000`
  - `http://localhost:3000/auth/callback`
- For production, add:
  - `https://<your-domain>`
  - `https://<your-domain>/auth/callback`
- Retrieve and save `service_role` key for n8n env.

## Local Run Checklist
- In your Next.js app:
  - `npm install @supabase/supabase-js @supabase/ssr html2canvas jspdf`
  - `npm run dev`
- Test flow:
  1. Public homepage query returns partial preview.
  2. Sign in with Google.
  3. Dashboard query returns full summary.
  4. Generate full report.
  5. History shows completed report.
  6. Clicking a report opens `/dashboard/reports/[id]`.

## Vercel Deploy Checklist
- Add Next.js env vars in Vercel project settings:
  - `NEXT_PUBLIC_SUPABASE_URL`
  - `NEXT_PUBLIC_SUPABASE_ANON_KEY`
  - `NEXT_PUBLIC_N8N_PREVIEW_URL`
  - `NEXT_PUBLIC_N8N_ANALYZE_URL`
  - `NEXT_PUBLIC_N8N_REPORT_URL`
- Redeploy.
- Add production URLs to Supabase Auth redirect list.

## Recommended Next Improvements
- Add authenticated ownership validation inside n8n for report generation.
- Add graceful error branches in n8n (clean 4xx/5xx JSON responses).
- Render markdown formatting more richly on report detail page.
