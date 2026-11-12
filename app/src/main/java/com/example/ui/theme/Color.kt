package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Bukhari Mosque Brand Palette (Emerald & Teal)
val Primary900 = Color(0xFF00201D)
val Primary800 = Color(0xFF003833)
val Primary700 = Color(0xFF005C55) // Main Brand Color
val Primary600 = Color(0xFF0F766E)
val Primary500 = Color(0xFF14B8A6)
val Primary200 = Color(0xFF99F6E4)
val Primary100 = Color(0xFFCCFBF1)
val Primary50 = Color(0xFFF0FDFA)

// Gold / Accent
val GoldPrimary = Color(0xFFD97706)
val GoldLight = Color(0xFFFEF3C7)
val GoldDark = Color(0xFF92400E)

// Backgrounds & Neutrals
val AppBackground = Color(0xFFF8F9FD)
val SurfacePure = Color(0xFFFFFFFF)
val SurfaceSubtle = Color(0xFFF1F5F9)
val TextMain = Color(0xFF0D1C2E)
val TextMuted = Color(0xFF64748B)
val BorderSoft = Color(0xFFE2E8F0)
val BorderActive = Color(0xFFCBD5E1)

// Priority Colors
val PriorityUrgentBg = Color(0xFFFEE2E2)
val PriorityUrgentText = Color(0xFFB91C1C)
val PriorityUrgentDot = Color(0xFFEF4444)

val PriorityHighBg = Color(0xFFFEF3C7)
val PriorityHighText = Color(0xFFB45309)
val PriorityHighDot = Color(0xFFF59E0B)

val PriorityNormalBg = Color(0xFFDCFCE7)
val PriorityNormalText = Color(0xFF15803D)
val PriorityNormalDot = Color(0xFF22C55E)

// 9 Stages Color Definitions
data class StageColorSet(
    val key: String,
    val name: String,
    val activeLabel: String,
    val emoji: String,
    val bg: Color,
    val text: Color,
    val border: Color,
    val dot: Color
)

val StageColorMap = mapOf(
    "registration" to StageColorSet(
        key = "registration",
        name = "التسجيل",
        activeLabel = "تم التسجيل",
        emoji = "📝",
        bg = Color(0xFFF1F5F9),
        text = Color(0xFF334155),
        border = Color(0xFFCBD5E1),
        dot = Color(0xFF94A3B8)
    ),
    "exam" to StageColorSet(
        key = "exam",
        name = "الامتحان",
        activeLabel = "في انتظار الامتحان",
        emoji = "🟦",
        bg = Color(0xFFEFF6FF),
        text = Color(0xFF1D4ED8),
        border = Color(0xFFBFDBFE),
        dot = Color(0xFF3B82F6)
    ),
    "scoring" to StageColorSet(
        key = "scoring",
        name = "التصحيح",
        activeLabel = "تم الامتحان / التصحيح",
        emoji = "🟠",
        bg = Color(0xFFFFFBEB),
        text = Color(0xFFB45309),
        border = Color(0xFFFDE68A),
        dot = Color(0xFFF59E0B)
    ),
    "aggregation" to StageColorSet(
        key = "aggregation",
        name = "تجميع الدرجات",
        activeLabel = "في التجميع",
        emoji = "🔵",
        bg = Color(0xFFEEF2FF),
        text = Color(0xFF4338CA),
        border = Color(0xFFC7D2FE),
        dot = Color(0xFF6366F1)
    ),
    "review" to StageColorSet(
        key = "review",
        name = "المراجعة",
        activeLabel = "في المراجعة والتدقيق",
        emoji = "🟡",
        bg = Color(0xFFFEFCE8),
        text = Color(0xFF854D0E),
        border = Color(0xFFFEF08A),
        dot = Color(0xFFEAB308)
    ),
    "sheet" to StageColorSet(
        key = "sheet",
        name = "كتابة الكشف",
        activeLabel = "كتابة الكشف",
        emoji = "🟣",
        bg = Color(0xFFFAF5FF),
        text = Color(0xFF7E22CE),
        border = Color(0xFFE9D5FF),
        dot = Color(0xFFA855F7)
    ),
    "accreditation" to StageColorSet(
        key = "accreditation",
        name = "الاعتمادية",
        activeLabel = "قيد الاعتمادية",
        emoji = "📜",
        bg = Color(0xFFECFDF5),
        text = Color(0xFF065F46),
        border = Color(0xFFA7F3D0),
        dot = Color(0xFF059669)
    ),
    "announcement" to StageColorSet(
        key = "announcement",
        name = "إعلان النتيجة",
        activeLabel = "إعلان النتيجة",
        emoji = "📢",
        bg = Color(0xFFECFEFF),
        text = Color(0xFF0E7490),
        border = Color(0xFFA5F3FC),
        dot = Color(0xFF06B6D4)
    ),
    "ceremony" to StageColorSet(
        key = "ceremony",
        name = "الحفل",
        activeLabel = "تم الحفل والتكريم",
        emoji = "🟢",
        bg = Color(0xFFF0FDF4),
        text = Color(0xFF15803D),
        border = Color(0xFFBBF7D0),
        dot = Color(0xFF22C55E)
    )
)
