'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import PublicNavbar from '@/components/PublicNavbar';
import { useLanguage } from '@/components/LanguageProvider';

export default function TrackGatewayPage() {
  const router = useRouter();
  const { t } = useLanguage();
  const [tripCode, setTripCode] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleTrack = (e: React.FormEvent) => {
    e.preventDefault();
    if (!tripCode.trim()) {
      setError(t('Please enter a valid tracking code.', 'অনুগ্রহ করে একটি সঠিক ট্র্যাকিং কোড দিন।'));
      return;
    }
    setError('');
    setIsLoading(true);
    
    // Simulate a brief network delay for UX
    setTimeout(() => {
      router.push(`/track/${tripCode.trim()}`);
    }, 600);
  };

  const handleDemo = () => {
    setIsLoading(true);
    // 'TRK-9X4-B2' is a dummy tracking code from our local mock database
    router.push('/track/TRK-9X4-B2');
  };

  return (
    <div className="min-h-screen bg-[#020617] text-slate-100 font-sans selection:bg-emerald-500/30 flex flex-col">
      <PublicNavbar />

      {/* BACKGROUND AMBIENT GLOWS */}
      <div className="fixed inset-0 z-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-10%] w-[40vw] h-[40vw] bg-emerald-600/10 blur-[120px] rounded-full mix-blend-screen"></div>
        <div className="absolute bottom-[-10%] right-[-10%] w-[50vw] h-[50vw] bg-cyan-600/10 blur-[120px] rounded-full mix-blend-screen"></div>
      </div>

      <main className="flex-1 flex flex-col items-center justify-center relative z-10 px-4 py-24 md:py-32 w-full max-w-4xl mx-auto">
        
        {/* Header Icon */}
        <div className="w-16 h-16 md:w-20 md:h-20 bg-slate-900 border border-slate-700 rounded-2xl flex items-center justify-center mb-6 md:mb-8 shadow-[0_0_30px_rgba(16,185,129,0.15)] relative">
          <div className="absolute inset-0 bg-emerald-500/20 rounded-2xl animate-pulse"></div>
          <span className="material-icons text-3xl md:text-4xl text-emerald-400 relative z-10">radar</span>
        </div>

        <h1 className="text-3xl md:text-5xl font-black text-white mb-4 tracking-tight text-center">
          {t('Secure ', 'সিকিউর ')}<span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-cyan-400">{t('Trip Tracking', 'ট্রিপ ট্র্যাকিং')}</span>
        </h1>
        <p className="text-slate-400 text-sm md:text-base text-center max-w-lg mb-10 leading-relaxed px-4">
          {t('Enter the secure Trip ID or 6-digit PIN provided by your trusted contact to view their live location and safety status.', 'আপনার প্রিয়জনের দেওয়া সিকিউর ট্রিপ আইডি বা ৬-ডিজিটের পিনটি নিচে দিয়ে তাদের লাইভ লোকেশন দেখুন।')}
        </p>

        {/* Tracking Input Card */}
        <div className="w-full max-w-md bg-slate-900/60 backdrop-blur-xl border border-slate-800 rounded-3xl p-6 md:p-8 shadow-2xl relative">
          <form onSubmit={handleTrack} className="space-y-4">
            <div>
              <label htmlFor="tripCode" className="block text-[11px] font-mono uppercase tracking-widest text-slate-400 mb-2 font-bold">
                {t('Trip ID / Tracking PIN', 'ট্রিপ আইডি / ট্র্যাকিং পিন')}
              </label>
              <div className="relative">
                <span className="material-icons absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 text-[20px]">lock_outline</span>
                <input 
                  id="tripCode"
                  type="text" 
                  value={tripCode}
                  onChange={(e) => {
                    setTripCode(e.target.value);
                    if (error) setError('');
                  }}
                  placeholder="e.g. trip-12345" 
                  className={`w-full bg-slate-950 border ${error ? 'border-red-500/50 focus:border-red-500' : 'border-slate-700 focus:border-emerald-500/50'} text-white text-base md:text-lg rounded-xl pl-12 pr-4 py-4 md:py-4 focus:outline-none focus:ring-1 focus:ring-emerald-500/50 transition-all placeholder:text-slate-600 font-mono tracking-wider`}
                />
              </div>
              {error && (
                <p className="text-red-400 text-xs mt-2 font-medium animate-in fade-in flex items-center gap-1">
                  <span className="material-icons text-[14px]">error</span> {error}
                </p>
              )}
            </div>

            <button 
              type="submit" 
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 font-bold text-base md:text-lg py-4 rounded-xl shadow-[0_0_20px_rgba(16,185,129,0.3)] hover:shadow-[0_0_30px_rgba(16,185,129,0.5)] transition-all flex items-center justify-center space-x-2 disabled:opacity-70 disabled:cursor-not-allowed group"
            >
              {isLoading ? (
                <span className="material-icons animate-spin">refresh</span>
              ) : (
                <>
                  <span>{t('Track Live Journey', 'লাইভ ট্র্যাকিং শুরু করুন')}</span>
                  <span className="material-icons group-hover:translate-x-1 transition-transform">arrow_forward</span>
                </>
              )}
            </button>
          </form>

          {/* Alternative Methods */}
          <div className="mt-8 pt-6 border-t border-slate-800/80">
            <div className="flex items-center justify-center gap-4 text-slate-400 font-mono text-[10px] uppercase tracking-widest mb-4">
              <div className="h-px bg-slate-800 flex-1"></div>
              <span>{t('Alternative Options', 'অন্যান্য অপশন')}</span>
              <div className="h-px bg-slate-800 flex-1"></div>
            </div>
            
            <div className="grid grid-cols-2 gap-3">
              <button className="flex flex-col items-center justify-center p-3 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-600 hover:bg-slate-800/50 transition-colors group">
                <span className="material-icons text-slate-400 group-hover:text-white mb-1">qr_code_scanner</span>
                <span className="text-[10px] md:text-xs font-bold text-slate-500 group-hover:text-slate-300 uppercase">{t('Scan QR Code', 'কিউআর স্ক্যান')}</span>
              </button>
              <button 
                type="button"
                onClick={handleDemo}
                className="flex flex-col items-center justify-center p-3 rounded-xl bg-blue-950/20 border border-blue-900/30 hover:border-blue-500/50 hover:bg-blue-900/30 transition-colors group"
              >
                <span className="material-icons text-blue-400 group-hover:text-blue-300 mb-1">play_circle</span>
                <span className="text-[10px] md:text-xs font-bold text-blue-500 group-hover:text-blue-400 uppercase">{t('View Demo Trip', 'ডেমো ট্রিপ দেখুন')}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Security Meta Features */}
        <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6 text-center max-w-3xl opacity-70">
          <div className="flex flex-col items-center">
            <span className="material-icons text-emerald-500 mb-2">lock</span>
            <h4 className="text-sm font-bold text-white">{t('End-to-End Encrypted', 'এন্ড-টু-এন্ড এনক্রিপ্টেড')}</h4>
            <p className="text-[11px] text-slate-400 mt-1">{t('Only you and the traveler can see this location data.', 'শুধুমাত্র আপনি এবং ট্রাভেলার এই ডেটা দেখতে পাবেন।')}</p>
          </div>
          <div className="flex flex-col items-center">
            <span className="material-icons text-cyan-500 mb-2">timer_off</span>
            <h4 className="text-sm font-bold text-white">{t('Auto Expires', 'স্বয়ংক্রিয়ভাবে এক্সপায়ার')}</h4>
            <p className="text-[11px] text-slate-400 mt-1">{t('Links are destroyed immediately when the trip ends.', 'ট্রিপ শেষ হওয়া মাত্রই লিংকটি নষ্ট হয়ে যায়।')}</p>
          </div>
          <div className="flex flex-col items-center">
            <span className="material-icons text-blue-500 mb-2">gpp_good</span>
            <h4 className="text-sm font-bold text-white">{t('100% Private', '১০০% প্রাইভেট')}</h4>
            <p className="text-[11px] text-slate-400 mt-1">{t('We never sell your real-time tracking data.', 'আমরা কখনোই ট্র্যাকিং ডেটা বিক্রি করি না।')}</p>
          </div>
        </div>

      </main>
    </div>
  );
}
