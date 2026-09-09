#!/usr/bin/env python3
"""Fail-closed risk classification for Deployment Verify."""

from __future__ import annotations

import argparse
import re
import subprocess
from dataclasses import dataclass
from enum import IntEnum
from pathlib import Path
from typing import Callable, Sequence


SHA = re.compile(r"[0-9a-fA-F]{40}\Z")
ASSET_INDEX = "design/challenge-cards/assets/ASSET_INDEX.csv"
PRODUCTION_ASSET = re.compile(
    r"design/challenge-cards/assets/(?:ingredients|open-concepts)/[^/]+\.png\Z"
)


class Mode(IntEnum):
    SKIP = 0
    SMOKE = 1
    FULL = 2

    def __str__(self) -> str:
        return self.name.lower()


@dataclass(frozen=True)
class Change:
    status: str
    paths: tuple[str, ...]


@dataclass(frozen=True)
class Classification:
    mode: Mode
    reason: str


def is_production_asset(path: str) -> bool:
    return bool(PRODUCTION_ASSET.fullmatch(path))


def path_mode(path: str) -> Mode:
    """Classify one unambiguously changed path.

    The allowlists are intentionally narrow. Anything not proven to be a
    documentation, test, development-only, application, or build path takes
    the conservative full lifecycle.
    """

    if (
        path == "Dockerfile"
        or path == ".dockerignore"
        or path == ".github/workflows/deployment-verify.yml"
        or path.startswith(".github/scripts/")
        or path.startswith("deploy/")
    ):
        return Mode.FULL

    if (
        path == "pom.xml"
        or path in {"mvnw", "mvnw.cmd"}
        or path.startswith(".mvn/")
        or path.startswith("src/main/")
    ):
        return Mode.SMOKE

    if (
        path in {
            ".editorconfig",
            ".gitattributes",
            ".gitignore",
            "AGENTS.md",
            "LICENSE",
            "README.md",
        }
        or ("/" not in path and path.endswith(".md"))
        or path.startswith("docs/")
        or path.startswith("src/test/")
        or path == ASSET_INDEX
        or is_production_asset(path)
    ):
        return Mode.SKIP

    return Mode.FULL


def classify_changes(changes: Sequence[Change]) -> Classification:
    if not changes:
        return Classification(Mode.FULL, "no changed paths; using the full lifecycle")

    mode = Mode.SKIP
    dominant_path = ""
    index_changed = False
    production_asset_changed = False

    for change in changes:
        if change.status not in {"A", "C", "D", "M", "R"}:
            return Classification(
                Mode.FULL,
                f"unsupported diff status {change.status!r}; using the full lifecycle",
            )
        expected_paths = 2 if change.status in {"C", "R"} else 1
        if len(change.paths) != expected_paths:
            return Classification(
                Mode.FULL,
                "ambiguous diff path record; using the full lifecycle",
            )

        for path in change.paths:
            candidate = path_mode(path)
            if candidate > mode:
                mode = candidate
                dominant_path = path
            if path == ASSET_INDEX:
                index_changed = True
            elif is_production_asset(path):
                production_asset_changed = True

        # Preserve the existing narrow asset fast-path contract. Deletions,
        # renames, and copies are not safe production-asset-only changes.
        if any(path == ASSET_INDEX or is_production_asset(path) for path in change.paths):
            if change.status not in {"A", "M"}:
                return Classification(
                    Mode.FULL,
                    "unsupported production asset operation; using the full lifecycle",
                )

    if production_asset_changed and not index_changed:
        return Classification(
            Mode.FULL,
            "a production PNG changed without ASSET_INDEX.csv; using the full lifecycle",
        )

    if mode is Mode.SKIP:
        return Classification(Mode.SKIP, "only documentation, test, or approved asset paths changed")
    if mode is Mode.SMOKE:
        return Classification(Mode.SMOKE, f"application/build path {dominant_path!r} requires runtime smoke")
    return Classification(Mode.FULL, f"deployment-sensitive or unknown path {dominant_path!r} requires full lifecycle")


def parse_name_status(payload: bytes) -> list[Change]:
    tokens = payload.split(b"\0")
    if tokens and tokens[-1] == b"":
        tokens.pop()
    changes: list[Change] = []
    cursor = 0
    while cursor < len(tokens):
        status_token = tokens[cursor]
        cursor += 1
        if not status_token:
            raise ValueError("empty diff status")
        status = status_token.decode("ascii", "strict")[0]
        path_count = 2 if status in {"R", "C"} else 1
        if cursor + path_count > len(tokens):
            raise ValueError(f"truncated {status!r} diff record")
        paths = tuple(
            token.decode("utf-8", "surrogateescape")
            for token in tokens[cursor : cursor + path_count]
        )
        cursor += path_count
        if not all(paths):
            raise ValueError(f"empty path in {status!r} diff record")
        changes.append(Change(status, paths))
    return changes


def select_diff_range(
    *,
    event_name: str,
    ref: str,
    before: str,
    current_sha: str,
    pull_request_base_sha: str,
    pull_request_head_sha: str,
) -> tuple[str, str] | None:
    if event_name == "pull_request":
        candidate = (pull_request_base_sha, pull_request_head_sha)
    elif event_name == "push" and ref == "refs/heads/main":
        candidate = (before, current_sha)
    else:
        return None
    if not all(SHA.fullmatch(value) for value in candidate):
        return None
    if set(candidate[0]) == {"0"}:
        return None
    return candidate


GitRunner = Callable[[Sequence[str], Path], bytes]


def run_git(command: Sequence[str], repository: Path) -> bytes:
    return subprocess.run(
        command,
        cwd=repository,
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    ).stdout


def classify_git_diff(
    repository: Path, base_sha: str, head_sha: str, runner: GitRunner = run_git
) -> Classification:
    try:
        payload = runner(
            (
                "git",
                "diff",
                "--name-status",
                "-z",
                "--find-renames",
                "--no-ext-diff",
                base_sha,
                head_sha,
                "--",
            ),
            repository,
        )
        return classify_changes(parse_name_status(payload))
    except (OSError, subprocess.SubprocessError, UnicodeError, ValueError) as error:
        return Classification(
            Mode.FULL,
            f"cannot classify Git diff ({error}); using the full lifecycle",
        )


def classify_event(
    *,
    event_name: str,
    ref: str,
    before: str,
    current_sha: str,
    pull_request_base_sha: str,
    pull_request_head_sha: str,
    diagnostic_mode: str,
    repository: Path,
    runner: GitRunner = run_git,
) -> Classification:
    if event_name == "schedule":
        return Classification(Mode.FULL, "daily schedule requires the full lifecycle")

    if event_name == "workflow_dispatch":
        requested = diagnostic_mode.strip().lower()
        try:
            mode = Mode[requested.upper()]
        except KeyError:
            return Classification(
                Mode.FULL,
                "missing or invalid workflow_dispatch diagnostic mode; using the full lifecycle",
            )
        return Classification(mode, f"workflow_dispatch diagnostic mode explicitly requested {mode}")

    sha_range = select_diff_range(
        event_name=event_name,
        ref=ref,
        before=before,
        current_sha=current_sha,
        pull_request_base_sha=pull_request_base_sha,
        pull_request_head_sha=pull_request_head_sha,
    )
    if sha_range is None:
        return Classification(
            Mode.FULL,
            "unsupported or incomplete event SHA range; using the full lifecycle",
        )
    return classify_git_diff(repository, *sha_range, runner)


def write_github_output(path: Path, classification: Classification) -> None:
    reason = classification.reason.replace("\r", " ").replace("\n", " ")
    with path.open("a", encoding="utf-8") as handle:
        handle.write(f"mode={classification.mode}\n")
        handle.write(f"reason={reason}\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--event-name", required=True)
    parser.add_argument("--ref", required=True)
    parser.add_argument("--before", default="")
    parser.add_argument("--current-sha", required=True)
    parser.add_argument("--pull-request-base-sha", default="")
    parser.add_argument("--pull-request-head-sha", default="")
    parser.add_argument("--diagnostic-mode", default="")
    parser.add_argument("--repository", type=Path, default=Path.cwd())
    parser.add_argument("--github-output", type=Path)
    args = parser.parse_args()

    classification = classify_event(
        event_name=args.event_name,
        ref=args.ref,
        before=args.before,
        current_sha=args.current_sha,
        pull_request_base_sha=args.pull_request_base_sha,
        pull_request_head_sha=args.pull_request_head_sha,
        diagnostic_mode=args.diagnostic_mode,
        repository=args.repository.resolve(),
    )

    if args.github_output:
        write_github_output(args.github_output, classification)
    print(f"mode={classification.mode}")
    print(f"reason={classification.reason}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
