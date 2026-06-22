#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path


def render_issue(title: str, evidence: str, recommendation: str, skill: str, kind: str) -> str:
    heading = "Question" if kind == "question" else "Issue"
    return f"""# {title}

## Type
{heading}

## Evidence
{evidence}

## Recommended follow-up
{recommendation}

## Suggested skill
{skill}

---
Suggested by {skill}
"""


def main() -> int:
    parser = argparse.ArgumentParser(description="Render a GitHub-ready issue draft from an audit finding.")
    parser.add_argument("--title", required=True, help="Issue title")
    parser.add_argument("--evidence", required=True, help="Evidence that triggered the finding")
    parser.add_argument("--recommendation", required=True, help="Suggested fix or follow-up")
    parser.add_argument("--skill", required=True, help="Skill name to attribute")
    parser.add_argument("--kind", choices=("issue", "question"), default="issue", help="Draft type")
    parser.add_argument("--output", type=Path, help="Optional file path to write")
    args = parser.parse_args()

    content = render_issue(args.title, args.evidence, args.recommendation, args.skill, args.kind)
    if args.output:
        args.output.write_text(content, encoding="utf-8")
    else:
        print(content, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
