# CodeTracker database migrations

Run migrations in filename order from the Supabase SQL Editor while connected
to the same database used by Railway.

## 20260829: user preferences and Echo history

`20260829_user_preferences_and_chat_history.sql` adds only new tables,
constraints, indexes, and row-level-security settings. It does not modify or
delete existing classroom, activity, submission, or user rows.

Recommended release order:

1. Take a Supabase backup or confirm point-in-time recovery is available.
2. Run the migration once and confirm it finishes successfully.
3. Deploy the backend containing the matching JPA entities and endpoints.
4. Deploy the frontend containing theme sync and Echo thread history.
5. Smoke-test theme persistence, thread continuation, deletion, and classroom
   access with separate professor and student accounts.

The backend defaults Echo to 12 messages per user per minute. Set the optional
Railway variable `CHATBOT_REQUESTS_PER_MINUTE` if a different limit is needed;
keep it low enough to protect Gemini usage costs.

The migration is transactional and idempotent for normal reruns. If the code
must be rolled back, roll back the application first and leave these empty/new
tables in place; the old code does not use them. Dropping tables is deliberately
not part of the rollback path because it would destroy user preferences and
conversation history.
