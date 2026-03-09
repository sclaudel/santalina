import jsPDF from 'jspdf';

// ── Couleurs ────────────────────────────────────────────────────────────────
const COLOR_PRIMARY   = [30, 64, 175] as [number, number, number];  // #1e40af
const COLOR_ACCENT    = [59, 130, 246] as [number, number, number]; // #3b82f6
const COLOR_DARK      = [17, 24, 39]  as [number, number, number];  // #111827
const COLOR_GRAY      = [107, 114, 128] as [number, number, number];// #6b7280
const COLOR_LIGHT_BG  = [239, 246, 255] as [number, number, number];// #eff6ff
const COLOR_TIP_BG    = [220, 252, 231] as [number, number, number];// #dcfce7
const COLOR_TIP_TEXT  = [22, 101, 52]  as [number, number, number]; // #166534
const COLOR_WARN_BG   = [254, 243, 199] as [number, number, number];// #fef3c7
const COLOR_WARN_TEXT = [146, 64, 14]  as [number, number, number]; // #92400e
const COLOR_WHITE     = [255, 255, 255] as [number, number, number];

const MARGIN    = 18;
const PAGE_W    = 210;
const CONTENT_W = PAGE_W - MARGIN * 2;

// ── Interface de données ─────────────────────────────────────────────────────

export interface PdfSection {
  icon: string;
  title: string;
  items: PdfItem[];
}

export type PdfItem =
  | { type: 'paragraph'; text: string }
  | { type: 'h4'; text: string }
  | { type: 'ol'; items: string[] }
  | { type: 'ul'; items: string[] }
  | { type: 'tip'; text: string }
  | { type: 'warning'; text: string };

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Rend un emoji en PNG via un canvas HTML et renvoie un data-URL.
 * Le navigateur utilise sa propre police emoji (Noto, Apple, Segoe…).
 */
const emojiCache = new Map<string, string>();
function emojiToPng(emoji: string, sizePx = 48): string {
  const key = `${emoji}:${sizePx}`;
  if (emojiCache.has(key)) return emojiCache.get(key)!;

  const canvas = document.createElement('canvas');
  canvas.width  = sizePx;
  canvas.height = sizePx;
  const ctx = canvas.getContext('2d')!;
  ctx.clearRect(0, 0, sizePx, sizePx);
  ctx.font         = `${Math.round(sizePx * 0.78)}px serif`;
  ctx.textAlign    = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(emoji, sizePx / 2, sizePx / 2 + sizePx * 0.04);
  const dataUrl = canvas.toDataURL('image/png');
  emojiCache.set(key, dataUrl);
  return dataUrl;
}

/** Supprime les caractères hors Latin-1 du texte destiné à Helvetica. */
function clean(text: string): string {
  return text
    .replace(/[^\u0000-\u00FF]/g, '')
    .replace(/\s{2,}/g, ' ')
    .trim();
}

function splitLines(doc: jsPDF, text: string, maxWidth: number): string[] {
  return doc.splitTextToSize(clean(text), maxWidth);
}

function addPageIfNeeded(doc: jsPDF, y: number, needed: number, pageH: number): number {
  if (y + needed > pageH - MARGIN) {
    doc.addPage();
    return MARGIN + 10;
  }
  return y;
}

// ── Générateur PDF ───────────────────────────────────────────────────────────

export function exportHelpPdf(sections: PdfSection[], siteName: string): void {
  const doc = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' });
  const pageH = doc.internal.pageSize.getHeight();
  let y = 0;

  // ── Page de couverture ──────────────────────────────────────────────────────
  doc.setFillColor(...COLOR_PRIMARY);
  doc.rect(0, 0, PAGE_W, 55, 'F');

  doc.setTextColor(...COLOR_WHITE);
  doc.setFontSize(24);
  doc.setFont('helvetica', 'bold');
  doc.text('Guide d\'utilisation', PAGE_W / 2, 22, { align: 'center' });

  doc.setFontSize(13);
  doc.setFont('helvetica', 'normal');
  doc.text(clean(siteName), PAGE_W / 2, 33, { align: 'center' });

  const today = new Date().toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  doc.setFontSize(10);
  doc.text(`Généré le ${clean(today)}`, PAGE_W / 2, 43, { align: 'center' });

  doc.setFillColor(...COLOR_ACCENT);
  doc.rect(0, 55, PAGE_W, 2, 'F');

  // ── Sommaire ─────────────────────────────────────────────────────────────────
  y = 70;
  doc.setTextColor(...COLOR_DARK);
  doc.setFontSize(14);
  doc.setFont('helvetica', 'bold');
  doc.text('Sommaire', MARGIN, y);
  y += 8;

  doc.setLineWidth(0.3);
  doc.setDrawColor(...COLOR_ACCENT);
  doc.line(MARGIN, y, PAGE_W - MARGIN, y);
  y += 7;

  const ICON_TOC = 4;   // taille emoji dans le sommaire (mm)

  // Enregistre la position de chaque ligne pour y ajouter les liens après coup
  let tocCurrentPage = 1;
  const tocEntries: Array<{ page: number; topY: number; baseY: number }> = [];

  sections.forEach((section, idx) => {
    const prevY = y;
    y = addPageIfNeeded(doc, y, 8, pageH);
    if (y < prevY) tocCurrentPage++;
    tocEntries.push({ page: tocCurrentPage, topY: y - 5, baseY: y });

    // Emoji
    doc.addImage(emojiToPng(section.icon, 56), 'PNG', MARGIN + 3, y - ICON_TOC + 0.5, ICON_TOC, ICON_TOC);

    // Titre souligné (couleur primaire = indique cliquable)
    const textX = MARGIN + 3 + ICON_TOC + 2;
    const label = `${idx + 1}.  ${clean(section.title)}`;
    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...COLOR_PRIMARY);
    doc.text(label, textX, y);
    const labelW = doc.getTextWidth(label);
    doc.setDrawColor(...COLOR_PRIMARY);
    doc.setLineWidth(0.15);
    doc.line(textX, y + 0.8, textX + labelW, y + 0.8);

    y += 7;
  });

  // Mémorise la dernière page du sommaire avant d'ajouter les pages de section
  const tocLastPage = doc.getNumberOfPages();

  // ── Sections ────────────────────────────────────────────────────────────────
  const ICON_HDR = 5.5; // taille emoji dans l'en-tête de section (mm)
  const ICON_BOX = 4.5; // taille emoji dans les encadrés tip/warning (mm)

  sections.forEach((section, idx) => {
    doc.addPage();
    y = MARGIN;

    // En-tête de section — rectangle bleu
    doc.setFillColor(...COLOR_PRIMARY);
    doc.roundedRect(MARGIN, y, CONTENT_W, 13, 3, 3, 'F');

    // Emoji dans l'en-tête (rendu blanc impossible → fond transparent sur PNG)
    doc.addImage(emojiToPng(section.icon, 64), 'PNG', MARGIN + 2.5, y + 3.5, ICON_HDR, ICON_HDR);

    doc.setFontSize(13);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(...COLOR_WHITE);
    doc.text(`${idx + 1}.  ${clean(section.title)}`, MARGIN + 2.5 + ICON_HDR + 2, y + 9);
    y += 19;

    // ── Items ──────────────────────────────────────────────────────────────────
    section.items.forEach(item => {
      switch (item.type) {

        case 'h4': {
          y = addPageIfNeeded(doc, y, 11, pageH);
          doc.setFontSize(11);
          doc.setFont('helvetica', 'bold');
          doc.setTextColor(...COLOR_PRIMARY);
          doc.text(clean(item.text), MARGIN, y);
          y += 5;
          doc.setDrawColor(...COLOR_ACCENT);
          doc.setLineWidth(0.2);
          doc.line(MARGIN, y, MARGIN + CONTENT_W * 0.4, y);
          y += 4;
          break;
        }

        case 'paragraph': {
          const lines = splitLines(doc, item.text, CONTENT_W);
          y = addPageIfNeeded(doc, y, lines.length * 5 + 3, pageH);
          doc.setFontSize(10);
          doc.setFont('helvetica', 'normal');
          doc.setTextColor(...COLOR_DARK);
          doc.text(lines, MARGIN, y);
          y += lines.length * 5 + 3;
          break;
        }

        case 'ul': {
          item.items.forEach(li => {
            const lines = splitLines(doc, li, CONTENT_W - 8);
            y = addPageIfNeeded(doc, y, lines.length * 5 + 2, pageH);
            doc.setFontSize(10);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...COLOR_DARK);
            doc.setFillColor(...COLOR_ACCENT);
            doc.circle(MARGIN + 2, y - 1.5, 1, 'F');
            doc.text(lines, MARGIN + 6, y);
            y += lines.length * 5 + 2;
          });
          y += 2;
          break;
        }

        case 'ol': {
          item.items.forEach((li, liIdx) => {
            const lines = splitLines(doc, li, CONTENT_W - 10);
            y = addPageIfNeeded(doc, y, lines.length * 5 + 2, pageH);
            doc.setFontSize(10);
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...COLOR_PRIMARY);
            doc.text(`${liIdx + 1}.`, MARGIN + 1, y);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...COLOR_DARK);
            doc.text(lines, MARGIN + 8, y);
            y += lines.length * 5 + 2;
          });
          y += 2;
          break;
        }

        case 'tip': {
          const lines = splitLines(doc, item.text, CONTENT_W - ICON_BOX - 8);
          const boxH  = Math.max(lines.length * 5 + 8, ICON_BOX + 6);
          y = addPageIfNeeded(doc, y, boxH + 4, pageH);
          doc.setFillColor(...COLOR_TIP_BG);
          doc.setDrawColor(...COLOR_TIP_TEXT);
          doc.setLineWidth(0.3);
          doc.roundedRect(MARGIN, y, CONTENT_W, boxH, 2, 2, 'FD');
          // Emoji 💡
          doc.addImage(emojiToPng('💡', 56), 'PNG', MARGIN + 2, y + (boxH - ICON_BOX) / 2, ICON_BOX, ICON_BOX);
          doc.setFontSize(10);
          doc.setFont('helvetica', 'italic');
          doc.setTextColor(...COLOR_TIP_TEXT);
          doc.text(lines, MARGIN + ICON_BOX + 5, y + 6);
          y += boxH + 5;
          break;
        }

        case 'warning': {
          const lines = splitLines(doc, item.text, CONTENT_W - ICON_BOX - 8);
          const boxH  = Math.max(lines.length * 5 + 8, ICON_BOX + 6);
          y = addPageIfNeeded(doc, y, boxH + 4, pageH);
          doc.setFillColor(...COLOR_WARN_BG);
          doc.setDrawColor(...COLOR_WARN_TEXT);
          doc.setLineWidth(0.3);
          doc.roundedRect(MARGIN, y, CONTENT_W, boxH, 2, 2, 'FD');
          // Emoji ⚠️
          doc.addImage(emojiToPng('⚠️', 56), 'PNG', MARGIN + 2, y + (boxH - ICON_BOX) / 2, ICON_BOX, ICON_BOX);
          doc.setFontSize(10);
          doc.setFont('helvetica', 'italic');
          doc.setTextColor(...COLOR_WARN_TEXT);
          doc.text(lines, MARGIN + ICON_BOX + 5, y + 6);
          y += boxH + 5;
          break;
        }
      }
    });
  });

  // ── Liens cliquables + numéros de page dans le sommaire ──────────────────────
  // Maintenant que toutes les pages de section existent, on connaît leurs numéros.
  // Section idx est sur la page tocLastPage + 1 + idx.
  sections.forEach((_section, idx) => {
    const sectionPageNum = tocLastPage + 1 + idx;
    const entry = tocEntries[idx];
    doc.setPage(entry.page);

    // Numéro de page aligné à droite
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...COLOR_GRAY);
    doc.text(`${sectionPageNum}`, PAGE_W - MARGIN, entry.baseY, { align: 'right' });

    // Zone cliquable couvrant toute la ligne
    doc.link(MARGIN, entry.topY, CONTENT_W, 7, { pageNumber: sectionPageNum });
  });

  // Retour à la dernière page pour la suite
  doc.setPage(doc.getNumberOfPages());

  // ── Pied de page sur toutes les pages (sauf couverture) ─────────────────────
  const total = doc.getNumberOfPages();
  for (let i = 2; i <= total; i++) {
    doc.setPage(i);
    doc.setFillColor(...COLOR_LIGHT_BG);
    doc.rect(0, pageH - 14, PAGE_W, 14, 'F');
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(...COLOR_GRAY);
    doc.text(clean(siteName), MARGIN, pageH - 6);
    doc.text(`Page ${i - 1} / ${total - 1}`, PAGE_W - MARGIN, pageH - 6, { align: 'right' });
  }

  doc.save('guide-utilisation.pdf');
}
