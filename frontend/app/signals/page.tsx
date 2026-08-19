import { listSignals } from "@/lib/queries";
import { SignalsView } from "@/components/signals-view";

export const dynamic = "force-dynamic";

export default function SignalsPage() {
  const signals = listSignals(300);
  return <SignalsView signals={signals} />;
}
