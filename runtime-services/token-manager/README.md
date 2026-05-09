# Token Manager

Ce dossier contient la configuration Docker du service d'authentification.

## Preparation

1. Builder l'image locale depuis [services-src-code/Authentication](/d:/PRJ-METIER/project-metier/services-src-code/Authentication/README.md:1)
2. Copier `.env.example` vers `.env`
3. Generer la cle privee RSA dans `certs/`

## Generation de la cle

Depuis ce dossier :

```sh
cp .env.example .env
sh create-jwt-private-key.sh
```

Le script cree `certs/jwt-private.pem`. Il refuse d'ecraser une cle existante.

## Variables importantes

- `REDIS_HOST`
- `REDIS_INTERNAL_PORT`
- `REDIS_PASSWORD`
- `FORWARDED_CLIENT_IP_HEADER`
- `AUTH_GATEWAY_SHARED_SECRET_HEADER`
- `AUTH_GATEWAY_SHARED_SECRET`

Le secret partage doit etre identique a celui du `rate-limiting-service`.

## Lancement

```sh
docker compose up -d
docker compose logs -f token-manager-service
```

## Test rapide

Une fois la stack complete demarree, tester la generation de token via le gateway et non directement contre ce service.
