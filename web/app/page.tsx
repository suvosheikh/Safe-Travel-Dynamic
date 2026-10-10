'use client';
import Link from 'next/link';
import { motion } from 'framer-motion';
import PublicNavbar from '@/components/PublicNavbar';
import HeroSection from '@/components/sections/HeroSection';
import CoreFeaturesSection from '@/components/sections/CoreFeaturesSection';
import { useLanguage } from '@/components/LanguageProvider';

export default function LandingPage() {
  const { t } = useLanguage();

  return (
    <div className="min-h-screen bg-[#020617] text-slate-100 font-sans selection:bg-emerald-500/30 overflow-x-hidden">
      
      {/* ---------------- BACKGROUND AMBIENT GLOWS ---------------- */}
      <div className="fixed inset-0 z-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[-10%] left-[-10%] w-[50vw] h-[50vw] bg-emerald-600/10 blur-[120px] md:blur-[150px] rounded-full mix-blend-screen"></div>
        <div className="absolute top-[20%] right-[-10%] w-[40vw] h-[40vw] bg-blue-600/10 blur-[120px] md:blur-[150px] rounded-full mix-blend-screen"></div>
        <div className="absolute bottom-[-20%] left-[20%] w-[60vw] h-[60vw] bg-indigo-600/10 blur-[120px] md:blur-[150px] rounded-full mix-blend-screen"></div>
      </div>

      {/* ---------------- NAVBAR ---------------- */}
      <PublicNavbar />

      {/* ---------------- HERO SECTION ---------------- */}
      <HeroSection />

      {/* ---------------- BENTO BOX FEATURES ---------------- */}
      <CoreFeaturesSection />

      {/* ---------------- WHO IS IT FOR? ---------------- */}
      <section id="use-cases" className="py-14 md:py-20 bg-[#020617] relative z-10 border-t border-b border-white/5 w-full">
        <div className="w-full max-w-[1760px] mx-auto px-4 sm:px-6 md:px-10 lg:px-14 xl:px-16">
          <h2 className="text-2xl sm:text-3xl md:text-5xl font-bold mb-8 md:mb-14 text-center">{t('Built for', 'যাদের জন্য')} <span className="text-indigo-400">{t('everyday peace of mind.', 'নিশ্চিন্ত জীবন।')}</span></h2>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8">
            <div className="p-6 md:p-8 rounded-3xl bg-slate-900 border border-slate-800 relative">
               <div className="w-10 h-10 md:w-12 md:h-12 rounded-full bg-indigo-500/20 text-indigo-400 flex items-center justify-center mb-4 md:mb-6"><span className="material-icons text-xl md:text-base">nightlight_round</span></div>
               <h4 className="text-lg md:text-xl font-bold text-white mb-2">{t('Late-Night Commuters', 'রাতে ভ্রমণকারী')}</h4>
               <p className="text-slate-400 text-sm md:text-sm leading-relaxed">
                 {t('Whether it\'s an Uber ride home or walking from the subway, your roommates will know exactly when to open the door.', 'উবার দিয়ে বাড়ি ফেরা হোক বা একা হাঁটা, আপনার পরিজনরা সর্বদা জানবেন আপনি কখন পৌঁছাবেন।')}
               </p>
            </div>
            <div className="p-6 md:p-8 rounded-3xl bg-slate-900 border border-slate-800 relative">
               <div className="w-10 h-10 md:w-12 md:h-12 rounded-full bg-pink-500/20 text-pink-400 flex items-center justify-center mb-4 md:mb-6"><span className="material-icons text-xl md:text-base">family_restroom</span></div>
               <h4 className="text-lg md:text-xl font-bold text-white mb-2">{t('Parents & Kids', 'বাবা-মা ও সন্তান')}</h4>
               <p className="text-slate-400 text-sm md:text-sm leading-relaxed">
                 {t('Get automated push notifications the second your child\'s school bus enters the home geofence zone.', 'সন্তানের স্কুল বাস বাড়ির কাছাকাছি পৌঁছালেই স্বয়ংক্রিয় নোটিফিকেশন পান।')}
               </p>
            </div>
            <div className="p-6 md:p-8 rounded-3xl bg-slate-900 border border-slate-800 relative">
               <div className="w-10 h-10 md:w-12 md:h-12 rounded-full bg-orange-500/20 text-orange-400 flex items-center justify-center mb-4 md:mb-6"><span className="material-icons text-xl md:text-base">hiking</span></div>
               <h4 className="text-lg md:text-xl font-bold text-white mb-2">{t('Solo Travelers & Hikers', 'একাকী ট্রাভেলার')}</h4>
               <p className="text-slate-400 text-sm md:text-sm leading-relaxed">
                 {t('Exploring a new city alone? Keep your offline GPS tracks logging so you\'re never truly off the grid.', 'নতুন শহরে একা ঘুরছেন? অফলাইন জিপিএস ট্র্যাকিংয়ের মাধ্যমে নিজেকে নিরাপদ রাখুন।')}
               </p>
            </div>
          </div>
        </div>
      </section>

      {/* ---------------- TESTIMONIALS ---------------- */}
      <section id="reviews" className="py-14 md:py-20 px-4 sm:px-6 md:px-10 lg:px-14 xl:px-16 relative z-10 w-full">
        <div className="w-full max-w-[1760px] mx-auto">
          <div className="text-center mb-8 md:mb-12">
            <h2 className="text-3xl md:text-4xl font-bold mb-3 md:mb-4">{t('Loved by', 'হাজারো মানুষের')} <span className="text-gold-400 text-[#f59e0b]">{t('thousands.', 'ভালোবাসা।')}</span></h2>
            <div className="flex items-center justify-center space-x-1 text-[#f59e0b] mb-3">
              <span className="material-icons text-lg md:text-base">star</span><span className="material-icons text-lg md:text-base">star</span><span className="material-icons text-lg md:text-base">star</span><span className="material-icons text-lg md:text-base">star</span><span className="material-icons text-lg md:text-base">star</span>
            </div>
            <p className="text-slate-400 text-sm md:text-lg">{t('4.9/5 Average Rating on the App Store.', 'অ্যাপ স্টোরে ৪.৯/৫ এভারেজ রেটিং।')}</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Review 1 */}
            <div className="bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800">
              <p className="text-slate-300 italic mb-6 text-sm md:text-base">
                "{t('I work night shifts at the hospital. Sharing my live route with my husband every morning gives us both incredible peace of mind. The battery alert feature is a lifesaver.', 'আমি হাসপাতালে নাইট শিফটে কাজ করি। প্রতিদিন সকালে ফেরার পথে স্বামীর সাথে লাইভ লোকেশন শেয়ার করি, যা আমাদের নিশ্চিন্ত রাখে। ব্যাটারি অ্যালার্ট ফিচারটি অসাধারণ।')}"
              </p>
              <div className="flex items-center space-x-3 md:space-x-4">
                <div className="w-10 h-10 md:w-12 md:h-12 rounded-full bg-slate-800 overflow-hidden"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Sarah&backgroundColor=b6e3f4" alt="Avatar"/></div>
                <div>
                  <h5 className="font-bold text-white text-xs md:text-sm">Sarah Jenkins</h5>
                  <p className="text-[10px] md:text-xs text-slate-500">{t('Registered Nurse', 'রেজিস্টার্ড নার্স')}</p>
                </div>
              </div>
            </div>
            {/* Review 2 */}
            <div className="bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800">
              <p className="text-slate-300 italic mb-6 text-sm md:text-base">
                "{t('The geofence feature is magical. My phone buzzes \'Alex arrived at School\' without him having to text me. Best safety app on the market right now.', 'জিওফেন্সিং ফিচারটি জাদুর মতো কাজ করে! আমার ছেলে স্কুলে পৌঁছানো মাত্রই আমার ফোনে নোটিফিকেশন চলে আসে। এটি বাজারের সেরা সেফটি অ্যাপ।')}"
              </p>
              <div className="flex items-center space-x-3 md:space-x-4">
                <div className="w-10 h-10 md:w-12 md:h-12 rounded-full bg-slate-800 overflow-hidden"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Michael&backgroundColor=c0aede" alt="Avatar"/></div>
                <div>
                  <h5 className="font-bold text-white text-xs md:text-sm">Michael T.</h5>
                  <p className="text-[10px] md:text-xs text-slate-500">{t('Parent of two', 'দুই সন্তানের বাবা')}</p>
                </div>
              </div>
            </div>
            {/* Review 3 */}
            <div className="bg-slate-900/50 p-6 md:p-8 rounded-3xl border border-slate-800">
              <p className="text-slate-300 italic mb-6 text-sm md:text-base">
                "{t('I travel solo a lot. Having an app that automatically sends my last location to my friends if my signal drops makes me feel so much safer abroad.', 'আমি প্রচুর একা ট্রাভেল করি। সিগন্যাল চলে গেলে বন্ধুদের কাছে অটোমেটিক লোকেশন চলে যাওয়ার এই ফিচারটি আমাকে অনেক বেশি নিরাপদ বোধ করায়।')}"
              </p>
              <div className="flex items-center space-x-3 md:space-x-4">
                <div className="w-10 h-10 md:w-12 md:h-12 rounded-full bg-slate-800 overflow-hidden"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Elena&backgroundColor=ffdfbf" alt="Avatar"/></div>
                <div>
                  <h5 className="font-bold text-white text-xs md:text-sm">Elena Rostova</h5>
                  <p className="text-[10px] md:text-xs text-slate-500">{t('Travel Blogger', 'ট্রাভেল ব্লগার')}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ---------------- FAQ ---------------- */}
      <section id="faq" className="py-14 md:py-20 bg-slate-900/30 border-t border-slate-800 relative z-10 w-full">
        <div className="w-full max-w-5xl mx-auto px-4 sm:px-6 md:px-10">
          <h2 className="text-2xl sm:text-3xl md:text-4xl font-bold mb-8 md:mb-12 text-center text-white">{t('Frequently Asked Questions', 'সাধারণ জিজ্ঞাসা')}</h2>
          <div className="space-y-4 md:space-y-6">
            <div className="bg-slate-900 border border-slate-800 p-5 md:p-6 rounded-2xl">
              <h4 className="text-base md:text-lg font-bold text-white mb-2">{t('Does the person watching me need to download the app?', 'যিনি লোকেশন দেখবেন তার কি অ্যাপ লাগবে?')}</h4>
              <p className="text-slate-400 text-xs md:text-sm leading-relaxed">{t('No! When you share your trip, it generates a secure, private web link. Your trusted contacts can monitor your journey from any web browser on any device.', 'না! আপনি আপনার ট্রিপ শেয়ার করলে একটি ওয়েব লিংক জেনারেট হবে। আপনার অভিভাবকরা যেকোনো ডিভাইসের ব্রাউজার থেকে এটি দেখতে পারবেন।')}</p>
            </div>
            <div className="bg-slate-900 border border-slate-800 p-5 md:p-6 rounded-2xl">
              <h4 className="text-base md:text-lg font-bold text-white mb-2">{t('Will this drain my battery?', 'এটি কি প্রচুর ব্যাটারি খরচ করবে?')}</h4>
              <p className="text-slate-400 text-xs md:text-sm leading-relaxed">{t('SafeTravel is designed to be gentle on your battery. It updates your GPS location intelligently during active trips and conserves power whenever your device is stationary.', 'সেফট্রেভেল অত্যন্ত কম ব্যাটারি খরচে কাজ করার জন্য অপ্টিমাইজ করা। ভ্রমণের সময় এটি স্মার্টভাবে লোকেশন আপডেট করে এবং বিশ্রামকালীন সময়ে ব্যাটারি সেভ করে।')}</p>
            </div>
            <div className="bg-slate-900 border border-slate-800 p-5 md:p-6 rounded-2xl">
              <h4 className="text-base md:text-lg font-bold text-white mb-2">{t('Is my location data secure?', 'আমার ডেটা কি নিরাপদ?')}</h4>
              <p className="text-slate-400 text-xs md:text-sm leading-relaxed">{t('Yes. Your location is end-to-end encrypted and only accessible by people you explicitly share your link with. We automatically delete trip history after 30 days unless you choose to save it.', 'হ্যাঁ, আপনার ডেটা এন্ড-টু-এন্ড এনক্রিপ্টেড। আপনি যাকে লিংক দিবেন শুধুমাত্র তিনিই দেখতে পারবেন। ৩০ দিন পর অটোমেটিক্যালি সব হিস্ট্রি ডিলিট হয়ে যায়।')}</p>
            </div>
          </div>
        </div>
      </section>

      {/* ---------------- CTA FOOTER ---------------- */}
      <footer className="pt-16 pb-10 md:pt-24 md:pb-16 px-4 sm:px-6 md:px-10 lg:px-14 xl:px-16 flex flex-col items-center justify-center text-center relative overflow-hidden w-full">
        {/* Vibrant gradient background */}
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-900/40 via-slate-950 to-emerald-900/30 z-0"></div>
        <div className="absolute bottom-0 w-full h-[300px] md:h-[500px] bg-cyan-600/10 blur-[100px] md:blur-[150px] pointer-events-none z-0"></div>
        
        <div className="relative z-10 max-w-5xl w-full">
          <h2 className="text-3xl sm:text-5xl md:text-7xl font-black mb-6 md:mb-8 text-white tracking-tight">{t('Your safety is in', 'আপনার নিরাপত্তা')} <br/> {t('your hands.', 'আপনার হাতেই।')}</h2>
          <p className="text-slate-300 text-sm sm:text-base md:text-xl mb-10 md:mb-12 max-w-2xl mx-auto font-light px-2">
            {t('Don\'t leave your personal security to chance. Download SafeTravel today and experience ultimate peace of mind.', 'নিরাপত্তার বিষয়ে কোনো ছাড় নয়। আজই ডাউনলোড করুন সেফট্রেভেল এবং পরিবারকে রাখুন দুশ্চিন্তামুক্ত।')}
          </p>
          
          <div className="flex flex-col sm:flex-row items-center justify-center space-y-4 sm:space-y-0 sm:space-x-6 mb-16 md:mb-24 w-full px-4 sm:px-0">
            <button className="w-full sm:w-auto px-8 py-4 md:px-10 md:py-5 rounded-2xl bg-white hover:bg-slate-200 text-slate-950 text-base md:text-lg font-bold shadow-[0_0_20px_rgba(255,255,255,0.3)] md:hover:shadow-[0_0_40px_rgba(255,255,255,0.5)] transition-all flex items-center justify-center space-x-3 group hover:-translate-y-1 transform duration-300">
              <span className="material-icons text-2xl md:text-3xl">apple</span>
              <div className="flex flex-col items-start leading-none text-left">
                 <span className="text-[9px] md:text-[10px] font-semibold text-slate-600 uppercase tracking-wider">{t('Download on the', 'ডাউনলোড করুন')}</span>
                 <span className="text-lg md:text-xl tracking-tight">App Store</span>
              </div>
            </button>
            <button className="w-full sm:w-auto px-8 py-4 md:px-10 md:py-5 rounded-2xl bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 text-base md:text-lg font-bold shadow-[0_0_20px_rgba(16,185,129,0.4)] md:hover:shadow-[0_0_40px_rgba(16,185,129,0.6)] transition-all flex items-center justify-center space-x-3 group hover:-translate-y-1 transform duration-300">
              <span className="material-icons text-2xl md:text-3xl">android</span>
              <div className="flex flex-col items-start leading-none text-left">
                 <span className="text-[9px] md:text-[10px] font-semibold text-slate-800 uppercase tracking-wider">{t('GET IT ON', 'ডাউনলোড করুন')}</span>
                 <span className="text-lg md:text-xl tracking-tight">Google Play</span>
              </div>
            </button>
          </div>
        </div>

        {/* Bottom links */}
        <div className="relative z-10 w-full max-w-[1760px] border-t border-white/10 pt-6 md:pt-8 flex flex-col md:flex-row justify-between items-center px-4 sm:px-6 md:px-10 lg:px-14 xl:px-16">
          <div className="flex items-center space-x-2 mb-6 md:mb-0">
             <div className="w-5 h-5 md:w-6 md:h-6 rounded bg-emerald-500 flex items-center justify-center"><span className="material-icons text-white text-[10px] md:text-[12px]">security</span></div>
             <span className="font-bold text-base md:text-lg tracking-tight text-white">Safe<span className="text-emerald-400">Travel</span></span>
          </div>
          
          <div className="flex flex-wrap justify-center gap-4 md:gap-6 text-xs md:text-sm text-slate-400 font-medium mb-6 md:mb-0">
             <Link href="#" className="hover:text-white transition-colors">Privacy</Link>
             <Link href="#" className="hover:text-white transition-colors">Terms</Link>
             <Link href="#" className="hover:text-white transition-colors">Support</Link>
             <Link href="/dashboard" className="text-emerald-400 hover:text-emerald-300 transition-colors md:ml-4 md:border-l border-slate-700 md:pl-4">{t('Admin Dashboard', 'অ্যাডমিন ড্যাশবোর্ড')}</Link>
          </div>
          
          <div className="text-slate-500 text-[10px] md:text-xs font-mono text-center">
            © {new Date().getFullYear()} SafeTravel Inc. <br className="md:hidden" />{t('All rights reserved.', 'সর্বস্বত্ব সংরক্ষিত।')}
          </div>
        </div>
      </footer>

    </div>
  );
}
