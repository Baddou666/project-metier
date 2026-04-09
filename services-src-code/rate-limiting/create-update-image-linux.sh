#!/bin/bash

echo "======================================================"
echo "  GENERATION DE L'IMAGE DOCKER (LINUX/MAC)"
echo "======================================================"

# Vérification de Docker
if ! command -v docker &> /dev/null
then
    echo "[ERREUR] Docker n'est pas installé ou n'est pas dans le PATH."
    exit 1
fi

# Rendre le wrapper exécutable au cas où
chmod +x mvnw

echo "[1/2] Nettoyage et compilation avec Maven Wrapper..."
./mvnw clean spring-boot:build-image -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "[2/2] Image(s) disponible(s) :"
    docker images | grep "rate-limiter"
    echo ""
    echo "======================================================"
    echo "  TERMINE : L'image est prête localement."
    echo "======================================================"
else
    echo ""
    echo "[ERREUR] Le build a échoué. Vérifiez les logs ci-dessus."
    exit 1
fi