# Dev Deployment

Ce dossier assemble les services Docker de developpement.

## Services inclus

- proxy HTTPS et frontend
- rate limiter, token manager et Redis
- model handler, transformer model et PostgreSQL
- Loki, Promtail et Grafana

## Preparation

1. Builder les images locales quand elles ne sont pas disponibles dans le registry.
2. Generer la cle RSA du `token-manager` depuis `runtime-services/token-manager`.
3. Generer les certificats du proxy HTTPS depuis `runtime-services/https-proxy-service`.
4. Renseigner les valeurs necessaires dans un fichier env local non versionne si les valeurs de `.env.example` ne suffisent pas.

## Lancement

Depuis la racine du projet :

```sh
docker compose --env-file deploy/dev/.env.example -f deploy/dev/docker-compose.yml up -d
docker compose --env-file deploy/dev/.env.example -f deploy/dev/docker-compose.yml ps
```

Pour utiliser des secrets locaux, creer un fichier non versionne dans `deploy/dev/` et le passer avec `--env-file`.

## Arret

```sh
docker compose --env-file deploy/dev/.env.example -f deploy/dev/docker-compose.yml down
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

## Monitoring

Grafana est disponible sur `http://localhost:${GRAFANA_PORT}`. Loki et Promtail sont raccordes au reseau `monitoring-network`.

Les dashboards provisionnes se trouvent dans `runtime-services/grafana-service/dashboards`.
