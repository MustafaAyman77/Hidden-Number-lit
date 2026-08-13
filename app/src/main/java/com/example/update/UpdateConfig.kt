package com.example.update

/**
 * ✅ إعدادات نظام التحديث التلقائي
 */
object UpdateConfig {
    // ✅ قائمة الروابط الاحتياطية لملف الإصدارات (JSON)
    val MANIFEST_URLS = listOf(
        "https://raw.githubusercontent.com/MustafaAyman77/Hidden-Number-lit/main/version.json",
        "https://raw.githubusercontent.com/MustafaAyman77/Hidden-Number-lit/master/version.json",
        "https://cdn.jsdelivr.net/gh/MustafaAyman77/Hidden-Number-lit@main/version.json",
        "https://cdn.jsdelivr.net/gh/MustafaAyman77/Hidden-Number-lit@master/version.json",
        "https://raw.githubusercontent.com/MustafaAyman77/Hidden-Number-lite/main/version.json",
        "https://cdn.jsdelivr.net/gh/MustafaAyman77/Hidden-Number-lite@main/version.json"
    )

    const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/MustafaAyman77/Hidden-Number-lit/main/version.json"
    
    // ✅ اسم المستودع على GitHub
    const val GITHUB_REPO = "MustafaAyman77/Hidden-Number-lit"
    
    // ✅ مدة التحقق من التحديث (بالمللي ثانية) - 6 ساعات
    const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    
    // ✅ مهلة الاتصال (بالثواني)
    const val CONNECTION_TIMEOUT = 15
}
