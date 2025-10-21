package me.timeto.shared.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import me.timeto.shared.db.KvDb
import me.timeto.shared.db.db
import me.timeto.shared.db.dbIo

/**
 * Configuration Supabase stockée dans KvDb
 * 
 * Utilise directement les queries SQLDelight car KvDb.KEY est un enum limité.
 * Les clés sont stockées comme des strings dans la table KVSQ.
 */
object SupabaseConfig {
    
    // Keys pour KvDb
    private const val KEY_SUPABASE_URL = "supabase_url"
    private const val KEY_SUPABASE_KEY = "supabase_key"
    private const val KEY_SUPABASE_ENABLED = "supabase_enabled"
    private const val KEY_LAST_SYNC_TIME = "supabase_last_sync"
    private const val KEY_SYNC_INTERVAL_HOURS = "supabase_sync_interval_hours"
    private const val KEY_SYNC_PERIOD_DAYS = "supabase_sync_period_days"
    
    // Flows réactifs pour l'UI
    val urlFlow: Flow<String?> = getFlowFor(KEY_SUPABASE_URL)
    val keyFlow: Flow<String?> = getFlowFor(KEY_SUPABASE_KEY)
    val isEnabledFlow: Flow<Boolean> = getFlowFor(KEY_SUPABASE_ENABLED).map { it == "1" }
    val syncIntervalHoursFlow: Flow<Int> = getFlowFor(KEY_SYNC_INTERVAL_HOURS).map { it?.toIntOrNull() ?: 6 }
    val syncPeriodDaysFlow: Flow<Int> = getFlowFor(KEY_SYNC_PERIOD_DAYS).map { it?.toIntOrNull() ?: 7 }
    val lastSyncTimeFlow: Flow<Long> = getFlowFor(KEY_LAST_SYNC_TIME).map { it?.toLongOrNull() ?: 0L }
    
    val isConfiguredFlow: Flow<Boolean> = combine(
        urlFlow, 
        keyFlow, 
        isEnabledFlow
    ) { url, key, enabled ->
        !url.isNullOrBlank() && !key.isNullOrBlank() && enabled
    }
    
    // Helper pour créer un Flow
    private fun getFlowFor(key: String): Flow<String?> =
        db.kVQueries.selectByKey(key)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.value_ }
    
    // Getters synchrones
    suspend fun getUrl(): String? = getValueFor(KEY_SUPABASE_URL)
    suspend fun getKey(): String? = getValueFor(KEY_SUPABASE_KEY)
    suspend fun isEnabled(): Boolean = getValueFor(KEY_SUPABASE_ENABLED) == "1"
    suspend fun getLastSyncTime(): Long = getValueFor(KEY_LAST_SYNC_TIME)?.toLongOrNull() ?: 0L
    
    // Helper pour récupérer une valeur
    private suspend fun getValueFor(key: String): String? = dbIo {
        db.kVQueries.selectByKey(key).executeAsOneOrNull()?.value_
    }
    
    /**
     * Configure Supabase avec validation
     */
    suspend fun configure(url: String, key: String) {
        require(url.isNotBlank()) { "Supabase URL cannot be blank" }
        require(key.isNotBlank()) { "Supabase API Key cannot be blank" }
        require(url.startsWith("https://")) { "Supabase URL must use HTTPS" }
        require(url.contains(".supabase.co")) { "Invalid Supabase URL format" }
        
        dbIo {
            db.kVQueries.upsert(key = KEY_SUPABASE_URL, value_ = url.trim())
            db.kVQueries.upsert(key = KEY_SUPABASE_KEY, value_ = key.trim())
            db.kVQueries.upsert(key = KEY_SUPABASE_ENABLED, value_ = "1")
        }
    }
    
    /**
     * Désactive la sync (garde les credentials)
     */
    suspend fun disable() = dbIo {
        db.kVQueries.upsert(key = KEY_SUPABASE_ENABLED, value_ = "0")
    }
    
    /**
     * Active la sync (si credentials déjà configurés)
     */
    suspend fun enable() {
        require(!getUrl().isNullOrBlank()) { "Supabase URL not configured" }
        require(!getKey().isNullOrBlank()) { "Supabase API Key not configured" }
        dbIo {
            db.kVQueries.upsert(key = KEY_SUPABASE_ENABLED, value_ = "1")
        }
    }
    
    /**
     * Met à jour le timestamp de dernière sync
     */
    suspend fun updateLastSyncTime(time: Long) = dbIo {
        db.kVQueries.upsert(key = KEY_LAST_SYNC_TIME, value_ = time.toString())
    }

    /**
     * Configure la fréquence de sync (heures)
     */
    suspend fun setSyncIntervalHours(hours: Int) = dbIo {
        require(hours > 0) { "Sync interval must be positive" }
        db.kVQueries.upsert(key = KEY_SYNC_INTERVAL_HOURS, value_ = hours.toString())
    }

    /**
     * Configure la période de sync (jours)
     */
    suspend fun setSyncPeriodDays(days: Int) = dbIo {
        require(days > 0) { "Sync period must be positive" }
        db.kVQueries.upsert(key = KEY_SYNC_PERIOD_DAYS, value_ = days.toString())
    }

    /**
     * Supprime toute la configuration
     */
    suspend fun clearConfig() = dbIo {
        db.kVQueries.deleteByKey(KEY_SUPABASE_URL)
        db.kVQueries.deleteByKey(KEY_SUPABASE_KEY)
        db.kVQueries.deleteByKey(KEY_SUPABASE_ENABLED)
        db.kVQueries.deleteByKey(KEY_LAST_SYNC_TIME)
        db.kVQueries.deleteByKey(KEY_SYNC_INTERVAL_HOURS)
        db.kVQueries.deleteByKey(KEY_SYNC_PERIOD_DAYS)
    }
}

