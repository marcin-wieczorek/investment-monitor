import { listDevelopers, listDeveloperCandidates } from "@/lib/queries";
import { DevelopersView } from "@/components/developers-view";

export const dynamic = "force-dynamic";

export default function DevelopersPage() {
  const developers = listDevelopers();
  const candidates = listDeveloperCandidates();
  return <DevelopersView developers={developers} candidates={candidates} />;
}
