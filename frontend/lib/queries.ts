import { getDb } from "@/lib/db";
import { DEFAULT_SCORING_PROFILE } from "@/lib/types";
import type {
  CorrelationRow,
  DeveloperCandidateRow,
  DeveloperRegistryRow,
  InvestmentDuplicateRow,
  InvestmentFilters,
  InvestmentSignalRow,
  InvestmentWithState,
  MonitoringRunRow,
  MunicipalityRegistryRow,
  ScoringProfile,
  SourceEvidenceRow,
  SourceSnapshotRow,
} from "@/lib/types";

const BASE_SELECT = `
  SELECT
    i.*,
    COALESCE(s.archived, 0) AS archived,
    COALESCE(s.watched, 0) AS watched,
    s.reviewed_at AS reviewed_at,
    n.note AS note,
    ss.source_category AS source_category,
    sc.overall_score AS overall_score,
    sc.property_type_match AS property_type_match,
    sc.location_tier_match AS location_tier_match,
    sc.house_area_score AS house_area_score,
    sc.plot_area_score AS plot_area_score,
    sc.price_score AS price_score,
    sc.large_plot_bonus AS large_plot_bonus,
    sc.plot_to_house_ratio AS plot_to_house_ratio
  FROM investment i
  LEFT JOIN investment_state s ON s.investment_id = i.id
  LEFT JOIN user_note n ON n.investment_id = i.id
  LEFT JOIN source_snapshot ss ON ss.source = i.source
  LEFT JOIN investment_score sc ON sc.investment_canonical_key = i.canonical_key
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
  if (filters.aggregatorOnly) {
    clauses.push("COALESCE(i.aggregator_only_discovery, 0) = 1");
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

export function countAggregatorOnlyDiscoveries(): number {
  const db = getDb();
  const result = db
    .prepare("SELECT COUNT(*) AS count FROM investment WHERE aggregator_only_discovery = 1")
    .get() as unknown as { count: number };
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

export function setWatched(investmentId: number, watched: boolean): void {
  const db = getDb();
  db.prepare(
    `INSERT INTO investment_state (investment_id, watched)
     VALUES (@investmentId, @watched)
     ON CONFLICT(investment_id) DO UPDATE SET watched = @watched`
  ).run({
    investmentId,
    watched: watched ? 1 : 0,
  });
}

function normalizeRow(row: InvestmentWithState): InvestmentWithState {
  return {
    ...row,
    archived: Boolean(row.archived),
    watched: Boolean(row.watched),
    property_type_match: row.property_type_match == null ? null : Boolean(row.property_type_match),
    location_tier_match: row.location_tier_match == null ? null : Boolean(row.location_tier_match),
    large_plot_bonus: row.large_plot_bonus == null ? null : Boolean(row.large_plot_bonus),
    aggregator_only_discovery: Boolean(row.aggregator_only_discovery),
  };
}

export function listSignals(limit = 200): InvestmentSignalRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM investment_signal ORDER BY detected_at DESC LIMIT ?")
    .all(limit) as unknown as InvestmentSignalRow[];
  return rows.map((row) => ({ ...row }));
}

export function getSignal(id: number): InvestmentSignalRow | undefined {
  const db = getDb();
  const row = db.prepare("SELECT * FROM investment_signal WHERE id = ?").get(id) as unknown as
    | InvestmentSignalRow
    | undefined;
  return row ? { ...row } : undefined;
}

export function countAllSignals(): number {
  const db = getDb();
  const result = db.prepare("SELECT COUNT(*) AS count FROM investment_signal").get() as unknown as {
    count: number;
  };
  return result.count;
}

export function listEvidenceForInvestment(investmentId: number): SourceEvidenceRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM source_evidence WHERE investment_id = ? ORDER BY captured_at DESC")
    .all(investmentId) as unknown as SourceEvidenceRow[];
  return rows.map((row) => ({ ...row }));
}

export function listEvidenceForSignal(signalId: number): SourceEvidenceRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM source_evidence WHERE signal_id = ? ORDER BY captured_at DESC")
    .all(signalId) as unknown as SourceEvidenceRow[];
  return rows.map((row) => ({ ...row }));
}

const CORRELATION_SELECT = `
  SELECT
    c.*,
    i.name AS investment_name,
    i.first_seen_at AS investment_first_seen,
    s.title AS signal_title,
    s.first_seen_at AS signal_first_seen,
    CAST(julianday(i.first_seen_at) - julianday(s.first_seen_at) AS INTEGER) AS lead_time_days
  FROM correlation c
  JOIN investment i ON i.id = c.investment_id
  JOIN investment_signal s ON s.id = c.signal_id
`;

export function listCorrelations(limit = 200): CorrelationRow[] {
  const db = getDb();
  const rows = db
    .prepare(`${CORRELATION_SELECT} ORDER BY c.created_at DESC LIMIT ?`)
    .all(limit) as unknown as CorrelationRow[];
  return rows.map((row) => ({ ...row }));
}

export function listCorrelationsForInvestment(investmentId: number): CorrelationRow[] {
  const db = getDb();
  const rows = db
    .prepare(`${CORRELATION_SELECT} WHERE c.investment_id = ? ORDER BY c.created_at DESC`)
    .all(investmentId) as unknown as CorrelationRow[];
  return rows.map((row) => ({ ...row }));
}

/** Average discovery lead time in days across every correlation - the core "early detection" KPI. */
export function averageDiscoveryLeadTime(): number | null {
  const db = getDb();
  const result = db
    .prepare(
      `SELECT AVG(CAST(julianday(i.first_seen_at) - julianday(s.first_seen_at) AS REAL)) AS avg_days
       FROM correlation c
       JOIN investment i ON i.id = c.investment_id
       JOIN investment_signal s ON s.id = c.signal_id`
    )
    .get() as unknown as { avg_days: number | null };
  return result.avg_days;
}

export function listDevelopers(): DeveloperRegistryRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM developer_registry ORDER BY tier, name")
    .all() as unknown as DeveloperRegistryRow[];
  return rows.map((row) => ({ ...row }));
}

export function listDeveloperCandidates(): DeveloperCandidateRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM developer_candidate ORDER BY discovered_at DESC")
    .all() as unknown as DeveloperCandidateRow[];
  return rows.map((row) => ({ ...row }));
}

const CANDIDATE_STATUSES = ["ACCEPTED", "REJECTED", "IMPLEMENTED", "BLOCKED"] as const;
export type DeveloperCandidateMutableStatus = (typeof CANDIDATE_STATUSES)[number];

export function isDeveloperCandidateMutableStatus(value: unknown): value is DeveloperCandidateMutableStatus {
  return typeof value === "string" && (CANDIDATE_STATUSES as readonly string[]).includes(value);
}

/**
 * Reviewer decision on a discovered developer candidate (see
 * `DeveloperCandidateRepository.updateStatus` on the Kotlin side, which
 * this mirrors). `NEW`/`REVIEW_REQUIRED` are scan-assigned only - a human
 * reviewer can only move a candidate to one of `CANDIDATE_STATUSES`.
 */
export function setDeveloperCandidateStatus(id: number, status: DeveloperCandidateMutableStatus): void {
  const db = getDb();
  db.prepare("UPDATE developer_candidate SET status = @status WHERE id = @id").run({ id, status });
}

export function listMunicipalities(): MunicipalityRegistryRow[] {
  const db = getDb();
  const rows = db
    .prepare("SELECT * FROM municipality_registry ORDER BY name")
    .all() as unknown as MunicipalityRegistryRow[];
  return rows.map((row) => ({ ...row }));
}

const DUPLICATE_SELECT = `
  SELECT
    d.*,
    a.name AS investment_name_a,
    a.source AS investment_source_a,
    b.name AS investment_name_b,
    b.source AS investment_source_b
  FROM investment_duplicate d
  JOIN investment a ON a.id = d.investment_id_a
  JOIN investment b ON b.id = d.investment_id_b
`;

/**
 * All deterministic cross-source duplicate links (see
 * InvestmentDeduplicator on the Kotlin side). Consumers group investments
 * client-side using HIGH/MEDIUM confidence pairs, the same pattern
 * `signals-view.tsx` uses to group signal case history by
 * `source:reference` - see `groupInvestmentClusters` in
 * `components/investments-view.tsx`.
 */
export function listInvestmentDuplicates(): InvestmentDuplicateRow[] {
  const db = getDb();
  const rows = db.prepare(DUPLICATE_SELECT).all() as unknown as InvestmentDuplicateRow[];
  return rows.map((row) => ({ ...row }));
}

export function listDuplicatesForInvestment(investmentId: number): InvestmentDuplicateRow[] {
  const db = getDb();
  const rows = db
    .prepare(`${DUPLICATE_SELECT} WHERE d.investment_id_a = ? OR d.investment_id_b = ?`)
    .all(investmentId, investmentId) as unknown as InvestmentDuplicateRow[];
  return rows.map((row) => ({ ...row }));
}

const SCORING_PROFILE_KEY = "scoring.profile";

/**
 * Reads the current scoring profile from `user_preferences` (see
 * `UserPreferencesRepository` on the Kotlin side, which this mirrors) -
 * falls back to [DEFAULT_SCORING_PROFILE] when nothing has been saved yet,
 * same rationale as `UserPreferencesRepository.effectiveScoringProfile()`.
 */
export function getScoringPreferences(): ScoringProfile {
  const db = getDb();
  const row = db
    .prepare("SELECT value FROM user_preferences WHERE key = ?")
    .get(SCORING_PROFILE_KEY) as unknown as { value: string } | undefined;
  if (!row) return DEFAULT_SCORING_PROFILE;
  return JSON.parse(row.value) as ScoringProfile;
}

export function saveScoringPreferences(profile: ScoringProfile): void {
  const db = getDb();
  db.prepare(
    `INSERT INTO user_preferences (key, value, updated_at)
     VALUES (@key, @value, @updatedAt)
     ON CONFLICT(key) DO UPDATE SET value = @value, updated_at = @updatedAt`
  ).run({
    key: SCORING_PROFILE_KEY,
    value: JSON.stringify(profile),
    updatedAt: new Date().toISOString(),
  });
}
