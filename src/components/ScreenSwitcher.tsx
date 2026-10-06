import React from 'react';
import { ScreenId } from '../types';

interface ScreenSwitcherProps {
  currentScreen: ScreenId;
  onSelectScreen: (screen: ScreenId) => void;
  isDeviceFrame: boolean;
  onToggleDeviceFrame: () => void;
}

export const ScreenSwitcher: React.FC<ScreenSwitcherProps> = ({
  currentScreen,
  onSelectScreen,
  isDeviceFrame,
  onToggleDeviceFrame,
}) => {
  const steps: { id: ScreenId; step: string; label: string }[] = [
    { id: 'onboarding', step: '1.', label: 'Onboarding' },
    { id: 'home', step: '2.', label: 'Home' },
    { id: 'calendar', step: '3.', label: 'Calendar' },
    { id: 'add_date', step: '4.', label: 'Add Date' },
    { id: 'history', step: '5.', label: 'History' },
  ];

  return (
    <div className="w-full bg-[#f8fafc] border-b border-slate-200/80 px-2 sm:px-4 py-2 sticky top-0 z-[60] shadow-xs">
      <div className="max-w-4xl mx-auto flex flex-wrap items-center justify-between gap-2">
        {/* Step navigation tabs matching Image 2 */}
        <div className="flex items-center gap-1 bg-slate-200/70 p-1 rounded-xl overflow-x-auto no-scrollbar max-w-full">
          {steps.map((item) => {
            const isActive =
              currentScreen === item.id ||
              (item.id === 'add_date' && currentScreen === 'schedule');
            return (
              <button
                key={item.id}
                onClick={() => onSelectScreen(item.id)}
                type="button"
                className={`flex-shrink-0 px-2.5 sm:px-3 py-1.5 rounded-lg text-center transition-all cursor-pointer ${
                  isActive
                    ? 'bg-white text-orange-600 shadow-sm font-bold'
                    : 'text-slate-600 hover:text-slate-900 font-medium hover:bg-slate-200/60'
                }`}
              >
                <div className="text-[10px] sm:text-[11px] leading-tight text-slate-400 font-semibold">
                  {item.step}
                </div>
                <div className="text-[12px] sm:text-[13px] leading-tight tracking-tight">
                  {item.label}
                </div>
              </button>
            );
          })}
        </div>

        {/* Frame Toggle & Sound info */}
        <div className="flex items-center gap-2">
          <button
            onClick={onToggleDeviceFrame}
            type="button"
            title={isDeviceFrame ? 'Switch to Full Screen View' : 'Switch to Mobile Frame Preview'}
            className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border text-[12px] font-semibold transition-all shadow-xs cursor-pointer ${
              isDeviceFrame
                ? 'bg-orange-50 border-orange-200 text-orange-700'
                : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
            }`}
          >
            <span className="material-symbols-outlined text-[17px]">
              {isDeviceFrame ? 'smartphone' : 'laptop'}
            </span>
            <span className="hidden sm:inline">
              {isDeviceFrame ? 'Phone View' : 'Full Canvas'}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
};
