#!/usr/bin/env python3
"""Fail-closed lane classification for the Verify workflow."""

from __future__ import annotations

import argparse
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Sequence


SHA = re.compile(r"[0-9a-fA-F]{40}\Z")
ASSET_INDEX = "design/challenge-cards/assets/ASSET_INDEX.csv"
PRODUCTION_ASSET = re.compile(
    r"design/challenge-cards/assets/(?:ingredients|open-concepts)/[^/]+\.png\Z"
)
CHALLENGE_CARD_TOOL_TEST = re.compile(
    r"design/challenge-cards/tools/test_[^/]+\.py\Z"
)
REGULAR_JAVA_TEST = re.compile(r"(?:Test|Tests|TestCase)\.java\Z")
TEST_METHOD = re.compile(
    r"@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b"
)
PURE_JAVA = re.compile(
    r"src/main/java/io/github/venomenon328/miseendice/(?:"
    r"challenge/internal/(?:CandidateSimilarityCalculator|CanonicalSetFingerprint|"
    r"GenerationPlanProjector|ProfileMatcher|SeedDerivation|SplitMix64|VotingRoundEvaluator)"
    r"|discord/internal/(?:Discord(?:Challenge|IngredientLookup)Renderer|"
    r"Discord(?:Ingredient)?ComponentId|DiscordProperties))\.java\Z"
)
GROUPING_DECLARATIONS = {
    "src/test/java/io/github/venomenon328/miseendice/testsupport/CurrentSchemaPostgresIntegrationTest.java",
    "src/test/java/io/github/venomenon328/miseendice/testsupport/PostgreSqlTestServer.java",
    "src/test/java/io/github/venomenon328/miseendice/testsupport/TestLaneAssignmentTest.java",
}


@dataclass(frozen=True)
class Change:
    status: str
    paths: tuple[str, ...]


@dataclass(frozen=True)
class Lanes:
    fast: bool = False
    postgresql: bool = False
    migration: bool = False

    def union(self, other: "Lanes") -> "Lanes":
        return Lanes(
            self.fast or other.fast,
            self.postgresql or other.postgresql,
            self.migration or other.migration,
        )

    def names(self) -> str:
        selected = [
            name
            for name, enabled in (
                ("fast", self.fast),
                ("postgresql", self.postgresql),
                ("migration", self.migration),
            )
            if enabled
        ]
        return ", ".join(selected) if selected else "none"


ALL_LANES = Lanes(True, True, True)


@dataclass(frozen=True)
class Classification:
    lanes: Lanes
    reason: str


def is_production_asset(path: str) -> bool:
    return bool(PRODUCTION_ASSET.fullmatch(path))


def is_approved_asset_only(changes: Sequence[Change]) -> bool:
    index_changed = False
    production_asset_changed = False
    for change in changes:
        if change.status not in {"A", "M"} or len(change.paths) != 1:
            return False
        path = change.paths[0]
        if path == ASSET_INDEX:
            index_changed = True
        elif is_production_asset(path):
            production_asset_changed = True
        else:
            return False
    return bool(changes) and (not production_asset_changed or index_changed)


def classify_java_test(path: str, repository: Path) -> Lanes:
    if path in GROUPING_DECLARATIONS:
        return ALL_LANES

    source_path = repository / path
    try:
        source = source_path.read_text(encoding="utf-8")
    except (OSError, UnicodeError):
        return ALL_LANES

    if '@Tag("migration")' in source:
        return Lanes(migration=True)
    if (
        '@Tag("postgresql")' in source
        or "extends CurrentSchemaPostgresIntegrationTest" in source
    ):
        return Lanes(postgresql=True)
    if REGULAR_JAVA_TEST.search(path) and TEST_METHOD.search(source):
        return Lanes(fast=True)
    return ALL_LANES


def path_lanes(path: str, repository: Path) -> Lanes:
    if (
        path == "pom.xml"
        or path in {"mvnw", "mvnw.cmd"}
        or path.startswith(".mvn/")
        or path == ".github/workflows/verify.yml"
        or path.startswith(".github/scripts/")
        or path in GROUPING_DECLARATIONS
    ):
        return ALL_LANES

    if path.startswith("src/main/resources/db/changelog/"):
        return Lanes(postgresql=True, migration=True)
    if path.startswith("src/test/resources/db/changelog/"):
        return Lanes(postgresql=True, migration=True)
    if path.startswith("src/test/java/"):
        return classify_java_test(path, repository)
    if path.startswith("src/test/"):
        return ALL_LANES

    if path.startswith("src/main/java/"):
        if PURE_JAVA.fullmatch(path):
            return Lanes(fast=True)
        return Lanes(fast=True, postgresql=True)
    if path.startswith("src/main/resources/templates/") or path.startswith(
        "src/main/resources/static/"
    ):
        return Lanes(fast=True)
    if path == "src/main/resources/application.yml":
        return Lanes(fast=True, postgresql=True)
    if path.startswith("src/main/"):
        return ALL_LANES

    if CHALLENGE_CARD_TOOL_TEST.fullmatch(path):
        return Lanes()
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
    ):
        return Lanes()

    return ALL_LANES


def classify_changes(changes: Sequence[Change], repository: Path) -> Classification:
    if not changes:
        return Classification(ALL_LANES, "no changed paths; running all lanes")

    for change in changes:
        if change.status not in {"A", "C", "D", "M", "R"}:
            return Classification(
                ALL_LANES,
                f"unsupported diff status {change.status!r}; running all lanes",
            )
        expected_paths = 2 if change.status in {"C", "R"} else 1
        if len(change.paths) != expected_paths or not all(change.paths):
            return Classification(ALL_LANES, "ambiguous diff record; running all lanes")

    asset_paths_present = any(
        path == ASSET_INDEX or is_production_asset(path)
        for change in changes
        for path in change.paths
    )
    if asset_paths_present:
        if is_approved_asset_only(changes):
            return Classification(
                Lanes(),
                "approved Challenge-Card production asset-only change; Maven lanes are not applicable",
            )
        return Classification(
            ALL_LANES,
            "production asset diff is outside the narrow asset-only contract; running all lanes",
        )

    lanes = Lanes()
    for change in changes:
        for path in change.paths:
            lanes = lanes.union(path_lanes(path, repository))
    return Classification(lanes, f"changed paths require lanes: {lanes.names()}")


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
        return classify_changes(parse_name_status(payload), repository)
    except (OSError, subprocess.SubprocessError, UnicodeError, ValueError) as error:
        return Classification(
            ALL_LANES,
            f"cannot classify Git diff ({error}); running all lanes",
        )


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
            ALL_LANES,
            "unsupported or incomplete event SHA range; running all lanes",
        )
    return classify_git_diff(repository, *sha_range, runner)


def write_github_output(path: Path, classification: Classification) -> None:
    reason = classification.reason.replace("\r", " ").replace("\n", " ")
    with path.open("a", encoding="utf-8") as handle:
        handle.write(f"fast={'true' if classification.lanes.fast else 'false'}\n")
        handle.write(
            f"postgresql={'true' if classification.lanes.postgresql else 'false'}\n"
        )
        handle.write(
            f"migration={'true' if classification.lanes.migration else 'false'}\n"
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
    print(f"fast={'true' if classification.lanes.fast else 'false'}")
    print(f"postgresql={'true' if classification.lanes.postgresql else 'false'}")
    print(f"migration={'true' if classification.lanes.migration else 'false'}")
    print(f"reason={classification.reason}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
