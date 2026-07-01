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
    ("livedata in viewmodel", re.compile(r"MutableLiveData|LiveData<")),
    ("direct state assignment", re.compile(r"_state\.value\s*=")),
    ("globalscope usage", re.compile(r"\bGlobalScope\b")),
    ("navcontroller in viewmodel", re.compile(r"NavController.*ViewModel|ViewModel.*NavController")),
    ("dto leak to domain", re.compile(r"import .*\.dto\.|@SerialName.*class.*UseCase")),
]

# ── Roadmap detection ─────────────────────────────────────────────────────────

def _has(root: Path, *globs: str) -> bool:
    return any(root.rglob(g) for g in globs)


def _count_files(root: Path, *globs: str) -> int:
    return sum(1 for g in globs for _ in root.rglob(g))


def _read_all(root: Path, *globs: str) -> str:
    parts = []
    for g in globs:
        for p in root.rglob(g):
            try:
                parts.append(p.read_text(encoding="utf-8", errors="ignore"))
            except OSError:
                pass
    return "\n".join(parts)


def _detect_state_mgmt(root: Path) -> str:
    kt = _read_all(root, "*.kt")
    if "MutableStateFlow" in kt and ("sealed interface Intent" in kt or "sealed class Intent" in kt):
        return "MVI (StateFlow + Contract)"
    if "MutableStateFlow" in kt:
        return "StateFlow (no MVI Contract)"
    if "MutableLiveData" in kt:
        return "LiveData (MVVM)"
    if "MutableState" in kt and "remember" in kt:
        return "Compose remember (no ViewModel)"
    return "unknown"


def _detect_modules(root: Path) -> str:
    settings = root / "settings.gradle.kts"
    if not settings.exists():
        settings = root / "settings.gradle"
    if not settings.exists():
        return "single-module (no settings.gradle found)"
    text = settings.read_text(encoding="utf-8", errors="ignore")
    feature_modules = re.findall(r'include\("[^"]*feature[^"]*"\)', text)
    if len(feature_modules) >= 4:
        return f"multi-module ({len(feature_modules)} feature modules)"
    if feature_modules:
        return f"partial split ({len(feature_modules)} feature modules)"
    return "single-module (no :feature: includes)"


def _detect_di(root: Path) -> str:
    kt = _read_all(root, "*.kt")
    if "@KoinViewModel" in kt or "koinViewModel()" in kt:
        return "Koin 4 (annotated)"
    if "koinInject" in kt or "val module = module {" in kt:
        return "Koin (manual)"
    if "@HiltViewModel" in kt or "@AndroidEntryPoint" in kt:
        return "Hilt"
    if "@Inject" in kt:
        return "Dagger / manual inject"
    return "none detected"


def _detect_tests(root: Path) -> str:
    test_files = _count_files(root, "*Test.kt", "*Spec.kt")
    if test_files == 0:
        return "none"
    if test_files < 5:
        return f"minimal ({test_files} test files)"
    return f"present ({test_files} test files)"


def _detect_detekt(root: Path) -> str:
    if _has(root, "detekt.yml", "detekt.yaml", "detekt-config.yml"):
        return "configured"
    return "missing"


def _detect_version_catalog(root: Path) -> str:
    if _has(root, "libs.versions.toml"):
        return "present"
    return "missing"


def assess_project(root: Path) -> dict:
    vm_info = _detect_viewmodel_size(root)
    return {
        "state_mgmt":       _detect_state_mgmt(root),
        "modules":          _detect_modules(root),
        "feature_split":    _detect_feature_split(root),
        "di":               _detect_di(root),
        "tests":            _detect_tests(root),
        "detekt":           _detect_detekt(root),
        "version_catalog":  _detect_version_catalog(root),
        "viewmodel_max_lines": vm_info["max_lines"],
        "large_vms":        vm_info["large_vms"],
    }


ADOPTION_PLAN = [
    # (condition_fn, priority, skill, reason, action)
    (
        lambda s: s["detekt"] == "missing",
        "HIGH",
        "kotlin-multiplatform-code-quality",
        "No Detekt gates — new violations accumulate faster than you migrate them",
        "Add detekt.yml with layer rules before touching any architecture code",
    ),
    (
        lambda s: s["version_catalog"] == "missing",
        "HIGH",
        "kotlin-multiplatform-feature-scaffold",
        "No version catalog — dependency versions drift across modules",
        "Add gradle/libs.versions.toml and migrate build files to use it",
    ),
    (
        lambda s: "LiveData" in s["state_mgmt"],
        "HIGH",
        "kotlin-multiplatform-mvi",
        "LiveData detected — migrate to StateFlow+MVI screen by screen",
        "Pick the highest-traffic screen, write tests for it, then migrate to StateFlow (Path A Step 1)",
    ),
    (
        lambda s: s["state_mgmt"] == "StateFlow (no MVI Contract)",
        "MEDIUM",
        "kotlin-multiplatform-mvi",
        "StateFlow present but no MVI Contract — effects may be using SharedFlow or callbacks",
        "Add Contract (State/Intent/Effect) to screens that have navigation side-effects",
    ),
    (
        lambda s: "single-module" in s["modules"],
        "MEDIUM",
        "kotlin-multiplatform-clean-architecture",
        "Single module — no layer isolation; UI can import data layer directly",
        "Extract :model first (zero-logic move), then :api, then :domain (see migration Path B)",
    ),
    (
        lambda s: "partial split" in s["modules"],
        "MEDIUM",
        "kotlin-multiplatform-clean-architecture",
        "Partial module split — some features separated, others still monolithic",
        "Complete the split for the highest-churn feature first",
    ),
    (
        lambda s: s["tests"] == "none",
        "MEDIUM",
        "kotlin-multiplatform-unit-testing",
        "No tests — migrating without tests risks invisible regressions",
        "Add ViewModel tests (with FakeRepository) before migrating each screen",
    ),
    (
        lambda s: "Hilt" in s["di"] or "Dagger" in s["di"],
        "LOW",
        "kotlin-multiplatform-dependency-injection",
        "Hilt/Dagger detected — not compatible with KMP non-Android targets",
        "Migrate one @Module at a time to Koin 4 (Path C); Hilt and Koin can coexist during migration",
    ),
    (
        lambda s: s["tests"] == "minimal",
        "LOW",
        "kotlin-multiplatform-unit-testing",
        "Few tests — coverage is too thin to migrate safely at speed",
        "Add tests for every ViewModel being migrated before the migration PR",
    ),
    (
        lambda s: s["viewmodel_max_lines"] >= 300,
        "HIGH",
        "kotlin-multiplatform-mvi",
        "God ViewModel detected (300+ lines) — business logic has leaked into the ViewModel",
        "Extract business operations into use cases (see 'ViewModel Size and Decomposition' in mvi skill); "
        "each handleIntent branch that touches 2+ repos belongs in a use case",
    ),
    (
        lambda s: 150 <= s["viewmodel_max_lines"] < 300,
        "MEDIUM",
        "kotlin-multiplatform-mvi",
        "Large ViewModel detected (150–299 lines) — growing toward monolithic",
        "Review handleIntent branches for inline logic that can be extracted to use cases before size crosses 300 lines",
    ),
    (
        lambda s: "no feature layer split" in s["feature_split"] and "multi-module" in s["modules"],
        "HIGH",
        "kotlin-multiplatform-clean-architecture",
        "Multi-module project but features have no :presenter / :domain / :ui layer split",
        "Apply the start-thin tier decision: each feature needs at least :ui; add :presenter when "
        "the screen has its own ViewModel; add :domain when use cases are shared or complex",
    ),
    (
        lambda s: "thin split" in s["feature_split"],
        "MEDIUM",
        "kotlin-multiplatform-clean-architecture",
        "Features have :ui modules only — no :presenter separation",
        "Promote features with complex ViewModels to medium tier (:presenter + :ui); "
        "reserve full tier for CRUD / offline-first features",
    ),
]


def build_roadmap(state: dict) -> list[dict]:
    plan = []
    for condition, priority, skill, reason, action in ADOPTION_PLAN:
        if condition(state):
            plan.append({
                "priority": priority,
                "skill":    skill,
                "reason":   reason,
                "action":   action,
            })
    plan.sort(key=lambda x: {"HIGH": 0, "MEDIUM": 1, "LOW": 2}[x["priority"]])
    return plan


def print_roadmap(root: Path, state: dict, plan: list[dict]) -> None:
    print(f"\n{'='*60}")
    print("  KMP ADOPTION ROADMAP")
    print(f"  Project: {root}")
    print(f"{'='*60}\n")

    print("Current state:")
    print(f"  State management : {state['state_mgmt']}")
    print(f"  Module structure : {state['modules']}")
    print(f"  Feature split    : {state['feature_split']}")
    print(f"  DI               : {state['di']}")
    print(f"  Tests            : {state['tests']}")
    print(f"  Detekt           : {state['detekt']}")
    print(f"  Version catalog  : {state['version_catalog']}")
    vm_max = state["viewmodel_max_lines"]
    if vm_max > 0:
        vm_label = "god ViewModel (300+)" if vm_max >= 300 else "large (150–299)"
        print(f"  Largest ViewModel: {vm_max} lines ({vm_label})")
        if state["large_vms"]:
            top = state["large_vms"][:3]
            for rel, n in top:
                print(f"    - {rel} ({n} lines)")
    else:
        print(f"  Largest ViewModel: not detected")
    print()

    if not plan:
        print("No adoption gaps detected. Project appears well-structured.")
        print("Run without --roadmap to check for implementation violations.\n")
        return

    print(f"Adoption plan ({len(plan)} items):\n")
    for i, item in enumerate(plan, 1):
        print(f"  {i}. [{item['priority']}] {item['skill']}")
        print(f"     Why:    {item['reason']}")
        print(f"     Action: {item['action']}")
        print()

    print("Run audit_project.py without --roadmap to check for implementation violations.")
    print()


# ── Standard audit ────────────────────────────────────────────────────────────

def _detect_viewmodel_size(root: Path) -> dict:
    """Return max line count and list of oversized ViewModel files."""
    large: list[tuple[Path, int]] = []
    for path in root.rglob("*ViewModel.kt"):
        try:
            lines = path.read_text(encoding="utf-8", errors="ignore").splitlines()
        except OSError:
            continue
        count = len(lines)
        if count >= 150:
            large.append((path, count))
    large.sort(key=lambda x: x[1], reverse=True)
    return {
        "max_lines": large[0][1] if large else 0,
        "large_vms": [(str(p.relative_to(root)), n) for p, n in large],
    }


def _detect_feature_split(root: Path) -> str:
    """Detect whether features follow the layer split convention."""
    settings = root / "settings.gradle.kts"
    if not settings.exists():
        settings = root / "settings.gradle"
    if not settings.exists():
        return "unknown (no settings.gradle)"
    text = settings.read_text(encoding="utf-8", errors="ignore")
    presenter = re.findall(r'include\("[^"]*:presenter[^"]*"\)', text)
    domain    = re.findall(r'include\("[^"]*:domain[^"]*"\)', text)
    ui        = re.findall(r'include\("[^"]*:ui[^"]*"\)', text)
    if presenter and domain and ui:
        return f"full split (presenter={len(presenter)}, domain={len(domain)}, ui={len(ui)})"
    if ui and not presenter:
        return f"thin split (:ui only, {len(ui)} modules — no :presenter or :domain)"
    if presenter and not domain:
        return f"medium split (:presenter+:ui, no :domain)"
    return "no feature layer split detected"


def iter_files(root: Path):
    for path in root.rglob("*"):
        if path.is_file() and path.suffix in {".kt", ".kts", ".md"}:
            yield path


def audit_project(root: Path) -> list[str]:
    findings: list[str] = []

    # ── ViewModel size check (not regex-detectable, needs line count) ──────────
    vm_info = _detect_viewmodel_size(root)
    for rel_path, line_count in vm_info["large_vms"]:
        severity = "god viewmodel" if line_count >= 300 else "large viewmodel"
        findings.append(f"{severity} ({line_count} lines): {rel_path}")

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
            if label == "dto leak to domain" and "/domain/" not in path.as_posix():
                continue
            if label == "navcontroller in viewmodel" and "/presenter/" not in path.as_posix():
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
    parser = argparse.ArgumentParser(description="KMP architecture audit and adoption roadmap.")
    parser.add_argument("project_root", type=Path, help="Path to the KMP project root")
    parser.add_argument(
        "--roadmap",
        action="store_true",
        help="Output a prioritized adoption plan instead of violation findings",
    )
    args = parser.parse_args()

    root = args.project_root.resolve()

    if args.roadmap:
        state = assess_project(root)
        plan  = build_roadmap(state)
        print_roadmap(root, state, plan)
        return 1 if plan else 0

    findings = audit_project(root)

    if findings:
        print("FINDINGS:")
        for finding in findings:
            print(f"- {finding}")
        return 1

    print("OK: no architecture violations detected")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
