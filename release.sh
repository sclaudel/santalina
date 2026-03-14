#!/usr/bin/env bash
# =============================================================================
# release.sh — Script de release avec versioning sémantique
#
# Usage: ./release.sh [major|minor|patch]
#
# Par défaut (sans argument), choisit 'patch'.
# Met à jour gradle.properties, crée un commit de release et un tag Git.
# =============================================================================

set -euo pipefail

# ── Couleurs ────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ── Fonctions utilitaires ────────────────────────────────────────────────────
info()    { echo -e "${CYAN}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC}   $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Répertoire du script ─────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROPS_FILE="$SCRIPT_DIR/gradle.properties"

[[ -f "$PROPS_FILE" ]] || error "Fichier introuvable : $PROPS_FILE"

# ── Lecture de la version courante ──────────────────────────────────────────
CURRENT_VERSION=$(grep -E '^appVersion=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
[[ -n "$CURRENT_VERSION" ]] || error "La propriété 'appVersion' est absente de gradle.properties"

if ! [[ "$CURRENT_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    error "La version '$CURRENT_VERSION' ne respecte pas le format semver (X.Y.Z)"
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

info "Version courante : ${CURRENT_VERSION}"

# ── Choix de l'incrément ────────────────────────────────────────────────────
INCREMENT="${1:-}"

if [[ -z "$INCREMENT" ]]; then
    echo ""
    echo "Quel niveau de version souhaitez-vous incrémenter ?"
    echo "  1) patch  → ${MAJOR}.${MINOR}.$((PATCH + 1))"
    echo "  2) minor  → ${MAJOR}.$((MINOR + 1)).0"
    echo "  3) major  → $((MAJOR + 1)).0.0"
    echo ""
    read -rp "Votre choix [1/2/3] (défaut : 1 - patch) : " CHOICE
    case "${CHOICE:-1}" in
        1) INCREMENT="patch" ;;
        2) INCREMENT="minor" ;;
        3) INCREMENT="major" ;;
        *) error "Choix invalide : '$CHOICE'" ;;
    esac
fi

case "$INCREMENT" in
    patch) NEW_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))" ;;
    minor) NEW_VERSION="${MAJOR}.$((MINOR + 1)).0" ;;
    major) NEW_VERSION="$((MAJOR + 1)).0.0" ;;
    *) error "Incrément invalide : '$INCREMENT'. Valeurs acceptées : major | minor | patch" ;;
esac

info "Nouvelle version : ${NEW_VERSION}"
echo ""
read -rp "Confirmer la release v${NEW_VERSION} ? [o/N] : " CONFIRM
[[ "$(echo "$CONFIRM" | tr '[:upper:]' '[:lower:]')" == "o" ]] || { warn "Release annulée."; exit 0; }

# ── Vérification de l'état Git ───────────────────────────────────────────────
if ! git diff --quiet || ! git diff --cached --quiet; then
    error "Il existe des modifications non commitées. Commitez ou stashez vos changements avant de lancer la release."
fi

# ── Mise à jour de gradle.properties ────────────────────────────────────────
info "Mise à jour de gradle.properties (appVersion=${NEW_VERSION})"
if [[ "$(uname)" == "Darwin" ]]; then
    sed -i '' "s/^appVersion=.*/appVersion=${NEW_VERSION}/" "$PROPS_FILE"
else
    sed -i "s/^appVersion=.*/appVersion=${NEW_VERSION}/" "$PROPS_FILE"
fi

# ── Commit et tag Git ────────────────────────────────────────────────────────
TAG="v${NEW_VERSION}"

info "Commit de release"
git add "$PROPS_FILE"
git commit -m "chore: release ${TAG}"

info "Création du tag ${TAG}"
git tag -a "${TAG}" -m "Release ${TAG}"

# ── Push ────────────────────────────────────────────────────────────────────
info "Push du commit et du tag vers origin"
git push origin HEAD
git push origin "${TAG}"

echo ""
success "Release ${TAG} publiée avec succès !"
echo -e "  ${CYAN}Branche :${NC} $(git rev-parse --abbrev-ref HEAD)"
echo -e "  ${CYAN}Tag     :${NC} ${TAG}"
echo -e "  ${CYAN}Commit  :${NC} $(git rev-parse --short HEAD)"
