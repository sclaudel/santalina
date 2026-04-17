/**
 * Données par défaut pour l'email d'organisation du directeur de plongée.
 * Ce module est le miroir frontend du DEFAULT_TEMPLATE Java (DpOrganizerMailer.java).
 *
 * Variables disponibles : {siteName}, {slotDate}, {startTime}, {endTime},
 *                         {slotTitle}, {dpName}, {dpEmail}, {dpPhone}
 */

const DEFAULT_TEMPLATE =
  '<p>Bonjour à tous,</p>' +
  '<p>Vous êtes inscrits à la sortie du <strong>{slotDate}</strong> à la {siteName} ' +
  '(RDV à la {siteName} à {startTime}).</p>' +
  '<p>Voici quelques informations utiles à l\'organisation de cette plongée.</p>' +
  '<h3>Horaires</h3>' +
  '<ul>' +
  '<li>Récupérez le matériel dans votre club, 1 bloc pour chaque plongée.</li>' +
  '<li>Pour ceux qui ont déjà tout le matériel, rendez-vous directement à la {siteName} à {startTime}. ' +
  'Dans ce cas merci de me prévenir.</li>' +
  '<li>Prévoyez de quoi pique-niquer sur place après la plongée et de vous hydrater ' +
  '(1 bouteille d\'eau ou gourde par personne).</li>' +
  '</ul>' +
  '<h3>Administratif</h3>' +
  '<ul>' +
  '<li>Pensez à prendre les papiers nécessaires à la plongée :<br/>' +
  'Certificat médical, Carte de niveau, Carnet de plongée, Licence</li>' +
  '</ul>' +
  '<p><strong>==&gt; Pas de papiers = Pas de plongée</strong></p>' +
  '<ul>' +
  '<li>Prenez également vos fiches de progression et de suivi.</li>' +
  '<li>Pensez à contrôler vos papiers avant samedi matin\u00a0! Licence active et CACI valide\u00a0!</li>' +
  '</ul>' +
  '<h3>Météo</h3>' +
  '<p>Pensez à prendre des vêtements chauds pour couvrir entre les 2 plongées.</p>' +
  '<p>La température de l\'eau peut être inférieure à 10\u00a0°C au-delà de 3\u00a0m. ' +
  'Prévoyez une combinaison adaptée, des gants, des chaussons et une cagoule. ' +
  'Pensez également à prendre vos plombs\u00a0!</p>' +
  '<h3>Règles de vie à la {siteName}</h3>' +
  '<ul>' +
  '<li>Les véhicules doivent être stationnés sur les emplacements prévus à cet effet. ' +
  'Attention\u00a0! ne pas se garer sur la pelouse des locataires.</li>' +
  '<li>Au niveau du plan d\'eau, aucun véhicule ne doit stationner. ' +
  'On peut décharger/charger son matériel, puis se garer sur le parking au niveau du local.</li>' +
  '<li>Pour le pique-nique, nous le prendrons dans le local pour ne pas avoir froid.</li>' +
  '</ul>' +
  '<h3>Rappel sur le règlement spécifique de la {siteName}</h3>' +
  '<ul>' +
  '<li>Espace médian 6-20\u00a0m\u00a0: chaque plongeur au-delà de 20\u00a0m doit être équipé de 2 premiers étages ' +
  'et d\'une lampe flash.</li>' +
  '<li>Espace lointain +20\u00a0m\u00a0: le guide de palanquée doit avoir une source de lumière ' +
  '(autre que la lampe flash).</li>' +
  '</ul>' +
  '<p>La présence d\'un parachute de palier par palanquée (pour les encadrants) est obligatoire ' +
  '(Code du sport).</p>' +
  '<p>En cas de contre-temps ou de retard, merci de me prévenir au plus tôt par mail ou téléphone ' +
  'afin que je puisse ajuster mon organisation.</p>' +
  '<p>({dpPhone} ou {dpEmail}).</p>' +
  '<p>Je reste à votre disposition pour toutes questions complémentaires.</p>' +
  '<p>Bonne fin de semaine à tous et à bientôt,</p>' +
  '<p><strong>{dpName}</strong><br/>{dpPhone}</p>';

/** Miroir frontend de DpOrganizerMailer.DEFAULT_TEMPLATE (Java) */
export const DpOrganizerMailer = {
  DEFAULT_TEMPLATE,
};
