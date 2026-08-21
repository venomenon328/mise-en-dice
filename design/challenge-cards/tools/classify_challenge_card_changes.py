#!/usr/bin/env python3
"""Fail closed classification for the Challenge-Card asset CI fast path."""

from __future__ import annotations

import argparse
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Sequence


ASSET_INDEX = "design/challenge-cards/assets/ASSET_INDEX.csv"
PRODUCTION_ASSET = re.compile(
    r"design/challenge-cards/assets/(?:ingredients|open-concepts)/[^/]+\.png\Z"
)
SHA = re.compile(r"[0-9a-fA-F]{40}\Z")


@dataclass(frozen=True)
class Change:
    status: str
    paths: tuple[str, ...]


@dataclass(frozen=True)
class Classification:
    asset_only: bool
    asset_validation_required: bool
    reason: str


def is_production_asset(path: str) -> bool:
    return bool(PRODUCTION_ASSET.fullmatch(path))


def asset_validation_required(changes: Sequence[Change]) -> bool:
    return any(
        path == ASSET_INDEX or is_production_asset(path)
        for change in changes
        for path in change.paths
    )


def classify_changes(changes: Sequence[Change]) -> Classification:
    validation_required = asset_validation_required(changes)
    if not changes:
        return Classification(False, True, "no changed paths; using the full path")

    index_changed = False
    production_png_changed = False
    for change in changes:
        # A/M are the only unambiguous statuses that can represent the narrow
        # asset-only contract. Deletions, renames, copies and every unknown
        # status intentionally go through the full path.
        if change.status not in {"A", "M"}:
            return Classification(
                False,
                True,
                f"unsupported diff status {change.status!r}; using the full path",
            )
        if len(change.paths) != 1:
            return Classification(
                False,
                True,
                "ambiguous diff path record; using the full path",
            )
        path = change.paths[0]
        if path == ASSET_INDEX:
            index_changed = True
        elif is_production_asset(path):
            production_png_changed = True
        else:
            return Classification(
                False,
                validation_required,
                f"non-production-asset path {path!r}; using the full path",
            )

    if production_png_changed and not index_changed:
        return Classification(
            False,
            validation_required,
            "a production PNG changed without ASSET_INDEX.csv; using the full path",
        )
    return Classification(True, validation_required, "only approved production asset additions/modifications")


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
        paths = tuple(token.decode("utf-8", "surrogateescape") for token in tokens[cursor : cursor + path_count])
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
    # GitHub uses the all-zero before SHA for a history-less push. It cannot
    # describe a safe range, so deliberately select the full path instead.
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
        return Classification(False, True, f"cannot classify Git diff ({error}); using the full path")


def classify_event(
    *,
    event_name: str,
    ref: str,
    before: str,
    current_sha: str,
    pull_request_base_sha: str,
    pull_request_head_sha: str,
    repository: Path,
    runner: GitRunner = run_git,
) -> Classification:
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
            False,
            True,
            "unsupported or incomplete event SHA range; using the full path",
        )
    return classify_git_diff(repository, *sha_range, runner)


def write_github_output(path: Path, classification: Classification) -> None:
    reason = classification.reason.replace("\r", " ").replace("\n", " ")
    with path.open("a", encoding="utf-8") as handle:
        handle.write(f"asset_only={'true' if classification.asset_only else 'false'}\n")
        handle.write(
            "asset_validation_required="
            f"{'true' if classification.asset_validation_required else 'false'}\n"
        )
        handle.write(f"reason={reason}\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--event-name", required=True)
    parser.add_argument("--ref", required=True)
    parser.add_argument("--before", default="")
    parser.add_argument("--current-sha", required=True)
    parser.add_argument("--pull-request-base-sha", default="")
    parser.add_argument("--pull-request-head-sha", default="")
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
        repository=args.repository.resolve(),
    )

    if args.github_output:
        write_github_output(args.github_output, classification)
    print(f"challenge-card-assets-only={'true' if classification.asset_only else 'false'}")
    print(f"asset-validation-required={'true' if classification.asset_validation_required else 'false'}")
    print(f"reason={classification.reason}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
