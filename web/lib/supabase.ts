// Supabase Integration & Mock Database Engine
// Supports both a active Supabase client and a local storage-backed mock DB for preview environments

export interface Profile {
  id: string;
  phone_number: string;
  full_name: string;
  is_premium: boolean;
  trip_credits: number;
  points_balance: number;
  role: 'admin' | 'dispatcher' | 'user';
  sos_settings?: any;
  avatar_url?: string | null;
  premium_until?: string | null;
}

export interface SubscriptionPlan {
  id: string;
  name: string;
  billing_period: 'monthly' | 'quarterly' | 'yearly' | string;
  duration_days: number;
  price: number;
  discount_price?: number | null;
  currency: string;
  features: string[];
  is_active: boolean;
  is_popular: boolean;
  display_order: number;
  created_at?: string;
}

export interface PaymentTransaction {
  id: string;
  user_id: string;
  plan_id?: string | null;
  amount: number;
  currency: string;
  payment_method: 'bkash' | 'nagad' | 'rocket' | string;
  sender_number: string;
  transaction_id: string;
  status: 'pending' | 'approved' | 'rejected';
  admin_notes?: string | null;
  reviewed_by?: string | null;
  reviewed_at?: string | null;
  created_at: string;
  // joined/enriched fields
  profiles?: { full_name?: string; phone_number?: string; email?: string } | null;
  subscription_plans?: { name?: string; duration_days?: number } | null;
}


export interface Trip {
  id: string;
  user_id: string;
  start_location: string;
  end_location: string;
  status: 'ongoing' | 'completed' | 'sos';
  created_at: string;
  start_coords?: [number, number];
  end_coords?: [number, number];
  transport_mode?: string;
  start_time?: string;
  end_time?: string;
  start_battery_level?: number;
  end_battery_level?: number;
  device_model?: string;
  total_distance?: number;
  route_path_log?: any;
  planned_route_log?: any;
  sos_activity_logs?: any;
  current_lat?: number | null;
  current_lng?: number | null;
  tracking_code?: string | null;
  vehicle_photo_url?: string | null;
  vehicle_plate_number?: string | null;
  vehicle_description?: string | null;
  audio_clip_url?: string | null;
  sos_triggered_at?: string | null;
  notified_guardians?: string[] | null;
  estimated_distance_km?: number | null;
  estimated_duration_min?: number | null;
}

export interface SOSRecord {
  id: string;
  trip_id: string;
  triggered_at: string;
  status: 'active' | 'resolved' | 'dismissed';
}

export interface PointsLog {
  id: string;
  user_id: string;
  points_added: number;
  source: string;
  timestamp: string;
}

export interface Guardian {
  id: string;
  user_id: string;
  name: string;
  relationship?: string | null;
  relation?: string | null;
  phone: string;
  avatar_index?: number | null;
  default_notify?: boolean | null;
  sos_permission?: boolean | null;
  created_at?: string | null;
}

export interface Banner {
  id: string;
  title: string;
  image_url: string;
  action_url?: string | null;
  display_order?: number | null;
  is_active: boolean;
  start_date?: string | null;
  end_date?: string | null;
  created_at?: string;
}

export interface TravelActivity {
  id: string;
  user_id?: string | null;
  transport_mode: string;
  start_address?: string | null;
  end_address?: string | null;
  start_coords?: string | null;
  end_coords?: string | null;
  start_time?: string | null;
  end_time?: string | null;
  total_distance?: number | null;
  estimated_distance_km?: number | null;
  estimated_duration_min?: number | null;
  vehicle_plate_number?: string | null;
  vehicle_description?: string | null;
  vehicle_photo_url?: string | null;
  driver_name_manual?: string | null;
  safety_status?: string | null; // 'ongoing' | 'completed' | 'sos' | 'deviation' | 'gps_lost'
  sos_triggered_at?: string | null;
  sos_activity_logs?: any | null;
  tracking_code?: string | null;
  route_path_log?: any | null; // JSON / Array of points (traveled trail)
  planned_route_log?: any | null; // JSON / Array of points (fixed planned route)
  current_lat?: number | null;
  current_lng?: number | null;
  route_deviation_count?: number | null;
  unexpected_stop_logs?: any | null; // JSON / Array of stops
  speed_profile?: any | null; // JSON / Array of speeds
  start_battery_level?: number | null;
  end_battery_level?: number | null;
  is_gps_lost?: boolean | null;
  device_model?: string | null;
  notified_guardians?: string[] | null;
  guardian_check_in_count?: number | null;
  audio_clip_url?: string | null;
  impact_detected?: boolean | null;
  area_safety_score?: number | null;
  created_at?: string | null;
}

export interface SafetyAudioLog {
  id: string;
  user_id?: string | null;
  trip_id?: string | null;
  audio_url: string;
  cloudinary_public_id?: string | null;
  duration_sec: number;
  file_size_bytes: number;
  recorded_address?: string | null;
  recorded_coords?: string | null;
  source_trigger?: string | null;
  created_at?: string | null;
  is_deleted_by_user?: boolean;
  user_deleted_at?: string | null;
}

// Raw SQL Schema representing the requested Supabase DB Tables
export const SUPABASE_SQL_SCHEMA = `-- Safe Travel Database Schema

-- 1. Create Enums for roles and trip states
CREATE TYPE user_role AS ENUM ('admin', 'dispatcher', 'user');
CREATE TYPE trip_status AS ENUM ('ongoing', 'completed', 'sos');
CREATE TYPE sos_status AS ENUM ('active', 'resolved', 'dismissed');

-- 2. Profiles Table (extending supabase auth.users)
CREATE TABLE profiles (
  id UUID REFERENCES auth.users ON DELETE CASCADE PRIMARY KEY,
  phone_number TEXT,
  full_name TEXT NOT NULL,
  is_premium BOOLEAN DEFAULT FALSE NOT NULL,
  trip_credits INT DEFAULT 0 NOT NULL,
  points_balance INT DEFAULT 0 NOT NULL,
  role user_role DEFAULT 'user'::user_role NOT NULL
);

-- 3. Trips Table
CREATE TABLE trips (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  start_location TEXT NOT NULL,
  end_location TEXT NOT NULL,
  status trip_status DEFAULT 'ongoing'::trip_status NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 4. SOS Records Table
CREATE TABLE sos_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  trip_id UUID REFERENCES trips(id) ON DELETE CASCADE NOT NULL,
  triggered_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
  status sos_status DEFAULT 'active'::sos_status NOT NULL
);

-- 5. Points Log Table
CREATE TABLE points_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  points_added INT NOT NULL,
  source TEXT NOT NULL,
  timestamp TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS (Row Level Security) and basic policies
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE sos_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE points_log ENABLE ROW LEVEL SECURITY;

-- 6. Banners Table
CREATE TABLE banners (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title TEXT NOT NULL,
  image_url TEXT NOT NULL,
  action_url TEXT,
  display_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

ALTER TABLE banners ENABLE ROW LEVEL SECURITY;

-- Dynamic profiles policies
CREATE POLICY "Users can edit their own profiles" ON profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Public profile view" ON profiles FOR SELECT USING (true);
CREATE POLICY "Admins/Dispatchers can view full tables" ON profiles FOR ALL USING (
  EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND profiles.role IN ('admin', 'dispatcher'))
);

-- Banners policies
CREATE POLICY "Public banner view" ON banners FOR SELECT USING (true);
CREATE POLICY "Admins can manage banners" ON banners FOR ALL USING (
  EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND profiles.role = 'admin')
);

-- 7. Travel Activities Table (Highly Optimized Real-time Telemetry logs)
CREATE TABLE travel_activities (
  id UUID NOT NULL DEFAULT gen_random_uuid (),
  user_id UUID NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  transport_mode TEXT NOT NULL,
  start_address TEXT NULL,
  end_address TEXT NULL,
  start_coords TEXT NULL,
  end_coords TEXT NULL,
  start_time TIMESTAMP WITH TIME ZONE NULL DEFAULT NOW(),
  end_time TIMESTAMP WITH TIME ZONE NULL,
  total_distance NUMERIC NULL,
  vehicle_plate_number TEXT NULL,
  vehicle_description TEXT NULL,
  vehicle_photo_url TEXT NULL,
  driver_name_manual TEXT NULL,
  safety_status TEXT NULL DEFAULT 'ongoing'::TEXT,
  sos_triggered_at TIMESTAMP WITH TIME ZONE NULL,
  route_path_log JSONB NULL,
  route_deviation_count INTEGER NULL DEFAULT 0,
  unexpected_stop_logs JSONB NULL,
  speed_profile JSONB NULL,
  start_battery_level INTEGER NULL,
  end_battery_level INTEGER NULL,
  is_gps_lost BOOLEAN NULL DEFAULT FALSE,
  device_model TEXT NULL,
  notified_guardians TEXT[] NULL,
  guardian_check_in_count INTEGER NULL DEFAULT 0,
  audio_clip_url TEXT NULL,
  impact_detected BOOLEAN NULL DEFAULT FALSE,
  area_safety_score NUMERIC NULL,
  created_at TIMESTAMP WITH TIME ZONE NULL DEFAULT NOW(),
  CONSTRAINT travel_activities_pkey PRIMARY KEY (id)
);

ALTER TABLE travel_activities ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow authenticated read" ON travel_activities FOR SELECT USING (true);
CREATE POLICY "Allow individual insert" ON travel_activities FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow individual update" ON travel_activities FOR UPDATE USING (true);
`;

// Default initial dataset
const INITIAL_PROFILES: Profile[] = [
  { id: 'usr-1', phone_number: '+1 (555) 234-5678', full_name: 'Sarah Jenkins', is_premium: true, trip_credits: 15, points_balance: 750, role: 'user' },
  { id: 'usr-2', phone_number: '+1 (555) 345-6789', full_name: 'Marcus Brody', is_premium: false, trip_credits: 0, points_balance: 120, role: 'user' },
  { id: 'usr-3', phone_number: '+1 (555) 456-7890', full_name: 'Amara Kante', is_premium: true, trip_credits: 8, points_balance: 450, role: 'user' },
  { id: 'usr-4', phone_number: '+1 (555) 901-2345', full_name: 'Hiroshi Tanaka', is_premium: true, trip_credits: 50, points_balance: 2400, role: 'admin' },
  { id: 'usr-5', phone_number: '+1 (555) 876-5432', full_name: 'Elena Rostova', is_premium: false, trip_credits: 3, points_balance: 310, role: 'dispatcher' },
  { id: 'usr-6', phone_number: '+1 (555) 765-4321', full_name: 'David Kim', is_premium: false, trip_credits: 0, points_balance: 50, role: 'user' },
];

const INITIAL_TRIPS: Trip[] = [
  { id: 'trip-101', user_id: 'usr-1', start_location: 'Downtown Terminal, NY', end_location: 'Central Airport JFK, NY', status: 'completed', created_at: new Date(Date.now() - 30 * 60000).toISOString() },
  { id: 'trip-102', user_id: 'usr-2', start_location: 'Union Square Hub, SF', end_location: 'SFO International, CA', status: 'ongoing', created_at: new Date(Date.now() - 45 * 60000).toISOString() },
  { id: 'trip-103', user_id: 'usr-3', start_location: 'Metro Station, Seattle', end_location: 'University District, WA', status: 'ongoing', created_at: new Date(Date.now() - 15 * 60000).toISOString() },
  { id: 'trip-104', user_id: 'usr-6', start_location: 'East Side Mall, Miami', end_location: 'South Beach Residences, FL', status: 'completed', created_at: new Date(Date.now() - 3 * 3600000).toISOString() },
];

const INITIAL_SOS: SOSRecord[] = [];

const INITIAL_POINTS: PointsLog[] = [
  { id: 'log-1', user_id: 'usr-1', points_added: 100, source: 'Safe Trip Streak', timestamp: new Date(Date.now() - 24 * 3600000).toISOString() },
  { id: 'log-2', user_id: 'usr-3', points_added: 50, source: 'Account Signup', timestamp: new Date(Date.now() - 48 * 3600000).toISOString() },
  { id: 'log-3', user_id: 'usr-4', points_added: 200, source: 'Platform Admin Setup', timestamp: new Date(Date.now() - 96 * 3600000).toISOString() },
];

// Helper to interact with browser environment smoothly
export type DbState = {
  profiles: Profile[];
  trips: Trip[];
  sosRecords: SOSRecord[];
  pointsLogs: PointsLog[];
  bannerUrl: string;
  banners: Banner[];
  travelActivities: TravelActivity[];
  safetyAudioLogs?: SafetyAudioLog[];
  remoteConfigs?: Record<string, string>;
  guardians?: Guardian[];
  subscriptionPlans?: SubscriptionPlan[];
  paymentTransactions?: PaymentTransaction[];
};

const INITIAL_BANNERS: Banner[] = [];

const INITIAL_TRAVEL_ACTIVITIES: TravelActivity[] = [
  {
    id: 'act-101',
    user_id: 'usr-1',
    transport_mode: 'Uber (Comfort)',
    start_address: 'Downtown Terminal, NY',
    end_address: 'Central Airport JFK, NY',
    start_coords: '40.7128,-74.0060',
    end_coords: '40.6413,-73.7781',
    start_time: new Date(Date.now() - 30 * 60000).toISOString(),
    total_distance: 24.5,
    vehicle_plate_number: 'T782-BCA',
    vehicle_description: 'Toyota Camry Hybrid (Silver)',
    driver_name_manual: 'Marcus Vance',
    tracking_code: 'TRK-9X4-B2',
    safety_status: 'ongoing',
    route_deviation_count: 0,
    start_battery_level: 88,
    end_battery_level: 82,
    device_model: 'iPhone 15 Pro Max',
    area_safety_score: 9.4,
    is_gps_lost: false,
    created_at: new Date(Date.now() - 30 * 60000).toISOString(),
    route_path_log: [
      { lat: 40.7128, lng: -74.0060 },
      { lat: 40.6901, lng: -73.9201 },
      { lat: 40.6655, lng: -73.8512 }
    ]
  },
  {
    id: 'act-102',
    user_id: 'usr-3',
    transport_mode: 'Walking',
    start_address: 'Metro Station, Seattle',
    end_address: 'University District, WA',
    start_coords: '47.6062,-122.3321',
    end_coords: '47.6553,-122.3035',
    start_time: new Date(Date.now() - 15 * 60000).toISOString(),
    total_distance: 3.2,
    vehicle_plate_number: 'N/A',
    vehicle_description: 'Solo Traveler (Walking)',
    driver_name_manual: 'None',
    safety_status: 'completed',
    route_deviation_count: 0,
    start_battery_level: 41,
    end_battery_level: 39,
    device_model: 'Google Pixel 8 Pro',
    area_safety_score: 8.5,
    is_gps_lost: false,
    created_at: new Date(Date.now() - 15 * 60000).toISOString(),
    route_path_log: [
      { lat: 47.6062, lng: -122.3321 },
      { lat: 47.6204, lng: -122.3210 }
    ],
    unexpected_stop_logs: [],
    notified_guardians: ['Family Emergency Group', '+1 (555) 456-1111'],
    guardian_check_in_count: 0,
    impact_detected: false
  },
  {
    id: 'act-103',
    user_id: 'usr-2',
    transport_mode: 'Metro Train',
    start_address: 'Union Square Hub, SF',
    end_address: 'SFO International, CA',
    start_coords: '37.7879,-122.4074',
    end_coords: '37.6213,-122.3790',
    start_time: new Date(Date.now() - 120 * 60000).toISOString(),
    end_time: new Date(Date.now() - 75 * 60000).toISOString(),
    total_distance: 22.1,
    vehicle_plate_number: 'BART-Car-8',
    vehicle_description: 'Bay Area Rapid Transit Line 12',
    driver_name_manual: 'Operator 402',
    safety_status: 'completed',
    route_deviation_count: 1,
    start_battery_level: 94,
    end_battery_level: 79,
    device_model: 'Samsung Galaxy S24 Ultra',
    area_safety_score: 8.9,
    is_gps_lost: false,
    created_at: new Date(Date.now() - 120 * 60000).toISOString()
  }
];

export function getLocalDatabase(): DbState {
  if (typeof window === 'undefined') {
    return { 
      profiles: INITIAL_PROFILES, 
      trips: INITIAL_TRIPS, 
      sosRecords: INITIAL_SOS, 
      pointsLogs: INITIAL_POINTS,
      bannerUrl: '',
      banners: INITIAL_BANNERS,
      travelActivities: INITIAL_TRAVEL_ACTIVITIES,
      guardians: []
    };
  }
  const saved = localStorage.getItem('safetravel_db');
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      if (!parsed.bannerUrl) {
        parsed.bannerUrl = '';
      }
      if (!parsed.banners) {
        parsed.banners = [];
      }
      if (!parsed.travelActivities) {
        parsed.travelActivities = INITIAL_TRAVEL_ACTIVITIES;
      }
      if (!parsed.guardians) {
        parsed.guardians = [];
      }
      // Purge any stale mock SOS or legacy dummy records from browser cache
      if (Array.isArray(parsed.sosRecords)) {
        parsed.sosRecords = parsed.sosRecords.filter((s: any) => s.id !== 'sos-001');
      } else {
        parsed.sosRecords = [];
      }
      if (Array.isArray(parsed.trips)) {
        parsed.trips = parsed.trips.map((t: any) => {
          if (t.id === 'trip-101' && t.status === 'sos') {
            return { ...t, status: 'completed' };
          }
          return t;
        });
      }
      if (Array.isArray(parsed.travelActivities)) {
        parsed.travelActivities = parsed.travelActivities.map((act: any) => {
          if (act.id === 'act-102' && act.safety_status === 'sos') {
            return { ...act, safety_status: 'completed', impact_detected: false, is_gps_lost: false };
          }
          return act;
        });
      }
      return parsed;
    } catch {
      // Fallback
    }
  }
  const initial: DbState = { 
    profiles: INITIAL_PROFILES, 
    trips: INITIAL_TRIPS, 
    sosRecords: INITIAL_SOS, 
    pointsLogs: INITIAL_POINTS,
    bannerUrl: '',
    banners: INITIAL_BANNERS,
    travelActivities: INITIAL_TRAVEL_ACTIVITIES,
    guardians: []
  };
  localStorage.setItem('safetravel_db', JSON.stringify(initial));
  return initial;
}

export function saveLocalDatabase(state: DbState) {
  if (typeof window !== 'undefined') {
    localStorage.setItem('safetravel_db', JSON.stringify(state));
  }
}

const DEFAULT_SUPABASE_URL = "https://vlwisltkkpeslhbdcact.supabase.co";
const DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZsd2lzbHRra3Blc2xoYmRjYWN0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODExNjUyMTAsImV4cCI6MjA5Njc0MTIxMH0.m05XoEXkoFN4jM-Q6ZLkacBh9hava2GHLoqaQZTZXc4";

// Safe check if Supabase URL and ANON key are initialized
export const hasSupabaseConfig = (): boolean => {
  return true;
};

// Safe, lazy-bound Supabase Client initialization to avoid startup compilation failure (Singleton pattern)
let cachedSupabaseClient: any = null;

export function getSupabaseClient() {
  if (cachedSupabaseClient) {
    return cachedSupabaseClient;
  }
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL || DEFAULT_SUPABASE_URL;
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || DEFAULT_SUPABASE_ANON_KEY;
  if (!url || !anonKey) {
    return null;
  }
  try {
    // Avoid import error at top level
    const { createClient } = require('@supabase/supabase-js');
    cachedSupabaseClient = createClient(url, anonKey);
    return cachedSupabaseClient;
  } catch (err) {
    console.error('Failed to instantiate Supabase client:', err);
    return null;
  }
}


// --- REAL-TIME SUPABASE INTEGRATION UTILS ---

export const fetchInitialData = async (supabase: any): Promise<Partial<DbState>> => {
  try {
    const [profilesRes, activitiesRes, sosRes, bannersRes, audioLogsRes, remoteConfigsRes, guardiansRes, plansRes, txsRes] = await Promise.all([
      supabase.from("profiles").select("*"),
      supabase.from("travel_activities").select("*"),
      supabase.from("sos_records").select("*"),
      supabase.from("banners").select("*"),
      supabase.from("safety_audio_logs").select("*").order("created_at", { ascending: false }),
      supabase.from("app_remote_configs").select("*"),
      supabase.from("guardians").select("*"),
      supabase.from("subscription_plans").select("*").order("display_order", { ascending: true }),
      supabase.from("payment_transactions").select("*, profiles(full_name, phone_number), subscription_plans(name, duration_days)").order("created_at", { ascending: false })
    ]);

    const profiles = profilesRes.data || [];
    const dbActivities = activitiesRes.data || [];
    const sosRecords = sosRes.data || [];
    const banners = bannersRes.data || [];
    const audioLogs = audioLogsRes.data || [];
    const guardians = guardiansRes.data || [];
    const subscriptionPlans = plansRes.data || [];
    const paymentTransactions = txsRes.data || [];
    const remoteConfigsList = remoteConfigsRes.data || [];
    const remoteConfigsMap: Record<string, string> = {};
    remoteConfigsList.forEach((rc: any) => {
      if (rc?.key) remoteConfigsMap[rc.key] = rc.value || "";
    });

    // Map travel_activities to trips and activities for the UI
    const mappedTrips: Trip[] = dbActivities.map((act: any) => {
      // If there is an active SOS for this trip, status is SOS
      const isSos = sosRecords.some((s: any) => s.trip_id === act.id && s.status === "active") || act.safety_status === "sos";
      
      // We assume is_active means ongoing, otherwise completed. (Fallback to safety_status if is_active is missing)
      let status: "ongoing" | "completed" | "sos" = "completed";
      if (isSos) status = "sos";
      else if (act.is_active === true || act.safety_status === "ongoing") status = "ongoing";

      let parsedStart: [number, number] | undefined = undefined;
      if (act.current_lat && act.current_lng) {
          parsedStart = [act.current_lng, act.current_lat];
      } else if (act.start_coords) {
          const parts = act.start_coords.split(',');
          if (parts.length === 2) parsedStart = [parseFloat(parts[1]), parseFloat(parts[0])];
      }

      let parsedEnd: [number, number] | undefined = undefined;
      if (act.end_coords) {
          const parts = act.end_coords.split(',');
          if (parts.length === 2) parsedEnd = [parseFloat(parts[1]), parseFloat(parts[0])];
      }

      return {
        id: act.id,
        user_id: act.user_id,
        tracking_code: act.tracking_code,
        start_location: act.start_address || "Current Location",
        end_location: act.end_address || "",
        status: status,
        created_at: act.start_time || act.created_at || new Date().toISOString(),
        start_coords: parsedStart,
        end_coords: parsedEnd,
        start_time: act.start_time,
        end_time: act.end_time,
        start_battery_level: act.start_battery_level,
        end_battery_level: act.end_battery_level,
        device_model: act.device_model,
        total_distance: act.total_distance,
        route_path_log: act.route_path_log,
        planned_route_log: act.planned_route_log,
        sos_activity_logs: act.sos_activity_logs || [],
        current_lat: act.current_lat,
        current_lng: act.current_lng,
        transport_mode: act.transport_mode || 'driving',
        vehicle_photo_url: act.vehicle_photo_url || null,
        vehicle_plate_number: act.vehicle_plate_number || null,
        vehicle_description: act.vehicle_description || null,
        audio_clip_url: act.audio_clip_url || null,
        sos_triggered_at: act.sos_triggered_at || null,
        notified_guardians: act.notified_guardians || [],
        estimated_distance_km: act.estimated_distance_km || null,
        estimated_duration_min: act.estimated_duration_min || null
      };
    });

    const mappedActivities: TravelActivity[] = dbActivities.map((act: any) => {
      // Map current_lat/current_lng into route_path_log so the MapboxMonitor can render the moving dot
      let routePath = act.route_path_log || [];
      if (act.current_lat && act.current_lng) {
        routePath = [...routePath, { lat: act.current_lat, lng: act.current_lng, t: new Date().toISOString() }];
      }

      return {
        id: act.id,
        user_id: act.user_id,
        tracking_code: act.tracking_code,
        transport_mode: act.transport_mode || "Unknown",
        start_address: act.start_address,
        end_address: act.end_address,
        start_coords: act.start_coords,
        end_coords: act.end_coords,
        start_time: act.start_time,
        end_time: act.end_time,
        safety_status: act.safety_status,
        route_path_log: routePath,
        planned_route_log: act.planned_route_log,
        current_lat: act.current_lat,
        current_lng: act.current_lng,
        device_model: act.device_model,
        start_battery_level: act.start_battery_level || act.end_battery_level,
        is_gps_lost: act.is_gps_lost,
        route_deviation_count: act.route_deviation_count,
        impact_detected: act.impact_detected,
        area_safety_score: act.area_safety_score,
        vehicle_plate_number: act.vehicle_plate_number,
        vehicle_description: act.vehicle_description,
        vehicle_photo_url: act.vehicle_photo_url || null,
        driver_name_manual: act.driver_name_manual || null,
        total_distance: act.total_distance != null ? Number(act.total_distance) : null,
        estimated_distance_km: act.estimated_distance_km != null ? Number(act.estimated_distance_km) : null,
        estimated_duration_min: act.estimated_duration_min != null ? Number(act.estimated_duration_min) : null,
        audio_clip_url: act.audio_clip_url || null,
        sos_triggered_at: act.sos_triggered_at || null,
        sos_activity_logs: act.sos_activity_logs || [],
        notified_guardians: act.notified_guardians || [],
        created_at: act.created_at
      };
    });

    return {
      profiles: profiles.map((p: any) => ({
        id: p.id,
        phone_number: p.phone_number || "",
        full_name: p.full_name || "Unknown User",
        is_premium: p.is_premium || false,
        trip_credits: p.trip_credits || 0,
        points_balance: p.points_balance || 0,
        role: p.role || "user",
        avatar_url: p.avatar_url || null,
        sos_settings: p.sos_settings || null,
        premium_until: p.premium_until || null
      })),
      trips: mappedTrips,
      travelActivities: mappedActivities,
      sosRecords: sosRecords.map((s: any) => ({
        id: s.id,
        trip_id: s.trip_id,
        triggered_at: s.triggered_at,
        status: s.status
      })),
      banners: banners.map((b: any) => ({
        id: String(b.id),
        title: b.title || 'Unnamed Banner',
        image_url: b.image_url,
        action_url: b.action_url,
        display_order: b.display_order,
        start_date: b.start_date,
        end_date: b.end_date,
        is_active: Boolean(b.is_active),
        created_at: b.created_at
      })),
      safetyAudioLogs: audioLogs.map((a: any) => ({
        id: a.id,
        user_id: a.user_id,
        trip_id: a.trip_id,
        audio_url: a.audio_url,
        cloudinary_public_id: a.cloudinary_public_id,
        duration_sec: a.duration_sec || 0,
        file_size_bytes: a.file_size_bytes || 0,
        recorded_address: a.recorded_address,
        recorded_coords: a.recorded_coords,
        source_trigger: a.source_trigger,
        created_at: a.created_at
      })),
      remoteConfigs: remoteConfigsMap,
      guardians: guardians.map((g: any) => ({
        id: String(g.id),
        user_id: String(g.user_id),
        name: g.name || "Guardian",
        relationship: g.relationship || g.relation || "Guardian",
        relation: g.relation || g.relationship || "Guardian",
        phone: g.phone || "",
        avatar_index: g.avatar_index ?? 0,
        default_notify: g.default_notify ?? true,
        sos_permission: g.sos_permission ?? true,
        created_at: g.created_at
      })),
      subscriptionPlans: subscriptionPlans.map((p: any) => ({
        id: p.id,
        name: p.name,
        billing_period: p.billing_period || 'monthly',
        duration_days: p.duration_days || 30,
        price: Number(p.price || 0),
        discount_price: p.discount_price != null ? Number(p.discount_price) : null,
        currency: p.currency || 'BDT',
        features: Array.isArray(p.features) ? p.features : [],
        is_active: Boolean(p.is_active),
        is_popular: Boolean(p.is_popular),
        display_order: p.display_order || 1,
        created_at: p.created_at
      })),
      paymentTransactions: paymentTransactions.map((tx: any) => ({
        id: tx.id,
        user_id: tx.user_id,
        plan_id: tx.plan_id,
        amount: Number(tx.amount || 0),
        currency: tx.currency || 'BDT',
        payment_method: tx.payment_method || 'bkash',
        sender_number: tx.sender_number || '',
        transaction_id: tx.transaction_id || '',
        status: tx.status || 'pending',
        admin_notes: tx.admin_notes || null,
        reviewed_by: tx.reviewed_by || null,
        reviewed_at: tx.reviewed_at || null,
        created_at: tx.created_at || new Date().toISOString(),
        profiles: tx.profiles || null,
        subscription_plans: tx.subscription_plans || null
      })),
      bannerUrl: banners.length > 0 ? banners[0].image_url : ""
    };
  } catch (error) {
    console.error("Error fetching initial data from Supabase", error);
    return {};
  }
};


