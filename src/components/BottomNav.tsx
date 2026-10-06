import React from 'react';
import { ScreenId } from '../types';

interface BottomNavProps {
  currentScreen: ScreenId;
  onNavigate: (screen: ScreenId) => void;
  onOpenAdd: () => void;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  currentScreen,
  onNavigate,
  onOpenAdd,
}) => {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-100 shadow-[0_-4px_16px_rgba(0,0,0,0.04)]">
      <div className="max-w-md mx-auto h-18 px-6 flex items-center justify-between relative">
        {/* Home */}
        <button
          type="button"
          onClick={() => onNavigate('home')}
          className={`flex flex-col items-center gap-1 transition-all cursor-pointer ${
            currentScreen === 'home'
              ? 'text-orange-500 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-600 font-medium'
          }`}
        >
          <span className="material-symbols-outlined text-[24px]">home</span>
          <span className="text-[11px] tracking-tight">Home</span>
        </button>

        {/* Calendar */}
        <button
          type="button"
          onClick={() => onNavigate('calendar')}
          className={`flex flex-col items-center gap-1 transition-all cursor-pointer ${
            currentScreen === 'calendar'
              ? 'text-orange-500 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-600 font-medium'
          }`}
        >
          <span className="material-symbols-outlined text-[24px]">calendar_month</span>
          <span className="text-[11px] tracking-tight">Calendar</span>
        </button>

        {/* Center Elevated Orange FAB */}
        <div className="relative -top-5 flex justify-center">
          <button
            type="button"
            onClick={onOpenAdd}
            aria-label="Add new date"
            className="w-14 h-14 rounded-full bg-orange-500 hover:bg-orange-600 active:scale-90 text-white flex items-center justify-center shadow-lg shadow-orange-500/40 transition-all cursor-pointer"
          >
            <span className="material-symbols-outlined text-[32px] font-bold">add</span>
          </button>
        </div>

        {/* History */}
        <button
          type="button"
          onClick={() => onNavigate('history')}
          className={`flex flex-col items-center gap-1 transition-all cursor-pointer ${
            currentScreen === 'history'
              ? 'text-orange-500 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-600 font-medium'
          }`}
        >
          <span className="material-symbols-outlined text-[24px]">history</span>
          <span className="text-[11px] tracking-tight">History</span>
        </button>
      </div>
    </nav>
  );
};
