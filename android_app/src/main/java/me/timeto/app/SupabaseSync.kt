package me.timeto.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.room.*
import androidx.work.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import me.timeto.shared.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

private const val TAG = "SupabaseSync"
private const val MAX_RETRIES = 3
private const val INITIAL_BACKOFF_MILLIS = 1000L

/**
 * Entity pour le cache Room des activités exportées
 */
@Entity(tableName = "exported_activities")
data class ExportedActivity(
    @PrimaryKey val timestamp: Long,
    val activityId: Int,
    val duration: Int,
    val exportedAt: Long = System.currentTimeMillis()
)

/**
 * DAO pour le cache Room
 */
@Dao
interface ExportedActivityDao {
    @Query("SELECT * FROM exported_activities WHERE timestamp = :timestamp")
    suspend fun getByTimestamp(timestamp: Long): ExportedActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ExportedActivity)

    @Query("DELETE FROM exported_activities WHERE exportedAt < :threshold")
    suspend fun cleanOld(threshold: Long)
}

/**
 * Database Room
 */
@Database(entities = [ExportedActivity::class], version = 1)
abstract class SupabaseDatabase : RoomDatabase() {
    abstract fun exportedActivityDao(): ExportedActivityDao

    companion object {
        @Volatile
        private var INSTANCE: SupabaseDatabase? = null

        fun getDatabase(context: Context): SupabaseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    SupabaseDatabase::class.java,
                    "supabase_cache"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

/**
 * Modèle de données pour Supabase
 */
@Serializable
data class ActivityExport(
    val timestamp: Long,
    val activity_id: Int,
    val activity_name: String,
    val duration_seconds: Int,
    val note: String?
)

/**
 * Résultat générique pour la gestion des erreurs
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

/**
 * Interface Supabase
 */
class SupabaseApi(private val context: Context) {
    private val client by lazy {
        SupabaseConfig.requireValidConfig()
        
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
        }
    }

    suspend fun uploadActivity(activity: ActivityExport): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isEnabled) {
            Log.i(TAG, "Export Supabase désactivé, ignoré")
            return@withContext Result.Success(Unit)
        }

        try {
            client.postgrest["activities"].insert(activity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading activity", e)
            Result.Error(e)
        }
    }
}

/**
 * Repository principal
 */
class SupabaseRepository(context: Context) {
    private val api = SupabaseApi(context)
    private val dao = SupabaseDatabase.getDatabase(context).exportedActivityDao()
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun uploadActivity(
        timestamp: Long,
        activityId: Int,
        activityName: String,
        duration: Int,
        note: String?,
        retryCount: Int = 0
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Vérifier si déjà exporté
            if (dao.getByTimestamp(timestamp) != null) {
                Log.d(TAG, "Activity already exported: $timestamp")
                return@withContext Result.Success(Unit)
            }

            // Vérifier la connexion
            if (!isConnected()) {
                throw Exception("No internet connection")
            }

            // Créer l'export
            val activityExport = ActivityExport(
                timestamp = timestamp,
                activity_id = activityId,
                activity_name = activityName,
                duration_seconds = duration,
                note = note
            )

            // Uploader vers Supabase
            when (val result = api.uploadActivity(activityExport)) {
                is Result.Success -> {
                    // Sauvegarder dans le cache
                    dao.insert(ExportedActivity(timestamp, activityId, duration))
                    Log.i(TAG, "Successfully exported activity: $timestamp")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    if (retryCount < MAX_RETRIES) {
                        // Retry avec backoff exponentiel
                        val backoffDelay = INITIAL_BACKOFF_MILLIS * (1 shl retryCount)
                        delay(backoffDelay)
                        uploadActivity(timestamp, activityId, activityName, duration, note, retryCount + 1)
                    } else {
                        Log.e(TAG, "Failed to export activity after $MAX_RETRIES retries")
                        result
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadActivity", e)
            Result.Error(e)
        }
    }

    suspend fun cleanOldCache(daysToKeep: Int = 30) {
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())
        dao.cleanOld(threshold)
    }
}

/**
 * WorkManager pour l'export en arrière-plan
 */
class SupabaseExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = SupabaseRepository(applicationContext)
            
            // Récupérer les données de l'activité depuis les InputData
            val timestamp = inputData.getLong("timestamp", 0)
            val activityId = inputData.getInt("activity_id", 0)
            val activityName = inputData.getString("activity_name") ?: return@withContext Result.failure()
            val duration = inputData.getInt("duration", 0)
            val note = inputData.getString("note")

            // Tenter l'export
            when (repository.uploadActivity(timestamp, activityId, activityName, duration, note)) {
                is me.timeto.app.Result.Success -> Result.success()
                is me.timeto.app.Result.Error -> Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in worker", e)
            Result.retry()
        }
    }

    companion object {
        fun enqueue(
            context: Context,
            timestamp: Long,
            activityId: Int,
            activityName: String,
            duration: Int,
            note: String?
        ) {
            val data = workDataOf(
                "timestamp" to timestamp,
                "activity_id" to activityId,
                "activity_name" to activityName,
                "duration" to duration,
                "note" to note
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SupabaseExportWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "export_${timestamp}",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
} 