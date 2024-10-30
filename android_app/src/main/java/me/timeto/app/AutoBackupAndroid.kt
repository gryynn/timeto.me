import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.activity.result.contract.ActivityResultContracts
import me.timeto.shared.*
import kotlin.jvm.Throws

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
        val savedUri = getSavedUri() // Récupère l'URI sauvegardé
        if (savedUri != null) {
            // Utilise l'URI sauvegardé pour la sauvegarde
            exportToUri(savedUri)
        } else {
            // Demande à l’utilisateur de sélectionner l'emplacement la première fois
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "backup.json")
            }
            exportLauncher.launch(intent)
        }
    }

    // Lance l'Intent pour choisir un emplacement de sauvegarde
    private val exportLauncher = App.instance.registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri != null) {
            saveUri(uri) // Sauvegarde l'URI pour les prochaines sauvegardes
            exportToUri(uri) // Sauvegarde immédiatement au nouvel emplacement choisi
        }
    }

    // Fonction pour écrire les données dans l'URI donné
    private fun exportToUri(uri: Uri) {
        App.instance.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val autoBackupData = AutoBackup.buildAutoBackup()
            outputStream.write(autoBackupData.jsonString.toByteArray())
        }
    }

    // Sauvegarde l’URI pour une utilisation future
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

    // Nettoyage des anciennes sauvegardes
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

    // Fonction pour trier et récupérer les sauvegardes existantes
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

    // Fonction pour obtenir la date de la dernière sauvegarde
    @Throws
    fun getLastTimeOrNull(): UnixTime? {
        val lastBackup = getAutoBackupsSortedDesc().firstOrNull()?.name ?: return null
        return Backup.fileNameToUnixTime(lastBackup)
    }

    private fun getVolume() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private class MyFileData(
        val id: String, // MediaStore.Files.FileColumns._ID
        val name: String, // MediaStore.Files.FileColumns.DISPLAY_NAME
        val path: String, // MediaStore.Files.FileColumns.RELATIVE_PATH
    )
}
