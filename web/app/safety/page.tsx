import PublicNavbar from '@/components/PublicNavbar';
import Link from 'next/link';

export default function SafetyPrivacyPage() {
  return (
    <div className="min-h-screen bg-[#020617] text-slate-100 font-sans selection:bg-emerald-500/30 overflow-x-hidden pt-24 pb-16">
      <PublicNavbar />
      
      <main className="max-w-3xl mx-auto px-6 mt-12">
        <div className="inline-flex items-center space-x-2 px-3 py-1.5 rounded-full bg-blue-900/30 border border-blue-500/30 mb-6">
          <span className="material-icons text-blue-400 text-sm">lock</span>
          <span className="text-[10px] md:text-xs font-mono text-blue-300 tracking-wide uppercase font-bold">Privacy First</span>
        </div>
        
        <h1 className="text-4xl md:text-5xl font-extrabold mb-8 tracking-tight">Your data is <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-cyan-400">Yours.</span></h1>
        
        <div className="space-y-8 text-slate-300 leading-relaxed">
          <section className="bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800">
            <h3 className="text-xl font-bold text-white mb-3">End-to-End Encryption</h3>
            <p className="text-sm md:text-base">
              All live location data transmitted between your device and your guardians' devices is fully encrypted. SafeTravel's servers only route the data; we cannot intercept or read your real-time coordinates.
            </p>
          </section>

          <section className="bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800">
            <h3 className="text-xl font-bold text-white mb-3">No Selling of Data</h3>
            <p className="text-sm md:text-base">
              Unlike other free tracking applications, SafeTravel operates on a strict non-monetization policy for personal data. Your location history, home addresses, and daily routines are never sold to advertisers or third-party brokers.
            </p>
          </section>

          <section className="bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800">
            <h3 className="text-xl font-bold text-white mb-3">Automatic 30-Day Deletion</h3>
            <p className="text-sm md:text-base">
              Any trip data temporarily stored for your review (e.g., past routes or SOS logs) is automatically and permanently wiped from our databases after 30 days. You also have the right to instantly delete your account and all associated data at any time from the app settings.
            </p>
          </section>
        </div>

        <div className="mt-16 text-center">
          <Link href="/" className="inline-flex items-center space-x-2 text-cyan-400 hover:text-cyan-300 font-semibold transition-colors">
            <span className="material-icons">arrow_back</span>
            <span>Back to Home</span>
          </Link>
        </div>
      </main>
    </div>
  );
}
