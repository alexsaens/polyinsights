import Link from "next/link";
import { redirect } from "next/navigation";
import LandingPreview from "../components/LandingPreview";
import { createClient } from "../lib/supabase/server";

export default async function LandingPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (user) redirect("/dashboard");

  return (
    <main
      className="relative min-h-screen overflow-hidden bg-zinc-950 text-zinc-100"
      style={{ fontFamily: '"Sora", "Space Grotesk", "Avenir Next", sans-serif' }}
    >
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(34,211,238,0.22),transparent_40%),radial-gradient(circle_at_80%_10%,rgba(249,115,22,0.2),transparent_35%),radial-gradient(circle_at_50%_90%,rgba(20,184,166,0.16),transparent_45%)]" />
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(to_right,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:48px_48px] opacity-20" />

      <section className="relative mx-auto flex min-h-screen w-full max-w-6xl flex-col items-center justify-center px-4 py-20 text-center sm:px-6 lg:px-8">
        <p className="mb-3 text-xs uppercase tracking-[0.35em] text-cyan-300">PolyInsights</p>
        <h1 className="text-balance text-4xl font-black leading-tight text-white sm:text-6xl">
          Market Insight
          <span className="block bg-gradient-to-r from-cyan-200 via-teal-300 to-amber-200 bg-clip-text text-transparent">
            Report Generator
          </span>
        </h1>
        <p className="mt-5 max-w-2xl text-sm text-zinc-300 sm:text-base">
          Try a query below and get a partial preview. Sign in to unlock full summaries, report generation, and your report history.
        </p>

        <div className="mt-8 w-full">
          <LandingPreview />
        </div>

        <div className="mt-7 flex flex-col gap-3 sm:flex-row">
          <Link
            href="/auth/sign-in"
            className="inline-flex h-12 items-center justify-center rounded-2xl bg-gradient-to-r from-cyan-300 to-teal-300 px-7 text-sm font-semibold text-zinc-900 transition hover:brightness-105"
          >
            Continue with Google
          </Link>
          <Link
            href="/dashboard"
            className="inline-flex h-12 items-center justify-center rounded-2xl border border-zinc-700 bg-zinc-900/70 px-7 text-sm font-semibold text-zinc-200 transition hover:border-zinc-500 hover:text-white"
          >
            Open Dashboard
          </Link>
        </div>
      </section>
    </main>
  );
}
