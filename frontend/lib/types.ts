export interface InvestmentRow {
  id: number;
  source: string;
  canonical_key: string;
  developer: string;
  name: string;
  url: string;
  location: string | null;
  property_type: string | null;
  units: number | null;
  house_area_min: number | null;
  house_area_max: number | null;
  plot_area_min: number | null;
  plot_area_max: number | null;
  price_min: number | null;
  price_max: number | null;
  status: string | null;
  image_url: string | null;
  first_seen_at: string;
  last_seen_at: string;
}

export interface InvestmentWithState extends InvestmentRow {
  archived: boolean;
  watched: boolean;
  reviewed_at: string | null;
  note: string | null;
  source_category: "DEVELOPER" | "DISCOVERY" | "AGGREGATOR" | null;
  overall_score: number | null;
  property_type_match: boolean | null;
  location_tier_match: boolean | null;
  house_area_score: number | null;
  plot_area_score: number | null;
  price_score: number | null;
  large_plot_bonus: boolean | null;
  plot_to_house_ratio: number | null;
}

export interface SourceSnapshotRow {
  source: string;
  captured_at: string;
  investment_count: number;
  content_hash: string;
  source_category: "DEVELOPER" | "DISCOVERY" | "AGGREGATOR";
}

export interface MonitoringRunRow {
  id: number;
  started_at: string;
  finished_at: string | null;
  status: string;
  sources_checked: number;
  sources_failed: number;
  new_investments: number;
}

export interface InvestmentSignalRow {
  id: number;
  source: string;
  canonical_key: string;
  municipality: string;
  location: string | null;
  signal_type: string;
  title: string;
  reference: string | null;
  detected_at: string;
  url: string;
  raw_facts: string | null;
  first_seen_at: string;
  last_seen_at: string;
}

export interface SourceEvidenceRow {
  id: number;
  investment_id: number | null;
  signal_id: number | null;
  source_id: string;
  source_category: "DEVELOPER" | "DISCOVERY" | "AGGREGATOR";
  captured_at: string;
  url: string;
  extraction_method: "PARSER" | "LLM" | "MANUAL";
  field_name: string;
  field_value: string;
}

export interface CorrelationRow {
  id: number;
  investment_id: number;
  signal_id: number;
  confidence: "HIGH" | "MEDIUM" | "LOW";
  matched_features: string;
  reason: string;
  created_at: string;
  // Joined for display convenience.
  investment_name?: string;
  signal_title?: string;
  investment_first_seen?: string;
  signal_first_seen?: string;
  lead_time_days?: number | null;
}

export interface InvestmentFilters {
  developer?: string;
  location?: string;
  includeArchived?: boolean;
}

export interface DeveloperRegistryRow {
  id: string;
  name: string;
  website: string | null;
  investment_list_urls: string;
  tier: "A" | "B" | "DISCOVERED";
  status:
    | "MONITORED"
    | "DISCOVERED"
    | "CANDIDATE"
    | "NO_CURRENT_INVESTMENTS"
    | "INACTIVE"
    | "BLOCKED";
  geographic_scope: string;
  adapter_source_id: string | null;
}

export interface DeveloperCandidateRow {
  id: number;
  developer_name: string;
  discovered_url: string;
  municipality: string | null;
  discovered_from_source: string;
  discovered_at: string;
  status: "NEW" | "REVIEW_REQUIRED" | "ACCEPTED" | "REJECTED" | "IMPLEMENTED" | "BLOCKED";
  evidence: string | null;
}

export interface MunicipalityRegistryRow {
  id: string;
  name: string;
  powiat: string;
  developer_coverage: "IMPLEMENTED" | "NOT_IMPLEMENTED" | "BLOCKED" | "DISABLED";
  discovery_coverage: "IMPLEMENTED" | "NOT_IMPLEMENTED" | "BLOCKED" | "DISABLED";
  aggregator_coverage: "IMPLEMENTED" | "NOT_IMPLEMENTED" | "BLOCKED" | "DISABLED";
}
