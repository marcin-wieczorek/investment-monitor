import { listInvestments, listLocationSyntheses } from "@/lib/queries";
import { MapView } from "@/components/map-view";

export const dynamic = "force-dynamic";

export default function MapPage() {
  const investments = listInvestments({ includeArchived: false });
  const locationSyntheses = listLocationSyntheses();
  return <MapView investments={investments} locationSyntheses={locationSyntheses} />;
}
