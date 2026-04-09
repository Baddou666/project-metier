@echo off
echo ======================================================
echo   GENERATION DE L'IMAGE DOCKER (WINDOWS)
echo ======================================================

:: Vérification de Docker
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Docker n'est pas lance ou n'est pas installe.
    pause
    exit /b
)

echo [1/2] Nettoyage et compilation avec Maven Wrapper...
call ./mvnw clean spring-boot:build-image -DskipTests

if %errorlevel% neq 0 (
    echo [ERREUR] Le build Maven a echoue. Verifiez votre JDK.
    pause
    exit /b
)

echo [2/2] Liste des images generees :
docker images | findstr "rate-limiter"

echo ======================================================
echo   TERMINE : L'image est prete localement.
echo ======================================================
pause