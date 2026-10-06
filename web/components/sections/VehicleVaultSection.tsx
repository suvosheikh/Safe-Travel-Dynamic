'use client';

import React, { useState, useMemo } from 'react';
import { DbState, TravelActivity, Trip } from '../../lib/supabase';

interface VehicleVaultSectionProps {
  db: DbState;
  onInspectTrip?: (tripId: string) => void;
  formatDateTime: (isoString: string) => string;
}

export default function VehicleVaultSection({
  db,
  onInspectTrip,
  formatDateTime
}: VehicleVaultSectionProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterMode, setFilterMode] = useState<'all' | 'with_photo' | 'car' | 'cng' | 'bike'>('all');
  const [lightboxImage, setLightboxImage] = useState<{ url: string; title: string; plate?: string } | null>(null);

  // Combine trips and travel activities to get richest vehicle metadata
  const vehicleRecords = useMemo(() => {
    const list: Array<{
      tripId: string;
      activityId?: string;
      travelerName: string;
      travelerPhone?: string;
      transportMode: string;
      plateNumber: string;
      description: string;
      driverName: string;
      photoUrl: string | null;
      status: string;
      createdAt: string;
      startLocation: string;
      endLocation: string;
    }> = [];

    // Prioritize travelActivities if available, fallback to trips
    if (db.travelActivities && db.travelActivities.length > 0) {
      db.travelActivities.forEach(act => {
        const traveler = db.profiles.find(p => p.id === act.user_id);
        list.push({
          tripId: act.id,
          activityId: act.id,
          travelerName: traveler?.full_name || 'Passenger',
          travelerPhone: traveler?.phone_number,
          transportMode: act.transport_mode || 'transit',
          plateNumber: act.vehicle_plate_number || 'UNLISTED',
          description: act.vehicle_description || 'Standard Transit Vehicle',
          driverName: act.driver_name_manual || 'Not Specified',
          photoUrl: act.vehicle_photo_url || null,
          status: act.safety_status || 'ongoing',
          createdAt: act.start_time || act.created_at || new Date().toISOString(),
          startLocation: act.start_address || 'Origin Point',
          endLocation: act.end_address || 'Destination'
        });
      });
    } else {
      db.trips.forEach(t => {
        const traveler = db.profiles.find(p => p.id === t.user_id);
        list.push({
          tripId: t.id,
          travelerName: traveler?.full_name || 'Passenger',
          travelerPhone: traveler?.phone_number,
          transportMode: t.transport_mode || 'transit',
          plateNumber: t.vehicle_plate_number || 'UNLISTED',
          description: t.vehicle_description || 'Standard Transit Vehicle',
          driverName: 'Not Specified',
          photoUrl: t.vehicle_photo_url || null,
          status: t.status,
          createdAt: t.start_time || t.created_at,
          startLocation: t.start_location,
          endLocation: t.end_location
        });
      });
    }

    return list;
  }, [db.travelActivities, db.trips, db.profiles]);

  // Filter records
  const filteredRecords = useMemo(() => {
    return vehicleRecords.filter(rec => {
      // Query filter
      const q = searchQuery.toLowerCase().trim();
      const matchesSearch = !q || 
        rec.plateNumber.toLowerCase().includes(q) ||
        rec.driverName.toLowerCase().includes(q) ||
        rec.travelerName.toLowerCase().includes(q) ||
        rec.description.toLowerCase().includes(q) ||
        rec.tripId.toLowerCase().includes(q);

      if (!matchesSearch) return false;

      // Category filter
      if (filterMode === 'with_photo') return Boolean(rec.photoUrl);
      if (filterMode === 'car') {
        const m = rec.transportMode.toLowerCase();
        return m.includes('car') || m.includes('taxi') || m.includes('uber') || m.includes('ride');
      }
      if (filterMode === 'cng') {
        const m = rec.transportMode.toLowerCase();
        return m.includes('cng') || m.includes('auto') || m.includes('rickshaw');
      }
      if (filterMode === 'bike') {
        const m = rec.transportMode.toLowerCase();
        return m.includes('bike') || m.includes('motor') || m.includes('scooter');
      }

      return true;
    });
  }, [vehicleRecords, searchQuery, filterMode]);

  const totalWithPhotos = vehicleRecords.filter(r => r.photoUrl).length;

  return (
    <div className="space-y-4 md:space-y-6 animate-fadeIn" id="vehicle-vault-root">
      
      {/* Header section with Stats summary */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-200" id="vehicle-vault-header">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-slate-800 flex items-center">
            <span className="material-icons text-cyan-600 mr-2 text-2xl">directions_car</span>
            Vehicle Verification & Plate Vault
          </h1>
          <p className="text-slate-500 text-xs mt-0.5">
            Cloudinary photo evidence archive, license plate registry, and vehicle safety audit logs.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="bg-white border border-slate-200 px-3 py-1.5 rounded-xl shadow-2xs flex items-center gap-2 text-xs font-mono">
            <span className="w-2 h-2 rounded-full bg-cyan-500"></span>
            <span className="text-slate-500">PHOTO VAULT:</span>
            <span className="font-bold text-slate-800">{totalWithPhotos} / {vehicleRecords.length} VERIFIED</span>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-2xs flex flex-col md:flex-row items-center justify-between gap-3">
        
        {/* Sub-navigation Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {[
            { id: 'all', label: 'All Fleet', count: vehicleRecords.length, icon: 'all_inclusive' },
            { id: 'with_photo', label: 'Photo Evidence', count: totalWithPhotos, icon: 'photo_camera' },
            { id: 'car', label: 'Cars & Taxis', icon: 'local_taxi' },
            { id: 'cng', label: 'CNG & Auto', icon: 'electric_rickshaw' },
            { id: 'bike', label: 'Bikes', icon: 'two_wheeler' },
          ].map(tab => {
            const active = filterMode === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setFilterMode(tab.id as any)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                  active 
                    ? 'bg-cyan-600 text-white font-semibold shadow-xs' 
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-900'
                }`}
              >
                <span className="material-icons text-sm">{tab.icon}</span>
                <span>{tab.label}</span>
                {tab.count !== undefined && (
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
                    active ? 'bg-cyan-800 text-cyan-100' : 'bg-slate-200 text-slate-700'
                  }`}>
                    {tab.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Search Input */}
        <div className="relative w-full md:w-72">
          <span className="material-icons text-slate-400 absolute left-3 top-2.5 text-base">search</span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search plate, driver, passenger..."
            className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-8 py-1.5 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-500/30 focus:border-cyan-500 transition-all"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-2 text-slate-400 hover:text-slate-600"
            >
              <span className="material-icons text-sm">close</span>
            </button>
          )}
        </div>
      </div>

      {/* Grid of Vehicle Records */}
      {filteredRecords.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center text-slate-400 font-mono text-xs">
          <span className="material-icons text-4xl text-slate-300 mb-2 block">no_crash</span>
          NO VEHICLE RECORDS MATCH CURRENT FILTER
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4" id="vehicle-grid">
          {filteredRecords.map((item) => {
            const hasPhoto = Boolean(item.photoUrl);
            const isSos = item.status === 'sos';

            return (
              <div 
                key={item.tripId} 
                className={`bg-white border rounded-2xl p-4 shadow-2xs hover:shadow-md transition-all flex flex-col justify-between ${
                  isSos ? 'border-red-300 ring-1 ring-red-400/40' : 'border-slate-200'
                }`}
              >
                <div>
                  {/* Top Header: Plate & Mode */}
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <span className="bg-slate-900 text-yellow-400 font-mono font-bold text-xs px-2.5 py-1 rounded-md border border-slate-700 tracking-wider shadow-inner">
                        {item.plateNumber}
                      </span>
                      {hasPhoto && (
                        <span className="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[9.5px] font-mono px-1.5 py-0.5 rounded font-bold flex items-center gap-1">
                          <span className="material-icons text-[11px]">verified</span>
                          PHOTO VERIFIED
                        </span>
                      )}
                    </div>

                    <span className={`text-[9.5px] font-mono font-bold uppercase px-2 py-0.5 rounded-full ${
                      isSos 
                        ? 'bg-red-100 text-red-700 border border-red-200 animate-pulse' 
                        : item.status === 'ongoing' 
                        ? 'bg-cyan-100 text-cyan-700 border border-cyan-200' 
                        : 'bg-slate-100 text-slate-600 border border-slate-200'
                    }`}>
                      {isSos ? 'SOS ALERT' : item.status}
                    </span>
                  </div>

                  {/* Vehicle Photo or Placeholder */}
                  <div className="relative mb-3.5 rounded-xl overflow-hidden bg-slate-900 border border-slate-200 aspect-video group">
                    {hasPhoto ? (
                      <>
                        <img 
                          src={item.photoUrl!} 
                          alt={`Vehicle ${item.plateNumber}`} 
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />
                        <button
                          type="button"
                          onClick={() => setLightboxImage({ url: item.photoUrl!, title: item.description, plate: item.plateNumber })}
                          className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5 text-white font-mono text-xs font-semibold cursor-pointer"
                        >
                          <span className="material-icons text-base">zoom_in</span>
                          INSPECT HIGH-RES
                        </button>
                      </>
                    ) : (
                      <div className="w-full h-full flex flex-col items-center justify-center text-slate-500 bg-slate-900/60 p-4 text-center">
                        <span className="material-icons text-3xl text-slate-600 mb-1">directions_car</span>
                        <span className="text-[10px] font-mono uppercase tracking-wider text-slate-400">NO PHOTO UPLOADED</span>
                        <span className="text-[9px] text-slate-500">Traveler started trip without taking vehicle picture</span>
                      </div>
                    )}
                  </div>

                  {/* Vehicle Details */}
                  <div className="space-y-1.5 text-xs text-slate-700">
                    <div>
                      <span className="text-[10px] font-mono text-slate-400 uppercase block">Vehicle / Transport</span>
                      <p className="font-semibold text-slate-800 leading-snug">{item.description}</p>
                    </div>

                    <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-100">
                      <div>
                        <span className="text-[9.5px] font-mono text-slate-400 uppercase block">Driver</span>
                        <p className="font-medium text-slate-700 truncate">{item.driverName}</p>
                      </div>
                      <div>
                        <span className="text-[9.5px] font-mono text-slate-400 uppercase block">Passenger</span>
                        <p className="font-medium text-slate-700 truncate">{item.travelerName}</p>
                      </div>
                    </div>

                    <div className="pt-1 border-t border-slate-100">
                      <span className="text-[9.5px] font-mono text-slate-400 uppercase block">Route</span>
                      <p className="text-[11px] text-slate-600 truncate">{item.startLocation} &rarr; {item.endLocation}</p>
                    </div>
                  </div>
                </div>

                {/* Footer Action */}
                <div className="pt-3 mt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-[9.5px] font-mono text-slate-400">
                    {formatDateTime(item.createdAt)}
                  </span>
                  {onInspectTrip && (
                    <button
                      type="button"
                      onClick={() => onInspectTrip(item.tripId)}
                      className="bg-cyan-50 hover:bg-cyan-100 text-cyan-700 border border-cyan-200 text-[10px] font-mono font-bold px-2.5 py-1 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                    >
                      <span>INSPECT TRIP</span>
                      <span className="material-icons text-xs">arrow_forward</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Lightbox Modal */}
      {lightboxImage && (
        <div 
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn"
          onClick={() => setLightboxImage(null)}
        >
          <div 
            className="bg-slate-900 border border-slate-800 rounded-3xl max-w-3xl w-full overflow-hidden shadow-2xl relative"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-4 border-b border-slate-800 flex items-center justify-between text-white">
              <div className="flex items-center gap-2">
                <span className="material-icons text-cyan-400">verified</span>
                <div>
                  <h3 className="font-bold text-sm">{lightboxImage.title}</h3>
                  <span className="text-[10px] font-mono text-cyan-400 uppercase">PLATE: {lightboxImage.plate}</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <a 
                  href={lightboxImage.url} 
                  target="_blank" 
                  rel="noreferrer"
                  className="bg-slate-800 hover:bg-slate-700 text-white text-xs px-2.5 py-1 rounded-lg flex items-center gap-1 transition-colors"
                  title="Open original image in full resolution"
                >
                  <span className="material-icons text-sm">open_in_new</span>
                  <span>ORIGINAL</span>
                </a>
                <button
                  type="button"
                  onClick={() => setLightboxImage(null)}
                  className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors"
                >
                  <span className="material-icons text-lg">close</span>
                </button>
              </div>
            </div>

            <div className="p-3 bg-black flex items-center justify-center max-h-[75vh] overflow-hidden">
              <img 
                src={lightboxImage.url} 
                alt={lightboxImage.title} 
                className="max-h-[70vh] w-auto object-contain rounded-lg shadow-lg"
              />
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
