import { listCorrelations } from "@/lib/queries";
import { CorrelationsView } from "@/components/correlations-view";

export const dynamic = "force-dynamic";

export default function CorrelationsPage() {
  const correlations = listCorrelations(300);
  return <CorrelationsView correlations={correlations} />;
}
