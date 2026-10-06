'use client';

import React, { useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';

export interface ConfirmationWizardProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'warning' | 'info' | 'success';
  icon?: string;
  consequences?: string[];
  isLoading?: boolean;
}

export default function ConfirmationWizard({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Yes, Confirm',
  cancelText = 'No, Cancel',
  variant = 'danger',
  icon,
  consequences = [],
  isLoading = false
}: ConfirmationWizardProps) {
  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isLoading) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isLoading, onClose]);

  if (!isOpen) return null;

  // Variant styling
  const variantStyles = {
    danger: {
      iconBg: 'bg-red-500/10 text-red-500 border-red-500/30',
      defaultIcon: 'delete_forever',
      confirmButton: 'bg-red-600 hover:bg-red-500 text-white shadow-sm shadow-red-600/30',
      badgeColor: 'text-red-400 bg-red-950/60 border-red-800/60'
    },
    warning: {
      iconBg: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
      defaultIcon: 'warning',
      confirmButton: 'bg-amber-600 hover:bg-amber-500 text-white shadow-sm shadow-amber-600/30',
      badgeColor: 'text-amber-400 bg-amber-950/60 border-amber-800/60'
    },
    info: {
      iconBg: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30',
      defaultIcon: 'help_outline',
      confirmButton: 'bg-cyan-600 hover:bg-cyan-500 text-white shadow-sm shadow-cyan-600/30',
      badgeColor: 'text-cyan-400 bg-cyan-950/60 border-cyan-800/60'
    },
    success: {
      iconBg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
      defaultIcon: 'check_circle',
      confirmButton: 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-sm shadow-emerald-600/30',
      badgeColor: 'text-emerald-400 bg-emerald-950/60 border-emerald-800/60'
    }
  }[variant];

  const displayIcon = icon || variantStyles.defaultIcon;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm select-none">
        {/* Backdrop click */}
        <div 
          className="fixed inset-0"
          onClick={() => {
            if (!isLoading) onClose();
          }}
        />

        {/* Modal Window */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 10 }}
          transition={{ duration: 0.15 }}
          className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden z-10"
        >
          <div className="p-5 sm:p-6 space-y-4">
            
            {/* Header: Icon & Title */}
            <div className="flex items-start gap-3.5">
              <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 border ${variantStyles.iconBg}`}>
                <span className="material-icons text-2xl">{displayIcon}</span>
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className={`text-[10px] font-mono uppercase px-2 py-0.5 rounded border font-semibold ${variantStyles.badgeColor}`}>
                    Action Verification
                  </span>
                </div>
                <h3 className="text-base font-bold text-white mt-1 leading-snug">
                  {title}
                </h3>
              </div>
            </div>

            {/* Message Body */}
            <div className="text-xs sm:text-sm text-slate-300 leading-relaxed bg-slate-950/40 p-3.5 rounded-xl border border-slate-800/80">
              {message}
            </div>

            {/* Consequence Bullets (If any) */}
            {consequences.length > 0 && (
              <div className="space-y-1.5 pt-1">
                <span className="text-[10.5px] font-mono uppercase tracking-wider text-slate-400 block font-semibold">
                  Important Consequences:
                </span>
                <ul className="space-y-1">
                  {consequences.map((item, idx) => (
                    <li key={idx} className="flex items-start gap-2 text-xs text-slate-400">
                      <span className="material-icons text-xs text-red-400 mt-0.5 shrink-0">fiber_manual_record</span>
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Action Buttons: Yes / No */}
            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-800">
              <button
                type="button"
                disabled={isLoading}
                onClick={onClose}
                className="px-4 py-2 text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-750 border border-slate-700 rounded-xl transition-colors disabled:opacity-50 cursor-pointer"
              >
                {cancelText}
              </button>
              <button
                type="button"
                disabled={isLoading}
                onClick={onConfirm}
                className={`px-4 py-2 text-xs font-bold rounded-xl transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50 ${variantStyles.confirmButton}`}
              >
                {isLoading ? (
                  <>
                    <span className="material-icons text-sm animate-spin">refresh</span>
                    <span>Processing...</span>
                  </>
                ) : (
                  <>
                    <span className="material-icons text-sm">check</span>
                    <span>{confirmText}</span>
                  </>
                )}
              </button>
            </div>

          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
