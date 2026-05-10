from pathlib import Path
import sys


SERVICE_DIR = Path(__file__).resolve().parent
sys.path.append(str(SERVICE_DIR.parent))

from image_build_sdk import DockerfileBuildConfig, run


run(
    DockerfileBuildConfig(
        image_name="ai-text-detector-frontend",
        service_dir=SERVICE_DIR,
        target="production",
    )
)
