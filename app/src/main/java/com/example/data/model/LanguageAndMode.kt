package com.example.data.model

enum class NoteLanguage(val code: String, val displayName: String, val nativeName: String, val flag: String) {
    ENGLISH("en", "English", "English", "🇺🇸"),
    SPANISH("es", "Spanish", "Español", "🇪🇸"),
    HINDI("hi", "Hindi", "हिन्दी", "🇮🇳"),
    FRENCH("fr", "French", "Français", "🇫🇷"),
    GERMAN("de", "German", "Deutsch", "🇩🇪"),
    CHINESE("zh", "Chinese", "中文", "🇨🇳"),
    JAPANESE("ja", "Japanese", "日本語", "🇯🇵"),
    KOREAN("ko", "Korean", "한국어", "🇰🇷"),
    ARABIC("ar", "Arabic", "العربية", "🇸🇦"),
    BENGALI("bn", "Bengali", "বাংলা", "🇧🇩"),
    PORTUGUESE("pt", "Portuguese", "Português", "🇧🇷"),
    RUSSIAN("ru", "Russian", "Русский", "🇷🇺"),
    ITALIAN("it", "Italian", "Italiano", "🇮🇹"),
    URDU("ur", "Urdu", "اردو", "🇵🇰"),
    TURKISH("tr", "Turkish", "Türkçe", "🇹🇷"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
    DUTCH("nl", "Dutch", "Nederlands", "🇳🇱");

    companion object {
        fun fromCode(code: String): NoteLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

enum class NoteMode(val id: String, val title: String, val description: String) {
    EXAM_TUTORIAL(
        "exam_tutorial",
        "Exam & Tutorial Study Guide",
        "Structured for test prep: key definitions, likely exam questions, step-by-step proofs & cheatsheet formulas"
    ),
    STANDARD_COMPREHENSIVE(
        "standard",
        "Comprehensive Tutorial & Code",
        "In-depth concept deconstruction, runnable code blocks, and real-world implementation walk-through"
    ),
    QUICK_RECAP(
        "quick_recap",
        "Quick High-Yield Recap",
        "Rapid review summary and top high-yield takeaways for last-minute study sessions"
    )
}
