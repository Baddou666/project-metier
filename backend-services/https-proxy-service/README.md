# HTTPS Proxy Service

Ce dossier contient le proxy Nginx frontal.

## Preparation

1. Copier `.env.example` vers `.env`
2. Generer les certificats locaux attendus par la configuration Nginx

## Generation des certificats

Sous Linux ou Git Bash :

```sh
sh install-certs.sh
```

Si vous utilisez une autre methode, assurez-vous d'obtenir les fichiers SSL attendus dans `certs/`.

## Variables importantes

- `PROXY_API_HOST`
- `PROXY_API_PORT`

Elles doivent pointer vers le service `rate-limiting-service`.

Le frontend est route par defaut vers `ai-text-detector-frontend:3000` via la configuration Docker Compose du proxy.

## Lancement

```sh
docker compose up -d
docker compose logs -f https-proxy
```

## Test rapide

Une fois la stack complete demarree :

```sh
curl -k https://localhost/api/anonym-token/get -X POST -H "X-Real-IP: 1.1.1.1"
```
