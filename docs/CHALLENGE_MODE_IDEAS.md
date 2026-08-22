# Explorative Ideen für Challenge-Modi

Stand: 22. August 2026

## Status und Abgrenzung

Dieses Dokument ist eine unverbindliche Ideensammlung. Es ist weder eine Fachspezifikation noch eine Implementierungszusage und begründet keinen unmittelbaren Lieferumfang. Konkrete Modi erhalten erst dann ein eigenes Issue und verbindliche Dokumente, wenn ihre Regeln, ihr Ablauf und ihre Abgrenzung bewusst festgelegt wurden.

Generatorprofile werden von Challenge-Modi getrennt betrachtet:

- Ein **Challenge-Modus** verändert den Ablauf oder führt zusätzliche persistente Entscheidungen beziehungsweise geheime Eingaben ein.
- Ein **Generatorprofil** verändert nur Auswahlwahrscheinlichkeiten, Zielverteilungen oder weiche beziehungsweise harte Generatorvorgaben, ohne einen neuen fachlichen Ablauf zu erzeugen.

## Gemeinsame Leitgedanken

- Ein Modus gilt für die gesamte `challenge_session`, nicht für einzelne Kandidaten innerhalb eines Zwölfer-Satzes.
- Alle Kandidaten und ein möglicher Reroll derselben Session unterliegen demselben Modus und denselben bereits materialisierten festen Vorgaben.
- Die Standard-Challenge behält grundsätzlich vier gemeinsame Vorgaben. Ob ein späterer Modus davon ausdrücklich abweicht, muss je Modus entschieden werden.
- Spezielle Modi sollen zunächst ausdrücklich auswählbar sein. Eine spätere automatische Aktivierung wäre nur mit einer kleinen gemeinsamen Gesamtwahrscheinlichkeit für irgendeinen Spezialmodus sinnvoll, nicht mit einer eigenen hohen Wahrscheinlichkeit je Modus.
- Persistierte Zufallsentscheidungen, geheime Beiträge und daraus abgeleitete feste Vorgaben müssen nach Restart und Retry unverändert bleiben.
- Modusspezifische Kuratorregeln dürfen harte strukturelle Integrität nicht aufheben, können aber bewusst andere Qualitäts-Fallbacks verwenden als die Standard-Kuration.

## 1. Vorab-Anker

### Grundidee

Vor der normalen Kandidatengenerierung reichen die maßgeblichen Personen jeweils geheim eine Katalogzutat ein. Aus den Beiträgen werden höchstens zwei Ankerzutaten bestimmt. Erst danach erzeugt der Generator vollständige Vierer-Kandidaten und ergänzt die noch freien Positionen unter Berücksichtigung der feststehenden Anker.

### Gewünschte Eigenschaften

- Die Ankerbeiträge werden geheim und veränderbar gesammelt, bis die Sammlung abgeschlossen wird.
- Bei einer kleinen Gruppe können alle unterschiedlichen Beiträge übernommen werden, solange höchstens zwei Anker entstehen.
- Bei drei oder mehr Beiträgen werden höchstens zwei Anker nach einer später festzulegenden, persistenten Regel ausgewählt.
- Doppelte Beiträge dürfen nicht zu derselben Zutat auf zwei Vorgabenpositionen führen. Ob Übereinstimmungen ihre Auswahlwahrscheinlichkeit erhöhen, ist noch offen.
- Die materialisierten Anker sind für Generator, Kurator und Reroll unverrückbar.
- Der Generator füllt nur die verbleibenden Positionen auf und berücksichtigt die Anker bei Redundanz, Rollen, Ausschlüssen und sonstiger struktureller Gültigkeit.
- Die Herkunft einer festen Vorgabe soll als Anker erkennbar bleiben und nicht lediglich wie eine gewöhnliche manuelle Admin-Vorgabe wirken.

### Kurations-Fallback

Dieser Modus soll die Beteiligten gerade dazu zwingen, aus ihrer eigenen Ankerkombination etwas zu machen. Deshalb darf die Kuration nach Ausschöpfung der höchstens zwei erlaubten Generator-/Kuratorrunden nicht allein wegen fehlender Qualität abbrechen.

- Auch wenn sämtliche vollständig materialisierten Kandidaten als `BAD` bewertet werden, müssen die angeforderte Zahl der bestgerankten verfügbaren Kandidaten ausgewählt werden.
- Der Standardfall „kein `GOOD` vorhanden, daher Kurationserschöpfung“ gilt für diesen Modus ausdrücklich nicht.
- Ein Fehlschlag bleibt nur zulässig, wenn keine vollständigen Kandidaten materialisiert werden konnten, eine harte technische beziehungsweise strukturelle Invariante verletzt wäre oder ein technischer Fehler vorliegt.
- Ein kulinarisch sehr schlechtes Ergebnis ist in diesem Modus kein technischer oder fachlicher Fehler, sondern Teil des gewollten Risikos.

## 2. Ergänzungsanker

### Grundidee

Der Generator bestimmt zunächst nur einen Teil der gemeinsamen Vorgaben, beispielsweise zwei Zutaten. Diese werden sichtbar gemacht. Anschließend bestimmen die maßgeblichen Personen geheime Anker, welche die noch freien Positionen auffüllen. Erst die vollständigen Kombinationen werden danach kuratiert.

### Abgrenzung zum Vorab-Anker

- Beim Vorab-Anker stehen die Anker fest, bevor der Generator weitere Zutaten auswählt.
- Beim Ergänzungsanker reagieren die Personen bewusst auf bereits bekannte Zufallsvorgaben.
- Dadurch unterscheiden sich Spielgefühl, Strategie und technischer Ablauf deutlich genug, um beide Varianten als eigenständige Modi zu behandeln.

### Offene Punkte

- Ob jeder Beitrag zwingend übernommen wird oder bei größeren Gruppen ebenfalls höchstens zwei ausgewählt werden.
- Ob alle Personen dieselben bereits gezogenen Vorgaben sehen, bevor sie ihren Beitrag abgeben.
- Wie mit redundanten, identischen oder strukturell kollidierenden Beiträgen umgegangen wird.
- Ob die Kuration denselben verpflichtenden `BAD`-Fallback wie beim Vorab-Anker erhält.

## 3. Bot-Anker

### Grundidee

Der Bot bestimmt vor der normalen Kandidatengenerierung selbst eine gemeinsame feste Ankerzutat. Der Generator ergänzt die übrigen Positionen; die Ankerzutat bleibt bei Kuration und Reroll bestehen.

### Beschaffbarkeit

Die Eignung als Bot-Anker darf nicht allein aus möglicherweise zu optimistischen allgemeinen Beschaffbarkeitsdaten abgeleitet werden.

- Bot-Anker sollen aus einem ausdrücklich kuratierten, sehr zuverlässig beschaffbaren Pool stammen.
- Eine spätere Eigenschaft wie `bot_anchor_eligible` wäre dafür geeigneter als eine rein implizite Ableitung.
- Gepflegte Beschaffbarkeitswerte können zusätzlich berücksichtigt werden, besitzen aber keine erfundenen Defaults.
- Fehlende Beschaffbarkeitswerte bedeuten lediglich „nicht gepflegt“ und weder verfügbar noch unverfügbar.
- Eine Zutat wie Ube soll nicht allein deshalb Bot-Anker werden können, weil irgendein Datenwert theoretische Beschaffbarkeit behauptet, praktisch aber nur eine unvernünftige Großbestellung oder eine Fernreise möglich wäre.

## 4. Persönlicher Twist

### Grundidee

Neben den gemeinsamen Challenge-Vorgaben erhält jede Person eine eigene geheime Pflichtzutat oder einen eigenen geheimen Twist. Die persönlichen Vorgaben werden erst bei der Ergebnispräsentation offengelegt.

### Reiz

- Alle arbeiten weiterhin an derselben Challenge, erhalten aber unterschiedliche zusätzliche Probleme oder Chancen.
- Die spätere Gegenüberstellung der Lösungen wird interessanter, ohne dass die gemeinsamen Vorgaben ihre Bedeutung verlieren.
- Geheimhaltung und spätere Auflösung schaffen einen eigenen Ablauf und machen dies zu einem echten Modus statt zu einem bloßen Generatorprofil.

### Offene Punkte

- Ob der persönliche Twist zusätzlich zu den vier gemeinsamen Vorgaben gilt oder eine andere Slot-Semantik erhält.
- Ob nur Katalogzutaten oder auch andere kleine Vorgaben zulässig sind.
- Wer die persönlichen Twists bestimmt: Bot, Zufall oder andere Personen.
- Wann die Twists sichtbar werden und ob sie vor der Ergebniserfassung noch geändert werden dürfen.
- Wie stark ihre Beschaffbarkeit abgesichert werden muss.

## 5. Generatorprofile

Generatorprofile sind keine Challenge-Modi, solange sie nur die Erzeugung der Kandidaten beeinflussen und keinen zusätzlichen Lifecycle einführen.

Mögliche Profile:

- besonders vertraute beziehungsweise alltagstaugliche Zutaten,
- kontrolliert ungewöhnliche oder abenteuerliche Zutaten,
- garantiert mindestens ein bewusst schwieriger „Störenfried“,
- stärkere Gewichtung bestimmter funktionaler Rollen,
- besonders breite oder besonders kompakte Warenkorbstruktur,
- saisonaler Schwerpunkt.

Profile können später ausdrücklich auswählbar oder mit kleinen Wahrscheinlichkeiten automatisch angewandt werden. Sie müssen dennoch reproduzierbar im Generation Context persistiert werden und dürfen nicht als lose Promptanweisung ausschließlich beim Kurator landen.

Eine „offene Challenge“, die lediglich bestimmte Anteile an `OPEN`-Vorgaben erzwingt, wird derzeit nicht als eigener sinnvoller Modus verfolgt. Eine veränderte Spezifitätsverteilung wäre eher ein Generatorprofil.

## 6. Übergreifend offene Entscheidungen

Vor einer verbindlichen Modus-Spezifikation sind insbesondere noch zu klären:

- Welche Personengruppe bei geheimen Beiträgen maßgeblich ist: aktuelles Elektorat, ausdrücklich ausgewählte Beitragende oder eine andere Gruppe.
- Wie geheime Beiträge gesammelt, geändert, abgeschlossen und bei ausbleibenden Eingaben behandelt werden.
- Welche Modi explizit auswählbar sind und ob beziehungsweise wann eine automatische Moduswahl eingeführt wird.
- Wie die gemeinsame Gesamtwahrscheinlichkeit für Spezialmodi konfiguriert und versioniert wird.
- Welche Modusdaten in öffentlicher Challenge-Anzeige, Historie und Ergebnisdarstellung sichtbar werden.
- Welche Fallbacks pro Modus gelten und welche Standard-Kurationsregeln ausdrücklich überschrieben werden.
- Wie Modus, Beiträge, Auswahlentscheidungen und feste Vorgaben für Restart, Audit und Replay persistiert werden.
