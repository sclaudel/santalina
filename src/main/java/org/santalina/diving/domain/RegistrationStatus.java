package org.santalina.diving.domain;

/**
 * Statut d'une inscription en liste d'attente.
 *
 * <ul>
 *   <li>{@code PENDING_VERIFICATION} — inscription reçue, pièces jointes en attente de vérification par le DP.</li>
 *   <li>{@code VERIFIED}             — le DP a validé les pièces jointes ; le plongeur est confirmé en L.A.</li>
 *   <li>{@code INCOMPLETE}           — le DP a signalé un dossier incomplet ; l'inscription est annulée.</li>
 * </ul>
 */
public enum RegistrationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    INCOMPLETE
}
