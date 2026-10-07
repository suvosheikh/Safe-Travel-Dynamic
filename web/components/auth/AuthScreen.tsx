'use client';

import React, { useState } from 'react';
import { motion } from 'motion/react';
import { getSupabaseClient, hasSupabaseConfig, Profile } from '../../lib/supabase';
import { useToast } from '../ui/Toast';

interface AuthScreenProps {
  onAuthSuccess: (user: { email: string; role: 'admin' | 'dispatcher' | 'user'; name: string; id: string }) => void;
  dbProfiles: Profile[];
  onUpdateProfiles: (updatedProfiles: Profile[]) => void;
}

export default function AuthScreen({ onAuthSuccess, dbProfiles, onUpdateProfiles }: AuthScreenProps) {
  const { toast } = useToast();
  const [isSignUp, setIsSignUp] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [role, setRole] = useState<'admin' | 'dispatcher' | 'user'>('admin');
  
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const supabaseActive = hasSupabaseConfig();

  // Handle Form Submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!email || !password) {
      setErrorMsg('Please fill in all required fields.');
      return;
    }

    if (isSignUp && (!fullName || !phoneNumber)) {
      setErrorMsg('Please specify your full name and contact number to register.');
      return;
    }

    setIsLoading(true);

    try {
      const supabase = getSupabaseClient();

      if (supabaseActive && supabase) {
        // --- REAL SUPABASE AUTHENTICATION ---
        if (isSignUp) {
          // 1. Sign up user in Auth
          const { data: signUpData, error: authError } = await supabase.auth.signUp({
            email,
            password,
          });

          if (authError) {
            throw new Error(authError.message);
          }

          const authUser = signUpData?.user;
          if (!authUser) {
            throw new Error('Authentication registration returned empty profile credentials.');
          }

          // 2. Insert corresponding profile row
          const { error: profileError } = await supabase
            .from('profiles')
            .insert([
              {
                id: authUser.id,
                full_name: fullName.trim(),
                phone_number: phoneNumber.trim(),
                is_premium: role === 'admin',
                trip_credits: role === 'admin' ? 100 : 10,
                points_balance: 500,
                role: role,
              },
            ]);

          if (profileError) {
            console.error('Failed to create profile row:', profileError);
            // Even if profile row fails, the auth user is created. We try to continue or warn.
          }

          setSuccessMsg('Account registered successfully! You can now log in.');
          setIsSignUp(false);
          setPassword('');
        } else {
          // Log In
          const { data: signInData, error: signInError } = await supabase.auth.signInWithPassword({
            email,
            password,
          });

          if (signInError) {
            throw new Error(signInError.message);
          }

          const authUser = signInData?.user;
          if (!authUser) {
            throw new Error('Sign-in returned empty auth details.');
          }

          // Fetch user profile
          const { data: profileData, error: profileErr } = await supabase
            .from('profiles')
            .select('id, full_name, phone_number, is_premium, trip_credits, points_balance, role')
            .eq('id', authUser.id)
            .single();

          if (profileErr) {
            console.warn('Could not retrieve database profile row, using defaults:', profileErr);
            // Fallback profile if row is not created yet
            const fallbackProfile: Profile = {
              id: authUser.id,
              full_name: email.split('@')[0],
              phone_number: '+1 (555) 000-0000',
              is_premium: true,
              trip_credits: 50,
              points_balance: 1000,
              role: 'admin',
            };
            onAuthSuccess({
              email: authUser.email || email,
              role: fallbackProfile.role,
              name: fallbackProfile.full_name,
              id: authUser.id,
            });
          } else if (profileData) {
            onAuthSuccess({
              email: authUser.email || email,
              role: profileData.role as 'admin' | 'dispatcher' | 'user',
              name: profileData.full_name,
              id: profileData.id,
            });
          }
        }
      } else {
        // --- LOCAL STORAGE BACKED MOCK AUTH ---
        // Retrieve local auth users list
        const savedUsersRaw = localStorage.getItem('safetravel_auth_users');
        let localUsers = savedUsersRaw ? JSON.parse(savedUsersRaw) : [
          { email: 'admin@safetravel.com', password: 'adminpassword', name: 'Alex Rivera', role: 'admin', phone_number: '+1 (555) 901-2345', id: 'usr-4' },
          { email: 'dispatcher@safetravel.com', password: 'dispatcherpassword', name: 'Elena Rostova', role: 'dispatcher', phone_number: '+1 (555) 876-5432', id: 'usr-5' },
        ];

        if (isSignUp) {
          // Check duplicate
          const exists = localUsers.some((u: any) => u.email.toLowerCase() === email.toLowerCase());
          if (exists) {
            throw new Error('Email address is already registered in local database.');
          }

          const newId = 'usr-' + Math.random().toString(36).substring(2, 9);
          const newUser = {
            id: newId,
            email: email.toLowerCase(),
            password,
            name: fullName.trim(),
            phone_number: phoneNumber.trim(),
            role,
          };

          localUsers.push(newUser);
          localStorage.setItem('safetravel_auth_users', JSON.stringify(localUsers));

          // Also inject into general Profiles list if not existing
          const newProfile: Profile = {
            id: newId,
            full_name: fullName.trim(),
            phone_number: phoneNumber.trim(),
            is_premium: role === 'admin',
            trip_credits: role === 'admin' ? 100 : 10,
            points_balance: 500,
            role: role,
          };

          const updatedProfiles = [...dbProfiles, newProfile];
          onUpdateProfiles(updatedProfiles);

          setSuccessMsg('Account registered in local system! You can now log in.');
          setIsSignUp(false);
          setPassword('');
        } else {
          // Local Log In
          const matchedUser = localUsers.find(
            (u: any) => u.email.toLowerCase() === email.toLowerCase() && u.password === password
          );

          if (!matchedUser) {
            throw new Error('Invalid email address or security key password.');
          }

          onAuthSuccess({
            email: matchedUser.email,
            role: matchedUser.role as 'admin' | 'dispatcher' | 'user',
            name: matchedUser.name,
            id: matchedUser.id,
          });
        }
      }
    } catch (err: any) {
      console.error('Authentication request error:', err);
      setErrorMsg(err.message || 'An unexpected authentication error occurred.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4 md:p-8 relative overflow-hidden" id="auth-screen-root">
      {/* Visual background accents */}
      <div className="absolute top-0 left-0 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2 pointer-events-none"></div>
      <div className="absolute bottom-0 right-0 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl translate-x-1/2 translate-y-1/2 pointer-events-none"></div>

      <motion.div 
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-md bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col"
        id="auth-card"
      >
        {/* Banner/Header decoration */}
        <div className="p-6 bg-gradient-to-r from-slate-900 to-slate-950 border-b border-slate-800 flex flex-col items-center justify-center text-center">
          <div className="w-12 h-12 bg-blue-600/15 border border-blue-500/30 rounded-xl flex items-center justify-center text-blue-500 mb-3 shadow-inner">
            <span className="material-icons text-2xl font-bold animate-pulse">radar</span>
          </div>
          <h1 className="text-lg font-bold text-white uppercase tracking-wider font-sans">
            SafeTravel <span className="text-blue-500 font-bold">Pro</span>
          </h1>
          <p className="text-[10px] text-slate-500 tracking-widest font-mono uppercase mt-1">
            Safety Dashboard & Portal
          </p>
        </div>

        {/* Form area */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 flex-1">
          {errorMsg && (
            <div className="p-3 bg-red-950/40 border border-red-900/60 rounded-xl flex items-start space-x-2 text-red-400 text-xs font-sans">
              <span className="material-icons text-sm mt-0.5 shrink-0">error_outline</span>
              <span>{errorMsg}</span>
            </div>
          )}

          {successMsg && (
            <div className="p-3 bg-emerald-950/40 border border-emerald-900/60 rounded-xl flex items-start space-x-2 text-emerald-400 text-xs font-sans">
              <span className="material-icons text-sm mt-0.5 shrink-0">check_circle_outline</span>
              <span>{successMsg}</span>
            </div>
          )}

          {isSignUp && (
            <>
              {/* Full Name */}
              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider font-mono">
                  Full Name
                </label>
                <div className="relative">
                  <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">person</span>
                  <input 
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Alex Rivera"
                    className="w-full bg-slate-900 border border-slate-800 hover:border-slate-700 focus:border-blue-500 text-slate-200 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
                  />
                </div>
              </div>

              {/* Phone Number */}
              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider font-mono">
                  Phone Number
                </label>
                <div className="relative">
                  <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">phone</span>
                  <input 
                    type="tel"
                    required
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    placeholder="+1 (555) 000-0000"
                    className="w-full bg-slate-900 border border-slate-800 hover:border-slate-700 focus:border-blue-500 text-slate-200 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
                  />
                </div>
              </div>

              {/* Role Selection Tabs */}
              <div className="space-y-1.5">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider font-mono">
                  Assign Administrative Role
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRole('admin')}
                    className={`p-2.5 rounded-xl border text-center transition-all cursor-pointer flex flex-col items-center justify-center space-y-1 ${
                      role === 'admin'
                        ? 'bg-blue-600/10 border-blue-500 text-blue-400 font-semibold'
                        : 'bg-slate-900 border-slate-800 text-slate-400 hover:bg-slate-850 hover:text-slate-300'
                    }`}
                  >
                    <span className="material-icons text-sm">admin_panel_settings</span>
                    <span className="text-[10px] font-sans">Administrator</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRole('dispatcher')}
                    className={`p-2.5 rounded-xl border text-center transition-all cursor-pointer flex flex-col items-center justify-center space-y-1 ${
                      role === 'dispatcher'
                        ? 'bg-blue-600/10 border-blue-500 text-blue-400 font-semibold'
                        : 'bg-slate-900 border-slate-800 text-slate-400 hover:bg-slate-850 hover:text-slate-300'
                    }`}
                  >
                    <span className="material-icons text-sm">support_agent</span>
                    <span className="text-[10px] font-sans">Dispatcher</span>
                  </button>
                </div>
              </div>
            </>
          )}

          {/* Email Address */}
          <div className="space-y-1.5">
            <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider font-mono">
              Email Address
            </label>
            <div className="relative">
              <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">mail</span>
              <input 
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="operator@safetravel.com"
                className="w-full bg-slate-900 border border-slate-800 hover:border-slate-700 focus:border-blue-500 text-slate-200 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
              />
            </div>
          </div>

          {/* Password */}
          <div className="space-y-1.5">
            <div className="flex justify-between items-center">
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider font-mono">
                Access Password Key
              </label>
              {!isSignUp && (
                <button
                  type="button"
                  onClick={() => toast.info('Admin: admin@safetravel.com / adminpassword\nDispatcher: dispatcher@safetravel.com / dispatcherpassword', 'Demo System Credentials', 7000)}
                  className="text-[9px] font-sans text-blue-400 hover:underline cursor-pointer"
                >
                  Need access?
                </button>
              )}
            </div>
            <div className="relative">
              <span className="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">vpn_key</span>
              <input 
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                className="w-full bg-slate-900 border border-slate-800 hover:border-slate-700 focus:border-blue-500 text-slate-200 font-sans text-xs rounded-xl pl-9 pr-4 py-2.5 outline-none transition-all"
              />
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading}
            className={`w-full py-2.5 rounded-xl text-xs font-bold text-white transition-all cursor-pointer flex items-center justify-center space-x-2 ${
              isLoading 
                ? 'bg-slate-700 border border-slate-650 cursor-not-allowed'
                : 'bg-blue-600 hover:bg-blue-500 active:scale-[0.98] shadow-lg shadow-blue-900/20'
            }`}
          >
            {isLoading ? (
              <>
                <span className="material-icons animate-spin text-sm">autorenew</span>
                <span>{isSignUp ? 'CREATING ACCOUNT...' : 'SIGNING IN...'}</span>
              </>
            ) : (
              <>
                <span className="material-icons text-sm">{isSignUp ? 'person_add' : 'lock_open'}</span>
                <span className="uppercase tracking-wider">{isSignUp ? 'CREATE ACCOUNT' : 'SIGN IN'}</span>
              </>
            )}
          </button>

          {/* Toggle Screen Mode */}
          <div className="pt-4 border-t border-slate-900 flex justify-center text-[11px]">
            <span className="text-slate-500 mr-1">
              {isSignUp ? 'Already have an account?' : 'Need to create an account?'}
            </span>
            <button
              type="button"
              onClick={() => {
                setErrorMsg('');
                setSuccessMsg('');
                setIsSignUp(!isSignUp);
              }}
              className="text-blue-400 font-semibold hover:underline cursor-pointer"
            >
              {isSignUp ? 'Log In Instead' : 'Register Here'}
            </button>
          </div>
        </form>

        {/* Database state footer indicator */}
        <div className="p-3 bg-slate-950 border-t border-slate-900 flex items-center justify-center space-x-2 text-[9px] font-mono text-slate-500">
          <span className={`w-2 h-2 rounded-full ${supabaseActive ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500 animate-pulse'}`}></span>
          <span className="uppercase tracking-widest">
            {supabaseActive ? 'Secure cloud connection active' : 'Local database connection active'}
          </span>
        </div>
      </motion.div>
    </div>
  );
}
