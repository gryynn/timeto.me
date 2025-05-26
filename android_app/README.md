# Configuration de l'application Android

## Configuration Supabase

Pour configurer l'export des données vers Supabase :

1. Copiez le fichier `local.properties.template` vers `local.properties` :
```bash
cp local.properties.template local.properties
```

2. Éditez `local.properties` et ajoutez vos clés Supabase :
```properties
# SDK Location (laissez tel quel)
sdk.dir=C:\\Users\\USERNAME\\AppData\\Local\\Android\\Sdk

# Supabase Configuration
SUPABASE_URL=your_supabase_url_here
SUPABASE_KEY=your_supabase_key_here
```

3. Les clés seront automatiquement chargées dans l'application via BuildConfig.

## Sécurité

- Ne commettez JAMAIS le fichier `local.properties` dans Git
- Ne partagez JAMAIS vos clés Supabase
- En cas de compromission des clés, régénérez-les immédiatement dans votre console Supabase

## Environnements multiples

Pour gérer différents environnements (dev/prod) :

1. Créez un fichier par environnement :
   - `local.properties.dev`
   - `local.properties.prod`

2. Copiez le fichier approprié avant de compiler :
```bash
cp local.properties.dev local.properties  # Pour le développement
cp local.properties.prod local.properties # Pour la production
``` 