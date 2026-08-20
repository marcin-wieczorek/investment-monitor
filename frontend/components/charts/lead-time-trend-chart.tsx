"use client";

import dynamic from "next/dynamic";
import type { ApexOptions } from "apexcharts";
import { useI18n } from "@/lib/i18n";
import type { CorrelationRow } from "@/lib/types";

const ReactApexChart = dynamic(() => import("react-apexcharts"), { ssr: false });

interface LeadTimeTrendChartProps {
  /** Correlations ordered most-recent-first, as returned by listCorrelations(). */
  correlations: CorrelationRow[];
}

export function LeadTimeTrendChart({ correlations }: LeadTimeTrendChartProps) {
  const { t, locale } = useI18n();
  const withLeadTime = correlations.filter(
    (c): c is CorrelationRow & { lead_time_days: number } => c.lead_time_days != null
  );
  const chronological = [...withLeadTime].reverse();

  const categories = chronological.map((c) =>
    new Date(c.created_at).toLocaleDateString(locale, { day: "2-digit", month: "short" })
  );
  const series = [
    {
      name: t("dashboard.avgLeadTime"),
      data: chronological.map((c) => c.lead_time_days),
    },
  ];

  const options: ApexOptions = {
    chart: {
      type: "line",
      height: 260,
      fontFamily: "var(--font-geist-sans), sans-serif",
      toolbar: { show: false },
    },
    colors: ["#8b5cf6"],
    stroke: { curve: "smooth", width: 2 },
    markers: { size: 3 },
    dataLabels: { enabled: false },
    grid: {
      xaxis: { lines: { show: false } },
      yaxis: { lines: { show: true } },
    },
    xaxis: {
      categories,
      axisBorder: { show: false },
      axisTicks: { show: false },
    },
    yaxis: {
      labels: { formatter: (value) => Math.round(value).toString() },
      title: { text: t("correlations.leadTimeDays") },
    },
    annotations: {
      yaxis: [
        {
          y: 0,
          borderColor: "#94a3b8",
          strokeDashArray: 4,
        },
      ],
    },
    tooltip: { theme: "light" },
  };

  if (chronological.length === 0) {
    return (
      <p className="flex h-64 items-center justify-center text-sm text-muted-foreground">
        {t("dashboard.noData")}
      </p>
    );
  }

  return <ReactApexChart options={options} series={series} type="line" height={260} />;
}
