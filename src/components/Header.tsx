import React from 'react';

interface HeaderProps {
  title?: string;
  subtitleTag?: string;
  onBack?: () => void;
  showBack?: boolean;
  onOpenNotifications: () => void;
  onOpenProfile: () => void;
  unreadCount?: number;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  subtitleTag,
  onBack,
  showBack,
  onOpenNotifications,
  onOpenProfile,
  unreadCount = 2,
}) => {
  return (
    <header className="sticky top-0 w-full z-40 bg-white/95 backdrop-blur-md border-b border-slate-100 shadow-xs">
      <div className="h-16 px-4 flex items-center justify-between">
        {showBack ? (
          <div className="flex items-center gap-3">
            <button
              aria-label="Go Back"
              type="button"
              onClick={onBack}
              className="w-10 h-10 flex items-center justify-center rounded-full text-slate-800 hover:bg-slate-100 active:scale-95 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-[24px]">arrow_back</span>
            </button>
            <div className="flex flex-col">
              {subtitleTag && (
                <div className="flex items-center gap-1.5">
                  <span className="inline-block w-2 h-2 rounded-full bg-orange-500 animate-pulse"></span>
                  <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider truncate max-w-[150px]">
                    {subtitleTag}
                  </span>
                </div>
              )}
              <h1 className="text-[19px] font-extrabold text-slate-900 tracking-tight leading-tight">
                {title || 'NeverMiss'}
              </h1>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3">
            {/* Orange clock logo */}
            <div className="w-10 h-10 rounded-full bg-orange-500 flex items-center justify-center text-white shadow-md shadow-orange-500/30">
              <span className="material-symbols-outlined text-[24px]">alarm</span>
            </div>
            <div className="flex flex-col">
              <span className="text-[18px] font-extrabold text-slate-900 tracking-tight leading-tight">
                NeverMiss
              </span>
              <div className="flex items-center gap-1.5 mt-0.5">
                <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                <span className="text-[11px] font-semibold text-slate-500">
                  Offline Ready • Local Only
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Right actions: Notification bell & Avatar */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <button
              aria-label="Notifications"
              type="button"
              onClick={onOpenNotifications}
              className="w-10 h-10 flex items-center justify-center rounded-full text-slate-700 hover:bg-slate-100 active:scale-95 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-[24px]">notifications</span>
            </button>
            {unreadCount > 0 && (
              <span className="absolute top-2 right-2 w-2.5 h-2.5 rounded-full bg-orange-500 ring-2 ring-white"></span>
            )}
          </div>
          <button
            aria-label="Profile and Settings"
            type="button"
            onClick={onOpenProfile}
            className="w-9 h-9 rounded-full bg-[#a35114] text-white flex items-center justify-center shadow-sm hover:opacity-90 active:scale-95 transition-all cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">person</span>
          </button>
        </div>
      </div>
    </header>
  );
};
