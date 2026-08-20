import { NextResponse } from "next/server";
import { triggerRescore } from "@/lib/rescore";

/**
 * Recomputes `investment_score` for every existing investment against the
 * currently stored scoring preferences, without a full live-source scan
 * (see `lib/rescore.ts`). Exposed as its own endpoint (in addition to
 * `PUT /api/preferences` triggering it automatically) so a manual
 * "recompute scores" action is possible without resaving preferences.
 */
export async function POST() {
  const result = await triggerRescore();
  return NextResponse.json(result, { status: result.ok ? 200 : 500 });
}
