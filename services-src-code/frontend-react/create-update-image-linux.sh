#!/usr/bin/env bash

# ======================================================
# CONFIGURATION
# ======================================================
IMAGE_NAME="ai-text-detector-frontend"
LOCAL_IMAGE="${IMAGE_NAME}:latest"
REGISTRY="ghcr.io"
REGISTRY_USERNAME="baddou666"
REPOSITORY="projet-metier"
TEST_CHANNEL="test"
FINAL_CHANNEL="final"
DEFAULT_CHANNEL="final"
DEFAULT_TAG="latest"
API_KEY_ENV="GIT_API"
DOCKERFILE="Dockerfile"
BUILD_CONTEXT="."
BUILD_TARGET="production"
PRE_BUILD_COMMAND=""
BUILD_COMMAND="docker build -f ${DOCKERFILE} --target ${BUILD_TARGET} -t ${LOCAL_IMAGE} ${BUILD_CONTEXT}"
BUILD_LABEL="Construction de l'image Docker frontend"

echo "======================================================"
echo "  GENERATION DE L'IMAGE DOCKER (LINUX/MAC)"
echo "======================================================"

MODE="$1"
VERSION="$2"
TAG="$3"
WAS_TAG_PROVIDED=true
if [ -z "$TAG" ]; then
    TAG="$DEFAULT_TAG"
    WAS_TAG_PROVIDED=false
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "[ERREUR] Docker n'est pas installe ou n'est pas dans le PATH."
    exit 1
fi

if [ -z "$MODE" ]; then
    echo
    echo "======================================================"
    echo "  CHOIX DU MODE D'OPERATION"
    echo "======================================================"
    echo "Que voulez-vous faire ?"
    echo "1. Build et push"
    echo "2. Seulement build"
    echo "3. Seulement push"
    echo
    read -r -p "Entrez 1, 2 ou 3 : " mode
else
    mode="$MODE"
fi

case "$mode" in
    1) mode="buildpush" ;;
    2) mode="build" ;;
    3) mode="push" ;;
esac

case "$mode" in
    buildpush) DO_BUILD=true; DO_PUSH=true ;;
    build) DO_BUILD=true; DO_PUSH=false ;;
    push) DO_BUILD=false; DO_PUSH=true ;;
    *)
        echo "[ERREUR] Mode invalide (${mode}). Utilisez build, push ou buildpush."
        exit 1
        ;;
esac

if [ "$DO_BUILD" = true ]; then
    if [ -n "$PRE_BUILD_COMMAND" ]; then
        eval "$PRE_BUILD_COMMAND"
    fi

    echo "[1/4] ${BUILD_LABEL}..."
    if ! eval "$BUILD_COMMAND"; then
        echo "[ERREUR] Le build a echoue."
        exit 1
    fi

    echo
    echo "[2/4] Liste des images generees :"
    docker images | grep "$IMAGE_NAME" || true
    echo
fi

if [ "$DO_PUSH" = true ]; then
    if [ "$DO_BUILD" = false ]; then
        if ! docker images | grep -q "$IMAGE_NAME"; then
            echo "[ERREUR] Aucune image ${IMAGE_NAME} trouvee localement. Faites un build d'abord."
            exit 1
        fi
        echo "Image ${IMAGE_NAME} trouvee localement."
    fi

    if [ -z "$VERSION" ]; then
        echo "======================================================"
        echo "  CHOIX DE LA VERSION A POUSSER"
        echo "======================================================"
        echo "Choisissez la version :"
        echo "1. Test (${REGISTRY}/${REGISTRY_USERNAME}/${REPOSITORY}/${TEST_CHANNEL}/${IMAGE_NAME})"
        echo "2. Final (${REGISTRY}/${REGISTRY_USERNAME}/${REPOSITORY}/${FINAL_CHANNEL}/${IMAGE_NAME})"
        echo
        read -r -p "Entrez 1 pour Test, 2 pour Final : " choice
    else
        case "$VERSION" in
            test) choice="1" ;;
            final) choice="2" ;;
            *)
                echo "[ERREUR] Version invalide: '${VERSION}'. Utilisez test ou final."
                exit 1
                ;;
        esac
    fi

    if [ "$choice" = "1" ]; then
        CHANNEL="$TEST_CHANNEL"
        echo "Version Test selectionnee."
    elif [ "$choice" = "2" ]; then
        CHANNEL="$FINAL_CHANNEL"
        echo "Version Final selectionnee."
    else
        CHANNEL="$DEFAULT_CHANNEL"
        echo "Version ${DEFAULT_CHANNEL} selectionnee par defaut."
    fi

    if [ "$WAS_TAG_PROVIDED" = false ]; then
        echo
        read -r -p "Entrez le tag de version (${DEFAULT_TAG} par defaut) : " tag
        if [ -z "$tag" ]; then
            tag="$DEFAULT_TAG"
        fi
    else
        tag="$TAG"
    fi

    IMAGE_TAG="${REGISTRY}/${REGISTRY_USERNAME}/${REPOSITORY}/${CHANNEL}/${IMAGE_NAME}:${tag}"
    API_TOKEN="${!API_KEY_ENV:-}"
    if [ -z "$API_TOKEN" ]; then
        echo "[ERREUR] La variable d'environnement ${API_KEY_ENV} n'est pas definie. Authentification impossible."
        exit 1
    fi

    echo "[3/4] Authentification a ${REGISTRY}..."
    if ! echo "$API_TOKEN" | docker login "$REGISTRY" -u "$REGISTRY_USERNAME" --password-stdin; then
        echo "[ERREUR] Echec de l'authentification a ${REGISTRY}."
        exit 1
    fi

    echo "[4/4] Tag et push de l'image..."
    docker tag "$LOCAL_IMAGE" "$IMAGE_TAG"
    docker push "$IMAGE_TAG"
    echo "======================================================"
    echo "  TERMINE : L'image a ete poussee vers ${IMAGE_TAG}"
    echo "======================================================"
else
    echo "======================================================"
    echo "  TERMINE : L'image est prete localement."
    echo "======================================================"
fi
