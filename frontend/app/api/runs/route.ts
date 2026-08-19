import { NextResponse } from "next/server";
import { listRuns } from "@/lib/queries";

export async function GET() {
  return NextResponse.json(listRuns());
}
