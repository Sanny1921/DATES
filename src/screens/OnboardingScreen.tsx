import React from 'react';
import { sound } from '../sound';

interface OnboardingScreenProps {
  onComplete: () => void;
}

export const OnboardingScreen: React.FC<OnboardingScreenProps> = ({ onComplete }) => {
  return (
    <div className="flex-1 flex flex-col justify-between p-6 max-w-md mx-auto w-full animate-in fade-in duration-200">
      <div className="flex flex-col items-center text-center pt-6 space-y-4">
        {/* Animated App Icon */}
        <div className="relative">
          <div className="w-24 h-24 rounded-3xl bg-gradient-to-tr from-orange-500 to-amber-500 flex items-center justify-center text-white shadow-xl shadow-orange-500/30">
            <span className="material-symbols-outlined text-[54px]">alarm</span>
          </div>
          <span className="absolute -bottom-2 -right-2 px-2.5 py-1 rounded-full bg-emerald-500 text-white text-[11px] font-extrabold flex items-center gap-1 shadow-md">
            <span>⚡ 100% Offline</span>
          </span>
        </div>

        <div>
          <h1 className="text-[28px] font-extrabold text-slate-900 tracking-tight leading-tight mt-3">
            Welcome to <span className="text-orange-500">NeverMiss</span>
          </h1>
          <p className="text-[14px] text-slate-500 mt-2 font-medium max-w-xs mx-auto">
            Tactile, reliable deadline engine built for critical utility bills, renewals, and personal milestones.
          </p>
        </div>

        {/* Feature Cards */}
        <div className="w-full space-y-3 pt-2 text-left">
          <div className="p-3.5 rounded-2xl bg-white border border-slate-100 shadow-sm flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center flex-shrink-0 mt-0.5">
              <span className="material-symbols-outlined text-[22px]">security</span>
            </div>
            <div>
              <h3 className="font-bold text-[14px] text-slate-900">
                100% Local &amp; Private
              </h3>
              <p className="text-[12px] text-slate-500 mt-0.5 leading-snug">
                Zero remote tracking, zero servers. Your data stays entirely in device storage.
              </p>
            </div>
          </div>

          <div className="p-3.5 rounded-2xl bg-white border border-slate-100 shadow-sm flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center flex-shrink-0 mt-0.5">
              <span className="material-symbols-outlined text-[22px]">route</span>
            </div>
            <div>
              <h3 className="font-bold text-[14px] text-slate-900">
                Granular Alert Pipelines
              </h3>
              <p className="text-[12px] text-slate-500 mt-0.5 leading-snug">
                Chain cascading reminders (10 days, 3 days, 1 hour before) on an interactive chrono rail.
              </p>
            </div>
          </div>

          <div className="p-3.5 rounded-2xl bg-white border border-slate-100 shadow-sm flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0 mt-0.5">
              <span className="material-symbols-outlined text-[22px]">verified</span>
            </div>
            <div>
              <h3 className="font-bold text-[14px] text-slate-900">
                Conflict &amp; Cutoff Guard
              </h3>
              <p className="text-[12px] text-slate-500 mt-0.5 leading-snug">
                Mathematical cutoff validation guarantees every reminder triggers prior to the deadline.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom CTA */}
      <div className="pt-6 pb-2">
        <button
          type="button"
          onClick={() => {
            sound.playChime();
            onComplete();
          }}
          className="w-full h-14 rounded-2xl bg-orange-500 hover:bg-orange-600 active:scale-98 text-white font-extrabold text-[16px] shadow-lg shadow-orange-500/30 flex items-center justify-center gap-2 transition-all cursor-pointer"
        >
          <span>Get Started</span>
          <span className="material-symbols-outlined text-[22px]">arrow_forward</span>
        </button>
        <p className="text-center text-[11px] text-slate-400 font-medium mt-3">
          Instant setup • Ready right now on this device
        </p>
      </div>
    </div>
  );
};
