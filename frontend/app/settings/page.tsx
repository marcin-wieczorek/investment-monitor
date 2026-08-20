import { getScoringPreferences } from "@/lib/queries";
import { SettingsView } from "@/components/settings-view";

export const dynamic = "force-dynamic";

export default function SettingsPage() {
  const profile = getScoringPreferences();
  return <SettingsView initialProfile={profile} />;
}
