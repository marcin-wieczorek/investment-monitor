# Source verification workflow

The project uses two complementary verification mechanisms.

## Fixture tests

Fixture tests are deterministic and run without network access:

```bash
./gradlew test
```

Fixtures live under `src/test/resources/fixtures/<source>/`. They are reviewed snapshots of real developer pages. Parser tests assert normalized domain fields, especially location, property type, house area, **plot area**, price, unit count and status.

## Live verification

Live verification contacts configured developer sites and validates parser output against source-health rules:

```bash
./gradlew verifySources
```

It is diagnostic only and must never update the trusted monitoring snapshot.

## Fixture capture

A deliberate capture command is provided for creating/updating fixtures:

```bash
./gradlew captureFixtures
```

Captured HTML is test input and must be reviewed before committing.

## Recommended workflow after a website change

1. Run `./gradlew verifySources`.
2. Identify the failing source/field.
3. Capture the current page.
4. Inspect the fixture.
5. Fix the parser.
6. Add/update regression assertions.
7. Run `./gradlew test`.
8. Run `./gradlew verifySources` again.
9. Commit parser and fixture together.

Live verification answers: **"Does the parser still understand today's website?"**

Fixture tests answer: **"Does this parser deterministically map known HTML correctly?"**

Neither replaces the other.
