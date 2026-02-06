"use client";

import { Suspense, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { createClient } from "../../../lib/supabase/client";

function SignInForm() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const searchParams = useSearchParams();

  const next = useMemo(() => searchParams.get("next") ?? "/dashboard", [searchParams]);

  async function signInWithGoogle() {
    setLoading(true);
    setError("");

    try {
      const supabase = createClient();
      const origin = window.location.origin;
      const redirectTo = `${origin}/auth/callback?next=${encodeURIComponent(next)}`;

      const { error: signInError } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: { redirectTo },
      });

      if (signInError) throw signInError;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to start Google sign-in.");
      setLoading(false);
    }
  }

  return (
    <div className="w-full max-w-md rounded-3xl border border-zinc-700/60 bg-zinc-900/70 p-8 shadow-[0_0_140px_-55px_rgba(34,211,238,0.75)] backdrop-blur">
      <p className="text-xs uppercase tracking-[0.25em] text-cyan-300">PolyInsights</p>
      <h1 className="mt-2 text-3xl font-black text-white">Sign in</h1>
      <p className="mt-2 text-sm text-zinc-300">Use Google OAuth to access your query history and reports.</p>

      <button
        type="button"
        onClick={signInWithGoogle}
        disabled={loading}
        className="mt-7 flex h-12 w-full items-center justify-center rounded-xl bg-white px-4 text-sm font-semibold text-zinc-900 transition hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-70"
      >
        {loading ? "Redirecting..." : "Continue with Google"}
      </button>

      {error ? <p className="mt-4 text-sm text-red-300">{error}</p> : null}
    </div>
  );
}

export default function SignInPage() {
  return (
    <main className="relative min-h-screen overflow-hidden bg-zinc-950 text-zinc-100">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_15%_10%,rgba(34,211,238,0.2),transparent_38%),radial-gradient(circle_at_85%_15%,rgba(249,115,22,0.16),transparent_30%),radial-gradient(circle_at_50%_85%,rgba(20,184,166,0.14),transparent_45%)]" />

      <section className="relative mx-auto flex min-h-screen w-full max-w-5xl items-center justify-center px-4 py-16">
        <Suspense>
          <SignInForm />
        </Suspense>
      </section>
    </main>
  );
}
