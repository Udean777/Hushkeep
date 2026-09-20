<div align="center">

# Hushkeep

**Private cloud memory vault**

*Memories, kept quietly.*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-UI-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Supabase-Backend-3ECF8E?style=flat-square&logo=supabase&logoColor=white)](https://supabase.com/)
[![Android](https://img.shields.io/badge/Android-SDK_26%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)

</div>

---

## About Hushkeep

Hushkeep gives personal photos a quiet, organized place to live. It is designed for people who want to keep meaningful memories close without turning them into public content.

Instead of a social feed, Hushkeep provides a private archive built around albums, dates, captions, favorites, and reliable cloud backup. Your memories stay connected to your account, not to a public profile or a stream of reactions.

Hushkeep works well for:

- Family moments that should remain personal.
- Travel photos that are easier to revisit in one timeline.
- Personal journals made from photos and short captions.
- Everyday moments that are worth keeping but do not need to be shared.

## What you can do

| Area | What it does |
| --- | --- |
| **Capture** | Import one photo or a batch of photos from the Android photo picker. |
| **Organize** | Create private albums and arrange memories by captured date. |
| **Describe** | Add a short caption to give a photo its context. |
| **Find** | Search by caption, album, favorite state, or date. |
| **Keep close** | Mark important memories as favorites for quick access. |
| **Back up** | Upload media to a private Supabase Storage bucket and track sync status. |
| **Recover** | Move photos to trash, restore them when needed, or delete them permanently. |
| **Take your data** | Download an individual photo or export an album or the entire vault. |
| **Stay in control** | Delete the account and its associated cloud data from Settings. |

### A typical Hushkeep flow

1. Create an account or sign in with email and password.
2. Import one or more photos from the gallery.
3. Add a caption, choose an album, or mark a photo as a favorite.
4. Let Hushkeep sync the memory to your private cloud vault.
5. Return to the timeline, albums, or search whenever you want to revisit it.
6. Export, restore, or permanently delete the memory when you choose.

## Privacy by design

Privacy is part of the product structure, not an optional social setting.

- **Private by default**: Hushkeep has no public profiles, public feed, likes, or followers.
- **Account-owned data**: PostgreSQL Row Level Security limits records to their authenticated owner.
- **Private media**: Photos are stored in the `hushkeep-private` bucket, which is not public.
- **Signed access**: Media is accessed through signed URLs rather than public file URLs.
- **User-controlled deletion**: Trash, permanent deletion, and account deletion are available in the app.
- **Export access**: Users can download individual photos or export their stored memories.
- **Protected secrets**: The Supabase service role key belongs only in the Edge Function environment and is never shipped in the Android app.

Hushkeep uses the standard encryption-at-rest protections provided by its storage infrastructure. The Android app is structured so stronger client-side encryption can be introduced without changing the user-facing memory model.

## How the app is built

Hushkeep uses a local-first workflow for the parts of the app that need to remain responsive. Local metadata, cached memories, and upload jobs are stored with Room. WorkManager handles queued uploads, retry behavior, and background cloud synchronization.

The cloud layer is kept behind repository interfaces. This lets the UI work with local state while cloud operations run independently and report their sync state back to the user.

### Main layers

| Layer | Responsibility |
| --- | --- |
| **Presentation** | Jetpack Compose screens, navigation, ViewModels, UI state, and user-facing errors. |
| **Domain** | Memory, album, media, upload, sync, and storage models plus repository contracts. |
| **Data** | Room database, local entities, mappers, session storage, and Supabase data sources. |
| **Sync** | WorkManager workers for cloud metadata sync, media upload, retry, and conflict handling. |
| **Backend** | Supabase Auth, PostgreSQL, private Storage, RLS policies, signed URLs, and Edge Functions. |

### Media storage model

Media objects are stored under an account-owned path inside the private bucket:

```text
hushkeep-private/<user-id>/<media-id>/original.<extension>
```

The user ID at the beginning of the path gives Storage policies a clear ownership boundary. The media metadata stored in PostgreSQL uses the same owner ID and is protected by RLS.

## Tech stack

### Android

- Kotlin 2.2.10
- Jetpack Compose and Material 3
- MVVM with Repository architecture
- Coroutines and StateFlow
- Hilt
- Navigation Compose
- Coil for image loading
- Room for local persistence
- DataStore for preferences and session-related local state
- WorkManager for uploads, retry, and cloud sync
- Android Photo Picker for gallery imports
- Android Keystore and BiometricPrompt integration points

### Backend and storage

- Supabase Auth for account authentication
- Supabase PostgreSQL for profiles, albums, memories, and media metadata
- PostgreSQL Row Level Security for per-account data isolation
- Supabase Storage for private media objects
- Signed URLs for media access
- Supabase Edge Functions for privileged account deletion work
- `supabase-kt` for Android integration

## Run locally

### Requirements

- Android Studio
- JDK 17
- Android SDK with compile and target SDK 37
- A Supabase project for authentication and cloud features
- An Android emulator or physical device running Android 8.0/API 26 or newer

### Configure the Android app

Create or edit `local.properties` in the project root:

```properties
hushkeep.supabase.url=https://your-project.supabase.co
hushkeep.supabase.anonKey=your-anon-key
hushkeep.environment=local
```

`local.properties` is ignored by Git and should never be committed. The Android app only needs the Supabase project URL and the public anonymous key. Never put the service role key in this file.

### Configure Supabase

Apply the database and Storage policies from the migration files:

```text
supabase/migrations/
```

The migrations create the core tables, indexes, RLS policies, and the private `hushkeep-private` Storage bucket.

Deploy the account deletion function after setting its server-side secrets:

```text
supabase/functions/delete-account/index.ts
```

The function requires these values in the Supabase Edge Function environment:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

The service role key must remain server-side. It must not be added to `local.properties`, `BuildConfig`, source code, or the APK.

### Build and test

Run the standard validation command:

```bash
./gradlew test lintDebug assembleDebug
```

For an installable debug build:

```bash
./gradlew installDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For release compilation:

```bash
./gradlew assembleRelease
```

Release signing is environment-specific. Configure signing credentials outside the repository before producing a distributable release APK or bundle.

## Project structure

```text
Hushkeep/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/ssajudn/hushkeep/
│       │   │   ├── core/        # config, networking, sync, and shared utilities
│       │   │   ├── data/        # Room, local storage, repositories, and Supabase sources
│       │   │   ├── domain/      # models and repository contracts
│       │   │   ├── feature/     # auth, memories, albums, settings, and privacy screens
│       │   │   ├── navigation/  # authentication and main navigation graphs
│       │   │   └── ui/theme/    # colors, typography, shapes, dimensions, and themes
│       │   ├── res/             # Android resources and launcher assets
│       │   └── AndroidManifest.xml
│       ├── test/                # unit tests
│       └── androidTest/         # instrumentation and Compose tests
├── supabase/
│   ├── migrations/              # PostgreSQL schema, RLS, and Storage policies
│   └── functions/               # privileged Edge Functions
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

## Data and account lifecycle

Hushkeep keeps the local and cloud sides of a memory connected through stable IDs:

1. A selected photo is represented locally with memory and media metadata.
2. The upload queue stores the local URI and retry state.
3. WorkManager uploads the media to the owner-specific Storage path.
4. Cloud metadata is synchronized after the media upload succeeds.
5. A signed URL is created when the app needs to display cloud-only media.
6. Deleting an account removes the user's cloud account and associated Storage objects through the protected Edge Function.

The UI receives generic, user-friendly error messages instead of raw Supabase or network errors. Detailed provider errors stay out of the user-facing surface.

## Privacy policy

The app includes an in-app Privacy Policy screen covering:

- Account and profile data.
- Photo metadata and private media storage.
- Supabase as the cloud service provider.
- Backup, signed media access, export, trash, and deletion behavior.
- Account deletion and related data cleanup.

The policy should be reviewed and completed with the final privacy contact and legal details before public distribution.

## Development notes

- The active authentication flow uses email and password.
- The OTP screen and repository functions are prepared in the codebase but are not used by the active navigation flow.
- The app is photo-focused and uses Android Photo Picker for media selection.
- Network failures are handled through sync state, retry behavior, and generic UI messages.
- Cloud access depends on the Supabase URL, anonymous key, migrations, and Storage policies being configured correctly.

## License

No open-source license has been declared for this repository yet. Treat the code as proprietary unless the project owner adds a license file.
