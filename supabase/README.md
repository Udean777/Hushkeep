# Hushkeep Supabase setup

Apply the migrations in order to the target Supabase project before enabling cloud sync.

## Required manual setup

1. Apply `migrations/0001_hushkeep_mvp.sql`.
2. Apply `migrations/0002_profiles_username_and_grants.sql`.
3. Apply `migrations/0003_enable_realtime.sql`.
4. In Supabase Dashboard, open **Database → Publications → `supabase_realtime`** and confirm these tables are enabled:
   - `public.albums`
   - `public.memories`
   - `public.media_objects`
5. In **Authentication → Providers**, confirm the Email provider is enabled.
6. In **Storage**, confirm bucket `hushkeep-private` exists and is **private**.
7. Confirm the table policies and Storage policies are owner-scoped with `auth.uid()`.

The Android client listens only to rows owned by the signed-in user. The Realtime
subscription does not replace RLS; RLS remains the authorization boundary.

## Realtime smoke test

Use two test devices or emulators with the same test account:

1. Sign in on both devices.
2. Create or edit an album on device A.
3. Confirm device B updates without leaving the screen or manually refreshing.
4. Upload, favorite, restore, and delete a test memory on device A.
5. Confirm the matching timeline/trash state updates on device B.
6. Repeat with two different accounts and confirm no cross-account data appears.

If the second device does not update, check the publication first, then verify
the device is authenticated and the app has a valid Supabase URL and anon key.

Do not use a service-role key in the Android app. It must remain only in the
Edge Function environment. Supabase injects its `SUPABASE_*` platform values
into Edge Functions; do not try to create a custom secret whose name starts
with `SUPABASE_`.

The Android app expects these values through Gradle properties or environment variables:

- `hushkeep.supabase.url` or `HUSHKEEP_SUPABASE_URL`
- `hushkeep.supabase.anonKey` or `HUSHKEEP_SUPABASE_ANON_KEY`

The service role key must only exist in the Edge Function environment. It must never be placed in `local.properties`, `BuildConfig`, or the Android APK.

Deploy `functions/delete-account` only after setting `SUPABASE_SERVICE_ROLE_KEY` in the function secrets.
