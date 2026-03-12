# Configuration de l'authentification email (SPF + DKIM)

Sans SPF et DKIM, les emails envoyés par l'application seront **refusés ou marqués comme spam**
par Gmail, Outlook et la majorité des FAI.

---

## 1. SPF — Enregistrement DNS à créer manuellement

Le SPF indique aux serveurs de réception quelles IPs sont autorisées à envoyer des emails
pour votre domaine.

**Dans la zone DNS de votre domaine (chez votre registrar / hébergeur DNS) :**

| Type | Nom | Valeur |
|------|-----|--------|
| TXT  | `@` | `v=spf1 ip4:<IP_DU_VPS> ~all` |

Remplacer `<IP_DU_VPS>` par l'adresse IPv4 publique de votre serveur.

> ⚠️ Si un enregistrement SPF existe déjà, **ne pas en créer un second** —
> ajouter `ip4:<IP_DU_VPS>` dans l'enregistrement existant.

**Si vous utilisez un relay SMTP externe** (`app_smtp_relayhost` dans `vars.yml`),
remplacer `ip4:...` par le mécanisme `include:` de votre fournisseur :

| Fournisseur | Valeur SPF |
|-------------|------------|
| OVH         | `v=spf1 include:mx.ovh.com ~all` |
| SendGrid    | `v=spf1 include:sendgrid.net ~all` |
| Brevo       | `v=spf1 include:spf.sendinblue.com ~all` |
| Amazon SES  | `v=spf1 include:amazonses.com ~all` |

---

## 2. DKIM — Automatisé par Ansible

Le DKIM signe cryptographiquement chaque email. La clé privée est générée automatiquement
par le conteneur `santalina-smtp` au premier démarrage, et persistée dans le répertoire `dkim_keys/` sur l'hôte (bind mount — survit à `docker compose down -v`).

### Étapes après le premier déploiement

**1. Récupérer la clé publique DKIM générée :**

```bash
docker exec santalina-smtp cat /etc/opendkim/domainkeys/<votre-domaine>.txt
```

Le playbook Ansible affiche automatiquement cette clé en fin d'exécution
(tâche *"Afficher la clé publique DKIM"*).

**2. Ajouter l'enregistrement DNS :**

| Type | Nom | Valeur |
|------|-----|--------|
| TXT  | `mail._domainkey.<votre-domaine>` | contenu du fichier `.txt` ci-dessus |

Exemple de valeur :
```
v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
```

> ⚠️ Ne jamais supprimer le répertoire `dkim_keys/` sur le serveur — cela invaliderait
> l'enregistrement DNS existant et nécessiterait de republier une nouvelle clé.

---

## 3. Vérification

Une fois les enregistrements DNS propagés (quelques minutes à 48h selon le TTL) :

- **SPF** : https://mxtoolbox.com/spf.aspx
- **DKIM** : https://mxtoolbox.com/dkim.aspx (sélecteur : `mail`)
- **Test d'envoi complet** : https://www.mail-tester.com

---

## 4. Option "mail désactivé"

Pendant la phase de configuration DNS (domaine non encore pointé, SPF/DKIM non configurés),
l'envoi réel de mail peut être désactivé via la variable Ansible `mail_disabled: true` dans `vars.yml`.

Dans ce mode, les emails sont interceptés par Quarkus et **loggués sans être envoyés**
(mode mock — aucune connexion SMTP n'est établie).

```yaml
# vars.yml
mail_disabled: true
```

Remettre à `false` (ou supprimer la ligne) une fois le domaine et les DNS configurés.
