import React, { useState } from 'react';
import { NeverMissItem } from '../types';
import { sound } from '../sound';

interface ProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  items: NeverMissItem[];
  onResetSeedData: () => void;
  onImportItems: (newItems: NeverMissItem[]) => void;
}

export const ProfileModal: React.FC<ProfileModalProps> = ({
  isOpen,
  onClose,
  items,
  onResetSeedData,
  onImportItems,
}) => {
  const [soundActive, setSoundActive] = useState(sound.isEnabled());
  const [copySuccess, setCopySuccess] = useState(false);

  if (!isOpen) return null;

  const completedCount = items.filter((i) => i.status === 'completed').length;
  const pendingCount = items.filter((i) => i.status !== 'completed').length;
  const totalReminders = items.reduce((acc, cur) => acc + cur.reminders.length, 0);

  const handleExport = () => {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(items, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', `nevermiss-backup-${new Date().toISOString().slice(0, 10)}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const handleFileImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (evt) => {
      try {
        const parsed = JSON.parse(evt.target?.result as string);
        if (Array.isArray(parsed)) {
          onImportItems(parsed);
          alert('Imported successfully!');
          onClose();
        }
      } catch {
        alert('Invalid JSON file format.');
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-100 overflow-hidden flex flex-col">
        {/* Profile Card Header */}
        <div className="p-5 bg-gradient-to-br from-orange-500 to-[#ea580c] text-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-white/20 backdrop-blur-xs flex items-center justify-center text-white font-extrabold text-[22px] border border-white/20">
              NM
            </div>
            <div>
              <h3 className="font-extrabold text-[18px] leading-tight">
                Local Device Storage
              </h3>
              <div className="flex items-center gap-1.5 mt-1 text-[12px] text-orange-100">
                <span className="w-2 h-2 rounded-full bg-emerald-300"></span>
                <span>100% Offline • Zero Telemetry</span>
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-black/10 hover:bg-black/20 flex items-center justify-center text-white cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        {/* Storage Stats */}
        <div className="p-5 space-y-4">
          <div className="grid grid-cols-3 gap-2">
            <div className="p-3 rounded-2xl bg-slate-50 border border-slate-100 text-center">
              <span className="text-[20px] font-extrabold text-slate-900 block">
                {items.length}
              </span>
              <span className="text-[11px] font-semibold text-slate-500">
                Total Events
              </span>
            </div>
            <div className="p-3 rounded-2xl bg-emerald-50/70 border border-emerald-100 text-center">
              <span className="text-[20px] font-extrabold text-emerald-700 block">
                {completedCount}
              </span>
              <span className="text-[11px] font-semibold text-emerald-600">
                Completed
              </span>
            </div>
            <div className="p-3 rounded-2xl bg-orange-50/70 border border-orange-100 text-center">
              <span className="text-[20px] font-extrabold text-orange-700 block">
                {totalReminders}
              </span>
              <span className="text-[11px] font-semibold text-orange-600">
                Alert Triggers
              </span>
            </div>
          </div>

          {/* Sound Synthesizer Toggle */}
          <div className="flex items-center justify-between p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
            <div className="flex items-center gap-2.5">
              <span className="material-symbols-outlined text-[22px] text-orange-500">
                {soundActive ? 'volume_up' : 'volume_off'}
              </span>
              <div>
                <span className="text-[13px] font-bold text-slate-900 block leading-tight">
                  Tactile Audio Feedback
                </span>
                <span className="text-[11px] text-slate-500">
                  Synthesized chimes & alert tones
                </span>
              </div>
            </div>
            <button
              type="button"
              onClick={() => {
                const next = sound.toggleSound();
                setSoundActive(next);
                if (next) sound.playClick();
              }}
              className={`w-12 h-6 rounded-full transition-colors relative flex items-center px-0.5 cursor-pointer ${
                soundActive ? 'bg-orange-500 justify-end' : 'bg-slate-300 justify-start'
              }`}
            >
              <span className="w-5 h-5 rounded-full bg-white shadow-xs"></span>
            </button>
          </div>

          {/* Data Backup & Restore */}
          <div className="space-y-2 pt-2 border-t border-slate-100">
            <h4 className="text-[12px] font-extrabold uppercase tracking-wider text-slate-400">
              Data Management
            </h4>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={handleExport}
                className="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 active:scale-95 transition-all text-slate-800 text-[12px] font-bold flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
              >
                <span className="material-symbols-outlined text-[17px]">download</span>
                <span>{copySuccess ? 'Exported!' : 'Export JSON'}</span>
              </button>

              <label className="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 active:scale-95 transition-all text-slate-800 text-[12px] font-bold flex items-center justify-center gap-1.5 cursor-pointer shadow-xs text-center">
                <span className="material-symbols-outlined text-[17px]">upload</span>
                <span>Import JSON</span>
                <input
                  type="file"
                  accept=".json"
                  onChange={handleFileImport}
                  className="hidden"
                />
              </label>
            </div>

            <button
              type="button"
              onClick={() => {
                if (confirm('Reset to initial sample events?')) {
                  onResetSeedData();
                  onClose();
                }
              }}
              className="w-full mt-2 py-2 rounded-xl text-rose-600 hover:bg-rose-50 active:scale-95 transition-all text-[12px] font-bold flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <span className="material-symbols-outlined text-[16px]">restart_alt</span>
              <span>Reset to Sample Data</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
