# Services Source Code

Ce dossier contient le code source des services applicatifs et les scripts Python de build/push d'images Docker.

## Build d'images Docker

La logique commune est centralisee dans :

- `image_build_sdk.py`

Chaque service declare sa propre image dans son fichier local :

- `authentication/build_image.py`
- `model-handler/build_image.py`
- `rate-limiting/build_image.py`
- `frontend-react/build_image.py`
- `ai-transformer-model/build_image.py`

La methode de build/push recommandee est maintenant Python.

## Prerequis

- Python 3.10 ou plus recent
- Docker disponible dans le `PATH`
- JDK 21 pour les services Maven/Spring Boot
- une variable d'environnement contenant le token GHCR

Exemple Windows PowerShell :

```powershell
$env:GIT_API="ghp_xxx"
```

Exemple Linux/macOS :

```bash
export GIT_API="ghp_xxx"
```

Par defaut, le SDK lit `GIT_API`. Le nom peut etre change pour toute l'execution avec `--token-env` :

```bash
python build_image.py buildpush --channel final --tag v1.0.0 --token-env MY_GHCR_TOKEN
```

Ou via une variable de controle :

```bash
export IMAGE_BUILD_TOKEN_ENV="MY_GHCR_TOKEN"
```

Si cette variable n'existe pas au moment du push, le script affiche une erreur claire et demande le token GHCR manuellement avec une saisie masquee. Le token n'est jamais affiche en sortie standard.

## Utilisation par service

Depuis le dossier d'un service :

```bash
python build_image.py
```

Si aucun argument n'est fourni, le script demande explicitement :

1. `build`, `push` ou `buildpush`
2. `test` ou `final` si un push est demande
3. le tag si un push est demande

Utilisation sans prompt :

```bash
python build_image.py build
python build_image.py push --channel test --tag v1.0.0
python build_image.py buildpush --channel final --tag v1.0.0
python build_image.py buildpush --channel final --tag v1.0.0 --token-env MY_GHCR_TOKEN
```

Verification sans executer Docker/Maven :

```bash
python build_image.py buildpush --channel final --tag v1.0.0 --dry-run
```

## Utilisation globale

Depuis `services-src-code` :

```bash
python build_all_images.py build
python build_all_images.py buildpush
python build_all_images.py buildpush --channel final --tag v1.0.0
python build_all_images.py buildpush --channel final --tag v1.0.0 --token-env MY_GHCR_TOKEN
```

`build_all_images.py` detecte automatiquement les sous-dossiers qui contiennent un `build_image.py`.

Important : si `--channel` et `--tag` ne sont pas fournis en mode `push` ou `buildpush`, chaque service demandera ses propres valeurs.

Verification globale sans executer Docker/Maven :

```bash
python build_all_images.py build --dry-run
```

## Images declarees

| Service | Image locale | Methode de build |
| --- | --- | --- |
| `authentication` | `token-manager:latest` | Maven Wrapper / Spring Boot Buildpacks |
| `model-handler` | `model-handler:latest` | Maven Wrapper / Spring Boot Buildpacks |
| `rate-limiting` | `rate-limiter:latest` | Maven Wrapper / Spring Boot Buildpacks |
| `frontend-react` | `ai-text-detector-frontend:latest` | Dockerfile target `production` |
| `ai-transformer-model` | `transformer-model:latest` | Dockerfile |

Les images poussees suivent ce format :

```text
ghcr.io/baddou666/projet-metier/<test|final>/<image>:<tag>
```

## Ajouter un service

Ajouter un fichier `build_image.py` dans le dossier du service.

Service Maven/Spring Boot :

```python
from pathlib import Path
import sys


SERVICE_DIR = Path(__file__).resolve().parent
sys.path.append(str(SERVICE_DIR.parent))

from image_build_sdk import MavenBuildConfig, run


run(
    MavenBuildConfig(
        image_name="my-image-name",
        service_dir=SERVICE_DIR,
    )
)
```

Service Dockerfile :

```python
from pathlib import Path
import sys


SERVICE_DIR = Path(__file__).resolve().parent
sys.path.append(str(SERVICE_DIR.parent))

from image_build_sdk import DockerfileBuildConfig, run


run(
    DockerfileBuildConfig(
        image_name="my-image-name",
        service_dir=SERVICE_DIR,
        dockerfile="Dockerfile",
        context=".",
    )
)
```

Si le Dockerfile utilise une target :

```python
DockerfileBuildConfig(
    image_name="my-image-name",
    service_dir=SERVICE_DIR,
    target="production",
)
```

## README par service

- [authentication/README.md](authentication/README.md)
- [frontend-react/README.md](frontend-react/README.md)
- [model-handler/README.md](model-handler/README.md)
- [rate-limiting/README.md](rate-limiting/README.md)
- [ai-transformer-model/README.md](ai-transformer-model/README.md)
