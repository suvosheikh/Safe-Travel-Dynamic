'use client';

import React, { useState } from 'react';
import { 
  DbState, 
  Banner, 
  getSupabaseClient, 
  saveLocalDatabase, 
  hasSupabaseConfig 
} from '../../lib/supabase';

interface PromoBannerSectionProps {
  db: DbState;
  setDb: React.Dispatch<React.SetStateAction<DbState>>;
  currentUser?: { email: string; role: 'admin' | 'dispatcher' | 'user'; name: string; id: string } | null;
}

// Convert ISO string to datetime-local input string (YYYY-MM-DDTHH:mm)
function formatForDateTimeInput(iso?: string | null): string {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '';
    const pad = (n: number) => String(n).padStart(2, '0');
    const yyyy = d.getFullYear();
    const mm = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());
    const hh = pad(d.getHours());
    const min = pad(d.getMinutes());
    return `${yyyy}-${mm}-${dd}T${hh}:${min}`;
  } catch {
    return '';
  }
}

export default function PromoBannerSection({ db, setDb, currentUser }: PromoBannerSectionProps) {
  // Role-Based Access Control: Support admin, super_admin, or case-insensitive admin
  const roleStr = String(currentUser?.role || '').toLowerCase();
  const isAdmin = !currentUser || roleStr === 'admin' || roleStr === 'super_admin' || roleStr.includes('admin');

  // Wizard Modal States
  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [editedBannerTitle, setEditedBannerTitle] = useState('');
  const [editedBannerImageUrl, setEditedBannerImageUrl] = useState('');
  const [editedBannerActionUrl, setEditedBannerActionUrl] = useState('');
  const [editedBannerDisplayOrder, setEditedBannerDisplayOrder] = useState<number>(0);
  const [editedBannerStartDate, setEditedBannerStartDate] = useState<string>('');
  const [editedBannerEndDate, setEditedBannerEndDate] = useState<string>('');
  const [dateValidationError, setDateValidationError] = useState<string>('');
  const [editingBannerId, setEditingBannerId] = useState<string | null>(null);
  const [isSavingBanner, setIsSavingBanner] = useState(false);
  const [bannerSaveSuccess, setBannerSaveSuccess] = useState(false);
  const [deleteTargetBanner, setDeleteTargetBanner] = useState<Banner | null>(null);
  const [isDeletingBanner, setIsDeletingBanner] = useState(false);
  const [bannerDeleteSuccess, setBannerDeleteSuccess] = useState(false);

  // Open Add New Wizard
  const handleOpenAddNewWizard = () => {
    if (!isAdmin) return;
    setEditingBannerId(null);
    setEditedBannerTitle('');
    setEditedBannerImageUrl('');
    setEditedBannerActionUrl('');
    // Suggest next display order (current highest + 1)
    const maxOrder = db.banners?.reduce((max, b) => Math.max(max, b.display_order ?? 0), -1) ?? -1;
    setEditedBannerDisplayOrder(maxOrder + 1);
    setEditedBannerStartDate('');
    setEditedBannerEndDate('');
    setDateValidationError('');
    setIsWizardOpen(true);
  };

  // Open Edit Wizard
  const handleStartEditBanner = (banner: Banner) => {
    if (!isAdmin) return;
    setEditingBannerId(banner.id);
    setEditedBannerTitle(banner.title);
    setEditedBannerImageUrl(banner.image_url);
    setEditedBannerActionUrl(banner.action_url || '');
    setEditedBannerDisplayOrder(banner.display_order ?? 0);
    setEditedBannerStartDate(formatForDateTimeInput(banner.start_date));
    setEditedBannerEndDate(formatForDateTimeInput(banner.end_date));
    setDateValidationError('');
    setIsWizardOpen(true);
  };

  const handleCancelWizard = () => {
    setIsWizardOpen(false);
    setEditingBannerId(null);
    setEditedBannerTitle('');
    setEditedBannerImageUrl('');
    setEditedBannerActionUrl('');
    setEditedBannerDisplayOrder(0);
    setEditedBannerStartDate('');
    setEditedBannerEndDate('');
    setDateValidationError('');
  };

  // Quick Preset Handlers for Campaign Duration
  const applyDurationPreset = (days: number) => {
    const now = new Date();
    const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
    setEditedBannerStartDate(formatForDateTimeInput(now.toISOString()));
    setEditedBannerEndDate(formatForDateTimeInput(end.toISOString()));
    setDateValidationError('');
  };

  // Safe handler to save or update banners
  const handleSaveBanner = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAdmin) return;
    if (!editedBannerTitle.trim() || !editedBannerImageUrl.trim()) return;

    // Date Validation
    if (editedBannerStartDate && editedBannerEndDate) {
      const startMs = new Date(editedBannerStartDate).getTime();
      const endMs = new Date(editedBannerEndDate).getTime();
      if (endMs <= startMs) {
        setDateValidationError('Campaign End Date must be after Start Date.');
        return;
      }
    }
    setDateValidationError('');

    setIsSavingBanner(true);
    const updatedBannerVal = editedBannerImageUrl.trim();
    const updatedBannerTitle = editedBannerTitle.trim();
    const updatedActionUrl = editedBannerActionUrl.trim() || null;
    const updatedDisplayOrder = editedBannerDisplayOrder;
    const updatedStartDate = editedBannerStartDate ? new Date(editedBannerStartDate).toISOString() : null;
    const updatedEndDate = editedBannerEndDate ? new Date(editedBannerEndDate).toISOString() : null;
    const isNew = !editingBannerId;

    let updatedBanners = [...(db.banners || [])];
    const tempId = 'banner-' + Math.random().toString(36).substring(2, 11);

    if (!isNew) {
      // Modify active item in standard array
      updatedBanners = updatedBanners.map(b => 
        b.id === editingBannerId 
          ? { 
              ...b, 
              title: updatedBannerTitle, 
              image_url: updatedBannerVal,
              action_url: updatedActionUrl,
              display_order: updatedDisplayOrder,
              start_date: updatedStartDate,
              end_date: updatedEndDate
            }
          : b
      );
    } else {
      // Multi-Active: Keep other banners active! Add the new banner as active
      updatedBanners.push({
        id: tempId,
        title: updatedBannerTitle,
        image_url: updatedBannerVal,
        action_url: updatedActionUrl,
        display_order: updatedDisplayOrder,
        start_date: updatedStartDate,
        end_date: updatedEndDate,
        is_active: true,
        created_at: new Date().toISOString()
      });
    }

    // Capture first active banner for fallback bannerUrl
    const activeB = updatedBanners.find(b => b.is_active) || updatedBanners[0];
    const finalActiveUrl = activeB ? activeB.image_url : updatedBannerVal;

    const updatedDb: DbState = {
      ...db,
      banners: updatedBanners,
      bannerUrl: finalActiveUrl
    };
    
    saveLocalDatabase(updatedDb);
    setDb(updatedDb);

    // Synchronize to the live Cloud Supabase database
    const supabase = getSupabaseClient();
    if (supabase) {
      try {
        if (!isNew) {
          if (!editingBannerId.startsWith('banner-')) {
            const { error } = await supabase
              .from('banners')
              .update({ 
                title: updatedBannerTitle, 
                image_url: updatedBannerVal,
                action_url: updatedActionUrl,
                display_order: updatedDisplayOrder,
                start_date: updatedStartDate,
                end_date: updatedEndDate
              })
              .eq('id', editingBannerId);

            if (error) {
              console.error('Error updating banner in Supabase:', error);
            }
          }
        } else {
          // Multi-Active: Do NOT deactivate prior banners on Supabase!
          const { data, error } = await supabase
            .from('banners')
            .insert([{ 
              title: updatedBannerTitle, 
              image_url: updatedBannerVal,
              action_url: updatedActionUrl,
              display_order: updatedDisplayOrder,
              start_date: updatedStartDate,
              end_date: updatedEndDate,
              is_active: true 
            }])
            .select();

          if (error) {
            console.error('Error inserting banner into Supabase:', error);
          } else if (data && data[0]) {
            const realBanner: Banner = {
              id: String(data[0].id),
              title: data[0].title,
              image_url: data[0].image_url,
              action_url: data[0].action_url,
              display_order: Number(data[0].display_order),
              start_date: data[0].start_date || updatedStartDate,
              end_date: data[0].end_date || updatedEndDate,
              is_active: Boolean(data[0].is_active),
              created_at: data[0].created_at
            };

            setDb(currentDb => {
              const cleanedBanners = currentDb.banners.map(b => 
                b.id === tempId ? realBanner : b
              );
              const nextDb = {
                ...currentDb,
                banners: cleanedBanners,
                bannerUrl: realBanner.image_url
              };
              saveLocalDatabase(nextDb);
              return nextDb;
            });
          }
        }
      } catch (err) {
        console.error('Cloud synchronization exception:', err);
      }
    }

    setIsSavingBanner(false);
    setBannerSaveSuccess(true);
    setIsWizardOpen(false);
    
    setEditingBannerId(null);
    setEditedBannerTitle('');
    setEditedBannerImageUrl('');
    setEditedBannerActionUrl('');
    setEditedBannerDisplayOrder(0);
    setEditedBannerStartDate('');
    setEditedBannerEndDate('');

    setTimeout(() => {
      setBannerSaveSuccess(false);
    }, 4000);
  };

  // Toggle active status: ONLY modifies targeted banner (Multiple active banners supported)
  const handleToggleActiveBanner = async (bannerId: string | number, currentStatus: boolean) => {
    if (!isAdmin) return;
    const idStr = String(bannerId);
    const targetStatus = !currentStatus;

    const updatedBanners = db.banners.map(b => 
      String(b.id) === idStr ? { ...b, is_active: targetStatus } : b
    );

    const activeB = updatedBanners.find(b => b.is_active);
    const finalActiveUrl = activeB ? activeB.image_url : (updatedBanners[0]?.image_url || 'https://images.unsplash.com/photo-1506015391300-4802dc74de2e?auto=format&fit=crop&w=1200&h=675&q=80');

    const updatedDb: DbState = {
      ...db,
      banners: updatedBanners,
      bannerUrl: finalActiveUrl
    };
    
    saveLocalDatabase(updatedDb);
    setDb(updatedDb);

    if (idStr.startsWith('banner-')) {
      return;
    }

    const supabase = getSupabaseClient();
    if (supabase) {
      try {
        // Multi-Active: Update ONLY this targeted banner row
        const { error } = await supabase
          .from('banners')
          .update({ is_active: targetStatus })
          .eq('id', idStr);

        if (error) {
          console.error('Error updating active state in Supabase:', error);
        }
      } catch (err) {
        console.error('Failed to toggle banner active state on Supabase:', err);
      }
    }
  };

  // Carousel Reordering: Move banner up or down
  const handleMoveOrder = async (bannerId: string | number, direction: 'up' | 'down') => {
    if (!isAdmin) return;
    const idStr = String(bannerId);

    // Sort existing banners by display_order ascending
    const sorted = [...(db.banners || [])].sort((a, b) => (a.display_order ?? 0) - (b.display_order ?? 0));
    const currentIndex = sorted.findIndex(b => String(b.id) === idStr);
    if (currentIndex === -1) return;

    const targetIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
    if (targetIndex < 0 || targetIndex >= sorted.length) return;

    const currentBanner = sorted[currentIndex];
    const neighborBanner = sorted[targetIndex];

    // Swap their display_order values
    const currentOrder = currentBanner.display_order ?? currentIndex;
    const neighborOrder = neighborBanner.display_order ?? targetIndex;

    const newCurrentOrder = currentOrder === neighborOrder 
      ? (direction === 'up' ? neighborOrder - 1 : neighborOrder + 1)
      : neighborOrder;
    const newNeighborOrder = currentOrder === neighborOrder 
      ? neighborOrder 
      : currentOrder;

    const updatedBanners = db.banners.map(b => {
      if (String(b.id) === String(currentBanner.id)) {
        return { ...b, display_order: newCurrentOrder };
      }
      if (String(b.id) === String(neighborBanner.id)) {
        return { ...b, display_order: newNeighborOrder };
      }
      return b;
    });

    const updatedDb: DbState = {
      ...db,
      banners: updatedBanners
    };

    saveLocalDatabase(updatedDb);
    setDb(updatedDb);

    // Sync swapped orders to Supabase
    const supabase = getSupabaseClient();
    if (supabase) {
      try {
        if (!String(currentBanner.id).startsWith('banner-')) {
          await supabase.from('banners').update({ display_order: newCurrentOrder }).eq('id', String(currentBanner.id));
        }
        if (!String(neighborBanner.id).startsWith('banner-')) {
          await supabase.from('banners').update({ display_order: newNeighborOrder }).eq('id', String(neighborBanner.id));
        }
      } catch (err) {
        console.error('Failed to sync reordered banners to Supabase:', err);
      }
    }
  };

  // Delete banner: removes banner row completely
  const handleDeleteBanner = async (bannerId: string | number) => {
    if (!isAdmin) return;
    const idStr = String(bannerId);
    setIsDeletingBanner(true);

    try {
      const backupBanners = [...(db.banners || [])];
      const updatedBanners = backupBanners.filter(b => String(b.id) !== idStr);
      
      const wasActive = backupBanners.find(b => String(b.id) === idStr)?.is_active;
      let finalActiveUrl = db.bannerUrl;

      if (wasActive) {
        const activeB = updatedBanners.find(b => b.is_active) || updatedBanners[0];
        finalActiveUrl = activeB ? activeB.image_url : 'https://images.unsplash.com/photo-1506015391300-4802dc74de2e?auto=format&fit=crop&w=1200&h=675&q=80';
      }

      const updatedDb: DbState = {
        ...db,
        banners: updatedBanners,
        bannerUrl: finalActiveUrl
      };
      
      saveLocalDatabase(updatedDb);
      setDb(updatedDb);
      setBannerDeleteSuccess(true);
      setDeleteTargetBanner(null);
      setTimeout(() => {
        setBannerDeleteSuccess(false);
      }, 4000);

      // Remove from remote Supabase cloud database
      const bannerToDelete = backupBanners.find(b => String(b.id) === idStr);
      const supabase = getSupabaseClient();
      if (supabase) {
        const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(idStr);
        if (isUuid) {
          const { error } = await supabase
            .from('banners')
            .delete()
            .eq('id', idStr);

          if (error) {
            console.error('Error deleting banner from Supabase by ID:', error);
          }
        } else if (bannerToDelete?.title) {
          // Fallback: If banner was stored with a non-UUID ID, delete by matching title
          const { error } = await supabase
            .from('banners')
            .delete()
            .eq('title', bannerToDelete.title);

          if (error) {
            console.error('Error deleting banner from Supabase by title:', error);
          }
        }
      }
    } catch (err) {
      console.error('Failed to execute banner deletion:', err);
    } finally {
      setIsDeletingBanner(false);
    }
  };

  // Helper to determine dynamic banner lifecycle status
  const getBannerLifecycle = (banner: Banner): {
    status: 'active' | 'scheduled' | 'expired' | 'inactive';
    label: string;
    badgeClass: string;
    icon: string;
    subtext: string;
  } => {
    if (!banner.is_active) {
      return {
        status: 'inactive',
        label: 'Inactive',
        badgeClass: 'bg-slate-100 text-slate-500 border-slate-200',
        icon: 'toggle_off',
        subtext: 'Manually paused'
      };
    }

    const now = Date.now();
    const startMs = banner.start_date ? new Date(banner.start_date).getTime() : null;
    const endMs = banner.end_date ? new Date(banner.end_date).getTime() : null;

    if (startMs && startMs > now) {
      const startStr = new Date(banner.start_date!).toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
      return {
        status: 'scheduled',
        label: 'Scheduled',
        badgeClass: 'bg-indigo-50 text-indigo-700 border-indigo-200',
        icon: 'schedule',
        subtext: `Starts ${startStr}`
      };
    }

    if (endMs && endMs < now) {
      const endStr = new Date(banner.end_date!).toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
      return {
        status: 'expired',
        label: 'Expired',
        badgeClass: 'bg-amber-50 text-amber-700 border-amber-200',
        icon: 'event_busy',
        subtext: `Ended ${endStr}`
      };
    }

    let subtext = 'Live in App & Web';
    if (endMs) {
      const endStr = new Date(banner.end_date!).toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
      subtext = `Until ${endStr}`;
    }

    return {
      status: 'active',
      label: 'Live Active',
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      icon: 'sensors',
      subtext
    };
  };

  // Sort banners by display_order ascending for display
  const sortedBanners = [...(db.banners || [])].sort((a, b) => (a.display_order ?? 0) - (b.display_order ?? 0));
  const activeBannersCount = sortedBanners.filter(b => b.is_active).length;

  return (
    <div id="promo-banner-manager-container" className="space-y-4 md:space-y-6">
      
      {/* Header with Role Badge & Add Button */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex flex-col space-y-1">
          <div className="flex items-center space-x-2 flex-wrap gap-y-1">
            <h1 className="text-xl font-bold tracking-tight text-slate-800 flex items-center">
              <span className="material-icons text-blue-600 mr-2 text-2xl">aspect_ratio</span>
              Homepage Customizer & Settings (Promo Banner)
            </h1>
            {isAdmin ? (
              <span className="bg-blue-50 text-blue-700 border border-blue-200 text-[10px] font-mono font-bold px-2 py-0.5 rounded-full flex items-center space-x-1">
                <span className="material-icons text-xs">admin_panel_settings</span>
                <span>ADMIN FULL ACCESS</span>
              </span>
            ) : (
              <span className="bg-amber-50 text-amber-700 border border-amber-200 text-[10px] font-mono font-bold px-2 py-0.5 rounded-full flex items-center space-x-1">
                <span className="material-icons text-xs">visibility</span>
                <span>VIEW-ONLY ({currentUser?.role?.toUpperCase() || 'DISPATCHER'})</span>
              </span>
            )}
          </div>
          <p className="text-slate-500 text-xs">
            Configure multi-banner carousel graphics, reorder sequence weights, schedule holiday campaigns, and monitor live promotion statuses.
          </p>
        </div>

        {isAdmin ? (
          <button 
            type="button"
            onClick={handleOpenAddNewWizard}
            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs px-4 py-2.5 rounded-xl border border-blue-700 hover:border-blue-800 shadow-md flex items-center justify-center space-x-1.5 transition-all cursor-pointer hover:shadow-lg shrink-0"
            id="add-new-banner-btn"
          >
            <span className="material-icons text-sm">add_to_photos</span>
            <span>Add New Banner</span>
          </button>
        ) : (
          <div className="text-slate-400 text-[11px] font-mono bg-slate-100 border border-slate-200 px-3 py-2 rounded-xl flex items-center space-x-1.5 shrink-0">
            <span className="material-icons text-sm text-slate-400">lock</span>
            <span>Admin permission required to add</span>
          </div>
        )}
      </div>

      {/* Non-Admin RBAC Notice */}
      {!isAdmin && (
        <div className="flex items-center space-x-3 bg-amber-50 border border-amber-200 p-3.5 rounded-xl text-amber-800 text-xs animate-fadeIn">
          <span className="material-icons text-amber-600 text-lg">shield</span>
          <div className="flex-1">
            <span className="font-bold uppercase tracking-wider">Restricted Modification Privileges</span>
            <p className="text-[11px] text-amber-700 mt-0.5">
              You are signed in as a <span className="font-bold uppercase">{currentUser?.role || 'Dispatcher'}</span>. You can inspect all campaign schedules and active banners, but creating, reordering, or toggling campaigns requires System Admin privileges.
            </p>
          </div>
        </div>
      )}

      {/* Success Notification */}
      {bannerSaveSuccess && (
        <div className="flex items-center space-x-2 text-emerald-600 bg-emerald-50 border border-emerald-200 p-3.5 rounded-xl text-xs font-semibold animate-fadeIn">
          <span className="material-icons text-sm">check_circle</span>
          <span>
            {hasSupabaseConfig() 
              ? 'Homepage Banner saved successfully in live cloud database!'
              : 'Homepage Banner saved successfully in local cache storage.'}
          </span>
        </div>
      )}

      {bannerDeleteSuccess && (
        <div className="flex items-center space-x-2 text-rose-600 bg-rose-50 border border-rose-200 p-3.5 rounded-xl text-xs font-semibold animate-fadeIn">
          <span className="material-icons text-sm">delete_sweep</span>
          <span>Promotional campaign banner has been permanently deleted.</span>
        </div>
      )}

      {/* Directory list of all homepage banners */}
      <div className="bg-white border border-slate-200 shadow-sm rounded-2xl p-6 space-y-6 animate-fadeIn">
        
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 flex-wrap gap-2">
          <div className="flex items-center space-x-2.5">
            <span className="material-icons text-emerald-600">collections</span>
            <h3 className="font-semibold text-xs text-slate-800 uppercase tracking-wider">
              Banners Campaign Directory
            </h3>
            <span className="bg-emerald-500/10 text-emerald-600 text-[10px] font-mono font-bold px-2.5 py-0.5 rounded-full">
              {sortedBanners.length} Registered
            </span>
            <span className="bg-blue-500/10 text-blue-600 text-[10px] font-mono font-bold px-2.5 py-0.5 rounded-full flex items-center space-x-1">
              <span className="inline-block h-1.5 w-1.5 rounded-full bg-blue-500 animate-pulse"></span>
              <span>{activeBannersCount} Live in Carousel</span>
            </span>
          </div>
          <div className="text-slate-400 text-[10px] font-mono flex items-center space-x-1">
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-blue-500 animate-ping"></span>
            <span>{hasSupabaseConfig() ? 'Live Supabase Cloud Database' : 'Local Storage Cache'}</span>
          </div>
        </div>

        {sortedBanners.length === 0 ? (
          <div className="py-12 text-center flex flex-col items-center justify-center text-slate-400 space-y-2">
            <span className="material-icons text-4xl text-slate-300">cloud_off</span>
            <p className="text-xs font-semibold">No promotional banners stored yet</p>
            <p className="text-[10px] text-slate-500 font-sans">
              {isAdmin 
                ? 'Click on the "Add New Banner" button above to launch campaign graphics.'
                : 'No promotional banners have been published by system admins yet.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto border border-slate-200 rounded-2xl bg-white shadow-sm">
            <table className="w-full text-left border-collapse min-w-[650px]">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-400 text-[10px] font-mono uppercase tracking-widest font-bold">
                  <th className="py-3.5 px-4 text-center w-16">Reorder</th>
                  <th className="py-3.5 px-4">Img (Thumbnail)</th>
                  <th className="py-3.5 px-4">Title / Campaign</th>
                  <th className="py-3.5 px-4">Action URL</th>
                  <th className="py-3.5 px-4">Schedule / Expiry</th>
                  <th className="py-3.5 px-4">Carousel Status</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs">
                {sortedBanners.map((banner, index) => {
                  const isActive = banner.is_active;
                  const lifecycle = getBannerLifecycle(banner);
                  const isFirst = index === 0;
                  const isLast = index === sortedBanners.length - 1;

                  return (
                    <tr 
                      key={banner.id}
                      className={`group hover:bg-slate-50/60 transition-colors ${
                        isActive ? 'bg-emerald-500/[0.01]' : ''
                      }`}
                    >
                      {/* Reorder Buttons (Up / Down) */}
                      <td className="py-4 px-3 text-center">
                        <div className="inline-flex flex-col items-center space-y-1">
                          <button
                            type="button"
                            disabled={!isAdmin || isFirst}
                            onClick={() => handleMoveOrder(banner.id, 'up')}
                            className={`p-1 rounded-md border text-[11px] transition-all ${
                              !isAdmin || isFirst
                                ? 'opacity-30 border-slate-200 text-slate-300 cursor-not-allowed'
                                : 'border-slate-200 text-slate-600 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-300 cursor-pointer shadow-2xs'
                            }`}
                            title={!isAdmin ? 'Admin only' : isFirst ? 'Already at top' : 'Move up in carousel order'}
                          >
                            <span className="material-icons text-xs leading-none">arrow_upward</span>
                          </button>
                          <span className="font-mono text-[9px] font-bold text-slate-400">
                            #{banner.display_order ?? index}
                          </span>
                          <button
                            type="button"
                            disabled={!isAdmin || isLast}
                            onClick={() => handleMoveOrder(banner.id, 'down')}
                            className={`p-1 rounded-md border text-[11px] transition-all ${
                              !isAdmin || isLast
                                ? 'opacity-30 border-slate-200 text-slate-300 cursor-not-allowed'
                                : 'border-slate-200 text-slate-600 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-300 cursor-pointer shadow-2xs'
                            }`}
                            title={!isAdmin ? 'Admin only' : isLast ? 'Already at bottom' : 'Move down in carousel order'}
                          >
                            <span className="material-icons text-xs leading-none">arrow_downward</span>
                          </button>
                        </div>
                      </td>

                      {/* Thumbnail Img */}
                      <td className="py-4 px-4">
                        <div className="relative aspect-[16/9] w-20 rounded-xl overflow-hidden bg-slate-950 border border-slate-200 shadow-sm group-hover:scale-[1.03] transition-transform duration-200">
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img 
                            src={banner.image_url} 
                            alt={banner.title}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              e.currentTarget.src = 'https://images.unsplash.com/photo-1506015391300-4802dc74de2e?auto=format&fit=crop&w=1200&h=675&q=80';
                            }}
                            referrerPolicy="no-referrer"
                          />
                        </div>
                      </td>

                      {/* Title column */}
                      <td className="py-4 px-4">
                        <div className="space-y-1 max-w-[200px] truncate">
                          <div className="font-bold text-slate-800 text-xs tracking-tight truncate" title={banner.title}>
                            {banner.title}
                          </div>
                          <div className="flex items-center space-x-1.5 min-w-0">
                            <span className="font-mono text-[9px] text-slate-400 bg-slate-100 px-1 py-0.5 rounded tracking-wider shrink-0">
                              ID: {banner.id.slice(0, 6)}..
                            </span>
                            <span className="text-[9px] font-mono text-slate-400 truncate max-w-[100px]" title={banner.image_url}>
                              {banner.image_url}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Action URL Column */}
                      <td className="py-4 px-4 font-mono text-[11px] text-slate-600 max-w-[140px] truncate">
                        {banner.action_url ? (
                          <a 
                            href={banner.action_url} 
                            target="_blank" 
                            rel="noopener noreferrer"
                            className="text-blue-600 hover:underline flex items-center space-x-1"
                          >
                            <span className="material-icons text-xs">open_in_new</span>
                            <span className="truncate">{banner.action_url}</span>
                          </a>
                        ) : (
                          <span className="text-slate-400">-</span>
                        )}
                      </td>

                      {/* Schedule & Expiry Column */}
                      <td className="py-4 px-4">
                        <div className="space-y-0.5 max-w-[160px]">
                          <div className="flex items-center space-x-1 text-[11px] text-slate-700 font-medium">
                            <span className="material-icons text-xs text-slate-400">event</span>
                            <span className="truncate">
                              {banner.start_date || banner.end_date ? (
                                <>
                                  {banner.start_date ? new Date(banner.start_date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : 'Anytime'}
                                  {' → '}
                                  {banner.end_date ? new Date(banner.end_date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : 'No End'}
                                </>
                              ) : (
                                <span className="text-slate-400 italic">Always Active</span>
                              )}
                            </span>
                          </div>
                          <p className="text-[10px] text-slate-400 font-mono">
                            {lifecycle.subtext}
                          </p>
                        </div>
                      </td>

                      {/* Carousel Lifecycle Status & Active Toggle */}
                      <td className="py-4 px-4">
                        <div className="flex flex-col space-y-1.5 items-start">
                          {/* Lifecycle Status Badge */}
                          <span className={`inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-[9px] font-mono font-bold uppercase tracking-wider border ${lifecycle.badgeClass}`}>
                            <span className="material-icons text-xs">{lifecycle.icon}</span>
                            <span>{lifecycle.label}</span>
                          </span>

                          {/* Switch toggle (Multi-Active) */}
                          <button
                            type="button"
                            disabled={!isAdmin}
                            onClick={() => handleToggleActiveBanner(banner.id, isActive)}
                            className={`inline-flex items-center space-x-1 px-2 py-0.5 rounded-md text-[9px] font-mono font-bold uppercase border transition-all ${
                              !isAdmin 
                                ? 'opacity-60 cursor-not-allowed bg-slate-100 text-slate-400 border-slate-200' 
                                : isActive 
                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100 cursor-pointer' 
                                  : 'bg-slate-100 text-slate-500 border-slate-200 hover:bg-slate-200 cursor-pointer'
                            }`}
                            title={!isAdmin ? 'Admin permission required' : isActive ? 'Click to disable' : 'Click to enable'}
                          >
                            <span className="material-icons text-xs">
                              {isActive ? 'toggle_on' : 'toggle_off'}
                            </span>
                            <span>{isActive ? 'Active' : 'Disabled'}</span>
                          </button>
                        </div>
                      </td>

                      {/* Edit / Delete actions */}
                      <td className="py-4 px-4 text-right whitespace-nowrap">
                        <div className="inline-flex items-center space-x-1">
                          
                          <button
                            type="button"
                            disabled={!isAdmin}
                            onClick={() => handleStartEditBanner(banner)}
                            className={`text-[10px] font-mono font-bold uppercase px-2 py-1 rounded-lg border transition-colors inline-block align-middle ${
                              !isAdmin 
                                ? 'opacity-40 border-slate-200 text-slate-400 cursor-not-allowed'
                                : 'text-slate-600 hover:text-blue-700 hover:bg-blue-50 hover:border-blue-200 border-slate-200 cursor-pointer'
                            }`}
                            title={!isAdmin ? 'Admin only' : 'Edit Banner Campaign'}
                          >
                            <span className="material-icons text-xs leading-none">edit</span>
                          </button>

                          <button
                            type="button"
                            disabled={!isAdmin}
                            onClick={() => setDeleteTargetBanner(banner)}
                            className={`text-[10px] font-mono font-bold uppercase px-2 py-1 rounded-lg border transition-colors inline-block align-middle ${
                              !isAdmin 
                                ? 'opacity-40 border-slate-200 text-slate-400 cursor-not-allowed'
                                : 'text-slate-400 hover:text-red-700 hover:bg-red-50 hover:border-red-200 border-slate-200 cursor-pointer'
                            }`}
                            title={!isAdmin ? 'Admin only' : 'Delete Campaign'}
                          >
                            <span className="material-icons text-xs leading-none">delete</span>
                          </button>

                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

      </div>

      {/* Elegant Banner Configuration Wizard Modal */}
      {isWizardOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fadeIn" id="banner-wizard-modal">
          <div className="relative w-full max-w-lg bg-white border border-slate-200 shadow-2xl rounded-2xl overflow-hidden flex flex-col max-h-[92vh]">
            <form onSubmit={handleSaveBanner} className="flex flex-col flex-1 overflow-hidden">
              {/* Header */}
              <div className="flex items-center justify-between px-6 py-4 bg-slate-50 border-b border-slate-200/80 shrink-0">
                <div className="flex items-center space-x-2">
                  <span className="material-icons text-blue-600">tune</span>
                  <h3 className="font-semibold text-sm text-slate-800 uppercase tracking-wider">
                    {editingBannerId ? 'Edit Banner Campaign' : 'Add New Banner Campaign'}
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={handleCancelWizard}
                  className="text-slate-400 hover:text-slate-600 transition-colors p-1 rounded-lg hover:bg-slate-200/60 cursor-pointer"
                >
                  <span className="material-icons text-base">close</span>
                </button>
              </div>

              {/* Content */}
              <div className="px-6 py-5 space-y-4 overflow-y-auto flex-1">
                <p className="text-slate-500 text-xs leading-relaxed">
                  Configure campaign details, carousel weight, start and end dates for automated scheduling, and high-resolution visuals.
                </p>

                {dateValidationError && (
                  <div className="flex items-center space-x-2 bg-red-50 border border-red-200 text-red-700 text-xs p-2.5 rounded-xl font-medium">
                    <span className="material-icons text-sm text-red-500">error</span>
                    <span>{dateValidationError}</span>
                  </div>
                )}

                {/* Banner Title */}
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                    Banner Name / Campaign Title
                  </label>
                  <div className="relative">
                    <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">badge</span>
                    <input 
                      type="text" 
                      required
                      value={editedBannerTitle}
                      onChange={(e) => setEditedBannerTitle(e.target.value)}
                      placeholder="e.g., Eid Highway Patrol, Monsoon Road Advisory"
                      className="w-full bg-slate-50 border border-slate-200 hover:border-slate-300 focus:border-blue-500 text-slate-800 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
                    />
                  </div>
                </div>

                {/* Action URL */}
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                    Action URL / Target Link (Optional)
                  </label>
                  <div className="relative">
                    <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">explore</span>
                    <input 
                      type="url" 
                      value={editedBannerActionUrl}
                      onChange={(e) => setEditedBannerActionUrl(e.target.value)}
                      placeholder="e.g., https://safetravel.app/safety or app/route"
                      className="w-full bg-slate-50 border border-slate-200 hover:border-slate-300 focus:border-blue-500 text-slate-800 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
                    />
                  </div>
                </div>

                {/* Display Order / Sequence Weight */}
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                    Carousel Order / Sequence Weight (Lower shows earlier)
                  </label>
                  <div className="relative">
                    <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">sort</span>
                    <input 
                      type="number" 
                      value={editedBannerDisplayOrder}
                      onChange={(e) => setEditedBannerDisplayOrder(parseInt(e.target.value) || 0)}
                      placeholder="e.g., 0, 1, 2"
                      className="w-full bg-slate-50 border border-slate-200 hover:border-slate-300 focus:border-blue-500 text-slate-800 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
                    />
                  </div>
                </div>

                {/* Campaign Scheduling: Start Date & End Date */}
                <div className="bg-slate-50/80 border border-slate-200 rounded-xl p-3.5 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider flex items-center space-x-1">
                      <span className="material-icons text-blue-600 text-sm">calendar_month</span>
                      <span>Campaign Scheduling & Auto-Expiry</span>
                    </span>
                    <span className="text-[10px] text-slate-400 font-mono">Optional</span>
                  </div>

                  {/* Preset Duration Buttons */}
                  <div className="flex items-center space-x-1.5 flex-wrap gap-y-1">
                    <span className="text-[10px] text-slate-500 font-mono mr-1">Presets:</span>
                    <button
                      type="button"
                      onClick={() => applyDurationPreset(3)}
                      className="px-2 py-0.5 bg-white hover:bg-slate-100 border border-slate-200 text-slate-600 text-[10px] font-mono rounded-md transition-colors cursor-pointer"
                    >
                      3 Days
                    </button>
                    <button
                      type="button"
                      onClick={() => applyDurationPreset(7)}
                      className="px-2 py-0.5 bg-white hover:bg-slate-100 border border-slate-200 text-slate-600 text-[10px] font-mono rounded-md transition-colors cursor-pointer"
                    >
                      7 Days
                    </button>
                    <button
                      type="button"
                      onClick={() => applyDurationPreset(30)}
                      className="px-2 py-0.5 bg-white hover:bg-slate-100 border border-slate-200 text-slate-600 text-[10px] font-mono rounded-md transition-colors cursor-pointer"
                    >
                      30 Days
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setEditedBannerStartDate('');
                        setEditedBannerEndDate('');
                        setDateValidationError('');
                      }}
                      className="px-2 py-0.5 bg-white hover:bg-slate-100 border border-slate-200 text-slate-400 hover:text-slate-600 text-[10px] font-mono rounded-md transition-colors cursor-pointer"
                    >
                      Clear
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {/* Start Date */}
                    <div className="space-y-1">
                      <label className="block text-[10px] font-bold text-slate-600 uppercase tracking-wider">
                        Start Date & Time
                      </label>
                      <input 
                        type="datetime-local" 
                        value={editedBannerStartDate}
                        onChange={(e) => {
                          setEditedBannerStartDate(e.target.value);
                          setDateValidationError('');
                        }}
                        className="w-full bg-white border border-slate-200 hover:border-slate-300 focus:border-blue-500 text-slate-800 font-mono text-[11px] rounded-lg px-2.5 py-2 outline-none transition-all"
                      />
                    </div>

                    {/* End Date */}
                    <div className="space-y-1">
                      <label className="block text-[10px] font-bold text-slate-600 uppercase tracking-wider">
                        End Date & Time (Auto-Expiry)
                      </label>
                      <input 
                        type="datetime-local" 
                        value={editedBannerEndDate}
                        onChange={(e) => {
                          setEditedBannerEndDate(e.target.value);
                          setDateValidationError('');
                        }}
                        className="w-full bg-white border border-slate-200 hover:border-slate-300 focus:border-blue-500 text-slate-800 font-mono text-[11px] rounded-lg px-2.5 py-2 outline-none transition-all"
                      />
                    </div>
                  </div>
                </div>

                {/* Image URL with Clear toggle */}
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                    Banner Image URL (16:9 recommended)
                  </label>
                  <div className="flex space-x-2">
                    <div className="relative flex-1">
                      <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">link</span>
                      <input 
                        type="url" 
                        required
                        value={editedBannerImageUrl}
                        onChange={(e) => setEditedBannerImageUrl(e.target.value)}
                        placeholder="https://images.unsplash.com/... or any public image URL"
                        className="w-full bg-slate-50 border border-slate-200 hover:border-slate-300 focus:border-blue-500 text-slate-800 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
                      />
                    </div>
                    
                    {editedBannerImageUrl && (
                      <button
                        type="button"
                        onClick={() => setEditedBannerImageUrl('')}
                        className="bg-slate-100 border border-slate-200 hover:bg-slate-200 text-slate-600 px-3 py-2 rounded-xl text-xs flex items-center justify-center transition-all cursor-pointer"
                        title="Clear Input"
                      >
                        <span className="material-icons text-base">clear</span>
                      </button>
                    )}
                  </div>
                </div>

                {/* Image URL preview check */}
                <div className="space-y-1.5">
                  <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                    Image Preview Check
                  </label>
                  <div className="relative w-full aspect-[16/9] rounded-xl overflow-hidden bg-slate-900 border border-slate-200 flex items-center justify-center">
                    {editedBannerImageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img 
                        src={editedBannerImageUrl} 
                        alt="Wizard Preview" 
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.src = 'https://images.unsplash.com/photo-1506015391300-4802dc74de2e?auto=format&fit=crop&w=1200&h=675&q=80';
                        }}
                        referrerPolicy="no-referrer"
                      />
                    ) : (
                      <div className="flex flex-col items-center justify-center text-slate-500 text-center p-4">
                        <span className="material-icons text-2xl mb-1 text-slate-600">image_not_supported</span>
                        <span className="text-[10px] font-mono">No Image URL Configured</span>
                      </div>
                    )}
                  </div>
                </div>

              </div>

              {/* Footer Controls */}
              <div className="flex items-center justify-end space-x-2 px-6 py-4 bg-slate-50 border-t border-slate-100 shrink-0">
                <button
                  type="button"
                  onClick={handleCancelWizard}
                  className="bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 font-semibold text-xs px-4 py-2.5 rounded-xl transition-all cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSavingBanner || !editedBannerTitle.trim() || !editedBannerImageUrl.trim()}
                  className={`text-white font-semibold text-xs px-5 py-2.5 rounded-xl border flex items-center justify-center space-x-1.5 transition-all cursor-pointer shadow-sm ${
                    isSavingBanner 
                      ? 'bg-slate-400 border-slate-450 cursor-not-allowed opacity-80' 
                      : 'bg-blue-600 hover:bg-blue-700 border-blue-700 hover:border-blue-800'
                  }`}
                >
                  <span className={`material-icons text-sm ${isSavingBanner ? 'animate-spin' : ''}`}>
                    {isSavingBanner ? 'sync' : 'cloud_done'}
                  </span>
                  <span>{isSavingBanner ? 'Saving...' : 'Save Banner'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteTargetBanner && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fadeIn" id="delete-confirmation-modal">
          <div className="relative w-full max-w-md bg-white border border-slate-200 shadow-2xl rounded-2xl overflow-hidden flex flex-col">
            <div className="flex items-start p-6 space-x-4">
              <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center shrink-0">
                <span className="material-icons text-red-600 text-xl">gpp_maybe</span>
              </div>
              <div className="space-y-1.5 flex-1 min-w-0">
                <h3 className="font-semibold text-sm text-slate-800 uppercase tracking-wider">
                  Delete Banner Campaign
                </h3>
                <p className="text-slate-500 text-xs leading-relaxed">
                  Are you sure you want to permanently delete campaign <span className="font-bold text-slate-700">&quot;{deleteTargetBanner.title}&quot;</span>? This will remove this promotional banner from both the cloud database and active cache lists.
                </p>
                <p className="text-[10px] text-red-500 font-semibold uppercase tracking-wider">
                  This action is irreversible.
                </p>
              </div>
            </div>
            
            <div className="flex items-center justify-end space-x-2 px-6 py-4 bg-slate-50 border-t border-slate-100">
              <button
                type="button"
                disabled={isDeletingBanner}
                onClick={() => setDeleteTargetBanner(null)}
                className="bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 font-semibold text-xs px-4 py-2 rounded-xl transition-all cursor-pointer disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isDeletingBanner}
                onClick={() => {
                  handleDeleteBanner(deleteTargetBanner.id);
                }}
                className={`text-white font-semibold text-xs px-4 py-2 rounded-xl border shadow-sm transition-all cursor-pointer inline-flex items-center space-x-1.5 ${
                  isDeletingBanner 
                    ? 'bg-slate-400 border-slate-500 cursor-not-allowed opacity-80'
                    : 'bg-red-600 hover:bg-red-700 border-red-700 hover:border-red-800'
                }`}
              >
                <span className={`material-icons text-sm ${isDeletingBanner ? 'animate-spin' : ''}`}>
                  {isDeletingBanner ? 'sync' : 'delete_forever'}
                </span>
                <span>{isDeletingBanner ? 'Deleting...' : 'Delete'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
