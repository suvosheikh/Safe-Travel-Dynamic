# Project Custom Rules & Coding Persona

The following persistent instructions must guide all future updates, enhancements, and refactoring tasks within the **Safe Travel** application.

## 1. Code Modularity & File Separation Rules (CRITICAL)
- **Zero Monolithic Bloat**: Strictly avoid placing all screens, models, components, state machines, or ViewModels in a single massive file (e.g., `MainActivity.kt`).
- **Clean Package Hierarchy**: Organize Kotlin code into distinct package folders:
  - `com.safetravel.tracker.ui.screens`: For main tabs/views (e.g., `HomeScreen.kt`, `HistoryScreen.kt`, `TravelScreen.kt`, `WalletScreen.kt`, `ProfileScreen.kt`).
  - `com.safetravel.tracker.ui.components`: For highly reusable, small, atomic UI elements (e.g., `BarItem.kt`, `BannerCarousel.kt`, custom grid components).
  - `com.safetravel.tracker.ui.theme`: Centralized colors, shapes, typography, and styling presets (`Theme.kt`, `Color.kt`, etc.).
  - `com.safetravel.tracker.viewmodel`: Dedicated architecture ViewModels (e.g., `SafeTravelViewModel.kt`).
- **Dry & Reusable Components**: Avoid duplicating code or styles. Extract double-used elements into generic, dynamic Composables that accept parameter customizer properties.

## 2. Platform Guidelines & UI Polish
- **Edge-to-Edge Execution**: Respect top status bars, notches, & bottom gesture bars using system Insets (`WindowInsets.safeDrawing`, `.navigationBarsPadding()`).
- **High-Contrast Dark Theme**: Keep the luxury, cybernetic-futuristic visual identity using slate dark canvases, neon emerald/red feedback indicators, and gold badges.


## 3. Database schema
- **Always check Database** "F:\Android studio\safe-travel\database.sql" file.
Here is database table schema sql instruction.