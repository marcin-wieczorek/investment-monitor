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
  reviewed_at: string | null;
  note: string | null;
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
}

export interface InvestmentFilters {
  developer?: string;
  location?: string;
  includeArchived?: boolean;
}
