"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";

type PreviewResponse = {
  summary?: string;
};

const PREVIEW_URL =
  process.env.NEXT_PUBLIC_N8N_PREVIEW_URL ?? "https://your-n8n-url/webhook/polyinsights/analyze/v1";

export default function LandingPreview() {
  const [question, setQuestion] = useState("");
  const [partial, setPartial] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handlePreview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!question.trim() || loading) return;

    setLoading(true);
    setError("");
    setPartial("");

    try {
      const response = await fetch(PREVIEW_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: question.trim() }),
      });

      if (!response.ok) throw new Error(`Preview failed (${response.status}).`);
      const data = (await response.json()) as PreviewResponse;

      const summary = (data.summary ?? "").trim();
      if (!summary) throw new Error("No preview text returned.");

      const truncated = summary.length > 420 ? `${summary.slice(0, 420)}...` : summary;
      setPartial(truncated);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load preview.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="w-full max-w-4xl rounded-3xl border border-zinc-700/60 bg-zinc-900/70 p-5 shadow-[0_0_120px_-50px_rgba(34,211,238,0.75)] backdrop-blur">
      <form onSubmit={handlePreview}>
        <label htmlFor="preview-question" className="mb-2 block text-xs uppercase tracking-[0.2em] text-zinc-400">
          Try A Query
        </label>
        <div className="flex flex-col gap-3 sm:flex-row">
          <input
            id="preview-question"
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="Will AI chip demand keep rising through 2026?"
            className="h-14 w-full rounded-2xl border border-zinc-700 bg-zinc-950/80 px-4 text-sm text-zinc-100 outline-none transition placeholder:text-zinc-500 focus:border-cyan-300 sm:text-base"
          />
          <button
            type="submit"
            disabled={loading || !question.trim()}
            className="h-14 rounded-2xl bg-gradient-to-r from-cyan-300 to-teal-300 px-6 text-sm font-semibold text-zinc-900 transition hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? "Generating preview..." : "Get Preview"}
          </button>
        </div>
      </form>

      {partial ? (
        <div className="mt-5 rounded-2xl border border-cyan-500/20 bg-zinc-950/60 p-4">
          <p className="text-sm leading-7 text-zinc-200">{partial}</p>
          <div className="mt-4 flex items-center justify-between gap-3">
            <p className="text-xs uppercase tracking-[0.18em] text-zinc-400">Sign in to unlock full summary + report</p>
            <Link
              href="/auth/sign-in"
              className="rounded-full bg-cyan-300 px-4 py-2 text-xs font-semibold text-zinc-900 transition hover:bg-cyan-200"
            >
              Continue with Google
            </Link>
          </div>
        </div>
      ) : null}

      {error ? <p className="mt-4 text-sm text-red-300">{error}</p> : null}
    </section>
  );
}
