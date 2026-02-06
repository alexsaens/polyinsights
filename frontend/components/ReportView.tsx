"use client";

type ReportViewProps = {
  report: string;
  downloading: boolean;
  onDownloadPdf: () => Promise<void>;
  onStartOver: () => void;
};

function renderLine(line: string, index: number) {
  if (line.startsWith("### ")) {
    return (
      <h3 key={index} className="mt-6 text-lg font-semibold text-zinc-100">
        {line.replace("### ", "")}
      </h3>
    );
  }

  if (line.startsWith("## ")) {
    return (
      <h2 key={index} className="mt-8 text-xl font-semibold text-zinc-100">
        {line.replace("## ", "")}
      </h2>
    );
  }

  if (line.startsWith("# ")) {
    return (
      <h1 key={index} className="mt-8 text-2xl font-bold text-zinc-50">
        {line.replace("# ", "")}
      </h1>
    );
  }

  if (!line.trim()) {
    return <div key={index} className="h-3" />;
  }

  return (
    <p key={index} className="leading-7 text-zinc-200/95">
      {line}
    </p>
  );
}

export default function ReportView({
  report,
  downloading,
  onDownloadPdf,
  onStartOver,
}: ReportViewProps) {
  return (
    <section className="w-full max-w-5xl">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-cyan-300">Final Report</p>
          <h2 className="text-2xl font-bold text-white sm:text-3xl">Market Intelligence Output</h2>
        </div>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={onStartOver}
            className="rounded-full border border-zinc-700/70 bg-zinc-900/60 px-5 py-2 text-sm font-medium text-zinc-200 transition hover:border-zinc-500 hover:text-white"
          >
            New Query
          </button>
          <button
            type="button"
            onClick={onDownloadPdf}
            disabled={downloading}
            className="rounded-full bg-cyan-300 px-5 py-2 text-sm font-semibold text-zinc-900 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {downloading ? "Generating PDF..." : "Download PDF"}
          </button>
        </div>
      </div>

      <article
        id="report-content"
        className="rounded-3xl border border-cyan-400/20 bg-zinc-900/80 p-6 shadow-[0_0_120px_-45px_rgba(34,211,238,0.6)] sm:p-10"
      >
        {report.split("\n").map(renderLine)}
      </article>
    </section>
  );
}
