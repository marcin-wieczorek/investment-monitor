import { getDb } from "@/lib/db";
import type {
  InvestmentFilters,
  InvestmentWithState,
  MonitoringRunRow,
  SourceSnapshotRow,
} from "@/lib/types";

const BASE_SELECT = `
  SELECT
    i.*,
    COALESCE(s.archived, 0) AS archived,
    s.reviewed_at AS reviewed_at,
    n.note AS note
  FROM investment i
  LEFT JOIN investment_state s ON s.investment_id = i.id
  LEFT JOIN user_note n ON n.investment_id = i.id
`;

export function listInvestments(filters: InvestmentFilters = {}): InvestmentWithState[] {
  const db = getDb();
  const clauses: string[] = [];
  const params: Record<string, string> = {};

  if (filters.developer) {
    clauses.push("i.developer = @developer");
    params.developer = filters.developer;
  }
  if (filters.location) {
    clauses.push("i.location LIKE @location");
    params.location = `%${filters.location}%`;
  }
  if (!filters.includeArchived) {
    clauses.push("COALESCE(s.archived, 0) = 0");
  }

  const where = clauses.length ? `WHERE ${clauses.join(" AND ")}` : "";
  const rows = db
    .prepare(`${BASE_SELECT} ${where} ORDER BY i.first_seen_at DESC`)
    .all(params) as unknown as InvestmentWithState[];

  return rows.map(normalizeRow);
}

export function getInvestment(id: number): InvestmentWithState | undefined {
  const db = getDb();
  const row = db.prepare(`${BASE_SELECT} WHERE i.id = ?`).get(id) as unknown as
    | InvestmentWithState
    | undefined;
  return row ? normalizeRow(row) : undefined;
}

export function countAllInvestments(): number {
  const db = getDb();
  const result = db.prepare("SELECT COUNT(*) AS count FROM investment").get() as unknown as {
    count: number;
  };
  return result.count;
}

export function countNewSince(sinceIso: string): number {
  const db = getDb();
  const result = db
    .prepare("SELECT COUNT(*) AS count FROM investment WHERE first_seen_at >= ?")
    .get(sinceIso) as unknown as { count: number };
  return result.count;
}

export function listRecentInvestments(limit = 5): InvestmentWithState[] {
  const db = getDb();
  const rows = db
    .prepare(`${BASE_SELECT} ORDER BY i.first_seen_at DESC LIMIT ?`)
    .all(limit) as unknown as InvestmentWithState[];
  return rows.map(normalizeRow);
}

export function listSources(): SourceSnapshotRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM source_snapshot ORDER BY source")
    .all() as unknown as SourceSnapshotRow[];
  return rows.map((row) => ({ ...row }));
}

export function listRuns(limit = 30): MonitoringRunRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM monitoring_run ORDER BY started_at DESC LIMIT ?")
    .all(limit) as unknown as MonitoringRunRow[];
  return rows.map((row) => ({ ...row }));
}

export function setNote(investmentId: number, note: string): void {
  const db = getDb();
  db.prepare(
    `INSERT INTO user_note (investment_id, note, updated_at)
     VALUES (@investmentId, @note, @updatedAt)
     ON CONFLICT(investment_id) DO UPDATE SET note = @note, updated_at = @updatedAt`
  ).run({ investmentId, note, updatedAt: new Date().toISOString() });
}

export function setArchived(investmentId: number, archived: boolean): void {
  const db = getDb();
  db.prepare(
    `INSERT INTO investment_state (investment_id, archived, reviewed_at)
     VALUES (@investmentId, @archived, @reviewedAt)
     ON CONFLICT(investment_id) DO UPDATE SET archived = @archived, reviewed_at = @reviewedAt`
  ).run({
    investmentId,
    archived: archived ? 1 : 0,
    reviewedAt: new Date().toISOString(),
  });
}
function normalizeRow(row: InvestmentWithState): InvestmentWithState {
  return { ...row, archived: Boolean(row.archived) };
}
