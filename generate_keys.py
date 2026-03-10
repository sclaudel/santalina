#!/usr/bin/env python3
"""
Script pour générer une paire de clés RSA (privée/publique) pour JWT (RS256).
Les clés seront enregistrées dans src/main/resources/privateKey.pem et publicKey.pem
Compatible Quarkus / SmallRye JWT
"""
import os
import base64
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend

# Répertoires de sortie
OUT_DIR = os.path.join("src", "main", "resources")
PRIVATE_KEY_PATH = os.path.join(OUT_DIR, "privateKey.pem")
PUBLIC_KEY_PATH = os.path.join(OUT_DIR, "publicKey.pem")

# Dossier pour les clés Docker (Ansible les lit depuis ce dossier)
KEYS_DIR = "keys"
DOCKER_PRIVATE_KEY_PATH = os.path.join(KEYS_DIR, "privateKey.pem")
DOCKER_PUBLIC_KEY_PATH  = os.path.join(KEYS_DIR, "publicKey.pem")

# Fichier .env local pour docker compose
ENV_FILE = ".env"

# Génération de la clé privée RSA 2048 bits
private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=2048,
    backend=default_backend()
)

# Sérialisation de la clé privée
private_pem = private_key.private_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PrivateFormat.PKCS8,
    encryption_algorithm=serialization.NoEncryption()
)

# Sérialisation de la clé publique
public_key = private_key.public_key()
public_pem = public_key.public_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)

# Création des dossiers si besoin
os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(KEYS_DIR, exist_ok=True)

# Écriture des fichiers — classpath (dev/test)
with open(PRIVATE_KEY_PATH, "wb") as f:
    f.write(private_pem)
    print(f"Clé privée (dev)    → {PRIVATE_KEY_PATH}")

with open(PUBLIC_KEY_PATH, "wb") as f:
    f.write(public_pem)
    print(f"Clé publique (dev)  → {PUBLIC_KEY_PATH}")

# Écriture des fichiers — Docker secrets (prod)
with open(DOCKER_PRIVATE_KEY_PATH, "wb") as f:
    f.write(private_pem)
    print(f"Clé privée (Docker) → {DOCKER_PRIVATE_KEY_PATH}")

with open(DOCKER_PUBLIC_KEY_PATH, "wb") as f:
    f.write(public_pem)
    print(f"Clé publique (Docker) → {DOCKER_PUBLIC_KEY_PATH}")

# Fichier .env — clés encodées en base64 pour docker compose (local et prod)
env_content = (
    f"MP_JWT_VERIFY_PUBLICKEY={base64.b64encode(public_pem).decode()}\n"
    f"SMALLRYE_JWT_SIGN_KEY={base64.b64encode(private_pem).decode()}\n"
)
with open(ENV_FILE, "w") as f:
    f.write(env_content)
print(f"Fichier .env         \u2192 {ENV_FILE}")

print("\n✅ Génération terminée.")
print("   \u2022 Développement local  : les clés dans src/main/resources/ sont lues par Quarkus au démarrage.")
print("   \u2022 Docker local         : le fichier .env est lu par docker compose (MP_JWT_VERIFY_PUBLICKEY / SMALLRYE_JWT_SIGN_KEY).")
print("   \u2022 Production (Ansible) : Ansible lit les fichiers PEM dans keys/ et les encode en base64 dans le .env distant.")
print("   \u26a0\ufe0f  Ne commitez jamais les fichiers .pem ni le .env \u2014 ils sont dans .gitignore.")
