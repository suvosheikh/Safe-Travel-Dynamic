import PublicNavbar from '@/components/PublicNavbar';
import Link from 'next/link';

export default function HowItWorksPage() {
  return (
    <div className="min-h-screen bg-[#020617] text-slate-100 font-sans selection:bg-emerald-500/30 overflow-x-hidden pt-24 pb-16">
      <PublicNavbar />
      
      <main className="max-w-4xl mx-auto px-6 mt-12">
        <h1 className="text-4xl md:text-5xl font-extrabold mb-6 tracking-tight">How SafeTravel <span className="text-emerald-400">Works</span></h1>
        <p className="text-slate-400 text-lg mb-16 leading-relaxed">
          Getting started with SafeTravel is incredibly simple. We designed the app to be intuitive so you can focus on your journey, not complex settings.
        </p>

        <div className="space-y-12 relative before:absolute before:inset-0 before:ml-6 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-slate-800 before:to-transparent">
          
          {/* Step 1 */}
          <div className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
            <div className="flex items-center justify-center w-12 h-12 rounded-full border-4 border-slate-900 bg-emerald-500 text-slate-900 font-bold shadow-[0_0_20px_rgba(16,185,129,0.5)] shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 z-10">
              1
            </div>
            <div className="w-[calc(100%-4rem)] md:w-[calc(50%-3rem)] bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800 backdrop-blur-sm">
              <h3 className="text-2xl font-bold text-white mb-2">Install & Setup</h3>
              <p className="text-slate-400 text-sm leading-relaxed">Download SafeTravel from the App Store or Google Play. Create your secure account and grant necessary location permissions for precise tracking.</p>
            </div>
          </div>

          {/* Step 2 */}
          <div className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
            <div className="flex items-center justify-center w-12 h-12 rounded-full border-4 border-slate-900 bg-blue-500 text-white font-bold shadow-[0_0_20px_rgba(59,130,246,0.5)] shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 z-10">
              2
            </div>
            <div className="w-[calc(100%-4rem)] md:w-[calc(50%-3rem)] bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800 backdrop-blur-sm">
              <h3 className="text-2xl font-bold text-white mb-2">Add Guardians</h3>
              <p className="text-slate-400 text-sm leading-relaxed">Invite your family members or close friends to be your "Guardians". They will receive automated alerts when you travel or if your battery runs low.</p>
            </div>
          </div>

          {/* Step 3 */}
          <div className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
            <div className="flex items-center justify-center w-12 h-12 rounded-full border-4 border-slate-900 bg-cyan-500 text-slate-900 font-bold shadow-[0_0_20px_rgba(6,182,212,0.5)] shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 z-10">
              3
            </div>
            <div className="w-[calc(100%-4rem)] md:w-[calc(50%-3rem)] bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800 backdrop-blur-sm">
              <h3 className="text-2xl font-bold text-white mb-2">Start a Session</h3>
              <p className="text-slate-400 text-sm leading-relaxed">Whenever you feel unsafe or are taking a late-night ride, simply tap "Start Session". Your guardians will instantly see your live GPS dot on their screens.</p>
            </div>
          </div>

        </div>

        <div className="mt-20 text-center">
          <Link href="/" className="inline-flex items-center space-x-2 text-emerald-400 hover:text-emerald-300 font-semibold transition-colors">
            <span className="material-icons">arrow_back</span>
            <span>Back to Home</span>
          </Link>
        </div>
      </main>
    </div>
  );
}
