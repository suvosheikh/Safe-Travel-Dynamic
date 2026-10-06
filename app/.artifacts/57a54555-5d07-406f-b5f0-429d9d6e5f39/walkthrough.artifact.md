# Dual-Route History Map Walkthrough

I have enhanced the Voyage Archive map to provide a side-by-side comparison of the planned route versus the actual path taken by the user, matching the visual style of your reference images.

## Changes Made

### 1. Planned Route Visualization (Red)
- **Dynamic Fetching:** When viewing a trip from history, the app now automatically fetches the **original suggested route** from Mapbox using the starting coordinates, destination, and the transport mode (Walk/Cycle/Car/Bus) used during that trip.
- **Styling:** This planned path is rendered as a **Red line** (`#EF4444`) with 0.6 opacity, acting as a historical reference for the intended journey.

### 2. Actual Path Visualization (Blue Neon)
- **High-Contrast Design:** The actual path taken (from recorded GPS logs) is now rendered with a dual-layer "Neon" effect:
    - **Outer Glow:** A vibrant **Blue** (`#3B82F6`) line (width 6.0).
    - **Inner Core:** A crisp **White** line (width 2.0) for precision.
- **Real-Time Data:** This line draws exactly where the user traveled based on the JSON coordinate logs.

### 3. Integrated Trip Logic
- **Transport Mode Awareness:** Updated [HistoryScreen.kt](file:///F:/Android studio/safe-travel/app/src/main/java/com/safetravel/tracker/ui/screens/HistoryScreen.kt) to pass the trip's specific transport mode to the map engine. This ensures the Red line follows roads for cars but might take shortcuts for walkers.
- **Visual Priority:** The Actual Path (Blue) is drawn on top of the Planned Path (Red), so if the user followed the route perfectly, the blue line will naturally sit within/on top of the red one.

## Verification Results
- **Build:** Successfully compiled using Gradle (`:app:assembleDebug`).
- **Logic:** Verified the Mapbox Directions API call handles "walking", "cycling", and "driving" profiles correctly based on history records.
- **UI:** Confirmed the Fullscreen button is in the top-left and markers remain the high-fidelity teardrop style.
