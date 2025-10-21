package me.timeto.shared.vm.settings

import kotlinx.coroutines.flow.*
import me.timeto.shared.launchExIo
import me.timeto.shared.sync.*
import me.timeto.shared.vm.Vm
import me.timeto.shared.zlog

/**
 * ViewModel pour la configuration Supabase
 */
class SupabaseSettingsVm : Vm<SupabaseSettingsVm.State>() {

    data class State(
        val url: String = "",
        val key: String = "",
        val displayKey: String = "",  // Clé affichée (masquée ou réelle)
        val isEditingKey: Boolean = false,  // True si on édite la clé
        val isEnabled: Boolean = false,
        val isConfigured: Boolean = false,
        val lastSyncTime: Long = 0L,
        val isTestingConnection: Boolean = false,
        val testResult: String? = null,
        val syncIntervalHours: Int = 6,  // Fréquence de sync par défaut
        val syncPeriodDays: Int = 7,  // Période de sync par défaut
    ) {
        val isSaveEnabled: Boolean
            get() = url.isNotBlank() && key.isNotBlank()
        
        val lastSyncText: String
            get() = if (lastSyncTime > 0) {
                val now = time()
                val diff = now - lastSyncTime
                when {
                    diff < 60 -> "just now"
                    diff < 3600 -> "${diff / 60}m ago"
                    diff < 86400 -> "${diff / 3600}h ago"
                    else -> "${diff / 86400}d ago"
                }
            } else "Never"
    }

    override val state = MutableStateFlow(State())

    private val repository = SupabaseRepository()

    init {
        val scope = scopeVm()
        
        // Load config
        SupabaseConfig.urlFlow
            .combine(SupabaseConfig.keyFlow) { url, key ->
                url to key
            }
            .onEach { (url, key) ->
                state.update {
                    val actualKey = key ?: ""
                    val maskedKey = if (actualKey.isNotBlank()) "•".repeat(40) else ""
                    it.copy(
                        url = url ?: "",
                        key = actualKey,
                        displayKey = if (it.isEditingKey) actualKey else maskedKey,
                    )
                }
            }
            .launchIn(scope)
        
        SupabaseConfig.isEnabledFlow
            .onEach { isEnabled ->
                state.update { it.copy(isEnabled = isEnabled) }
            }
            .launchIn(scope)
        
        SupabaseConfig.isConfiguredFlow
            .onEach { isConfigured ->
                state.update { it.copy(isConfigured = isConfigured) }
            }
            .launchIn(scope)
        
        SupabaseConfig.lastSyncTimeFlow
            .onEach { lastSyncTime ->
                state.update { it.copy(lastSyncTime = lastSyncTime) }
            }
            .launchIn(scope)

        SupabaseConfig.syncIntervalHoursFlow
            .onEach { syncIntervalHours ->
                state.update { it.copy(syncIntervalHours = syncIntervalHours) }
            }
            .launchIn(scope)

        SupabaseConfig.syncPeriodDaysFlow
            .onEach { syncPeriodDays ->
                state.update { it.copy(syncPeriodDays = syncPeriodDays) }
            }
            .launchIn(scope)
    }

    fun setUrl(url: String) {
        state.update { it.copy(url = url.trim()) }
    }

    fun setKey(key: String) {
        state.update { 
            it.copy(
                key = key.trim(),
                displayKey = key.trim(),
                isEditingKey = true  // On est en train d'éditer
            ) 
        }
    }
    
    fun startEditingKey() {
        state.update { 
            it.copy(
                displayKey = it.key,  // Afficher la vraie clé
                isEditingKey = true
            ) 
        }
    }

    fun save(onSuccess: () -> Unit) {
        val currentState = state.value
        if (!currentState.isSaveEnabled) return
        
        launchExIo {
            try {
                SupabaseConfig.configure(
                    url = currentState.url,
                    key = currentState.key
                )
                zlog("✅ Supabase config saved")
                onSuccess()
            } catch (e: Exception) {
                zlog("❌ Failed to save config: ${e.message}")
                state.update { it.copy(testResult = "Error: ${e.message}") }
            }
        }
    }

    fun enable() {
        launchExIo {
            SupabaseConfig.enable()
            zlog("✅ Supabase sync enabled")
        }
    }

    fun disable() {
        launchExIo {
            SupabaseConfig.disable()
            zlog("❌ Supabase sync disabled")
        }
    }

    fun testConnection() {
        launchExIo {
            state.update { it.copy(isTestingConnection = true, testResult = null) }
            
            val result = repository.testConnection()
            
            state.update {
                it.copy(
                    isTestingConnection = false,
                    testResult = if (result.isSuccess) "✅ Connection successful!" else "❌ Connection failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun clearTestResult() {
        state.update { it.copy(testResult = null) }
    }

    fun clearConfig() {
        launchExIo {
            SupabaseConfig.clearConfig()
            zlog("🗑️ Supabase config cleared")
        }
    }

    fun setSyncPeriodDays(days: Int) {
        if (days < 1) return  // Minimum 1 jour
        launchExIo {
            SupabaseConfig.setSyncPeriodDays(days)
            state.update { it.copy(syncPeriodDays = days) }
        }
    }

    fun fullSync() {
        launchExIo {
            try {
                val result = repository.syncFull()
                state.update {
                    it.copy(
                        testResult = if (result.success)
                            "✅ Full sync completed: ${result.itemsSynced} items"
                        else
                            "❌ Full sync failed: ${result.errors?.firstOrNull() ?: "Unknown error"}"
                    )
                }
                zlog("Full sync ${if (result.success) "successful" else "failed"}")
            } catch (e: Exception) {
                state.update { it.copy(testResult = "❌ Full sync error: ${e.message}") }
                zlog("Full sync exception: ${e.message}")
            }
        }
    }

    fun syncNow() {
        launchExIo {
            try {
                val result = repository.syncIncremental(days = state.value.syncPeriodDays)
                state.update {
                    it.copy(
                        testResult = if (result.success)
                            "✅ Sync completed: ${result.itemsSynced} items"
                        else
                            "❌ Sync failed: ${result.errors?.firstOrNull() ?: "Unknown error"}"
                    )
                }
                zlog("Incremental sync ${if (result.success) "successful" else "failed"}")
            } catch (e: Exception) {
                state.update { it.copy(testResult = "❌ Sync error: ${e.message}") }
                zlog("Incremental sync exception: ${e.message}")
            }
        }
    }
}

