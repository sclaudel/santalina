# Déploiement manuel — sans Ansible

Ce répertoire contient les fichiers nécessaires pour déployer Santalina sur un
VPS Ubuntu **sans Ansible**, en effectuant les étapes manuellement via SSH.

Pour un déploiement entièrement automatisé (recommandé), référez-vous au
[README Ansible](../ansible/README.md).

---

## Prérequis

- VPS Ubuntu 22.04 LTS ou 24.04 LTS  
- Accès SSH avec droits `sudo`  
- Un nom de domaine pointant vers l'IP du VPS  
- L'image Docker publiée sur DockerHub (par le workflow `release.yml`)

---

## Structure des fichiers

```
deploy/
├── docker-compose.prod.yml   # Composition des services de production
├── .env.example              # Modèle de variables d'environnement
└── nginx/
    └── nginx.conf            # Configuration Nginx (reverse proxy HTTPS)
```

---

## Installation — pas à pas

### 1. Connexion au VPS et installation de Docker

```bash
ssh ubuntu@VOTRE_IP_VPS

# Mettre à jour les paquets
sudo apt update && sudo apt upgrade -y

# Installer les prérequis
sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release ufw openssl

# Ajouter la clé GPG Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Ajouter le dépôt Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Installer Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Ajouter votre utilisateur au groupe docker (évite de taper sudo à chaque fois)
sudo usermod -aG docker $USER
newgrp docker

# Vérifier l'installation
docker --version
docker compose version
```

### 2. Configurer le pare-feu

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https
sudo ufw enable
sudo ufw status
```

### 3. Préparer le répertoire de déploiement

```bash
sudo mkdir -p /opt/santalina/keys /opt/santalina/nginx /opt/santalina/logs/app /opt/santalina/logs/nginx
sudo chown -R $USER:$USER /opt/santalina
chmod 750 /opt/santalina
chmod 700 /opt/santalina/keys
chmod 777 /opt/santalina/logs/app
```

### 4. Générer les clés RSA JWT

```bash
openssl genrsa -out /opt/santalina/keys/privateKey.pem 2048
openssl rsa \
  -in  /opt/santalina/keys/privateKey.pem \
  -pubout \
  -out /opt/santalina/keys/publicKey.pem
chmod 600 /opt/santalina/keys/privateKey.pem /opt/santalina/keys/publicKey.pem
```

> Ces clés ne doivent être générées **qu'une seule fois**. Les supprimer
> invaliderait toutes les sessions JWT actives.

### 5. Copier les fichiers de configuration

Depuis votre machine locale, copiez les fichiers de ce répertoire vers le VPS :

```bash
# Depuis la racine du projet local
scp deploy/docker-compose.prod.yml ubuntu@VOTRE_IP_VPS:/opt/santalina/docker-compose.yml
scp deploy/nginx/nginx.conf        ubuntu@VOTRE_IP_VPS:/opt/santalina/nginx/nginx.conf
```

Ou directement sur le VPS en créant les fichiers à la main.

### 6. Créer le fichier `.env`

```bash
cp /dev/stdin /opt/santalina/.env << 'EOF'
DB_PASSWORD=UnMotDePasseSQL_Solide!
ADMIN_EMAIL=admin@votre-domaine.fr
ADMIN_PASSWORD=UnMotDePasseAdmin_Solide!
MAILER_FROM=noreply@votre-domaine.fr
MAILER_HOST=smtp.votre-domaine.fr
MAILER_PORT=587
EOF
chmod 600 /opt/santalina/.env
```

### 7. Adapter `docker-compose.yml` et `nginx.conf`

Dans `/opt/santalina/docker-compose.yml`, remplacez `DOCKERHUB_USERNAME` par
votre nom d'utilisateur DockerHub :

```bash
sed -i 's/DOCKERHUB_USERNAME/mon_username/g' /opt/santalina/docker-compose.yml
```

Dans `/opt/santalina/nginx/nginx.conf`, remplacez `YOUR_DOMAIN` par votre
domaine réel. **Attention :** le domaine apparaît à plusieurs endroits dans
le fichier :

```bash
sed -i 's/YOUR_DOMAIN/santalina.mondomaine.fr/g' /opt/santalina/nginx/nginx.conf
```

### 8. Premier démarrage (HTTP uniquement)

Avant d'obtenir le certificat SSL, démarrez Nginx en HTTP uniquement afin que
Let's Encrypt puisse vérifier votre domaine.

Modifiez temporairement `nginx.conf` pour supprimer le bloc `server` HTTPS et
conserver uniquement le bloc port 80 avec la route `/.well-known/acme-challenge/`.

Ensuite, démarrez les services :

```bash
cd /opt/santalina
docker compose pull
docker compose up -d
docker compose ps    # vérifier que tous les services sont "running"
```

### 9. Obtenir le certificat SSL Let's Encrypt

```bash
docker compose run --rm certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d santalina.mondomaine.fr \
  --email moi@mondomaine.fr \
  --agree-tos \
  --non-interactive
```

### 10. Activer HTTPS dans Nginx

Restaurez la configuration Nginx complète (avec le bloc HTTPS) si vous l'aviez
modifiée à l'étape 8, puis rechargez :

```bash
docker compose exec nginx nginx -s reload
```

Vérifiez que l'application est accessible sur `https://santalina.mondomaine.fr`.

---

## Mises à jour

### Mettre à jour l'application vers la dernière version

```bash
cd /opt/santalina
docker compose pull app
docker compose up -d --no-deps --force-recreate app
docker image prune -f
```

### Mettre à jour vers un tag spécifique

```bash
# Éditer le tag dans docker-compose.yml
sed -i 's|santalina:.*|santalina:1.2.3|' /opt/santalina/docker-compose.yml
docker compose pull app
docker compose up -d --no-deps --force-recreate app
```

---

## Commandes de maintenance utiles

```bash
cd /opt/santalina

# Voir les logs en temps réel
docker compose logs -f

# Logs d'un seul service
docker compose logs -f app
docker compose logs -f nginx

# État des services
docker compose ps

# Redémarrer un service
docker compose restart app

# Recharger la config Nginx sans interruption
docker compose exec nginx nginx -s reload

# Vérifier le certificat SSL
docker compose exec certbot certbot certificates

# Sauvegarder la base de données
docker compose exec db pg_dump -U santalina santalina > backup_$(date +%Y%m%d).sql
```

---

## Structure des fichiers sur le VPS

```
/opt/santalina/
├── .env                     ← variables d'environnement (chmod 600)
├── docker-compose.yml       ← composition des services
├── keys/
│   ├── privateKey.pem       ← clé privée RSA JWT (chmod 600)
│   └── publicKey.pem        ← clé publique RSA JWT (chmod 600)
├── nginx/
│   └── nginx.conf           ← configuration Nginx
└── logs/
    ├── app/                 ← logs applicatifs Quarkus (bind mount)
    │   ├── santalina.log
    │   └── santalina.log.2026-03-16  ← fichiers rotatés par Quarkus (15 jours)
    └── nginx/               ← logs Nginx (bind mount, lu par fail2ban)
        ├── access.log
        └── error.log

Volumes Docker gérés automatiquement :
  santalina_postgres_data    ← données PostgreSQL
  santalina_app_data         ← données applicatives
  santalina_certbot_certs    ← certificats Let's Encrypt
  santalina_certbot_www      ← challenge ACME
```

### Rotation des logs

| Service     | Mécanisme                  | Rétention | Détails                                                      |
|-------------|----------------------------|-----------|--------------------------------------------------------------|
| Application | Quarkus (intégré)          | 15 jours  | Rotation quotidienne, suffixe `.yyyy-MM-dd`                  |
| Nginx       | logrotate (`/etc/logrotate.d/santalina`) | 15 jours | Rotation quotidienne, compression, signal `nginx -s reopen`  |
| SMTP        | Docker `json-file` driver  | 15 × 10 Mo | Rotation automatique par taille                              |
| Autres      | Docker `json-file` driver  | 5 × 10 Mo  | Rotation automatique par taille                              |

Les logs SMTP ne sont pas exportés en fichier. Pour les consulter :

```bash
docker logs santalina-smtp
docker logs --since 24h santalina-smtp
```

---

## Renouvellement SSL automatique

Le conteneur `certbot` tourne en arrière-plan et tente un renouvellement toutes
les 12 heures. Let's Encrypt ne renouvelle que si le certificat expire dans
moins de 30 jours. Aucune action manuelle n'est nécessaire.

---

## Résolution de problèmes

### Le site n'est pas accessible

```bash
# Vérifier que les conteneurs tournent
docker compose ps

# Vérifier le pare-feu
sudo ufw status

# Vérifier les ports écoutés
sudo ss -tlnp | grep -E '80|443'
```

### Erreur de certificat SSL

```bash
# Vérifier que le DNS pointe vers le bon serveur
dig +short votre-domaine.fr

# Vérifier les logs certbot
docker compose logs certbot
```

### L'application ne se connecte pas à la base de données

```bash
docker compose logs db
docker compose logs app | grep -i "datasource\|flyway\|postgresql"
```
