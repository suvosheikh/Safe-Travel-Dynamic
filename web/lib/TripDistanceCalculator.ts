/**
 * TripDistanceCalculator.ts
 * 
 * Precision real-route distance calculator for SafeTravel trips.
 * Strictly adheres to the zero-guess policy: only calculates distance
 * from real hardware GPS sensor logs (recorded total_distance or 
 * actual breadcrumbs in route_path_log). Never uses planned route estimates.
 */

import { TravelActivity, Trip } from './supabase';

/**
 * Standard Haversine formula to compute great-circle distance between two GPS coordinates in kilometers.
 */
export function haversineDistance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const EARTH_RADIUS_KM = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return EARTH_RADIUS_KM * c;
}

/**
 * Calculates the actual real-world distance traveled by the user for a single trip.
 * 
 * Priority:
 * 1. Hardware/sensor recorded `total_distance` column (if > 0).
 * 2. Cumulative real distance along actual GPS coordinates stored in `route_path_log`.
 *    - Filters stationary GPS drift jitter (< 8 meters).
 *    - Filters unrealistic satellite teleports (> 5 kilometers per point).
 * 3. Returns 0 if no real route was traveled or logged.
 */
export function calculateRealTripDistance(
  activity: TravelActivity | Trip | null | undefined
): number {
  if (!activity) return 0;

  // 1. Check if hardware sensor already saved a valid real distance
  const recorded = Number(activity.total_distance);
  if (!isNaN(recorded) && recorded > 0) {
    return recorded;
  }

  // 2. Compute cumulative distance from actual breadcrumbs in route_path_log
  let path = activity.route_path_log;
  if (typeof path === 'string') {
    try {
      path = JSON.parse(path);
    } catch {
      return 0;
    }
  }

  if (!Array.isArray(path) || path.length < 2) {
    return 0;
  }

  let accumulatedKm = 0;
  let lastLat: number | null = null;
  let lastLng: number | null = null;

  for (const pt of path) {
    if (!pt) continue;

    const lat = typeof pt.lat === 'number' ? pt.lat : parseFloat(pt.lat);
    const lng = typeof pt.lng === 'number' ? pt.lng : parseFloat(pt.lng);

    if (isNaN(lat) || isNaN(lng)) continue;

    if (lastLat !== null && lastLng !== null) {
      const stepKm = haversineDistance(lastLat, lastLng, lat, lng);
      // Filter out small stationary indoor GPS jitter (< 8 meters / 0.008 km)
      // Filter out unrealistic GPS teleport jumps (> 5 km)
      if (stepKm >= 0.008 && stepKm <= 5.0) {
        accumulatedKm += stepKm;
        lastLat = lat;
        lastLng = lng;
      }
    } else {
      lastLat = lat;
      lastLng = lng;
    }
  }

  return accumulatedKm;
}

/**
 * Calculates the total real distance traveled across an array of trips/activities.
 */
export function calculateTotalRealDistance(
  activities: (TravelActivity | Trip)[] | null | undefined
): number {
  if (!Array.isArray(activities) || activities.length === 0) {
    return 0;
  }

  return activities.reduce((acc, curr) => {
    return acc + calculateRealTripDistance(curr);
  }, 0);
}

/**
 * Calculates and formats the real elapsed duration between start_time and end_time.
 * Strictly calculates real elapsed travel time from user's trip start to trip end.
 */
export function formatRealTripDuration(
  activity: TravelActivity | Trip | null | undefined
): string | null {
  if (!activity) return null;

  const start = activity.start_time || activity.created_at;
  if (!start) return null;

  const startMs = new Date(start).getTime();
  const status = ('status' in activity ? activity.status : (activity as TravelActivity).safety_status) || '';
  const isOngoing = status === 'ongoing' || (!activity.end_time && status !== 'sos' && status !== 'completed');
  const endMs = activity.end_time ? new Date(activity.end_time).getTime() : (isOngoing ? Date.now() : null);

  if (isNaN(startMs) || endMs === null || isNaN(endMs) || endMs < startMs) {
    return null;
  }

  const diffMinutes = Math.round((endMs - startMs) / 60000);

  if (diffMinutes >= 60) {
    const hours = Math.floor(diffMinutes / 60);
    const mins = diffMinutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  }

  if (diffMinutes >= 1) {
    return `${diffMinutes} min`;
  }

  return '< 1 min';
}

