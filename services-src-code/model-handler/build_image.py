from pathlib import Path
import sys


SERVICE_DIR = Path(__file__).resolve().parent
sys.path.append(str(SERVICE_DIR.parent))

from image_build_sdk import MavenBuildConfig, run


run(
    MavenBuildConfig(
        image_name="model-handler",
        service_dir=SERVICE_DIR,
    )
)
