package me.timeto.app

import android.util.Log

/**
 * Configuration sécurisée pour Supabase.
 * Les clés sont chargées depuis BuildConfig, qui les obtient de local.properties.
 */
object SupabaseConfig {
    private const val TAG = "SupabaseConfig"

    /**
     * Indique si l'export Supabase est activé pour cette variante de build
     */
    val isEnabled: Boolean = BuildConfig.ENABLE_SUPABASE_EXPORT

    /**
     * URL de l'API Supabase, chargée de manière sécurisée depuis local.properties
     * @throws IllegalStateException si la configuration est invalide
     */
    val SUPABASE_URL: String by lazy {
        if (!isEnabled) return@lazy ""
        
        BuildConfig.SUPABASE_URL.also { url ->
            require(url.isNotBlank()) {
                "SUPABASE_URL n'est pas configuré dans local.properties"
            }
            require(url.startsWith("https://")) {
                "SUPABASE_URL doit commencer par https://"
            }
        }
    }

    /**
     * Clé API Supabase, chargée de manière sécurisée depuis local.properties
     * @throws IllegalStateException si la configuration est invalide
     */
    val SUPABASE_KEY: String by lazy {
        if (!isEnabled) return@lazy ""
        
        BuildConfig.SUPABASE_KEY.also { key ->
            require(key.isNotBlank()) {
                "SUPABASE_KEY n'est pas configuré dans local.properties"
            }
            require(key.length >= 30) {
                "SUPABASE_KEY semble invalide (trop courte)"
            }
        }
    }

    /**
     * Vérifie que la configuration Supabase est valide et utilisable
     * @return true si la configuration est valide et que l'export est activé
     */
    fun isConfigValid(): Boolean {
        if (!isEnabled) {
            Log.i(TAG, "Export Supabase désactivé pour cette variante de build")
            return false
        }

        return try {
            SUPABASE_URL.isNotBlank() && 
            SUPABASE_KEY.isNotBlank() &&
            SUPABASE_URL.startsWith("https://") &&
            SUPABASE_KEY.length >= 30
        } catch (e: Exception) {
            Log.e(TAG, "Configuration Supabase invalide", e)
            false
        }
    }

    /**
     * Vérifie la configuration et lance une exception si elle est invalide
     * @throws IllegalStateException si la configuration est invalide
     */
    fun requireValidConfig() {
        if (!isEnabled) {
            throw IllegalStateException("L'export Supabase est désactivé pour cette variante de build")
        }
        if (!isConfigValid()) {
            throw IllegalStateException("Configuration Supabase invalide. Vérifiez local.properties")
        }
    }
} 