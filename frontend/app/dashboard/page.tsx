import Link from "next/link";
import { redirect } from "next/navigation";
import QueryWorkbench from "../../components/QueryWorkbench";
import SignOutButton from "../../components/SignOutButton";
import { createClient } from "../../lib/supabase/server";

type QueryRow = {
  id: string;
  question: string;
};

type ReportRow = {
  id: string;
  query_id: string;
  status: string;
  created_at: string;
};

export default async function DashboardPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) redirect("/auth/sign-in");

  const { data: reports } = await supabase
    .from("reports")
    .select("id,query_id,status,created_at")
    .eq("status", "completed")
    .order("created_at", { ascending: false })
    .limit(8);

  const queryIds = (reports ?? []).map((report) => report.query_id);
  const { data: reportQueries } = queryIds.length
    ? await supabase.from("queries").select("id,question").in("id", queryIds).eq("user_id", user.id)
    : { data: [] as QueryRow[] };
  const reportQuestionMap = new Map((reportQueries ?? []).map((query) => [query.id, query.question]));
  const ownedReports = (reports ?? []).filter((report) => reportQuestionMap.has(report.query_id));

  return (
    <main
      className="relative min-h-screen overflow-hidden bg-zinc-950 text-zinc-100"
      style={{ fontFamily: '"Sora", "Space Grotesk", "Avenir Next", sans-serif' }}
    >
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(34,211,238,0.22),transparent_40%),radial-gradient(circle_at_80%_10%,rgba(249,115,22,0.2),transparent_35%),radial-gradient(circle_at_50%_90%,rgba(20,184,166,0.16),transparent_45%)]" />
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(to_right,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:48px_48px] opacity-20" />

      <section className="relative mx-auto flex w-full max-w-6xl flex-col gap-8 px-4 pt-8 sm:px-6 lg:px-8">
        <header className="flex flex-col justify-between gap-5 rounded-3xl border border-zinc-700/60 bg-zinc-900/60 p-6 backdrop-blur sm:flex-row sm:items-center">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-cyan-300">Dashboard</p>
            <h1 className="mt-2 text-3xl font-black text-white">Welcome back, {user.email}</h1>
            <p className="mt-1 text-sm text-zinc-300">Run a new market analysis and keep your report history in one place.</p>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/dashboard/history"
              className="rounded-full border border-zinc-700 bg-zinc-900/70 px-4 py-2 text-sm font-medium text-zinc-200 transition hover:border-zinc-500 hover:text-white"
            >
              View History
            </Link>
            <SignOutButton />
          </div>
        </header>

        <section className="rounded-3xl border border-zinc-700/60 bg-zinc-900/60 p-5 backdrop-blur">
          <h2 className="mb-3 text-sm uppercase tracking-[0.2em] text-zinc-400">Latest Completed Reports</h2>
          {ownedReports.length > 0 ? (
            <div className="grid gap-3 sm:grid-cols-2">
              {(ownedReports as ReportRow[]).map((report) => (
                <Link
                  key={report.id}
                  href={`/dashboard/reports/${report.id}`}
                  className="rounded-2xl border border-zinc-700/70 bg-zinc-950/70 p-4 transition hover:border-cyan-400/40"
                >
                  <p className="line-clamp-2 text-sm text-zinc-200">{reportQuestionMap.get(report.query_id)}</p>
                  <div className="mt-3 flex items-center justify-between text-xs text-zinc-400">
                    <span className="uppercase">{report.status}</span>
                    <span>{new Date(report.created_at).toLocaleDateString()}</span>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <p className="text-sm text-zinc-300">No reports yet. Generate your first full report below.</p>
          )}
        </section>
      </section>

      <QueryWorkbench userId={user.id} />
    </main>
  );
}
