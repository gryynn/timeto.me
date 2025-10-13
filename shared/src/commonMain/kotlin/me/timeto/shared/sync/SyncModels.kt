package me.timeto.shared.sync

import kotlinx.serialization.Serializable

/**
 * Résultat d'une synchronisation
 */
@Serializable
data class SyncResult(
    val success: Boolean,
    val itemsSynced: Int = 0,
    val duration: Long = 0,
    val errors: List<String> = emptyList(),
    val timestamp: Long = 0
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
    
    companion object {
        fun success(itemsSynced: Int, duration: Long, timestamp: Long) = SyncResult(
            success = true,
            itemsSynced = itemsSynced,
            duration = duration,
            timestamp = timestamp,
            errors = emptyList()
        )
        
        fun failure(errors: List<String>, duration: Long, timestamp: Long) = SyncResult(
            success = false,
            itemsSynced = 0,
            duration = duration,
            timestamp = timestamp,
            errors = errors
        )
        
        fun failure(error: String, duration: Long, timestamp: Long) = failure(
            listOf(error), 
            duration, 
            timestamp
        )
    }
}

/**
 * Statistiques d'une sync
 */
@Serializable
data class SyncStats(
    val goalsCount: Int = 0,
    val intervalsCount: Int = 0,
    val tasksCount: Int = 0,
    val taskFoldersCount: Int = 0,
    val checklistsCount: Int = 0,
    val checklistItemsCount: Int = 0,
    val eventsCount: Int = 0,
    val eventTemplatesCount: Int = 0,
    val repeatingsCount: Int = 0,
    val notesCount: Int = 0,
    val shortcutsCount: Int = 0,
    val kvCount: Int = 0
) {
    val totalCount: Int get() = goalsCount + intervalsCount + tasksCount + 
        taskFoldersCount + checklistsCount + checklistItemsCount + 
        eventsCount + eventTemplatesCount + repeatingsCount + 
        notesCount + shortcutsCount + kvCount
}

/**
 * État de la synchronisation
 */
enum class SyncStatus {
    IDLE,           // Aucune sync en cours
    SYNCING,        // Sync en cours
    SUCCESS,        // Dernière sync réussie
    ERROR           // Dernière sync échouée
}

