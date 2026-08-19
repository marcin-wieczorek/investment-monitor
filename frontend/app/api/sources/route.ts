import { NextResponse } from "next/server";
import { listSources } from "@/lib/queries";

export async function GET() {
  return NextResponse.json(listSources());
}
