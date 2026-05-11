@echo off
REM ============================================================
REM setup-docker.bat — Installation complète avec Docker (Windows)
REM ============================================================
echo.
echo ===========================================
echo  Installation de Santalina avec Docker
echo ===========================================

REM Vérifier les prérequis
echo.
echo [VERIFICATION] Vérification des prérequis...

where python >nul 2>nul
if %errorlevel% neq 0 (
    echo ERREUR: Python n'est pas installé. Veuillez l'installer et relancer le script.
    pause
    exit /b 1
)

where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ERREUR: Node.js n'est pas installé. Veuillez l'installer et relancer le script.
    pause
    exit /b 1
)

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERREUR: Java n'est pas installé. Veuillez installer Java 21+ et relancer le script.
    pause
    exit /b 1
)

where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo ERREUR: Docker n'est pas installé. Veuillez l'installer et relancer le script.
    pause
    exit /b 1
)

docker compose version >nul 2>nul
if %errorlevel% neq 0 (
    echo ERREUR: Docker Compose n'est pas installé. Veuillez l'installer et relancer le script.
    pause
    exit /b 1
)

echo Prérequis vérifiés avec succès.

REM 1. Générer les clés JWT
echo.
echo [1/3] Génération des clés JWT...
if not exist "keys\privateKey.pem" (
    if not exist "keys\publicKey.pem" (
        python generate_keys.py
        echo Clés JWT générées avec succès.
    )
) else (
    echo INFO: Clés JWT déjà présentes, génération ignorée.
)

REM 2. Build de l'application
echo.
echo [2/3] Build de l'application...
call build.bat

REM 3. Démarrage avec Docker
echo.
echo [3/3] Démarrage avec Docker Compose...
docker compose up --build -d

echo.
echo ===========================================
echo  Installation terminée !
echo ===========================================
echo.
echo L'application est disponible sur : http://localhost:8085
echo Interface de test des emails : http://localhost:8025
echo.
echo Comptes par défaut :
echo   Admin : admin@santalina.com / AdminSecure@2024!
echo.
echo Pour arrêter : docker compose down
echo Pour voir les logs : docker compose logs -f
echo.
pause