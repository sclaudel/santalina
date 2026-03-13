import { useState, useEffect, useCallback } from 'react';
import dayjs from 'dayjs';
import { statsService } from '../services/statsService';
import type { MyStatsResponse, PeriodStat, GroupStat } from '../types';

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

// ---- Page principale ----
const CURRENT_YEAR = dayjs().year();
const YEARS = Array.from({ length: 5 }, (_, i) => CURRENT_YEAR - i);
const MONTHS_FR = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];

export function MyStatsPage() {
  const [stats, setStats]     = useState<MyStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');

  const [filterYear, setFilterYear]   = useState<string>('all');
  const [filterMonth, setFilterMonth] = useState<string>('all');

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

  const periodData = filterMonth !== 'all' ? [] : (stats?.byMonth ?? []);
  const yearData   = stats?.byYear ?? [];
  const barData    = filterYear !== 'all' ? periodData : yearData;
  const maxPeriod  = Math.max(1, ...barData.map(d => Math.max(d.slots, d.divers)));

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
      <h1>📊 Mes statistiques</h1>

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
          {/* Totaux */}
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
            {stats.totalSlots > 0 && (
              <div className="stats-total-card">
                <span className="stats-total-icon">📈</span>
                <span className="stats-total-value">{stats.avgDiversPerSlot.toFixed(1)}</span>
                <span className="stats-total-label">Plongeurs / créneau moy.</span>
              </div>
            )}
          </div>

          {/* Évolution par période */}
          {filterMonth === 'all' && (
            <div className="stats-section">
              <h2>📅 Évolution {filterYear !== 'all' ? `— ${filterYear}` : 'par année'}</h2>
              <div className="stats-row">
                <div className="stats-card stats-card-wide">
                  <h3>Par {filterYear !== 'all' ? 'mois' : 'année'}</h3>
                  <BarChart data={barData} max={maxPeriod} />
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
