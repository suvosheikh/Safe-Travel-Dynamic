'use client';
 
import React, { useState, useMemo } from 'react';
import { DbState, SOSRecord } from '../../lib/supabase';
import ConfirmationWizard from '../ConfirmationWizard';

interface SosAlertsSectionProps {
  db: DbState;
  activeSOSRecords: SOSRecord[];
  resolveSos: (sosRecordId: string) => void;
  formatDateTime: (isoString: string) => string;
  onInspectTrip?: (tripId: string) => void;
}

export default function SosAlertsSection({
  db,
  activeSOSRecords,
  resolveSos,
  formatDateTime,
  onInspectTrip
}: SosAlertsSectionProps) {
  const [subTab, setSubTab] = useState<'active' | 'all' | 'forensics'>('active');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Confirmation Wizard State
  const [sosPendingResolve, setSosPendingResolve] = useState<SOSRecord | null>(null);
  const [isResolving, setIsResolving] = useState(false);

  // Collect forensic trips that have sos_activity_logs
  const forensicTrips = useMemo(() => {
    return (db.trips || []).filter(t => {
      const hasLogs = t.sos_activity_logs && (
        Array.isArray(t.sos_activity_logs) 
          ? t.sos_activity_logs.length > 0 
          : typeof t.sos_activity_logs === 'string' && t.sos_activity_logs.length > 4
      );
      return hasLogs || t.status === 'sos' || t.sos_triggered_at;
    });
  }, [db.trips]);

  // Filtered SOS Records based on subTab
  const displayedRecords = useMemo(() => {
    let records = db.sosRecords || [];
    if (subTab === 'active') {
      records = records.filter(r => r.status === 'active');
    }

    if (!searchQuery.trim()) return records;

    const q = searchQuery.toLowerCase().trim();
    return records.filter(r => {
      const trip = db.trips.find(t => t.id === r.trip_id);
      const traveler = trip ? db.profiles.find(p => p.id === trip.user_id) : null;
      return (
        r.id.toLowerCase().includes(q) ||
        r.trip_id.toLowerCase().includes(q) ||
        (traveler?.full_name && traveler.full_name.toLowerCase().includes(q)) ||
        (traveler?.phone_number && traveler.phone_number.includes(q))
      );
    });
  }, [db.sosRecords, db.trips, db.profiles, subTab, searchQuery]);

  return (
    <div className="space-y-4 md:space-y-6 animate-fadeIn" id="sos-alerts-root">
      
      {/* Header section with Stats summary */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-200" id="sos-section-header">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-slate-800 flex items-center">
            <span className="material-icons text-red-600 mr-2 text-2xl animate-pulse">security</span>
            Emergency SOS Alerts
          </h1>
          <p className="text-slate-500 text-xs mt-0.5">
            Live emergency alerts, response actions, and SOS incident history.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {activeSOSRecords.length > 0 ? (
            <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-1.5 rounded-xl shadow-2xs flex items-center gap-2 text-xs font-mono font-bold animate-pulse">
              <span className="w-2.5 h-2.5 rounded-full bg-red-600"></span>
              <span>{activeSOSRecords.length} ACTIVE EMERGENCY ALERTS</span>
            </div>
          ) : (
            <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3 py-1.5 rounded-xl shadow-2xs flex items-center gap-2 text-xs font-mono font-bold">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-600"></span>
              <span>ALL CLEAR &bull; ZERO ACTIVE SOS</span>
            </div>
          )}
        </div>
      </div>

      {/* Filter and Sub-navigation Controls */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-2xs flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Sub Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {[
            { id: 'active', label: 'Active Alerts', count: activeSOSRecords.length, icon: 'warning', danger: true },
            { id: 'all', label: 'Incident History', count: db.sosRecords.length, icon: 'archive' },
            { id: 'forensics', label: 'SOS Timelines', count: forensicTrips.length, icon: 'manage_search' },
          ].map(tab => {
            const active = subTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setSubTab(tab.id as any)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                  active
                    ? tab.danger 
                      ? 'bg-red-600 text-white font-semibold shadow-xs' 
                      : 'bg-blue-600 text-white font-semibold shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-900'
                }`}
              >
                <span className="material-icons text-sm">{tab.icon}</span>
                <span>{tab.label}</span>
                {tab.count !== undefined && (
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
                    active 
                      ? 'bg-black/20 text-white' 
                      : tab.danger && tab.count > 0 
                      ? 'bg-red-100 text-red-700 font-bold' 
                      : 'bg-slate-200 text-slate-700'
                  }`}>
                    {tab.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Search */}
        <div className="relative w-full md:w-72">
          <span className="material-icons text-slate-400 absolute left-3 top-2.5 text-base">search</span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search incident, trip, passenger..."
            className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-8 py-1.5 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-red-500/30 focus:border-red-500 transition-all"
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

      {/* Main Content Area based on subTab */}
      {subTab === 'forensics' ? (
        /* Forensic Audit Trails View */
        <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-4">
          <div className="flex justify-between items-center">
            <div>
              <h3 className="font-mono text-xs font-bold uppercase text-slate-700">SOS Incident Timelines</h3>
              <p className="text-slate-400 text-[11px]">Trips with recorded SOS emergency alerts, safety check responses, and location logs.</p>
            </div>
            <span className="font-mono text-[10px] bg-slate-100 px-2 py-0.5 rounded text-slate-600">
              {forensicTrips.length} INCIDENT LOGS RECORDED
            </span>
          </div>

          {forensicTrips.length === 0 ? (
            <div className="py-12 text-center text-slate-400 font-mono text-xs">
              NO SOS INCIDENT LOGS FOUND
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {forensicTrips.map(trip => {
                const traveler = db.profiles.find(p => p.id === trip.user_id);
                const isSos = trip.status === 'sos';
                let parsedLogs: any[] = [];
                try {
                  parsedLogs = Array.isArray(trip.sos_activity_logs) 
                    ? trip.sos_activity_logs 
                    : typeof trip.sos_activity_logs === 'string' 
                    ? JSON.parse(trip.sos_activity_logs) 
                    : [];
                } catch {
                  parsedLogs = [];
                }

                return (
                  <div 
                    key={trip.id}
                    className={`p-4 rounded-xl border transition-all ${
                      isSos ? 'border-red-300 bg-red-50/20 ring-1 ring-red-400/30' : 'border-slate-200 bg-white'
                    }`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <div className="flex items-center gap-1.5">
                          <span className="font-bold text-xs text-slate-900">{traveler?.full_name || 'Passenger'}</span>
                          {trip.vehicle_plate_number && (
                            <span className="bg-slate-900 text-yellow-400 font-mono font-bold text-[9px] px-1.5 py-0.5 rounded">
                              {trip.vehicle_plate_number}
                            </span>
                          )}
                        </div>
                        <span className="font-mono text-[9.5px] text-blue-600 block mt-0.5">#{trip.id.slice(0, 12)}</span>
                      </div>

                      <span className={`text-[9.5px] font-mono font-bold uppercase px-2 py-0.5 rounded-full ${
                        isSos ? 'bg-red-100 text-red-700 border border-red-200 animate-pulse' : 'bg-slate-100 text-slate-600'
                      }`}>
                        {isSos ? 'ACTIVE SOS' : trip.status}
                      </span>
                    </div>

                    {/* Timeline items preview */}
                    <div className="bg-slate-50 p-2.5 rounded-lg border border-slate-100 space-y-1.5 my-2">
                      <span className="text-[9px] font-mono text-slate-400 uppercase block font-bold">Recent Timeline Events</span>
                      {parsedLogs.length === 0 ? (
                        <p className="text-[10.5px] text-slate-500 font-sans">SOS triggered at {trip.sos_triggered_at ? formatDateTime(trip.sos_triggered_at) : 'Trip start'}</p>
                      ) : (
                        parsedLogs.slice(0, 3).map((item, idx) => (
                          <div key={idx} className="flex items-center gap-1.5 text-[10px] text-slate-700">
                            <span className="w-1.5 h-1.5 rounded-full bg-rose-500 shrink-0"></span>
                            <span className="font-mono text-slate-500 font-semibold">{item.timestamp ? new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : `T+${idx}`}</span>
                            <span className="truncate">{item.event || item.details || item.response || 'Event logged'}</span>
                          </div>
                        ))
                      )}
                    </div>

                    <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                      <span className="text-[9.5px] font-mono text-slate-400">
                        {formatDateTime(trip.created_at)}
                      </span>
                      {onInspectTrip && (
                        <button
                          type="button"
                          onClick={() => onInspectTrip(trip.id)}
                          className="bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 text-[10px] font-mono font-bold px-2.5 py-1 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                        >
                          <span>INSPECT AUDIT TRAIL</span>
                          <span className="material-icons text-xs">arrow_forward</span>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : (
        /* Active & All Incident Logs Table */
        <div className="bg-white border border-slate-200 shadow-sm p-5 rounded-2xl space-y-4 text-slate-800" id="sos-feed-card">
          <div className="flex items-center justify-between">
            <h3 className="font-mono text-xs font-bold uppercase text-blue-650">
              {subTab === 'active' ? 'Active Crisis Queue' : 'Incident Master Database'}
            </h3>
            <div className="text-[10px] font-mono text-red-600 bg-red-50 px-2.5 py-0.5 rounded border border-red-200">
              {activeSOSRecords.length} ACTIVE INCIDENTS CURRENTLY RED
            </div>
          </div>

          <div className="hidden md:block overflow-x-auto border border-slate-200 rounded-lg">
            <table className="w-full text-left text-xs font-sans divide-y divide-slate-100 bg-white" id="sos-feed-table">
              <thead className="bg-slate-50 text-slate-550 font-mono text-[10px] uppercase tracking-wider">
                <tr>
                  <th className="px-3 py-2.5">Alert ID</th>
                  <th className="px-3 py-2.5">Trip & Vehicle</th>
                  <th className="px-3 py-2.5">Traveler</th>
                  <th className="px-3 py-2.5">Trigger Timestamp</th>
                  <th className="px-3 py-2.5">Status</th>
                  <th className="px-3 py-2.5 text-right">Relay Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {displayedRecords.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-3 py-8 text-center text-slate-400 font-mono bg-white">
                      {subTab === 'active' 
                        ? 'NO ACTIVE S.O.S INCIDENTS. SYSTEM HEALTHY.' 
                        : 'NO S.O.S TRIGGER LOGS FOUND IN DATABASE.'}
                    </td>
                  </tr>
                ) : (
                  displayedRecords.map(record => {
                    const trip = db.trips.find(t => t.id === record.trip_id);
                    const traveler = trip ? db.profiles.find(p => p.id === trip.user_id) : null;
                    const isPending = record.status === 'active';

                    return (
                      <tr key={record.id} className={`hover:bg-slate-50/50 transition-colors ${isPending ? 'bg-red-50/50' : ''}`} id={`sos-row-${record.id}`}>
                        <td className="px-3 py-3 font-mono text-slate-700 font-bold">{record.id}</td>
                        <td className="px-3 py-3">
                          <div className="font-mono text-blue-600 font-semibold">{record.trip_id}</div>
                          {trip?.vehicle_plate_number && (
                            <div className="flex items-center gap-1 mt-0.5">
                              <span className="bg-slate-900 text-yellow-400 font-mono text-[9px] px-1 py-0.2 rounded font-bold">
                                {trip.vehicle_plate_number}
                              </span>
                              {trip.vehicle_photo_url && (
                                <span className="material-icons text-cyan-600 text-xs" title="Vehicle Photo Available">photo_camera</span>
                              )}
                            </div>
                          )}
                        </td>
                        <td className="px-3 py-3">
                          <div className="font-semibold text-slate-800">{traveler?.full_name || 'System Test Unit'}</div>
                          <div className="text-[10px] text-slate-500 font-mono">{traveler?.phone_number || 'Sandbox Trigger'}</div>
                        </td>
                        <td className="px-3 py-3 font-mono text-slate-550">
                          {formatDateTime(record.triggered_at)}
                        </td>
                        <td className="px-3 py-3">
                          {isPending ? (
                            <span className="bg-red-100 text-red-655 border border-red-200 text-[9px] font-mono font-bold px-2.5 py-0.5 rounded-full uppercase animate-pulse">
                              ACTIVE CRISIS
                            </span>
                          ) : (
                            <span className="bg-slate-100 text-slate-500 border border-slate-205 text-[9px] font-mono px-2.5 py-0.5 rounded-full uppercase">
                              RESOLVED SAFELY
                            </span>
                          )}
                        </td>
                        <td className="px-3 py-3 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {onInspectTrip && (
                              <button
                                type="button"
                                onClick={() => onInspectTrip(record.trip_id)}
                                className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-mono text-[9px] font-bold uppercase tracking-wider px-2 py-1 rounded cursor-pointer transition-colors"
                                title="Inspect Trip Details"
                              >
                                INSPECT
                              </button>
                            )}

                            {isPending ? (
                              <button
                                type="button"
                                onClick={() => setSosPendingResolve(record)}
                                className="bg-emerald-600 hover:bg-emerald-500 text-white font-mono text-[9px] font-bold uppercase tracking-wider px-3 py-1 rounded cursor-pointer transition-colors"
                                id={`sos-close-btn-${record.id}`}
                              >
                                CLOSE FILE
                              </button>
                            ) : (
                              <span className="text-[10px] font-mono text-slate-450">CLOSED</span>
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

          {/* Mobile responsive view */}
          <div className="md:hidden space-y-3" id="sos-mobile-list">
            {displayedRecords.length === 0 ? (
              <div className="text-center py-8 text-slate-400 font-mono bg-white border border-slate-200 rounded-xl text-xs">
                {subTab === 'active' ? 'NO ACTIVE S.O.S INCIDENTS. SYSTEM HEALTHY.' : 'NO S.O.S TRIGGER LOGS FOUND.'}
              </div>
            ) : (
              displayedRecords.map(record => {
                const trip = db.trips.find(t => t.id === record.trip_id);
                const traveler = trip ? db.profiles.find(p => p.id === trip.user_id) : null;
                const isPending = record.status === 'active';

                return (
                  <div key={record.id} className={`p-4 rounded-xl border-2 shadow-sm space-y-3 text-slate-805 transition-all ${
                    isPending ? 'border-red-400 bg-red-50/30' : 'border-slate-200 bg-white'
                  }`} id={`sos-mobile-card-${record.id}`}>
                    <div className="flex justify-between items-start">
                      <div>
                        <span className="font-mono text-[10px] text-red-655 font-bold block">ID: {record.id}</span>
                        <span className="font-mono text-[9px] text-blue-600 font-semibold block mt-0.5">TRIP REF: {record.trip_id}</span>
                      </div>
                      <div>
                        {isPending ? (
                          <span className="bg-red-100 text-red-655 border border-red-200 text-[9px] font-mono font-bold px-2.5 py-0.5 rounded-full uppercase animate-pulse">
                            ACTIVE
                          </span>
                        ) : (
                          <span className="bg-slate-100 text-slate-500 border border-slate-200 text-[9px] font-mono px-2.5 py-0.5 rounded-full uppercase">
                            RESOLVED
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="pt-2 border-t border-slate-100 text-xs">
                      <p className="font-bold text-slate-855">{traveler?.full_name || 'System Test Unit'}</p>
                      <p className="text-[10px] text-slate-500 font-mono mt-0.5">{traveler?.phone_number || 'Sandbox Trigger'}</p>
                    </div>

                    <p className="text-[9.5px] font-mono text-slate-500 bg-slate-50 p-2 rounded">
                      Triggered: {formatDateTime(record.triggered_at)}
                    </p>

                    <div className="flex gap-2">
                      {onInspectTrip && (
                        <button
                          type="button"
                          onClick={() => onInspectTrip(record.trip_id)}
                          className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-mono text-[10px] font-bold uppercase py-2 rounded-lg cursor-pointer transition-colors text-center"
                        >
                          INSPECT
                        </button>
                      )}

                      {isPending && (
                        <button
                          type="button"
                          onClick={() => setSosPendingResolve(record)}
                          className="flex-1 bg-emerald-600 hover:bg-emerald-500 text-white font-mono text-[10px] font-bold uppercase py-2 rounded-lg cursor-pointer transition-colors text-center"
                          id={`sos-mobile-close-${record.id}`}
                        >
                          CLOSE
                        </button>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>

        </div>
      )}

      {/* SOS Resolution Confirmation Wizard */}
      <ConfirmationWizard
        isOpen={!!sosPendingResolve}
        onClose={() => setSosPendingResolve(null)}
        onConfirm={async () => {
          if (!sosPendingResolve) return;
          setIsResolving(true);
          try {
            await resolveSos(sosPendingResolve.id);
          } finally {
            setIsResolving(false);
            setSosPendingResolve(null);
          }
        }}
        isLoading={isResolving}
        variant="warning"
        icon="security"
        title="Resolve & Close SOS Emergency?"
        message={`Are you sure you want to resolve emergency record #${sosPendingResolve?.id?.substring(0, 8)} for trip #${sosPendingResolve?.trip_id?.substring(0, 8)}?`}
        consequences={[
          "The active distress alarm will be dismissed on all operational dispatch consoles.",
          "Incident record will be permanently marked as RESOLVED in the database.",
          "Please verify traveler safety status before completing this action."
        ]}
        confirmText="Yes, Resolve Incident"
        cancelText="No, Keep Active"
      />

    </div>
  );
}
