import { listInvestments } from "@/lib/queries";
import { MapView } from "@/components/map-view";

export const dynamic = "force-dynamic";

export default function MapPage() {
  const investments = listInvestments({ includeArchived: false });
  return <MapView investments={investments} />;
}
