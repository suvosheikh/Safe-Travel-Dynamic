'use client';

import React, { useMemo } from 'react';
import { DbState, Trip, SOSRecord } from '../../lib/supabase';

interface ExecutiveOverviewSectionProps {
  db: DbState;
  activeSOSRecords: SOSRecord[];
  activeTripsList: Trip[];
  setActiveTab: (tab: any) => void;
  formatDateTime: (isoString: string) => string;
  onInspectTrip: (tripId: string) => void;
}

interface SafetyActivityItem {
  id: string;
  tripId: string;
  type: 'sos' | 'boarding_photo' | 'audio' | 'ongoing' | 'completed';
  title: string;
  description: string;
  travelerName: string;
  timestamp: string;
  badgeText: string;
  badgeType: 'red' | 'cyan' | 'purple' | 'blue' | 'emerald';
  icon: string;
  meta?: string;
}

export default function ExecutiveOverviewSection({
  db,
  activeSOSRecords,
  activeTripsList,
  setActiveTab,
  formatDateTime,
  onInspectTrip
}: ExecutiveOverviewSectionProps) {

  // 1. High-Level Metrics Calculations (Derived strictly from real db state)
  const totalTripsCount = useMemo(() => {
    const seen = new Set<string>();
    (db.trips || []).forEach(t => { if (t.id) seen.add(t.id); });
    (db.travelActivities || []).forEach(a => { if (a.id) seen.add(a.id); });
    return seen.size;
  }, [db.trips, db.travelActivities]);
  const completedTripsCount = useMemo(() => {
    const tCount = (db.trips || []).filter(t => t.status === 'completed').length;
    const aCount = (db.travelActivities || []).filter(a => a.safety_status === 'completed' || a.end_time).length;
    return Math.max(tCount, aCount);
  }, [db.trips, db.travelActivities]);

  const activeSosCount = activeSOSRecords.length;

  const verifiedBoardingCount = useMemo(() => {
    let count = 0;
    const seen = new Set<string>();
    (db.trips || []).forEach(t => {
      if ((t.vehicle_photo_url || t.vehicle_plate_number) && !seen.has(t.id)) {
        seen.add(t.id);
        count++;
      }
    });
    (db.travelActivities || []).forEach(a => {
      if ((a.vehicle_photo_url || a.vehicle_plate_number) && !seen.has(a.id)) {
        seen.add(a.id);
        count++;
      }
    });
    return count;
  }, [db.trips, db.travelActivities]);

  const audioVaultCount = useMemo(() => {
    let count = db.safetyAudioLogs?.length || 0;
    // Also include any trips that have audio_clip_url directly
    (db.trips || []).forEach(t => {
      if (t.audio_clip_url) count++;
    });
    return count;
  }, [db.safetyAudioLogs, db.trips]);

  const totalProfilesCount = db.profiles?.length || 0;
  const premiumProfilesCount = useMemo(() => {
    return (db.profiles || []).filter(p => p.is_premium).length;
  }, [db.profiles]);

  // 2. Transport Mode Breakdown (Real data from actual trips)
  const transportDistribution = useMemo(() => {
    const counts: Record<string, number> = {
      cng: 0,
      car: 0,
      bus: 0,
      bike: 0,
      other: 0
    };

    const countMode = (modeStr?: string | null) => {
      if (!modeStr) {
        counts.other++;
        return;
      }
      const m = modeStr.toLowerCase();
      if (m.includes('cng') || m.includes('auto') || m.includes('rickshaw')) {
        counts.cng++;
      } else if (m.includes('car') || m.includes('taxi') || m.includes('uber')) {
        counts.car++;
      } else if (m.includes('bus')) {
        counts.bus++;
      } else if (m.includes('bike') || m.includes('motor') || m.includes('scooter')) {
        counts.bike++;
      } else {
        counts.other++;
      }
    };

    const seen = new Set<string>();
    (db.trips || []).forEach(t => {
      if (t.id && !seen.has(t.id)) {
        seen.add(t.id);
        countMode(t.transport_mode);
      }
    });
    (db.travelActivities || []).forEach(a => {
      if (a.id && !seen.has(a.id)) {
        seen.add(a.id);
        countMode(a.transport_mode);
      }
    });

    const total = Math.max(1, counts.cng + counts.car + counts.bus + counts.bike + counts.other);

    return [
      { key: 'cng', label: 'CNG Auto-Rickshaw', count: counts.cng, pct: Math.round((counts.cng / total) * 100), color: 'bg-emerald-500' },
      { key: 'car', label: 'Car / Ride-Share', count: counts.car, pct: Math.round((counts.car / total) * 100), color: 'bg-blue-500' },
      { key: 'bus', label: 'Public Bus', count: counts.bus, pct: Math.round((counts.bus / total) * 100), color: 'bg-amber-500' },
      { key: 'bike', label: 'Motorbike / Scooter', count: counts.bike, pct: Math.round((counts.bike / total) * 100), color: 'bg-cyan-500' },
      { key: 'other', label: 'Other Transits', count: counts.other, pct: Math.round((counts.other / total) * 100), color: 'bg-slate-500' }
    ];
  }, [db.trips, db.travelActivities]);

  // 3. Device & Battery Watchdog (Extract trips with battery status and device models)
  const deviceWatchdog = useMemo(() => {
    const lowBatteryTrips: Array<{
      tripId: string;
      travelerName: string;
      batteryLevel: number;
      deviceModel?: string;
      status: string;
    }> = [];

    const deviceBrands: Record<string, number> = {};
    const seen = new Set<string>();

    const inspectItem = (item: any) => {
      if (!item || !item.id || seen.has(item.id)) return;
      seen.add(item.id);

      const bat = item.start_battery_level;
      if (bat !== undefined && bat !== null && bat <= 20) {
        const traveler = (db.profiles || []).find(p => p.id === item.user_id);
        lowBatteryTrips.push({
          tripId: item.id,
          travelerName: traveler?.full_name || 'Unidentified Traveler',
          batteryLevel: bat,
          deviceModel: item.device_model || 'Android Handset',
          status: item.status || item.safety_status || 'ongoing'
        });
      }

      if (item.device_model) {
        const brand = item.device_model.split(' ')[0] || item.device_model;
        deviceBrands[brand] = (deviceBrands[brand] || 0) + 1;
      }
    };

    (db.trips || []).forEach(inspectItem);
    (db.travelActivities || []).forEach(inspectItem);

    const topBrands = Object.entries(deviceBrands)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 4)
      .map(([brand, count]) => ({ brand, count }));

    return {
      lowBatteryTrips: lowBatteryTrips.slice(0, 5),
      topBrands
    };
  }, [db.trips, db.travelActivities, db.profiles]);

  // 4. Chronological Realtime Activity Stream (Unified from real database events)
  const activityStream = useMemo(() => {
    const items: SafetyActivityItem[] = [];

    // A. SOS Records
    (db.sosRecords || []).forEach(r => {
      const trip = (db.trips || []).find(t => t.id === r.trip_id) || (db.travelActivities || []).find(a => a.id === r.trip_id);
      const traveler = (db.profiles || []).find(p => p.id === trip?.user_id);
      const isAct = r.status === 'active';

      items.push({
        id: `sos-${r.id}`,
        tripId: r.trip_id,
        type: 'sos',
        title: isAct ? 'Emergency SOS Alert Triggered' : 'SOS Incident Acknowledged & Resolved',
        description: `Trip #${trip?.tracking_code || r.trip_id.substring(0, 8)} • Traveler: ${traveler?.full_name || 'Passenger'}`,
        travelerName: traveler?.full_name || 'Traveler',
        timestamp: r.triggered_at || '',
        badgeText: isAct ? '[ACTIVE ALARM]' : '[RESOLVED]',
        badgeType: isAct ? 'red' : 'emerald',
        icon: 'security',
        meta: traveler?.phone_number || undefined
      });
    });

    // B. Verified Boarding Evidence (Trips with photos or plates)
    (db.trips || []).forEach(t => {
      if (t.vehicle_photo_url || t.vehicle_plate_number) {
        const traveler = (db.profiles || []).find(p => p.id === t.user_id);
        items.push({
          id: `photo-${t.id}`,
          tripId: t.id,
          type: 'boarding_photo',
          title: `Vehicle Boarded: ${t.vehicle_plate_number || 'Plate Logged'}`,
          description: `${traveler?.full_name || 'Traveler'} logged vehicle snapshot for ${t.transport_mode || 'transit'}.`,
          travelerName: traveler?.full_name || 'Traveler',
          timestamp: t.start_time || t.created_at || '',
          badgeText: '[BOARDING EVIDENCE]',
          badgeType: 'cyan',
          icon: 'directions_car',
          meta: t.vehicle_plate_number || undefined
        });
      }
    });

    // C. Audio Blackbox Uploads
    (db.safetyAudioLogs || []).forEach(log => {
      const traveler = (db.profiles || []).find(p => p.id === log.user_id);
      items.push({
        id: `audio-${log.id}`,
        tripId: log.trip_id || log.id,
        type: 'audio',
        title: 'Safety Audio Recording Saved',
        description: `15s encrypted recording captured • Trigger: ${log.source_trigger || 'Periodic Safety'}`,
        travelerName: traveler?.full_name || 'Traveler',
        timestamp: log.created_at || '',
        badgeText: '[AUDIO RECORDED]',
        badgeType: 'purple',
        icon: 'graphic_eq',
        meta: `${log.duration_sec || 15}s`
      });
    });

    // D. Ongoing and Completed Trips
    (db.trips || []).forEach(t => {
      const traveler = (db.profiles || []).find(p => p.id === t.user_id);
      if (t.status === 'ongoing') {
        items.push({
          id: `ongoing-${t.id}`,
          tripId: t.id,
          type: 'ongoing',
          title: `Journey In Progress (${t.transport_mode || 'Transit'})`,
          description: `From ${t.start_location || 'Origin'} to ${t.end_location || 'Destination'}`,
          travelerName: traveler?.full_name || 'Traveler',
          timestamp: t.start_time || t.created_at || '',
          badgeText: '[IN TRANSIT]',
          badgeType: 'blue',
          icon: 'navigation',
          meta: t.tracking_code || undefined
        });
      } else if (t.status === 'completed') {
        items.push({
          id: `completed-${t.id}`,
          tripId: t.id,
          type: 'completed',
          title: 'Trip Safely Completed',
          description: `${t.start_location || 'Origin'} to ${t.end_location || 'Destination'} • Safe Arrival`,
          travelerName: traveler?.full_name || 'Traveler',
          timestamp: t.end_time || t.created_at || '',
          badgeText: '[SAFE ARRIVAL]',
          badgeType: 'emerald',
          icon: 'check_circle',
          meta: t.total_distance ? `${t.total_distance} km` : undefined
        });
      }
    });

    // Sort newest first
    return items
      .filter(item => item.timestamp)
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 10);
  }, [db.sosRecords, db.trips, db.travelActivities, db.safetyAudioLogs, db.profiles]);

  return (
    <div className="space-y-5 flex-1 flex flex-col pt-1" id="executive-dashboard-root">
      
      {/* 1. Header Bar with Operations Status */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3.5 bg-slate-900/80 border border-slate-800 p-4 rounded-2xl backdrop-blur-md">
        <div className="flex flex-col space-y-1">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
              <span className="material-icons text-lg">space_dashboard</span>
            </div>
            <h1 className="text-lg md:text-xl font-bold tracking-tight text-white flex items-center">
              Operations & Safety Dashboard
            </h1>
          </div>
          <p className="text-slate-400 text-xs pl-10.5">
            Real-time platform overview of active journeys, emergency alarms, vehicle records, and traveler safety.
          </p>
        </div>
        
        {/* Right Action: Live Telemetry Indicator & Jump to Map */}
        <div className="flex items-center gap-2.5 shrink-0 pl-10.5 md:pl-0">
          <div className="flex items-center gap-2 bg-slate-950/80 border border-slate-800 px-3 py-1.5 rounded-xl text-xs font-mono text-slate-300">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span>LIVE SYNC</span>
          </div>

          <button
            type="button"
            onClick={() => setActiveTab('monitor')}
            className="flex items-center gap-1.5 bg-cyan-600/20 hover:bg-cyan-600/30 border border-cyan-500/40 text-cyan-300 px-3 py-1.5 rounded-xl text-xs font-medium transition-all cursor-pointer hover:shadow-lg hover:shadow-cyan-950/50"
          >
            <span className="material-icons text-sm">map</span>
            <span>Live Tracking Map</span>
          </button>
        </div>
      </div>

      {/* 2. Top Metric Pulse (4 Primary KPI Cards) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5" id="dashboard-kpi-grid">
        
        {/* KPI 1: Active Safe Journeys */}
        <div 
          onClick={() => setActiveTab('monitor')}
          className="bg-slate-900/60 hover:bg-slate-900/90 border border-slate-800 hover:border-blue-500/40 p-4 rounded-2xl transition-all cursor-pointer group flex items-start space-x-3.5 shadow-sm"
        >
          <div className="w-10 h-10 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400 group-hover:scale-105 transition-transform shrink-0">
            <span className="material-icons text-xl">navigation</span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Active Trips</p>
              <span className="text-[9px] font-mono bg-blue-500/10 text-blue-400 border border-blue-500/20 px-1.5 py-0.5 rounded">
                LIVE
              </span>
            </div>
            <h2 className="text-2xl font-bold text-white font-sans mt-0.5">
              {activeTripsList.length} <span className="text-xs font-normal text-slate-400">on road</span>
            </h2>
            <p className="text-[11px] text-slate-400 mt-1 truncate">
              {completedTripsCount} journeys safely completed
            </p>
          </div>
        </div>

        {/* KPI 2: SOS Emergency Signals */}
        <div 
          onClick={() => setActiveTab('sos')}
          className={`p-4 rounded-2xl border transition-all cursor-pointer group flex items-start space-x-3.5 shadow-sm ${
            activeSosCount > 0 
              ? 'bg-red-950/40 border-red-500/60 shadow-lg shadow-red-950/50' 
              : 'bg-slate-900/60 hover:bg-slate-900/90 border-slate-800 hover:border-slate-700'
          }`}
        >
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center group-hover:scale-105 transition-transform shrink-0 ${
            activeSosCount > 0
              ? 'bg-red-500/20 text-red-400 border border-red-500/40 animate-pulse'
              : 'bg-slate-800 text-slate-400 border border-slate-700'
          }`}>
            <span className="material-icons text-xl">security</span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between">
              <p className={`text-[10px] font-bold uppercase tracking-widest ${activeSosCount > 0 ? 'text-red-400' : 'text-slate-400'}`}>
                Emergency SOS
              </p>
              <span className={`text-[9px] font-mono px-1.5 py-0.5 rounded border ${
                activeSosCount > 0 
                  ? 'bg-red-500/20 text-red-300 border-red-500/40' 
                  : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
              }`}>
                {activeSosCount > 0 ? 'ATTENTION' : 'ALL CLEAR'}
              </span>
            </div>
            <h2 className={`text-2xl font-bold font-sans mt-0.5 ${activeSosCount > 0 ? 'text-red-400' : 'text-white'}`}>
              {activeSosCount} <span className="text-xs font-normal text-slate-400">active alerts</span>
            </h2>
            <p className="text-[11px] text-slate-400 mt-1 truncate">
              {activeSosCount > 0 ? 'Immediate assistance needed' : 'Zero emergency incidents'}
            </p>
          </div>
        </div>

        {/* KPI 3: Verified Vehicle Records */}
        <div 
          onClick={() => setActiveTab('trips')}
          className="bg-slate-900/60 hover:bg-slate-900/90 border border-slate-800 hover:border-cyan-500/40 p-4 rounded-2xl transition-all cursor-pointer group flex items-start space-x-3.5 shadow-sm"
        >
          <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 group-hover:scale-105 transition-transform shrink-0">
            <span className="material-icons text-xl">directions_car</span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Vehicle & Driver Records</p>
              <span className="text-[9px] font-mono bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 px-1.5 py-0.5 rounded">
                VERIFIED
              </span>
            </div>
            <h2 className="text-2xl font-bold text-white font-sans mt-0.5">
              {verifiedBoardingCount} <span className="text-xs font-normal text-slate-400">records</span>
            </h2>
            <p className="text-[11px] text-slate-400 mt-1 truncate">
              Saved photos & license plates
            </p>
          </div>
        </div>

        {/* KPI 4: Emergency Audio Recordings */}
        <div 
          onClick={() => setActiveTab('audio')}
          className="bg-slate-900/60 hover:bg-slate-900/90 border border-slate-800 hover:border-purple-500/40 p-4 rounded-2xl transition-all cursor-pointer group flex items-start space-x-3.5 shadow-sm"
        >
          <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400 group-hover:scale-105 transition-transform shrink-0">
            <span className="material-icons text-xl">graphic_eq</span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Audio Recordings</p>
              <span className="text-[9px] font-mono bg-purple-500/10 text-purple-400 border border-purple-500/20 px-1.5 py-0.5 rounded">
                SECURED
              </span>
            </div>
            <h2 className="text-2xl font-bold text-white font-sans mt-0.5">
              {audioVaultCount} <span className="text-xs font-normal text-slate-400">recordings</span>
            </h2>
            <p className="text-[11px] text-slate-400 mt-1 truncate">
              Emergency voice recordings
            </p>
          </div>
        </div>

      </div>

      {/* 3. Middle Section: Activity Stream (Left) + Transport & Watchdog (Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        
        {/* Left Column: Recent Realtime Activity Stream (7 Cols) */}
        <div className="lg:col-span-7 bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col space-y-3.5">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <div className="flex items-center space-x-2">
              <span className="material-icons text-cyan-400 text-lg">history</span>
              <h3 className="text-sm font-semibold text-white tracking-wide">
                Recent Safety Activity Stream
              </h3>
            </div>
            <span className="text-[10px] font-mono bg-slate-800/80 text-slate-400 border border-slate-700/80 px-2 py-0.5 rounded-full">
              {activityStream.length} LIVE EVENTS
            </span>
          </div>

          {/* Timeline Feed */}
          {activityStream.length === 0 ? (
            <div className="py-12 flex flex-col items-center justify-center text-center text-slate-500">
              <span className="material-icons text-3xl mb-2 text-slate-600">hourglass_empty</span>
              <p className="text-xs font-medium">No safety activities recorded yet.</p>
              <p className="text-[11px] text-slate-600 mt-0.5">When passengers start trips or upload boarding evidence, events will stream here live.</p>
            </div>
          ) : (
            <div className="space-y-2.5 max-h-[440px] overflow-y-auto pr-1">
              {activityStream.map((item, idx) => {
                const badgeColorClasses = {
                  red: 'bg-red-500/10 text-red-400 border-red-500/20',
                  cyan: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20',
                  purple: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
                  blue: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
                  emerald: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
                }[item.badgeType];

                const iconColorClasses = {
                  red: 'text-red-400 bg-red-500/10 border-red-500/30',
                  cyan: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/30',
                  purple: 'text-purple-400 bg-purple-500/10 border-purple-500/30',
                  blue: 'text-blue-400 bg-blue-500/10 border-blue-500/30',
                  emerald: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30',
                }[item.badgeType];

                return (
                  <div
                    key={`${item.id}-${idx}`}
                    onClick={() => {
                      if (item.tripId) onInspectTrip(item.tripId);
                    }}
                    className="p-3 bg-slate-950/40 hover:bg-slate-950/80 border border-slate-800/80 hover:border-slate-700 rounded-xl transition-all flex items-start justify-between gap-3 cursor-pointer group"
                  >
                    <div className="flex items-start space-x-3 min-w-0">
                      <div className={`w-8 h-8 rounded-lg border flex items-center justify-center shrink-0 mt-0.5 ${iconColorClasses}`}>
                        <span className="material-icons text-base">{item.icon}</span>
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center space-x-2">
                          <h4 className="text-xs font-semibold text-slate-200 group-hover:text-cyan-300 transition-colors truncate">
                            {item.title}
                          </h4>
                          <span className={`text-[9px] font-mono px-1.5 py-0.2 rounded border shrink-0 ${badgeColorClasses}`}>
                            {item.badgeText}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-400 mt-0.5 line-clamp-1">
                          {item.description}
                        </p>
                        <div className="flex items-center gap-2 mt-1 text-[10px] text-slate-500 font-mono">
                          <span>{formatDateTime(item.timestamp)}</span>
                          {item.meta && (
                            <>
                              <span>•</span>
                              <span className="text-slate-400">{item.meta}</span>
                            </>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="shrink-0 flex items-center self-center text-slate-500 group-hover:text-cyan-400 transition-colors">
                      <span className="material-icons text-sm">chevron_right</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: Transport Distribution & Device Watchdog (5 Cols) */}
        <div className="lg:col-span-5 space-y-4">
          
          {/* Card A: Transport Mode Distribution */}
          <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-2.5">
              <div className="flex items-center space-x-2">
                <span className="material-icons text-blue-400 text-lg">commute</span>
                <h3 className="text-sm font-semibold text-white tracking-wide">
                  Transport Distribution
                </h3>
              </div>
              <span className="text-[10px] font-mono text-slate-400">
                ACTIVE & COMPLETED
              </span>
            </div>

            <div className="space-y-2.5">
              {transportDistribution.map(item => (
                <div key={item.key} className="space-y-1">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-300 font-medium">{item.label}</span>
                    <span className="text-slate-400 font-mono text-[11px]">
                      {item.count} trips ({item.pct}%)
                    </span>
                  </div>
                  <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                    <div 
                      className={`h-full rounded-full ${item.color} transition-all duration-500`}
                      style={{ width: `${Math.max(item.pct, item.count > 0 ? 5 : 0)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Card B: Device Battery & Health Watchdog */}
          <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-2.5">
              <div className="flex items-center space-x-2">
                <span className="material-icons text-amber-400 text-lg">battery_alert</span>
                <h3 className="text-sm font-semibold text-white tracking-wide">
                  Device Battery & Health Monitor
                </h3>
              </div>
              <span className="text-[10px] font-mono text-slate-400">
                BATTERY RISK
              </span>
            </div>

            {deviceWatchdog.lowBatteryTrips.length === 0 ? (
              <div className="p-3 bg-emerald-950/20 border border-emerald-500/20 rounded-xl flex items-center space-x-3">
                <div className="w-7 h-7 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                  <span className="material-icons text-base">battery_charging_full</span>
                </div>
                <div className="text-xs">
                  <p className="font-semibold text-emerald-300">All Devices Operating Normally</p>
                  <p className="text-[11px] text-emerald-400/80">No active trips reporting low battery (under 20%).</p>
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                {deviceWatchdog.lowBatteryTrips.map((trip, idx) => (
                  <div 
                    key={`${trip.tripId}-${idx}`}
                    onClick={() => onInspectTrip(trip.tripId)}
                    className="p-2.5 bg-amber-950/30 border border-amber-500/30 rounded-xl flex items-center justify-between cursor-pointer hover:border-amber-400 transition-colors"
                  >
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-amber-200 truncate">{trip.travelerName}</p>
                      <p className="text-[10px] text-slate-400">{trip.deviceModel}</p>
                    </div>
                    <span className="text-[10px] font-mono font-bold bg-amber-500/20 border border-amber-500/40 text-amber-300 px-2 py-0.5 rounded-full shrink-0">
                      {trip.batteryLevel}% BATTERY
                    </span>
                  </div>
                ))}
              </div>
            )}

            {/* Top Device Handsets */}
            {deviceWatchdog.topBrands.length > 0 && (
              <div className="pt-1.5 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400">
                <span className="font-mono text-[10px] uppercase text-slate-500">Active Handsets:</span>
                <div className="flex items-center gap-2">
                  {deviceWatchdog.topBrands.map(b => (
                    <span key={b.brand} className="bg-slate-800/80 px-2 py-0.5 rounded text-slate-300 font-mono text-[10px]">
                      {b.brand} ({b.count})
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

        </div>

      </div>

      {/* 4. Bottom Row: Traveler Registry & Cloud Health */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        
        {/* Card 1: Traveler Registry Snapshot */}
        <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex items-center justify-between">
          <div className="flex items-center space-x-3.5">
            <div className="w-11 h-11 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 shrink-0">
              <span className="material-icons text-2xl">people</span>
            </div>
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Traveler Network</p>
              <h4 className="text-lg font-bold text-white mt-0.5">
                {totalProfilesCount} Registered Passengers
              </h4>
              <p className="text-[11px] text-cyan-400">
                {premiumProfilesCount} Premium Subscribers Active
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setActiveTab('users')}
            className="bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 px-3 py-1.5 rounded-xl text-xs font-medium transition-colors cursor-pointer shrink-0"
          >
            Manage Profiles
          </button>
        </div>

        {/* Card 2: Cloud Infrastructure Health */}
        <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex items-center justify-between">
          <div className="flex items-center space-x-3.5">
            <div className="w-11 h-11 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 shrink-0">
              <span className="material-icons text-2xl">cloud_done</span>
            </div>
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Cloud Services</p>
              <h4 className="text-lg font-bold text-white mt-0.5 flex items-center gap-2">
                Supabase & Cloudinary
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              </h4>
              <p className="text-[11px] text-emerald-400">
                Realtime Updates & Media Storage Active
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setActiveTab('settings')}
            className="bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 px-3 py-1.5 rounded-xl text-xs font-medium transition-colors cursor-pointer shrink-0"
          >
            View Configs
          </button>
        </div>

      </div>

    </div>
  );
}
