'use client';

import React, { useState, useEffect } from 'react';
import { getSupabaseClient } from '../../lib/supabase';
import { useToast } from '../ui/Toast';

export default function AdMonetizationSection() {
  const { toast } = useToast();
  const [activeNetworkTab, setActiveNetworkTab] = useState<'admob' | 'meta'>('admob');

  // Cloud Remote Configs State for Ads Monetization
  const [remoteConfigs, setRemoteConfigs] = useState<Record<string, string>>({
    ads_master_switch: 'true',
    ads_non_pro_only: 'true',
    ads_banner_enabled: 'true',
    admob_banner_unit_id: 'ca-app-pub-3940256099942544/6300978111',
    ads_directory_card_enabled: 'true',
    ads_directory_card_interval: '4',
    ads_history_card_enabled: 'true',
    ads_history_card_interval: '3',
    ads_history_card_size: 'medium_rectangle',
    ads_alert_card_enabled: 'true',
    ads_alert_card_interval: '3',
    ads_alert_card_size: 'large_banner',
    ads_rewarded_enabled: 'true',
    admob_rewarded_unit_id: 'ca-app-pub-3940256099942544/5224354917',
    ads_reward_credits_amount: '1',
    ads_interstitial_enabled: 'false',
    admob_interstitial_unit_id: 'ca-app-pub-3940256099942544/1033173712',
    // Future Meta Audience Network Keys
    meta_ads_enabled: 'false',
    meta_app_id: '',
    meta_banner_placement_id: '',
    meta_native_placement_id: '',
    meta_rewarded_placement_id: '',
    meta_interstitial_placement_id: '',
  });

  const [isSavingCloud, setIsSavingCloud] = useState(false);
  const [cloudSaveMessage, setCloudSaveMessage] = useState<string | null>(null);

  // Load live remote configs from Supabase
  useEffect(() => {
    const supabase = getSupabaseClient();
    if (!supabase) return;

    const loadConfigs = async () => {
      try {
        const { data, error } = await supabase
          .from('app_remote_configs')
          .select('key, value');
        if (data && !error && data.length > 0) {
          const map: Record<string, string> = {};
          data.forEach((r: any) => {
            map[r.key] = r.value;
          });
          setRemoteConfigs(prev => ({ ...prev, ...map }));
        }
      } catch (e) {
        console.warn('Could not load app_remote_configs for ads:', e);
      }
    };

    loadConfigs();
  }, []);

  // Save updated configs to Supabase
  const handleSaveRemoteConfigs = async () => {
    setIsSavingCloud(true);
    setCloudSaveMessage(null);
    try {
      const supabase = getSupabaseClient();
      if (!supabase) throw new Error('Supabase connection unavailable');

      const upsertRows = Object.entries(remoteConfigs).map(([key, value]) => ({
        key,
        value,
        updated_at: new Date().toISOString()
      }));

      const { error } = await supabase
        .from('app_remote_configs')
        .upsert(upsertRows, { onConflict: 'key' });

      if (error) throw error;
      setCloudSaveMessage('Successfully updated Supabase app_remote_configs!');
      toast.success('Ad monetization remote parameters synced to cloud.', 'Monetization Saved');
      setTimeout(() => setCloudSaveMessage(null), 4000);
    } catch (e: any) {
      console.error('Failed to save ads remote configs:', e);
      setCloudSaveMessage(`Error: ${e.message || 'Failed to save to Supabase'}`);
      toast.error(`Failed to save ad configs: ${e.message || 'Error occurred'}`, 'Sync Error');
    } finally {
      setIsSavingCloud(false);
    }
  };

  return (
    <div className="space-y-6" id="ad-monetization-view">
      {/* Top Header Card */}
      <div className="bg-white border border-slate-200 shadow-sm rounded-2xl p-5 sm:p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-5">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="material-icons text-amber-500 text-2xl">campaign</span>
              <h2 className="text-base sm:text-lg font-bold text-slate-800 uppercase tracking-tight">
                In-App Advertising & Monetization
              </h2>
            </div>
            <p className="text-xs text-slate-500">
              Centralized mobile advertising control center for Google AdMob and Meta Audience Network.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 font-mono text-[10px] text-amber-700 bg-amber-50 px-2.5 py-1.5 rounded-lg border border-amber-200">
              <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
              <span className="font-bold">LIVE DATABASE SYNC</span>
            </div>
          </div>
        </div>

        {/* Network Sub-Tabs Selector */}
        <div className="flex items-center gap-2 pt-4">
          <button
            type="button"
            onClick={() => setActiveNetworkTab('admob')}
            className={`px-4 py-2 rounded-xl text-xs font-mono font-bold uppercase transition-all flex items-center gap-2 cursor-pointer ${
              activeNetworkTab === 'admob'
                ? 'bg-amber-500 text-white shadow-sm shadow-amber-500/30'
                : 'bg-slate-100 text-slate-600 hover:text-slate-900 hover:bg-slate-200/70'
            }`}
          >
            <span className="material-icons text-sm">ads_click</span>
            <span>Google AdMob</span>
            <span className="bg-amber-600/30 text-white text-[9px] px-1.5 py-0.5 rounded">ACTIVE</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveNetworkTab('meta')}
            className={`px-4 py-2 rounded-xl text-xs font-mono font-bold uppercase transition-all flex items-center gap-2 cursor-pointer ${
              activeNetworkTab === 'meta'
                ? 'bg-blue-600 text-white shadow-sm shadow-blue-600/30'
                : 'bg-slate-100 text-slate-600 hover:text-slate-900 hover:bg-slate-200/70'
            }`}
          >
            <span className="material-icons text-sm">public</span>
            <span>Meta Audience Network</span>
            <span className="bg-slate-200 text-slate-600 text-[9px] px-1.5 py-0.5 rounded font-normal">IN FUTURE</span>
          </button>
        </div>
      </div>

      {/* Cloud Save Notification Toast */}
      {cloudSaveMessage && (
        <div className={`p-3.5 rounded-xl border text-xs font-mono flex items-center justify-between ${
          cloudSaveMessage.includes('Error')
            ? 'bg-rose-50 border-rose-200 text-rose-700'
            : 'bg-emerald-50 border-emerald-200 text-emerald-700'
        }`}>
          <div className="flex items-center gap-2">
            <span className="material-icons text-sm">
              {cloudSaveMessage.includes('Error') ? 'error_outline' : 'check_circle'}
            </span>
            <span>{cloudSaveMessage}</span>
          </div>
          <button type="button" onClick={() => setCloudSaveMessage(null)} className="font-bold">
            <span className="material-icons text-xs">close</span>
          </button>
        </div>
      )}

      {/* TAB 1: GOOGLE ADMOB CONTROLS */}
      {activeNetworkTab === 'admob' && (
        <div className="space-y-6">
          {/* Master Global Controls */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono flex items-center gap-1.5">
              <span className="material-icons text-sm text-blue-600">tune</span>
              Master Controls & Subscriber Protection
            </span>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Master Ads Switch */}
              <div className="flex items-center justify-between p-3.5 bg-slate-50 border border-slate-200 rounded-xl">
                <div className="space-y-0.5 pr-2">
                  <span className="text-xs font-bold text-slate-800 block">Master In-App Ads Switch</span>
                  <span className="text-[10px] text-slate-400 block">Turn off to completely disable all ads across the app.</span>
                </div>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_master_switch: prev.ads_master_switch === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_master_switch === 'true' ? 'bg-amber-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_master_switch === 'true' ? 'translate-x-5' : 'translate-x-0'
                  }`} />
                </button>
              </div>

              {/* Pro User 100% Ad-Free Filter */}
              <div className="flex items-center justify-between p-3.5 bg-slate-50 border border-slate-200 rounded-xl">
                <div className="space-y-0.5 pr-2">
                  <span className="text-xs font-bold text-slate-800 block">Pro Subscribers 100% Ad-Free</span>
                  <span className="text-[10px] text-slate-400 block">Ensure active premium subscribers never see any ads.</span>
                </div>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_non_pro_only: prev.ads_non_pro_only === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_non_pro_only === 'true' ? 'bg-emerald-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_non_pro_only === 'true' ? 'translate-x-5' : 'translate-x-0'
                  }`} />
                </button>
              </div>
            </div>
          </div>

          {/* Banner Ads Placement */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono flex items-center gap-1.5">
                <span className="material-icons text-sm text-cyan-600">view_stream</span>
                Standard Banner Ads (Alerts & History Screen)
              </span>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono text-slate-500">Banner Enabled</span>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_banner_enabled: prev.ads_banner_enabled === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_banner_enabled === 'true' ? 'bg-cyan-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_banner_enabled === 'true' ? 'translate-x-4' : 'translate-x-0'
                  }`} />
                </button>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                Banner Ad Unit ID
              </label>
              <input
                type="text"
                value={remoteConfigs.admob_banner_unit_id || ''}
                onChange={(e) => setRemoteConfigs(prev => ({ ...prev, admob_banner_unit_id: e.target.value }))}
                placeholder="ca-app-pub-3940256099942544/6300978111"
                className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              />
              <span className="text-[10px] text-slate-400">Official AdMob Banner ID. Default is Google test unit ID.</span>
            </div>
          </div>

          {/* Directory In-Feed Card Ads (Police, Hospital, Fire, Blood Bank) */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono flex items-center gap-1.5">
                <span className="material-icons text-sm text-blue-600">view_stream</span>
                Directory In-Feed Card Ads (Police, Hospital, Fire, Blood)
              </span>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono text-slate-500">In-Feed Enabled</span>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_directory_card_enabled: prev.ads_directory_card_enabled === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_directory_card_enabled === 'true' ? 'bg-blue-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_directory_card_enabled === 'true' ? 'translate-x-4' : 'translate-x-0'
                  }`} />
                </button>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                Card Frequency (Show ad after every X directory items)
              </label>
              <input
                type="number"
                min="2"
                max="20"
                value={remoteConfigs.ads_directory_card_interval || '4'}
                onChange={(e) => setRemoteConfigs(prev => ({ ...prev, ads_directory_card_interval: e.target.value }))}
                placeholder="4"
                className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              />
              <span className="text-[10px] text-slate-400">Controls how often an ad card is inserted in directory lists (e.g. 4 means 1 ad after every 4 police/hospital cards).</span>
            </div>
          </div>

          {/* History Screen Trip In-Feed Ads */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono flex items-center gap-1.5">
                <span className="material-icons text-sm text-emerald-600">history_edu</span>
                History Screen In-Feed Ads (Between Trip Cards)
              </span>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono text-slate-500">History In-Feed Enabled</span>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_history_card_enabled: prev.ads_history_card_enabled === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_history_card_enabled === 'true' ? 'bg-emerald-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_history_card_enabled === 'true' ? 'translate-x-4' : 'translate-x-0'
                  }`} />
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  Trip Frequency (Every X Trips)
                </label>
                <input
                  type="number"
                  min="1"
                  max="15"
                  value={remoteConfigs.ads_history_card_interval || '3'}
                  onChange={(e) => setRemoteConfigs(prev => ({ ...prev, ads_history_card_interval: e.target.value }))}
                  placeholder="3"
                  className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                />
                <span className="text-[10px] text-slate-400">Show an ad card after every X completed trip cards.</span>
              </div>

              <div className="space-y-1">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  Ad Card Size
                </label>
                <select
                  value={remoteConfigs.ads_history_card_size || 'medium_rectangle'}
                  onChange={(e) => setRemoteConfigs(prev => ({ ...prev, ads_history_card_size: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                >
                  <option value="medium_rectangle">Medium Rectangle (300x250) - Large Card</option>
                  <option value="large_banner">Large Banner (320x100) - Medium Card</option>
                  <option value="banner">Standard Banner (320x50) - Compact Card</option>
                </select>
                <span className="text-[10px] text-slate-400">Medium Rectangle is optimal for spacious trip history lists.</span>
              </div>
            </div>
          </div>

          {/* Alerts & Live Watch Screen In-Feed Ads */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono flex items-center gap-1.5">
                <span className="material-icons text-sm text-cyan-600">visibility</span>
                Alerts & Live Watch Screen Ads (Live Watch & History Tabs)
              </span>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono text-slate-500">Live Watch In-Feed Enabled</span>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_alert_card_enabled: prev.ads_alert_card_enabled === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_alert_card_enabled === 'true' ? 'bg-cyan-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_alert_card_enabled === 'true' ? 'translate-x-4' : 'translate-x-0'
                  }`} />
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  Share Item Frequency (Every X Items)
                </label>
                <input
                  type="number"
                  min="1"
                  max="15"
                  value={remoteConfigs.ads_alert_card_interval || '3'}
                  onChange={(e) => setRemoteConfigs(prev => ({ ...prev, ads_alert_card_interval: e.target.value }))}
                  placeholder="3"
                  className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500"
                />
                <span className="text-[10px] text-slate-400">Show an ad card after every X shared journey cards.</span>
              </div>

              <div className="space-y-1">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  Banner / Card Size
                </label>
                <select
                  value={remoteConfigs.ads_alert_card_size || 'large_banner'}
                  onChange={(e) => setRemoteConfigs(prev => ({ ...prev, ads_alert_card_size: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500"
                >
                  <option value="large_banner">Large Banner (320x100) - Medium Card</option>
                  <option value="medium_rectangle">Medium Rectangle (300x250) - Large Card</option>
                  <option value="banner">Standard Banner (320x50) - Compact Card</option>
                </select>
                <span className="text-[10px] text-slate-400">Controls banner dimensions in Live Watch and Shared History feeds.</span>
              </div>
            </div>
          </div>

          {/* Rewarded Video Ads */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono flex items-center gap-1.5">
                <span className="material-icons text-sm text-emerald-600">smart_display</span>
                Rewarded Video Ads (Wallet & Free SOS Credits)
              </span>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono text-slate-500">Rewarded Enabled</span>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_rewarded_enabled: prev.ads_rewarded_enabled === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_rewarded_enabled === 'true' ? 'bg-emerald-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_rewarded_enabled === 'true' ? 'translate-x-4' : 'translate-x-0'
                  }`} />
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-2 space-y-1">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  Rewarded Ad Unit ID
                </label>
                <input
                  type="text"
                  value={remoteConfigs.admob_rewarded_unit_id || ''}
                  onChange={(e) => setRemoteConfigs(prev => ({ ...prev, admob_rewarded_unit_id: e.target.value }))}
                  placeholder="ca-app-pub-3940256099942544/5224354917"
                  className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                />
                <span className="text-[10px] text-slate-400">Users willingly watch video to earn emergency SOS credits.</span>
              </div>

              <div className="space-y-1">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  SOS Credits Awarded
                </label>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={remoteConfigs.ads_reward_credits_amount || '1'}
                  onChange={(e) => setRemoteConfigs(prev => ({ ...prev, ads_reward_credits_amount: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                />
                <span className="text-[10px] text-slate-400">Credits added per completed video.</span>
              </div>
            </div>
          </div>

          {/* Interstitial Ads (Trip End Only) */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono flex items-center gap-1.5">
                <span className="material-icons text-sm text-purple-600">fullscreen</span>
                Interstitial Ads (Safe Trip Finish Only)
              </span>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono text-slate-500">Interstitial Enabled</span>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    ads_interstitial_enabled: prev.ads_interstitial_enabled === 'true' ? 'false' : 'true'
                  }))}
                  className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    remoteConfigs.ads_interstitial_enabled === 'true' ? 'bg-purple-500' : 'bg-slate-200'
                  }`}
                >
                  <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    remoteConfigs.ads_interstitial_enabled === 'true' ? 'translate-x-4' : 'translate-x-0'
                  }`} />
                </button>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                Interstitial Ad Unit ID
              </label>
              <input
                type="text"
                value={remoteConfigs.admob_interstitial_unit_id || ''}
                onChange={(e) => setRemoteConfigs(prev => ({ ...prev, admob_interstitial_unit_id: e.target.value }))}
                placeholder="ca-app-pub-3940256099942544/1033173712"
                className="w-full bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              />
              <div className="flex items-center gap-1.5 text-[10px] text-amber-700 bg-amber-50/80 p-2.5 rounded-lg border border-amber-200/80 mt-1">
                <span className="material-icons text-xs shrink-0">shield</span>
                <span>Safety Policy: Interstitial ads are strictly blocked during active journey tracking, emergency SOS, and nearby police/hospital directory searches.</span>
              </div>
            </div>
          </div>

          {/* Save Button for AdMob Configuration */}
          <div className="flex justify-end pt-2">
            <button
              type="button"
              onClick={handleSaveRemoteConfigs}
              disabled={isSavingCloud}
              className="bg-amber-600 hover:bg-amber-700 disabled:opacity-50 text-white font-mono text-xs font-bold uppercase tracking-wider px-6 py-3 rounded-xl transition-all flex items-center gap-2 shadow-md shadow-amber-600/20 cursor-pointer"
            >
              <span className={`material-icons text-sm ${isSavingCloud ? 'animate-spin' : ''}`}>
                {isSavingCloud ? 'sync' : 'check'}
              </span>
              <span>{isSavingCloud ? 'Saving to Supabase...' : 'Save AdMob Configuration'}</span>
            </button>
          </div>
        </div>
      )}

      {/* TAB 2: META AUDIENCE NETWORK (FUTURE EXPANSION) */}
      {activeNetworkTab === 'meta' && (
        <div className="space-y-6">
          <div className="bg-white p-6 rounded-2xl border border-blue-200/80 shadow-sm space-y-4">
            <div className="flex items-start justify-between">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="material-icons text-blue-600 text-xl">public</span>
                  <h3 className="font-bold text-sm text-slate-800 uppercase tracking-tight">
                    Meta Audience Network (Facebook Ads)
                  </h3>
                  <span className="bg-blue-100 text-blue-700 text-[9.5px] font-mono font-bold px-2 py-0.5 rounded-md border border-blue-200">
                    IN FUTURE ROADMAP
                  </span>
                </div>
                <p className="text-xs text-slate-500 leading-relaxed max-w-2xl">
                  Meta Audience Network integration will be enabled in upcoming mobile app releases. When active, you will be able to utilize AdMob Bidding Mediation or direct Meta placement delivery for maximized eCPM in South Asian and global traffic.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-3">
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  Meta App ID (Future)
                </label>
                <input
                  type="text"
                  disabled
                  value={remoteConfigs.meta_app_id || ''}
                  placeholder="Meta Business App ID (Coming Soon)"
                  className="w-full bg-slate-100 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-400 cursor-not-allowed"
                />
                <span className="text-[10px] text-slate-400">Will be linked to Meta Developers Business Manager.</span>
              </div>

              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                  Mediation Adapter Strategy
                </label>
                <select
                  disabled
                  className="w-full bg-slate-100 border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-400 cursor-not-allowed"
                >
                  <option>Google AdMob Bidding with Meta Audience Adapter (Recommended)</option>
                  <option>Waterfall Fallback Mediation</option>
                  <option>Direct Meta Delivery</option>
                </select>
                <span className="text-[10px] text-slate-400">Configured automatically when Meta SDK is bundled.</span>
              </div>
            </div>

            <div className="p-4 bg-blue-50/60 rounded-xl border border-blue-200/60 flex items-center gap-3">
              <span className="material-icons text-blue-600 text-lg shrink-0">info</span>
              <p className="text-xs text-blue-900 leading-relaxed font-sans">
                <strong>Status:</strong> Currently, Google AdMob is fully active and delivering banner, rewarded, and in-feed safety ads across the Android app. Meta Audience Network will be unlocked in Phase 2 monetization without requiring database structural changes.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
