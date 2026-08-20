# Głęboka analiza projektu — fundamentalne obserwacje i plan działania

Data analizy: 2026-08-20

## Status quo

Architektura pipeline solidna: deterministyczny diff (`canonicalKey`), fail-closed
validation (50% drop guard), per-field evidence tracking, separacja scraping↔persistence.
26 źródeł deweloperskich, 7 discovery (BIP/WZ), 1 agregator (RynekPierwotny).
Frontend z sensownymi filtrami na `/investments` (text search, range sliders, sort).
Kod Kotlin czysty, idiomatyczny, fixture-based testy.

Poniżej 6 fundamentalnych problemów ograniczających użyteczność portalu
jako narzędzia do szybkiego, czytelnego wyszukiwania ofert z różnych źródeł.

---

## 1. BRAK CROSS-SOURCE DEDUP — użytkownik widzi duplikaty

### Problem

System NIE rozpoznaje tej samej inwestycji z różnych źródeł.
`canonicalKey = source:normalized-url` → permanentnie odrębne identity.

Przykład: RynekPierwotny → "Osiedle Tercja | Chronos", ChronosSource → "Tercja".
Dwa osobne rekordy w DB, dwa wiersze w tabeli, brak powiązania.

`InvestmentCorrelator` łączy Signal↔Investment, **nigdy** Investment↔Investment.
Flag `aggregator_only_discovery` to heurystyka lokalizacyjna, nie dedup.

### Skutek

Użytkownik szukający ofert widzi te same inwestycje wielokrotnie.
Zamiast konsolidacji — szum. Fundamentalnie podważa cel portalu.

### Plan działania

1. **Nowy `InvestmentDeduplicator`** (`correlation/InvestmentDeduplicator.kt`)
   - Matching: normalized developer name (via `DeveloperNameMatcher`) +
     location match (via `LocationCatalog`) + name similarity (Jaccard na tokenach)
   - Confidence: HIGH (developer+location+name), MEDIUM (developer+location),
     LOW (location+name bez developer match)

2. **Nowa tabela `investment_cluster`** (Flyway V10)
   - `cluster_id` INTEGER PK
   - `canonical_investment_id` INTEGER FK → investment(id) — representative
   - Tabela `investment_cluster_member`: `cluster_id` + `investment_id`

3. **Frontend: grupowanie w tabeli**
   - Na `/investments` wyświetlaj klastry, nie surowe rekordy
   - Expandable row → pokaż źródła z których pochodzi ta sama inwestycja
   - Badge "potwierdzone przez N źródeł"

4. **Cross-enrichment** — po dedup, merguj dane z różnych źródeł:
   cena z RynekPierwotny + metraże z detail parsera + lokalizacja z developera

**Trudność**: średnia · **Priorytet**: krytyczny

---

## 2. MASYWNE LUKI DANYCH — scoring bezużyteczny

### Problem

| Pole | Waga w scorer | Parserów z danymi |
|---|---|---|
| `price` | 0.15 | **0** deweloperskich, 1 agregator |
| `plotArea` | 0.25 (najwyższa!) | 2 deweloperskie |
| `houseArea` | 0.20 | 5 deweloperskich |
| `propertyType` | 0.25 | 4 deweloperskie |

14 z 25 parserów deweloperskich daje **wyłącznie** name/url/location/imageUrl.
Scorer redistrybuuje wagi, ale przy brakujących 3–4 komponentach wynik oparty
na jednym polu jest bezwartościowy.

### Skutek

`overall_score` wygląda jak sensowna metryka, ale dla większości inwestycji
to artefakt jednego-dwóch pól. Sortowanie/filtrowanie po score → wyniki mylące.

### Plan działania

1. **Detail parsery** — priorytet, bo infrastruktura (`InvestmentDetailEnricher`)
   już istnieje, jedyny impl to `TercjaDetailParser`
   - Strony szczegółowe deweloperów zwykle publikują metraże/ceny
   - Cel: detail parser dla każdego z 14 baseline-only deweloperów
   - Kolejność wg popularności/wielkości: Agrobex, Develia, Robyg,
     ATAL, Murapol, Ataner, Duda, EBF, Konimpex, Linea

2. **Data completeness indicator** na frontendzie
   - Per-inwestycja: ile z 6 kluczowych pól wypełnionych (0–6)
   - Badge/progress bar obok score
   - Score z <3 pól → wyświetlaj jako "niewystarczające dane" zamiast %

3. **Cross-enrichment z agregatora** (wymaga dedup z punktu #1)
   - RynekPierwotny dostarcza ceny i metraże
   - Po powiązaniu z deweloperskim rekordem → merge brakujących pól

**Trudność**: duża (per-site work) · **Priorytet**: wysoki

---

## 3. BRAK NOTYFIKACJI — pipeline bez wartości real-time

### Problem

System działa jako `bootRun` → skan → exit. Jedyny sposób zobaczenia
nowych inwestycji: otworzyć dashboard, przejrzeć ręcznie. Brak:
- email/push/telegram notyfikacji o NEW investments
- alertów na watched investments ze zmianami
- cron/scheduled execution

### Skutek

Pipeline wykrywa zmiany, ale nikt ich nie widzi jeśli nie otwiera portalu.
Fundamentalnie ogranicza "szybkie znalezienie" — wymaga aktywnego szukania
zamiast pasywnego powiadamiania.

### Plan działania

1. **Scheduled execution**
   - systemd timer lub cron: `./gradlew bootRun` co 6h
   - Alternatywa: GitHub Actions scheduled workflow

2. **Notification service** (`reporting/NotificationService.kt`)
   - Interface `NotificationChannel` z impl: `TelegramNotificationChannel`,
     `EmailNotificationChannel`
   - Trigger: po `ScanReport` z `newInvestments > 0`
   - Payload: lista nowych inwestycji z linkami do portalu
   - Config w `application.yml` (bot token, chat ID, SMTP)

3. **Watch alerts**
   - Na CHANGED investments z `watched = true` → dodatkowe powiadomienie
   - Treść: co się zmieniło (diff pól)

**Trudność**: niska · **Priorytet**: wysoki

---

## 4. BRAK MAPY — lokalizacja to tekst, nie przestrzeń

### Problem

Inwestycje nieruchomościowe to fundamentalnie problem przestrzenny.
System przechowuje `location` jako free-text string.
Frontend wyświetla tekst w tabeli.

Brak widoku mapowego, geokodowania, filtrowania "w promieniu X km".

### Skutek

Użytkownik z pytaniem "co jest w okolicy Swarzędza" musi ręcznie filtrować
tekst. Nie jest to "szybkie i czytelne wyszukiwanie" dla domeny nieruchomości.

### Plan działania

1. **Statyczny geocoding `LocationCatalog`**
   - ~50 lokalizacji → hardcoded mapping `name → {lat, lng}`
   - Nie wymaga API — współrzędne z OpenStreetMap, jednorazowy lookup
   - Nowa tabela lub rozszerzenie `location_profile` o `latitude`/`longitude`

2. **Nowa strona `/map`**
   - Leaflet + OpenStreetMap tiles (darmowe, bez klucza API)
   - Markery kolorowane wg source category (deweloper/discovery/agregator)
   - Popup z nazwą, deweloperem, ceną, linkiem do detail
   - Klaster markerów przy dużym zoomie out

3. **Filtr spatial**
   - Dropdown "okolica" (gmina/lokalizacja) + slider "promień km"
   - Obliczenie haversine na 50 lokalizacjach → zerowy koszt

4. **Sidebar nav**: dodaj `/map` z ikoną MapPin

**Trudność**: średnia · **Priorytet**: wysoki

---

## 5. CORRELATOR ZBYT OGRANICZONY — niski recall

### Problem

`InvestmentCorrelator` łączy Signal↔Investment wyłącznie na:
- exact location match (via `LocationCatalog.findIn`) → MEDIUM
- \+ developer name in signal text → HIGH

Ograniczenia:
- Sygnał bez lokalizacji z katalogu → nigdy skorelowany
- Sygnał z lokalizacją == municipality → odrzucony
- Brak fuzzy matching nazwy inwestycji w treści sygnału
- Brak temporal proximity boosting (WZ z marca → inwestycja w maju)

### Skutek

Wiele realnych powiązań pomijanych. `/correlations` rzadko ma HIGH matches.

### Plan działania

1. **Fuzzy location matching**
   - Oprócz exact `LocationCatalog.findIn`: substring match na nazwie ulicy
   - Sygnały WZ często zawierają nazwę ulicy ("ul. Poznańska 15") →
     matchuj z inwestycjami w tej samej gminie

2. **Investment name matching**
   - Tokenizacja nazwy inwestycji → szukaj tokenów w tytule sygnału
   - Np. sygnał "budowa osiedla Tercja" ↔ inwestycja "Tercja"

3. **Temporal proximity boost**
   - Sygnał WZ 1–6 miesięcy przed `first_seen_at` inwestycji →
     confidence + 1 level (LOW→MEDIUM, MEDIUM→HIGH)

4. **Municipality-level correlation jako LOW**
   - Obecnie odrzucane. Lepiej: dopuść jako LOW confidence
     (lepszy recall, użytkownik sam oceni)

**Trudność**: średnia · **Priorytet**: średni

---

## 6. BRAK PORÓWNYWANIA OFERT

### Problem

Portal pozwala przeglądać oferty, ale brak fundamentalnej funkcji
nieruchomościowej: porównywanie ofert side-by-side.

### Skutek

Użytkownik znalazł 5 ciekawych ofert → przełącza się między tabami,
zapamiętuje dane ręcznie.

### Plan działania

1. **Checkbox "dodaj do porównania"** na `/investments`
   - Stan w localStorage (jak sidebar collapsed)
   - Badge z licznikiem na nowym nav item `/compare`

2. **Strona `/compare`**
   - Tabela: kolumny = wybrane inwestycje, wiersze = cechy
     (deweloper, lokalizacja, metraż domu, metraż działki, cena,
     typ, score, źródła)
   - Kolorowanie najlepszej/najgorszej wartości w wierszu
   - Link do detail każdej inwestycji

3. **"Podobne inwestycje"** na `/investments/[id]`
   - Query: same location OR same developer OR similar area/price range
   - Limit 5, posortowane wg similarity score

**Trudność**: niska · **Priorytet**: średni

---

## Matryca priorytetów

| # | Problem | Wpływ | Trudność | Priorytet |
|---|---|---|---|---|
| 1 | Cross-source dedup | Krytyczny | Średnia | **P0** |
| 3 | Notyfikacje | Wysoki | Niska | **P1** |
| 4 | Mapa | Wysoki | Średnia | **P1** |
| 2 | Luki danych / detail parsers | Wysoki | Duża | **P2** |
| 5 | Lepszy correlator | Średni | Średnia | **P3** |
| 6 | Porównywanie ofert | Średni | Niska | **P3** |

## Kluczowe pytanie architektoniczne

Czy portal ma być **dashboardem monitoringowym** (obserwuj rynek)
czy **narzędziem decyzyjnym** (znajdź i wybierz inwestycję)?

Obecny stan → monitoring, ale bez notyfikacji nawet monitoring jest pasywny.

Na narzędzie decyzyjne brakuje: dedup + mapa + porównywanie + pełne dane.

Rekomendacja: P0 (dedup) + P1 (notyfikacje + mapa) transformują portal
z "ciekawego side-project" w realnie użyteczne narzędzie.
