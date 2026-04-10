import type { DiveSlot, SlotDiver } from '../types';

function fmtDate(d: string): string {
  const [y, m, day] = d.split('-');
  return `${day}/${m}/${y}`;
}

/**
 * Génère et télécharge un fichier CSV avec la liste des plongeurs inscrits sur un créneau.
 * Colonnes : Nom, Prénom, Niveau, Email
 */
export function exportDiverListCsv(slot: DiveSlot, divers: SlotDiver[]): void {
  const dateLabel = slot.slotDate ? fmtDate(slot.slotDate) : '';
  const filename = `${slot.slotDate ?? 'creneau'}-${slot.startTime?.replace(':', '-') ?? ''}-liste-plongeurs.csv`;

  // En-tête CSV
  const lines: string[] = ['\uFEFF' + 'Nom;Prénom;Niveau;Email;Directeur de plongée;Date certificat médical;Commentaire'];

  // Tri : DP en premier, puis alphabétique
  const sorted = [...divers].sort(
    (a, b) => (b.isDirector ? 1 : 0) - (a.isDirector ? 1 : 0) || a.lastName.localeCompare(b.lastName),
  );

  for (const d of sorted) {
    const fields = [
      d.lastName ?? '',
      d.firstName ?? '',
      d.level ?? '',
      d.email ?? '',
      d.isDirector ? 'Oui' : '',
      d.medicalCertDate ? fmtDate(d.medicalCertDate) : '',
      d.comment ?? '',
    ].map(f => `"${f.replace(/"/g, '""')}"`);
    lines.push(fields.join(';'));
  }

  const csv = lines.join('\r\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);

  console.info(`[exportDiverList] ${dateLabel} — ${divers.length} plongeur(s) exporté(s)`);
}
