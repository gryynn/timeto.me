# Git Workflow Guide

## Workflow recommandé pour ce projet

### 1. Commits réguliers
Faites des commits fréquents avec des messages clairs :

```bash
# Ajout d'une fonctionnalité
git add .
git commit -m "✨ Add new feature: [description]"

# Correction de bug
git add .
git commit -m "🐛 Fix: [description du bug]"

# Amélioration
git add .
git commit -m "⚡ Improve: [description]"

# Refactoring
git add .
git commit -m "♻️ Refactor: [description]"

# Documentation
git add .
git commit -m "📚 Docs: [description]"
```

### 2. Messages de commit
Utilisez des emojis pour catégoriser :
- ✨ Nouvelle fonctionnalité
- 🐛 Correction de bug
- ⚡ Amélioration de performance
- ♻️ Refactoring
- 📚 Documentation
- 🎨 Style/UI
- 🔧 Configuration
- ✅ Test
- 🚀 Déploiement

### 3. Branches
- `main` : Version stable
- `feature/supabase-sync` : Développement Supabase
- `feature/[nom]` : Nouvelles fonctionnalités
- `bugfix/[nom]` : Corrections

### 4. Avant chaque commit
```bash
# Vérifier les changements
git status
git diff

# Ajouter les fichiers
git add .

# Commit avec message
git commit -m "Message clair"

# Push vers le remote (si configuré)
git push
```

### 5. Historique propre
Évitez les commits avec des messages comme :
- ❌ "fix"
- ❌ "update"
- ❌ "changes"

Préférez :
- ✅ "Fix: authentication crash on startup"
- ✅ "Add: user profile synchronization"
- ✅ "Improve: sync performance by 40%"

### 6. Exemples de commits pour ce projet
```bash
git commit -m "✨ Add Supabase sync functionality"
git commit -m "🐛 Fix: intervals table column mapping"
git commit -m "⚡ Improve: sync performance with batch operations"
git commit -m "📚 Docs: add Supabase setup guide"
git commit -m "🔧 Config: update sync intervals to 6 hours"
```

