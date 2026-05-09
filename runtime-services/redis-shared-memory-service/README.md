# Redis Shared Memory Service

Ce dossier lance l'instance Redis partagee par les services backend.

## Preparation

```sh
cp .env.example .env
```

Renseigner :

- `REDIS_INTERNAL_PORT`
- `REDIS_EXTERNAL_PORT` si vous voulez publier Redis localement
- `REDIS_PASSWORD`

## Lancement

```sh
docker compose up -d
docker compose logs -f redis-server
```

## Test rapide

Si le port externe est publie, verifier avec `redis-cli` :

```sh
redis-cli -p <port> -a <password> ping
```

La reponse attendue est `PONG`.
