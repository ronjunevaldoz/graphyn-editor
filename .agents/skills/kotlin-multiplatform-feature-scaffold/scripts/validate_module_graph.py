#!/usr/bin/env python3
from __future__ import annotations

import argparse
import sys
from pathlib import Path


EXPECTED_MODULES = ("model", "api", "domain", "data", "presenter", "ui")


def validate_module_graph(root: Path, feature: str) -> list[str]:
    errors: list[str] = []

    settings = root / "settings.gradle.kts"
    if not settings.exists():
        errors.append("missing settings.gradle.kts")

    build_logic = root / "build-logic"
    if not build_logic.exists():
        errors.append("missing build-logic directory")

    for module in EXPECTED_MODULES:
        module_dir = root / "feature" / feature / module
        build_file = module_dir / "build.gradle.kts"
        if not build_file.exists():
            errors.append(f"missing {build_file.relative_to(root)}")

    android_app_build = root / "androidApp" / "build.gradle.kts"
    if android_app_build.exists():
        text = android_app_build.read_text(encoding="utf-8")
        if f"projects.feature.{feature}.ui" not in text:
            errors.append(
                f"androidApp/build.gradle.kts does not reference projects.feature.{feature}.ui"
            )

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a KMP feature module graph.")
    parser.add_argument("project_root", type=Path, help="Path to the KMP project root")
    parser.add_argument("feature_name", help="Feature name, e.g. auth")
    args = parser.parse_args()

    root = args.project_root.resolve()
    feature = args.feature_name
    errors = validate_module_graph(root, feature)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(f"OK: feature '{feature}' has the expected {len(EXPECTED_MODULES)} module files (6-layer: model/api/domain/data/presenter/ui)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
