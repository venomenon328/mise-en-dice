from __future__ import annotations

import sys
import unittest
from pathlib import Path


TEMPLATES = Path(__file__).resolve().parents[1] / "templates"
sys.path.insert(0, str(TEMPLATES))

import render_challenge_card_from_spec as renderer


class CardSpecAssetContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.approved_assets = renderer.approved_asset_metadata()

    def test_approved_concrete_and_open_assets_are_accepted(self) -> None:
        concrete = renderer.requirement_from_json(
            {"display_name": "Tofu", "asset": "assets/ingredients/tofu.png"},
            0,
            self.approved_assets,
        )
        open_concept = renderer.requirement_from_json(
            {
                "display_name": "Blattgemüse",
                "asset": "assets/open-concepts/blattgemuese.png",
                "open_concept": True,
            },
            1,
            self.approved_assets,
        )
        self.assertFalse(concrete.open_concept)
        self.assertTrue(open_concept.open_concept)

    def test_open_concept_flag_cannot_use_a_concrete_asset(self) -> None:
        with self.assertRaisesRegex(ValueError, "open_concept does not match"):
            renderer.requirement_from_json(
                {
                    "display_name": "Tofu",
                    "asset": "assets/ingredients/tofu.png",
                    "open_concept": True,
                },
                0,
                self.approved_assets,
            )

    def test_concrete_flag_cannot_use_an_open_concept_asset(self) -> None:
        with self.assertRaisesRegex(ValueError, "open_concept does not match"):
            renderer.requirement_from_json(
                {
                    "display_name": "Blattgemüse",
                    "asset": "assets/open-concepts/blattgemuese.png",
                },
                0,
                self.approved_assets,
            )


if __name__ == "__main__":
    unittest.main()
