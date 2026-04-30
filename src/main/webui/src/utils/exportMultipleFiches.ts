/**
 * Exporte plusieurs fiches de sécurité (une par plongée) dans une archive ZIP unique.
 * Résout le blocage des téléchargements multiples sur mobile (iOS Safari, Android Chrome).
 */

import { zipSync } from 'fflate';
import type { DiveSlot, SlotDiver, Palanquee } from '../types';
import { buildFicheSecuriteBuffer } from './exportFicheSecuriteAvecPalanquees';

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

export interface FicheExportEntry {
  palanquees: Palanquee[];
  label: string;
  startTime?: string | null;
  endTime?: string | null;
}

/**
 * Génère N fiches de sécurité et les télécharge dans un fichier ZIP unique.
 * Un seul téléchargement → compatible mobile.
 */
export async function exportMultipleFichesZip(
  slot: DiveSlot,
  divers: SlotDiver[],
  entries: FicheExportEntry[],
): Promise<void> {
  const zipEntries: Record<string, Uint8Array> = {};

  for (const entry of entries) {
    const { buffer, filename } = await buildFicheSecuriteBuffer(
      slot,
      divers,
      entry.palanquees,
      entry.label,
      entry.startTime,
      entry.endTime,
    );
    zipEntries[filename] = new Uint8Array(buffer);
  }

  const zipped = zipSync(zipEntries, { level: 1 });
  const zipBlob = new Blob([zipped.buffer as ArrayBuffer], { type: 'application/zip' });
  downloadBlob(zipBlob, `${slot.slotDate}-Fiches-securite.zip`);
}
