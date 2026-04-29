# Authentication Source

Code source Spring Boot du `token-manager`.

## Prerequis

- JDK 21
- Docker Desktop actif pour `spring-boot:build-image`

## Build de l'image locale

Sous Windows :

```sh
./mvnw clean spring-boot:build-image -DskipTests
```

Sous Linux ou Git Bash :

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

La cle privee RSA attendue au runtime Docker se trouve dans `backend-services/token-manager/certs/jwt-private.pem`.

## Ensuite

Pour lancer le conteneur, voir [backend-services/token-manager/README.md](/d:/PRJ-METIER/project-metier/backend-services/token-manager/README.md:1).
