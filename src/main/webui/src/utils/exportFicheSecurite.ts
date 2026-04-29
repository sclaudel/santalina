import ExcelJS from 'exceljs';
import type { DiveSlot, SlotDiver } from '../types';

function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Formate une date YYYY-MM-DD en jj/mm/aaaa */
function fmtDate(d: string): string {
  const [y, m, day] = d.split('-');
  return `${day}/${m}/${y}`;
}

/** Capitalise la première lettre */
function cap(s: string): string {
  return s ? s.charAt(0).toUpperCase() + s.slice(1) : '';
}

/**
 * Structure du template (feuille P1-1) :
 *
 *  R2        : Titre / email
 *  R4        : Infos générales (B4=Date+Club+DP, H4=AM, I4=PM, J4=NbPlongeurs)
 *  R6        : En-têtes colonnes
 *  R8–R11    : Groupe 1  (4 lignes plongeurs)
 *  R12       : Séparateur → NE PAS TOUCHER
 *  R13–R16   : Groupe 2
 *  R17       : Séparateur → NE PAS TOUCHER
 *  R18–R21   : Groupe 3
 *  R22       : Séparateur → NE PAS TOUCHER
 *  R23–R26   : Groupe 4
 *  R27       : Séparateur → NE PAS TOUCHER
 *  R28–R31   : Groupe 5
 *  R34       : Rappels réglementaires
 *
 *  Colonnes remplies : B(Nom), C(Prénom), F(CACI vide), G(Gaz vide)
 *  Colonnes non touchées : A(N° — template), D(Aptitude — non demandée),
 *                          E(Fonction — saisie manuelle), H–M (template d'origine)
 *
 *  Capacité : 5 groupes × 4 lignes = 20 plongeurs par page.
 *  Si plus de 20 plongeurs → page supplémentaire (copie du template).
 */

// Lignes de données disponibles par page : groupes de 4, séparés par une ligne vide
const DIVER_ROWS: number[] = [
  8,  9,  10, 11,   // groupe 1
  13, 14, 15, 16,   // groupe 2
  18, 19, 20, 21,   // groupe 3
  23, 24, 25, 26,   // groupe 4
  28, 29, 30, 31,   // groupe 5
];
const MAX_PER_PAGE = DIVER_ROWS.length; // 20

// Seules les colonnes B, C, F, G sont effacées/réécrites.
// A, D, E et H–M ne sont pas touchées (contenu et mise en forme du template conservés).
const COLS_TO_CLEAR = [2, 3, 6, 7]; // B C F G

/** Remplit l'en-tête d'une feuille avec les infos du créneau */
function fillHeader(
  ws: ExcelJS.Worksheet,
  slot: DiveSlot,
  allDivers: SlotDiver[],
  pageNum: number,
  totalPages: number,
) {
  const director   = allDivers.find(d => d.isDirector);
  const dirName    = director ? `${director.lastName.toUpperCase()} ${cap(director.firstName)}` : '';
  const dirLevel   = director?.level ?? '';
  const dirLicense = director?.licenseNumber ?? '';
  const dpInfo     = [dirName, dirLevel, dirLicense].filter(Boolean).join(' - ');

  const startTime = slot.startTime.slice(0, 5);
  const endTime   = slot.endTime.slice(0, 5);

  ws.getCell('B4').value =
    `Date : ${fmtDate(slot.slotDate)} ${startTime}–${endTime}\nClub : ${slot.club ?? ''}\nNom, Prénom et Brevet du DP : ${dpInfo}`;

  const startHour = parseInt(startTime.split(':')[0], 10);

  // Appliquer le style de H4 (référence du template) à I4 pour un formatage identique
  ws.getCell('I4').style = JSON.parse(JSON.stringify(ws.getCell('H4').style ?? {}));

  if (startHour < 13) {
    ws.getCell('H4').value = `AM\n${startTime} – ${endTime}`;
    ws.getCell('I4').value = 'PM';
  } else {
    ws.getCell('H4').value = 'AM';
    ws.getCell('I4').value = `PM\n${startTime} – ${endTime}`;
  }

  // Libellé + valeur Nb Plongeurs dans la même cellule J4
  ws.getCell('J4').value = `Nb Plongeurs\n${allDivers.length} / ${slot.diverCount}`;
  ws.getCell('J4').alignment = { wrapText: true, vertical: 'middle', horizontal: 'center' };

  if (totalPages > 1) {
    const cell = ws.getCell('B2');
    const base  = typeof cell.value === 'string'
      ? cell.value.replace(/ – page \d+\/\d+$/, '')
      : 'Fiche de sécurité';
    cell.value = `${base} – page ${pageNum}/${totalPages}`;
  }
}

/**
 * Remplit les lignes plongeurs d'une feuille.
 * @param ws         Feuille cible
 * @param pageDivers Plongeurs de cette page (max 20)
 * @param modelStyles  Styles sauvegardés de la ligne modèle (R8)
 * @param modelHeight  Hauteur de la ligne modèle
 */
function fillPageDivers(
  ws: ExcelJS.Worksheet,
  pageDivers: SlotDiver[],
  modelStyles: Partial<ExcelJS.Style>[],
  modelHeight: number,
) {
  // D'abord effacer toutes les cellules des lignes plongeurs (sauf colonne M)
  for (const r of DIVER_ROWS) {
    for (const col of COLS_TO_CLEAR) {
      ws.getCell(r, col).value = null;
    }
  }

  // Remplir chaque plongeur sur sa ligne dédiée
  pageDivers.forEach((d, i) => {
    const r = DIVER_ROWS[i];

    // Restaurer le style modèle sur les colonnes concernées
    ws.getRow(r).height = modelHeight;
    for (const col of COLS_TO_CLEAR) {
      const cell = ws.getCell(r, col);
      cell.style = JSON.parse(JSON.stringify(modelStyles[col] ?? {}));
      cell.value = null;
    }

    // Remplir uniquement Nom (B) et Prénom (C)
    // A (N°), D (Aptitude), E (Fonction), H–M : non touchés (template d'origine)
    ws.getCell(r, 2).value = d.lastName.toUpperCase();      // B : Nom
    ws.getCell(r, 3).value = cap(d.firstName);              // C : Prénom
    // F (CACI) et G (Gaz) sont effacés via COLS_TO_CLEAR mais laissés vides pour saisie manuelle
  });
}

export async function exportFicheSecurite(slot: DiveSlot, divers: SlotDiver[]): Promise<void> {
  // ── Chargement du template ────────────────────────────────────────────────
  const response = await fetch('/templates/Fiche-de-securite-template.xlsx');
  if (!response.ok) throw new Error(`Impossible de charger le template : ${response.status}`);
  const arrayBuffer = await response.arrayBuffer();

  // Tri : directeur en premier, puis alphabétique
  const sorted = [...divers].sort((a, b) => {
    if (a.isDirector && !b.isDirector) return -1;
    if (!a.isDirector && b.isDirector) return 1;
    return a.lastName.localeCompare(b.lastName);
  });

  const totalPages = Math.max(1, Math.ceil(sorted.length / MAX_PER_PAGE));

  // ── Chargement du workbook ────────────────────────────────────────────────
  const wb = new ExcelJS.Workbook();
  await wb.xlsx.load(arrayBuffer);

  const ws1 = wb.getWorksheet(1);
  if (!ws1) throw new Error('Feuille introuvable dans le template');

  // Sauvegarder les styles de la ligne modèle (R8) avant toute modification
  const modelStyles: Partial<ExcelJS.Style>[] = [];
  for (const col of COLS_TO_CLEAR) {
    modelStyles[col] = JSON.parse(JSON.stringify(ws1.getCell(8, col).style ?? {}));
  }
  const modelHeight = ws1.getRow(8).height ?? 16;

  // ── Page 1 ─────��───────────────────────────────────────────���──────────────
  ws1.name = totalPages > 1 ? 'Page 1' : 'Fiche sécurité';
  fillHeader(ws1, slot, sorted, 1, totalPages);
  fillPageDivers(ws1, sorted.slice(0, MAX_PER_PAGE), modelStyles, modelHeight);

  if (slot.notes) {
    ws1.getCell('J33').value = `Notes : ${slot.notes}`;
  }

  // ── Pages supplémentaires ─────────────────────────────────────────────────
  for (let page = 2; page <= totalPages; page++) {
    const offset     = (page - 1) * MAX_PER_PAGE;
    const pageDivers = sorted.slice(offset, offset + MAX_PER_PAGE);

    // Cloner le template pour cette nouvelle page
    const wbTmp = new ExcelJS.Workbook();
    await wbTmp.xlsx.load(arrayBuffer);
    const wsTmp = wbTmp.getWorksheet(1)!;

    const wsNew = wb.addWorksheet(`Page ${page}`);
    const tmpModel: any = { ...wsTmp.model, name: `Page ${page}`, id: wsNew.id };

    // ExcelJS model setter skips Merge-type cells (non-master cells in merged
    // ranges), losing their border styles. Capture them before import.
    const mergeCellStyles: { address: string; style: any }[] = [];
    if (Array.isArray(tmpModel.rows)) {
      for (const row of tmpModel.rows) {
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

    wsNew.model = tmpModel as typeof wsNew.model;

    // Restore border styles on merged cells that were skipped
    for (const { address, style } of mergeCellStyles) {
      wsNew.getCell(address).style = style;
    }

    fillHeader(wsNew, slot, sorted, page, totalPages);
    fillPageDivers(wsNew, pageDivers, modelStyles, modelHeight);
  }

  // Supprimer les feuilles vides du template (Feuil2, Feuil3)
  ['Feuil2', 'Feuil3'].forEach(name => {
    const s = wb.getWorksheet(name);
    if (s) wb.removeWorksheet(s.id);
  });

  // Date de génération
  const now = new Date();
  const genLabel = `Généré le ${now.toLocaleDateString('fr-FR')} à ${now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`;
  wb.eachSheet(ws => { ws.getCell('A40').value = genLabel; });

  // ── Export ──────────────────────────────────────────────────────────���─────
  const buffer = await wb.xlsx.writeBuffer();
  downloadBlob(
    new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    `${slot.slotDate}-${slot.startTime.replace(':', '-')}-Fiche-securite-Saint-Lin.xlsx`,
  );
}
