package me.timeto.shared.sync

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import me.timeto.shared.backups.Backup
import me.timeto.shared.time
import me.timeto.shared.zlog

/**
 * Repository pour synchroniser les données vers Supabase
 * 
 * Réutilise le système de Backup existant pour sérialiser les données
 * et les envoie vers Supabase via upsert (insert or update)
 */
class SupabaseRepository {
    
    private var client: io.github.jan.supabase.SupabaseClient? = null
    
    /**
     * Récupère le client Supabase (lazy initialization)
     * 
     * @throws IllegalStateException si la config n'est pas valide
     */
    private suspend fun getClient(): io.github.jan.supabase.SupabaseClient {
        if (client == null) {
            val url = SupabaseConfig.getUrl() 
                ?: throw IllegalStateException("Supabase URL not configured")
            val key = SupabaseConfig.getKey() 
                ?: throw IllegalStateException("Supabase API key not configured")
            
            client = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Postgrest) {
                    defaultSchema = "timeto"  // Notre schéma dédié
                }
            }
        }
        return client!!
    }
    
    /**
     * Test la connexion Supabase
     * 
     * @return Result.success si la connexion fonctionne
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = getClient()
            // Test simple : query avec limit 1
            client.from("goals").select {
                limit(1)
            }
            Result.success(true)
        } catch (e: Exception) {
            zlog("Supabase connection test failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Synchronisation complète : Envoie TOUTES les données
     * 
     * Utilise le système Backup existant pour sérialiser les données,
     * puis les envoie table par table vers Supabase.
     * 
     * @return SyncResult avec le status et les stats
     */
    suspend fun syncFull(): SyncResult = withContext(Dispatchers.IO) {
        val startTime = time() * 1000
        val errors = mutableListOf<String>()
        val stats = mutableMapOf<String, Int>()
        
        try {
            if (!SupabaseConfig.isEnabled()) {
                return@withContext SyncResult.failure(
                    "Supabase sync is disabled",
                    0L,
                    time()
                )
            }
            
            val client = getClient()
            
            // Récupère le backup complet (réutilise le système existant !)
            val backupJson = Backup.create(type = "supabase_sync_full")
            val backup = Json.parseToJsonElement(backupJson).jsonObject
            
            // Sync chaque type d'entité
            syncEntity(client, "goals", backup, stats, errors)
            syncEntity(client, "intervals", backup, stats, errors)
            syncEntity(client, "task_folders", backup, stats, errors)
            syncEntity(client, "tasks", backup, stats, errors)
            syncEntity(client, "checklists", backup, stats, errors)
            syncEntity(client, "checklist_items", backup, stats, errors)
            syncEntity(client, "events", backup, stats, errors)
            syncEntity(client, "event_templates", backup, stats, errors)
            syncEntity(client, "repeatings", backup, stats, errors)
            syncEntity(client, "notes", backup, stats, errors)
            syncEntity(client, "shortcuts", backup, stats, errors)
            syncEntity(client, "kv_settings", backup, stats, errors)
            
            val totalItems = stats.values.sum()
            val duration = time() * 1000 - startTime
            
            // Log la sync dans Supabase
            logSync(client, totalItems, errors.isEmpty(), errors.firstOrNull(), startTime)
            
            // Met à jour le timestamp local
            SupabaseConfig.updateLastSyncTime(time().toLong())
            
            if (errors.isEmpty()) {
                SyncResult.success(totalItems, duration, time().toLong())
            } else {
                SyncResult.failure(errors, duration, time().toLong())
            }
            
        } catch (e: Exception) {
            val duration = time() * 1000 - startTime
            val errorMsg = e.message ?: "Unknown error"
            zlog("Supabase sync error: $errorMsg")
            
            SyncResult.failure(errorMsg, duration, time())
        }
    }
    
    /**
     * Synchronise une table spécifique
     * 
     * @param client Client Supabase
     * @param tableName Nom de la table Supabase
     * @param backup Backup JSON complet
     * @param stats Map pour accumuler les stats
     * @param errors Liste pour accumuler les erreurs
     */
    private suspend fun syncEntity(
        client: io.github.jan.supabase.SupabaseClient,
        tableName: String,
        backup: JsonObject,
        stats: MutableMap<String, Int>,
        errors: MutableList<String>
    ) {
        try {
            // Map des noms de tables vers les clés du backup
            val backupKey = when (tableName) {
                "task_folders" -> "task_folders"
                "checklist_items" -> "checklist_items"
                "event_templates" -> "event_templates"
                "kv_settings" -> "kv"
                else -> tableName
            }
            
            val entities = backup[backupKey]?.jsonArray ?: run {
                zlog("⚠️ No entities found for $tableName (key: $backupKey)")
                stats[tableName] = 0
                return
            }
            
            if (entities.isEmpty()) {
                zlog("⚠️ Empty entities for $tableName")
                stats[tableName] = 0
                return
            }
            
            zlog("🔄 Syncing $tableName: ${entities.size} entities from backup")
            
            // Convertit les entities en JsonObject pour Supabase
            var errorCount = 0
            val data = entities.mapIndexedNotNull { index, entity ->
                try {
                    val map = entityToSupabaseMap(entity.jsonArray, tableName)
                    // Convertir Map en JsonObject pour Supabase-kt 3.x
                    buildJsonObject {
                        map.forEach { (key, value) ->
                            when (value) {
                                null -> put(key, JsonNull)
                                is Boolean -> put(key, value)
                                is Number -> put(key, value)
                                is String -> put(key, value)
                                is JsonElement -> put(key, value)
                                else -> put(key, value.toString())
                            }
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    zlog("❌ Error mapping $tableName entity #$index: ${e.message}")
                    zlog("   Raw entity data: ${entity.jsonArray}")
                    null
                }
            }
            
            if (errorCount > 0) {
                zlog("⚠️ $tableName: $errorCount/${entities.size} entities failed to map")
            }
            
            if (data.isEmpty()) {
                zlog("❌ $tableName: All entities failed to map")
                stats[tableName] = 0
                return
            }
            
            // Upsert par batch de 100 (limite Supabase)
            data.chunked(100).forEach { batch ->
                client.from(tableName).upsert(batch)
            }
            
            stats[tableName] = data.size
            zlog("✅ Synced ${data.size} $tableName")
            
        } catch (e: Exception) {
            val error = "Error syncing $tableName: ${e.message}"
            errors.add(error)
            zlog("❌ $error")
        }
    }
    
    /**
     * Convertit une entité du format Backup (JsonArray) vers Map pour Supabase
     * 
     * Chaque table a son propre mapping basé sur la fonction backupable__backup()
     * des modèles XxxDb.kt
     * 
     * IMPORTANT : L'ordre des champs correspond EXACTEMENT à celui défini
     * dans les fonctions backupable__backup() de chaque modèle Db
     */
    private fun entityToSupabaseMap(entity: JsonArray, tableName: String): Map<String, Any?> {
        return when (tableName) {
            
            // Goals (Goal2Db.kt)
            // Ordre : id, parent_id, type_id, name, seconds, timer, period_json, finish_text,
            //         home_button_sort, color_rgba, keep_screen_on, pomodoro_timer
            "goals" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "parent_id" to when (val parentElement = entity.getOrNull(1)) {
                    is JsonNull, null -> null
                    else -> parentElement.jsonPrimitive.longOrNull
                },
                "type_id" to entity[2].jsonPrimitive.int,
                "name" to entity[3].jsonPrimitive.content,
                "seconds" to entity[4].jsonPrimitive.int,
                "timer" to entity[5].jsonPrimitive.int,
                "period_json" to Json.parseToJsonElement(entity[6].jsonPrimitive.content),
                "finish_text" to entity[7].jsonPrimitive.content,
                "home_button_sort" to entity[8].jsonPrimitive.content,
                "color_rgba" to entity[9].jsonPrimitive.content,
                "keep_screen_on" to entity[10].jsonPrimitive.int,
                "pomodoro_timer" to entity[11].jsonPrimitive.int,
                "created_at" to entity[0].jsonPrimitive.long,  // ID est le timestamp
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Intervals (IntervalDb.kt)
            // Ordre RÉEL : id, timer, note, goal_id (confirmé dans backupable__backup)
            "intervals" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "utc_time" to entity[1].jsonPrimitive.long,  // timer → utc_time dans Supabase
                "note" to when (val noteElement = entity.getOrNull(2)) {
                    is JsonNull, null -> ""
                    else -> noteElement.jsonPrimitive.contentOrNull ?: ""
                },
                "goal_id" to entity[3].jsonPrimitive.long,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Task Folders (TaskFolderDb.kt)
            // Ordre : id, name, sort
            "task_folders" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "name" to entity[1].jsonPrimitive.content,
                "sort" to entity[2].jsonPrimitive.int,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Tasks (TaskDb.kt)
            // Ordre : id, text, folder_id
            "tasks" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "text" to entity[1].jsonPrimitive.content,
                "folder_id" to entity[2].jsonPrimitive.long,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Checklists (ChecklistDb.kt)
            // Ordre : id, name
            "checklists" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "name" to entity[1].jsonPrimitive.content,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Checklist Items (ChecklistItemDb.kt)
            // Ordre : id, text, list_id, check_time, sort
            "checklist_items" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "text" to entity[1].jsonPrimitive.content,
                "list_id" to entity[2].jsonPrimitive.long,
                "check_time" to entity[3].jsonPrimitive.long,
                "sort" to entity[4].jsonPrimitive.int,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Events (EventDb.kt)
            // Ordre : id, utc_time, text
            "events" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "utc_time" to entity[1].jsonPrimitive.long,
                "text" to entity[2].jsonPrimitive.content,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Event Templates (EventTemplateDb.kt)
            // Ordre : id, sort, daytime, text
            "event_templates" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "sort" to entity[1].jsonPrimitive.int,
                "daytime" to entity[2].jsonPrimitive.int,
                "text" to entity[3].jsonPrimitive.content,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Repeatings (RepeatingDb.kt)
            // Ordre : id, text, last_day, type_id, value, daytime, is_important
            "repeatings" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "text" to entity[1].jsonPrimitive.content,
                "last_day" to entity[2].jsonPrimitive.int,
                "type_id" to entity[3].jsonPrimitive.int,
                "value" to entity[4].jsonPrimitive.content,
                "daytime" to when (val daytimeElement = entity.getOrNull(5)) {
                    is JsonNull, null -> null
                    else -> daytimeElement.jsonPrimitive.intOrNull
                },
                "is_important" to entity[6].jsonPrimitive.int,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Notes (NoteDb.kt)
            // Ordre RÉEL : id, sort, text (confirmé dans backupable__backup)
            "notes" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "sort" to entity[1].jsonPrimitive.int,
                "text" to entity[2].jsonPrimitive.content,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // Shortcuts (ShortcutDb.kt)
            // Ordre : id, name, uri
            "shortcuts" -> mapOf(
                "id" to entity[0].jsonPrimitive.long,
                "name" to entity[1].jsonPrimitive.content,
                "uri" to entity[2].jsonPrimitive.content,
                "created_at" to entity[0].jsonPrimitive.long,
                "updated_at" to entity[0].jsonPrimitive.long,
                "is_deleted" to false
            )
            
            // KV Settings (KvDb.kt)
            // Ordre : key, value
            "kv_settings" -> mapOf(
                "key" to entity[0].jsonPrimitive.content,
                "value" to entity[1].jsonPrimitive.content,
                "created_at" to time().toLong(),  // KV n'a pas d'ID timestamp
                "updated_at" to time().toLong(),
                "is_deleted" to false
            )
            
            else -> {
                zlog("⚠️ Unknown table: $tableName")
                emptyMap()
            }
        }
    }
    
    /**
     * Log une sync dans la table sync_log de Supabase
     */
    private suspend fun logSync(
        client: io.github.jan.supabase.SupabaseClient,
        totalItems: Int,
        success: Boolean,
        errorMessage: String?,
        startTime: Long
    ) {
        try {
            val logData = buildJsonObject {
                put("total_items", totalItems)
                put("success", success)
                if (errorMessage != null) {
                    put("error_message", errorMessage)
                } else {
                    put("error_message", JsonNull)
                }
                put("duration_ms", (time() * 1000 - startTime).toInt())
            }
            client.from("sync_log").insert(logData)
        } catch (e: Exception) {
            zlog("⚠️ Failed to log sync: ${e.message}")
        }
    }
    
    /**
     * Synchronisation incrémentielle : Envoie seulement les données récentes
     *
     * Contrairement à syncFull() qui envoie TOUT, cette méthode filtre
     * les données par âge (en jours) pour éviter les timeouts avec de gros volumes.
     *
     * @param days Nombre de jours de données à synchroniser (défaut: 7)
     * @return SyncResult avec le status et les stats
     */
    suspend fun syncIncremental(days: Int = 7): SyncResult = withContext(Dispatchers.IO) {
        val startTime = time() * 1000
        val errors = mutableListOf<String>()
        val stats = mutableMapOf<String, Int>()

        try {
            if (!SupabaseConfig.isEnabled()) {
                return@withContext SyncResult.failure(
                    "Supabase sync is disabled",
                    0L,
                    time()
                )
            }

            val client = getClient()

            // Calculer le timestamp de cutoff (il y a X jours)
            val cutoffTime = time() - (days * 24 * 60 * 60)

            // Récupère le backup complet (réutilise le système existant !)
            val backupJson = Backup.create(type = "supabase_sync_incremental")
            val backup = Json.parseToJsonElement(backupJson).jsonObject

            // Sync chaque type d'entité avec filtrage temporel
            syncEntityIncremental(client, "goals", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "intervals", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "task_folders", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "tasks", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "checklists", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "checklist_items", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "events", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "event_templates", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "repeatings", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "notes", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "shortcuts", backup, cutoffTime, stats, errors)
            syncEntityIncremental(client, "kv_settings", backup, cutoffTime, stats, errors)

            val totalItems = stats.values.sum()
            val duration = time() * 1000 - startTime

            // Log la sync dans Supabase
            logSync(client, totalItems, errors.isEmpty(), errors.firstOrNull(), startTime)

            // Met à jour le timestamp local
            SupabaseConfig.updateLastSyncTime(time())

            if (errors.isEmpty()) {
                SyncResult.success(totalItems, duration, time())
            } else {
                SyncResult.failure(errors, duration, time())
            }

        } catch (e: Exception) {
            val duration = time() * 1000 - startTime
            val errorMsg = e.message ?: "Unknown error"
            zlog("Supabase incremental sync error: $errorMsg")

            SyncResult.failure(errorMsg, duration, time())
        }
    }

    /**
     * Synchronise une table spécifique avec filtrage temporel
     */
    private suspend fun syncEntityIncremental(
        client: io.github.jan.supabase.SupabaseClient,
        tableName: String,
        backup: JsonObject,
        cutoffTime: Long,
        stats: MutableMap<String, Int>,
        errors: MutableList<String>
    ) {
        try {
            // Map des noms de tables vers les clés du backup
            val backupKey = when (tableName) {
                "task_folders" -> "task_folders"
                "checklist_items" -> "checklist_items"
                "event_templates" -> "event_templates"
                "kv_settings" -> "kv"
                else -> tableName
            }

            val entities = backup[backupKey]?.jsonArray ?: return

            if (entities.isEmpty()) {
                stats[tableName] = 0
                return
            }

            // Filtrer les entités récentes selon le cutoff
            val recentEntities = entities.filter { entity ->
                val entityArray = entity.jsonArray
                when (tableName) {
                    "goals", "intervals", "tasks", "checklists", "checklist_items",
                    "events", "event_templates", "repeatings", "notes", "shortcuts" -> {
                        // Ces entités ont leur ID comme timestamp de création
                        val id = entityArray[0].jsonPrimitive.long
                        id >= cutoffTime
                    }
                    "task_folders" -> {
                        // Task folders utilisent aussi l'ID comme timestamp
                        val id = entityArray[0].jsonPrimitive.long
                        id >= cutoffTime
                    }
                    "kv_settings" -> {
                        // KV settings n'ont pas de timestamp, on les sync tous
                        true
                    }
                    else -> true
                }
            }

            if (recentEntities.isEmpty()) {
                stats[tableName] = 0
                return
            }

            // Convertir les entities en maps pour Supabase
            val data = recentEntities.mapNotNull { entity ->
                try {
                    entityToSupabaseMap(entity.jsonArray, tableName)
                } catch (e: Exception) {
                    zlog("Error mapping $tableName entity: ${e.message}")
                    null
                }
            }

            if (data.isEmpty()) {
                stats[tableName] = 0
                return
            }

            // Upsert par batch de 100 (limite Supabase)
            data.chunked(100).forEach { batch ->
                client.from(tableName).upsert(batch)
            }

            stats[tableName] = data.size
            zlog("✅ Synced ${data.size} recent $tableName (last $cutoffTime)")

        } catch (e: Exception) {
            val error = "Error syncing $tableName: ${e.message}"
            errors.add(error)
            zlog("❌ $error")
        }
    }

    /**
     * Reset le client (force reconnexion)
     * Utile si les credentials changent
     */
    fun resetClient() {
        client = null
    }
}

