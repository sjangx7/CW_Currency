# GBP Currency Converter — Sae Jang (s2432618)

Coursework 1 for  module Mobile Platform Development at Glasgow Caledonian University.

---

## Features
- Live GBP exchange rates from [FX-Exchange.com](https://www.fx-exchange.com/gbp/rss.xml)
- Search by code, name, or country
- Quick buttons (USD / EUR / JPY)
- Colour-coded rates + flag icons
- Bi-directional converter (GBP ↔ Currency)
- Auto-update every 15 minutes (WorkManager)
- Offline error banner
- Portrait & landscape layouts
- Accessibility labels and TalkBack support


## Architecture
**Model-View-View-Model (MVVM)**
- `RateItem` – data model
- `RatesViewModel` – business logic & LiveData
- `MainActivity` – RecyclerView, SearchView, ViewFlipper
- `ConverterFragment` – conversion UI


##  Testing
See full test table in coursework report.  
Key checks: search filtering, conversion accuracy, auto-refresh, offline handling, orientation switch.


## Build Instructions
- **Min SDK:** 24  •  **Target SDK:** 34
- **Permissions:** `INTERNET`
- Open project in Android Studio (Flamingo or newer) → Run on emulator/device.
- **Android package kit (APK):** `app/build/outputs/apk/debug/app-debug.apk`

---

##  Demo Video
_Link to ScreenPal / OneDrive video here_

---

## Credits
- FX-Exchange.com RSS Feed
- Flagpedia Commons (CC-BY-SA 4.0) / System Emoji Flags
- Material Design Colours
- GCU Mobile Platform Development Course (2025 / 26)

---

© 2025 Sae Jang — Glasgow Caledonian University
