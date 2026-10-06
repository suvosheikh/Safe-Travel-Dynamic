# Project Rules & Guidelines for SafeTravel

## 1. Android Build & Execution Rule (CRITICAL)
- **NEVER** run Android Gradle build or execution commands (e.g., `./gradlew assembleDebug`, `./gradlew build`, `compileDebugKotlin`, `installDebug`, etc.) in the terminal or background.
- The user builds, compiles, and deploys the Android application using **Android Studio**.
- Whenever an Android app build or test run on device/emulator is needed after Kotlin/Java/XML code changes, **prompt the user** to open Android Studio and click **Run (▶️)** or **Build APK**.

---

## 2. Feature & Design Preservation Rule (Regression Prevention)
- **Never Break Existing Features**: When implementing new updates, bug fixes, or enhancements, NEVER break, modify, or delete existing features, screens, buttons, HUD controls, or design layouts without explicit user consent.
- **Lightweight Impact Analysis**: Before modifying an existing function or shared state, check its immediate call sites using targeted grep search. Avoid broad, wasteful scans.
- **Surgical, Targeted Edits**: Avoid broad, sweeping file overwrites. Only modify the specific lines and blocks required for the task.
- **Think Before Action ("agei code koro na")**: Do not rush to write code. Analyze the requirements, check potential side-effects on existing features, and formulate a clear approach first.

---

## 3. Strictly NO Mock Data Policy (Zero Hardcoded/Mock Values Anywhere)
- **Zero Mock Data in UI or Logic**: Never hardcode mock data, dummy names, fake phone numbers, dummy cards, placeholder guardians, or artificial fallback values directly in UI components, JSX/TSX, or business logic.
- **Database-First Data Principle**: Even if data is needed purely for UI demonstration, visual testing, or presentation, it MUST be inserted/seeded into the database (Supabase/PostgreSQL) first. The UI must ALWAYS fetch and render data dynamically from the database.
- **Honest Empty States**: If the database has no records for a user or section (e.g., no guardians or trips), render an honest empty state with clear calls to action (e.g., "+ Add Guardian") rather than hardcoding fake placeholder items.
- **Real GPS Hardware & Telemetry**: All map positions, live movements, breadcrumb trails, distances, and speeds must strictly originate from the device's real GPS sensor, Mapbox directions routing, or Supabase live records.

---

## 4. Code Modularity & Small Support Files Policy
- **Prevent File Bloat**: Do NOT bloat primary files like `SafeTravelViewModel.kt` or `TravelScreen.kt` with large blocks of business logic or mathematical calculations.
- **Dedicated Support Files**: For new algorithms, mathematical formulas, device helpers, or data parsers, create small, isolated, and focused utility files (e.g., `TripTelemetryCalculator.kt`, `AddressUtils.kt`, `DateTimeUtils.kt`, `AudioRecordHelper.kt`).
- **Clean Call-Site Integration**: Keep the integration footprint in ViewModels or UI screens minimal (1–2 lines calling the helper methods). Small files drastically cut down token consumption.
- **Compose On-Demand Star Imports (`.*`)**: To prevent import bloat and save context tokens in Jetpack Compose files, use on-demand star imports for standard Compose packages (e.g., `androidx.compose.foundation.layout.*`, `androidx.compose.material3.*`, `androidx.compose.runtime.*`, `androidx.compose.foundation.*`).

---

## 5. Token Conservation & Ultra-Lean Inspection Policy
- **Zero Full-File Dumps**: NEVER view entire large files (e.g., 1000+ line ViewModels or Screens) in a single tool call. Always specify tight line ranges (`StartLine` and `EndLine` for 20–40 lines max) after finding the line number via grep.
- **Restricted Search Scope**: Always scope search and grep tools to the specific relevant package/directory rather than scanning the entire repository.
- **No Redundant Analysis**: For isolated UI or logic adjustments, only inspect the immediately relevant component. Avoid scanning unrelated modules.
- **Concise Responses**: Keep explanations focused, structured, and to the point to prevent context bloat and preserve API quota.

---

## 6. Full-Stack Synchronization & Supabase Egress Protection
- **Multi-Platform Consistency**: Keep data models and table columns aligned between Android (`SupabaseModels.kt`), the Web Portal (`web/lib/supabase.ts`), and Supabase PostgreSQL.
- **Zero Egress Waste**:
  - Live tracking during active trips must prioritize persistent Supabase Realtime WebSockets (`SupabaseRealtimeBroadcaster`) rather than high-frequency HTTP REST polling.
  - Master data / static catalogs (hospitals, police stations, hotlines) must be cached locally; never poll static tables on high-frequency loops.

---

## 7. UI/UX Consistency & Visual Identity
- **Design Language**: Maintain the sleek, borderless, frosted-glass cyber aesthetic (dark slate canvases, neon cyan `#06B6D4`, emerald green `#10B981`, and high-contrast red `#EF4444` for SOS).
- **Human-Friendly Language**: Use clean, everyday language (e.g., "Trip History", "Start Journey", "Emergency Guardians"). Strictly avoid robotic or sci-fi jargon (e.g., "telemetry airlock", "vessel performance").
- **Edge-to-Edge & Safe Insets**: Respect system navigation bars and status bars using appropriate window insets (`navigationBarsPadding()`) so floating HUDs and buttons never overlap or get clipped.

---

## 8. Communication, Planning & User Approval Workflow (CRITICAL)
- **Bengali Communication**: Any implementation plan, proposal, or architectural breakdown MUST be presented in Bengali.
- **Explicit Approval Before Code Changes**: Before modifying or creating code, clearly explain the proposed changes and ask the user: "এটা কি ইমপ্লিমেন্ট করব?" (Shall I implement this?).
- **Iterative Discussion & Active Listening**: If the user wants to discuss, adjust, or refine the plan, carefully note down all points and constraints, adapt the strategy accordingly, and only proceed to code after explicit user consent.

---

## 9. Strictly NO Emojis Policy (Use Vector Icons Instead)
- **Zero Raw Emojis**: NEVER use raw emoji characters (e.g., 🚨, 🚗, 🛡️, ⚠️, 📍, etc.) in backend metadata, context parameters (e.g., Cloudinary, Supabase), API payloads, database columns, logging, or UI texts.
- **Why**: Multi-byte 4-byte Unicode emojis break third-party API multipart parsers (e.g., Cloudinary `Context Invalid encoding in context`), corrupt string encodings, and look unprofessional.
- **Vector Icons for UI**: When visual indicators, buttons, or decorative elements are needed in the UI, strictly use official Jetpack Compose Material Vector Icons (`ImageVector`, `Icons.Default.*`, `Icons.Filled.*`, etc.) or dedicated SVG/XML vector drawables.
- **Clean Text for Metadata**: For labels or metadata, use clean ASCII tags (e.g., `[SOS EMERGENCY]`, `[TRIP AUDIO]`, `[SAFETY EVIDENCE]`) instead of emoji icons.

---

## 10. Google Play Store Policy & Security Compliance (CRITICAL)
- **Foreground Service Transparency**: Whenever live tracking, audio blackbox recording, or SOS telemetry runs in the background, strictly maintain an active, non-dismissible user notification with accurate `foregroundServiceType` (`location`, `microphone`, etc.) declared in `AndroidManifest.xml`. Never run stealth or hidden background operations.
- **Prominent In-App Disclosures**: For sensitive runtime permissions (`ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `RECORD_AUDIO`), always provide prominent in-app disclosure dialogs explaining the exact safety purpose *before* showing the system permission dialog.
- **User Data Deletion & Privacy Rights**: Fully comply with Google Play's User Data & Account Deletion policy by providing explicit in-app options for users to delete their account data and evidence logs.
- **Target SDK & Security Standards**: Strictly target current Google Play SDK standards (Android 14 / API 34+), enforce HTTPS/TLS for all external network endpoints, and never hardcode production API secrets in plain text.
- **Zero Deceptive Patterns**: Strictly avoid deceptive, hidden, or stalkerware patterns. All safety tracking must be explicitly user-initiated or triggered by authorized safety protocols.

---

## 11. Multi-PC Git Synchronization Workflow (Home & Office Sync)
- **Repository URL**: `https://github.com/suvosheikh/Safe-Travel-Dynamic.git`
- **End of Work Session (Office or Home)**:
  - Whenever the user states they are finishing work (e.g., "office er kaj sesh", "basar kaj sesh", "kaj sesh git e upload kore daw", "git push koro", "git e rekhe daw"):
    1. Check git status to identify all changed and untracked files.
    2. Stage and commit all modifications with a clear, descriptive English commit message outlining what was done.
    3. Push the commits to the remote repository (`origin main` or active branch) so the latest code is safely backed up on GitHub.
- **Start of Work Session (Home or Office)**:
  - Whenever the user states they are switching PC or starting work (e.g., "basar pc te boslam", "office e boslam", "project sync koro", "git theke update koro", "git pull koro"):
    1. Run `git pull origin <branch>` to fetch and integrate any updates pushed from the other machine.
    2. Confirm workspace clean status and report the sync status to the user before starting work.
- **Conflict Prevention**: Always ensure uncommitted changes are resolved or committed before pulling to avoid merge clashes.
