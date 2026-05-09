# Runtime Services

Ce dossier contient les briques Docker reutilisees par les modules.

## Dossiers utiles

- `https-proxy-service/` : proxy Nginx en facade
- `rate-limiting-service/` : conteneur du gateway/rate limiter
- `token-manager/` : conteneur du service d'authentification
- `redis-shared-memory-service/` : Redis partage

## Preparation

1. Copier les fichiers `.env.example` vers `.env` quand ils existent.
2. Generer les artefacts locaux necessaires avant `docker compose up`.
3. Verifier que les dossiers montes en volume existent : `config/`, `logs/`, `certs/`.

## Lecture recommandee

- [rate-limiting-service/README.md](/d:/PRJ-METIER/project-metier/runtime-services/rate-limiting-service/README.md:1)
- [token-manager/README.md](/d:/PRJ-METIER/project-metier/runtime-services/token-manager/README.md:1)
- [redis-shared-memory-service/README.md](/d:/PRJ-METIER/project-metier/runtime-services/redis-shared-memory-service/README.md:1)
- [https-proxy-service/README.md](/d:/PRJ-METIER/project-metier/runtime-services/https-proxy-service/README.md:1)
