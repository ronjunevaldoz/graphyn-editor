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
    "## Changelog",
)

# Design-system content checks ─────────────────────────────────────────────────

# The base skill declares exactly these 6 core components — all must be implemented.
_DS_CORE_COMPONENTS = (
    "fun AppButton",
    "fun AppBadge",
    "fun AppCard",
    "fun AppChip",
    "fun AppTextField",
    "fun AppText",
)

# AppTheme.spacing.X / AppTheme.colors.X / AppTheme.typography.X — static access anti-pattern.
# Requires the trailing property name (.lg, .primary, …) to avoid matching inline-doc backticks.
_DS_STATIC_ACCESS_RE = re.compile(r"AppTheme\.(spacing|colors|typography)\.\w+")

# override val X = N.dp — hardcoded dp in a sealed-interface layout property.
# `override val dp = N.dp` (component dimension enums like IconSize, AvatarSize) is exempt.
# Plain `val` token definitions are also exempt; only other `override val` properties are flagged.
_DS_HARDCODED_DP_RE = re.compile(r"override val (?!dp\b)\w+ = \d+\.dp\b")


def _check_design_system(skill_dir: Path, text: str, findings: list[str]) -> None:
    """Targeted content checks for the design-system skills."""
    name = skill_dir.name
    if name not in (
        "kotlin-multiplatform-design-system",
        "kotlin-multiplatform-design-system-extended",
    ):
        return

    if name == "kotlin-multiplatform-design-system":
        # All 6 declared core components must have an implementation.
        missing = [c for c in _DS_CORE_COMPONENTS if c not in text]
        if missing:
            findings.append(
                f"{name}: missing component implementation(s): "
                + ", ".join(missing)
            )

        # Must have a Component Previews section with at least one previews/ block.
        if "## Component Previews" not in text or "### `previews/" not in text:
            findings.append(
                f"{name}: missing Component Previews section — "
                "add '## Component Previews' with previews/AppXxxPreview.kt blocks"
            )

        # `enum class TextStyle` collides with Compose's own TextStyle — rename to AppTextStyle.
        if re.search(r"\benum class TextStyle\b", text):
            findings.append(
                f"{name}: 'enum class TextStyle' shadows Compose's TextStyle — "
                "rename to AppTextStyle"
            )

        # ExperimentalStylesApi usage requires a visible @OptIn note.
        if "ExperimentalStylesApi" in text and "OptIn(ExperimentalStylesApi" not in text:
            findings.append(
                f"{name}: ExperimentalStylesApi used without @OptIn — "
                "add @file:OptIn(ExperimentalStylesApi::class) or a note in Steps 5–6"
            )

    # Both skills: AppTheme.<property>.<field> is a compile error — use appTheme accessor.
    if _DS_STATIC_ACCESS_RE.search(text):
        findings.append(
            f"{name}: AppTheme.<property>.<field> static access found — "
            "use the appTheme @Composable accessor instead"
        )

    # Both skills: hardcoded dp in override val should reference AppSpacing() tokens.
    if _DS_HARDCODED_DP_RE.search(text):
        findings.append(
            f"{name}: 'override val X = N.dp' found — "
            "use AppSpacing() token reference (e.g. AppSpacing().xxl)"
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


# Subdirectories where all .md files must be kebab-case
KEBAB_DIRS = ("agents", "commands", "docs", "samples")

# Root-level .md files must be SCREAMING_CASE (all uppercase stem + optional underscores/hyphens)
_SCREAMING_RE = re.compile(r"^[A-Z][A-Z0-9_-]*$")
_KEBAB_RE = re.compile(r"^[a-z][a-z0-9-]*$")


def _check_naming_conventions(root: Path, findings: list[str]) -> None:
    # Root-level .md files must be SCREAMING_CASE
    for f in root.glob("*.md"):
        if not _SCREAMING_RE.match(f.stem):
            findings.append(
                f"naming: root-level {f.name} should be SCREAMING_CASE "
                f"(e.g. {f.stem.upper()}.md)"
            )

    # Subdirectory .md files must be kebab-case
    for subdir_name in KEBAB_DIRS:
        subdir = root / subdir_name
        if not subdir.exists():
            continue
        for f in subdir.rglob("*.md"):
            if not _KEBAB_RE.match(f.stem):
                findings.append(
                    f"naming: {f.relative_to(root)} should be kebab-case "
                    f"(e.g. {f.stem.lower().replace('_', '-')}.md)"
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

        _check_design_system(skill_dir, text, findings)

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

    _check_naming_conventions(root, findings)

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
