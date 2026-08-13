from pathlib import Path
import hashlib
import subprocess

ROOT = Path.cwd()


def replace_exact(path, old, new, expected=1):
    p = ROOT / path
    text = p.read_text()
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{path}: expected {expected} occurrence(s), got {count}: {old!r}")
    p.write_text(text.replace(old, new))


def run(*args):
    print('+', ' '.join(args), flush=True)
    subprocess.run(args, check=True)


# --- Catalog semantics found during review ---
migration = 'src/main/resources/db/changelog/catalog/016-final-catalog-snapshot.sql'
replace_exact(
    migration,
    "    ('BAY_LEAF', 'Lorbeerblatt', true, true, 'SPECIFIC', 0.6000, 1, null),",
    "    ('BAY_LEAF', 'Lorbeerblatt', true, false, 'SPECIFIC', 0.6000, 1,\n"
    "        'Bewusst nicht zufällig ziehbares Würzblatt; bleibt als bekannte manuelle Vorgabe und Katalogwissen erhalten.'),"
)
replace_exact(
    migration,
    "    ('BAGOONG', 'BAGOONG_ALAMANG'),",
    "    ('FERMENTED_SEASONINGS', 'BAGOONG'),\n"
    "    ('READY_SAUCES_AND_PASTES', 'BAGOONG'),\n"
    "    ('BAGOONG', 'BAGOONG_ALAMANG'),"
)
replace_exact(
    migration,
    "    ('SAUCES_AND_PASTES', 'READY_SAUCES_AND_PASTES'),",
    "    ('SAUCES_AND_PASTES', 'READY_SAUCES_AND_PASTES'),\n"
    "    ('READY_SAUCES_AND_PASTES', 'ALIGUE'),"
)
replace_exact(
    migration,
    "    ('NO_NUTS', 'ALMOND_DRINK', false),",
    "    ('NO_DAIRY', 'MILK_CHOCOLATE', false),\n"
    "    ('NO_DAIRY', 'WHITE_CHOCOLATE', false),\n"
    "    ('NO_NUTS', 'ALMOND_DRINK', false),"
)

# --- Structural regression expectations ---
baseline = 'src/test/java/io/github/venomenon328/miseendice/CatalogBaselineConsistencyTest.java'
replace_exact(baseline, '            "ALIGUE",\n            "BAGOONG",\n', '')
replace_exact(baseline, 'isEqualTo(652);', 'isEqualTo(651);')
replace_exact(baseline, 'isEqualTo(590);', 'isEqualTo(589);')
replace_exact(baseline, 'isEqualTo(777);', 'isEqualTo(780);')
replace_exact(baseline, ').containsExactly("ALIGUE", "COFFEE");', ').containsExactly("COFFEE");')

# --- Human-readable baseline documentation ---
initial = 'docs/INITIAL_CATALOG.md'
replace_exact(initial, '- davon **652 zufällig ziehbar**,', '- davon **651 zufällig ziehbar**,')
replace_exact(initial, '- **590 spezifische** ziehbare Vorgaben,', '- **589 spezifische** ziehbare Vorgaben,')
replace_exact(
    initial,
    '- **46 nicht ziehbare Konzepte**, darunter vier reine Strukturknoten und 42 bewusst deaktivierte breite oder sehr gewöhnliche Vorgaben,',
    '- **47 nicht ziehbare Konzepte**, darunter vier reine Strukturknoten, 42 bewusst deaktivierte breite oder sehr gewöhnliche Vorgaben und das bewusst nicht zufällig ziehbare Lorbeerblatt,'
)
replace_exact(initial, '- **777 bekannte Konkretisierungsbeziehungen**,', '- **780 bekannte Konkretisierungsbeziehungen**,')
replace_exact(
    initial,
    'Nach der Finalisierung besitzt der aktive Katalog **28 Root-Konzepte**. Davon sind `Aligue` und `Kaffee` spezifische Vorgaben; alle übrigen Wurzeln sind bewusst breite Familien oder fachlich eigenständige offene Konzepte.',
    'Nach der Finalisierung besitzt der aktive Katalog **26 Root-Konzepte**. `Kaffee` bleibt das einzige spezifische Root-Konzept; alle übrigen Wurzeln sind bewusst breite Familien oder fachlich eigenständige offene Konzepte. `Bagoong` bleibt unter den fermentierten Würzzutaten und zugleich unter der engen Familie fertiger Saucen und Pasten eingeordnet; `Aligue` ist dort ebenfalls als Würzpaste verankert.'
)

readiness = 'docs/CANDIDATE_GENERATOR_DATA_READINESS.md'
for old, new, expected in [
    ('| aktive Ziehkandidaten | 652 |', '| aktive Ziehkandidaten | 651 |', 1),
    ('| `SPECIFIC` | 590 |', '| `SPECIFIC` | 589 |', 1),
    ('| mit Neuigkeitsstufe | 652 |', '| mit Neuigkeitsstufe | 651 |', 1),
    ('Alle 652 Ziehkandidaten', 'Alle 651 Ziehkandidaten', 1),
    ('| `AROMATIC` | 102 | 95 | 7 |', '| `AROMATIC` | 101 | 94 | 7 |', 1),
    ('| `SEASONING` | 229 | 210 | 19 |', '| `SEASONING` | 228 | 209 | 19 |', 1),
    ('| `AROMATIC+SEASONING` | 55 |', '| `AROMATIC+SEASONING` | 54 |', 1),
    ('497 Konzepte besitzen mindestens eine der breit als strukturell oder unterstützend gemessenen Rollen einschließlich `FAT`; 155 sind ausschließlich geschmacksgebend.',
     '497 Konzepte besitzen mindestens eine der breit als strukturell oder unterstützend gemessenen Rollen einschließlich `FAT`; 154 sind ausschließlich geschmacksgebend.', 1),
    ('Für 652 Ziehkandidaten und zwei aktive Teilnehmer existieren vollständig 1.304 Zuordnungen.',
     'Für 651 Ziehkandidaten und zwei aktive Teilnehmer existieren vollständig 1.302 Zuordnungen.', 1),
    ('| `EASY` | 465 |', '| `EASY` | 464 |', 1),
    ('| `EASY` | 437 |', '| `EASY` | 436 |', 2),
    ('| 1 | 282 | 246 | 36 |', '| 1 | 281 | 245 | 36 |', 1),
    ('| `ACIDITY` | 93 | 559 | 14,3 % |', '| `ACIDITY` | 93 | 558 | 14,3 % |', 1),
    ('| `BITTERNESS` | 76 | 576 | 11,7 % |', '| `BITTERNESS` | 75 | 576 | 11,5 % |', 1),
    ('| `DOMINANCE` | 596 | 56 | 91,4 % |', '| `DOMINANCE` | 595 | 56 | 91,4 % |', 1),
    ('| `FATTINESS` | 150 | 502 | 23,0 % |', '| `FATTINESS` | 150 | 501 | 23,0 % |', 1),
    ('| `HEAT` | 54 | 598 | 8,3 % |', '| `HEAT` | 54 | 597 | 8,3 % |', 1),
    ('| `SALTINESS` | 60 | 592 | 9,2 % |', '| `SALTINESS` | 60 | 591 | 9,2 % |', 1),
    ('| `SWEETNESS` | 201 | 451 | 30,8 % |', '| `SWEETNESS` | 201 | 450 | 30,9 % |', 1),
    ('| `UMAMI` | 256 | 396 | 39,3 % |', '| `UMAMI` | 255 | 396 | 39,2 % |', 1),
    ('`DOMINANCE` ist für alle 590 spezifischen Ziehkandidaten', '`DOMINANCE` ist für alle 589 spezifischen Ziehkandidaten', 1),
    ('| `AROMATIC` | 99 | 102 |', '| `AROMATIC` | 98 | 101 |', 1),
    ('| `SEASONING` | 216 | 229 |', '| `SEASONING` | 215 | 228 |', 1),
    ('| 3 | 221 |', '| 3 | 220 |', 1),
    ('| `DRIED` | 44 |', '| `DRIED` | 43 |', 1),
    ('| direkte Kanten | 777 |', '| direkte Kanten | 780 |', 1),
    ('| Konzepte mit mehreren direkten Eltern | 102 |', '| Konzepte mit mehreren direkten Eltern | 103 |', 1),
    ('| aktive Wurzeln | 28 |', '| aktive Wurzeln | 26 |', 1),
    ('| verbundene Ziehkandidaten | 650 von 652 |', '| verbundene Ziehkandidaten | 650 von 651 |', 1),
    ('| isolierte Ziehkandidaten | 2 von 652 |', '| isolierte Ziehkandidaten | 1 von 651 |', 1),
    ('Die beiden isolierten Vorgaben sind kein Blocker.', 'Die einzelne isolierte Vorgabe `Kaffee` ist kein Blocker.', 1),
]:
    replace_exact(readiness, old, new, expected)

# --- Generate the canonical target from real PostgreSQL before hard-coding its SHA ---
final_test = ROOT / 'src/test/java/io/github/venomenon328/miseendice/FinalCatalogSnapshotIntegrationTest.java'
original_test = final_test.read_text()
needle = '            List<String> expectedLines = snapshotLines();\n'
if original_test.count(needle) != 1:
    raise SystemExit('FinalCatalogSnapshotIntegrationTest: snapshot insertion point not unique')
write_block = '''            if (Boolean.getBoolean("finalCatalogSnapshot.write")) {\n                List<String> generatedLines = canonicalLines(fresh);\n                Path output = Path.of("target", "final-catalog-snapshot-20260813.txt");\n                Files.createDirectories(output.getParent());\n                Files.write(output, generatedLines, UTF_8);\n                Files.writeString(Path.of("target", "final-catalog-fingerprint.txt"),\n                        fingerprint(fresh) + System.lineSeparator(), UTF_8);\n                return;\n            }\n\n'''
final_test.write_text(original_test.replace(needle, write_block + needle))
run('./mvnw',
    '-Dtest=FinalCatalogSnapshotIntegrationTest#freshBaselineUpgradeAndProductionFixtureConvergeWithoutChangingExistingIds',
    '-DfinalCatalogSnapshot.write=true',
    'test')

snapshot_generated = ROOT / 'target/final-catalog-snapshot-20260813.txt'
fingerprint_file = ROOT / 'target/final-catalog-fingerprint.txt'
if not snapshot_generated.is_file() or not fingerprint_file.is_file():
    raise SystemExit('canonical snapshot generation did not produce its outputs')
lines = snapshot_generated.read_text().splitlines()
if len(lines) != 6296:
    raise SystemExit(f'unexpected canonical line count: {len(lines)}')
fingerprint = fingerprint_file.read_text().strip()
if hashlib.sha256((snapshot_generated.read_text().rstrip('\n') + '\n').encode()).hexdigest() != fingerprint:
    raise SystemExit('generated snapshot SHA-256 does not match test fingerprint')
print(f'FINAL_CATALOG_SHA256={fingerprint}')

(ROOT / 'src/main/resources/db/catalog/final-catalog-snapshot-20260813.txt').write_text(
    snapshot_generated.read_text()
)

# Restore the normal test and make its new contract permanent.
final_test.write_text(original_test)
final_path = str(final_test.relative_to(ROOT))
replace_exact(
    final_path,
    '            "26c62af11e8b5c41bd93e29960799d2602b322d551afa8d0e1c68d81615e1a52";',
    f'            "{fingerprint}";'
)
replace_exact(final_path, 'assertThat(expectedLines).hasSize(6291);', 'assertThat(expectedLines).hasSize(6296);')
replace_exact(final_path, '.isEqualTo(652);', '.isEqualTo(651);')
replace_exact(final_path, '.isEqualTo(777);', '.isEqualTo(780);')
anchor = '            assertThat(value(connection, "select count(*) from exclusion_rule where active", Integer.class)).isEqualTo(22);\n'
extra = anchor + '''\n            assertThat(value(connection, """\n                    select count(*)\n                    from ingredient_refinement relation\n                    join ingredient_concept parent on parent.id = relation.parent_concept_id\n                    join ingredient_concept child on child.id = relation.child_concept_id\n                    where (parent.code, child.code) in (\n                        ('FERMENTED_SEASONINGS', 'BAGOONG'),\n                        ('READY_SAUCES_AND_PASTES', 'BAGOONG'),\n                        ('READY_SAUCES_AND_PASTES', 'ALIGUE')\n                    )\n                    """, Integer.class)).isEqualTo(3);\n            assertThat(value(connection, """\n                    select count(*)\n                    from ingredient_concept\n                    where code = 'BAY_LEAF' and active and not random_draw_enabled\n                    """, Integer.class)).isOne();\n'''
replace_exact(final_path, anchor, extra)
anchor = '''            assertThat(directExclusionTargets(connection, "NO_SOY_SAUCE"))\n                    .containsExactly("SOY_SAUCE:true");\n'''
replace_exact(
    final_path,
    anchor,
    anchor + '''            assertThat(directExclusionTargets(connection, "NO_DAIRY"))\n                    .contains("MILK_CHOCOLATE:false", "WHITE_CHOCOLATE:false");\n'''
)

# Keep the generated fingerprint consistent everywhere it is documented.
old_fingerprint = '26c62af11e8b5c41bd93e29960799d2602b322d551afa8d0e1c68d81615e1a52'
replace_exact(readiness, old_fingerprint, fingerprint)

contract = 'docs/analysis/final-catalog-snapshot-contract-20260813.md'
for old, new in [
    ('- normalisierte Fachzeilen: **6.291**', '- normalisierte Fachzeilen: **6.296**'),
    (f'`{old_fingerprint}`', f'`{fingerprint}`'),
    ('- ziehbare Konzepte: **652** (`590 SPECIFIC`, `62 OPEN`)', '- ziehbare Konzepte: **651** (`589 SPECIFIC`, `62 OPEN`)'),
    ('- direkte Konkretisierungen: **777**', '- direkte Konkretisierungen: **780**'),
    ('- Ausschlussregeln/-ziele: **22 / 56**', '- Ausschlussregeln/-ziele: **22 / 58**'),
    ('der Endwert umfasst 652 Ziehkandidaten', 'der Endwert umfasst 651 Ziehkandidaten'),
    ('| `ACIDITY` | 78 / 621 (12,6 %) | 93 / 652 (14,3 %) |', '| `ACIDITY` | 78 / 621 (12,6 %) | 93 / 651 (14,3 %) |'),
    ('| `BITTERNESS` | 53 / 621 (8,5 %) | 76 / 652 (11,7 %) |', '| `BITTERNESS` | 53 / 621 (8,5 %) | 75 / 651 (11,5 %) |'),
    ('| `DOMINANCE` | 349 / 621 (56,2 %) | 596 / 652 (91,4 %) |', '| `DOMINANCE` | 349 / 621 (56,2 %) | 595 / 651 (91,4 %) |'),
    ('| `FATTINESS` | 126 / 621 (20,3 %) | 150 / 652 (23,0 %) |', '| `FATTINESS` | 126 / 621 (20,3 %) | 150 / 651 (23,0 %) |'),
    ('| `HEAT` | 53 / 621 (8,5 %) | 54 / 652 (8,3 %) |', '| `HEAT` | 53 / 621 (8,5 %) | 54 / 651 (8,3 %) |'),
    ('| `SALTINESS` | nicht vorhanden | 60 / 652 (9,2 %) |', '| `SALTINESS` | nicht vorhanden | 60 / 651 (9,2 %) |'),
    ('| `SWEETNESS` | 150 / 621 (24,2 %) | 201 / 652 (30,8 %) |', '| `SWEETNESS` | 150 / 621 (24,2 %) | 201 / 651 (30,9 %) |'),
    ('| `UMAMI` | 217 / 621 (34,9 %) | 256 / 652 (39,3 %) |', '| `UMAMI` | 217 / 621 (34,9 %) | 255 / 651 (39,2 %) |'),
    ('Alle 590 spezifischen Endkandidaten', 'Alle 589 spezifischen Endkandidaten'),
    ('| `AROMATIC` | 89 / 94 | 99 / 100 |', '| `AROMATIC` | 89 / 94 | 98 / 100 |'),
    ('| `SEASONING` | 196 / 212 | 216 / 229 |', '| `SEASONING` | 196 / 212 | 215 / 229 |'),
]:
    replace_exact(contract, old, new)

run('git', 'diff', '--check')
run('./mvnw', 'clean', 'verify')

# Temporary review machinery must not remain in the final PR.
for temporary in [
    ROOT / '.github/workflows/pr55-review-fix.yml',
    ROOT / '.github/workflows/pr55-workflow-repair.yml',
    ROOT / '.github/scripts/pr55_review_fix.py',
]:
    if temporary.exists():
        temporary.unlink()

run('git', 'config', 'user.name', 'github-actions[bot]')
run('git', 'config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com')
run('git', 'add', '-A')
run('git', 'commit', '-m', 'Fix final catalog review findings')
run('git', 'push', 'origin', 'HEAD:feat/52-final-catalog-snapshot')
