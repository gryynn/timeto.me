-- ====================================================================================
-- SCHÉMA SUPABASE POUR TIMETO.ME - VERSION CORRIGÉE
-- ====================================================================================
--
-- Ce schéma correspond EXACTEMENT aux données disponibles dans les backups de l'app.
-- Chaque champ ici existe dans le format backupable__backup() des modèles Db.kt
--
-- ====================================================================================

-- Créer le schéma dédié
CREATE SCHEMA IF NOT EXISTS timeto;

-- Utiliser le schéma par défaut
SET search_path TO timeto;

-- ====================================================================================
-- TABLE 1 : Goals
-- ====================================================================================

CREATE TABLE goals (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT,
    type_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    seconds INTEGER NOT NULL,
    timer INTEGER NOT NULL,
    period_json JSONB NOT NULL,
    finish_text TEXT NOT NULL DEFAULT '',
    home_button_sort TEXT NOT NULL DEFAULT '',
    color_rgba TEXT NOT NULL,
    keep_screen_on INTEGER NOT NULL DEFAULT 0,
    pomodoro_timer INTEGER NOT NULL DEFAULT 0,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_goals_updated_at ON goals(updated_at DESC);
CREATE INDEX idx_goals_parent_id ON goals(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_goals_is_deleted ON goals(is_deleted) WHERE is_deleted = true;

-- ====================================================================================
-- TABLE 2 : Intervals
-- ====================================================================================

CREATE TABLE intervals (
    id BIGINT PRIMARY KEY,
    timer INTEGER NOT NULL,
    goal_id BIGINT NOT NULL,
    note TEXT,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_intervals_updated_at ON intervals(updated_at DESC);
CREATE INDEX idx_intervals_goal_id ON intervals(goal_id);
CREATE INDEX idx_intervals_timer ON intervals(timer DESC);

-- ====================================================================================
-- TABLE 3 : Task Folders
-- ====================================================================================

CREATE TABLE task_folders (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    sort INTEGER NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_task_folders_updated_at ON task_folders(updated_at DESC);
CREATE INDEX idx_task_folders_sort ON task_folders(sort ASC);

-- ====================================================================================
-- TABLE 4 : Tasks
-- ====================================================================================

CREATE TABLE tasks (
    id BIGINT PRIMARY KEY,
    text TEXT NOT NULL,
    folder_id BIGINT NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_tasks_updated_at ON tasks(updated_at DESC);
CREATE INDEX idx_tasks_folder_id ON tasks(folder_id);
CREATE INDEX idx_tasks_text_search ON tasks USING gin(to_tsvector('english', text));

-- ====================================================================================
-- TABLE 5 : Checklists
-- ====================================================================================

CREATE TABLE checklists (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_checklists_updated_at ON checklists(updated_at DESC);

-- ====================================================================================
-- TABLE 6 : Checklist Items
-- ====================================================================================

CREATE TABLE checklist_items (
    id BIGINT PRIMARY KEY,
    text TEXT NOT NULL,
    list_id BIGINT NOT NULL,
    check_time BIGINT NOT NULL,  -- 0 = unchecked, > 0 = checked timestamp
    sort INTEGER NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_checklist_items_updated_at ON checklist_items(updated_at DESC);
CREATE INDEX idx_checklist_items_list_id ON checklist_items(list_id);
CREATE INDEX idx_checklist_items_sort ON checklist_items(list_id, sort ASC);

-- ====================================================================================
-- TABLE 7 : Events
-- ====================================================================================

CREATE TABLE events (
    id BIGINT PRIMARY KEY,
    utc_time BIGINT NOT NULL,
    text TEXT NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_events_updated_at ON events(updated_at DESC);
CREATE INDEX idx_events_utc_time ON events(utc_time DESC);

-- ====================================================================================
-- TABLE 8 : Event Templates
-- ====================================================================================

CREATE TABLE event_templates (
    id BIGINT PRIMARY KEY,
    sort INTEGER NOT NULL,
    daytime INTEGER NOT NULL,
    text TEXT NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_event_templates_updated_at ON event_templates(updated_at DESC);
CREATE INDEX idx_event_templates_sort ON event_templates(sort ASC);

-- ====================================================================================
-- TABLE 9 : Repeatings
-- ====================================================================================

CREATE TABLE repeatings (
    id BIGINT PRIMARY KEY,
    text TEXT NOT NULL,
    last_day INTEGER NOT NULL,
    type_id INTEGER NOT NULL,
    value TEXT NOT NULL,
    daytime INTEGER,
    is_important INTEGER NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_repeatings_updated_at ON repeatings(updated_at DESC);

-- ====================================================================================
-- TABLE 10 : Notes
-- ====================================================================================

CREATE TABLE notes (
    id BIGINT PRIMARY KEY,
    text TEXT NOT NULL,
    sort INTEGER NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_notes_updated_at ON notes(updated_at DESC);
CREATE INDEX idx_notes_sort ON notes(sort ASC);

-- ====================================================================================
-- TABLE 11 : Shortcuts
-- ====================================================================================

CREATE TABLE shortcuts (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    uri TEXT NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_shortcuts_updated_at ON shortcuts(updated_at DESC);

-- ====================================================================================
-- TABLE 12 : Key-Value Settings
-- ====================================================================================

CREATE TABLE kv_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    synced_at TIMESTAMPTZ DEFAULT now(),
    
    CONSTRAINT valid_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_kv_settings_updated_at ON kv_settings(updated_at DESC);

-- ====================================================================================
-- TABLE 13 : Sync Log
-- ====================================================================================

CREATE TABLE sync_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    sync_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    total_items INTEGER DEFAULT 0,
    success BOOLEAN NOT NULL,
    duration_ms INTEGER,
    error_message TEXT
);

CREATE INDEX idx_sync_log_time ON sync_log(sync_time DESC);
CREATE INDEX idx_sync_log_success ON sync_log(success);

-- ====================================================================================
-- VUES UTILES
-- ====================================================================================

CREATE VIEW active_goals AS
SELECT * FROM goals WHERE is_deleted = false;

CREATE VIEW active_tasks AS
SELECT * FROM tasks WHERE is_deleted = false;

CREATE VIEW active_intervals AS
SELECT * FROM intervals WHERE is_deleted = false;

CREATE VIEW recent_changes AS
SELECT 
    'goal' as entity_type, 
    id, 
    name as title, 
    updated_at, 
    synced_at,
    is_deleted
FROM goals
WHERE updated_at > EXTRACT(EPOCH FROM (now() - interval '7 days'))
UNION ALL
SELECT 
    'task' as entity_type, 
    id, 
    text as title, 
    updated_at, 
    synced_at,
    is_deleted
FROM tasks
WHERE updated_at > EXTRACT(EPOCH FROM (now() - interval '7 days'))
ORDER BY updated_at DESC;

-- ====================================================================================
-- FONCTIONS UTILITAIRES
-- ====================================================================================

-- Nettoyer les vieux logs (90 jours)
CREATE OR REPLACE FUNCTION cleanup_old_sync_logs()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM sync_log 
    WHERE sync_time < now() - interval '90 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ====================================================================================
-- ROW LEVEL SECURITY (désactivé pour simplifier)
-- ====================================================================================

ALTER TABLE goals DISABLE ROW LEVEL SECURITY;
ALTER TABLE intervals DISABLE ROW LEVEL SECURITY;
ALTER TABLE tasks DISABLE ROW LEVEL SECURITY;
ALTER TABLE task_folders DISABLE ROW LEVEL SECURITY;
ALTER TABLE checklists DISABLE ROW LEVEL SECURITY;
ALTER TABLE checklist_items DISABLE ROW LEVEL SECURITY;
ALTER TABLE events DISABLE ROW LEVEL SECURITY;
ALTER TABLE event_templates DISABLE ROW LEVEL SECURITY;
ALTER TABLE repeatings DISABLE ROW LEVEL SECURITY;
ALTER TABLE notes DISABLE ROW LEVEL SECURITY;
ALTER TABLE shortcuts DISABLE ROW LEVEL SECURITY;
ALTER TABLE kv_settings DISABLE ROW LEVEL SECURITY;
ALTER TABLE sync_log DISABLE ROW LEVEL SECURITY;

-- ====================================================================================
-- VALIDATION
-- ====================================================================================

DO $$
BEGIN
    RAISE NOTICE '✅ Schéma timeto créé avec succès !';
    RAISE NOTICE 'Tables : %', (SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'timeto');
    RAISE NOTICE 'Index : %', (SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'timeto');
    RAISE NOTICE 'Vues : %', (SELECT COUNT(*) FROM information_schema.views WHERE table_schema = 'timeto');
END $$;

-- ====================================================================================
-- FIN DU SCHÉMA
-- ====================================================================================

