# Atmos 3D — Futuristic 3D Weather Forecast Android App

Atmos 3D transforms routine weather checking into a living, interactive 3D atmospheric window. Combining real-time volumetric 3D weather rendering, hardware sensor parallax, an interactive 3D globe explorer, and live meteorological data with offline resilience, the application delivers a cinematic weather experience optimized for smooth performance on mid-range and flagship Android devices.

### User Review & Critical Decisions

> [!IMPORTANT]
> Because the initial clarification questions were skipped, we have selected the recommended defaults below so the application works out-of-the-box with zero manual setup while still supporting optional custom API keys via AI Studio Secrets.

- **Live Weather & Geocoding Engine (Recommended Default)**: Uses the **Open-Meteo Forecast & Geocoding API** as the primary zero-key live provider for immediate out-of-the-box real weather data, alongside built-in support for an optional `WEATHER_API_KEY` (via `BuildConfig` and `.env.example`) for OpenWeatherMap/WeatherAPI.com integration.
- **3D Rendering & Visual Style (Recommended Default)**: Uses a custom **Volumetric 3D Canvas & Depth-Particle Engine** with 3D perspective projection, multi-layer parallax driven by hardware accelerometer/gyroscope sensors (`SensorManager`) and touch drag gestures, and an adaptive 2D performance mode toggle.
- **Persistence & Offline Caching (Recommended Default)**: Uses **Room Database** for offline weather snapshot caching and saved favorite locations, paired with **Jetpack DataStore Preferences** for user settings (temperature units, wind speed units, theme mode, animation quality, and auto-refresh interval).

---

### 1. Overview & Core Concept

- **What It Does**: Atmos 3D renders live local and global weather conditions inside an interactive 3D atmospheric viewport. Every weather state—Sunny, Cloudy, Rainy, Thunderstorm, Snowy, Foggy, Sunset, and Night—features distinct 3D depth layers, volumetric lighting, particle physics, and interactive touch/tilt camera movement.
- **Target Audience / Persona**: Everyday commuters, travelers, and weather enthusiasts who want both immediate, glanceable atmospheric clarity and deep meteorological analytics (hourly/7-day charts, UV index, pressure, visibility, wind vectors, and global regional comparisons).
- **Key Value**: Bridges cinematic 3D visual immersion with trustworthy, real-time meteorological telemetry, transparent offline cache indicators, and zero-friction city discovery via both search and an interactive 3D Earth globe.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **3D Atmospheric Splash & Onboarding**: Launches with a brief, fluid 3D atmospheric particle vortex and glowing orbital horizon transition into the main dashboard.
2. **Main Weather Dashboard & Immersive Mode**:
   - Displays the active city/country header, live vs. cached status badge, last updated timestamp, quick-refresh button, and GPS location button (with an explicit rationale dialog before requesting Android runtime location permissions).
   - Features the interactive 3D Weather Viewport in the upper/full-bleed hero area (supports device tilt parallax, touch rotation/scrubbing, and a one-tap **Immersive Full-Screen 3D Mode** toggle that hides UI chrome).
   - Presents frosted-glass telemetry cards: large current temperature, condition label, feels-like, daily High/Low, horizontally scrollable 24-hour strip, and a 2-column grid of compact cards for Humidity, Wind Speed & 3D Compass Direction, UV Index gauge, Visibility, Atmospheric Pressure gauge, and Sunrise/Sunset orbital arc.
3. **Detailed Forecasts & Interactive Charts (Hourly & 7-Day)**:
   - Dedicated view with segmented switching and interactive scrubbing across the 24-hour timeline and 7-day outlook.
   - Interactive temperature curve, precipitation probability bars, wind speed trends, and humidity curve. Tapping any hour or day expands a detailed meteorological breakdown sheet.
4. **Interactive 3D Earth Globe & City Explorer**:
   - A draggable, zoomable 3D spherical Earth rendered with latitude/longitude graticules, continental landmass vectors, day/night terminator shading, atmospheric limb glow, and live interactive weather pins for global hubs and user-saved cities.
   - Tapping any pin or region on the 3D globe previews its live weather and lets the user jump directly to its full 3D environment or save it to favorites.
5. **Weather Analytics & Scene Studio**:
   - Deep-dive comparative telemetry (comfort index, dew point, solar radiation arc, barometric trend, wind rose) plus an interactive **3D Atmosphere Preview Switcher** allowing users to inspect all 8 weather scenes (Sunny, Cloudy, Rainy, Thunderstorm, Snowy, Foggy, Sunset, Night) in real time.
6. **Saved Locations & City Search**:
   - Live geocoding city search with autocomplete results (city, admin region, country, coordinates), favorite city cards showing cached temperatures and weather condition badges, swipe/tap removal, and default city selection (defaulting to Dhaka, Bangladesh if GPS permission is declined on first launch).
7. **Settings & Performance Control**:
   - Instant toggles for Temperature Units (°C / °F), Wind Speed Units (km/h, mph, m/s), Theme Mode (Dynamic Weather Adaptive, Dark, Light), 3D Graphics Quality (High 3D Volumetric, Balanced, 2D Battery-Saver Fallback), Auto-Refresh Interval, and Location Permission status.

#### Visual Identity & Theme
- **Aesthetic Direction**: *Cinematic Atmospheric Glassmorphism*—deep spatial gradients, translucent frosted surfaces with subtle specular borders, and luminous telemetry accents that dynamically shift palette based on current weather and time of day.
- **Color Palette & Mood**:
  - *Sunny / Clear Day*: Deep Cerulean Sky (`#0C4A6E` to `#0284C7`) with Solar Amber accents (`#FBBF24`).
  - *Sunset / Golden Hour*: Twilight Indigo (`#1E1B4B`) through Warm Crimson-Orange (`#EA580C`) with Rose Gold highlights (`#F472B6`).
  - *Rainy / Thunderstorm*: Abyssal Slate (`#090D16` to `#1E293B`) with Electric Cyan lightning/rain accents (`#38BDF8`) and Violet storm glow (`#818CF8`).
  - *Snowy / Foggy*: Glacial Navy (`#0F172A` to `#334155`) with Frost Mint (`#67E8F9`) and Crisp Ice White (`#F8FAFC`).
  - *Night*: Cosmic Obsidian (`#050811` to `#0F172A`) with Starlight Silver and Lunar Cyan (`#22D3EE`).
- **Typography & Hierarchy**:
  - *Display / Hero Temperatures*: **Space Grotesk** (bundled locally via `font-util`) for crisp, geometric, futuristic numerical readouts and city headers.
  - *Body & Telemetry Labels*: **Plus Jakarta Sans** (bundled locally via `font-util`) for high-legibility metrics, charts, and forecast lists.
- **Component Styling & Layout**:
  - Edge-to-edge full-bleed 3D canvas backdrop with safe-area inset padding (`WindowInsets.safeDrawing` and `WindowInsets.navigationBars`).
  - Translucent frosted cards (`Color.White.copy(alpha = 0.10f)` in dark/atmospheric mode with `1.dp` gradient border strokes) and responsive layout adaptation (`Compact` bottom navigation bar vs. `Expanded` tablet side navigation rail + two-column dashboard).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Engine Live Weather API (Open-Meteo + Optional Secret Key Support)**
  - *Chosen Approach*: Use Retrofit + Moshi with Open-Meteo's live Weather Forecast and Geocoding endpoints by default while reading `BuildConfig.WEATHER_API_KEY` (configured via `.env.example` and the Secrets Gradle Plugin) to support optional authenticated weather endpoints.
  - *Why*: Guarantees 100% real, live meteorological data for any city or GPS coordinate immediately upon launch without failing on missing user keys, while strictly fulfilling secure API credential configuration requirements.
  - *Alternatives Considered*: Requiring a mandatory OpenWeatherMap key before showing data—rejected because an unconfigured key would block live weather functionality on first run.
- **Decision 2: Custom 3D Projection & Hardware-Accelerated Shader/Canvas Engine**
  - *Chosen Approach*: Build a dedicated 3D projection, lighting, and particle physics engine in Jetpack Compose Canvas with `SensorEventListener` (accelerometer/gyroscope tilt parallax) and touch-drag camera orbits, plus a 3D spherical globe renderer with 3D-to-2D perspective math and back-face culling.
  - *Why*: Provides 60fps real-time 3D weather environments (volumetric ray shafts, 3D rain/snow depth particles, wet-glass droplet refraction, lightning arcs, and a rotating 3D Earth) without heavyweight native `.so` engine bloat or compatibility crashes on emulator/mid-range GPUs, and includes an instant 2D fallback mode.
- **Decision 3: Offline-First Room Cache + DataStore Preferences**
  - *Chosen Approach*: Persist every fetched city forecast as a serialized JSON snapshot in Room alongside a `lastUpdatedEpochMs` timestamp, and store saved cities and user preferences in Room/DataStore.
  - *Why*: Enables instant startup, seamless offline browsing with a clear "Cached · Offline" vs. "Live · Updated just now" indicator, and automatic background refresh when connectivity resumes.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### Architecture & Component Diagram
```
┌────────────────────────────────────────────────────────────────────────────┐
│                        ATMOS 3D PRESENTATION LAYER                         │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │  3D Weather Hero │  │  Forecast &      │  │  Interactive 3D Earth    │  │
│  │  Scene Renderer  │  │  Analytics Views │  │  Globe & City Explorer   │  │
│  │ (8 3D Environments│  │ (24h Strip, 7d   │  │ (3D Sphere Projection,   │  │
│  │ + Tilt Parallax  │  │  List, Temp/Rain │  │  Day/Night Terminator,   │  │
│  │ + Immersive Mode)│  │  Interactive     │  │  Live Global City Pins)  │  │
│  └────────▲─────────┘  │  Charts)         │  └────────────▲─────────────┘  │
│           │            └────────▲─────────┘               │                │
│           └─────────────────────┼─────────────────────────┘                │
│                                 │ StateFlow<AtmosUiState>                  │
│                        ┌────────┴─────────┐                                │
│                        │  AtmosViewModel  │                                │
│                        └────────▲─────────┘                                │
└─────────────────────────────────┼──────────────────────────────────────────┘
                                  │
┌─────────────────────────────────┼──────────────────────────────────────────┐
│                         DOMAIN & DATA LAYER                                │
│                        ┌────────┴─────────┐                                │
│                        │ WeatherRepository│                                │
│                        └──▲─────▲──────▲──┘                                │
│                           │     │      │                                   │
│      ┌────────────────────┘     │      └────────────────────┐              │
│      ▼                          ▼                           ▼              │
│ ┌────────────────────┐ ┌──────────────────────┐ ┌────────────────────────┐ │
│ │  Retrofit + Moshi  │ │  Room Database       │ │  Device Sensors & GPS  │ │
│ │  Live Weather &    │ │  - SavedLocationDao  │ │  - FusedLocation /     │ │
│ │  Geocoding APIs    │ │  - WeatherCacheDao   │ │    LocationManager     │ │
│ │  + BuildConfig Key │ │  + DataStore Prefs   │ │  - Tilt Parallax Sensor│ │
│ └────────────────────┘ └──────────────────────┘ └────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────┘
```

#### Data Model & State
- **`WeatherBundle` (Domain Model)**:
  - `location`: `SavedLocation` (id, cityName, region, country, latitude, longitude, timezone, isCurrentLocation, isFavorite).
  - `current`: `CurrentWeather` (temperatureC, feelsLikeC, highC, lowC, conditionCode, sceneType: `WeatherSceneType`, humidity, windSpeedKmh, windDirectionDeg, uvIndex, visibilityKm, pressureHpa, sunriseIso, sunsetIso, isDay).
  - `hourly`: `List<HourlyForecast>` (24 hours: timeIso, tempC, feelsLikeC, precipProbPercent, windSpeedKmh, humidity, conditionCode, sceneType).
  - `daily`: `List<DailyForecast>` (7 days: dateIso, maxTempC, minTempC, precipProbPercent, maxWindKmh, uvIndexMax, sunriseIso, sunsetIso, conditionCode, sceneType).
  - `fetchedAtMillis`: Epoch timestamp used to compute relative age and distinguish `isFromCache` vs. live network response.
- **`WeatherSceneType` (8 Distinct 3D Environments)**:
  - `SUNNY`, `CLOUDY`, `RAINY`, `THUNDERSTORM`, `SNOWY`, `FOGGY`, `SUNSET`, `NIGHT`.
- **`UserSettings` (DataStore)**:
  - `tempUnit` (`CELSIUS` / `FAHRENHEIT`), `windUnit` (`KMH` / `MPH` / `MS`), `themeMode` (`DYNAMIC` / `DARK` / `LIGHT`), `graphicsQuality` (`HIGH_3D` / `BALANCED_3D` / `FALLBACK_2D`), `autoRefreshMinutes` (`15` / `30` / `60` / `OFF`).

#### Interactive Component & State Mapping
- **Location Rationale & Permission Flow**: Tapping "Detect My Location" checks `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`. Before launching the system permission prompt, a glassmorphic rationale modal explains that location is used solely to fetch local atmospheric telemetry. If granted, GPS coordinates are resolved to a city name and live weather is loaded; if denied, the app gracefully falls back to the last active saved city (defaulting to Dhaka, Bangladesh) with an informative banner.
- **Interactive Forecasts & Detail Inspection**: Tapping any hourly pill or 7-day forecast row opens a rich bottom sheet / detail panel showing hourly curve progressions, wind vector compass, UV safety guidance, and sunrise/sunset solar progress for that specific time window; users can also preview that forecast's 3D weather environment.
- **3D Globe Interaction**: Dragging across the 3D Earth canvas rotates pitch and yaw angles with inertia; pinch/slider controls zoom; tapping a 3D projected city marker highlights its live weather card and allows instant switching or saving to favorites.
- **Automated Verification**: Includes JUnit unit tests for unit conversion (°C/°F, km/h/mph/m/s), WMO weather code to `WeatherSceneType` mapping, and offline cache expiration logic, plus Robolectric Compose UI tests verifying dashboard rendering, navigation across all screens, and interactive forecast selection.
