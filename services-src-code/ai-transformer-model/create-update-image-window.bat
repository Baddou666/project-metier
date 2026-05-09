@echo off
setlocal

:: ======================================================
:: CONFIGURATION
:: ======================================================
set "IMAGE_NAME=transformer-model"
set "LOCAL_IMAGE=%IMAGE_NAME%:latest"
set "REGISTRY=ghcr.io"
set "REGISTRY_USERNAME=baddou666"
set "REPOSITORY=projet-metier"
set "TEST_CHANNEL=test"
set "FINAL_CHANNEL=final"
set "DEFAULT_CHANNEL=final"
set "DEFAULT_TAG=latest"
set "API_KEY_ENV=GIT_API"
set "DOCKERFILE=Dockerfile"
set "BUILD_CONTEXT=."
set "BUILD_COMMAND=docker build -f %DOCKERFILE% -t %LOCAL_IMAGE% %BUILD_CONTEXT%"
set "BUILD_LABEL=Construction de l'image Docker"

echo ======================================================
echo   GENERATION DE L'IMAGE DOCKER (WINDOWS)
echo ======================================================

set "MODE=%~1"
set "VERSION=%~2"
set "TAG=%~3"
if "%TAG%"=="" set "TAG=%DEFAULT_TAG%"

docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Docker n'est pas lance ou n'est pas installe.
    pause
    exit /b 1
)

if "%MODE%"=="" (
    echo.
    echo ======================================================
    echo   CHOIX DU MODE D'OPERATION
    echo ======================================================
    echo Que voulez-vous faire ?
    echo 1. Build et push
    echo 2. Seulement build
    echo 3. Seulement push
    echo.
    set /p "mode=Entrez 1, 2 ou 3 : "
) else (
    set "mode=%MODE%"
)

if "%mode%"=="1" set "mode=buildpush"
if "%mode%"=="2" set "mode=build"
if "%mode%"=="3" set "mode=push"
if "%mode%"=="buildpush" goto build_and_push
if "%mode%"=="build" goto only_build
if "%mode%"=="push" goto only_push
echo [ERREUR] Mode invalide (%mode%). Utilisez build, push ou buildpush.
pause
exit /b 1

:only_build
echo [1/2] %BUILD_LABEL%...
%BUILD_COMMAND%
if %errorlevel% neq 0 (
    echo [ERREUR] Le build a echoue.
    pause
    exit /b 1
)

echo [2/2] Liste des images generees :
docker images | findstr "%IMAGE_NAME%"
echo ======================================================
echo   TERMINE : L'image est prete localement.
echo ======================================================
pause
exit /b 0

:build_and_push
echo [1/4] %BUILD_LABEL%...
%BUILD_COMMAND%
if %errorlevel% neq 0 (
    echo [ERREUR] Le build a echoue.
    pause
    exit /b 1
)

echo [2/4] Liste des images generees :
docker images | findstr "%IMAGE_NAME%"
goto choose_version

:only_push
docker images | findstr "%IMAGE_NAME%" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Aucune image %IMAGE_NAME% trouvee localement. Faites un build d'abord.
    pause
    exit /b 1
)

echo Image %IMAGE_NAME% trouvee localement.
goto choose_version

:choose_version
if "%VERSION%"=="" (
    echo.
    echo ======================================================
    echo   CHOIX DE LA VERSION A POUSSER
    echo ======================================================
    echo Choisissez la version :
    echo 1. Test "%REGISTRY%/%REGISTRY_USERNAME%/%REPOSITORY%/%TEST_CHANNEL%/%IMAGE_NAME%"
    echo 2. Final "%REGISTRY%/%REGISTRY_USERNAME%/%REPOSITORY%/%FINAL_CHANNEL%/%IMAGE_NAME%"
    echo.
    set /p "choice=Entrez 1 pour Test, 2 pour Final : "
) else (
    if "%VERSION%"=="test" set "choice=1"
    if "%VERSION%"=="final" set "choice=2"
    call :check_choice
    if errorlevel 1 exit /b 1
)

if "%choice%"=="1" (
    set "CHANNEL=%TEST_CHANNEL%"
    echo Version Test selectionnee.
) else if "%choice%"=="2" (
    set "CHANNEL=%FINAL_CHANNEL%"
    echo Version Final selectionnee.
) else (
    set "CHANNEL=%DEFAULT_CHANNEL%"
    echo Version %DEFAULT_CHANNEL% selectionnee par defaut.
)

if "%~3"=="" (
    echo.
    set /p "tag=Entrez le tag de version (%DEFAULT_TAG% par defaut) : "
    if "%tag%"=="" set "tag=%DEFAULT_TAG%"
) else (
    set "tag=%TAG%"
)

set "IMAGE_TAG=%REGISTRY%/%REGISTRY_USERNAME%/%REPOSITORY%/%CHANNEL%/%IMAGE_NAME%:%tag%"

call set "API_TOKEN=%%%API_KEY_ENV%%%"
if "%API_TOKEN%"=="" (
    echo [ERREUR] La variable d'environnement %API_KEY_ENV% n'est pas definie. Authentification impossible.
    pause
    exit /b 1
)

echo [3/4] Authentification a %REGISTRY%...
echo %API_TOKEN% | docker login %REGISTRY% -u %REGISTRY_USERNAME% --password-stdin
if %errorlevel% neq 0 (
    echo [ERREUR] Echec de l'authentification a %REGISTRY%.
    pause
    exit /b 1
)

echo [4/4] Tag et push de l'image...
docker tag %LOCAL_IMAGE% %IMAGE_TAG%
docker push %IMAGE_TAG%
if %errorlevel% neq 0 (
    echo [ERREUR] Echec du push de l'image.
    pause
    exit /b 1
)

echo ======================================================
echo   TERMINE : L'image a ete poussee vers %IMAGE_TAG%
echo ======================================================
pause
exit /b 0

:check_choice
if "%choice%"=="" (
    echo [ERREUR] Version invalide: '%VERSION%'. Utilisez test ou final.
    pause
    exit /b 1
)
goto :EOF
