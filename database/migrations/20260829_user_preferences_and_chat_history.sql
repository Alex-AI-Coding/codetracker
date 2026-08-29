BEGIN;

CREATE TABLE IF NOT EXISTS public.user_preference (
    user_id uuid PRIMARY KEY,
    theme_preference varchar(16) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.chat_thread (
    thread_id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    classroom_id uuid NULL,
    title varchar(100) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.chat_message (
    message_id uuid PRIMARY KEY,
    thread_id uuid NOT NULL,
    role varchar(16) NOT NULL,
    content text NOT NULL,
    position integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_user_preference_user'
          AND conrelid = 'public.user_preference'::regclass
    ) THEN
        ALTER TABLE public.user_preference
            ADD CONSTRAINT fk_user_preference_user
            FOREIGN KEY (user_id)
            REFERENCES public.users(user_id)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_user_preference_theme'
          AND conrelid = 'public.user_preference'::regclass
    ) THEN
        ALTER TABLE public.user_preference
            ADD CONSTRAINT chk_user_preference_theme
            CHECK (theme_preference IN ('SYSTEM', 'LIGHT', 'DARK'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_chat_thread_user'
          AND conrelid = 'public.chat_thread'::regclass
    ) THEN
        ALTER TABLE public.chat_thread
            ADD CONSTRAINT fk_chat_thread_user
            FOREIGN KEY (user_id)
            REFERENCES public.users(user_id)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_chat_thread_classroom'
          AND conrelid = 'public.chat_thread'::regclass
    ) THEN
        ALTER TABLE public.chat_thread
            ADD CONSTRAINT fk_chat_thread_classroom
            FOREIGN KEY (classroom_id)
            REFERENCES public.classroom(classroom_id)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_chat_message_thread'
          AND conrelid = 'public.chat_message'::regclass
    ) THEN
        ALTER TABLE public.chat_message
            ADD CONSTRAINT fk_chat_message_thread
            FOREIGN KEY (thread_id)
            REFERENCES public.chat_thread(thread_id)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_chat_message_role'
          AND conrelid = 'public.chat_message'::regclass
    ) THEN
        ALTER TABLE public.chat_message
            ADD CONSTRAINT chk_chat_message_role
            CHECK (role IN ('USER', 'ASSISTANT'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_chat_message_position'
          AND conrelid = 'public.chat_message'::regclass
    ) THEN
        ALTER TABLE public.chat_message
            ADD CONSTRAINT chk_chat_message_position
            CHECK (position > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_chat_message_thread_position'
          AND conrelid = 'public.chat_message'::regclass
    ) THEN
        ALTER TABLE public.chat_message
            ADD CONSTRAINT uk_chat_message_thread_position
            UNIQUE (thread_id, position);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_chat_thread_user_updated
    ON public.chat_thread (user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_thread_classroom
    ON public.chat_thread (classroom_id);

CREATE INDEX IF NOT EXISTS idx_chat_message_thread_position
    ON public.chat_message (thread_id, position);

-- These tables contain private account and conversation data. Enabling RLS
-- prevents Supabase anon/authenticated API roles from reading them. The direct
-- PostgreSQL owner connection used by the Spring backend is not affected.
ALTER TABLE public.user_preference ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_thread ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_message ENABLE ROW LEVEL SECURITY;

COMMIT;
