import type { DiveSlot } from '../types';

/**
 * Ouvre le fichier .ics pour déclencher l'association OS vers l'app calendrier.
 * Utilise window.open() sans attribut download pour laisser le navigateur/OS
 * décider d'ouvrir directement l'application (Calendar, Outlook, Google Agenda…).
 * Sur les navigateurs qui ne gèrent pas l'ouverture automatique, le fichier est téléchargé.
 */
export function downloadSlotIcs(slot: DiveSlot): void {
  window.open(`/api/slots/${slot.id}/ics`, '_blank');
}
