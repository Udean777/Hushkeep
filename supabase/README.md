# Hushkeep Supabase setup

Apply `migrations/0001_hushkeep_mvp.sql` to a Supabase project before enabling cloud sync.

The Android app expects these values through Gradle properties or environment variables:

- `hushkeep.supabase.url` or `HUSHKEEP_SUPABASE_URL`
- `hushkeep.supabase.anonKey` or `HUSHKEEP_SUPABASE_ANON_KEY`

The service role key must only exist in the Edge Function environment. It must never be placed in `local.properties`, `BuildConfig`, or the Android APK.

Deploy `functions/delete-account` only after setting `SUPABASE_SERVICE_ROLE_KEY` in the function secrets.
