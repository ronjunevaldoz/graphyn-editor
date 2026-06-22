#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


PATTERNS = [
    ("state copy race", re.compile(r"_state\.value\s*=\s*_state\.value\.copy\(")),
    ("sharedflow replay effect", re.compile(r"MutableSharedFlow<.*replay\s*=\s*1")),
    ("network result in ui", re.compile(r"NetworkResult<")),
    ("data import in ui", re.compile(r"import .*\.data\.")),
    ("manual screen capture", re.compile(
        r"playwright|adb\s+screencap|xcrun\s+simctl\s+io|Robot\(\)\.createScreenCapture|ProcessBuilder.*screenshot",
        re.IGNORECASE,
    )),
    ("magic color literal", re.compile(r"\bColor\(0x[0-9A-Fa-f]")),
    ("system dark theme scatter", re.compile(r"\bisSystemInDarkTheme\(\)")),
    ("hardcoded spacing", re.compile(r"\bpadding\([^)]*[1-9]\d*\.dp")),
]


def iter_files(root: Path):
    for path in root.rglob("*"):
        if path.is_file() and path.suffix in {".kt", ".kts", ".md"}:
            yield path


def audit_project(root: Path) -> list[str]:
    findings: list[str] = []

    for path in iter_files(root):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for label, pattern in PATTERNS:
            if label == "network result in ui" and not any(
                token in path.as_posix() for token in ("/ui/", "/presentation/")
            ):
                continue
            if label == "data import in ui" and not any(
                token in path.as_posix() for token in ("/ui/", "/presentation/")
            ):
                continue
            if label == "magic color literal":
                if not any(token in path.as_posix() for token in ("/ui/", "/presentation/")):
                    continue
                if any(part in path.stem for part in ("Color", "Token", "Theme", "color", "token", "theme")):
                    continue
            if label == "system dark theme scatter" and any(
                part in path.stem for part in ("Theme", "theme", "App")
            ):
                continue
            if label == "hardcoded spacing":
                if not any(token in path.as_posix() for token in ("/ui/", "/presentation/")):
                    continue
                if any(part in path.stem for part in ("Spacing", "spacing", "Token", "token", "Theme", "theme")):
                    continue
            if pattern.search(text):
                findings.append(f"{label}: {path.relative_to(root)}")

    return findings


def main() -> int:
    parser = argparse.ArgumentParser(description="Run a lightweight KMP architecture audit.")
    parser.add_argument("project_root", type=Path, help="Path to the KMP project root")
    args = parser.parse_args()

    root = args.project_root.resolve()
    findings = audit_project(root)

    if findings:
        print("FINDINGS:")
        for finding in findings:
            print(f"- {finding}")
        return 1

    print("OK: no lightweight architecture smells matched the current scan")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
