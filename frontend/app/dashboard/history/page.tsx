import Link from "next/link";
import { redirect } from "next/navigation";
import { createClient } from "../../../../lib/supabase/server";

type ReportRow = {
  id: string;
  query_id: string;
  report_content: string | null;
  status: string;
  created_at: string;
};

type QueryRow = {
  id: string;
  question: string;
};

export default async function HistoryPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) redirect("/auth/sign-in");

  const { data: reports } = await supabase
    .from("reports")
    .select("id,query_id,status,created_at,report_content")
    .eq("status", "completed")
    .order("created_at", { ascending: false })
    .limit(50);

  const queryIds = (reports ?? []).map((report) => report.query_id);
  const { data: queryRows } = queryIds.length
    ? await supabase.from("queries").select("id,question").in("id", queryIds).eq("user_id", user.id)
    : { data: [] as QueryRow[] };

  const questionById = new Map((queryRows ?? []).map((query) => [query.id, query.question]));
  const ownedReports = (reports ?? []).filter((report) => questionById.has(report.query_id));

  return (
    <main className="min-h-screen bg-zinc-950 px-4 py-10 text-zinc-100 sm:px-6">
      <section className="mx-auto w-full max-w-5xl">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-bold">Report History</h1>
          <Link href="/dashboard" className="text-sm text-cyan-300 hover:text-cyan-200">
            Back to dashboard
          </Link>
        </div>

        <div className="space-y-3">
          {(ownedReports as ReportRow[]).map((report) => (
            <Link
              key={report.id}
              href={`/dashboard/reports/${report.id}`}
              className="block rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4 transition hover:border-cyan-400/40"
            >
              <p className="text-sm text-zinc-200">{questionById.get(report.query_id)}</p>
              <p className="mt-2 line-clamp-2 text-sm text-zinc-400">{report.report_content ?? "No content"}</p>
              <div className="mt-3 flex items-center justify-between text-xs text-zinc-400">
                <span className="uppercase">{report.status}</span>
                <span>{new Date(report.created_at).toLocaleString()}</span>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </main>
  );
}
