# Basisgewichte von Zutatenkonzepten

Stand: 17. September 2026
Status: verbindlicher Pflegemaßstab ab Issue #288

## 1. Bedeutung und Abgrenzung

`ingredient_concept.base_draw_weight` beschreibt die gewünschte relative Grundpräsenz eines eigenständigen Zutatenkonzepts gegenüber den Konzepten, mit denen es in einer konkreten gewichteten Ziehung tatsächlich konkurriert. Es ist weder eine Prozentwahrscheinlichkeit noch eine Bewertung der allgemeinen Wichtigkeit oder Qualität eines Lebensmittels.

Das Gewicht wird vor Saison, personenspezifischer Beschaffbarkeit, exaktem Cooldown und Novelty-Zielfaktor angewendet:

```text
effectiveWeight = baseDrawWeight
                × seasonFactor
                × availabilityFactor
                × exactCooldownFactor
                × noveltyTargetFactor
```

Availability, Novelty, Preis, Vorrat, persönliche Vorliebe, Herkunft, Länderzahl, Rollenanzahl und Graphposition leiten das Basisgewicht nicht automatisch ab. Insbesondere wird ein Availability- oder Novelty-Abschlag weder im Basisgewicht wiederholt noch durch ein invers erhöhtes Basisgewicht kompensiert. Die bestehende `OPEN`-/`SPECIFIC`-Mischung wird nicht ein zweites Mal über pauschale Gewichte geregelt.

## 2. Skala und Referenzanker

| Gewicht | Bedeutung | repräsentative Anker aus #288 |
|---:|---|---|
| `1.0000` | normale Grundpräsenz; eigenständig, breit integrierbar und ohne fachlichen Abschlagsgrund | `MEAT`, `VEGETABLES`, `EGGS`, `SEAFOOD` |
| `0.7500` | eigenständiges Kochprofil mit merklicher Überschneidung zu nahen Varianten | `APPLE`, `BEEF_STEAK`, `CUMIN`, `SALMON` |
| `0.5000` | klar unterscheidbare, aber enger festgelegte Produkt-, Würz- oder Verarbeitungsform | `AIOLI`, `DARK_CHOCOLATE`, `MISO`, `WIENER_SAUSAGE` |
| `0.2500` | sehr fokussierte beziehungsweise stark bindende Pflichtvorgabe mit bewusst begrenzter Präsenz | `COOKING_ALCOHOL`, `TRUFFLE`, `XO_SAUCE`, `ALIGUE` |

Die Anker erläutern die Relation und sind keine mechanischen Schablonen. Gleiche Gewichte sind bei gleicher Begründung richtig; es gibt keine Zielquote je Stufe. Weitere Werte benötigen eine ausdrücklich dokumentierte fachliche oder gemessene Kalibrierungsbegründung.

Der unabhängige niedrige Gewichtsvertrag für `COOKING_ALCOHOL` und seine konkreten Formen bleibt bestehen. Er beruht auf der engen verantwortbaren Einsatzbreite als verpflichtende Kochaufgabe, nicht auf Availability oder Novelty.

## 3. Einzel- und Familienentscheidung

Jede Bewertung beantwortet gemeinsam:

1. Wie viele sinnvoll unterschiedliche Kochaufgaben erzeugt die konkrete verpflichtende Vorgabe wiederholt?
2. Wie frei lässt sie sich kulinarisch integrieren, ohne ihre Identität zu verlieren?
3. Wie eigenständig ist sie gegenüber breiten Parents und nahen Produkt- oder Artvarianten?
4. Erzeugt die Erfassungstiefe einer engen Familie unangemessen viel gemeinsame Präsenz?

Jedes anwendbare Konzept erhält eine primäre Vergleichsgruppe und einen Anker. Diese Angaben sind reine Reviewmetadaten, keine neue produktive Ontologie. Mehrfachrollen und weitere Graphbeziehungen dienen der Ausreißerdiagnose, werden aber nicht mehrfach als Abschlag verrechnet. Weder werden alle Familien gleich gewichtet noch wird ein Parentgewicht durch die Kinderzahl geteilt. Tatsächlich unterschiedliche Kochaufgaben dürfen zusammen mehr Präsenz erhalten; bloße Erfassungstiefe genügt dafür nicht.

`random_draw_enabled = false` begründet allein kein `NOT_APPLICABLE`. Nur echte Strukturknoten ohne eigenständige Zutatenvorgabe werden so geführt; ihr technisch erforderlicher Bestandswert bleibt erhalten.

## 4. Dauerhafter Pflegeablauf

Für Neuaufnahmen und wesentliche Produktformänderungen bleiben die bestehenden fachlichen Gates aus [`INGREDIENT_CONCEPT_CURATION.md`](INGREDIENT_CONCEPT_CURATION.md) und dem Länderworkflow unverändert. Innerhalb der dortigen Metadatenentscheidung wird zusätzlich festgehalten:

- Zielgewicht nach der obigen Skala,
- primäre Vergleichsgruppe und konkreter Anker,
- kurze eigenständige Begründung zur Einsatzbreite, Eigenständigkeit und Familienbalance,
- ausdrücklich geltende Ausnahme oder offener Scope-Widerspruch.

Die Erstbewertung erfolgt möglichst ohne Sicht auf das alte Gewicht. Anschließend werden Altwert und Änderungsstatus ergänzt und Familienausreißer, historische mechanische Abschläge sowie doppelt berücksichtigte Faktoren kontrolliert. Gewichtsbegründungen gehören in den Entscheidungsnachweis, nicht in die nutzerseitige Kuratornotiz.

Eine Einpflege verwendet stabile Konzeptcodes, prüft die vorausgesetzten Altwerte und sperrt die betroffenen Zeilen in derselben Transaktion. Unbekannte Abweichungen brechen atomar ab; ein bereits identischer Zielwert ist ein geprüfter No-op. Nur tatsächlich geänderte Konzepte erhalten genau einen zusätzlichen Aggregatversionsschritt. `active`, `random_draw_enabled` und andere fachliche Felder sind weder Laufzeitgate noch Schreibziel.

Der Repository-Validator kann mit folgendem Befehl auf vollständige Export- und Entscheidungsdateien angewendet werden:

```powershell
.\mvnw.cmd -Pcatalog-draw-weights "-Dexec.args=validate --repository-root . --source <source.jsonl> --decisions <decisions.tsv>" exec:java
```

Er prüft Vollständigkeit, Eindeutigkeit, Codes, Altwerte, Skala, Referenzen, N/A-Verwendung und Änderungsstatus. `render-migration` erzeugt daraus die per Code geschützte Migration atomar; es gibt keinen dauerhaft selbstschreibenden Gewichtsjob.

## 5. Katalogweiter Referenzstand #288

Der Vollreview beruht auf `main@c6044088c3ff501261e01e36bdec915f9bfdb1d5` einschließlich des gemergten Japan-Pakets #287. Vollständige Anhänge:

- [PostgreSQL-17-Quellenexport](analysis/catalog-draw-weights-source-20260917.jsonl)
- [Quellenmanifest mit Ausgangscommit und vollständigem Input-Blobinventar](analysis/catalog-draw-weights-source-manifest-20260917.json)
- [Entscheidungstabelle mit Alt-/Zielwert, Gruppe, Anker und Einzelbegründung](analysis/catalog-draw-weights-decisions-20260917.tsv)
- [Wirkungsbericht und QA-Zusammenfassung](analysis/catalog-draw-weights-review-20260917.md)
- [Maschinenlesbarer Generatorvergleich](analysis/catalog-draw-weight-calibration-report-20260917.json)
- [Reproduzierbare Exportabfrage](analysis/catalog-draw-weights-source-export.sql)

Die markierten Widersprüche aus #278/#279 bleiben auf ihren bestehenden Identitäten und Metadaten bewertet. Der Gewichtsreview erfindet keine künftige Parentform und nimmt die dortigen Konzept-, Graph- oder Notizarbeiten nicht vorweg.
