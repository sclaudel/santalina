/**
 * Fonctions de dessin de graphiques pour l'export jsPDF.
 * Dessin vectoriel natif — aucune dépendance supplémentaire.
 */
import jsPDF from 'jspdf';
import type { PeriodStat, GroupStat } from '../types';

type RGB = [number, number, number];

// Palette de couleurs cohérente avec le web
export const PDF_COLORS: RGB[] = [
  [59, 130, 246],   // blue
  [16, 185, 129],   // green
  [245, 158, 11],   // amber
  [239, 68, 68],    // red
  [139, 92, 246],   // purple
  [6, 182, 212],    // cyan
  [249, 115, 22],   // orange
  [132, 204, 18],   // lime
  [236, 72, 153],   // pink
  [99, 102, 241],   // indigo
];

// ── Barre de taux de remplissage ──────────────────────────────────────────────
export function drawPdfFillRate(
  doc: jsPDF, rate: number,
  x: number, y: number, width: number,
): number {
  const pct   = Math.min(Math.max(rate, 0), 100);
  const color: RGB = pct >= 70 ? [16, 185, 129] : pct >= 40 ? [245, 158, 11] : [239, 68, 68];

  // Piste
  doc.setFillColor(229, 231, 235);
  doc.roundedRect(x, y, width, 7, 2, 2, 'F');

  // Remplissage
  if (pct > 0) {
    doc.setFillColor(...color);
    doc.roundedRect(x, y, (pct / 100) * width, 7, 2, 2, 'F');
  }

  // Valeur
  doc.setFontSize(9);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(...color);
  doc.text(`${pct.toFixed(1)} %`, x + width + 5, y + 5.5);

  // Reset
  doc.setTextColor(0); doc.setFont('helvetica', 'normal');
  return y + 14;
}

// ── Barres proportionnelles horizontales (remplace camembert en PDF) ──────────
export function drawPdfPropBars(
  doc: jsPDF,
  data: GroupStat[],
  valueKey: 'slots' | 'divers',
  x: number, y: number, width: number,
): number {
  const filtered = data.filter(d => d[valueKey] > 0);
  if (filtered.length === 0) return y;

  const total = filtered.reduce((s, d) => s + d[valueKey], 0);
  // Largeurs proportionnelles à la largeur disponible pour éviter barW négatif
  const valW  = Math.min(32, width * 0.22);   // colonne valeur à droite
  const lblW  = Math.min(58, width * 0.42);   // colonne label à gauche
  const barX  = x + lblW + 2;
  const barW  = width - lblW - 2 - valW;
  const rowH  = 11;
  // Nb max de caractères pour le label selon la largeur disponible (~1.5mm/char à 7.5pt)
  const maxChars = Math.max(8, Math.floor(lblW / 1.5));

  filtered.slice(0, 12).forEach((d, i) => {
    const pct   = d[valueKey] / total;
    const fillW = pct * barW;
    const color = PDF_COLORS[i % PDF_COLORS.length];

    // Label (tronqué selon la largeur réelle disponible)
    const lbl = d.label.length > maxChars ? d.label.slice(0, maxChars - 1) + '…' : d.label;
    doc.setFontSize(7.5);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(80, 80, 80);
    doc.text(lbl, x + lblW, y + 5.5, { align: 'right' });

    // Fond
    doc.setFillColor(229, 231, 235);
    doc.roundedRect(barX, y + 0.5, barW, 6, 1.5, 1.5, 'F');

    // Remplissage
    if (fillW > 0.5) {
      doc.setFillColor(...color);
      doc.roundedRect(barX, y + 0.5, fillW, 6, 1.5, 1.5, 'F');
    }

    // Valeur + %
    doc.setFontSize(7);
    doc.setTextColor(80, 80, 80);
    doc.text(`${d[valueKey]} (${(pct * 100).toFixed(0)} %)`, barX + barW + 2, y + 5.5);

    y += rowH;
  });

  doc.setTextColor(0);
  return y + 3;
}

// ── Graphique en courbes (évolution temporelle) ───────────────────────────────
export function drawPdfLineChart(
  doc: jsPDF,
  data: PeriodStat[],
  x: number, y: number, width: number, height: number,
): number {
  if (data.length === 0) return y;

  const PL = 32; const PR = 12; const PT = 6; const PB = 20;
  const iW = width  - PL - PR;
  const iH = height - PT - PB;
  const ox = x + PL;
  const oy = y + PT;
  const n  = data.length;

  const maxV = Math.max(1, ...data.map(d => Math.max(d.slots, d.divers)));
  const txX  = (i: number) => ox + (n === 1 ? iW / 2 : i * (iW / (n - 1)));
  const txY  = (v: number) => oy + iH - (v / maxV) * iH;

  // Grille + graduations Y
  const ticks = [0, 0.25, 0.5, 0.75, 1];
  doc.setLineWidth(0.2);
  doc.setFontSize(6.5);
  ticks.forEach(f => {
    const gy = txY(maxV * f);
    doc.setDrawColor(220, 220, 220);
    doc.line(ox, gy, ox + iW, gy);
    doc.setTextColor(160, 160, 160);
    doc.text(String(Math.round(maxV * f)), ox - 2, gy + 2, { align: 'right' });
  });

  // Axes
  doc.setDrawColor(180, 180, 180);
  doc.setLineWidth(0.4);
  doc.line(ox, oy, ox, oy + iH);
  doc.line(ox, oy + iH, ox + iW, oy + iH);

  // Labels X
  const step = Math.max(1, Math.ceil(n / 10));
  doc.setFontSize(6);
  doc.setTextColor(100, 100, 100);
  data.forEach((d, i) => {
    if (i % step === 0) {
      doc.text(d.label, txX(i), oy + iH + 6, { align: 'center' });
    }
  });

  // Aire créneaux (bleu clair transparent — simulé avec opacité basse via rectangle)
  // jsPDF ne gère pas l'opacité nativement → on dessine juste les lignes

  // Ligne créneaux (bleu)
  doc.setDrawColor(59, 130, 246);
  doc.setLineWidth(0.7);
  for (let i = 0; i < n - 1; i++) {
    doc.line(txX(i), txY(data[i].slots), txX(i + 1), txY(data[i + 1].slots));
  }

  // Ligne plongées (vert)
  doc.setDrawColor(16, 185, 129);
  doc.setLineWidth(1.1);
  for (let i = 0; i < n - 1; i++) {
    doc.line(txX(i), txY(data[i].divers), txX(i + 1), txY(data[i + 1].divers));
  }

  // Points
  if (n <= 36) {
    doc.setDrawColor(255, 255, 255);
    doc.setLineWidth(0.4);
    doc.setFillColor(59, 130, 246);
    data.forEach((d, i) => { doc.circle(txX(i), txY(d.slots),  1.1, 'FD'); });
    doc.setFillColor(16, 185, 129);
    data.forEach((d, i) => { doc.circle(txX(i), txY(d.divers), 1.4, 'FD'); });
  }

  // Légende
  const ly = oy + iH + 14;
  doc.setFillColor(59, 130, 246);
  doc.roundedRect(ox, ly - 2.5, 7, 3, 1, 1, 'F');
  doc.setFontSize(7);
  doc.setTextColor(80, 80, 80);
  doc.text('Créneaux', ox + 9, ly + 0.5);

  doc.setFillColor(16, 185, 129);
  doc.roundedRect(ox + 46, ly - 2.5, 7, 3, 1, 1, 'F');
  doc.text('Plongées', ox + 55, ly + 0.5);

  // Reset
  doc.setTextColor(0); doc.setDrawColor(0); doc.setLineWidth(0.5);
  return y + height + 4;
}

// ── Barres groupées horizontales (jours de la semaine, etc.) ─────────────────
export function drawPdfGroupedBars(
  doc: jsPDF,
  data: PeriodStat[],
  x: number, y: number, width: number,
): number {
  if (data.length === 0) return y;

  const maxV = Math.max(1, ...data.map(d => Math.max(d.slots, d.divers)));
  const lblW = 26;
  const barX = x + lblW + 3;
  const barW = width - lblW - 38;
  const rowH = 12;

  data.forEach(d => {
    const sW = (d.slots  / maxV) * barW;
    const dW = (d.divers / maxV) * barW;

    // Label
    doc.setFontSize(7.5);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(80, 80, 80);
    doc.text(d.label, x + lblW, y + 6, { align: 'right' });

    // Fond
    doc.setFillColor(229, 231, 235);
    doc.roundedRect(barX, y + 0.5, barW, 7, 1.5, 1.5, 'F');

    // Créneaux (bleu clair)
    if (sW > 0) {
      doc.setFillColor(147, 197, 253);
      doc.roundedRect(barX, y + 0.5, sW, 7, 1.5, 1.5, 'F');
    }
    // Plongées (bleu foncé, superposé)
    if (dW > 0) {
      doc.setFillColor(59, 130, 246);
      doc.roundedRect(barX, y + 1.5, dW, 4, 1, 1, 'F');
    }

    // Valeurs
    doc.setFontSize(7);
    doc.setTextColor(80, 80, 80);
    doc.text(`${d.slots} cr. / ${d.divers} plg.`, barX + barW + 3, y + 6);

    y += rowH;
  });

  // Légende
  doc.setFillColor(147, 197, 253);
  doc.roundedRect(barX, y + 1, 7, 4, 1, 1, 'F');
  doc.setFontSize(7);
  doc.setTextColor(80, 80, 80);
  doc.text('Créneaux', barX + 9, y + 5);
  doc.setFillColor(59, 130, 246);
  doc.roundedRect(barX + 48, y + 1, 7, 4, 1, 1, 'F');
  doc.text('Plongées', barX + 57, y + 5);

  doc.setTextColor(0);
  return y + 14;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Retourne true et ajoute une page si l'espace restant est insuffisant */
export function ensureSpace(doc: jsPDF, y: number, needed: number): number {
  const pageH = doc.internal.pageSize.getHeight();
  if (y + needed > pageH - 15) {
    doc.addPage();
    return 18;
  }
  return y;
}

/** Dessine un titre de section avec filet */
export function drawSectionTitle(doc: jsPDF, title: string, x: number, y: number, width: number): number {
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(30, 64, 175);
  doc.text(title, x, y);
  y += 2;
  doc.setDrawColor(30, 64, 175);
  doc.setLineWidth(0.4);
  doc.line(x, y, x + width, y);
  doc.setTextColor(0);
  doc.setDrawColor(0);
  doc.setLineWidth(0.5);
  return y + 6;
}
