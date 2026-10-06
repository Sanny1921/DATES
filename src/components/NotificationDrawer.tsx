import React from 'react';
import { NeverMissItem } from '../types';
import { sound } from '../sound';

interface NotificationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  items: NeverMissItem[];
  onSelectItem: (item: NeverMissItem) => void;
  onCompleteItem: (id: string) => void;
}

export const NotificationDrawer: React.FC<NotificationDrawerProps> = ({
  isOpen,
  onClose,
  items,
  onSelectItem,
  onCompleteItem,
}) => {
  if (!isOpen) return null;

  // Gather upcoming triggers from pending/attention items
  const activeAlerts: {
    item: NeverMissItem;
    alertLabel: string;
    alertNote: string;
    triggerDate: string;
    severity: 'normal' | 'warn' | 'critical';
  }[] = [];

  items
    .filter((i) => i.status !== 'completed')
    .forEach((item) => {
      item.reminders.forEach((r) => {
        activeAlerts.push({
          item,
          alertLabel: r.label,
          alertNote: r.note,
          triggerDate: r.triggerDate,
          severity: r.severity,
        });
      });
    });

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center p-3 sm:p-4 bg-slate-900/40 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[85vh] mt-10">
        {/* Drawer Header */}
        <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-[20px]">notifications_active</span>
            </div>
            <div>
              <h3 className="font-extrabold text-[16px] text-slate-900">
                Trigger Feed & Alerts
              </h3>
              <p className="text-[12px] text-slate-500">
                {activeAlerts.length} scheduled offline alerts
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-slate-200/80 flex items-center justify-center text-slate-500 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        {/* Test Alert Sound CTA */}
        <div className="px-4 py-2.5 bg-orange-50/80 border-b border-orange-100 flex items-center justify-between">
          <span className="text-[12px] font-semibold text-orange-800">
            🔔 Test synthesized alert chime
          </span>
          <button
            type="button"
            onClick={() => sound.playAlert()}
            className="px-2.5 py-1 rounded-lg bg-orange-500 text-white text-[11px] font-bold shadow-xs hover:bg-orange-600 active:scale-95 transition-all cursor-pointer"
          >
            Play Chime
          </button>
        </div>

        {/* Alerts list */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {activeAlerts.length === 0 ? (
            <div className="text-center py-10 text-slate-400">
              <span className="material-symbols-outlined text-[42px] mb-2 opacity-50">
                notifications_off
              </span>
              <p className="text-[14px] font-medium">All clear! No alerts scheduled.</p>
            </div>
          ) : (
            activeAlerts.map((alert, idx) => (
              <div
                key={idx}
                className="p-3.5 rounded-2xl bg-slate-50/80 border border-slate-100 hover:border-orange-200 transition-all flex flex-col gap-2"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span
                      className={`text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-md ${
                        alert.severity === 'critical'
                          ? 'bg-red-100 text-red-700'
                          : alert.severity === 'warn'
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-orange-100 text-orange-700'
                      }`}
                    >
                      {alert.alertLabel}
                    </span>
                    <h4 className="font-bold text-[14px] text-slate-900 mt-1">
                      {alert.item.title}
                    </h4>
                  </div>
                  <span className="text-[11px] font-semibold text-slate-400">
                    {alert.triggerDate}
                  </span>
                </div>
                {alert.alertNote && (
                  <p className="text-[12px] text-slate-600 line-clamp-2">
                    {alert.alertNote}
                  </p>
                )}
                <div className="flex items-center justify-end gap-2 pt-1 border-t border-slate-200/50">
                  <button
                    type="button"
                    onClick={() => {
                      onSelectItem(alert.item);
                      onClose();
                    }}
                    className="text-[12px] font-bold text-orange-600 hover:text-orange-700 px-2 py-1 rounded-md hover:bg-orange-50 cursor-pointer"
                  >
                    View Pipeline
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      onCompleteItem(alert.item.id);
                    }}
                    className="text-[12px] font-bold text-emerald-600 hover:text-emerald-700 px-2 py-1 rounded-md hover:bg-emerald-50 cursor-pointer"
                  >
                    Mark Done
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
