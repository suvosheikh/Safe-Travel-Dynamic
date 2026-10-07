'use client';

import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useLanguage } from '@/components/LanguageProvider';

export default function CoreFeaturesSection() {
  const { t } = useLanguage();
  const [activeTab, setActiveTab] = useState<'all' | 'vehicle' | 'audio' | 'sos' | 'guardian'>('all');

  return (
    <section id="features" className="py-16 md:py-24 px-4 sm:px-6 md:px-12 relative z-10">
      <div className="max-w-7xl mx-auto">
        
        {/* Section Header */}
        <div className="text-center mb-12 md:mb-16">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-cyan-950/40 border border-cyan-500/30 mb-4 backdrop-blur-md">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse shadow-[0_0_8px_#22d3ee]"></span>
            <span className="text-[10px] md:text-xs font-mono text-cyan-300 tracking-wider uppercase font-bold">
              {t('SMART TRAVEL SAFETY FEATURES', 'স্মার্ট ভ্রমণ সুরক্ষা')}
            </span>
          </div>

          <h2 className="text-3xl sm:text-4xl md:text-5.5xl font-black text-white tracking-tight mb-4 md:mb-6">
            {t('Designed to keep you and your loved ones', 'আপনার প্রতিটি যাত্রায়')} <br className="hidden sm:block"/>
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-cyan-400 to-blue-500">
              {t('safe during every trip.', 'সর্বোচ্চ নিরাপত্তা ও সুরক্ষার নিশ্চয়তা।')}
            </span>
          </h2>

          <p className="text-slate-300 max-w-2xl mx-auto text-sm sm:text-base md:text-lg leading-relaxed font-normal">
            {t(
              'We combine real-time GPS tracking with vehicle photo capture, emergency background audio recording, and fast 2-second SOS alerts to protect every journey.',
              'রিয়েল-টাইম জিপিএস ট্র্যাকিং, গাড়ির ছবি সংরক্ষণ, ব্যাকগ্রাউন্ড জরুরি অডিও রেকর্ডিং এবং ২-সেকেন্ডের দ্রুত এসওএস সতর্কবার্তার সমন্বয়ে নিশ্চিত হয় আপনার প্রতিটি যাত্রার পূর্ণ সুরক্ষা।'
            )}
          </p>
        </div>

        {/* Bento Grid Architecture */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-5 md:gap-6">
          
          {/* ========================================================
              BENTO 1: VEHICLE & LICENSE PLATE VERIFICATION VAULT (Cols: 7)
             ======================================================== */}
          <div className="md:col-span-7 bg-gradient-to-br from-slate-900/90 to-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 relative overflow-hidden group hover:border-cyan-500/50 transition-all flex flex-col justify-between shadow-2xl">
            {/* Ambient Background Glow */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-cyan-500/10 blur-[80px] rounded-full group-hover:bg-cyan-500/20 transition-all"></div>

            <div>
              {/* Header Badges */}
              <div className="flex items-center justify-between mb-5">
                <div className="w-12 h-12 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                  <span className="material-icons text-2xl">photo_camera</span>
                </div>
                <span className="text-[10px] font-mono font-bold text-cyan-400 bg-cyan-950/60 border border-cyan-500/30 px-2.5 py-1 rounded-full uppercase tracking-wider">
                  SECURE PHOTO STORAGE
                </span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-extrabold text-white mb-3">
                {t('Vehicle & Driver Details', 'ভেহিকেল ফটো ও লাইসেন্স প্লেট সংরক্ষণ')}
              </h3>

              <p className="text-slate-300 text-sm sm:text-base leading-relaxed mb-6 font-normal">
                {t(
                  'Board any CNG auto-rickshaw, taxi, or bus with absolute confidence. Take a quick snapshot of the vehicle license plate before starting; it is immediately saved to secure cloud storage, visible to your family even if your phone is lost or stolen.',
                  'যেকোনো সিএনজি, ট্যাক্সি বা বাসে ওঠার আগে গাড়ির লাইসেন্স প্লেটের ছবি তুলুন। ছবি তোলার সাথে সাথেই তা ক্লাউডে সংরক্ষিত হয়ে যায় এবং পরিবারের ট্র্যাকিং স্ক্রিনে লাইভ যুক্ত হয়—যাতে ফোন হারিয়ে গেলেও প্রমাণ অক্ষত থাকে।'
                )}
              </p>
            </div>

            {/* Simulated Live Photographic Card UI */}
            <div className="bg-slate-950/90 border border-slate-800 rounded-2xl p-4 shadow-xl relative z-10">
              <div className="flex items-center justify-between mb-3 text-[11px] font-mono">
                <div className="flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                  <span className="text-slate-300 font-bold uppercase">{t('VEHICLE PHOTO SAVED', 'গাড়ির ছবি সংরক্ষিত')}</span>
                </div>
                <span className="text-cyan-400 font-semibold">AES-256 ENCRYPTED</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-center">
                {/* Vehicle Thumbnail Preview */}
                <div className="relative rounded-xl overflow-hidden aspect-video bg-slate-900 border border-slate-700/80 group/photo">
                  <img 
                    src="https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=350&q=80" 
                    alt="Verified Vehicle Evidence" 
                    className="w-full h-full object-cover group-hover/photo:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute top-2 left-2 bg-slate-950/80 backdrop-blur-md px-2 py-0.5 rounded text-[9px] font-mono text-emerald-400 font-bold border border-emerald-500/30">
                    MATCH: CONFIRMED
                  </div>
                </div>

                {/* Extracted Details */}
                <div className="space-y-2 text-xs">
                  <div>
                    <span className="text-[9.5px] font-mono text-slate-500 uppercase block">{t('EXTRACTED PLATE NUMBER', 'লাইসেন্স প্লেট নম্বর')}</span>
                    <span className="bg-slate-900 text-yellow-400 font-mono font-bold text-xs px-2 py-1 rounded border border-slate-700 tracking-wider inline-block mt-0.5">
                      DHAKA METRO-GA 12-3456
                    </span>
                  </div>

                  <div>
                    <span className="text-[9.5px] font-mono text-slate-500 uppercase block">{t('TRANSPORT & DRIVER', 'বাহন ও ড্রাইভার')}</span>
                    <p className="text-slate-200 font-medium truncate">Toyota Premio &bull; Kabir Hossain</p>
                  </div>

                  <div className="pt-1 border-t border-slate-800 text-[10px] text-emerald-400 font-mono flex items-center gap-1">
                    <span className="material-icons text-xs">verified</span>
                    <span>{t('Saved securely for safety verification', 'নিরাপত্তার স্বার্থে সুরক্ষিতভাবে সংরক্ষিত')}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* ========================================================
              BENTO 2: AUTOMATED AUDIO RECORDING (Cols: 5)
             ======================================================== */}
          <div className="md:col-span-5 bg-gradient-to-br from-amber-950/20 via-slate-900/90 to-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 relative overflow-hidden group hover:border-amber-500/50 transition-all flex flex-col justify-between shadow-2xl">
            {/* Ambient Background Glow */}
            <div className="absolute top-0 right-0 w-56 h-56 bg-amber-500/10 blur-[80px] rounded-full group-hover:bg-amber-500/20 transition-all"></div>

            <div>
              {/* Header Badge */}
              <div className="flex items-center justify-between mb-5">
                <div className="w-12 h-12 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
                  <span className="material-icons text-2xl">graphic_eq</span>
                </div>
                <span className="text-[10px] font-mono font-bold text-amber-400 bg-amber-950/60 border border-amber-500/30 px-2.5 py-1 rounded-full uppercase tracking-wider">
                  EMERGENCY VOICE RECORDING
                </span>
              </div>

              <h3 className="text-2xl font-extrabold text-white mb-3">
                {t('Emergency Audio Recording', 'জরুরি অডিও রেকর্ডিং')}
              </h3>

              <p className="text-slate-300 text-sm leading-relaxed mb-6 font-normal">
                {t(
                  'During an emergency or SOS alert, SafeTravel automatically records background audio and uploads it securely so your family can hear what is happening.',
                  'বিপদের সময় বা জরুরি এসওএস ট্রিগার হলে ব্যাকগ্রাউন্ডে স্বয়ংক্রিয়ভাবে অডিও রেকর্ড শুরু হয়। এই ভয়েস ক্লিপ সরাসরি ক্লাউডে সেভ হয়, ফলে অভিভাবক ও জরুরি রেসপন্ডাররা গাড়ির ভেতরের পরিস্থিতি স্পষ্ট শুনতে পান।'
                )}
              </p>
            </div>

            {/* Simulated Live Audio Player Snippet */}
            <div className="bg-slate-950/90 border border-slate-800 rounded-2xl p-3.5 shadow-xl relative z-10 space-y-2.5">
              <div className="flex items-center justify-between text-[10px] font-mono text-amber-400">
                <span className="flex items-center gap-1 font-bold">
                  <span className="material-icons text-xs">volume_up</span>
                  <span>AUDIO CLIP</span>
                </span>
                <span className="text-slate-400">DURATION: 00:15 &bull; 245 KB</span>
              </div>

              {/* Fake Audio Waveform */}
              <div className="h-8 bg-slate-900 rounded-lg flex items-center justify-between px-3 gap-1 overflow-hidden border border-slate-800">
                <div className="w-5 h-5 rounded-full bg-amber-500 text-slate-950 flex items-center justify-center shrink-0">
                  <span className="material-icons text-[14px]">play_arrow</span>
                </div>
                <div className="flex-1 flex items-center gap-0.5 h-4 px-2">
                  {[40, 65, 80, 45, 90, 75, 60, 85, 95, 70, 50, 65, 85, 40, 75, 90, 60, 80, 55, 70, 85, 60, 45].map((h, i) => (
                    <div 
                      key={i} 
                      className="flex-1 bg-amber-400/80 rounded-full" 
                      style={{ height: `${h}%` }}
                    ></div>
                  ))}
                </div>
                <span className="text-[9.5px] font-mono text-slate-400 shrink-0">00:15</span>
              </div>

              <div className="flex justify-between items-center text-[9px] font-mono text-slate-400 pt-0.5">
                <span>[EMERGENCY SOS ALERT]</span>
                <span className="text-emerald-400 font-bold">SAVED SECURELY</span>
              </div>
            </div>
          </div>

          {/* ========================================================
              BENTO 3: 2-SECOND SMART SOS & WHATSAPP ALERT (Cols: 5)
             ======================================================== */}
          <div className="md:col-span-5 bg-gradient-to-br from-rose-950/20 via-slate-900/90 to-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 relative overflow-hidden group hover:border-rose-500/50 transition-all flex flex-col justify-between shadow-2xl">
            {/* Ambient Background Glow */}
            <div className="absolute top-0 right-0 w-56 h-56 bg-rose-500/10 blur-[80px] rounded-full group-hover:bg-rose-500/20 transition-all"></div>

            <div>
              {/* Header Badge */}
              <div className="flex items-center justify-between mb-5">
                <div className="w-12 h-12 rounded-2xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
                  <span className="material-icons text-2xl">emergency</span>
                </div>
                <span className="text-[10px] font-mono font-bold text-rose-400 bg-rose-950/60 border border-rose-500/30 px-2.5 py-1 rounded-full uppercase tracking-wider">
                  EMERGENCY SOS ALERT
                </span>
              </div>

              <h3 className="text-2xl font-extrabold text-white mb-3">
                {t('2-Second Smart SOS & WhatsApp Alert', '২-সেকেন্ড স্মার্ট এসওএস ও হোয়াটসঅ্যাপ অ্যালার্ট')}
              </h3>

              <p className="text-slate-300 text-sm leading-relaxed mb-6 font-normal">
                {t(
                  'Accidental taps in your pocket will never cause false emergency panics. To trigger SOS, hold the button for 2 seconds. You then have a 3-second grace countdown to cancel. If not cancelled, instant WhatsApp alerts with your live tracking link blast to all guardians.',
                  'পকেটে অসাবধানতাবশত চাপ লেগে কখনো ভুল অ্যালার্ট যাবে না। এসওএস সক্রিয় করতে ২ সেকেন্ড চেপে রাখতে হয়, এরপর ৩ সেকেন্ডের ক্যানসেল উইন্ডো থাকে। ক্যানসেল না করলে স্বয়ংক্রিয়ভাবে পরিবারের সব গার্ডিয়ানের হোয়াটসঅ্যাপে লাইভ ট্র্যাকিং লিঙ্ক চলে যায়।'
                )}
              </p>
            </div>

            {/* Simulated 2s Hold Controller UI */}
            <div className="bg-slate-950/90 border border-slate-800 rounded-2xl p-4 shadow-xl relative z-10 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-full bg-rose-600/30 border border-rose-500 flex items-center justify-center text-rose-400">
                    <span className="material-icons text-sm">touch_app</span>
                  </div>
                  <div>
                    <span className="font-bold text-xs text-white block">{t('2-Second Hold Guard', '২-সেকেন্ড হোল্ড সুরক্ষা')}</span>
                    <span className="text-[9.5px] font-mono text-slate-400">{t('Configurable 0s - 5s in settings', 'সেটিংসে পরিবর্তনযোগ্য')}</span>
                  </div>
                </div>

                <span className="text-[10px] font-mono font-bold text-rose-400 bg-rose-950/80 px-2 py-0.5 rounded border border-rose-500/40">
                  3s CANCEL WINDOW
                </span>
              </div>

              {/* Emergency WhatsApp Preview */}
              <div className="bg-emerald-950/30 border border-emerald-500/30 rounded-xl p-2.5 text-xs text-slate-200 space-y-1">
                <div className="flex items-center justify-between text-[10px] font-mono text-emerald-400">
                  <span className="font-bold flex items-center gap-1">
                    <span className="material-icons text-xs">chat</span>
                    AUTO WHATSAPP ALERT
                  </span>
                  <span>DELIVERED (2/2)</span>
                </div>
                <p className="text-[11px] text-slate-300 font-sans leading-tight">
                  "{t('EMERGENCY SOS: I need help near Kazipara. Watch live: safetravel.app/track/TRK-9X4', 'জরুরি অ্যালার্ট: আমি কাজীপাড়ায় বিপদে পড়েছি। লাইভ দেখুন: safetravel.app/track/TRK-9X4')}"
                </p>
              </div>
            </div>
          </div>

          {/* ========================================================
              BENTO 4: ZERO-INSTALL GUARDIAN TRACKING (Cols: 7)
             ======================================================== */}
          <div className="md:col-span-7 bg-gradient-to-br from-slate-900/90 to-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 relative overflow-hidden group hover:border-emerald-500/50 transition-all flex flex-col justify-between shadow-2xl">
            {/* Ambient Background Glow */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/10 blur-[80px] rounded-full group-hover:bg-emerald-500/20 transition-all"></div>

            <div>
              {/* Header Badges */}
              <div className="flex items-center justify-between mb-5">
                <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
                  <span className="material-icons text-2xl">radar</span>
                </div>
                <span className="text-[10px] font-mono font-bold text-emerald-400 bg-emerald-950/60 border border-emerald-500/30 px-2.5 py-1 rounded-full uppercase tracking-wider">
                  LIVE GUARDIAN PORTAL
                </span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-extrabold text-white mb-3">
                {t('Live Location & Status Sharing', 'পরিবারের জন্য সরাসরি লাইভ ট্র্যাকিং')}
              </h3>

              <p className="text-slate-300 text-sm sm:text-base leading-relaxed mb-6 font-normal">
                {t(
                  'Your parents, spouses, and friends do NOT need to download or register on any app. When you start a trip, they receive an encrypted private web link. Clicking it opens a live tracking view showing real-time GPS coordinates, vehicle photo, speed, and phone battery percentage.',
                  'আপনার বাবা-মা, ভাই-বোন বা বন্ধুদের ফোনে কোনো অ্যাপ ইনস্টল করতে হবে না। ট্রিপ শুরুর সাথে সাথে তারা একটি সিকিউর ওয়েব লিংক পাবেন। যেকোনো ব্রাউজারেই স্মুথ ম্যাপে গাড়ির লাইভ লোকেশন, গতি, গাড়ির ছবি এবং ফোনের ব্যাটারির চার্জ সরাসরি দেখা যাবে।'
                )}
              </p>
            </div>

            {/* Simulated Guardian Web HUD Card */}
            <div className="bg-slate-950/90 border border-slate-800 rounded-2xl p-4 shadow-xl relative z-10 space-y-3">
              <div className="flex items-center justify-between text-xs font-mono">
                <div className="flex items-center gap-2">
                  <span className="material-icons text-emerald-400 text-sm">open_in_browser</span>
                  <span className="text-slate-300 font-bold uppercase">LIVE WEB TRACKING</span>
                </div>
                <span className="text-emerald-400 font-bold bg-emerald-950/80 px-2 py-0.5 rounded border border-emerald-500/40">
                  60 FPS REALTIME
                </span>
              </div>

              {/* 3 Telemetry Metrics */}
              <div className="grid grid-cols-3 gap-2 text-center text-xs font-mono">
                <div className="bg-slate-900 p-2 rounded-xl border border-slate-800">
                  <span className="text-[9px] text-slate-500 uppercase block">{t('CURRENT SPEED', 'লাইভ গতি')}</span>
                  <span className="font-bold text-white text-sm">34 km/h</span>
                </div>

                <div className="bg-slate-900 p-2 rounded-xl border border-slate-800">
                  <span className="text-[9px] text-slate-500 uppercase block">{t('PHONE BATTERY', 'ব্যাটারি চার্জ')}</span>
                  <span className="font-bold text-emerald-400 text-sm">86% Normal</span>
                </div>

                <div className="bg-slate-900 p-2 rounded-xl border border-slate-800">
                  <span className="text-[9px] text-slate-500 uppercase block">{t('ROUTE STATUS', 'রুট স্ট্যাটাস')}</span>
                  <span className="font-bold text-cyan-400 text-sm">On Path</span>
                </div>
              </div>

              <div className="flex items-center justify-between text-[10px] font-mono text-slate-400 pt-1">
                <span>{t('Alerts family automatically if battery drops below 15%', 'চার্জ ১৫% এর নিচে নামলে স্বয়ংক্রিয় সতর্কবার্তা')}</span>
                <span className="text-emerald-400 font-bold">{t('ACTIVE', 'সক্রিয়')}</span>
              </div>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
}
