import PublicNavbar from '@/components/PublicNavbar';
import Link from 'next/link';

export default function AboutPage() {
  return (
    <div className="min-h-screen bg-[#020617] text-slate-100 font-sans selection:bg-emerald-500/30 overflow-x-hidden pt-24 pb-16">
      <PublicNavbar />
      
      <main className="max-w-3xl mx-auto px-6 mt-12 text-center">
        <h1 className="text-4xl md:text-5xl font-extrabold mb-6 tracking-tight">Making the world <span className="text-emerald-400">Safer.</span></h1>
        <p className="text-slate-400 text-lg mb-12 leading-relaxed">
          SafeTravel was born out of a simple necessity: the need to feel secure when traveling alone. 
        </p>
        
        <div className="w-full h-64 md:h-96 rounded-3xl overflow-hidden mb-12 relative">
          <div className="absolute inset-0 bg-gradient-to-t from-[#020617] via-transparent to-transparent z-10"></div>
          <img src="https://images.unsplash.com/photo-1517673132405-a56a62b18caf?q=80&w=2076&auto=format&fit=crop" alt="Team working" className="w-full h-full object-cover opacity-60" />
        </div>

        <div className="text-left space-y-6 text-slate-300 bg-slate-900/50 p-6 md:p-10 rounded-3xl border border-slate-800">
          <h3 className="text-2xl font-bold text-white mb-4">Our Mission</h3>
          <p className="text-sm md:text-base leading-relaxed">
            Whether it's a late-night commute from the office or a teenager taking their first solo trip to school, personal security should never be a luxury. Our mission is to provide accessible, reliable, and privacy-focused tracking tools for everyone.
          </p>
          <p className="text-sm md:text-base leading-relaxed">
            We are a team of dedicated developers, designers, and security experts committed to building technology that protects. By utilizing modern GPS, smart battery monitoring, and automated alerts, we bridge the gap between technology and peace of mind.
          </p>
        </div>

        <div className="mt-16 text-center">
          <Link href="/" className="inline-flex items-center space-x-2 text-emerald-400 hover:text-emerald-300 font-semibold transition-colors">
            <span className="material-icons">arrow_back</span>
            <span>Back to Home</span>
          </Link>
        </div>
      </main>
    </div>
  );
}
