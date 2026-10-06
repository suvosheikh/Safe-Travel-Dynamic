'use client';

import React, { useState, useMemo, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { DbState, TravelActivity, Profile, Guardian, getSupabaseClient } from '../../lib/supabase';
import dynamic from 'next/dynamic';
import ConfirmationWizard from '../ConfirmationWizard';

const TripDetailsModal = dynamic(() => import('../TripDetailsModal'), { ssr: false });

interface TravelerRegistrySectionProps {
  db: DbState;
  newProfileName: string;
  setNewProfileName: (val: string) => void;
  newProfilePhone: string;
  setNewProfilePhone: (val: string) => void;
  newProfileRole: 'admin' | 'dispatcher' | 'user';
  setNewProfileRole: (val: 'admin' | 'dispatcher' | 'user') => void;
  handleAddProfile: (e: React.FormEvent) => void;
  adjustCredits: (userId: string, amount: number) => void;
  awardPoints: (userId: string, points: number, reason: string) => void;
  togglePremium: (userId: string) => void;
}

export default function TravelerRegistrySection({
  db,
  newProfileName,
  setNewProfileName,
  newProfilePhone,
  setNewProfilePhone,
  newProfileRole,
  setNewProfileRole,
  handleAddProfile,
  adjustCredits,
  awardPoints,
  togglePremium
}: TravelerRegistrySectionProps) {
  const router = useRouter();
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [selectedTripDetails, setSelectedTripDetails] = useState<TravelActivity | null>(null);
  
  // Filtering & Search
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<'all' | 'sos' | 'ongoing' | 'offline' | 'low_battery'>('all');

  // Close drawer on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setSelectedUserId(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Compute Enriched User Profiles
  const enrichedProfiles = useMemo(() => {
    return db.profiles.map(profile => {
      // Find all activities for this user and sort by newest first
      const userActivities = db.travelActivities
        .filter(a => a.user_id === profile.id)
        .sort((a, b) => new Date((b.start_time || b.created_at) as string).getTime() - new Date((a.start_time || a.created_at) as string).getTime());
        
      const latestActivity = userActivities.length > 0 ? userActivities[0] : null;
      
      let liveStatus: 'sos' | 'ongoing' | 'offline' = 'offline';
      let battery = 100;
      let gpsLost = false;
      let lastLocation = 'No recorded trip history';
      let hasLowBattery = false;

      if (latestActivity) {
        const isSos = db.sosRecords.some(s => s.trip_id === latestActivity.id && s.status === 'active') || latestActivity.safety_status === 'sos';
        
        if (isSos) {
          liveStatus = 'sos';
        } else if (latestActivity.safety_status === 'ongoing' || !latestActivity.end_time) {
          liveStatus = 'ongoing';
        }
        
        battery = latestActivity.end_battery_level || latestActivity.start_battery_level || 100;
        gpsLost = Boolean(latestActivity.is_gps_lost);
        lastLocation = latestActivity.end_address || latestActivity.start_address || 'Last GPS Coordinate';
        
        if (battery <= 20) {
          hasLowBattery = true;
        }
      }

      return {
        ...profile,
        latestActivity,
        liveStatus,
        battery,
        gpsLost,
        lastLocation,
        hasLowBattery,
        userActivities
      };
    });
  }, [db]);

  // Filter counts
  const filterCounts = useMemo(() => {
    return {
      all: enrichedProfiles.length,
      sos: enrichedProfiles.filter(p => p.liveStatus === 'sos').length,
      ongoing: enrichedProfiles.filter(p => p.liveStatus === 'ongoing').length,
      low_battery: enrichedProfiles.filter(p => p.hasLowBattery).length,
      offline: enrichedProfiles.filter(p => p.liveStatus === 'offline').length,
    };
  }, [enrichedProfiles]);

  // Apply filters
  const filteredProfiles = useMemo(() => {
    return enrichedProfiles.filter(p => {
      const searchMatch = p.full_name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          (p.phone_number && p.phone_number.includes(searchQuery));
      if (!searchMatch) return false;

      if (activeFilter === 'sos') return p.liveStatus === 'sos';
      if (activeFilter === 'ongoing') return p.liveStatus === 'ongoing';
      if (activeFilter === 'offline') return p.liveStatus === 'offline';
      if (activeFilter === 'low_battery') return p.hasLowBattery;

      return true;
    });
  }, [enrichedProfiles, searchQuery, activeFilter]);

  const selectedUser = enrichedProfiles.find(p => p.id === selectedUserId);

  // Optimistic local state for immediately added/deleted guardians
  const [localAddedGuardians, setLocalAddedGuardians] = useState<Guardian[]>([]);

  // Selected user's real guardians from DB + optimistic local state
  const selectedUserGuardians = useMemo(() => {
    if (!selectedUser) return [];
    const fromDb = (db.guardians || []).filter(g => g.user_id === selectedUser.id);
    const fromLocal = localAddedGuardians.filter(
      g => g.user_id === selectedUser.id && !fromDb.some(dbG => dbG.id === g.id)
    );
    return [...fromDb, ...fromLocal];
  }, [db.guardians, selectedUser, localAddedGuardians]);

  // Guardian Management Form State
  const [isAddingGuardian, setIsAddingGuardian] = useState(false);
  const [guardianName, setGuardianName] = useState('');
  const [guardianRelation, setGuardianRelation] = useState('Parent');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [guardianDefaultNotify, setGuardianDefaultNotify] = useState(true);
  const [isSubmittingGuardian, setIsSubmittingGuardian] = useState(false);
  const [guardianActionError, setGuardianActionError] = useState<string | null>(null);

  // Reset form when selected user changes
  useEffect(() => {
    setIsAddingGuardian(false);
    setGuardianActionError(null);
    setGuardianName('');
    setGuardianPhone('');
  }, [selectedUserId]);

  const handleSaveGuardian = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser || !guardianName.trim() || !guardianPhone.trim()) return;
    setIsSubmittingGuardian(true);
    setGuardianActionError(null);
    try {
      const supabase = getSupabaseClient();
      if (!supabase) {
        throw new Error('Supabase database connection not ready');
      }
      const { data, error } = await supabase.from('guardians').insert({
        user_id: selectedUser.id,
        name: guardianName.trim(),
        relationship: guardianRelation.trim(),
        phone: guardianPhone.trim(),
        default_notify: guardianDefaultNotify,
        sos_permission: true
      }).select();

      if (error) throw error;

      if (data && data[0]) {
        setLocalAddedGuardians(prev => [data[0] as Guardian, ...prev]);
      }

      setGuardianName('');
      setGuardianPhone('');
      setGuardianRelation('Parent');
      setGuardianDefaultNotify(true);
      setIsAddingGuardian(false);
    } catch (err: any) {
      console.error('Failed to add guardian:', err);
      setGuardianActionError(err.message || 'Failed to save guardian to database');
    } finally {
      setIsSubmittingGuardian(false);
    }
  };

  // Guardian Deletion Confirmation Wizard State
  const [guardianPendingDelete, setGuardianPendingDelete] = useState<Guardian | null>(null);
  const [isDeletingGuardian, setIsDeletingGuardian] = useState(false);

  const handleDeleteGuardian = async (guardianId: string) => {
    setIsDeletingGuardian(true);
    try {
      const supabase = getSupabaseClient();
      if (!supabase) return;
      await supabase.from('guardians').delete().eq('id', guardianId);
      setLocalAddedGuardians(prev => prev.filter(g => g.id !== guardianId));
    } catch (err) {
      console.error('Failed to delete guardian:', err);
    } finally {
      setIsDeletingGuardian(false);
      setGuardianPendingDelete(null);
    }
  };

  return (
    <div className="flex flex-col h-full space-y-3" id="users-module-root">
      
      {/* 1. TOP BAR / FILTERS (Compact & Responsive) */}
      <div className="bg-white p-3 sm:p-3.5 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row justify-between items-center gap-3">
        {/* Search Bar */}
        <div className="relative w-full md:w-80">
          <span className="material-icons absolute left-3 top-2 text-slate-400 text-lg">search</span>
          <input 
            type="text" 
            placeholder="Search travelers by name, phone..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-8 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-all text-slate-800"
          />
          {searchQuery && (
            <button 
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-2 text-slate-400 hover:text-slate-600 text-xs"
              title="Clear search"
            >
              <span className="material-icons text-sm">close</span>
            </button>
          )}
        </div>

        {/* Filter Pills with Counts */}
        <div className="flex flex-wrap items-center gap-1.5 w-full md:w-auto">
          <button 
            onClick={() => setActiveFilter('all')}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all border flex items-center gap-1.5 ${
              activeFilter === 'all' 
                ? 'bg-slate-800 text-white border-slate-800 shadow-sm' 
                : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
            }`}
          >
            All Users <span className="opacity-75 text-[10px] font-mono">({filterCounts.all})</span>
          </button>
          
          <button 
            onClick={() => setActiveFilter('sos')}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all border flex items-center gap-1.5 ${
              activeFilter === 'sos' 
                ? 'bg-red-600 text-white border-red-600 shadow-sm' 
                : 'bg-white text-red-600 border-red-200 hover:bg-red-50'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-current animate-ping"></span>
            SOS Active <span className="opacity-80 text-[10px] font-mono">({filterCounts.sos})</span>
          </button>
          
          <button 
            onClick={() => setActiveFilter('ongoing')}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all border flex items-center gap-1.5 ${
              activeFilter === 'ongoing' 
                ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm' 
                : 'bg-white text-emerald-600 border-emerald-200 hover:bg-emerald-50'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-current"></span>
            On Trip <span className="opacity-80 text-[10px] font-mono">({filterCounts.ongoing})</span>
          </button>
          
          <button 
            onClick={() => setActiveFilter('low_battery')}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all border flex items-center gap-1.5 ${
              activeFilter === 'low_battery' 
                ? 'bg-amber-600 text-white border-amber-600 shadow-sm' 
                : 'bg-white text-amber-600 border-amber-200 hover:bg-amber-50'
            }`}
          >
            <span className="material-icons text-[13px]">battery_alert</span>
            Low Battery <span className="opacity-80 text-[10px] font-mono">({filterCounts.low_battery})</span>
          </button>
          
          <button 
            onClick={() => setActiveFilter('offline')}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all border flex items-center gap-1.5 ${
              activeFilter === 'offline' 
                ? 'bg-slate-600 text-white border-slate-600 shadow-sm' 
                : 'bg-white text-slate-500 border-slate-200 hover:bg-slate-50'
            }`}
          >
            Offline <span className="opacity-75 text-[10px] font-mono">({filterCounts.offline})</span>
          </button>
        </div>
      </div>

      {/* 2. MAIN USER TABLE CONTAINER (Always full-width to prevent column cutoff on HD laptops) */}
      <div className="bg-white border border-slate-200 shadow-sm rounded-xl overflow-hidden flex-1 flex flex-col min-h-0">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left text-xs whitespace-nowrap">
            <thead className="bg-slate-50/90 border-b border-slate-200 text-slate-500 text-[11px] uppercase tracking-wider font-semibold sticky top-0 z-10 backdrop-blur-sm">
              <tr>
                <th className="px-3.5 py-2.5">User Info</th>
                <th className="px-3 py-2.5">Live Status</th>
                <th className="px-3 py-2.5">Last Known Location</th>
                <th className="px-3 py-2.5">Device & Network</th>
                <th className="px-3.5 py-2.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredProfiles.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-10 text-slate-400 font-mono text-xs">
                    No travelers match the current search or filters.
                  </td>
                </tr>
              ) : (
                filteredProfiles.map(profile => {
                  const isSelected = selectedUserId === profile.id;
                  return (
                    <tr 
                      key={profile.id}
                      onClick={() => setSelectedUserId(profile.id)}
                      className={`cursor-pointer transition-colors ${
                        isSelected 
                          ? 'bg-blue-50/80 border-l-4 border-blue-600' 
                          : 'hover:bg-slate-50/80'
                      }`}
                    >
                      {/* USER INFO */}
                      <td className="px-3.5 py-2.5">
                        <div className="flex items-center gap-2.5">
                          <img 
                            src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(profile.full_name)}&backgroundColor=b6e3f4`} 
                            alt="Avatar" 
                            className="w-8 h-8 rounded-full border border-slate-200 shrink-0 bg-white" 
                          />
                          <div>
                            <div className="font-bold text-slate-800 text-xs flex items-center gap-1.5">
                              {profile.full_name}
                              {profile.is_premium && (
                                <span className="text-[9px] font-bold text-amber-600 bg-amber-50 border border-amber-200 px-1 rounded uppercase">
                                  PRO
                                </span>
                              )}
                            </div>
                            <div className="text-[10.5px] text-slate-500 font-mono">{profile.phone_number || 'No phone'}</div>
                          </div>
                        </div>
                      </td>

                      {/* LIVE STATUS */}
                      <td className="px-3 py-2.5">
                        {profile.liveStatus === 'sos' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-red-100 text-red-700 text-[10.5px] font-bold border border-red-200 animate-pulse">
                            <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span> SOS Active
                          </span>
                        )}
                        {profile.liveStatus === 'ongoing' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-[10.5px] font-bold border border-emerald-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse"></span> On Trip
                          </span>
                        )}
                        {profile.liveStatus === 'offline' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[10.5px] font-medium border border-slate-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-400"></span> Offline
                          </span>
                        )}
                      </td>

                      {/* LAST KNOWN LOCATION */}
                      <td className="px-3 py-2.5 text-xs text-slate-600 max-w-[200px] xl:max-w-[300px] truncate" title={profile.lastLocation}>
                        <div className="flex items-center gap-1">
                          <span className="material-icons text-[14px] text-slate-400 shrink-0">place</span>
                          <span className="truncate">{profile.lastLocation}</span>
                        </div>
                      </td>

                      {/* DEVICE & NETWORK */}
                      <td className="px-3 py-2.5">
                        <div className="flex items-center gap-3 text-xs font-mono">
                          <div className={`flex items-center gap-1 font-bold ${
                            profile.battery <= 20 ? 'text-red-500' : profile.battery <= 50 ? 'text-amber-500' : 'text-emerald-600'
                          }`}>
                            <span className="material-icons text-[14px]">
                              {profile.battery <= 20 ? 'battery_alert' : profile.battery < 100 ? 'battery_4_bar' : 'battery_full'}
                            </span>
                            {profile.battery}%
                          </div>
                          <div className={`flex items-center gap-1 ${profile.gpsLost ? 'text-red-500 font-bold' : 'text-slate-400'}`}>
                            <span className="material-icons text-[14px]" title={profile.gpsLost ? 'GPS Signal Lost' : 'Signal OK'}>
                              {profile.gpsLost ? 'signal_cellular_connected_no_internet_0_bar' : 'signal_cellular_4_bar'}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* ACTIONS (Always clearly visible and responsive) */}
                      <td className="px-3.5 py-2.5 text-right">
                        <button 
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedUserId(profile.id);
                          }}
                          className="inline-flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50/60 hover:bg-blue-100 border border-blue-200/60 px-2 py-1 rounded-md transition-colors"
                          title="View Traveler Details & Trip History"
                        >
                          <span>Profile</span>
                          <span className="material-icons text-[14px]">arrow_forward</span>
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 3. USER DETAILS SLIDE-OVER DRAWER (Sleek flyout panel that never compresses the table) */}
      {selectedUser && (
        <>
          {/* Backdrop Scrim */}
          <div 
            className="fixed inset-0 bg-slate-950/40 backdrop-blur-[2px] z-50 transition-opacity duration-200 animate-in fade-in"
            onClick={() => setSelectedUserId(null)}
          />

          {/* Slide-over Sheet */}
          <aside 
            className="fixed inset-y-0 right-0 z-50 w-full sm:w-[420px] md:w-[450px] bg-white shadow-2xl flex flex-col border-l border-slate-200 animate-in slide-in-from-right duration-300 ease-out"
            role="dialog"
            aria-label="User Profile Drawer"
          >
            {/* Drawer Header */}
            <div className="px-4 py-3 bg-slate-900 text-white flex justify-between items-center shrink-0 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <span className="material-icons text-emerald-400 text-lg">shield_person</span>
                <div>
                  <h2 className="font-bold text-sm tracking-wide leading-tight">Traveler Profile</h2>
                  <p className="text-[10px] text-slate-400 font-mono">ID: {selectedUser.id}</p>
                </div>
              </div>
              
              <div className="flex items-center gap-2">
                {selectedUser.liveStatus === 'sos' && (
                  <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-red-500/20 text-red-400 border border-red-500/30">
                    SOS Active
                  </span>
                )}
                {selectedUser.liveStatus === 'ongoing' && (
                  <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                    On Trip
                  </span>
                )}
                {selectedUser.liveStatus === 'offline' && (
                  <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                    Offline
                  </span>
                )}
                <button 
                  onClick={() => setSelectedUserId(null)} 
                  className="text-slate-400 hover:text-white bg-white/10 hover:bg-white/20 p-1 rounded-lg transition-colors"
                  title="Close Drawer (Esc)"
                >
                  <span className="material-icons text-lg leading-none">close</span>
                </button>
              </div>
            </div>

            {/* Drawer Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4 text-slate-800">
              
              {/* Top Hero Card */}
              <div className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 flex items-start gap-3.5">
                <img 
                  src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(selectedUser.full_name)}&backgroundColor=b6e3f4`} 
                  alt="Avatar" 
                  className="w-14 h-14 rounded-full border-2 border-white shadow-sm bg-white shrink-0" 
                />
                <div className="flex-1 min-w-0">
                  <h3 className="text-base font-bold text-slate-900 truncate leading-tight">{selectedUser.full_name}</h3>
                  <p className="text-xs text-slate-500 font-mono mt-0.5 mb-2">{selectedUser.phone_number || 'No contact number'}</p>
                  
                  <div className="flex flex-wrap gap-1.5">
                    <span className="text-[10px] font-mono font-bold px-2 py-0.5 bg-blue-50 border border-blue-200 text-blue-700 rounded">
                      POINTS: <span className="font-extrabold">{selectedUser.points_balance ?? 0}</span>
                    </span>
                    <button 
                      onClick={() => togglePremium(selectedUser.id)}
                      className={`text-[10px] uppercase tracking-wider font-bold px-2 py-0.5 border rounded transition-colors ${
                        selectedUser.is_premium 
                          ? 'bg-amber-100 border-amber-300 text-amber-800' 
                          : 'bg-slate-100 border-slate-200 text-slate-600 hover:bg-slate-200'
                      }`}
                      title="Click to toggle premium tier"
                    >
                      {selectedUser.is_premium ? '★ VIP Premium' : 'Standard Tier'}
                    </button>
                  </div>
                </div>
              </div>

              {/* Personal & Safety Telemetry Card */}
              <div className="bg-white border border-slate-200 rounded-xl p-3 space-y-2 text-xs">
                <h4 className="text-[10.5px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                  <span className="material-icons text-sm text-blue-500">contact_emergency</span> Personal Information
                </h4>
                <div className="grid grid-cols-2 gap-2 pt-1 font-mono text-[11px]">
                  <div className="bg-slate-50 p-2 rounded border border-slate-100">
                    <span className="block text-[9.5px] uppercase text-slate-400 font-sans">Role</span>
                    <span className="font-bold uppercase text-slate-800">{selectedUser.role}</span>
                  </div>
                  <div className="bg-slate-50 p-2 rounded border border-slate-100">
                    <span className="block text-[9.5px] uppercase text-slate-400 font-sans">Trip Credits</span>
                    <span className="font-bold text-slate-800">{selectedUser.trip_credits ?? 0} Credits</span>
                  </div>
                  <div className="bg-slate-50 p-2 rounded border border-slate-100">
                    <span className="block text-[9.5px] uppercase text-slate-400 font-sans">Battery Status</span>
                    <span className="font-bold text-slate-800">{selectedUser.battery}% ({selectedUser.gpsLost ? 'GPS Lost' : 'GPS Active'})</span>
                  </div>
                  <div className="bg-slate-50 p-2 rounded border border-slate-100">
                    <span className="block text-[9.5px] uppercase text-slate-400 font-sans">Total Trips</span>
                    <span className="font-bold text-slate-800">{selectedUser.userActivities.length} journeys</span>
                  </div>
                </div>
              </div>

              {/* Trusted Guardians Section */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <h4 className="text-[10.5px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                    <span className="material-icons text-sm text-indigo-500">group</span> Trusted Circle & Guardians
                    <span className="ml-1 px-1.5 py-0.5 bg-indigo-50 text-indigo-600 rounded-full font-mono text-[9.5px]">
                      {selectedUserGuardians.length}
                    </span>
                  </h4>
                  <button
                    type="button"
                    onClick={() => { setIsAddingGuardian(!isAddingGuardian); setGuardianActionError(null); }}
                    className="text-[10.5px] font-medium text-indigo-600 hover:text-indigo-700 bg-indigo-50 hover:bg-indigo-100 px-2 py-0.5 rounded transition-colors flex items-center gap-1"
                  >
                    <span className="material-icons text-[13px]">{isAddingGuardian ? 'close' : 'person_add'}</span>
                    <span>{isAddingGuardian ? 'Cancel' : 'Add Guardian'}</span>
                  </button>
                </div>

                {/* Add Guardian Form */}
                {isAddingGuardian && (
                  <form onSubmit={handleSaveGuardian} className="bg-slate-50 border border-indigo-100 rounded-xl p-3 space-y-2.5 shadow-xs">
                    <div className="text-[11.5px] font-semibold text-slate-700 flex items-center gap-1.5">
                      <span className="material-icons text-sm text-indigo-600">add_moderator</span>
                      Register Guardian to Database
                    </div>
                    {guardianActionError && (
                      <div className="text-[11px] text-red-600 bg-red-50 p-2 rounded border border-red-200">
                        {guardianActionError}
                      </div>
                    )}
                    <div className="space-y-1.5">
                      <input
                        type="text"
                        placeholder="Guardian Full Name"
                        value={guardianName}
                        onChange={e => setGuardianName(e.target.value)}
                        required
                        className="w-full text-xs px-2.5 py-1.5 rounded-lg border border-slate-200 focus:outline-none focus:border-indigo-500 bg-white"
                      />
                      <div className="grid grid-cols-2 gap-2">
                        <select
                          value={guardianRelation}
                          onChange={e => setGuardianRelation(e.target.value)}
                          className="text-xs px-2 py-1.5 rounded-lg border border-slate-200 focus:outline-none focus:border-indigo-500 bg-white"
                        >
                          <option value="Father">Father</option>
                          <option value="Mother">Mother</option>
                          <option value="Spouse">Spouse</option>
                          <option value="Sibling">Sibling</option>
                          <option value="Friend">Friend</option>
                          <option value="Emergency Contact">Emergency Contact</option>
                        </select>
                        <input
                          type="text"
                          placeholder="Phone (+880...)"
                          value={guardianPhone}
                          onChange={e => setGuardianPhone(e.target.value)}
                          required
                          className="text-xs px-2.5 py-1.5 rounded-lg border border-slate-200 focus:outline-none focus:border-indigo-500 bg-white font-mono"
                        />
                      </div>
                      <label className="flex items-center gap-2 text-[11px] text-slate-600 pt-1 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={guardianDefaultNotify}
                          onChange={e => setGuardianDefaultNotify(e.target.checked)}
                          className="rounded border-slate-300 text-indigo-600 focus:ring-0"
                        />
                        <span>Auto-notify during SOS emergency</span>
                      </label>
                    </div>
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        type="button"
                        onClick={() => setIsAddingGuardian(false)}
                        className="text-[11px] px-2.5 py-1 text-slate-500 hover:text-slate-700"
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        disabled={isSubmittingGuardian}
                        className="text-[11px] px-3 py-1 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm disabled:opacity-50 flex items-center gap-1"
                      >
                        {isSubmittingGuardian ? (
                          <span>Saving...</span>
                        ) : (
                          <>
                            <span className="material-icons text-xs">save</span>
                            <span>Save to Database</span>
                          </>
                        )}
                      </button>
                    </div>
                  </form>
                )}

                {/* Real Guardians List or Honest Empty State */}
                {selectedUserGuardians.length === 0 ? (
                  <div className="bg-slate-50 border border-dashed border-slate-200 rounded-xl p-3.5 text-center space-y-1">
                    <div className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                      <span className="material-icons text-base">group_off</span>
                    </div>
                    <p className="text-xs font-semibold text-slate-600">No Trusted Guardians Registered</p>
                    <p className="text-[10.5px] text-slate-400">
                      This traveler has not configured emergency contacts in the database.
                    </p>
                  </div>
                ) : (
                  <div className="bg-slate-50 border border-slate-200 rounded-xl p-2.5 space-y-1.5">
                    {selectedUserGuardians.map(guardian => {
                      const initials = (guardian.name || guardian.relationship || 'G')
                        .split(' ')
                        .map((n: string) => n[0])
                        .slice(0, 2)
                        .join('')
                        .toUpperCase();

                      return (
                        <div
                          key={guardian.id}
                          className="flex justify-between items-center text-xs bg-white p-2.5 rounded-lg border border-slate-200/80 hover:border-slate-300 transition-colors"
                        >
                          <div className="flex items-center gap-2.5">
                            <span className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-[9.5px] font-bold">
                              {initials}
                            </span>
                            <div>
                              <div className="flex items-center gap-1.5">
                                <span className="font-semibold text-slate-800">{guardian.name}</span>
                                <span className="text-[9.5px] px-1.5 py-0.2 rounded bg-slate-100 text-slate-500 font-sans">
                                  {guardian.relationship || guardian.relation || 'Contact'}
                                </span>
                                {guardian.default_notify && (
                                  <span className="text-[9px] px-1.5 py-0.2 rounded bg-indigo-50 text-indigo-600 font-semibold">
                                    Primary
                                  </span>
                                )}
                              </div>
                              <div className="text-[11px] text-slate-500 font-mono flex items-center gap-1 mt-0.5">
                                <span className="material-icons text-[11px] text-slate-400">phone</span>
                                {guardian.phone}
                              </div>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => setGuardianPendingDelete(guardian)}
                            title="Remove guardian from database"
                            className="text-slate-300 hover:text-red-500 p-1.5 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
                          >
                            <span className="material-icons text-sm">delete_outline</span>
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Recent Trip History (Clickable cards that open TripDetailsModal) */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <h4 className="text-[10.5px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                    <span className="material-icons text-sm text-blue-500">route</span> Recent Trip History
                  </h4>
                  <span className="text-[10px] font-mono text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">
                    Click to replay route
                  </span>
                </div>
                
                <div className="space-y-2">
                  {selectedUser.userActivities.length === 0 ? (
                    <div className="text-center text-slate-400 font-mono text-xs py-5 uppercase tracking-wider border border-dashed border-slate-200 rounded-xl bg-slate-50/50">
                      No recorded travel logs
                    </div>
                  ) : (
                    selectedUser.userActivities.slice(0, 5).map(trip => (
                      <div 
                        key={trip.id} 
                        onClick={() => setSelectedTripDetails(trip)}
                        className="p-2.5 bg-white border border-slate-200 rounded-xl hover:border-blue-500 hover:shadow-md hover:bg-blue-50/20 transition-all cursor-pointer group relative"
                      >
                        <div className="flex justify-between items-start mb-1">
                          <span className="font-bold text-xs text-slate-800 flex items-center gap-1.5">
                            <span className="material-icons text-sm text-slate-400 group-hover:text-blue-600 transition-colors">
                              {trip.transport_mode === 'walking' ? 'directions_walk' : trip.transport_mode === 'cycling' ? 'pedal_bike' : 'directions_car'}
                            </span>
                            <span className="capitalize">{trip.transport_mode || 'Transit'}</span>
                          </span>
                          <span className="text-[9.5px] font-mono bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded border border-slate-200 group-hover:bg-blue-100 group-hover:text-blue-700 transition-colors">
                            {new Date((trip as any).start_time || (trip as any).created_at || new Date()).toLocaleDateString()}
                          </span>
                        </div>
                        
                        <div className="text-[11px] text-slate-500 space-y-0.5 pl-1 border-l-2 border-slate-200 group-hover:border-blue-400 transition-colors my-1">
                          <div className="flex items-center gap-1.5 truncate">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 shrink-0"></span>
                            <span className="truncate">{trip.start_address || 'Origin'}</span>
                          </div>
                          <div className="flex items-center gap-1.5 truncate">
                            <span className="w-1.5 h-1.5 rounded-full bg-rose-500 shrink-0"></span>
                            <span className="truncate">{trip.end_address || 'Destination'}</span>
                          </div>
                        </div>

                        <div className="mt-1 pt-1 border-t border-slate-100 flex justify-between items-center text-[10px] text-blue-600 font-medium">
                          <span>View Speed & Interactive Route</span>
                          <span className="material-icons text-sm group-hover:translate-x-0.5 transition-transform">chevron_right</span>
                        </div>
                      </div>
                    ))
                  )}
                </div>

                {/* View All Travel History Button */}
                {selectedUser.userActivities.length > 0 ? (
                  <button
                    type="button"
                    onClick={() => router.push(`/dashboard/users/${selectedUser.id}/trips`)}
                    className="w-full mt-2 py-2.5 px-3 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all shadow-xs group cursor-pointer"
                  >
                    <span className="material-icons text-sm group-hover:translate-x-0.5 transition-transform">travel_explore</span>
                    <span>View All Travel History ({selectedUser.userActivities.length} Trips)</span>
                    <span className="material-icons text-sm">arrow_forward</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => router.push(`/dashboard/users/${selectedUser.id}/trips`)}
                    className="w-full mt-2 py-2 px-3 bg-slate-50 hover:bg-slate-100 text-slate-600 border border-slate-200 rounded-xl text-xs font-medium flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <span className="material-icons text-sm">history</span>
                    <span>Open Travel Vault</span>
                    <span className="material-icons text-sm">chevron_right</span>
                  </button>
                )}
              </div>

            </div>

            {/* Drawer Footer */}
            <div className="px-4 py-3 bg-slate-50 border-t border-slate-200 flex justify-between items-center shrink-0">
              <span className="text-[10.5px] font-mono text-slate-400">
                SafeTravel Control Unit
              </span>
              <button 
                onClick={() => setSelectedUserId(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-xs font-bold transition-colors"
              >
                Close Drawer
              </button>
            </div>
          </aside>
        </>
      )}

      {/* 4. TRIP DETAILS MODAL (Completely responsive for laptops & mobile) */}
      {selectedTripDetails && (
        <TripDetailsModal 
          trip={selectedTripDetails} 
          profiles={db.profiles} 
          onClose={() => setSelectedTripDetails(null)} 
        />
      )}

      {/* 5. GUARDIAN DELETE CONFIRMATION WIZARD */}
      <ConfirmationWizard
        isOpen={!!guardianPendingDelete}
        onClose={() => setGuardianPendingDelete(null)}
        onConfirm={() => {
          if (guardianPendingDelete) {
            handleDeleteGuardian(guardianPendingDelete.id);
          }
        }}
        isLoading={isDeletingGuardian}
        variant="danger"
        icon="delete_forever"
        title="Remove Trusted Guardian?"
        message={`Are you sure you want to permanently remove ${guardianPendingDelete?.name || 'this guardian'} (${guardianPendingDelete?.relationship || guardianPendingDelete?.relation || 'Contact'}) from the trusted circle of ${selectedUser?.full_name || 'this traveler'}?`}
        consequences={[
          "This contact will immediately stop receiving SOS emergency alerts and SMS notifications.",
          "Their phone number and permissions will be permanently deleted from the database.",
          "This action takes effect immediately across all dispatch monitors."
        ]}
        confirmText="Yes, Delete Guardian"
        cancelText="No, Keep Guardian"
      />

    </div>
  );
}
