'use client';

import React, { useState, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import { DbState, TravelActivity } from '../../lib/supabase';
import { calculateRealTripDistance, calculateTotalRealDistance, formatRealTripDuration } from '../../lib/TripDistanceCalculator';

const TripDetailsModal = dynamic(() => import('../TripDetailsModal'), { ssr: false });

interface UserTravelHistorySectionProps {
  db: DbState;
  userId: string;
  onBack?: () => void;
}

export default function UserTravelHistorySection({
  db,
  userId,
  onBack
}: UserTravelHistorySectionProps) {
  const router = useRouter();
  const [selectedTripDetails, setSelectedTripDetails] = useState<TravelActivity | null>(null);

  // Search & Filter States
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'ongoing' | 'sos' | 'completed'>('all');
  const [modeFilter, setModeFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<'newest' | 'oldest' | 'distance'>('newest');

  // Find User Profile
  const profile = useMemo(() => {
    return db.profiles.find(p => p.id === userId) || null;
  }, [db.profiles, userId]);

  // Find all activities for this user
  const userActivities = useMemo(() => {
    return (db.travelActivities || []).filter(a => a.user_id === userId);
  }, [db.travelActivities, userId]);

  // Filtered & Sorted Activities
  const filteredActivities = useMemo(() => {
    return userActivities
      .filter(act => {
        // Status filter
        if (statusFilter === 'sos') {
          const isSos = db.sosRecords.some(s => s.trip_id === act.id && s.status === 'active') || act.safety_status === 'sos';
          if (!isSos) return false;
        } else if (statusFilter === 'ongoing') {
          const isOngoing = act.safety_status === 'ongoing' || (!act.end_time && act.safety_status !== 'sos');
          if (!isOngoing) return false;
        } else if (statusFilter === 'completed') {
          const isCompleted = act.safety_status === 'completed' || (act.end_time && act.safety_status !== 'sos');
          if (!isCompleted) return false;
        }

        // Mode filter
        if (modeFilter !== 'all') {
          const mode = (act.transport_mode || 'transit').toLowerCase();
          if (mode !== modeFilter.toLowerCase()) return false;
        }

        // Search Query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchStart = act.start_address?.toLowerCase().includes(q) ?? false;
          const matchEnd = act.end_address?.toLowerCase().includes(q) ?? false;
          const matchPlate = act.vehicle_plate_number?.toLowerCase().includes(q) ?? false;
          const matchDriver = act.driver_name_manual?.toLowerCase().includes(q) ?? false;
          const matchCode = act.tracking_code?.toLowerCase().includes(q) ?? false;
          if (!matchStart && !matchEnd && !matchPlate && !matchDriver && !matchCode) {
            return false;
          }
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'newest') {
          return new Date((b.start_time || b.created_at) as string).getTime() - new Date((a.start_time || a.created_at) as string).getTime();
        }
        if (sortBy === 'oldest') {
          return new Date((a.start_time || a.created_at) as string).getTime() - new Date((b.start_time || b.created_at) as string).getTime();
        }
        if (sortBy === 'distance') {
          return calculateRealTripDistance(b) - calculateRealTripDistance(a);
        }
        return 0;
      });
  }, [userActivities, statusFilter, modeFilter, searchQuery, sortBy, db.sosRecords]);

  // Aggregate Metrics (Strictly real route distance traveled)
  const metrics = useMemo(() => {
    const totalKm = calculateTotalRealDistance(userActivities);
    const sosCount = userActivities.filter(a => a.safety_status === 'sos' || db.sosRecords.some(s => s.trip_id === a.id)).length;
    const completedCount = userActivities.filter(a => a.safety_status === 'completed' || a.end_time).length;
    const ongoingCount = userActivities.filter(a => a.safety_status === 'ongoing' || (!a.end_time && a.safety_status !== 'sos')).length;

    return {
      totalTrips: userActivities.length,
      totalKm: totalKm.toFixed(1),
      sosCount,
      completedCount,
      ongoingCount
    };
  }, [userActivities, db.sosRecords]);

  const handleBack = () => {
    if (onBack) {
      onBack();
    } else {
      router.push('/dashboard/users');
    }
  };

  const getTransportIcon = (mode?: string | null) => {
    const m = (mode || '').toLowerCase();
    if (m === 'walking') return 'directions_walk';
    if (m === 'cycling' || m === 'bicycle') return 'pedal_bike';
    if (m === 'driving' || m === 'car') return 'directions_car';
    if (m === 'bus' || m === 'transit') return 'directions_bus';
    if (m === 'metro' || m === 'train') return 'train';
    return 'navigation';
  };

  const initials = profile?.full_name
    ? profile.full_name.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase()
    : 'U';

  return (
    <div className="flex flex-col h-full space-y-4" id="user-travel-history-root">
      
      {/* 1. TOP HEADER & TRAVELER IDENTITY CARD */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 sm:p-5">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          
          {/* Identity Info */}
          <div className="flex items-center gap-3.5">
            <button
              type="button"
              onClick={handleBack}
              title="Return to Traveler Registry"
              className="w-9 h-9 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 flex items-center justify-center transition-colors cursor-pointer shrink-0"
            >
              <span className="material-icons text-lg">arrow_back</span>
            </button>

            <div className="w-11 h-11 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-sm shrink-0 border-2 border-indigo-200">
              {initials}
            </div>

            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-base sm:text-lg font-bold text-slate-900 leading-none">
                  {profile?.full_name || 'Traveler Travel History'}
                </h2>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-semibold border border-slate-200">
                  {profile?.phone_number || userId}
                </span>
                {profile?.is_premium && (
                  <span className="text-[9.5px] font-bold px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200">
                    PREMIUM
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5">
                <span className="material-icons text-sm text-blue-500">route</span>
                <span>Complete Trip & Route History</span>
              </p>
            </div>
          </div>

          {/* Quick Metrics Bar */}
          <div className="flex items-center gap-2 sm:gap-3 w-full md:w-auto overflow-x-auto pb-1 md:pb-0">
            <div className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-center min-w-[75px] shrink-0">
              <span className="text-[10px] font-mono text-slate-500 uppercase block font-semibold">Total Trips</span>
              <span className="text-sm font-bold text-slate-800 font-mono">{metrics.totalTrips}</span>
            </div>

            <div className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-center min-w-[75px] shrink-0">
              <span className="text-[10px] font-mono text-slate-500 uppercase block font-semibold">Distance</span>
              <span className="text-sm font-bold text-indigo-600 font-mono">{metrics.totalKm} km</span>
            </div>

            <div className="px-3 py-2 bg-emerald-50 border border-emerald-200 rounded-lg text-center min-w-[75px] shrink-0">
              <span className="text-[10px] font-mono text-emerald-700 uppercase block font-semibold">Completed</span>
              <span className="text-sm font-bold text-emerald-800 font-mono">{metrics.completedCount}</span>
            </div>

            {metrics.sosCount > 0 && (
              <div className="px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-center min-w-[75px] shrink-0">
                <span className="text-[10px] font-mono text-red-700 uppercase block font-semibold">SOS Alerts</span>
                <span className="text-sm font-bold text-red-800 font-mono">{metrics.sosCount}</span>
              </div>
            )}
          </div>

        </div>
      </div>

      {/* 2. SEARCH & FILTER TOOLBAR */}
      <div className="bg-white p-3 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row justify-between items-center gap-3">
        {/* Search Input */}
        <div className="relative w-full md:w-80">
          <span className="material-icons absolute left-3 top-2.5 text-slate-400 text-sm">search</span>
          <input
            type="text"
            placeholder="Search by location, plate, code..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-8 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-all text-slate-800"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-2 text-slate-400 hover:text-slate-600 text-xs"
              title="Clear search"
            >
              <span className="material-icons text-sm">close</span>
            </button>
          )}
        </div>

        {/* Filter Badges & Sort Dropdown */}
        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          {/* Status Filter */}
          <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg">
            <button
              type="button"
              onClick={() => setStatusFilter('all')}
              className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                statusFilter === 'all'
                  ? 'bg-white text-slate-800 shadow-xs'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              All ({userActivities.length})
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('ongoing')}
              className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-all flex items-center gap-1 cursor-pointer ${
                statusFilter === 'ongoing'
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'text-slate-500 hover:text-emerald-700'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
              Ongoing ({metrics.ongoingCount})
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('sos')}
              className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-all flex items-center gap-1 cursor-pointer ${
                statusFilter === 'sos'
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'text-slate-500 hover:text-red-700'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
              SOS ({metrics.sosCount})
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('completed')}
              className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                statusFilter === 'completed'
                  ? 'bg-white text-slate-800 shadow-xs'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              Completed ({metrics.completedCount})
            </button>
          </div>

          {/* Transport Mode Dropdown */}
          <select
            value={modeFilter}
            onChange={e => setModeFilter(e.target.value)}
            className="text-xs px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 focus:outline-none focus:border-blue-500 cursor-pointer"
          >
            <option value="all">All Modes</option>
            <option value="walking">Walking</option>
            <option value="cycling">Cycling</option>
            <option value="driving">Driving</option>
            <option value="transit">Transit</option>
          </select>

          {/* Sort Dropdown */}
          <select
            value={sortBy}
            onChange={e => setSortBy(e.target.value as any)}
            className="text-xs px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 focus:outline-none focus:border-blue-500 cursor-pointer"
          >
            <option value="newest">Newest First</option>
            <option value="oldest">Oldest First</option>
            <option value="distance">Longest Distance</option>
          </select>
        </div>
      </div>

      {/* 3. TRAVEL TIMELINE CARDS GRID */}
      <div className="flex-1 overflow-y-auto">
        {filteredActivities.length === 0 ? (
          <div className="bg-white rounded-xl border border-dashed border-slate-200 p-8 text-center space-y-2">
            <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <span className="material-icons text-xl">route</span>
            </div>
            <p className="text-sm font-semibold text-slate-700">No Travel Records Found</p>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              {searchQuery || statusFilter !== 'all' || modeFilter !== 'all'
                ? 'No travel logs match your selected filter criteria. Try resetting the filters.'
                : 'This user does not have any travel logs recorded in the database yet.'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
            {filteredActivities.map(trip => {
              const isSos = db.sosRecords.some(s => s.trip_id === trip.id && s.status === 'active') || trip.safety_status === 'sos';
              const isOngoing = trip.safety_status === 'ongoing' || (!trip.end_time && !isSos);
              const tripDate = new Date((trip.start_time || trip.created_at) as string).toLocaleDateString(undefined, {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
              });
              const tripTime = new Date((trip.start_time || trip.created_at) as string).toLocaleTimeString(undefined, {
                hour: '2-digit',
                minute: '2-digit'
              });

              const realDistance = calculateRealTripDistance(trip);
              const realDuration = formatRealTripDuration(trip);

              return (
                <div
                  key={trip.id}
                  onClick={() => setSelectedTripDetails(trip)}
                  className={`bg-white rounded-xl border transition-all p-3.5 flex flex-col justify-between cursor-pointer group hover:shadow-md relative ${
                    isSos
                      ? 'border-red-300 hover:border-red-500 bg-red-50/10'
                      : isOngoing
                      ? 'border-emerald-300 hover:border-emerald-500 bg-emerald-50/10'
                      : 'border-slate-200 hover:border-blue-400'
                  }`}
                >
                  <div>
                    {/* Top Row: Mode & Status */}
                    <div className="flex justify-between items-start mb-2">
                      <div className="flex items-center gap-1.5">
                        <span className={`w-7 h-7 rounded-lg flex items-center justify-center text-sm ${
                          isSos ? 'bg-red-100 text-red-700' : isOngoing ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-700 group-hover:bg-blue-100 group-hover:text-blue-700'
                        } transition-colors`}>
                          <span className="material-icons text-base">
                            {getTransportIcon(trip.transport_mode)}
                          </span>
                        </span>
                        <div>
                          <span className="font-bold text-xs text-slate-800 capitalize block leading-tight">
                            {trip.transport_mode || 'Transit'}
                          </span>
                          <span className="text-[10px] text-slate-400 font-mono">
                            {tripDate} at {tripTime}
                          </span>
                        </div>
                      </div>

                      {/* Status Tag */}
                      <span className={`text-[9.5px] font-bold font-mono px-2 py-0.5 rounded-full ${
                        isSos
                          ? 'bg-red-100 text-red-700 border border-red-200 animate-pulse'
                          : isOngoing
                          ? 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                          : 'bg-slate-100 text-slate-600 border border-slate-200'
                      }`}>
                        {isSos ? 'SOS ALERT' : isOngoing ? 'ONGOING' : 'COMPLETED'}
                      </span>
                    </div>

                    {/* Route Details */}
                    <div className="space-y-1.5 my-2.5 pl-1.5 border-l-2 border-slate-200 group-hover:border-blue-400 transition-colors">
                      <div className="flex items-start gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-500 shrink-0 mt-1"></span>
                        <div className="min-w-0 flex-1">
                          <span className="text-[10px] font-mono text-slate-400 uppercase block leading-none">Origin</span>
                          <span className="text-xs text-slate-700 font-medium line-clamp-1">
                            {trip.start_address || 'Origin GPS Pin'}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-start gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-rose-500 shrink-0 mt-1"></span>
                        <div className="min-w-0 flex-1">
                          <span className="text-[10px] font-mono text-slate-400 uppercase block leading-none">Destination</span>
                          <span className="text-xs text-slate-700 font-medium line-clamp-1">
                            {trip.end_address || 'Destination GPS Pin'}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Telemetry Snapshot Badges */}
                    <div className="flex flex-wrap items-center gap-1.5 pt-1">
                      {realDistance > 0 && (
                        <span className="text-[10px] font-mono bg-slate-50 text-slate-600 px-2 py-0.5 rounded border border-slate-200 flex items-center gap-1">
                          <span className="material-icons text-[11px] text-slate-400">straighten</span>
                          {realDistance.toFixed(1)} km
                        </span>
                      )}

                      {realDuration && (
                        <span className="text-[10px] font-mono bg-slate-50 text-slate-600 px-2 py-0.5 rounded border border-slate-200 flex items-center gap-1">
                          <span className="material-icons text-[11px] text-slate-400">schedule</span>
                          {realDuration}
                        </span>
                      )}

                      {trip.end_battery_level != null && (
                        <span className="text-[10px] font-mono bg-slate-50 text-slate-600 px-2 py-0.5 rounded border border-slate-200 flex items-center gap-1">
                          <span className="material-icons text-[11px] text-slate-400">battery_std</span>
                          {trip.end_battery_level}%
                        </span>
                      )}

                      {trip.vehicle_plate_number && (
                        <span className="text-[10px] font-mono bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded border border-indigo-200 flex items-center gap-1">
                          <span className="material-icons text-[11px]">pin</span>
                          {trip.vehicle_plate_number}
                        </span>
                      )}

                      {trip.audio_clip_url && (
                        <span className="text-[10px] font-mono bg-violet-50 text-violet-700 px-2 py-0.5 rounded border border-violet-200 flex items-center gap-1">
                          <span className="material-icons text-[11px]">graphic_eq</span>
                          Audio Log
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Card Action Link */}
                  <div className="mt-3 pt-2.5 border-t border-slate-100 flex justify-between items-center text-xs text-blue-600 font-semibold group-hover:text-blue-700">
                    <span className="flex items-center gap-1">
                      <span className="material-icons text-sm">visibility</span>
                      View Speed & Interactive Route
                    </span>
                    <span className="material-icons text-sm group-hover:translate-x-1 transition-transform">
                      chevron_right
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* 4. TRIP DETAILS TELEMETRY WIZARD MODAL */}
      {selectedTripDetails && (
        <TripDetailsModal
          trip={selectedTripDetails}
          profiles={db.profiles}
          onClose={() => setSelectedTripDetails(null)}
        />
      )}

    </div>
  );
}
