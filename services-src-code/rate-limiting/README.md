# Rate Limiting Source

Code source du gateway/rate limiter base sur Spring Cloud Gateway WebFlux.

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

L'image produite est `rate-limiter:latest`.

## Configuration

Le service charge sa configuration depuis :

- `src/main/resources/config/*.properties` pour les valeurs par defaut
- `./config/*.properties` si vous lancez localement avec un dossier de config
- `/workspace/config/*.properties` dans le conteneur Docker

Les routes sont maintenant definies avec les proprietes natives du gateway :

- `spring.cloud.gateway.routes[n].id`
- `spring.cloud.gateway.routes[n].uri`
- `spring.cloud.gateway.routes[n].predicates[*]`
- `spring.cloud.gateway.routes[n].metadata.access-policy`

Valeurs de `access-policy` actuellement supportees :

- `public-token` pour `/api/anonym-token/get`
- `protected-anonymous` pour `/api/ai-detector/**`

Le flux utilisateur connecte n'est pas encore implemente. Toute requete `Bearer ...` sur une route protegee retourne `501 Not Implemented` avec un message explicite.

## Test local

Si Maven fonctionne sur votre machine :

```sh
./mvnw test
```

## Ensuite

Pour lancer le service dans Docker, revenir vers :

- [runtime-services/rate-limiting-service](/d:/PRJ-METIER/project-metier/runtime-services/rate-limiting-service/README.md:1)
- ou [deploy/dev](/d:/PRJ-METIER/project-metier/deploy/dev/README.md:1)
