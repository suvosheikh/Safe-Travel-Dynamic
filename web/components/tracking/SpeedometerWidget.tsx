'use client';

import React from 'react';

interface SpeedometerWidgetProps {
  currentDisplaySpeedKmH: number;
  speedLimit?: number;
  isSimulating: boolean;
  isSimFinished: boolean;
  simSpeed: number;
  onCycleSpeedLimit?: () => void;
  showSpeedLimit?: boolean;
  className?: string;
}

export default function SpeedometerWidget({
  currentDisplaySpeedKmH,
  speedLimit = 30,
  isSimulating,
  isSimFinished,
  simSpeed,
  onCycleSpeedLimit,
  showSpeedLimit = false,
  className = 'fixed top-48 md:top-44 left-4 z-40 flex flex-col items-center space-y-2 select-none pointer-events-auto',
}: SpeedometerWidgetProps) {
  return (
    <div className={className}>
      {/* Digital Speedometer Gauge HUD */}
      <div className="bg-slate-900/95 border border-slate-750 text-white rounded-2xl px-3 py-2 shadow-2xl backdrop-blur-md flex flex-col items-center min-w-[70px]">
        <div className="flex items-center space-x-1 text-[9px] font-mono text-cyan-400 font-semibold mb-0.5">
          <span className="material-icons text-[11px]">speed</span>
          <span>{isSimulating ? (isSimFinished ? 'ARRIVED' : `REPLAY ${simSpeed}x`) : 'LIVE'}</span>
        </div>
        <span className={`text-2xl font-black font-mono tracking-tight leading-none ${
          isSimFinished ? 'text-cyan-400' : (currentDisplaySpeedKmH > 0 ? 'text-emerald-400' : 'text-slate-400')
        }`}>
          {currentDisplaySpeedKmH}
        </span>
        <span className="text-[8.5px] font-mono font-bold tracking-wider text-slate-400 mt-1 uppercase">
          KM/H
        </span>
      </div>

      {/* Road Speed Limit Badge (EU/Waze Style) - Hidden by default unless showSpeedLimit is true */}
      {showSpeedLimit && speedLimit !== undefined && onCycleSpeedLimit && (
        <div 
          onClick={onCycleSpeedLimit}
          className="bg-white border-2 border-rose-600 text-slate-950 rounded-full w-10 h-10 shadow-lg flex flex-col items-center justify-center cursor-pointer hover:scale-105 active:scale-95 transition-transform"
          title="Speed limit for this road segment (Click to cycle)"
        >
          <span className="text-[6.5px] font-black uppercase text-rose-600 leading-none">MAX</span>
          <span className="text-xs font-black tracking-tighter leading-none">{speedLimit}</span>
        </div>
      )}
    </div>
  );
}
