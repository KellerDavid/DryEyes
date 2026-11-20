# DryEyes – szemkímélő desktop alkalmazás

Kotlin + Compose Multiplatform (desktop/JVM) alkalmazás, amely heti rutin alapján emlékeztet a szemkímélő
szünetekre, lehetővé teszi az emlékeztető elhalasztását (szundi), és rövid, lépésenkénti szemtornát vezet végig.

## MVP funkciók

| Követelmény | Megvalósítás |
|---|---|
| US-01 Szemkímélő gyakorlat | Beépített 4 lépéses, ~1 perces gyakorlat visszaszámlálóval |
| US-03 Előrehaladás | Sikerképernyő gratulációval, mai / összes / sorozat (nap) / pont |
| US-07 Nem blokkol | Kompakt ablak, a gyakorlat alatt minden más program használható; az emlékeztető kis sarokablak, amely nem veszi el a fókuszt |
| US-13 Értesítés ütemezés | Heti rutin (napok, kezdés–vége, időköz), alapértelmezés: H–P 9:00–17:00, 20 percenként |
| US-14 Szundi | Natív OS-értesítés + sarokablak „Gyakorlat indítása” / „Szundi” gombokkal; szundi hossza állítható (5/10/15/30 perc) |
| US-18 Hirdetések | Statikus, beépített minta bannerek csak a Főoldalon és a Beállításokban |

Analitika: `AnalyticsTracker` interfész, jelenleg `NoOpAnalyticsTracker` – semmilyen adat nem hagyja el a gépet.
PostHog-implementáció később beköthető (`core/analytics`).

## Futtatás és build

Szükséges: **JDK 21** (a csomagoláshoz olyan JDK kell, amely tartalmazza a `jpackage`-et, pl. Temurin).

```bash
./gradlew :composeApp:run            # alkalmazás indítása
./gradlew :composeApp:desktopTest    # unit- és adatbázistesztek
./gradlew :composeApp:packageDmg     # macOS telepítő (DMG)
./gradlew :composeApp:packageMsi     # Windows telepítő (Windows-on)
./gradlew :composeApp:packageDeb     # Linux csomag (Linuxon)
```

A telepítők a `composeApp/build/compose/binaries/main/` alá kerülnek. Minden platform csomagját az adott
operációs rendszeren kell elkészíteni.

Fejlesztéshez külön adatkönyvtár használható: `DRYEYES_DATA_DIR=/tmp/dryeyes ./gradlew :composeApp:run`.

## Adattárolás

Helyi SQLite adatbázis (Room) az operációs rendszer felhasználói adatkönyvtárában – nem a telepítési könyvtárban,
így frissítéskor a beállítások megmaradnak:

- macOS: `~/Library/Application Support/DryEyes/dryeyes.db`
- Windows: `%APPDATA%\DryEyes\dryeyes.db`
- Linux: `$XDG_DATA_HOME/dryeyes/dryeyes.db` (alapértelmezés: `~/.local/share/dryeyes`)

## Háttérben futás – fontos korlát

Az ablak bezárásakor az alkalmazás a **rendszertálcán fut tovább**, az ütemező aktív marad. A tálcamenüből
(„Kilépés”) történő teljes kilépés után **nem érkeznek emlékeztetők** – ehhez később natív OS-ütemezés vagy
háttérszolgáltatás kellene.

Az ütemező 15 másodpercenként újraértékel, ezért alvó állapotból visszatérve automatikusan újraszámol;
az alvás alatt elmaradt emlékeztetők **nem halmozódnak fel** (csak a 2 percen belül esedékessé vált alkalom
jelenik meg), és ugyanaz az alkalom soha nem jelenik meg kétszer.

## Projektstruktúra

```
composeApp/src/
├── commonMain/kotlin/com/eyecare/
│   ├── presentation/   UI (Compose) + ViewModel-ek: home, schedule, exercise, settings, reminder
│   ├── domain/         modellek, repository-interfészek, use case-ek, óra-absztrakció
│   ├── data/           Room adatbázis, entitások, DAO-k, repository-implementációk, alapadatok
│   ├── core/           scheduler, notification, gamification, advertisement, analytics
│   └── di/             AppContainer (kézi függőségkezelés)
├── commonMain/composeResources/drawable/   bannerek, ikon
├── desktopMain/kotlin/com/eyecare/
│   ├── Main.kt         ablak, tálca, emlékeztető-ablak
│   ├── platform/       natív értesítés, emlékeztető-ablak
│   └── database/       adatbázis-gyár, OS adatkönyvtár
└── desktopTest/        tesztek (ütemezés + DST, szundi, gyakorlat, haladás, Room)
```
