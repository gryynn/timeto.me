# 🔄 GUIDE : Sync Gradle dans Android Studio

## 📍 Où tu en es

✅ Tables Supabase créées (13/13)
✅ Code Kotlin créé (SupabaseConfig, SupabaseRepository, etc.)
⏳ Sync Gradle à faire

---

## 🚀 ÉTAPES DÉTAILLÉES

### 1. Ouvre le projet dans Android Studio

**Si pas encore ouvert :**
1. Lance Android Studio
2. File → Open
3. Navigue vers : `C:\Users\marti\OneDrive\Onedrive martingreengraham\Documents\02-WIP\timeto.me`
4. **Sélectionne le dossier `timeto.me`** (pas `android_app`)
5. Clique "OK"

**Attends 10-20 secondes** que Android Studio scanne le projet.

---

### 2. Sync Gradle

**Tu devrais voir un popup comme ça :**

```
┌─────────────────────────────────────────────────┐
│ Gradle files have changed                       │
│ A project sync may be necessary                 │
│                                                  │
│  [Sync Now]  [Remind me later]  [Don't show]   │
└─────────────────────────────────────────────────┘
```

**Clique sur "Sync Now"**

**OU si tu ne vois pas le popup :**

**Méthode 1 : Menu**
```
File → Sync Project with Gradle Files
```

**Méthode 2 : Icône**
- Cherche l'icône 🐘 (éléphant Gradle) dans la toolbar en haut
- Clique dessus

**Méthode 3 : Raccourci clavier**
- Windows : `Ctrl + Shift + O`

---

### 3. Attends la fin

**Tu verras en bas de Android Studio :**

```
🔄 Gradle sync in progress...
```

**Durée : 1-3 minutes** (première fois avec les nouvelles dépendances Supabase)

**Quand c'est fini, tu verras :**

```
✅ Gradle sync finished in 1m 23s
```

**OU**

```
❌ Gradle sync failed
```

---

### 4. Vérifications

**A. Si Sync OK ✅**

En bas de Android Studio, onglet "Build" :
```
BUILD SUCCESSFUL in 1m 23s
```

**B. Si Sync échoue ❌**

Tu verras les erreurs dans l'onglet "Build". **Copie-colle les erreurs** et envoie-les moi.

---

## 🧪 TEST : Compile le projet

Une fois le sync Gradle terminé avec succès :

**Dans Android Studio :**
1. Menu : `Build → Make Project`
2. OU raccourci : `Ctrl + F9`

**OU en ligne de commande :**
```bash
cd android_app
..\gradlew assembleDebug
```

**Résultat attendu :**
```
BUILD SUCCESSFUL in Xs
```

---

## ❓ PROBLÈMES FRÉQUENTS

### Problème 1 : "Could not resolve io.github.jan-tennert.supabase:postgrest-kt:2.7.2"

**Solution :**
```bash
# Nettoie le cache Gradle
.\gradlew clean --refresh-dependencies
```

### Problème 2 : "Project uses Java 21 but..."

**Solution :**
1. File → Project Structure
2. SDK Location → JDK → Sélectionne Java 21
3. Clique "Apply"
4. Re-sync Gradle

### Problème 3 : Le sync reste bloqué

**Solution :**
1. File → Invalidate Caches → Invalidate and Restart
2. Attends le redémarrage
3. Re-sync Gradle

---

## 🎯 APRÈS LE SYNC

Une fois que `BUILD SUCCESSFUL` :

1. ✅ Commit les changements
2. ✅ Push sur GitHub
3. ✅ Réponds "Prêt pour étapes 6-10"

```bash
git add .
git commit -m "feat(sync): Setup Supabase synchronization (Steps 2-5 complete)"
git push origin feature/supabase-sync
```

---

## 📞 BESOIN D'AIDE ?

Si tu vois des erreurs :
1. **Copie-colle l'erreur exacte**
2. **Screenshot de l'onglet Build** (si possible)
3. Envoie-moi ça et je corrige immédiatement

**GO ! 🚀**

