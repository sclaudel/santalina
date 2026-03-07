#!/usr/bin/env bash
# ============================================================
# build.sh — Script de build complet : Frontend + Backend
# ============================================================
set -e

echo "🌊 Build de l'application"
echo "======================================"

# 1. Build du frontend React
echo ""
echo "📦 [1/2] Build du frontend React..."
cd src/main/webui
npm install --silent
npm run build
cd ../../..
echo "✅ Frontend buildé → src/main/resources/META-INF/resources/"

# 2. Build du backend Quarkus
echo ""
echo "☕ [2/2] Build du backend Quarkus..."
./gradlew build -x test --no-daemon
echo "✅ Backend buildé → build/quarkus-app/"

echo ""
echo "🎉 Build terminé avec succès !"
echo ""
echo "Pour démarrer l'application :"
echo "  java -jar build/quarkus-app/quarkus-run.jar"
echo ""
echo "Ou avec Docker :"
echo "  docker compose up --build"

