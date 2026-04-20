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

:: Demander le mode d'operation
echo.
echo ======================================================
echo   CHOIX DU MODE D'OPERATION
echo ======================================================
echo Que voulez-vous faire ?
echo 1. Build et push
echo 2. Seulement build
echo 3. Seulement push
echo.
set /p mode="Entrez 1, 2 ou 3 : "

if "%mode%"=="1" goto build_and_push
if "%mode%"=="2" goto only_build
if "%mode%"=="3" goto only_push
echo [ERREUR] Choix invalide.
pause
exit /b

:only_build
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
exit /b

:build_and_push
echo [1/4] Nettoyage et compilation avec Maven Wrapper...
call ./mvnw clean spring-boot:build-image -DskipTests

if %errorlevel% neq 0 (
    echo [ERREUR] Le build Maven a echoue. Verifiez votre JDK.
    pause
    exit /b
)

echo [2/4] Liste des images generees :
docker images | findstr "rate-limiter"

goto choose_version

:only_push
:: Verifier si l'image existe
docker images | findstr "rate-limiter" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Aucune image rate-limiter trouvee localement. Faites un build d'abord.
    pause
    exit /b
)

echo Image rate-limiter trouvee localement.
goto choose_version

:choose_version
:: Demander le choix de version
echo.
echo ======================================================
echo   CHOIX DE LA VERSION A POUSSER
echo ======================================================
echo Choisissez la version :
echo 1. Test (ghcr.io/baddou666/projet-metier/test/rate-limiter)
echo 2. Final (ghcr.io/baddou666/projet-metier/final/rate-limiter)
echo.
set /p choice="Entrez 1 pour Test, 2 pour Final : "

if "%choice%"=="1" (
    set BASE_TAG=ghcr.io/baddou666/projet-metier/test/rate-limiter
    echo Version Test selectionnee.
) else if "%choice%"=="2" (
    set BASE_TAG=ghcr.io/baddou666/projet-metier/final/rate-limiter
    echo Version Final selectionnee.
) else (
    set BASE_TAG=ghcr.io/baddou666/projet-metier/final/rate-limiter
    echo Version Final selectionnee par defaut.
)

echo.
set /p tag="Entrez le tag de version (latest par defaut) : "
if "%tag%"=="" set tag=latest
set IMAGE_TAG=%BASE_TAG%:%tag%

:: Vérification de la clé API
if "%GIT_API%"=="" (
    echo [ERREUR] La variable d'environnement GIT_API n'est pas definie. Authentification impossible.
    pause
    exit /b
)

echo [3/4] Authentification a GHCR...
echo %GIT_API% | docker login ghcr.io -u baddou666 --password-stdin
if %errorlevel% neq 0 (
    echo [ERREUR] Echec de l'authentification a GHCR.
    pause
    exit /b
)

echo [4/4] Tag et push de l'image...
docker tag rate-limiter:latest %IMAGE_TAG%
docker push %IMAGE_TAG%

if %errorlevel% neq 0 (
    echo [ERREUR] Echec du push de l'image.
    pause
    exit /b
)

echo ======================================================
echo   TERMINE : L'image a ete poussee vers %IMAGE_TAG%
echo ======================================================
pause