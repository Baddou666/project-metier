# API Gateway Module

Ce module assemble la stack principale :

- `https-proxy-service`
- `rate-limiting-service`
- `token-manager`
- `redis-shared-memory-service`

## Preparation

1. Builder les images locales :
   - [services-src-code/rate-limiting](/d:/PRJ-METIER/project-metier/services-src-code/rate-limiting/README.md:1)
   - [services-src-code/Authentication](/d:/PRJ-METIER/project-metier/services-src-code/Authentication/README.md:1)
2. Copier `.env.example` vers `.env`
3. Verifier que `AUTH_GATEWAY_SHARED_SECRET` est renseigne
4. Generer la cle RSA dans [backend-services/token-manager](/d:/PRJ-METIER/project-metier/backend-services/token-manager/README.md:1)
5. Generer les certificats du proxy HTTPS

## Lancement

Depuis ce dossier :

```sh
cp .env.example .env
docker compose up -d
docker compose ps
```

## Arret

```sh
docker compose down
```

## Test du flux token

HTTP :

```sh
curl -X POST http://localhost/api/anonym-token/get -H "X-Real-IP: 1.1.1.1"
```

HTTPS :

```sh
curl -k -X POST https://localhost/api/anonym-token/get -H "X-Real-IP: 1.1.1.1"
```

Attendu :

- un statut `202`
- un JSON contenant un champ `token`

## Diagnostic rapide

- `403` sur token : verifier `AUTH_GATEWAY_SHARED_SECRET`
- `400` sur token : verifier `X-Real-IP`
- `5xx` : lire les logs du proxy, du rate limiter et du token manager
