'use client';

import React, { useState, useEffect } from 'react';
import { 
  DbState, 
  SubscriptionPlan, 
  PaymentTransaction, 
  PromoCode,
  getSupabaseClient, 
  hasSupabaseConfig 
} from '../../lib/supabase';
import ConfirmationWizard from '../ConfirmationWizard';
import { useToast } from '../ui/Toast';
import { formatGmtPlus6DateTime } from '../../lib/dateTimeUtils';

interface SubscriptionPlansSectionProps {
  db: DbState;
  setDb: React.Dispatch<React.SetStateAction<DbState>>;
  currentUser?: { email: string; role: 'admin' | 'dispatcher' | 'user'; name: string; id: string } | null;
}

function formatBillingPeriodLabel(period?: string | null, days?: number): string {
  const d = days || 30;
  switch (period) {
    case 'daily':
      return `${d} Day Pass`;
    case '3_days':
      return `${d} Days Pass`;
    case 'weekly':
      return `${d} Days Weekly Pass`;
    case '15_days':
      return `${d} Days Pass`;
    case 'monthly':
      return `Monthly (${d} Days)`;
    case 'quarterly':
      return `Quarterly (${d} Days)`;
    case 'half_yearly':
      return `Half-Yearly (${d} Days)`;
    case 'yearly':
      return `Yearly (${d} Days)`;
    case 'pay_per_trip':
      return `Pay-Per-Trip (${d} Days)`;
    case 'custom':
      return `Custom (${d} Days)`;
    default:
      return `${d} Days Pass`;
  }
}

export default function SubscriptionPlansSection({ db, setDb, currentUser }: SubscriptionPlansSectionProps) {
  const { toast } = useToast();
  const roleStr = String(currentUser?.role || '').toLowerCase();
  const isAdmin = !currentUser || roleStr === 'admin' || roleStr === 'super_admin' || roleStr.includes('admin');

  // Subtab navigation: 'plans' | 'verifications' | 'promo_codes' | 'payment_settings'
  const [activeSubTab, setActiveSubTab] = useState<'plans' | 'verifications' | 'promo_codes' | 'payment_settings'>('plans');

  // Local state for plans and transactions to allow instant reactivity
  const [plans, setPlans] = useState<SubscriptionPlan[]>(db.subscriptionPlans || []);
  const [transactions, setTransactions] = useState<PaymentTransaction[]>(db.paymentTransactions || []);
  const [promoCodes, setPromoCodes] = useState<PromoCode[]>(db.promoCodes || []);
  const [isLoadingData, setIsLoadingData] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // Manual payment numbers from remote configs
  const [bkashNumber, setBkashNumber] = useState(db.remoteConfigs?.['payment_manual_bkash'] || '01700000000');
  const [nagadNumber, setNagadNumber] = useState(db.remoteConfigs?.['payment_manual_nagad'] || '01800000000');
  const [rocketNumber, setRocketNumber] = useState(db.remoteConfigs?.['payment_manual_rocket'] || '01900000000');
  const [paymentInstructions, setPaymentInstructions] = useState(
    db.remoteConfigs?.['payment_instructions'] || 
    'Send Money / Payment to our verified numbers, copy the Transaction ID (TrxID), and submit for verification.'
  );
  const [isSavingSettings, setIsSavingSettings] = useState(false);

  // Filter for transactions
  const [txFilter, setTxFilter] = useState<'all' | 'pending' | 'approved' | 'rejected'>('all');
  const [txSearchQuery, setTxSearchQuery] = useState('');

  // Plan Modal state
  const [isPlanModalOpen, setIsPlanModalOpen] = useState(false);
  const [editingPlanId, setEditingPlanId] = useState<string | null>(null);
  const [planName, setPlanName] = useState('');
  const [planType, setPlanType] = useState<'time_pass' | 'trip_credits'>('time_pass');
  const [planCreditsCount, setPlanCreditsCount] = useState<number>(10);
  const [planPeriod, setPlanPeriod] = useState<string>('monthly');
  const [planDurationDays, setPlanDurationDays] = useState<number>(30);
  const [planPrice, setPlanPrice] = useState<number>(199);
  const [planDiscountPrice, setPlanDiscountPrice] = useState<number | ''>('');
  const [planCurrency, setPlanCurrency] = useState('BDT');
  const [planFeatures, setPlanFeatures] = useState<string[]>([]);
  const [newFeatureInput, setNewFeatureInput] = useState('');
  const [planIsActive, setPlanIsActive] = useState(true);
  const [planIsPopular, setPlanIsPopular] = useState(false);
  const [planDisplayOrder, setPlanDisplayOrder] = useState<number>(1);
  const [isSavingPlan, setIsSavingPlan] = useState(false);

  // Promo Code Modal & Filter state
  const [isPromoModalOpen, setIsPromoModalOpen] = useState(false);
  const [editingPromoId, setEditingPromoId] = useState<string | null>(null);
  const [promoCodeInput, setPromoCodeInput] = useState('');
  const [promoDiscountType, setPromoDiscountType] = useState<'percent' | 'fixed' | 'bonus_days' | 'bonus_credits'>('percent');
  const [promoDiscountValue, setPromoDiscountValue] = useState<number>(10);
  const [promoBonusDays, setPromoBonusDays] = useState<number>(0);
  const [promoBonusCredits, setPromoBonusCredits] = useState<number>(0);
  const [promoMinOrder, setPromoMinOrder] = useState<number>(0);
  const [promoMaxDiscount, setPromoMaxDiscount] = useState<number | ''>('');
  const [promoApplicablePlanTypes, setPromoApplicablePlanTypes] = useState<string[]>(['time_pass', 'trip_credits']);
  const [promoMaxUses, setPromoMaxUses] = useState<number | ''>('');
  const [promoValidUntil, setPromoValidUntil] = useState<string>('');
  const [promoIsActive, setPromoIsActive] = useState(true);
  const [isSavingPromo, setIsSavingPromo] = useState(false);
  const [deletePromoTarget, setDeletePromoTarget] = useState<PromoCode | null>(null);
  const [isDeletingPromo, setIsDeletingPromo] = useState(false);
  const [promoSearchQuery, setPromoSearchQuery] = useState('');

  // Plan Deletion Modal state
  const [deletePlanTarget, setDeletePlanTarget] = useState<SubscriptionPlan | null>(null);
  const [isDeletingPlan, setIsDeletingPlan] = useState(false);

  // Transaction Review Modals
  const [approveTargetTx, setApproveTargetTx] = useState<PaymentTransaction | null>(null);
  const [isApprovingTx, setIsApprovingTx] = useState(false);
  const [rejectTargetTx, setRejectTargetTx] = useState<PaymentTransaction | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [isRejectingTx, setIsRejectingTx] = useState(false);

  // Copied TrxID indicator
  const [copiedTrxId, setCopiedTrxId] = useState<string | null>(null);

  // Fetch / Refresh data from Supabase
  const refreshData = async () => {
    const supabase = getSupabaseClient();
    if (!supabase) return;
    setIsLoadingData(true);
    try {
      const [plansRes, txsRes, configsRes, promoRes] = await Promise.all([
        supabase.from('subscription_plans').select('*').order('display_order', { ascending: true }),
        supabase
          .from('payment_transactions')
          .select('*, profiles:profiles!payment_transactions_user_id_fkey(full_name, phone_number), subscription_plans(name, duration_days, plan_type, credits_count)')
          .order('created_at', { ascending: false }),
        supabase.from('app_remote_configs').select('*'),
        supabase.from('promo_codes').select('*').order('created_at', { ascending: false })
      ]);

      if (plansRes.data) {
        setPlans(plansRes.data);
        setDb(prev => ({ ...prev, subscriptionPlans: plansRes.data }));
      }
      if (txsRes.data) {
        setTransactions(txsRes.data);
        setDb(prev => ({ ...prev, paymentTransactions: txsRes.data }));
      }
      if (promoRes?.data) {
        setPromoCodes(promoRes.data);
        setDb(prev => ({ ...prev, promoCodes: promoRes.data }));
      }
      if (configsRes.data) {
        const map: Record<string, string> = {};
        configsRes.data.forEach((c: any) => { if (c?.key) map[c.key] = c.value || ''; });
        if (map['payment_manual_bkash']) setBkashNumber(map['payment_manual_bkash']);
        if (map['payment_manual_nagad']) setNagadNumber(map['payment_manual_nagad']);
        if (map['payment_manual_rocket']) setRocketNumber(map['payment_manual_rocket']);
        if (map['payment_instructions']) setPaymentInstructions(map['payment_instructions']);
      }
    } catch (err) {
      console.error('Error refreshing subscription data:', err);
    } finally {
      setIsLoadingData(false);
    }
  };

  useEffect(() => {
    refreshData();
  }, []);

  const showNotification = (text: string, type: 'success' | 'error' = 'success') => {
    setStatusMessage({ text, type });
    if (type === 'success') {
      toast.success(text, 'Plans & Billing');
    } else {
      toast.error(text, 'Plans & Billing');
    }
    setTimeout(() => setStatusMessage(null), 4000);
  };

  // Open Create Modal
  const handleOpenCreateModal = () => {
    setEditingPlanId(null);
    setPlanName('');
    setPlanType('time_pass');
    setPlanCreditsCount(10);
    setPlanPeriod('monthly');
    setPlanDurationDays(30);
    setPlanPrice(199);
    setPlanDiscountPrice('');
    setPlanCurrency('BDT');
    setPlanFeatures([
      'Unlimited Safe Trips',
      'Emergency Cloud Audio Recording',
      'Priority SOS Emergency Support',
      '2x Safe Trip Reward Points'
    ]);
    setPlanIsActive(true);
    setPlanIsPopular(false);
    setPlanDisplayOrder(plans.length + 1);
    setIsPlanModalOpen(true);
  };

  // Open Edit Modal
  const handleOpenEditModal = (plan: SubscriptionPlan) => {
    setEditingPlanId(plan.id);
    setPlanName(plan.name);
    setPlanType((plan.plan_type as 'time_pass' | 'trip_credits') || 'time_pass');
    setPlanCreditsCount(plan.credits_count || 10);
    setPlanPeriod(plan.billing_period || 'monthly');
    setPlanDurationDays(plan.duration_days || 30);
    setPlanPrice(plan.price);
    setPlanDiscountPrice(plan.discount_price != null ? plan.discount_price : '');
    setPlanCurrency(plan.currency || 'BDT');
    setPlanFeatures(Array.isArray(plan.features) ? plan.features : []);
    setPlanIsActive(plan.is_active);
    setPlanIsPopular(plan.is_popular);
    setPlanDisplayOrder(plan.display_order || 1);
    setIsPlanModalOpen(true);
  };

  // Add a feature bullet point
  const handleAddFeature = () => {
    if (!newFeatureInput.trim()) return;
    setPlanFeatures([...planFeatures, newFeatureInput.trim()]);
    setNewFeatureInput('');
  };

  // Remove a feature bullet point
  const handleRemoveFeature = (idx: number) => {
    setPlanFeatures(planFeatures.filter((_, i) => i !== idx));
  };

  // Save Plan (Create or Update)
  const handleSavePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!planName.trim()) {
      showNotification('Plan title cannot be empty.', 'error');
      return;
    }

    const supabase = getSupabaseClient();
    if (!supabase) return;

    setIsSavingPlan(true);
    try {
      const payload: any = {
        name: planName.trim(),
        plan_type: planType,
        credits_count: planType === 'trip_credits' ? Number(planCreditsCount) : 0,
        billing_period: planPeriod,
        duration_days: planDurationDays,
        price: planPrice,
        discount_price: planDiscountPrice !== '' ? Number(planDiscountPrice) : null,
        currency: planCurrency,
        features: planFeatures,
        is_active: planIsActive,
        is_popular: planIsPopular,
        display_order: planDisplayOrder
      };

      if (editingPlanId) {
        const { error } = await supabase
          .from('subscription_plans')
          .update(payload)
          .eq('id', editingPlanId);

        if (error) throw error;
        showNotification('Plan updated successfully.');
      } else {
        const { error } = await supabase
          .from('subscription_plans')
          .insert(payload);

        if (error) throw error;
        showNotification('New subscription plan created successfully.');
      }

      setIsPlanModalOpen(false);
      refreshData();
    } catch (err: any) {
      console.error('Error saving plan:', err);
      showNotification(err.message || 'Failed to save plan.', 'error');
    } finally {
      setIsSavingPlan(false);
    }
  };

  // Promo Code Handlers
  const handleOpenCreatePromoModal = () => {
    setEditingPromoId(null);
    setPromoCodeInput('');
    setPromoDiscountType('percent');
    setPromoDiscountValue(10);
    setPromoBonusDays(0);
    setPromoBonusCredits(0);
    setPromoMinOrder(0);
    setPromoMaxDiscount('');
    setPromoApplicablePlanTypes(['time_pass', 'trip_credits']);
    setPromoMaxUses('');
    setPromoValidUntil('');
    setPromoIsActive(true);
    setIsPromoModalOpen(true);
  };

  const handleOpenEditPromoModal = (promo: PromoCode) => {
    setEditingPromoId(promo.id);
    setPromoCodeInput(promo.code);
    setPromoDiscountType(promo.discount_type);
    setPromoDiscountValue(promo.discount_value);
    setPromoBonusDays(promo.bonus_days || 0);
    setPromoBonusCredits(promo.bonus_credits || 0);
    setPromoMinOrder(promo.min_order_amount || 0);
    setPromoMaxDiscount(promo.max_discount_amount != null ? promo.max_discount_amount : '');
    setPromoApplicablePlanTypes(promo.applicable_plan_types || ['time_pass', 'trip_credits']);
    setPromoMaxUses(promo.max_uses != null ? promo.max_uses : '');
    setPromoValidUntil(promo.valid_until ? promo.valid_until.substring(0, 16) : '');
    setPromoIsActive(promo.is_active);
    setIsPromoModalOpen(true);
  };

  const handleSavePromoCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!promoCodeInput.trim()) {
      showNotification('Promo code cannot be empty.', 'error');
      return;
    }

    const supabase = getSupabaseClient();
    if (!supabase) return;

    setIsSavingPromo(true);
    try {
      const payload: any = {
        code: promoCodeInput.trim().toUpperCase(),
        discount_type: promoDiscountType,
        discount_value: Number(promoDiscountValue) || 0,
        bonus_days: Number(promoBonusDays) || 0,
        bonus_credits: Number(promoBonusCredits) || 0,
        min_order_amount: Number(promoMinOrder) || 0,
        max_discount_amount: promoMaxDiscount !== '' ? Number(promoMaxDiscount) : null,
        applicable_plan_types: promoApplicablePlanTypes,
        max_uses: promoMaxUses !== '' ? Number(promoMaxUses) : null,
        valid_until: promoValidUntil ? new Date(promoValidUntil).toISOString() : null,
        is_active: promoIsActive
      };

      if (editingPromoId) {
        const { error } = await supabase
          .from('promo_codes')
          .update(payload)
          .eq('id', editingPromoId);

        if (error) throw error;
        showNotification(`Promo code "${payload.code}" updated successfully.`);
      } else {
        const { error } = await supabase
          .from('promo_codes')
          .insert(payload);

        if (error) throw error;
        showNotification(`Promo code "${payload.code}" created successfully.`);
      }

      setIsPromoModalOpen(false);
      refreshData();
    } catch (err: any) {
      console.error('Error saving promo code:', err);
      showNotification(err.message || 'Failed to save promo code.', 'error');
    } finally {
      setIsSavingPromo(false);
    }
  };

  const handleTogglePromoActive = async (promo: PromoCode) => {
    const supabase = getSupabaseClient();
    if (!supabase) return;

    const nextState = !promo.is_active;
    try {
      const { error } = await supabase
        .from('promo_codes')
        .update({ is_active: nextState })
        .eq('id', promo.id);

      if (error) throw error;
      setPromoCodes(prev => prev.map(p => p.id === promo.id ? { ...p, is_active: nextState } : p));
      showNotification(`Promo code ${promo.code} ${nextState ? 'activated' : 'deactivated'}.`);
    } catch (err: any) {
      console.error('Error toggling promo code status:', err);
      showNotification('Failed to toggle promo code.', 'error');
    }
  };

  const handleDeletePromoCode = async () => {
    if (!deletePromoTarget) return;
    const supabase = getSupabaseClient();
    if (!supabase) return;

    setIsDeletingPromo(true);
    try {
      const { error } = await supabase
        .from('promo_codes')
        .delete()
        .eq('id', deletePromoTarget.id);

      if (error) throw error;
      showNotification(`Promo code "${deletePromoTarget.code}" deleted.`);
      setDeletePromoTarget(null);
      refreshData();
    } catch (err: any) {
      console.error('Error deleting promo code:', err);
      showNotification(err.message || 'Failed to delete promo code.', 'error');
    } finally {
      setIsDeletingPromo(false);
    }
  };

  // Delete Plan
  const handleDeletePlan = async () => {
    if (!deletePlanTarget) return;
    const supabase = getSupabaseClient();
    if (!supabase) return;

    setIsDeletingPlan(true);
    try {
      const { error } = await supabase
        .from('subscription_plans')
        .delete()
        .eq('id', deletePlanTarget.id);

      if (error) throw error;
      showNotification(`Plan "${deletePlanTarget.name}" deleted successfully.`);
      setDeletePlanTarget(null);
      refreshData();
    } catch (err: any) {
      console.error('Error deleting plan:', err);
      showNotification(err.message || 'Failed to delete plan.', 'error');
    } finally {
      setIsDeletingPlan(false);
    }
  };

  // Toggle Plan Active State directly from card
  const handleTogglePlanActive = async (plan: SubscriptionPlan) => {
    const supabase = getSupabaseClient();
    if (!supabase) return;

    const nextState = !plan.is_active;
    try {
      const { error } = await supabase
        .from('subscription_plans')
        .update({ is_active: nextState })
        .eq('id', plan.id);

      if (error) throw error;
      setPlans(prev => prev.map(p => p.id === plan.id ? { ...p, is_active: nextState } : p));
      showNotification(`Plan ${nextState ? 'activated' : 'deactivated'}.`);
    } catch (err: any) {
      console.error('Error updating plan status:', err);
      showNotification('Failed to toggle plan status.', 'error');
    }
  };

  // Save Payment Settings
  const handleSavePaymentSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    const supabase = getSupabaseClient();
    if (!supabase) return;

    setIsSavingSettings(true);
    try {
      const configsToUpsert = [
        { key: 'payment_manual_bkash', value: bkashNumber.trim(), description: 'Admin bKash number' },
        { key: 'payment_manual_nagad', value: nagadNumber.trim(), description: 'Admin Nagad number' },
        { key: 'payment_manual_rocket', value: rocketNumber.trim(), description: 'Admin Rocket number' },
        { key: 'payment_instructions', value: paymentInstructions.trim(), description: 'Payment instructions' }
      ];

      for (const item of configsToUpsert) {
        await supabase
          .from('app_remote_configs')
          .upsert(item, { onConflict: 'key' });
      }

      showNotification('Payment settings and mobile numbers saved successfully.');
    } catch (err: any) {
      console.error('Error saving payment settings:', err);
      showNotification('Failed to update payment settings.', 'error');
    } finally {
      setIsSavingSettings(false);
    }
  };

  // Approve Transaction
  const handleApproveTransaction = async () => {
    if (!approveTargetTx) return;
    const supabase = getSupabaseClient();
    if (!supabase) return;

    setIsApprovingTx(true);
    try {
      const durationDays = approveTargetTx.subscription_plans?.duration_days || 30;
      const isTripCreditPack = approveTargetTx.plan_type === 'trip_credits' || approveTargetTx.subscription_plans?.plan_type === 'trip_credits';

      // 1. Fetch user's current profile to calculate new balances & expiry
      const { data: userProfile } = await supabase
        .from('profiles')
        .select('id, premium_until, is_premium, trip_credits, credits_valid_until')
        .eq('id', approveTargetTx.user_id)
        .single();

      if (isTripCreditPack) {
        // Calculate credits and expiration
        const baseCredits = approveTargetTx.credits_count || approveTargetTx.subscription_plans?.credits_count || 10;
        const bonusCredits = approveTargetTx.bonus_credits || 0;
        const totalCreditsToAdd = baseCredits + bonusCredits;
        const currentCredits = Number(userProfile?.trip_credits || 0);

        let creditBaseDate = new Date();
        if (userProfile?.credits_valid_until) {
          const curValid = new Date(userProfile.credits_valid_until);
          if (curValid > creditBaseDate) {
            creditBaseDate = curValid;
          }
        }
        const newCreditsExpiry = new Date(creditBaseDate.getTime() + durationDays * 24 * 60 * 60 * 1000).toISOString();

        const { error: profileErr } = await supabase
          .from('profiles')
          .update({
            trip_credits: currentCredits + totalCreditsToAdd,
            credits_valid_until: newCreditsExpiry
          })
          .eq('id', approveTargetTx.user_id);

        if (profileErr) throw profileErr;
      } else {
        // Time Pass: Calculate new expiry including any bonus days
        const bonusDays = approveTargetTx.bonus_days || 0;
        const totalDays = durationDays + bonusDays;

        let baseDate = new Date();
        if (userProfile?.premium_until) {
          const currentExp = new Date(userProfile.premium_until);
          if (currentExp > baseDate) {
            baseDate = currentExp;
          }
        }

        const newExpiry = new Date(baseDate.getTime() + totalDays * 24 * 60 * 60 * 1000).toISOString();

        const { error: profileErr } = await supabase
          .from('profiles')
          .update({
            is_premium: true,
            premium_until: newExpiry
          })
          .eq('id', approveTargetTx.user_id);

        if (profileErr) throw profileErr;
      }

      // 2. Increment promo code usage if promo was applied
      if (approveTargetTx.promo_code) {
        try {
          const cleanCode = approveTargetTx.promo_code.trim().toUpperCase();
          const { data: promoData } = await supabase
            .from('promo_codes')
            .select('id, used_count')
            .eq('code', cleanCode)
            .single();

          if (promoData) {
            await supabase
              .from('promo_codes')
              .update({ used_count: (promoData.used_count || 0) + 1 })
              .eq('id', promoData.id);
          }
        } catch (pErr) {
          console.warn('Could not increment promo code used_count:', pErr);
        }
      }

      // 3. Update payment_transactions: status = 'approved', reviewed_at, reviewed_by
      const { error: txErr } = await supabase
        .from('payment_transactions')
        .update({
          status: 'approved',
          reviewed_at: new Date().toISOString(),
          reviewed_by: currentUser?.id || null
        })
        .eq('id', approveTargetTx.id);

      if (txErr) throw txErr;

      const successMsg = isTripCreditPack
        ? `Trip Credit pack approved! Added ${approveTargetTx.credits_count || 10} trips to user account.`
        : `Subscription approved! User profile upgraded to Premium.`;

      showNotification(successMsg);
      setApproveTargetTx(null);
      refreshData();
    } catch (err: any) {
      console.error('Error approving transaction:', err);
      showNotification(err.message || 'Failed to approve transaction.', 'error');
    } finally {
      setIsApprovingTx(false);
    }
  };

  // Reject Transaction
  const handleRejectTransaction = async () => {
    if (!rejectTargetTx) return;
    const supabase = getSupabaseClient();
    if (!supabase) return;

    setIsRejectingTx(true);
    try {
      const { error } = await supabase
        .from('payment_transactions')
        .update({
          status: 'rejected',
          admin_notes: rejectReason.trim() || 'Transaction ID verification failed.',
          reviewed_at: new Date().toISOString(),
          reviewed_by: currentUser?.id || null
        })
        .eq('id', rejectTargetTx.id);

      if (error) throw error;

      showNotification('Transaction marked as rejected.');
      setRejectTargetTx(null);
      setRejectReason('');
      refreshData();
    } catch (err: any) {
      console.error('Error rejecting transaction:', err);
      showNotification(err.message || 'Failed to reject transaction.', 'error');
    } finally {
      setIsRejectingTx(false);
    }
  };

  // Copy TrxID to clipboard
  const handleCopyTrxId = (trxId: string) => {
    navigator.clipboard.writeText(trxId);
    setCopiedTrxId(trxId);
    toast.info(`TrxID ${trxId} copied to clipboard.`, 'Copied');
    setTimeout(() => setCopiedTrxId(null), 2000);
  };

  // Filtered transactions
  const pendingCount = transactions.filter(t => t.status === 'pending').length;
  const filteredTransactions = transactions.filter(tx => {
    const matchesFilter = txFilter === 'all' || tx.status === txFilter;
    const query = txSearchQuery.toLowerCase().trim();
    if (!query) return matchesFilter;
    const matchesSearch = 
      tx.transaction_id?.toLowerCase().includes(query) ||
      tx.sender_number?.toLowerCase().includes(query) ||
      tx.profiles?.full_name?.toLowerCase().includes(query) ||
      tx.profiles?.phone_number?.toLowerCase().includes(query) ||
      tx.subscription_plans?.name?.toLowerCase().includes(query);
    return matchesFilter && matchesSearch;
  });

  return (
    <div className="space-y-6">
      
      {/* Top Banner / Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-500/5 rounded-full blur-3xl pointer-events-none"></div>
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center space-x-3 mb-2">
              <span className="material-icons text-blue-400 text-2xl">card_membership</span>
              <h2 className="text-xl font-bold text-white tracking-tight">
                Subscription & Premium Plans Console
              </h2>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                MONETIZATION ENGINE
              </span>
            </div>
            <p className="text-slate-400 text-xs">
              Configure premium safety packages, review manual mobile payments (bKash, Nagad, Rocket), and manage subscriber access.
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              type="button"
              onClick={refreshData}
              disabled={isLoadingData}
              className="flex items-center space-x-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-semibold transition-all border border-slate-700 cursor-pointer disabled:opacity-50"
            >
              <span className={`material-icons text-sm ${isLoadingData ? 'animate-spin' : ''}`}>refresh</span>
              <span>Sync Cloud</span>
            </button>
            {isAdmin && activeSubTab === 'plans' && (
              <button
                type="button"
                onClick={handleOpenCreateModal}
                className="flex items-center space-x-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-blue-600/30 cursor-pointer"
              >
                <span className="material-icons text-sm">add_circle</span>
                <span>Create New Plan</span>
              </button>
            )}
            {isAdmin && activeSubTab === 'promo_codes' && (
              <button
                type="button"
                onClick={handleOpenCreatePromoModal}
                className="flex items-center space-x-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-indigo-600/30 cursor-pointer"
              >
                <span className="material-icons text-sm">add_circle</span>
                <span>Create Promo Code</span>
              </button>
            )}
          </div>
        </div>

        {/* Status Notification Toast */}
        {statusMessage && (
          <div className={`mt-4 p-3 rounded-xl border text-xs font-medium flex items-center space-x-2 ${
            statusMessage.type === 'success' 
              ? 'bg-emerald-950/60 border-emerald-800 text-emerald-300' 
              : 'bg-red-950/60 border-red-800 text-red-300'
          }`}>
            <span className="material-icons text-sm">
              {statusMessage.type === 'success' ? 'check_circle' : 'error'}
            </span>
            <span>{statusMessage.text}</span>
          </div>
        )}

        {/* Sub-tabs Navigation */}
        <div className="flex items-center space-x-2 mt-6 border-b border-slate-800/80 pb-0 overflow-x-auto">
          <button
            type="button"
            onClick={() => setActiveSubTab('plans')}
            className={`flex items-center space-x-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeSubTab === 'plans'
                ? 'border-blue-500 text-blue-400 bg-blue-500/5'
                : 'border-transparent text-slate-400 hover:text-white hover:border-slate-700'
            }`}
          >
            <span className="material-icons text-sm">workspace_premium</span>
            <span>Subscription Packages</span>
            <span className="text-[10px] font-mono bg-slate-800 text-slate-300 px-1.5 py-0.5 rounded">
              {plans.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveSubTab('promo_codes')}
            className={`flex items-center space-x-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeSubTab === 'promo_codes'
                ? 'border-indigo-500 text-indigo-400 bg-indigo-500/5'
                : 'border-transparent text-slate-400 hover:text-white hover:border-slate-700'
            }`}
          >
            <span className="material-icons text-sm">local_offer</span>
            <span>Promo Codes & Discounts</span>
            <span className="text-[10px] font-mono bg-slate-800 text-slate-300 px-1.5 py-0.5 rounded">
              {promoCodes.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveSubTab('verifications')}
            className={`flex items-center space-x-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeSubTab === 'verifications'
                ? 'border-blue-500 text-blue-400 bg-blue-500/5'
                : 'border-transparent text-slate-400 hover:text-white hover:border-slate-700'
            }`}
          >
            <span className="material-icons text-sm">verified</span>
            <span>Manual Payment Verifications</span>
            {pendingCount > 0 ? (
              <span className="text-[10px] font-mono bg-amber-500 text-slate-950 font-black px-1.5 py-0.5 rounded-full animate-pulse">
                {pendingCount} PENDING
              </span>
            ) : (
              <span className="text-[10px] font-mono bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded">
                {transactions.length}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setActiveSubTab('payment_settings')}
            className={`flex items-center space-x-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeSubTab === 'payment_settings'
                ? 'border-blue-500 text-blue-400 bg-blue-500/5'
                : 'border-transparent text-slate-400 hover:text-white hover:border-slate-700'
            }`}
          >
            <span className="material-icons text-sm">tune</span>
            <span>Payment Numbers & Rules</span>
          </button>
        </div>
      </div>

      {/* SUBTAB 1: SUBSCRIPTION PACKAGES */}
      {activeSubTab === 'plans' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-200 flex items-center space-x-2">
              <span className="material-icons text-slate-400 text-sm">view_carousel</span>
              <span>Available Safety Subscription Packages</span>
            </h3>
            <span className="text-xs text-slate-400 font-mono">
              Active: {plans.filter(p => p.is_active).length} / Total: {plans.length}
            </span>
          </div>

          {plans.length === 0 ? (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center">
              <span className="material-icons text-slate-600 text-5xl mb-3">card_membership</span>
              <h4 className="text-slate-200 font-semibold text-sm">No Subscription Plans Found</h4>
              <p className="text-slate-400 text-xs mt-1 max-w-sm mx-auto">
                Create packages to allow users to subscribe to premium safety protection.
              </p>
              {isAdmin && (
                <button
                  type="button"
                  onClick={handleOpenCreateModal}
                  className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition-all shadow-md cursor-pointer"
                >
                  Create First Plan
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {plans.map(plan => {
                const hasDiscount = plan.discount_price != null && plan.discount_price < plan.price;
                return (
                  <div
                    key={plan.id}
                    className={`bg-slate-900 border rounded-2xl p-5 flex flex-col justify-between transition-all relative overflow-hidden ${
                      plan.is_popular 
                        ? 'border-blue-500/50 shadow-lg shadow-blue-500/10' 
                        : 'border-slate-800 hover:border-slate-700'
                    } ${!plan.is_active ? 'opacity-60 bg-slate-950/50' : ''}`}
                  >
                    {/* Header Ribbon / Badges */}
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center space-x-1.5 flex-wrap gap-y-1">
                        {plan.is_popular && (
                          <span className="px-2 py-0.5 rounded text-[9.5px] font-mono font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center space-x-1">
                            <span className="material-icons text-[11px]">star</span>
                            <span>POPULAR</span>
                          </span>
                        )}
                        <span className={`px-2 py-0.5 rounded text-[9.5px] font-mono uppercase border ${
                          plan.plan_type === 'trip_credits'
                            ? 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30 font-bold'
                            : 'bg-slate-800 text-slate-300 border-slate-700'
                        }`}>
                          {plan.plan_type === 'trip_credits' 
                            ? `${plan.credits_count || 10} TRIPS PACK`
                            : formatBillingPeriodLabel(plan.billing_period, plan.duration_days)}
                        </span>
                      </div>

                      <div className="flex items-center space-x-1.5">
                        <span className={`w-2 h-2 rounded-full ${plan.is_active ? 'bg-emerald-400 animate-pulse' : 'bg-slate-600'}`}></span>
                        <span className="text-[10px] font-mono text-slate-400">
                          {plan.is_active ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                      </div>
                    </div>

                    {/* Plan Title & Price */}
                    <div>
                      <h4 className="text-base font-bold text-white tracking-tight">{plan.name}</h4>
                      
                      <div className="flex items-baseline space-x-2 mt-2">
                        {hasDiscount ? (
                          <>
                            <span className="text-2xl font-black text-emerald-400">
                              ৳{plan.discount_price}
                            </span>
                            <span className="text-sm line-through text-slate-500">
                              ৳{plan.price}
                            </span>
                          </>
                        ) : (
                          <span className="text-2xl font-black text-white">
                            ৳{plan.price}
                          </span>
                        )}
                        <span className="text-xs text-slate-400 font-mono">
                          {plan.plan_type === 'trip_credits'
                            ? `/ ${plan.duration_days} days validity`
                            : `/ ${plan.duration_days} days`}
                        </span>
                      </div>

                      {/* Features Bullet Points */}
                      <div className="mt-4 pt-4 border-t border-slate-800 space-y-2">
                        <p className="text-[10.5px] font-mono uppercase tracking-wider text-slate-500 font-bold">
                          Included Perks:
                        </p>
                        <ul className="space-y-1.5">
                          {plan.features?.map((feat, i) => (
                            <li key={i} className="flex items-start space-x-2 text-xs text-slate-300">
                              <span className="material-icons text-emerald-400 text-sm shrink-0 mt-0.5">check_circle</span>
                              <span className="leading-tight">{feat}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>

                    {/* Action Controls */}
                    {isAdmin && (
                      <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between gap-2">
                        <button
                          type="button"
                          onClick={() => handleTogglePlanActive(plan)}
                          className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-semibold transition-all border cursor-pointer ${
                            plan.is_active 
                              ? 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700' 
                              : 'bg-emerald-600/10 hover:bg-emerald-600/20 text-emerald-400 border-emerald-500/30'
                          }`}
                        >
                          {plan.is_active ? 'Deactivate' : 'Activate'}
                        </button>

                        <button
                          type="button"
                          onClick={() => handleOpenEditModal(plan)}
                          className="py-1.5 px-3 rounded-lg text-xs font-semibold bg-blue-600/10 hover:bg-blue-600/20 text-blue-400 border border-blue-500/20 transition-all cursor-pointer flex items-center space-x-1"
                        >
                          <span className="material-icons text-xs">edit</span>
                          <span>Edit</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => setDeletePlanTarget(plan)}
                          className="py-1.5 px-2.5 rounded-lg text-xs font-semibold bg-red-600/10 hover:bg-red-600/20 text-red-400 border border-red-500/20 transition-all cursor-pointer"
                          title="Delete Plan"
                        >
                          <span className="material-icons text-xs">delete</span>
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* SUBTAB 2: PAYMENT VERIFICATIONS */}
      {activeSubTab === 'verifications' && (
        <div className="space-y-4">
          {/* Filter & Search Bar */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex flex-col md:flex-row items-center justify-between gap-4">
            {/* Status pills */}
            <div className="flex items-center space-x-2 overflow-x-auto w-full md:w-auto">
              {(['all', 'pending', 'approved', 'rejected'] as const).map(tab => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setTxFilter(tab)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer uppercase font-mono ${
                    txFilter === tab
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  {tab === 'all' && `All (${transactions.length})`}
                  {tab === 'pending' && `Pending (${transactions.filter(t => t.status === 'pending').length})`}
                  {tab === 'approved' && `Approved (${transactions.filter(t => t.status === 'approved').length})`}
                  {tab === 'rejected' && `Rejected (${transactions.filter(t => t.status === 'rejected').length})`}
                </button>
              ))}
            </div>

            {/* Search Input */}
            <div className="relative w-full md:w-72">
              <span className="material-icons absolute left-3 top-2.5 text-slate-500 text-sm">search</span>
              <input
                type="text"
                placeholder="Search TrxID, Phone, Name..."
                value={txSearchQuery}
                onChange={e => setTxSearchQuery(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          {/* Table / List */}
          {filteredTransactions.length === 0 ? (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center">
              <span className="material-icons text-slate-600 text-5xl mb-3">receipt_long</span>
              <h4 className="text-slate-200 font-semibold text-sm">No Payment Records Found</h4>
              <p className="text-slate-400 text-xs mt-1">
                There are no transaction submissions matching the selected filter.
              </p>
            </div>
          ) : (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950/80 text-[10.5px] uppercase font-mono text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4 font-semibold">User / Traveler</th>
                      <th className="py-3.5 px-4 font-semibold">Package Requested</th>
                      <th className="py-3.5 px-4 font-semibold">Amount</th>
                      <th className="py-3.5 px-4 font-semibold">Method & Sender</th>
                      <th className="py-3.5 px-4 font-semibold">Transaction ID (TrxID)</th>
                      <th className="py-3.5 px-4 font-semibold">Submitted At</th>
                      <th className="py-3.5 px-4 font-semibold">Status</th>
                      {isAdmin && <th className="py-3.5 px-4 font-semibold text-right">Actions</th>}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-sans">
                    {filteredTransactions.map(tx => {
                      const methodColors: Record<string, string> = {
                        bkash: 'bg-pink-500/10 text-pink-400 border-pink-500/20',
                        nagad: 'bg-orange-500/10 text-orange-400 border-orange-500/20',
                        rocket: 'bg-purple-500/10 text-purple-400 border-purple-500/20'
                      };
                      const methodClass = methodColors[tx.payment_method?.toLowerCase()] || 'bg-slate-800 text-slate-300 border-slate-700';

                      return (
                        <tr key={tx.id} className="hover:bg-slate-800/30 transition-colors">
                          {/* User */}
                          <td className="py-3.5 px-4">
                            <div className="font-semibold text-white">
                              {tx.profiles?.full_name || 'Traveler User'}
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono">
                              {tx.profiles?.phone_number || tx.sender_number}
                            </div>
                          </td>

                          {/* Package */}
                          <td className="py-3.5 px-4">
                            <div className="flex items-center space-x-1.5 flex-wrap gap-y-1">
                              <span className="font-medium text-slate-200">
                                {tx.subscription_plans?.name || 'Safety Plan'}
                              </span>
                              <span className={`px-1.5 py-0.5 rounded text-[9px] font-mono uppercase font-bold border ${
                                tx.plan_type === 'trip_credits' || tx.subscription_plans?.plan_type === 'trip_credits'
                                  ? 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30'
                                  : 'bg-blue-500/10 text-blue-400 border-blue-500/30'
                              }`}>
                                {tx.plan_type === 'trip_credits' || tx.subscription_plans?.plan_type === 'trip_credits'
                                  ? `${tx.credits_count || tx.subscription_plans?.credits_count || 10} TRIPS`
                                  : 'TIME PASS'}
                              </span>
                            </div>
                            <div className="text-[10px] text-slate-500 font-mono mt-0.5">
                              {tx.subscription_plans?.duration_days ? `${tx.subscription_plans.duration_days} Days Validity` : '30 Days'}
                            </div>
                          </td>

                          {/* Amount */}
                          <td className="py-3.5 px-4">
                            <div className="flex items-baseline space-x-1.5">
                              <span className="font-black text-emerald-400 font-mono text-sm">
                                ৳{tx.amount}
                              </span>
                              {tx.discount_amount != null && Number(tx.discount_amount) > 0 && (
                                <span className="text-[10px] text-pink-400 font-mono">
                                  (-৳{tx.discount_amount})
                                </span>
                              )}
                            </div>
                            {tx.promo_code && (
                              <div className="flex items-center space-x-1 mt-1 flex-wrap gap-1">
                                <span className="px-1.5 py-0.5 rounded text-[9px] font-mono font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                                  PROMO: {tx.promo_code}
                                </span>
                                {tx.bonus_days != null && tx.bonus_days > 0 && (
                                  <span className="text-[9px] font-mono text-amber-300">+{tx.bonus_days}d</span>
                                )}
                                {tx.bonus_credits != null && tx.bonus_credits > 0 && (
                                  <span className="text-[9px] font-mono text-cyan-300">+{tx.bonus_credits}tr</span>
                                )}
                              </div>
                            )}
                          </td>

                          {/* Method & Sender */}
                          <td className="py-3.5 px-4">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-mono uppercase font-bold border ${methodClass}`}>
                              {tx.payment_method}
                            </span>
                            <div className="text-[11px] text-slate-300 font-mono mt-1">
                              {tx.sender_number}
                            </div>
                          </td>

                          {/* TrxID */}
                          <td className="py-3.5 px-4 font-mono">
                            <div className="flex items-center space-x-1.5">
                              <span className="bg-slate-950 px-2 py-1 rounded border border-slate-800 text-blue-300 font-bold tracking-wider select-all">
                                {tx.transaction_id}
                              </span>
                              <button
                                type="button"
                                onClick={() => handleCopyTrxId(tx.transaction_id)}
                                className="text-slate-500 hover:text-white transition-colors cursor-pointer"
                                title="Copy Transaction ID"
                              >
                                <span className="material-icons text-xs">
                                  {copiedTrxId === tx.transaction_id ? 'check' : 'content_copy'}
                                </span>
                              </button>
                            </div>
                          </td>

                          {/* Submitted */}
                          <td className="py-3.5 px-4 text-slate-400 text-[11px] font-mono">
                            {formatGmtPlus6DateTime(tx.created_at)}
                          </td>

                          {/* Status */}
                          <td className="py-3.5 px-4">
                            {tx.status === 'pending' && (
                              <span className="px-2 py-1 rounded-full text-[10px] font-mono font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30 flex items-center space-x-1 w-max">
                                <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-ping"></span>
                                <span>PENDING</span>
                              </span>
                            )}
                            {tx.status === 'approved' && (
                              <span className="px-2 py-1 rounded-full text-[10px] font-mono font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 flex items-center space-x-1 w-max">
                                <span className="material-icons text-xs">done_all</span>
                                <span>APPROVED</span>
                              </span>
                            )}
                            {tx.status === 'rejected' && (
                              <div>
                                <span className="px-2 py-1 rounded-full text-[10px] font-mono font-bold bg-red-500/10 text-red-400 border border-red-500/30 flex items-center space-x-1 w-max">
                                  <span className="material-icons text-xs">close</span>
                                  <span>REJECTED</span>
                                </span>
                                {tx.admin_notes && (
                                  <div className="text-[10px] text-red-400/80 mt-1 max-w-xs truncate" title={tx.admin_notes}>
                                    {tx.admin_notes}
                                  </div>
                                )}
                              </div>
                            )}
                          </td>

                          {/* Action Buttons */}
                          {isAdmin && (
                            <td className="py-3.5 px-4 text-right">
                              {tx.status === 'pending' ? (
                                <div className="flex items-center justify-end space-x-2">
                                  <button
                                    type="button"
                                    onClick={() => setApproveTargetTx(tx)}
                                    className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition-all shadow-sm flex items-center space-x-1 cursor-pointer"
                                    title="Approve Payment & Grant Premium"
                                  >
                                    <span className="material-icons text-xs">check</span>
                                    <span>Approve</span>
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => {
                                      setRejectTargetTx(tx);
                                      setRejectReason('');
                                    }}
                                    className="px-2.5 py-1.5 bg-red-600/10 hover:bg-red-600/20 text-red-400 border border-red-500/20 rounded-lg text-xs font-bold transition-all flex items-center space-x-1 cursor-pointer"
                                    title="Reject Payment"
                                  >
                                    <span className="material-icons text-xs">close</span>
                                    <span>Reject</span>
                                  </button>
                                </div>
                              ) : (
                                <span className="text-[10px] text-slate-500 font-mono">
                                  {tx.reviewed_at ? `Verified ${formatGmtPlus6DateTime(tx.reviewed_at)}` : 'Archived'}
                                </span>
                              )}
                            </td>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* SUBTAB: PROMO CODES & DISCOUNTS */}
      {activeSubTab === 'promo_codes' && (
        <div className="space-y-4">
          {/* Header & Search Bar */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center space-x-3 w-full md:w-auto">
              <span className="material-icons text-indigo-400 text-lg">local_offer</span>
              <div>
                <h3 className="text-sm font-bold text-white tracking-tight">Active Promo Campaigns</h3>
                <p className="text-[11px] text-slate-400">
                  Manage discount codes, extra safety days, and bonus trip credit offers.
                </p>
              </div>
            </div>

            <div className="flex items-center space-x-3 w-full md:w-auto">
              {/* Search box */}
              <div className="relative w-full md:w-64">
                <span className="material-icons text-slate-500 text-xs absolute left-3 top-2.5">search</span>
                <input
                  type="text"
                  placeholder="Filter by promo code..."
                  value={promoSearchQuery}
                  onChange={e => setPromoSearchQuery(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-8 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 font-mono"
                />
              </div>

              {isAdmin && (
                <button
                  type="button"
                  onClick={handleOpenCreatePromoModal}
                  className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold transition-all shadow-md flex items-center space-x-1.5 shrink-0 cursor-pointer"
                >
                  <span className="material-icons text-xs">add</span>
                  <span>New Code</span>
                </button>
              )}
            </div>
          </div>

          {/* Promo Codes Table */}
          {promoCodes.length === 0 ? (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center">
              <span className="material-icons text-slate-600 text-5xl mb-3">local_offer</span>
              <h4 className="text-slate-200 font-semibold text-sm">No Promo Codes Created</h4>
              <p className="text-slate-400 text-xs mt-1 max-w-sm mx-auto">
                Create promotional discount codes and bonus days to boost user subscriptions.
              </p>
              {isAdmin && (
                <button
                  type="button"
                  onClick={handleOpenCreatePromoModal}
                  className="mt-4 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold transition-all shadow-md cursor-pointer"
                >
                  Create First Promo Code
                </button>
              )}
            </div>
          ) : (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950/80 text-[10.5px] uppercase font-mono text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4 font-semibold">Promo Code</th>
                      <th className="py-3.5 px-4 font-semibold">Benefit & Type</th>
                      <th className="py-3.5 px-4 font-semibold">Min Order & Cap</th>
                      <th className="py-3.5 px-4 font-semibold">Applicable Plans</th>
                      <th className="py-3.5 px-4 font-semibold">Redemptions</th>
                      <th className="py-3.5 px-4 font-semibold">Validity</th>
                      <th className="py-3.5 px-4 font-semibold">Status</th>
                      {isAdmin && <th className="py-3.5 px-4 font-semibold text-right">Actions</th>}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-sans">
                    {promoCodes
                      .filter(p => {
                        const q = promoSearchQuery.toLowerCase().trim();
                        if (!q) return true;
                        return p.code.toLowerCase().includes(q) || p.discount_type.toLowerCase().includes(q);
                      })
                      .map(promo => {
                        const isExpired = promo.valid_until && new Date(promo.valid_until) < new Date();
                        const isMaxReached = promo.max_uses != null && (promo.used_count || 0) >= promo.max_uses;

                        return (
                          <tr key={promo.id} className="hover:bg-slate-800/30 transition-colors">
                            {/* Code */}
                            <td className="py-3.5 px-4">
                              <div className="flex items-center space-x-2">
                                <span className="font-mono font-black text-indigo-300 bg-slate-950 px-2.5 py-1 rounded border border-indigo-500/30 tracking-wider">
                                  {promo.code}
                                </span>
                                <button
                                  type="button"
                                  onClick={() => {
                                    navigator.clipboard.writeText(promo.code);
                                    toast.info(`Promo code ${promo.code} copied.`, 'Copied');
                                  }}
                                  className="text-slate-500 hover:text-white transition-colors cursor-pointer"
                                  title="Copy Code"
                                >
                                  <span className="material-icons text-xs">content_copy</span>
                                </button>
                              </div>
                            </td>

                            {/* Benefit & Type */}
                            <td className="py-3.5 px-4">
                              {promo.discount_type === 'percent' && (
                                <div>
                                  <span className="font-black text-emerald-400 font-mono text-sm">
                                    {promo.discount_value}% OFF
                                  </span>
                                  <div className="text-[10px] text-slate-500 font-mono">Percentage Discount</div>
                                </div>
                              )}
                              {promo.discount_type === 'fixed' && (
                                <div>
                                  <span className="font-black text-emerald-400 font-mono text-sm">
                                    ৳{promo.discount_value} OFF
                                  </span>
                                  <div className="text-[10px] text-slate-500 font-mono">Flat Cash Discount</div>
                                </div>
                              )}
                              {promo.discount_type === 'bonus_days' && (
                                <div>
                                  <span className="font-black text-amber-300 font-mono text-sm">
                                    +{promo.bonus_days} Days
                                  </span>
                                  <div className="text-[10px] text-slate-500 font-mono">Extra Protection Days</div>
                                </div>
                              )}
                              {promo.discount_type === 'bonus_credits' && (
                                <div>
                                  <span className="font-black text-cyan-300 font-mono text-sm">
                                    +{promo.bonus_credits} Trips
                                  </span>
                                  <div className="text-[10px] text-slate-500 font-mono">Bonus Trip Credits</div>
                                </div>
                              )}
                            </td>

                            {/* Min Order & Cap */}
                            <td className="py-3.5 px-4 font-mono text-[11px]">
                              <div className="text-slate-300">
                                Min: {promo.min_order_amount ? `৳${promo.min_order_amount}` : 'None'}
                              </div>
                              <div className="text-slate-500 text-[10px]">
                                Cap: {promo.max_discount_amount ? `৳${promo.max_discount_amount}` : 'No limit'}
                              </div>
                            </td>

                            {/* Applicable Plans */}
                            <td className="py-3.5 px-4">
                              <div className="flex items-center space-x-1 flex-wrap gap-1">
                                {(!promo.applicable_plan_types || promo.applicable_plan_types.length === 0 || promo.applicable_plan_types.length >= 2) ? (
                                  <span className="px-2 py-0.5 rounded text-[9.5px] font-mono uppercase bg-slate-800 text-slate-300 border border-slate-700">
                                    ALL PLANS
                                  </span>
                                ) : (
                                  promo.applicable_plan_types.map((type, idx) => (
                                    <span
                                      key={idx}
                                      className={`px-2 py-0.5 rounded text-[9.5px] font-mono uppercase border ${
                                        type === 'trip_credits'
                                          ? 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30'
                                          : 'bg-blue-500/10 text-blue-400 border-blue-500/30'
                                      }`}
                                    >
                                      {type === 'trip_credits' ? 'TRIP CREDITS' : 'TIME PASSES'}
                                    </span>
                                  ))
                                )}
                              </div>
                            </td>

                            {/* Redemptions */}
                            <td className="py-3.5 px-4 font-mono">
                              <div className="flex items-center space-x-1.5">
                                <span className={`font-bold ${isMaxReached ? 'text-red-400' : 'text-slate-200'}`}>
                                  {promo.used_count || 0}
                                </span>
                                <span className="text-slate-500">/</span>
                                <span className="text-slate-400">
                                  {promo.max_uses != null ? promo.max_uses : '∞'}
                                </span>
                              </div>
                              {isMaxReached && (
                                <div className="text-[9.5px] text-red-400 font-mono mt-0.5">MAX USES REACHED</div>
                              )}
                            </td>

                            {/* Validity */}
                            <td className="py-3.5 px-4 text-[11px] font-mono">
                              {promo.valid_until ? (
                                <div>
                                  <span className={isExpired ? 'text-red-400 font-bold' : 'text-slate-300'}>
                                    {formatGmtPlus6DateTime(promo.valid_until)}
                                  </span>
                                  {isExpired && (
                                    <div className="text-[9.5px] text-red-400 font-mono font-bold mt-0.5">EXPIRED</div>
                                  )}
                                </div>
                              ) : (
                                <span className="text-emerald-400">Never Expires</span>
                              )}
                            </td>

                            {/* Status */}
                            <td className="py-3.5 px-4">
                              <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono font-bold flex items-center space-x-1 w-max border ${
                                promo.is_active && !isExpired && !isMaxReached
                                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                                  : 'bg-slate-800 text-slate-400 border-slate-700'
                              }`}>
                                <span className={`w-1.5 h-1.5 rounded-full ${
                                  promo.is_active && !isExpired && !isMaxReached
                                    ? 'bg-emerald-400 animate-pulse'
                                    : 'bg-slate-600'
                                }`}></span>
                                <span>{promo.is_active && !isExpired && !isMaxReached ? 'ACTIVE' : 'INACTIVE'}</span>
                              </span>
                            </td>

                            {/* Actions */}
                            {isAdmin && (
                              <td className="py-3.5 px-4 text-right">
                                <div className="flex items-center justify-end space-x-1.5">
                                  <button
                                    type="button"
                                    onClick={() => handleTogglePromoActive(promo)}
                                    className={`py-1 px-2 rounded-lg text-xs font-semibold transition-all border cursor-pointer ${
                                      promo.is_active
                                        ? 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
                                        : 'bg-emerald-600/10 hover:bg-emerald-600/20 text-emerald-400 border-emerald-500/30'
                                    }`}
                                    title={promo.is_active ? 'Deactivate promo code' : 'Activate promo code'}
                                  >
                                    <span className="material-icons text-xs">
                                      {promo.is_active ? 'pause' : 'play_arrow'}
                                    </span>
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => handleOpenEditPromoModal(promo)}
                                    className="py-1 px-2 rounded-lg text-xs font-semibold bg-indigo-600/10 hover:bg-indigo-600/20 text-indigo-400 border border-indigo-500/20 transition-all cursor-pointer"
                                    title="Edit Promo Code"
                                  >
                                    <span className="material-icons text-xs">edit</span>
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => setDeletePromoTarget(promo)}
                                    className="py-1 px-2 rounded-lg text-xs font-semibold bg-red-600/10 hover:bg-red-600/20 text-red-400 border border-red-500/20 transition-all cursor-pointer"
                                    title="Delete Promo Code"
                                  >
                                    <span className="material-icons text-xs">delete</span>
                                  </button>
                                </div>
                              </td>
                            )}
                          </tr>
                        );
                      })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* SUBTAB 3: PAYMENT SETTINGS */}
      {activeSubTab === 'payment_settings' && (
        <form onSubmit={handleSavePaymentSettings} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-6">
          <div className="border-b border-slate-800 pb-4">
            <h3 className="text-base font-bold text-white flex items-center space-x-2">
              <span className="material-icons text-blue-400 text-lg">tune</span>
              <span>Manual Payment Numbers & Traveler Guidelines</span>
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              These mobile banking numbers and instructions are shown inside the Android app when users choose to pay and subscribe.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* bKash */}
            <div className="bg-slate-950 border border-slate-800 p-4 rounded-xl space-y-2">
              <div className="flex items-center space-x-2 text-pink-400 font-bold text-xs uppercase font-mono">
                <span className="material-icons text-sm">phone_android</span>
                <span>bKash Number</span>
              </div>
              <input
                type="text"
                value={bkashNumber}
                onChange={e => setBkashNumber(e.target.value)}
                disabled={!isAdmin}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-pink-500"
                placeholder="017XXXXXXXX"
              />
              <p className="text-[10px] text-slate-500">Merchant or Personal number for bKash Send Money.</p>
            </div>

            {/* Nagad */}
            <div className="bg-slate-950 border border-slate-800 p-4 rounded-xl space-y-2">
              <div className="flex items-center space-x-2 text-orange-400 font-bold text-xs uppercase font-mono">
                <span className="material-icons text-sm">phone_android</span>
                <span>Nagad Number</span>
              </div>
              <input
                type="text"
                value={nagadNumber}
                onChange={e => setNagadNumber(e.target.value)}
                disabled={!isAdmin}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-orange-500"
                placeholder="018XXXXXXXX"
              />
              <p className="text-[10px] text-slate-500">Merchant or Personal number for Nagad Send Money.</p>
            </div>

            {/* Rocket */}
            <div className="bg-slate-950 border border-slate-800 p-4 rounded-xl space-y-2">
              <div className="flex items-center space-x-2 text-purple-400 font-bold text-xs uppercase font-mono">
                <span className="material-icons text-sm">phone_android</span>
                <span>Rocket Number</span>
              </div>
              <input
                type="text"
                value={rocketNumber}
                onChange={e => setRocketNumber(e.target.value)}
                disabled={!isAdmin}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-purple-500"
                placeholder="019XXXXXXXX"
              />
              <p className="text-[10px] text-slate-500">Merchant or Personal number for Rocket.</p>
            </div>
          </div>

          {/* Instructions Box */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300 flex items-center space-x-2">
              <span className="material-icons text-slate-400 text-sm">info</span>
              <span>Traveler Payment Guidelines (Shown on Checkout Screen)</span>
            </label>
            <textarea
              rows={3}
              value={paymentInstructions}
              onChange={e => setPaymentInstructions(e.target.value)}
              disabled={!isAdmin}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
              placeholder="Guidelines for user to follow..."
            />
          </div>

          {isAdmin && (
            <div className="flex justify-end pt-2">
              <button
                type="submit"
                disabled={isSavingSettings}
                className="flex items-center space-x-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-blue-600/30 cursor-pointer disabled:opacity-50"
              >
                <span className={`material-icons text-sm ${isSavingSettings ? 'animate-spin' : ''}`}>save</span>
                <span>{isSavingSettings ? 'Saving...' : 'Save Payment Settings'}</span>
              </button>
            </div>
          )}
        </form>
      )}

      {/* PLAN CREATE / EDIT MODAL WIZARD */}
      {isPlanModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-xl overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="p-5 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span className="material-icons text-blue-400 text-xl">
                  {editingPlanId ? 'edit' : 'add_circle'}
                </span>
                <h3 className="text-base font-bold text-white">
                  {editingPlanId ? 'Edit Subscription Package' : 'Create Subscription Package'}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsPlanModalOpen(false)}
                className="text-slate-400 hover:text-white transition-colors cursor-pointer"
              >
                <span className="material-icons text-sm">close</span>
              </button>
            </div>

            <form onSubmit={handleSavePlan} className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
              {/* Plan Category / Type */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">Plan Category</label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      setPlanType('time_pass');
                      if (planPeriod === 'pay_per_trip') setPlanPeriod('monthly');
                    }}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center justify-center space-x-2 cursor-pointer ${
                      planType === 'time_pass'
                        ? 'bg-blue-600/20 border-blue-500 text-blue-300 shadow-sm'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="material-icons text-sm">schedule</span>
                    <span>Safety Time Pass</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setPlanType('trip_credits');
                      setPlanPeriod('pay_per_trip');
                    }}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center justify-center space-x-2 cursor-pointer ${
                      planType === 'trip_credits'
                        ? 'bg-cyan-600/20 border-cyan-500 text-cyan-300 shadow-sm'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="material-icons text-sm">confirmation_number</span>
                    <span>Trip Credits Pack</span>
                  </button>
                </div>
              </div>

              {/* Title */}
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Package Title *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Pro Guard Elite"
                  value={planName}
                  onChange={e => setPlanName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              {/* Included Trip Credits if trip_credits pack */}
              {planType === 'trip_credits' && (
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-cyan-300 flex items-center justify-between">
                    <span>Included Trip Credits *</span>
                    <span className="text-[10px] font-mono text-cyan-400">Total individual trips</span>
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={1000}
                    required
                    value={planCreditsCount}
                    onChange={e => setPlanCreditsCount(parseInt(e.target.value) || 1)}
                    className="w-full bg-slate-950 border border-cyan-800/80 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-cyan-500"
                    placeholder="e.g. 10"
                  />
                  <p className="text-[10px] text-slate-500">
                    User will be able to complete this many trips within the validity duration.
                  </p>
                </div>
              )}

              {/* Billing Period & Duration */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Billing Period & Type</label>
                  <select
                    value={planPeriod}
                    onChange={e => {
                      const newPeriod = e.target.value;
                      setPlanPeriod(newPeriod);
                      if (newPeriod === 'daily') setPlanDurationDays(1);
                      else if (newPeriod === '3_days') setPlanDurationDays(3);
                      else if (newPeriod === 'weekly') setPlanDurationDays(7);
                      else if (newPeriod === '15_days') setPlanDurationDays(15);
                      else if (newPeriod === 'monthly') setPlanDurationDays(30);
                      else if (newPeriod === 'quarterly') setPlanDurationDays(90);
                      else if (newPeriod === 'half_yearly') setPlanDurationDays(180);
                      else if (newPeriod === 'yearly') setPlanDurationDays(365);
                      else if (newPeriod === 'pay_per_trip') setPlanDurationDays(30);
                    }}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  >
                    <optgroup label="Day-Based Passes">
                      <option value="daily">1 Day Pass (Daily)</option>
                      <option value="3_days">3 Days Pass</option>
                      <option value="weekly">7 Days / Weekly Pass</option>
                      <option value="15_days">15 Days Pass</option>
                    </optgroup>
                    <optgroup label="Standard Period">
                      <option value="monthly">Monthly (30 Days)</option>
                      <option value="quarterly">Quarterly (90 Days)</option>
                      <option value="half_yearly">Half-Yearly (180 Days)</option>
                      <option value="yearly">Yearly (365 Days)</option>
                    </optgroup>
                    <optgroup label="Custom & Usage Based">
                      <option value="pay_per_trip">Pay-Per-Trip / Credit Pack</option>
                      <option value="custom">Custom Duration (Set Days Below)</option>
                    </optgroup>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300 flex items-center justify-between">
                    <span>Duration (Days) *</span>
                    {planPeriod === 'custom' && <span className="text-cyan-400 text-[10px] font-mono">Custom Days</span>}
                    {planPeriod === 'pay_per_trip' && <span className="text-amber-400 text-[10px] font-mono">Credit Validity</span>}
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={3650}
                    required
                    value={planDurationDays}
                    onChange={e => setPlanDurationDays(parseInt(e.target.value) || 1)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                  <p className="text-[10px] text-slate-500">
                    {planPeriod === 'pay_per_trip' 
                      ? 'Duration until trip credits expire.' 
                      : 'Number of active premium days granted upon purchase.'}
                  </p>
                </div>
              </div>

              {/* Price & Discount Price */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Regular Price (BDT ৳) *</label>
                  <input
                    type="number"
                    min={0}
                    required
                    value={planPrice}
                    onChange={e => setPlanPrice(parseFloat(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Discount Offer Price (Optional)</label>
                  <input
                    type="number"
                    min={0}
                    placeholder="Leave empty if none"
                    value={planDiscountPrice}
                    onChange={e => setPlanDiscountPrice(e.target.value === '' ? '' : parseFloat(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Features Bullet Points */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-300">Included Perks & Features</label>
                <div className="flex space-x-2">
                  <input
                    type="text"
                    placeholder="e.g. Priority SOS Emergency Support"
                    value={newFeatureInput}
                    onChange={e => setNewFeatureInput(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); handleAddFeature(); } }}
                    className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  />
                  <button
                    type="button"
                    onClick={handleAddFeature}
                    className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-bold transition-all border border-slate-700 cursor-pointer"
                  >
                    + Add
                  </button>
                </div>

                {/* Tags List */}
                <div className="space-y-1.5 pt-2">
                  {planFeatures.map((feat, idx) => (
                    <div key={idx} className="flex items-center justify-between bg-slate-950 border border-slate-800/80 px-3 py-1.5 rounded-lg text-xs text-slate-300">
                      <span className="flex items-center space-x-2">
                        <span className="material-icons text-emerald-400 text-xs">check</span>
                        <span>{feat}</span>
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemoveFeature(idx)}
                        className="text-slate-500 hover:text-red-400 cursor-pointer"
                      >
                        <span className="material-icons text-xs">close</span>
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Toggles */}
              <div className="grid grid-cols-2 gap-4 pt-2 border-t border-slate-800">
                <label className="flex items-center space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={planIsPopular}
                    onChange={e => setPlanIsPopular(e.target.checked)}
                    className="w-4 h-4 rounded text-blue-600 bg-slate-950 border-slate-800"
                  />
                  <span className="text-xs font-medium text-slate-300">Highlight as "Popular"</span>
                </label>

                <label className="flex items-center space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={planIsActive}
                    onChange={e => setPlanIsActive(e.target.checked)}
                    className="w-4 h-4 rounded text-blue-600 bg-slate-950 border-slate-800"
                  />
                  <span className="text-xs font-medium text-slate-300">Package Active</span>
                </label>
              </div>

              {/* Footer Buttons */}
              <div className="flex items-center justify-end space-x-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsPlanModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition-all cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSavingPlan}
                  className="px-5 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-blue-600/30 cursor-pointer disabled:opacity-50 flex items-center space-x-1.5"
                >
                  <span className={`material-icons text-sm ${isSavingPlan ? 'animate-spin' : ''}`}>
                    {isSavingPlan ? 'hourglass_top' : 'save'}
                  </span>
                  <span>{isSavingPlan ? 'Saving...' : 'Save Plan'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CONFIRMATION: APPROVE TRANSACTION */}
      {approveTargetTx && (
        <ConfirmationWizard
          isOpen={true}
          title={
            approveTargetTx.plan_type === 'trip_credits' || approveTargetTx.subscription_plans?.plan_type === 'trip_credits'
              ? 'Approve Trip Credit Pack Payment'
              : 'Approve Safety Pass Payment'
          }
          message={`Are you sure you want to approve payment of ৳${approveTargetTx.amount} from sender ${approveTargetTx.sender_number} (TrxID: ${approveTargetTx.transaction_id})? This will immediately grant ${
            approveTargetTx.plan_type === 'trip_credits' || approveTargetTx.subscription_plans?.plan_type === 'trip_credits'
              ? `${(approveTargetTx.credits_count || approveTargetTx.subscription_plans?.credits_count || 10) + (approveTargetTx.bonus_credits || 0)} Trip Credits`
              : 'Premium Pass status'
          } to ${approveTargetTx.profiles?.full_name || 'the user'}.`}
          confirmText="Approve & Grant"
          cancelText="Cancel"
          variant="success"
          icon="verified"
          isLoading={isApprovingTx}
          onConfirm={handleApproveTransaction}
          onClose={() => setApproveTargetTx(null)}
          consequences={
            approveTargetTx.plan_type === 'trip_credits' || approveTargetTx.subscription_plans?.plan_type === 'trip_credits'
              ? [
                  `User will receive ${(approveTargetTx.credits_count || approveTargetTx.subscription_plans?.credits_count || 10) + (approveTargetTx.bonus_credits || 0)} trip credits.`,
                  `Trip credits validity will be extended by ${approveTargetTx.subscription_plans?.duration_days || 30} days.`,
                  'The transaction status will be permanently marked as Approved.'
                ]
              : [
                  'The user profile will be marked is_premium = true.',
                  `The premium_until timestamp will be extended by ${(approveTargetTx.subscription_plans?.duration_days || 30) + (approveTargetTx.bonus_days || 0)} days.`,
                  'The transaction status will be permanently marked as Approved.'
                ]
          }
        />
      )}

      {/* PROMO CODE CREATE / EDIT MODAL */}
      {isPromoModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-xl overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="p-5 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span className="material-icons text-indigo-400 text-xl">
                  {editingPromoId ? 'edit' : 'local_offer'}
                </span>
                <h3 className="text-base font-bold text-white">
                  {editingPromoId ? 'Edit Promo Code' : 'Create New Promo Code'}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsPromoModalOpen(false)}
                className="text-slate-400 hover:text-white transition-colors cursor-pointer"
              >
                <span className="material-icons text-sm">close</span>
              </button>
            </div>

            <form onSubmit={handleSavePromoCode} className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
              {/* Promo Code String */}
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Promo Code (Uppercase) *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. EID50, SUMMER2026, SAFE100"
                  value={promoCodeInput}
                  onChange={e => setPromoCodeInput(e.target.value.toUpperCase())}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-indigo-300 font-mono font-bold tracking-wider focus:outline-none focus:border-indigo-500"
                />
                <p className="text-[10px] text-slate-500">Travelers will type this code during checkout.</p>
              </div>

              {/* Discount / Benefit Type */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300">Benefit Type *</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setPromoDiscountType('percent')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center space-x-2 cursor-pointer ${
                      promoDiscountType === 'percent'
                        ? 'bg-indigo-600/20 border-indigo-500 text-indigo-300'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="material-icons text-sm">percent</span>
                    <span>Percentage (%)</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPromoDiscountType('fixed')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center space-x-2 cursor-pointer ${
                      promoDiscountType === 'fixed'
                        ? 'bg-indigo-600/20 border-indigo-500 text-indigo-300'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="material-icons text-sm">payments</span>
                    <span>Flat Cash (৳)</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPromoDiscountType('bonus_days')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center space-x-2 cursor-pointer ${
                      promoDiscountType === 'bonus_days'
                        ? 'bg-amber-600/20 border-amber-500 text-amber-300'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="material-icons text-sm">more_time</span>
                    <span>Bonus Days (+Days)</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPromoDiscountType('bonus_credits')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center space-x-2 cursor-pointer ${
                      promoDiscountType === 'bonus_credits'
                        ? 'bg-cyan-600/20 border-cyan-500 text-cyan-300'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="material-icons text-sm">confirmation_number</span>
                    <span>Bonus Trips (+Trips)</span>
                  </button>
                </div>
              </div>

              {/* Benefit Value inputs */}
              <div className="grid grid-cols-2 gap-4">
                {promoDiscountType === 'percent' && (
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-slate-300">Discount Percentage (%) *</label>
                    <input
                      type="number"
                      min={1}
                      max={100}
                      required
                      value={promoDiscountValue}
                      onChange={e => setPromoDiscountValue(parseFloat(e.target.value) || 0)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                      placeholder="e.g. 20"
                    />
                  </div>
                )}

                {promoDiscountType === 'fixed' && (
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-slate-300">Discount Amount (BDT ৳) *</label>
                    <input
                      type="number"
                      min={1}
                      required
                      value={promoDiscountValue}
                      onChange={e => setPromoDiscountValue(parseFloat(e.target.value) || 0)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                      placeholder="e.g. 50"
                    />
                  </div>
                )}

                {promoDiscountType === 'bonus_days' && (
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-amber-300">Bonus Extra Days *</label>
                    <input
                      type="number"
                      min={1}
                      required
                      value={promoBonusDays}
                      onChange={e => setPromoBonusDays(parseInt(e.target.value) || 0)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                      placeholder="e.g. 7"
                    />
                  </div>
                )}

                {promoDiscountType === 'bonus_credits' && (
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-cyan-300">Bonus Extra Trips *</label>
                    <input
                      type="number"
                      min={1}
                      required
                      value={promoBonusCredits}
                      onChange={e => setPromoBonusCredits(parseInt(e.target.value) || 0)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                      placeholder="e.g. 5"
                    />
                  </div>
                )}

                {/* Min Order Amount */}
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Minimum Order (BDT ৳)</label>
                  <input
                    type="number"
                    min={0}
                    value={promoMinOrder}
                    onChange={e => setPromoMinOrder(parseFloat(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                    placeholder="0"
                  />
                  <p className="text-[10px] text-slate-500">Order must be at least this amount to apply.</p>
                </div>

                {/* Max Discount Cap (if percent) */}
                {promoDiscountType === 'percent' && (
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-slate-300">Max Discount Cap (BDT ৳)</label>
                    <input
                      type="number"
                      min={1}
                      value={promoMaxDiscount}
                      onChange={e => setPromoMaxDiscount(e.target.value === '' ? '' : parseFloat(e.target.value) || 0)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                      placeholder="No limit"
                    />
                    <p className="text-[10px] text-slate-500">Maximum savings allowed for % discount.</p>
                  </div>
                )}
              </div>

              {/* Applicable Plan Types */}
              <div className="space-y-1.5 pt-2 border-t border-slate-800">
                <label className="text-xs font-semibold text-slate-300">Applicable Plan Categories</label>
                <div className="flex items-center space-x-4">
                  <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={promoApplicablePlanTypes.includes('time_pass')}
                      onChange={e => {
                        if (e.target.checked) {
                          setPromoApplicablePlanTypes([...promoApplicablePlanTypes, 'time_pass']);
                        } else {
                          setPromoApplicablePlanTypes(promoApplicablePlanTypes.filter(t => t !== 'time_pass'));
                        }
                      }}
                      className="w-4 h-4 rounded text-indigo-600 bg-slate-950 border-slate-800"
                    />
                    <span>Safety Time Passes</span>
                  </label>

                  <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={promoApplicablePlanTypes.includes('trip_credits')}
                      onChange={e => {
                        if (e.target.checked) {
                          setPromoApplicablePlanTypes([...promoApplicablePlanTypes, 'trip_credits']);
                        } else {
                          setPromoApplicablePlanTypes(promoApplicablePlanTypes.filter(t => t !== 'trip_credits'));
                        }
                      }}
                      className="w-4 h-4 rounded text-indigo-600 bg-slate-950 border-slate-800"
                    />
                    <span>Trip Credit Packs</span>
                  </label>
                </div>
              </div>

              {/* Usage & Expiration Limits */}
              <div className="grid grid-cols-2 gap-4 pt-2 border-t border-slate-800">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Max Redemptions</label>
                  <input
                    type="number"
                    min={1}
                    value={promoMaxUses}
                    onChange={e => setPromoMaxUses(e.target.value === '' ? '' : parseInt(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                    placeholder="Unlimited"
                  />
                  <p className="text-[10px] text-slate-500">Leave empty for unlimited redemptions.</p>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Expiry Date & Time</label>
                  <input
                    type="datetime-local"
                    value={promoValidUntil}
                    onChange={e => setPromoValidUntil(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                  />
                  <p className="text-[10px] text-slate-500">Leave empty for no expiry.</p>
                </div>
              </div>

              {/* Is Active Toggle */}
              <div className="pt-2 border-t border-slate-800">
                <label className="flex items-center space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={promoIsActive}
                    onChange={e => setPromoIsActive(e.target.checked)}
                    className="w-4 h-4 rounded text-indigo-600 bg-slate-950 border-slate-800"
                  />
                  <span className="text-xs font-medium text-slate-300">Promo Code Active</span>
                </label>
              </div>

              {/* Footer Buttons */}
              <div className="flex items-center justify-end space-x-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsPromoModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition-all cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSavingPromo}
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-600/30 cursor-pointer disabled:opacity-50 flex items-center space-x-1.5"
                >
                  <span className={`material-icons text-sm ${isSavingPromo ? 'animate-spin' : ''}`}>
                    {isSavingPromo ? 'hourglass_top' : 'save'}
                  </span>
                  <span>{isSavingPromo ? 'Saving...' : 'Save Promo Code'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CONFIRMATION: DELETE PROMO CODE */}
      {deletePromoTarget && (
        <ConfirmationWizard
          isOpen={true}
          title={`Delete Promo Code "${deletePromoTarget.code}"?`}
          message="Are you sure you want to permanently delete this promotional code? Travelers will no longer be able to apply this discount."
          confirmText="Yes, Delete Code"
          cancelText="Cancel"
          variant="danger"
          icon="delete_forever"
          isLoading={isDeletingPromo}
          onConfirm={handleDeletePromoCode}
          onClose={() => setDeletePromoTarget(null)}
        />
      )}

      {/* CONFIRMATION: REJECT TRANSACTION */}
      {rejectTargetTx && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 space-y-4 shadow-2xl">
            <div className="flex items-center space-x-3 text-red-400">
              <span className="material-icons text-2xl">cancel</span>
              <h3 className="text-base font-bold text-white">Reject Payment Submission</h3>
            </div>
            <p className="text-xs text-slate-300">
              You are rejecting TrxID: <span className="font-mono font-bold text-white">{rejectTargetTx.transaction_id}</span> from sender {rejectTargetTx.sender_number}.
            </p>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-400">Reason for Rejection (Optional):</label>
              <input
                type="text"
                placeholder="e.g. TrxID not found in mobile banking statement"
                value={rejectReason}
                onChange={e => setRejectReason(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-red-500"
              />
            </div>

            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                type="button"
                onClick={() => setRejectTargetTx(null)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition-all cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleRejectTransaction}
                disabled={isRejectingTx}
                className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-bold transition-all shadow-md cursor-pointer disabled:opacity-50"
              >
                {isRejectingTx ? 'Rejecting...' : 'Confirm Reject'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CONFIRMATION: DELETE PLAN */}
      {deletePlanTarget && (
        <ConfirmationWizard
          isOpen={true}
          title={`Delete Plan "${deletePlanTarget.name}"?`}
          message="Are you sure you want to permanently delete this subscription package? Existing subscribers will retain their current active dates, but new users will no longer see this package."
          confirmText="Yes, Delete Package"
          cancelText="Cancel"
          variant="danger"
          icon="delete_forever"
          isLoading={isDeletingPlan}
          onConfirm={handleDeletePlan}
          onClose={() => setDeletePlanTarget(null)}
        />
      )}

    </div>
  );
}
