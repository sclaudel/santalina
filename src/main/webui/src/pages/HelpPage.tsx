import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

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
          <h4>Depuis la vue Jour ou Semaine</h4>
          <ol>
            <li>Survolez (desktop) ou appuyez sur l'en-tête d'un jour dans la grille.</li>
            <li>Cliquez sur le bouton <strong>+</strong> qui apparaît.</li>
            <li>Remplissez le formulaire : heure de début, heure de fin, titre (optionnel), type de plongée, club, nombre de plongeurs, notes.</li>
            <li>Cliquez sur <strong>Créer</strong>.</li>
          </ol>
          <h4>Depuis la vue Mois</h4>
          <ol>
            <li>Survolez une cellule de jour.</li>
            <li>Cliquez sur le bouton <strong>+</strong> qui apparaît en bas à droite de la cellule.</li>
          </ol>
          <div className="help-tip">💡 Le créneau créé apparaît immédiatement dans le calendrier. Il est possible de modifier son titre, ses notes, son type et son club en cliquant dessus.</div>
        </>
      ),
    },

    // ── MODIFIER UN CRÉNEAU ──────────────────────────────────────────────────
    {
      id: 'modifier-creneau',
      icon: '✏️',
      title: 'Modifier les infos d\'un créneau',
      roles: ['ADMIN', 'DIVE_DIRECTOR'],
      content: (
        <>
          <p>Après création, il est possible de modifier les informations d'un créneau.</p>
          <ol>
            <li>Cliquez sur le créneau pour ouvrir le panneau de détails.</li>
            <li>Cliquez sur le bouton <strong>✏️ Modifier les infos</strong>.</li>
            <li>Modifiez le titre, le type, le club, le nombre de plongeurs ou les notes.</li>
            <li>Cliquez sur <strong>Enregistrer</strong>.</li>
          </ol>
          <h4>Modifier le nombre de places</h4>
          <p>Dans le panneau de détails, cliquez sur l'icône ✏️ à côté du compte « X / Y plongeurs » pour ajuster la capacité du créneau.</p>
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
            <li>Cliquez sur <strong>Ajouter</strong>.</li>
          </ol>
          <h4>Sur un plongeur déjà inscrit</h4>
          <ol>
            <li>Dans la liste des plongeurs, cliquez sur l'icône ✏️ à côté du nom.</li>
            <li>Cochez <strong>Directeur de plongée</strong> et renseignez les coordonnées.</li>
            <li>Cliquez sur <strong>Enregistrer</strong>.</li>
          </ol>
          <div className="help-tip">💡 Les coordonnées du directeur (email, téléphone) sont exportées dans la fiche de sécurité Excel.</div>
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
            <li>Dans la liste des plongeurs, survolez (desktop) ou appuyez (mobile) sur le nom du plongeur.</li>
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
            <li>Remplissez votre prénom/nom, email, téléphone et mot de passe.</li>
            <li>Votre compte est créé avec le rôle <strong>Plongeur</strong>. Un administrateur peut ensuite modifier votre rôle.</li>
          </ol>
          <h4>Mot de passe oublié</h4>
          <ol>
            <li>Dans la fenêtre de connexion, cliquez sur <strong>Mot de passe oublié ?</strong>.</li>
            <li>Saisissez votre email. Un lien de réinitialisation vous sera envoyé.</li>
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
            <li><strong>Nom</strong> et <strong>email</strong> — affichés dans le menu et les listes de plongeurs.</li>
            <li><strong>Téléphone</strong> — utilisé pour contacter le directeur de plongée.</li>
            <li><strong>Mot de passe</strong> — modifiable depuis cette page.</li>
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
          <p>La gestion des utilisateurs est accessible dans <strong>⚙️ Administration → section Utilisateurs</strong>.</p>

          <h4>Créer un utilisateur</h4>
          <ol>
            <li>Cliquez sur <strong>+ Nouvel utilisateur</strong>.</li>
            <li>Renseignez l'email, le nom, le téléphone et le mot de passe provisoire.</li>
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
          <p>Le tableau de bord des statistiques est accessible via <strong>📊 Statistiques</strong> dans la barre de navigation (réservé aux administrateurs).</p>

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
          <p>La configuration est accessible dans <strong>⚙️ Administration</strong>.</p>

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

  return (
    <div className="page">
      <div className="help-page">
        <div className="help-header">
          <div className="help-header-top">
            <h1>📖 Guide d'utilisation</h1>
            <button className="help-print-btn" onClick={() => window.print()} title="Imprimer le guide">🖨️ Imprimer</button>
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
