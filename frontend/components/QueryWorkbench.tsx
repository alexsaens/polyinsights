"use client";

import { FormEvent, useMemo, useState } from "react";
import ReportView from "./ReportView";

type ViewState = "idle" | "loadingSummary" | "review" | "loadingReport" | "final" | "error";

type AnalyzeResponse = {
  session_id: string;
  summary: string;
  sophistication_label?: "low" | "high";
  sophistication_score?: number;
  market_count?: number;
};

type ReportResponse = {
  report: string;
  format?: "markdown" | "html";
};

const ANALYZE_URL =
  process.env.NEXT_PUBLIC_N8N_ANALYZE_URL ?? "https://your-n8n-url/webhook/polyinsights/analyze/v1";
const REPORT_URL =
  process.env.NEXT_PUBLIC_N8N_REPORT_URL ?? "https://your-n8n-url/webhook/polyinsights/report/v1";

type QueryWorkbenchProps = {
  userId: string;
};

export default function QueryWorkbench({ userId }: QueryWorkbenchProps) {
  const [question, setQuestion] = useState("");
  const [summary, setSummary] = useState("");
  const [sessionId, setSessionId] = useState("");
  const [report, setReport] = useState("");
  const [viewState, setViewState] = useState<ViewState>("idle");
  const [errorMessage, setErrorMessage] = useState("");
  const [downloadingPdf, setDownloadingPdf] = useState(false);
  const [meta, setMeta] = useState({ score: 0, label: "", marketCount: 0 });

  const isLoading = viewState === "loadingSummary" || viewState === "loadingReport";
  const ctaLabel = useMemo(() => {
    if (viewState === "loadingSummary") return "Analyzing markets...";
    if (viewState === "loadingReport") return "Generating full report...";
    return "Analyze Market Question";
  }, [viewState]);

  async function handleAnalyze(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!question.trim() || isLoading) return;

    setErrorMessage("");
    setViewState("loadingSummary");

    try {
      const response = await fetch(ANALYZE_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: question.trim(), user_id: userId }),
      });

      if (!response.ok) throw new Error(`Analyze failed with status ${response.status}`);

      const data = (await response.json()) as AnalyzeResponse;
      if (!data.session_id || !data.summary) throw new Error("Invalid response: missing summary context.");

      setSessionId(data.session_id);
      setSummary(data.summary);
      setMeta({
        score: data.sophistication_score ?? 0,
        label: data.sophistication_label ?? "n/a",
        marketCount: data.market_count ?? 0,
      });
      setViewState("review");
    } catch (error) {
      setViewState("error");
      setErrorMessage(error instanceof Error ? error.message : "Unexpected error while analyzing.");
    }
  }

  async function handleGenerateReport() {
    if (!sessionId || isLoading) return;

    setErrorMessage("");
    setViewState("loadingReport");

    try {
      const response = await fetch(REPORT_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ session_id: sessionId, action: "move_forward" }),
      });

      if (!response.ok) throw new Error(`Report failed with status ${response.status}`);

      const data = (await response.json()) as ReportResponse;
      if (!data.report) throw new Error("Invalid response: missing report.");

      setReport(data.report);
      setViewState("final");
    } catch (error) {
      setViewState("error");
      setErrorMessage(error instanceof Error ? error.message : "Unexpected error while generating report.");
    }
  }

  async function handleDownloadPdf() {
    const element = document.getElementById("report-content");
    if (!element) return;

    setDownloadingPdf(true);
    try {
      const [{ default: html2canvas }, { default: jsPDF }] = await Promise.all([
        import("html2canvas"),
        import("jspdf"),
      ]);

      const canvas = await html2canvas(element, { scale: 2, backgroundColor: "#09090b" });
      const imageData = canvas.toDataURL("image/png");
      const pdf = new jsPDF({ orientation: "portrait", unit: "pt", format: "a4" });

      const pageWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();
      const ratio = Math.min(pageWidth / canvas.width, pageHeight / canvas.height);

      const width = canvas.width * ratio;
      const height = canvas.height * ratio;

      pdf.addImage(imageData, "PNG", (pageWidth - width) / 2, 24, width, height);
      pdf.save(`polyinsights-report-${sessionId.slice(0, 8)}.pdf`);
    } finally {
      setDownloadingPdf(false);
    }
  }

  function resetSession() {
    setQuestion("");
    setSummary("");
    setSessionId("");
    setReport("");
    setMeta({ score: 0, label: "", marketCount: 0 });
    setErrorMessage("");
    setViewState("idle");
  }

  return (
    <section className="relative mx-auto mt-10 flex w-full max-w-6xl flex-col items-center gap-8 px-4 pb-16 sm:px-6 lg:px-8">
      <form
        onSubmit={handleAnalyze}
        className="w-full max-w-4xl rounded-3xl border border-zinc-700/60 bg-zinc-900/70 p-4 shadow-[0_0_120px_-50px_rgba(34,211,238,0.75)] backdrop-blur"
      >
        <label htmlFor="question" className="mb-2 block text-xs uppercase tracking-[0.2em] text-zinc-400">
          Your Question
        </label>
        <div className="flex flex-col gap-3 sm:flex-row">
          <input
            id="question"
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="Will shipping be impacted by new US-based tariffs?"
            className="h-14 w-full rounded-2xl border border-zinc-700 bg-zinc-950/80 px-4 text-sm text-zinc-100 outline-none transition placeholder:text-zinc-500 focus:border-cyan-300 sm:text-base"
          />
          <button
            type="submit"
            disabled={isLoading || !question.trim()}
            className="h-14 rounded-2xl bg-gradient-to-r from-cyan-300 to-teal-300 px-5 text-sm font-semibold text-zinc-900 transition hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60 sm:px-7 sm:text-base"
          >
            {ctaLabel}
          </button>
        </div>
      </form>

      {viewState === "loadingSummary" || viewState === "loadingReport" ? (
        <section className="w-full max-w-3xl rounded-3xl border border-zinc-700/60 bg-zinc-900/65 p-8 text-center">
          <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-2 border-zinc-700 border-t-cyan-300" />
          <p className="text-zinc-200">{ctaLabel}</p>
        </section>
      ) : null}

      {viewState === "review" ? (
        <section className="w-full max-w-4xl rounded-3xl border border-cyan-500/20 bg-zinc-900/75 p-6 sm:p-8">
          <div className="mb-5 flex flex-wrap gap-2 text-xs">
            <span className="rounded-full border border-zinc-700 px-3 py-1 text-zinc-300">Sophistication: {meta.label}</span>
            <span className="rounded-full border border-zinc-700 px-3 py-1 text-zinc-300">Score: {meta.score}</span>
            <span className="rounded-full border border-zinc-700 px-3 py-1 text-zinc-300">Markets: {meta.marketCount}</span>
          </div>

          <h2 className="mb-3 text-2xl font-bold text-white">Review Summary</h2>
          <p className="whitespace-pre-wrap leading-7 text-zinc-200">{summary}</p>

          <div className="mt-7 flex flex-col gap-3 sm:flex-row">
            <button
              type="button"
              onClick={resetSession}
              className="h-12 rounded-full border border-zinc-700 bg-zinc-950/60 px-5 text-sm font-medium text-zinc-200 transition hover:border-zinc-500 hover:text-white"
            >
              Refine Query
            </button>
            <button
              type="button"
              onClick={handleGenerateReport}
              className="h-12 rounded-full bg-cyan-300 px-5 text-sm font-semibold text-zinc-900 transition hover:bg-cyan-200"
            >
              Generate Full Report
            </button>
          </div>
        </section>
      ) : null}

      {viewState === "final" ? (
        <ReportView report={report} downloading={downloadingPdf} onDownloadPdf={handleDownloadPdf} onStartOver={resetSession} />
      ) : null}

      {viewState === "error" ? (
        <section className="w-full max-w-3xl rounded-2xl border border-red-300/30 bg-red-500/10 p-5 text-sm text-red-100">
          Something went wrong: {errorMessage}
        </section>
      ) : null}
    </section>
  );
}
