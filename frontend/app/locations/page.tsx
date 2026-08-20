import { getHotspotSynthesis, listLocationSyntheses } from "@/lib/queries";
import { LocationsView } from "@/components/locations-view";

export const dynamic = "force-dynamic";

export default function LocationsPage() {
  const locationSyntheses = listLocationSyntheses();
  const hotspotSynthesis = getHotspotSynthesis();
  return <LocationsView locationSyntheses={locationSyntheses} hotspotSynthesis={hotspotSynthesis} />;
}
