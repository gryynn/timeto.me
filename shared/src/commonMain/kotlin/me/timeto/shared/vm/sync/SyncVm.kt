package me.timeto.shared.vm.sync

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.timeto.shared.launchExIo
import me.timeto.shared.sync.*
import me.timeto.shared.time
import me.timeto.shared.vm.Vm
import me.timeto.shared.zlog

/**
 * ViewModel pour la synchronisation Supabase
 * 
 * Gère :
 * - État de la sync (IDLE, SYNCING, SUCCESS, ERROR)
 * - Trigger de la sync manuelle
 * - Status et erreurs
 */
class SyncVm : Vm<SyncVm.State>() {

    data class State(
        val status: SyncStatus = SyncStatus.IDLE,
        val lastSyncTime: Long = 0L,
        val lastResult: SyncResult? = null,
        val isConfigured: Boolean = false,
        val isSyncing: Boolean = false
    ) {
        val statusMessage: String
            get() = when (status) {
                SyncStatus.IDLE -> if (lastSyncTime > 0) "Last sync: ${formatTime(lastSyncTime)}" else "Not synced yet"
                SyncStatus.SYNCING -> "Syncing..."
                SyncStatus.SUCCESS -> "Sync completed (${lastResult?.itemsSynced ?: 0} items)"
                SyncStatus.ERROR -> "Sync failed: ${lastResult?.errors?.firstOrNull() ?: "Unknown error"}"
            }
        
        private fun formatTime(timestamp: Long): String {
            // Simple format - peut être amélioré
            val now = time()
            val diff = now - timestamp
            return when {
                diff < 60L -> "just now"
                diff < 3600L -> "${diff / 60}m ago"
                diff < 86400L -> "${diff / 3600}h ago"
                else -> "${diff / 86400}d ago"
            }
        }
    }

    override val state = MutableStateFlow(State())

    private val repository = SupabaseRepository()

    init {
        val scope = scopeVm()
        
        // Observe la configuration
        SupabaseConfig.isConfiguredFlow
            .onEach { isConfigured ->
                state.update { it.copy(isConfigured = isConfigured) }
            }
            .launchIn(scope)
        
        // Observe le temps de dernière sync
        SupabaseConfig.lastSyncTimeFlow
            .onEach { lastSyncTime ->
                state.update { it.copy(lastSyncTime = lastSyncTime) }
            }
            .launchIn(scope)
    }

    /**
     * Lance une synchronisation manuelle
     */
    fun syncNow() {
        if (state.value.isSyncing) {
            zlog("Sync already in progress")
            return
        }
        
        if (!state.value.isConfigured) {
            state.update { 
                it.copy(
                    status = SyncStatus.ERROR,
                    lastResult = SyncResult.failure("Supabase not configured", 0, time())
                )
            }
            return
        }
        
        launchExIo {
            state.update { it.copy(isSyncing = true, status = SyncStatus.SYNCING) }
            
            try {
                // Test de connexion d'abord
                val connectionTest = repository.testConnection()
                if (connectionTest.isFailure) {
                    val error = connectionTest.exceptionOrNull()?.message ?: "Connection failed"
                    state.update {
                        it.copy(
                            isSyncing = false,
                            status = SyncStatus.ERROR,
                            lastResult = SyncResult.failure(error, 0, time())
                        )
                    }
                    return@launchExIo
                }
                
                // Sync complète
                val result = repository.syncFull()
                
                state.update {
                    it.copy(
                        isSyncing = false,
                        status = if (result.success) SyncStatus.SUCCESS else SyncStatus.ERROR,
                        lastResult = result,
                        lastSyncTime = if (result.success) result.timestamp else it.lastSyncTime
                    )
                }
                
                if (result.success) {
                    zlog("✅ Sync successful: ${result.itemsSynced} items")
                } else {
                    zlog("❌ Sync failed: ${result.errors}")
                }
                
            } catch (e: Exception) {
                zlog("Sync exception: ${e.message}")
                state.update {
                    it.copy(
                        isSyncing = false,
                        status = SyncStatus.ERROR,
                        lastResult = SyncResult.failure(e.message ?: "Unknown error", 0, time())
                    )
                }
            }
        }
    }

    /**
     * Réinitialise le status (après affichage d'une erreur par exemple)
     */
    fun resetStatus() {
        state.update { it.copy(status = SyncStatus.IDLE) }
    }
}

