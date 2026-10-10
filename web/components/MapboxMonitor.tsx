'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Trip, Profile } from '../lib/supabase';
import PublicTrackingHud, { PublicTelemetry } from './tracking/PublicTrackingHud';
import SpeedometerWidget from './tracking/SpeedometerWidget';

export type { PublicTelemetry };

const parseCoordsFromLocation = (locationName: string | undefined): [number, number] | null => {
  if (!locationName) return null;
  const coordRegex = /[\(\[](-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)[\)\]]/;
  const match = locationName.match(coordRegex);
  if (match) {
    let val1 = parseFloat(match[1]);
    let val2 = parseFloat(match[2]);
    
    // Validate to ensure Lat is within bounds [-90, 90].
    // If val1 > 90 or val1 < -90, it must be Longitude.
    // In BD, Lng is ~90 and Lat is ~23.
    if (Math.abs(val1) > 90 || (Math.abs(val2) <= 90 && Math.abs(val1) > 80 && Math.abs(val1) <= 180)) {
      // val1 is Lng, val2 is Lat
      return [val1, val2];
    }
    
    // Otherwise assume standard Google/text format: (Lat, Lng)
    return [val2, val1]; // Mapbox expects [Lng, Lat]
  }
  return null;
};

const getStaticLocationCoords = (locationName: string): [number, number] | null => {
  const parsed = parseCoordsFromLocation(locationName);
  if (parsed) return parsed;

  const norm = locationName.toLowerCase().trim();
  if (norm.includes('jfk') || norm.includes('airport')) return [-73.7781, 40.6413]; // JFK Airport
  if (norm.includes('downtown') || norm.includes('terminal')) return [-74.0090, 23.8103]; // Dhaka City Hall
  if (norm.includes('union') || norm.includes('square') || norm.includes('sf')) return [-122.4074, 37.7879]; // SF Union Square
  if (norm.includes('sfo') || norm.includes('international')) return [-122.3789, 37.6213]; // SFO Airport
  if (norm.includes('seattle') || norm.includes('metro')) return [-122.3321, 47.6062]; // Seattle Metro
  if (norm.includes('university') || norm.includes('district')) return [-122.3121, 47.6622]; // Seattle UW
  if (norm.includes('mall') || norm.includes('miami') || norm.includes('east')) return [-80.1918, 25.7617]; // Miami Downtown
  if (norm.includes('beach') || norm.includes('south beach') || norm.includes('reside')) return [-80.1300, 25.7781]; // Miami Beach
  
  return null;
};

const getCoordsSync = (locationName: string | undefined, cache: Record<string, [number, number]>): [number, number] => {
  if (!locationName) return [90.4125, 23.8103];
  if (cache[locationName]) return cache[locationName];
  const staticCoords = getStaticLocationCoords(locationName);
  if (staticCoords) return staticCoords;
  return [90.4125, 23.8103]; // Default Dhaka
};

const getRadarCenter = (newTripStart: string | undefined, cache: Record<string, [number, number]>, fallbackCoords?: [number, number] | null): [number, number] => {
  if (newTripStart) {
    const coords = getCoordsSync(newTripStart, cache);
    const isDhakaFallback = coords[0] === 90.4125 && coords[1] === 23.8103;
    // If it's NOT the default Dhaka fallback coordinate, or it's a current location, use it as the radar's center!
    if (!isDhakaFallback || newTripStart.toLowerCase().includes('manhattan') || newTripStart.toLowerCase().includes('current location')) {
      return coords;
    }
  }
  return fallbackCoords || [90.4125, 23.8103]; // Default to browser or Dhaka
};

const calculateSimpleHeading = (start: [number, number], end: [number, number]): number => {
  const dLng = end[0] - start[0];
  const dLat = end[1] - start[1];
  const angleRad = Math.atan2(dLng, dLat); // angle relative to North in radians
  let angleDeg = angleRad * (180 / Math.PI);
  if (angleDeg < 0) angleDeg += 360;
  return angleDeg;
};

const getRouteHeading = (coords: [number, number][]): number => {
  if (!coords || coords.length < 2) return 0;
  const p1 = coords[0];
  const p2 = coords[1];
  return calculateSimpleHeading(p1, p2);
};

const getRadarHeading = (start: { left: number, top: number }, end: { left: number, top: number }): number => {
  const dX = end.left - start.left;
  const dY = start.top - end.top;
  const angleRad = Math.atan2(dX, dY);
  let angleDeg = angleRad * (180 / Math.PI);
  if (angleDeg < 0) angleDeg += 360;
  return angleDeg;
};

// Keep for legacy compatibility check on general utility imports
const getLocationCoords = (locationName: string): [number, number] => {
  const staticCoords = getStaticLocationCoords(locationName);
  if (staticCoords) return staticCoords;
  return [90.4125, 23.8103];
};

// Default production-verified Mapbox access token
export const DEFAULT_MAPBOX_TOKEN = 'pk.eyJ1Ijoic3V2b3NoZWlraCIsImEiOiJjbXRpZmt1dnowMDJtMzFzaGJtZGR3cnBlIn0.9P1CkB5iSVCt7w3exz6dcw';

// Mapbox Token format checking helper to avoid initializing mapbox with invalid tokens
const isValidMapboxToken = (token: string | null | undefined): boolean => {
  if (!token) return false;
  const t = token.trim();
  // Mapbox public tokens start with 'pk.' and typically contain 3 dot-separated base64-encoded segments
  return (t.startsWith('pk.') || t.startsWith('sk.')) && t.split('.').length >= 3;
};

// Calculate distance in meters between two [lng, lat] points
const getCoordDistanceMeters = (p1: [number, number] | null | undefined, p2: [number, number] | null | undefined): number => {
  if (!p1 || !p2) return 0;
  const dLng = (p2[0] - p1[0]) * (Math.PI / 180) * 6378137 * Math.cos((p1[1] * Math.PI) / 180);
  const dLat = (p2[1] - p1[1]) * (Math.PI / 180) * 6378137;
  return Math.sqrt(dLng * dLng + dLat * dLat);
};

// Format timestamp into clean 12-hour AM/PM string (e.g. "9:01 AM")
const formatPuckTime = (dateMs: number): string => {
  if (!dateMs || isNaN(dateMs)) return '';
  try {
    const d = new Date(dateMs);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', hour12: true });
  } catch {
    return '';
  }
};

// Calculate realistic start and end epoch milliseconds for time interpolation
const getTripTimeRange = (trip: Trip, totalDistMeters = 0): { startMs: number; endMs: number } => {
  let startMs = trip?.start_time ? new Date(trip.start_time).getTime() : 0;
  if (!startMs && trip?.created_at) {
    startMs = new Date(trip.created_at).getTime();
  }
  if (!startMs || isNaN(startMs)) {
    startMs = Date.now() - 30 * 60 * 1000;
  }

  let endMs = trip?.end_time ? new Date(trip.end_time).getTime() : 0;
  if (!endMs || isNaN(endMs) || endMs <= startMs) {
    const distKm = (trip?.total_distance && trip.total_distance > 0)
      ? trip.total_distance
      : (totalDistMeters > 0 ? totalDistMeters / 1000 : 5);
    const estDurationMinutes = Math.max(8, Math.min(180, Math.round((distKm / 30) * 60)));
    endMs = startMs + estDurationMinutes * 60 * 1000;
  }

  return { startMs, endMs };
};

// Create a stunning block-by-block grid path simulating street routes
const generateSimulatedRoadRoute = (start: [number, number], end: [number, number]): [number, number][] => {
  const points: [number, number][] = [start];
  const lngDiff = end[0] - start[0];
  const latDiff = end[1] - start[1];
  
  if (Math.abs(lngDiff) < 0.001 && Math.abs(latDiff) < 0.001) {
    return [start, end];
  }

  // Create block-by-block turns
  points.push([start[0] + lngDiff * 0.35, start[1]]);
  points.push([start[0] + lngDiff * 0.35, start[1] + latDiff * 0.65]);
  points.push([start[0] + lngDiff * 0.85, start[1] + latDiff * 0.65]);
  points.push([start[0] + lngDiff * 0.85, end[1]]);
  points.push(end);
  
  return points;
};

// Intelligently slice a road route polyline from start up to the traveler's current coordinate.
// Finds the closest segment on the road and returns all road coordinates from index 0 up to
// that projection point, ensuring a 100% road-following green trail in real-time.
const sliceRoadRouteUpTo = (
  plannedCoords: [number, number][],
  livePt: [number, number]
): [number, number][] => {
  if (!plannedCoords || plannedCoords.length < 2) {
    return [livePt];
  }

  let minDistanceSq = Infinity;
  let bestSegmentIndex = 0;
  let bestProjectedPt: [number, number] = plannedCoords[0];

  const targetLng = livePt[0];
  const targetLat = livePt[1];
  const cosLat = Math.cos((targetLat * Math.PI) / 180);

  for (let i = 0; i < plannedCoords.length - 1; i++) {
    const p1 = plannedCoords[i];
    const p2 = plannedCoords[i + 1];

    const dx = p2[0] - p1[0];
    const dy = p2[1] - p1[1];
    const segLenSq = (dx * cosLat) * (dx * cosLat) + dy * dy;

    let t = 0;
    if (segLenSq > 1e-12) {
      const apx = (targetLng - p1[0]) * cosLat;
      const apy = targetLat - p1[1];
      const abx = dx * cosLat;
      const aby = dy;
      t = (apx * abx + apy * aby) / segLenSq;
      t = Math.max(0, Math.min(1, t));
    }

    const projLng = p1[0] + t * dx;
    const projLat = p1[1] + t * dy;

    const distSq =
      ((targetLng - projLng) * cosLat) * ((targetLng - projLng) * cosLat) +
      (targetLat - projLat) * (targetLat - projLat);

    if (distSq < minDistanceSq) {
      minDistanceSq = distSq;
      bestSegmentIndex = i;
      bestProjectedPt = [projLng, projLat];
    }
  }

  // Slice road coordinates up to the best segment
  const sliced: [number, number][] = plannedCoords.slice(0, bestSegmentIndex + 1);

  // Add projected point on road if not identical to the last point
  const lastPt = sliced[sliced.length - 1];
  if (
    Math.abs(lastPt[0] - bestProjectedPt[0]) > 0.00001 ||
    Math.abs(lastPt[1] - bestProjectedPt[1]) > 0.00001
  ) {
    sliced.push(bestProjectedPt);
  }

  // Connect directly to live point if slightly off-road (e.g. sidewalk / GPS drift)
  if (
    Math.abs(bestProjectedPt[0] - targetLng) > 0.00002 ||
    Math.abs(bestProjectedPt[1] - targetLat) > 0.00002
  ) {
    sliced.push(livePt);
  }

  return sliced;
};

// Projection mapping utility for the fallback Cyber Radar HUD
const getRadarPositions = (trip: Trip, idx: number, cache: Record<string, [number, number]>) => {
  const start = getCoordsSync(trip.start_location, cache);
  const end = getCoordsSync(trip.end_location, cache);

  // If both locations are near NY center, use realistic coordinate offset projection
  const isDhaka = Math.abs(start[0] - (90.4125)) < 2 && Math.abs(start[1] - 23.8103) < 2;

  let startLeft = 50;
  let startTop = 50;
  let endLeft = 50;
  let endTop = 50;

  if (isDhaka) {
    const scale = 0.35;
    startLeft = 50 + ((start[0] - (90.4125)) / scale) * 45;
    startTop = 50 - ((start[1] - 23.8103) / scale) * 45;
    
    endLeft = 50 + ((end[0] - (90.4125)) / scale) * 45;
    endTop = 50 - ((end[1] - 23.8103) / scale) * 45;
  } else {
    // For other cities like San Francisco, Seattle, Miami, or Dhaka, layout custom beautifully distributed HUD tracks
    const offsets = [
      { sL: 18, sT: 28, eL: 42, eT: 58 }, // Track 1
      { sL: 78, sT: 22, eL: 52, eT: 62 }, // Track 2
      { sL: 28, sT: 68, eL: 68, eT: 42 }, // Track 3
      { sL: 62, sT: 78, eL: 32, eT: 32 }, // Track 4
    ];
    const off = offsets[idx % offsets.length];
    startLeft = off.sL;
    startTop = off.sT;
    endLeft = off.eL;
    endTop = off.eT;
  }

  return {
    start: {
      left: Math.max(6, Math.min(94, startLeft)),
      top: Math.max(6, Math.min(94, startTop)),
    },
    end: {
      left: Math.max(6, Math.min(94, endLeft)),
      top: Math.max(6, Math.min(94, endTop)),
    }
  };
};

export const PUBLIC_MAPBOX_STYLE = 'mapbox://styles/suvosheikh/cmtlanria00m701sa0nh00xnk';

interface MapboxMonitorProps {
  activeTripsList: Trip[];
  profiles: Profile[];
  selectedTripId: string | null;
  onSelectTrip: (tripId: string) => void;
  onMapClick?: (lng: number, lat: number) => void;
  destinationPinCoords?: [number, number] | null;
  newTripStart?: string;
  isPublicView?: boolean;
  isModalMode?: boolean;
  customMapStyle?: string;
  publicTelemetry?: PublicTelemetry;
}

export default function MapboxMonitor({
  activeTripsList,
  profiles,
  selectedTripId,
  onSelectTrip,
  onMapClick,
  destinationPinCoords,
  newTripStart,
  isPublicView = false,
  isModalMode = false,
  customMapStyle,
  publicTelemetry,
}: MapboxMonitorProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<any>(null);
  const markersRef = useRef<{ [key: string]: any }>({});
  const startMarkersRef = useRef<{ [key: string]: any }>({});
  const destMarkersRef = useRef<{ [key: string]: any }>({});
  const actualEndMarkersRef = useRef<{ [key: string]: any }>({});
  const liveBreadcrumbsRef = useRef<{ [key: string]: [number, number][] }>({});
  
  const [mapboxToken, setMapboxToken] = useState<string>('');
  const [isTokenConfigured, setIsTokenConfigured] = useState<boolean>(false);
  const [tokenInput, setTokenInput] = useState<string>('');
  const [validationError, setValidationError] = useState<string>('');
  // Keep a stable ref of onMapClick to prevent rebinding mapbox-gl click listeners
  const onMapClickRef = useRef(onMapClick);
  useEffect(() => {
    onMapClickRef.current = onMapClick;
  }, [onMapClick]);

  const defaultStyle = customMapStyle || (isPublicView ? PUBLIC_MAPBOX_STYLE : 'mapbox://styles/mapbox/dark-v11');
  const [mapStyle, setMapStyle] = useState<string>(defaultStyle);

  useEffect(() => {
    if (customMapStyle) {
      setMapStyle(customMapStyle);
    } else if (isPublicView) {
      setMapStyle(PUBLIC_MAPBOX_STYLE);
    }
  }, [customMapStyle, isPublicView]);
  const [showConfig, setShowConfig] = useState<boolean>(false);
  const [roadGeometries, setRoadGeometries] = useState<Record<string, [number, number][]>>({});
  const [resolvedCoords, setResolvedCoords] = useState<Record<string, [number, number]>>({});
  const [previewRouteCoords, setPreviewRouteCoords] = useState<[number, number][] | null>(null);
  const [mapLoadedTrigger, setMapLoadedTrigger] = useState<number>(0);

  const [userBrowserLocation, setUserBrowserLocation] = useState<[number, number] | null>(null);

  useEffect(() => {
    if (typeof window !== "undefined" && "geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserBrowserLocation([position.coords.longitude, position.coords.latitude]);
        },
        (err) => console.warn("Geolocation denied or failed:", err),
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
      );
    }
  }, []);

  useEffect(() => {
    if (mapRef.current && userBrowserLocation && !selectedTripId) {
      try {
        mapRef.current.flyTo({
          center: userBrowserLocation,
          zoom: 13,
          speed: 1.2,
          curve: 1.42,
          essential: true
        });
      } catch (e) {}
    }
  }, [userBrowserLocation, selectedTripId, mapLoadedTrigger]);


  // Navigation simulation states
  const [interpolatedPath, setInterpolatedPath] = useState<[number, number][]>([]);
  const [simIndex, setSimIndex] = useState<number>(0);
  const [isSimulating, setIsSimulating] = useState<boolean>(false);
  const [isSimFinished, setIsSimFinished] = useState<boolean>(false);
  const [simSpeed, setSimSpeed] = useState<number>(1); // Multiplier: 1x, 2x, 5x
  const [simMuted, setSimMuted] = useState<boolean>(false);
  const [currentDisplaySpeedKmH, setCurrentDisplaySpeedKmH] = useState<number>(0);
  const [speedLimit, setSpeedLimit] = useState<number>(30);

  // Smoothen and interpolate steps for simulated navigator
  const interpolatePoints = (path: [number, number][], stepsCount = 120): [number, number][] => {
    if (!path || path.length === 0) return [];
    if (path.length === 1) return [path[0]];
    const interpolated: [number, number][] = [];
    const segments = path.length - 1;
    const stepsPerSegment = Math.ceil(stepsCount / segments);
    for (let i = 0; i < segments; i++) {
      const start = path[i];
      const end = path[i + 1];
      for (let s = 0; s < stepsPerSegment; s++) {
        const t = s / stepsPerSegment;
        const lng = start[0] + (end[0] - start[0]) * t;
        const lat = start[1] + (end[1] - start[1]) * t;
        interpolated.push([lng, lat]);
      }
    }
    interpolated.push(path[path.length - 1]);
    return interpolated;
  };

  const extractRouteLog = (log: any): [number, number][] | null => {
    if (!log) return null;
    let list = log;
    if (typeof list === 'string') {
      try {
        list = JSON.parse(list);
      } catch {
        return null;
      }
    }
    if (!Array.isArray(list) || list.length < 2) return null;
    const parsedLog = list.map((pt: any) => {
      if (pt && pt.lat != null && pt.lng != null) {
        const lat = Number(pt.lat);
        const lng = Number(pt.lng);
        if (!isNaN(lat) && !isNaN(lng)) {
          return [lng, lat] as [number, number];
        }
      }
      if (Array.isArray(pt) && pt.length >= 2) {
        const lng = Number(pt[0]);
        const lat = Number(pt[1]);
        if (!isNaN(lat) && !isNaN(lng)) {
          return [lng, lat] as [number, number];
        }
      }
      return null;
    }).filter(Boolean) as [number, number][];
    
    if (parsedLog.length >= 2) return parsedLog;
    return null;
  };

  // Helper to fetch selected trip key and raw geometry path
  const getSelectedTripPath = (): [number, number][] => {
    if (!selectedTripId) return [];
    const trip = activeTripsList.find(t => t.id === selectedTripId);
    if (!trip) return [];
    
    const isCompleted = trip.status === 'completed';
    const dbPath = extractRouteLog(trip.route_path_log);
    const plannedPath = extractRouteLog(trip.planned_route_log);

    // 1. For completed trips: ALWAYS follow the real route that the user actually traveled!
    if (isCompleted && dbPath && dbPath.length > 1) {
      return dbPath;
    }

    // 2. If planned path is available, use it for active navigation preview:
    if (plannedPath && plannedPath.length > 1) return plannedPath;

    // 3. Fallback to traveled breadcrumbs if available:
    if (dbPath && dbPath.length > 1) return dbPath;
    
    const startCoords = (trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords));
    const isUnknownDest = !trip.end_location || trip.end_location.includes('Unknown Destination');
      if (isUnknownDest) {
        if (destMarkersRef.current[trip.id]) {
          destMarkersRef.current[trip.id].remove();
          delete destMarkersRef.current[trip.id];
        }
        return []; // skip dest pin
      }
      const endCoords = (trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords));
    const key = `${trip.id}-${trip.start_location}-${endCoords[0].toFixed(4)},${endCoords[1].toFixed(4)}`;
    return roadGeometries[key] || generateSimulatedRoadRoute(startCoords, endCoords);
  };

  // Helper to compute overall heading for a trip (travel direction facing upwards)
  const getTripOverallHeading = (trip: Trip): number => {
    // 1. Check genuine traveled GPS log
    const traveledLog = extractRouteLog(trip.route_path_log);
    if (traveledLog && traveledLog.length >= 2) {
      const pStart = traveledLog[0];
      const pEnd = traveledLog[traveledLog.length - 1];
      const dist = getCoordDistanceMeters(pStart, pEnd);
      if (dist > 30) {
        return calculateSimpleHeading(pStart, pEnd);
      }
      // If round trip or start and end are close, find point furthest from start
      let maxDist = 0;
      let furthestPt = traveledLog[1];
      for (const pt of traveledLog) {
        const d = getCoordDistanceMeters(pStart, pt);
        if (d > maxDist) {
          maxDist = d;
          furthestPt = pt;
        }
      }
      if (maxDist > 30) {
        return calculateSimpleHeading(pStart, furthestPt);
      }
    }

    // 2. Check planned route log
    const plannedLog = extractRouteLog(trip.planned_route_log);
    if (plannedLog && plannedLog.length >= 2) {
      const pStart = plannedLog[0];
      const pEnd = plannedLog[plannedLog.length - 1];
      const dist = getCoordDistanceMeters(pStart, pEnd);
      if (dist > 30) {
        return calculateSimpleHeading(pStart, pEnd);
      }
    }

    // 3. Check start and end locations/coordinates
    const start = trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords);
    const end = trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords);
    if (start && end) {
      const dist = getCoordDistanceMeters(start, end);
      if (dist > 30) {
        return calculateSimpleHeading(start, end);
      }
    }

    return 0;
  };

  // 1. Generate interpolated guidance route whenever active selected trip changes (Manual preview only)
  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    const path = getSelectedTripPath();
    if (path && path.length > 1) {
      const smoothed = interpolatePoints(path, 150); // 150 dense steps for smooth transitions
      setInterpolatedPath(smoothed);
      setSimIndex(0);
      setIsSimulating(false); // Live GPS is default; do not auto-run artificial loop!
    } else {
      setInterpolatedPath([]);
      setSimIndex(0);
      setIsSimulating(false);
    }
    /* eslint-enable react-hooks/set-state-in-effect */
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedTripId, resolvedCoords, roadGeometries, activeTripsList]);

  // 2. Active timer simulation loop
  
  
  const toRad = (x: number) => (x * Math.PI) / 180;
  const haversine = (coords1: [number, number], coords2: [number, number]) => {
    if (!coords1 || !coords2) return 0;
    const R = 3958.8; // Radius of earth in miles
    const dLat = toRad(coords2[1] - coords1[1]);
    const dLon = toRad(coords2[0] - coords1[0]);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(toRad(coords1[1])) * Math.cos(toRad(coords2[1])) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };
  
  const animationRef = useRef<number>(undefined);
  const currentDistRef = useRef<number>(0);
  const currentHeadingRef = useRef<number>(0);
  const isNorthUpRef = useRef<boolean>(false);
  const currentTripHeadingRef = useRef<number>(0);

  const lerpAngle = (current: number, target: number, amount: number): number => {
    let diff = (target - current) % 360;
    if (diff < -180) diff += 360;
    if (diff > 180) diff -= 360;
    return (current + diff * amount + 360) % 360;
  };

  // Reset simulation state and default to Heading-Up when switching trips
  useEffect(() => {
    setIsSimFinished(false);
    currentDistRef.current = 0;
    isNorthUpRef.current = false;
  }, [selectedTripId]);

  // Live GPS Speedometer calculation when not in simulation
  useEffect(() => {
    if (isSimulating) return;
    const activeTrip = activeTripsList.find(t => t.id === selectedTripId);
    if (!activeTrip) {
      setCurrentDisplaySpeedKmH(0);
      return;
    }
    const log = extractRouteLog(activeTrip.route_path_log);
    if (log && log.length >= 2) {
      const p1 = log[log.length - 2];
      const p2 = log[log.length - 1];
      const dist = getCoordDistanceMeters(p1, p2);
      if (dist > 3) {
        const mode = (activeTrip.transport_mode || 'car').toLowerCase();
        const estKmh = mode.includes('walk') ? 5 : (mode.includes('cycle') || mode.includes('bike')) ? 15 : Math.min(65, Math.round((dist / 2.5) * 3.6));
        setCurrentDisplaySpeedKmH(estKmh);
      } else {
        setCurrentDisplaySpeedKmH(0);
      }
    } else {
      setCurrentDisplaySpeedKmH(0);
    }
  }, [isSimulating, activeTripsList, selectedTripId]);

  // 2. High-Performance 60 FPS Arc-Length Parameterized Uniform-Speed Animation Loop (Single-Pass with Destination Stop)
  useEffect(() => {
    if (!isSimulating || isSimFinished || interpolatedPath.length < 2 || !selectedTripId) {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
        animationRef.current = undefined;
      }
      return;
    }

    const selectedTrip = activeTripsList.find(t => t.id === selectedTripId);
    const isCompletedTrip = selectedTrip?.status === 'completed' || (selectedTrip as any)?.safety_status === 'completed';
    const mode = (selectedTrip?.transport_mode || 'car').toLowerCase();
    const baseKmh = mode.includes('walk') ? 6 : (mode.includes('cycle') || mode.includes('bike')) ? 16 : 38;

    // Pre-calculate cumulative metric distance along every segment of the polyline
    const cumulative: number[] = [0];
    for (let i = 0; i < interpolatedPath.length - 1; i++) {
      const d = getCoordDistanceMeters(interpolatedPath[i], interpolatedPath[i + 1]);
      cumulative.push(cumulative[i] + d);
    }
    const totalDistanceMeters = cumulative[cumulative.length - 1];
    if (totalDistanceMeters <= 0) return;

    // Real trip average speed (Derived from genuine trip duration & distance without simSpeed distortion)
    let realBaseKmh = baseKmh;
    const { startMs: tripStartMs, endMs: tripEndMs } = getTripTimeRange(selectedTrip || ({} as any), totalDistanceMeters);
    if (selectedTrip?.start_time && selectedTrip?.end_time) {
      const startMs = new Date(selectedTrip.start_time).getTime();
      const endMs = new Date(selectedTrip.end_time).getTime();
      const durationHours = (endMs - startMs) / (1000 * 3600);
      if (durationHours > 0.01 && durationHours < 24) {
        const distKm = (selectedTrip.total_distance && selectedTrip.total_distance > 0)
          ? selectedTrip.total_distance
          : totalDistanceMeters / 1000;
        const calculatedKmh = Math.round(distKm / durationHours);
        if (calculatedKmh >= 3 && calculatedKmh <= 120) {
          realBaseKmh = calculatedKmh;
        }
      }
    }
    setCurrentDisplaySpeedKmH(realBaseKmh);

    // Uniform target duration for the entire route: ~18 seconds at 1x speed, scaled by simSpeed
    const baseDurationSec = Math.max(12, Math.min(28, totalDistanceMeters / 100));
    // Uniform speed in meters per second (100% constant across straight roads and sharp curves!)
    const speedMps = (totalDistanceMeters / (baseDurationSec / simSpeed));

    let lastTime = performance.now();
    let lastHudUpdate = 0;
    let currentDist = currentDistRef.current;
    let lastIdx = 0;

    if (interpolatedPath.length >= 2 && currentDist === 0) {
      currentHeadingRef.current = calculateSimpleHeading(interpolatedPath[0], interpolatedPath[1]);
    }

    const animate = (now: number) => {
      // Calculate delta time in seconds, clamped to max 100ms
      const dt = Math.min((now - lastTime) / 1000, 0.1);
      lastTime = now;

      // Uniform speed progress: exact meters covered per second
      currentDist += speedMps * dt;
      if (currentDist >= totalDistanceMeters) {
        // Single pass reached: STOP FIRMLY AT DESTINATION!
        currentDist = totalDistanceMeters;
        currentDistRef.current = totalDistanceMeters;

        const destCoord = interpolatedPath[interpolatedPath.length - 1];
        const targetMarker = markersRef.current[selectedTripId];
        if (targetMarker) {
          targetMarker.setLngLat(destCoord);
          const el = targetMarker.getElement();
          if (el) {
            const tooltipEl = el.querySelector('.marker-tooltip') as HTMLElement;
            if (tooltipEl) {
              const traveler = profiles.find(p => p.id === selectedTrip?.user_id);
              const travelerName = traveler?.full_name || 'Traveler';
              tooltipEl.innerHTML = `
                <span class="font-bold text-rose-400">DESTINATION ARRIVED 🏁</span> • ${travelerName}
                <br/><span class="text-[9px] text-slate-300 font-mono">Route Completed</span>
              `;
            }
            const timeTextEl = el.querySelector('.puck-time-text') as HTMLElement;
            if (timeTextEl) {
              timeTextEl.textContent = formatPuckTime(tripEndMs);
            }
          }
        }

        setSimIndex(interpolatedPath.length - 1);
        setIsSimFinished(true);
        setIsSimulating(false);
        setCurrentDisplaySpeedKmH(0);

        if (!isCompletedTrip && mapRef.current) {
          const traveledSource: any = mapRef.current.getSource(`traveled-path-source-${selectedTripId}`);
          if (traveledSource) {
            traveledSource.setData({
              type: 'Feature',
              properties: {},
              geometry: {
                type: 'LineString',
                coordinates: interpolatedPath
              }
            });
          }
        }

        if (animationRef.current) {
          cancelAnimationFrame(animationRef.current);
          animationRef.current = undefined;
        }
        return; // Terminate animation cleanly
      }
      currentDistRef.current = currentDist;

      // Locate the exact segment corresponding to current metric distance
      while (lastIdx < cumulative.length - 2 && cumulative[lastIdx + 1] < currentDist) {
        lastIdx++;
      }
      if (cumulative[lastIdx] > currentDist) {
        lastIdx = 0;
      }

      const p1 = interpolatedPath[lastIdx];
      const p2 = interpolatedPath[Math.min(lastIdx + 1, interpolatedPath.length - 1)] || p1;
      const segStart = cumulative[lastIdx];
      const segEnd = cumulative[Math.min(lastIdx + 1, cumulative.length - 1)];
      const segLen = Math.max(0.0001, segEnd - segStart);
      const frac = Math.max(0, Math.min(1, (currentDist - segStart) / segLen));

      // Sub-millimeter continuous coordinate interpolation with uniform velocity
      const currentLng = p1[0] + (p2[0] - p1[0]) * frac;
      const currentLat = p1[1] + (p2[1] - p1[1]) * frac;

      // Smooth heading with continuous corner turning lerp
      const targetHeading = calculateSimpleHeading(p1, p2);
      currentHeadingRef.current = lerpAngle(currentHeadingRef.current, targetHeading, Math.min(1, 14 * dt));

      // Direct GPU-accelerated DOM marker update (Zero React re-render overhead!)
      const targetMarker = markersRef.current[selectedTripId];
      if (targetMarker) {
        targetMarker.setLngLat([currentLng, currentLat]);
        const el = targetMarker.getElement();
        if (el) {
          const rotationEl = el.querySelector('.puck-rotation') as HTMLElement;
          if (rotationEl) {
            (rotationEl as any)._heading = currentHeadingRef.current;
            const b = mapRef.current ? mapRef.current.getBearing() : 0;
            const screenAngle = (currentHeadingRef.current - b + 360) % 360;
            rotationEl.style.transform = `rotate(${screenAngle}deg)`;
          }
          // Update live progress time badge beside traveler point
          const timeTextEl = el.querySelector('.puck-time-text') as HTMLElement;
          if (timeTextEl) {
            const progressRatio = Math.max(0, Math.min(1, currentDist / totalDistanceMeters));
            const progressTimeMs = tripStartMs + progressRatio * (tripEndMs - tripStartMs);
            const liveTimeStr = formatPuckTime(progressTimeMs);
            if (timeTextEl.textContent !== liveTimeStr) {
              timeTextEl.textContent = liveTimeStr;
            }
          }
        }
      }

      // Direct GPU-accelerated live green trail update behind simulation vehicle ONLY for live ongoing demo simulation (NOT for completed trip replay!)
      if (!isCompletedTrip && mapRef.current) {
        const traveledSource: any = mapRef.current.getSource(`traveled-path-source-${selectedTripId}`);
        if (traveledSource) {
          const simProgressCoords = [...interpolatedPath.slice(0, lastIdx + 1), [currentLng, currentLat]];
          traveledSource.setData({
            type: 'Feature',
            properties: {},
            geometry: {
              type: 'LineString',
              coordinates: simProgressCoords
            }
          });
        }
      }

      // Throttle React state update for HUD metrics (~3 updates/sec) to keep React completely idle
      if (now - lastHudUpdate > 280) {
        lastHudUpdate = now;
        setSimIndex(lastIdx);

        // Dynamically modulate real vehicle speed (sharp corners slow down, open avenues accelerate)
        const angleDiff = Math.abs((targetHeading - currentHeadingRef.current + 180) % 360 - 180);
        const curveFactor = angleDiff > 25 ? Math.max(0.4, 1 - (angleDiff / 90) * 0.6) : (angleDiff < 8 ? 1.15 : 1.0);
        const progressFrac = currentDist / totalDistanceMeters;
        const edgeFactor = progressFrac < 0.05 ? Math.max(0.3, progressFrac / 0.05) : (progressFrac > 0.95 ? Math.max(0.2, (1 - progressFrac) / 0.05) : 1.0);
        const dynamicSpeed = Math.round(realBaseKmh * curveFactor * edgeFactor);
        setCurrentDisplaySpeedKmH(Math.max(3, dynamicSpeed));
      }

      animationRef.current = requestAnimationFrame(animate);
    };

    lastTime = performance.now();
    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
        animationRef.current = undefined;
      }
    };
  }, [isSimulating, isSimFinished, interpolatedPath, simSpeed, selectedTripId, profiles, activeTripsList]);


  // Load and validate token on mount client-side to prevent hydration mismatch
  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => {
      if (!active) return;
      const envToken = (process.env.NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN || '').trim();
      let storedToken = (typeof window !== 'undefined' ? localStorage.getItem('mapbox_access_token') || '' : '').trim();
      
      // Auto-purge any broken or invalid token from browser storage
      if (storedToken.includes('YmRtYXBib3gi')) {
        try { localStorage.removeItem('mapbox_access_token'); } catch {}
        storedToken = '';
      }

      // Pick working token: valid env token, custom stored token, or fallback to DEFAULT_MAPBOX_TOKEN
      const candidate = (envToken && !envToken.includes('YmRtYXBib3gi'))
        ? envToken 
        : (storedToken || DEFAULT_MAPBOX_TOKEN);

      if (candidate && isValidMapboxToken(candidate)) {
        setMapboxToken(candidate);
        setIsTokenConfigured(true);
      } else {
        setIsTokenConfigured(false);
      }
    }, 0);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, []);

  // 1. Inject Mapbox GL CSS dynamically
  useEffect(() => {
    if (!isTokenConfigured) return;

    const link = document.createElement('link');
    link.href = 'https://api.mapbox.com/mapbox-gl-js/v3.3.0/mapbox-gl.css';
    link.rel = 'stylesheet';
    document.head.appendChild(link);

    return () => {
      try {
        document.head.removeChild(link);
      } catch (err) {
        // Ignored
      }
    };
  }, [isTokenConfigured]);

  // 2. Initialize Mapbox Instance
  useEffect(() => {
    if (!isTokenConfigured || !mapboxToken || !mapContainerRef.current) return;

    // Load mapbox-gl client-side only
    let mapboxgl: any;
    try {
      mapboxgl = require('mapbox-gl');
      mapboxgl.accessToken = mapboxToken;
    } catch (e) {
      console.error('Failed to require mapbox-gl on client side', e);
      return;
    }

    // Determine initial center based on selected trip, or general default (Dhaka, Bangladesh) on map initialize
    let initialCenter: [number, number] = [90.4125, 23.8103];
    let initialZoom = 7; // Zoom level 7 shows the whole country generally, or 12 for Dhaka city
    if (mapRef.current) {
      try {
        const currentCenter = mapRef.current.getCenter();
        initialCenter = [currentCenter.lng, currentCenter.lat];
        initialZoom = mapRef.current.getZoom();
      } catch (err) {
        console.warn('Could not read existing center during re-init:', err);
      }
    } else {
      if (selectedTripId) {
        const activeTrip = activeTripsList.find(t => t.id === selectedTripId);
        if (activeTrip) {
          initialCenter = activeTrip.start_coords || getCoordsSync(activeTrip.start_location, resolvedCoords);
        }
      } else if (activeTripsList.length > 0) {
        initialCenter = activeTripsList[0].start_coords || getCoordsSync(activeTripsList[0].start_location, resolvedCoords);
      } else if (userBrowserLocation) {
        initialCenter = userBrowserLocation;
      }
    }

    let mapInstance: any;
    try {
      mapInstance = new mapboxgl.Map({
        container: mapContainerRef.current,
        style: mapStyle,
        center: initialCenter,
        zoom: initialZoom,
        pitch: 45,
        bearing: -17,
        antialias: true,
        attributionControl: false
      });

      // Catch styling or loading auth errors gracefully instead of letting mapbox-gl crash
      mapInstance.on('error', (e: any) => {
        console.warn('Mapbox GL error caught gracefully:', e?.error?.message || e);
      });

      // Bind dynamic Mapbox click coordinates back to parent state
      mapInstance.on('click', (e: any) => {
        const { lng, lat } = e.lngLat;
        if (onMapClickRef.current) {
          onMapClickRef.current(lng, lat);
        }
      });

      // Add navigation controls (zoom, compass with pitch visualization)
      const navControl = new mapboxgl.NavigationControl({ visualizePitch: true });
      mapInstance.addControl(navControl, 'top-right');

      // Wire compass click to toggle between Heading-Up (travel direction UP) and North-Up
      setTimeout(() => {
        const compassBtn = mapContainerRef.current?.querySelector('.mapboxgl-ctrl-compass') as HTMLButtonElement | null;
        if (compassBtn) {
          compassBtn.title = 'Reset to North Up';
          const onCompassClick = (e: MouseEvent) => {
            if (!mapRef.current) return;
            const currentBearing = mapRef.current.getBearing();
            if (Math.abs(currentBearing) < 4) {
              // Already facing North: Switch back to Heading-Up
              e.preventDefault();
              e.stopPropagation();
              isNorthUpRef.current = false;
              const targetHeading = currentTripHeadingRef.current || 0;
              mapRef.current.easeTo({
                bearing: targetHeading,
                pitch: targetHeading !== 0 ? 35 : 0,
                duration: 900
              });
            } else {
              // Heading-Up is active: Switch to North-Up
              e.preventDefault();
              e.stopPropagation();
              isNorthUpRef.current = true;
              mapRef.current.easeTo({
                bearing: 0,
                pitch: 0,
                duration: 900
              });
            }
          };
          compassBtn.addEventListener('click', onCompassClick, true);
        }
      }, 100);

      // Rotate listener to update compass button tooltip and marker orientations smoothly
      mapInstance.on('rotate', () => {
        const b = mapInstance.getBearing();
        const compassBtn = mapContainerRef.current?.querySelector('.mapboxgl-ctrl-compass') as HTMLButtonElement | null;
        if (compassBtn) {
          compassBtn.title = Math.abs(b) < 4 ? 'Orient Heading Up (Travel Direction)' : 'Reset to North Up';
        }
        Object.values(markersRef.current).forEach((m: any) => {
          try {
            const el = m.getElement();
            const rotEl = el?.querySelector('.puck-rotation') as HTMLElement;
            if (rotEl && (rotEl as any)._heading !== undefined) {
              const angle = ((rotEl as any)._heading - b + 360) % 360;
              rotEl.style.transform = `rotate(${angle}deg)`;
            }
          } catch {}
        });
      });

      mapRef.current = mapInstance;
      setTimeout(() => {
        setMapLoadedTrigger(prev => prev + 1);
      }, 0);
    } catch (err) {
      console.error('Could not initialize Mapbox map:', err);
      return;
    }

    const handleResize = () => {
      if (mapRef.current) {
        try { mapRef.current.resize(); } catch {}
      }
    };
    window.addEventListener('resize', handleResize);

    // Wait until map loads style before placing static layer decorations
    mapInstance.on('load', () => {
      try { mapInstance.resize(); } catch {}
      setMapLoadedTrigger(prev => prev + 1);
      // Add custom 3D building styling if dark theme or custom style is chosen
      if (mapStyle.includes('dark') || mapStyle.includes('suvosheikh')) {
        try {
          const styleObj = mapInstance.getStyle();
          const layers = styleObj?.layers || [];
          const labelLayerId = layers.find(
            (layer: any) => layer.type === 'symbol' && layer.layout && layer.layout['text-field']
          )?.id;

          if (mapInstance.getSource('composite') && labelLayerId && !mapInstance.getLayer('add-3d-buildings')) {
            mapInstance.addLayer(
              {
                id: 'add-3d-buildings',
                source: 'composite',
                'source-layer': 'building',
                filter: ['==', 'extrude', 'true'],
                type: 'fill-extrusion',
                minzoom: 15,
                paint: {
                  'fill-extrusion-color': '#111827',
                  'fill-extrusion-height': [
                    'interpolate',
                    ['linear'],
                    ['zoom'],
                    15,
                    0,
                    15.05,
                    ['get', 'height']
                  ],
                  'fill-extrusion-base': [
                    'interpolate',
                    ['linear'],
                    ['zoom'],
                    15,
                    0,
                    15.05,
                    ['get', 'min_height']
                  ],
                  'fill-extrusion-opacity': 0.85
                }
              },
              labelLayerId
            );
          }
        } catch (e) {
          // Gracefully continue if composite source is not present
        }
      }
    });

    return () => {
      window.removeEventListener('resize', handleResize);
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
      // Clear marker refs completely because the previous map is destroyed!
      markersRef.current = {};
      startMarkersRef.current = {};
      destMarkersRef.current = {};
      actualEndMarkersRef.current = {};
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isTokenConfigured, mapboxToken, mapStyle]);

  // 3. Update Markers and Keep Synchronized
  useEffect(() => {
    if (!mapRef.current || !isTokenConfigured || !mapboxToken) return;

    let mapboxgl: any;
    try {
      mapboxgl = require('mapbox-gl');
    } catch {
      return;
    }

    const activeIds = new Set(activeTripsList.map(t => t.id));

    // Remove existing traveler markers that are no longer in our active trips list
    Object.keys(markersRef.current).forEach(id => {
      if (!activeIds.has(id)) {
        try {
          markersRef.current[id].remove();
        } catch {
          // Ignore
        }
        delete markersRef.current[id];
      }
    });

    // Remove target destination markers that are no longer active
    Object.keys(destMarkersRef.current).forEach(id => {
      if (!activeIds.has(id)) {
        try { destMarkersRef.current[id].remove(); } catch {}
        delete destMarkersRef.current[id];
      }
    });
    // Remove start markers that are no longer active
    Object.keys(startMarkersRef.current).forEach(id => {
      if (!activeIds.has(id)) {
        try { startMarkersRef.current[id].remove(); } catch {}
        delete startMarkersRef.current[id];
      }
    });
    // Remove actual end markers that are no longer active
    Object.keys(actualEndMarkersRef.current).forEach(id => {
      if (!activeIds.has(id)) {
        try { actualEndMarkersRef.current[id].remove(); } catch {}
        delete actualEndMarkersRef.current[id];
      }
    });

    // Place or update 4 distinct pins for active/completed trips
    activeTripsList.forEach(trip => {
      const isSOS = trip.status === 'sos';
      const isCompleted = trip.status === 'completed';
      const isSelected = trip.id === selectedTripId;
      const traveler = profiles.find(p => p.id === trip.user_id);
      const traveledLog = extractRouteLog(trip.route_path_log);
      const isManualSim = isSelected && interpolatedPath.length > 0 && (isSimulating || currentDistRef.current > 0 || isSimFinished);
      
      const coords: [number, number] = (() => {
        // 1. If manual simulation is active or in progress:
        if (isManualSim && interpolatedPath[simIndex]) {
          return interpolatedPath[simIndex];
        }
        // 2. Selected completed trip ready for replay:
        if (isCompleted && isSelected && interpolatedPath.length > 0) {
          return interpolatedPath[0];
        }
        // 3. Real-time Live device GPS fix broadcast:
        if (typeof trip.current_lng === 'number' && typeof trip.current_lat === 'number' && 
            trip.current_lng !== 0 && trip.current_lat !== 0 && 
            !isNaN(trip.current_lng) && !isNaN(trip.current_lat)) {
          return [trip.current_lng, trip.current_lat] as [number, number];
        }
        // 4. Front tip of recorded travel breadcrumbs (genuine user position):
        if (traveledLog && traveledLog.length > 0) {
          return traveledLog[traveledLog.length - 1];
        }
        // 5. Trip started; user is at origin/home GPS:
        if (trip.start_coords) {
          return trip.start_coords;
        }
        return getCoordsSync(trip.start_location, resolvedCoords);
      })();

      // ----------------------------------------------------
      // PIN 1: START LOCATION PIN (শুরুর পয়েন্ট - Royal Blue #2563EB)
      // Anchored strictly to user's actual origin GPS (inside home/building)
      // ----------------------------------------------------
      const startPointCoords: [number, number] = (() => {
        if (trip.start_coords) return trip.start_coords;
        const traveled = extractRouteLog(trip.route_path_log);
        if (traveled && traveled.length > 0) return traveled[0];
        const planned = extractRouteLog(trip.planned_route_log);
        if (planned && planned.length > 0) return planned[0];
        return getCoordsSync(trip.start_location, resolvedCoords);
      })();

      const endCoords = (trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords));
      const key = `${trip.id}-${trip.start_location}-${endCoords[0].toFixed(4)},${endCoords[1].toFixed(4)}`;
      const plannedCoords = extractRouteLog(trip.planned_route_log) || roadGeometries[key] || generateSimulatedRoadRoute(startPointCoords, endCoords);

      // Calculate rotation heading for moving traveler
      let heading = 0;
      if (isManualSim) {
        const current = interpolatedPath[simIndex];
        const next = interpolatedPath[simIndex + 1] || current;
        if (current && next) heading = calculateSimpleHeading(current, next);
      } else if (isCompleted && isSelected && interpolatedPath.length >= 2) {
        heading = calculateSimpleHeading(interpolatedPath[0], interpolatedPath[1]);
      } else if (traveledLog && traveledLog.length >= 2) {
        heading = calculateSimpleHeading(traveledLog[traveledLog.length - 2], traveledLog[traveledLog.length - 1]);
      } else if (plannedCoords && plannedCoords.length >= 2) {
        heading = calculateSimpleHeading(plannedCoords[0], plannedCoords[1]);
      } else {
        heading = getRouteHeading(plannedCoords);
      }

      if (startMarkersRef.current[trip.id]) {
        startMarkersRef.current[trip.id].setLngLat(startPointCoords);
      } else {
        try {
          const el = document.createElement('div');
          el.className = 'mapbox-start-pin group';
          el.innerHTML = `
            <div class="gps-pin-wrapper fallback-size">
              <div class="gps-pin-pulse-ring blue"></div>
              <div class="gps-map-pin blue-pin ${isSelected ? 'selected' : ''}">
                <span class="material-icons map-pin-icon" style="font-size:11px;margin-top:1px;">trip_origin</span>
              </div>
            </div>
            <div class="marker-tooltip">
              <span class="font-bold text-blue-400">START</span> • ${trip.start_location || 'Trip Origin'}
            </div>
          `;
          el.addEventListener('click', (e) => {
            e.stopPropagation();
            onSelectTrip(trip.id);
          });
          const marker = new mapboxgl.Marker({ element: el, anchor: 'bottom' })
            .setLngLat(startPointCoords)
            .addTo(mapRef.current);
          startMarkersRef.current[trip.id] = marker;
        } catch (e) {}
      }

      // ----------------------------------------------------
      // PIN 2: PLANNED DESTINATION PIN (পরিকল্পিত গন্তব্য - Rose Red #E11D48)
      // Anchored strictly to the exact visual end of the planned route
      // ----------------------------------------------------
      const plannedEndCoords: [number, number] = (() => {
        if (plannedCoords && plannedCoords.length > 0) {
          return plannedCoords[plannedCoords.length - 1];
        }
        if (trip.end_coords) return trip.end_coords;
        return getCoordsSync(trip.end_location, resolvedCoords);
      })();

      // ----------------------------------------------------
      // PIN 3: ACTUAL END LOCATION PIN (আসল সমাপ্তি পয়েন্ট - Emerald Green / Amber)
      // Rendered when trip is completed or cancelled
      // ----------------------------------------------------
      const isCancelled = (trip.status as string) === 'cancelled' || (trip as any).safety_status === 'cancelled';
      const actualEndCoords: [number, number] = (() => {
        if (traveledLog && traveledLog.length > 0) {
          return traveledLog[traveledLog.length - 1];
        }
        if (typeof trip.current_lng === 'number' && typeof trip.current_lat === 'number' && trip.current_lng !== 0 && trip.current_lat !== 0) {
          return [trip.current_lng, trip.current_lat];
        }
        return endCoords;
      })();

      const distFromTarget = getCoordDistanceMeters(actualEndCoords, plannedEndCoords);
      // 100m Arrival Geofence: within 100m of destination and not cancelled = Successfully Arrived!
      const didArriveTarget = distFromTarget <= 100 && !isCancelled;
      const isSuccess = isCompleted && didArriveTarget;

      // Render Planned Destination Pin:
      // ALWAYS display the pin at the end of the planned route with status-reactive styling
      const isUnknownDest = !trip.end_location || trip.end_location.includes('Unknown Destination');
      const shouldShowPlannedPin = !isUnknownDest || (plannedCoords && plannedCoords.length > 0);

      if (shouldShowPlannedPin) {
        if (destMarkersRef.current[trip.id]) {
          destMarkersRef.current[trip.id].setLngLat(plannedEndCoords);
          const el = destMarkersRef.current[trip.id].getElement();
          if (el) {
            const ring = el.querySelector('.gps-pin-pulse-ring');
            if (ring) ring.className = `gps-pin-pulse-ring ${isSuccess ? 'green' : 'red'}`;
            const pin = el.querySelector('.gps-map-pin');
            if (pin) pin.className = `gps-map-pin ${isSuccess ? 'green-pin' : 'red-pin'} ${isSelected ? 'selected' : ''}`;
            const icon = el.querySelector('.map-pin-icon');
            if (icon) icon.textContent = isSuccess ? 'check_circle' : 'flag';
            const tooltip = el.querySelector('.marker-tooltip');
            if (tooltip) {
              tooltip.innerHTML = isSuccess
                ? `<span class="font-bold text-emerald-400">DESTINATION ARRIVED</span> • ${trip.end_location || 'Destination'}<br/><span class="text-[9.5px] text-slate-300 font-mono">✓ Successfully Completed</span>`
                : `<span class="font-bold text-rose-400">PLANNED TARGET</span> • ${trip.end_location || 'Destination'}${ (isCompleted || isCancelled) ? `<br/><span class="text-[9.5px] text-amber-300 font-mono">Stopped ${distFromTarget > 1000 ? (distFromTarget / 1000).toFixed(1) + ' km away' : Math.round(distFromTarget) + 'm away'}</span>` : ''}`;
            }
          }
        } else {
          try {
            const el = document.createElement('div');
            el.className = 'mapbox-destination-pin group';
            el.innerHTML = `
              <div class="gps-pin-wrapper fallback-size">
                <div class="gps-pin-pulse-ring ${isSuccess ? 'green' : 'red'}"></div>
                <div class="gps-map-pin ${isSuccess ? 'green-pin' : 'red-pin'} ${isSelected ? 'selected' : ''}">
                  <span class="material-icons map-pin-icon" style="font-size:11px;margin-top:1px;">${isSuccess ? 'check_circle' : 'flag'}</span>
                </div>
              </div>
              <div class="marker-tooltip">
                ${isSuccess 
                  ? `<span class="font-bold text-emerald-400">DESTINATION ARRIVED</span> • ${trip.end_location || 'Destination'}<br/><span class="text-[9.5px] text-slate-300 font-mono">✓ Successfully Completed</span>`
                  : `<span class="font-bold text-rose-400">PLANNED TARGET</span> • ${trip.end_location || 'Destination'}${ (isCompleted || isCancelled) ? `<br/><span class="text-[9.5px] text-amber-300 font-mono">Stopped ${distFromTarget > 1000 ? (distFromTarget / 1000).toFixed(1) + ' km away' : Math.round(distFromTarget) + 'm away'}</span>` : ''}`
                }
              </div>
            `;
            el.addEventListener('click', (e) => {
              e.stopPropagation();
              onSelectTrip(trip.id);
            });
            const marker = new mapboxgl.Marker({ element: el, anchor: 'bottom' })
              .setLngLat(plannedEndCoords)
              .addTo(mapRef.current);
            destMarkersRef.current[trip.id] = marker;
          } catch (e) {}
        }
      } else {
        if (destMarkersRef.current[trip.id]) {
          try { destMarkersRef.current[trip.id].remove(); } catch {}
          delete destMarkersRef.current[trip.id];
        }
      }

      // Render Actual End Pin ONLY when trip was cancelled or stopped early away from target (>100m)
      if ((isCompleted || isCancelled) && !didArriveTarget) {
        const isStoppedEarly = isCancelled || !didArriveTarget;
        if (actualEndMarkersRef.current[trip.id]) {
          actualEndMarkersRef.current[trip.id].setLngLat(actualEndCoords);
          const el = actualEndMarkersRef.current[trip.id].getElement();
          if (el) {
            const ring = el.querySelector('.gps-pin-pulse-ring');
            if (ring) ring.className = `gps-pin-pulse-ring ${isStoppedEarly ? 'amber' : 'green'}`;
            const pin = el.querySelector('.gps-map-pin');
            if (pin) pin.className = `gps-map-pin ${isStoppedEarly ? 'amber-pin' : 'green-pin'} ${isSelected ? 'selected' : ''}`;
            const icon = el.querySelector('.map-pin-icon');
            if (icon) icon.textContent = isStoppedEarly ? 'stop_circle' : 'check_circle';
            const tooltip = el.querySelector('.marker-tooltip');
            if (tooltip) {
              tooltip.innerHTML = `
                <span class="font-bold ${isStoppedEarly ? 'text-amber-400' : 'text-emerald-400'}">
                  ${isCancelled ? 'TRIP CANCELLED HERE' : 'TRIP STOPPED EARLY'}
                </span> • ${distFromTarget > 1000 ? (distFromTarget / 1000).toFixed(1) + ' km from target' : Math.round(distFromTarget) + 'm from target'}
              `;
            }
          }
        } else {
          try {
            const el = document.createElement('div');
            el.className = 'mapbox-actual-end-pin group';
            el.innerHTML = `
              <div class="gps-pin-wrapper fallback-size">
                <div class="gps-pin-pulse-ring ${isStoppedEarly ? 'amber' : 'green'}"></div>
                <div class="gps-map-pin ${isStoppedEarly ? 'amber-pin' : 'green-pin'} ${isSelected ? 'selected' : ''}">
                  <span class="material-icons map-pin-icon" style="font-size:12px;margin-top:1px;">${isStoppedEarly ? 'stop_circle' : 'check_circle'}</span>
                </div>
              </div>
              <div class="marker-tooltip">
                <span class="font-bold ${isStoppedEarly ? 'text-amber-400' : 'text-emerald-400'}">
                  ${isCancelled ? 'TRIP CANCELLED HERE' : 'TRIP STOPPED EARLY'}
                </span> • ${distFromTarget > 1000 ? (distFromTarget / 1000).toFixed(1) + ' km from target' : Math.round(distFromTarget) + 'm from target'}
              </div>
            `;
            el.addEventListener('click', (e) => {
              e.stopPropagation();
              onSelectTrip(trip.id);
            });
            const marker = new mapboxgl.Marker({ element: el, anchor: 'bottom' })
              .setLngLat(actualEndCoords)
              .addTo(mapRef.current);
            actualEndMarkersRef.current[trip.id] = marker;
          } catch (e) {}
        }
      } else {
        if (actualEndMarkersRef.current[trip.id]) {
          try { actualEndMarkersRef.current[trip.id].remove(); } catch {}
          delete actualEndMarkersRef.current[trip.id];
        }
      }

      // ----------------------------------------------------
      // PIN 4: LIVE TRAVELER MOVING PUCK (রানিং ট্র্যাকার - Electric Cyan #06B6D4)
      // Active journeys only; hidden once trip is completed
      // ----------------------------------------------------
      // Determine real-time status label & styling for the traveler
      const movementStatus = (() => {
        if (isSOS) return { badge: 'EMERGENCY SOS', color: 'text-red-400', desc: 'Distress Alert Active' };
        if (isManualSim) {
          if (isSimFinished) {
            return { badge: 'DESTINATION ARRIVED 🏁', color: 'text-rose-400', desc: 'Route Completed' };
          }
          if (isCompleted) {
            return { badge: 'ROUTE REPLAY', color: 'text-cyan-400', desc: 'Traveled Route Playback' };
          }
          return { badge: 'DEMO SIMULATION', color: 'text-amber-400', desc: 'Simulated Route Playback' };
        }
        if (isCompleted && isSelected) {
          return { badge: 'READY FOR REPLAY', color: 'text-cyan-400', desc: 'Click Play to Replay' };
        }
        if (traveledLog && traveledLog.length >= 2) {
          const p1 = traveledLog[traveledLog.length - 2];
          const p2 = traveledLog[traveledLog.length - 1];
          const distMoved = getCoordDistanceMeters(p1, p2);
          if (distMoved > 3) {
            const mode = trip.transport_mode ? trip.transport_mode.toUpperCase() : 'TRANSIT';
            return { badge: 'LIVE • MOVING', color: 'text-emerald-400', desc: `${mode} In-Transit` };
          }
          return { badge: 'LIVE • STATIONARY', color: 'text-cyan-400', desc: 'Stopped / Paused' };
        }
        return { badge: 'LIVE • READY AT START', color: 'text-blue-400', desc: 'At Departure Point' };
      })();

      const { startMs: tripStartMs, endMs: tripEndMs } = getTripTimeRange(trip, 0);
      const initialPuckTimeStr = (() => {
        if (isManualSim) {
          const frac = (interpolatedPath.length > 1 && simIndex > 0)
            ? simIndex / (interpolatedPath.length - 1)
            : 0;
          return formatPuckTime(tripStartMs + frac * (tripEndMs - tripStartMs));
        }
        if (isCompleted) {
          return formatPuckTime(tripStartMs);
        }
        return formatPuckTime(Date.now());
      })();

      if (isCompleted && (!isSelected || interpolatedPath.length === 0)) {
        if (markersRef.current[trip.id]) {
          try { markersRef.current[trip.id].remove(); } catch {}
          delete markersRef.current[trip.id];
        }
      } else {
        if (markersRef.current[trip.id]) {
          const m = markersRef.current[trip.id];
          try {
            if (!isSimulating) {
              m.setLngLat(coords);
              const el = m.getElement();
              if (el) {
                const rotationEl = el.querySelector('.puck-rotation') as HTMLElement;
                if (rotationEl) {
                  (rotationEl as any)._heading = heading;
                  const b = mapRef.current ? mapRef.current.getBearing() : 0;
                  const screenAngle = (heading - b + 360) % 360;
                  rotationEl.style.transform = `rotate(${screenAngle}deg)`;
                }
              }
            }
            const el = m.getElement();
            if (el) {
              const tooltipEl = el.querySelector('.marker-tooltip') as HTMLElement;
              if (tooltipEl) {
                const travelerName = traveler?.full_name || 'Traveler';
                tooltipEl.innerHTML = `
                  <span class="font-bold ${movementStatus.color}">${movementStatus.badge}</span> • ${travelerName}
                  <br/><span class="text-[9px] text-slate-300 font-mono">${movementStatus.desc}</span>
                `;
              }
              let timePill = el.querySelector('.puck-time-pill');
              if (!timePill) {
                timePill = document.createElement('div');
                timePill.className = 'puck-time-pill';
                timePill.innerHTML = `
                  <span class="material-icons puck-time-icon">schedule</span>
                  <span class="puck-time-text">${initialPuckTimeStr}</span>
                `;
                el.appendChild(timePill);
              } else if (!isManualSim) {
                const timeTextEl = timePill.querySelector('.puck-time-text');
                if (timeTextEl && timeTextEl.textContent !== initialPuckTimeStr) {
                  timeTextEl.textContent = initialPuckTimeStr;
                }
              }
            }
          } catch (e) {}
        } else {
          try {
            const el = document.createElement('div');
            el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
            if (isSelected) el.classList.add('selected-pulse');

            const b = mapRef.current ? mapRef.current.getBearing() : 0;
            const screenAngle = (heading - b + 360) % 360;
            const puckInner = document.createElement('div');
            puckInner.className = `gps-navigation-puck ${isSelected ? '' : 'mini'} ${isSOS ? 'sos-alarm' : ''}`;
            puckInner.innerHTML = `
              <div class="puck-pulse-ring ${isSOS ? 'puck-sos' : isSelected ? 'puck-selected' : ''}"></div>
              <div class="puck-core ${isSOS ? 'sos' : isSelected ? 'selected' : ''}">
                <div class="puck-rotation" style="transform: rotate(${screenAngle}deg)">
                  <div class="puck-arrow-delta"></div>
                </div>
              </div>
            `;
            const rotEl = puckInner.querySelector('.puck-rotation');
            if (rotEl) {
              (rotEl as any)._heading = heading;
            }
            el.appendChild(puckInner);

            // Flat visible progress time pill (always displayed, no hover required)
            const timePill = document.createElement('div');
            timePill.className = 'puck-time-pill';
            timePill.innerHTML = `
              <span class="material-icons puck-time-icon">schedule</span>
              <span class="puck-time-text">${initialPuckTimeStr}</span>
            `;
            el.appendChild(timePill);

            const tooltip = document.createElement('div');
            tooltip.className = 'marker-tooltip';
            const travelerName = traveler?.full_name || 'Traveler';
            tooltip.innerHTML = `
              <span class="font-bold ${movementStatus.color}">${movementStatus.badge}</span> • ${travelerName}
              <br/><span class="text-[9px] text-slate-300 font-mono">${movementStatus.desc}</span>
            `;
            el.appendChild(tooltip);

            el.addEventListener('click', (e) => {
              e.stopPropagation();
              onSelectTrip(trip.id);
            });

            const marker = new mapboxgl.Marker({
              element: el,
              anchor: 'center'
            })
            .setLngLat(coords)
            .addTo(mapRef.current);

            markersRef.current[trip.id] = marker;
          } catch (err) {}
        }
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ activeTripsList, profiles, selectedTripId, isTokenConfigured, mapboxToken, onSelectTrip, resolvedCoords, roadGeometries, mapLoadedTrigger, interpolatedPath, simIndex, isSimulating, isSimFinished]);

  // 3.3. Smart auto-fit camera bounds to selected trip's complete journey
  const fitMapToTrip = (trip: Trip, smooth = true) => {
    if (!mapRef.current) return;
    let mapboxgl: any;
    try {
      mapboxgl = require('mapbox-gl');
    } catch {
      return;
    }

    const start = trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords);
    const end = trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords);

    const bounds = new mapboxgl.LngLatBounds();
    const isStartDhakaFallback = start[0] === 90.4125 && start[1] === 23.8103 && !trip.start_location.toLowerCase().includes('dhaka');
    const isEndDhakaFallback = end[0] === 90.4125 && end[1] === 23.8103 && !trip.end_location.toLowerCase().includes('dhaka');

    if (!isStartDhakaFallback) bounds.extend(start);
    if (!isEndDhakaFallback && trip.end_location && !trip.end_location.includes('Unknown Destination')) bounds.extend(end);

    const traveledLog = extractRouteLog(trip.route_path_log);
    if (traveledLog && traveledLog.length > 0) {
      traveledLog.forEach(pt => bounds.extend(pt));
    }

    const plannedLog = extractRouteLog(trip.planned_route_log);
    if (plannedLog && plannedLog.length > 0) {
      plannedLog.forEach(pt => bounds.extend(pt));
    }

    if (typeof trip.current_lng === 'number' && typeof trip.current_lat === 'number' && trip.current_lng !== 0 && trip.current_lat !== 0) {
      bounds.extend([trip.current_lng, trip.current_lat]);
    }

    if (!bounds.isEmpty()) {
      try {
        const tripHeading = getTripOverallHeading(trip);
        currentTripHeadingRef.current = tripHeading;
        const targetBearing = isNorthUpRef.current ? 0 : tripHeading;
        const targetPitch = (isNorthUpRef.current || tripHeading === 0) ? 0 : 35;

        mapRef.current.fitBounds(bounds, {
          padding: { 
            top: isModalMode ? 40 : 90, 
            bottom: isModalMode ? 60 : (isPublicView ? 160 : 90), 
            left: isModalMode ? 40 : 80, 
            right: isModalMode ? 40 : 80 
          },
          bearing: targetBearing,
          pitch: targetPitch,
          maxZoom: 16,
          duration: smooth ? 1200 : 0
        });
      } catch (err) {}
    }
  };

  const lastFittedTripKeyRef = useRef<string | null>(null);
  useEffect(() => {
    if (!mapRef.current || !selectedTripId) return;
    const trip = activeTripsList.find(t => t.id === selectedTripId);
    if (!trip) return;

    const start = trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords);
    const end = trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords);
    const fitKey = `${selectedTripId}-${start[0].toFixed(3)},${start[1].toFixed(3)}-${end[0].toFixed(3)},${end[1].toFixed(3)}`;
    if (lastFittedTripKeyRef.current === fitKey) return;
    lastFittedTripKeyRef.current = fitKey;

    fitMapToTrip(trip);
  }, [selectedTripId, activeTripsList, resolvedCoords, mapLoadedTrigger]);

  // 3.4 Geocode custom addresses
  useEffect(() => {
    if (!isTokenConfigured || !mapboxToken) return;
    let active = true;

    const geocodeAllNeeded = async () => {
      let changed = false;
      const newResolved: Record<string, [number, number]> = {};

      const locationsToGeocode = new Set<string>();
      activeTripsList.forEach(trip => {
        if (trip.start_location && !trip.start_coords && !resolvedCoords[trip.start_location] && !getStaticLocationCoords(trip.start_location)) {
          locationsToGeocode.add(trip.start_location);
        }
        if (trip.end_location && !trip.end_coords && !resolvedCoords[trip.end_location] && !getStaticLocationCoords(trip.end_location) && !trip.end_location.includes('Unknown Destination')) {
          locationsToGeocode.add(trip.end_location);
        }
      });
      
      if (newTripStart && !resolvedCoords[newTripStart] && !getStaticLocationCoords(newTripStart)) {
        locationsToGeocode.add(newTripStart);
      }

      for (const loc of locationsToGeocode) {
        try {
          const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(loc)}.json?access_token=${mapboxToken}&limit=1`;
          const res = await fetch(url);
          const data = await res.json();
          if (data.features && data.features.length > 0) {
            newResolved[loc] = data.features[0].center as [number, number];
            changed = true;
          }
        } catch (err) {
          console.warn('Geocoding error for', loc, err);
        }
      }

      if (active && changed) {
        setResolvedCoords(prev => ({ ...prev, ...newResolved }));
      }
    };

    geocodeAllNeeded();
    return () => { active = false; };
  }, [activeTripsList, isTokenConfigured, mapboxToken, newTripStart]);

  // Background geocoding and routing
  useEffect(() => {
    if (!isTokenConfigured || !mapboxToken || activeTripsList.length === 0) return;

    let active = true;

    const fetchNeededRoutes = async () => {
      let changed = false;
      const newGeometries = { ...roadGeometries };

      for (const trip of activeTripsList) {
        if (extractRouteLog(trip.route_path_log)) continue; // Already has DB log

        const startCoords = (trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords));
        const endCoords = (trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords));
        const isUnknownDest = !trip.end_location || trip.end_location.includes('Unknown Destination');
        if (isUnknownDest) continue;

        const isStartFallback = startCoords[0] === 90.4125 && startCoords[1] === 23.8103;
        const isEndFallback = endCoords[0] === 90.4125 && endCoords[1] === 23.8103;
        if (isStartFallback || isEndFallback) continue;

        const key = `${trip.id}-${trip.start_location}-${endCoords[0].toFixed(4)},${endCoords[1].toFixed(4)}`;
        if (roadGeometries[key] || newGeometries[key]) continue;

        try {
          const url = `https://api.mapbox.com/directions/v5/mapbox/driving-traffic/${startCoords[0]},${startCoords[1]};${endCoords[0]},${endCoords[1]}?geometries=geojson&overview=full&access_token=${mapboxToken}`;
          const res = await fetch(url);
          const data = await res.json();
          if (data.routes && data.routes[0]?.geometry?.coordinates) {
            newGeometries[key] = data.routes[0].geometry.coordinates;
            changed = true;
          }
        } catch (err) {
          console.error("Mapbox Directions API error:", err);
        }
      }

      if (active && changed) {
        setRoadGeometries(prev => ({ ...prev, ...newGeometries }));
      }
    };

    fetchNeededRoutes();

    return () => { active = false; };
  }, [activeTripsList, isTokenConfigured, mapboxToken, resolvedCoords]);

  // 3.6. Manage route lines/paths on Mapbox GL Map
  useEffect(() => {
    if (!mapRef.current || !isTokenConfigured || !mapboxToken) return;

    const mapInstance = mapRef.current;
    
    const updateRoutes = () => {
      if (!mapInstance.isStyleLoaded()) {
        setTimeout(updateRoutes, 200);
        return;
      }
      
      activeTripsList.forEach((trip) => {
        const isSelected = trip.id === selectedTripId;
        const sourceId = `route-source-${trip.id}`;
        const layerId = `route-layer-${trip.id}`;
        const shadowLayerId = `route-shadow-${trip.id}`;

        const endCoords = (trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords));
        const key = `${trip.id}-${trip.start_location}-${endCoords[0].toFixed(4)},${endCoords[1].toFixed(4)}`;
        
        let rawCoords = extractRouteLog(trip.route_path_log) || roadGeometries[key] || generateSimulatedRoadRoute(
          (trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords)), 
          endCoords
        );

        // Clean up legacy single-route layers if present
        const legacyLayer = `route-layer-${trip.id}`;
        const legacyShadow = `route-shadow-${trip.id}`;
        const legacySource = `route-source-${trip.id}`;
        if (mapInstance.getLayer(legacyLayer)) mapInstance.removeLayer(legacyLayer);
        if (mapInstance.getLayer(legacyShadow)) mapInstance.removeLayer(legacyShadow);
        if (mapInstance.getSource(legacySource)) mapInstance.removeSource(legacySource);

        // --- 1. FIXED PLANNED ROUTE (Rose Red #E11D48 - 100% Locked Planned Path) ---
        const plannedSourceId = `planned-route-source-${trip.id}`;
        const plannedLayerId = `planned-route-layer-${trip.id}`;
        const plannedShadowId = `planned-route-shadow-${trip.id}`;

        const plannedCoords = extractRouteLog(trip.planned_route_log) || roadGeometries[key] || generateSimulatedRoadRoute(
          (trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords)), 
          endCoords
        );

        if (plannedCoords && plannedCoords.length >= 2) {
          if (!mapInstance.getSource(plannedSourceId)) {
            mapInstance.addSource(plannedSourceId, {
              type: 'geojson',
              data: {
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: plannedCoords
                }
              }
            });

            mapInstance.addLayer({
              id: plannedShadowId,
              type: 'line',
              source: plannedSourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: {
                'line-color': '#0f172a',
                'line-width': isSelected ? 10 : 7,
                'line-opacity': 0.6,
                'line-blur': 3
              }
            });

            mapInstance.addLayer({
              id: plannedLayerId,
              type: 'line',
              source: plannedSourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: {
                'line-color': '#E11D48', // Rose Red - 100% Fixed Planned Route
                'line-width': isSelected ? 6 : 4,
                'line-opacity': isSelected ? 0.95 : 0.7
              }
            });
          } else {
            const sourceObj = mapInstance.getSource(plannedSourceId);
            if (sourceObj) {
              sourceObj.setData({
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: plannedCoords
                }
              });
            }
            if (mapInstance.getLayer(plannedLayerId)) {
              mapInstance.setPaintProperty(plannedLayerId, 'line-color', '#E11D48');
              mapInstance.setPaintProperty(plannedLayerId, 'line-width', isSelected ? 6 : 4);
              mapInstance.setPaintProperty(plannedLayerId, 'line-opacity', isSelected ? 0.95 : 0.7);
            }
            if (mapInstance.getLayer(plannedShadowId)) {
              mapInstance.setPaintProperty(plannedShadowId, 'line-width', isSelected ? 10 : 7);
            }
          }
        } else {
          const sourceObj = mapInstance.getSource(plannedSourceId);
          if (sourceObj) {
            sourceObj.setData({
              type: 'Feature',
              properties: {},
              geometry: {
                type: 'LineString',
                coordinates: []
              }
            });
          }
        }
        // --- 1.1 FIRST-MILE OFF-ROAD CONNECTOR (House to Street Entry - Dotted Line) ---
        const startConnectorSourceId = `start-connector-source-${trip.id}`;
        const startConnectorLayerId = `start-connector-layer-${trip.id}`;
        
        const originCoords = trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords);
        const streetStartCoords = (plannedCoords && plannedCoords.length > 0) ? plannedCoords[0] : null;

        const startDistMeters = originCoords && streetStartCoords ? getCoordDistanceMeters(originCoords, streetStartCoords) : 0;
        // Only draw dashed line if within realistic short driveway/compound distance (5m to 100m)
        const needsStartConnector = originCoords && streetStartCoords && startDistMeters > 4 && startDistMeters <= 100;
        const startConnectorCoords = needsStartConnector ? [originCoords, streetStartCoords] : null;

        if (startConnectorCoords) {
          if (!mapInstance.getSource(startConnectorSourceId)) {
            mapInstance.addSource(startConnectorSourceId, {
              type: 'geojson',
              data: {
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: startConnectorCoords
                }
              }
            });
            mapInstance.addLayer({
              id: startConnectorLayerId,
              type: 'line',
              source: startConnectorSourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: {
                'line-color': '#2563EB', // Royal Blue dashed link matching Origin Pin
                'line-width': isSelected ? 4 : 3,
                'line-dasharray': [1.5, 2.5],
                'line-opacity': 0.95
              }
            });
          } else {
            const sObj = mapInstance.getSource(startConnectorSourceId);
            if (sObj) {
              sObj.setData({
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: startConnectorCoords
                }
              });
            }
            if (mapInstance.getLayer(startConnectorLayerId)) {
              mapInstance.setPaintProperty(startConnectorLayerId, 'line-width', isSelected ? 4 : 3);
            }
          }
        } else {
          if (mapInstance.getLayer(startConnectorLayerId)) mapInstance.removeLayer(startConnectorLayerId);
          if (mapInstance.getSource(startConnectorSourceId)) mapInstance.removeSource(startConnectorSourceId);
        }

        // --- 1.2 LAST-MILE OFF-ROAD CONNECTOR (Street Exit to Target Building - Dotted Line) ---
        const endConnectorSourceId = `end-connector-source-${trip.id}`;
        const endConnectorLayerId = `end-connector-layer-${trip.id}`;

        const destTargetCoords = trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords);
        const streetEndCoords = (plannedCoords && plannedCoords.length > 0) ? plannedCoords[plannedCoords.length - 1] : null;

        const endDistMeters = destTargetCoords && streetEndCoords ? getCoordDistanceMeters(streetEndCoords, destTargetCoords) : 0;
        // Only draw dashed line if within realistic short driveway/compound distance (5m to 100m)
        const needsEndConnector = destTargetCoords && streetEndCoords && endDistMeters > 4 && endDistMeters <= 100;
        const endConnectorCoords = needsEndConnector ? [streetEndCoords, destTargetCoords] : null;

        if (endConnectorCoords) {
          if (!mapInstance.getSource(endConnectorSourceId)) {
            mapInstance.addSource(endConnectorSourceId, {
              type: 'geojson',
              data: {
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: endConnectorCoords
                }
              }
            });
            mapInstance.addLayer({
              id: endConnectorLayerId,
              type: 'line',
              source: endConnectorSourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: {
                'line-color': '#E11D48', // Rose Red dashed link matching Destination Pin
                'line-width': isSelected ? 4 : 3,
                'line-dasharray': [1.5, 2.5],
                'line-opacity': 0.95
              }
            });
          } else {
            const sObj = mapInstance.getSource(endConnectorSourceId);
            if (sObj) {
              sObj.setData({
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: endConnectorCoords
                }
              });
            }
            if (mapInstance.getLayer(endConnectorLayerId)) {
              mapInstance.setPaintProperty(endConnectorLayerId, 'line-width', isSelected ? 4 : 3);
            }
          }
        } else {
          if (mapInstance.getLayer(endConnectorLayerId)) mapInstance.removeLayer(endConnectorLayerId);
          if (mapInstance.getSource(endConnectorSourceId)) mapInstance.removeSource(endConnectorSourceId);
        }

        // --- 2. REAL-TIME TRAVELED TRAIL (Emerald Green #10B981 - Genuine Traveled Path) ---
        const traveledSourceId = `traveled-path-source-${trip.id}`;
        const traveledLayerId = `traveled-path-layer-${trip.id}`;
        const traveledShadowId = `traveled-path-shadow-${trip.id}`;

        const isManualSim = trip.id === selectedTripId && interpolatedPath.length > 0 && (isSimulating || currentDistRef.current > 0 || isSimFinished);
        const isCompleted = trip.status === 'completed' || (trip as any).safety_status === 'completed';

        // Extract logged coordinates from Supabase
        const dbCoords = extractRouteLog(trip.route_path_log);
        if (dbCoords && dbCoords.length >= 2) {
          liveBreadcrumbsRef.current[trip.id] = dbCoords;
        }

        // Live coordinate from ongoing GPS telemetry
        let livePt: [number, number] | null = null;
        if (typeof trip.current_lng === 'number' && typeof trip.current_lat === 'number' &&
            !isNaN(trip.current_lng) && !isNaN(trip.current_lat) &&
            (trip.current_lng !== 0 || trip.current_lat !== 0)) {
          livePt = [trip.current_lng, trip.current_lat];
        }

        let traveledCoords: [number, number][] | null = null;

        if (isCompleted) {
          // 1. Completed Trip (Travel History): The full traveled route MUST ALWAYS remain 100% visible on the map!
          // During replay, the user point moves along the route, but the route itself is never erased or sliced!
          traveledCoords = dbCoords || plannedCoords;
        } else if (isManualSim) {
          // 2. Manual Simulation Mode on Live Ongoing Trip: Trail matches simulator progress along interpolated road path
          const activeIdx = Math.min(simIndex, interpolatedPath.length - 1);
          traveledCoords = interpolatedPath.slice(0, activeIdx + 1);
        } else if (dbCoords && dbCoords.length >= 2) {
          // 3. Genuine GPS breadcrumb trail from database
          traveledCoords = dbCoords;
          if (livePt) {
            const last = dbCoords[dbCoords.length - 1];
            if (Math.abs(last[0] - livePt[0]) > 0.00002 || Math.abs(last[1] - livePt[1]) > 0.00002) {
              traveledCoords = [...dbCoords, livePt];
            }
          }
        } else if (plannedCoords && plannedCoords.length >= 2 && livePt) {
          // 4. Ongoing Live Trip: Dynamically slice the planned road path up to the traveler's exact live location!
          // Ensures the road behind the traveler turns vibrant emerald green along the street curves in 0ms real-time!
          traveledCoords = sliceRoadRouteUpTo(plannedCoords, livePt);
        } else if (liveBreadcrumbsRef.current[trip.id] && liveBreadcrumbsRef.current[trip.id].length >= 2) {
          traveledCoords = liveBreadcrumbsRef.current[trip.id];
        }

        if (traveledCoords && traveledCoords.length >= 2) {
          if (!mapInstance.getSource(traveledSourceId)) {
            mapInstance.addSource(traveledSourceId, {
              type: 'geojson',
              data: {
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: traveledCoords
                }
              }
            });

            mapInstance.addLayer({
              id: traveledShadowId,
              type: 'line',
              source: traveledSourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: {
                'line-color': '#064e3b',
                'line-width': isSelected ? 11 : 8,
                'line-opacity': 0.6,
                'line-blur': 2
              }
            });

            mapInstance.addLayer({
              id: traveledLayerId,
              type: 'line',
              source: traveledSourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: {
                'line-color': '#10B981', // Vibrant Emerald Green - Live Traveled Trail
                'line-width': isSelected ? 6.5 : 5,
                'line-opacity': 1.0
              }
            });
          } else {
            const sourceObj = mapInstance.getSource(traveledSourceId);
            if (sourceObj) {
              sourceObj.setData({
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: traveledCoords
                }
              });
            }
            if (mapInstance.getLayer(traveledLayerId)) {
              mapInstance.setPaintProperty(traveledLayerId, 'line-color', '#10B981');
              mapInstance.setPaintProperty(traveledLayerId, 'line-width', isSelected ? 6.5 : 5);
              mapInstance.setPaintProperty(traveledLayerId, 'line-opacity', 1.0);
            }
            if (mapInstance.getLayer(traveledShadowId)) {
              mapInstance.setPaintProperty(traveledShadowId, 'line-width', isSelected ? 11 : 8);
            }
          }

          // Move the green traveled layer to the very top so it always overlays the red planned path
          try {
            if (mapInstance.getLayer(traveledShadowId)) mapInstance.moveLayer(traveledShadowId);
            if (mapInstance.getLayer(traveledLayerId)) mapInstance.moveLayer(traveledLayerId);
          } catch (_) {}
        } else {
          // If less than 2 points recorded yet, reset or clear traveled layer
          const sourceObj = mapInstance.getSource(traveledSourceId);
          if (sourceObj) {
            sourceObj.setData({
              type: 'Feature',
              properties: {},
              geometry: {
                type: 'LineString',
                coordinates: []
              }
            });
          }
        }
      });
    };

    updateRoutes();

  }, [activeTripsList, selectedTripId, isTokenConfigured, mapboxToken, resolvedCoords, roadGeometries, mapLoadedTrigger, isSimulating, isSimFinished, simIndex]);

  // 3.5. Render Interactive Destination Pin Coordinates (with dynamic road-snapping and self-cleanup)

  useEffect(() => {
    if (!mapRef.current || !isTokenConfigured || !mapboxToken || !destinationPinCoords) return;

    let mapboxgl: any;
    try {
      mapboxgl = require('mapbox-gl');
    } catch {
      return;
    }

    // Use exact clicked coordinates to prevent jumping/mismatching
    const finalCoords = destinationPinCoords;

    let marker: any = null;
      try {
        marker = new mapboxgl.Marker({ color: '#ec4899' })
        .setLngLat(finalCoords)
        .addTo(mapRef.current);
        
        const el = marker.getElement();
        el.classList.add('mapbox-destination-pin');
        const tooltip = document.createElement('div');
        tooltip.className = 'marker-tooltip';
        tooltip.innerHTML = 'Destination Pin';
        el.appendChild(tooltip);
      } catch (err) {
      console.error('Failed to render custom destination pin:', err);
    }

    return () => {
      if (marker) {
        try {
          marker.remove();
        } catch {}
      }
    };
  }, [destinationPinCoords, previewRouteCoords, isTokenConfigured, mapboxToken, mapLoadedTrigger]);

  // 3.5.1. Render Departure Base Pin (e.g. detected current location) with self-cleanup
  useEffect(() => {
    if (!mapRef.current || !isTokenConfigured || !mapboxToken || !newTripStart) return;

    let mapboxgl: any;
    try {
      mapboxgl = require('mapbox-gl');
    } catch {
      return;
    }

    const startCoords = getCoordsSync(newTripStart, resolvedCoords);
    // Avoid showing fallback Dhaka if it's not actually Dhaka or current location
    const isDhakaFallback = startCoords[0] === 90.4125 && startCoords[1] === 23.8103;
    if (isDhakaFallback && !newTripStart.toLowerCase().includes('manhattan') && !newTripStart.toLowerCase().includes('current location')) {
      return;
    }

    let marker: any = null;
      try {
        marker = new mapboxgl.Marker({ color: '#10b981' })
        .setLngLat(startCoords)
        .addTo(mapRef.current);
        
        const el = marker.getElement();
        el.classList.add('mapbox-departure-pin');
        const tooltip = document.createElement('div');
        tooltip.className = 'marker-tooltip';
        tooltip.innerHTML = 'Departure Base: ' + newTripStart;
        el.appendChild(tooltip);
      } catch (err) {
      console.error('Failed to render custom departure pin:', err);
    }

    return () => {
      if (marker) {
        try {
          marker.remove();
        } catch {}
      }
    };
  }, [newTripStart, resolvedCoords, isTokenConfigured, mapboxToken, mapLoadedTrigger]);

  // 4.1. Fly to Departure Base coordinate (e.g. detected current location) when set
  const lastFlownStartRef = useRef<string | null>(null);
  useEffect(() => {
    if (!mapRef.current || !newTripStart) return;

    const trimmed = newTripStart.trim();
    if (trimmed === lastFlownStartRef.current) return;
    lastFlownStartRef.current = trimmed;

    // Get coordinates for start location
    const coords = getCoordsSync(trimmed, resolvedCoords);
    // Avoid flying to default Dhaka fallback unless the user input is actually Dhaka or current location
    const isDhakaFallback = coords[0] === 90.4125 && coords[1] === 23.8103;
    if (isDhakaFallback && !trimmed.toLowerCase().includes('manhattan') && !trimmed.toLowerCase().includes('current location')) {
      return; 
    }

    try {
      mapRef.current.flyTo({
        center: coords,
        zoom: 14,
        speed: 1.2,
        curve: 1.42,
        essential: true
      });
    } catch (err) {
      console.warn("Fly to start location error:", err);
    }
  }, [newTripStart, resolvedCoords]);

  // Handle Manual Token Application
  const handleApplyToken = () => {
    const trimmed = tokenInput.trim();
    if (isValidMapboxToken(trimmed)) {
      setValidationError('');
      localStorage.setItem('mapbox_access_token', trimmed);
      setMapboxToken(trimmed);
      setIsTokenConfigured(true);
      setShowConfig(false);
    } else {
      setValidationError('Invalid token format! Must start with "pk." or "sk." and contain valid segments.');
    }
  };

  // Clear Custom Token
  const handleClearToken = () => {
    localStorage.removeItem('mapbox_access_token');
    setMapboxToken('');
    setIsTokenConfigured(false);
    setTokenInput('');
    setValidationError('');
  };

  // Click listener for fallback radar to set coordinates
  const handleFallbackClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (isTokenConfigured || !onMapClick) return;
    
    const rect = e.currentTarget.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 105;
    
    // Convert click percent to coordinates spread around the dynamic radar center
    const [baseLng, baseLat] = getRadarCenter(newTripStart, resolvedCoords, userBrowserLocation);
    const scale = 0.08;
    const clickLng = baseLng + ((x - 50) / 100) * scale;
    const clickLat = baseLat - ((y - 50) / 100) * scale;
    
    onMapClick(clickLng, clickLat);
  };

  return (
    <div className={`flex flex-col h-full w-full relative min-h-[350px] ${isModalMode ? 'mapbox-modal-mode' : 'mapbox-standard-mode'}`} id="mapbox-monitor-box">
      
      {/* Dynamic Style Select Bar (Live Map & Modal Mode - hidden for public tracking view) */}
      {isTokenConfigured && !isPublicView && (
        <div className="absolute top-3 left-3 z-30 bg-slate-900/90 backdrop-blur-md border border-slate-750 px-2.5 py-1.5 rounded-lg flex items-center space-x-2 shadow-lg">
          <span className="material-icons text-slate-400 text-xs">layers</span>
          <select 
            value={mapStyle} 
            onChange={(e) => setMapStyle(e.target.value)}
            className="bg-transparent text-[10px] text-slate-200 outline-none border-none font-mono cursor-pointer pr-1"
          >
            <option value="mapbox://styles/mapbox/dark-v11" className="bg-slate-900">Dark Matte</option>
            <option value="mapbox://styles/mapbox/satellite-streets-v12" className="bg-slate-900">Satellite Live</option>
            <option value="mapbox://styles/mapbox/light-v11" className="bg-slate-900">Light Minimal</option>
            <option value="mapbox://styles/mapbox/navigation-day-v1" className="bg-slate-900">Navigation Day</option>
            <option value="mapbox://styles/mapbox/navigation-dark-v1" className="bg-slate-900">Traffic Night</option>
            <option value="mapbox://styles/suvosheikh/cmtlanria00m701sa0nh00xnk" className="bg-slate-900">Custom Night (Suvo)</option>
          </select>
        </div>
      )}

      {/* Main Map Elements Canvas */}
      {isTokenConfigured ? (
        <div className="flex-1 w-full h-full relative rounded-xl overflow-hidden border border-slate-250 shadow-inner">
          <div ref={mapContainerRef} className="absolute inset-0 w-full h-full" id="mapbox-gl-renderer-canvas" />
          
          {/* Multi-Pin & Dual-Route Live Tracking Badge (Admin Console Only - hidden in modal) */}
          {!isPublicView && !isModalMode && (
            <div className="absolute top-12 left-3 z-30 pointer-events-none select-none">
              <div className="inline-flex flex-wrap items-center gap-2 bg-slate-950/90 backdrop-blur-md border border-slate-800/80 px-2.5 py-1.5 rounded-lg text-[10px] font-mono shadow-xl">
                <div className="flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-[#2563EB] shadow-[0_0_6px_#2563EB]"></span>
                  <span className="text-blue-300 font-medium">Start</span>
                </div>
                <span className="text-slate-700">•</span>
                <div className="flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-[#E11D48] shadow-[0_0_6px_#E11D48]"></span>
                  <span className="text-rose-300 font-medium">Planned</span>
                </div>
                <span className="text-slate-700">•</span>
                <div className="flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-[#10B981] shadow-[0_0_6px_#10B981]"></span>
                  <span className="text-emerald-300 font-medium">Traveled / End</span>
                </div>
                <span className="text-slate-700">•</span>
                <div className="flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-[#06B6D4] animate-pulse shadow-[0_0_6px_#06B6D4]"></span>
                  <span className="text-cyan-300 font-medium">Live</span>
                </div>
              </div>
            </div>
          )}
          
          {/* Quick Clear Token / Settings Access Key (Admin Console Only - hidden in modal) */}
          {!isPublicView && !isModalMode && (
            <button
              type="button"
              onClick={handleClearToken}
              className="absolute bottom-3 right-3 z-30 bg-slate-950/80 hover:bg-slate-900 text-[9px] text-red-400 hover:text-red-300 font-mono px-2 py-1 rounded border border-slate-800 transition-all flex items-center space-x-1 cursor-pointer"
              title="Reset active Mapbox token config"
            >
              <span className="material-icons text-[10.5px]">logout</span>
              <span>DISCONNECT MAPBOX</span>
            </button>
          )}
        </div>
      ) : (
        /* 
          POLISHED FALLBACK: Animated Cyber Military Radar HUD Screen
          Avoids any blank screen, looks stunning and has interactive beacons!
        */
        <div 
          onClick={handleFallbackClick}
          className="flex-1 w-full h-full min-h-[350px] bg-slate-950 rounded-xl border border-slate-800 relative overflow-hidden flex flex-col items-center justify-center cursor-crosshair"
        >
          
          {/* Render Map Selected Destination on Fallback Radar HUD */}
          {!isTokenConfigured && destinationPinCoords && (() => {
            const [baseLng, baseLat] = getRadarCenter(newTripStart, resolvedCoords, userBrowserLocation);
            const scale = 0.08;
            const [lng, lat] = destinationPinCoords;
            const left = ((lng - baseLng) / scale) * 100 + 50;
            const top = 50 - ((lat - baseLat) / scale) * 100;
            
            return (
              <div 
                className="absolute z-30 flex flex-col items-center pointer-events-none"
                style={{
                  top: `${Math.max(5, Math.min(95, top))}%`,
                  left: `${Math.max(5, Math.min(95, left))}%`,
                  transform: 'translate(-50%, -100%)'
                }}
              >
                <div className="relative flex h-8 w-8 items-center justify-center">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-pink-500 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-6 w-6 bg-pink-600 border border-white flex items-center justify-center shadow-lg shadow-pink-900/50">
                    <span className="material-icons text-white text-[11px]">flag</span>
                  </span>
                </div>
                <div className="bg-slate-950 border border-slate-800 text-pink-400 text-[8px] px-1 rounded shadow-xl font-mono whitespace-nowrap mt-0.5">
                  TARGET DESTINATION
                </div>
              </div>
            );
          })()}

          {/* Render Fallback Departure Base (Current Location) on Fallback Radar HUD */}
          {!isTokenConfigured && newTripStart && (() => {
            const [baseLng, baseLat] = getRadarCenter(newTripStart, resolvedCoords, userBrowserLocation);
            const scale = 0.08;
            const startCoords = getCoordsSync(newTripStart, resolvedCoords);
            // Ignore default Dhaka fallback unless it actually contains current location or manhattan
            const isDhakaFallback = startCoords[0] === 90.4125 && startCoords[1] === 23.8103;
            if (isDhakaFallback && !newTripStart.toLowerCase().includes('manhattan') && !newTripStart.toLowerCase().includes('current location')) {
              return null;
            }
            const left = ((startCoords[0] - baseLng) / scale) * 100 + 50;
            const top = 50 - ((startCoords[1] - baseLat) / scale) * 100;
            
            return (
              <div 
                className="absolute z-35 flex flex-col items-center pointer-events-none"
                style={{
                  top: `${Math.max(5, Math.min(95, top))}%`,
                  left: `${Math.max(5, Math.min(95, left))}%`,
                  transform: 'translate(-50%, -100%)'
                }}
              >
                <div className="relative flex h-8 w-8 items-center justify-center">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-500 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-6 w-6 bg-emerald-600 border border-white flex items-center justify-center shadow-lg shadow-emerald-950/50">
                    <span className="material-icons text-white text-[11px]">my_location</span>
                  </span>
                </div>
                <div className="bg-slate-950 border border-slate-800 text-emerald-400 text-[8px] px-1 rounded shadow-xl font-mono whitespace-nowrap mt-0.5">
                  DEPARTURE BASE
                </div>
              </div>
            );
          })()}

          {/* Circular Grid Lines */}
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none opacity-20">
            <div className="w-[85%] h-[85%] border border-slate-700 rounded-full flex items-center justify-center">
              <div className="w-[70%] h-[70%] border border-slate-750 rounded-full flex items-center justify-center">
                <div className="w-[50%] h-[50%] border border-slate-800 rounded-full flex items-center justify-center">
                  <div className="w-12 h-12 border border-slate-850 rounded-full"></div>
                </div>
              </div>
            </div>
          </div>

          {/* Sweep scanning line overlay */}
          <div className="absolute w-[240px] h-[240px] origin-center rounded-full pointer-events-none opacity-25 animate-[spin_6s_linear_infinite]"
               style={{ background: 'conic-gradient(from 0deg, transparent 50%, rgba(59, 130, 246, 0.22) 100%)' }}>
          </div>

          {/* Coordinate grid overlay */}
          <div className="absolute inset-0 bg-[linear-gradient(rgba(30,41,59,0.25)_1px,transparent_1px),linear-gradient(90deg,rgba(30,41,59,0.25)_1px,transparent_1px)] bg-[size:28px_28px] pointer-events-none"></div>

          {/* Animated SVG Route lines on Radar HUD fallback screen */}
          <svg className="absolute inset-0 w-full h-full pointer-events-none z-10">
            {/* Draw preview route line in fallback mode if active */}
            {!isTokenConfigured && destinationPinCoords && newTripStart && (() => {
              const [baseLng, baseLat] = getRadarCenter(newTripStart, resolvedCoords, userBrowserLocation);
              const scale = 0.08;
              
              const startCoords = getCoordsSync(newTripStart, resolvedCoords);
              const endCoords = destinationPinCoords;
              
              const startLeft = ((startCoords[0] - baseLng) / scale) * 100 + 50;
              const startTop = 50 - ((startCoords[1] - baseLat) / scale) * 100;
              const endLeft = ((endCoords[0] - baseLng) / scale) * 100 + 50;
              const endTop = 50 - ((endCoords[1] - baseLat) / scale) * 100;
              
              return (
                <g key="fallback-route-preview">
                  <line
                    x1={`${Math.max(2, Math.min(98, startLeft))}%`}
                    y1={`${Math.max(2, Math.min(98, startTop))}%`}
                    x2={`${Math.max(2, Math.min(98, endLeft))}%`}
                    y2={`${Math.max(2, Math.min(98, endTop))}%`}
                    stroke="#ec3cc9"
                    strokeWidth={3}
                    strokeOpacity={0.25}
                  />
                  <line
                    x1={`${Math.max(2, Math.min(98, startLeft))}%`}
                    y1={`${Math.max(2, Math.min(98, startTop))}%`}
                    x2={`${Math.max(2, Math.min(98, endLeft))}%`}
                    y2={`${Math.max(2, Math.min(98, endTop))}%`}
                    stroke="#ec4899"
                    strokeWidth={1.5}
                    strokeDasharray="4, 4"
                    strokeOpacity={0.8}
                    className="flowing-dashed-line"
                  />
                </g>
              );
            })()}

            {!isTokenConfigured && activeTripsList.map((trip, idx) => {
              const { start, end } = getRadarPositions(trip, idx, resolvedCoords);
              const isSOS = trip.status === 'sos';
              const isSelected = trip.id === selectedTripId;
              const strokeColor = isSOS ? '#ef4444' : (isSelected ? '#3b82f6' : '#14b8a6');

              return (
                <g key={`fallback-route-${trip.id}`}>
                  {/* Glowing route base line */}
                  <line
                    x1={`${start.left}%`}
                    y1={`${start.top}%`}
                    x2={`${end.left}%`}
                    y2={`${end.top}%`}
                    stroke={strokeColor}
                    strokeWidth={isSelected ? 4 : 2}
                    strokeOpacity={0.15}
                  />
                  {/* Dashed moving line pointing along direction of travel */}
                  <line
                    x1={`${start.left}%`}
                    y1={`${start.top}%`}
                    x2={`${end.left}%`}
                    y2={`${end.top}%`}
                    stroke={strokeColor}
                    strokeWidth={isSelected ? 2 : 1.2}
                    strokeDasharray={isSelected ? "8, 5" : "5, 5"}
                    strokeOpacity={isSelected ? 0.9 : 0.6}
                    className="flowing-dashed-line"
                  />
                </g>
              );
            })}
          </svg>

          {/* Interactive Navigation simulated travel points */}
          {!isTokenConfigured && activeTripsList.map((trip, idx) => {
            const isSOS = trip.status === 'sos';
            const isCompleted = trip.status === 'completed';
            const isSelected = trip.id === selectedTripId;
            const traveler = profiles.find(p => p.id === trip.user_id);
            const { start, end } = getRadarPositions(trip, idx, resolvedCoords);
            const heading = getRadarHeading(start, end);

            return (
              <React.Fragment key={trip.id}>
                {/* 1. Start Origin Pin (Royal Blue #2563EB) */}
                <div
                  className={`absolute transition-transform duration-300 z-20 pointer-events-none ${isSelected ? 'scale-110' : ''}`}
                  style={{
                    top: `${start.top}%`,
                    left: `${start.left}%`,
                    transform: 'translate(-50%, -100%)'
                  }}
                >
                  <div className="gps-pin-wrapper fallback-size">
                    <div className="gps-pin-pulse-ring blue"></div>
                    <div className={`gps-map-pin blue-pin ${isSelected ? 'selected' : ''}`}>
                      <span className="material-icons map-pin-icon" style={{ fontSize: '10px' }}>trip_origin</span>
                    </div>
                  </div>
                </div>

                {/* 2. Target Destination Pin (Rose Red #E11D48) */}
                <div
                  className={`absolute transition-transform duration-300 z-20 pointer-events-none ${isSelected ? 'scale-110' : ''}`}
                  style={{
                    top: `${end.top}%`,
                    left: `${end.left}%`,
                    transform: 'translate(-50%, -100%)'
                  }}
                >
                  <div className="gps-pin-wrapper fallback-size">
                    <div className="gps-pin-pulse-ring red"></div>
                    <div className={`gps-map-pin red-pin ${isSelected ? 'selected' : ''}`}>
                      <span className="material-icons map-pin-icon" style={{ fontSize: '10px' }}>flag</span>
                    </div>
                  </div>
                </div>

                {/* 3. Actual End Pin (Emerald Green #10B981) - if completed */}
                {isCompleted && (
                  <button
                    type="button"
                    onClick={() => onSelectTrip(trip.id)}
                    className={`absolute transition-transform duration-300 hover:scale-125 z-25 group cursor-pointer ${isSelected ? 'scale-110 z-30' : ''}`}
                    style={{
                      top: `${end.top}%`,
                      left: `${end.left}%`,
                      transform: 'translate(-50%, -100%)'
                    }}
                    title={`Trip Completed: ${traveler?.full_name || 'Traveler'}`}
                  >
                    <div className="gps-pin-wrapper fallback-size">
                      <div className="gps-pin-pulse-ring green"></div>
                      <div className={`gps-map-pin green-pin ${isSelected ? 'selected' : ''}`}>
                        <span className="material-icons map-pin-icon" style={{ fontSize: '11px' }}>check_circle</span>
                      </div>
                    </div>
                    <div className="absolute top-8 left-1/2 -translate-x-1/2 bg-slate-950 border border-emerald-800 text-emerald-300 text-[8.5px] px-2 py-0.5 rounded shadow-xl opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity whitespace-nowrap font-mono z-30">
                      COMPLETED • {traveler?.full_name || 'Traveler'}
                    </div>
                  </button>
                )}

                {/* 4. Live Traveler Moving Puck (Electric Cyan #06B6D4) - if ongoing */}
                {!isCompleted && (
                  <button
                    type="button"
                    onClick={() => onSelectTrip(trip.id)}
                    className={`absolute transition-transform duration-300 hover:scale-125 z-25 group cursor-pointer ${isSelected ? 'scale-110 z-30' : ''}`}
                    style={{
                      top: `${start.top}%`,
                      left: `${start.left}%`,
                      transform: 'translate(-50%, -50%)'
                    }}
                    title={`Live Traveler: ${traveler?.full_name || 'Traveler'}`}
                  >
                    {isSOS ? (
                      <span className="relative flex h-7 w-7 items-center justify-center">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-7 w-7 bg-red-600 border border-white flex items-center justify-center shadow-lg shadow-red-900/50">
                          <span className="material-icons text-white text-xs font-bold">gpp_maybe</span>
                        </span>
                      </span>
                    ) : (
                      <div className="gps-navigation-puck mini">
                        <div className={`puck-pulse-ring ${isSelected ? 'puck-selected' : ''}`}></div>
                        <div className={`puck-core ${isSelected ? 'selected' : ''}`}>
                          <div className="puck-rotation" style={{ transform: `rotate(${heading}deg)` }}>
                            <div className="puck-arrow-delta"></div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Micro tooltip HUD badge */}
                    <div className="absolute top-8 left-1/2 -translate-x-1/2 bg-slate-950 border border-slate-800 text-slate-100 text-[9px] px-2 py-0.5 rounded shadow-xl opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity whitespace-nowrap font-mono z-30">
                      LIVE • {traveler?.full_name || 'Traveler'} ({trip.transport_mode || 'Walking'})
                    </div>
                  </button>
                )}
              </React.Fragment>
            );
          })}

          {/* Quick Config HUD Overlay */}
          <div className="absolute bottom-3 left-3 right-3 p-3 bg-slate-900/95 border border-slate-800 rounded-lg shadow-xl z-20 flex flex-col justify-between space-y-2 md:flex-row md:space-y-0 md:items-center">
            <div className="flex flex-col">
              <span className="text-slate-300 font-mono text-[10px] uppercase font-bold tracking-wider flex items-center">
                <span className="material-icons text-blue-400 text-xs mr-1 animate-pulse font-bold">map</span> 
                INTERACTIVE MAP VIEW
              </span>
              <span className="text-slate-500 text-[8.5px] font-mono uppercase mt-0.5">
                Map token not configured • Click indicators to select
              </span>
            </div>

            {/* Expand / Collapse Mapbox activation input panel */}
            {!showConfig ? (
              <button
                type="button"
                onClick={() => setShowConfig(true)}
                className="bg-blue-600/15 hover:bg-blue-600/25 border border-blue-500/30 text-blue-400 font-mono text-[9.5px] px-2.5 py-1 rounded transition-all cursor-pointer flex items-center space-x-1 self-start md:self-auto"
              >
                <span className="material-icons text-[11px]">vpn_key</span>
                <span>CONFIGURE MAPBOX TOKEN</span>
              </button>
            ) : (
              <div className="flex flex-col space-y-1.5 bg-slate-950 border border-slate-800 p-2.5 rounded-lg w-full md:max-w-xs transition-all">
                <div className="flex items-center justify-between border-b border-slate-850 pb-1">
                  <span className="text-[9px] font-mono text-slate-400 uppercase font-bold flex items-center">
                    <span className="material-icons text-[10px] mr-1 text-slate-500">vpn_key</span>
                    Mapbox Config
                  </span>
                  <button 
                    type="button"
                    onClick={() => setShowConfig(false)}
                    className="text-slate-500 hover:text-slate-300 font-mono text-[9.5px] cursor-pointer"
                  >
                    CLOSE
                  </button>
                </div>

                <div className="flex items-center space-x-1.5 mt-1">
                  <input 
                    type="text"
                    placeholder="pk.eyJ1..."
                    value={tokenInput}
                    onChange={(e) => {
                      setTokenInput(e.target.value);
                      setValidationError('');
                    }}
                    className="flex-1 bg-slate-900 border border-slate-800 text-[9px] px-2 py-1 rounded text-white font-mono placeholder-slate-650 focus:outline-none focus:border-blue-500/70"
                  />
                  <button
                    type="button"
                    onClick={handleApplyToken}
                    className="bg-blue-600 hover:bg-blue-500 text-white font-mono text-[9px] font-bold px-2.5 py-1 rounded transition-colors cursor-pointer shrink-0"
                  >
                    MOUNT
                  </button>
                </div>
                {validationError && (
                  <p className="text-red-400 text-[8px] font-mono uppercase mt-1 leading-snug">
                    {validationError}
                  </p>
                )}
                <p className="text-slate-550 text-[7px] font-sans leading-snug">
                  * Public token can also be persistent in workspace via env variable <code className="font-mono bg-slate-900 text-slate-400 px-0.5 rounded border border-slate-850">NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN</code>
                </p>
              </div>
            )}
          </div>

        </div>
      )}

      {/* 
        High-Fidelity PILOT HUD INSTRUMENTS (Styled after mobile Turn-by-Turn GPS tracking)
        Renders elegantly on top of both Live Mapbox and Fallback Radar screens when a monitored transit signal is inspected!
      */}
      {selectedTripId && (() => {
        const activeTrip = activeTripsList.find(t => t.id === selectedTripId);
        if (!activeTrip) return null;

        const traveler = profiles.find(p => p.id === activeTrip.user_id);
        const travelerName = traveler?.full_name || 'Active Traveler';
        
        const totalSteps = interpolatedPath.length;
        
        const toRad = (x: number) => (x * Math.PI) / 180;
        const haversine = (coords1: [number, number], coords2: [number, number]) => {
          if (!coords1 || !coords2) return 0;
          const R = 3958.8; // Radius of earth in miles
          const dLat = toRad(coords2[1] - coords1[1]);
          const dLon = toRad(coords2[0] - coords1[0]);
          const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(toRad(coords1[1])) * Math.cos(toRad(coords2[1])) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
          const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
          return R * c;
        };

        let totalMiles = 0;
        let coveredMiles = 0;
        for (let i = 0; i < interpolatedPath.length - 1; i++) {
           const dist = haversine(interpolatedPath[i], interpolatedPath[i+1]);
           totalMiles += dist;
           if (i < simIndex) {
             coveredMiles += dist;
           }
        }
        
        // Compute real-world traveled distance from recorded route breadcrumbs
        const realLog = extractRouteLog(activeTrip.route_path_log);
        let realCoveredMiles = 0;
        if (realLog && realLog.length >= 2) {
          for (let i = 0; i < realLog.length - 1; i++) {
            realCoveredMiles += haversine(realLog[i], realLog[i+1]);
          }
        }
        
        let displayMiles = isSimulating ? coveredMiles : realCoveredMiles;
        if (totalSteps === 0 && !isSimulating && realCoveredMiles === 0) displayMiles = 0;
        
        let startTimeMs = activeTrip.start_time ? new Date(activeTrip.start_time).getTime() : Date.now();
        let endTimeMs = activeTrip.end_time ? new Date(activeTrip.end_time).getTime() : 0;
        
        // If end_time is missing from DB (app crashed, etc), calculate dynamically
        if (!endTimeMs) {
          const rawLog = extractRouteLog(activeTrip.route_path_log);
          if (rawLog && activeTrip.route_path_log && activeTrip.route_path_log.length > 0) {
             const lastLog = activeTrip.route_path_log[activeTrip.route_path_log.length - 1];
             if (lastLog.t) endTimeMs = new Date(lastLog.t).getTime();
          }
          if (!endTimeMs || endTimeMs <= startTimeMs) {
             endTimeMs = startTimeMs; 
          }
        }
        
        let totalDurationMin = Math.max(1, Math.round((endTimeMs - startTimeMs) / 60000));
        let coveredMin = Math.round(totalDurationMin * (totalSteps > 0 ? (simIndex / Math.max(1, totalSteps - 1)) : 0));
        let elapsedMin = Math.max(0, Math.round((Date.now() - startTimeMs) / 60000));
        let displayMin = isSimulating ? coveredMin : elapsedMin;
        
        let currentTimeMs = startTimeMs + (coveredMin * 60000);
        const d = new Date(currentTimeMs);
        let hours = d.getHours();
        const minutes = d.getMinutes();
        const ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        const minStr = minutes < 10 ? '0' + minutes : minutes;
        const simEtaStr = `${hours}:${minStr} ${ampm}`;

        return (
          <>
            {/* Modular Public Tracking HUD & Speedometer */}
            {isModalMode ? (
              /* Sleek, unobtrusive Replay / Simulation Controller Floating Bar in Modal Mode */
              <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-40 bg-slate-900/90 backdrop-blur-md border border-slate-750 text-white px-3 py-1.5 rounded-full shadow-2xl flex items-center gap-2 pointer-events-auto select-none">
                {/* For Ongoing / Live trips: SIM ACTIVE <-> LIVE GPS Toggle */}
                {activeTrip.status !== 'completed' ? (
                  <button
                    type="button"
                    onClick={() => {
                      if (!isSimulating) {
                        currentDistRef.current = 0;
                        setSimIndex(0);
                        setIsSimFinished(false);
                        setIsSimulating(true);
                        if (interpolatedPath.length > 0 && markersRef.current[activeTrip.id]) {
                          markersRef.current[activeTrip.id].setLngLat(interpolatedPath[0]);
                        }
                      } else {
                        setIsSimulating(false);
                        setIsSimFinished(false);
                        currentDistRef.current = 0;
                        setSimIndex(0);
                        const realCoords: [number, number] = (typeof activeTrip.current_lng === 'number' && typeof activeTrip.current_lat === 'number' && activeTrip.current_lng !== 0) 
                          ? [activeTrip.current_lng, activeTrip.current_lat] 
                          : (activeTrip.start_coords || [90.4125, 23.8103]);
                        if (markersRef.current[activeTrip.id]) {
                          markersRef.current[activeTrip.id].setLngLat(realCoords);
                        }
                      }
                    }}
                    className={`text-[10.5px] font-mono font-bold px-2.5 py-1 rounded-full flex items-center gap-1 transition-all shadow-sm cursor-pointer shrink-0 ${
                      isSimulating 
                        ? 'bg-amber-600 hover:bg-amber-500 text-white animate-pulse' 
                        : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                    }`}
                    title={isSimulating ? 'Switch back to Live device GPS' : 'Simulate route progression from start (Demo)'}
                  >
                    <span className="material-icons text-xs">{isSimulating ? 'stop_circle' : 'play_circle'}</span>
                    <span>{isSimulating ? 'SIM ACTIVE' : 'LIVE GPS'}</span>
                  </button>
                ) : (
                  /* For Completed trips: Replay Controller */
                  <button
                    type="button"
                    onClick={() => {
                      if (isSimFinished || !isSimulating) {
                        currentDistRef.current = 0;
                        setSimIndex(0);
                        setIsSimFinished(false);
                        setIsSimulating(true);
                        fitMapToTrip(activeTrip);
                        if (interpolatedPath.length > 0 && markersRef.current[activeTrip.id]) {
                          markersRef.current[activeTrip.id].setLngLat(interpolatedPath[0]);
                        }
                      } else {
                        setIsSimulating(false);
                      }
                    }}
                    className={`text-[10.5px] font-mono font-bold px-2.5 py-1 rounded-full flex items-center gap-1 transition-all shadow-sm cursor-pointer shrink-0 ${
                      isSimFinished 
                        ? 'bg-blue-600 hover:bg-blue-500 text-white' 
                        : isSimulating 
                        ? 'bg-amber-600 hover:bg-amber-500 text-white animate-pulse' 
                        : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                    }`}
                    title={isSimulating ? 'Pause Route Replay' : 'Play GPS Route Replay'}
                  >
                    <span className="material-icons text-xs">{isSimFinished ? 'replay' : isSimulating ? 'pause' : 'play_arrow'}</span>
                    <span>{isSimFinished ? 'REPLAY' : isSimulating ? 'PAUSE' : 'REPLAY ROUTE'}</span>
                  </button>
                )}

                {/* Speed Multiplier Pill */}
                <button
                  type="button"
                  onClick={() => setSimSpeed(prev => prev === 1 ? 2 : (prev === 2 ? 5 : 1))}
                  className="bg-slate-800 hover:bg-slate-700 border border-slate-700 text-[9.5px] font-mono text-slate-200 px-2 py-0.5 rounded-full flex items-center gap-0.5 transition-colors cursor-pointer shrink-0"
                  title="Simulation playback speed multiplier"
                >
                  <span className="material-icons text-[10px] text-blue-400">speed</span>
                  <span>{simSpeed}x</span>
                </button>

                {/* Current Simulated Speedometer Readout */}
                {isSimulating && (
                  <span className="text-[10px] font-mono text-cyan-400 font-bold px-1.5 py-0.5 bg-cyan-950/80 rounded-full border border-cyan-800/60 animate-pulse shrink-0">
                    {Math.round(currentDisplaySpeedKmH)} km/h
                  </span>
                )}

                {/* Recenter button */}
                <button
                  type="button"
                  onClick={() => fitMapToTrip(activeTrip)}
                  className="p-1 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer shrink-0"
                  title="Recenter Map on Route"
                >
                  <span className="material-icons text-sm leading-none">center_focus_strong</span>
                </button>
              </div>
            ) : isPublicView ? (
              <>
                <PublicTrackingHud
                  travelerName={travelerName}
                  activeTrip={activeTrip}
                  startLocation={activeTrip.start_location || ''}
                  endLocation={activeTrip.end_location || ''}
                  isSimulating={isSimulating}
                  isSimFinished={isSimFinished}
                  simSpeed={simSpeed}
                  onSetSimSpeed={(s) => setSimSpeed(s)}
                  onStartSim={() => {
                    currentDistRef.current = 0;
                    setSimIndex(0);
                    setIsSimFinished(false);
                    setIsSimulating(true);
                    if (interpolatedPath.length > 0 && markersRef.current[activeTrip.id]) {
                      markersRef.current[activeTrip.id].setLngLat(interpolatedPath[0]);
                    }
                  }}
                  onStopSim={() => {
                    setIsSimulating(false);
                    setIsSimFinished(false);
                    currentDistRef.current = 0;
                    setSimIndex(0);
                    const realCoords: [number, number] = (typeof activeTrip.current_lng === 'number' && typeof activeTrip.current_lat === 'number' && activeTrip.current_lng !== 0) 
                      ? [activeTrip.current_lng, activeTrip.current_lat] 
                      : (activeTrip.start_coords || [90.4125, 23.8103]);
                    if (markersRef.current[activeTrip.id]) {
                      markersRef.current[activeTrip.id].setLngLat(realCoords);
                    }
                  }}
                  onReplay={() => {
                    currentDistRef.current = 0;
                    setSimIndex(0);
                    setIsSimFinished(false);
                    setIsSimulating(true);
                    fitMapToTrip(activeTrip);
                    if (interpolatedPath.length > 0 && markersRef.current[activeTrip.id]) {
                      markersRef.current[activeTrip.id].setLngLat(interpolatedPath[0]);
                      const timeTextEl = markersRef.current[activeTrip.id].getElement()?.querySelector('.puck-time-text') as HTMLElement;
                      if (timeTextEl) {
                        const { startMs } = getTripTimeRange(activeTrip);
                        timeTextEl.textContent = formatPuckTime(startMs);
                      }
                    }
                  }}
                  publicTelemetry={publicTelemetry}
                  onRecenter={() => fitMapToTrip(activeTrip)}
                />

                <SpeedometerWidget
                  currentDisplaySpeedKmH={currentDisplaySpeedKmH}
                  speedLimit={speedLimit}
                  isSimulating={isSimulating}
                  isSimFinished={isSimFinished}
                  simSpeed={simSpeed}
                  onCycleSpeedLimit={() => setSpeedLimit(prev => prev === 30 ? 45 : (prev === 45 ? 60 : 30))}
                  className="fixed top-48 md:top-44 left-4 z-40 flex flex-col items-center space-y-2 select-none pointer-events-auto"
                />
              </>
            ) : (
              <>
                {/* 1. ADMIN TOP NAV TURN BOX */}
                <div className="absolute top-4 left-4 right-4 z-40 bg-slate-900/95 border border-slate-800 text-white rounded-2xl shadow-xl p-3 sm:p-4 flex flex-col md:max-w-[450px] md:mx-auto select-none pointer-events-auto hover:bg-slate-900 transition-all duration-300 backdrop-blur-md">
                  <div className="flex items-center space-x-3 pb-2.5 border-b border-slate-800">
                    <div className="w-10 h-10 bg-blue-600/10 border border-blue-500/20 rounded-xl flex items-center justify-center shrink-0">
                      <span className="material-icons text-xl text-blue-400">
                        satellite_alt
                      </span>
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <h4 className="text-[10px] text-slate-400 font-mono tracking-wider uppercase truncate">Live Tracking: {travelerName}</h4>
                      <p className="font-bold text-sm tracking-tight text-white line-clamp-1">{activeTrip.end_location?.replace(/\[.*?\]|\(.*?\)/g, '').trim() || 'Unknown Destination'}</p>
                      <div className="flex items-center flex-wrap gap-x-2.5 gap-y-0.5 mt-0.5">
                        <p className={`text-[11px] font-mono font-bold flex items-center ${isSimFinished ? 'text-rose-400' : 'text-slate-300'}`}>
                          <span className={`inline-block h-1.5 w-1.5 rounded-full mr-1.5 ${isSimFinished ? 'bg-rose-400' : 'bg-emerald-500 animate-pulse'}`}></span>
                          {isSimFinished ? 'Destination Arrived 🏁' : 'Transit in progress'}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2.5">
                    <div className="flex items-center space-x-1.5 text-xs text-slate-400 font-mono max-w-[55%]">
                      <span className="material-icons text-[13px]">my_location</span>
                      <span className="truncate">{activeTrip.start_location?.replace(/\[.*?\]|\(.*?\)/g, '').trim() || 'Unknown Origin'}</span>
                    </div>

                    <div className="flex items-center space-x-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={() => setSimSpeed(prev => prev === 1 ? 2 : (prev === 2 ? 5 : 1))}
                        className="bg-slate-950 border border-slate-800 text-[9px] font-mono text-slate-300 hover:text-white px-2 py-1 rounded transition-colors cursor-pointer flex items-center space-x-1"
                        title="Change simulation speed multiplier"
                      >
                        <span className="material-icons text-[10px] text-blue-400">speed</span>
                        <span>SPEED {simSpeed}x</span>
                      </button>

                      {isSimFinished ? (
                        <>
                          <button
                            type="button"
                            onClick={() => {
                              currentDistRef.current = 0;
                              setSimIndex(0);
                              setIsSimFinished(false);
                              setIsSimulating(true);
                            }}
                            className="bg-blue-600/20 border border-blue-400 text-blue-300 hover:bg-blue-600/30 text-[9px] font-mono px-2 py-1 rounded transition-colors cursor-pointer flex items-center space-x-1 shadow-sm font-semibold"
                            title="Replay route simulation from beginning"
                          >
                            <span className="material-icons text-[10px]">replay</span>
                            <span>REPLAY</span>
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setIsSimulating(false);
                              setIsSimFinished(false);
                              currentDistRef.current = 0;
                            }}
                            className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20 text-[9px] font-mono px-2 py-1 rounded transition-colors cursor-pointer flex items-center space-x-1"
                            title="Return to real-time Live GPS mode"
                          >
                            <span className="material-icons text-[10px]">gps_fixed</span>
                            <span>LIVE GPS</span>
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          onClick={() => {
                            if (!isSimulating) {
                              currentDistRef.current = 0;
                              setSimIndex(0);
                              setIsSimFinished(false);
                              setIsSimulating(true);
                              if (interpolatedPath.length > 0 && markersRef.current[activeTrip.id]) {
                                markersRef.current[activeTrip.id].setLngLat(interpolatedPath[0]);
                              }
                            } else {
                              setIsSimulating(false);
                              setIsSimFinished(false);
                              currentDistRef.current = 0;
                              setSimIndex(0);
                              const realCoords: [number, number] = (typeof activeTrip.current_lng === 'number' && typeof activeTrip.current_lat === 'number' && activeTrip.current_lng !== 0) 
                                ? [activeTrip.current_lng, activeTrip.current_lat] 
                                : (activeTrip.start_coords || [90.4125, 23.8103]);
                              if (markersRef.current[activeTrip.id]) {
                                markersRef.current[activeTrip.id].setLngLat(realCoords);
                              }
                            }
                          }}
                          className={`border text-[9px] font-mono px-2 py-1 rounded transition-colors cursor-pointer flex items-center space-x-1 ${
                            isSimulating 
                              ? 'bg-amber-500/10 border-amber-500/30 text-amber-400 hover:bg-amber-500/20' 
                              : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20'
                          }`}
                          title={isSimulating ? 'Switch back to Live device GPS' : 'Simulate route progression (Demo)'}
                        >
                          <span className="material-icons text-[10px]">{isSimulating ? 'stop_circle' : 'play_circle'}</span>
                          <span>{isSimulating ? 'SIM ACTIVE' : 'LIVE GPS'}</span>
                        </button>
                      )}
                    </div>
                  </div>
                </div>

                {/* 2. ADMIN ACTIVE LIVE SPEEDOMETER & SPEED LIMIT SIGN */}
                <SpeedometerWidget
                  currentDisplaySpeedKmH={currentDisplaySpeedKmH}
                  speedLimit={speedLimit}
                  isSimulating={isSimulating}
                  isSimFinished={isSimFinished}
                  simSpeed={simSpeed}
                  onCycleSpeedLimit={() => setSpeedLimit(prev => prev === 30 ? 45 : (prev === 45 ? 60 : 30))}
                  className="absolute top-44 left-4 z-40 flex flex-col items-center space-y-2 select-none pointer-events-auto"
                />
              </>
            )}

            {/* 3. FLOAT HUD ACTIONS (Hidden on public tracking and modal preview) */}
            {!isPublicView && !isModalMode && (
              <div className="absolute right-4 top-44 z-40 flex flex-col space-y-2 pointer-events-auto select-none">
                <button
                  type="button"
                  onClick={() => setSimMuted(!simMuted)}
                  className="w-10 h-10 bg-slate-900/95 hover:bg-slate-800 border border-slate-800 rounded-full flex items-center justify-center shadow-lg transition-colors cursor-pointer text-slate-300 hover:text-white"
                  title={simMuted ? 'Unmute voice navigation guidance' : 'Mute voice navigation guidance'}
                >
                  <span className="material-icons text-lg">{simMuted ? 'volume_off' : 'volume_up'}</span>
                </button>
                
                <button
                  type="button"
                  onClick={() => {
                    setSpeedLimit(prev => prev === 30 ? 45 : (prev === 45 ? 55 : 30));
                  }}
                  className="w-10 h-10 bg-slate-900/95 hover:bg-slate-800 border border-slate-800 rounded-full flex items-center justify-center shadow-lg transition-colors cursor-pointer text-slate-300 hover:text-white"
                  title="Toggle alternative route speeds"
                >
                  <span className="material-icons text-lg">alt_route</span>
                </button>

                <button
                  type="button"
                  className="w-10 h-10 bg-slate-900/95 hover:bg-slate-800 border border-slate-800 rounded-full flex items-center justify-center shadow-lg transition-colors cursor-pointer text-slate-300 hover:text-white"
                  title="Send transit event report"
                >
                  <span className="material-icons text-lg">chat_bubble</span>
                </button>
              </div>
            )}

            {/* 4. FLOATING STREET TAG PILL (Admin only - hidden in modal) */}
            {!isPublicView && !isModalMode && totalSteps > 0 && (
              <div className="absolute bottom-24 left-1/2 -translate-x-1/2 z-35 bg-slate-900/90 text-cyan-400 font-sans font-semibold text-[11px] px-3.5 py-1.5 rounded-full border border-cyan-500/30 shadow-xl flex items-center space-x-1.5 select-none backdrop-blur-sm">
                <span className={`w-2 h-2 rounded-full ${isSimFinished ? 'bg-rose-400' : 'bg-cyan-400 animate-pulse'}`}></span>
                <span className={isSimFinished ? 'text-rose-300 font-bold' : 'text-cyan-400'}>
                  {isSimFinished 
                    ? '🏁 Destination Reached • Route Complete' 
                    : (isSimulating ? 'Simulated Route Playback' : 'Live Tracking Active')}
                </span>
              </div>
            )}

            {/* 5. BOTTOM ETA HUD CARD (Admin only - hidden in modal) */}
            {!isPublicView && !isModalMode && (
              <div className="absolute bottom-4 left-4 right-4 z-40 bg-slate-900/95 border border-slate-800 text-white p-4.5 rounded-2xl shadow-xl flex items-center justify-between md:max-w-[420px] md:mx-auto select-none pointer-events-auto">
                <div className="flex items-baseline space-x-2">
                  <span className="text-lime-400 font-sans font-black text-2xl tracking-tight leading-none">
                    <span id="hud-min-display">{isSimFinished ? '0 min' : `${displayMin} min`}</span>
                  </span>
                  <span className="text-slate-300 font-mono text-xs">
                    <span id="hud-mi-display">{(displayMiles * 1.60934).toFixed(2)} km</span>
                  </span>
                </div>

                <div className="flex flex-col items-end">
                  <span className={`text-[11px] font-mono font-bold flex items-center gap-1 ${isSimFinished ? 'text-rose-400' : 'text-emerald-400'}`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${isSimFinished ? 'bg-rose-400' : 'bg-emerald-400 animate-ping'}`}></span>
                    {isSimFinished ? 'ARRIVED 🏁' : (isSimulating ? 'DEMO SIMULATION' : 'LIVE GPS')}
                  </span>
                  <span className="text-[10px] text-slate-400 font-mono">
                    {isSimFinished ? 'Route Completed' : (isSimulating ? simEtaStr : (activeTrip.transport_mode ? `${activeTrip.transport_mode.toUpperCase()}` : 'ONGOING'))}
                  </span>
                </div>

                <button 
                  type="button"
                  onClick={() => onSelectTrip('')}
                  className="text-slate-400 hover:text-white hover:bg-slate-800 p-1.5 rounded-full transition-colors cursor-pointer flex items-center justify-center"
                  title="Exit Sim Guidance"
                >
                  <span className="material-icons text-sm font-bold">close</span>
                </button>
              </div>
            )}
          </>
        );
      })()}

      {/* Styled Embed Styles for Custom Mapbox Markers */}
        <style jsx global>{`
          .mapbox-custom-marker {
            z-index: 10;
            cursor: pointer;
            width: 36px;
            height: 36px;
            display: flex;
            align-items: center;
            justify-content: center;
            position: relative;
          }

        .mapbox-start-pin,
        .mapbox-destination-pin,
        .mapbox-actual-end-pin,
        .mapbox-dest-marker,
        .mapbox-departure-pin {
          z-index: 9;
          cursor: pointer;
          width: 44px;
          height: 44px;
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
        }

        .mapbox-start-pin { z-index: 10; }
        .mapbox-destination-pin { z-index: 10; }
        .mapbox-actual-end-pin { z-index: 11; }
        .mapbox-custom-marker, .mapbox-sos-marker { z-index: 12; }

        .mapbox-dest-marker.selected .gps-map-pin {
          transform: scale(1.22) rotate(-45deg);
        }

        .mapbox-dest-marker:hover .gps-map-pin {
          transform: scale(1.15) rotate(-45deg);
        }

        .mapbox-dest-marker:hover .marker-tooltip,
        .mapbox-dest-marker.selected .marker-tooltip {
          opacity: 1;
        }

        .flowing-dashed-line {
          animation: fallback-line-flow 1.5s linear infinite;
        }

        @keyframes fallback-line-flow {
          to {
            stroke-dashoffset: -20;
          }
        }
        
        .marker-wrapper {
          position: relative;
          display: flex;
          flex-direction: column;
          align-items: center;
        }

        .mapbox-custom-marker.selected .puck-core {
          transform: scale(1.22);
          border-color: #3b82f6;
          box-shadow: 0 0 14px rgba(59, 130, 246, 0.95), 0 3px 6px rgba(0,0,0,0.4);
        }

        .mapbox-custom-marker:hover .puck-core {
          transform: scale(1.15);
        }

        .marker-ping-ring {
          position: absolute;
          width: 24px;
          height: 24px;
          border-radius: 9999px;
          opacity: 0.75;
          animation: marker-pulse-anim 1.8s infinite;
        }

        .mapbox-custom-marker.sos-alarm .marker-ping-ring {
          background-color: #ef4444;
        }

          /* Dynamic 3D Water-drop Pin shape in pure CSS - Mathematically Geo-Anchored */
          .gps-pin-wrapper {
            position: relative;
            width: 44px;
            height: 44px;
          }

          .gps-pin-wrapper.fallback-size {
            width: 24px;
            height: 24px;
          }

          .gps-map-pin {
            width: 28px;
            height: 28px;
            background: linear-gradient(135deg, #f43f5e, #be123c);
            border-radius: 50% 50% 50% 0;
            
            /* Geo-locking the tip to bottom-center (x: 50%, y: 100%) */
            position: absolute;
            left: 50%;
            bottom: 0;
            transform-origin: 0% 100%; /* Anchor rotation exactly at the sharp bottom-left corner */
            transform: rotate(-45deg);
            
            box-shadow: -2px 2px 5px rgba(0, 0, 0, 0.4);
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          }

        .map-pin-icon {
          transform: rotate(45deg);
          font-size: 16px;
          color: white;
          margin-top: 2px;
          margin-left: 2px;
        }

        .gps-pin-wrapper.fallback-size .gps-map-pin {
          width: 16px;
          height: 16px;
        }

        .gps-pin-wrapper.fallback-size .map-pin-icon {
          font-size: 10px;
          margin-top: 1px;
          margin-left: 1px;
        }

        /* PIN 1: Royal Blue Pin (Start Location / Trip Origin) */
        .gps-map-pin.blue-pin {
          background: linear-gradient(135deg, #3b82f6, #1d4ed8);
          box-shadow: -2px 2px 6px rgba(29, 78, 216, 0.6), inset 1.5px 1.5px 2px rgba(255, 255, 255, 0.85);
        }
        .gps-map-pin.blue-pin.selected {
          background: #2563eb;
          transform: scale(1.22) rotate(-45deg);
          box-shadow: -3px 3px 8px rgba(37, 99, 235, 0.7), inset 1.5px 1.5px 3px rgba(255, 255, 255, 0.95);
        }

        /* PIN 2: Rose Red Pin (Target Planned Destination) */
        .gps-map-pin.red-pin {
          background: linear-gradient(135deg, #f43f5e, #be123c);
          box-shadow: -2px 2px 6px rgba(190, 18, 60, 0.6), inset 1.5px 1.5px 2px rgba(255, 255, 255, 0.85);
        }
        .gps-map-pin.red-pin.selected {
          background: #e11d48;
          transform: scale(1.22) rotate(-45deg);
          box-shadow: -3px 3px 8px rgba(225, 29, 72, 0.7), inset 1.5px 1.5px 3px rgba(255, 255, 255, 0.95);
        }

        /* PIN 3: Emerald Green Pin (Actual End Point / Completed Trip) */
        .gps-map-pin.green-pin {
          background: linear-gradient(135deg, #10b981, #047857);
          box-shadow: -2px 2px 6px rgba(4, 120, 87, 0.6), inset 1.5px 1.5px 2px rgba(255, 255, 255, 0.85);
        }
        .gps-map-pin.green-pin.selected {
          background: #059669;
          transform: scale(1.22) rotate(-45deg);
          box-shadow: -3px 3px 8px rgba(16, 185, 129, 0.7), inset 1.5px 1.5px 3px rgba(255, 255, 255, 0.95);
        }

        /* PIN 3b: Amber Pin (Actual Stop Point when Cancelled or Ended Early) */
        .gps-map-pin.amber-pin {
          background: linear-gradient(135deg, #f59e0b, #d97706);
          box-shadow: -2px 2px 6px rgba(217, 119, 6, 0.6), inset 1.5px 1.5px 2px rgba(255, 255, 255, 0.85);
        }
        .gps-map-pin.amber-pin.selected {
          background: #d97706;
          transform: scale(1.22) rotate(-45deg);
          box-shadow: -3px 3px 8px rgba(217, 119, 6, 0.7), inset 1.5px 1.5px 3px rgba(255, 255, 255, 0.95);
        }

        .gps-map-pin.dynamic-pin {
          background: linear-gradient(135deg, #ec4899, #be185d);
        }
        
        .gps-map-pin.dynamic-pin.selected {
          background: #db2777;
          transform: scale(1.22) rotate(-45deg);
          box-shadow: -3px 3px 6px rgba(236, 72, 153, 0.5), inset 1.5px 1.5px 3px rgba(255, 255, 255, 0.82);
        }

        .gps-map-pin.dynamic-pin.departure {
          background: #2563eb;
        }

        .gps-map-pin.dynamic-pin.departure.selected {
          background: #1d4ed8;
          transform: scale(1.22) rotate(-45deg);
          box-shadow: -3px 3px 6px rgba(37, 99, 235, 0.5), inset 1.5px 1.5px 3px rgba(255, 255, 255, 0.82);
        }

        .gps-pin-pulse-ring {
          position: absolute;
          width: 24px;
          height: 10px;
          background: rgba(225, 29, 72, 0.22);
          border: 1px solid rgba(225, 29, 72, 0.5);
          border-radius: 50%;
          left: 50%;
          bottom: 0;
          margin-left: -12px;
          margin-bottom: -5px;
          transform: scale(1);
          animation: pin-pulse-grow 1.8s infinite ease-out;
          pointer-events: none;
        }

        .gps-pin-pulse-ring.blue {
          background: rgba(37, 99, 235, 0.25);
          border-color: rgba(37, 99, 235, 0.6);
        }

        .gps-pin-pulse-ring.red {
          background: rgba(225, 29, 72, 0.25);
          border-color: rgba(225, 29, 72, 0.6);
        }

        .gps-pin-pulse-ring.green {
          background: rgba(16, 185, 129, 0.25);
          border-color: rgba(16, 185, 129, 0.6);
        }

        .gps-pin-pulse-ring.amber {
          background: rgba(245, 158, 11, 0.25);
          border-color: rgba(245, 158, 11, 0.6);
        }

        .gps-pin-pulse-ring.pink {
          background: rgba(236, 72, 153, 0.22);
          border-color: rgba(236, 72, 153, 0.5);
        }

        @keyframes pin-pulse-grow {
          0% {
            transform: scale(0.6);
            opacity: 1;
          }
          100% {
            transform: scale(2.4);
            opacity: 0;
          }
        }

        .gps-navigation-puck {
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 36px;
          height: 36px;
        }

        .gps-navigation-puck.mini {
          width: 26px;
          height: 26px;
        }

        .puck-pulse-ring {
          position: absolute;
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: rgba(6, 182, 212, 0.2);
          border: 1px solid rgba(6, 182, 212, 0.5);
          animation: marker-pulse-anim 1.8s infinite;
          pointer-events: none;
        }

        .gps-navigation-puck.mini .puck-pulse-ring {
          width: 24px;
          height: 24px;
        }

        .puck-pulse-ring.puck-selected {
          background: rgba(6, 182, 212, 0.25);
          border-color: rgba(6, 182, 212, 0.7);
        }

        .puck-pulse-ring.puck-sos {
          background: rgba(239, 68, 68, 0.35);
          border-color: rgba(239, 68, 68, 0.85);
          animation: marker-pulse-anim 0.9s infinite;
        }

        .puck-core {
          position: relative;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: #06b6d4;
          border: 2px solid #ffffff;
          box-shadow: 0 0 12px rgba(6, 182, 212, 0.85), 0 3px 6px rgba(0,0,0,0.4);
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .gps-navigation-puck.mini .puck-core {
          width: 16px;
          height: 16px;
        }

        .puck-core.selected {
          background: #ffffff;
          border: 2.2px solid #0891b2;
          box-shadow: 0 0 16px rgba(6, 182, 212, 0.95), 0 4px 8px rgba(0,0,0,0.35);
        }

        .puck-core.selected .puck-arrow-delta {
          border-bottom-color: #0891b2;
        }

        .puck-core.sos {
          background: #ef4444;
          border: 2.2px solid #ffffff;
          box-shadow: 0 0 18px rgba(239, 68, 68, 1), 0 4px 8px rgba(0,0,0,0.4);
          animation: sos-beacon-blink 0.7s infinite alternate;
        }

        @keyframes sos-beacon-blink {
          from { transform: scale(1); }
          to { transform: scale(1.2); }
        }

        .puck-rotation {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .puck-arrow-delta {
          width: 0;
          height: 0;
          border-left: 5px solid transparent;
          border-right: 5px solid transparent;
          border-bottom: 9px solid #ffffff;
          transform: translateY(-1px);
        }

        .gps-navigation-puck.mini .puck-arrow-delta {
          border-left-width: 4px;
          border-right-width: 4px;
          border-bottom-width: 7.5px;
        }

        /* Flat, persistent progress time pill beside traveler puck */
        .puck-time-pill {
          position: absolute;
          left: calc(100% + 5px);
          top: 50%;
          transform: translateY(-50%);
          display: flex;
          align-items: center;
          gap: 3.5px;
          background: rgba(15, 23, 42, 0.92);
          border: 1px solid rgba(6, 182, 212, 0.5);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.6), 0 0 10px rgba(6, 182, 212, 0.2);
          backdrop-filter: blur(4px);
          -webkit-backdrop-filter: blur(4px);
          padding: 2px 6px;
          border-radius: 6px;
          pointer-events: none;
          white-space: nowrap;
          user-select: none;
          z-index: 25;
        }

        .puck-time-text {
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 10px;
          font-weight: 700;
          color: #38bdf8;
          letter-spacing: -0.01em;
          line-height: 1;
        }

        .puck-time-icon {
          font-size: 10.5px;
          color: #06b6d4;
          line-height: 1;
          display: inline-flex;
        }

        .mapbox-custom-marker:not(.selected-pulse) .puck-time-pill {
          display: none;
        }

        .mapbox-sos-marker .puck-time-pill {
          display: flex;
          border-color: rgba(239, 68, 68, 0.6);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.6), 0 0 10px rgba(239, 68, 68, 0.3);
        }
        .mapbox-sos-marker .puck-time-text {
          color: #f87171;
        }
        .mapbox-sos-marker .puck-time-icon {
          color: #ef4444;
        }

        .marker-tooltip {
          position: absolute;
          top: -28px;
          background-color: rgba(15, 23, 42, 0.95);
          color: white;
          font-family: monospace;
          font-size: 8.5px;
          padding: 2.5px 6.5px;
          border-radius: 5px;
          border: 1px solid #334155;
          white-space: nowrap;
          pointer-events: none;
          opacity: 0;
          transition: opacity 0.2s ease;
          box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.6);
          z-index: 50;
        }

        .mapbox-custom-marker:hover .marker-tooltip,
        .mapbox-custom-marker.selected .marker-tooltip,
        .mapbox-destination-pin:hover .marker-tooltip,
        .mapbox-destination-pin.selected .marker-tooltip,
        .mapbox-start-pin:hover .marker-tooltip,
        .mapbox-start-pin.selected .marker-tooltip,
        .mapbox-actual-end-pin:hover .marker-tooltip,
        .mapbox-actual-end-pin.selected .marker-tooltip,
        .mapbox-departure-pin:hover .marker-tooltip {
          opacity: 1;
        }

        @keyframes marker-pulse-anim {
          0% {
            transform: scale(1);
            opacity: 0.85;
          }
          100% {
            transform: scale(2.6);
            opacity: 0;
          }
        }

        /* Dynamic Traffic signals styling for pilot simulator navigation */
        .traffic-light-node {
          display: flex;
          flex-direction: column;
          background: #0f172a;
          border: 1.5px solid #334155;
          border-radius: 6px;
          padding: 2.5px;
          gap: 2.5px;
          box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
          align-items: center;
        }

        .light-dot {
          width: 5.5px;
          height: 5.5px;
          border-radius: 50%;
          background: #334155;
          transition: all 0.2s ease;
        }
        
        /* Interactive Blinking animations for custom Mapbox traffic lights */
        .traffic-light-marker:nth-child(1) .light-dot.red {
          animation: nav-traffic-red-1 7s infinite;
        }
        .traffic-light-marker:nth-child(1) .light-dot.yellow {
          animation: nav-traffic-yellow-1 7s infinite;
        }
        .traffic-light-marker:nth-child(1) .light-dot.green {
          animation: nav-traffic-green-1 7s infinite;
        }

        .traffic-light-marker:nth-child(2) .light-dot.red {
          animation: nav-traffic-red-2 7s infinite;
        }
        .traffic-light-marker:nth-child(2) .light-dot.yellow {
          animation: nav-traffic-yellow-2 7s infinite;
        }
        .traffic-light-marker:nth-child(2) .light-dot.green {
          animation: nav-traffic-green-2 7s infinite;
        }

        /* Signal 1 cycles */
        @keyframes nav-traffic-red-1 {
          0%, 45% { background: #ef4444; box-shadow: 0 0 6px #f87171, inset 0.5px 0.5px 1px #ffffff; }
          48%, 100% { background: #1e293b; box-shadow: none; }
        }
        @keyframes nav-traffic-green-1 {
          0%, 45% { background: #1e293b; box-shadow: none; }
          48%, 92% { background: #22c55e; box-shadow: 0 0 6px #4ade80, inset 0.5px 0.5px 1px #ffffff; }
          95%, 100% { background: #1e293b; box-shadow: none; }
        }
        @keyframes nav-traffic-yellow-1 {
          0%, 91% { background: #1e293b; box-shadow: none; }
          92%, 94% { background: #eab308; box-shadow: 0 0 6px #fde047; }
          95%, 100% { background: #1e293b; box-shadow: none; }
        }

        /* Signal 2 cycles */
        @keyframes nav-traffic-red-2 {
          0%, 35% { background: #1e293b; box-shadow: none; }
          38%, 85% { background: #ef4444; box-shadow: 0 0 6px #f87171, inset 0.5px 0.5px 1px #ffffff; }
          88%, 100% { background: #1e293b; box-shadow: none; }
        }
        @keyframes nav-traffic-green-2 {
          0%, 35% { background: #22c55e; box-shadow: 0 0 6px #4ade80, inset 0.5px 0.5px 1px #ffffff; }
          38%, 92% { background: #1e293b; box-shadow: none; }
          93%, 100% { background: #22c55e; box-shadow: 0 0 6px #4ade80, inset 0.5px 0.5px 1px #ffffff; }
        }
        @keyframes nav-traffic-yellow-2 {
          0%, 84% { background: #1e293b; box-shadow: none; }
          85%, 87% { background: #eab308; box-shadow: 0 0 6px #fde047; }
          88%, 100% { background: #1e293b; box-shadow: none; }
        /* Completely remove Mapbox branding, logos, and attribution links */
        .mapboxgl-ctrl-logo,
        .mapboxgl-ctrl-attrib,
        .mapboxgl-ctrl-attrib-inner,
        .mapbox-improve-map,
        .mapboxgl-compact,
        .mapboxgl-ctrl-bottom-left,
        .mapboxgl-ctrl-bottom-right {
          display: none !important;
          visibility: hidden !important;
          opacity: 0 !important;
          pointer-events: none !important;
          height: 0 !important;
          width: 0 !important;
        }

        /* Elevate and style Mapbox zoom and compass controls so they are 100% clickable */
        .mapboxgl-ctrl-top-right {
          top: 14px !important;
          right: 14px !important;
          z-index: 40 !important;
          pointer-events: auto !important;
        }
        .mapboxgl-ctrl-top-right .mapboxgl-ctrl-group {
          background: rgba(15, 23, 42, 0.9) !important;
          backdrop-filter: blur(12px) !important;
          border: 1px solid rgba(51, 65, 85, 0.8) !important;
          border-radius: 14px !important;
          overflow: hidden !important;
          box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.6) !important;
        }
        .mapboxgl-ctrl-top-right .mapboxgl-ctrl-group button {
          width: 36px !important;
          height: 36px !important;
          border-bottom: 1px solid rgba(51, 65, 85, 0.5) !important;
          cursor: pointer !important;
          pointer-events: auto !important;
          background-color: transparent !important;
        }
        .mapboxgl-ctrl-top-right .mapboxgl-ctrl-group button:hover {
          background-color: rgba(51, 65, 85, 0.7) !important;
        }
        .mapboxgl-ctrl-top-right .mapboxgl-ctrl-group button .mapboxgl-ctrl-icon {
          filter: invert(1) brightness(2) !important;
        }
      `}</style>

    </div>
  );
}

































