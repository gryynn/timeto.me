# 📝 Instructions pour Commit

## 🎯 Commandes à exécuter

```bash
# 1. Va dans le dossier du projet
cd "C:\Users\marti\OneDrive\Onedrive martingreengraham\Documents\02-WIP\timeto.me"

# 2. Vérifie les fichiers modifiés
git status

# 3. Ajoute tous les fichiers
git add .

# 4. Commit avec un message détaillé
git commit -m "feat(sync): Setup Supabase synchronization (Steps 2-5 complete)

- Add Supabase dependencies (postgrest-kt 2.6.1)
- Implement SupabaseConfig with direct SQLDelight queries
- Implement SupabaseRepository with full sync + testConnection()
- Add all entity mappings (12 types verified)
- Create corrected SQL schema (13 tables in timeto schema)
- Temporary applicationId for parallel testing

Fixes:
- Use stable Supabase version 2.6.1 instead of 2.7.2
- Fix SupabaseConfig to use real KvDb API (direct SQLDelight queries)
- Verified Supabase tables created and tested successfully

Supabase schema includes:
- goals, intervals, tasks, task_folders
- checklists, checklist_items
- events, event_templates
- repeatings, notes, shortcuts
- kv_settings, sync_log

Ready for Steps 6-10 (ViewModel + UI)"

# 5. Push vers GitHub
git push origin feature/supabase-sync
```

---

## 📊 Fichiers modifiés

- ✅ `shared/build.gradle.kts` - Dépendances Supabase
- ✅ `android_app/build.gradle.kts` - applicationId temporaire
- ✅ `shared/src/commonMain/kotlin/me/timeto/shared/sync/SupabaseConfig.kt` - Nouveau
- ✅ `shared/src/commonMain/kotlin/me/timeto/shared/sync/SyncModels.kt` - Nouveau
- ✅ `shared/src/commonMain/kotlin/me/timeto/shared/sync/SupabaseRepository.kt` - Nouveau
- ✅ `SUPABASE_SCHEMA.sql` - Nouveau
- ✅ Documentation (ETAPE_5_COMPLETE.md, QUICK_START.md, etc.)

---

## ✅ Après le commit

**Réponds :** "✅ Commit fait, push OK"

**Et je passe immédiatement aux étapes 6-10 ! 🚀**

