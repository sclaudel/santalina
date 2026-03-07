@echo off
REM ============================================================
REM build.bat — Script de build complet pour Windows
REM ============================================================
echo.
echo ======================================
echo  Build (Windows)
echo ======================================

echo.
echo [1/2] Build du frontend React...
cd src\main\webui
call npm install --silent
call npm run build
cd ..\..\..
echo Frontend buildé avec succès.

echo.
echo [2/2] Build du backend Quarkus...
call gradlew.bat build -x test --no-daemon
echo Backend buildé avec succès.

echo.
echo Build termine !
echo Pour demarrer : java -jar build\quarkus-app\quarkus-run.jar

