'use client';

import React, { useState } from 'react';
import { Trip } from '../../lib/supabase';

export interface PublicTelemetry {
  battery?: number;
  phoneNumber?: string;
  lastUpdated?: string;
  isGpsLost?: boolean;
}

interface PublicTrackingHudProps {
  travelerName: string;
  activeTrip: Trip;
  startLocation: string;
  endLocation: string;
  isSimulating: boolean;
  isSimFinished: boolean;
  simSpeed: number;
  onSetSimSpeed: (speed: number) => void;
  onStartSim: () => void;
  onStopSim: () => void;
  onReplay: () => void;
  publicTelemetry?: PublicTelemetry;
  onRecenter?: () => void;
}

export default function PublicTrackingHud({
  travelerName,
  activeTrip,
  startLocation,
  endLocation,
  isSimulating,
  isSimFinished,
  simSpeed,
  onSetSimSpeed,
  onStartSim,
  onStopSim,
  onReplay,
  publicTelemetry,
  onRecenter,
}: PublicTrackingHudProps) {
  const [isMinimized, setIsMinimized] = useState(false);

  const cleanOrigin = startLocation.replace(/\[.*?\]|\(.*?\)/g, '').trim() || 'Unknown Origin';
  const cleanDest = endLocation.replace(/\[.*?\]|\(.*?\)/g, '').trim() || 'Unknown Destination';
  const battery = publicTelemetry?.battery ?? (activeTrip.end_battery_level != null ? Number(activeTrip.end_battery_level) : undefined);

  // Formatting trip start and end time
  const rawStartTime = activeTrip.start_time || activeTrip.created_at;
  const formattedStartTime = React.useMemo(() => {
    if (!rawStartTime) return '';
    try {
      const d = new Date(rawStartTime);
      if (isNaN(d.getTime())) return '';
      return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', hour12: true });
    } catch {
      return '';
    }
  }, [rawStartTime]);

  const rawEndTime = activeTrip.end_time;
  const formattedEndTime = React.useMemo(() => {
    if (!rawEndTime) return '';
    try {
      const d = new Date(rawEndTime);
      if (isNaN(d.getTime())) return '';
      return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', hour12: true });
    } catch {
      return '';
    }
  }, [rawEndTime]);

  // Trip state determination
  const isTripCompleted = activeTrip.status === 'completed' || isSimFinished;
  const isSos = activeTrip.status === 'sos';

  // Formatting traveler name & responsive typography per user specs
  const isLongName = travelerName.length > 15;
  const displayTravelerName = travelerName.length > 20 
    ? `${travelerName.slice(0, 20)}...` 
    : travelerName;

  // Unified persistent container with smooth CSS Grid height expansion (No DOM unmount or shape jumps)
  return (
    <div 
      className="fixed bottom-4 left-4 right-4 md:right-auto md:w-[460px] z-40 bg-slate-900/95 backdrop-blur-md border border-slate-800 text-white rounded-3xl shadow-2xl pointer-events-auto select-none overflow-hidden"
      style={{
        transition: 'padding 320ms cubic-bezier(0.16, 1, 0.3, 1), border-color 300ms ease',
        padding: isMinimized ? '10px 16px' : '16px',
      }}
    >
      {/* 1. Header Row (Always mounted, seamless transition) */}
      <div 
        className="flex items-center justify-between"
        style={{
          paddingBottom: isMinimized ? '0px' : '10px',
          borderBottom: isMinimized ? '1px solid transparent' : '1px solid rgba(30, 41, 59, 0.8)',
          transition: 'padding-bottom 300ms cubic-bezier(0.16, 1, 0.3, 1), border-color 300ms ease',
        }}
      >
        <div className="flex items-center space-x-2.5 min-w-0 flex-1 mr-2">
          <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 shadow-inner ${
            isSos 
              ? 'bg-red-600/20 border border-red-500/40 text-red-400 animate-pulse' 
              : isTripCompleted
              ? 'bg-cyan-600/15 border border-cyan-500/30 text-cyan-400'
              : 'bg-blue-600/15 border border-blue-500/30 text-blue-400'
          }`}>
            <span className="material-icons text-base">
              {isSos ? 'warning' : isTripCompleted ? 'flag' : 'satellite_alt'}
            </span>
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center space-x-2 min-w-0">
              <h3 
                className={`font-bold tracking-tight text-white leading-tight truncate ${isLongName ? 'text-xs' : 'text-sm'}`}
                title={`Live Tracking: ${travelerName}`}
              >
                <span className="hidden sm:inline">Live Tracking: </span>
                <span className="sm:hidden">Live: </span>
                {displayTravelerName}
              </h3>

              {/* In Minimized state: show compact status & battery inline */}
              <div 
                className="flex items-center space-x-1.5 shrink-0"
                style={{
                  opacity: isMinimized ? 1 : 0,
                  maxWidth: isMinimized ? '220px' : '0px',
                  overflow: 'hidden',
                  transition: 'opacity 250ms ease, max-width 320ms cubic-bezier(0.16, 1, 0.3, 1)',
                  pointerEvents: isMinimized ? 'auto' : 'none',
                }}
              >
                <span className="text-slate-600 shrink-0">•</span>
                <span className={`text-[10px] font-mono font-bold flex items-center shrink-0 ${
                  isSos
                    ? 'text-red-400'
                    : isTripCompleted
                    ? 'text-cyan-400'
                    : 'text-emerald-400'
                }`}>
                  <span className={`w-1.5 h-1.5 rounded-full mr-1 ${
                    isSos
                      ? 'bg-red-400 animate-pulse'
                      : isTripCompleted
                      ? 'bg-cyan-400'
                      : 'bg-emerald-400 animate-pulse'
                  }`}></span>
                  {isSos ? 'SOS Active' : isTripCompleted ? 'Completed' : 'In Transit'}
                </span>
                {battery !== undefined && (
                  <>
                    <span className="text-slate-600 shrink-0">•</span>
                    <span className={`text-[10px] font-mono font-bold flex items-center gap-0.5 shrink-0 ${
                      battery <= 15 ? 'text-red-400' : battery <= 50 ? 'text-amber-400' : 'text-emerald-400'
                    }`}>
                      <span className="material-icons text-[11px]">
                        {battery <= 15 ? 'battery_alert' : battery <= 50 ? 'battery_4_bar' : 'battery_full'}
                      </span>
                      {battery}%
                    </span>
                  </>
                )}
              </div>
            </div>

            {/* Subtitle (Transport mode / Completed status) - cleanly slides away when minimized */}
            <div
              style={{
                maxHeight: isMinimized ? '0px' : '20px',
                opacity: isMinimized ? 0 : 1,
                overflow: 'hidden',
                transition: 'max-height 250ms ease, opacity 200ms ease',
              }}
            >
              <p className={`text-[9px] font-mono uppercase tracking-wider mt-0.5 truncate ${
                isSos 
                  ? 'text-red-400 font-bold animate-pulse' 
                  : isTripCompleted 
                  ? 'text-cyan-400 font-bold' 
                  : 'text-slate-400'
              }`}>
                {isSos 
                  ? '[EMERGENCY ALERT ACTIVE]' 
                  : isTripCompleted 
                  ? 'TRIP COMPLETED' 
                  : (activeTrip.transport_mode ? `${activeTrip.transport_mode.toUpperCase()} MODE` : 'TRANSIT SIGNAL')}
              </p>
            </div>
          </div>
        </div>

        {/* Right Badges: GPS Recenter Button + Minimize Toggle Button */}
        <div className="flex items-center space-x-1.5 shrink-0">
          {/* GPS Recenter Button (In place of battery in Header row) */}
          {onRecenter && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onRecenter();
              }}
              className="w-7 h-7 rounded-full bg-slate-800/80 hover:bg-blue-600/30 border border-slate-700/60 hover:border-blue-500/50 text-blue-400 hover:text-blue-300 flex items-center justify-center transition-all cursor-pointer shadow-sm active:scale-90"
              title="Fit map to route / Recenter"
              aria-label="Fit map to route"
            >
              <span className="material-icons text-[15px]">my_location</span>
            </button>
          )}

          {/* Smooth Rotating Minimize/Maximize Button */}
          <button
            type="button"
            onClick={() => setIsMinimized(prev => !prev)}
            className="w-7 h-7 rounded-full bg-slate-800/80 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors cursor-pointer ml-0.5 shadow-sm"
            title={isMinimized ? 'Expand Details' : 'Minimize Card'}
          >
            <span 
              className="material-icons text-base transition-transform duration-300 ease-out"
              style={{
                transform: isMinimized ? 'rotate(180deg)' : 'rotate(0deg)'
              }}
            >
              keyboard_arrow_down
            </span>
          </button>
        </div>
      </div>

      {/* Collapsible Body using CSS Grid Template Rows (Smooth 60fps auto-height animation) */}
      <div
        style={{
          display: 'grid',
          gridTemplateRows: isMinimized ? '0fr' : '1fr',
          opacity: isMinimized ? 0 : 1,
          transition: 'grid-template-rows 320ms cubic-bezier(0.16, 1, 0.3, 1), opacity 260ms ease',
          pointerEvents: isMinimized ? 'none' : 'auto',
        }}
      >
        <div style={{ overflow: 'hidden', minHeight: 0 }}>
          {/* 2. Journey Route Flow (From -> To) */}
          <div className="pt-2.5 pb-2 flex flex-col space-y-2">
            {/* From Origin */}
            <div className="flex items-start space-x-2.5">
              <div className="flex flex-col items-center mt-0.5 shrink-0">
                <div className="w-4 h-4 rounded-full bg-blue-600/20 border-2 border-blue-500 flex items-center justify-center shadow-[0_0_8px_rgba(59,130,246,0.5)]">
                  <div className="w-1.5 h-1.5 rounded-full bg-blue-400"></div>
                </div>
                <div className="w-0.5 h-6 bg-gradient-to-b from-blue-500 via-slate-700 to-rose-500 my-0.5 opacity-60"></div>
              </div>
              <div className="min-w-0 flex-1">
                <span className="text-[9.5px] font-mono text-slate-400 uppercase tracking-wider block font-semibold">Pickup Location</span>
                <p className="text-xs font-medium text-slate-200 truncate leading-snug">{cleanOrigin}</p>
              </div>
            </div>

            {/* To Destination */}
            <div className="flex items-start space-x-2.5 -mt-1">
              <div className="w-4 h-4 rounded-full bg-rose-600/20 border-2 border-rose-500 flex items-center justify-center shadow-[0_0_8px_rgba(244,63,94,0.5)] mt-0.5 shrink-0">
                <div className="w-1.5 h-1.5 rounded-full bg-rose-400"></div>
              </div>
              <div className="min-w-0 flex-1">
                <span className="text-[9.5px] font-mono text-slate-400 uppercase tracking-wider block font-semibold">Destination</span>
                <p className="text-xs font-bold text-white truncate leading-snug">{cleanDest}</p>
              </div>
            </div>
          </div>

          {/* 3. Status & Telemetry Slot (Directly below destination, flat styled metrics) */}
          <div className="bg-slate-950/70 border border-slate-800/80 rounded-2xl p-2.5 mb-2.5 flex items-center justify-between gap-2">
            {/* When NOT completed: show Transit in progress on sm+ */}
            {!isTripCompleted && (
              <div 
                className="hidden sm:flex items-center space-x-1.5 shrink-0" 
                title={isSos ? 'Emergency Alert Active' : 'Trip in progress'}
              >
                <span className={`text-[11px] font-mono font-bold flex items-center ${
                  isSos ? 'text-red-400' : 'text-emerald-400'
                }`}>
                  <span className={`w-2.5 h-2.5 rounded-full shrink-0 mr-1.5 ${
                    isSos 
                      ? 'bg-red-400 animate-pulse shadow-[0_0_8px_#F87171]' 
                      : 'bg-emerald-400 animate-pulse shadow-[0_0_8px_#10B981]'
                  }`}></span>
                  <span className="truncate">
                    {isSos ? 'EMERGENCY SOS ACTIVE' : 'Trip in progress'}
                  </span>
                </span>
              </div>
            )}

            {/* Trip Start Time */}
            {formattedStartTime ? (
              <div className="flex items-center space-x-1.5 text-slate-300 font-mono text-[11px] font-semibold shrink-0">
                <span className="material-icons text-xs text-blue-400">schedule</span>
                <span>Started: {formattedStartTime}</span>
              </div>
            ) : null}

            {/* Trip End Time (Shown when completed and end_time exists) */}
            {isTripCompleted && formattedEndTime ? (
              <div className="flex items-center space-x-1.5 text-slate-300 font-mono text-[11px] font-semibold shrink-0">
                <span className="material-icons text-xs text-emerald-400">task_alt</span>
                <span>Ended: {formattedEndTime}</span>
              </div>
            ) : null}

            {/* Battery Level Badge (Moved to where GPS badge was) */}
            {battery !== undefined && (
              <div className={`px-2.5 py-0.5 rounded-lg border font-mono text-[10.5px] font-bold flex items-center gap-1.5 shadow-sm whitespace-nowrap shrink-0 ${
                battery <= 15 ? 'bg-red-500/15 border-red-500/30 text-red-400 animate-pulse' :
                battery <= 50 ? 'bg-amber-500/15 border-amber-500/30 text-amber-400' :
                'bg-emerald-500/15 border-emerald-500/30 text-emerald-400'
              }`}>
                <span className="material-icons text-xs">
                  {battery <= 15 ? 'battery_alert' : battery <= 50 ? 'battery_4_bar' : 'battery_full'}
                </span>
                <span>{battery}%</span>
              </div>
            )}
          </div>

          {/* Urgent SOS Crisis Banner */}
          {isSos && (
            <div className="bg-red-500/20 border border-red-500/40 p-2.5 rounded-2xl mb-2.5 flex items-center justify-between gap-2 animate-pulse">
              <div className="flex items-center gap-2 min-w-0">
                <span className="material-icons text-red-400 text-lg shrink-0">emergency</span>
                <div className="min-w-0">
                  <span className="text-xs font-bold text-red-200 block truncate">SOS PANIC ENGAGED</span>
                  <span className="text-[10px] text-red-300/80 font-mono block truncate">Immediate responder assistance advised</span>
                </div>
              </div>
              <a 
                href="tel:999" 
                className="bg-red-600 hover:bg-red-700 text-white font-mono text-[10.5px] font-bold px-3 py-1.5 rounded-xl flex items-center gap-1 shrink-0 shadow-lg cursor-pointer transition-colors"
              >
                <span className="material-icons text-xs">call</span>
                CALL 999
              </a>
            </div>
          )}

          {/* Vehicle Verification Card */}
          {(activeTrip.vehicle_plate_number || activeTrip.vehicle_photo_url || activeTrip.vehicle_description) && (
            <div className="bg-slate-950/70 border border-slate-800/80 rounded-2xl p-2.5 mb-2.5 flex items-center justify-between gap-2.5 text-xs">
              <div className="flex items-center gap-2.5 min-w-0">
                {activeTrip.vehicle_photo_url ? (
                  <img 
                    src={activeTrip.vehicle_photo_url} 
                    alt="Vehicle evidence" 
                    className="w-12 h-10 object-cover rounded-lg border border-slate-700 shrink-0"
                  />
                ) : (
                  <div className="w-10 h-10 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center shrink-0 text-slate-400">
                    <span className="material-icons text-lg">directions_car</span>
                  </div>
                )}
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    {activeTrip.vehicle_plate_number && (
                      <span className="bg-slate-900 text-yellow-400 font-mono font-bold text-[10px] px-1.5 py-0.5 rounded border border-slate-700">
                        {activeTrip.vehicle_plate_number}
                      </span>
                    )}
                    <span className="font-semibold text-slate-200 text-xs truncate">
                      {activeTrip.vehicle_description || activeTrip.transport_mode || 'Vehicle'}
                    </span>
                  </div>
                  <span className="text-[10px] font-mono text-cyan-400 flex items-center gap-1 mt-0.5">
                    <span className="material-icons text-[11px]">verified</span>
                    Traveler Verified Vehicle
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* 4. Controls & Action Bar */}
          <div className="flex items-center justify-between pt-1 border-t border-slate-800/80">
            {/* Simulation Controls */}
            <div className="flex items-center space-x-1.5">
              <button
                type="button"
                onClick={() => {
                  const next = simSpeed === 1 ? 2 : (simSpeed === 2 ? 5 : 1);
                  onSetSimSpeed(next);
                }}
                className="bg-slate-950 border border-slate-800 text-[9.5px] font-mono text-slate-300 hover:text-white px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center space-x-1 shadow-sm"
                title="Change route speed multiplier"
              >
                <span className="material-icons text-[11px] text-blue-400">speed</span>
                <span>SPEED {simSpeed}x</span>
              </button>

              {isTripCompleted ? (
                <>
                  {/* Replay / Restart from beginning */}
                  <button
                    type="button"
                    onClick={onReplay}
                    className="bg-blue-600/20 border border-blue-400 text-blue-300 hover:bg-blue-600/30 text-[9.5px] font-mono px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center space-x-1 shadow-sm font-semibold active:scale-95"
                    title="Restart replay from beginning"
                  >
                    <span className="material-icons text-[11px]">replay</span>
                    <span>REPLAY</span>
                  </button>

                  {/* Play / Pause Toggle Button (Replaces Live GPS on completed trips) */}
                  <button
                    type="button"
                    onClick={isSimulating && !isSimFinished ? onStopSim : (isSimFinished ? onReplay : onStartSim)}
                    className={`border text-[9.5px] font-mono px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center space-x-1 font-semibold shadow-sm active:scale-95 ${
                      isSimulating && !isSimFinished
                        ? 'bg-amber-500/15 border-amber-500/40 text-amber-300 hover:bg-amber-500/25'
                        : 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300 hover:bg-emerald-500/25'
                    }`}
                    title={isSimulating && !isSimFinished ? 'Pause route playback' : 'Play / Resume route playback'}
                  >
                    <span className="material-icons text-[12px]">
                      {isSimulating && !isSimFinished ? 'pause' : 'play_arrow'}
                    </span>
                    <span>{isSimulating && !isSimFinished ? 'PAUSE' : 'PLAY'}</span>
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  onClick={isSimulating ? onStopSim : onStartSim}
                  className={`border text-[9.5px] font-mono px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center space-x-1 shadow-sm ${
                    isSimulating
                      ? 'bg-amber-500/10 border-amber-500/30 text-amber-400 hover:bg-amber-500/20'
                      : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20'
                  }`}
                  title={isSimulating ? 'Switch back to Live device GPS' : 'Simulate route progression'}
                >
                  <span className="material-icons text-[11px]">{isSimulating ? 'stop_circle' : 'play_circle'}</span>
                  <span>{isSimulating ? 'SIM ACTIVE' : 'LIVE GPS'}</span>
                </button>
              )}
            </div>

            {/* Emergency Call Button with Tooltip */}
            {publicTelemetry?.phoneNumber && !publicTelemetry.phoneNumber.includes('Protected') ? (
              <a
                href={`tel:${publicTelemetry.phoneNumber}`}
                className="flex items-center space-x-1.5 bg-emerald-600 hover:bg-emerald-500 text-white px-3 py-1.5 rounded-xl font-mono text-xs font-bold transition-all shadow-[0_0_15px_rgba(5,150,105,0.4)] active:scale-95"
                title={`Call ${travelerName}`}
              >
                <span className="material-icons text-sm">call</span>
                <span>Call</span>
              </a>
            ) : (
              <div 
                className="flex items-center space-x-1.5 bg-slate-800 text-slate-400 px-3 py-1.5 rounded-xl font-mono text-xs cursor-not-allowed opacity-75"
                title="Contact number protected for privacy"
              >
                <span className="material-icons text-sm">call</span>
                <span>Call</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
