# 🌊 Lac Plongée — Système de Réservation

Application de réservation de créneaux de plongée en lac, développée avec **Quarkus 3.32.2** (Java 21) et **React 19** (TypeScript).

## ✨ Fonctionnalités

- 📅 **Calendrier public** : vues Jour et Semaine sans authentification
- 🔐 **Authentification JWT** : login, inscription, reset de mot de passe par email
- 👥 **3 rôles** :
  - `ADMIN` 🔑 : configuration, tous les créneaux, gestion des utilisateurs
  - `DIVE_DIRECTOR` 🤿 : création et suppression de ses propres créneaux
  - `GUEST` 👁️ : lecture seule
- ⏰ **Créneaux** : 1h min, 10h max, résolution 15 min, chevauchements autorisés dans la limite de capacité
- 🎯 **Capacité configurable** : max 25 plongeurs simultanés (modifiable par l'admin)
- 🗄️ **Double base de données** : H2 fichier (dev) / PostgreSQL (prod) — couche d'abstraction Panache
- 🐳 **Docker-ready** : Dockerfile multi-stage + docker-compose

---

## 🚀 Démarrage rapide (développement)

### Prérequis
- Java 21+
- Node.js 18+
- (Optionnel) Docker & Docker Compose

### 1. Générer les clés RSA JWT
```bash
python generate_keys.py
# Ou lancer une première fois via Gradle (les clés sont déjà générées)
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

> En mode dev, Quarkus utilise H2 (fichier `./data/lac-plongee`)

### Compte administrateur par défaut
- **Email** : `admin@lacplongee.com`
- **Mot de passe** : `Admin1234`

---

## 🐳 Déploiement Docker

```bash
# Build et démarrage complet (app + PostgreSQL + Mailhog)
docker compose up --build

# L'application est sur http://localhost:8080
# Interface mail de test : http://localhost:8025
```

### Variables d'environnement (docker-compose.yml)
| Variable | Description | Défaut |
|----------|-------------|--------|
| `JDBC_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://db:5432/lacplongee` |
| `DB_USER` | Utilisateur DB | `lacplongee` |
| `DB_PASSWORD` | Mot de passe DB | `lacplongee_secret` |
| `ADMIN_EMAIL` | Email admin initial | `admin@lacplongee.com` |
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
| `GET` | `/api/users/me` | Authentifié | Mon profil |
| `PUT` | `/api/users/me` | Authentifié | Modifier profil |
| `GET` | `/api/users` | ADMIN | Liste utilisateurs |
| `PUT` | `/api/users/{id}/role` | ADMIN | Changer rôle |
| `GET` | `/api/config` | Public | Config du lac |
| `PUT` | `/api/config/max-divers` | ADMIN | Max plongeurs |

Documentation Swagger : **http://localhost:8085/q/swagger-ui**

---

## 🏗️ Architecture

```
src/main/
├── java/com/example/diving/
│   ├── config/          # DivingConfig (@ConfigProperty)
│   ├── domain/          # Entités JPA (User, DiveSlot, AppConfigEntry)
│   ├── dto/             # Records Java (request/response)
│   ├── exception/       # GlobalExceptionMapper
│   ├── mail/            # PasswordResetMailer
│   ├── resource/        # JAX-RS endpoints
│   ├── security/        # JwtUtil, PasswordUtil (BCrypt)
│   ├── service/         # AuthService, SlotService, UserService, ConfigService
│   └── startup/         # AppStartup (init admin + config)
├── resources/
│   ├── application.properties
│   ├── db/migration/V1__init.sql
│   ├── privateKey.pem   # Clé privée JWT (générée, ne pas committer)
│   └── publicKey.pem    # Clé publique JWT
└── webui/               # Frontend React + TypeScript + Vite
    └── src/
        ├── components/  # NavBar, CalendarPicker, DayView, WeekView, SlotCard, SlotForm, LoginModal
        ├── context/     # AuthContext (JWT + state)
        ├── pages/       # CalendarPage, ProfilePage, AdminPage, ResetPasswordPage
        ├── services/    # api.ts, authService, slotService, adminService
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
