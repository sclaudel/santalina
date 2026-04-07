/**
 * Export de la fiche de sécurité Excel avec organisation en palanquées.
 *
 * Structure du template :
 *  - 5 groupes par feuille, chaque groupe = 1 palanquée, 4 lignes de plongeurs
 *  - Groupe 1 → lignes 8-11, Groupe 2 → lignes 13-16, ... Groupe 5 → lignes 28-31
 *  - Colonne A première ligne du groupe = numéro de palanquée (P1, P2…)
 *  - Si plus de 5 palanquées → nouvelle feuille clonée depuis le template
 */

import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';
import type { DiveSlot, SlotDiver, Palanquee } from '../types';

function fmtDate(d: string): string {
  const [y, m, day] = d.split('-');
  return `${day}/${m}/${y}`;
}

function cap(s: string): string {
  return s ? s.charAt(0).toUpperCase() + s.slice(1) : '';
}

// 5 groupes par page — chaque groupe correspond à une palanquée (4 lignes)
const GROUPS: number[][] = [
  [8, 9, 10, 11],
  [13, 14, 15, 16],
  [18, 19, 20, 21],
  [23, 24, 25, 26],
  [28, 29, 30, 31],
];
const MAX_GROUPS_PER_PAGE = GROUPS.length; // 5
const MAX_DIVERS_PER_GROUP = 4;

// Colonnes effacées/réécrites dans la zone plongeurs (valeur + style réinitialisés)
const DIVER_COLS = [2, 3, 4, 6, 7]; // B C D F G
// Colonnes dont le style est restauré depuis le template (valeur non touchée)
const STYLE_RESTORE_COLS = [8, 9, 10, 11, 12]; // H I J K L
// Colonne A uniquement sur la première ligne de chaque groupe (numéro palanquée)

interface ExportGroup {
  label: string;
  divers: SlotDiver[];
  depth?: string;
  duration?: string;
}

/** Remplit l'en-tête de la feuille */
function fillHeader(
  ws: ExcelJS.Worksheet,
  slot: DiveSlot,
  allDivers: SlotDiver[],
) {
  const director   = allDivers.find(d => d.isDirector);
  const dirName    = director ? `${director.lastName.toUpperCase()} ${cap(director.firstName)}` : '';
  const dirLevel   = director?.level ?? '';
  const dirLicense = director?.licenseNumber ?? '';
  const dpInfo     = [dirName, dirLevel, dirLicense].filter(Boolean).join(' - ');

  ws.getCell('B4').value =
    `Date : ${fmtDate(slot.slotDate)} ${slot.startTime}–${slot.endTime}\nClub : ${slot.club ?? ''}\nNom, Prénom et Brevet du DP : ${dpInfo}`;

  const startHour = parseInt(slot.startTime.split(':')[0], 10);
  if (startHour < 13) {
    ws.getCell('H4').value = `AM\n${slot.startTime} – ${slot.endTime}`;
    ws.getCell('I4').value = 'PM';
  } else {
    ws.getCell('H4').value = 'AM';
    ws.getCell('I4').value = `PM\n${slot.startTime} – ${slot.endTime}`;
  }

  ws.getCell('J4').value = `Nb Plongeurs\n${allDivers.length} / ${slot.diverCount}`;
  ws.getCell('J4').alignment = { wrapText: true, vertical: 'middle', horizontal: 'center' };
}

/**
 * Remplit les 5 groupes d'une feuille à partir d'un tableau de 5 ExportGroup (ou undefined si vide).
 * Chaque groupe occupe ses 4 lignes dans GROUPS[g].
 * - Ligne 0 du groupe : col A = label palanquée, col B/C = premier plongeur
 * - Lignes 1-3 : col B/C = plongeurs suivants (A laissé intact, peut être fusionné)
 */
function fillSheetGroups(
  ws: ExcelJS.Worksheet,
  pageGroups: (ExportGroup | undefined)[],
  modelStyles: Partial<ExcelJS.Style>[],
  modelHeight: number,
) {
  for (let g = 0; g < MAX_GROUPS_PER_PAGE; g++) {
    const group = pageGroups[g];
    const rows  = GROUPS[g];

    for (let ri = 0; ri < rows.length; ri++) {
      const r = rows[ri];

      // Réappliquer hauteur et styles sur les colonnes plongeurs
      ws.getRow(r).height = modelHeight;
      for (const col of DIVER_COLS) {
        const cell = ws.getCell(r, col);
        cell.style = JSON.parse(JSON.stringify(modelStyles[col] ?? {}));
        cell.value = null;
      }
      // Restaurer le style des colonnes H-L (sans toucher aux valeurs du template)
      for (const col of STYLE_RESTORE_COLS) {
        if (modelStyles[col]) {
          ws.getCell(r, col).style = JSON.parse(JSON.stringify(modelStyles[col]));
        }
      }

      // Vider col A uniquement sur la première ligne du groupe
      if (ri === 0) {
        const cellA = ws.getCell(r, 1);
        cellA.value = null;

        if (group?.label) {
          cellA.value = group.label;
          cellA.alignment = { horizontal: 'center', vertical: 'middle' };
        }

        // Profondeur max et Temps max dans la cellule fusionnée H8:H11 / I8:I11
        // (et équivalents pour les autres groupes) — écriture sur la cellule maître (ri=0)
        if (group?.depth) {
          const cellDepth = ws.getCell(r, 8);
          cellDepth.value = `Profondeur max\n\n${group.depth}`;
          cellDepth.alignment = { wrapText: true, vertical: 'top', horizontal: 'center' };
        }
        if (group?.duration) {
          const cellDur = ws.getCell(r, 9);
          cellDur.value = `Temps\nmax\n\n${group.duration}`;
          cellDur.alignment = { wrapText: true, vertical: 'top', horizontal: 'center' };
        }
      }

      if (group && ri < group.divers.length) {
        const diver = group.divers[ri];
        ws.getCell(r, 2).value = diver.lastName.toUpperCase();
        ws.getCell(r, 3).value = cap(diver.firstName);
        if (diver.aptitudes) ws.getCell(r, 4).value = diver.aptitudes;
      }
    }
  }
}

/**
 * Charge le template depuis son buffer dans un workbook temporaire et injecte
 * la feuille résultante dans `wb` — garantit une copie parfaite du template
 * (fusions, styles, images, mise en page) sans dépendre du clonage manuel.
 *
 * ExcelJS a une incohérence interne : le getter sort `model.merges` mais le
 * setter lit `model.mergeCells`.  On corrige ici avant d'assigner le modèle.
 */
async function loadFreshSheet(
  arrayBuffer: ArrayBuffer,
  wb: ExcelJS.Workbook,
  name: string,
): Promise<ExcelJS.Worksheet> {
  const tempWb = new ExcelJS.Workbook();
  await tempWb.xlsx.load(arrayBuffer);
  const tempWs = tempWb.getWorksheet(1);
  if (!tempWs) throw new Error('Feuille template introuvable dans le buffer');

  // ① Transférer les médias (images) de tempWb → wb avec remapping d'IDs
  const wbMedia: any[]  = (wb     as any).media ?? [];
  const tmpMedia: any[] = (tempWb as any).media ?? [];
  const idMap = new Map<number, number>();
  for (let i = 0; i < tmpMedia.length; i++) {
    idMap.set(i, wbMedia.length);
    wbMedia.push(tmpMedia[i]);
  }
  (wb as any).media = wbMedia;

  // ② Créer la feuille de destination dans le classeur principal
  const dest = wb.addWorksheet(name);
  const destId: number = (dest as any).id;

  // ③ Récupérer le modèle source et corriger les incohérences ExcelJS
  const wsModel: any = (tempWs as any).model;

  // Le getter sort "merges" (array de strings) mais le setter lit "mergeCells" !
  wsModel.mergeCells = wsModel.merges ?? [];

  // Remapper les imageIds vers les indices du classeur principal
  if (Array.isArray(wsModel.media)) {
    wsModel.media = wsModel.media.map((m: any) => ({
      ...m,
      imageId: idMap.has(m.imageId) ? idMap.get(m.imageId)! : m.imageId,
    }));
  }

  // ④ Le setter du modèle ExcelJS ignore les cellules de type Merge (= cellules
  //    non-maîtres dans les plages fusionnées), ce qui perd leurs styles (bordures).
  //    On capture ces styles AVANT l'import pour les restaurer ensuite.
  const mergeCellStyles: { address: string; style: any }[] = [];
  if (Array.isArray(wsModel.rows)) {
    for (const row of wsModel.rows) {
      if (!row?.cells) continue;
      for (const cell of row.cells) {
        if (cell.type === 1 /* ExcelJS ValueType.Merge */ && cell.style) {
          mergeCellStyles.push({
            address: cell.address,
            style: JSON.parse(JSON.stringify(cell.style)),
          });
        }
      }
    }
  }

  // ⑤ Injecter dans la feuille destination
  wsModel.id   = destId;
  wsModel.name = name;
  (dest as any).model = wsModel;

  // ⑥ Restaurer les styles des cellules fusionnées non-maîtres
  for (const { address, style } of mergeCellStyles) {
    dest.getCell(address).style = style;
  }

  return dest;
}

export async function exportFicheSecuriteAvecPalanquees(
  slot: DiveSlot,
  allDivers: SlotDiver[],
  palanquees: Palanquee[],
): Promise<void> {
  // ── Construire la liste des groupes à exporter ────────────────────────────
  const assignedIds = new Set(palanquees.flatMap(p => p.divers.map(d => d.id)));

  const exportGroups: ExportGroup[] = palanquees.map((p, idx) => ({
    label: `P${idx + 1}`,
    divers: p.divers.slice(0, MAX_DIVERS_PER_GROUP),
    depth: p.depth,
    duration: p.duration,
  }));

  // Plongeurs non assignés → groupe sans label (affiché après les palanquées)
  const unassigned = allDivers
    .filter(d => !assignedIds.has(d.id))
    .sort((a, b) => a.lastName.localeCompare(b.lastName));

  // Les non-assignés remplissent des groupes de 4 supplémentaires
  for (let i = 0; i < unassigned.length; i += MAX_DIVERS_PER_GROUP) {
    exportGroups.push({ label: '', divers: unassigned.slice(i, i + MAX_DIVERS_PER_GROUP) });
  }

  const totalPages = Math.max(1, Math.ceil(exportGroups.length / MAX_GROUPS_PER_PAGE));

  // ── Chargement du template ────────────────────────────────────────────────
  const response = await fetch('/templates/Fiche-de-securite-template.xlsx');
  if (!response.ok) throw new Error(`Impossible de charger le template : ${response.status}`);
  const arrayBuffer = await response.arrayBuffer();

  const wb = new ExcelJS.Workbook();
  await wb.xlsx.load(arrayBuffer);

  const ws1 = wb.getWorksheet(1);
  if (!ws1) throw new Error('Feuille introuvable dans le template');

  // Sauvegarder le style modèle depuis la ligne 8 (avant toute écriture)
  const modelStyles: Partial<ExcelJS.Style>[] = [];
  for (const col of [...DIVER_COLS, ...STYLE_RESTORE_COLS]) {
    modelStyles[col] = JSON.parse(JSON.stringify(ws1.getCell(8, col).style ?? {}));
  }
  const modelHeight = ws1.getRow(8).height ?? 16;

  // ── Cloner les feuilles supplémentaires AVANT toute écriture sur ws1 ─────
  // loadFreshSheet charge le template depuis le buffer original → copie parfaite.
  const extraSheets: ExcelJS.Worksheet[] = [];
  for (let page = 2; page <= totalPages; page++) {
    extraSheets.push(await loadFreshSheet(arrayBuffer, wb, `Page ${page}`));
  }

  // ── Page 1 ────────────────────────────────────────────────────────────────
  ws1.name = 'Fiche sécurité (palanquées)';
  fillHeader(ws1, slot, allDivers);

  const page1Groups = Array.from({ length: MAX_GROUPS_PER_PAGE }, (_, i) => exportGroups[i]);
  fillSheetGroups(ws1, page1Groups, modelStyles, modelHeight);

  if (slot.notes) {
    ws1.getCell('J33').value = `Notes : ${slot.notes}`;
  }

  // ── Pages supplémentaires ─────────────────────────────────────────────────
  for (let page = 2; page <= totalPages; page++) {
    const offset     = (page - 1) * MAX_GROUPS_PER_PAGE;
    const pageGroups = Array.from({ length: MAX_GROUPS_PER_PAGE }, (_, i) => exportGroups[offset + i]);
    const wsNew      = extraSheets[page - 2];

    fillHeader(wsNew, slot, allDivers);
    fillSheetGroups(wsNew, pageGroups, modelStyles, modelHeight);
  }

  // Supprimer les feuilles vides du template
  ['Feuil2', 'Feuil3'].forEach(name => {
    const s = wb.getWorksheet(name);
    if (s) wb.removeWorksheet(s.id);
  });

  // Date de génération
  const now = new Date();
  const genLabel = `Généré le ${now.toLocaleDateString('fr-FR')} à ${now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })} — avec palanquées`;
  wb.eachSheet(ws => { ws.getCell('A40').value = genLabel; });

  // ── Export ────────────────────────────────────────────────────────────────
  const buffer = await wb.xlsx.writeBuffer();
  saveAs(
    new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    `${slot.slotDate}-${slot.startTime.replace(':', '-')}-Fiche-securite-Saint-Lin.xlsx`,
  );
}

