# Dual-Route History Map Plan

Implement a "Planned vs. Actual" route visualization in the History Map to show where the user intended to go versus the path they actually took.

## User Review Required

> [!IMPORTANT]
> - **Planned Route (Red):** This line represents the path suggested by Mapbox based on the user's initial destination and transport mode. It will be shown in **Red**.
> - **Actual Route (Blue):** This line represents the actual GPS coordinates recorded during the trip. It will be shown in **Blue** with a white core (Neon effect).
> - **Transport Mode Logic:** The map will automatically fetch the appropriate route (Walking, Cycling, Driving) based on the history record to draw the planned path accurately.

## Proposed Changes

### [Component: UI - Components]

#### [MODIFY] [HistoryMapView.kt](file:///F:/Android studio/safe-travel/app/src/main/java/com/safetravel/tracker/ui/components/travel/HistoryMapView.kt)
- **New Parameter:** Add `transportMode: String?` to the component.
- **Planned Path Logic:**
    - Use `LaunchedEffect` to call the Mapbox Directions API using the `startPoint`, `endPoint`, and `transportMode`.
    - Store the result in a `plannedPath` state.
- **Dual Rendering:**
    - Render the **Planned Path (Red)** using `#EF4444`.
    - Render the **Actual Path (Blue)** using `#3B82F6` on top of the red line.
- **Layering:** Ensure the actual path is drawn last so it appears on top when the routes overlap.

### [Component: UI - Screens]

#### [MODIFY] [HistoryScreen.kt](file:///F:/Android studio/safe-travel/app/src/main/java/com/safetravel/tracker/ui/screens/HistoryScreen.kt)
- Update `HistoryMapView` invocation in `TripDetailSheet` to pass `trip.transportMode`.

## Verification Plan

### Manual Verification
- Open a trip from the History Archive.
- **Verification:**
    - Verify a **Red line** appears showing the suggested route.
    - Verify a **Blue line** appears showing the actual path recorded in the JSON log.
    - If the user deviated from the suggested route, both lines should be clearly visible separately.
    - If they followed the route perfectly, the Blue line should sit on top of the Red line.
