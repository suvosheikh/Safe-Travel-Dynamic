'use client';

import React from 'react';
import { DbState } from '../../lib/supabase';

interface TravelerAppSectionProps {
  db: DbState;
  simulatedUserId: string;
  setSimulatedUserId: (val: string) => void;
  simTransitStart: string;
  setSimTransitStart: (val: string) => void;
  simTransitEnd: string;
  setSimTransitEnd: (val: string) => void;
  simBookingSuccess: boolean;
  handleSimulatePassengerBooking: () => void;
  handleSimulatePassengerSOS: () => void;
}

export default function TravelerAppSection({
  db,
  simulatedUserId,
  setSimulatedUserId,
  simTransitStart,
  setSimTransitStart,
  simTransitEnd,
  setSimTransitEnd,
  simBookingSuccess,
  handleSimulatePassengerBooking,
  handleSimulatePassengerSOS
}: TravelerAppSectionProps) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8" id="traveler-app-root">
      
      {/* Simulation sidebar controls left (span 4) */}
      <div className="lg:col-span-4 bg-white border border-slate-205 shadow-sm p-5 rounded-2xl space-y-4 flex flex-col justify-between text-slate-800" id="simulate-control-card">
        <div className="space-y-4">
          
          <div className="flex items-center space-x-2 pb-2 border-b border-slate-100">
            <span className="material-icons text-blue-600">smartphone</span>
            <h3 className="font-mono text-xs font-bold uppercase tracking-wider text-slate-705">
              Simulate Device Context
            </h3>
          </div>

          <p className="text-slate-500 text-xs">
            Toggle which passenger profile is currently holding the simulated smart device. 
            Any transit orders or SOS panic alerts dispatched from the mock UI will belong to this user.
          </p>

          <div className="space-y-1.5 text-xs">
            <label className="text-[10px] font-mono text-slate-400 uppercase">Interactive Device Owner</label>
            <select 
              className="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-blue-600 font-sans bg-white"
              value={simulatedUserId}
              onChange={(e) => setSimulatedUserId(e.target.value)}
              id="device-owner-select"
            >
              {db.profiles.map(p => (
                <option key={p.id} value={p.id}>
                  {p.full_name} ({p.phone_number})
                </option>
              ))}
            </select>
          </div>

        </div>

        <div className="bg-blue-50 border border-blue-150 p-4 rounded-xl text-[10.5px] font-mono leading-relaxed mt-6 text-slate-655 space-y-1">
          <span className="font-bold text-blue-700 uppercase">REAL-TIME SIGNAL PROPAGATION:</span>
          <p>
            This interactive mock simulates standard React Native websocket or pub/sub clients. 
            When dispatch is clicked, rows populate instantly in the central live monitors.
          </p>
        </div>

      </div>

      {/* iPhone Device mock layout right (span 8) */}
      <div className="lg:col-span-8 flex justify-center py-4" id="iphone-mockup-wrapper">
        
        {/* Visual iOS hardware frame */}
        <div className="w-[310px] h-[610px] bg-slate-950 rounded-[48px] p-3.5 shadow-2xl ring-12 ring-slate-900 border border-slate-850 relative flex flex-col justify-between shrink-0" id="iphone-hardware-container">
          
          {/* iOS camera dynamic island notch pill */}
          <div className="absolute top-6 left-1/2 -translate-x-1/2 w-28 h-5 bg-black rounded-full z-45 flex items-center justify-center space-x-1 border border-slate-900">
            <div className="w-1.5 h-1.5 rounded-full bg-slate-900 animate-pulse" />
            <div className="w-4 h-1 bg-slate-900 rounded-full" />
          </div>

          {/* Simulated phone operating screen content scroll container */}
          <div className="bg-slate-100 flex-1 rounded-[34px] overflow-hidden flex flex-col relative" id="iphone-screen-contents">
            
            {/* iOS operational top status bar context */}
            <div className="bg-slate-900 text-white h-10 px-6 pt-5 flex items-center justify-between shrink-0 font-sans font-medium text-[9px] select-none text-slate-200">
              <span>9:41 AM</span>
              <div className="flex items-center space-x-1.5 text-[10px]">
                <span className="material-icons text-[10px] leading-none">signal_cellular_alt</span>
                <span className="material-icons text-[10px] leading-none">wifi</span>
                <span className="material-icons text-[11px] leading-none">battery_full</span>
              </div>
            </div>

            {/* Simulated smartphone content space */}
            <div className="flex-1 overflow-y-auto px-4 py-3.5 space-y-3.5 scrolling-touch" id="iphone-scrollable-app">
              
              {/* Dynamic Header client select profile header */}
              {(() => {
                const profile = db.profiles.find(p => p.id === simulatedUserId) || db.profiles[0];
                return (
                  <div className="bg-white rounded-2xl p-3 flex items-center space-x-2 border border-slate-250 shadow-sm">
                    <div className="h-6 w-6 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 border border-blue-200 shrink-0">
                      <span className="material-icons text-[13px] leading-none">account_circle</span>
                    </div>
                    <div className="flex-1 min-w-0 font-sans">
                      <span className="text-[7.5px] font-mono text-slate-400 block tracking-wider font-bold uppercase leading-none mb-0.5">Device Identity</span>
                      <h2 className="text-[10px] font-bold text-slate-800 leading-none truncate">
                        {profile?.full_name || 'Passenger Traveler'}
                      </h2>
                    </div>
                  </div>
                );
              })()}

              {/* DYNAMIC PROMO FLYER CONTAINER */}
              {(() => {
                const activeB = db.banners.find(b => b.is_active);
                const imgUrl = activeB ? activeB.image_url : 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&h=675&q=80';
                const bannerLabel = activeB ? activeB.title : 'Exclusive Safety Offer';
                const actionUrl = activeB?.action_url || null;

                const FlyerContent = (
                  <div className="relative w-full aspect-[16/9] rounded-2xl overflow-hidden shadow-md border border-slate-200/90 group shrink-0">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img 
                      src={imgUrl} 
                      alt="Mobile App Promo Banner" 
                      className="w-full h-full object-cover transition-transform duration-300"
                      onError={(e) => {
                        e.currentTarget.src = 'https://images.unsplash.com/photo-1506015391300-4802dc74de2e?auto=format&fit=crop&w=1200&h=675&q=80';
                      }}
                      referrerPolicy="no-referrer"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-slate-900/10 to-transparent flex flex-col justify-end p-3 font-sans">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-[7.5px] font-bold tracking-widest uppercase bg-amber-500 text-slate-950 mb-1 w-max">
                        Promotional Event
                      </span>
                      <h3 className="text-[11px] font-bold text-white tracking-tight truncate leading-tight">
                        {bannerLabel}
                      </h3>
                      <p className="text-[8px] text-slate-300 leading-none mt-0.5">
                        Dynamic campaign synchronizer active.
                      </p>
                    </div>
                  </div>
                );

                if (actionUrl) {
                  return (
                    <a href={actionUrl} target="_blank" rel="noopener noreferrer" className="block w-full">
                      {FlyerContent}
                    </a>
                  );
                }

                return FlyerContent;
              })()}

              {/* Simulated Traveler wallet summary */}
              {(() => {
                const profile = db.profiles.find(p => p.id === simulatedUserId) || db.profiles[0];
                return (
                  <div className="bg-slate-905 bg-slate-900 rounded-2xl p-3 text-white flex items-center justify-between shadow-sm border border-slate-800">
                    <div className="space-y-0.5">
                      <span className="text-[8px] font-mono text-slate-400 uppercase tracking-widest block font-bold leading-none">Rider Account Balances</span>
                      <p className="text-[11px] font-bold text-emerald-400 font-mono mt-1">
                        <span className="material-icons text-[10px] mr-1 align-middle leading-none">workspace_premium</span>
                        {profile?.points_balance || 0} Safety Points
                      </p>
                    </div>
                    <div className="text-right">
                      <span className="text-[8px] font-mono text-slate-400 block font-bold leading-none mb-0.5">CREDITS</span>
                      <p className="text-[12px] font-extrabold text-blue-400 font-mono">
                        {profile?.trip_credits || 0} Rides
                      </p>
                    </div>
                  </div>
                );
              })()}

              {/* Sim Passenger Ride Booker form Section */}
              <div className="bg-white border border-slate-250 p-3 rounded-2xl space-y-2.5 shadow-sm">
                <span className="text-[9px] font-mono font-bold tracking-wider uppercase text-blue-600 block">Simulate Secure Transit</span>
                
                <div className="space-y-1.5 text-xs">
                  <div>
                    <label className="block text-[8.5px] font-semibold text-slate-400 uppercase">From Location</label>
                    <input 
                      type="text" 
                      value={simTransitStart}
                      onChange={(e) => setSimTransitStart(e.target.value)}
                      placeholder="Departure Address"
                      className="w-full bg-slate-50 border border-slate-200 text-[10.5px] px-2 py-1.5 rounded-lg outline-none font-sans text-slate-800 focus:border-blue-500 bg-white"
                      id="sim-transit-start-input"
                    />
                  </div>
                  <div>
                    <label className="block text-[8.5px] font-semibold text-slate-400 uppercase">To Location</label>
                    <input 
                      type="text" 
                      value={simTransitEnd}
                      onChange={(e) => setSimTransitEnd(e.target.value)}
                      placeholder="Destination Address"
                      className="w-full bg-slate-50 border border-slate-200 text-[10.5px] px-2 py-1.5 rounded-lg outline-none font-sans text-slate-800 focus:border-blue-500 bg-white"
                      id="sim-transit-end-input"
                    />
                  </div>
                </div>

                {/* Booking success overlay animation */}
                {simBookingSuccess && (
                  <div className="text-[9.5px] font-mono font-bold text-center text-emerald-600 bg-emerald-50 border border-emerald-100 p-2 rounded-lg animate-pulse" id="sim-success-alert">
                    Transit Requested Successfully!
                    <span className="block text-[8px] text-slate-450 mt-0.5 font-normal">Propagated instantly to Monitor Console dashboard.</span>
                  </div>
                )}

                <button
                  type="button"
                  onClick={handleSimulatePassengerBooking}
                  disabled={!simTransitStart.trim() || !simTransitEnd.trim()}
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white font-mono text-[9.5px] font-bold uppercase tracking-wider py-2.5 rounded-xl cursor-pointer transition-colors shadow-sm inline-flex items-center justify-center space-x-1"
                  id="sim-book-btn"
                >
                  <span className="material-icons text-xs leading-none">hail</span>
                  <span>Dispatch Secure Ride</span>
                </button>
              </div>

              {/* Trigger Instant SOS emergency panic beacon */}
              <div className="bg-red-50 border border-red-150 p-3 rounded-2xl space-y-2 text-center shadow-sm">
                <span className="text-[8.5px] font-mono font-bold uppercase tracking-wider text-red-600 block">Panic Emergency Panel</span>
                <p className="text-[8.5px] text-slate-500 leading-tight">
                  Triggering silent beacon instantly flags Dispatch Center & map coordinates.
                </p>
                <button
                  type="button"
                  onClick={handleSimulatePassengerSOS}
                  className="w-full bg-red-600 hover:bg-red-700 text-white font-mono text-[9.5px] font-bold uppercase tracking-wider py-2.5 rounded-xl cursor-pointer transition-all border border-red-700 hover:scale-[1.01] shadow-md flex items-center justify-center space-x-1.5"
                  id="sim-sos-btn"
                >
                  <span className="material-icons text-sm leading-none animate-pulse">gpp_maybe</span>
                  <span>Broadcast Silent SOS Alert</span>
                </button>
              </div>

            </div>

            {/* Mock home gesture indicator bar */}
            <div className="bg-transparent h-4 pb-2 flex items-center justify-center shrink-0">
              <div className="w-24 h-1 bg-slate-400 rounded-full" />
            </div>

          </div>

        </div>

      </div>

    </div>
  );
}
