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
  - `DIVE_DIRECTOR` 🤿 : création et suppression de ses propres créneaux + gestion des plongeurs
  - `DIVER` 🏊 : lecture seule
- ⏰ **Créneaux** : 1h min, 10h max, résolution 15 min, chevauchements affichés côte à côte
- 🤿 **Plongeurs** : ajout/modification/suppression de plongeurs (nom, prénom, niveau) sur chaque créneau
- 📋 **Auto-inscription plongeurs** : un plongeur authentifié peut s'inscrire sur un créneau ; sa demande passe en **file d'attente (PENDING)** et doit être validée par le Directeur de Plongée affecté. Emails automatiques à chaque étape (inscription, validation, annulation).
- 🔴 **Inscriptions activables par créneau** : le DP affecté active/désactive les inscriptions et peut programmer une **date d'ouverture** ; désactivées par défaut.
- 🎯 **Capacité configurable** : max 25 plongeurs simultanés (modifiable par l'admin)
- 🐳 **Docker-ready** : Dockerfile multi-stage + docker-compose
- 🗄️ **Double base de données** : H2 fichier (dev) / PostgreSQL (prod) — couche d'abstraction Panache
- 📊 **Statistiques** (ADMIN) : tableau de bord avec camemberts, histogrammes et tableaux — par mois, par année, par club, par type de créneau, avec filtres période
- 🍔 **Navigation** : barre réduite à « Calendrier » ; Administration, Statistiques, Aide et Profil regroupés dans le menu utilisateur déroulant

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
| `REGISTRATION_SIMULATION_ENABLED` | Simule les mails d'inscription (redirige tout vers une adresse unique) | `false` |
| `REGISTRATION_SIMULATION_TO` | Adresse cible en mode simulation | *(vide)* |

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
| `POST` | `/api/slots/{id}/divers/register` | DIVER, DIVE_DIRECTOR | S'inscrire sur un créneau (→ PENDING) |
| `GET` | `/api/slots/{id}/divers/waitlist` | DIVE_DIRECTOR affecté | Voir la file d'attente |
| `POST` | `/api/slots/{id}/divers/waitlist/{diverId}/validate` | DIVE_DIRECTOR affecté | Valider une inscription (PENDING → CONFIRMED) |
| `DELETE` | `/api/slots/{id}/divers/registrations/me` | DIVER, DIVE_DIRECTOR | Annuler sa participation |
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
│   ├── domain/          # Entités JPA (User, DiveSlot, SlotDiver, AppConfigEntry)
│   ├── dto/             # Records Java (request/response)
│   ├── exception/       # GlobalExceptionMapper
│   ├── mail/            # PasswordResetMailer, ActivationMailer, BookingNotificationMailer
│   ├── resource/        # JAX-RS endpoints (AuthResource, SlotResource, UserResource, StatsResource…)
│   ├── security/        # JwtUtil, PasswordUtil (BCrypt)
│   ├── service/         # AuthService, SlotService, UserService, ConfigService
│   └── startup/         # AppStartup (init admin + config)
├── resources/
│   ├── application.properties
│   └── db/migration/    # Flyway V1..V23 (V23 : file d'attente inscriptions)
└── webui/               # Frontend React + TypeScript + Vite
    └── src/
        ├── components/  # NavBar, CalendarPicker, DayView, WeekView, MonthView, SlotBlock, SlotForm, LoginModal
        ├── context/     # AuthContext (JWT + état multi-rôles)
        ├── pages/       # CalendarPage, ProfilePage, AdminPage, StatsPage, MyStatsPage,
        │               #   HelpPage, ResetPasswordPage, ActivatePage
        ├── services/    # api.ts, authService, slotService, adminService, slotDiverService, statsService
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

---

## 📋 Auto-inscription plongeurs

### Pour le plongeur

1. Ouvre la vue Jour ou Semaine — un créneaux avec inscriptions ouvertes (🟢) affiche le bouton **✍️ Je m'inscris sur ce créneau**.
2. Remplit le formulaire : *niveau* (obligatoire), *nombre de plongées*, *date de dernière plongée*, *email*, *niveau préparé* (optionnel), *commentaire pour le DP* (optionnel).
3. Reçoit un email de confirmation de réception (statut **🕐 En attente de validation**).
4. Une fois validé par le DP, reçoit un email **Inscription confirmée** et apparaît dans la liste publique du créneau.
5. Peut **annuler** sa participation à tout moment (bouton dans le créneau) ; le DP reçoit alors un email d'annulation.

> L'email saisi dans le formulaire est automatiquement mis à jour dans le profil si différent.

### Pour le Directeur de Plongée (affecté au créneau)

1. Active les inscriptions via la modification du créneau (icône ✏️ dans le bloc créneau) ou à la création.
2. Peut programmer une **date d'ouverture** (🕐 Inscriptions programmées le …).
3. Accède à la **file d'attente** (▼ File d'attente, visible uniquement pour lui) triée par date d'inscription.
4. **Valide** les demandes une par une → le plongeur est confirmé et reçoit un email.

> La file d'attente n'est visible que par le DP affecté au créneau. Les autres plongeurs ne peuvent pas y accéder.

### Mode simulation des emails (développement)

Pour éviter l'envoi de mails réels en dev/staging, activez le mode simulation :

```env
REGISTRATION_SIMULATION_ENABLED=true
REGISTRATION_SIMULATION_TO=devteam@example.com
```

Tous les mails d'inscription sont alors redirigés vers `devteam@example.com` avec une bannière indiquant le destinataire réel.
