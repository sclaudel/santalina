import { useState, useEffect, useCallback } from 'react';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import dayjs from 'dayjs';
import { statsService } from '../services/statsService';
import type { MyStatsResponse, PeriodStat, GroupStat } from '../types';
import {
  drawPdfFillRate, drawPdfPropBars, drawPdfLineChart,
  drawPdfGroupedBars, ensureSpace, drawSectionTitle,
} from '../utils/pdfCharts';

/** Convertit "yyyy-MM" en "MM/yyyy" pour l'affichage. Laisse les autres valeurs inchangées. */
function fmtMonthLabel(label: string): string {
  const m = label.match(/^(\d{4})-(\d{2})$/);
  return m ? `${m[2]}/${m[1]}` : label;
}

// ---- Couleurs communes ----
const PIE_COLORS = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
  '#06b6d4', '#f97316', '#84cc16', '#ec4899', '#6366f1',
];

// ---- Camembert SVG ----
function PieChart({ data, valueKey }: { data: GroupStat[]; valueKey: 'slots' | 'divers' }) {
  const total = data.reduce((s, d) => s + d[valueKey], 0);
  if (total === 0) return <p className="stats-empty">Aucune donnée</p>;

  const size = 180;
  const cx = size / 2;
  const cy = size / 2;
  const r = 70;
  const innerR = 38;

  let angle = -Math.PI / 2;
  const slices = data.map((d, i) => {
    const pct = d[valueKey] / total;
    const start = angle;
    angle += pct * 2 * Math.PI;
    const end = angle;

    let path: string;
    let isFullCircle = false;
    if (pct >= 0.9999) {
      isFullCircle = true;
      path =
        `M ${cx - r} ${cy} A ${r} ${r} 0 1 1 ${cx + r} ${cy} A ${r} ${r} 0 1 1 ${cx - r} ${cy} Z` +
        ` M ${cx - innerR} ${cy} A ${innerR} ${innerR} 0 1 0 ${cx + innerR} ${cy} A ${innerR} ${innerR} 0 1 0 ${cx - innerR} ${cy} Z`;
    } else {
      const x1 = cx + r * Math.cos(start);
      const y1 = cy + r * Math.sin(start);
      const x2 = cx + r * Math.cos(end);
      const y2 = cy + r * Math.sin(end);
      const xi1 = cx + innerR * Math.cos(start);
      const yi1 = cy + innerR * Math.sin(start);
      const xi2 = cx + innerR * Math.cos(end);
      const yi2 = cy + innerR * Math.sin(end);
      const large = pct > 0.5 ? 1 : 0;
      path = `M ${xi1} ${yi1} L ${x1} ${y1} A ${r} ${r} 0 ${large} 1 ${x2} ${y2} L ${xi2} ${yi2} A ${innerR} ${innerR} 0 ${large} 0 ${xi1} ${yi1} Z`;
    }
    return { path, isFullCircle, color: PIE_COLORS[i % PIE_COLORS.length], label: d.label, value: d[valueKey], pct };
  });

  return (
    <div className="pie-wrapper">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ display: 'block', margin: '0 auto' }}>
        {slices.map((s, i) => (
          <path key={i} d={s.path} fill={s.color} stroke="#fff" strokeWidth={1.5}
            fillRule={s.isFullCircle ? 'evenodd' : undefined}>
            <title>{s.label} : {s.value} ({(s.pct * 100).toFixed(1)}%)</title>
          </path>
        ))}
        <text x={cx} y={cy - 6} textAnchor="middle" fontSize={11} fill="#6b7280">Total</text>
        <text x={cx} y={cy + 10} textAnchor="middle" fontSize={16} fontWeight={700} fill="#111827">{total}</text>
      </svg>
      <div className="pie-legend">
        {slices.map((s, i) => (
          <div key={i} className="pie-legend-item">
            <span className="pie-dot" style={{ background: s.color }} />
            <span className="pie-legend-label" title={s.label}>{s.label}</span>
            <span className="pie-legend-value">{s.value} ({(s.pct * 100).toFixed(1)}%)</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ---- Barre horizontale ----
function BarChart({ data, max }: { data: PeriodStat[]; max: number }) {
  if (data.length === 0) return <p className="stats-empty">Aucune donnée</p>;
  return (
    <div className="bar-chart">
      {data.map(d => (
        <div key={d.label} className="bar-row">
          <span className="bar-label">{d.label}</span>
          <div className="bar-track">
            <div
              className="bar-fill bar-fill-divers"
              style={{ width: max ? `${(d.divers / max) * 100}%` : '0%' }}
              title={`${d.divers} plongées`}
            />
            <div
              className="bar-fill bar-fill-slots"
              style={{ width: max ? `${(d.slots / max) * 100}%` : '0%' }}
              title={`${d.slots} créneaux`}
            />
          </div>
          <span className="bar-values">{d.divers} plg. / {d.slots} cr.</span>
        </div>
      ))}
      <div className="bar-legend">
        <span><span className="bar-dot bar-dot-divers" /> Plongées</span>
        <span><span className="bar-dot bar-dot-slots" /> Créneaux</span>
      </div>
    </div>
  );
}

// ---- Tableau générique ----
function StatsTable({ data, cols }: {
  data: (PeriodStat | GroupStat)[];
  cols: { key: keyof (PeriodStat & GroupStat); label: string }[];
}) {
  if (data.length === 0) return <p className="stats-empty">Aucune donnée</p>;
  return (
    <div className="stats-table-wrapper">
      <table className="stats-table">
        <thead>
          <tr>{cols.map(c => <th key={c.key}>{c.label}</th>)}</tr>
        </thead>
        <tbody>
          {data.map((row, i) => (
            <tr key={i}>
              {cols.map(c => <td key={c.key}>{row[c.key]}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ── Graphique en courbes SVG ──────────────────────────────────────────────────
function LineChart({ data }: { data: PeriodStat[] }) {
  if (data.length === 0) return <p className="stats-empty">Aucune donnée</p>;

  const W = 520; const H = 200;
  const PL = 44; const PR = 16; const PT = 16; const PB = 38;
  const innerW = W - PL - PR;
  const innerH = H - PT - PB;

  const maxVal = Math.max(1, ...data.map(d => Math.max(d.slots, d.divers)));
  const n = data.length;
  const toX = (i: number) => PL + (n === 1 ? innerW / 2 : i * (innerW / (n - 1)));
  const toY = (v: number) => PT + innerH - (v / maxVal) * innerH;

  const diversPts = data.map((d, i) => `${toX(i)},${toY(d.divers)}`);
  const slotsPts  = data.map((d, i) => `${toX(i)},${toY(d.slots)}`);
  const toPolyline = (pts: string[]) => pts.join(' ');
  const toArea = (pts: string[]) => {
    const y0 = PT + innerH;
    const lastX = pts[pts.length - 1].split(',')[0];
    return `M ${PL},${y0} L ${pts.join(' L ')} L ${lastX},${y0} Z`;
  };
  const ticks = [0, 0.25, 0.5, 0.75, 1].map(f => ({ v: Math.round(maxVal * f), y: toY(maxVal * f) }));
  const labelStep = Math.max(1, Math.ceil(n / 10));

  return (
    <div className="line-chart-wrapper">
      <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto', display: 'block', overflow: 'visible' }}>
        {ticks.map((t, i) => <line key={i} x1={PL} y1={t.y} x2={W - PR} y2={t.y} stroke="#e5e7eb" strokeWidth={1} />)}
        {ticks.map((t, i) => <text key={i} x={PL - 6} y={t.y + 4} textAnchor="end" fontSize={9} fill="#9ca3af">{t.v}</text>)}
        <path d={toArea(slotsPts)}  fill="#3b82f6" fillOpacity={0.08} />
        <path d={toArea(diversPts)} fill="#10b981" fillOpacity={0.13} />
        <polyline points={toPolyline(slotsPts)}  fill="none" stroke="#3b82f6" strokeWidth={2}   strokeLinecap="round" strokeLinejoin="round" />
        <polyline points={toPolyline(diversPts)} fill="none" stroke="#10b981" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" />
        {data.map((d, i) => (
          <g key={i}>
            <circle cx={toX(i)} cy={toY(d.slots)}  r={n <= 24 ? 3 : 2}   fill="#3b82f6"><title>{d.label} — {d.slots} créneau{d.slots !== 1 ? 'x' : ''}</title></circle>
            <circle cx={toX(i)} cy={toY(d.divers)} r={n <= 24 ? 3.5 : 2.5} fill="#10b981"><title>{d.label} — {d.divers} plongée{d.divers !== 1 ? 's' : ''}</title></circle>
          </g>
        ))}
        {data.map((d, i) => i % labelStep === 0 && (
          <text key={i} x={toX(i)} y={H - 4} textAnchor="middle" fontSize={9} fill="#6b7280"
            transform={n > 8 ? `rotate(-35, ${toX(i)}, ${H - 4})` : undefined}>{d.label}</text>
        ))}
        <line x1={PL} y1={PT}          x2={PL}     y2={PT + innerH} stroke="#d1d5db" strokeWidth={1} />
        <line x1={PL} y1={PT + innerH} x2={W - PR} y2={PT + innerH} stroke="#d1d5db" strokeWidth={1} />
      </svg>
      <div className="bar-legend">
        <span><span className="bar-dot" style={{ background: '#10b981' }} /> Plongées</span>
        <span><span className="bar-dot" style={{ background: '#3b82f6' }} /> Créneaux</span>
      </div>
    </div>
  );
}

// ── Jauge arc SVG ─────────────────────────────────────────────────────────────
function GaugeChart({ rate }: { rate: number }) {
  const pct   = Math.min(Math.max(rate, 0), 100);
  const color = pct >= 70 ? '#10b981' : pct >= 40 ? '#f59e0b' : '#ef4444';
  const cx = 80; const cy = 80; const r = 60;
  const startAngle = Math.PI * 0.75;
  const endAngle   = Math.PI * 2.25;
  const filled     = startAngle + (pct / 100) * (endAngle - startAngle);
  const toX = (a: number) => cx + r * Math.cos(a);
  const toY = (a: number) => cy + r * Math.sin(a);
  const arcPath = (from: number, to: number, stroke: string, width: number) => {
    const large = (to - from) > Math.PI ? 1 : 0;
    return (
      <path
        d={`M ${toX(from)} ${toY(from)} A ${r} ${r} 0 ${large} 1 ${toX(to)} ${toY(to)}`}
        fill="none" stroke={stroke} strokeWidth={width} strokeLinecap="round"
      />
    );
  };
  return (
    <svg width={160} height={110} viewBox="0 0 160 110" style={{ display: 'block', margin: '0 auto' }}>
      {arcPath(startAngle, endAngle, '#e5e7eb', 12)}
      {pct > 0 && arcPath(startAngle, filled, color, 12)}
      <text x={cx} y={cy + 14} textAnchor="middle" fontSize={22} fontWeight={700} fill={color}>{pct.toFixed(1)}%</text>
      <text x={cx} y={cy + 30} textAnchor="middle" fontSize={9} fill="#6b7280">Taux de remplissage</text>
      <text x={cx - r + 4} y={cy + 24} textAnchor="middle" fontSize={8} fill="#9ca3af">0%</text>
      <text x={cx + r - 4} y={cy + 24} textAnchor="middle" fontSize={8} fill="#9ca3af">100%</text>
    </svg>
  );
}

// ── Export PDF ────────────────────────────────────────────────────────────────
const MONTHS_FR_FULL = [
  'Janvier','Février','Mars','Avril','Mai','Juin',
  'Juillet','Août','Septembre','Octobre','Novembre','Décembre',
];

function exportMyStatsToPdf(stats: MyStatsResponse, filterYear: string, filterMonth: string, userName: string) {
  const doc    = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
  const pageW  = doc.internal.pageSize.getWidth();
  const M      = 15;
  const usable = pageW - 2 * M;
  let y        = 18;

  const periodeLabel = filterYear !== 'all'
    ? (filterMonth !== 'all'
        ? `${MONTHS_FR_FULL[parseInt(filterMonth) - 1]} ${filterYear}`
        : String(filterYear))
    : 'Toutes les périodes';

  // ── En-tête ────────────────────────────────────────────────────────────────
  doc.setFontSize(18); doc.setFont('helvetica', 'bold'); doc.setTextColor(30, 64, 175);
  doc.text(`Mes statistiques${userName ? ` — ${userName}` : ''}`, M, y);
  y += 7;
  doc.setFontSize(9); doc.setFont('helvetica', 'normal'); doc.setTextColor(100);
  doc.text(`Période : ${periodeLabel}`, M, y);
  doc.text(`Exporté le ${dayjs().format('DD/MM/YYYY à HH:mm')}`, pageW - M, y, { align: 'right' });
  y += 4;
  doc.setDrawColor(200); doc.line(M, y, pageW - M, y); doc.setDrawColor(0);
  y += 7;

  // ── KPIs ───────────────────────────────────────────────────────────────────
  doc.setTextColor(0); doc.setFontSize(12); doc.setFont('helvetica', 'bold');
  doc.text('Indicateurs clés', M, y); y += 3;

  const kpiRows: (string | number)[][] = [
    ['Créneaux dirigés',         stats.totalSlots],
    ['Plongeurs encadrés',       stats.totalDivers],
    ['Capacité totale',          stats.totalCapacity],
    ['Jours de plongée',         stats.totalUniqueDays],
    ['Moy. plongeurs / créneau', stats.avgDiversPerSlot.toFixed(1)],
  ];
  if (stats.bestDayDate)    kpiRows.push(['Meilleure journée', `${dayjs(stats.bestDayDate).format('DD/MM/YYYY')} (${stats.bestDayDivers} plg.)`]);
  if (stats.bestMonthLabel) kpiRows.push(['Meilleur mois',     `${stats.bestMonthLabel} (${stats.bestMonthDivers} plg.)`]);

  autoTable(doc, {
    startY: y, head: [['Indicateur', 'Valeur']], body: kpiRows.map(r => r.map(String)),
    margin: { left: M, right: M }, styles: { fontSize: 9 },
    headStyles: { fillColor: [30, 64, 175] },
    columnStyles: { 1: { halign: 'right' as const, fontStyle: 'bold' as const } },
  });
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  y = (doc as any).lastAutoTable.finalY + 5;

  // Taux de remplissage (barre graphique)
  if (stats.totalCapacity > 0) {
    doc.setFontSize(8); doc.setFont('helvetica', 'normal'); doc.setTextColor(80, 80, 80);
    doc.text('Taux de remplissage :', M, y + 5.5);
    y = drawPdfFillRate(doc, stats.fillRate, M + 42, y, usable - 55);
  }
  y += 4;

  // ── Évolution temporelle ──────────────────────────────────────────────────
  if (filterMonth === 'all') {
    const evolData = filterYear !== 'all' ? stats.byMonth : stats.byYear;
    if (evolData.length > 0) {
      const chartH = 55;
      y = ensureSpace(doc, y, chartH + 30);
      y = drawSectionTitle(doc, `Évolution ${filterYear !== 'all' ? 'mensuelle' : 'annuelle'}`, M, y, usable);
      y = drawPdfLineChart(doc, evolData, M, y, usable, chartH);
      y += 4;
    }
  }

  // ── Par club ───────────────────────────────────────────────────────────────
  if (stats.byClub.length > 0) {
    const needed = stats.byClub.length * 11 + 30;
    y = ensureSpace(doc, y, needed);
    y = drawSectionTitle(doc, 'Par club', M, y, usable);
    const halfW = (usable - 8) / 2;
    doc.setFontSize(8); doc.setFont('helvetica', 'bold'); doc.setTextColor(60, 60, 60);
    doc.text('Plongées', M, y); doc.text('Créneaux', M + halfW + 8, y);
    y += 4;
    const yAfter = drawPdfPropBars(doc, stats.byClub, 'divers', M, y, halfW);
    drawPdfPropBars(doc, stats.byClub, 'slots', M + halfW + 8, y, halfW);
    y = yAfter + 2;
  }

  // ── Par type ───────────────────────────────────────────────────────────────
  if (stats.byType.length > 0) {
    const needed = stats.byType.length * 11 + 30;
    y = ensureSpace(doc, y, needed);
    y = drawSectionTitle(doc, 'Par type de créneau', M, y, usable);
    const halfW = (usable - 8) / 2;
    doc.setFontSize(8); doc.setFont('helvetica', 'bold'); doc.setTextColor(60, 60, 60);
    doc.text('Plongées', M, y); doc.text('Créneaux', M + halfW + 8, y);
    y += 4;
    const yAfter = drawPdfPropBars(doc, stats.byType, 'divers', M, y, halfW);
    drawPdfPropBars(doc, stats.byType, 'slots', M + halfW + 8, y, halfW);
    y = yAfter + 2;
  }

  // ── Par jour de la semaine ─────────────────────────────────────────────────
  {
    const needed = stats.byDayOfWeek.length * 12 + 30;
    y = ensureSpace(doc, y, needed);
    y = drawSectionTitle(doc, 'Par jour de la semaine', M, y, usable);
    y = drawPdfGroupedBars(doc, stats.byDayOfWeek, M, y, usable);
    y += 4;
  }

  // ── Par niveau ─────────────────────────────────────────────────────────────
  if (stats.byLevel.length > 0) {
    const needed = stats.byLevel.length * 11 + 30;
    y = ensureSpace(doc, y, needed);
    y = drawSectionTitle(doc, 'Par niveau de plongeur', M, y, usable);
    y = drawPdfPropBars(doc, stats.byLevel, 'divers', M, y, usable);
    y += 4;
  }

  void y;
  doc.save(`mes-stats-${filterYear !== 'all' ? filterYear : 'global'}-${dayjs().format('YYYYMMDD')}.pdf`);
}
const CURRENT_YEAR = dayjs().year();
const YEARS = Array.from({ length: 5 }, (_, i) => CURRENT_YEAR - i);
const MONTHS_FR = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];

export function MyStatsPage() {
  const [stats, setStats]     = useState<MyStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');

  const [filterYear, setFilterYear]   = useState<string>('all');
  const [filterMonth, setFilterMonth] = useState<string>('all');

  // Récupére le nom de l'utilisateur courant depuis le token stocké dans localStorage
  const userName = (() => {
    try {
      const raw = localStorage.getItem('jwtUser');
      if (!raw) return '';
      const parsed = JSON.parse(raw);
      return `${parsed.firstName ?? ''} ${parsed.lastName ?? ''}`.trim();
    } catch { return ''; }
  })();

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      let from: string | undefined;
      let to: string | undefined;
      if (filterYear !== 'all') {
        const y = parseInt(filterYear);
        if (filterMonth !== 'all') {
          const m = parseInt(filterMonth);
          from = dayjs(`${y}-${m}-01`).startOf('month').format('YYYY-MM-DD');
          to   = dayjs(`${y}-${m}-01`).endOf('month').format('YYYY-MM-DD');
        } else {
          from = `${y}-01-01`;
          to   = `${y}-12-31`;
        }
      }
      const data = await statsService.getMyStats(from, to);
      setStats(data);
    } catch {
      setError('Impossible de charger les statistiques');
    } finally {
      setLoading(false);
    }
  }, [filterYear, filterMonth]);

  useEffect(() => { load(); }, [load]);

  const periodData = filterMonth !== 'all' ? [] : (stats?.byMonth ?? []).map(d => ({ ...d, label: fmtMonthLabel(d.label) }));
  const yearData   = stats?.byYear ?? [];
  const lineData   = filterYear !== 'all' ? periodData : yearData;

  const periodCols = [
    { key: 'label'  as const, label: 'Période' },
    { key: 'slots'  as const, label: 'Créneaux' },
    { key: 'divers' as const, label: 'Plongées' },
  ];
  const groupCols = [
    { key: 'label'  as const, label: 'Nom' },
    { key: 'slots'  as const, label: 'Créneaux' },
    { key: 'divers' as const, label: 'Plongées' },
  ];

  return (
    <div className="page">
      {/* En-tête avec bouton PDF */}
      <div className="stats-page-header">
        <h1>📊 Mes statistiques</h1>
        {stats && !loading && (
          <button
            className="btn btn-outline stats-pdf-btn"
            onClick={() => exportMyStatsToPdf(stats, filterYear, filterMonth, userName)}
          >
            📄 Exporter PDF
          </button>
        )}
      </div>

      {/* Filtres */}
      <div className="stats-filters">
        <label>
          Année
          <select value={filterYear} onChange={e => { setFilterYear(e.target.value); setFilterMonth('all'); }}>
            <option value="all">Toutes</option>
            {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
          </select>
        </label>
        {filterYear !== 'all' && (
          <label>
            Mois
            <select value={filterMonth} onChange={e => setFilterMonth(e.target.value)}>
              <option value="all">Tous</option>
              {MONTHS_FR.map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
            </select>
          </label>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? (
        <div className="loading">Chargement des statistiques...</div>
      ) : stats ? (
        <>
          {/* KPIs */}
          <div className="stats-totals">
            <div className="stats-total-card">
              <span className="stats-total-icon">🤿</span>
              <span className="stats-total-value">{stats.totalSlots}</span>
              <span className="stats-total-label">Créneaux dirigés</span>
            </div>
            <div className="stats-total-card">
              <span className="stats-total-icon">🏊</span>
              <span className="stats-total-value">{stats.totalDivers}</span>
              <span className="stats-total-label">Plongeurs encadrés</span>
            </div>
            <div className="stats-total-card">
              <span className="stats-total-icon">&#128197;</span>
              <span className="stats-total-value">{stats.totalUniqueDays}</span>
              <span className="stats-total-label">Jours de plongée</span>
            </div>
            {stats.totalSlots > 0 && (
              <div className="stats-total-card">
                <span className="stats-total-icon">📈</span>
                <span className="stats-total-value">{stats.avgDiversPerSlot.toFixed(1)}</span>
                <span className="stats-total-label">Plongeurs / créneau moy.</span>
              </div>
            )}
            {stats.totalCapacity > 0 && (
              <div className="stats-total-card stats-total-card-gauge">
                <GaugeChart rate={stats.fillRate} />
              </div>
            )}
            {stats.bestDayDate && (
              <div className="stats-total-card stats-total-card-record">
                <span className="stats-total-icon">🏆</span>
                <span className="stats-total-value">{stats.bestDayDivers}</span>
                <span className="stats-total-label">Record du jour</span>
                <span className="stats-record-desc">Nb max de plongées réalisées en une seule journée</span>
                <span className="stats-record-date">{dayjs(stats.bestDayDate).format('DD/MM/YYYY')}</span>
              </div>
            )}
            {stats.bestMonthLabel && (
              <div className="stats-total-card stats-total-card-record">
                <span className="stats-total-icon">📆</span>
                <span className="stats-total-value">{stats.bestMonthDivers}</span>
                <span className="stats-total-label">Record du mois</span>
                <span className="stats-record-desc">Nb max de plongées réalisées en un seul mois</span>
                <span className="stats-record-date">{fmtMonthLabel(stats.bestMonthLabel)}</span>
              </div>
            )}
          </div>

          {/* Évolution par période */}
          {filterMonth === 'all' && (
            <div className="stats-section">
              <h2>📅 Évolution {filterYear !== 'all' ? `— ${filterYear}` : 'par année'}</h2>
              <div className="stats-row">
                <div className="stats-card stats-card-wide">
                  <h3>Courbe {filterYear !== 'all' ? 'mensuelle' : 'annuelle'}</h3>
                  <LineChart data={lineData} />
                </div>
                <div className="stats-card">
                  <h3>Tableau {filterYear !== 'all' ? 'mensuel' : 'annuel'}</h3>
                  <StatsTable data={filterYear !== 'all' ? periodData : yearData} cols={periodCols} />
                </div>
              </div>
            </div>
          )}

          {/* Par club */}
          {stats.byClub.length > 0 && (
            <div className="stats-section">
              <h2>🏊 Par club</h2>
              <div className="stats-row">
                <div className="stats-card">
                  <h3>Plongées par club</h3>
                  <PieChart data={stats.byClub} valueKey="divers" />
                </div>
                <div className="stats-card">
                  <h3>Créneaux par club</h3>
                  <PieChart data={stats.byClub} valueKey="slots" />
                </div>
                <div className="stats-card stats-card-wide">
                  <h3>Détail par club</h3>
                  <StatsTable data={stats.byClub} cols={groupCols} />
                </div>
              </div>
            </div>
          )}

          {/* Par type */}
          {stats.byType.length > 0 && (
            <div className="stats-section">
              <h2>🏷️ Par type de créneau</h2>
              <div className="stats-row">
                <div className="stats-card">
                  <h3>Plongées par type</h3>
                  <PieChart data={stats.byType} valueKey="divers" />
                </div>
                <div className="stats-card">
                  <h3>Créneaux par type</h3>
                  <PieChart data={stats.byType} valueKey="slots" />
                </div>
                <div className="stats-card stats-card-wide">
                  <h3>Détail par type</h3>
                  <StatsTable data={stats.byType} cols={groupCols} />
                </div>
              </div>
            </div>
          )}

          {/* Par jour de la semaine */}
          <div className="stats-section">
            <h2>📆 Fréquentation par jour de la semaine</h2>
            <div className="stats-row">
              <div className="stats-card stats-card-wide">
                <h3>Créneaux et plongées par jour</h3>
                <BarChart
                  data={stats.byDayOfWeek}
                  max={Math.max(1, ...stats.byDayOfWeek.map(d => Math.max(d.slots, d.divers)))}
                />
              </div>
              <div className="stats-card">
                <h3>Tableau</h3>
                <StatsTable data={stats.byDayOfWeek} cols={periodCols} />
              </div>
            </div>
          </div>

          {/* Par niveau */}
          {stats.byLevel.length > 0 && (
            <div className="stats-section">
              <h2>🎓 Par niveau de plongeur</h2>
              <div className="stats-row">
                <div className="stats-card">
                  <h3>Répartition des plongées par niveau</h3>
                  <PieChart data={stats.byLevel} valueKey="divers" />
                </div>
                <div className="stats-card stats-card-wide">
                  <h3>Détail par niveau</h3>
                  <StatsTable
                    data={stats.byLevel}
                    cols={[
                      { key: 'label'  as const, label: 'Niveau' },
                      { key: 'divers' as const, label: 'Plongées' },
                    ]}
                  />
                </div>
              </div>
            </div>
          )}

          {stats.totalSlots === 0 && (
            <div className="stats-empty" style={{ textAlign: 'center', padding: '2rem', fontSize: '1.1rem' }}>
              Aucun créneau créé sur cette période.
            </div>
          )}
        </>
      ) : null}
    </div>
  );
}
