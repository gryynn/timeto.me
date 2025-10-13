# 🚀 QUICK START - Synchronisation Supabase pour timeto.me

## 📋 CE QUE TU AS MAINTENANT

✅ **Fichiers créés :**
- `shared/src/commonMain/kotlin/me/timeto/shared/sync/SupabaseConfig.kt`
- `shared/src/commonMain/kotlin/me/timeto/shared/sync/SyncModels.kt`
- `shared/src/commonMain/kotlin/me/timeto/shared/sync/SupabaseRepository.kt`

✅ **Fichiers modifiés :**
- `shared/build.gradle.kts` (dépendances Supabase ajoutées)
- `android_app/build.gradle.kts` (applicationId temporaire)

✅ **Fichiers de référence :**
- `SUPABASE_SCHEMA.sql` (schéma SQL corrigé)
- `ETAPE_5_COMPLETE.md` (documentation complète)

---

## ⚡ ACTIONS RAPIDES (5 minutes)

### 1. Vérifie Supabase (2 min)

**Dans Supabase SQL Editor :**
```sql
SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'timeto';
```
**Résultat attendu : 13**

**Si pas 13 tables :**
```sql
-- Option A : Re-créer le schéma
DROP SCHEMA IF EXISTS timeto CASCADE;
-- Puis exécute tout SUPABASE_SCHEMA.sql

-- Option B : Vérifier quel schéma tu as utilisé
SHOW search_path;
```

### 2. Sync Gradle (1 min)

**Dans Android Studio :**
1. Ouvre le projet `timeto.me`
2. File → Sync Project with Gradle Files
3. Attends la fin (1-2 min)

**OU en ligne de commande :**
```bash
cd android_app
./gradlew build --refresh-dependencies
```

### 3. Compile (2 min)

```bash
cd android_app
./gradlew assembleDebug
```

**Résultat attendu :**
```
BUILD SUCCESSFUL in Xs
```

**Si erreur :** Copie-colle l'erreur et je t'aide.

---

## 🎯 COMMIT (Optionnel)

```bash
git add .
git status  # Vérifie les fichiers

git commit -m "feat(sync): Setup Supabase synchronization (Étape 2-5)

- Add Supabase dependencies (postgrest-kt 2.7.2)
- Implement SupabaseConfig with KvDb storage
- Implement SupabaseRepository with full sync
- Add all entity mappings (Goals, Tasks, Checklists, etc.)
- Create corrected SQL schema
- Temporary applicationId for parallel testing"

git push origin feature/supabase-sync
```

---

## 🧪 TEST RAPIDE (Optionnel)

### Dans Kotlin (après les étapes 6-10)

```kotlin
// Test de connexion
val repo = SupabaseRepository()
val result = repo.testConnection()
if (result.isSuccess) {
    println("✅ Connexion Supabase OK")
} else {
    println("❌ Erreur : ${result.exceptionOrNull()?.message}")
}
```

---

## 📊 STATUT ACTUEL

| Étape | Statut | Détails |
|-------|--------|---------|
| 1. Schéma SQL | ✅ | 13 tables dans schéma `timeto` |
| 2. Dépendances | ✅ | Supabase postgrest-kt 2.7.2 |
| 3. Configuration | ✅ | SupabaseConfig avec KvDb |
| 4. Modèles | ✅ | SyncResult, SyncStats, SyncStatus |
| 5. Repository | ✅ | Tous les mappings + testConnection() |
| **6. ViewModel** | ⏳ | **PROCHAINE ÉTAPE** |
| 7. UI Config | ⏳ | À venir |
| 8. UI Status | ⏳ | À venir |
| 9. Background | ⏳ | À venir (optionnel) |
| 10. Tests | ⏳ | À venir |

---

## 🚀 PROCHAINE ÉTAPE

**Dis-moi quand tu es prêt et je passe aux étapes 6-10 :**

1. ✅ **Vérifié Supabase** (13 tables)
2. ✅ **Gradle sync** réussi
3. ✅ **Compilation** réussie
4. ✅ **(Optionnel) Commit** fait

**Réponds : "Prêt pour étapes 6-10" et je continue !** 🔥

