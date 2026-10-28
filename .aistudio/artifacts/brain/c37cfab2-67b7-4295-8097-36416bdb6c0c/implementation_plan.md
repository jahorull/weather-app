# Atmos 3D — Premium Real-Time Telemetry, Air Quality, Alerts & 3D Accessibility Upgrade

This upgrade elevates the existing **Atmos 3D** Android application while preserving its dark glassmorphic visual identity, 6-destination navigation, interactive 3D Earth globe, and `SensorEventListener` 3D camera perspective engine. It adds live **US AQI & Particulate Air Quality**, real **Weather Alerts (NWS API + live WMO meteorological threshold advisories)**, strict **null-vs-zero telemetry handling**, **no-mock-data network/cache integrity**, **system Reduced Motion accessibility detection**, and upgraded **cubic-Bézier interactive forecast charts**.

### User Review & Critical Decisions

> [!IMPORTANT]
> All three product decisions below incorporate your confirmed selections from the clarification step.

- **Confirmed Decision 1 — Live Air Quality Telemetry**: Integrates the **Open-Meteo Air Quality API** (`https://air-quality-api.open-meteo.com/v1/air-quality`) to display real-time **US AQI**, **PM2.5**, **PM10**, **Ozone (O₃)**, and **NO₂** with EPA health category badges.
- **Confirmed Decision 2 — Authentic Weather Alerts**: Queries the live **National Weather Service (NWS) Alerts API** (`https://api.weather.gov/alerts/active?point={lat},{lon}`) for official regional warnings (displaying severity, affected area, issue time, and issuing office/source) and pairs it with **WMO Live Threshold Advisories** triggered strictly when actual live API readings exceed severe thresholds (e.g., WMO code 95–99 thunderstorm, wind ≥ 50 km/h, UV ≥ 8). When no alerts are active, the UI cleanly reports *"No active severe weather alerts"*—never inventing warnings.
- **Confirmed Decision 3 — Reduced Motion & Low-End Device Optimization**: Automatically detects the Android system's `Settings.Global.ANIMATOR_DURATION_SCALE` accessibility setting and pairs it with a manual **Reduced Motion** switch and **Low-Performance 2D Mode** in Settings to pause/simplify 3D camera tilt, particle velocity, and lightning flashes when enabled.

---

### 1. Overview & Core Concept

- **What It Does**: Upgrades the existing Kotlin + Jetpack Compose Atmos 3D app to deliver verifiable meteorological accuracy (Current, 24h Hourly, 7-Day Daily, US AQI Air Quality, and Active Weather Alerts) inside an adaptive 3D atmospheric viewport synchronized with the city's actual local time and weather code.
- **Preserved Features**: Retains all 6 screens (Weather Dashboard, Forecasts, 3D Globe, Analytics & 3D Studio, Cities, Settings), the `MainActivity` `SensorEventListener` 3D tilt parallax, frosted-glass M3 components, and Room + DataStore persistence.
- **Key Value**: Guarantees 100% authentic live or explicitly badged cached telemetry (never presenting fabricated mock numbers as live weather), distinguishes missing API fields (`Unavailable`) from `0`, and ensures smooth performance and accessibility across both flagship and low-end Android devices.

---

### 2. User Experience & Visual Design

#### Key Upgraded User Flows
1. **Strict Live vs. Cached Telemetry & Retry Flow**:
   - On launch or city selection, the app fetches live Forecast, Air Quality, and Weather Alerts in parallel.
   - If the network succeeds, a green **`LIVE API`** badge and exact timestamp (`Updated 11:28 AM · City Local Time`) are shown.
   - If the network fails and a Room cache snapshot exists for that city, an amber **`CACHED DATA`** banner clearly states when the cached snapshot was saved and provides an immediate **Retry Live Fetch** button.
   - If the network fails for a brand-new city with no cached data, a dedicated glassmorphic **Connection Error & Retry** state is shown instead of fake weather numbers.
2. **Air Quality (US AQI, PM2.5, PM10) & Weather Alerts Dashboard Section**:
   - Adds a dedicated **Air Quality Card** on the Dashboard and Analytics screens displaying the live **US AQI** score, color-coded EPA category (*Good*, *Moderate*, *Unhealthy for Sensitive Groups*, *Unhealthy*, *Very Unhealthy*, *Hazardous*), and individual **PM2.5** and **PM10** (µg/m³) readouts.
   - Adds a **Weather Alerts Card** displaying any active official NWS alert or WMO live threshold advisory with **Severity Badge**, **Affected Area**, **Issue Time**, **Source**, and expandable description.
3. **Upgraded Interactive Forecast Charts (24-Hour & 7-Day)**:
   - Upgrades the 24-hour chart to render a smooth cubic-Bézier temperature curve with glowing gradient fill underneath, direct temperature value callouts, precipitation probability bars, and horizontal drag/tap scrubbing.
   - Tapping any hour or day updates the inspection card with full telemetry (Temperature, Feels Like, Rain Probability, Wind Speed & Cardinal Direction, Humidity, UV Index, Sunrise/Sunset) and a one-tap button to preview that condition in the 3D viewport.
4. **Synchronized 3D Atmosphere & Accessibility Controls**:
   - Synchronizes the 3D sky dome, solar/lunar elevation angle, and volumetric lighting with the selected city's **actual local hour** (`current.time` from API) and WMO weather code.
   - Respects system **Reduced Motion** (`ANIMATOR_DURATION_SCALE == 0f`) and the in-app **Reduced Motion** toggle by freezing continuous camera sway, disabling bright lightning screen flashes, and minimizing particle motion.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Strict Null-vs-Zero Telemetry Representation**
  - *Chosen Approach*: Represent optional sensor/API metrics (`uvIndex`, `visibilityKm`, `precipProbabilityPercent`, `usAqi`, `pm25`, `pm10`) as nullable types (`Double?` / `Int?`) throughout the DTO and domain layers. Format `null` as `"Unavailable"` and `0` as `"0"` (`0%`, `0.0 km`, `AQI 0`).
  - *Why*: Prevents misleading users into thinking UV, visibility, or air pollution is zero when an API station omits a reading.
- **Decision 2: Parallel Non-Blocking API Aggregation**
  - *Chosen Approach*: Use Kotlin Coroutines `supervisorScope` + `async` inside `WeatherRepository` so that if the secondary Air Quality or NWS Alerts endpoint times out, the primary Open-Meteo Forecast still renders immediately while marking only the failed sub-section as `"Unavailable"`.
  - *Why*: Ensures fast load times and high resilience without unnecessary retries or full-screen failures.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### Architecture & Component Diagram
```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     ATMOS 3D UPGRADED PRESENTATION LAYER                     │
│  ┌────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐  │
│  │ 3D Weather Scene   │  │ Upgraded Dashboard   │  │ Interactive Cubic    │  │
│  │ + Local Time Sync  │  │ + US AQI Air Quality │  │ Charts (24h & 7d)    │  │
│  │ + Reduced Motion / │  │ + Live Weather Alerts│  │ + Null-Safe Readouts │  │
│  │   Low-Power Guard  │  │ + Retry & Cache Badge│  │ + 3D Earth Explorer  │  │
│  └─────────▲──────────┘  └──────────▲───────────┘  └──────────▲───────────┘  │
│            └────────────────────────┼─────────────────────────┘              │
│                                     │ StateFlow<AtmosUiState>                │
│                            ┌────────┴─────────┐                              │
│                            │  AtmosViewModel  │                              │
│                            └────────▲─────────┘                              │
└─────────────────────────────────────┼────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼────────────────────────────────────────┐
│                      UPGRADED DOMAIN & DATA LAYER                            │
│                            ┌────────┴─────────┐                              │
│                            │ WeatherRepository│                              │
│                            └──▲─────▲──────▲──┘                              │
│           ┌───────────────────┘     │      └───────────────────┐             │
│           ▼                         ▼                          ▼             │
│ ┌───────────────────────┐ ┌──────────────────────┐ ┌───────────────────────┐ │
│ │ Open-Meteo Forecast   │ │ Open-Meteo Air       │ │ NWS Active Alerts API │ │
│ │ + Geocoding API       │ │ Quality API          │ │ (api.weather.gov) +   │ │
│ │ (Current, 24h, 7d)    │ │ (US AQI, PM2.5, PM10)│ │ WMO Live Thresholds   │ │
│ └───────────────────────┘ └──────────────────────┘ └───────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Data Model Additions & State Mapping
- **`AirQualityData`**: `usAqi: Int?`, `pm25: Double?`, `pm10: Double?`, `ozone: Double?`, `nitrogenDioxide: Double?`, `categoryLabel: String`.
- **`WeatherAlert`**: `id: String`, `headline: String`, `severity: String` (*Extreme*, *Severe*, *Moderate*, *Advisory*), `affectedArea: String`, `issuedAt: String`, `source: String`, `description: String`.
- **`UserSettings` Additions**: `reducedMotion: Boolean = false` (combined at runtime with Android's system `ANIMATOR_DURATION_SCALE` check).
- **`WeatherBundle` Additions**: Includes `airQuality: AirQualityData?`, `alerts: List<WeatherAlert>`, `cityLocalTime: String`, and `isFromCache: Boolean`. When no network and no Room cache exist for a city, `AtmosUiState` exposes `hasNoDataError: Boolean` with a one-tap **Retry** handler rather than synthetic weather numbers.
