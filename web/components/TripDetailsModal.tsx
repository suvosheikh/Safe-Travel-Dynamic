'use client';

import React, { useState, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { TravelActivity, Trip, getSupabaseClient } from '../lib/supabase';
import { calculateRealTripDistance, formatRealTripDuration } from '../lib/TripDistanceCalculator';
import { useToast } from './ui/Toast';

const MapboxMonitor = dynamic(() => import('./MapboxMonitor'), {
  ssr: false,
  loading: () => (
    <div className="flex-1 w-full h-full min-h-[300px] bg-slate-900 flex flex-col items-center justify-center text-center">
      <div className="w-10 h-10 rounded-full bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400 animate-pulse">
        <span className="material-icons text-lg animate-spin">autorenew</span>
      </div>
      <p className="text-slate-400 text-[10px] font-mono mt-2.5 uppercase tracking-wider animate-pulse">
        Loading map view...
      </p>
    </div>
  )
});

interface TripDetailsModalProps {
  trip: TravelActivity;
  profiles: any[];
  onClose: () => void;
}

const safeParseCoords = (coordsStr: string | null | undefined): [number, number] | undefined => {
  if (!coordsStr) return undefined;
  
  const validateAndSwap = (v1: number, v2: number): [number, number] => {
    if (Math.abs(v2) > 90 || (Math.abs(v1) <= 90 && Math.abs(v2) > 80 && Math.abs(v2) <= 180)) {
      return [v2, v1]; // Swapped to [lng, lat]
    }
    return [v1, v2]; // Already [lng, lat]
  };

  try {
    const parsed = JSON.parse(coordsStr);
    if (Array.isArray(parsed) && parsed.length >= 2) {
      return validateAndSwap(Number(parsed[0]), Number(parsed[1]));
    }
  } catch (e) {
    const match = coordsStr.match(/(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)/);
    if (match) {
      return validateAndSwap(Number(match[1]), Number(match[2]));
    }
  }
  return undefined;
};

export default function TripDetailsModal({ trip, profiles, onClose }: TripDetailsModalProps) {
  const { toast } = useToast();
  const [copied, setCopied] = useState(false);
  const [activeTrip, setActiveTrip] = useState<TravelActivity>(trip);
  // Mobile tab state: on mobile (< md), toggle between Map view and Telemetry details view
  const [mobileTab, setMobileTab] = useState<'map' | 'info'>('map');
  const [lightboxImage, setLightboxImage] = useState<string | null>(null);

  // Safely parse forensic SOS activity logs if present
  const sosActivityLogs = React.useMemo(() => {
    if (!activeTrip.sos_activity_logs) return [];
    if (Array.isArray(activeTrip.sos_activity_logs)) return activeTrip.sos_activity_logs;
    try {
      const parsed = typeof activeTrip.sos_activity_logs === 'string' 
        ? JSON.parse(activeTrip.sos_activity_logs) 
        : activeTrip.sos_activity_logs;
      return Array.isArray(parsed) ? parsed : [parsed];
    } catch {
      return [];
    }
  }, [activeTrip.sos_activity_logs]);

  // Escape key listener to close modal
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // Always fetch the freshest trip activity initially, then connect via direct Realtime WebSocket (0 extra egress)
  useEffect(() => {
    setActiveTrip(trip);
    let isMounted = true;
    const supabase = getSupabaseClient();

    const fetchFresh = async () => {
      try {
        if (!supabase || !trip.id) return;
        const { data: acts, error } = await supabase
          .from('travel_activities')
          .select('*')
          .eq('id', trip.id)
          .limit(1);
        if (acts && acts.length > 0 && isMounted && !error) {
          setActiveTrip(acts[0]);
        }
      } catch (e) {
        console.warn('Could not fetch fresh trip details in modal:', e);
      }
    };
    fetchFresh();

    // Direct Realtime WebSocket subscription: updates state immediately on incoming data without any HTTP polling/refetching
    let channel: any = null;
    if (supabase && trip.id) {
      channel = supabase
        .channel(`tracking_${trip.id}`, {
          config: { broadcast: { self: false } }
        })
        .on(
          'broadcast',
          { event: 'pos' },
          (payload: any) => {
            if (!isMounted) return;
            const data = payload?.payload;
            if (!data || data.lat == null || data.lng == null) return;
            setActiveTrip(prev => ({
              ...prev,
              current_lat: data.lat,
              current_lng: data.lng,
              end_battery_level: data.battery ?? prev.end_battery_level
            }));
          }
        )
        .on(
          'postgres_changes',
          {
            event: 'UPDATE',
            schema: 'public',
            table: 'travel_activities',
            filter: `id=eq.${trip.id}`
          },
          (payload: any) => {
            if (!isMounted) return;
            const updated = payload.new as TravelActivity;
            if (updated && updated.id === trip.id) {
              // Direct payload merge with live coordinate accumulation - 0 HTTP REST queries, 0 egress waste!
              setActiveTrip(prev => {
                let mergedLog = updated.route_path_log;
                // If updated log is null or empty, preserve previous trail and append new live point
                if (!mergedLog || (Array.isArray(mergedLog) && mergedLog.length === 0)) {
                  mergedLog = prev.route_path_log;
                  if (updated.current_lat != null && updated.current_lng != null) {
                    const prevList = Array.isArray(mergedLog) ? [...mergedLog] : [];
                    mergedLog = [...prevList, { lat: updated.current_lat, lng: updated.current_lng, t: new Date().toISOString() }];
                  }
                }
                return {
                  ...prev,
                  ...updated,
                  route_path_log: mergedLog
                };
              });
            }
          }
        )
        .subscribe();
    }

    return () => {
      isMounted = false;
      if (channel && supabase) {
        supabase.removeChannel(channel);
      }
    };
  }, [trip.id]);
  
  // Format TravelActivity to match the Trip interface expected by MapboxMonitor
  const formattedTrip: Trip = {
    id: activeTrip.id,
    user_id: activeTrip.user_id || 'unknown',
    start_location: activeTrip.start_address || 'Unknown Origin',
    end_location: activeTrip.end_address || 'Unknown Destination',
    status: activeTrip.safety_status === 'sos' ? 'sos' : (activeTrip.safety_status === 'ongoing' || !activeTrip.end_time) ? 'ongoing' : 'completed',
    created_at: activeTrip.start_time || activeTrip.created_at || new Date().toISOString(),
    start_coords: safeParseCoords(activeTrip.start_coords),
    end_coords: safeParseCoords(activeTrip.end_coords),
    start_time: activeTrip.start_time || undefined,
    end_time: activeTrip.end_time || undefined,
    transport_mode: activeTrip.transport_mode,
    route_path_log: activeTrip.route_path_log,
    planned_route_log: activeTrip.planned_route_log,
    current_lat: activeTrip.current_lat,
    current_lng: activeTrip.current_lng
  };

  const isCancelled = activeTrip.safety_status === 'cancelled' || (activeTrip as any).status === 'cancelled';
  const isOngoing = !isCancelled && (activeTrip.safety_status === 'ongoing' || !activeTrip.end_time);
  const isSos = activeTrip.safety_status === 'sos';
  const trackingCode = activeTrip.tracking_code || (activeTrip.id ? activeTrip.id.split('-')[0].toUpperCase() : 'UNKNOWN');

  // Traveler info
  const traveler = profiles.find((p: any) => p.id === activeTrip.user_id);
  const travelerName = traveler?.full_name || 'Shuvo Sheikh';

  // Duration computation
  const startTime = activeTrip.start_time ? new Date(activeTrip.start_time) : null;
  const endTime = activeTrip.end_time ? new Date(activeTrip.end_time) : null;
  const durationText = formatRealTripDuration(activeTrip) || (isOngoing ? 'Ongoing' : '---');

  // Distance computation (strictly from real hardware GPS sensor or real route breadcrumbs, zero guessing)
  const realDist = calculateRealTripDistance(activeTrip);
  const distanceText = realDist > 0
    ? `${realDist.toFixed(2)} km`
    : isOngoing
    ? 'Calculating...'
    : '0.00 km';

  // Battery computation & drain calculation
  const startBat = activeTrip.start_battery_level != null ? Number(activeTrip.start_battery_level) : null;
  const endBat = activeTrip.end_battery_level != null ? Number(activeTrip.end_battery_level) : null;
  const hasBothBat = startBat !== null && endBat !== null && !isNaN(startBat) && !isNaN(endBat);
  const batDrain = hasBothBat ? (startBat - endBat) : null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/75 backdrop-blur-sm p-2 sm:p-4 md:p-5 lg:p-6 animate-in fade-in duration-200">
      
      {/* Main Modal Container (Responsive & compact for HD laptops) */}
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl xl:max-w-6xl h-[92vh] max-h-[720px] flex flex-col md:flex-row overflow-hidden border border-slate-200 relative">

        {/* Mobile Segmented Control (< md only) */}
        <div className="md:hidden flex bg-slate-100 border-b border-slate-200 p-1 shrink-0">
          <button
            onClick={() => setMobileTab('map')}
            className={`flex-1 py-1.5 text-xs font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all ${
              mobileTab === 'map' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span className="material-icons text-sm">map</span> Route Map
          </button>
          <button
            onClick={() => setMobileTab('info')}
            className={`flex-1 py-1.5 text-xs font-bold rounded-lg flex items-center justify-center gap-1.5 transition-all ${
              mobileTab === 'info' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span className="material-icons text-sm">assessment</span> Trip & Status Info
          </button>
          <button
            onClick={onClose}
            className="px-2.5 text-slate-400 hover:text-slate-700 flex items-center"
            title="Close"
          >
            <span className="material-icons text-base">close</span>
          </button>
        </div>

        {/* Left Panel: Trip Info & Telemetry */}
        <div className={`w-full md:w-[340px] lg:w-[370px] shrink-0 bg-slate-50 flex flex-col h-full border-r border-slate-200 ${
          mobileTab === 'info' ? 'flex' : 'hidden md:flex'
        }`}>
          {/* Header */}
          <div className="px-4 py-2.5 border-b border-slate-200 bg-white flex justify-between items-center shrink-0">
            <div className="flex items-center gap-2 min-w-0">
              <span className="material-icons text-blue-600 text-lg">route</span>
              <h2 className="font-bold text-slate-800 text-xs sm:text-sm truncate">Trip Details</h2>
              <span 
                className="text-[9.5px] font-mono text-slate-600 bg-slate-100 hover:bg-slate-200 px-1.5 py-0.5 rounded border border-slate-200 uppercase tracking-wider cursor-pointer transition-colors shrink-0"
                title="Click to copy tracking code"
                onClick={() => {
                  navigator.clipboard.writeText(trackingCode);
                  setCopied(true);
                  toast.info(`Tracking code #${trackingCode} copied to clipboard.`, 'Tracking Code Copied');
                  setTimeout(() => setCopied(false), 2000);
                }}
              >
                #{trackingCode}
              </span>
            </div>
            
            <button 
              onClick={onClose} 
              className="text-slate-400 hover:text-slate-700 p-1 rounded-lg hover:bg-slate-100 transition-colors hidden md:block"
              title="Close modal (Esc)"
            >
              <span className="material-icons text-lg leading-none">close</span>
            </button>
          </div>

          {/* Scrollable Telemetry Details */}
          <div className="px-3.5 py-3 overflow-y-auto flex-1 space-y-3 text-slate-700">
            
            {/* Status & Traveler Row */}
            <div className="flex justify-between items-center bg-white p-2.5 rounded-xl border border-slate-200 shadow-2xs">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-xs shrink-0">
                  {travelerName.charAt(0)}
                </div>
                <div>
                  <span className="block font-bold text-xs text-slate-900 leading-tight">{travelerName}</span>
                  <span className="text-[10px] text-slate-400 font-mono">Traveler</span>
                </div>
              </div>

              <div className="text-right">
                <span className={`inline-flex items-center gap-1 font-mono text-[10.5px] font-bold uppercase px-2 py-0.5 rounded-md ${
                  isSos 
                    ? 'bg-rose-100 text-rose-700 border border-rose-200 animate-pulse' 
                    : isCancelled
                    ? 'bg-amber-100 text-amber-700 border border-amber-200'
                    : isOngoing 
                    ? 'bg-cyan-100 text-cyan-700 border border-cyan-200' 
                    : 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                }`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${
                    isSos ? 'bg-rose-600' : isCancelled ? 'bg-amber-600' : isOngoing ? 'bg-cyan-600' : 'bg-emerald-600'
                  }`}></span>
                  {isSos ? 'SOS ALERT' : isCancelled ? 'CANCELLED' : isOngoing ? 'ON TRIP' : 'COMPLETED'}
                </span>
                <span className="block text-[10px] text-slate-500 font-sans capitalize mt-0.5">
                  Mode: {activeTrip.transport_mode || 'Transit'}
                </span>
              </div>
            </div>

            {/* Key Metrics 4-Card Grid */}
            <div className="grid grid-cols-2 gap-1.5 text-xs">
              <div className="bg-white p-2 rounded-lg border border-slate-200">
                <span className="block text-[9px] uppercase font-mono text-slate-400 flex items-center gap-1">
                  <span className="material-icons text-[11px] text-blue-500">schedule</span> Duration
                </span>
                <span className="font-bold text-xs text-slate-800 block mt-0.5">
                  {durationText}
                </span>
              </div>

              <div className="bg-white p-2 rounded-lg border border-slate-200">
                <span className="block text-[9px] uppercase font-mono text-slate-400 flex items-center gap-1">
                  <span className="material-icons text-[11px] text-emerald-500">straighten</span> Distance
                </span>
                <span className="font-bold text-xs text-slate-800 block mt-0.5">
                  {distanceText}
                </span>
              </div>

              <div className="bg-white p-2 rounded-lg border border-slate-200">
                <span className="block text-[9px] uppercase font-mono text-slate-400 flex items-center gap-1">
                  <span className="material-icons text-[11px] text-amber-500">battery_charging_full</span> Battery
                </span>
                <div className="flex items-center gap-1.5 flex-wrap mt-0.5">
                  <span className="font-mono text-[11px] font-semibold text-slate-800">
                    {startBat !== null ? `${startBat}%` : 'N/A'}
                    {endBat !== null ? ` → ${endBat}%` : ''}
                  </span>
                  {batDrain !== null && (
                    <span 
                      className={`text-[9.5px] font-mono font-bold px-1.5 py-0.5 rounded border leading-none shrink-0 ${
                        batDrain > 0 
                          ? 'bg-amber-50 text-amber-700 border-amber-200' 
                          : batDrain < 0 
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-200' 
                          : 'bg-slate-100 text-slate-600 border-slate-200'
                      }`}
                      title={batDrain > 0 ? `Total drain: ${batDrain}%` : batDrain < 0 ? `Total charged: ${Math.abs(batDrain)}%` : 'No battery drain'}
                    >
                      {batDrain > 0 ? `-${batDrain}% drain` : batDrain < 0 ? `+${Math.abs(batDrain)}%` : '0% drain'}
                    </span>
                  )}
                </div>
              </div>

              <div className="bg-white p-2 rounded-lg border border-slate-200">
                <span className="block text-[9px] uppercase font-mono text-slate-400 flex items-center gap-1">
                  <span className="material-icons text-[11px] text-indigo-500">smartphone</span> Device
                </span>
                <span className="font-mono text-[11px] font-semibold text-slate-800 truncate block mt-0.5">
                  {activeTrip.device_model || 'IV2201'}
                </span>
              </div>
            </div>

            {/* Route Timeline */}
            <div className="bg-white p-2.5 rounded-xl border border-slate-200 shadow-2xs space-y-2">
              <h3 className="font-mono text-[10px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-100 pb-1 flex items-center gap-1">
                <span className="material-icons text-xs text-blue-500">alt_route</span> Journey Path
              </h3>
              
              <div className="relative pl-3.5 border-l-2 border-slate-200 space-y-2 ml-1 text-xs">
                <div className="relative">
                  <div className="absolute w-2 h-2 bg-blue-500 rounded-full -left-[19px] top-1 ring-2 ring-white"></div>
                  <span className="block text-[9.5px] font-mono text-slate-400">
                    START &bull; {startTime ? startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Origin'}
                  </span>
                  <span className="block font-semibold text-slate-800 leading-snug mt-0.5 text-xs">
                    {activeTrip.start_address || 'Kazipara Madrasha Road, Mirpur, Dhaka'}
                  </span>
                </div>
                
                <div className="relative">
                  <div className="absolute w-2 h-2 bg-emerald-500 rounded-full -left-[19px] top-1 ring-2 ring-white"></div>
                  <span className="block text-[9.5px] font-mono text-slate-400">
                    DESTINATION &bull; {endTime ? endTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : (isCancelled ? 'Cancelled' : isOngoing ? 'In Transit' : 'Completed')}
                  </span>
                  <span className="block font-semibold text-slate-800 leading-snug mt-0.5 text-xs">
                    {activeTrip.end_address || '1100, Kafrul Thana, Dhaka, Dhaka'}
                  </span>
                </div>
              </div>
            </div>

            {/* Route Map Legend */}
            <div className="bg-slate-100/80 p-2 rounded-lg border border-slate-200 text-[10px] font-mono text-slate-600 flex items-center justify-around">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-1 bg-blue-500 rounded-full"></span>
                <span>Planned Route</span>
              </span>
              <span className="text-slate-300">|</span>
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-1 bg-emerald-500 rounded-full"></span>
                <span>Traveled Trail</span>
              </span>
            </div>

            {/* Vehicle & Verification Card */}
            {(activeTrip.vehicle_plate_number || activeTrip.vehicle_photo_url || activeTrip.vehicle_description) && (
              <div className="bg-white p-2.5 rounded-xl border border-slate-200 shadow-2xs space-y-2">
                <div className="flex items-center justify-between border-b border-slate-100 pb-1">
                  <h3 className="font-mono text-[10px] font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1">
                    <span className="material-icons text-xs text-cyan-600">directions_car</span>
                    Vehicle Verification
                  </h3>
                  {activeTrip.vehicle_plate_number && (
                    <span className="bg-slate-900 text-yellow-400 font-mono font-bold text-[10px] px-2 py-0.5 rounded border border-slate-700">
                      {activeTrip.vehicle_plate_number}
                    </span>
                  )}
                </div>

                {activeTrip.vehicle_photo_url && (
                  <div className="relative rounded-lg overflow-hidden border border-slate-200 bg-slate-950 aspect-video group">
                    <img 
                      src={activeTrip.vehicle_photo_url} 
                      alt="Vehicle Verification Evidence" 
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    <button
                      type="button"
                      onClick={() => setLightboxImage(activeTrip.vehicle_photo_url || null)}
                      className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1 text-white font-mono text-[11px] font-bold cursor-pointer"
                    >
                      <span className="material-icons text-base">zoom_in</span>
                      INSPECT PHOTO
                    </button>
                  </div>
                )}

                <div className="space-y-1 text-xs">
                  {activeTrip.vehicle_description && (
                    <div>
                      <span className="text-[9px] font-mono text-slate-400 uppercase block">Model / Description</span>
                      <p className="font-medium text-slate-800 text-[11.5px] leading-tight">{activeTrip.vehicle_description}</p>
                    </div>
                  )}
                  {activeTrip.driver_name_manual && (
                    <div className="pt-1 border-t border-slate-100">
                      <span className="text-[9px] font-mono text-slate-400 uppercase block">Driver</span>
                      <p className="font-medium text-slate-700 text-[11px]">{activeTrip.driver_name_manual}</p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Audio Blackbox Recording Player */}
            {activeTrip.audio_clip_url && (
              <div className="bg-amber-50/60 p-2.5 rounded-xl border border-amber-200/80 shadow-2xs space-y-2">
                <div className="flex items-center justify-between border-b border-amber-200/60 pb-1">
                  <h3 className="font-mono text-[10px] font-bold uppercase tracking-wider text-amber-800 flex items-center gap-1">
                    <span className="material-icons text-xs text-amber-600">graphic_eq</span>
                    Emergency Audio Recording
                  </h3>
                  <span className="text-[9px] font-mono font-bold bg-amber-200/70 text-amber-900 px-1.5 py-0.5 rounded uppercase">
                    SAVED AUDIO
                  </span>
                </div>

                <div className="bg-slate-900 p-2 rounded-lg">
                  <audio 
                    controls 
                    className="w-full h-7"
                    src={activeTrip.audio_clip_url}
                  >
                    Your browser does not support audio element.
                  </audio>
                </div>

                <div className="flex justify-between items-center text-[9.5px] font-mono text-amber-900/80 pt-0.5">
                  <span>Saved in Cloud Storage</span>
                  <a 
                    href={activeTrip.audio_clip_url}
                    target="_blank"
                    rel="noreferrer"
                    download
                    className="font-bold underline hover:text-amber-950 flex items-center gap-0.5"
                  >
                    <span className="material-icons text-[11px]">download</span>
                    Download
                  </a>
                </div>
              </div>
            )}

            {/* Emergency SOS Timeline Card */}
            {(isSos || sosActivityLogs.length > 0 || activeTrip.sos_triggered_at) && (
              <div className="bg-rose-50/60 p-2.5 rounded-xl border border-rose-200 shadow-2xs space-y-2">
                <div className="flex items-center justify-between border-b border-rose-200 pb-1">
                  <h3 className="font-mono text-[10px] font-bold uppercase tracking-wider text-rose-800 flex items-center gap-1">
                    <span className="material-icons text-xs text-rose-600">security</span>
                    Emergency SOS Timeline
                  </h3>
                  <span className="text-[9px] font-mono font-bold bg-rose-200 text-rose-900 px-1.5 py-0.5 rounded uppercase">
                    {isSos ? 'EMERGENCY ACTIVE' : 'RESOLVED'}
                  </span>
                </div>

                {sosActivityLogs.length === 0 ? (
                  <div className="text-[11px] text-rose-700 space-y-1">
                    <p className="font-semibold">Emergency SOS alert triggered.</p>
                    <p className="text-[9.5px] font-mono text-rose-600">
                      Triggered at: {activeTrip.sos_triggered_at ? new Date(activeTrip.sos_triggered_at).toLocaleTimeString() : 'Trip active'}
                    </p>
                  </div>
                ) : (
                  <div className="relative pl-3 border-l-2 border-rose-300 space-y-2 ml-1 text-xs">
                    {sosActivityLogs.map((log: any, idx: number) => {
                      const logTime = log.timestamp ? new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : `T+${idx}`;
                      return (
                        <div key={idx} className="relative">
                          <div className="absolute w-2 h-2 bg-rose-500 rounded-full -left-[17px] top-1 ring-2 ring-white"></div>
                          <span className="block text-[9px] font-mono text-rose-600 font-bold uppercase">
                            {logTime} &bull; {log.event || 'SOS EVENT'}
                          </span>
                          <span className="block font-medium text-slate-800 text-[11px] leading-snug">
                            {log.details || log.source || (log.response ? `Safety Response: ${log.response}` : 'Alert logged by traveler device')}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}

          </div>
          
          {/* Footer Action Buttons */}
          <div className="p-2.5 border-t border-slate-200 bg-white space-y-1.5 shrink-0">
            <div className="flex gap-1.5">
              <button 
                onClick={() => {
                  navigator.clipboard.writeText(`${window.location.origin}/track/${trackingCode}`);
                  setCopied(true);
                  toast.success('Live trip tracking link copied to clipboard.', 'Link Copied');
                  setTimeout(() => setCopied(false), 2000);
                }} 
                className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white font-mono text-[10px] font-bold uppercase tracking-wider py-2 rounded-lg transition-all flex justify-center items-center gap-1.5 shadow-sm"
              >
                <span className="material-icons text-sm">content_copy</span> Copy Tracking Link
              </button>
              
              <a 
                href={`/track/${trackingCode}`}
                target="_blank"
                rel="noreferrer"
                className="bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 py-2 px-2.5 rounded-lg transition-all flex justify-center items-center"
                title="Open Public Tracking Page in new tab"
              >
                <span className="material-icons text-base">open_in_new</span>
              </a>
            </div>

            <button 
              onClick={onClose} 
              className="w-full bg-slate-800 hover:bg-slate-900 text-white font-mono text-[10.5px] font-bold uppercase tracking-wider py-1.5 rounded-lg transition-all"
            >
              Close Details
            </button>
          </div>
        </div>

        {/* Right Panel: Mapbox Monitor Engine (Clean, unobstructed map view with isModalMode) */}
        <div className={`w-full md:w-auto flex-1 h-full relative bg-slate-950 flex flex-col ${
          mobileTab === 'map' ? 'flex' : 'hidden md:flex'
        }`}>
          <MapboxMonitor 
            activeTripsList={[formattedTrip]} 
            profiles={profiles} 
            selectedTripId={formattedTrip.id} 
            onSelectTrip={() => {}} 
            isModalMode={true}
          />
          
        </div>

      </div>

      {/* Lightbox Modal */}
      {lightboxImage && (
        <div 
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn"
          onClick={() => setLightboxImage(null)}
        >
          <div 
            className="bg-slate-900 border border-slate-800 rounded-3xl max-w-2xl w-full overflow-hidden shadow-2xl relative"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-3 border-b border-slate-800 flex items-center justify-between text-white">
              <span className="font-mono text-xs text-cyan-400 font-bold uppercase flex items-center gap-1.5">
                <span className="material-icons text-sm">verified</span>
                Vehicle Verification Evidence
              </span>
              <button
                type="button"
                onClick={() => setLightboxImage(null)}
                className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors"
              >
                <span className="material-icons text-base">close</span>
              </button>
            </div>
            <div className="p-2 bg-black flex items-center justify-center max-h-[70vh]">
              <img 
                src={lightboxImage} 
                alt="Vehicle Evidence High-Res" 
                className="max-h-[65vh] w-auto object-contain rounded-lg"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
