export type ChangeType = "NEW" | "CHANGED" | "UNCHANGED";

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

export interface InvestmentFilters {
  developer?: string;
  location?: string;
  onlyNew?: boolean;
  includeArchived?: boolean;
}
