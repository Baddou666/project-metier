# AI Transformer Model Source

Code source du service `transformer-model`.

## Prerequis

- Python 3.10 ou plus recent
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

Verification sans executer Docker :

```sh
python build_image.py buildpush --channel final --tag v1.0.0 --dry-run
```

Commande Docker equivalente :

```sh
docker build -f Dockerfile -t transformer-model:latest .
```

L'image produite est :

```text
transformer-model:latest
```
