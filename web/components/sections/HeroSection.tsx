'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { useLanguage } from '@/components/LanguageProvider';

export default function HeroSection() {
  const { t } = useLanguage();
  const router = useRouter();
  const [trackCode, setTrackCode] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showQrModal, setShowQrModal] = useState(false);

  const handleTrackSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!trackCode.trim()) return;
    setIsSubmitting(true);
    router.push(`/track/${trackCode.trim()}`);
  };

  const handleDemoClick = (code: string) => {
    setTrackCode(code);
    setIsSubmitting(true);
    router.push(`/track/${code}`);
  };

  return (
    <main className="relative pt-24 pb-12 md:pt-32 md:pb-20 px-4 md:px-8 lg:px-12 flex flex-col items-center justify-center text-center z-10">
      
      {/* 1. TOP LIVE SECURITY TELEMETRY BADGE */}
      <motion.div 
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-full bg-emerald-950/50 border border-emerald-500/30 mb-6 md:mb-8 backdrop-blur-md shadow-[0_0_20px_rgba(16,185,129,0.15)]"
      >
        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse shadow-[0_0_10px_#34d399]"></span>
        <span className="text-[10px] md:text-xs font-mono text-emerald-300 tracking-wider uppercase font-bold">
          {t('LIVE PASSENGER SAFETY &bull; REAL-TIME LOCATION SHARING', 'যাত্রীদের লাইভ নিরাপত্তা &bull; রিয়েল-টাইম লোকেশন ট্র্যাকিং')}
        </span>
      </motion.div>

      {/* 2. MAIN VALUE PROPOSITION HEADLINE */}
      <motion.h1 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
        className="text-4xl sm:text-5xl md:text-7xl lg:text-7.5xl font-black tracking-tight max-w-5xl leading-[1.1] mb-5 md:mb-7 text-white"
      >
        {t('Travel anywhere in confidence.', 'যেখানেই যান নিশ্চিন্তে থাকুন।')} <br className="hidden sm:block"/>
        <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-cyan-400 to-blue-500">
          {t('Never travel alone.', 'কখনো একা অনুভব করবেন না।')}
        </span>
      </motion.h1>
      
      {/* 3. SUBTITLE SPECIFYING REAL TRANSIT PROTECTION */}
      <motion.p 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.2 }}
        className="text-sm sm:text-base md:text-xl text-slate-300 max-w-3xl mb-8 md:mb-10 font-normal leading-relaxed px-2"
      >
        {t(
          'Taking a late-night ride, taxi, or bus? Snap your vehicle photo before boarding, share your live location with family via WhatsApp, and stay protected with instant 2-second SOS and emergency audio recording.',
          'সিএনজি, বাস বা ট্যাক্সি নিয়ে ভ্রমণ করছেন? গাড়ির ছবি তুলুন, পরিবারের সাথে হোয়াটসঅ্যাপে লাইভ লোকেশন শেয়ার করুন এবং বিপদের সময় ২-সেকেন্ডের এসওএস ও জরুরি অডিও রেকর্ডিংয়ে থাকুন নিরাপদ।'
        )}
      </motion.p>

      {/* 4. INSTANT GUARDIAN TRACKING SEARCH CARD */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.25 }}
        className="w-full max-w-2xl mb-10 md:mb-14 relative z-20"
      >
        <div className="bg-slate-900/90 border border-slate-700/80 rounded-2xl md:rounded-3xl p-3 sm:p-4 shadow-2xl backdrop-blur-2xl ring-1 ring-white/10 hover:border-cyan-500/50 transition-all">
          <form onSubmit={handleTrackSubmit} className="flex flex-col sm:flex-row items-center gap-2.5">
            <div className="relative flex-1 w-full">
              <span className="material-icons text-cyan-400 absolute left-3.5 top-3 text-xl">radar</span>
              <input 
                type="text" 
                value={trackCode}
                onChange={(e) => setTrackCode(e.target.value)}
                placeholder={t('Enter 6-digit Trip Code (e.g. TRK-9X4-B2)...', '৬-ডিজিটের ট্র্যাকিং কোড দিন (যেমন TRK-9X4-B2)...')}
                className="w-full bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-xs sm:text-sm font-mono rounded-xl pl-11 pr-4 py-3 focus:outline-none focus:ring-2 focus:ring-cyan-500/40 focus:border-cyan-500 uppercase tracking-wider"
              />
            </div>

            <button 
              type="submit"
              disabled={isSubmitting}
              className="w-full sm:w-auto px-6 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs sm:text-sm uppercase tracking-wider flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(34,211,238,0.3)] cursor-pointer transition-all hover:scale-102 shrink-0 disabled:opacity-50"
            >
              <span className="font-mono font-extrabold">{isSubmitting ? t('CONNECTING...', 'যুক্ত হচ্ছে...') : t('TRACK JOURNEY', 'লাইভ দেখুন')}</span>
              <span className="material-icons text-base">arrow_forward</span>
            </button>
          </form>

          {/* Quick Demo Pill & Zero-Install Assurance */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-2 mt-3 pt-3 border-t border-slate-800/80 text-[11px] font-mono text-slate-400">
            <div className="flex items-center gap-1.5 flex-wrap justify-center sm:justify-start">
              <span>{t('Try Sample Code:', 'ডেমো ট্রিপ দেখুন:')}</span>
              <button
                type="button"
                onClick={() => handleDemoClick('TRK-9X4-B2')}
                className="bg-slate-800 hover:bg-slate-700 text-cyan-300 border border-slate-700 hover:border-cyan-500/50 px-2 py-0.5 rounded font-bold transition-colors cursor-pointer"
              >
                #TRK-9X4-B2
              </button>
            </div>

            <div className="flex items-center gap-1 text-emerald-400">
              <span className="material-icons text-xs">verified_user</span>
              <span>{t('No App Required For Guardians &bull; Browser Live View', 'অভিভাবকদের অ্যাপ লাগবে না &bull; ব্রাউজারেই সরাসরি লাইভ')}</span>
            </div>
          </div>
        </div>
      </motion.div>

      {/* 5. APP DOWNLOAD BUTTONS & QR SCANNER TOGGLE */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.3 }}
        className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4 w-full sm:w-auto mb-12 md:mb-16"
        id="download"
      >
        <button 
          type="button"
          onClick={() => setShowQrModal(true)}
          className="w-full sm:w-auto px-6 py-3 md:px-7 md:py-3.5 rounded-xl bg-slate-900 border border-slate-700 hover:border-slate-500 text-white font-bold text-sm hover:bg-slate-850 transition-all flex items-center justify-center gap-3 shadow-lg cursor-pointer group"
        >
          <span className="material-icons text-emerald-400 text-2xl group-hover:scale-110 transition-transform">android</span>
          <div className="flex flex-col items-start leading-none text-left">
            <span className="text-[9px] font-mono font-semibold text-slate-400 uppercase tracking-wider">{t('AVAILABLE FOR ANDROID', 'অ্যান্ড্রয়েডের জন্য')}</span>
            <span className="text-base tracking-tight font-extrabold text-white">Google Play APK</span>
          </div>
        </button>

        <button 
          type="button"
          onClick={() => setShowQrModal(true)}
          className="w-full sm:w-auto px-6 py-3 md:px-7 md:py-3.5 rounded-xl bg-slate-900 border border-slate-700 hover:border-slate-500 text-white font-bold text-sm hover:bg-slate-850 transition-all flex items-center justify-center gap-3 shadow-lg cursor-pointer group"
        >
          <span className="material-icons text-cyan-400 text-2xl group-hover:scale-110 transition-transform">qr_code_2</span>
          <div className="flex flex-col items-start leading-none text-left">
            <span className="text-[9px] font-mono font-semibold text-slate-400 uppercase tracking-wider">{t('SCAN WITH PHONE', 'মোবাইলে স্ক্যান')}</span>
            <span className="text-base tracking-tight font-extrabold text-white">{t('Get QR Installer', 'কিউআর কোড স্ক্যানার')}</span>
          </div>
        </button>
      </motion.div>

      {/* 6. CENTERPIECE SMARTPHONE MOCKUP WITH REAL TELEMETRY HUD */}
      <motion.div 
        initial={{ opacity: 0, y: 35 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, delay: 0.35 }}
        className="relative z-10 w-full max-w-sm sm:max-w-md md:max-w-lg mx-auto"
      >
        {/* Smartphone Hardware Frame */}
        <div className="bg-slate-950 rounded-[2.8rem] md:rounded-[3.2rem] p-3 md:p-3.5 shadow-[0_25px_60px_rgba(0,0,0,0.9),0_0_50px_rgba(16,185,129,0.25)] border-[5px] md:border-[7px] border-slate-800 relative overflow-hidden">
          
          {/* Dynamic Island */}
          <div className="absolute top-4 left-1/2 -translate-x-1/2 w-28 md:w-32 h-6 bg-black rounded-full z-40 flex items-center justify-between px-3 border border-slate-800/80">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></div>
            <span className="text-[9px] font-mono font-bold text-emerald-400 uppercase">LIVE GPS</span>
            <div className="w-1.5 h-1.5 rounded-full bg-blue-500"></div>
          </div>

          {/* Screen Canvas */}
          <div className="w-full bg-[#030712] rounded-[2.2rem] md:rounded-[2.5rem] relative overflow-hidden flex flex-col min-h-[580px] md:min-h-[660px]">
            
            {/* Map Vector Texture Background */}
            <div className="absolute inset-0 bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 opacity-95">
              {/* Simulated Map Streets & Grid Lines */}
              <svg className="w-full h-full opacity-20" xmlns="http://www.w3.org/2000/svg">
                <defs>
                  <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
                    <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#06b6d4" strokeWidth="0.8"/>
                  </pattern>
                </defs>
                <rect width="100%" height="100%" fill="url(#grid)" />
              </svg>
            </div>

            {/* Simulated Live Route Path Line */}
            <div className="absolute inset-0 z-10 pointer-events-none">
              <svg viewBox="0 0 400 660" className="w-full h-full" style={{ filter: 'drop-shadow(0 0 10px #10b981)' }}>
                {/* Planned Path (Blue Dotted) */}
                <path d="M 120 220 L 220 280 L 270 360 L 210 440" fill="none" stroke="#0ea5e9" strokeWidth="4" strokeDasharray="5 5" opacity="0.6" />
                {/* Traveled Trail (Solid Emerald) */}
                <path d="M 120 220 L 220 280 L 245 320" fill="none" stroke="#10b981" strokeWidth="5" strokeLinecap="round" />
              </svg>

              {/* Origin Marker */}
              <div className="absolute top-[32%] left-[30%] -translate-x-1/2 -translate-y-1/2 flex flex-col items-center">
                <div className="w-3.5 h-3.5 rounded-full bg-blue-500 border-2 border-white shadow-lg"></div>
                <span className="text-[8px] font-mono font-bold text-blue-300 bg-slate-900/90 px-1.5 py-0.5 rounded mt-1 border border-blue-500/30 uppercase">Mirpur-10</span>
              </div>

              {/* Destination Marker */}
              <div className="absolute top-[66%] left-[52%] -translate-x-1/2 -translate-y-1/2 flex flex-col items-center">
                <div className="w-3.5 h-3.5 rounded-full bg-emerald-500 border-2 border-white shadow-lg"></div>
                <span className="text-[8px] font-mono font-bold text-emerald-300 bg-slate-900/90 px-1.5 py-0.5 rounded mt-1 border border-emerald-500/30 uppercase">Gulshan Hub</span>
              </div>

              {/* Moving Vehicle Dot (Current GPS) */}
              <div className="absolute top-[48%] left-[61%] -translate-x-1/2 -translate-y-1/2">
                <div className="w-12 h-12 rounded-full bg-emerald-400/25 animate-ping absolute -inset-2"></div>
                <div className="w-8 h-8 rounded-full bg-slate-900 border-2 border-emerald-400 shadow-[0_0_15px_#10b981] flex items-center justify-center text-emerald-400 relative z-20">
                  <span className="material-icons text-base">navigation</span>
                </div>
              </div>
            </div>

            {/* Top Bar inside Phone Screen */}
            <div className="relative z-30 pt-12 px-4 pb-3 bg-gradient-to-b from-slate-950 via-slate-950/80 to-transparent flex items-center justify-between">
              <div className="flex items-center gap-1.5 bg-slate-900/90 border border-slate-700/80 px-2.5 py-1 rounded-full text-[10px] font-mono text-slate-200">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                <span>DHAKA &bull; 34 KM/H</span>
              </div>

              <div className="flex items-center gap-2 text-[10px] font-mono text-slate-300 bg-slate-900/90 border border-slate-700/80 px-2.5 py-1 rounded-full">
                <span className="material-icons text-xs text-emerald-400">battery_charging_full</span>
                <span>86%</span>
              </div>
            </div>

            {/* Center Float: Vehicle Verification Evidence Badge */}
            <div className="relative z-30 mx-4 mt-2 bg-slate-900/95 border border-cyan-500/40 rounded-2xl p-2.5 shadow-xl backdrop-blur-md">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-[9px] font-mono text-cyan-400 font-bold uppercase flex items-center gap-1">
                  <span className="material-icons text-[11px]">verified</span>
                  {t('VEHICLE & DRIVER DETAILS', 'গাড়ির তথ্য ও ছবি')}
                </span>
                <span className="bg-emerald-500/20 text-emerald-300 text-[8.5px] font-mono font-bold px-1.5 py-0.5 rounded border border-emerald-500/30">
                  SAVED SECURELY
                </span>
              </div>

              <div className="flex items-center gap-2.5">
                <div className="w-12 h-10 rounded-lg bg-slate-800 border border-slate-700 overflow-hidden relative shrink-0">
                  <img 
                    src="https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=150&q=80" 
                    alt="Verified Vehicle Evidence" 
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-cyan-950/20"></div>
                </div>

                <div className="flex-1 min-w-0 text-left">
                  <div className="flex items-center gap-1.5">
                    <span className="bg-slate-950 text-yellow-400 font-mono font-bold text-[10.5px] px-1.5 py-0.2 rounded border border-slate-700 tracking-wider">
                      DHAKA METRO-GA 12-3456
                    </span>
                  </div>
                  <p className="text-[10px] text-slate-300 truncate mt-0.5">Toyota Premio (Silver) &bull; Kabir Hossain</p>
                </div>
              </div>
            </div>

            {/* Bottom HUD: Live Journey & 2-Second Emergency SOS Controller */}
            <div className="mt-auto relative z-30 p-3.5 bg-slate-950/95 border-t border-slate-800/90 rounded-t-3xl backdrop-blur-2xl space-y-2.5">
              
              {/* Trip Route Status */}
              <div className="flex items-center justify-between text-left">
                <div>
                  <span className="text-[9px] font-mono text-slate-400 uppercase tracking-wider block">{t('IN TRANSIT DESTINATION', 'গন্তব্যের পথে')}</span>
                  <h3 className="font-extrabold text-sm text-white leading-tight">Gulshan Circle-2, Dhaka</h3>
                </div>
                <div className="text-right">
                  <span className="text-[9px] font-mono text-emerald-400 uppercase font-bold block">14 MINS (4.2 KM)</span>
                  <span className="text-[9px] font-mono text-slate-400">ETA 10:45 PM</span>
                </div>
              </div>

              {/* Guardians on Live Radar */}
              <div className="flex items-center justify-between bg-slate-900/80 p-2 rounded-xl border border-slate-800 text-[10px]">
                <div className="flex items-center gap-1.5">
                  <span className="material-icons text-emerald-400 text-xs">share_location</span>
                  <span className="text-slate-300 font-medium">{t('Family WhatsApp Live Sharing Active (2 Contacts)', 'পরিবারের সাথে লাইভ শেয়ারিং সক্রিয়')}</span>
                </div>
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              </div>

              {/* 2-Second Press & Hold SOS Button */}
              <div className="relative group">
                <div className="w-full bg-gradient-to-r from-red-600 to-rose-700 hover:from-red-500 hover:to-rose-600 text-white py-3 rounded-2xl font-mono font-extrabold text-xs flex items-center justify-center gap-2 shadow-[0_0_25px_rgba(239,68,68,0.4)] transition-all cursor-pointer">
                  <span className="material-icons text-base">emergency</span>
                  <span>HOLD 2 SECONDS FOR EMERGENCY S.O.S</span>
                </div>
                <span className="block text-[8.5px] font-mono text-slate-400 text-center mt-1">
                  {t('Includes 3-second cancel countdown window to prevent false alarms', 'ভুল ক্লিক ঠেকাতে রয়েছে ৩-সেকেন্ডের ক্যানসেল উইন্ডো')}
                </span>
              </div>

            </div>

          </div>
        </div>
      </motion.div>

      {/* 7. HERO SECURITY TRUST STRIP (4 Key Pillars) */}
      <motion.div
        initial={{ opacity: 0, y: 25 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay: 0.5 }}
        className="w-full max-w-5xl mt-12 md:mt-16 grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4 relative z-10"
      >
        {[
          {
            icon: 'wifi_tethering',
            title: t('Live Location Sharing', 'লাইভ লোকেশন ট্র্যাকিং'),
            desc: t('Share live location directly to family browsers without installing an app', 'কোনো অ্যাপ ছাড়াই ব্রাউজারে পরিবারের সাথে সরাসরি লাইভ লোকেশন শেয়ার')
          },
          {
            icon: 'directions_car',
            title: t('Vehicle & Driver Photos', 'গাড়ির ছবি ও প্লেট নম্বর'),
            desc: t('Save vehicle details and photos securely before boarding', 'যেকোনো বাহনে ওঠার আগে ছবি ও লাইসেন্স প্লেট নিরাপদে সংরক্ষণ')
          },
          {
            icon: 'graphic_eq',
            title: t('Emergency Audio Recording', 'জরুরি অডিও রেকর্ডিং'),
            desc: t('Automatically record background audio when an emergency SOS is triggered', 'বিপদের সময় ব্যাকগ্রাউন্ডে স্বয়ংক্রিয় অডিও রেকর্ড সংরক্ষণ')
          },
          {
            icon: 'local_police',
            title: t('Direct 999 Hotline', 'জাতীয় হেল্পলাইন ৯৯৯'),
            desc: t('Instant one-tap emergency police & ambulance hotline connection', 'জরুরি পুলিশ ও অ্যাম্বুলেন্স সহায়তার তাত্ক্ষণিক হটলাইন')
          }
        ].map((item, idx) => (
          <div 
            key={idx} 
            className="bg-slate-900/60 border border-slate-800/80 hover:border-slate-700 p-4 rounded-2xl text-left backdrop-blur-sm transition-all hover:-translate-y-0.5"
          >
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-2.5">
              <span className="material-icons text-lg">{item.icon}</span>
            </div>
            <h4 className="font-bold text-xs sm:text-sm text-white">{item.title}</h4>
            <p className="text-[11px] text-slate-400 mt-1 leading-snug font-sans">{item.desc}</p>
          </div>
        ))}
      </motion.div>

      {/* QR Code Modal Dialog */}
      {showQrModal && (
        <div 
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 animate-fadeIn"
          onClick={() => setShowQrModal(false)}
        >
          <div 
            className="bg-slate-900 border border-slate-800 rounded-3xl max-w-sm w-full p-6 text-center shadow-2xl relative"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setShowQrModal(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white p-1 rounded-lg"
            >
              <span className="material-icons text-lg">close</span>
            </button>

            <div className="w-12 h-12 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <span className="material-icons text-2xl">qr_code_scanner</span>
            </div>

            <h3 className="font-bold text-lg text-white mb-1">{t('Scan to Install App', 'মোবাইলে স্ক্যান করে নামান')}</h3>
            <p className="text-slate-400 text-xs mb-5">
              {t('Point your smartphone camera to download the SafeTravel passenger app directly.', 'আপনার ফোনের ক্যামেরা দিয়ে কিউআর কোড স্ক্যান করে সরাসরি অ্যাপটি নামিয়ে নিন।')}
            </p>

            <div className="bg-white p-4 rounded-2xl inline-block shadow-inner mb-4">
              {/* QR Image Representation */}
              <img 
                src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=https://safetravel.app" 
                alt="SafeTravel Download QR Code" 
                className="w-40 h-40 object-contain mx-auto"
              />
            </div>

            <div className="text-[11px] font-mono text-slate-400 flex items-center justify-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span>ANDROID 14 COMPATIBLE &bull; APK DIRECT</span>
            </div>
          </div>
        </div>
      )}

    </main>
  );
}
