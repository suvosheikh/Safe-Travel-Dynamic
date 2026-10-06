const fs = require('fs');
const path = require('path');

const oldPagePath = 'D:/Soft/safe-travel-web/app/page.tsx';
const newDir = 'D:/Soft/safe-travel-web/app/(dashboard)/dashboard/[[...tab]]';
const newPagePath = path.join(newDir, 'page.tsx');

fs.mkdirSync(newDir, { recursive: true });

let code = fs.readFileSync(oldPagePath, 'utf8');

// 1. Update relative imports
code = code.replace(/\.\.\/components/g, '../../../../components');
code = code.replace(/\.\.\/lib/g, '../../../../lib');

// 2. Inject useRouter and useParams
if (!code.includes('next/navigation')) {
  code = code.replace(
    /import React, { useState } from 'react';/, 
    `import React, { useState } from 'react';\nimport { useParams, useRouter } from 'next/navigation';`
  );
}

// 3. Replace activeTab state with URL params logic
const stateRegex = /const \[activeTab, setActiveTab\] = useState<[^>]+>\('dashboard'\);/;
const replacementCode = `const params = useParams();
  const router = useRouter();
  const activeTab = (params?.tab && params.tab[0]) || 'dashboard';
  
  const setActiveTab = (tab: string) => {
    if (tab === 'dashboard') router.push('/dashboard');
    else router.push('/dashboard/' + tab);
  };`;

code = code.replace(stateRegex, replacementCode);

// Write to new location
fs.writeFileSync(newPagePath, code);

// Create the Landing Page in the old location
const landingPageCode = `import Link from 'next/link';
import { motion } from 'framer-motion';

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col font-sans selection:bg-emerald-500/30">
      
      {/* Navbar */}
      <nav className="w-full flex items-center justify-between px-8 py-6 border-b border-white/5 bg-slate-950/50 backdrop-blur-md fixed top-0 z-50">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-emerald-500 flex items-center justify-center shadow-[0_0_15px_rgba(16,185,129,0.5)]">
            <span className="material-icons text-white text-lg">public</span>
          </div>
          <span className="font-bold text-xl tracking-tight">Safe<span className="text-emerald-400">Travel</span></span>
        </div>
        <div className="flex items-center space-x-6">
          <Link href="#features" className="text-sm font-medium text-slate-400 hover:text-white transition-colors">Features</Link>
          <Link href="#security" className="text-sm font-medium text-slate-400 hover:text-white transition-colors">Security</Link>
          <Link href="/dashboard" className="px-5 py-2.5 rounded-full bg-blue-600 hover:bg-blue-500 text-white text-sm font-bold shadow-[0_0_20px_rgba(37,99,235,0.4)] transition-all flex items-center space-x-2 group">
            <span>Go to Dashboard</span>
            <span className="material-icons text-sm group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <main className="flex-1 flex flex-col items-center justify-center px-4 pt-32 pb-20 text-center relative overflow-hidden">
        
        {/* Background glow effects */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[600px] bg-blue-600/20 blur-[120px] rounded-full pointer-events-none"></div>
        <div className="absolute bottom-0 left-1/4 w-[400px] h-[400px] bg-emerald-600/10 blur-[100px] rounded-full pointer-events-none"></div>
        
        <div className="inline-flex items-center space-x-2 px-3 py-1.5 rounded-full bg-slate-800/50 border border-slate-700 mb-8 backdrop-blur-sm">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span className="text-xs font-mono text-slate-300">v2.0 Admin Portal Now Live</span>
        </div>

        <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight max-w-4xl leading-tight mb-8">
          Next-Generation <br/>
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-emerald-400">Transit Security & Tracking</span>
        </h1>
        
        <p className="text-lg md:text-xl text-slate-400 max-w-2xl mb-12 font-light leading-relaxed">
          Monitor your travelers globally with real-time GPS pucks, dynamic geofencing, AI-driven SOS alerts, and live battery telemetry. The ultimate command center for safety.
        </p>

        <div className="flex flex-col sm:flex-row items-center space-y-4 sm:space-y-0 sm:space-x-6 z-10">
          <Link href="/dashboard" className="w-full sm:w-auto px-8 py-4 rounded-xl bg-white text-slate-950 font-bold text-lg hover:bg-slate-100 transition-colors shadow-xl flex items-center justify-center space-x-3">
            <span className="material-icons">admin_panel_settings</span>
            <span>Launch Console</span>
          </Link>
          <Link href="#features" className="w-full sm:w-auto px-8 py-4 rounded-xl bg-slate-800 text-white font-bold text-lg hover:bg-slate-700 transition-colors border border-slate-700 flex items-center justify-center space-x-3">
            <span className="material-icons text-slate-400">explore</span>
            <span>Explore Features</span>
          </Link>
        </div>

        {/* Dashboard Preview Mockup */}
        <div className="mt-24 w-full max-w-5xl rounded-2xl border border-slate-800 bg-slate-900/50 p-2 shadow-2xl relative z-10 overflow-hidden group">
           <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-transparent to-transparent z-20"></div>
           <div className="w-full h-[400px] rounded-xl bg-slate-950 border border-slate-800 relative overflow-hidden flex items-center justify-center">
              <div className="absolute inset-0 bg-[url('https://api.mapbox.com/styles/v1/mapbox/dark-v11/static/-122.4,37.8,12/1000x600?access_token=pk.eyJ1Ijoic3V2b3NoZWlraCIsImEiOiJjbXRpZmt1dnowMDJtMzFzaGJtZGR3cnBlIn0.9P1CkB5iSVCt7w3exz6dcw')] bg-cover bg-center opacity-40 mix-blend-screen"></div>
              
              {/* Fake UI Elements for preview */}
              <div className="absolute top-6 left-6 w-64 h-32 bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-xl backdrop-blur-md">
                 <div className="h-4 w-24 bg-slate-800 rounded mb-4"></div>
                 <div className="h-3 w-48 bg-slate-800/50 rounded mb-2"></div>
                 <div className="h-3 w-32 bg-slate-800/50 rounded"></div>
              </div>
              <div className="absolute bottom-6 right-6 w-80 h-16 bg-slate-900/90 border border-slate-800 rounded-xl shadow-xl backdrop-blur-md flex items-center px-4 space-x-4">
                 <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center"><span className="material-icons text-blue-400">speed</span></div>
                 <div className="flex-1 h-3 bg-slate-800 rounded"></div>
              </div>
           </div>
        </div>

      </main>

    </div>
  );
}
`;

fs.writeFileSync(oldPagePath, landingPageCode);
console.log("Refactoring complete!");
