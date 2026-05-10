from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path
from typing import Sequence


ROOT_DIR = Path(__file__).resolve().parent
VALID_ACTIONS = ("build", "push", "buildpush")


def main(argv: Sequence[str] | None = None) -> None:
    args = _parse_args(argv)
    action = args.action or _prompt_action()
    scripts = _discover_service_scripts()

    if not scripts:
        raise SystemExit("[ERREUR] Aucun build_image.py de service trouve.")

    print("[INFO] Services detectes:", flush=True)
    for script in scripts:
        print(f"  - {script.parent.name}", flush=True)

    forwarded_args = [action]
    if args.channel:
        forwarded_args.extend(["--channel", args.channel])
    if args.tag:
        forwarded_args.extend(["--tag", args.tag])
    if args.token_env:
        forwarded_args.extend(["--token-env", args.token_env])
    if args.dry_run:
        forwarded_args.append("--dry-run")

    for script in scripts:
        command = [sys.executable, str(script), *forwarded_args]
        print(f"[INFO] Execution: {' '.join(command)}", flush=True)
        completed = subprocess.run(command, cwd=script.parent)
        if completed.returncode != 0:
            raise SystemExit(
                f"[ERREUR] Build interrompu apres echec du service {script.parent.name}."
            )

    print("[OK] Tous les services detectes ont ete traites.", flush=True)


def _parse_args(argv: Sequence[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Execute les build_image.py de tous les services detectes."
    )
    parser.add_argument(
        "action",
        nargs="?",
        choices=VALID_ACTIONS,
        default=None,
        help="Action a executer. Si absent, un choix est demande.",
    )
    parser.add_argument(
        "--channel",
        choices=("test", "final"),
        default=None,
        help="Canal GHCR cible transmis aux services.",
    )
    parser.add_argument(
        "--tag",
        default=None,
        help="Tag distant transmis aux services.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Afficher les commandes sans les executer.",
    )
    parser.add_argument(
        "--token-env",
        default=None,
        help="Nom de la variable d'environnement contenant le token GHCR.",
    )
    return parser.parse_args(argv)


def _prompt_action() -> str:
    while True:
        print("Que voulez-vous faire pour tous les services detectes ?")
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


def _discover_service_scripts() -> list[Path]:
    scripts: list[Path] = []
    for child in sorted(ROOT_DIR.iterdir(), key=lambda item: item.name):
        if not child.is_dir():
            continue
        script = child / "build_image.py"
        if script.is_file():
            scripts.append(script)
    return scripts


if __name__ == "__main__":
    main()
