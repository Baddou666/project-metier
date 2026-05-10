# Authentication Source

Code source Spring Boot du `token-manager`.

## Prerequis

- JDK 21
- Docker Desktop actif pour `spring-boot:build-image`

Pour pousser vers GHCR, le script utilise `GIT_API` par defaut. Le nom est configurable avec `--token-env` ou `IMAGE_BUILD_TOKEN_ENV`.

## Build de l'image locale

Methode recommandee :

```sh
python build_image.py build
```

Build et push :

```sh
python build_image.py buildpush --channel final --tag v1.0.0
```

Si `--channel` ou `--tag` ne sont pas fournis, le script les demande explicitement.

Verification sans executer Docker/Maven :

```sh
python build_image.py buildpush --channel final --tag v1.0.0 --dry-run
```

Commande Maven equivalente sous Windows :

```sh
./mvnw clean spring-boot:build-image -DskipTests
```

Commande Maven equivalente sous Linux ou Git Bash :

```sh
chmod +x mvnw
./mvnw clean spring-boot:build-image -DskipTests
```

L'image produite est `token-manager:latest`.

## Test local

```sh
./mvnw test
```

## Configuration attendue au runtime

Le service consomme au minimum :

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `FORWARDED_CLIENT_IP_HEADER`
- `AUTH_GATEWAY_SHARED_SECRET_HEADER`
- `AUTH_GATEWAY_SHARED_SECRET`

La cle privee RSA attendue au runtime Docker se trouve dans `runtime-services/token-manager/certs/jwt-private.pem`.

## Ensuite

Pour lancer le conteneur, voir [runtime-services/token-manager/README.md](/d:/PRJ-METIER/project-metier/runtime-services/token-manager/README.md:1).
