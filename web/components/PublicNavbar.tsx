'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useLanguage } from '@/components/LanguageProvider';

export default function PublicNavbar() {
  const { language, setLanguage, t } = useLanguage();
  const router = useRouter();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [navTrackCode, setNavTrackCode] = useState('');
  const [trackSearchOpen, setTrackSearchOpen] = useState(false);

  const handleNavTrack = (e: React.FormEvent) => {
    e.preventDefault();
    if (!navTrackCode.trim()) return;
    router.push(`/track/${navTrackCode.trim()}`);
    setTrackSearchOpen(false);
    setMobileMenuOpen(false);
  };

  return (
    <nav className="w-full border-b border-slate-800/80 bg-[#020617]/85 backdrop-blur-xl fixed top-0 z-50 transition-all duration-300">
      <div className="max-w-7xl mx-auto flex items-center justify-between px-4 sm:px-6 md:px-10 h-16 md:h-20">
        
        {/* Left: Brand Logo & Live Signal */}
        <div className="flex items-center space-x-3 sm:space-x-4">
          <Link href="/" className="flex items-center space-x-2.5 sm:space-x-3 group">
            <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-cyan-500 flex items-center justify-center shadow-[0_0_20px_rgba(16,185,129,0.35)] group-hover:shadow-[0_0_28px_rgba(16,185,129,0.55)] transition-all">
              <span className="material-icons text-slate-950 text-xl font-bold">security</span>
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-lg sm:text-xl tracking-tight text-white leading-none">
                Safe<span className="text-emerald-400">Travel</span>
              </span>
              <span className="text-[9px] font-mono tracking-wider text-slate-400 uppercase mt-0.5">
                TRAVEL SAFETY PLATFORM
              </span>
            </div>
          </Link>

          {/* Live Operational Indicator (Desktop) */}
          <div className="hidden lg:flex items-center space-x-2 bg-emerald-950/40 border border-emerald-500/20 px-2.5 py-1 rounded-full text-[10px] font-mono text-emerald-300">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse shadow-[0_0_8px_#34d399]"></span>
            <span className="font-semibold uppercase tracking-wider">{t('SYSTEM ACTIVE &bull; READY', 'সিস্টেম সক্রিয় &bull; প্রস্তুত')}</span>
          </div>
        </div>

        {/* Center: Desktop Navigation Links */}
        <div className="hidden md:flex items-center space-x-6 lg:space-x-8 text-sm font-medium text-slate-300">
          <Link href="/#features" className="hover:text-emerald-400 transition-colors">
            {t('Features', 'ফিচারসমূহ')}
          </Link>
          <Link href="/how-it-works" className="hover:text-emerald-400 transition-colors">
            {t('How It Works', 'কীভাবে কাজ করে')}
          </Link>
          <Link href="/safety" className="hover:text-emerald-400 transition-colors">
            {t('Safety & 999', 'নিরাপত্তা ও ৯৯৯')}
          </Link>
          <Link href="/track" className="hover:text-cyan-400 transition-colors flex items-center space-x-1">
            <span className="material-icons text-base text-cyan-400">radar</span>
            <span>{t('Track Journey', 'ট্রিপ ট্র্যাক')}</span>
          </Link>
          <Link href="/#faq" className="hover:text-emerald-400 transition-colors">
            {t('FAQ', 'সাধারণ জিজ্ঞাসা')}
          </Link>
        </div>

        {/* Right: Quick Track Input, Language, Dashboard, and App CTA */}
        <div className="flex items-center space-x-2 sm:space-x-3">
          
          {/* Quick Track Trip Icon Button (Toggles compact search) */}
          <div className="relative hidden sm:block">
            {trackSearchOpen ? (
              <form onSubmit={handleNavTrack} className="flex items-center bg-slate-900 border border-cyan-500/50 rounded-full pl-3 pr-1 py-1 shadow-lg animate-fadeIn">
                <span className="material-icons text-cyan-400 text-sm mr-1">search</span>
                <input
                  type="text"
                  value={navTrackCode}
                  onChange={(e) => setNavTrackCode(e.target.value)}
                  placeholder="TRK-9X4-B2..."
                  autoFocus
                  className="w-28 md:w-32 bg-transparent text-xs font-mono text-white placeholder-slate-500 focus:outline-none uppercase"
                />
                <button
                  type="submit"
                  className="bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-[10px] font-bold px-2 py-1 rounded-full uppercase transition-colors"
                >
                  GO
                </button>
                <button
                  type="button"
                  onClick={() => setTrackSearchOpen(false)}
                  className="text-slate-400 hover:text-white p-1"
                >
                  <span className="material-icons text-xs">close</span>
                </button>
              </form>
            ) : (
              <button
                type="button"
                onClick={() => setTrackSearchOpen(true)}
                className="flex items-center space-x-1.5 bg-slate-900/80 hover:bg-slate-800 border border-slate-700 hover:border-cyan-500/40 text-slate-300 hover:text-cyan-300 text-xs font-mono px-3 py-1.5 rounded-full transition-all cursor-pointer"
                title="Quick Track Journey by Code"
              >
                <span className="material-icons text-sm text-cyan-400">search</span>
                <span className="hidden xl:inline">{t('Enter Trip Code', 'ট্রিপ কোড দিন')}</span>
              </button>
            )}
          </div>

          {/* Language Switcher Pill */}
          <button 
            type="button"
            onClick={() => setLanguage(language === 'en' ? 'bn' : 'en')}
            className="flex items-center justify-center h-8 px-2.5 rounded-full bg-slate-900/90 border border-slate-700 text-slate-300 hover:text-emerald-400 hover:border-emerald-500/50 transition-colors text-xs font-bold font-mono cursor-pointer"
            title="Switch Language (English / বাংলা)"
          >
            <span>{language === 'en' ? 'বাংলা' : 'EN'}</span>
          </button>

          {/* Admin Control Link */}
          <Link 
            href="/dashboard" 
            className="hidden lg:flex items-center space-x-1 text-xs font-mono font-medium text-slate-400 hover:text-slate-200 bg-slate-900/50 border border-slate-800 hover:border-slate-700 px-2.5 py-1.5 rounded-xl transition-all"
          >
            <span className="material-icons text-sm text-slate-400">admin_panel_settings</span>
            <span>{t('Admin Console', 'অ্যাডমিন')}</span>
          </Link>

          {/* Primary Get App Button */}
          <Link
            href="/#download"
            className="hidden sm:flex px-4 py-2 rounded-full bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 text-xs font-extrabold shadow-[0_0_15px_rgba(16,185,129,0.3)] transition-all items-center space-x-1.5 group cursor-pointer"
          >
            <span>{t('Get App', 'অ্যাপ নামান')}</span>
            <span className="material-icons text-xs group-hover:translate-y-0.5 transition-transform">south</span>
          </Link>

          {/* Mobile Hamburger Toggle Button */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden flex items-center justify-center w-9 h-9 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white transition-colors"
            aria-label="Toggle navigation menu"
          >
            <span className="material-icons text-xl">{mobileMenuOpen ? 'close' : 'menu'}</span>
          </button>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-slate-950/95 border-b border-slate-800 px-5 py-5 space-y-4 backdrop-blur-2xl animate-fadeIn">
          {/* Quick Track Input in Mobile Drawer */}
          <form onSubmit={handleNavTrack} className="flex items-center bg-slate-900 border border-slate-700 rounded-xl p-1.5">
            <span className="material-icons text-cyan-400 text-base ml-2 mr-1.5">radar</span>
            <input
              type="text"
              value={navTrackCode}
              onChange={(e) => setNavTrackCode(e.target.value)}
              placeholder="Enter Trip Code (e.g. TRK-9X4-B2)..."
              className="flex-1 bg-transparent text-xs font-mono text-white placeholder-slate-500 focus:outline-none uppercase"
            />
            <button
              type="submit"
              className="bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold px-3 py-1.5 rounded-lg uppercase transition-colors"
            >
              {t('Track', 'ট্র্যাক')}
            </button>
          </form>

          {/* Navigation Links */}
          <div className="flex flex-col space-y-2.5 text-sm font-semibold text-slate-200 pt-2 border-t border-slate-800/80">
            <Link 
              href="/#features" 
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center justify-between py-2 text-slate-300 hover:text-emerald-400"
            >
              <span>{t('Features', 'ফিচারসমূহ')}</span>
              <span className="material-icons text-sm text-slate-600">chevron_right</span>
            </Link>
            <Link 
              href="/how-it-works" 
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center justify-between py-2 text-slate-300 hover:text-emerald-400"
            >
              <span>{t('How It Works', 'কীভাবে কাজ করে')}</span>
              <span className="material-icons text-sm text-slate-600">chevron_right</span>
            </Link>
            <Link 
              href="/safety" 
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center justify-between py-2 text-slate-300 hover:text-emerald-400"
            >
              <span>{t('Safety & 999 Hotline', 'নিরাপত্তা ও ৯৯৯ হটলাইন')}</span>
              <span className="material-icons text-sm text-slate-600">chevron_right</span>
            </Link>
            <Link 
              href="/track" 
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center justify-between py-2 text-cyan-400"
            >
              <span>{t('Guardian Tracking Portal', 'গার্ডিয়ান ট্র্যাকিং পোর্টাল')}</span>
              <span className="material-icons text-sm text-cyan-500">radar</span>
            </Link>
            <Link 
              href="/#faq" 
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center justify-between py-2 text-slate-300 hover:text-emerald-400"
            >
              <span>{t('FAQ', 'সাধারণ জিজ্ঞাসা')}</span>
              <span className="material-icons text-sm text-slate-600">chevron_right</span>
            </Link>
            <Link 
              href="/dashboard" 
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center justify-between py-2 text-slate-400 hover:text-white"
            >
              <span>{t('Admin Control Console', 'অ্যাডমিন কন্ট্রোল রুম')}</span>
              <span className="material-icons text-sm text-slate-600">admin_panel_settings</span>
            </Link>
          </div>

          {/* Mobile Footer CTAs */}
          <div className="pt-2 border-t border-slate-800 flex gap-2">
            <Link
              href="/#download"
              onClick={() => setMobileMenuOpen(false)}
              className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 text-slate-950 text-xs font-bold text-center shadow-lg"
            >
              {t('Download SafeTravel App', 'সেফট্রেভেল অ্যাপ নামান')}
            </Link>
          </div>
        </div>
      )}
    </nav>
  );
}
