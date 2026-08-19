import { listRuns } from "@/lib/queries";
import { HistoryView } from "@/components/history-view";

export const dynamic = "force-dynamic";

export default function HistoryPage() {
  const runs = listRuns(50);
  return <HistoryView runs={runs} />;
}
