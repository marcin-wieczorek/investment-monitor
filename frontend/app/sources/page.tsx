import { listSources } from "@/lib/queries";
import { SourcesView } from "@/components/sources-view";

export const dynamic = "force-dynamic";

export default function SourcesPage() {
  const sources = listSources();
  return <SourcesView sources={sources} />;
}
