import { listRecentInvestments, listRuns, listSources } from "@/lib/queries";
import { DashboardView } from "@/components/dashboard-view";

export const dynamic = "force-dynamic";

export default function DashboardPage() {
  const recentInvestments = listRecentInvestments(6);
  const sources = listSources();
  const runs = listRuns(1);

  return (
    <DashboardView
      recentInvestments={recentInvestments}
      sources={sources}
      latestRun={runs[0]}
    />
  );
}
