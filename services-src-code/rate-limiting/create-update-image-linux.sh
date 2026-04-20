#!/bin/bash

echo "======================================================"
echo "  GENERATION DE L'IMAGE DOCKER (LINUX/MAC)"
echo "======================================================"

# Parsing des arguments
MODE=$1
VERSION=$2
TAG=$3

# Défauts
WAS_TAG_PROVIDED=false
if [ -n "$3" ]; then
    WAS_TAG_PROVIDED=true
else
    TAG="latest"
fi

# Vérification de Docker
if ! command -v docker &> /dev/null
then
    echo "[ERREUR] Docker n'est pas installé ou n'est pas dans le PATH."
    exit 1
fi

# Demander le mode d'opération si pas fourni
if [ -z "$MODE" ]; then
    echo ""
    echo "======================================================"
    echo "  CHOIX DU MODE D'OPERATION"
    echo "======================================================"
    echo "Que voulez-vous faire ?"
    echo "1. Build et push"
    echo "2. Seulement build"
    echo "3. Seulement push"
    echo ""
    read -p "Entrez 1, 2 ou 3 : " mode
else
    mode=$MODE
fi

if [ "$mode" == "1" ]; then
    mode="buildpush"
elif [ "$mode" == "2" ]; then
    mode="build"
elif [ "$mode" == "3" ]; then
    mode="push"
fi

if [ "$mode" == "buildpush" ]; then
    DO_BUILD=true
    DO_PUSH=true
elif [ "$mode" == "build" ]; then
    DO_BUILD=true
    DO_PUSH=false
elif [ "$mode" == "push" ]; then
    DO_BUILD=false
    DO_PUSH=true
else
    echo "[ERREUR] Mode invalide. Utilisez build, push ou buildpush."
    exit 1
fi

if [ "$DO_BUILD" == "true" ]; then
    # Rendre le wrapper exécutable au cas où
    chmod +x mvnw

    echo "[1/4] Nettoyage et compilation avec Maven Wrapper..."
    ./mvnw clean spring-boot:build-image -DskipTests

    if [ $? -ne 0 ]; then
        echo ""
        echo "[ERREUR] Le build a échoué. Vérifiez les logs ci-dessus."
        exit 1
    fi

    echo ""
    echo "[2/4] Image(s) disponible(s) :"
    docker images | grep "rate-limiter"
    echo ""
fi

if [ "$DO_PUSH" == "true" ]; then
    # Vérifier si l'image existe (si pas de build)
    if [ "$DO_BUILD" == "false" ]; then
        if ! docker images | grep -q "rate-limiter"; then
            echo "[ERREUR] Aucune image rate-limiter trouvée localement. Faites un build d'abord."
            exit 1
        fi
        echo "Image rate-limiter trouvée localement."
    fi

    # Utiliser les arguments ou demander
    if [ -z "$VERSION" ]; then
        # Demander le choix de version
        echo "======================================================"
        echo "  CHOIX DE LA VERSION A POUSSER"
        echo "======================================================"
        echo "Choisissez la version :"
        echo "1. Test (ghcr.io/baddou666/projet-metier/test/rate-limiter)"
        echo "2. Final (ghcr.io/baddou666/projet-metier/final/rate-limiter)"
        echo ""
        read -p "Entrez 1 pour Test, 2 pour Final : " choice
    else
        if [ "$VERSION" == "test" ]; then
            choice="1"
        elif [ "$VERSION" == "final" ]; then
            choice="2"
        else
            echo "[ERREUR] Version invalide. Utilisez test ou final."
            exit 1
        fi
    fi

    if [ "$choice" == "1" ]; then
        BASE_TAG="ghcr.io/baddou666/projet-metier/test/rate-limiter"
        echo "Version Test sélectionnée."
    elif [ "$choice" == "2" ]; then
        BASE_TAG="ghcr.io/baddou666/projet-metier/final/rate-limiter"
        echo "Version Final sélectionnée."
    else
        BASE_TAG="ghcr.io/baddou666/projet-metier/final/rate-limiter"
        echo "Version Final sélectionnée par défaut."
    fi

    if [ "$WAS_TAG_PROVIDED" == "false" ]; then
        echo ""
        read -p "Entrez le tag de version (latest par défaut) : " tag
        if [ -z "$tag" ]; then
            tag="latest"
        fi
    else
        tag="$TAG"
    fi
    IMAGE_TAG="$BASE_TAG:$tag"

    # Vérification de la clé API
    if [ -z "$GIT_API" ]; then
        echo "[ERREUR] La variable d'environnement GIT_API n'est pas définie. Authentification impossible."
        exit 1
    fi

    echo "[3/4] Authentification à GHCR..."
    echo "$GIT_API" | docker login ghcr.io -u baddou666 --password-stdin
    if [ $? -ne 0 ]; then
        echo "[ERREUR] Échec de l'authentification à GHCR."
        exit 1
    fi

    echo "[4/4] Tag et push de l'image..."
    docker tag rate-limiter:latest "$IMAGE_TAG"
    docker push "$IMAGE_TAG"

    if [ $? -ne 0 ]; then
        echo "[ERREUR] Échec du push de l'image."
        exit 1
    fi

    echo "======================================================"
    echo "  TERMINE : L'image a été poussée vers $IMAGE_TAG"
    echo "======================================================"
else
    echo "======================================================"
    echo "  TERMINE : L'image est prête localement."
    echo "======================================================"
fi