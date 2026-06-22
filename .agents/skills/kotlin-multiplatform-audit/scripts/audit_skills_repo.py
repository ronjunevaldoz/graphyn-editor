#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from pathlib import Path


REQUIRED_MARKERS = (
    "## When to Use This Skill",
    "Trigger keywords:",
    "metadata:",
    "last-updated:",
)

FAST_MOVING_HINTS = (
    "agp",
    "buildkonfig",
    "compose",
    "koin",
    "ktor",
    "kotlin rpc",
    "kotlinx rpc",
    "navigation",
    "sqldelight",
    "resources",
    "graphics",
    "network",
    "database",
    "mvi",
)


def audit_skills_repo(root: Path) -> list[str]:
    findings: list[str] = []
    skills_dir = root / "skills"

    if not skills_dir.exists():
        return [f"missing skills directory: {skills_dir}"]

    for skill_dir in sorted(p for p in skills_dir.iterdir() if p.is_dir()):
        skill_file = skill_dir / "SKILL.md"
        if not skill_file.exists():
            findings.append(f"{skill_dir.name}: missing SKILL.md")
            continue

        text = skill_file.read_text(encoding="utf-8")
        missing = [marker for marker in REQUIRED_MARKERS if marker not in text]
        if missing:
            findings.append(f"{skill_dir.name}: missing markers: {', '.join(missing)}")

        if (skill_dir / "references").exists() and "Reference" not in text and "Docs to Recheck First" not in text:
            findings.append(f"{skill_dir.name}: has references/ but no references guidance in SKILL.md")

        if (skill_dir / "scripts").exists() and "Script" not in text and "scripts/" not in text:
            findings.append(f"{skill_dir.name}: has scripts/ but no script guidance in SKILL.md")

        if any(hint in text.lower() for hint in FAST_MOVING_HINTS) and re.search(
            r"latest|freshness|recheck",
            text,
            re.IGNORECASE,
        ) is None:
            findings.append(f"{skill_dir.name}: missing freshness guidance for fast-moving dependencies")

        if skill_dir.name == "kotlin-multiplatform-feature-scaffold" and "all-targets" not in text:
            findings.append(
                "kotlin-multiplatform-feature-scaffold: missing all-targets branch guidance for full-stack KMP scaffolds"
            )

        if skill_dir.name == "kotlin-multiplatform-feature-scaffold" and (
            "build-logic" not in text or "libs.versions.toml" not in text
        ):
            findings.append(
                "kotlin-multiplatform-feature-scaffold: missing build-logic and libs.versions.toml guidance"
            )

    readme = root / "README.md"
    if readme.exists():
        readme_text = readme.read_text(encoding="utf-8")
        if "Start here" not in readme_text:
            findings.append("README.md: missing start-here guidance")
        if "Roadmap" not in readme_text:
            findings.append("README.md: missing roadmap section")
    else:
        findings.append("missing README.md")

    return findings


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit the skills repo for documentation hygiene.")
    parser.add_argument("root", type=Path, help="Repo root")
    args = parser.parse_args()

    findings = audit_skills_repo(args.root.resolve())
    for finding in findings:
        print(finding)
    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
