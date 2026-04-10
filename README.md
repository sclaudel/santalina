# 🌊 Santalina — Système de Réservation

[![Dernière version](https://img.shields.io/github/v/release/sclaudel/santalina?label=derni%C3%A8re%20version&logo=github&color=blue&include_prereleases)](https://github.com/sclaudel/santalina/releases/latest)
[![Licence MIT](https://img.shields.io/badge/licence-MIT-green)](./LICENSE)
[![Dependabot](https://img.shields.io/badge/Dependabot-activé-0075ca?logo=dependabot&logoColor=white)](/.github/dependabot.yml)
[![Renovate](https://img.shields.io/badge/Renovate-activé-1a1f6c?logo=renovatebot&logoColor=white)](https://github.com/renovatebot/renovate)

Application de réservation de créneaux de plongée en lac, développée avec **Quarkus 3.32.2** (Java 21) et **React 19** (TypeScript).

## ✨ Fonctionnalités

- 📅 **Calendrier public** : vues Jour, Semaine et Mois **chronologiques** sans authentification
- 🔐 **Authentification JWT** : login, inscription avec **activation par email** (lien 24 h), reset de mot de passe par email
- 🛡️ **RGPD** : consentement explicite collecté à l'inscription
- 👥 **3 rôles** (cumulables) :
  - `ADMIN` 🔑 : configuration, tous les créneaux, gestion des utilisateurs
  - `DIVE_DIRECTOR` 🤿 : création et suppression de ses propres créneaux, gestion des plongeurs sur ses créneaux et ceux où il est assigné comme DP, auto-assignation comme DP sur tout créneau sans directeur
  - `DIVER` 🏊 : consultation + inscription libre sur les créneaux ouverts
- ⏰ **Créneaux** : 1h min, 10h max, résolution 15 min, chevauchements affichés côte à côte
- 🤿 **Plongeurs** : ajout/modification/suppression (nom, prénom capitalisé, niveau) sur chaque créneau
- 🎯 **Capacité configurable** : max 25 plongeurs simultanés (modifiable par l'admin)
- 📋 **Liste d'attente** : inscription libre pour les plongeurs (DIVER/DIVE_DIRECTOR non créateur), validation/refus par le DP responsable, badge de notification
- 🔓 **Inscriptions libres** : le DP assigné (ou le créateur du créneau) peut ouvrir les inscriptions avec une date d'ouverture optionnelle — les plongeurs s'inscrivent eux-mêmes et peuvent annuler leur inscription (avertissement si < 48 h avant la sortie)
- 🤿 **Auto-assignation DP** : un directeur de plongée peut se désigner lui-même comme DP sur n'importe quel créneau sans directeur, depuis le panneau de détails du calendrier, sans passer par le formulaire d'ajout
- 🗂️ **Organisation des palanquées** : drag-and-drop, gestion des aptitudes/profondeurs, export Excel fiche de sécurité, export CSV liste des plongeurs avec emails
- 🔒 **Normalisation des données** : prénoms capitalisés (composés inclus), emails en minuscules — à la saisie et à l'import backup
- 🐳 **Docker-ready** : Dockerfile multi-stage + docker-compose
- 🗄️ **Double base de données** : H2 fichier (dev) / PostgreSQL (prod) — couche d'abstraction Panache
- 📊 **Statistiques** (ADMIN) : tableau de bord avec camemberts, histogrammes et tableaux
- 💾 **Sauvegarde / restauration** : export JSON complet ou config+utilisateurs, import avec normalisation automatique

---

## 🚀 Démarrage rapide (développement)

### Prérequis
- Java 21+
- Node.js 18+
- (Optionnel) Docker & Docker Compose

### 1. Générer les clés RSA JWT
```bash
python generate_keys.py
```

### 2. Build du frontend React
```bash
cd src/main/webui
npm install
npm run build
```

### 3. Lancer le backend Quarkus (mode dev)
```bash
./gradlew quarkusDev
```

L'application est disponible sur **http://localhost:8085**

> En mode dev, Quarkus utilise H2 (fichier `./data/santalina`)

### Compte administrateur par défaut
- **Email** : `admin@santalina.com`
- **Mot de passe** : `Admin1234`

---

## 🐳 Déploiement Docker

```bash
# Build et démarrage complet (app + PostgreSQL + Mailhog)
docker compose up --build

# L'application est sur http://localhost:8085
# Interface mail de test : http://localhost:8025
```

### Variables d'environnement (docker-compose.yml)
| Variable | Description | Défaut |
|----------|-------------|--------|
| `JDBC_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://db:5432/santalina` |
| `DB_USER` | Utilisateur DB | `santalina` |
| `DB_PASSWORD` | Mot de passe DB | `santalina_secret` |
| `ADMIN_EMAIL` | Email admin initial | `admin@santalina.com` |
| `ADMIN_PASSWORD` | Mot de passe admin | `Admin1234` |
| `APP_DIVING_MAX_DIVERS` | Capacité maximale | `25` |
| `MAILER_HOST` | Serveur SMTP | `mailhog` |
| `MAILER_PORT` | Port SMTP | `1025` |

---

## 📡 API REST

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/api/auth/register` | Public | Inscription (crée un compte inactif, envoie l'email d'activation) |
| `POST` | `/api/auth/activate` | Public | Activation du compte + définition du mot de passe |
| `POST` | `/api/auth/login` | Public | Connexion → JWT |
| `POST` | `/api/auth/password-reset/request` | Public | Demande reset MDP |
| `POST` | `/api/auth/password-reset/confirm` | Public | Confirmer reset |
| `POST` | `/api/auth/change-password` | Authentifié | Changer MDP |
| `GET` | `/api/slots?date=YYYY-MM-DD` | Public | Créneaux du jour |
| `GET` | `/api/slots/week?from=YYYY-MM-DD` | Public | Créneaux de la semaine |
| `POST` | `/api/slots` | ADMIN, DIVE_DIRECTOR | Créer un créneau |
| `DELETE` | `/api/slots/{id}` | ADMIN (tout), DIRECTOR (sien) | Supprimer |
| `GET` | `/api/slots/{id}/divers` | Public | Plongeurs d'un créneau |
| `POST` | `/api/slots/{id}/divers` | ADMIN, DIVE_DIRECTOR | Ajouter un plongeur |
| `PUT` | `/api/slots/{id}/divers/{diverId}` | ADMIN, DIVE_DIRECTOR | Modifier un plongeur |
| `DELETE` | `/api/slots/{id}/divers/{diverId}` | ADMIN, DIVE_DIRECTOR | Retirer un plongeur |
| `DELETE` | `/api/slots/{id}/divers/me` | Authentifié | Auto-désinscription du plongeur connecté |
| `GET` | `/api/slots/{id}/waiting-list` | ADMIN, DIVE_DIRECTOR | Liste d'attente du créneau |
| `POST` | `/api/slots/{id}/waiting-list` | Authentifié | S'inscrire en liste d'attente |
| `GET` | `/api/slots/{id}/waiting-list/me` | Authentifié | Mon entrée en liste d'attente |
| `DELETE` | `/api/slots/{id}/waiting-list/{entryId}` | Authentifié | Annuler une inscription en liste d'attente |
| `POST` | `/api/slots/{id}/waiting-list/{entryId}/approve` | ADMIN, DIVE_DIRECTOR | Valider une inscription |
| `POST` | `/api/slots/{id}/waiting-list/{entryId}/reject` | ADMIN, DIVE_DIRECTOR | Refuser une inscription |
| `GET` | `/api/slots/{id}/palanquees` | ADMIN, DIVE_DIRECTOR | Palanquées du créneau |
| `POST` | `/api/slots/{id}/palanquees` | ADMIN, DIVE_DIRECTOR | Créer une palanquée |
| `PUT` | `/api/slots/{id}/palanquees/{pid}` | ADMIN, DIVE_DIRECTOR | Modifier une palanquée |
| `DELETE` | `/api/slots/{id}/palanquees/{pid}` | ADMIN, DIVE_DIRECTOR | Supprimer une palanquée |
| `GET` | `/api/users/me` | Authentifié | Mon profil |
| `PUT` | `/api/users/me` | Authentifié | Modifier profil |
| `GET` | `/api/users` | ADMIN | Liste utilisateurs |
| `PUT` | `/api/users/{id}/roles` | ADMIN | Changer rôles |
| `GET` | `/api/config` | Public | Config du site |
| `PUT` | `/api/config/max-divers` | ADMIN | Max plongeurs |
| `PUT` | `/api/config/site-name` | ADMIN | Nom du site |
| `GET` | `/api/stats?from=YYYY-MM-DD&to=YYYY-MM-DD` | ADMIN | Statistiques agrégées (période optionnelle) |

Documentation Swagger : **http://localhost:8085/q/swagger-ui**

---

## 📊 Statistiques (ADMIN)

Accessible via le **menu utilisateur → 📊 Statistiques** (réservé au rôle `ADMIN`).

### Filtres disponibles
- **Année** : toutes les années ou une année spécifique
- **Mois** : tous les mois ou un mois spécifique (si une année est sélectionnée)

### Visualisations
| Vue | Description |
|-----|-------------|
| Cartes de totaux | Nombre de créneaux, plongées inscrites, ratio moyen plongeurs/créneau |
| Histogramme | Évolution par mois ou par année (barres Plongées + Créneaux) |
| Camemberts par club | Répartition des plongées et des créneaux par club, avec % |
| Camemberts par type | Répartition des plongées et des créneaux par type de créneau, avec % |
| Tableaux de détail | Chiffres bruts par club et par type de créneau |

### Structure de la réponse `/api/stats`
```json
{
  "byMonth":  [{ "label": "2026-03", "slots": 12, "divers": 48 }],
  "byYear":   [{ "label": "2026",    "slots": 45, "divers": 180 }],
  "byClub":   [{ "label": "Club A",  "slots": 20, "divers": 80 }],
  "byType":   [{ "label": "Lac",     "slots": 30, "divers": 120 }],
  "totalSlots":  45,
  "totalDivers": 180
}
```

---

## 🏗️ Architecture

```
src/main/
├── java/org/santalina/diving/
│   ├── config/          # DivingConfig (@ConfigProperty)
│   ├── domain/          # Entités JPA (User, DiveSlot, SlotDiver, WaitingListEntry, Palanquee, AppConfigEntry)
│   ├── dto/             # Records Java (request/response)
│   ├── exception/       # GlobalExceptionMapper
│   ├── mail/            # PasswordResetMailer, ActivationMailer, WaitingListMailer
│   ├── resource/        # JAX-RS endpoints (AuthResource, SlotResource, SlotDiverResource,
│   │                   #   WaitingListResource, PalanqueeResource, UserResource, StatsResource, BackupResource…)
│   ├── security/        # JwtUtil, PasswordUtil (BCrypt), NameUtil (capitalisation)
│   ├── service/         # AuthService, UserService, ConfigService, BackupService
│   └── startup/         # AppStartup (init admin + config)
├── resources/
│   ├── application.properties
│   └── db/migration/    # Flyway V1..V14+
└── webui/               # Frontend React + TypeScript + Vite
    └── src/
        ├── components/  # NavBar, CalendarPicker, DayView, WeekView, MonthView, SlotBlock,
        │               #   SlotForm, LoginModal, SelfRegistrationModal
        ├── context/     # AuthContext (JWT + état multi-rôles)
        ├── pages/       # CalendarPage, ProfilePage, AdminPage, StatsPage, MyStatsPage,
        │               #   HelpPage, ResetPasswordPage, ActivatePage, PalanqueePage
        ├── services/    # api.ts, authService, slotService, adminService, slotDiverService,
        │               #   waitingListService, palanqueeService, statsService
        ├── utils/       # exportFicheSecurite, exportFicheSecuriteAvecPalanquees,
        │               #   exportDiverList (CSV), exportHelpPdf, slotTypeColors
        └── types/       # Types TypeScript partagés
```

---

## 🔧 Technologies

| Couche | Technologie |
|--------|-------------|
| Backend | Quarkus 3.32.2, Java 21, JAX-RS, Panache ORM |
| Sécurité | SmallRye JWT (RS256), BCrypt |
| Email | Quarkus Mailer |
| DB Dev | H2 (fichier) |
| DB Prod | PostgreSQL 16 |
| Migrations | Flyway |
| Frontend | React 19, TypeScript, Vite, Day.js, Axios |
| Container | Docker, Docker Compose |
