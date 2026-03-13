# 🌊 Santalina — Système de Réservation

[![Dernière version](https://img.shields.io/github/v/release/sclaudel/santalina?label=derni%C3%A8re%20version&logo=github&color=blue&include_prereleases)](https://github.com/sclaudel/santalina/releases/latest)
[![Dependabot](https://img.shields.io/badge/Dependabot-activé-0075ca?logo=dependabot&logoColor=white)](/.github/dependabot.yml)
[![Renovate](https://img.shields.io/badge/Renovate-activé-1a1f6c?logo=renovatebot&logoColor=white)](https://github.com/renovatebot/renovate)

Application de réservation de créneaux de plongée en lac, développée avec **Quarkus 3.32.2** (Java 21) et **React 19** (TypeScript).

## ✨ Fonctionnalités

- 📅 **Calendrier public** : vues Jour, Semaine et Mois **chronologiques** sans authentification
- 🔐 **Authentification JWT** : login, inscription, reset de mot de passe par email
- 👥 **3 rôles** (cumulables) :
  - `ADMIN` 🔑 : configuration, tous les créneaux, gestion des utilisateurs
  - `DIVE_DIRECTOR` 🤿 : création et suppression de ses propres créneaux + gestion des plongeurs
  - `DIVER` 🏊 : lecture seule
- ⏰ **Créneaux** : 1h min, 10h max, résolution 15 min, chevauchements affichés côte à côte
- 🤿 **Plongeurs** : ajout/modification/suppression de plongeurs (nom, prénom, niveau) sur chaque créneau
- 🎯 **Capacité configurable** : max 25 plongeurs simultanés (modifiable par l'admin)
- 🗄️ **Double base de données** : H2 fichier (dev) / PostgreSQL (prod) — couche d'abstraction Panache
- � **Statistiques** (ADMIN) : tableau de bord avec camemberts, histogrammes et tableaux — par mois, par année, par club, par type de créneau, avec filtres période
- �🐳 **Docker-ready** : Dockerfile multi-stage + docker-compose

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
| `POST` | `/api/auth/register` | Public | Inscription |
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

Accessible via le menu **Statistiques** (réservé au rôle `ADMIN`).

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
├── java/com/example/diving/
│   ├── config/          # DivingConfig (@ConfigProperty)
│   ├── domain/          # Entités JPA (User, DiveSlot, SlotDiver, AppConfigEntry)
│   ├── dto/             # Records Java (request/response)
│   ├── exception/       # GlobalExceptionMapper
│   ├── mail/            # PasswordResetMailer
│   ├── resource/        # JAX-RS endpoints (dont StatsResource)
│   ├── security/        # JwtUtil, PasswordUtil (BCrypt)
│   ├── service/         # AuthService, SlotService, UserService, ConfigService
│   └── startup/         # AppStartup (init admin + config)
├── resources/
│   ├── application.properties
│   ├── db/migration/    # Flyway V1..V5
│   ├── privateKey.pem   # Clé privée JWT (générée, ne pas committer)
│   └── publicKey.pem    # Clé publique JWT
└── webui/               # Frontend React + TypeScript + Vite
    └── src/
        ├── components/  # NavBar, CalendarPicker, DayView, WeekView, MonthView, SlotBlock, SlotForm, LoginModal
        ├── context/     # AuthContext (JWT + état multi-rôles)
        ├── pages/       # CalendarPage, ProfilePage, AdminPage, ResetPasswordPage, StatsPage
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
