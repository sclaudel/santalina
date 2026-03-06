import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';
import type { DiveSlot, SlotDiver } from '../types';

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
 *  Colonnes remplies : A(N°), B(Nom), C(Prénom), D(Aptitude), F(CACI vide), G(Gaz vide)
 *  Colonne M         : NE PAS TOUCHER (mise en forme du template)
 *  Colonne E (Fonction) : NE PAS REMPLIR (saisie manuelle)
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

// Colonnes à effacer/remplir (on exclut la colonne 13 = M)
const COLS_TO_CLEAR = [1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12]; // A B C D F G H I J K L

/** Remplit l'en-tête d'une feuille avec les infos du créneau */
function fillHeader(
  ws: ExcelJS.Worksheet,
  slot: DiveSlot,
  allDivers: SlotDiver[],
  pageNum: number,
  totalPages: number,
) {
  const director = allDivers.find(d => d.isDirector);
  const dirName  = director ? `${director.lastName.toUpperCase()} ${cap(director.firstName)}` : '';
  const dirLevel = director?.level ?? '';
  const dpInfo   = dirLevel ? `${dirName} (${dirLevel})` : dirName;

  ws.getCell('B4').value =
    `Date : ${fmtDate(slot.slotDate)}\nClub : ${slot.club ?? ''}\nNom, Prénom et Brevet du DP : ${dpInfo}`;

  const startHour = parseInt(slot.startTime.split(':')[0], 10);
  if (startHour < 13) {
    ws.getCell('H4').value = `AM\n${slot.startTime} – ${slot.endTime}`;
    ws.getCell('I4').value = 'PM';
  } else {
    ws.getCell('H4').value = 'AM';
    ws.getCell('I4').value = `PM\n${slot.startTime} – ${slot.endTime}`;
  }

  ws.getCell('J4').value = `${allDivers.length} / ${slot.diverCount}`;

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
 * @param globalOffset Index global du premier plongeur de la page
 * @param modelStyles  Styles sauvegardés de la ligne modèle (R8)
 * @param modelHeight  Hauteur de la ligne modèle
 */
function fillPageDivers(
  ws: ExcelJS.Worksheet,
  pageDivers: SlotDiver[],
  globalOffset: number,
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

    // Remplir les données (pas la colonne E=Fonction, pas la colonne M)
    ws.getCell(r, 1).value = globalOffset + i + 1;          // A : numéro
    ws.getCell(r, 2).value = d.lastName.toUpperCase();      // B : Nom
    ws.getCell(r, 3).value = cap(d.firstName);              // C : Prénom
    ws.getCell(r, 4).value = d.level ?? '';                 // D : Aptitude
    // E (Fonction) : laissée vide — saisie manuelle
    // F (CACI), G (Gaz), H-L (paramètres) : vides
    // M : non touché (mise en forme template)
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
  fillPageDivers(ws1, sorted.slice(0, MAX_PER_PAGE), 0, modelStyles, modelHeight);

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
    wsNew.model = { ...wsTmp.model, name: `Page ${page}`, id: wsNew.id } as typeof wsNew.model;

    fillHeader(wsNew, slot, sorted, page, totalPages);
    fillPageDivers(wsNew, pageDivers, offset, modelStyles, modelHeight);
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
  saveAs(
    new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    `fiche-securite_${slot.slotDate}_${slot.startTime.replace(':', 'h')}.xlsx`,
  );
}
