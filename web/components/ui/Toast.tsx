'use client';

import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastItem {
  id: string;
  type: ToastType;
  title?: string;
  message: string;
  duration?: number;
}

interface ToastContextValue {
  toast: {
    success: (message: string, title?: string, duration?: number) => void;
    error: (message: string, title?: string, duration?: number) => void;
    warning: (message: string, title?: string, duration?: number) => void;
    info: (message: string, title?: string, duration?: number) => void;
  };
  showToast: (type: ToastType, message: string, title?: string, duration?: number) => void;
  dismissToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

// Global standalone dispatcher for use outside React hooks
type ToastListener = (toast: ToastItem) => void;
const listeners: Set<ToastListener> = new Set();

export const toastGlobal = {
  success: (message: string, title?: string, duration?: number) => {
    emitGlobalToast('success', message, title, duration);
  },
  error: (message: string, title?: string, duration?: number) => {
    emitGlobalToast('error', message, title, duration);
  },
  warning: (message: string, title?: string, duration?: number) => {
    emitGlobalToast('warning', message, title, duration);
  },
  info: (message: string, title?: string, duration?: number) => {
    emitGlobalToast('info', message, title, duration);
  }
};

function emitGlobalToast(type: ToastType, message: string, title?: string, duration: number = 4500) {
  const item: ToastItem = {
    id: 't-' + Math.random().toString(36).substring(2, 9),
    type,
    title,
    message,
    duration
  };
  listeners.forEach(fn => fn(item));
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (type: ToastType, message: string, title?: string, duration: number = 4500) => {
      const id = 't-' + Math.random().toString(36).substring(2, 9);
      const newToast: ToastItem = { id, type, title, message, duration };
      setToasts((prev) => [...prev.slice(-4), newToast]); // keep max 5 active
    },
    []
  );

  useEffect(() => {
    const handleGlobal = (t: ToastItem) => {
      setToasts((prev) => [...prev.slice(-4), t]);
    };
    listeners.add(handleGlobal);
    return () => {
      listeners.delete(handleGlobal);
    };
  }, []);

  const toastMethods = {
    success: (msg: string, title?: string, dur?: number) => showToast('success', msg, title ?? 'Success', dur),
    error: (msg: string, title?: string, dur?: number) => showToast('error', msg, title ?? 'Error', dur),
    warning: (msg: string, title?: string, dur?: number) => showToast('warning', msg, title ?? 'Warning', dur),
    info: (msg: string, title?: string, dur?: number) => showToast('info', msg, title ?? 'Information', dur),
  };

  return (
    <ToastContext.Provider value={{ toast: toastMethods, showToast, dismissToast }}>
      {children}
      {/* Toast Viewport Container */}
      <aside aria-label="Notifications" className="fixed top-5 right-5 z-[999999] flex flex-col gap-3 max-w-sm sm:max-w-md w-full pointer-events-none p-2 sm:p-0">
        <AnimatePresence mode="popLayout">
          {toasts.map((t) => (
            <ToastCard key={t.id} item={t} onDismiss={() => dismissToast(t.id)} />
          ))}
        </AnimatePresence>
      </aside>
    </ToastContext.Provider>
  );
}

function ToastCard({ item, onDismiss }: { item: ToastItem; onDismiss: () => void }) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onDismiss();
    }, item.duration ?? 4500);
    return () => clearTimeout(timer);
  }, [item, onDismiss]);

  const config = {
    success: {
      border: 'border-emerald-500/40 bg-slate-900/95 text-emerald-400 shadow-emerald-950/40',
      glow: 'bg-emerald-500/10',
      bar: 'bg-emerald-400',
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />,
      defaultTitle: 'Success'
    },
    error: {
      border: 'border-rose-500/40 bg-slate-900/95 text-rose-400 shadow-rose-950/40',
      glow: 'bg-rose-500/10',
      bar: 'bg-rose-400',
      icon: <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />,
      defaultTitle: 'Error'
    },
    warning: {
      border: 'border-amber-500/40 bg-slate-900/95 text-amber-400 shadow-amber-950/40',
      glow: 'bg-amber-500/10',
      bar: 'bg-amber-400',
      icon: <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />,
      defaultTitle: 'Attention'
    },
    info: {
      border: 'border-cyan-500/40 bg-slate-900/95 text-cyan-400 shadow-cyan-950/40',
      glow: 'bg-cyan-500/10',
      bar: 'bg-cyan-400',
      icon: <Info className="w-5 h-5 text-cyan-400 shrink-0" />,
      defaultTitle: 'Notification'
    }
  }[item.type];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: -16, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -10, scale: 0.9, transition: { duration: 0.15 } }}
      transition={{ type: 'spring', stiffness: 420, damping: 30 }}
      className={`pointer-events-auto relative overflow-hidden rounded-2xl border backdrop-blur-xl shadow-2xl p-4 flex flex-col gap-2 ${config.border}`}
    >
      <div className={`absolute inset-0 pointer-events-none ${config.glow}`} />
      <div className="relative flex items-start gap-3 z-10">
        <div className="mt-0.5">{config.icon}</div>
        <div className="flex-1 min-w-0 pr-2">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-200">
            {item.title || config.defaultTitle}
          </p>
          <p className="text-xs sm:text-sm text-slate-300 mt-0.5 leading-relaxed break-words font-normal">
            {item.message}
          </p>
        </div>
        <button
          onClick={onDismiss}
          className="shrink-0 p-1 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-white/10 transition-colors"
          aria-label="Close notification"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Progress countdown indicator */}
      <div className="relative w-full h-0.5 bg-slate-800 rounded-full overflow-hidden mt-1">
        <motion.div
          initial={{ width: '100%' }}
          animate={{ width: '0%' }}
          transition={{ duration: (item.duration ?? 4500) / 1000, ease: 'linear' }}
          className={`h-full ${config.bar}`}
        />
      </div>
    </motion.div>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    // Fallback to global dispatcher if outside provider
    return {
      toast: toastGlobal,
      showToast: (type: ToastType, msg: string, title?: string, dur?: number) => {
        emitGlobalToast(type, msg, title, dur);
      },
      dismissToast: () => {}
    };
  }
  return context;
}
