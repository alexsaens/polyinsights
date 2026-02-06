# Frontend Drop-In

These files are designed for a Next.js App Router project using Tailwind CSS.

## Add dependencies
```bash
npm install @supabase/supabase-js @supabase/ssr html2canvas jspdf
```

## Configure env vars
Copy `.env.example` to your app env file and set real webhook URLs:
```bash
cp frontend/.env.example .env.local
```
- `NEXT_PUBLIC_N8N_PREVIEW_URL` should return a summary without requiring auth persistence (for the public landing preview).
- `NEXT_PUBLIC_N8N_ANALYZE_URL` and `NEXT_PUBLIC_N8N_REPORT_URL` are authenticated dashboard flows.

## Supabase Auth setup checklist
- Google OAuth enabled in Supabase Auth providers.
- Add these URLs in Supabase Auth URL configuration:
  - `http://localhost:3000`
  - `http://localhost:3000/auth/callback`
- For production, add your deployed domain and callback equivalent.

## Files
- `frontend/app/page.tsx`
- `frontend/app/layout.tsx`
- `frontend/app/auth/sign-in/page.tsx`
- `frontend/app/auth/callback/route.ts`
- `frontend/app/dashboard/page.tsx`
- `frontend/app/dashboard/history/page.tsx`
- `frontend/app/dashboard/reports/[id]/page.tsx`
- `frontend/components/ReportView.tsx`
- `frontend/components/LandingPreview.tsx`
- `frontend/components/QueryWorkbench.tsx`
- `frontend/components/SignOutButton.tsx`
- `frontend/lib/supabase/client.ts`
- `frontend/lib/supabase/server.ts`
- `frontend/lib/supabase/middleware.ts`
- `frontend/middleware.ts`
