package com.example.update

/**
 * ✅ إعدادات نظام التحديث التلقائي
 */
object UpdateConfig {
    // ✅ رابط ملف الإصدارات (JSON) - استخدم الرابط الخام (raw)
    const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/MustafaAyman77/Hidden-Number-lit/main/version.json"
    
    // ✅ اسم المستودع على GitHub
    const val GITHUB_REPO = "MustafaAyman77/Hidden-Number-lit"
    
    // ✅ مدة التحقق من التحديث (بالمللي ثانية) - 6 ساعات
    const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    
    // ✅ مهلة الاتصال (بالثواني)
    const val CONNECTION_TIMEOUT = 30
}
