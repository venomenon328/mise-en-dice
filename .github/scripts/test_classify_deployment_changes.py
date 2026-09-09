from __future__ import annotations

import sys
import unittest
from pathlib import Path


SCRIPTS = Path(__file__).resolve().parent
REPOSITORY = SCRIPTS.parents[1]
sys.path.insert(0, str(SCRIPTS))

from classify_deployment_changes import (
    ASSET_INDEX,
    Change,
    Classification,
    Mode,
    classify_changes,
    classify_event,
    classify_git_diff,
    parse_name_status,
    select_diff_range,
)


INGREDIENT = "design/challenge-cards/assets/ingredients/mayonnaise.png"


class DeploymentChangeClassificationTest(unittest.TestCase):
    def assert_mode(self, expected: Mode, *changes: Change) -> Classification:
        classification = classify_changes(changes)
        self.assertEqual(expected, classification.mode, classification.reason)
        return classification

    def test_documentation_process_and_test_only_changes_skip(self) -> None:
        for path in (
            "docs/DEPLOYMENT.md",
            "README.md",
            "CONTRIBUTING.md",
            "AGENTS.md",
            "src/test/java/example/Test.java",
            "design/challenge-cards/tools/test_validate_asset_catalog.py",
            ".gitignore",
        ):
            with self.subTest(path=path):
                self.assert_mode(Mode.SKIP, Change("M", (path,)))

    def test_application_build_runtime_and_liquibase_changes_smoke(self) -> None:
        for path in (
            "src/main/java/example/App.java",
            "src/main/resources/application.yml",
            "src/main/resources/db/changelog/db.changelog-master.yaml",
            "pom.xml",
            "mvnw",
            ".mvn/wrapper/maven-wrapper.properties",
        ):
            with self.subTest(path=path):
                self.assert_mode(Mode.SMOKE, Change("M", (path,)))

    def test_deployment_workflow_classifier_and_unknown_paths_are_full(self) -> None:
        for path in (
            "deploy/tests/runtime-test.sh",
            "Dockerfile",
            ".dockerignore",
            ".github/workflows/deployment-verify.yml",
            ".github/scripts/classify_deployment_changes.py",
            ".github/scripts/test_classify_deployment_changes.py",
            "design/challenge-cards/tools/validate_asset_catalog.py",
            "design/challenge-cards/templates/challenge-card-master-4.svg",
            ".github/workflows/unknown-future-workflow.yml",
        ):
            with self.subTest(path=path):
                self.assert_mode(Mode.FULL, Change("M", (path,)))

    def test_mixed_changes_use_highest_mode(self) -> None:
        self.assert_mode(
            Mode.SMOKE,
            Change("M", ("docs/README.md",)),
            Change("M", ("src/main/java/example/App.java",)),
        )
        self.assert_mode(
            Mode.FULL,
            Change("M", ("src/main/java/example/App.java",)),
            Change("M", ("deploy/lib.sh",)),
        )

    def test_narrow_production_asset_contract_is_preserved(self) -> None:
        self.assert_mode(
            Mode.SKIP,
            Change("M", (ASSET_INDEX,)),
            Change("A", (INGREDIENT,)),
        )
        self.assert_mode(Mode.SKIP, Change("M", (ASSET_INDEX,)))
        self.assert_mode(Mode.FULL, Change("A", (INGREDIENT,)))
        self.assert_mode(Mode.FULL, Change("D", (INGREDIENT,)))

    def test_renames_escalate_across_path_classes(self) -> None:
        changes = parse_name_status(
            b"R100\0docs/old.md\0src/main/resources/new.yml\0"
        )
        self.assert_mode(Mode.SMOKE, *changes)

    def test_empty_ambiguous_or_unsupported_diffs_fail_closed(self) -> None:
        self.assert_mode(Mode.FULL)
        self.assert_mode(Mode.FULL, Change("X", ("docs/README.md",)))
        self.assert_mode(Mode.FULL, Change("R", ("docs/README.md",)))

        def malformed_runner(command: tuple[str, ...], repository: Path) -> bytes:
            return b"R100\0docs/old.md\0"

        self.assertEqual(
            Mode.FULL,
            classify_git_diff(REPOSITORY, "a" * 40, "b" * 40, malformed_runner).mode,
        )

        def failing_runner(command: tuple[str, ...], repository: Path) -> bytes:
            raise OSError("Git diff is unavailable")

        self.assertEqual(
            Mode.FULL,
            classify_git_diff(REPOSITORY, "a" * 40, "b" * 40, failing_runner).mode,
        )

    def test_pr_and_main_push_ranges_are_exact(self) -> None:
        base = "a" * 40
        head = "b" * 40
        self.assertEqual(
            (base, head),
            select_diff_range(
                event_name="pull_request",
                ref="refs/pull/227/merge",
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

    def test_schedule_forces_full_and_only_dispatch_accepts_diagnostics(self) -> None:
        common = dict(
            ref="refs/heads/main",
            before="a" * 40,
            current_sha="b" * 40,
            pull_request_base_sha="a" * 40,
            pull_request_head_sha="b" * 40,
            repository=REPOSITORY,
        )
        self.assertEqual(
            Mode.FULL,
            classify_event(event_name="schedule", diagnostic_mode="skip", **common).mode,
        )
        for diagnostic, expected in (
            ("skip", Mode.SKIP),
            ("smoke", Mode.SMOKE),
            ("full", Mode.FULL),
            ("unexpected", Mode.FULL),
            ("", Mode.FULL),
        ):
            with self.subTest(diagnostic=diagnostic):
                self.assertEqual(
                    expected,
                    classify_event(
                        event_name="workflow_dispatch",
                        diagnostic_mode=diagnostic,
                        **common,
                    ).mode,
                )

        seen: list[tuple[str, ...]] = []

        def pull_request_runner(command: tuple[str, ...], repository: Path) -> bytes:
            seen.append(tuple(command))
            return b"M\0docs/README.md\0"

        classification = classify_event(
            event_name="pull_request",
            diagnostic_mode="full",
            runner=pull_request_runner,
            **common,
        )
        self.assertEqual(Mode.SKIP, classification.mode)
        self.assertTrue(seen)


class DeploymentWorkflowContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = (REPOSITORY / ".github/workflows/deployment-verify.yml").read_text(
            encoding="utf-8"
        )

    def test_daily_schedule_and_explicit_dispatch_modes_are_wired(self) -> None:
        self.assertIn("schedule:", self.workflow)
        self.assertIn("cron: '", self.workflow)
        self.assertIn("workflow_dispatch:", self.workflow)
        self.assertIn("diagnostic_mode:", self.workflow)
        for mode in ("skip", "smoke", "full"):
            self.assertIn(f"- {mode}", self.workflow)

    def test_dispatch_override_is_guarded_by_event_name(self) -> None:
        self.assertIn(
            "DIAGNOSTIC_MODE: ${{ github.event_name == 'workflow_dispatch' && inputs.diagnostic_mode || '' }}",
            self.workflow,
        )

    def test_legacy_preview_is_pull_request_full_only(self) -> None:
        self.assertIn(
            "github.event_name == 'pull_request' && needs.classify.outputs.mode == 'full'",
            self.workflow,
        )


if __name__ == "__main__":
    unittest.main()
