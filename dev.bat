@echo off
REM ============================================================
REM dev.bat — Démarrage en mode développement
REM Lance le frontend React (Vite dev) + Backend Quarkus en parallèle
REM ============================================================

echo.
echo ======================================
echo  Mode développement - Lac Plongée
echo ======================================
echo.
echo Backend  : http://localhost:8085
echo Frontend : http://localhost:5173 (proxy vers :8085)
echo Swagger  : http://localhost:8085/q/swagger-ui
echo.

REM Démarrer le frontend Vite en arrière-plan
start "Frontend React (Vite)" cmd /k "cd src\main\webui && npm run dev"

REM Démarrer Quarkus en mode dev
call gradlew.bat quarkusDev

