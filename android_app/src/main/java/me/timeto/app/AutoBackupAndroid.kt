import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.activity.result.contract.ActivityResultContracts
import me.timeto.shared.*
import kotlin.jvm.Throws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.Q) // MediaStore.MediaColumns.RELATIVE_PATH
object AutoBackupAndroid {

    private const val AUTOBACKUPS_FOLDER_NAME = "timetome_autobackups"
    private const val AUTOBACKUPS_PATH = "Download/$AUTOBACKUPS_FOLDER_NAME"

    suspend fun dailyBackupIfNeeded() {
        try {
            val lastBackupUnixDay = getLastTimeOrNull()?.localDay ?: 0
            if (lastBackupUnixDay < UnixTime().localDay) {
                newBackup()
                cleanOld()
            }
        } catch (e: Throwable) {
            reportApi("AutoBackupAndroid.dailyBackupIfNeeded()\n$e")
        }
    }

    @Throws
    suspend fun newBackup() {
        // Sauvegarde locale
        val savedUri = getSavedUri()
        if (savedUri != null) {
            exportToUri(savedUri)
        } else {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "backup.json")
            }
            exportLauncher.launch(intent)
        }

        // Export Supabase des dernières activités
        try {
            exportLatestActivitiesToSupabase()
        } catch (e: Exception) {
            // Log l'erreur mais ne bloque pas le processus de sauvegarde
            reportApi("AutoBackupAndroid.newBackup() Supabase export error\n$e")
        }
    }

    private suspend fun exportLatestActivitiesToSupabase() = withContext(Dispatchers.IO) {
        try {
            // Récupérer les dernières activités (dernières 24h par exemple)
            val currentTime = UnixTime()
            val oneDayAgo = currentTime.time - 86400 // 24h en secondes
            
            // Limite à 50 activités par export pour optimiser les performances
            IntervalDb.getBetweenIdDesc(
                timeStart = oneDayAgo,
                timeFinish = currentTime.time,
                limit = 50 // Réduit de 100 à 50 pour de meilleures performances
            ).forEach { interval ->
                val activity = interval.getActivity()
                
                // Enqueue l'export via WorkManager avec un tag pour le grouping
                SupabaseExportWorker.enqueue(
                    context = App.instance,
                    timestamp = interval.id.toLong(),
                    activityId = activity.id,
                    activityName = activity.name,
                    duration = interval.timer,
                    note = interval.note
                )
            }
        } catch (e: Exception) {
            reportApi("AutoBackupAndroid.exportLatestActivitiesToSupabase()\n$e")
        }
    }

    // Lance l'Intent pour choisir un emplacement de sauvegarde
    private val exportLauncher = App.instance.registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri != null) {
            saveUri(uri)
            exportToUri(uri)
        }
    }

    // Fonction pour écrire les données dans l'URI donné
    private fun exportToUri(uri: Uri) {
        App.instance.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val autoBackupData = AutoBackup.buildAutoBackup()
            outputStream.write(autoBackupData.jsonString.toByteArray())
        }
    }

    // Sauvegarde l'URI pour une utilisation future
    private fun saveUri(uri: Uri) {
        val sharedPreferences = App.instance.getSharedPreferences("my_prefs", App.instance.MODE_PRIVATE)
        sharedPreferences.edit().putString("backup_uri", uri.toString()).apply()
    }

    // Récupère l'URI sauvegardé
    private fun getSavedUri(): Uri? {
        val sharedPreferences = App.instance.getSharedPreferences("my_prefs", App.instance.MODE_PRIVATE)
        val uriString = sharedPreferences.getString("backup_uri", null)
        return uriString?.let { Uri.parse(it) }
    }

    @Throws
    fun cleanOld() {
        getAutoBackupsSortedDesc()
            .drop(10)
            .forEach { fileData ->
                val resCode = App.instance.contentResolver.delete(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    MediaStore.Files.FileColumns._ID + "=?",
                    listOf(fileData.id).toTypedArray(),
                )
            }
    }

    @Throws
    private fun getAutoBackupsSortedDesc(): List<MyFileData> {
        val cursor = App.instance.contentResolver.query(getVolume(), null, null, null, null)
                     ?: throw Exception("AutoBackupAndroid.getAutoBackupsSortedDesc() cursor nullable")
        val files = mutableListOf<MyFileData>()
        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH))
                if (!path.contains(AUTOBACKUPS_FOLDER_NAME))
                    continue
                files.add(
                    MyFileData(
                        id = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)),
                        name = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)),
                        path = path
                    )
                )
            }
        }
        return files.sortedByDescending { it.name }
    }

    @Throws
    fun getLastTimeOrNull(): UnixTime? {
        val lastBackup = getAutoBackupsSortedDesc().firstOrNull()?.name ?: return null
        return Backup.fileNameToUnixTime(lastBackup)
    }

    private fun getVolume() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private class MyFileData(
        val id: String,
        val name: String,
        val path: String,
    )
}
