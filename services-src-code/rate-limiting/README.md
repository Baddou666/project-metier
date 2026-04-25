# Rate Limiting Source

Code source Spring Boot du gateway/rate limiter.

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

L'image produite est `rate-limiter:latest`.

## Test local

Si Maven fonctionne sur votre machine :

```sh
./mvnw test
```

## Ensuite

Pour lancer le service dans Docker, revenir vers :

- [backend-services/rate-limiting-service](/d:/PRJ-METIER/project-metier/backend-services/rate-limiting-service/README.md:1)
- ou [modules/api-gateway-service](/d:/PRJ-METIER/project-metier/modules/api-gateway-service/README.md:1)
