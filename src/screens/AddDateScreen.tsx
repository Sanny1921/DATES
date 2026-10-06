import React, { useState } from 'react';
import { CategoryType, NeverMissItem, PriorityType } from '../types';
import { sound } from '../sound';

interface AddDateScreenProps {
  initialItem?: NeverMissItem | null;
  onSave: (item: NeverMissItem, proceedToSchedule?: boolean) => void;
  onCancel: () => void;
}

export const AddDateScreen: React.FC<AddDateScreenProps> = ({
  initialItem,
  onSave,
  onCancel,
}) => {
  const [title, setTitle] = useState(initialItem?.title || 'Electricity Bill');
  const [category, setCategory] = useState<CategoryType>(initialItem?.category || 'Payment');
  const [targetDate, setTargetDate] = useState(initialItem?.targetDate || '2026-10-15');
  const [targetTime, setTargetTime] = useState(initialItem?.targetTime || '23:59');
  const [priority, setPriority] = useState<PriorityType>(initialItem?.priority || 'high');
  const [notes, setNotes] = useState(initialItem?.notes || 'Power & Utility Account #4928-11. Pay before 11:59 PM to avoid disconnection penalty.');

  const categories: CategoryType[] = [
    'Birthday',
    'Anniversary',
    'Health',
    'Work',
    'Payment',
    'Travel',
    'Renewal',
    'Education',
    'Event',
    'Personal',
    'Other',
  ];

  const handleSave = (proceedToSchedule = false) => {
    if (!title.trim()) {
      alert('Please enter an event / task title.');
      return;
    }
    sound.playClick();
    const item: NeverMissItem = {
      id: initialItem?.id || `item-${Date.now()}`,
      title: title.trim(),
      category,
      targetDate,
      targetTime,
      priority,
      notes: notes.trim(),
      status: initialItem?.status || 'pending',
      createdAt: initialItem?.createdAt || new Date().toISOString(),
      reminders: initialItem?.reminders || [
        {
          id: `rem-${Date.now()}-1`,
          daysBefore: 7,
          hoursBefore: 0,
          minutesBefore: 0,
          label: '7 Days Before',
          triggerDate: 'Auto calculated',
          note: 'Advance notification',
          channels: ['notification'],
          severity: 'normal',
        },
        {
          id: `rem-${Date.now()}-2`,
          daysBefore: 1,
          hoursBefore: 0,
          minutesBefore: 0,
          label: '1 Day Before',
          triggerDate: 'Auto calculated',
          note: 'Action day prompt',
          channels: ['notification', 'sms'],
          severity: 'warn',
        },
        {
          id: `rem-${Date.now()}-3`,
          daysBefore: 0,
          hoursBefore: 1,
          minutesBefore: 0,
          label: '! 1 Hour Before (Imminent)',
          triggerDate: 'Auto calculated',
          note: 'Final urgent deadline chime',
          channels: ['loud_alarm', 'notification'],
          severity: 'critical',
        },
      ],
    };

    onSave(item, proceedToSchedule);
  };

  // Convert YYYY-MM-DD to human friendly string for display
  const formatDateDisplay = (isoStr: string) => {
    try {
      const parts = isoStr.split('-');
      if (parts.length === 3) {
        const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
      }
    } catch {}
    return isoStr;
  };

  // Convert HH:mm to 12-hr
  const formatTimeDisplay = (timeStr: string) => {
    try {
      const [h, m] = timeStr.split(':').map(Number);
      const period = h >= 12 ? 'PM' : 'AM';
      const hour12 = h % 12 || 12;
      return `${hour12}:${m < 10 ? '0' + m : m} ${period}`;
    } catch {
      return timeStr;
    }
  };

  return (
    <div className="flex-1 flex flex-col w-full px-4 pt-3 pb-24 gap-4 max-w-md mx-auto animate-in fade-in duration-150">
      {/* Offline-First Engine Subheader & Cancel */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-orange-500"></span>
          <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
            OFFLINE-FIRST ENGINE
          </span>
        </div>
        <button
          type="button"
          onClick={onCancel}
          className="text-[14px] font-bold text-orange-600 hover:text-orange-700 cursor-pointer"
        >
          Cancel
        </button>
      </div>

      {/* Main Form Card matching Image 2 */}
      <section className="bg-white rounded-3xl p-4 sm:p-5 shadow-card border border-slate-100 flex flex-col gap-4">
        {/* EVENT / TASK TITLE */}
        <div>
          <label className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500 block mb-2">
            EVENT / TASK TITLE
          </label>
          <div className="relative flex items-center bg-slate-100/90 rounded-2xl px-4 py-3 border border-transparent focus-within:border-orange-400 focus-within:bg-white transition-all shadow-xs">
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Electricity Bill, Passport Renewal"
              className="w-full bg-transparent font-extrabold text-[19px] sm:text-[20px] text-slate-900 focus:outline-none tracking-tight pr-8"
            />
            <span className="material-symbols-outlined text-[20px] text-slate-700 absolute right-4 pointer-events-none">
              edit
            </span>
          </div>
        </div>

        {/* CATEGORY SELECTOR */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
              CATEGORY
            </label>
            <span className="text-[12px] font-bold text-orange-600">
              Tap to select
            </span>
          </div>

          <div className="flex flex-wrap gap-2">
            {categories.map((cat) => {
              const isSelected = category === cat;
              return (
                <button
                  key={cat}
                  type="button"
                  onClick={() => {
                    sound.playClick();
                    setCategory(cat);
                  }}
                  className={`px-3.5 py-1.5 rounded-full font-bold text-[13px] transition-all cursor-pointer flex items-center gap-1.5 ${
                    isSelected
                      ? 'bg-orange-500 text-white shadow-sm scale-102'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200/80'
                  }`}
                >
                  {isSelected && (
                    <span className="material-symbols-outlined text-[16px] font-extrabold">
                      check
                    </span>
                  )}
                  <span>{cat}</span>
                </button>
              );
            })}
          </div>
        </div>
      </section>

      {/* TARGET DATE & TIME CARD */}
      <section className="bg-white rounded-3xl p-4 sm:p-5 shadow-card border border-slate-100 flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3">
          {/* Target Date */}
          <div>
            <label className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500 block mb-2">
              TARGET DATE
            </label>
            <div className="relative bg-slate-100/90 rounded-2xl p-3 border border-slate-100 flex items-center gap-2.5 shadow-xs cursor-pointer focus-within:ring-2 focus-within:ring-orange-400">
              <span className="material-symbols-outlined text-[20px] text-orange-500">
                calendar_month
              </span>
              <span className="text-[14px] sm:text-[15px] font-extrabold text-slate-900 truncate">
                {formatDateDisplay(targetDate)}
              </span>
              <input
                type="date"
                value={targetDate}
                onChange={(e) => setTargetDate(e.target.value)}
                className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
              />
            </div>
          </div>

          {/* Time */}
          <div>
            <label className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500 block mb-2">
              TIME
            </label>
            <div className="relative bg-slate-100/90 rounded-2xl p-3 border border-slate-100 flex items-center gap-2.5 shadow-xs cursor-pointer focus-within:ring-2 focus-within:ring-orange-400">
              <span className="material-symbols-outlined text-[20px] text-orange-500">
                schedule
              </span>
              <span className="text-[14px] sm:text-[15px] font-extrabold text-slate-900 truncate">
                {formatTimeDisplay(targetTime)}
              </span>
              <input
                type="time"
                value={targetTime}
                onChange={(e) => setTargetTime(e.target.value)}
                className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
              />
            </div>
          </div>
        </div>

        {/* PRIORITY LEVEL */}
        <div>
          <label className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500 block mb-2">
            PRIORITY LEVEL
          </label>
          <div className="grid grid-cols-4 gap-1.5 bg-slate-100/90 p-1.5 rounded-2xl">
            {(['low', 'medium', 'high', 'critical'] as PriorityType[]).map((p) => {
              const isSelected = priority === p;
              return (
                <button
                  key={p}
                  type="button"
                  onClick={() => {
                    sound.playClick();
                    setPriority(p);
                  }}
                  className={`py-1.5 text-center rounded-xl font-bold text-[12px] capitalize transition-all cursor-pointer ${
                    isSelected
                      ? p === 'critical'
                        ? 'bg-rose-500 text-white shadow-xs'
                        : p === 'high'
                        ? 'bg-orange-500 text-white shadow-xs'
                        : p === 'medium'
                        ? 'bg-amber-500 text-white shadow-xs'
                        : 'bg-emerald-500 text-white shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {p}
                </button>
              );
            })}
          </div>
        </div>

        {/* NOTES & DESCRIPTION */}
        <div>
          <label className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500 block mb-2">
            NOTES &amp; DETAILS (OPTIONAL)
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
            placeholder="Account numbers, locations, critical instructions..."
            className="w-full bg-slate-100/90 rounded-2xl p-3 text-[13px] text-slate-800 font-medium border border-transparent focus:border-orange-400 focus:bg-white focus:outline-none transition-all shadow-xs resize-none"
          ></textarea>
        </div>
      </section>

      {/* ACTION CTA BUTTONS */}
      <div className="flex flex-col gap-2.5 pt-1">
        <button
          type="button"
          onClick={() => handleSave(true)}
          className="w-full h-13 rounded-2xl bg-orange-500 hover:bg-orange-600 active:scale-98 text-white font-extrabold text-[15px] shadow-lg shadow-orange-500/30 flex items-center justify-center gap-2 transition-all cursor-pointer"
        >
          <span className="material-symbols-outlined text-[20px]">tune</span>
          <span>Configure Reminder Schedule &rarr;</span>
        </button>

        <button
          type="button"
          onClick={() => handleSave(false)}
          className="w-full h-12 rounded-2xl bg-white hover:bg-slate-50 border border-slate-200 active:scale-98 text-slate-800 font-bold text-[14px] flex items-center justify-center gap-2 transition-all cursor-pointer shadow-xs"
        >
          <span className="material-symbols-outlined text-[19px] text-emerald-600">check_circle</span>
          <span>Save Date Directly</span>
        </button>
      </div>
    </div>
  );
};
