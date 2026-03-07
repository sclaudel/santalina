#!/usr/bin/env python3
"""
Script pour générer une paire de clés RSA (privée/publique) pour JWT (RS256).
Les clés seront enregistrées dans src/main/resources/privateKey.pem et publicKey.pem
Compatible Quarkus / SmallRye JWT
"""
import os
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend

# Répertoires de sortie
OUT_DIR = os.path.join("src", "main", "resources")
PRIVATE_KEY_PATH = os.path.join(OUT_DIR, "privateKey.pem")
PUBLIC_KEY_PATH = os.path.join(OUT_DIR, "publicKey.pem")

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

# Création du dossier si besoin
os.makedirs(OUT_DIR, exist_ok=True)

# Écriture des fichiers
with open(PRIVATE_KEY_PATH, "wb") as f:
    f.write(private_pem)
    print(f"Clé privée écrite dans {PRIVATE_KEY_PATH}")

with open(PUBLIC_KEY_PATH, "wb") as f:
    f.write(public_pem)
    print(f"Clé publique écrite dans {PUBLIC_KEY_PATH}")

print("\n✅ Génération terminée. Les clés sont prêtes à l'emploi.")
