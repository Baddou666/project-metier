# Project Metier

Ce depot contient deux niveaux de travail :

- `services-src-code/` : le code source Spring Boot a builder localement
- `backend-services/` et `modules/` : les stacks Docker pour lancer et tester les services

## Prerequis

- Docker Desktop + Docker Compose
- JDK 21
- Git Bash ou un shell Linux pour les scripts `sh`
- `openssl` pour generer les certificats locaux

## Demarrage rapide

1. Builder les images locales des services Java utiles.
2. Prepararer les fichiers `.env` a partir des `.env.example`.
3. Generer la cle RSA du `token-manager`.
4. Lancer la stack depuis `modules/api-gateway-service`.
5. Tester les endpoints via le proxy HTTP/HTTPS.

## Ordre recommande

1. Lire [services-src-code/README.md](/d:/PRJ-METIER/project-metier/services-src-code/README.md:1)
2. Lire [backend-services/README.md](/d:/PRJ-METIER/project-metier/backend-services/README.md:1)
3. Suivre [modules/api-gateway-service/README.md](/d:/PRJ-METIER/project-metier/modules/api-gateway-service/README.md:1)

## Test minimal

Une fois la stack demarree, le flux nominal est :

1. appeler `POST /api/anonym-token/get` via le proxy
2. recuperer le JWT
3. utiliser ce JWT sur une route protegee du gateway

Les README de chaque dossier donnent les commandes exactes a executer.
