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
          <div className="help-tip">💡 Les modifications de niveau, aptitudes, profondeur et temps sont enregistrées immédiatement sur le serveur.</div>
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
            <li>Remplissez votre <strong>prénom</strong>, <strong>nom</strong>, <strong>email</strong> et <strong>téléphone</strong>, puis acceptez la politique de confidentialité.</li>
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
            <li><strong>Téléphone</strong> — utilisé pour contacter le directeur de plongée.</li>
            <li><strong>N° de licence fédérale</strong> — champ <em>optionnel</em>. S'il est renseigné, il sera automatiquement repris lorsque vous êtes sélectionné comme directeur de plongée sur un créneau, et apparaîtra dans la fiche de sécurité Excel.</li>
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
          <p>La gestion des utilisateurs est accessible via le menu utilisateur → <strong>⚙️ Administration → section Utilisateurs</strong>.</p>

          <h4>Créer un utilisateur</h4>
          <ol>
            <li>Cliquez sur <strong>+ Nouvel utilisateur</strong>.</li>
            <li>Renseignez le prénom, le nom, l'email, le téléphone et le mot de passe provisoire.</li>
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
            <li>Modifiez l'email, le nom ou le téléphone.</li>
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
            <li><strong>Durée min / max d'un créneau</strong> — en heures.</li>
            <li><strong>Résolution de la grille horaire</strong> — en minutes (15, 30 ou 60).</li>
          </ul>

          <h4>Listes configurables</h4>
          <ul>
            <li><strong>Types de plongée</strong> — ex : Exploration, Formation, Apnée. Chaque entrée peut être personnalisée avec une couleur.</li>
            <li><strong>Clubs</strong> — liste des clubs participants, associée à chaque créneau.</li>
          </ul>

          <h4>Accès &amp; inscriptions</h4>
          <ul>
            <li>
              <strong>Accès public au calendrier</strong> — si désactivé, les visiteurs non connectés voient une page de connexion au lieu du calendrier.
            </li>
            <li>
              <strong>Inscription libre</strong> — si désactivé, seul un administrateur peut créer des comptes. Le bouton "S'inscrire" est masqué.
            </li>
          </ul>
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
          { type: 'paragraph' as const, text: 'La gestion des utilisateurs est accessible dans Administration → section Utilisateurs.' },
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
          { type: 'ul' as const, items: ['Types de plongée — ex : Exploration, Formation, Apnée.', 'Clubs — liste des clubs participants.'] },
          { type: 'h4' as const, text: 'Accès & inscriptions' },
          { type: 'ul' as const, items: ['Accès public au calendrier — si désactivé, les visiteurs non connectés voient une page de connexion.', 'Inscription libre — si désactivé, seul un administrateur peut créer des comptes.'] },
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
