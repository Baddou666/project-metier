# Project Metier

Ce depot contient deux niveaux de travail :

- `services-src-code/` : le code source Spring Boot a builder localement
- `backend-services/` : les compose unitaires des services Docker
- `deploy/dev/` : l'assemblage Docker de developpement

## Prerequis

- Docker Desktop + Docker Compose
- JDK 21
- Git Bash ou un shell Linux pour les scripts `sh`
- `openssl` pour generer les certificats locaux

## Demarrage rapide

1. Builder les images locales des services Java utiles.
2. Renseigner les valeurs necessaires dans un fichier env local non versionne, ou utiliser `deploy/dev/.env.example` pour un lancement de base.
3. Generer la cle RSA du `token-manager`.
4. Lancer l'environnement depuis `deploy/dev`.
5. Tester les endpoints via le proxy HTTP/HTTPS.

## Ordre recommande

1. Lire [services-src-code/README.md](/d:/PRJ-METIER/project-metier/services-src-code/README.md:1)
2. Lire [backend-services/README.md](/d:/PRJ-METIER/project-metier/backend-services/README.md:1)
3. Suivre [deploy/dev/README.md](/d:/PRJ-METIER/project-metier/deploy/dev/README.md:1)

## Test minimal

Une fois la stack demarree, le flux nominal est :

1. appeler `POST /api/anonym-token/get` via le proxy
2. recuperer le JWT
3. utiliser ce JWT sur une route protegee du gateway

Les README de chaque dossier donnent les commandes exactes a executer.
