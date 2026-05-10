from __future__ import annotations

import argparse
import getpass
import os
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


REGISTRY = "ghcr.io"
REGISTRY_USERNAME = "baddou666"
REPOSITORY = "projet-metier"
TEST_CHANNEL = "test"
FINAL_CHANNEL = "final"
DEFAULT_TOKEN_ENV = "GIT_API"
TOKEN_ENV_OVERRIDE_ENV = "IMAGE_BUILD_TOKEN_ENV"

VALID_ACTIONS = ("build", "push", "buildpush")
VALID_CHANNELS = (TEST_CHANNEL, FINAL_CHANNEL)


@dataclass(frozen=True)
class MavenBuildConfig:
    image_name: str
    service_dir: Path
    skip_tests: bool = True


@dataclass(frozen=True)
class DockerfileBuildConfig:
    image_name: str
    service_dir: Path
    dockerfile: str = "Dockerfile"
    context: str = "."
    target: str | None = None


BuildConfig = MavenBuildConfig | DockerfileBuildConfig


def run(config: BuildConfig, argv: Sequence[str] | None = None) -> None:
    args = _parse_args(argv)
    action = args.action or _prompt_action()
    channel = args.channel
    tag = args.tag
    token_env = _resolve_token_env(args.token_env)
    if action in ("push", "buildpush"):
        channel = channel or _prompt_channel(config.image_name)
        tag = tag or _prompt_tag(config.image_name)

    service_dir = config.service_dir.resolve()
    local_image = f"{config.image_name}:latest"

    _validate_service_dir(service_dir)
    if not args.dry_run:
        _ensure_docker_available()

    if action in ("build", "buildpush"):
        _build(config, service_dir, local_image, args.dry_run)

    if action in ("push", "buildpush"):
        remote_image = _remote_image(config.image_name, channel, tag)
        if action == "push":
            _ensure_local_image_exists(local_image, args.dry_run)
        _docker_login(token_env, args.dry_run)
        _run_command(["docker", "tag", local_image, remote_image], service_dir, args.dry_run)
        _run_command(["docker", "push", remote_image], service_dir, args.dry_run)

    print(f"[OK] {action} termine pour {config.image_name}")


def _parse_args(argv: Sequence[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build et push d'une image Docker de service."
    )
    parser.add_argument(
        "action",
        nargs="?",
        choices=VALID_ACTIONS,
        default=None,
        help="Action a executer: build, push ou buildpush. Si absent, un choix est demande.",
    )
    parser.add_argument(
        "--channel",
        choices=VALID_CHANNELS,
        default=None,
        help="Canal GHCR cible. Si absent pendant un push, un choix est demande.",
    )
    parser.add_argument(
        "--tag",
        default=None,
        help="Tag distant a pousser. Si absent pendant un push, un choix est demande.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Afficher les commandes sans les executer.",
    )
    parser.add_argument(
        "--token-env",
        default=None,
        help=(
            "Nom de la variable d'environnement contenant le token GHCR. "
            f"Defaut: ${TOKEN_ENV_OVERRIDE_ENV}, sinon {DEFAULT_TOKEN_ENV}."
        ),
    )
    return parser.parse_args(argv)


def _resolve_token_env(cli_value: str | None) -> str:
    token_env = cli_value or os.environ.get(TOKEN_ENV_OVERRIDE_ENV) or DEFAULT_TOKEN_ENV
    token_env = token_env.strip()
    if not token_env:
        raise SystemExit("[ERREUR] Le nom de variable du token GHCR est vide.")
    return token_env


def _prompt_action() -> str:
    while True:
        print("Que voulez-vous faire ?")
        print("1. Build et push")
        print("2. Seulement build")
        print("3. Seulement push")
        choice = input("Entrez 1, 2 ou 3 : ").strip()
        if choice == "1":
            return "buildpush"
        if choice == "2":
            return "build"
        if choice == "3":
            return "push"
        print("[ERREUR] Mode invalide. Utilisez 1, 2 ou 3.")


def _prompt_channel(image_name: str) -> str:
    while True:
        print(f"Choisissez la version a pousser pour {image_name}:")
        print(f"1. Test ({TEST_CHANNEL})")
        print(f"2. Final ({FINAL_CHANNEL})")
        choice = input("Entrez 1 pour Test, 2 pour Final : ").strip().lower()
        if choice == "1" or choice == TEST_CHANNEL:
            return TEST_CHANNEL
        if choice == "2" or choice == FINAL_CHANNEL:
            return FINAL_CHANNEL
        print("[ERREUR] Version invalide. Utilisez test ou final.")


def _prompt_tag(image_name: str) -> str:
    while True:
        tag = input(f"Entrez le tag de version pour {image_name} : ").strip()
        if tag:
            return tag
        print("[ERREUR] Le tag est obligatoire pour push/buildpush.")


def _build(config: BuildConfig, service_dir: Path, local_image: str, dry_run: bool) -> None:
    if isinstance(config, MavenBuildConfig):
        command = _maven_command(config)
        _run_command(command, service_dir, dry_run)
        return

    command = ["docker", "build", "-f", config.dockerfile]
    if config.target:
        command.extend(["--target", config.target])
    command.extend(["-t", local_image, config.context])
    _run_command(command, service_dir, dry_run)


def _maven_command(config: MavenBuildConfig) -> list[str]:
    wrapper = "mvnw.cmd" if os.name == "nt" else "./mvnw"
    command = [wrapper, "clean", "spring-boot:build-image"]
    if config.skip_tests:
        command.append("-DskipTests")
    return command


def _docker_login(token_env: str, dry_run: bool) -> None:
    token = os.environ.get(token_env)
    if not token and not dry_run:
        print(
            f"[INFO] La variable d'environnement {token_env} n'est pas definie.",
            file=sys.stderr,
        )
        print(
            f"[INFO] Definissez {token_env} ou entrez le token GHCR maintenant.",
            file=sys.stderr,
        )
        token = getpass.getpass("Token GHCR (saisie masquee) : ", stream=sys.stderr)
        if not token:
            raise SystemExit(
                f"[ERREUR] Token manquant. Definissez {token_env} ou relancez avec un token valide."
            )

    command = ["docker", "login", REGISTRY, "-u", REGISTRY_USERNAME, "--password-stdin"]
    if dry_run:
        print(f"[DRY-RUN] {' '.join(command)} < ${token_env}")
        return

    print(f"[RUN] {' '.join(command)} < ${token_env}")
    completed = subprocess.run(command, input=token, text=True)
    if completed.returncode != 0:
        raise SystemExit(f"[ERREUR] Echec de l'authentification a {REGISTRY}.")


def _ensure_docker_available() -> None:
    if shutil.which("docker") is None:
        raise SystemExit("[ERREUR] Docker n'est pas installe ou n'est pas dans le PATH.")


def _ensure_local_image_exists(local_image: str, dry_run: bool) -> None:
    command = ["docker", "image", "inspect", local_image]
    if dry_run:
        print(f"[DRY-RUN] {' '.join(command)}")
        return

    completed = subprocess.run(
        command,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    if completed.returncode != 0:
        raise SystemExit(
            f"[ERREUR] Aucune image locale {local_image} trouvee. Faites un build d'abord."
        )


def _validate_service_dir(service_dir: Path) -> None:
    if not service_dir.is_dir():
        raise SystemExit(f"[ERREUR] Dossier service introuvable: {service_dir}")


def _remote_image(image_name: str, channel: str, tag: str) -> str:
    return f"{REGISTRY}/{REGISTRY_USERNAME}/{REPOSITORY}/{channel}/{image_name}:{tag}"


def _run_command(command: Sequence[str], cwd: Path, dry_run: bool) -> None:
    display = _format_command(command)
    if dry_run:
        print(f"[DRY-RUN] ({cwd}) {display}")
        return

    print(f"[RUN] ({cwd}) {display}")
    completed = subprocess.run(command, cwd=cwd)
    if completed.returncode != 0:
        raise SystemExit(f"[ERREUR] Commande echouee: {display}")


def _format_command(command: Sequence[str]) -> str:
    return " ".join(str(part) for part in command)


if __name__ == "__main__":
    print(
        "Ce module est un SDK. Lancez plutot le build_image.py d'un service.",
        file=sys.stderr,
    )
    raise SystemExit(2)
