package com.saathi.app.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

data class SupportedLanguage(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val scriptChar: String
)

object AppLocaleManager {
    private const val PREFS_NAME = "sevacare_locale_fast"
    private const val KEY_FAST_TAG = "fast_locale_tag"

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("en", "English", "English", "A"),
        SupportedLanguage("hi", "हिंदी", "Hindi", "अ"),
        SupportedLanguage("bn", "বাংলা", "Bengali", "অ"),
        SupportedLanguage("ta", "தமிழ்", "Tamil", "அ"),
        SupportedLanguage("te", "తెలుగు", "Telugu", "అ"),
        SupportedLanguage("mr", "मराठी", "Marathi", "म")
    )

    fun getLocaleFlow(context: Context): Flow<String> = callbackFlow {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FAST_TAG) {
                trySend(prefs.getString(KEY_FAST_TAG, "en") ?: "en")
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString(KEY_FAST_TAG, "en") ?: "en")
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    /**
     * Cold-start initialization called in Application.onCreate and MainActivity.onCreate
     * before the view hierarchy is drawn.
     */
    fun initializeLocale(context: Context) {
        val fastPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTag = fastPrefs.getString(KEY_FAST_TAG, "en") ?: "en"
        fastPrefs.edit().putString(KEY_FAST_TAG, savedTag).apply()
        applyLocales(context, savedTag)
    }

    /**
     * Switches the application locale, persists to SharedPreferences,
     * updates system/AppCompat locales, and recreates the host activity if provided.
     */
    fun setLocale(context: Context, tag: String, activity: Activity? = null) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAST_TAG, tag)
            .apply()

        applyLocales(context, tag)
        activity?.recreate()
    }

    private fun applyLocales(context: Context, tag: String) {
        val targetLocale = Locale.forLanguageTag(tag)
        Locale.setDefault(targetLocale)

        val targetLocalesCompat = LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(targetLocalesCompat)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList.forLanguageTags(tag)
            } catch (_: Exception) {
            }
        }

        try {
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(targetLocale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (_: Exception) {
        }
    }

    fun getCurrentLocaleTag(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FAST_TAG, null)
            ?: LocaleManagerCompat.getApplicationLocales(context).toLanguageTags().ifBlank { "en" }
    }
}
