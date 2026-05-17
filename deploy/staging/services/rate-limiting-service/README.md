# Rate Limiting Service

Ce dossier contient la configuration Docker du service `rate-limiting-service`.

## Avant de lancer

1. Builder l'image locale depuis [services-src-code/rate-limiting](/d:/PRJ-METIER/project-metier/services-src-code/rate-limiting/README.md:1)
2. Copier `.env.example` vers `.env`
3. Renseigner au minimum :
   - `REDIS_HOST`
   - `REDIS_INTERNAL_PORT`
   - `REDIS_PASSWORD`
   - `TOKEN_MANAGER_SERVICE_URI`
   - `JWKS_SRC_URL`
   - `AUTH_GATEWAY_SHARED_SECRET`

## Commandes

```sh
cp .env.example .env
docker compose up -d
docker compose logs -f rate-limiting-service
```

## Points de verification

- le conteneur monte `./config` et `./logs`
- `TOKEN_ROUTE_ID` doit correspondre a l'id de la route token configuree
- `AUTH_GATEWAY_SHARED_SECRET` doit etre identique a celui du `token-manager`

## Structure de configuration

Le gateway utilise maintenant la configuration native Spring Cloud Gateway WebFlux.

Le fichier `config/routes.properties` doit contenir des routes de la forme :

- `spring.cloud.gateway.routes[n].id`
- `spring.cloud.gateway.routes[n].uri`
- `spring.cloud.gateway.routes[n].predicates[*]`
- `spring.cloud.gateway.routes[n].metadata.access-policy`

Politiques actuellement prises en charge :

- `public-token`
- `protected-anonymous`

Le flux des utilisateurs connectes n'est pas encore implemente. Une requete protegee avec `Authorization: Bearer ...` renvoie `501 Not Implemented`.

## Test rapide

Le service n'est pas expose directement dans ce dossier. Le test normal se fait via l'environnement [deploy/staging](/d:/PRJ-METIER/project-metier/deploy/staging/README.md:1).
