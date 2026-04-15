import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import type { PdfSection } from '../utils/exportHelpPdf';

interface Section {
  id: string;
  icon: string;
  title: string;
  roles?: string[]; // si défini, section visible uniquement pour ces rôles
  content: React.ReactNode;
}

export function HelpPage() {
  const { user } = useAuth();
  const [openSection, setOpenSection] = useState<string | null>('calendrier');

  const role = user?.role ?? 'GUEST';
  const siteName = document.title || 'Santalina Plongée';

  const toggle = (id: string) => setOpenSection(prev => (prev === id ? null : id));

  const sections: Section[] = [
    // ── CALENDRIER ──────────────────────────────────────────────────────────
    {
      id: 'calendrier',
      icon: '📅',
      title: 'Consulter le calendrier',
      content: (
        <>
          <p>Le calendrier est la page principale de l'application. Il affiche tous les créneaux de plongée disponibles.</p>
          <h4>Changer de vue</h4>
          <ul>
            <li><strong>Jour</strong> — affiche tous les créneaux d'une journée sur une grille horaire.</li>
            <li><strong>Semaine</strong> — affiche 7 colonnes côte à côte, une par jour.</li>
            <li><strong>Mois</strong> — vue mensuelle avec un résumé des créneaux par jour.</li>
          </ul>
          <p>Utilisez les boutons <strong>◀ ▶</strong> pour naviguer d'une période à l'autre, ou cliquez sur un jour dans le mini-calendrier (barre latérale à gauche) pour aller directement à cette date.</p>
          <h4>Ouvrir les détails d'un créneau</h4>
          <p>Cliquez sur un bloc de créneau pour afficher le panneau de détails : horaires, nombre de plongeurs inscrits, directeur de plongée, etc.</p>
        </>
      ),
    },

    // ── CRÉER UN CRÉNEAU ─────────────────────────────────────────────────────
    {
      id: 'creer-creneau',
      icon: '➕',
      title: 'Créer un créneau',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>La création de créneaux est réservée aux <strong>administrateurs</strong> et aux <strong>directeurs de plongée</strong>.</p>

          <h4>Bouton « + Nouveau créneau » (toutes les vues)</h4>
          <ol>
            <li>Cliquez sur le bouton <strong>+ Nouveau créneau</strong> en haut à droite du calendrier (visible quelle que soit la vue active).</li>
            <li>Remplissez le formulaire : <strong>date</strong>, heure de début, heure de fin, titre (optionnel), type de plongée, club, nombre de plongeurs, notes.</li>
            <li>La date est préremplie avec la date sélectionnée dans le calendrier, mais vous pouvez la modifier directement dans le formulaire.</li>
            <li>Cliquez sur <strong>Créer</strong>.</li>
          </ol>

          <h4>Depuis la vue Jour</h4>
          <ol>
            <li>Survolez (sur ordinateur) ou appuyez (mobile) sur la <strong>grille horaire</strong> à l'heure souhaitée.</li>
            <li>Cliquez sur le bouton <strong>+</strong> qui apparaît sur la plage horaire ciblée — l'heure de début est préremplie automatiquement.</li>
            <li>Si la journée est entièrement vide, un bouton <strong>+ Créer le premier créneau</strong> apparaît également au centre de la vue.</li>
          </ol>

          <h4>Depuis la vue Semaine</h4>
          <ol>
            <li>Survolez (sur ordinateur) ou appuyez (mobile) sur une <strong>cellule horaire</strong> dans la colonne du jour souhaité.</li>
            <li>Cliquez sur le bouton <strong>+</strong> qui apparaît — la date et l'heure de début sont préremplies.</li>
          </ol>

          <h4>Depuis la vue Mois</h4>
          <ol>
            <li>Survolez (sur ordinateur uniquement) ou appuyez sur une cellule de jour.</li>
            <li>Cliquez sur le bouton <strong>+</strong> qui apparaît en bas à droite de la cellule.</li>
          </ol>

          {role === 'ADMIN' && (
            <>
              <h4>👤 Créer un créneau pour le compte d'un autre DP (administrateurs)</h4>
              <p>En tant qu'administrateur, vous pouvez créer un créneau qui sera attribué à un autre directeur de plongée. Cela est utile pour pré-créer les créneaux de DP externes ou migrer des créneaux depuis une autre application.</p>
              <ol>
                <li>Ouvrez le formulaire de création de créneau.</li>
                <li>En bas du formulaire, le champ <strong>Créer pour le compte de…</strong> affiche la liste des directeurs de plongée actifs.</li>
                <li>Sélectionnez le DP pour qui créer le créneau. Laissez sur <em>Moi-même</em> pour créer un créneau en votre propre nom.</li>
                <li>Validez normalement — le créneau apparaît avec le nom du DP sélectionné comme créateur.</li>
              </ol>
            </>
          )}

          <div className="help-tip">💡 La date proposée dans le formulaire correspond à votre sélection dans le calendrier. Vous pouvez la modifier avant de valider pour créer un créneau à une autre date.</div>
        </>
      ),
    },

    // ── MODIFIER UN CRÉNEAU ──────────────────────────────────────────────────
    {
      id: 'modifier-creneau',
      icon: '✏️',
      title: 'Modifier un créneau',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>Après création, il est possible de modifier toutes les informations d'un créneau, y compris sa date, ses horaires (déplacement) et sa capacité.</p>
          <ol>
            <li>Cliquez sur le créneau pour ouvrir le panneau de détails.</li>
            <li>Cliquez sur l'icône <strong>✏️</strong> à côté du titre du créneau.</li>
            <li>Modifiez la <strong>date</strong>, l'<strong>heure de début</strong>, l'<strong>heure de fin</strong>, le <strong>nombre de places</strong>, le titre, le type, le club ou les notes.</li>
            <li>Cliquez sur <strong>Enregistrer</strong>.</li>
          </ol>
          <div className="help-tip">💡 Le déplacement d'un créneau respecte les mêmes règles que la création : il est impossible de choisir une date passée, et la capacité de la plage horaire cible est vérifiée automatiquement.</div>
        </>
      ),
    },

    // ── SUPPRIMER UN CRÉNEAU ─────────────────────────────────────────────────
    {
      id: 'supprimer-creneau',
      icon: '🗑️',
      title: 'Supprimer un créneau',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>La suppression d'un créneau est définitive et supprime également tous les plongeurs inscrits.</p>
          <ol>
            <li>Cliquez sur le créneau pour ouvrir le panneau de détails.</li>
            <li>Faites défiler vers le bas et cliquez sur <strong>🗑 Supprimer le créneau</strong>.</li>
            <li>La suppression est immédiate.</li>
          </ol>
          <div className="help-warning">⚠️ Cette action est irréversible. Tous les plongeurs inscrits sur ce créneau seront également supprimés.</div>
        </>
      ),
    },

    // ── AJOUTER UN PLONGEUR ──────────────────────────────────────────────────
    {
      id: 'ajouter-plongeur',
      icon: '🤿',
      title: 'Ajouter un plongeur à un créneau',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>Seuls les <strong>administrateurs</strong> et les <strong>directeurs de plongée</strong> peuvent inscrire des plongeurs.</p>
          <ol>
            <li>Cliquez sur le créneau pour ouvrir le panneau de détails.</li>
            <li>Cliquez sur <strong>+ Ajouter un plongeur</strong>.</li>
            <li>
              <strong>Option 1 — Utilisateur existant :</strong> tapez son nom dans le champ de recherche et sélectionnez-le. Son prénom, nom et niveau seront préremplis.
            </li>
            <li>
              <strong>Option 2 — Saisie manuelle :</strong> renseignez directement le prénom, le nom et le niveau de certification.
            </li>
            <li>Cliquez sur <strong>Ajouter</strong>.</li>
          </ol>
          <div className="help-tip">💡 Le nombre de plongeurs inscrits est limité par la capacité du créneau (nombre de places). Quand il est atteint, le bouton d'ajout disparaît.</div>
        </>
      ),
    },

    // ── DIRECTEUR DE PLONGÉE ─────────────────────────────────────────────────
    {
      id: 'directeur',
      icon: '🎖️',
      title: 'Désigner un directeur de plongée',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>Chaque créneau peut avoir <strong>un seul directeur de plongée</strong>. Il est identifié par le badge 🎖️ dans la liste des plongeurs.</p>
          <h4>Lors de l'ajout d'un plongeur</h4>
          <ol>
            <li>Dans le formulaire d'ajout, cochez <strong>🎖 Directeur de plongée sur ce créneau</strong>.</li>
            <li>Renseignez son <strong>email</strong> et son <strong>téléphone</strong> (obligatoires pour le directeur).</li>
            <li>Renseignez optionnellement son <strong>N° de licence</strong>. Si le directeur est sélectionné depuis la recherche d'utilisateurs, son numéro de licence est prérempli automatiquement depuis son profil.</li>
            <li>Cliquez sur <strong>Ajouter</strong>.</li>
          </ol>
          <h4>Sur un plongeur déjà inscrit</h4>
          <ol>
            <li>Dans la liste des plongeurs, cliquez sur l'icône ✏️ à côté du nom.</li>
            <li>Cochez <strong>Directeur de plongée</strong> et renseignez les coordonnées.</li>
            <li>Cliquez sur <strong>Enregistrer</strong>.</li>
          </ol>
          <h4>S'auto-assigner comme directeur (depuis le calendrier)</h4>
          <p>
            Si un créneau n'a pas encore de directeur de plongée, tout directeur de plongée peut
            s'y désigner directement depuis le panneau de détails du créneau.
          </p>
          <ol>
            <li>Ouvrez le créneau (il doit être <strong>sans directeur</strong> actuellement).</li>
            <li>Cliquez sur le bouton <strong>🤿 M'assigner comme DP sur ce créneau</strong>.</li>
            <li>Vos informations de profil (prénom, nom, email, téléphone, n° de licence) sont utilisées automatiquement.</li>
            <li>Une fois assigné, vous pouvez <strong>configurer les inscriptions libres</strong> et gérer les plongeurs du créneau.</li>
          </ol>
          <div className="help-tip">💡 Ce bouton n'est visible que si le créneau n'a pas encore de DP. Il apparaît indépendamment de l'état des inscriptions libres. Votre <strong>numéro de téléphone</strong> est repris directement depuis <strong>Mon profil</strong> — assurez-vous qu'il est bien renseigné avant de vous assigner.</div>
          <div className="help-tip">💡 Les coordonnées du directeur (email, téléphone, N° de licence) sont exportées dans la fiche de sécurité Excel. La licence s'affiche sur la même ligne que le nom et le niveau : ex. <code>DUPONT Jean - MF2 - 12345678A</code>.</div>
        </>
      ),
    },

    // ── SUPPRIMER UN PLONGEUR ────────────────────────────────────────────────
    {
      id: 'supprimer-plongeur',
      icon: '❌',
      title: 'Retirer un plongeur d\'un créneau',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <ol>
            <li>Cliquez sur le créneau pour ouvrir le panneau de détails.</li>
            <li>Dans la liste des plongeurs, survolez (sur ordinateur uniquement) ou appuyez (mobile) sur le nom du plongeur.</li>
            <li>Cliquez sur la croix <strong>✕</strong> à droite de son nom.</li>
          </ol>
          <p>La place est immédiatement libérée.</p>
        </>
      ),
    },

    // ── EXPORTER FICHE SÉCURITÉ ──────────────────────────────────────────────
    {
      id: 'export-fiche',
      icon: '📊',
      title: 'Exporter la fiche de sécurité',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>Une <strong>fiche de sécurité Excel</strong> peut être générée pour chaque créneau. Elle récapitule les plongeurs inscrits et les informations du directeur de plongée.</p>
          <ol>
            <li>Cliquez sur le créneau pour ouvrir le panneau de détails.</li>
            <li>Cliquez sur <strong>📊 Exporter fiche de sécurité (Excel)</strong>.</li>
            <li>Le fichier <code>.xlsx</code> est téléchargé automatiquement.</li>
          </ol>
          <h4>Contenu de la fiche</h4>
          <ul>
            <li><strong>Cellule B4</strong> — Date, club et informations du directeur sur une seule ligne : <code>NOM Prénom - Niveau - N°Licence</code> (la licence n'apparaît que si elle est renseignée).</li>
            <li><strong>Colonnes A–C</strong> — Nom, prénom et niveau de certification de chaque plongeur.</li>
            <li><strong>Colonne D</strong> — Aptitudes du plongeur si elles ont été renseignées dans l'organisation des palanquées (ex : PE20, PA40, GP…).</li>
            <li>Si des palanquées sont organisées, chaque palanquée est présentée dans un tableau séparé, sinon tous les plongeurs apparaissent dans un tableau unique.</li>
          </ul>
          <div className="help-tip">💡 Pour que la licence du DP apparaisse dans l'export, elle doit être saisie dans son profil utilisateur ou directement dans le formulaire DP sur le créneau.</div>

          <h4>Exporter la liste en CSV</h4>
          <p>Un export CSV de la liste des plongeurs est également disponible depuis le panneau de détails du créneau.</p>
          <ol>
            <li>Cliquez sur le créneau pour ouvrir le panneau de détails.</li>
            <li>Cliquez sur <strong>📥 Télécharger liste CSV</strong>.</li>
            <li>Le fichier <code>.csv</code> est téléchargé automatiquement (encodage UTF-8, séparateur <code>;</code>).</li>
          </ol>
          <h4>Contenu du CSV</h4>
          <ul>
            <li><strong>Nom</strong> — nom de famille en majuscules.</li>
            <li><strong>Prénom</strong> — prénom du plongeur.</li>
            <li><strong>Niveau</strong> — niveau de certification.</li>
            <li><strong>Email</strong> — adresse e-mail si renseignée.</li>
            <li><strong>Directeur de plongée</strong> — « Oui » si le plongeur est le directeur du créneau.</li>
            <li><strong>Club</strong> — club d'appartenance repris depuis le profil du plongeur lors de l'inscription libre (vide si le plongeur a été ajouté manuellement par un DP).</li>
            <li><strong>Date certificat médical</strong> — date fournie lors de l'inscription libre (vide si le plongeur a été ajouté manuellement par un DP).</li>
            <li><strong>Commentaire</strong> — commentaire laissé par le plongeur lors de l'inscription libre (vide si ajout manuel).</li>
          </ul>
        </>
      ),
    },

    // ── ORGANISATION DES PALANQUÉES ──────────────────────────────────────────
    {
      id: 'palanquees',
      icon: '🫧',
      title: 'Organiser les palanquées',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>La page d'organisation des palanquées permet de répartir les plongeurs inscrits sur un créneau en groupes de plongée, par glisser-déposer.</p>
          <h4>Accéder à la page</h4>
          <ol>
            <li>Cliquez sur un créneau pour ouvrir le panneau de détails.</li>
            <li>Cliquez sur <strong>🫧 Organiser les palanquées</strong>.</li>
          </ol>
          <h4>Répartir les plongeurs</h4>
          <ul>
            <li>Les plongeurs non affectés apparaissent dans la zone <strong>Non assignés</strong>.</li>
            <li><strong>Glissez-déposez</strong> une fiche plongeur vers une palanquée existante ou vers <strong>+ Nouvelle palanquée</strong> pour créer un nouveau groupe.</li>
            <li>Pour retirer un plongeur d'une palanquée, glissez-le vers la zone <strong>Non assignés</strong>.</li>
          </ul>
          <h4>Modifier le niveau d'un plongeur</h4>
          <ul>
            <li><strong>Double-cliquez</strong> sur le niveau affiché sur la fiche (ex : MF1) pour ouvrir un menu déroulant et le modifier.</li>
          </ul>
          <h4>Renseigner les aptitudes d'un plongeur</h4>
          <ul>
            <li><strong>Double-cliquez</strong> sur la zone aptitudes (sous le niveau, affichée en grisé « aptitudes » si vide) pour ouvrir un menu déroulant.</li>
            <li>Sélectionnez parmi : <strong>PE12, PE20, PE40, PE60, PA12, PA20, PA40, PA60, E1, E2, E3, E4, GP</strong>.</li>
            <li>Choisissez <strong>— aucune —</strong> pour effacer l'aptitude.</li>
            <li>Les aptitudes saisies ici apparaissent dans la <strong>colonne D</strong> de la fiche de sécurité Excel exportée.</li>
          </ul>
          <h4>Définir la profondeur et le temps d'une palanquée</h4>
          <ul>
            <li>Dans l'en-tête de chaque colonne palanquée, deux menus déroulants compacts sont affichés : <strong>Prof. ▾</strong> et <strong>Temps ▾</strong>.</li>
            <li>Cliquez sur <strong>Prof. ▾</strong> pour choisir la profondeur maximale : 6 m, 12 m, 20 m, 30 m, 40 m, 50 m ou 60 m.</li>
            <li>Cliquez sur <strong>Temps ▾</strong> pour choisir la durée maximale : de 10' à 240' par pas de 10 minutes.</li>
            <li>Ces valeurs sont exportées dans la fiche de sécurité Excel, dans les cellules <strong>Profondeur max</strong> et <strong>Temps max</strong> de chaque groupe de plongeurs.</li>
          </ul>
          <h4>Remettre un plongeur en liste d'attente</h4>
          <ul>
            <li>Lorsque les <strong>inscriptions sont ouvertes</strong> sur le créneau, un bouton <strong>Remettre en liste d'attente</strong> apparaît en bas de chaque fiche plongeur (hors directeur de plongée assigné).</li>
            <li>Cliquez sur ce bouton pour annuler la place du plongeur et le replacer en liste d'attente : une confirmation est demandée avant l'action.</li>
            <li>Ce bouton est <strong>masqué automatiquement</strong> lorsque les inscriptions sont fermées ou pas encore ouvertes sur le créneau.</li>
          </ul>
          <div className="help-tip">💡 Les modifications de niveau, aptitudes, profondeur et temps sont enregistrées immédiatement sur le serveur.</div>
        </>
      ),
    },

    // ── INSCRIPTION LIBRE ─────────────────────────────────────────────────────
    {
      id: 'inscription-libre',
      icon: '📋',
      title: 'Inscription libre sur un créneau',
      content: (
        <>
          <p>
            Lorsqu'un directeur de plongée a <strong>activé les inscriptions libres</strong> sur un créneau,
            tout plongeur inscrit sur la plateforme peut soumettre une demande d'inscription.
          </p>

          <h4>Pour les plongeurs (DIVER)</h4>
          <ol>
            <li>Un pictogramme 📋 apparaît sur les créneaux ouverts aux inscriptions libres.</li>
            <li>Cliquez sur le créneau, puis sur <strong>📋 S'inscrire sur la liste d'attente</strong>.</li>
            <li>
              Remplissez le formulaire d'inscription :
              <ul>
                <li><strong>Prénom / Nom</strong> — champs obligatoires.</li>
                <li><strong>E-mail</strong> (double saisie obligatoire pour éviter les erreurs)</li>
                <li><strong>Niveau de certification</strong> — sélectionner parmi la liste configurée par l'administrateur (par défaut : N1, N2, N3, N4, N5, E2, E3, E4, PE12, PE40, PE60, MF1, MF2). Les directeurs de plongée disposent d'une liste spécifique.</li>
                <li><strong>Club d'appartenance</strong> — repris automatiquement depuis votre profil utilisateur. Pour modifier votre club, rendez-vous sur la page <strong>Mon profil</strong>.</li>
                <li><strong>Nombre de plongées effectuées</strong> — champ obligatoire.</li>
                <li><strong>Date de la dernière plongée</strong> — champ obligatoire.</li>
                <li><strong>Niveau en préparation</strong> — optionnel (liste configurable par l'administrateur).</li>
                <li><strong>Commentaire pour le DP</strong> — optionnel, pour indiquer ce que vous souhaitez travailler ou faire durant la plongée.</li>
                <li><strong>Date de début du certificat médical</strong> — <em>obligatoire</em>. Le certificat doit avoir moins d'un an à la date du créneau.</li>
                <li>
                  <strong>Confirmation de la validité de la licence FFESSM</strong> — <em>obligatoire</em>.
                  Vous devez cocher : « Je confirme avoir vérifié sur le site de la FFESSM la validité de ma licence : OUI ».
                </li>
              </ul>
            </li>
            <li>Cliquez sur <strong>M'inscrire sur la liste d'attente</strong>.</li>
            <li>Un <strong>e-mail de confirmation</strong> est envoyé à l'adresse renseignée.</li>
          </ol>
          <div className="help-tip">
            💡 Votre demande sera examinée par le directeur de plongée. Vous recevrez un e-mail de validation lorsqu'il aura accepté votre inscription.
          </div>

          <h4>Annuler sa demande d'inscription</h4>
          <p>
            Si vous souhaitez annuler votre demande, contactez directement le directeur de plongée du créneau.
            Une annulation après validation de votre inscription génère automatiquement un e-mail de notification au directeur.
          </p>
          <div className="help-warning">
            ⚠️ L'annulation peut perturber l'organisation des palanquées. Une fenêtre de confirmation vous rappelle cet impact avant de valider.
          </div>

          <h4>Pour les directeurs de plongée (DIVE_DIRECTOR)</h4>
          <ol>
            <li>Ouvrez le panneau de détails du créneau.</li>
            <li>
              Cliquez sur <strong>⚙️ Configurer les inscriptions libres</strong>.
              <ul>
                <li>Cochez <strong>Autoriser l'inscription libre des plongeurs</strong> pour ouvrir les inscriptions.</li>
                <li>Renseignez optionnellement une <strong>date d'ouverture</strong> (les inscriptions ne seront acceptées qu'à partir de cette date/heure).</li>
                <li>Sans date d'ouverture, les inscriptions sont actives immédiatement.</li>
              </ul>
            </li>
            <li>Cliquez sur <strong>✓ Enregistrer</strong>.</li>
          </ol>
          <div className="help-tip">💡 Le bouton « Configurer les inscriptions libres » n'est visible que pour le <strong>directeur de plongée assigné</strong> sur le créneau ou son <strong>créateur</strong>. Si vous n'êtes pas encore assigné comme DP, utilisez d'abord le bouton 🤿 M'assigner comme DP.</div>

          <h4>Gérer la liste d'attente</h4>
          <p>
            La liste d'attente est accessible depuis la page <strong>Organisation des palanquées</strong> de votre créneau.
            Elle est visible uniquement par vous (DP du créneau) et les administrateurs.
          </p>
          <ol>
            <li>Les inscriptions apparaissent dans l'ordre d'arrivée (#1 = le premier inscrit).</li>
            <li>
              Pour chaque entrée, vous voyez : nom, niveau, club d'appartenance (repris depuis le profil du plongeur), e-mail, nombre de plongées, date de la dernière plongée, niveau en préparation et commentaire éventuel.
            </li>
            <li>
              Cliquez sur <strong>✓ Valider</strong> pour accepter la demande :
              le plongeur est transféré dans la liste des inscrits et reçoit un e-mail de confirmation après un délai de 15 minutes
              (pour laisser le temps au DP de réorganiser les palanquées avant que le plongeur soit notifié).
              La <strong>date du certificat médical</strong>, le <strong>commentaire</strong> et le <strong>club d'appartenance</strong> sont conservés et apparaîtront dans la liste des inscrits.
            </li>
            <li>
              Cliquez sur <strong>✕</strong> pour refuser / supprimer la demande ; 
              le plongeur ayant un compte reçoit un e-mail d'annulation.
            </li>
          </ol>
          <div className="help-tip">
            💡 Les plongeurs validés apparaissent dans le pool <strong>Non assignés</strong> de la page d'organisation des palanquées.
          </div>

          <h4>Remettre un plongeur en liste d'attente</h4>
          <p>
            Depuis la vue <strong>Organisation des palanquées</strong>, chaque post-it de plongeur (hors directeur de plongée)
            affiche un bouton <strong>Remettre en liste d'attente</strong>.
            Ce bouton est accessible au DP du créneau et aux administrateurs.
          </p>
          <ol>
            <li>Cliquez sur <strong>Remettre en liste d'attente</strong> sur le post-it du plongeur concerné.</li>
            <li>Confirmez l'action dans la fenêtre de confirmation.</li>
            <li>Le plongeur est retiré de sa palanquée et replacé en liste d'attente.</li>
            <li>Il reçoit un e-mail de notification après un délai de 15 minutes.</li>
          </ol>
        </>
      ),
    },

    // ── SAUVEGARDE & RESTAURATION (ADMIN) ───────────────────────────────────
    {
      id: 'sauvegarde',
      icon: '💾',
      title: 'Sauvegarde et restauration',
      roles: ['ADMIN'],
      content: (
        <>
          <p>La section <strong>💾 Sauvegarde</strong> est accessible via le menu utilisateur → <strong>⚙️ Administration → onglet 🛠️ Opérations</strong>.</p>

          <h4>Exporter une sauvegarde complète</h4>
          <p>Exporte l'intégralité des données : configuration, utilisateurs, créneaux, plongeurs, palanquées et listes d'attente.</p>
          <ol>
            <li>Cliquez sur <strong>💾 Exporter une sauvegarde complète</strong>.</li>
            <li>Un fichier <code>.json</code> est téléchargé automatiquement (<code>santalina-full-AAAA-MM-JJ.json</code>).</li>
          </ol>
          <div className="help-tip">💡 À utiliser avant une mise à jour importante ou pour migrer l'application vers un nouveau serveur.</div>

          <h4>Exporter configuration + utilisateurs</h4>
          <p>Exporte uniquement la configuration de l'application et les comptes utilisateurs (sans créneaux, plongeurs ni listes d'attente).</p>
          <ol>
            <li>Cliquez sur <strong>📋 Exporter configuration + utilisateurs</strong>.</li>
            <li>Un fichier <code>santalina-config-users-AAAA-MM-JJ.json</code> est téléchargé.</li>
          </ol>
          <div className="help-tip">💡 Pratique pour réinitialiser les données plongée tout en conservant les comptes et la configuration du site.</div>

          <h4>Importer une sauvegarde</h4>
          <p>Restaure une sauvegarde précédemment exportée.</p>
          <ol>
            <li>Cliquez sur <strong>📂 Choisir un fichier</strong> et sélectionnez le fichier <code>.json</code>.</li>
            <li>Cliquez sur <strong>⬆️ Importer</strong>.</li>
            <li>Un résumé indique le nombre d'éléments restaurés (config, utilisateurs, créneaux, plongeurs, palanquées, listes d'attente).</li>
          </ol>
          <div className="help-warning">⚠️ L'import est irréversible : toutes les données existantes sont supprimées avant la restauration. Effectuez une sauvegarde complète juste avant si nécessaire.</div>
        </>
      ),
    },

    // ── CONNEXION / INSCRIPTION ───────────────────────────────────────────────
    {
      id: 'connexion',
      icon: '🔐',
      title: 'Se connecter / S\'inscrire',
      content: (
        <>
          <h4>Se connecter</h4>
          <ol>
            <li>Cliquez sur <strong>🔐 Connexion</strong> dans la barre de navigation.</li>
            <li>Saisissez votre email et votre mot de passe.</li>
            <li>Cliquez sur <strong>Se connecter</strong>.</li>
          </ol>
          <h4>S'inscrire (si activé)</h4>
          <ol>
            <li>Cliquez sur <strong>🔐 Connexion</strong>, puis sur <strong>Pas encore de compte ? S'inscrire</strong>.</li>
            <li>Remplissez votre <strong>prénom</strong>, <strong>nom</strong>, <strong>email</strong> et <strong>téléphone</strong>, puis sélectionnez optionnellement votre <strong>club d'appartenance</strong>, et acceptez la politique de confidentialité.</li>
            <li>Un <strong>email de confirmation</strong> vous est envoyé. Aucun mot de passe n'est demandé à ce stade.</li>
            <li>Cliquez sur le lien reçu par email pour accéder à la page d'activation.</li>
            <li>Définissez votre mot de passe&nbsp;: vous êtes automatiquement connecté.</li>
            <li>Votre compte est créé avec le rôle <strong>Plongeur</strong>. Un administrateur peut ensuite modifier votre rôle.</li>
          </ol>
          <div className="help-tip">💡 Le lien d'activation est valable <strong>24 heures</strong>. Si vous ne le retrouvez pas, vérifiez le dossier spam.</div>
          <h4>Mot de passe oublié</h4>
          <ol>
            <li>Dans la fenêtre de connexion, cliquez sur <strong>Mot de passe oublié ?</strong>.</li>
            <li>Saisissez votre email. Un lien de réinitialisation (valable 30 minutes) vous sera envoyé.</li>
          </ol>
        </>
      ),
    },

    // ── MON PROFIL ────────────────────────────────────────────────────────────
    {
      id: 'profil',
      icon: '👤',
      title: 'Mon profil',
      content: (
        <>
          <p>La page <strong>Mon profil</strong> vous permet de consulter et modifier vos informations personnelles.</p>
          <ul>
            <li><strong>Prénom</strong> et <strong>Nom</strong> — affichés dans le menu et les listes de plongeurs.</li>
            <li><strong>Email</strong> — identifiant de connexion.</li>
            <li><strong>Téléphone</strong> — champ <em>optionnel</em>. Format attendu : <code>0612345678</code> ou <code>+33612345678</code>. Utilisé pour contacter le directeur de plongée.</li>
            <li><strong>N° de licence fédérale</strong> — champ <em>optionnel</em>. Format attendu : <code>A-14-1234567890</code> (lettre, tiret, 2 chiffres, tiret, 6 à 10 chiffres). S'il est renseigné, il sera automatiquement repris lorsque vous êtes sélectionné comme directeur de plongée sur un créneau, et apparaîtra dans la fiche de sécurité Excel.</li>
            <li><strong>Club d'appartenance</strong> — champ <em>optionnel</em>. Votre club est automatiquement repris lors de votre inscription en liste d'attente sur un créneau ouvert aux inscriptions libres, et apparaît dans la liste d'attente visible par le directeur de plongée.</li>
            <li><strong>Mot de passe</strong> — modifiable depuis cette page (champ optionnel, laissez vide pour ne pas le changer).</li>
          </ul>
          <p>Accès : cliquez sur votre nom dans la barre de navigation → <strong>👤 Mon profil</strong>.</p>
        </>
      ),
    },

    // ── GESTION UTILISATEURS (ADMIN) ─────────────────────────────────────────
    {
      id: 'gestion-utilisateurs',
      icon: '👥',
      title: 'Gérer les utilisateurs',
      roles: ['ADMIN'],
      content: (
        <>
          <p>La gestion des utilisateurs est accessible via le menu utilisateur → <strong>⚙️ Administration → onglet 👥 Utilisateurs</strong>.</p>
          <p>La page d'administration est organisée en onglets : <strong>⚙️ Général</strong>, <strong>📚 Référentiels</strong>, <strong>👥 Utilisateurs</strong> et <strong>🛠️ Opérations</strong>.</p>

          <h4>Créer un utilisateur</h4>
          <ol>
            <li>Cliquez sur <strong>+ Nouvel utilisateur</strong>.</li>
            <li>Renseignez le prénom, le nom, l'email et le mot de passe provisoire.</li>
            <li>Renseignez optionnellement le téléphone (format <code>0612345678</code> ou <code>+33612345678</code>), le N° de licence fédérale (format <code>A-14-1234567</code>) et le club d'appartenance.</li>
            <li>Attribuez un ou plusieurs rôles :
              <ul>
                <li><strong>🏊 Plongeur</strong> — peut consulter le calendrier et ses créneaux.</li>
                <li><strong>🤿 Directeur de plongée</strong> — peut créer des créneaux et gérer les plongeurs.</li>
                <li><strong>🔑 Administrateur</strong> — accès complet, gestion des utilisateurs et de la configuration.</li>
              </ul>
            </li>
            <li>Cliquez sur <strong>Créer</strong>.</li>
          </ol>

          <h4>Modifier un utilisateur</h4>
          <ol>
            <li>Dans le tableau des utilisateurs, cliquez sur <strong>✏️ Modifier</strong> sur la ligne concernée.</li>
            <li>Modifiez l'email, le nom, le téléphone (optionnel), le N° de licence (optionnel) ou le club d'appartenance (optionnel).</li>
            <li>Cliquez sur <strong>Enregistrer</strong>.</li>
          </ol>

          <h4>Changer le rôle d'un utilisateur</h4>
          <ol>
            <li>Dans la colonne <strong>Rôles</strong>, cochez ou décochez les rôles souhaités directement dans le tableau.</li>
            <li>Le changement est immédiatement pris en compte.</li>
          </ol>

          <h4>Supprimer un utilisateur</h4>
          <ol>
            <li>Cliquez sur <strong>🗑</strong> à droite de la ligne de l'utilisateur.</li>
            <li>La suppression est immédiate et irréversible.</li>
          </ol>
          <div className="help-warning">⚠️ La suppression d'un utilisateur ne supprime pas ses inscriptions sur les créneaux existants.</div>

          <h4>Exporter la liste des utilisateurs (CSV)</h4>
          <p>Cliquez sur <strong>📥 Exporter CSV</strong> pour télécharger un fichier CSV contenant tous les comptes, triés par club.
            Le fichier est encodé en UTF-8 avec le séparateur point-virgule (compatible Excel).</p>
          <p>Colonnes exportées : <code>club</code>, <code>nom</code>, <code>prénom</code>, <code>email</code>, <code>téléphone</code>, <code>licence</code>.</p>

          <h4>Importer des utilisateurs depuis un CSV</h4>
          <ol>
            <li>Préparez un fichier CSV UTF-8 avec la ligne d'en-tête : <code>club;nom;prenom;email;telephone;licence</code>.</li>
            <li>Cliquez sur <strong>📤 Importer CSV</strong> pour afficher le panneau d'import.</li>
            <li>Sélectionnez le fichier CSV.</li>
            <li>Saisissez un mot de passe provisoire qui sera assigné à tous les comptes importés (minimum 6 caractères).</li>
            <li>Cliquez sur <strong>Importer</strong>.</li>
          </ol>
          <p>Les utilisateurs dont l'adresse e-mail est déjà présente dans la base sont automatiquement ignorés (aucune modification du compte existant). Le résultat affiche le nombre de comptes importés, ignorés et les éventuelles erreurs de format.</p>
          <div className="help-tip">💡 Le fichier CSV exporté peut servir de modèle pour préparer un import.</div>
        </>
      ),
    },

    // ── STATISTIQUES (ADMIN) ─────────────────────────────────────────────────
    {
      id: 'statistiques',
      icon: '📊',
      title: 'Consulter les statistiques',
      roles: ['ADMIN'],
      content: (
        <>
          <p>Le tableau de bord des statistiques est accessible via le menu utilisateur → <strong>📊 Statistiques</strong> (réservé aux administrateurs).</p>

          <h4>Filtres de période</h4>
          <ul>
            <li><strong>Année</strong> — sélectionnez une année ou « Toutes » pour afficher toutes les données.</li>
            <li><strong>Mois</strong> — disponible uniquement si une année est sélectionnée. Permet de restreindre l'affichage à un mois précis.</li>
          </ul>

          <h4>Visualisations disponibles</h4>
          <ul>
            <li><strong>Cartes de totaux</strong> — nombre de créneaux, plongées inscrites et ratio moyen plongeurs / créneau sur la période.</li>
            <li><strong>Histogramme</strong> — évolution mensuelle (si une année est choisie) ou annuelle, avec les barres Plongées et Créneaux.</li>
            <li><strong>Camemberts par club</strong> — répartition des plongées et des créneaux entre les clubs, avec les pourcentages affichés dans la légende.</li>
            <li><strong>Camemberts par type</strong> — répartition par type de créneau (Exploration, Formation, Apnée…), avec les pourcentages.</li>
            <li><strong>Tableaux de détail</strong> — chiffres bruts par club et par type pour une lecture précise.</li>
          </ul>

          <div className="help-tip">💡 Survolez une tranche d'un camembert pour afficher le détail (nom, valeur et pourcentage exact) dans une infobulle.</div>
        </>
      ),
    },

    // ── CONFIGURATION (ADMIN) ────────────────────────────────────────────────
    {
      id: 'configuration',
      icon: '⚙️',
      title: 'Configuration de l\'application',
      roles: ['ADMIN'],
      content: (
        <>
          <p>La configuration est accessible via le menu utilisateur → <strong>⚙️ Administration</strong>.</p>

          <h4>Configuration du lac</h4>
          <ul>
            <li><strong>Nom du site</strong> — affiché dans la barre de navigation et les exports.</li>
            <li><strong>Capacité max de plongeurs</strong> — valeur par défaut à la création d'un créneau.</li>
            <li><strong>Durée maximale d'un créneau</strong> — modifiable via le formulaire dédié (1–24h). Doit être supérieure ou égale à la durée minimale.</li>
            <li><strong>Durée par défaut d'un créneau</strong> — pré-sélectionnée à la création d'un créneau (1–12h).</li>
            <li><strong>Durée min d'un créneau</strong> — fixée dans la configuration applicative (non modifiable via l'interface).</li>
            <li><strong>Résolution de la grille horaire</strong> — en minutes (15, 30 ou 60).</li>
          </ul>

          <h4>Listes configurables</h4>
          <ul>
            <li><strong>Types de plongée</strong> — ex : Exploration, Formation, Apnée. Chaque entrée peut être personnalisée avec une couleur.</li>
            <li><strong>Clubs</strong> — liste des clubs participants. Utilisée à deux endroits : associée à chaque <strong>créneau</strong> (club organisateur) et proposée aux plongeurs lors de leur <strong>inscription libre</strong> pour indiquer leur club d'appartenance.</li>
            <li><strong>Niveaux de plongeurs</strong> — liste maître de tous les niveaux de certification, utilisée par le DP pour l'ajout manuel d'un plongeur sur un créneau.</li>
          </ul>

          <h4>Niveaux d'inscription libre</h4>
          <p>Dans l'onglet <strong>📚 Référentiels</strong>, la section <strong>🎓 Niveaux d'inscription libre</strong> permet de configurer trois listes de niveaux indépendantes :</p>
          <ul>
            <li><strong>Niveaux plongeurs</strong> — niveaux proposés aux plongeurs lors de l'inscription libre (par défaut : N1 à MF2).</li>
            <li><strong>Niveaux directeur de plongée</strong> — niveaux proposés aux DP lors de leur auto-inscription (par défaut : N5, E3, E4, MF1, MF2).</li>
            <li><strong>Niveaux en préparation</strong> — liste optionnelle proposée dans le formulaire d'inscription libre (par défaut : Aucun, N1 à PV2).</li>
          </ul>
          <p>Chaque liste est modifiable : un niveau par ligne. Les modifications sont prises en compte immédiatement.</p>

          <h4>Accès &amp; inscriptions</h4>
          <ul>
            <li>
              <strong>Accès public au calendrier</strong> — si désactivé, les visiteurs non connectés voient une page de connexion au lieu du calendrier.
            </li>
            <li>
              <strong>Inscription libre</strong> — si désactivé, seul un administrateur peut créer des comptes. Le bouton "S'inscrire" est masqué.
            </li>
          </ul>

          <h4>Notifications par e-mail (admin)</h4>
          <p>
            La section <strong>🔔 Notifications par e-mail</strong> de l'administration permet d'activer ou de
            désactiver chaque type de notification pour l'ensemble des utilisateurs.
            Lorsqu'une notification est désactivée globalement, son contenu complet (destinataire, sujet, corps) est
            tracé dans les <strong>logs du serveur</strong> (niveau INFO) pour faciliter le débogage.
          </p>
          <ul>
            <li><strong>📩 Confirmation inscription en liste d'attente</strong> — envoyée au plongeur quand il s'inscrit en liste d'attente.</li>
            <li><strong>✅ Inscription validée</strong> — envoyée au plongeur après validation par le DP (délai de 15 min).</li>
            <li><strong>❌ Inscription annulée/supprimée</strong> — envoyée au plongeur si son inscription est annulée par un DP ou un admin.</li>
            <li><strong>⏳ Remis en liste d'attente</strong> — envoyée au plongeur si un DP le remet en liste d'attente depuis la vue palanquées (délai de 15 min).</li>
            <li><strong>📋 Nouvelles inscriptions sur un créneau (→ DP / créateur)</strong> — envoyée au directeur de plongée assigné et au créateur du créneau quand un plongeur s'inscrit librement en liste d'attente, ou est ajouté directement.</li>
            <li><strong>📋 Rappel fiche de sécurité</strong> — envoyé au directeur de plongée assigné X jours après la sortie pour lui rappeler de transmettre la fiche de sécurité remplie. Désactivée par défaut. Le délai (en jours) et le contenu du mail sont configurables. Ce rappel n'est envoyé qu'une seule fois par créneau.</li>
          </ul>
        </>
      ),
    },
    {
      id: 'notifications-profil',
      icon: '🔔',
      title: '🔔 Mes préférences de notification',
      roles: ['DIVER', 'DIVE_DIRECTOR', 'ADMIN'],
      content: (
        <>
          <p>
            Dans votre <strong>profil</strong> (menu utilisateur → Profil), la section
            <strong> 🔔 Notifications par e-mail</strong> vous permet de désactiver les types de
            notifications que vous ne souhaitez pas recevoir, indépendamment des paramètres globaux.
          </p>
          <ul>
            <li><strong>📩 Confirmation d'inscription</strong> — reçue quand vous vous inscrivez en liste d'attente.</li>
            <li><strong>✅ Inscription validée</strong> — reçue quand le DP valide votre inscription.</li>
            <li><strong>❌ Inscription annulée</strong> — reçue si votre inscription est annulée par un DP ou admin.</li>
            <li><strong>⏳ Remis en liste d'attente</strong> — reçue si vous êtes remis en liste d'attente.</li>
            <li><strong>📋 Nouvelles inscriptions (en tant que DP assigné)</strong> — reçue quand un plongeur s'inscrit sur votre créneau et que vous êtes le directeur de plongée désigné. <em>Activée par défaut.</em></li>
            <li><strong>📋 Nouvelles inscriptions (en tant que créateur)</strong> — reçue quand un plongeur s'inscrit sur un créneau que vous avez créé. <em>Désactivée par défaut.</em></li>
            <li><strong>📋 Rappel fiche de sécurité (en tant que DP assigné)</strong> — reçu quelques jours après la sortie pour vous rappeler de transmettre la fiche de sécurité remplie. <em>Activée par défaut (si la fonctionnalité est activée globalement par l'administrateur).</em></li>
          </ul>
          <p>
            <strong>Note :</strong> si la notification est désactivée globalement par l'administrateur,
            elle ne sera pas envoyée même si vous l'avez activée dans votre profil.
          </p>
        </>
      ),
    },
  ];

  // Filtrer les sections selon le rôle
  const visibleSections = sections.filter(s =>
    !s.roles || s.roles.includes(role)
  );

  // ── Données structurées pour l'export PDF ──────────────────────────────────
  const pdfSections: PdfSection[] = [
    {
      icon: '📅', title: 'Consulter le calendrier',
      items: [
        { type: 'paragraph', text: 'Le calendrier est la page principale de l\'application. Il affiche tous les créneaux de plongée disponibles.' },
        { type: 'h4', text: 'Changer de vue' },
        { type: 'ul', items: ['Jour — affiche tous les créneaux d\'une journée sur une grille horaire.', 'Semaine — affiche 7 colonnes côte à côte, une par jour.', 'Mois — vue mensuelle avec un résumé des créneaux par jour.'] },
        { type: 'paragraph', text: 'Utilisez les boutons ◀ ▶ pour naviguer d\'une période à l\'autre, ou cliquez sur un jour dans le mini-calendrier (barre latérale à gauche) pour aller directement à cette date.' },
        { type: 'h4', text: 'Ouvrir les détails d\'un créneau' },
        { type: 'paragraph', text: 'Cliquez sur un bloc de créneau pour afficher le panneau de détails : horaires, nombre de plongeurs inscrits, directeur de plongée, etc.' },
      ],
    },
    ...(role === 'ADMIN' || role === 'DIVE_DIRECTOR' ? [
      {
        icon: '➕', title: 'Créer un créneau',
        items: [
          { type: 'paragraph' as const, text: 'La création de créneaux est réservée aux administrateurs et aux directeurs de plongée.' },
          { type: 'h4' as const, text: 'Bouton "+ Nouveau créneau" (toutes les vues)' },
          { type: 'ol' as const, items: ['Cliquez sur le bouton "+ Nouveau créneau" en haut à droite du calendrier.', 'Remplissez le formulaire : date, heure de début, heure de fin, titre (optionnel), type de plongée, club, nombre de plongeurs, notes.', 'La date est préremplie avec la date sélectionnée dans le calendrier, mais vous pouvez la modifier directement dans le formulaire.', 'Cliquez sur Créer.'] },
          { type: 'h4' as const, text: 'Depuis la vue Jour' },
          { type: 'ol' as const, items: ['Si la journée est vide, un bouton "+ Créer le premier créneau" apparaît au centre de la vue.', 'Cliquez dessus pour ouvrir le formulaire de création.'] },
          { type: 'h4' as const, text: 'Depuis la vue Semaine' },
          { type: 'ol' as const, items: ['Survolez (sur ordinateur uniquement) ou appuyez sur l\'en-tête d\'une colonne de jour dans la grille.', 'Cliquez sur le bouton + qui apparaît dans l\'en-tête.'] },
          { type: 'h4' as const, text: 'Depuis la vue Mois' },
          { type: 'ol' as const, items: ['Survolez (sur ordinateur uniquement) ou appuyez sur une cellule de jour.', 'Cliquez sur le bouton + qui apparaît en bas à droite de la cellule.'] },
          ...(role === 'ADMIN' ? [
            { type: 'h4' as const, text: '👤 Créer pour le compte d\'un autre DP (administrateurs)' },
            { type: 'paragraph' as const, text: 'En bas du formulaire de création, le champ « Créer pour le compte de… » liste les directeurs de plongée actifs. Sélectionnez un DP pour lui attribuer le créneau. Utile pour migrer des créneaux depuis une autre application ou pré-créer les créneaux de DP externes.' },
          ] : []),
          { type: 'tip' as const, text: 'La date proposée dans le formulaire correspond à votre sélection dans le calendrier. Vous pouvez la modifier avant de valider pour créer un créneau à une autre date.' },
        ],
      },
      {
        icon: '✏️', title: 'Modifier un créneau',
        items: [
          { type: 'paragraph' as const, text: 'Après création, il est possible de modifier toutes les informations d\'un créneau, y compris sa date, ses horaires (déplacement) et sa capacité.' },
          { type: 'ol' as const, items: ['Cliquez sur le créneau pour ouvrir le panneau de détails.', 'Cliquez sur l\'icône ✏️ à côté du titre du créneau.', 'Modifiez la date, l\'heure de début, l\'heure de fin, le nombre de places, le titre, le type, le club ou les notes.', 'Cliquez sur Enregistrer.'] },
          { type: 'tip' as const, text: 'Le déplacement d\'un créneau respecte les mêmes règles que la création : date passée impossible, capacité de la plage horaire cible vérifiée automatiquement.' },
        ],
      },
      {
        icon: '🗑️', title: 'Supprimer un créneau',
        items: [
          { type: 'warning' as const, text: 'La suppression d\'un créneau est définitive et supprime également tous les plongeurs inscrits.' },
          { type: 'ol' as const, items: ['Cliquez sur le créneau pour ouvrir le panneau de détails.', 'Faites défiler vers le bas et cliquez sur Supprimer le créneau.', 'La suppression est immédiate.'] },
        ],
      },
      {
        icon: '🤿', title: 'Ajouter un plongeur à un créneau',
        items: [
          { type: 'paragraph' as const, text: 'Seuls les administrateurs et les directeurs de plongée peuvent inscrire des plongeurs.' },
          { type: 'ol' as const, items: ['Cliquez sur le créneau pour ouvrir le panneau de détails.', 'Cliquez sur + Ajouter un plongeur.', 'Option 1 — Utilisateur existant : tapez son nom dans le champ de recherche et sélectionnez-le.', 'Option 2 — Saisie manuelle : renseignez directement le prénom, le nom et le niveau de certification.', 'Cliquez sur Ajouter.'] },
          { type: 'tip' as const, text: 'Le nombre de plongeurs inscrits est limité par la capacité du créneau. Quand il est atteint, le bouton d\'ajout disparaît.' },
        ],
      },
      {
        icon: '🎖️', title: 'Désigner un directeur de plongée',
        items: [
          { type: 'paragraph' as const, text: 'Chaque créneau peut avoir un seul directeur de plongée. Il est identifié par le badge 🎖️ dans la liste des plongeurs.' },
          { type: 'h4' as const, text: 'Lors de l\'ajout d\'un plongeur' },
          { type: 'ol' as const, items: ['Dans le formulaire d\'ajout, cochez 🎖 Directeur de plongée sur ce créneau.', 'Renseignez son email et son téléphone (obligatoires).', 'Renseignez optionnellement son N° de licence (prérempli automatiquement si le DP est sélectionné depuis la recherche d\'utilisateurs).', 'Cliquez sur Ajouter.'] },
          { type: 'h4' as const, text: 'Sur un plongeur déjà inscrit' },
          { type: 'ol' as const, items: ['Dans la liste des plongeurs, cliquez sur l\'icône ✏️ à côté du nom.', 'Cochez Directeur de plongée et renseignez les coordonnées.', 'Cliquez sur Enregistrer.'] },
          { type: 'h4' as const, text: 'S\'auto-assigner comme directeur (depuis le calendrier)' },
          { type: 'paragraph' as const, text: 'Si un créneau n\'a pas encore de directeur, tout directeur de plongée peut s\'y désigner directement depuis le panneau de détails.' },
          { type: 'ol' as const, items: [
            'Ouvrez le créneau (il doit être sans directeur actuellement).',
            'Cliquez sur le bouton 🤿 M\'assigner comme DP sur ce créneau.',
            'Vos informations de profil (prénom, nom, email, téléphone, N° de licence) sont utilisées automatiquement.',
            'Une fois assigné, vous pouvez configurer les inscriptions libres et gérer les plongeurs.',
          ] },
          { type: 'tip' as const, text: 'Votre numéro de téléphone est repris directement depuis Mon profil. Assurez-vous qu\'il est renseigné avant de vous assigner comme DP.' },
          { type: 'tip' as const, text: 'Les coordonnées du directeur (email, téléphone, N° licence) sont exportées dans la fiche de sécurité. La licence apparaît sur la ligne DP : NOM Prénom - Niveau - 12345678A.' },
        ],
      },
      {
        icon: '❌', title: 'Retirer un plongeur d\'un créneau',
        items: [
          { type: 'ol' as const, items: ['Cliquez sur le créneau pour ouvrir le panneau de détails.', 'Dans la liste des plongeurs, survolez (sur ordinateur uniquement) ou appuyez (mobile) sur le nom du plongeur.', 'Cliquez sur la croix ✕ à droite de son nom.'] },
          { type: 'paragraph' as const, text: 'La place est immédiatement libérée.' },
        ],
      },
      {
        icon: '📊', title: 'Exporter la fiche de sécurité',
        items: [
          { type: 'paragraph' as const, text: 'Une fiche de sécurité Excel peut être générée pour chaque créneau. Elle récapitule les plongeurs inscrits et les informations du directeur de plongée.' },
          { type: 'ol' as const, items: ['Cliquez sur le créneau pour ouvrir le panneau de détails.', 'Cliquez sur Exporter fiche de sécurité (Excel).', 'Le fichier .xlsx est téléchargé automatiquement.'] },
          { type: 'h4' as const, text: 'Contenu de la fiche' },
          { type: 'ul' as const, items: ['Cellule B4 — Date, club et infos DP : NOM Prénom - Niveau - N°Licence.', 'Colonnes A–C — Nom, prénom et niveau de chaque plongeur.', 'Colonne D — Aptitudes du plongeur (PE20, PA40, GP…) si renseignées dans l\'organisation des palanquées.', 'Si des palanquées sont organisées, chaque palanquée est dans un tableau séparé.'] },
          { type: 'h4' as const, text: 'Export CSV' },
          { type: 'paragraph' as const, text: 'Un export CSV est également disponible depuis le panneau de détails (bouton Télécharger liste CSV). Il contient : Nom, Prénom, Niveau, Email, Directeur de plongée, Club, Date certificat médical, Commentaire.' },
          { type: 'tip' as const, text: 'Pour que la licence du DP apparaisse, elle doit être saisie dans son profil ou dans le formulaire DP sur le créneau.' },
        ],
      },
      {
        icon: '🫧', title: 'Organiser les palanquées',
        items: [
          { type: 'paragraph' as const, text: 'La page d\'organisation des palanquées permet de répartir les plongeurs en groupes de plongée par glisser-déposer.' },
          { type: 'h4' as const, text: 'Accéder à la page' },
          { type: 'ol' as const, items: ['Cliquez sur un créneau pour ouvrir le panneau de détails.', 'Cliquez sur Organiser les palanquées.'] },
          { type: 'h4' as const, text: 'Répartir les plongeurs' },
          { type: 'ul' as const, items: ['Glissez-déposez une fiche vers une palanquée ou vers "+ Nouvelle palanquée".', 'Pour retirer un plongeur d\'une palanquée, glissez-le vers la zone Non assignés.'] },
          { type: 'h4' as const, text: 'Modifier le niveau ou les aptitudes' },
          { type: 'ul' as const, items: ['Double-cliquez sur le niveau affiché pour le modifier.', 'Double-cliquez sur la zone aptitudes pour sélectionner : PE12–PE60, PA12–PA60, E1–E4, GP.', 'Les aptitudes apparaissent en colonne D de l\'export Excel.'] },
          { type: 'tip' as const, text: 'Les modifications de niveau et d\'aptitudes sont enregistrées immédiatement sur le serveur.' },
        ],
      },
    ] : []),
    {
      icon: '🔐', title: 'Se connecter / S\'inscrire',
      items: [
        { type: 'h4', text: 'Se connecter' },
        { type: 'ol', items: ['Cliquez sur Connexion dans la barre de navigation.', 'Saisissez votre email et votre mot de passe.', 'Cliquez sur Se connecter.'] },
        { type: 'h4', text: 'S\'inscrire (si activé)' },
        { type: 'ol', items: ['Cliquez sur Connexion, puis sur Pas encore de compte ? S\'inscrire.', 'Remplissez votre prénom/nom, email, téléphone et mot de passe.', 'Votre compte est créé avec le rôle Plongeur. Un administrateur peut ensuite modifier votre rôle.'] },
        { type: 'h4', text: 'Mot de passe oublié' },
        { type: 'ol', items: ['Dans la fenêtre de connexion, cliquez sur Mot de passe oublié ?.', 'Saisissez votre email. Un lien de réinitialisation vous sera envoyé.'] },
      ],
    },
    {
      icon: '👤', title: 'Mon profil',
      items: [
        { type: 'paragraph', text: 'La page Mon profil vous permet de consulter et modifier vos informations personnelles.' },
        { type: 'ul', items: ['Nom et email — affichés dans le menu et les listes de plongeurs.', 'Téléphone — utilisé pour contacter le directeur de plongée.', 'N° de licence fédérale (optionnel) — repris automatiquement si vous êtes désigné DP sur un créneau, et exporté dans la fiche de sécurité.', 'Mot de passe — modifiable depuis cette page.'] },
        { type: 'paragraph', text: 'Accès : cliquez sur votre nom dans la barre de navigation → Mon profil.' },
      ],
    },
    ...(role === 'ADMIN' ? [
      {
        icon: '👥', title: 'Gérer les utilisateurs',
        items: [
          { type: 'paragraph' as const, text: 'La gestion des utilisateurs est accessible dans Administration → onglet 👥 Utilisateurs.' },
          { type: 'paragraph' as const, text: 'La page Administration est organisée en onglets : ⚙️ Général, 📚 Référentiels, 👥 Utilisateurs et 🛠️ Opérations.' },
          { type: 'h4' as const, text: 'Créer un utilisateur' },
          { type: 'ol' as const, items: ['Cliquez sur + Nouvel utilisateur.', 'Renseignez l\'email, le nom, le téléphone et le mot de passe provisoire.', 'Attribuez un ou plusieurs rôles : Plongeur, Directeur de plongée, ou Administrateur.', 'Cliquez sur Créer.'] },
          { type: 'h4' as const, text: 'Modifier un utilisateur' },
          { type: 'ol' as const, items: ['Dans le tableau des utilisateurs, cliquez sur Modifier sur la ligne concernée.', 'Modifiez l\'email, le nom ou le téléphone.', 'Cliquez sur Enregistrer.'] },
          { type: 'h4' as const, text: 'Changer le rôle d\'un utilisateur' },
          { type: 'ol' as const, items: ['Dans la colonne Rôles, cochez ou décochez les rôles souhaités directement dans le tableau.', 'Le changement est immédiatement pris en compte.'] },
          { type: 'warning' as const, text: 'La suppression d\'un utilisateur ne supprime pas ses inscriptions sur les créneaux existants.' },
        ],
      },
      {
        icon: '📊', title: 'Consulter les statistiques',
        items: [
          { type: 'paragraph' as const, text: 'Le tableau de bord des statistiques est accessible via Statistiques dans la barre de navigation (réservé aux administrateurs).' },
          { type: 'h4' as const, text: 'Filtres de période' },
          { type: 'ul' as const, items: ['Année — sélectionnez une année ou « Toutes » pour afficher toutes les données.', 'Mois — disponible uniquement si une année est sélectionnée.'] },
          { type: 'h4' as const, text: 'Visualisations disponibles' },
          { type: 'ul' as const, items: ['Cartes de totaux — nombre de créneaux, plongées inscrites et ratio moyen.', 'Histogramme — évolution mensuelle ou annuelle.', 'Camemberts par club — répartition des plongées et des créneaux entre les clubs.', 'Camemberts par type — répartition par type de créneau.', 'Tableaux de détail — chiffres bruts par club et par type.'] },
          { type: 'tip' as const, text: 'Survolez une tranche d\'un camembert pour afficher le détail dans une infobulle.' },
        ],
      },
      {
        icon: '⚙️', title: 'Configuration de l\'application',
        items: [
          { type: 'paragraph' as const, text: 'La configuration est accessible dans Administration.' },
          { type: 'h4' as const, text: 'Configuration du lac' },
          { type: 'ul' as const, items: ['Nom du site — affiché dans la barre de navigation et les exports.', 'Capacité max de plongeurs — valeur par défaut à la création d\'un créneau.', 'Durée min / max d\'un créneau — en heures.', 'Résolution de la grille horaire — en minutes (15, 30 ou 60).'] },
          { type: 'h4' as const, text: 'Listes configurables' },
          { type: 'ul' as const, items: ['Types de plongée — ex : Exploration, Formation, Apnée.', 'Clubs — liste des clubs participants.', 'Niveaux de plongeurs — liste maître pour l\'ajout manuel d\'un plongeur.'] },
          { type: 'h4' as const, text: 'Niveaux d\'inscription libre' },
          { type: 'ul' as const, items: ['Niveaux plongeurs — proposés aux plongeurs lors de l\'inscription libre.', 'Niveaux directeur de plongée — proposés aux DP lors de leur auto-inscription.', 'Niveaux en préparation — liste optionnelle dans le formulaire d\'inscription.'] },
          { type: 'h4' as const, text: 'Accès & inscriptions' },
          { type: 'ul' as const, items: ['Accès public au calendrier — si désactivé, les visiteurs non connectés voient une page de connexion.', 'Inscription libre — si désactivé, seul un administrateur peut créer des comptes.'] },
        ],
      },
      {
        icon: '💾', title: 'Sauvegarde et restauration',
        items: [
          { type: 'paragraph' as const, text: 'Section accessible dans Administration → onglet 🛠️ Opérations.' },
          { type: 'h4' as const, text: 'Exporter une sauvegarde complète' },
          { type: 'paragraph' as const, text: 'Exporte tout : configuration, utilisateurs, créneaux, plongeurs, palanquées et listes d\'attente.' },
          { type: 'ol' as const, items: ['Cliquez sur Exporter une sauvegarde complète.', 'Le fichier santalina-full-AAAA-MM-JJ.json est téléchargé.'] },
          { type: 'h4' as const, text: 'Exporter configuration + utilisateurs' },
          { type: 'paragraph' as const, text: 'Exporte la configuration et les comptes utilisateurs uniquement (sans créneaux ni listes d\'attente).' },
          { type: 'ol' as const, items: ['Cliquez sur Exporter configuration + utilisateurs.', 'Le fichier santalina-config-users-AAAA-MM-JJ.json est téléchargé.'] },
          { type: 'h4' as const, text: 'Importer une sauvegarde' },
          { type: 'ol' as const, items: ['Cliquez sur Choisir un fichier et sélectionnez le fichier .json.', 'Cliquez sur Importer.', 'Un résumé indique le nombre d\'éléments restaurés.'] },
          { type: 'warning' as const, text: 'L\'import est irréversible : toutes les données existantes sont supprimées avant la restauration.' },
        ],
      },
    ] : []),
  ];

  const handleDownloadPdf = async () => {
    const { exportHelpPdf } = await import('../utils/exportHelpPdf');
    exportHelpPdf(pdfSections, siteName);
  };

  return (
    <div className="page">
      <div className="help-page">
        <div className="help-header">
          <div className="help-header-top">
            <h1>📖 Guide d'utilisation</h1>
            <button className="help-print-btn" onClick={handleDownloadPdf} title="Télécharger le guide en PDF">📥 Télécharger PDF</button>
          </div>
          <p>Retrouvez ici toutes les informations pour utiliser l'application de réservation de créneaux de plongée.</p>
          {role === 'GUEST' && (
            <div className="help-tip">💡 Connectez-vous pour accéder aux sections réservées aux plongeurs et aux directeurs.</div>
          )}
        </div>

        <div className="help-sections">
          {visibleSections.map(section => (
            <div key={section.id} className={`help-section ${openSection === section.id ? 'open' : ''}`}>
              <button
                className="help-section-header"
                onClick={() => toggle(section.id)}
                aria-expanded={openSection === section.id}
              >
                <span className="help-section-icon">{section.icon}</span>
                <span className="help-section-title">{section.title}</span>
                <span className="help-section-chevron">{openSection === section.id ? '▲' : '▼'}</span>
              </button>
              <div className={`help-section-body${openSection === section.id ? ' help-section-body--open' : ''}`}>
                {section.content}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
