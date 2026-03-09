# Déploiement automatisé — Ansible

Ce répertoire contient un playbook Ansible permettant de **provisionner un VPS Ubuntu
vierge** et d'y déployer l'application Santalina de bout en bout :

- Installation de Docker Engine + Docker Compose Plugin  
- Configuration du pare-feu UFW (ports 22 / 80 / 443 uniquement)  
- Génération des clés RSA JWT  
- Déploiement des services via Docker Compose (PostgreSQL, Quarkus, Nginx, Certbot)  
- Obtention automatique d'un certificat SSL Let's Encrypt  
- Rechargement de Nginx en HTTPS

---

## Prérequis

### Sur votre machine locale

| Outil | Version minimale | Installation |
|---|---|---|
| Python 3 | 3.9+ | `sudo apt install python3` |
| Ansible | 2.14+ | `pip install ansible` |
| OpenSSH client | — | déjà présent sur Linux/macOS |

Installez les collections Ansible requises :

```bash
cd ansible/
ansible-galaxy collection install -r requirements.yml
```

### Côté VPS Ubuntu

- Ubuntu 22.04 LTS ou 24.04 LTS (testé)  
- Accès SSH avec une **clé publique** déjà déposée (pas de mot de passe)  
- L'utilisateur SSH doit pouvoir utiliser `sudo` sans mot de passe, ce qui est le
  cas par défaut pour l'utilisateur `ubuntu` sur la plupart des hébergeurs (OVH,
  Scaleway, Hetzner, DigitalOcean…)  
- Un **nom de domaine** pointant vers l'IP du VPS (enregistrement A ou CNAME) —
  obligatoire pour Let's Encrypt

> **Note Hetzner / OVH :** le pare-feu du fournisseur est indépendant d'UFW.
> Vérifiez que les ports 80 et 443 sont ouverts dans la console de votre
> hébergeur.

---

## Structure des fichiers

```
ansible/
├── playbook.yml              # Point d'entrée principal
├── requirements.yml          # Collections Galaxy requises
├── inventory.ini.example     # Modèle d'inventaire (à copier)
├── vars.yml.example          # Modèle de variables (à copier)
└── roles/
    ├── docker/
    │   └── tasks/
    │       └── main.yml      # Installation Docker + configuration pare-feu
    └── santalina/
        ├── tasks/
        │   └── main.yml      # Déploiement de l'application
        └── templates/
            ├── docker-compose.prod.yml.j2  # Compose de production
            ├── nginx.conf.j2               # Configuration Nginx
            └── env.j2                      # Fichier .env de l'application
```

---

## Première installation — pas à pas

### 1. Préparer les fichiers de configuration

```bash
# Depuis la racine du projet
cp ansible/inventory.ini.example ansible/inventory.ini
cp ansible/vars.yml.example       ansible/vars.yml
```

Ces deux fichiers sont ignorés par Git (`.gitignore`) : ils ne seront jamais
commités.

### 2. Remplir l'inventaire

Éditez `ansible/inventory.ini` :

```ini
[vps]
203.0.113.42 ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/id_rsa

[vps:vars]
ansible_python_interpreter=/usr/bin/python3
```

- Remplacez `203.0.113.42` par l'IP publique de votre VPS.  
- `ansible_user=ubuntu` est correct pour Ubuntu chez la majorité des hébergeurs.
  Chez certains (ex. OVH VPS), l'utilisateur peut être `debian` ou le vôtre.  
- Adaptez `ansible_ssh_private_key_file` si votre clé SSH n'est pas `~/.ssh/id_rsa`.

Testez la connexion avant de continuer :

```bash
ansible -i ansible/inventory.ini vps -m ping
```

Réponse attendue : `vps | SUCCESS => { "ping": "pong" }`

### 3. Remplir les variables

Éditez `ansible/vars.yml` en remplissant **toutes** les valeurs :

```yaml
# Domaine public — doit déjà pointer vers le VPS
app_domain: santalina.mondomaine.fr

# Email pour les alertes d'expiration SSL Let's Encrypt
letsencrypt_email: moi@mondomaine.fr

# Répertoire de déploiement sur le VPS
app_dir: /opt/santalina

# Votre nom d'utilisateur DockerHub
dockerhub_username: mon_username_dockerhub

# Tag de l'image à déployer
app_image_tag: latest

# Mettre à false si le domaine ne pointe pas encore vers le VPS
obtain_ssl: true

# Secrets applicatifs
app_db_password: "UnMotDePasseSQL_Solide!"
app_admin_email: admin@mondomaine.fr
app_admin_password: "UnMotDePasseAdmin_Solide!"

# Serveur SMTP pour l'envoi d'emails
app_mailer_from: noreply@mondomaine.fr
app_mailer_host: smtp.mondomaine.fr
app_mailer_port: 587
```

> **Mots de passe :** utilisez des mots de passe forts (16+ caractères, mixte
> majuscules/minuscules/chiffres/symboles). Ces valeurs ne doivent jamais être
> commitées.

### 4. Lancer le playbook

```bash
cd ansible/
ansible-playbook -i inventory.ini playbook.yml
```

Le playbook s'exécute en deux phases automatiques :

1. **Phase 1** — Nginx démarre en HTTP uniquement pour répondre aux vérifications
   ACME de Let's Encrypt.
2. **Phase 2** — Certbot obtient le certificat, puis Nginx est rechargé en HTTPS.

Durée typique : 3 à 5 minutes sur un VPS standard.

À la fin, l'application est accessible sur `https://votre-domaine`.

---

## Options utiles

### Installer Docker uniquement (sans déployer l'application)

```bash
ansible-playbook -i inventory.ini playbook.yml --tags docker
```

### Redéployer l'application uniquement (sans retoucher Docker)

```bash
ansible-playbook -i inventory.ini playbook.yml --tags app
```

### Démarrer sans SSL (domaine pas encore configuré)

```bash
ansible-playbook -i inventory.ini playbook.yml -e "obtain_ssl=false"
```

Dans ce cas, l'application est accessible en HTTP sur le port 80. Vous pourrez
obtenir le certificat plus tard en relançant le playbook avec `obtain_ssl=true`.

### Déployer une version spécifique

```bash
ansible-playbook -i inventory.ini playbook.yml -e "app_image_tag=1.2.3"
```

### Mode simulation (dry-run, sans modifier le serveur)

```bash
ansible-playbook -i inventory.ini playbook.yml --check
```

---

## Déploiements suivants (mise à jour de l'application)

Pour les mises à jour ultérieures, le workflow GitHub Actions
(`.github/workflows/deploy.yml`) s'en charge automatiquement après chaque
publication d'un tag Git. Voir la section [CI/CD](#cicd--github-actions) ci-dessous.

Vous pouvez aussi déclencher un redéploiement manuellement depuis GitHub Actions
(onglet *Actions → Deploy to VPS → Run workflow*) en précisant un tag d'image.

Pour mettre à jour **manuellement** depuis le VPS :

```bash
# Sur le VPS
cd /opt/santalina
docker compose pull app
docker compose up -d --no-deps --force-recreate app
docker image prune -f
```

---

## CI/CD — GitHub Actions

Le fichier `.github/workflows/deploy.yml` orchestre le cycle complet :

```
git tag v1.2.3 → push → release.yml (build + push DockerHub) → deploy.yml (SSH → VPS)
```

### Secrets GitHub à configurer

Rendez-vous dans **Settings → Secrets and variables → Actions** de votre dépôt
et créez les secrets suivants :

| Secret | Description |
|---|---|
| `VPS_HOST` | IP publique ou domaine du VPS |
| `VPS_USER` | Utilisateur SSH (ex. `ubuntu`) |
| `VPS_SSH_KEY` | Clé privée SSH complète (contenu du fichier `~/.ssh/id_rsa`) |
| `DOCKERHUB_USERNAME` | Nom d'utilisateur DockerHub |
| `DOCKERHUB_TOKEN` | Token d'accès DockerHub (pas le mot de passe) |

### Créer et pousser un tag pour déclencher le déploiement

```bash
git tag v1.0.0
git push origin v1.0.0
```

Le pipeline:
1. Construit l'application (Gradle + npm)
2. Construit et pousse l'image Docker sur DockerHub
3. Se connecte au VPS en SSH
4. Remplace l'image applicative et recrée le conteneur sans interruption de service

---

## Structure des services sur le VPS

```
/opt/santalina/               ← app_dir
├── .env                      ← variables d'environnement (600)
├── docker-compose.yml        ← composition des services
├── keys/
│   ├── privateKey.pem        ← clé privée RSA JWT (600, générée une fois)
│   └── publicKey.pem         ← clé publique RSA JWT (600)
└── nginx/
    └── nginx.conf            ← configuration Nginx

Volumes Docker :
  postgres_data               ← données PostgreSQL
  app_data                    ← données applicatives
  certbot_certs               ← certificats Let's Encrypt
  certbot_www                 ← challenge ACME
```

> **Important :** les clés JWT dans `keys/` sont générées une **seule** fois lors
> de la première installation. Le playbook ne les écrase jamais si elles existent
> déjà. Ne les supprimez pas : cela invaliderait toutes les sessions actives.

---

## Renouvellement SSL

Le renouvellement est **entièrement automatique** : le conteneur `certbot` tourne
en arrière-plan et tente un renouvellement toutes les 12 heures. Let's Encrypt ne
renouvelle que si le certificat expire dans moins de 30 jours.

Pour vérifier l'état du certificat :

```bash
# Sur le VPS
docker compose -f /opt/santalina/docker-compose.yml exec certbot certbot certificates
```

---

## Résolution de problèmes fréquents

### `ansible: command not found`

```bash
pip install ansible
# ou sur Ubuntu
sudo apt install ansible
```

### `Permission denied (publickey)`

Vérifiez que la clé publique correspondant à `ansible_ssh_private_key_file` est
bien présente dans `~/.ssh/authorized_keys` sur le VPS.

### Let's Encrypt échoue (`Challenge failed`)

- Vérifiez que le DNS de votre domaine pointe bien vers l'IP du VPS :
  ```bash
  dig +short votre-domaine.fr
  ```
- Vérifiez qu'aucun pare-feu fournisseur ne bloque le port 80.
- Relancez avec `obtain_ssl=true` une fois le DNS propagé.

### L'application ne démarre pas

```bash
# Sur le VPS
cd /opt/santalina
docker compose logs app
docker compose logs db
```

### Rechargement Nginx après modification de la config

```bash
# Sur le VPS
docker compose exec nginx nginx -s reload
```
