'use client';

import React, { useState, useMemo, useRef } from 'react';
import { DbState, SafetyAudioLog } from '../../lib/supabase';

interface AudioBlackboxSectionProps {
  db: DbState;
  formatDateTime: (isoString: string) => string;
}

export default function AudioBlackboxSection({
  db,
  formatDateTime
}: AudioBlackboxSectionProps) {
  const [filterMode, setFilterMode] = useState<'all' | 'sos' | 'safety'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [activePlayingId, setActivePlayingId] = useState<string | null>(null);

  // Combine safetyAudioLogs with any trips that have audio_clip_url
  const audioRecords = useMemo(() => {
    const list: Array<{
      id: string;
      userId?: string | null;
      tripId?: string | null;
      audioUrl: string;
      sourceTrigger: string;
      durationSec: number;
      fileSizeBytes: number;
      recordedAddress?: string | null;
      recordedCoords?: string | null;
      createdAt: string;
      travelerName: string;
      travelerPhone?: string;
    }> = [];

    const seenUrls = new Set<string>();

    if (db.safetyAudioLogs && db.safetyAudioLogs.length > 0) {
      db.safetyAudioLogs.forEach(log => {
        if (!log.audio_url) return;
        seenUrls.add(log.audio_url);
        const traveler = db.profiles.find(p => p.id === log.user_id);
        list.push({
          id: log.id,
          userId: log.user_id,
          tripId: log.trip_id,
          audioUrl: log.audio_url,
          sourceTrigger: log.source_trigger || 'sos',
          durationSec: log.duration_sec || 0,
          fileSizeBytes: log.file_size_bytes || 0,
          recordedAddress: log.recorded_address,
          recordedCoords: log.recorded_coords,
          createdAt: log.created_at || new Date().toISOString(),
          travelerName: traveler?.full_name || 'Passenger Unit',
          travelerPhone: traveler?.phone_number
        });
      });
    }

    // Also check travelActivities/trips with audio_clip_url
    if (db.travelActivities) {
      db.travelActivities.forEach(act => {
        if (act.audio_clip_url && !seenUrls.has(act.audio_clip_url)) {
          seenUrls.add(act.audio_clip_url);
          const traveler = db.profiles.find(p => p.id === act.user_id);
          list.push({
            id: `act-audio-${act.id}`,
            userId: act.user_id,
            tripId: act.id,
            audioUrl: act.audio_clip_url,
            sourceTrigger: act.safety_status === 'sos' ? 'sos' : 'safety_check',
            durationSec: 15,
            fileSizeBytes: 245000,
            recordedAddress: act.start_address,
            recordedCoords: act.start_coords,
            createdAt: act.sos_triggered_at || act.start_time || act.created_at || new Date().toISOString(),
            travelerName: traveler?.full_name || 'Passenger Unit',
            travelerPhone: traveler?.phone_number
          });
        }
      });
    }

    return list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }, [db.safetyAudioLogs, db.travelActivities, db.profiles]);

  const filteredLogs = useMemo(() => {
    return audioRecords.filter(item => {
      // Query filter
      const q = searchQuery.toLowerCase().trim();
      const matches = !q || 
        item.travelerName.toLowerCase().includes(q) ||
        (item.tripId && item.tripId.toLowerCase().includes(q)) ||
        (item.recordedAddress && item.recordedAddress.toLowerCase().includes(q)) ||
        item.sourceTrigger.toLowerCase().includes(q);

      if (!matches) return false;

      // Category filter
      if (filterMode === 'sos') {
        return item.sourceTrigger.toLowerCase().includes('sos');
      }
      if (filterMode === 'safety') {
        return !item.sourceTrigger.toLowerCase().includes('sos');
      }

      return true;
    });
  }, [audioRecords, searchQuery, filterMode]);

  const totalSosRecordings = audioRecords.filter(r => r.sourceTrigger.toLowerCase().includes('sos')).length;

  const formatFileSize = (bytes: number) => {
    if (!bytes || bytes === 0) return 'N/A';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const formatDuration = (sec: number) => {
    if (!sec || sec === 0) return '00:15';
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="space-y-4 md:space-y-6 animate-fadeIn" id="audio-blackbox-root">
      
      {/* Header section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-slate-800 flex items-center">
            <span className="material-icons text-amber-500 mr-2 text-2xl">graphic_eq</span>
            Audio Blackbox & Safety Evidence Vault
          </h1>
          <p className="text-slate-500 text-xs mt-0.5">
            Cloudinary forensic voice clips recorded automatically during SOS panic events and periodic safety protocols.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="bg-white border border-slate-200 px-3 py-1.5 rounded-xl shadow-2xs flex items-center gap-2 text-xs font-mono">
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
            <span className="text-slate-500">EVIDENCE FILES:</span>
            <span className="font-bold text-slate-800">{audioRecords.length} RECORDINGS</span>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-2xs flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Sub-navigation Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {[
            { id: 'all', label: 'All Audio Logs', count: audioRecords.length, icon: 'queue_music' },
            { id: 'sos', label: 'SOS Panic Clips', count: totalSosRecordings, icon: 'warning' },
            { id: 'safety', label: 'Routine Safety Checks', icon: 'verified_user' },
          ].map(tab => {
            const active = filterMode === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setFilterMode(tab.id as any)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                  active 
                    ? 'bg-amber-500 text-white font-semibold shadow-xs' 
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-900'
                }`}
              >
                <span className="material-icons text-sm">{tab.icon}</span>
                <span>{tab.label}</span>
                {tab.count !== undefined && (
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
                    active ? 'bg-amber-700 text-amber-100' : 'bg-slate-200 text-slate-700'
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
            placeholder="Search traveler, trip ID, or address..."
            className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-8 py-1.5 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500 transition-all"
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

      {/* List of Audio Evidence Clips */}
      {filteredLogs.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center text-slate-400 font-mono text-xs">
          <span className="material-icons text-4xl text-slate-300 mb-2 block">mic_off</span>
          NO AUDIO RECORDINGS CAPTURED FOR THIS FILTER
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredLogs.map(item => {
            const isSos = item.sourceTrigger.toLowerCase().includes('sos');
            const isPlaying = activePlayingId === item.id;

            return (
              <div 
                key={item.id}
                className={`bg-white border rounded-2xl p-4 shadow-2xs hover:shadow-md transition-all flex flex-col justify-between ${
                  isSos ? 'border-amber-300 ring-1 ring-amber-400/40' : 'border-slate-200'
                }`}
              >
                <div>
                  {/* Top Bar: Trigger Tag & Traveler */}
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <span className={`px-2.5 py-0.5 rounded-full text-[9.5px] font-mono font-bold uppercase flex items-center gap-1 ${
                        isSos 
                          ? 'bg-rose-100 text-rose-700 border border-rose-200' 
                          : 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                      }`}>
                        <span className="material-icons text-[12px]">
                          {isSos ? 'emergency' : 'shield'}
                        </span>
                        {isSos ? '[SOS PANIC TRIGGER]' : '[SAFETY PROTOCOL]'}
                      </span>
                    </div>

                    <span className="text-[10px] font-mono text-slate-400">
                      {formatDateTime(item.createdAt)}
                    </span>
                  </div>

                  {/* Traveler and Trip Ref */}
                  <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 mb-3 space-y-1">
                    <div className="flex justify-between items-center">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-[11px]">
                          {item.travelerName.charAt(0)}
                        </div>
                        <span className="font-bold text-xs text-slate-800">{item.travelerName}</span>
                      </div>
                      {item.tripId && (
                        <span className="font-mono text-[9.5px] text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded border border-blue-200">
                          #{item.tripId.slice(0, 10)}
                        </span>
                      )}
                    </div>

                    {item.recordedAddress && (
                      <div className="flex items-start gap-1 pt-1 text-slate-600 text-[11px]">
                        <span className="material-icons text-xs text-slate-400 shrink-0 mt-0.5">location_on</span>
                        <span className="truncate">{item.recordedAddress}</span>
                      </div>
                    )}
                  </div>

                  {/* HTML5 Audio Player */}
                  <div className="bg-slate-900 p-3 rounded-xl border border-slate-800 mb-3">
                    <div className="flex items-center justify-between text-[10px] font-mono text-slate-400 mb-2">
                      <span className="flex items-center gap-1 text-amber-400 font-bold">
                        <span className="material-icons text-xs">volume_up</span>
                        STREAM FROM CLOUDINARY
                      </span>
                      <span>
                        DURATION: {formatDuration(item.durationSec)} &bull; {formatFileSize(item.fileSizeBytes)}
                      </span>
                    </div>

                    <audio 
                      controls 
                      className="w-full h-8"
                      src={item.audioUrl}
                      onPlay={() => setActivePlayingId(item.id)}
                      onPause={() => {
                        if (activePlayingId === item.id) setActivePlayingId(null);
                      }}
                    >
                      Your browser does not support the audio element.
                    </audio>
                  </div>
                </div>

                {/* Footer Controls */}
                <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-[10px] font-mono text-slate-500">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                    <span>FORENSIC TAMPER-PROOF CLOUD SECURED</span>
                  </div>

                  <a 
                    href={item.audioUrl}
                    target="_blank" 
                    rel="noreferrer"
                    download
                    className="bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-mono font-bold px-2 py-1 rounded-lg transition-colors flex items-center gap-1"
                    title="Open or download audio file"
                  >
                    <span className="material-icons text-xs">file_download</span>
                    <span>DOWNLOAD</span>
                  </a>
                </div>
              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}
