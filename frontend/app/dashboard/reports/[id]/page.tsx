import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { createClient } from "../../../../../lib/supabase/server";

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

export default async function ReportDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) redirect("/auth/sign-in");

  const { data: report } = await supabase.from("reports").select("*").eq("id", id).maybeSingle();
  if (!report) notFound();

  const { data: query } = await supabase
    .from("queries")
    .select("id,question")
    .eq("id", report.query_id)
    .eq("user_id", user.id)
    .maybeSingle();

  if (!query) notFound();

  return (
    <main className="min-h-screen bg-zinc-950 px-4 py-10 text-zinc-100 sm:px-6">
      <section className="mx-auto w-full max-w-5xl">
        <div className="mb-6 flex items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-cyan-300">Report</p>
            <h1 className="mt-1 text-2xl font-bold text-white">{(query as QueryRow).question}</h1>
          </div>
          <Link href="/dashboard/history" className="text-sm text-cyan-300 hover:text-cyan-200">
            Back to history
          </Link>
        </div>

        <article className="rounded-3xl border border-cyan-400/20 bg-zinc-900/80 p-6 sm:p-10">
          {((report as ReportRow).report_content ?? "Report content is empty.").split("\n").map((line, index) => (
            <p key={`${index}-${line.slice(0, 8)}`} className="mb-3 whitespace-pre-wrap leading-7 text-zinc-200">
              {line}
            </p>
          ))}
        </article>
      </section>
    </main>
  );
}
