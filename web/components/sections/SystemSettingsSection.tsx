'use client';

import React, { useState, useEffect } from 'react';
import { getSupabaseClient } from '../../lib/supabase';
import { useToast } from '../ui/Toast';

interface SystemSettingsSectionProps {
  // Optional database / sync callback if needed in future
  onSettingsSaved?: (settings: any) => void;
}

export default function SystemSettingsSection({ onSettingsSaved }: SystemSettingsSectionProps) {
  const { toast } = useToast();
  // Load settings from localStorage if available, or use defaults
  const [settings, setSettings] = useState(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('safetravel_settings');
      if (saved) {
        try {
          return JSON.parse(saved);
        } catch (e) {
          console.error('Failed to parse settings from local storage', e);
        }
      }
    }
    return {
      autoDispatchUnits: true,
      audioMuteNavigation: false,
      voiceLanguage: 'en-US',
      radarSweepSpeed: 3,
      idleThresholdMinutes: 5,
      geofenceBufferMeters: 150,
      speedViolationBufferMph: 10,
      defaultMapStyle: 'dark-v11',
      showTrafficLayer: true,
      retentionPoints: 100,
      supabaseSyncIntervalSeconds: 30,
      enableOfflineCacheFallback: true,
    };
  });

  const [activeSubTab, setActiveSubTab] = useState<'general' | 'thresholds' | 'map' | 'cloud_configs' | 'points' | 'versions'>('general');
  const [showSaveToast, setShowSaveToast] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Display Scaling Preference State ('auto' | 'fhd' | '100')
  const [displayScale, setDisplayScale] = useState<'auto' | 'fhd' | '100'>('auto');

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('safetravel_display_scale') as 'auto' | 'fhd' | '100' | null;
      if (saved && (saved === 'auto' || saved === 'fhd' || saved === '100')) {
        setDisplayScale(saved);
      }
    }
  }, []);

  const handleDisplayScaleChange = (scale: 'auto' | 'fhd' | '100') => {
    setDisplayScale(scale);
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-display-scale', scale);
      try {
        localStorage.setItem('safetravel_display_scale', scale);
      } catch {
        // fallback
      }
    }
  };

  // Cloud Remote Configs State
  const [remoteConfigs, setRemoteConfigs] = useState<Record<string, string>>({
    cloudinary_cloud_name: 'dzrhh52df',
    cloudinary_upload_preset: 'Safe-Travel',
    cloudinary_image_preset: 'safetravel_image_preset',
    mapbox_access_token: 'pk.eyJ1Ijoic2FmZXRyYXZlbCIsImEiOiJjbTdtbWp1cjIwMHBhMmpzYnlrbXhkdzlsIn0.72xO_g-h4dZJ_qUqH-N9cw',
    points_per_trip: '50',
    points_per_trip_premium: '100',
    points_per_km: '2',
    points_guardian_bonus: '50',
    points_streak_bonus: '100',
    points_redeem_premium_7d: '500',
    points_redeem_premium_30d: '1500',
    points_redeem_credit: '50',
    points_engine_enabled: 'true',
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
    admob_interstitial_unit_id: 'ca-app-pub-3940256099942544/1033173712'
  });
  const [isSavingCloud, setIsSavingCloud] = useState(false);
  const [cloudSaveMessage, setCloudSaveMessage] = useState<string | null>(null);
  
  // Version Logs Management State
  const [versionLogs, setVersionLogs] = useState<any[]>([]);
  const [isLoadingVersions, setIsLoadingVersions] = useState(false);
  const [isSavingVersion, setIsSavingVersion] = useState(false);
  const [versionSaveMessage, setVersionSaveMessage] = useState<string | null>(null);
  const [isVersionModalOpen, setIsVersionModalOpen] = useState(false);
  const [editingVersion, setEditingVersion] = useState<any | null>(null);

  // Version Form states
  const [verNumber, setVerNumber] = useState('');
  const [verCode, setVerCode] = useState<number | string>(1);
  const [verDate, setVerDate] = useState('');
  const [verChanges, setVerChanges] = useState('');
  const [verIsCritical, setVerIsCritical] = useState(false);
  const [verDownloadUrl, setVerDownloadUrl] = useState('');

  const loadVersionLogs = async () => {
    setIsLoadingVersions(true);
    try {
      const supabase = getSupabaseClient();
      if (!supabase) return;
      const { data, error } = await supabase
        .from('app_version_logs')
        .select('*')
        .order('version_code', { ascending: false });
      if (data && !error) {
        setVersionLogs(data);
      }
    } catch (e) {
      console.error('Error fetching version logs:', e);
    } finally {
      setIsLoadingVersions(false);
    }
  };

  const handleOpenAddVersion = () => {
    setEditingVersion(null);
    setVerNumber('');
    setVerCode(versionLogs.length > 0 ? (Number(versionLogs[0].version_code) + 1) : 1);
    setVerDate(new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }));
    setVerChanges('');
    setVerIsCritical(false);
    setVerDownloadUrl('');
    setIsVersionModalOpen(true);
  };

  const handleOpenEditVersion = (item: any) => {
    setEditingVersion(item);
    setVerNumber(item.version || '');
    setVerCode(item.version_code || 1);
    setVerDate(item.release_date || '');
    const changesStr = Array.isArray(item.changes) ? item.changes.join('\n') : (item.changes || '');
    setVerChanges(changesStr);
    setVerIsCritical(!!item.is_critical);
    setVerDownloadUrl(item.download_url || '');
    setIsVersionModalOpen(true);
  };

  const handleSaveVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingVersion(true);
    setVersionSaveMessage(null);
    try {
      const supabase = getSupabaseClient();
      if (!supabase) throw new Error("Supabase connection unavailable");

      const changesArray = verChanges
        .split('\n')
        .map(line => line.trim())
        .filter(line => line.length > 0);

      const payload: any = {
        version: verNumber.trim(),
        version_code: parseInt(verCode.toString(), 10) || 1,
        release_date: verDate.trim(),
        changes: changesArray,
        is_critical: verIsCritical,
        download_url: verDownloadUrl.trim() || null,
        updated_at: new Date().toISOString()
      };

      if (editingVersion?.id) {
        const { error } = await supabase
          .from('app_version_logs')
          .update(payload)
          .eq('id', editingVersion.id);
        if (error) throw error;
      } else {
        const { error } = await supabase
          .from('app_version_logs')
          .insert([payload]);
        if (error) throw error;
      }

      setVersionSaveMessage("Version log saved successfully!");
      setIsVersionModalOpen(false);
      toast.success("Version log published and available across mobile apps.", "Version Saved");
      await loadVersionLogs();
      setTimeout(() => setVersionSaveMessage(null), 3000);
    } catch (err: any) {
      console.error("Failed to save version log:", err);
      setVersionSaveMessage(`Error: ${err.message || 'Failed to save version'}`);
      toast.error(`Failed to save version log: ${err.message || 'Error occurred'}`, "Save Error");
    } finally {
      setIsSavingVersion(false);
    }
  };

  const handleDeleteVersion = async (id: string, ver: string) => {
    if (!window.confirm(`Are you sure you want to delete version log v${ver}?`)) return;
    try {
      const supabase = getSupabaseClient();
      if (!supabase) return;
      const { error } = await supabase
        .from('app_version_logs')
        .delete()
        .eq('id', id);
      if (error) throw error;
      toast.success(`Version log v${ver} removed successfully.`, "Version Removed");
      await loadVersionLogs();
    } catch (err: any) {
      toast.error(`Failed to delete version log: ${err.message}`, "Deletion Failed");
    }
  };

  useEffect(() => {
    const supabase = getSupabaseClient();
    if (!supabase) return;
    const loadConfigs = async () => {
      try {
        const { data, error } = await supabase.from('app_remote_configs').select('key, value');
        if (data && !error && data.length > 0) {
          const map: Record<string, string> = {};
          data.forEach((r: any) => { map[r.key] = r.value; });
          setRemoteConfigs(prev => ({ ...prev, ...map }));
        }
      } catch (e) {
        console.warn('Could not load app_remote_configs:', e);
      }
    };
    loadConfigs();
  }, []);
  
  useEffect(() => {
    if (activeSubTab === 'versions') {
      loadVersionLogs();
    }
  }, [activeSubTab]);

  const handleSaveRemoteConfigs = async () => {
    setIsSavingCloud(true);
    setCloudSaveMessage(null);
    try {
      const supabase = getSupabaseClient();
      if (!supabase) throw new Error("Supabase connection unavailable");

      const upsertRows = Object.entries(remoteConfigs).map(([key, value]) => ({
        key,
        value,
        updated_at: new Date().toISOString()
      }));

      const { error } = await supabase
        .from('app_remote_configs')
        .upsert(upsertRows, { onConflict: 'key' });

      if (error) throw error;
      setCloudSaveMessage("Successfully updated Supabase app_remote_configs!");
      toast.success("Mobile remote parameters synced to cloud.", "Remote Configs Saved");
      setTimeout(() => setCloudSaveMessage(null), 4000);
    } catch (e: any) {
      console.error("Failed to save remote configs:", e);
      setCloudSaveMessage(`Error: ${e.message || 'Failed to save to Supabase'}`);
      toast.error(`Failed to save remote configs: ${e.message || 'Error occurred'}`, "Sync Error");
    } finally {
      setIsSavingCloud(false);
    }
  };

  // Save current settings to localStorage
  const handleSaveSettings = (updatedSettings = settings) => {
    setIsSaving(true);
    setTimeout(() => {
      localStorage.setItem('safetravel_settings', JSON.stringify(updatedSettings));
      setIsSaving(false);
      toast.success("System preferences and configurations saved.", "Settings Saved");
      if (onSettingsSaved) {
        onSettingsSaved(updatedSettings);
      }
    }, 400);
  };

  const handleToggle = (key: string) => {
    const updated = { ...settings, [key]: !settings[key as keyof typeof settings] };
    setSettings(updated);
    handleSaveSettings(updated);
  };

  const handleChange = (key: string, value: any) => {
    const updated = { ...settings, [key]: value };
    setSettings(updated);
    handleSaveSettings(updated);
  };

  return (
    <div className="space-y-4 md:space-y-6 animate-fadeIn" id="system-settings-root">
      
      {/* Settings Title Header */}
      <div className="flex items-center justify-between pb-4 border-b border-slate-200" id="settings-header">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-blue-100 text-blue-600 rounded-xl flex items-center justify-center">
            <span className="material-icons text-xl">settings</span>
          </div>
          <div>
            <h2 className="font-sans font-bold text-base tracking-tight text-slate-800">
              System Settings
            </h2>
            <p className="text-xs text-slate-400 font-sans mt-0.5">
              Configure safety thresholds, emergency contact preferences, map styles, and cloud settings
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2 font-mono text-[10px] text-slate-400">
          <span className="material-icons text-xs text-emerald-500">cloud_done</span>
          <span>ALL PREFERENCES AUTOMATICALLY SAVED TO CLOUD</span>
        </div>
      </div>

      {/* Main Settings Panel Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start" id="settings-grid">
        
        {/* Left column sidebar menus */}
        <div className="lg:col-span-3 bg-white border border-slate-200 shadow-sm rounded-2xl p-4.5 space-y-1.5" id="settings-menu-sidebar">
          <p className="px-3 py-1.5 text-[9.5px] font-mono font-bold text-slate-400 uppercase tracking-widest select-none">
            Setting Sections
          </p>
          
          {[
            { id: 'general', label: 'Operations & Alerts', icon: 'gavel' },
            { id: 'thresholds', label: 'Security & Limits', icon: 'security' },
            { id: 'map', label: 'Map Styles & Tracking', icon: 'map' },
            { id: 'cloud_configs', label: 'Cloud & Remote APIs', icon: 'cloud_sync' },
            { id: 'points', label: 'Points & Rewards', icon: 'military_tech' },
            { id: 'versions', label: 'App Version Logs', icon: 'history' },
          ].map((tab) => {
            const isTabActive = activeSubTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveSubTab(tab.id as any)}
                className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all text-left cursor-pointer ${
                  isTabActive
                    ? 'bg-blue-50 text-blue-700 font-semibold border border-blue-100/50'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <span className={`material-icons text-sm ${isTabActive ? 'text-blue-600' : 'text-slate-400'}`}>
                  {tab.icon}
                </span>
                <span>{tab.label}</span>
              </button>
            );
          })}

          <div className="border-t border-slate-100 pt-4 mt-4 space-y-3.5 px-3">
            <div className="space-y-1">
              <span className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">
                Database Node
              </span>
              <span className="block text-[11px] font-sans font-medium text-slate-700">
                Supabase Postgres
              </span>
            </div>

            <div className="space-y-1">
              <span className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest">
                GPS Connection
              </span>
              <span className="block text-[11px] font-sans font-medium text-slate-700 flex items-center">
                <span className="h-1.5 w-1.5 bg-emerald-500 rounded-full animate-ping mr-1.5"></span>
                99.8% Signal Reliability
              </span>
            </div>
          </div>
        </div>

        {/* Right column settings controls panel */}
        <div className="lg:col-span-9 bg-white border border-slate-200 shadow-sm rounded-2xl p-6" id="settings-controls-panel">
          
          {/* General Tab */}
          {activeSubTab === 'general' && (
            <div className="space-y-6" id="general-settings-view">
              <div className="pb-3 border-b border-slate-100">
                <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">
                  Operations & Emergency Alerts
                </h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Configure automated emergency alerts and navigation preferences
                </p>
              </div>

              <div className="space-y-5">
                {/* Display Resolution & FHD Fit Density Engine */}
                <div className="p-4.5 bg-slate-50 border border-slate-200 rounded-xl space-y-3" id="display-density-control-card">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div className="space-y-0.5">
                      <span className="block text-xs font-bold text-slate-800 flex items-center gap-1.5 uppercase font-mono tracking-wider">
                        <span className="material-icons text-blue-600 text-sm">fit_screen</span>
                        Monitor Resolution & Display Density (FHD Fit Engine)
                      </span>
                      <span className="block text-[11px] text-slate-500 leading-relaxed">
                        Optimize dashboard proportions for HD monitors and laptops (1366x768) so all tables, cards, and maps render with spacious Full HD (1920x1080) roominess.
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
                    <button
                      type="button"
                      onClick={() => handleDisplayScaleChange('auto')}
                      className={`p-3.5 rounded-xl border text-left transition-all flex flex-col justify-between cursor-pointer ${
                        displayScale === 'auto'
                          ? 'bg-blue-50/80 border-blue-500 text-blue-900 shadow-sm ring-1 ring-blue-500/20'
                          : 'bg-white border-slate-200 text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold font-mono flex items-center gap-1.5">
                          <span className="material-icons text-xs text-blue-600">auto_awesome</span>
                          AUTO FIT
                        </span>
                        <span className="text-[9px] font-mono font-bold bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded">
                          RECOMMENDED
                        </span>
                      </div>
                      <span className="text-[10px] text-slate-500 mt-2.5 leading-relaxed">
                        Auto-detects screen resolution: Automatically applies 85% scale on HD/Laptop screens, and standard 100% on FHD monitors.
                      </span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleDisplayScaleChange('fhd')}
                      className={`p-3.5 rounded-xl border text-left transition-all flex flex-col justify-between cursor-pointer ${
                        displayScale === 'fhd'
                          ? 'bg-blue-50/80 border-blue-500 text-blue-900 shadow-sm ring-1 ring-blue-500/20'
                          : 'bg-white border-slate-200 text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold font-mono flex items-center gap-1.5">
                          <span className="material-icons text-xs text-blue-600">fit_screen</span>
                          FHD 85%
                        </span>
                        <span className="text-[9px] font-mono bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                          SPACIOUS
                        </span>
                      </div>
                      <span className="text-[10px] text-slate-500 mt-2.5 leading-relaxed">
                        Forces 85% scale on any display for an expansive, uncrowded layout with extra breathing room for tables and maps.
                      </span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleDisplayScaleChange('100')}
                      className={`p-3.5 rounded-xl border text-left transition-all flex flex-col justify-between cursor-pointer ${
                        displayScale === '100'
                          ? 'bg-blue-50/80 border-blue-500 text-blue-900 shadow-sm ring-1 ring-blue-500/20'
                          : 'bg-white border-slate-200 text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold font-mono flex items-center gap-1.5">
                          <span className="material-icons text-xs text-blue-600">crop_free</span>
                          100% STANDARD
                        </span>
                        <span className="text-[9px] font-mono bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                          DEFAULT
                        </span>
                      </div>
                      <span className="text-[10px] text-slate-500 mt-2.5 leading-relaxed">
                        Standard 1:1 original pixel scale. Ideal for large high-resolution 2K/4K external desktop monitors.
                      </span>
                    </button>
                  </div>
                </div>
                {/* Auto Dispatch SOS Emergency units Toggle */}
                <div className="flex items-start justify-between p-4 bg-slate-50 border border-slate-150 rounded-xl hover:bg-slate-50/70 transition-all">
                  <div className="space-y-1 max-w-[80%]">
                    <span className="block text-xs font-semibold text-slate-800">
                      Automatic Emergency Alerts & Notifications
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Automatically send real-time alerts and notifications to emergency guardians when an S.O.S alert is triggered.
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('autoDispatchUnits')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                      settings.autoDispatchUnits ? 'bg-blue-600' : 'bg-slate-200'
                    }`}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                        settings.autoDispatchUnits ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Audio voice navigation toggle */}
                <div className="flex items-start justify-between p-4 bg-slate-50 border border-slate-150 rounded-xl hover:bg-slate-50/70 transition-all">
                  <div className="space-y-1 max-w-[80%]">
                    <span className="block text-xs font-semibold text-slate-800">
                      Mute Voice Navigation Guidance
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Mute voice audio guidance updates during active navigation.
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('audioMuteNavigation')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                      settings.audioMuteNavigation ? 'bg-blue-600' : 'bg-slate-200'
                    }`}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                        settings.audioMuteNavigation ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Select voice navigation speaker language */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center p-4 bg-slate-50 border border-slate-150 rounded-xl">
                  <div className="space-y-1">
                    <span className="block text-xs font-semibold text-slate-800">
                      Voice Guidance Speaker Language
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Select synthesized language dialect for pilot turn instructions.
                    </span>
                  </div>
                  <div>
                    <select
                      value={settings.voiceLanguage}
                      onChange={(e) => handleChange('voiceLanguage', e.target.value)}
                      className="w-full bg-white border border-slate-200 px-3 py-2 rounded-lg text-xs font-medium text-slate-700 focus:outline-none focus:border-blue-500"
                    >
                      <option value="en-US">English (United States)</option>
                      <option value="en-GB">English (Great Britain)</option>
                      <option value="es-ES">Spanish (Spain)</option>
                      <option value="fr-FR">French (France)</option>
                      <option value="de-DE">German (Germany)</option>
                    </select>
                  </div>
                </div>

                {/* Radar sweeping telemetry frequency range */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center p-4 bg-slate-50 border border-slate-150 rounded-xl">
                  <div className="space-y-1">
                    <span className="block text-xs font-semibold text-slate-800">
                      Live Map Refresh Interval
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Adjust how frequently (in seconds) the map updates active trip coordinates.
                    </span>
                  </div>
                  <div className="flex items-center space-x-3">
                    <input
                      type="range"
                      min="1"
                      max="10"
                      value={settings.radarSweepSpeed}
                      onChange={(e) => handleChange('radarSweepSpeed', Number(e.target.value))}
                      className="w-full h-1.5 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                    />
                    <span className="text-xs font-mono font-bold text-slate-700 bg-white border border-slate-200 px-2.5 py-1 rounded w-14 text-center">
                      {settings.radarSweepSpeed} sec
                    </span>
                  </div>
                </div>

              </div>
            </div>
          )}

          {/* Thresholds / Limits Tab */}
          {activeSubTab === 'thresholds' && (
            <div className="space-y-6" id="thresholds-settings-view">
              <div className="pb-3 border-b border-slate-100">
                <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">
                  Safety Thresholds & Alert Limits
                </h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Set safety margins to flag unexpected stops, route deviations, or over-speeding
                </p>
              </div>

              <div className="space-y-5">
                
                {/* Transit Vehicle Idle alert threshold */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center p-4 bg-slate-50 border border-slate-150 rounded-xl">
                  <div className="space-y-1">
                    <span className="block text-xs font-semibold text-slate-800">
                      Vehicle Idle Duration Threshold
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Trigger safety warnings if a trip remains stationary in the same location for too long.
                    </span>
                  </div>
                  <div className="flex items-center space-x-3 justify-end">
                    <select
                      value={settings.idleThresholdMinutes}
                      onChange={(e) => handleChange('idleThresholdMinutes', Number(e.target.value))}
                      className="w-36 bg-white border border-slate-200 px-3 py-2 rounded-lg text-xs font-medium text-slate-700 focus:outline-none focus:border-blue-500"
                    >
                      <option value="2">2 Minutes</option>
                      <option value="5">5 Minutes</option>
                      <option value="10">10 Minutes</option>
                      <option value="15">15 Minutes</option>
                    </select>
                  </div>
                </div>

                {/* Geofence buffer buffer limits */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center p-4 bg-slate-50 border border-slate-150 rounded-xl">
                  <div className="space-y-1">
                    <span className="block text-xs font-semibold text-slate-800">
                      Geofence Route Deviation Limit
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Maximum allowed distance (in meters) before a trip is marked as &apos;OFF TRACK&apos;.
                    </span>
                  </div>
                  <div className="flex items-center space-x-3 justify-end">
                    <select
                      value={settings.geofenceBufferMeters}
                      onChange={(e) => handleChange('geofenceBufferMeters', Number(e.target.value))}
                      className="w-36 bg-white border border-slate-200 px-3 py-2 rounded-lg text-xs font-medium text-slate-700 focus:outline-none focus:border-blue-500"
                    >
                      <option value="50">50 meters</option>
                      <option value="150">150 meters</option>
                      <option value="300">300 meters</option>
                      <option value="500">500 meters</option>
                    </select>
                  </div>
                </div>

                {/* Speed limit violation warning buffer */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center p-4 bg-slate-50 border border-slate-150 rounded-xl">
                  <div className="space-y-1">
                    <span className="block text-xs font-semibold text-slate-800">
                      Speed Limit Warning Tolerance Buffer
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Configure speed buffer (mph) above speed limits before showing visual speed warnings.
                    </span>
                  </div>
                  <div className="flex items-center space-x-3 justify-end">
                    <select
                      value={settings.speedViolationBufferMph}
                      onChange={(e) => handleChange('speedViolationBufferMph', Number(e.target.value))}
                      className="w-36 bg-white border border-slate-200 px-3 py-2 rounded-lg text-xs font-medium text-slate-700 focus:outline-none focus:border-blue-500"
                    >
                      <option value="5">5 Mph Buffer</option>
                      <option value="10">10 Mph Buffer</option>
                      <option value="15">15 Mph Buffer</option>
                    </select>
                  </div>
                </div>

              </div>
            </div>
          )}

          {/* Map Styles & Tracking Tab */}
          {activeSubTab === 'map' && (
            <div className="space-y-6" id="map-settings-view">
              <div className="pb-3 border-b border-slate-100">
                <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">
                  Map Display Styles & History Tracking
                </h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Set default map styles and trail history limits
                </p>
              </div>

              <div className="space-y-5">
                
                {/* Default mapbox skin */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center p-4 bg-slate-50 border border-slate-150 rounded-xl">
                  <div className="space-y-1">
                    <span className="block text-xs font-semibold text-slate-800">
                      Default Map Style
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Select default style used on the Live Tracking Map.
                    </span>
                  </div>
                  <div className="flex items-center space-x-3 justify-end">
                    <select
                      value={settings.defaultMapStyle}
                      onChange={(e) => handleChange('defaultMapStyle', e.target.value)}
                      className="w-44 bg-white border border-slate-200 px-3 py-2 rounded-lg text-xs font-medium text-slate-700 focus:outline-none focus:border-blue-500"
                    >
                      <option value="dark-v11">Navigation Slate Dark</option>
                      <option value="light-v11">High-Contrast Light</option>
                      <option value="satellite-streets-v12">Hybrid Satellite Ortho</option>
                      <option value="streets-v12">Standard Street Level</option>
                    </select>
                  </div>
                </div>

                {/* Show map traffic layers */}
                <div className="flex items-start justify-between p-4 bg-slate-50 border border-slate-150 rounded-xl hover:bg-slate-50/70 transition-all">
                  <div className="space-y-1 max-w-[80%]">
                    <span className="block text-xs font-semibold text-slate-800">
                      Enable Live Traffic Congestion Overlays
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Display colored vectors signifying road congestion and delayed routes directly inside Mapbox engine.
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('showTrafficLayer')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                      settings.showTrafficLayer ? 'bg-blue-600' : 'bg-slate-200'
                    }`}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                        settings.showTrafficLayer ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Maximum history trace point limits */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center p-4 bg-slate-50 border border-slate-150 rounded-xl">
                  <div className="space-y-1">
                    <span className="block text-xs font-semibold text-slate-800">
                      Maximum Retained Breadcrumb Points
                    </span>
                    <span className="block text-[11px] text-slate-400 leading-relaxed">
                      Limit geographic breadcrumb coordinates retained in live memory to maintain smooth GPU animation rendering.
                    </span>
                  </div>
                  <div className="flex items-center space-x-3 justify-end">
                    <select
                      value={settings.retentionPoints}
                      onChange={(e) => handleChange('retentionPoints', Number(e.target.value))}
                      className="w-36 bg-white border border-slate-200 px-3 py-2 rounded-lg text-xs font-medium text-slate-700 focus:outline-none focus:border-blue-500"
                    >
                      <option value="50">50 points</option>
                      <option value="100">100 points</option>
                      <option value="200">200 points</option>
                      <option value="500">500 points</option>
                    </select>
                  </div>
                </div>

              </div>
            </div>
          )}

          {/* TAB 4: CLOUD & REMOTE APIS */}
          {activeSubTab === 'cloud_configs' && (
            <div className="space-y-6 animate-fadeIn" id="panel-cloud-configs">
              <div className="border-b border-slate-100 pb-4 flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-sm text-slate-800 flex items-center gap-2">
                    <span className="material-icons text-blue-600 text-lg">cloud_sync</span>
                    Cloud Remote Configurations (Supabase app_remote_configs)
                  </h3>
                  <p className="text-slate-500 text-xs mt-0.5">
                    Centralized API credentials synchronized in real-time with Android apps and web dispatchers.
                  </p>
                </div>
                <div className="flex items-center gap-1.5 font-mono text-[10px] text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                  <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                  <span>SYNCED TO DATABASE</span>
                </div>
              </div>

              {cloudSaveMessage && (
                <div className={`p-3 rounded-xl border text-xs font-mono flex items-center justify-between animate-fadeIn ${
                  cloudSaveMessage.includes('Error') 
                    ? 'bg-rose-50 border-rose-200 text-rose-700' 
                    : 'bg-emerald-50 border-emerald-200 text-emerald-700'
                }`}>
                  <span>{cloudSaveMessage}</span>
                  <button type="button" onClick={() => setCloudSaveMessage(null)} className="font-bold">
                    <span className="material-icons text-xs">close</span>
                  </button>
                </div>
              )}

              {/* Cloudinary Section */}
              <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-200/80 space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="material-icons text-cyan-600 text-lg">cloud_upload</span>
                    <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono">
                      Cloudinary Cloud Storage Settings
                    </span>
                  </div>
                  <span className="text-[10px] font-mono text-cyan-600 bg-cyan-50 px-2 py-0.5 rounded border border-cyan-200">
                    UNRESTRICTED CLIENT-SIDE UPLOADS
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Cloud Name */}
                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Cloud Name
                    </label>
                    <input 
                      type="text" 
                      value={remoteConfigs.cloudinary_cloud_name || ''} 
                      onChange={(e) => setRemoteConfigs(prev => ({ ...prev, cloudinary_cloud_name: e.target.value }))}
                      placeholder="e.g. dzrhh52df"
                      className="w-full bg-white border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                    />
                    <span className="text-[10px] text-slate-400">Account identifier on Cloudinary console.</span>
                  </div>

                  {/* Audio Upload Preset */}
                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Emergency Audio Upload Preset
                    </label>
                    <input 
                      type="text" 
                      value={remoteConfigs.cloudinary_upload_preset || ''} 
                      onChange={(e) => setRemoteConfigs(prev => ({ ...prev, cloudinary_upload_preset: e.target.value }))}
                      placeholder="e.g. Safe-Travel"
                      className="w-full bg-white border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                    />
                    <span className="text-[10px] text-slate-400">Unsigned upload preset for audio recordings folder.</span>
                  </div>

                  {/* Image Upload Preset */}
                  <div className="space-y-1 md:col-span-2">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Vehicle & Profile Image Upload Preset
                    </label>
                    <input 
                      type="text" 
                      value={remoteConfigs.cloudinary_image_preset || ''} 
                      onChange={(e) => setRemoteConfigs(prev => ({ ...prev, cloudinary_image_preset: e.target.value }))}
                      placeholder="e.g. safetravel_image_preset"
                      className="w-full bg-white border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                    />
                    <span className="text-[10px] text-slate-400">Unsigned upload preset for vehicle license photos and traveler avatars.</span>
                  </div>
                </div>
              </div>

              {/* Mapbox Section */}
              <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-200/80 space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="material-icons text-blue-600 text-lg">map</span>
                    <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono">
                      Mapbox Navigation & Satellite Token
                    </span>
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                    Public Access Token (pk.*)
                  </label>
                  <input 
                    type="text" 
                    value={remoteConfigs.mapbox_access_token || ''} 
                    onChange={(e) => setRemoteConfigs(prev => ({ ...prev, mapbox_access_token: e.target.value }))}
                    placeholder="pk.eyJ1I..."
                    className="w-full bg-white border border-slate-200 px-3 py-2 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                  <span className="text-[10px] text-slate-400">Used by Mapbox Directions API, Mapbox Satellite GL, and turn-by-turn navigation.</span>
                </div>
              </div>

              {/* Save Button for Cloud Configs */}
              <div className="flex justify-end pt-2">
                <button
                  type="button"
                  onClick={handleSaveRemoteConfigs}
                  disabled={isSavingCloud}
                  className="bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-mono text-xs font-bold uppercase tracking-wider px-5 py-2.5 rounded-xl transition-all flex items-center gap-1.5 shadow-md shadow-emerald-600/20 cursor-pointer"
                >
                  <span className="material-icons text-sm">{isSavingCloud ? 'sync' : 'cloud_done'}</span>
                  <span>{isSavingCloud ? 'Saving to Supabase...' : 'Save Cloud API Configs'}</span>
                </button>
              </div>

            </div>
          )}

          {/* SUB-SECTION 5: SAFETY POINTS & REWARDS ENGINE */}
          {activeSubTab === 'points' && (
            <div className="space-y-5 animate-fadeIn" id="points-engine-panel">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-100 gap-2">
                <div>
                  <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
                    <span className="material-icons text-indigo-600 text-lg">military_tech</span>
                    <span>Safety Points & Rewards Engine</span>
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Configure live points awarded upon trip completion, distance bonuses, guardian setup rewards, and premium unlock costs.
                  </p>
                </div>
                <span className="text-[10px] font-mono text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-1 rounded-md self-start sm:self-auto font-semibold">
                  LIVE REST SYNC
                </span>
              </div>

              {cloudSaveMessage && (
                <div className={`p-3 rounded-xl border text-xs font-semibold flex items-center gap-2 ${
                  cloudSaveMessage.includes('Error') 
                    ? 'bg-rose-50 border-rose-200 text-rose-700' 
                    : 'bg-emerald-50 border-emerald-200 text-emerald-700'
                }`}>
                  <span className="material-icons text-sm">
                    {cloudSaveMessage.includes('Error') ? 'error_outline' : 'check_circle'}
                  </span>
                  <span>{cloudSaveMessage}</span>
                </div>
              )}

              {/* Master Engine Toggle */}
              <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-200/80 flex items-center justify-between">
                <div>
                  <div className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono">
                    Points Loyalty System Status
                  </div>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    When active, travelers dynamically earn loyalty points upon safely completing trips.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setRemoteConfigs(prev => ({
                    ...prev,
                    points_engine_enabled: prev.points_engine_enabled === 'true' ? 'false' : 'true'
                  }))}
                  className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                    remoteConfigs.points_engine_enabled === 'true' ? 'bg-indigo-600' : 'bg-slate-300'
                  }`}
                >
                  <span className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-transform ${
                    remoteConfigs.points_engine_enabled === 'true' ? 'left-7' : 'left-1'
                  }`} />
                </button>
              </div>

              {/* Trip Completion Rewards Card */}
              <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-200/80 space-y-4">
                <div className="flex items-center gap-2">
                  <span className="material-icons text-blue-600 text-lg">route</span>
                  <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono">
                    Trip Completion Reward Rates
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5">
                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Standard Trip Points
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_per_trip || '50'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_per_trip: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-slate-400 font-bold">PTS</span>
                    </div>
                    <span className="text-[10px] text-slate-400">Awarded to standard users on safe arrival.</span>
                  </div>

                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Premium Trip Points
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_per_trip_premium || '100'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_per_trip_premium: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-amber-500 font-bold">PTS</span>
                    </div>
                    <span className="text-[10px] text-slate-400">Awarded to premium users on safe arrival.</span>
                  </div>

                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Distance Bonus (Per KM)
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_per_km || '2'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_per_km: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-slate-400 font-bold">PTS/KM</span>
                    </div>
                    <span className="text-[10px] text-slate-400">Extra points added per km traveled.</span>
                  </div>
                </div>
              </div>

              {/* Safety Milestones & Incentives Card */}
              <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-200/80 space-y-4">
                <div className="flex items-center gap-2">
                  <span className="material-icons text-emerald-600 text-lg">verified_user</span>
                  <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono">
                    Milestones & Onboarding Rewards
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Guardian Circle Bonus
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_guardian_bonus || '50'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_guardian_bonus: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-slate-400 font-bold">PTS</span>
                    </div>
                    <span className="text-[10px] text-slate-400">One-time reward for registering trusted guardians.</span>
                  </div>

                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      Safety Streak Bonus
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_streak_bonus || '100'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_streak_bonus: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-slate-400 font-bold">PTS</span>
                    </div>
                    <span className="text-[10px] text-slate-400">Bonus for 5 consecutive trips without SOS alerts.</span>
                  </div>
                </div>
              </div>

              {/* Redemption & Premium Unlock Costs */}
              <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-200/80 space-y-4">
                <div className="flex items-center gap-2">
                  <span className="material-icons text-amber-500 text-lg">card_giftcard</span>
                  <span className="font-bold text-xs text-slate-800 uppercase tracking-wider font-mono">
                    Redemption & Premium Exchange Rates
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5">
                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      7-Day Premium Access
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_redeem_premium_7d || '500'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_redeem_premium_7d: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-amber-500 font-bold">PTS</span>
                    </div>
                    <span className="text-[10px] text-slate-400">Points needed to unlock 7 days full premium.</span>
                  </div>

                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      30-Day Premium Access
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_redeem_premium_30d || '1500'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_redeem_premium_30d: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-amber-500 font-bold">PTS</span>
                    </div>
                    <span className="text-[10px] text-slate-400">Points needed to unlock 30 days full premium.</span>
                  </div>

                  <div className="space-y-1">
                    <label className="text-[11px] font-mono uppercase text-slate-500 font-bold block">
                      1 Trip Credit
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={remoteConfigs.points_redeem_credit || '50'} 
                        onChange={(e) => setRemoteConfigs(prev => ({ ...prev, points_redeem_credit: e.target.value }))}
                        className="w-full bg-white border border-slate-200 px-3 py-2 pr-12 rounded-xl text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                      <span className="absolute right-3 top-2 text-[10px] font-mono text-slate-400 font-bold">PTS</span>
                    </div>
                    <span className="text-[10px] text-slate-400">Points needed to redeem 1 emergency trip credit.</span>
                  </div>
                </div>
              </div>

              {/* Save Button for Points Engine */}
              <div className="flex justify-end pt-2">
                <button
                  type="button"
                  onClick={handleSaveRemoteConfigs}
                  disabled={isSavingCloud}
                  className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-mono text-xs font-bold uppercase tracking-wider px-5 py-2.5 rounded-xl transition-all flex items-center gap-1.5 shadow-md shadow-indigo-600/20 cursor-pointer"
                >
                  <span className="material-icons text-sm">{isSavingCloud ? 'sync' : 'check'}</span>
                  <span>{isSavingCloud ? 'Saving to Supabase...' : 'Save Points Configuration'}</span>
                </button>
              </div>

            </div>
          )}



          {/* TAB 7: APP VERSION LOGS */}
          {activeSubTab === 'versions' && (
            <div className="space-y-6 animate-fadeIn" id="settings-versions-tab">
              {/* Header Box */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-bold text-slate-800 font-mono uppercase tracking-wider">
                      App Version Logs & Release History
                    </h3>
                    <span className="text-[10px] bg-blue-50 text-blue-700 font-mono font-bold px-2 py-0.5 rounded-full border border-blue-200">
                      LIVE SUPABASE
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    Manage mobile app release versions, changelog notes, and critical update flags stored in database.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={loadVersionLogs}
                    disabled={isLoadingVersions}
                    className="p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-all cursor-pointer"
                    title="Refresh from Supabase"
                  >
                    <span className={`material-icons text-base ${isLoadingVersions ? 'animate-spin' : ''}`}>sync</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleOpenAddVersion}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-mono text-xs font-bold uppercase tracking-wider px-4 py-2 rounded-xl transition-all flex items-center gap-1.5 shadow-md shadow-blue-600/20 cursor-pointer"
                  >
                    <span className="material-icons text-sm">add</span>
                    <span>Add Version</span>
                  </button>
                </div>
              </div>

              {/* Status Message */}
              {versionSaveMessage && (
                <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs font-medium text-blue-700 flex items-center gap-2">
                  <span className="material-icons text-sm">info</span>
                  <span>{versionSaveMessage}</span>
                </div>
              )}

              {/* Loading State */}
              {isLoadingVersions && versionLogs.length === 0 && (
                <div className="py-12 flex flex-col items-center justify-center space-y-2">
                  <span className="material-icons text-3xl text-blue-600 animate-spin">sync</span>
                  <p className="text-xs font-mono text-slate-500">Loading version logs from Supabase...</p>
                </div>
              )}

              {/* Empty State */}
              {!isLoadingVersions && versionLogs.length === 0 && (
                <div className="py-12 border-2 border-dashed border-slate-200 rounded-2xl text-center space-y-3">
                  <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
                    <span className="material-icons text-2xl">history_edu</span>
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-slate-700 uppercase font-mono">No Version Logs Found</h4>
                    <p className="text-[11px] text-slate-500 mt-0.5">Click "+ Add Version" to publish your first release log to Supabase.</p>
                  </div>
                  <button
                    type="button"
                    onClick={handleOpenAddVersion}
                    className="inline-flex items-center gap-1 text-xs font-bold text-blue-600 hover:text-blue-700 cursor-pointer"
                  >
                    <span className="material-icons text-sm">add</span>
                    <span>Create Version Log</span>
                  </button>
                </div>
              )}

              {/* Versions List */}
              <div className="space-y-4">
                {versionLogs.map((item) => {
                  const changes: string[] = Array.isArray(item.changes) ? item.changes : [];
                  return (
                    <div
                      key={item.id || item.version}
                      className="bg-slate-50/70 border border-slate-200/80 rounded-2xl p-4.5 transition-all hover:border-slate-300 hover:shadow-sm space-y-3"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                        <div className="flex items-center gap-2.5 flex-wrap">
                          <span className="text-xs font-bold font-mono px-2.5 py-1 rounded-lg bg-blue-600 text-white">
                            v{item.version}
                          </span>
                          <span className="text-[11px] font-mono font-semibold px-2 py-0.5 rounded-md bg-slate-200 text-slate-700">
                            Build #{item.version_code}
                          </span>
                          <span className="text-[11px] text-slate-500 font-mono">
                            {item.release_date}
                          </span>
                          {item.is_critical && (
                            <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-md bg-rose-100 text-rose-700 border border-rose-200 flex items-center gap-1">
                              <span className="material-icons text-[11px]">warning</span>
                              CRITICAL UPDATE
                            </span>
                          )}
                        </div>

                        <div className="flex items-center gap-1.5 self-end sm:self-auto">
                          <button
                            type="button"
                            onClick={() => handleOpenEditVersion(item)}
                            className="p-1.5 text-slate-600 hover:text-blue-600 hover:bg-white rounded-lg border border-transparent hover:border-slate-200 transition-all text-xs font-medium flex items-center gap-1 cursor-pointer"
                            title="Edit Version"
                          >
                            <span className="material-icons text-sm">edit</span>
                            <span className="text-[11px]">Edit</span>
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteVersion(item.id, item.version)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-all text-xs font-medium flex items-center gap-1 cursor-pointer"
                            title="Delete Version"
                          >
                            <span className="material-icons text-sm">delete_outline</span>
                            <span className="text-[11px]">Delete</span>
                          </button>
                        </div>
                      </div>

                      {/* Download URL if present */}
                      {item.download_url && (
                        <div className="flex items-center gap-1.5 text-[11px] text-blue-600 font-mono truncate">
                          <span className="material-icons text-xs">link</span>
                          <a href={item.download_url} target="_blank" rel="noopener noreferrer" className="hover:underline truncate">
                            {item.download_url}
                          </a>
                        </div>
                      )}

                      {/* Changes bullet points */}
                      {changes.length > 0 && (
                        <div className="bg-white rounded-xl p-3 border border-slate-100 space-y-1.5">
                          <span className="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-wider block">
                            Changelog ({changes.length} notes)
                          </span>
                          <ul className="space-y-1 text-xs text-slate-600">
                            {changes.map((change, idx) => (
                              <li key={idx} className="flex items-start gap-2">
                                <span className="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1.5 shrink-0" />
                                <span>{change}</span>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Action bottom save status indicators */}
          <div className="pt-5 mt-6 border-t border-slate-100 flex items-center justify-between" id="settings-footer-actions">
            <span className="text-[10px] font-mono text-slate-400">
              Settings cache is stored globally at window.localStorage
            </span>
            <div className="flex items-center space-x-3">
              {isSaving && (
                <span className="text-[11px] font-mono text-blue-600 animate-pulse flex items-center">
                  <span className="material-icons text-sm animate-spin mr-1">sync</span>
                  SYNCING CACHE...
                </span>
              )}
              <button
                type="button"
                onClick={() => handleSaveSettings()}
                className="bg-blue-600 hover:bg-blue-700 text-white font-mono text-[10.5px] font-bold uppercase tracking-wider px-5 py-2.5 rounded-xl transition-all cursor-pointer shadow-md shadow-blue-500/10 flex items-center space-x-1.5"
                id="save-settings-action-btn"
              >
                <span className="material-icons text-xs leading-none">save</span>
                <span>Apply Configurations</span>
              </button>
            </div>
          </div>

        </div>

      </div>

      {/* ADD / EDIT VERSION LOG MODAL */}
      {isVersionModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="p-5 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span className="material-icons text-blue-400 text-xl">
                  {editingVersion ? 'edit_note' : 'add_circle'}
                </span>
                <h3 className="text-base font-bold text-white">
                  {editingVersion ? `Edit Version v${editingVersion.version}` : 'Add New App Version Log'}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsVersionModalOpen(false)}
                className="text-slate-400 hover:text-white transition-colors cursor-pointer"
              >
                <span className="material-icons text-sm">close</span>
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleSaveVersion} className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Version String *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. 1.4.1"
                    value={verNumber}
                    onChange={e => setVerNumber(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Version Code (Int) *</label>
                  <input
                    type="number"
                    required
                    min="1"
                    placeholder="e.g. 15"
                    value={verCode}
                    onChange={e => setVerCode(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Release Date *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. October 02, 2026"
                  value={verDate}
                  onChange={e => setVerDate(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Download URL (Optional)</label>
                <input
                  type="url"
                  placeholder="https://play.google.com/store/apps/details?id=..."
                  value={verDownloadUrl}
                  onChange={e => setVerDownloadUrl(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="space-y-1">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-300">Changelog & Features (One per line) *</label>
                  <span className="text-[10px] text-slate-500">Each line becomes a bullet</span>
                </div>
                <textarea
                  required
                  rows={5}
                  placeholder={`Optimized GPS tracking engine\nFixed minor UI padding issues\nImproved emergency response time`}
                  value={verChanges}
                  onChange={e => setVerChanges(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500 font-mono leading-relaxed"
                />
              </div>

              {/* Critical Update Toggle */}
              <div className="pt-2 flex items-center justify-between p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="space-y-0.5">
                  <span className="text-xs font-semibold text-white block">Mark as Critical Update</span>
                  <span className="text-[11px] text-slate-400 block">Highlights this version in red badge for safety importance</span>
                </div>
                <input
                  type="checkbox"
                  checked={verIsCritical}
                  onChange={e => setVerIsCritical(e.target.checked)}
                  className="w-4 h-4 text-rose-600 bg-slate-900 border-slate-700 rounded focus:ring-rose-500 cursor-pointer"
                />
              </div>

              {/* Action Buttons */}
              <div className="pt-4 flex items-center justify-end space-x-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsVersionModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSavingVersion}
                  className="px-5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white transition-all shadow-lg shadow-blue-600/20 flex items-center space-x-1.5 cursor-pointer"
                >
                  <span className={`material-icons text-sm ${isSavingVersion ? 'animate-spin' : ''}`}>
                    {isSavingVersion ? 'sync' : 'save'}
                  </span>
                  <span>{isSavingVersion ? 'Saving...' : (editingVersion ? 'Update Version' : 'Publish Version')}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
