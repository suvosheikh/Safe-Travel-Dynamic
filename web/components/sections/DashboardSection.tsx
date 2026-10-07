'use client';

import React from 'react';
import dynamic from 'next/dynamic';
import { DbState, Trip, SOSRecord } from '../../lib/supabase';

const MapboxMonitor = dynamic(() => import('../MapboxMonitor'), {
  ssr: false,
  loading: () => (
    <div className="flex-1 w-full h-full min-h-[350px] bg-slate-900 rounded-xl border border-slate-800 p-6 flex flex-col items-center justify-center text-center">
      <div className="w-12 h-12 rounded-full bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400 animate-pulse">
        <span className="material-icons text-xl animate-spin">autorenew</span>
      </div>
      <p className="text-slate-400 text-[10.5px] font-mono mt-3 uppercase tracking-wider animate-pulse">Loading live tracking map...</p>
    </div>
  )
});

interface DashboardSectionProps {
  db: DbState;
  activeSOSRecords: SOSRecord[];
  activeTripsList: Trip[];
  selectedTripId: string | null;
  setSelectedTripId: (id: string | null) => void;
  triggerSos: (tripId: string) => void;
  resolveSos: (sosRecordId: string) => void;
  setActiveTab: (tab: any) => void;
  formatTime: (isoString: string) => string;
}

export default function DashboardSection({
  db,
  activeSOSRecords,
  activeTripsList,
  selectedTripId,
  setSelectedTripId,
  triggerSos,
  resolveSos,
  setActiveTab,
  formatTime,
}: DashboardSectionProps) {
  
  const [isMaximized, setIsMaximized] = React.useState(false);
  
  const totalProfilesCount = db.profiles.length;
  const premiumProfilesCount = db.profiles.filter(p => p.is_premium).length;
  const totalTripCredits = db.profiles.reduce((sum, p) => sum + p.trip_credits, 0);
  const estimatedRevenue = (premiumProfilesCount * 29) + (totalTripCredits * 12);

  const isMapVisible = Boolean(selectedTripId && activeTripsList.some(t => t.id === selectedTripId));
  const selectedTrip = isMapVisible
    ? (activeTripsList.find(t => t.id === selectedTripId) || db.trips.find(t => t.id === selectedTripId))
    : null;
  const selectedTripUser = selectedTrip ? db.profiles.find(p => p.id === selectedTrip.user_id) : null;
  const selectedTripSOS = selectedTrip ? db.sosRecords.find(s => s.trip_id === selectedTrip.id && s.status === 'active') : null;

  return (
    <div className="space-y-6 flex-1 flex flex-col pt-2" id="dashboard-section-root">
      
      {/* Visual state headers */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4" id="dashboard-header-container">
        <div className="flex flex-col space-y-1">
          <h1 className="text-xl font-bold tracking-tight text-slate-800 flex items-center" id="dashboard-main-heading">
            <span className="material-icons text-blue-600 mr-2 text-2xl">safety_check</span>
            Live Trip & Safety Monitor
          </h1>
          <p className="text-slate-500 text-xs" id="dashboard-subheading">
            Real-time GPS tracking and trip status of registered travelers.
          </p>
        </div>
        
        {/* Fill the right-side gap with a status badge */}
        <div className="flex items-center gap-3 bg-white border border-slate-200 px-4 py-2 rounded-xl shadow-sm text-xs font-mono text-slate-600">
           <span className="relative flex h-3 w-3">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
          </span>
          SYSTEM ONLINE & CONNECTED
        </div>
      </div>

      {/* Dashboard statistics panel */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" id="dashboard-stats-grid">
        
        {/* Card 1: Registered Accounts */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center space-x-4" id="stat-card-travelers">
          <div className="bg-blue-50 text-blue-600 border border-blue-100 p-3 rounded-xl flex items-center justify-center shrink-0">
            <span className="material-icons">people_outline</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-0.5">Registered Travelers</p>
            <h2 className="text-2xl font-bold text-slate-900 font-sans">{totalProfilesCount} Profiles</h2>
            <p className="text-[10px] text-blue-600 font-medium mt-0.5">
              {premiumProfilesCount} Premium Accounts
            </p>
          </div>
        </div>

        {/* Card 2: Active SOS records */}
        <div className={`p-5 rounded-2xl border transition-all flex items-center space-x-4 shadow-sm ${
          activeSOSRecords.length > 0 
            ? 'border-2 border-red-500 bg-red-50/50' 
            : 'bg-white border-slate-200'
        }`} id="stat-card-alerts">
          <div className={`p-3 rounded-xl flex items-center justify-center shrink-0 ${
            activeSOSRecords.length > 0 
              ? 'bg-red-500 text-white border border-red-650 animate-pulse' 
              : 'bg-slate-100 text-slate-400 border border-slate-200'
          }`}>
            <span className="material-icons">gpp_maybe</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className={`text-[10px] uppercase tracking-widest mb-0.5 font-bold ${activeSOSRecords.length > 0 ? 'text-red-500' : 'text-slate-400'}`}>Active SOS Alerts</p>
            <h2 className={`text-2xl font-bold font-sans ${activeSOSRecords.length > 0 ? 'text-red-650' : 'text-slate-900'}`}>
              {activeSOSRecords.length} Active Alerts
            </h2>
            <p className="text-[10px] text-slate-500 mt-0.5">
              {activeSOSRecords.length > 0 ? 'Immediate attention required' : 'All safe right now'}
            </p>
          </div>
        </div>

        {/* Card 3: Safe Tracks ongoing */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center space-x-4" id="stat-card-transits">
          <div className="bg-emerald-50 text-emerald-650 border border-emerald-100 p-3 rounded-xl flex items-center justify-center shrink-0">
            <span className="material-icons">navigation</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-0.5">Active Trips</p>
            <h2 className="text-2xl font-bold text-slate-900 font-sans">
              {db.trips.filter(t => t.status === 'ongoing').length} Track(s)
            </h2>
            <p className="text-[10px] text-emerald-650 font-medium mt-0.5">
              Live GPS tracking active
            </p>
          </div>
        </div>

        {/* Card 4: Daily Revenue calculation */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center space-x-4" id="stat-card-revenue">
          <div className="bg-slate-100 text-slate-700 border border-slate-200 p-3 rounded-xl flex items-center justify-center shrink-0">
            <span className="material-icons">payments</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-0.5">Daily Revenue</p>
            <h2 className="text-2xl font-bold text-slate-900 font-sans">
              ${estimatedRevenue}.00
            </h2>
            <p className="text-[10px] text-slate-500 mt-0.5">
              Based on active memberships
            </p>
          </div>
        </div>

      </div>

      {/* Main Visual Layout Grid (Map + Active Dispatch Feeds) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 md:gap-6 flex-1" id="dashboard-main-grid">
        
        {/* Left Column: List of Transit Signals & Emergency controls (span 12 if map is closed, span 7 if map is open) */}
        <div className={`${isMapVisible ? 'lg:col-span-7' : 'lg:col-span-12'} bg-white rounded-2xl border border-slate-200 shadow-sm p-5 flex flex-col space-y-4 text-slate-900 transition-all duration-300`} id="dashboard-signals-panel">
          
          <div className="flex items-center justify-between" id="signals-header-bar">
            <div className="flex items-center space-x-2">
              <span className="material-icons text-blue-600 text-sm animate-pulse">online_prediction</span>
              <h3 className="font-mono text-xs font-bold uppercase tracking-wider text-slate-700">
                Active Live Trips ({activeTripsList.length})
              </h3>
            </div>
            <div className="flex items-center space-x-1 text-[10px] text-emerald-600 font-mono bg-emerald-50 border border-emerald-200/60 px-2.5 py-1 rounded-lg shadow-2xs">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              <span className="font-semibold">LIVE TRIPS FEED</span>
            </div>
          </div>

          <div className="hidden md:block overflow-x-auto border border-slate-200 rounded-lg animate-fadeIn" id="signals-table-wrapper">
            <table className="w-full text-left text-xs font-sans divide-y divide-slate-100" id="signals-table">
              <thead className="bg-slate-50 text-slate-500 font-mono text-[9px] uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Traveler & Device</th>
                  <th className="px-4 py-3">Transport Mode</th>
                  <th className="px-4 py-3">Safety Metrics</th>
                  <th className="px-4 py-3">Status Feed</th>
                  <th className="px-4 py-3 text-right">Emergency Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {activeTripsList.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-slate-400 font-mono bg-white">
                      NO ACTIVE TRIPS RUNNING AT THE MOMENT.
                    </td>
                  </tr>
                ) : (
                  activeTripsList.map(trip => {
                    const traveler = db.profiles.find(p => p.id === trip.user_id);
                    const isSOS = trip.status === 'sos';
                    const isSelected = trip.id === selectedTripId;
                    
                    // Match with travel activities table
                    const activity = db.travelActivities?.find(a => a.id === trip.id) || ({} as any);

                    return (
                      <tr 
                        key={trip.id}
                        id={`trip-row-${trip.id}`}
                        onClick={() => setSelectedTripId(isSelected ? null : trip.id)}
                        className={`cursor-pointer transition-all ${
                          isSelected ? 'bg-blue-50/55 border-l-2 border-blue-600' : 'hover:bg-slate-50/70'
                        } ${isSOS ? 'bg-rose-50/40 hover:bg-rose-50/60' : ''}`}
                      >
                        <td className="px-4 py-3">
                          <div className="font-semibold text-slate-800 flex items-center space-x-1.5">
                            <span>{traveler?.full_name || 'Anonymous'}</span>
                            {traveler?.is_premium && (
                              <span className="bg-blue-100 text-blue-700 text-[8px] font-bold font-mono px-1.5 py-0.2 rounded-full uppercase scale-90">PREMIUM</span>
                            )}
                          </div>
                          <div className="text-[10px] text-slate-400 font-mono flex items-center space-x-1 mt-0.5">
                            <span className="material-icons text-[11px] text-slate-400">phone</span>
                            <span>{traveler?.phone_number || '+1 (555) 000-0000'}</span>
                          </div>
                          <div className="text-[9.5px] text-slate-400 font-mono flex items-center space-x-1 mt-0.5">
                            <span className="material-icons text-[11px]">smartphone</span>
                            <span className="text-slate-500 font-sans">{activity.device_model }</span>
                            <span className="text-slate-300">|</span>
                            <span className="material-icons text-[11px] text-emerald-500">battery_charging_full</span>
                            <span>{activity.end_battery_level || activity.start_battery_level || 0}%</span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center space-x-2 bg-slate-50 border border-slate-150 px-2.5 py-1 rounded-lg w-max">
                            <span className="material-icons text-slate-500 text-sm">
                              {activity.transport_mode?.toLowerCase().includes('walk') ? 'directions_walk' :
                               activity.transport_mode?.toLowerCase().includes('train') || activity.transport_mode?.toLowerCase().includes('metro') ? 'train' :
                               activity.transport_mode?.toLowerCase().includes('bike') ? 'directions_bike' :
                               'directions_car'}
                            </span>
                            <span className="font-mono text-[10.5px] font-bold text-slate-700">{activity.transport_mode || 'Shuttle'}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="space-y-1">
                            <div className="flex items-center space-x-1 text-[10px] font-mono">
                              <span className="text-slate-400">Safety Index:</span>
                              <span className={`font-bold ${activity.area_safety_score && activity.area_safety_score < 6 ? 'text-rose-500' : 'text-emerald-600'}`}>
                                {activity.area_safety_score || '8.5'}/10
                              </span>
                            </div>
                            <div className="flex items-center space-x-1 text-[10px] font-mono">
                              <span className="text-slate-400">Deviations:</span>
                              <span className={`font-bold ${activity.route_deviation_count && activity.route_deviation_count > 0 ? 'text-amber-500' : 'text-slate-500'}`}>
                                {activity.route_deviation_count ?? 0}
                              </span>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          {isSOS ? (
                            <span className="inline-flex items-center space-x-1 font-mono text-[9.5px] font-bold tracking-wider bg-red-100 text-red-600 border border-red-200 px-2.5 py-0.5 rounded-full uppercase animate-pulse">
                              <span className="h-1.5 w-1.5 bg-red-500 rounded-full"></span>
                              <span>SOS ALERT</span>
                            </span>
                          ) : activity.is_gps_lost ? (
                            <span className="inline-flex items-center space-x-1 font-mono text-[9.5px] bg-amber-50 text-amber-700 border border-amber-200 px-2.5 py-0.5 rounded-full uppercase animate-pulse">
                              <span className="material-icons text-xs">gps_off</span>
                              <span>GPS SIGNAL LOST</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center space-x-1 font-mono text-[9.5px] bg-emerald-50 text-emerald-700 border border-emerald-200 px-2.5 py-0.5 rounded-full uppercase">
                              <span className="h-1.5 w-1.5 bg-emerald-500 rounded-full animate-ping"></span>
                              <span>LIVE ON TRACK</span>
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center justify-end space-x-2">
                            <button
                              type="button"
                              onClick={() => setSelectedTripId(isSelected ? null : trip.id)}
                              className={`inline-flex items-center gap-1 font-mono text-[9.5px] font-bold uppercase tracking-wider px-2.5 py-1.5 rounded transition-all cursor-pointer shadow-xs ${
                                isSelected
                                  ? 'bg-blue-600 text-white'
                                  : 'bg-blue-50 text-blue-600 hover:bg-blue-100 border border-blue-200/60'
                              }`}
                              title={isSelected ? 'Close Live Map' : 'Track on Live Satellite Map'}
                            >
                              <span className="material-icons text-xs">{isSelected ? 'close' : 'map'}</span>
                              <span>{isSelected ? 'CLOSE MAP' : 'TRACK MAP'}</span>
                            </button>

                            {isSOS ? (
                              (() => {
                                const r = db.sosRecords.find(s => s.trip_id === trip.id && s.status === 'active');
                                return r ? (
                                  <button
                                    type="button"
                                    onClick={() => resolveSos(r.id)}
                                    className="bg-emerald-650 hover:bg-emerald-550 text-white font-mono text-[9.5px] font-bold uppercase tracking-wider px-2.5 py-1.5 rounded cursor-pointer transition-colors shadow-sm"
                                    id={`resolve-btn-${trip.id}`}
                                  >
                                    RESOLVE CASE
                                  </button>
                                ) : null;
                              })()
                            ) : (
                              <button
                                type="button"
                                onClick={() => triggerSos(trip.id)}
                                className="bg-red-50 text-red-600 border border-red-200 hover:bg-red-600 hover:text-white font-mono text-[9.5px] font-bold uppercase tracking-wider px-2.5 py-1.5 rounded cursor-pointer transition-all shadow-sm"
                                id={`trigger-sos-btn-${trip.id}`}
                              >
                                TRIGGER S.O.S
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Mobile optimized card list view */}
          <div className="md:hidden space-y-3 animate-fadeIn" id="signals-mobile-list">
            {activeTripsList.length === 0 ? (
              <div className="text-center py-8 text-slate-400 font-mono text-xs bg-slate-50 rounded-xl border border-dashed border-slate-200">
                NO MONITORED TRANSITS RECORDED AT THIS STATION.
              </div>
            ) : (
              activeTripsList.map(trip => {
                const traveler = db.profiles.find(p => p.id === trip.user_id);
                const isSOS = trip.status === 'sos';
                const isSelected = trip.id === selectedTripId;
                
                const activity = db.travelActivities?.find(a => a.id === trip.id) || ({} as any);

                return (
                  <div 
                    key={trip.id}
                    id={`mobile-card-${trip.id}`}
                    onClick={() => setSelectedTripId(isSelected ? null : trip.id)}
                    className={`p-4 rounded-xl border transition-all cursor-pointer ${
                      isSelected ? 'border-blue-500 bg-blue-50/35 ring-1 ring-blue-100' : 'border-slate-200 bg-white'
                    } ${isSOS ? 'border-red-400 bg-red-50/30' : ''}`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <div className="font-semibold text-slate-800 text-sm flex items-center space-x-1.5">
                          <span>{traveler?.full_name || 'Unknown User'}</span>
                          {traveler?.is_premium && (
                            <span className="bg-blue-100 text-blue-700 text-[8px] font-bold font-mono px-1.5 rounded-full scale-90">PREMIUM</span>
                          )}
                        </div>
                        <div className="text-[10px] text-slate-400 font-mono">{traveler?.phone_number || 'No phone'}</div>
                      </div>
                      <div>
                        {isSOS ? (
                          <span className="inline-flex items-center space-x-1 font-mono text-[9px] font-bold tracking-wider bg-red-100 text-red-600 border border-red-200 px-2 py-0.5 rounded-full uppercase animate-pulse">
                            <span>SOS</span>
                          </span>
                        ) : activity.is_gps_lost ? (
                          <span className="inline-flex items-center space-x-1 font-mono text-[9px] bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-full uppercase animate-pulse">
                            <span>GPS LOST</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center space-x-1 font-mono text-[9px] bg-emerald-50 text-emerald-700 border border-emerald-250 px-2 py-0.5 rounded-full uppercase">
                            <span className="h-1 w-1 bg-emerald-500 rounded-full animate-ping"></span>
                            <span>ON TRACK</span>
                          </span>
                        )}
                      </div>
                    </div>
                    
                    <div className="text-xs space-y-1 mb-3 pt-2 border-t border-slate-100 text-slate-600">
                      <div className="flex items-center">
                        <span className="material-icons text-slate-400 text-xs mr-1.5">location_on</span>
                        <span className="font-medium truncate">{trip.start_location}</span>
                      </div>
                      <div className="flex items-center">
                        <span className="material-icons text-blue-500 text-xs mr-1.5">flag</span>
                        <span className="font-medium truncate text-blue-650">to {trip.end_location}</span>
                      </div>
                    </div>

                    <div className="flex justify-between items-center bg-slate-50 p-2 rounded-lg" onClick={(e) => e.stopPropagation()}>
                      <span className="font-mono text-[9px] text-slate-450">{activity.transport_mode || 'Shuttle'}</span>
                      <div className="flex items-center space-x-1.5">
                        <button
                          type="button"
                          onClick={() => setSelectedTripId(isSelected ? null : trip.id)}
                          className={`font-mono text-[9px] font-bold uppercase tracking-wider px-2 py-1 rounded cursor-pointer transition-all ${
                            isSelected
                              ? 'bg-blue-600 text-white'
                              : 'bg-blue-50 text-blue-600 border border-blue-200'
                          }`}
                        >
                          {isSelected ? 'CLOSE MAP' : 'TRACK MAP'}
                        </button>
                        {isSOS ? (
                          (() => {
                            const r = db.sosRecords.find(s => s.trip_id === trip.id && s.status === 'active');
                            return r ? (
                              <button
                                type="button"
                                onClick={() => resolveSos(r.id)}
                                className="bg-emerald-600 hover:bg-emerald-500 text-white font-mono text-[9px] font-bold uppercase tracking-wider px-2.5 py-1 rounded cursor-pointer"
                                id={`mobile-resolve-btn-${trip.id}`}
                              >
                                RESOLVE
                              </button>
                            ) : null;
                          })()
                        ) : (
                          <button
                            type="button"
                            onClick={() => triggerSos(trip.id)}
                            className="bg-red-50 hover:bg-red-600 hover:text-white text-red-600 border border-red-200 font-mono text-[9px] font-bold uppercase tracking-wider px-2.5 py-1 rounded cursor-pointer transition-colors"
                            id={`mobile-trigger-sos-btn-${trip.id}`}
                          >
                            TRIG SOS
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Show detailed traveler inspection card */}
          {selectedTrip && (() => {
            const traveler = db.profiles.find(p => p.id === selectedTrip.user_id);
            const sosExist = db.sosRecords.find(s => s.trip_id === selectedTrip.id && s.status === 'active');
            
            // Match with detailed travel activities table fields
            const activity = db.travelActivities?.find(a => a.id === selectedTrip.id) || ({} as any);

            const isSOS = selectedTrip.status === 'sos';

            return (
              <div className="bg-slate-950 text-slate-100 p-5 rounded-2xl mt-4 border border-slate-800 shadow-xl space-y-4 animate-slideUp font-sans" id="inspection-panel">
                
                <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                  <div className="flex items-center space-x-2">
                    <span className="h-2 w-2 bg-rose-600 rounded-full animate-ping"></span>
                    <span className="font-mono text-[10px] text-slate-400 uppercase tracking-widest font-black">
                      DETAILED TRIP & SAFETY ANALYSIS
                    </span>
                  </div>
                  <span className="font-mono text-[10.5px] text-rose-500 bg-rose-500/10 px-2 py-0.5 rounded border border-rose-500/20 font-bold">
                    Code: {activity.tracking_code || (activity.id ? activity.id.split('-')[0].toUpperCase() : 'UNKNOWN')}
                  </span>
                </div>

                {/* Main grid splitter */}
                <div className="grid grid-cols-1 md:grid-cols-12 gap-4 md:gap-5">
                  
                  {/* Left block: Client Profile & Devices (span 4) */}
                  <div className="md:col-span-4 space-y-4">
                    
                    <div className="bg-slate-900/80 p-3.5 rounded-xl border border-slate-800 space-y-2.5">
                      <div className="text-[9px] font-mono text-slate-500 uppercase font-black tracking-wider">Registered Client</div>
                      <div>
                        <div className="text-sm font-bold text-white font-sans">{traveler?.full_name}</div>
                        <div className="text-[10px] text-slate-400 font-mono mt-0.5 flex items-center space-x-1">
                          <span className="material-icons text-xs">phone</span>
                          <span>{traveler?.phone_number}</span>
                        </div>
                        <div className="text-[10.5px] text-blue-400 font-semibold mt-1">
                          {traveler?.is_premium ? '★ PREMIUM ACCESS CLIENT' : 'STANDARD TIER ACCOUNT'}
                        </div>
                      </div>
                    </div>

                    <div className="bg-slate-900/80 p-3.5 rounded-xl border border-slate-800 space-y-2">
                      <div className="text-[9px] font-mono text-slate-500 uppercase font-black tracking-wider">Sensors & Devices</div>
                      <div className="space-y-1.5 text-[11px] font-mono">
                        <div className="flex justify-between items-center text-slate-300">
                          <span className="text-slate-500">Device:</span>
                          <span>{activity.device_model }</span>
                        </div>
                        <div className="flex justify-between items-center text-slate-300">
                          <span className="text-slate-500">G-Sensor:</span>
                          <span className={activity.impact_detected ? 'text-rose-500 font-bold' : 'text-emerald-500'}>
                            {activity.impact_detected ? '🚨 HIGH G-FORCE SHOCK' : 'Nominal (0.02G)'}
                          </span>
                        </div>
                        <div className="flex justify-between items-center text-slate-300">
                          <span className="text-slate-500">Battery Level:</span>
                          <span className="flex items-center space-x-1.5">
                            <span className="text-slate-400 font-mono text-[10px]">Start: {activity.start_battery_level || 0}%</span>
                            <span className="text-slate-600">|</span>
                            <span className="flex items-center text-emerald-400 font-bold">
                              <span className="material-icons text-[11px] mr-0.5">battery_charging_full</span>
                              {activity.end_battery_level || activity.start_battery_level || 0}%
                            </span>
                            {((activity.start_battery_level || 0) - (activity.end_battery_level || activity.start_battery_level || 0)) > 0 && (
                              <span className="text-rose-500 text-[10px] ml-1 font-bold">(-{(activity.start_battery_level || 0) - (activity.end_battery_level || activity.start_battery_level || 0)}%)</span>
                            )}
                          </span>
                        </div>
                      </div>
                    </div>

                  </div>

                  {/* Middle block: Ride & Vehicle Telemetry (span 4) */}
                  <div className="md:col-span-4 space-y-4">
                    
                    <div className="bg-slate-900/80 p-3.5 rounded-xl border border-slate-800 space-y-2.5">
                      <div className="text-[9px] font-mono text-slate-500 uppercase font-black tracking-wider">
                        {activity.transport_mode?.toLowerCase() === 'walking' ? 'Walking Transit HUD' : 'Vehicle & Driver HUD'}
                      </div>
                      {(!activity.transport_mode || activity.transport_mode.toLowerCase() !== 'walking') ? (
                        activity.vehicle_plate_number && activity.vehicle_plate_number !== 'N/A' ? (
                          <div className="space-y-2">
                            <div className="flex items-center space-x-2">
                              <span className="bg-rose-600/10 text-rose-500 border border-rose-600/20 px-2 py-0.5 rounded font-mono text-[10.5px] font-bold">
                                {activity.vehicle_plate_number }
                              </span>
                              <span className="text-[11.5px] text-slate-300 font-sans font-medium truncate">
                                {activity.vehicle_description }
                              </span>
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono flex items-center space-x-1 bg-slate-950 p-1.5 rounded">
                              <span className="material-icons text-xs text-rose-500">badge</span>
                              <span>Driver:</span>
                              <span className="text-white font-sans font-bold">{activity.driver_name_manual || 'Unknown'}</span>
                            </div>
                          </div>
                        ) : (
                          <div className="text-slate-400 text-xs font-mono py-1">
                            No vehicle details assigned.
                          </div>
                        )
                      ) : (
                        <div className="text-slate-400 text-[11px] font-mono py-1 flex items-center space-x-1.5 bg-slate-950 p-2 rounded border border-slate-800/50">
                          <span className="material-icons text-[14px] text-emerald-500">directions_walk</span>
                          <span>Solo walking transit mode. No vehicle.</span>
                        </div>
                      )}
                    </div>

                    <div className="bg-slate-900/80 p-3.5 rounded-xl border border-slate-800 space-y-2.5">
                        <div className="text-[9px] font-mono text-slate-500 uppercase font-black tracking-wider">Live Route Tracker</div>
                        <div className="space-y-1.5 text-xs text-slate-300">
                          <div className="flex items-start space-x-1.5">
                            <span className="material-icons text-[13px] text-slate-500 mt-0.5">location_on</span>
                            <span className="truncate">{activity.start_address || selectedTrip.start_location}</span>
                          </div>
                          <div className="flex items-start space-x-1.5">
                            <span className="material-icons text-[13px] text-rose-500 mt-0.5">flag</span>
                            <span className="truncate font-semibold text-rose-400">to {(activity.end_address || selectedTrip.end_location || '').replace(/\[.*?\]|\(.*?\)/g, '').trim() || 'Selected Destination'}</span>
                          </div>
                        </div>
                      </div>

                    </div>

                    {/* Right block: Safety Indices & Audio Monitoring (span 4) */}
                  <div className="md:col-span-4 space-y-4">
                    
                    <div className="bg-slate-900/80 p-3.5 rounded-xl border border-slate-800 space-y-2">
                      <div className="text-[9px] font-mono text-slate-500 uppercase font-black tracking-wider">Live Area Security Score</div>
                      <div className="flex items-center justify-between">
                        <div>
                          <div className={`text-2xl font-black font-mono ${activity.area_safety_score && activity.area_safety_score < 6 ? 'text-rose-500' : 'text-emerald-400'}`}>
                            {activity.area_safety_score || '8.8'}/10
                          </div>
                          <div className="text-[9.5px] font-mono text-slate-400 uppercase mt-0.5">
                            {activity.area_safety_score && activity.area_safety_score < 6 ? 'High Threat Territory' : 'Secure Zone Index'}
                          </div>
                        </div>
                        
                        {/* Deviations Alert Indicator */}
                        <div className="text-right">
                          <div className={`text-lg font-mono font-bold ${activity.route_deviation_count && activity.route_deviation_count > 0 ? 'text-rose-500 animate-pulse' : 'text-slate-400'}`}>
                            {activity.route_deviation_count || 0}
                          </div>
                          <div className="text-[8.5px] font-mono text-slate-500 uppercase">Deviations</div>
                        </div>
                      </div>
                    </div>

                    {/* Cabin Audio Waveform Stream simulator */}
                    <div className="bg-slate-900/80 p-3.5 rounded-xl border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-[9px] font-mono text-slate-500 uppercase font-black tracking-wider">Cabin Sound Feed</span>
                        <span className="flex items-center space-x-1">
                          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-ping"></span>
                          <span className="text-[8px] font-mono text-emerald-400 uppercase">Live Stream</span>
                        </span>
                      </div>
                      
                      {/* CSS-Animated Audio Waveform bars */}
                      <div className="h-8 flex items-center justify-center space-x-1.5 bg-slate-950 rounded-lg px-3 overflow-hidden border border-slate-800/40">
                        <span className="h-4 w-1 bg-rose-500 rounded animate-[soundPulse_1.2s_infinite_ease-in-out]"></span>
                        <span className="h-6 w-1 bg-rose-400 rounded animate-[soundPulse_0.8s_infinite_ease-in-out]"></span>
                        <span className="h-3 w-1 bg-rose-600 rounded animate-[soundPulse_1.5s_infinite_ease-in-out]"></span>
                        <span className="h-5 w-1 bg-rose-400 rounded animate-[soundPulse_1s_infinite_ease-in-out]"></span>
                        <span className="h-2 w-1 bg-rose-500 rounded animate-[soundPulse_1.3s_infinite_ease-in-out]"></span>
                        <span className="h-5 w-1 bg-rose-400 rounded animate-[soundPulse_0.9s_infinite_ease-in-out]"></span>
                        <span className="h-3 w-1 bg-rose-600 rounded animate-[soundPulse_1.4s_infinite_ease-in-out]"></span>
                      </div>
                    </div>

                  </div>

                </div>

                {/* Bottom row: Guardians list or SOS critical details */}
                {isSOS && (
                  <div className="bg-rose-900/30 border-2 border-rose-600/50 p-4 rounded-xl flex items-start space-x-3 text-xs animate-pulse" id="active-alert-panel">
                    <span className="material-icons text-rose-500 text-2xl">emergency_share</span>
                    <div className="flex-1 space-y-1">
                      <p className="font-mono text-xs font-black text-rose-500 uppercase tracking-wide flex items-center space-x-1.5">
                        <span>CRITICAL: PASSENGER PANIC SOS BROADCAST ACTIVE</span>
                      </p>
                      <p className="text-slate-300 font-mono text-[10px]">
                        Emergency SOS protocol triggered at {sosExist ? formatTime(sosExist.triggered_at) : 'recently'}. 
                        Impact collision trigger: {activity.impact_detected ? '🚨 TRUE (VEHICLE CRASH WARNING)' : 'FALSE'}.
                      </p>
                      {activity.notified_guardians && activity.notified_guardians.length > 0 && (
                        <div className="text-[10px] text-slate-400 font-mono pt-1">
                          <span className="text-rose-400 font-bold">Broadcasted Guardians:</span> {activity.notified_guardians.join(' | ')}
                        </div>
                      )}
                    </div>
                  </div>
                )}

              </div>
            );
          })()}

        </div>

        {/* Right Column: Visual Radar and Live Map (span 5, conditionally shown ONLY when a trip is selected!) */}
        {isMapVisible && (
          <div className="lg:col-span-5 bg-white rounded-2xl border border-slate-200 shadow-sm p-5 flex flex-col space-y-4 lg:sticky lg:top-2 self-start lg:max-h-[calc(100vh-120px)] animate-fadeIn" id="dashboard-radar-panel">
            
            <div className="flex items-center justify-between" id="radar-header-bar">
              <div className="flex items-center space-x-2">
                <span className="material-icons text-blue-600 text-sm">satellite_alt</span>
                <h3 className="font-mono text-xs font-bold uppercase tracking-wider text-slate-700">
                  Live Trip Map View
                </h3>
              </div>
              
              <div className="flex items-center space-x-2">
                <div className="flex items-center space-x-1">
                  <span className="h-2 w-2 rounded-full bg-blue-500 animate-ping"></span>
                  <span className="font-mono text-[9px] text-blue-600 tracking-wider select-none">LIVE GPS</span>
                </div>
                <button
                  type="button"
                  onClick={() => setIsMaximized(true)}
                  className="bg-blue-50 text-blue-600 hover:bg-blue-100 border border-blue-200/40 hover:border-blue-300 p-1.5 rounded-lg flex items-center justify-center transition-all cursor-pointer"
                  title="Expand Map to Fullscreen"
                  id="radar-expand-button"
                >
                  <span className="material-icons text-sm leading-none">open_in_full</span>
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedTripId(null)}
                  className="bg-slate-100 hover:bg-slate-200 text-slate-600 hover:text-slate-900 border border-slate-200 p-1.5 rounded-lg flex items-center justify-center transition-all cursor-pointer"
                  title="Close Map View (Expand Table to Full Width)"
                  id="radar-close-button"
                >
                  <span className="material-icons text-sm leading-none">close</span>
                </button>
              </div>
            </div>

            {/* Live Satellite Mapbox Map Container */}
            <div className="flex-1 min-h-[380px] lg:min-h-[440px] relative rounded-xl overflow-hidden flex flex-col" id="monitor-mapbox-container">
              <MapboxMonitor
                activeTripsList={activeTripsList}
                profiles={db.profiles}
                selectedTripId={selectedTripId}
                onSelectTrip={(tripId) => setSelectedTripId(tripId)}
              />
            </div>

            <div className="text-[10px] text-slate-500 text-center font-mono uppercase bg-slate-100 p-2.5 rounded border border-slate-200">
              GRID MAP COORDINATES RENDERED LIVE VIA MAPBOX GL SATELLITE ENGINE
            </div>

          </div>
        )}

      </div>

      {/* High-Fidelity Fullscreen Map Overlay / Dashboard Wizard */}
      {isMaximized && (
        <div 
          className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-md flex items-center justify-center p-4 md:p-6 select-none"
          id="fullscreen-radar-backdrop"
        >
          <div 
            className="bg-white border border-slate-200 rounded-3xl shadow-2xl w-full max-w-6xl h-[88vh] flex flex-col overflow-hidden animate-fadeIn"
            id="fullscreen-radar-wizard-container"
          >
            {/* Wizard Header bar */}
            <div className="px-6 py-4.5 border-b border-slate-100 flex items-center justify-between bg-slate-50/70" id="fullscreen-wizard-header">
              <div className="flex items-center space-x-3">
                <div className="w-9 h-9 bg-blue-100/65 border border-blue-200/50 rounded-xl flex items-center justify-center text-blue-600">
                  <span className="material-icons text-lg">satellite_alt</span>
                </div>
                <div>
                  <h3 className="font-bold text-slate-800 text-sm tracking-tight">
                    Full-Screen Live Trip Tracking Map
                  </h3>
                  <p className="text-[10.5px] text-slate-400 font-sans mt-0.5">
                    Viewing real-time trip route, traveler location, and active status updates
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-3">
                <span className="bg-emerald-100 text-emerald-700 border border-emerald-200 text-[9px] font-mono font-black tracking-widest px-2.5 py-1 rounded-full flex items-center uppercase">
                  <span className="h-1.5 w-1.5 bg-emerald-500 rounded-full animate-pulse mr-1.5"></span>
                  Live GPS Active
                </span>
                
                <button
                  type="button"
                  onClick={() => setIsMaximized(false)}
                  className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 flex items-center justify-center transition-all cursor-pointer border border-slate-200/60"
                  title="Close Fullscreen View"
                  id="fullscreen-wizard-close"
                >
                  <span className="material-icons text-sm leading-none font-bold">close</span>
                </button>
              </div>
            </div>

            {/* Expansive Map Container viewport */}
            <div className="flex-1 relative bg-slate-900 overflow-hidden" id="fullscreen-wizard-body">
              <MapboxMonitor
                activeTripsList={activeTripsList}
                profiles={db.profiles}
                selectedTripId={selectedTripId}
                onSelectTrip={(tripId) => setSelectedTripId(tripId)}
              />
            </div>

            {/* Wizard Footer bar */}
            <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex items-center justify-between" id="fullscreen-wizard-footer">
              <div className="flex items-center space-x-1 font-mono text-[9.5px] text-slate-400">
                <span className="material-icons text-xs text-blue-500">online_prediction</span>
                <span>GLOBAL SYSTEM POSITIONING ACTIVE VIA MAPBOX GL FLIGHT PATH ENGINE</span>
              </div>
              <button
                type="button"
                onClick={() => setIsMaximized(false)}
                className="bg-slate-900 hover:bg-slate-800 text-white font-mono text-[10px] font-bold uppercase tracking-wider px-4 py-2 rounded-xl transition-colors cursor-pointer"
              >
                Minimize Screen
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}




