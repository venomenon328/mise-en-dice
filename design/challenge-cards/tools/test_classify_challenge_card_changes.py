from __future__ import annotations

import sys
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

from classify_challenge_card_changes import (
    ASSET_INDEX,
    Change,
    classify_changes,
    classify_git_diff,
    parse_name_status,
    select_diff_range,
)


INGREDIENT = "design/challenge-cards/assets/ingredients/mayonnaise.png"
OPEN_CONCEPT = "design/challenge-cards/assets/open-concepts/brokkoli.png"


class ChallengeCardChangeClassificationTest(unittest.TestCase):
    def assert_asset_only(self, *changes: Change) -> None:
        classification = classify_changes(changes)
        self.assertTrue(classification.asset_only, classification.reason)
        self.assertTrue(classification.asset_validation_required)

    def assert_full_path(self, *changes: Change) -> None:
        classification = classify_changes(changes)
        self.assertFalse(classification.asset_only, classification.reason)

    def test_index_plus_added_production_png_is_asset_only(self) -> None:
        self.assert_asset_only(Change("M", (ASSET_INDEX,)), Change("A", (INGREDIENT,)))

    def test_index_plus_modified_production_png_is_asset_only(self) -> None:
        self.assert_asset_only(Change("M", (ASSET_INDEX,)), Change("M", (OPEN_CONCEPT,)))

    def test_only_the_index_is_asset_only(self) -> None:
        self.assert_asset_only(Change("M", (ASSET_INDEX,)))

    def test_readme_template_java_workflow_or_validator_change_requires_full_path(self) -> None:
        cases = (
            "design/challenge-cards/README.md",
            "design/challenge-cards/templates/challenge-card-master-4.svg",
            "src/main/java/io/github/venomenon328/miseendice/App.java",
            ".github/workflows/verify.yml",
            "design/challenge-cards/tools/validate_asset_catalog.py",
        )
        for path in cases:
            with self.subTest(path=path):
                self.assert_full_path(Change("M", (ASSET_INDEX,)), Change("M", (path,)))

    def test_deleted_or_renamed_production_png_requires_full_path(self) -> None:
        self.assert_full_path(Change("D", (INGREDIENT,)))
        renamed = parse_name_status(
            b"R100\0design/challenge-cards/assets/ingredients/mayonnaise.png\0"
            b"design/challenge-cards/assets/ingredients/aioli.png\0"
        )
        self.assertEqual("R", renamed[0].status)
        self.assert_full_path(*renamed)

    def test_production_png_without_index_change_requires_full_path(self) -> None:
        self.assert_full_path(Change("M", (INGREDIENT,)))

    def test_pr_and_push_sha_ranges_are_exact(self) -> None:
        base = "a" * 40
        head = "b" * 40
        merge_commit = "c" * 40
        self.assertEqual(
            (base, head),
            select_diff_range(
                event_name="pull_request",
                ref="refs/pull/145/merge",
                before="",
                current_sha=merge_commit,
                pull_request_base_sha=base,
                pull_request_head_sha=head,
            ),
        )
        self.assertEqual(
            (base, merge_commit),
            select_diff_range(
                event_name="push",
                ref="refs/heads/main",
                before=base,
                current_sha=merge_commit,
                pull_request_base_sha="",
                pull_request_head_sha="",
            ),
        )
        self.assertIsNone(
            select_diff_range(
                event_name="push",
                ref="refs/heads/main",
                before="0" * 40,
                current_sha=merge_commit,
                pull_request_base_sha="",
                pull_request_head_sha="",
            )
        )

    def test_git_diff_uses_the_selected_sha_pair_including_merge_commit(self) -> None:
        base = "a" * 40
        merge_commit = "c" * 40
        seen: list[tuple[str, ...]] = []

        def runner(command: tuple[str, ...], repository: Path) -> bytes:
            seen.append(tuple(command))
            return f"M\0{ASSET_INDEX}\0".encode()

        classification = classify_git_diff(Path.cwd(), base, merge_commit, runner)
        self.assertTrue(classification.asset_only, classification.reason)
        self.assertEqual(base, seen[0][-3])
        self.assertEqual(merge_commit, seen[0][-2])
        self.assertEqual("--", seen[0][-1])


if __name__ == "__main__":
    unittest.main()
