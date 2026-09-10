from __future__ import annotations

import sys
import unittest
from pathlib import Path


SCRIPTS = Path(__file__).resolve().parent
REPOSITORY = SCRIPTS.parents[1]
sys.path.insert(0, str(SCRIPTS))

from classify_verify_changes import (
    ALL_LANES,
    ASSET_INDEX,
    Change,
    Lanes,
    classify_changes,
    classify_event,
    classify_git_diff,
    parse_name_status,
    select_diff_range,
)


INGREDIENT = "design/challenge-cards/assets/ingredients/mayonnaise.png"


class VerifyChangeClassificationTest(unittest.TestCase):
    def assert_lanes(self, expected: Lanes, *changes: Change) -> None:
        classification = classify_changes(changes, REPOSITORY)
        self.assertEqual(expected, classification.lanes, classification.reason)

    def test_documentation_and_process_changes_need_no_maven_lane(self) -> None:
        for path in ("docs/README.md", "README.md", "AGENTS.md", ".gitignore"):
            with self.subTest(path=path):
                self.assert_lanes(Lanes(), Change("M", (path,)))

    def test_fast_test_and_known_database_independent_java_are_fast(self) -> None:
        self.assert_lanes(
            Lanes(fast=True),
            Change(
                "M",
                (
                    "src/test/java/io/github/venomenon328/miseendice/"
                    "challenge/internal/CandidateSimilarityCalculatorTest.java",
                ),
            ),
        )
        self.assert_lanes(
            Lanes(fast=True),
            Change(
                "M",
                (
                    "src/main/java/io/github/venomenon328/miseendice/"
                    "challenge/internal/CandidateSimilarityCalculator.java",
                ),
            ),
        )

    def test_postgresql_test_and_persistence_java_require_postgresql(self) -> None:
        self.assert_lanes(
            Lanes(postgresql=True),
            Change(
                "M",
                (
                    "src/test/java/io/github/venomenon328/miseendice/"
                    "catalog/internal/CatalogQueriesIntegrationTest.java",
                ),
            ),
        )
        self.assert_lanes(
            Lanes(fast=True, postgresql=True),
            Change(
                "M",
                (
                    "src/main/java/io/github/venomenon328/miseendice/"
                    "catalog/internal/JdbcCatalogQueries.java",
                ),
            ),
        )

    def test_liquibase_baseline_and_migration_test_require_database_lanes(self) -> None:
        expected = Lanes(postgresql=True, migration=True)
        self.assert_lanes(
            expected,
            Change("M", ("src/main/resources/db/changelog/db.changelog-master.yaml",)),
        )
        self.assert_lanes(
            expected,
            Change(
                "M",
                ("src/test/resources/db/changelog/db.changelog-production-baseline.yaml",),
            ),
        )
        self.assert_lanes(
            Lanes(migration=True),
            Change(
                "M",
                (
                    "src/test/java/io/github/venomenon328/miseendice/"
                    "ProductionBaselineMigrationIntegrationTest.java",
                ),
            ),
        )

    def test_build_workflow_classifier_and_grouping_changes_require_all_lanes(self) -> None:
        for path in (
            "pom.xml",
            "mvnw",
            ".github/workflows/verify.yml",
            ".github/scripts/classify_verify_changes.py",
            "src/test/java/io/github/venomenon328/miseendice/testsupport/"
            "CurrentSchemaPostgresIntegrationTest.java",
            "src/test/java/io/github/venomenon328/miseendice/testsupport/"
            "TestLaneAssignmentTest.java",
        ):
            with self.subTest(path=path):
                self.assert_lanes(ALL_LANES, Change("M", (path,)))

    def test_mixed_diff_uses_the_union_of_required_lanes(self) -> None:
        self.assert_lanes(
            Lanes(fast=True, postgresql=True, migration=True),
            Change(
                "M",
                (
                    "src/test/java/io/github/venomenon328/miseendice/"
                    "challenge/internal/CandidateSimilarityCalculatorTest.java",
                ),
            ),
            Change("M", ("src/main/resources/db/changelog/db.changelog-master.yaml",)),
        )

    def test_unknown_deleted_test_or_broken_diff_fails_closed(self) -> None:
        self.assert_lanes(ALL_LANES, Change("M", ("future/unknown.file",)))
        self.assert_lanes(
            ALL_LANES,
            Change("D", ("src/test/java/example/RemovedTest.java",)),
        )
        self.assert_lanes(ALL_LANES)
        self.assert_lanes(ALL_LANES, Change("X", ("docs/README.md",)))
        self.assert_lanes(ALL_LANES, Change("R", ("docs/old.md",)))

        def malformed_runner(command: tuple[str, ...], repository: Path) -> bytes:
            return b"R100\0docs/old.md\0"

        self.assertEqual(
            ALL_LANES,
            classify_git_diff(
                REPOSITORY, "a" * 40, "b" * 40, malformed_runner
            ).lanes,
        )

        def failing_runner(command: tuple[str, ...], repository: Path) -> bytes:
            raise OSError("Git diff unavailable")

        self.assertEqual(
            ALL_LANES,
            classify_git_diff(
                REPOSITORY, "a" * 40, "b" * 40, failing_runner
            ).lanes,
        )

    def test_narrow_challenge_card_asset_contract_is_unchanged(self) -> None:
        self.assert_lanes(
            Lanes(), Change("M", (ASSET_INDEX,)), Change("A", (INGREDIENT,))
        )
        self.assert_lanes(Lanes(), Change("M", (ASSET_INDEX,)))
        self.assert_lanes(ALL_LANES, Change("A", (INGREDIENT,)))
        self.assert_lanes(ALL_LANES, Change("D", (INGREDIENT,)))
        self.assert_lanes(
            ALL_LANES,
            Change("M", (ASSET_INDEX,)),
            Change("M", ("docs/README.md",)),
        )

    def test_pr_and_main_push_ranges_are_exact(self) -> None:
        base = "a" * 40
        head = "b" * 40
        self.assertEqual(
            (base, head),
            select_diff_range(
                event_name="pull_request",
                ref="refs/pull/226/merge",
                before="",
                current_sha="c" * 40,
                pull_request_base_sha=base,
                pull_request_head_sha=head,
            ),
        )
        self.assertEqual(
            (base, head),
            select_diff_range(
                event_name="push",
                ref="refs/heads/main",
                before=base,
                current_sha=head,
                pull_request_base_sha="",
                pull_request_head_sha="",
            ),
        )
        self.assertIsNone(
            select_diff_range(
                event_name="push",
                ref="refs/heads/main",
                before="0" * 40,
                current_sha=head,
                pull_request_base_sha="",
                pull_request_head_sha="",
            )
        )

    def test_event_classification_does_not_accept_an_untrusted_override(self) -> None:
        seen: list[tuple[str, ...]] = []

        def runner(command: tuple[str, ...], repository: Path) -> bytes:
            seen.append(tuple(command))
            return b"M\0docs/README.md\0"

        classification = classify_event(
            event_name="pull_request",
            ref="refs/pull/226/merge",
            before="",
            current_sha="c" * 40,
            pull_request_base_sha="a" * 40,
            pull_request_head_sha="b" * 40,
            repository=REPOSITORY,
            runner=runner,
        )
        self.assertEqual(Lanes(), classification.lanes)
        self.assertTrue(seen)


class VerifyWorkflowContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = (REPOSITORY / ".github/workflows/verify.yml").read_text(
            encoding="utf-8"
        )

    def test_parallel_lanes_and_stable_gate_are_wired(self) -> None:
        for job in ("classify", "fast", "postgresql", "migration", "verify"):
            self.assertIn(f"  {job}:\n", self.workflow)
        self.assertIn("if: ${{ always() }}", self.workflow)
        self.assertIn("needs: [classify, fast, postgresql, migration]", self.workflow)

    def test_each_lane_uses_its_explicit_maven_profile(self) -> None:
        for profile in ("verify-fast", "verify-postgresql", "verify-migration"):
            self.assertIn(f"./mvnw -P{profile} -DforkCount=2 clean verify", self.workflow)

    def test_challenge_card_tooling_remains_in_preflight(self) -> None:
        self.assertIn("python -m unittest discover design/challenge-cards/tools", self.workflow)
        self.assertIn("validate_asset_catalog.py", self.workflow)


if __name__ == "__main__":
    unittest.main()
