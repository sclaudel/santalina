#!/usr/bin/env bash
# ============================================================
# setup-docker.sh — Installation complète avec Docker
# ============================================================
set -e

echo "🌊 Installation de Santalina avec Docker"
echo "=========================================="

# Vérifier les prérequis
echo ""
echo "🔍 Vérification des prérequis..."

if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 n'est pas installé. Veuillez l'installer et relancer le script."
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ Node.js n'est pas installé. Veuillez l'installer et relancer le script."
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé. Veuillez installer Java 21+ et relancer le script."
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo "❌ Docker n'est pas installé. Veuillez l'installer et relancer le script."
    exit 1
fi

if ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose n'est pas installé. Veuillez l'installer et relancer le script."
    exit 1
fi

echo "✅ Prérequis vérifiés."

# 1. Générer les clés JWT
echo ""
echo "🔑 [1/3] Génération des clés JWT..."
if [ ! -f "keys/privateKey.pem" ] || [ ! -f "keys/publicKey.pem" ]; then
    python3 generate_keys.py
    echo "✅ Clés JWT générées."
else
    echo "ℹ️  Clés JWT déjà présentes, génération ignorée."
fi

# 2. Build de l'application
echo ""
echo "🔨 [2/3] Build de l'application..."
./build.sh

# 3. Démarrage avec Docker
echo ""
echo "🐳 [3/3] Démarrage avec Docker Compose..."
docker compose up --build -d

echo ""
echo "🎉 Installation terminée !"
echo ""
echo "L'application est disponible sur : http://localhost:8085"
echo "Interface de test des emails : http://localhost:8025"
echo ""
echo "Comptes par défaut :"
echo "  Admin : admin@santalina.com / AdminSecure@2024!"
echo ""
echo "Pour arrêter : docker compose down"
echo "Pour voir les logs : docker compose logs -f"