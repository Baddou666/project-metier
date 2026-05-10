# Model Handler Source

Code source Spring Boot du service `model-handler`.

## Prerequis

- Python 3.10 ou plus recent
- JDK 21
- Docker disponible dans le `PATH`
- `GIT_API` defini dans l'environnement pour pousser vers GHCR

Le nom de cette variable est configurable globalement avec `--token-env` ou `IMAGE_BUILD_TOKEN_ENV`.

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

Commande Maven equivalente :

```sh
./mvnw clean spring-boot:build-image -DskipTests
```

L'image produite est :

```text
model-handler:latest
```
