package com.example.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.BangladeshCrop
import com.example.model.PersonalThresholdAlert
import com.example.model.SavedLocation
import com.example.model.SmartFarmingAdvisory
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Engine for:
 * - Feature 3: Bangladesh Smart Farming Mode (Grounded bilingual agricultural weather guidance)
 * - Feature 4: Personalized Weather Alerts (User-configured threshold evaluation & real local notifications)
 */
object SmartFarmingAndAlertsEngine {

    private const val PERSONAL_ALERT_CHANNEL_ID = "atmos_personal_weather_alerts"
    private const val PERSONAL_ALERT_CHANNEL_NAME = "Atmosyn Personalized Threshold Alerts"

    /**
     * Generates verified weather-based guidance for the selected location and Bangladesh crop
     * in both English and simple Bengali (বাংলা).
     *
     * Strictly adheres to agricultural safety rules:
     * - Grounds every recommendation in actual API forecast numbers (or clearly reports unavailable data).
     * - Explains forecast uncertainty using multi-model spread when available.
     * - Never provides unsupported pesticide/chemical dosages or guarantees crop outcomes.
     * - Indicates when local agricultural data or DAE (16123) expert guidance is needed.
     */
    fun buildSmartFarmingAdvisory(
        location: SavedLocation,
        bundle: WeatherBundle?,
        crop: BangladeshCrop,
        settings: UserSettings
    ): SmartFarmingAdvisory {
        if (bundle == null) {
            return SmartFarmingAdvisory(
                location = location,
                crop = crop,
                isDataAvailable = false,
                rainfallGuidanceEn = "Live precipitation telemetry is currently unavailable for ${location.cityName}. Do not schedule drainage or water-sensitive field operations until forecast data is refreshed.",
                rainfallGuidanceBn = "${location.cityName}-এর লাইভ বৃষ্টিপাতের তথ্য বর্তমানে পাওয়া যাচ্ছে না। আবহাওয়ার তথ্য আপডেট না হওয়া পর্যন্ত সেচ বা নিষ্কাশনের সিদ্ধান্ত নেবেন না।",
                irrigationGuidanceEn = "Unavailable — refresh live weather data before planning irrigation cycles for ${crop.nameEn}.",
                irrigationGuidanceBn = "তথ্য পাওয়া যায়নি — ${crop.nameBn}-এর সেচ দেওয়ার আগে লাইভ আবহাওয়া রিফ্রেশ করুন।",
                dryingGuidanceEn = "Unavailable — cannot verify solar radiation, humidity, or rain risk for grain/crop drying right now.",
                dryingGuidanceBn = "তথ্য পাওয়া যায়নি — এই মুহূর্তে ফসল বা বীজ শুকানোর জন্য আর্দ্রতা ও রোদের তথ্য যাচাই করা সম্ভব হচ্ছে না।",
                outdoorActivityWindowsEn = "Unavailable — connect to the internet and tap Refresh to inspect hourly wind and rain windows.",
                outdoorActivityWindowsBn = "তথ্য পাওয়া যায়নি — ঘণ্টাভিত্তিক বাতাস ও বৃষ্টির সময়সীমা দেখতে ইন্টারনেট সংযোগসহ রিফ্রেশ করুন।",
                uncertaintyExplanationEn = "No numerical weather prediction (NWP) telemetry is loaded. Forecast confidence cannot be evaluated offline.",
                uncertaintyExplanationBn = "কোনো আবহাওয়া মডেলের তথ্য লোড হয়নি। অফলাইনে পূর্বাভাসের অনিশ্চয়তা মূল্যায়ন করা সম্ভব নয়।",
                expertGuidanceNoticeEn = "Safety & Agronomy Notice: This tool provides weather-based scheduling guidance only and never guarantees crop yields or prescribes pesticide/fertilizer dosages. For soil-specific or pest management advice in Bangladesh, consult your local Upazila Agriculture Officer (DAE / কৃষি সম্প্রসারণ অধিদপ্তর) or call the Krishi Call Center at 16123.",
                expertGuidanceNoticeBn = "কৃষি নিরাপত্তা বিজ্ঞপ্তি: এই অ্যাপটি শুধুমাত্র আবহাওয়া-ভিত্তিক সাধারণ পরামর্শ দেয়; এটি ফসলের ফলনের নিশ্চয়তা দেয় না এবং কোনো কীটনাশক বা রাসায়নিক সারের মাত্রা নির্ধারণ করে না। মাটি পরীক্ষা বা রোগ-পোকামাকড় দমনের জন্য স্থানীয় উপসহকারী কৃষি কর্মকর্তা (কৃষি সম্প্রসারণ অধিদপ্তর - DAE) অথবা কৃষি কল সেন্টার ১৬১২৩ নম্বরে যোগাযোগ করুন।",
                updateTimestampLabel = "${location.cityName}, ${location.country} · Live forecast data unavailable"
            )
        }

        val cur = bundle.current
        val futureHours = bundle.hourly.filter { !it.isHistoricalObservation }
        val next24h = futureHours.take(24)
        val next3Days = bundle.daily.take(3)

        val maxRainProb24h = next24h.mapNotNull { it.precipProbabilityPercent }.maxOrNull()
            ?: cur.precipProbabilityPercent
            ?: 0
        val totalRainMm24h = next24h.sumOf { it.precipitationMm }
        val maxRainProb3d = next3Days.mapNotNull { it.precipProbabilityPercent }.maxOrNull() ?: maxRainProb24h
        val maxWind24h = next24h.maxOfOrNull { it.windSpeedKmh } ?: cur.windSpeedKmh
        val avgHumidity = cur.humidityPercent ?: 70
        val highTempC = cur.highC
        val lowTempC = cur.lowC

        val tempFormatted = WeatherFormatter.formatTemp(cur.temperatureC, settings.tempUnit)
        val highFormatted = WeatherFormatter.formatTemp(highTempC, settings.tempUnit)
        val lowFormatted = WeatherFormatter.formatTemp(lowTempC, settings.tempUnit)
        val windFormatted = WeatherFormatter.formatWind(cur.windSpeedKmh, settings.windUnit)
        val maxWindFormatted = WeatherFormatter.formatWind(maxWind24h, settings.windUnit)
        val totalRainMmLabel = String.format(Locale.US, "%.1f mm", totalRainMm24h)

        // Identify suitable calm & low-rain outdoor windows in the next 12-24h
        val calmDrySlots = next24h.filter { slot ->
            (slot.precipProbabilityPercent ?: 0) < 35 &&
                slot.precipitationMm < 0.4 &&
                slot.windSpeedKmh < 24.0
        }.take(4)

        val calmWindowLabels = if (calmDrySlots.isNotEmpty()) {
            calmDrySlots.joinToString(", ") { "${it.displayHour} (${it.precipProbabilityPercent ?: 0}% rain)" }
        } else {
            "Limited dry windows in the next 24h (peak rain chance $maxRainProb24h%)"
        }

        // 1. Rainfall & Drainage Guidance
        val (rainEn, rainBn) = when {
            maxRainProb24h >= 65 || totalRainMm24h >= 10.0 -> {
                val cropSpecificEn = when (crop) {
                    BangladeshCrop.AMAN_BORO_RICE -> "Inspect bunds (আইল) and keep drainage channels open so excess standing water does not submerge young seedlings."
                    BangladeshCrop.JUTE -> "Prevent prolonged waterlogging in juvenile jute plots; clear field side-drains before heavy showers."
                    BangladeshCrop.POTATO -> "Critical: Potato ridges are highly sensitive to standing water and late-blight moisture. Clear furrow drains immediately."
                    BangladeshCrop.MANGO_ORCHARD -> "Heavy rain during flowering/fruiting can increase blossom drop and fungal moisture; ensure orchard basin drainage."
                    BangladeshCrop.WHEAT_MAIZE -> "Maize and wheat roots suffer in waterlogged soil; open surface drainage trenches before rainfall peaks."
                    BangladeshCrop.VEGETABLES_MUSTARD -> "Raised vegetable beds and mustard plots require clear drainage furrows to prevent root rot during heavy rain."
                }
                val cropSpecificBn = when (crop) {
                    BangladeshCrop.AMAN_BORO_RICE -> "জমির আইল ও নিষ্কাশন নালা পরীক্ষা করুন যাতে অতিরিক্ত বৃষ্টির পানি জমে চারা ডুবে না যায়।"
                    BangladeshCrop.JUTE -> "পাট ক্ষেতে জলাবদ্ধতা রোধে চারপাশের নালার মুখ পরিষ্কার রাখুন।"
                    BangladeshCrop.POTATO -> "সতর্কতা: আলুর ভেলিতে পানি জমলে পচন ও নাবি ধ্বসা রোগের ঝুঁকি বাড়ে; দ্রুত নালা পরিষ্কার করে পানি নিষ্কাশনের ব্যবস্থা নিন।"
                    BangladeshCrop.MANGO_ORCHARD -> "আম বাগানে গাছের গোড়ায় যেন বৃষ্টির পানি জমে না থাকে সেজন্য নিষ্কাশন নালা সচল রাখুন।"
                    BangladeshCrop.WHEAT_MAIZE -> "গম ও ভুট্টা ক্ষেতে জলাবদ্ধতা ক্ষতিকর; বৃষ্টির আগেই জমির অতিরিক্ত পানি বের হওয়ার নালা তৈরি রাখুন।"
                    BangladeshCrop.VEGETABLES_MUSTARD -> "শাকসবজি ও সরিষার বেডে পানি জমলে শিকড় পচে যেতে পারে, তাই নিষ্কাশন নালা পরিষ্কার রাখুন।"
                }
                Pair(
                    "High rain probability ($maxRainProb24h%, est. $totalRainMmLabel over 24h; 3-day peak $maxRainProb3d%). $cropSpecificEn",
                    "আগামী ২৪ ঘণ্টায় বৃষ্টির প্রবল সম্ভাবনা ($maxRainProb24h%, সম্ভাব্য বৃষ্টিপাত $totalRainMmLabel; ৩ দিনের সর্বোচ্চ $maxRainProb3d%)। $cropSpecificBn"
                )
            }
            maxRainProb24h >= 35 -> {
                Pair(
                    "Moderate rain chance ($maxRainProb24h%, est. $totalRainMmLabel in 24h) for ${crop.nameEn}. Monitor sky conditions and keep secondary field drains clear in case showers intensify.",
                    "${crop.nameBn}-এর জন্য মাঝারি বৃষ্টির সম্ভাবনা ($maxRainProb24h%, ২৪ ঘণ্টায় সম্ভাব্য $totalRainMmLabel)। আকাশের অবস্থা পর্যবেক্ষণ করুন এবং জমির নিষ্কাশন নালা পরিষ্কার রাখুন।"
                )
            }
            else -> {
                Pair(
                    "Low rain probability ($maxRainProb24h%, est. $totalRainMmLabel in 24h; 3-day max $maxRainProb3d%). No immediate rainfall runoff risk for ${crop.nameEn}.",
                    "আগামী ২৪ ঘণ্টায় বৃষ্টির সম্ভাবনা কম ($maxRainProb24h%, সম্ভাব্য $totalRainMmLabel; ৩ দিনের সর্বোচ্চ $maxRainProb3d%)। ${crop.nameBn} ক্ষেতে তাৎক্ষণিক জলাবদ্ধতার ঝুঁকি নেই।"
                )
            }
        }

        // 2. Irrigation Timing
        val (irrigationEn, irrigationBn) = when {
            maxRainProb24h >= 55 || totalRainMm24h >= 5.0 -> {
                Pair(
                    "Postpone supplemental irrigation for ${crop.nameEn}. Forecasted rain ($maxRainProb24h% chance, ~$totalRainMmLabel) can supply natural soil moisture and help avoid over-saturation and unnecessary pump fuel/electricity costs.",
                    "${crop.nameBn} ক্ষেতে এখন সেচ দেওয়া থেকে বিরত থাকুন। পূর্বাভাস অনুযায়ী বৃষ্টির সম্ভাবনা ($maxRainProb24h%, প্রায় $totalRainMmLabel) থাকায় প্রাকৃতিকভাবেই মাটির আর্দ্রতা পূরণ হতে পারে এবং অতিরিক্ত পানি ও সেচের খরচ বাঁচবে।"
                )
            }
            highTempC >= 34.0 && maxRainProb24h < 35 -> {
                Pair(
                    "Warm & dry conditions (High $highFormatted, Low $lowFormatted, Rain chance $maxRainProb24h%, Humidity $avgHumidity%). Schedule light irrigation for ${crop.nameEn} during early morning (05:30–08:30) or late afternoon (16:30–18:30) to minimize evaporation loss.",
                    "উষ্ণ ও শুষ্ক আবহাওয়া (সর্বোচ্চ তাপমাত্রা $highFormatted, বৃষ্টির সম্ভাবনা মাত্র $maxRainProb24h%, আর্দ্রতা $avgHumidity%)। পানির অপচয় কমাতে ভোরবেলা (সকাল ৫:৩০–৮:৩০) অথবা বিকেলে (৪:৩০–৬:৩০) ${crop.nameBn} ক্ষেতে হালকা সেচ দেওয়ার উপযুক্ত সময়।"
                )
            }
            else -> {
                Pair(
                    "Moderate evapotranspiration (Temp $tempFormatted, Humidity $avgHumidity%, Rain chance $maxRainProb24h%). Check topsoil moisture (2–5 cm depth) in your ${crop.nameEn} plot; irrigate in the cool morning hours only if the root zone feels dry.",
                    "মাঝারি তাপমাত্রা ও আর্দ্রতা (তাপমাত্রা $tempFormatted, আর্দ্রতা $avgHumidity%, বৃষ্টির সম্ভাবনা $maxRainProb24h%)। ${crop.nameBn} ক্ষেতের উপরিভাগের মাটির রস পরীক্ষা করে প্রয়োজন হলে সকালের দিকে হালকা সেচ দিন।"
                )
            }
        }

        // 3. Drying & Post-Harvest Conditions
        val (dryingEn, dryingBn) = when {
            maxRainProb24h < 30 && avgHumidity <= crop.dryingHumidityCeilingPercent && cur.uvIndex != null && cur.uvIndex >= 4.0 -> {
                Pair(
                    "Favorable open-air sun drying window (Rain chance $maxRainProb24h%, Humidity $avgHumidity%, UV ${WeatherFormatter.formatNullableUv(cur.uvIndex)}). Suitable for sun-drying harvested paddy, jute fibers, wheat, or seeds on clean dry mats between 09:30 and 15:30.",
                    "ফসল ও বীজ রোদে শুকানোর জন্য অনুকূল আবহাওয়া (বৃষ্টির সম্ভাবনা $maxRainProb24h%, আর্দ্রতা $avgHumidity%)। সকাল ৯:৩০ থেকে বিকেল ৩:৩০ পর্যন্ত ধান, পাটের আঁশ, গম বা বীজ শুকানোর উপযুক্ত সময়।"
                )
            }
            maxRainProb24h >= 50 || avgHumidity >= 82 -> {
                Pair(
                    "Poor outdoor drying conditions (Rain probability $maxRainProb24h%, Humidity $avgHumidity%). Avoid spreading harvested ${crop.nameEn} in open courtyards without tarpaulin/polythene covers ready, and keep storage areas ventilated.",
                    "খোলা জায়গায় ফসল শুকানোর জন্য প্রতিকূল আবহাওয়া (বৃষ্টির সম্ভাবনা $maxRainProb24h%, বাতাসের আর্দ্রতা $avgHumidity%)। ত্রিপল বা পলিথিন ছাড়া খোলা মাঠে ফসল ছড়িয়ে রাখবেন না এবং গুদামঘরে বাতাস চলাচলের ব্যবস্থা রাখুন।"
                )
            }
            else -> {
                Pair(
                    "Mixed drying conditions (Rain chance $maxRainProb24h%, Humidity $avgHumidity%, High $highFormatted). Short sun-drying is possible during midday, but keep protective covers nearby in case of passing cloud bursts.",
                    "ফসল শুকানোর জন্য মাঝারি আবহাওয়া (বৃষ্টির সম্ভাবনা $maxRainProb24h%, আর্দ্রতা $avgHumidity%, সর্বোচ্চ $highFormatted)। দুপুরের রোদে অল্প সময়ের জন্য ফসল শুকানো যেতে পারে, তবে হঠাৎ বৃষ্টির জন্য ত্রিপল প্রস্তুত রাখুন।"
                )
            }
        }

        // 4. Suitable Windows for Outdoor Farm Activities
        val (activityEn, activityBn) = when {
            maxWind24h >= 35.0 || maxRainProb24h >= 70 -> {
                Pair(
                    "Caution for outdoor operations: Peak wind $maxWindFormatted (current $windFormatted) and $maxRainProb24h% rain chance. Avoid tall orchard ladder work, bamboo staking in strong gusts, or foliar care that could be washed off. Best relative slots: $calmWindowLabels.",
                    "মাঠের কাজে সতর্কতা: বাতাসের সর্বোচ্চ গতি $maxWindFormatted (বর্তমানে $windFormatted) এবং বৃষ্টির সম্ভাবনা $maxRainProb24h%। ঝড়ো বাতাস ও বৃষ্টির সময় উঁচু গাছে ওঠা বা স্প্রে করা থেকে বিরত থাকুন। তুলনামূলক অনুকূল সময়: $calmWindowLabels।"
                )
            }
            else -> {
                Pair(
                    "Suitable windows available for weeding, mulching, harvesting, and routine field inspection (Current wind $windFormatted, 24h peak $maxWindFormatted). Recommended low-wind/low-rain hours: $calmWindowLabels.",
                    "আগাছা পরিষ্কার, মালচিং, ফসল কাটা ও মাঠ পরিদর্শনের জন্য অনুকূল সময় রয়েছে (বর্তমান বাতাস $windFormatted, ২৪ ঘণ্টার সর্বোচ্চ $maxWindFormatted)। কাজের জন্য উপযুক্ত সময়সীমা: $calmWindowLabels।"
                )
            }
        }

        // 5. Forecast Uncertainty Note (from multi-model comparison when available)
        val confidence = bundle.confidenceReport
        val todayComparison = confidence?.dailyComparisons?.firstOrNull()
        val (uncertaintyEn, uncertaintyBn) = if (todayComparison != null && todayComparison.models.size >= 2) {
            val modelsList = todayComparison.models.joinToString(", ") { it.modelName }
            val tempSpread = todayComparison.maxTempSpreadC ?: 0.0
            val rainSpread = todayComparison.precipSpreadPercent ?: 0
            Pair(
                "Forecast Uncertainty (${todayComparison.agreementLevel.labelEn}): Compared across ${todayComparison.models.size} models ($modelsList). Today's temperature spread is ±${String.format(Locale.US, "%.1f", tempSpread / 2.0)}°C (Δ${tempSpread}°C) and rain probability spread is Δ${rainSpread}%. Weather forecasts carry inherent physical uncertainty and are never guaranteed.",
                "পূর্বাভাসের অনিশ্চয়তা (${todayComparison.agreementLevel.labelBn}): ${todayComparison.models.size}টি আবহাওয়া মডেল ($modelsList) তুলনা করা হয়েছে। আজকের তাপমাত্রার পার্থক্য Δ${tempSpread}°C এবং বৃষ্টির সম্ভাবনার পার্থক্য Δ${rainSpread}%। আবহাওয়ার পূর্বাভাসে সবসময় কিছুটা অনিশ্চয়তা থাকে, তাই এটি শতভাগ নিশ্চিত নয়।"
            )
        } else {
            Pair(
                "Forecast Uncertainty: Based on single-source Open-Meteo numerical weather telemetry (${WeatherFormatter.formatRelativeUpdateTime(bundle.fetchedAtMillis)}). Convective showers in South Asia can form locally within 1–3 hours; re-check radar before major field work.",
                "পূর্বাভাসের অনিশ্চয়তা: ওপেন-মেটিও আবহাওয়া তথ্যের ওপর ভিত্তি করে তৈরি (${WeatherFormatter.formatRelativeUpdateTime(bundle.fetchedAtMillis)})। স্থানীয়ভাবে ১-৩ ঘণ্টার মধ্যে মেঘ ও বৃষ্টি পরিবর্তন হতে পারে, তাই বড় কাজের আগে রাডার ও সর্বশেষ পূর্বাভাস দেখে নিন।"
            )
        }

        val expertNoticeEn = "Agronomy & Safety Boundary: Recommendations are strictly weather-timing aids and never guarantee crop outcomes. Atmosyn never prescribes pesticide or chemical dosages. For pest identification, fertilizer rates, or local salinity/flood advisories, consult your local Upazila Agriculture Office (DAE / কৃষি সম্প্রসারণ অধিদপ্তর) or call 16123."
        val expertNoticeBn = "কৃষি ও নিরাপত্তা নির্দেশিকা: এই পরামর্শগুলো শুধুমাত্র আবহাওয়া-ভিত্তিক সময় নির্ধারণের সহায়িকা—এটি ফসলের ফলনের কোনো নিশ্চয়তা দেয় না এবং কোনো কীটনাশক বা রাসায়নিকের মাত্রা নির্ধারণ করে না। রোগবালাই দমন ও স্থানীয় কৃষি পরামর্শের জন্য উপজেলা কৃষি অফিস (কৃষি সম্প্রসারণ অধিদপ্তর - DAE) অথবা কৃষি কল সেন্টার ১৬১২৩-এ যোগাযোগ করুন।"

        return SmartFarmingAdvisory(
            location = location,
            crop = crop,
            isDataAvailable = true,
            rainfallGuidanceEn = rainEn,
            rainfallGuidanceBn = rainBn,
            irrigationGuidanceEn = irrigationEn,
            irrigationGuidanceBn = irrigationBn,
            dryingGuidanceEn = dryingEn,
            dryingGuidanceBn = dryingBn,
            outdoorActivityWindowsEn = activityEn,
            outdoorActivityWindowsBn = activityBn,
            uncertaintyExplanationEn = uncertaintyEn,
            uncertaintyExplanationBn = uncertaintyBn,
            expertGuidanceNoticeEn = expertNoticeEn,
            expertGuidanceNoticeBn = expertNoticeBn,
            updateTimestampLabel = "${location.cityName}, ${location.country} · ${WeatherFormatter.formatRelativeUpdateTime(bundle.fetchedAtMillis)}${if (bundle.isFromCache) " (Cached)" else " (Live API)"}"
        )
    }

    /**
     * Evaluates user-configured personalized weather thresholds against live weather telemetry.
     * Clearly labels every triggered alert as a user-configured threshold alert, never fabricating
     * official government weather warnings.
     */
    fun evaluatePersonalThresholdAlerts(
        activeBundle: WeatherBundle?,
        additionalFavoriteBundles: List<WeatherBundle> = emptyList(),
        settings: UserSettings
    ): List<PersonalThresholdAlert> {
        if (!settings.personalizedAlertsEnabled) return emptyList()

        val bundlesToCheck = buildList {
            if (activeBundle != null) add(activeBundle)
            if (settings.monitorAllFavoriteCities) {
                additionalFavoriteBundles.forEach { fav ->
                    if (none { it.location.id == fav.location.id }) {
                        add(fav)
                    }
                }
            }
        }

        val triggered = mutableListOf<PersonalThresholdAlert>()

        bundlesToCheck.forEach { bundle ->
            val city = bundle.location.cityName
            val cur = bundle.current
            val evalLabel = WeatherFormatter.formatRelativeUpdateTime(bundle.fetchedAtMillis)
            val next12hRainMax = bundle.hourly
                .filter { !it.isHistoricalObservation }
                .take(12)
                .mapNotNull { it.precipProbabilityPercent }
                .maxOrNull()

            val effectiveRainProb = listOfNotNull(cur.precipProbabilityPercent, next12hRainMax).maxOrNull()
            if (effectiveRainProb != null && effectiveRainProb >= settings.rainAlertThresholdPercent) {
                triggered += PersonalThresholdAlert(
                    id = "personal_rain_${bundle.location.id}",
                    locationName = city,
                    metricCategory = "Rain Probability Threshold Reached",
                    triggeredValueLabel = "$effectiveRainProb% (Current/Next 12h Peak)",
                    thresholdValueLabel = "User Threshold: >= ${settings.rainAlertThresholdPercent}%",
                    messageEn = "Rain probability in $city ($effectiveRainProb%) meets or exceeds your custom ${settings.rainAlertThresholdPercent}% alert threshold. Consider carrying an umbrella or rain protection.",
                    messageBn = "$city-তে বৃষ্টির সম্ভাবনা ($effectiveRainProb%) আপনার নির্ধারিত ${settings.rainAlertThresholdPercent}% সীমা অতিক্রম করেছে। বাইরে বের হলে ছাতা বা রেইনকোট সাথে রাখুন।",
                    evaluatedAtLabel = evalLabel,
                    sourceBadge = "Personalized User Threshold · Not an Official Government Warning"
                )
            }

            val maxTempC = maxOf(cur.temperatureC, cur.highC)
            if (maxTempC >= settings.tempAlertThresholdC) {
                val obsTemp = WeatherFormatter.formatTemp(maxTempC, settings.tempUnit)
                val threshTemp = WeatherFormatter.formatTemp(settings.tempAlertThresholdC.toDouble(), settings.tempUnit)
                triggered += PersonalThresholdAlert(
                    id = "personal_temp_${bundle.location.id}",
                    locationName = city,
                    metricCategory = "High Temperature Threshold Reached",
                    triggeredValueLabel = "$obsTemp (Observed/Daily High)",
                    thresholdValueLabel = "User Threshold: >= $threshTemp",
                    messageEn = "Temperature in $city ($obsTemp) meets or exceeds your custom $threshTemp threshold. Stay hydrated and limit direct midday sun exposure.",
                    messageBn = "$city-তে তাপমাত্রা ($obsTemp) আপনার নির্ধারিত $threshTemp সীমা অতিক্রম করেছে। পর্যাপ্ত পানি পান করুন এবং দুপুরের কড়া রোদ এড়িয়ে চলুন।",
                    evaluatedAtLabel = evalLabel,
                    sourceBadge = "Personalized User Threshold · Not an Official Government Warning"
                )
            }

            val maxWindKmh = maxOf(
                cur.windSpeedKmh,
                bundle.hourly.filter { !it.isHistoricalObservation }.take(12).maxOfOrNull { it.windSpeedKmh } ?: cur.windSpeedKmh
            )
            if (maxWindKmh >= settings.windAlertThresholdKmh) {
                val obsWind = WeatherFormatter.formatWind(maxWindKmh, settings.windUnit)
                val threshWind = WeatherFormatter.formatWind(settings.windAlertThresholdKmh.toDouble(), settings.windUnit)
                triggered += PersonalThresholdAlert(
                    id = "personal_wind_${bundle.location.id}",
                    locationName = city,
                    metricCategory = "Wind Speed Threshold Reached",
                    triggeredValueLabel = "$obsWind (${maxWindKmh.roundToInt()} km/h)",
                    thresholdValueLabel = "User Threshold: >= $threshWind",
                    messageEn = "Wind speed in $city ($obsWind) meets or exceeds your custom $threshWind threshold. Secure loose outdoor items and use caution in open areas.",
                    messageBn = "$city-তে বাতাসের গতিবেগ ($obsWind) আপনার নির্ধারিত $threshWind সীমা অতিক্রম করেছে। খোলা জায়গায় ও নদীপথে চলাচলে সতর্ক থাকুন।",
                    evaluatedAtLabel = evalLabel,
                    sourceBadge = "Personalized User Threshold · Not an Official Government Warning"
                )
            }

            val currentAqi = bundle.airQuality?.usAqi
            if (currentAqi != null && currentAqi >= settings.aqiAlertThresholdUs) {
                val catLabel = WeatherFormatter.aqiCategoryLabel(currentAqi)
                triggered += PersonalThresholdAlert(
                    id = "personal_aqi_${bundle.location.id}",
                    locationName = city,
                    metricCategory = "Air Quality (US AQI) Threshold Reached",
                    triggeredValueLabel = "US AQI $currentAqi ($catLabel)",
                    thresholdValueLabel = "User Threshold: >= US AQI ${settings.aqiAlertThresholdUs}",
                    messageEn = "Air quality in $city (US AQI $currentAqi) meets or exceeds your custom threshold (${settings.aqiAlertThresholdUs}). Sensitive groups should reduce prolonged outdoor exertion.",
                    messageBn = "$city-তে বায়ুর মান (US AQI $currentAqi) আপনার নির্ধারিত ${settings.aqiAlertThresholdUs} সীমা অতিক্রম করেছে। বাইরে দীর্ঘক্ষণ পরিশ্রমের কাজ কমান এবং প্রয়োজনে মাস্ক ব্যবহার করুন।",
                    evaluatedAtLabel = evalLabel,
                    sourceBadge = "Personalized User Threshold · Not an Official Government Warning"
                )
            }
        }

        return triggered
    }

    /**
     * Checks whether the app currently holds runtime permission to post local Android notifications.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            runCatching {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }.getOrDefault(true)
        }
    }

    /**
     * Dispatches a real Android local notification for a triggered [PersonalThresholdAlert] when
     * `POST_NOTIFICATIONS` permission is granted. Clearly badges the notification as a
     * user-configured threshold alert rather than an official government warning.
     */
    fun dispatchPersonalAlertNotification(
        context: Context,
        alert: PersonalThresholdAlert
    ): Boolean {
        if (!hasNotificationPermission(context)) return false

        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    PERSONAL_ALERT_CHANNEL_ID,
                    PERSONAL_ALERT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "User-configured weather threshold notifications (Rain %, Temp, Wind, US AQI)"
                }
                manager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, PERSONAL_ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${alert.locationName}: ${alert.metricCategory}")
                .setContentText("${alert.triggeredValueLabel} (${alert.thresholdValueLabel})")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${alert.messageEn}\n\n${alert.messageBn}\n\n[${alert.sourceBadge}]")
                )
                .setSubText("Custom User Threshold")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            manager.notify(alert.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
