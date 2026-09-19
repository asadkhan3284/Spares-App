# Spares Register (Android)

A native Android port of the "Spares Register — Offline Edition" HTML tool, connected
directly to **OneDrive** and **SharePoint** via Microsoft Graph, instead of requiring a
manual file upload. Read-only viewer: browses your spares inventory spreadsheet and part
photos straight from the cloud, with the same filtering, sorting, dashboard and photo
matching behavior as the original tool, plus camera barcode/QR scan-to-search.

## What it does

- Sign in with a **personal Microsoft account or a work/school (Microsoft 365) account**.
- Pick your inventory spreadsheet (`.xlsx` / `.csv`) and your part-photos folder directly
  from OneDrive or a SharePoint site, using an in-app browser (no manual export/upload).
  The app remembers your last picks and re-fetches from the same location until you change it.
- Parses the spreadsheet with the exact same column-header aliases and stock-status rules
  as the original HTML tool (`Material #`, `Old Material #`, `Safety Stock`, `ROP`, etc. —
  see `InventoryParser.kt`).
- Matches photos to parts by **Old Material #** first, falling back to **Material #**,
  grouping `PARTNO.jpg`, `PARTNO (1).jpg`, `PARTNO (2).jpg` — same rule as the desktop tool.
- List, grid, and dashboard (KPI) views; search; filters by stock status, controller, unit,
  area, and photo presence; sortable by material #, description, qty, or price.
- Fullscreen photo viewer with pinch-to-zoom, pan, and swipe between a part's photos.
- Camera barcode/QR scanning to jump straight to a part's record.
- Export the current filtered view to a new local `.xlsx` (never writes back to the source
  file — this app is a read-only viewer of your OneDrive/SharePoint data).
- The spreadsheet is cached locally (Room) so the list/dashboard still work offline; photos
  are always fetched fresh from OneDrive/SharePoint (not cached), matching what you asked for.

## Architecture

- **UI**: Jetpack Compose + Navigation Compose, Material 3.
- **Auth**: MSAL (`com.microsoft.identity.client:msal`), single-account mode, `common`
  authority with `AzureADandPersonalMicrosoftAccount` audience so both account types work.
- **Graph API**: Retrofit + OkHttp + Moshi, `graph/` package. `GraphRepository` exposes
  source browsing (My OneDrive + followed/searched SharePoint sites), folder listing, and
  file download.
- **Parsing**: `data/InventoryParser.kt` (xlsx via `fastexcel-reader`, csv via a small
  built-in parser) — a direct Kotlin port of the HTML tool's `parseWorkbookRows()` /
  `deriveStatus()` logic, same header aliases, same status thresholds.
- **Cache**: Room (`data/local/`) for the spreadsheet only; DataStore Preferences remembers
  the picked OneDrive/SharePoint file & folder locations.
- **Export**: `export/ExcelExporter.kt` using `fastexcel` (writer) via Storage Access Framework.

## Required setup: Azure AD (Entra ID) app registration

Every third-party app needs to be registered with Microsoft before it can call Microsoft
Graph on a user's behalf — this is a one-time step you (or your IT admin) do once, not
something specific to this codebase.

1. Go to **[entra.microsoft.com](https://entra.microsoft.com)** → **App registrations** →
   **New registration**.
   - Any Microsoft account can register an app for itself. If your organization's IT has
     locked down app registration, ask them to do this step or grant you the
     "Application Developer" role — it's a one-time five-minute ask.
2. **Supported account types**: choose *"Accounts in any organizational directory and
   personal Microsoft accounts"* — required since this app supports both.
3. Skip "Redirect URI" for now (added in step 5).
4. After creation, copy the **Application (client) ID** from the Overview page.
5. **Authentication** → **Add a platform** → **Android**:
   - Package name: `com.sparesapp.register`
   - Signature hash: generate it from your keystore:
     ```
     keytool -exportcert -alias <your-key-alias> -keystore <your-keystore-path> | openssl sha1 -binary | openssl base64
     ```
     For local debug builds, use the auto-generated debug keystore:
     ```
     keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64
     ```
6. **API permissions** → **Add a permission** → **Microsoft Graph** → **Delegated
   permissions** → add `Files.Read`, `Files.Read.All`, `Sites.Read.All`, `User.Read`,
   `offline_access`. `Sites.Read.All` may show "admin consent required" in a company
   tenant — that's normal; a tenant admin approves it once, or you can test first with a
   personal OneDrive account which doesn't need it.
7. Plug in your values:
   - `app/src/main/res/raw/msal_config.json` → `client_id` and `redirect_uri`
     (`msauth://com.sparesapp.register/<your-signature-hash>`, URL-encode `+`, `/`, `=`
      in the hash if present).
   - `app/src/main/res/values/msal_config_values.xml` → `msal_signature_hash_path`
     (`/<your-signature-hash>`, matching the manifest's redirect intent-filter).

## Building

Requires Android Studio (or the command line with an Android SDK installed) — this app
was scaffolded in a sandboxed environment without network access to Google's Maven
repository or an installed Android SDK, so a full Gradle build could not be run here.
Every API surface used (MSAL, Retrofit/Moshi, Room, fastexcel, CameraX/ML Kit, Compose
Navigation/Material3) was individually verified against upstream source, but you should
open the project in Android Studio and do a first build/sync there before anything else,
since that's the first environment that can actually compile it end-to-end.

```
./gradlew assembleDebug
```

or open the project root in Android Studio and let it sync/build normally.

## Distribution

You said you'd like Google Play's private/internal testing track:

1. Create an app entry in the [Play Console](https://play.google.com/console).
2. **Testing** → **Internal testing** → create a release, upload a signed `.aab`
   (`./gradlew bundleRelease` once you have a release signing config).
3. Add your team's email addresses as internal testers; they install via the opt-in link
   Play Console gives you — no public listing required.

## Notes on scope

- This is a **read-only viewer** by design (per your answers) — it never writes back to
  the OneDrive/SharePoint spreadsheet.
- Access control follows OneDrive/SharePoint's own sharing permissions — whoever can open
  the file/folder in Microsoft's apps can view it here; there's no separate in-app role system.
- The in-app OneDrive/SharePoint browser (`ui/picker/`) lets you re-pick the inventory
  file or photos folder at any time from the settings screen (hamburger menu on Home).
