import {
  averageDiscoveryLeadTime,
  countAllInvestments,
  countAllSignals,
  listDevelopers,
  listMunicipalities,
  listRecentInvestments,
  listRuns,
  listSources,
} from "@/lib/queries";
import { DashboardView } from "@/components/dashboard-view";

export const dynamic = "force-dynamic";

export default function DashboardPage() {
  const recentInvestments = listRecentInvestments(6);
  const sources = listSources();
  const runs = listRuns(30);
  const totalInvestments = countAllInvestments();
  const totalSignals = countAllSignals();
  const developers = listDevelopers();
  const municipalities = listMunicipalities();
  const avgLeadTimeDays = averageDiscoveryLeadTime();

  return (
    <DashboardView
      recentInvestments={recentInvestments}
      sources={sources}
      runs={runs}
      totalInvestments={totalInvestments}
      totalSignals={totalSignals}
      developers={developers}
      municipalities={municipalities}
      avgLeadTimeDays={avgLeadTimeDays}
    />
  );
}
