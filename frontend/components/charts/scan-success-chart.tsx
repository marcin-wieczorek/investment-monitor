"use client";

import dynamic from "next/dynamic";
import type { ApexOptions } from "apexcharts";
import { useI18n } from "@/lib/i18n";
import type { MonitoringRunRow } from "@/lib/types";

const ReactApexChart = dynamic(() => import("react-apexcharts"), { ssr: false });

interface ScanSuccessChartProps {
  /** Runs ordered most-recent-first, as returned by listRuns(). */
  runs: MonitoringRunRow[];
}

export function ScanSuccessChart({ runs }: ScanSuccessChartProps) {
  const { t, locale } = useI18n();
  const chronological = [...runs].reverse();

  const categories = chronological.map((run) =>
    new Date(run.started_at).toLocaleDateString(locale, { day: "2-digit", month: "short" })
  );

  const series = [
    {
      name: t("history.sourcesChecked"),
      data: chronological.map((run) => run.sources_checked - run.sources_failed),
    },
    {
      name: t("history.sourcesFailed"),
      data: chronological.map((run) => run.sources_failed),
    },
  ];

  const options: ApexOptions = {
    chart: {
      type: "bar",
      height: 260,
      fontFamily: "var(--font-geist-sans), sans-serif",
      stacked: true,
      toolbar: { show: false },
    },
    colors: ["#3b82f6", "#f43f5e"],
    plotOptions: {
      bar: { columnWidth: "45%", borderRadius: 4, borderRadiusApplication: "end" },
    },
    dataLabels: { enabled: false },
    legend: { position: "top", horizontalAlign: "left" },
    grid: {
      yaxis: { lines: { show: true } },
    },
    xaxis: {
      categories,
      axisBorder: { show: false },
      axisTicks: { show: false },
    },
    yaxis: {
      labels: { formatter: (value) => Math.round(value).toString() },
    },
    tooltip: { theme: "light" },
  };

  if (chronological.length === 0) {
    return (
      <p className="flex h-64 items-center justify-center text-sm text-muted-foreground">
        {t("history.noRuns")}
      </p>
    );
  }

  return <ReactApexChart options={options} series={series} type="bar" height={260} />;
}
