'use client';

import React, { useEffect, useState } from 'react';
import dynamic from 'next/dynamic';
import { useParams } from 'next/navigation';
import { getLocalDatabase, Trip, Profile, TravelActivity, getSupabaseClient } from '@/lib/supabase';
import Link from 'next/link';

const MapboxMonitor = dynamic(() => import('@/components/MapboxMonitor'), { ssr: false });

export default function GuardianTrackingPage() {
  const params = useParams();
  const tripId = params.id as string;

  const [trip, setTrip] = useState<Trip | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [activity, setActivity] = useState<TravelActivity | null>(null);
  const [loading, setLoading] = useState(true);
  const [isSos, setIsSos] = useState(false);

  // Frontend protection: Disable inspect element shortcuts and context menu on public tracking page
  useEffect(() => {
    const handleContextMenu = (e: MouseEvent) => {
      e.preventDefault();
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      // Block F12 (DevTools)
      if (e.key === 'F12' || e.keyCode === 123) {
        e.preventDefault();
        return;
      }

      const isCtrlOrCmd = e.ctrlKey || e.metaKey;

      if (isCtrlOrCmd) {
        // Block Ctrl+Shift+I (Inspect), Ctrl+Shift+J (Console), Ctrl+Shift+C (Element Picker)
        if (e.shiftKey && ['I', 'i', 'J', 'j', 'C', 'c'].includes(e.key)) {
          e.preventDefault();
          return;
        }

        // Block Ctrl+U (View Page Source), Ctrl+S (Save Page)
        if (['U', 'u', 'S', 's'].includes(e.key)) {
          e.preventDefault();
          return;
        }
      }
    };

    document.addEventListener('contextmenu', handleContextMenu);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('contextmenu', handleContextMenu);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  // Poll database & listen to Realtime for instant updates (Supabase live first, with local DB fallback)
  useEffect(() => {
    let isMounted = true;
    const cleanTripId = tripId ? (tripId as string).trim() : '';

    const fetchData = async () => {
      try {
        const supabase = getSupabaseClient();
        let foundActivity: TravelActivity | null = null;
        let foundProfile: Profile | null = null;
        let activeSos = false;

        if (supabase && cleanTripId) {
          try {
            const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(cleanTripId);
            let query = supabase.from('travel_activities').select('*');
            if (isUuid) {
              query = query.or(`id.eq.${cleanTripId},tracking_code.eq.${cleanTripId}`);
            } else {
              query = query.eq('tracking_code', cleanTripId);
            }
            const { data: acts, error } = await query.order('created_at', { ascending: false }).limit(1);
            if (acts && acts.length > 0 && !error) {
              const act = acts[0];
              foundActivity = act;
              if (act.user_id) {
                try {
                  const { data: prof } = await supabase.from('profiles').select('*').eq('id', act.user_id).maybeSingle();
                  if (prof) foundProfile = prof;
                } catch {
                  // RLS might block anon read from profiles, which is expected for public guardians
                }
              }
              try {
                const { data: sos } = await supabase.from('sos_records').select('*').eq('trip_id', act.id).eq('status', 'active').maybeSingle();
                if (sos) activeSos = true;
              } catch {
                // Ignore SOS query error if any
              }
            }
          } catch (err) {
            console.warn('Supabase fetch failed in GuardianTrackingPage, falling back to local DB', err);
          }
        }

        if (!foundActivity && cleanTripId) {
          const db = getLocalDatabase();
          foundActivity = db.travelActivities.find(a => 
            a.id === cleanTripId || 
            a.tracking_code === cleanTripId ||
            (a.id ? a.id.split('-')[0].toUpperCase() : '') === cleanTripId
          ) || null;
          if (foundActivity) {
            foundProfile = db.profiles.find(p => p.id === foundActivity?.user_id) || null;
            activeSos = db.sosRecords.some(s => s.trip_id === foundActivity?.id && s.status === 'active');
          }
        }

        if (!isMounted) return;

        if (foundActivity) {
          setActivity(foundActivity);

          const parseCoords = (c?: string | null): [number, number] | undefined => {
            if (!c) return undefined;
            const parts = c.split(',').map(n => parseFloat(n.trim()));
            if (parts.length >= 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
              return [parts[1], parts[0]]; // usually stored as "lat, lng", mapbox needs [lng, lat]
            }
            return undefined;
          };

          // Map the activity to the 'Trip' format expected by MapboxMonitor and the UI
          const mappedTrip: Trip = {
            id: foundActivity.id,
            user_id: foundActivity.user_id || 'unknown',
            start_location: foundActivity.start_address || 'Unknown Origin',
            end_location: foundActivity.end_address || 'Unknown Destination',
            status: foundActivity.safety_status === 'sos' 
              ? 'sos' 
              : (foundActivity.safety_status === 'completed' || !!foundActivity.end_time) 
              ? 'completed' 
              : 'ongoing',
            created_at: foundActivity.start_time || foundActivity.created_at || new Date().toISOString(),
            start_time: foundActivity.start_time || foundActivity.created_at || undefined,
            end_time: foundActivity.end_time || undefined,
            start_coords: parseCoords(foundActivity.start_coords),
            end_coords: parseCoords(foundActivity.end_coords),
            route_path_log: foundActivity.route_path_log,
            planned_route_log: foundActivity.planned_route_log,
            current_lat: foundActivity.current_lat,
            current_lng: foundActivity.current_lng,
            transport_mode: foundActivity.transport_mode
          };
          setTrip(mappedTrip);

          const fallbackProfile: Profile = foundProfile || {
            id: foundActivity.user_id || 'traveler',
            full_name: 'SafeTravel Traveler',
            phone_number: 'Emergency Contact Protected',
            is_premium: false,
            trip_credits: 0,
            points_balance: 0,
            role: 'user'
          };
          setProfile(fallbackProfile);

          setIsSos(activeSos || mappedTrip.status === 'sos');
        }
      } catch (err) {
        console.error('Fetch error:', err);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchData();

    // Supabase Realtime channel for 0ms instant push updates & direct payload processing
    const supabase = getSupabaseClient();
    let channel: any = null;

    if (supabase && cleanTripId) {
      try {
        channel = supabase
          .channel(`tracking_${cleanTripId}`, {
            config: { broadcast: { self: false } }
          })
          .on(
            'broadcast',
            { event: 'pos' },
            (payload: any) => {
              if (!isMounted) return;
              const data = payload?.payload;
              if (!data || data.lat == null || data.lng == null) return;
              setTrip(prev => {
                if (!prev) return prev;
                return {
                  ...prev,
                  current_lat: data.lat,
                  current_lng: data.lng,
                };
              });
              if (data.battery != null) {
                setActivity(prev => prev ? { ...prev, end_battery_level: data.battery } : prev);
              }
            }
          )
          .on(
            'postgres_changes',
            {
              event: '*',
              schema: 'public',
              table: 'travel_activities'
            },
            (payload: any) => {
              if (!isMounted) return;
              const updated = payload.new as TravelActivity;
              if (!updated) return;

              // Check if update matches this tracking activity
              const matches = 
                updated.id === cleanTripId || 
                updated.tracking_code === cleanTripId ||
                (activity && updated.id === activity.id);

              if (matches) {
                // Direct payload update (0 HTTP queries, 0 egress)
                setActivity(prev => ({ ...(prev || {}), ...updated }));
                setTrip(prev => {
                  if (!prev) return prev;
                  return {
                    ...prev,
                    current_lat: updated.current_lat ?? prev.current_lat,
                    current_lng: updated.current_lng ?? prev.current_lng,
                    status: updated.safety_status === 'sos' 
                      ? 'sos' 
                      : (updated.safety_status === 'completed' || !!updated.end_time) 
                      ? 'completed' 
                      : prev.status,
                    end_time: updated.end_time ?? prev.end_time,
                    route_path_log: updated.route_path_log ?? prev.route_path_log,
                    transport_mode: updated.transport_mode ?? prev.transport_mode,
                  };
                });

                if (updated.safety_status === 'sos') {
                  setIsSos(true);
                }
              }
            }
          )
          .on(
            'postgres_changes',
            {
              event: '*',
              schema: 'public',
              table: 'sos_records'
            },
            (payload: any) => {
              if (!isMounted) return;
              const updatedSos = payload.new;
              if (updatedSos && (updatedSos.trip_id === cleanTripId || (activity && updatedSos.trip_id === activity.id))) {
                if (updatedSos.status === 'active') {
                  setIsSos(true);
                  setTrip(prev => prev ? ({ ...prev, status: 'sos' }) : prev);
                } else if (updatedSos.status === 'resolved') {
                  setIsSos(false);
                }
              }
            }
          )
          .subscribe((status: string) => {
            // Auto-reconnect hook: when socket reconnects after network drop, sync once
            if (status === 'SUBSCRIBED' && isMounted) {
              fetchData();
            }
          });
      } catch (subErr) {
        console.warn('Realtime subscription error, continuing with fallback:', subErr);
      }
    }

    // Auto-reconnect on browser network recovery
    const handleOnline = () => {
      if (isMounted) fetchData();
    };

    // Auto-resync when browser tab returns to foreground
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && isMounted) {
        fetchData();
      }
    };

    window.addEventListener('online', handleOnline);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    // Relaxed 45s safety-net heartbeat (only active when tab is visible)
    const interval = setInterval(() => {
      if (document.visibilityState === 'visible' && isMounted) {
        fetchData();
      }
    }, 45000);

    return () => {
      isMounted = false;
      clearInterval(interval);
      window.removeEventListener('online', handleOnline);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      if (channel && supabase) {
        supabase.removeChannel(channel);
      }
    };
  }, [tripId]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center text-slate-100">
        <div className="w-12 h-12 border-4 border-emerald-500/30 border-t-emerald-500 rounded-full animate-spin mb-4"></div>
        <p className="font-mono text-xs uppercase tracking-widest text-emerald-400 animate-pulse">Establishing Secure Connection...</p>
      </div>
    );
  }

  if (!trip) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center text-slate-100 p-6 text-center">
        <span className="material-icons text-6xl text-slate-600 mb-4">link_off</span>
        <h1 className="text-2xl font-bold text-white mb-2">Tracking Link Expired or Invalid</h1>
        <p className="text-slate-400 mb-8 max-w-md">The secure tracking session you are looking for does not exist, has ended, or expired after 30 days.</p>
        <Link href="/" className="bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 px-6 rounded-xl transition-colors">
          Return to SafeTravel
        </Link>
      </div>
    );
  }

  const battery = activity?.end_battery_level || activity?.start_battery_level || 100;
  const isTripCompleted = trip.status === 'completed';
  const displayProfile: Profile = profile || {
    id: trip.user_id || 'traveler',
    full_name: 'SafeTravel Traveler',
    phone_number: 'Emergency Contact Protected',
    is_premium: false,
    trip_credits: 0,
    points_balance: 0,
    role: 'user'
  };

  return (
    <div className={`min-h-screen flex flex-col relative overflow-hidden ${isSos ? 'bg-red-950' : 'bg-slate-950'}`}>
      
      {/* MAP BACKGROUND (Real MapboxMonitor with public telemetry) */}
      <div className="absolute inset-0 z-0">
        <MapboxMonitor 
          activeTripsList={[trip]} 
          profiles={[displayProfile]} 
          selectedTripId={trip.id} 
          onSelectTrip={() => {}} 
          isPublicView={true}
          publicTelemetry={{
            battery: battery,
            phoneNumber: displayProfile.phone_number,
            lastUpdated: new Date().toLocaleTimeString(),
            isGpsLost: activity?.is_gps_lost ?? false
          }}
        />
        {/* Subtle overlay to ensure the UI on top is readable */}
        <div className={`absolute inset-0 z-10 pointer-events-none transition-colors duration-500 ${isSos ? 'bg-red-900/30' : 'bg-slate-900/10'}`}></div>
      </div>

      {/* TOP HEADER OVERLAY (pointer-events-none allows clicks to pass to Mapbox controls) */}
      <header className="fixed top-0 left-0 right-0 z-30 pointer-events-none p-3.5 md:p-5 flex justify-between items-start">
        {/* Top-Left: High-contrast Frosted Glass Card with SafeTravel Logo + Tagline + Single Unified Legend */}
        <div className="flex flex-col items-start pointer-events-auto select-none bg-slate-950/90 backdrop-blur-md border border-slate-800/90 shadow-2xl rounded-2xl p-3 sm:p-3.5 max-w-[calc(100vw-80px)] md:max-w-none">
          <div className="flex items-center space-x-2">
            <div className={`w-8 h-8 rounded-xl flex items-center justify-center ${isSos ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 shadow-[0_0_12px_rgba(16,185,129,0.25)]'}`}>
              <span className="material-icons text-lg">security</span>
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="font-bold text-lg tracking-tight text-white">Safe<span className={isSos ? 'text-red-400' : 'text-emerald-400'}>Travel</span></span>
                <span className={`px-2 py-0.5 rounded-full text-[9px] font-mono font-bold uppercase tracking-wider ${
                  isSos ? 'bg-red-600/30 text-red-300 border border-red-500/40 animate-pulse' :
                  isTripCompleted ? 'bg-slate-800 text-slate-400 border border-slate-700' :
                  'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                }`}>
                  {isSos ? 'S.O.S' : isTripCompleted ? 'Completed' : 'Live Mode'}
                </span>
              </div>
              <p className="text-[9.5px] font-mono uppercase tracking-widest text-slate-400 flex items-center gap-1">
                <span className="material-icons text-[11px] text-emerald-400">lock</span> End-to-End Encrypted
              </p>
            </div>
          </div>

          {/* Single Unified Legend Pill directly underneath Logo & Tagline */}
          <div className="mt-2.5 inline-flex flex-wrap items-center gap-2 bg-slate-900/95 border border-slate-800 px-3 py-1 rounded-full text-[10px] font-mono shadow-md">
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-[#2563EB] shadow-[0_0_6px_#2563EB]"></span>
              <span className="text-blue-300 font-medium">Start</span>
            </div>
            <span className="text-slate-700">•</span>
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-[#E11D48] shadow-[0_0_6px_#E11D48]"></span>
              <span className="text-rose-300 font-medium">Planned</span>
            </div>
            <span className="text-slate-700">•</span>
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-[#10B981] shadow-[0_0_6px_#10B981]"></span>
              <span className="text-emerald-300 font-medium">{isTripCompleted ? 'Ended' : 'Traveled'}</span>
            </div>
            {!isTripCompleted && (
              <>
                <span className="text-slate-700">•</span>
                <div className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-[#06B6D4] animate-pulse shadow-[0_0_6px_#06B6D4]"></span>
                  <span className="text-cyan-300 font-medium">Live</span>
                </div>
              </>
            )}
          </div>
        </div>

        {/* Top-Right area is kept completely free of DOM obstructions so Mapbox Zoom controls are 100% clickable */}
      </header>

      {/* S.O.S WARNING BANNER (Floating if SOS active) */}
      {isSos && (
        <div className="fixed top-24 left-4 right-4 z-50 bg-red-600/95 border-2 border-red-400 p-3.5 rounded-2xl shadow-[0_0_40px_rgba(220,38,38,0.7)] backdrop-blur-md animate-bounce text-center max-w-xl mx-auto pointer-events-auto">
          <h2 className="text-white font-black text-lg mb-0.5 flex items-center justify-center gap-2">
            <span className="material-icons text-2xl">warning</span> EMERGENCY ALERT
          </h2>
          <p className="text-red-100 font-medium text-xs">
            {displayProfile.full_name} has triggered an S.O.S alarm. Authorities and local dispatchers have been notified. Please try contacting them immediately.
          </p>
        </div>
      )}

      {/* Global CSS guarantees: zero Mapbox attribution and full clickability for zoom controls */}
      <style jsx global>{`
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

        .mapboxgl-ctrl-top-right {
          top: 14px !important;
          right: 14px !important;
          z-index: 50 !important;
          pointer-events: auto !important;
        }

        .mapboxgl-ctrl-top-right .mapboxgl-ctrl-group {
          background: rgba(15, 23, 42, 0.92) !important;
          backdrop-filter: blur(12px) !important;
          border: 1px solid rgba(51, 65, 85, 0.8) !important;
          border-radius: 14px !important;
          overflow: hidden !important;
          box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.6) !important;
          pointer-events: auto !important;
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
