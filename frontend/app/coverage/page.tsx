import { listMunicipalities } from "@/lib/queries";
import { CoverageView } from "@/components/coverage-view";

export const dynamic = "force-dynamic";

export default function CoveragePage() {
  const municipalities = listMunicipalities();
  return <CoverageView municipalities={municipalities} />;
}
