#!/usr/bin/env python3
"""
scan_design_violations.py — scan a KMP project's Compose source files for
design-system usage violations.

Detects:
  hardcoded_color      Color(0xFF...) or Color(r, g, b) — use appTheme.colors.*
  hardcoded_dp         Literal dp values in layout modifiers — use AppSpacing tokens
  material_theme       MaterialTheme.colors/typography/shapes — use appTheme.*
  direct_textstyle     TextStyle(...) construction — use AppTextStyle enum
  nested_container     Card { Card { or Surface { Surface { — redundant nesting

Usage:
  python3 scan_design_violations.py <project_root>
  python3 scan_design_violations.py <project_root> --json
  python3 scan_design_violations.py <project_root> --file path/to/Foo.kt

Exit codes:
  0 — no violations found
  1 — violations found
  2 — project root does not exist
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


# ── Violation patterns (line-by-line) ───────────────────────────────────────

_PATTERNS: list[tuple[str, str, str, re.Pattern]] = [
    (
        "hardcoded_color",
        "error",
        "Use appTheme.colors.* token instead of a hardcoded Color()",
        re.compile(
            r"\bColor\s*\(\s*0x[A-Fa-f0-9]{6,8}"                         # Color(0xFFRRGGBB)
            r"|\bColor\s*\(\s*\d+(?:\.\d+)?f?\s*,\s*\d+(?:\.\d+)?f?\s*,",  # Color(r, g, b)
        ),
    ),
    (
        "hardcoded_dp",
        "warning",
        "Use AppSpacing tokens (appTheme.spacing.*) instead of literal dp values",
        re.compile(
            # Modifier.padding/height/width/size/offset with a literal ≥ 2 dp
            r"(?:\.padding|\.height|\.width|\.size|\.offset)\s*\(\s*"
            r"(?:[2-9]|\d{2,})(?:\.\d+)?\.dp"
            # Spacer with literal dp
            r"|Spacer\s*\(\s*modifier\s*=\s*Modifier\s*\.\s*(?:height|width)\s*\(\s*"
            r"(?:[2-9]|\d{2,})(?:\.\d+)?\.dp",
        ),
    ),
    (
        "material_theme",
        "error",
        "Use appTheme.colors / appTheme.typography / appTheme.shapes instead of MaterialTheme.*",
        re.compile(r"\bMaterialTheme\s*\.\s*(?:colors|typography|shapes)\s*\."),
    ),
    (
        "direct_textstyle",
        "error",
        "Use AppTextStyle enum values instead of constructing TextStyle() directly",
        # Negative lookbehind so AppTextStyle( is not flagged
        re.compile(r"(?<![A-Za-z])TextStyle\s*\("),
    ),
]

# Containers where nesting is a structural smell.
# Matches both `Card(` and `Card {` (Kotlin trailing-lambda form).
_CONTAINER_OPEN_RE = re.compile(r"\b(Card|Surface)\b\s*[\({]")

# Files / directories that are design-system source — allowed to use primitives directly
_SKIP_NAME_SUFFIXES = (
    "Styles.kt", "Theme.kt", "Tokens.kt",
    "Colors.kt", "Typography.kt", "Spacing.kt", "Shapes.kt",
)
_SKIP_DIR_FRAGMENTS = {"designsystem", "design_system", "theme"}


def _should_skip(path: Path) -> bool:
    if any(path.name.endswith(s) for s in _SKIP_NAME_SUFFIXES):
        return True
    parts = {p.lower() for p in path.parts}
    return bool(parts & _SKIP_DIR_FRAGMENTS)


def _scan_patterns(path: Path, lines: list[str]) -> list[dict]:
    findings = []
    for lineno, line in enumerate(lines, start=1):
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*"):
            continue
        for vtype, severity, message, pattern in _PATTERNS:
            if pattern.search(line):
                findings.append({
                    "type": vtype,
                    "severity": severity,
                    "file": str(path),
                    "line": lineno,
                    "code": line.rstrip(),
                    "message": message,
                })
    return findings


def _scan_nested_containers(path: Path, lines: list[str]) -> list[dict]:
    """Flag nested Card/Surface using a brace-depth stack."""
    findings = []
    depth = 0
    # stack: (container_type, brace_depth_when_opened)
    stack: list[tuple[str, int]] = []

    for lineno, line in enumerate(lines, start=1):
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*"):
            continue

        # Detect container opens BEFORE counting braces on this line
        # (the opening { typically follows the container call on the same line)
        for m in _CONTAINER_OPEN_RE.finditer(line):
            ctype = m.group(1)
            open_types = [t for t, _ in stack]
            if ctype in open_types:
                findings.append({
                    "type": "nested_container",
                    "severity": "warning",
                    "file": str(path),
                    "line": lineno,
                    "code": line.rstrip(),
                    "message": (
                        f"Nested {ctype}() — remove the outer wrapper or "
                        "restructure with a flat layout"
                    ),
                })
            stack.append((ctype, depth))

        # Count braces
        for ch in line:
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                # Pop containers that have now closed
                while stack and stack[-1][1] >= depth:
                    stack.pop()

    return findings


def scan_file(path: Path) -> list[dict]:
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return []
    lines = text.splitlines()
    return _scan_patterns(path, lines) + _scan_nested_containers(path, lines)


def scan(project_root: Path, single_file: Path | None = None) -> list[dict]:
    if single_file:
        return [] if _should_skip(single_file) else scan_file(single_file)

    all_findings: list[dict] = []
    for kt_file in sorted(project_root.rglob("*.kt")):
        if _should_skip(kt_file):
            continue
        all_findings.extend(scan_file(kt_file))
    return all_findings


def _print_summary(findings: list[dict], project_root: Path) -> None:
    if not findings:
        print("✅  No design violations found.")
        return

    by_file: dict[str, list[dict]] = {}
    for f in findings:
        by_file.setdefault(f["file"], []).append(f)

    errors = sum(1 for f in findings if f["severity"] == "error")
    warnings = sum(1 for f in findings if f["severity"] == "warning")
    print(
        f"Design violations — {len(findings)} total  "
        f"({errors} error{'s' if errors != 1 else ''}, "
        f"{warnings} warning{'s' if warnings != 1 else ''})\n"
    )

    sev_icon = {"error": "❌", "warning": "⚠️ "}

    for filepath, file_findings in by_file.items():
        try:
            rel = Path(filepath).relative_to(project_root)
        except ValueError:
            rel = Path(filepath)
        print(f"  {rel}  ({len(file_findings)} issue{'s' if len(file_findings) != 1 else ''})")
        for f in file_findings:
            icon = sev_icon.get(f["severity"], "  ")
            print(f"    {icon} L{f['line']:>4}  [{f['type']}]  {f['message']}")
            print(f"              {f['code'].strip()}")
        print()


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Scan KMP Compose source files for design-system usage violations."
    )
    parser.add_argument("project_root", type=Path, help="Root of the KMP project")
    parser.add_argument("--json", action="store_true", help="Output findings as JSON array")
    parser.add_argument(
        "--file", type=Path, metavar="PATH",
        help="Scan a single file instead of the whole project",
    )
    args = parser.parse_args()

    root = args.project_root.resolve()
    if not root.exists():
        print(f"error: {root} does not exist", file=sys.stderr)
        return 2

    single = args.file.resolve() if args.file else None
    findings = scan(root, single_file=single)

    if args.json:
        print(json.dumps(findings, indent=2))
    else:
        _print_summary(findings, root)

    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
