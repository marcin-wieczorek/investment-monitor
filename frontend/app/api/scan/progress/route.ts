import { NextResponse } from "next/server";
import { scanState } from "@/lib/scan-state";

export const dynamic = "force-dynamic";

export async function GET() {
  return NextResponse.json(scanState);
}
