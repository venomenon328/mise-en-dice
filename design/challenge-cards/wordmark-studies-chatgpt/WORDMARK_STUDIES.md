# Expressive `Mise en Dice` wordmark studies

## Why this study exists

The earlier wordmark exploration stayed too close to ordinary typography. The utility type system remains valid, but the brand mark itself needs substantially more personality and integration than a font choice alone provides.

The supplied user reference is therefore treated only as a **difference-in-kind reference**: custom lettering, subordinate `en`, integrated die and flowing connective strokes. None of the studies copies the reference composition or exact lettering.

## Shared design rules

All three studies use:

- the exact readable wording `Mise en Dice`,
- `Mise` and `Dice` as the primary visual anchors,
- a deliberately smaller `en`,
- one integrated six-sided die,
- warm yellow-to-orange-gold surfaces,
- a dark espresso-brown outline,
- limited highlight detail instead of high-gloss casino/sticker rendering,
- transparent backgrounds,
- path-only SVG output after conversion; no `<text>` elements remain.

The existing Card geometry, Style A palette, utility typography and illustration system are not redefined by this package.

## Study A — Script Ribbon

A flowing lettering direction with a continuous lower sweep. The smaller `en` sits in the transition from `Mise` to the die, while the die acts as the visual hinge before `Dice`.

**Strengths**

- strongest overall brand character,
- immediately feels like a designed wordmark rather than typeset text,
- remains comparatively open and readable,
- long lower sweep visually holds the mark together without creating an enclosing badge.

**Risks**

- the long swash needs careful final sizing in the production header,
- final polish should reduce any remaining visual collision between the `en`, die and the leading `D`.

**First-pass assessment:** current favorite for refinement.

## Study B — Kitchen Crest

A more compact, emblematic direction. The die forms the centre of the mark and the subordinate `en` sits on a dark central banner.

**Strengths**

- strongest compact/emblem character,
- stable at small sizes,
- central composition makes the die unmistakable.

**Risks**

- less fluid and less distinctive than A,
- the centre banner can feel more like a badge or restaurant crest than a lightweight card wordmark,
- visually heavier in the already structured Challenge Card header.

**First-pass assessment:** useful counterpoint, but not the preferred direction unless compactness is prioritised.

## Study C — Refined Game Accent

The most dynamic direction and the closest to the energy of the user reference. The die is pushed directly into the `Dice` transition and the upper/lower sweeps create a stronger sense of movement.

**Strengths**

- most energetic,
- strongest visual integration of the die,
- clear custom-logo character.

**Risks**

- the leading `D` becomes busier and needs cleanup,
- higher risk of drifting toward gaming/casino language if the die, highlight ring or swooshes are pushed further,
- more sensitive to very small rendering.

**First-pass assessment:** strong second candidate if a bolder mark is desired.

## Header checks

Every study is checked in a `1200 × 222` header context derived from the approved warm-gold Style A environment and as a native `320 × 59` header rendering, corresponding to the small card export.

The small check is intentionally strict: fine detail that only survives in the isolated 1200-wide logo should not drive the final selection.

## Recommendation before user review

**A > C > B.**

Study A currently offers the best balance between recognisable branding, readability and compatibility with the established card system. Study C is worth refining instead if the final brand should lean more strongly into the game/dice identity. Study B is the safest compact alternative, but has the least movement.

No production logo should be committed until the user selects a direction.
