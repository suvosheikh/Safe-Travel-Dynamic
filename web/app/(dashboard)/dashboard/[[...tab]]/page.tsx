'use client';

import React, { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'motion/react';
import { 
  getLocalDatabase, 
  saveLocalDatabase, 
  hasSupabaseConfig, 
  SUPABASE_SQL_SCHEMA,
  Profile, 
  Trip, 
  SOSRecord, 
  PointsLog,
  DbState,
  getSupabaseClient,
  Banner,
  fetchInitialData
} from '../../../../lib/supabase';

import ExecutiveOverviewSection from '../../../../components/sections/ExecutiveOverviewSection';
import DashboardSection from '../../../../components/sections/DashboardSection';
import TravelerRegistrySection from '../../../../components/sections/TravelerRegistrySection';
import UserTravelHistorySection from '../../../../components/sections/UserTravelHistorySection';
import SosAlertsSection from '../../../../components/sections/SosAlertsSection';
import VehicleVaultSection from '../../../../components/sections/VehicleVaultSection';
import AudioBlackboxSection from '../../../../components/sections/AudioBlackboxSection';
import PromoBannerSection from '../../../../components/sections/PromoBannerSection';
import AdMonetizationSection from '../../../../components/sections/AdMonetizationSection';
import SystemSettingsSection from '../../../../components/sections/SystemSettingsSection';
import SubscriptionPlansSection from '../../../../components/sections/SubscriptionPlansSection';
import TripDetailsModal from '../../../../components/TripDetailsModal';
import AuthScreen from '../../../../components/auth/AuthScreen';

// Module-level pure helper functions to guarantee component rendering purity
function createSafeId(prefix: string): string {
  if (typeof window !== 'undefined' && window.crypto && window.crypto.randomUUID) {
    return `${prefix}-${window.crypto.randomUUID().substring(0, 8)}`;
  }
  return `${prefix}-${Math.random().toString(36).substring(2, 11)}`;
}

function createTimestamp(): string {
  return new Date().toISOString();
}

export default function AdminDashboardPage() {
  const [mounted, setMounted] = useState(false);
  const [currentUser, setCurrentUser] = useState<{ email: string; role: 'admin' | 'dispatcher' | 'user'; name: string; id: string } | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);

  React.useEffect(() => {
    let active = true;
    
    const checkSession = async () => {
      try {
        const supabase = getSupabaseClient();
        if (hasSupabaseConfig() && supabase) {
          const { data: { session } } = await supabase.auth.getSession();
          if (session?.user && active) {
            // Fetch profile
            const { data: profile } = await supabase
              .from('profiles')
              .select('id, full_name, role')
              .eq('id', session.user.id)
              .single();

            if (profile) {
              const roleRaw = String(profile.role || '').toLowerCase();
              const normalizedRole = (roleRaw === 'super_admin' || roleRaw === 'admin')
                ? 'admin'
                : (roleRaw === 'dispatcher' ? 'dispatcher' : 'user');

              setCurrentUser({
                id: session.user.id,
                email: session.user.email || '',
                role: normalizedRole,
                name: profile.full_name || 'Terminal Operator',
              });
            } else {
              setCurrentUser({
                id: session.user.id,
                email: session.user.email || '',
                role: 'admin',
                name: session.user.email?.split('@')[0] || 'Terminal Operator',
              });
            }
          }
        } else {
          // Check local storage mock session
          const savedSession = localStorage.getItem('safetravel_session');
          if (savedSession && active) {
            try {
              const parsed = JSON.parse(savedSession);
              const r = String(parsed?.role || '').toLowerCase();
              if (r === 'super_admin' || r === 'admin') {
                parsed.role = 'admin';
              }
              setCurrentUser(parsed);
            } catch {
              // fallback
            }
          }
        }
      } catch (err) {
        console.error('Session validation failed:', err);
      } finally {
        if (active) {
          setCheckingSession(false);
          setMounted(true);
        }
      }
    };

    checkSession();

    return () => {
      active = false;
    };
  }, []);

  const handleLogout = async () => {
    try {
      const supabase = getSupabaseClient();
      if (hasSupabaseConfig() && supabase) {
        await supabase.auth.signOut();
      }
      localStorage.removeItem('safetravel_session');
      setCurrentUser(null);
    } catch (err) {
      console.error('Error during secure session termination:', err);
    }
  };

  const handleAuthSuccess = (user: { email: string; role: 'admin' | 'dispatcher' | 'user'; name: string; id: string }) => {
    setCurrentUser(user);
    if (!hasSupabaseConfig()) {
      localStorage.setItem('safetravel_session', JSON.stringify(user));
    }
  };

  // Helper to prevent hydration mismatch for localized date/time values
  const formatTime = (isoString: string) => {
    if (!mounted) return '...';
    try {
      return new Date(isoString).toLocaleTimeString();
    } catch {
      return '...';
    }
  };

  const formatDateTime = (isoString: string) => {
    if (!mounted) return '...';
    try {
      return new Date(isoString).toLocaleString();
    } catch {
      return '...';
    }
  };

  // Navigation & UI State
  const params = useParams();
  const router = useRouter();
  const tabSegments = (params?.tab as string[]) || [];
  const rawTab = tabSegments[0] || 'dashboard';
  const activeTab = rawTab;
  const isUserTravelHistory = tabSegments.length >= 2 && tabSegments[0] === 'users';
  const targetUserIdForTravels = isUserTravelHistory ? tabSegments[1] : null;
  
  const setActiveTab = (tab: string) => {
    if (tab === 'dashboard') router.push('/dashboard');
    else router.push('/dashboard/' + tab);
  };
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [inspectedTripId, setInspectedTripId] = useState<string | null>(null);
  
  // Database States initialized lazily to avoid state-setting in side-effects
  const [db, setDb] = useState<DbState>(() => getLocalDatabase());

  // Traveler App Simulator States
  const [simulatedUserId, setSimulatedUserId] = useState<string>('usr-1');
  const [simTransitStart, setSimTransitStart] = useState<string>('Times Square, Manhattan, NY');
  const [simTransitEnd, setSimTransitEnd] = useState<string>('JFK Airport, Queens, NY');
  const [simBookingSuccess, setSimBookingSuccess] = useState<boolean>(false);

  // Selected Active Trip for Radar Map Highlight initialized dynamically on load
  const [selectedTripId, setSelectedTripId] = useState<string | null>(() => {
    const localDb = getLocalDatabase();
    const activeSos = localDb.trips.find(t => t.status === 'sos');
    const ongoing = localDb.trips.find(t => t.status === 'ongoing');
    return activeSos ? activeSos.id : (ongoing ? ongoing.id : null);
  });

  // Initialize saved display scale preference on mount
  React.useEffect(() => {
    try {
      const saved = localStorage.getItem('safetravel_display_scale') as 'auto' | 'fhd' | '100' | null;
      if (saved && (saved === 'auto' || saved === 'fhd' || saved === '100')) {
        document.documentElement.setAttribute('data-display-scale', saved);
      } else {
        document.documentElement.setAttribute('data-display-scale', 'auto');
      }
    } catch {
      document.documentElement.setAttribute('data-display-scale', 'auto');
    }
  }, []);

  // Real-time Operational Telemetry & Sync States
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [lastSyncTime, setLastSyncTime] = useState<Date | null>(null);
  const [realtimeStatus, setRealtimeStatus] = useState<'connected' | 'connecting' | 'error'>('connecting');

  // Centralized, Resilient Fetcher for Operational Data
  const fetchOperationalData = React.useCallback(async (silent = false) => {
    const supabase = getSupabaseClient();
    if (!supabase || !hasSupabaseConfig()) return;

    if (!silent) setIsSyncing(true);
    try {
      const initialData = await fetchInitialData(supabase);
      if (initialData && Object.keys(initialData).length > 0) {
        setDb(currentDb => {
          const mappedBanners = initialData.banners || currentDb.banners;
          const activeB = mappedBanners.find((b: any) => b.is_active) || mappedBanners[0];
          const activeUrl = activeB ? activeB.image_url : currentDb.bannerUrl;

          const updatedDb = { 
            ...currentDb, 
            ...initialData,
            banners: mappedBanners,
            bannerUrl: activeUrl || currentDb.bannerUrl
          };
          saveLocalDatabase(updatedDb);

          // Auto-select a trip if none selected
          if (!selectedTripId) {
            const activeSos = updatedDb.trips.find(t => t.status === "sos");
            const ongoing = updatedDb.trips.find(t => t.status === "ongoing");
            if (activeSos) setSelectedTripId(activeSos.id);
            else if (ongoing) setSelectedTripId(ongoing.id);
          }

          return updatedDb;
        });
        setLastSyncTime(new Date());
      }
    } catch (err) {
      console.error('[fetchOperationalData] Failed to fetch:', err);
    } finally {
      if (!silent) {
        setTimeout(() => setIsSyncing(false), 350);
      }
    }
  }, [selectedTripId]);

  // New Profile Form State
  const [newProfileName, setNewProfileName] = useState('');
  const [newProfilePhone, setNewProfilePhone] = useState('');
  const [newProfileRole, setNewProfileRole] = useState<'admin' | 'dispatcher' | 'user'>('user');


  // Sync back to local storage on changes
  const updateDbState = (newDb: typeof db) => {
    setDb(newDb);
    saveLocalDatabase(newDb);
  };

  // Trigger emergency alarm (S.O.S) on a trip
  const triggerSos = (tripId: string) => {
    const updatedTrips = db.trips.map(trip => {
      if (trip.id === tripId) {
        return { ...trip, status: 'sos' as const };
      }
      return trip;
    });

    // Create SOS record if it doesn't already exist
    const sosExist = db.sosRecords.some(s => s.trip_id === tripId && s.status === 'active');
    let updatedSos = [...db.sosRecords];
    if (!sosExist) {
      const newSos: SOSRecord = {
        id: createSafeId('sos'),
        trip_id: tripId,
        triggered_at: createTimestamp(),
        status: 'active'
      };
      updatedSos = [newSos, ...updatedSos];
    }

    updateDbState({
      ...db,
      trips: updatedTrips,
      sosRecords: updatedSos
    });
    setSelectedTripId(tripId);
  };

  // Resolve emergency alarm
  const resolveSos = async (sosRecordId: string) => {
    const sosRecord = db.sosRecords.find(s => s.id === sosRecordId);
    if (!sosRecord) return;

    const updatedSos = db.sosRecords.map(s => {
      if (s.id === sosRecordId) return { ...s, status: 'resolved' as const };
      return s;
    });

    const updatedTrips = db.trips.map(t => {
      if (t.id === sosRecord.trip_id) return { ...t, status: 'completed' as const };
      return t;
    });

    updateDbState({
      ...db,
      trips: updatedTrips,
      sosRecords: updatedSos
    });

    try {
      const supabase = getSupabaseClient();
      if (supabase && hasSupabaseConfig()) {
        await supabase
          .from('sos_records')
          .update({ status: 'resolved' })
          .eq('id', sosRecordId);

        if (sosRecord.trip_id) {
          await supabase
            .from('travel_activities')
            .update({ safety_status: 'completed' })
            .eq('id', sosRecord.trip_id);
        }
      }
    } catch (err) {
      console.error('Failed to sync resolved SOS to Supabase:', err);
    }
  };

  // Toggle user account tier (Premium/Standard)
  const togglePremium = (userId: string) => {
    const updatedProfiles = db.profiles.map(p => {
      if (p.id === userId) {
        return { ...p, is_premium: !p.is_premium };
      }
      return p;
    });
    updateDbState({ ...db, profiles: updatedProfiles });
  };

  // Reset traveler trip credits
  const adjustCredits = (userId: string, amount: number) => {
    const updatedProfiles = db.profiles.map(p => {
      if (p.id === userId) {
        return { ...p, trip_credits: Math.max(0, p.trip_credits + amount) };
      }
      return p;
    });
    updateDbState({ ...db, profiles: updatedProfiles });
  };

  // Award traveler points (and log it)
  const awardPoints = (userId: string, points: number, reason: string) => {
    const updatedProfiles = db.profiles.map(p => {
      if (p.id === userId) {
        return { ...p, points_balance: p.points_balance + points };
      }
      return p;
    });

    const newLog: PointsLog = {
      id: createSafeId('log'),
      user_id: userId,
      points_added: points,
      source: reason,
      timestamp: createTimestamp()
    };

    updateDbState({
      ...db,
      profiles: updatedProfiles,
      pointsLogs: [newLog, ...db.pointsLogs]
    });
  };

  // Handle addition of a traveler profile
  const handleAddProfile = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProfileName) return;

    const newProfile: Profile = {
      id: createSafeId('usr'),
      full_name: newProfileName,
      phone_number: newProfilePhone || '+1 (555) 000-0000',
      is_premium: false,
      trip_credits: 5,
      points_balance: 100,
      role: newProfileRole
    };

    updateDbState({
      ...db,
      profiles: [...db.profiles, newProfile]
    });

    // Clear form inputs
    setNewProfileName('');
    setNewProfilePhone('');
    setNewProfileRole('user');
  };



  // Simulated Passenger App Event Handlers
  const handleSimulatePassengerBooking = () => {
    if (!simulatedUserId || !simTransitStart.trim() || !simTransitEnd.trim()) return;

    const newTripId = 'trip-' + Math.random().toString(36).substring(2, 11);
    const newTrip: Trip = {
      id: newTripId,
      user_id: simulatedUserId,
      start_location: simTransitStart.trim(),
      end_location: simTransitEnd.trim(),
      status: 'ongoing',
      created_at: new Date().toISOString()
    };

    const targetUser = db.profiles.find(p => p.id === simulatedUserId);
    const isPremium = targetUser?.is_premium ?? false;
    const currentCredits = targetUser?.trip_credits ?? 0;

    if (!isPremium && currentCredits <= 0) {
      alert("[WARNING] Request Rejected: Insufficient trip credit balance! Add safe credits in Traveler Registry, or toggle standard profile as premium.");
      return;
    }

    // Deduct credits if not premium, and add traveler loyalty safety points!
    const updatedProfiles = db.profiles.map(p => {
      if (p.id === simulatedUserId) {
        const cost = p.is_premium ? 0 : 1;
        const newCredits = Math.max(0, p.trip_credits - cost);
        const newPoints = p.points_balance + 40;
        return {
          ...p,
          trip_credits: newCredits,
          points_balance: newPoints
        };
      }
      return p;
    });

    const newLog: PointsLog = {
      id: 'log-' + Math.random().toString(36).substring(2, 11),
      user_id: simulatedUserId,
      points_added: 40,
      source: 'Mobile App Safe Booking Reward',
      timestamp: new Date().toISOString()
    };

    updateDbState({
      ...db,
      profiles: updatedProfiles,
      trips: [newTrip, ...db.trips],
      pointsLogs: [newLog, ...db.pointsLogs]
    });

    setSimBookingSuccess(true);
    setTimeout(() => {
      setSimBookingSuccess(false);
    }, 4500);
  };

  const handleSimulatePassengerSOS = () => {
    // Check if the current passenger has any ongoing transit rides
    const activeTrip = db.trips.find(t => t.user_id === simulatedUserId && t.status === 'ongoing');
    if (!activeTrip) {
      alert("[WARNING] SOS Locked: There are no active, ongoing transit rides running for this traveler! Request a secure ride in the simulated phone interface first.");
      return;
    }

    // Mark the selected transit status as SOS
    const updatedTrips = db.trips.map(t => 
      t.id === activeTrip.id ? { ...t, status: 'sos' as const } : t
    );

    // Create a live SOS report record
    const newSOSId = 'sos-' + Math.random().toString(36).substring(2, 11);
    const newSos: SOSRecord = {
      id: newSOSId,
      trip_id: activeTrip.id,
      triggered_at: new Date().toISOString(),
      status: 'active'
    };

    updateDbState({
      ...db,
      trips: updatedTrips,
      sosRecords: [newSos, ...db.sosRecords]
    });

    alert("[SOS EMERGENCY] PANIC SILENT ALARM DISPATCHED! Dispatchers in HQ are instantly loaded with safety beacon and real-time transit telemetry map tracker.");
  };

  // Main Real-time Supabase Synchronizer for Telemetry & Operations (Self-Healing)
  React.useEffect(() => {
    const supabase = getSupabaseClient();
    if (!supabase || !mounted) return;

    let active = true;

    // 1. Initial Fetch of all operational data
    fetchOperationalData(false);

    // 2. Setup Realtime channels for all mission-critical tables
    const channel = supabase
      .channel("dashboard_updates")
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "travel_activities" },
        async (payload: any) => {
          if (!active) return;
          // Zero-Egress Optimization: On continuous UPDATE telemetry, merge payload directly without any REST HTTP calls
          if (payload.eventType === 'UPDATE' && payload.new) {
            const updated = payload.new;
            setDb(currentDb => {
              const updatedTrips = currentDb.trips.map(t => {
                if (t.id !== updated.id) return t;
                let mergedLog = updated.route_path_log;
                if (!mergedLog || (Array.isArray(mergedLog) && mergedLog.length === 0)) {
                  mergedLog = t.route_path_log;
                  if (updated.current_lat != null && updated.current_lng != null) {
                    const prevList = Array.isArray(mergedLog) ? [...mergedLog] : [];
                    mergedLog = [...prevList, { lat: updated.current_lat, lng: updated.current_lng, t: new Date().toISOString() }];
                  }
                }
                return {
                  ...t,
                  current_lat: updated.current_lat ?? t.current_lat,
                  current_lng: updated.current_lng ?? t.current_lng,
                  route_path_log: mergedLog,
                  status: (updated.safety_status === 'sos') 
                    ? ('sos' as const) 
                    : (updated.safety_status === 'completed' || updated.end_time) 
                    ? ('completed' as const) 
                    : t.status,
                  end_time: updated.end_time ?? t.end_time,
                  total_distance: updated.total_distance ?? t.total_distance,
                  end_battery_level: updated.end_battery_level ?? t.end_battery_level,
                  transport_mode: updated.transport_mode ?? t.transport_mode,
                };
              });

              const updatedActivities = currentDb.travelActivities.map(a => {
                if (a.id !== updated.id) return a;
                return {
                  ...a,
                  ...updated
                };
              });

              const updatedDb = {
                ...currentDb,
                trips: updatedTrips,
                travelActivities: updatedActivities
              };
              saveLocalDatabase(updatedDb);
              return updatedDb;
            });
            setLastSyncTime(new Date());
            return;
          }

          // On INSERT or DELETE, perform a fresh silent query
          await fetchOperationalData(true);
        }
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "sos_records" },
        async (payload: any) => {
          if (!active) return;
          if (payload.eventType === 'INSERT' && payload.new) {
            const newSos = payload.new;
            setDb(currentDb => {
              const updatedTrips = currentDb.trips.map(t => {
                if (t.id === newSos.trip_id && newSos.status === 'active') {
                  return { ...t, status: 'sos' as const };
                }
                return t;
              });
              const updatedDb = {
                ...currentDb,
                trips: updatedTrips,
                sosRecords: [newSos, ...currentDb.sosRecords.filter(s => s.id !== newSos.id)]
              };
              saveLocalDatabase(updatedDb);
              return updatedDb;
            });
            setLastSyncTime(new Date());
            return;
          }
          await fetchOperationalData(true);
        }
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "profiles" },
        async (payload: any) => {
          if (!active) return;
          if (payload.eventType === 'UPDATE' && payload.new) {
            const updatedProf = payload.new;
            setDb(currentDb => {
              const updatedProfiles = currentDb.profiles.map(p => p.id === updatedProf.id ? { ...p, ...updatedProf } : p);
              const updatedDb = { ...currentDb, profiles: updatedProfiles };
              saveLocalDatabase(updatedDb);
              return updatedDb;
            });
            setLastSyncTime(new Date());
            return;
          }
          await fetchOperationalData(true);
        }
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "guardians" },
        async () => {
          if (!active) return;
          await fetchOperationalData(true);
        }
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "safety_audio_logs" },
        async () => {
          if (!active) return;
          await fetchOperationalData(true);
        }
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "payment_transactions" },
        async () => {
          if (!active) return;
          await fetchOperationalData(true);
        }
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "banners" },
        async () => {
          if (!active) return;
          await fetchOperationalData(true);
        }
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "app_remote_configs" },
        async () => {
          if (!active) return;
          await fetchOperationalData(true);
        }
      )
      .subscribe((status, err) => {
        if (!active) return;
        if (status === 'SUBSCRIBED') {
          setRealtimeStatus('connected');
        } else if (status === 'CLOSED' || status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
          setRealtimeStatus('error');
          console.warn(`[Realtime Channel] Status: ${status}`, err);
        } else {
          setRealtimeStatus('connecting');
        }
      });

    // 3. Tab Visibility, Focus & Network Recovery Listeners (Instant Anti-Stall)
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && active) {
        fetchOperationalData(true);
      }
    };

    const handleWindowFocus = () => {
      if (active) {
        fetchOperationalData(true);
      }
    };

    const handleOnline = () => {
      if (active) {
        fetchOperationalData(false);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('focus', handleWindowFocus);
    window.addEventListener('online', handleOnline);

    // 4. Smart Heartbeat (Every 14s when tab is visible to prevent silent desync)
    const heartbeatTimer = setInterval(() => {
      if (document.visibilityState === 'visible' && active) {
        fetchOperationalData(true);
      }
    }, 14000);

    return () => {
      active = false;
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('focus', handleWindowFocus);
      window.removeEventListener('online', handleOnline);
      clearInterval(heartbeatTimer);
      supabase.removeChannel(channel);
    };
  }, [mounted, fetchOperationalData]);

  // 3. Persistent 0ms WebSocket broadcast receiver for selected trip in Monitor
  React.useEffect(() => {
    if (!selectedTripId || !mounted) return;
    const supabase = getSupabaseClient();
    if (!supabase) return;

    const bChannel = supabase
      .channel(`tracking_${selectedTripId}`, {
        config: { broadcast: { self: false } }
      })
      .on('broadcast', { event: 'pos' }, (payload: any) => {
        const data = payload?.payload;
        if (!data || data.lat == null || data.lng == null) return;
        setDb(currentDb => {
          const updatedTrips = currentDb.trips.map(t => {
            if (t.id !== selectedTripId) return t;
            return {
              ...t,
              current_lat: data.lat,
              current_lng: data.lng,
              speed: data.speed ?? (t as any).speed,
              heading: data.heading ?? (t as any).heading
            };
          });
          return { ...currentDb, trips: updatedTrips };
        });
      })
      .subscribe();

    return () => {
      supabase.removeChannel(bChannel);
    };
  }, [selectedTripId, mounted]);


  // Database helper values
  const activeSOSRecords = db.sosRecords.filter(s => s.status === 'active');
  const activeTripsList = db.trips.filter(t => t.status === 'ongoing' || t.status === 'sos');
  const pendingPaymentsCount = db.paymentTransactions?.filter(t => t.status === 'pending').length || 0;
  
  if (checkingSession || !mounted) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans select-none items-center justify-center">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400">
            <span className="material-icons text-xl animate-spin">radar</span>
          </div>
          <p className="text-slate-400 text-xs font-mono uppercase tracking-wider animate-pulse">
            LOADING SAFETRAVEL MONITOR...
          </p>
        </div>
      </div>
    );
  }

  if (!currentUser) {
    return (
      <AuthScreen 
        onAuthSuccess={handleAuthSuccess}
        dbProfiles={db.profiles}
        onUpdateProfiles={(updatedProfiles) => {
          const nextDb = { ...db, profiles: updatedProfiles };
          updateDbState(nextDb);
        }}
      />
    );
  }

  return (
    <div className="h-screen h-[100dvh] bg-slate-50 text-slate-900 flex flex-col font-sans select-none overflow-hidden">
      
      {/* Dynamic S.O.S Header Alert Overlay */}
      <AnimatePresence>
        {activeSOSRecords.length > 0 && (
          <motion.div 
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="bg-red-600 text-white font-mono px-4 py-2 text-center text-xs flex items-center justify-center space-x-2 animate-pulse shrink-0 border-b border-red-700 z-50"
            id="sos-banner-alert"
          >
            <span className="material-icons text-sm">gpp_maybe</span>
            <span className="font-bold uppercase tracking-wider">CRITICAL INCIDENT RED CONSOLE ACTIVE:</span>
            <span>{activeSOSRecords.length} HIGH-PRIORITY TRANSIT SOS TRIGGERED. DISPATCH UNITS STANDBY.</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Top Bar Navigation */}
      <header className="h-14 border-b border-slate-200 bg-white flex items-center justify-between px-4 sm:px-6 shrink-0 z-40 text-slate-850" id="main-header">
        <div className="flex items-center space-x-3 sm:space-x-4">
          <button 
            type="button" 
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="text-slate-500 hover:text-slate-900 transition-colors p-1 rounded hover:bg-slate-100"
            id="toggle-sidebar-btn"
          >
            <span className="material-icons text-xl">{sidebarOpen ? 'menu_open' : 'menu'}</span>
          </button>
          
          <div className="flex items-center space-x-2">
            <span className="material-icons text-blue-600 text-xl font-bold animate-pulse">radar</span>
            <span className="font-sans text-sm sm:text-base font-bold uppercase tracking-tight text-slate-800">
              SafeTravel <span className="text-blue-600 font-bold">Pro</span>
            </span>
            <span className="bg-blue-50 text-blue-600 border border-blue-200 text-[9.5px] uppercase px-1.5 py-0.5 rounded font-mono">
              MONITOR v2.1
            </span>
          </div>
        </div>

        {/* Global telemetry metadata & Real-Time Sync Console */}
        <div className="flex items-center space-x-2 sm:space-x-4 text-xs font-mono text-slate-600">
          <div className="hidden md:flex flex-col text-right">
            <span className="text-slate-400 text-[9.5px] uppercase tracking-wider">Control Operator</span>
            <span className="text-blue-600 font-medium font-sans font-semibold text-xs">{currentUser?.email}</span>
          </div>

          <div className="h-5 w-px bg-slate-200 hidden md:block"></div>

          {/* Real-time Cloud Status with Last Sync Time */}
          <div 
            className="flex items-center space-x-2 bg-slate-100 border border-slate-200 px-2.5 py-1 rounded-lg text-slate-700 select-none"
            title={lastSyncTime ? `Last operational sync: ${lastSyncTime.toLocaleTimeString()}` : 'Connecting real-time channel...'}
          >
            <span className={`h-2 w-2 rounded-full shrink-0 ${
              realtimeStatus === 'connected' 
                ? 'bg-emerald-500 animate-pulse' 
                : realtimeStatus === 'connecting' 
                ? 'bg-amber-500 animate-pulse' 
                : 'bg-red-500'
            }`}></span>
            <div className="flex flex-col leading-none">
              <span className="text-[9.5px] uppercase font-bold tracking-wider text-slate-800">
                {hasSupabaseConfig() ? (realtimeStatus === 'connected' ? 'LIVE SYNC' : 'RECONNECTING') : 'LOCAL CACHE'}
              </span>
              <span className="text-[8px] text-slate-500 font-mono mt-0.5">
                {isSyncing ? 'Syncing...' : (lastSyncTime ? `${lastSyncTime.toLocaleTimeString()}` : 'Ready')}
              </span>
            </div>
          </div>

          {/* Instant 1-Click Refresh Button */}
          <button
            type="button"
            onClick={() => fetchOperationalData(false)}
            disabled={isSyncing}
            className={`p-1.5 rounded-lg border transition-all flex items-center justify-center cursor-pointer ${
              isSyncing 
                ? 'bg-blue-50 border-blue-200 text-blue-600 shadow-sm' 
                : 'bg-white border-slate-200 text-slate-600 hover:text-blue-600 hover:bg-slate-50 hover:border-slate-300'
            } active:scale-95`}
            title="Instant Refresh: Click to immediately sync all data without reloading the page"
          >
            <span className={`material-icons text-base ${isSyncing ? 'animate-spin text-blue-600' : ''}`}>
              sync
            </span>
          </button>
        </div>
      </header>

      {/* Core Layout Canvas */}
      <div className="flex-1 flex overflow-hidden min-h-0">
        
        {/* SIDE NAVIGATION PANEL */}
        {sidebarOpen && (
          <div 
            onClick={() => setSidebarOpen(false)}
            className="fixed inset-0 bg-slate-950/60 z-40 md:hidden transition-opacity duration-300"
          ></div>
        )}

        <nav className={`bg-slate-900 border-r border-slate-800 transition-all duration-300 flex flex-col justify-between shrink-0
          fixed inset-y-0 left-0 z-50 h-full shadow-2xl md:relative md:h-full md:shadow-none md:translate-x-0
          ${sidebarOpen ? 'w-60 translate-x-0' : 'w-0 -translate-x-full md:w-16 md:-translate-x-0 overflow-hidden md:overflow-visible'}`} id="side-nav">
          <div className="py-4 flex-1 overflow-y-auto">
            
            {/* System user profile summary */}
            {sidebarOpen && (
              <div className="mx-4 mb-4 p-3 bg-slate-800/45 rounded-xl border border-slate-800" id="user-info-card">
                <div className="flex items-center justify-between mb-3 border-b border-slate-800 pb-2">
                  <span className="text-[9px] font-mono text-slate-500 font-bold uppercase tracking-wider">Session Active</span>
                  <button 
                    type="button" 
                    onClick={handleLogout}
                    title="Log Out"
                    className="text-red-400 hover:text-red-300 focus:outline-none flex items-center justify-center space-x-1 bg-red-500/10 hover:bg-red-500/20 px-2 py-0.5 rounded text-[9px] transition-all cursor-pointer font-bold border border-red-500/20"
                  >
                    <span className="material-icons text-xs">logout</span>
                    <span>LOGOUT</span>
                  </button>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="w-8 h-8 bg-blue-500 rounded-lg flex items-center justify-center font-bold text-white shrink-0 text-sm uppercase">
                    {currentUser?.name?.charAt(0) || 'U'}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-semibold text-white truncate leading-none">{currentUser?.name}</p>
                    <p className="text-[10px] text-blue-400 truncate mt-1 leading-none uppercase tracking-wide font-semibold">
                      {currentUser?.role === 'admin' ? 'SYSTEM ADMIN' : 'DISPATCH AGENT'}
                    </p>
                  </div>
                </div>
                <div className="flex items-center space-x-1.5 mt-2.5">
                  <span className="h-1.5 w-1.5 bg-green-500 rounded-full animate-ping"></span>
                  <p className="text-[9px] text-green-400 font-mono tracking-wide leading-none uppercase">GPS TRACKING ACTIVE</p>
                </div>
              </div>
            )}
            <div className="space-y-4 px-2">
              
              {/* SECTION 1: OPERATIONS CONSOLE */}
              <div>
                {sidebarOpen && (
                  <p className="px-3 py-1.5 text-[9.5px] font-mono font-bold text-slate-500 uppercase tracking-widest select-none">
                    Operations
                  </p>
                )}
                <ul className="space-y-1">
                  {[
                    { id: 'dashboard', label: 'Dashboard', icon: 'space_dashboard' },
                    { id: 'monitor', label: 'Live Map & Tracking', icon: 'map' },
                    { id: 'sos', label: 'SOS Emergency Alerts', icon: 'security', isDanger: true },
                    { id: 'trips', label: 'Vehicle & Driver Records', icon: 'directions_car' },
                    { id: 'audio', label: 'Emergency Audio Records', icon: 'graphic_eq' },
                    { id: 'users', label: 'Registered Travelers', icon: 'people' },
                    { id: 'plans', label: 'Subscription Plans', icon: 'workspace_premium', badge: pendingPaymentsCount },
                  ].map(item => {
                    const isActive = activeTab === item.id;
                    const isSOSAlert = item.id === 'sos' && activeSOSRecords.length > 0;
                    const badgeNum = typeof (item as any).badge === 'number' ? (item as any).badge : 0;
                    const hasBadge = badgeNum > 0;
                    
                    return (
                      <li key={item.id}>
                        <button
                          type="button"
                          onClick={() => {
                            setActiveTab(item.id as any);
                            if (window.innerWidth < 768) {
                              setSidebarOpen(false);
                            }
                          }}
                          className={`w-full flex items-center justify-start rounded-lg transition-all px-3 py-2.5 text-xs font-medium relative group cursor-pointer ${
                            isActive 
                              ? 'bg-blue-600 text-white font-semibold' 
                              : 'text-slate-400 hover:text-white hover:bg-slate-800/80'
                          }`}
                        >
                          <span className={`material-icons mr-3 transition-colors ${
                            isActive ? 'text-white' : 'text-slate-400 group-hover:text-blue-400'
                          } ${isSOSAlert ? 'text-red-500 animate-bounce' : ''}`}>
                            {item.icon}
                          </span>
                          
                          {sidebarOpen && (
                            <span className="truncate">{item.label}</span>
                          )}

                          {isSOSAlert && sidebarOpen && (
                            <span className="absolute right-3 bg-red-600 text-white text-[9px] px-1.5 py-0.5 rounded font-bold animate-pulse">
                              {activeSOSRecords.length}
                            </span>
                          )}

                          {hasBadge && sidebarOpen && !isSOSAlert ? (
                            <span className="absolute right-3 bg-amber-500 text-slate-950 text-[9px] px-1.5 py-0.5 rounded-full font-black animate-pulse">
                              {badgeNum}
                            </span>
                          ) : null}

                          {!sidebarOpen && (
                            <div className="absolute left-14 bg-slate-900 border border-slate-705 text-white text-[10px] px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity font-sans font-normal whitespace-nowrap z-50 shadow-md">
                              {item.label}
                            </div>
                          )}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              </div>

              {/* SECTION 2: MONETIZATION & ADS */}
              <div>
                {sidebarOpen && (
                  <p className="px-3 py-1.5 text-[9.5px] font-mono font-bold text-slate-500 uppercase tracking-widest select-none border-t border-slate-800/30 mt-2 pt-3">
                    Monetization
                  </p>
                )}
                <ul className="space-y-1">
                  {[
                    { id: 'ads', label: 'Ad Monetization', icon: 'campaign' },
                    { id: 'homepage', label: 'Promo Banner', icon: 'aspect_ratio' },
                  ].map(item => {
                    const isActive = activeTab === item.id;
                    
                    return (
                      <li key={item.id}>
                        <button
                          type="button"
                          onClick={() => {
                            setActiveTab(item.id as any);
                            if (window.innerWidth < 768) {
                              setSidebarOpen(false);
                            }
                          }}
                          className={`w-full flex items-center justify-start rounded-lg transition-all px-3 py-2.5 text-xs font-medium relative group cursor-pointer ${
                            isActive 
                              ? 'bg-blue-600 text-white font-semibold' 
                              : 'text-slate-400 hover:text-white hover:bg-slate-800/80'
                          }`}
                        >
                          <span className={`material-icons mr-3 transition-colors ${
                            isActive ? 'text-white' : 'text-slate-400 group-hover:text-blue-400'
                          }`}>
                            {item.icon}
                          </span>
                          
                          {sidebarOpen && (
                            <span className="truncate">{item.label}</span>
                          )}

                          {!sidebarOpen && (
                            <div className="absolute left-14 bg-slate-900 border border-slate-705 text-white text-[10px] px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity font-sans font-normal whitespace-nowrap z-50 shadow-md">
                              {item.label}
                            </div>
                          )}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              </div>

              {/* SECTION 3: SYSTEM SETUP */}
              <div>
                {sidebarOpen && (
                  <p className="px-3 py-1.5 text-[9.5px] font-mono font-bold text-slate-500 uppercase tracking-widest select-none border-t border-slate-800/30 mt-2 pt-3">
                    System Setup
                  </p>
                )}
                <ul className="space-y-1">
                  {[
                    { id: 'settings', label: 'Settings', icon: 'settings' },
                  ].map(item => {
                    const isActive = activeTab === item.id;
                    
                    return (
                      <li key={item.id}>
                        <button
                          type="button"
                          onClick={() => {
                            setActiveTab(item.id as any);
                            if (window.innerWidth < 768) {
                              setSidebarOpen(false);
                            }
                          }}
                          className={`w-full flex items-center justify-start rounded-lg transition-all px-3 py-2.5 text-xs font-medium relative group cursor-pointer ${
                            isActive 
                              ? 'bg-blue-600 text-white font-semibold' 
                              : 'text-slate-400 hover:text-white hover:bg-slate-800/80'
                          }`}
                        >
                          <span className={`material-icons mr-3 transition-colors ${
                            isActive ? 'text-white' : 'text-slate-400 group-hover:text-blue-400'
                          }`}>
                            {item.icon}
                          </span>
                          
                          {sidebarOpen && (
                            <span className="truncate">{item.label}</span>
                          )}

                          {!sidebarOpen && (
                            <div className="absolute left-14 bg-slate-900 border border-slate-705 text-white text-[10px] px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity font-sans font-normal whitespace-nowrap z-50 shadow-md">
                              {item.label}
                            </div>
                          )}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              </div>

            </div>
          </div>

          <div className="p-4 border-t border-slate-800">
            {sidebarOpen ? (
              <div className="bg-slate-950/40 p-2 rounded border border-slate-850 flex items-center justify-between text-[11px] font-mono text-slate-400">
                <span>SYSTEM RUNTIME:</span>
                <span className="text-blue-400 font-bold">ONLINE</span>
              </div>
            ) : (
              <div>
                <button 
                  type="button"
                  onClick={handleLogout}
                  title="Secure Session Logout"
                  className="w-full text-slate-500 hover:text-red-500 transition-colors py-1 flex items-center justify-center font-bold cursor-pointer"
                >
                  <span className="material-icons text-xl">logout</span>
                </button>
              </div>
            )}
          </div>
        </nav>

        {/* CONTENT & FOOTER COLUMN */}
        <div className="flex-1 flex flex-col min-w-0 min-h-0 overflow-hidden">

          {/* MAIN TELEMETRY VIEWS CANVAS */}
          <main className="flex-1 bg-slate-50 overflow-y-auto min-h-0 flex flex-col">
          
          <div className="p-2.5 sm:p-3.5 md:p-4 lg:p-4.5 w-full mx-auto flex-1 flex flex-col space-y-3.5">
            {/* 1. OPERATIONS DASHBOARD (EXECUTIVE OVERVIEW) */}
            {activeTab === 'dashboard' && (
              <ExecutiveOverviewSection
                db={db}
                activeSOSRecords={activeSOSRecords}
                activeTripsList={activeTripsList}
                setActiveTab={setActiveTab}
                formatDateTime={formatDateTime}
                onInspectTrip={(tripId) => setInspectedTripId(tripId)}
              />
            )}

            {/* 2. MONITOR CONSOLE TAB (LIVE SATELLITE RADAR & TELEMETRY) */}
            {activeTab === 'monitor' && (
              <DashboardSection
                db={db}
                activeSOSRecords={activeSOSRecords}
                activeTripsList={activeTripsList}
                selectedTripId={selectedTripId}
                setSelectedTripId={setSelectedTripId}
                triggerSos={triggerSos}
                resolveSos={resolveSos}
                setActiveTab={setActiveTab}
                formatTime={formatTime}
              />
            )}

            {/* 2. SOS CRISIS CENTER TAB */}
            {activeTab === 'sos' && (
              <SosAlertsSection
                db={db}
                activeSOSRecords={activeSOSRecords}
                resolveSos={resolveSos}
                formatDateTime={formatDateTime}
                onInspectTrip={(tripId) => setInspectedTripId(tripId)}
              />
            )}

            {/* 3. VEHICLE & FLEET VAULT TAB */}
            {activeTab === 'trips' && (
              <VehicleVaultSection
                db={db}
                onInspectTrip={(tripId) => setInspectedTripId(tripId)}
                formatDateTime={formatDateTime}
              />
            )}

            {/* 4. AUDIO BLACKBOX VAULT TAB */}
            {activeTab === 'audio' && (
              <AudioBlackboxSection
                db={db}
                formatDateTime={formatDateTime}
              />
            )}

            {/* 5. TRAVELER REGISTRY TAB OR USER TRAVEL HISTORY */}
            {activeTab === 'users' && (
              isUserTravelHistory && targetUserIdForTravels ? (
                <UserTravelHistorySection
                  db={db}
                  userId={targetUserIdForTravels}
                  onBack={() => router.push('/dashboard/users')}
                />
              ) : (
                <TravelerRegistrySection
                  db={db}
                  newProfileName={newProfileName}
                  setNewProfileName={setNewProfileName}
                  newProfilePhone={newProfilePhone}
                  setNewProfilePhone={setNewProfilePhone}
                  newProfileRole={newProfileRole}
                  setNewProfileRole={setNewProfileRole}
                  handleAddProfile={handleAddProfile}
                  adjustCredits={adjustCredits}
                  awardPoints={awardPoints}
                  togglePremium={togglePremium}
                />
              )
            )}

            {/* 6. SUBSCRIPTION & PREMIUM PLANS TAB */}
            {activeTab === 'plans' && (
              <SubscriptionPlansSection db={db} setDb={setDb} currentUser={currentUser} />
            )}

            {/* 7. HOMEPAGE ADMIN BANNER CUSTOMIZER TAB */}
            {activeTab === 'homepage' && (
              <PromoBannerSection db={db} setDb={setDb} currentUser={currentUser} />
            )}

            {/* 8. AD MONETIZATION & IN-APP ADVERTISING TAB */}
            {activeTab === 'ads' && (
              <AdMonetizationSection />
            )}

            {/* 9. SYSTEM SETTINGS TAB */}
            {activeTab === 'settings' && (
              <SystemSettingsSection />
            )}

            {/* Global Inspector Modal */}
            {inspectedTripId && (() => {
              const tripToInspect = db.travelActivities?.find(a => a.id === inspectedTripId) ||
                db.trips.find(t => t.id === inspectedTripId);
              if (!tripToInspect) return null;
              return (
                <TripDetailsModal
                  trip={tripToInspect as any}
                  profiles={db.profiles}
                  onClose={() => setInspectedTripId(null)}
                />
              );
            })()}


          </div>
        </main>

        {/* Master operational footer bar (FIXED AT BOTTOM) */}
        <footer className="border-t border-slate-200 py-2.5 px-6 text-center text-[10.5px] text-slate-500 bg-white shrink-0 font-mono uppercase tracking-wider z-20" id="fixed-dashboard-footer">
          Safe Travel Transit Control Console &copy; 2026 Operations Dispatch Division. Connected as {currentUser?.email || 'mdsadakkas86@gmail.com'}
        </footer>

      </div>

      </div>
    </div>
  );
}


