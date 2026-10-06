import React, { useState, useMemo, useEffect, useRef } from 'react';
import confetti from 'canvas-confetti';
import { NeverMissItem, ReminderAlert } from '../types';
import { getCategoryColor } from '../storage';
import { sound } from '../sound';

interface ReminderScheduleScreenProps {
  item: NeverMissItem;
  onSaveSchedule: (updatedItem: NeverMissItem) => void;
  onBack: () => void;
}

export const ReminderScheduleScreen: React.FC<ReminderScheduleScreenProps> = ({
  item,
  onSaveSchedule,
  onBack,
}) => {
  // Stepper state
  const [daysEnabled, setDaysEnabled] = useState(true);
  const [hoursEnabled, setHoursEnabled] = useState(true);
  const [minutesEnabled, setMinutesEnabled] = useState(true);

  const [days, setDays] = useState(10);
  const [hours, setHours] = useState(6);
  const [minutes, setMinutes] = useState(30);

  // Exact time delivery lock
  const [exactTimeEnabled, setExactTimeEnabled] = useState(false);
  const [exactTimeValue, setExactTimeValue] = useState('09:00');

  // Active view tab in the pills: 'presets' | 'custom' | 'pipeline'
  const [activeTab, setActiveTab] = useState<'presets' | 'custom' | 'pipeline'>('custom');

  // Reminder pipeline list
  const [pipeline, setPipeline] = useState<ReminderAlert[]>(() => {
    if (item.reminders && item.reminders.length > 0) {
      return item.reminders;
    }
    // Default 4 reminders matching mockup
    return [
      {
        id: 'rem-node-1',
        daysBefore: 10,
        hoursBefore: 0,
        minutesBefore: 0,
        exactTime: '10:00',
        label: '10 DAYS BEFORE',
        triggerDate: 'Oct 14, 2025 • 10:00 AM',
        note: 'Early planning brief & logistics ping',
        channels: ['sms', 'notification'],
        severity: 'normal',
      },
      {
        id: 'rem-node-2',
        daysBefore: 5,
        hoursBefore: 0,
        minutesBefore: 0,
        exactTime: '10:00',
        label: '5 DAYS BEFORE',
        triggerDate: 'Oct 19, 2025 • 10:00 AM',
        note: 'Keynote dry-run recap check',
        channels: ['notification'],
        severity: 'normal',
      },
      {
        id: 'rem-node-3',
        daysBefore: 1,
        hoursBefore: 6,
        minutesBefore: 0,
        exactTime: '20:00',
        label: '1 DAY, 6 HOURS BEFORE',
        triggerDate: 'Oct 22, 2025 • 08:00 PM',
        note: 'Urgent VIP briefing & slide freeze reminder',
        channels: ['email', 'notification'],
        severity: 'warn',
      },
      {
        id: 'rem-node-4',
        daysBefore: 0,
        hoursBefore: 1,
        minutesBefore: 0,
        exactTime: '09:00',
        label: '! 1 HOUR BEFORE (IMMINENT)',
        triggerDate: 'Oct 24, 2025 • 09:00 AM',
        note: 'High-priority loud chime & live link',
        channels: ['loud_alarm', 'notification'],
        severity: 'critical',
      },
    ];
  });

  const [customNote, setCustomNote] = useState('');
  const [showSavedToast, setShowSavedToast] = useState(false);
  const pipelineRef = useRef<HTMLDivElement>(null);

  // Compute target Date object
  const targetDateObj = useMemo(() => {
    try {
      const [y, m, d] = item.targetDate.split('-').map(Number);
      const [h, min] = (item.targetTime || '10:00').split(':').map(Number);
      return new Date(y, m - 1, d, h || 0, min || 0, 0);
    } catch {
      return new Date();
    }
  }, [item.targetDate, item.targetTime]);

  // Compute live trigger preview
  const livePreview = useMemo(() => {
    const d = daysEnabled ? Math.max(0, days) : 0;
    const h = hoursEnabled ? Math.max(0, hours) : 0;
    const m = minutesEnabled ? Math.max(0, minutes) : 0;

    const parts: string[] = [];
    if (d > 0) parts.push(`${d} ${d === 1 ? 'day' : 'days'}`);
    if (h > 0) parts.push(`${h} ${h === 1 ? 'hour' : 'hours'}`);
    if (m > 0) parts.push(`${m} ${m === 1 ? 'minute' : 'minutes'}`);
    const offsetLabel = parts.length > 0 ? parts.join(', ') : 'At event start';

    let triggerDate = new Date(targetDateObj.getTime());
    if (exactTimeEnabled) {
      const [clockH, clockM] = exactTimeValue.split(':').map(Number);
      triggerDate.setDate(triggerDate.getDate() - d);
      triggerDate.setHours(clockH, clockM, 0, 0);
    } else {
      const totalMs = (d * 24 * 60 + h * 60 + m) * 60000;
      triggerDate = new Date(targetDateObj.getTime() - totalMs);
    }

    const dateFormatted = triggerDate.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

    const isPrior = triggerDate.getTime() < targetDateObj.getTime();

    return {
      offsetLabel,
      triggerDate,
      dateFormatted,
      isPrior,
    };
  }, [
    daysEnabled,
    hoursEnabled,
    minutesEnabled,
    days,
    hours,
    minutes,
    exactTimeEnabled,
    exactTimeValue,
    targetDateObj,
  ]);

  // Calculate live countdown to event
  const countdownText = useMemo(() => {
    const now = new Date();
    const diffMs = targetDateObj.getTime() - now.getTime();
    if (diffMs <= 0) return 'Deadline passed';
    const totalHours = Math.floor(diffMs / (1000 * 60 * 60));
    const d = Math.floor(totalHours / 24);
    const h = totalHours % 24;
    return `T-${d}d ${h < 10 ? '0' + h : h}h left`;
  }, [targetDateObj]);

  // Validate cutoff rules for all pipeline nodes
  const cutoffValidation = useMemo(() => {
    const conflicts = pipeline.filter((node) => {
      const totalOffsetMs =
        (node.daysBefore * 24 * 60 + node.hoursBefore * 60 + node.minutesBefore) * 60000;
      return totalOffsetMs < 0;
    });

    return {
      isValid: conflicts.length === 0,
      conflictCount: conflicts.length,
      message:
        conflicts.length === 0
          ? `All ${pipeline.length} alerts trigger prior to ${item.targetDate}, ${item.targetTime}`
          : `${conflicts.length} alerts have timing conflicts!`,
    };
  }, [pipeline, item.targetDate, item.targetTime]);

  const toggleUnit = (unit: 'days' | 'hours' | 'minutes') => {
    sound.playClick();
    if (unit === 'days') setDaysEnabled((prev) => !prev);
    if (unit === 'hours') setHoursEnabled((prev) => !prev);
    if (unit === 'minutes') setMinutesEnabled((prev) => !prev);
  };

  const stepVal = (unit: 'days' | 'hours' | 'minutes', delta: number) => {
    sound.playClick();
    if (unit === 'days') setDays((prev) => Math.max(0, Math.min(60, prev + delta)));
    if (unit === 'hours') setHours((prev) => Math.max(0, Math.min(23, prev + delta)));
    if (unit === 'minutes') setMinutes((prev) => Math.max(0, Math.min(55, prev + delta)));
  };

  const applyPreset = (d: number, h: number, m: number) => {
    sound.playClick();
    setDays(d);
    setHours(h);
    setMinutes(m);
    setDaysEnabled(d > 0);
    setHoursEnabled(h > 0);
    setMinutesEnabled(m > 0);
    if (d === 0 && h === 0 && m === 0) {
      setMinutesEnabled(true);
    }
  };

  const handleAppendToPipeline = () => {
    sound.playChime();
    const d = daysEnabled ? Math.max(0, days) : 0;
    const h = hoursEnabled ? Math.max(0, hours) : 0;
    const m = minutesEnabled ? Math.max(0, minutes) : 0;

    const parts: string[] = [];
    if (d > 0) parts.push(`${d}D`);
    if (h > 0) parts.push(`${h}H`);
    if (m > 0) parts.push(`${m}M`);
    const shortLabel = parts.length > 0 ? parts.join(', ') + ' BEFORE' : 'AT EVENT START';

    const severity =
      d === 0 && h <= 2 ? 'critical' : d <= 1 ? 'warn' : 'normal';

    const newNode: ReminderAlert = {
      id: `rem-node-${Date.now()}`,
      daysBefore: d,
      hoursBefore: h,
      minutesBefore: m,
      exactTime: exactTimeEnabled ? exactTimeValue : undefined,
      label: d === 0 && h <= 1 ? `! ${shortLabel} (IMMINENT)` : shortLabel,
      triggerDate: livePreview.dateFormatted,
      note: customNote.trim() || 'Custom scheduled trigger',
      channels: ['notification', ...(severity === 'critical' ? (['loud_alarm'] as const) : [])],
      severity,
    };

    setPipeline((prev) => [...prev, newNode]);
    setCustomNote('');

    // Smooth scroll down to pipeline
    setTimeout(() => {
      if (pipelineRef.current) {
        pipelineRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    }, 100);
  };

  const handleRemoveNode = (id: string) => {
    sound.playClick();
    setPipeline((prev) => prev.filter((p) => p.id !== id));
  };

  const handleClearAll = () => {
    sound.playClick();
    setPipeline([]);
  };

  const handleSaveSchedule = () => {
    sound.playChime();
    confetti({
      particleCount: 40,
      spread: 50,
      origin: { y: 0.8 },
      colors: ['#f97316', '#10b981', '#3b82f6'],
    });
    const updated: NeverMissItem = {
      ...item,
      reminders: pipeline,
    };
    onSaveSchedule(updated);
    setShowSavedToast(true);
    setTimeout(() => {
      setShowSavedToast(false);
      onBack();
    }, 1200);
  };

  const categoryCfg = getCategoryColor(item.category);

  return (
    <div className="flex-1 flex flex-col w-full pb-32 max-w-md mx-auto animate-in fade-in duration-150">
      {/* Scrollable Content Container */}
      <div className="flex flex-col w-full px-4 pt-3 gap-4">
        {/* Signature NeverMiss Status / Sub-Header Badge */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-orange-500"></span>
            <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
              OFFLINE-FIRST ENGINE
            </span>
          </div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200/60 text-[12px] font-bold shadow-xs">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>⚡ 100% Offline</span>
          </span>
        </div>

        {/* Target Event Card (NeverMiss Style) */}
        <section className="bg-white rounded-3xl p-4 sm:p-5 shadow-card border border-slate-100 relative overflow-hidden">
          <div className="flex items-start justify-between gap-3">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className="px-2.5 py-0.5 rounded-md bg-orange-100/70 text-orange-600 text-[11px] font-extrabold uppercase tracking-wider">
                  Target Event
                </span>
                <span className="text-[12px] font-semibold text-slate-400">
                  {item.targetDate}
                </span>
              </div>
              <h2 className="text-[20px] font-extrabold text-slate-900 tracking-tight truncate">
                {item.title}
              </h2>
              <div className="flex items-center gap-1.5 mt-1 text-[13px] text-slate-600 font-medium">
                <span className="material-symbols-outlined text-[17px] text-orange-500">
                  schedule
                </span>
                <span>
                  Due at {item.targetTime} • {countdownText}
                </span>
              </div>
            </div>

            {/* Event Category Icon */}
            <div className="w-12 h-12 rounded-2xl bg-orange-50 text-orange-500 flex items-center justify-center flex-shrink-0 shadow-xs border border-orange-100">
              <span className="material-symbols-outlined text-[26px]">
                {item.category === 'Payment' ? 'payments' : categoryCfg.icon}
              </span>
            </div>
          </div>

          {/* Progress bar matching NeverMiss 50% widget */}
          <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden mt-3.5 flex">
            <div className="bg-orange-500 h-full rounded-full w-[45%]"></div>
          </div>
        </section>

        {/* Navigation Segment Tabs */}
        <section>
          <div className="p-1 rounded-2xl bg-slate-200/70 grid grid-cols-3 gap-1">
            <button
              type="button"
              onClick={() => {
                sound.playClick();
                setActiveTab('presets');
                applyPreset(3, 0, 0);
              }}
              className={`py-2 px-2 text-center rounded-xl font-bold text-[13px] transition-all cursor-pointer ${
                activeTab === 'presets'
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Presets
            </button>
            <button
              type="button"
              onClick={() => {
                sound.playClick();
                setActiveTab('custom');
              }}
              className={`py-2 px-2 text-center rounded-xl font-bold text-[13px] transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                activeTab === 'custom'
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>Custom</span>
              <span className="w-1.5 h-1.5 rounded-full bg-orange-500"></span>
            </button>
            <button
              type="button"
              onClick={() => {
                sound.playClick();
                setActiveTab('pipeline');
                if (pipelineRef.current) {
                  pipelineRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
              }}
              className={`py-2 px-2 text-center rounded-xl font-bold text-[13px] transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                activeTab === 'pipeline'
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>Pipeline</span>
              <span className="px-1.5 py-0.2 rounded-full bg-orange-100 text-orange-600 text-[10px] font-extrabold">
                {pipeline.length}
              </span>
            </button>
          </div>
        </section>

        {/* Quick Horizon Presets Chips (NeverMiss Category Pill Style) */}
        <section className="py-0.5">
          <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
            <span className="text-[12px] font-bold text-slate-400 flex-shrink-0 flex items-center gap-1 pl-0.5">
              <span className="material-symbols-outlined text-[15px] text-orange-500">bolt</span>
              Quick:
            </span>
            {[
              { label: '7d', d: 7, h: 0, m: 0 },
              { label: '3d', d: 3, h: 0, m: 0 },
              { label: '1d', d: 1, h: 0, m: 0 },
              { label: '12h', d: 0, h: 12, m: 0 },
              { label: '6h', d: 0, h: 6, m: 0 },
              { label: '1h', d: 0, h: 1, m: 0 },
              { label: '30m', d: 0, h: 0, m: 30 },
            ].map((preset) => (
              <button
                key={preset.label}
                type="button"
                onClick={() => applyPreset(preset.d, preset.h, preset.m)}
                className="flex-shrink-0 px-3.5 py-1.5 rounded-xl bg-slate-100/90 text-slate-700 font-bold text-[13px] hover:bg-slate-200 active:scale-95 transition-all shadow-xs cursor-pointer"
              >
                {preset.label}
              </button>
            ))}
          </div>
        </section>

        {/* Interactive Custom Duration Builder Card */}
        <section className="bg-white rounded-3xl p-4 sm:p-5 shadow-card border border-slate-100 flex flex-col gap-4">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <span className="text-[11px] font-extrabold uppercase tracking-wider text-orange-500">
                CUSTOM OFFSETS
              </span>
              <h3 className="text-[18px] font-extrabold text-slate-900 tracking-tight leading-tight">
                How early should we alert you?
              </h3>
              <p className="text-[13px] text-slate-500">
                Combine granular units or toggle single offsets
              </p>
            </div>
            <span className="w-10 h-10 rounded-2xl bg-orange-50 border border-orange-100 text-orange-500 flex items-center justify-center flex-shrink-0">
              <span className="material-symbols-outlined text-[20px]">tune</span>
            </span>
          </div>

          {/* Tactile Units Builder */}
          <div className="flex flex-col gap-3">
            {/* DAYS UNIT ROW */}
            <div
              className={`rounded-2xl border p-3 transition-all duration-200 ${
                daysEnabled
                  ? 'bg-slate-50/90 border-slate-200/80 opacity-100'
                  : 'bg-slate-50/40 border-slate-100 opacity-60'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <button
                    aria-label="Toggle Days"
                    type="button"
                    onClick={() => toggleUnit('days')}
                    className={`w-6 h-6 rounded-md flex items-center justify-center transition-all shadow-xs cursor-pointer ${
                      daysEnabled ? 'bg-orange-500 text-white' : 'bg-slate-200 text-slate-400'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px] font-bold">
                      {daysEnabled ? 'check' : 'remove'}
                    </span>
                  </button>
                  <label className="text-[14px] font-bold text-slate-800">Days Before</label>
                </div>
                <span className="text-[12px] font-semibold text-slate-500">
                  {daysEnabled ? 'Included' : 'Excluded'}
                </span>
              </div>

              <div className="mt-2.5 flex items-center gap-2">
                <div className="flex-1 flex items-center justify-between bg-white border border-slate-200 rounded-xl px-2 py-1 shadow-xs">
                  <button
                    type="button"
                    disabled={!daysEnabled}
                    onClick={() => stepVal('days', -1)}
                    className="w-10 h-10 rounded-lg bg-slate-100 hover:bg-slate-200 active:scale-90 transition-all flex items-center justify-center text-slate-700 cursor-pointer disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-[20px]">remove</span>
                  </button>
                  <input
                    type="number"
                    disabled={!daysEnabled}
                    value={days}
                    onChange={(e) => setDays(Math.max(0, parseInt(e.target.value) || 0))}
                    className="w-16 text-center bg-transparent font-extrabold text-[20px] text-slate-900 focus:outline-none"
                  />
                  <button
                    type="button"
                    disabled={!daysEnabled}
                    onClick={() => stepVal('days', 1)}
                    className="w-10 h-10 rounded-lg bg-slate-100 hover:bg-slate-200 active:scale-90 transition-all flex items-center justify-center text-slate-700 cursor-pointer disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-[20px]">add</span>
                  </button>
                </div>
                <div className="h-12 px-3 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-[13px] font-bold text-slate-600 shadow-xs">
                  Days (d)
                </div>
              </div>
            </div>

            {/* HOURS UNIT ROW */}
            <div
              className={`rounded-2xl border p-3 transition-all duration-200 ${
                hoursEnabled
                  ? 'bg-slate-50/90 border-slate-200/80 opacity-100'
                  : 'bg-slate-50/40 border-slate-100 opacity-60'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <button
                    aria-label="Toggle Hours"
                    type="button"
                    onClick={() => toggleUnit('hours')}
                    className={`w-6 h-6 rounded-md flex items-center justify-center transition-all shadow-xs cursor-pointer ${
                      hoursEnabled ? 'bg-orange-500 text-white' : 'bg-slate-200 text-slate-400'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px] font-bold">
                      {hoursEnabled ? 'check' : 'remove'}
                    </span>
                  </button>
                  <label className="text-[14px] font-bold text-slate-800">Hours Before</label>
                </div>
                <span className="text-[12px] font-semibold text-slate-500">
                  {hoursEnabled ? 'Included' : 'Excluded'}
                </span>
              </div>

              <div className="mt-2.5 flex items-center gap-2">
                <div className="flex-1 flex items-center justify-between bg-white border border-slate-200 rounded-xl px-2 py-1 shadow-xs">
                  <button
                    type="button"
                    disabled={!hoursEnabled}
                    onClick={() => stepVal('hours', -1)}
                    className="w-10 h-10 rounded-lg bg-slate-100 hover:bg-slate-200 active:scale-90 transition-all flex items-center justify-center text-slate-700 cursor-pointer disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-[20px]">remove</span>
                  </button>
                  <input
                    type="number"
                    disabled={!hoursEnabled}
                    value={hours}
                    onChange={(e) => setHours(Math.max(0, parseInt(e.target.value) || 0))}
                    className="w-16 text-center bg-transparent font-extrabold text-[20px] text-slate-900 focus:outline-none"
                  />
                  <button
                    type="button"
                    disabled={!hoursEnabled}
                    onClick={() => stepVal('hours', 1)}
                    className="w-10 h-10 rounded-lg bg-slate-100 hover:bg-slate-200 active:scale-90 transition-all flex items-center justify-center text-slate-700 cursor-pointer disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-[20px]">add</span>
                  </button>
                </div>
                <div className="h-12 px-3 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-[13px] font-bold text-slate-600 shadow-xs">
                  Hours (h)
                </div>
              </div>
            </div>

            {/* MINUTES UNIT ROW */}
            <div
              className={`rounded-2xl border p-3 transition-all duration-200 ${
                minutesEnabled
                  ? 'bg-slate-50/90 border-slate-200/80 opacity-100'
                  : 'bg-slate-50/40 border-slate-100 opacity-60'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <button
                    aria-label="Toggle Minutes"
                    type="button"
                    onClick={() => toggleUnit('minutes')}
                    className={`w-6 h-6 rounded-md flex items-center justify-center transition-all shadow-xs cursor-pointer ${
                      minutesEnabled ? 'bg-orange-500 text-white' : 'bg-slate-200 text-slate-400'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px] font-bold">
                      {minutesEnabled ? 'check' : 'remove'}
                    </span>
                  </button>
                  <label className="text-[14px] font-bold text-slate-800">Minutes Before</label>
                </div>
                <span className="text-[12px] font-semibold text-slate-500">
                  {minutesEnabled ? 'Included' : 'Excluded'}
                </span>
              </div>

              <div className="mt-2.5 flex items-center gap-2">
                <div className="flex-1 flex items-center justify-between bg-white border border-slate-200 rounded-xl px-2 py-1 shadow-xs">
                  <button
                    type="button"
                    disabled={!minutesEnabled}
                    onClick={() => stepVal('minutes', -5)}
                    className="w-10 h-10 rounded-lg bg-slate-100 hover:bg-slate-200 active:scale-90 transition-all flex items-center justify-center text-slate-700 cursor-pointer disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-[20px]">remove</span>
                  </button>
                  <input
                    type="number"
                    disabled={!minutesEnabled}
                    value={minutes}
                    onChange={(e) => setMinutes(Math.max(0, parseInt(e.target.value) || 0))}
                    className="w-16 text-center bg-transparent font-extrabold text-[20px] text-slate-900 focus:outline-none"
                  />
                  <button
                    type="button"
                    disabled={!minutesEnabled}
                    onClick={() => stepVal('minutes', 5)}
                    className="w-10 h-10 rounded-lg bg-slate-100 hover:bg-slate-200 active:scale-90 transition-all flex items-center justify-center text-slate-700 cursor-pointer disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-[20px]">add</span>
                  </button>
                </div>
                <div className="h-12 px-3 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-[13px] font-bold text-slate-600 shadow-xs">
                  Minutes (m)
                </div>
              </div>
            </div>
          </div>

          {/* Lock Exact Time of Day Option */}
          <div className="rounded-2xl bg-slate-50/80 border border-slate-100 p-3.5 flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span className="material-symbols-outlined text-orange-500 text-[22px]">
                  alarm
                </span>
                <div>
                  <span className="text-[14px] font-bold text-slate-900 block leading-tight">
                    Lock Exact Time of Day
                  </span>
                  <span className="text-[12px] text-slate-500">
                    Deliver at fixed clock time
                  </span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  sound.playClick();
                  setExactTimeEnabled(!exactTimeEnabled);
                }}
                className={`w-12 h-6 rounded-full transition-colors relative flex items-center px-0.5 cursor-pointer ${
                  exactTimeEnabled ? 'bg-orange-500 justify-end' : 'bg-slate-200 justify-start'
                }`}
              >
                <span className="w-5 h-5 rounded-full bg-white transition-transform shadow-xs"></span>
              </button>
            </div>

            {exactTimeEnabled && (
              <div className="pt-2.5 border-t border-slate-200/70 flex items-center justify-between gap-2">
                <p className="text-[12px] font-medium text-slate-600">
                  Deliver on trigger date at:
                </p>
                <div className="flex items-center gap-1 bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 shadow-xs">
                  <input
                    type="time"
                    value={exactTimeValue}
                    onChange={(e) => setExactTimeValue(e.target.value)}
                    className="bg-transparent text-[13px] font-bold text-slate-800 focus:outline-none cursor-pointer"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Optional Prompt / Action Note for Trigger */}
          <div className="pt-1">
            <input
              type="text"
              value={customNote}
              onChange={(e) => setCustomNote(e.target.value)}
              placeholder="Optional alert label / custom note..."
              className="w-full bg-slate-50 border border-slate-200/80 rounded-xl px-3 py-2 text-[12px] font-medium text-slate-800 focus:bg-white focus:outline-none focus:border-orange-400"
            />
          </div>

          {/* Live Real-Time Trigger Preview Banner (Warm Orange Card) */}
          <div className="rounded-2xl bg-orange-50/90 border border-orange-200/80 p-3.5 flex items-start gap-2.5">
            <span className="material-symbols-outlined text-orange-500 text-[22px] flex-shrink-0 mt-0.5">
              notifications_active
            </span>
            <div className="flex flex-col">
              <span className="text-[11px] font-extrabold uppercase tracking-wider text-orange-600">
                Live Trigger Preview
              </span>
              <p className="text-[13px] font-medium text-slate-800 mt-0.5">
                🔔 {livePreview.offsetLabel} before
                {exactTimeEnabled ? ' (Anchored to exact time)' : ''} • Scheduled for{' '}
                <span className="font-extrabold text-orange-600">
                  {livePreview.dateFormatted}
                </span>
              </p>
            </div>
          </div>

          {/* Append to Pipeline Action Button */}
          <button
            type="button"
            onClick={handleAppendToPipeline}
            className="h-12 w-full rounded-2xl bg-orange-500 hover:bg-orange-600 text-white font-extrabold text-[14px] flex items-center justify-center gap-2 active:scale-98 transition-all shadow-md shadow-orange-500/20 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">add_task</span>
            <span>Append To Reminder Pipeline</span>
          </button>
        </section>

        {/* Offline Validation Status Pill */}
        <section
          className={`bg-white rounded-2xl p-3 border shadow-soft flex items-center justify-between ${
            cutoffValidation.isValid ? 'border-slate-100' : 'border-rose-200 bg-rose-50/30'
          }`}
        >
          <div className="flex items-center gap-2.5 min-w-0">
            <span
              className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 border ${
                cutoffValidation.isValid
                  ? 'bg-emerald-50 text-emerald-600 border-emerald-100'
                  : 'bg-rose-50 text-rose-600 border-rose-100'
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">
                {cutoffValidation.isValid ? 'verified' : 'warning'}
              </span>
            </span>
            <div className="flex flex-col min-w-0">
              <span className="text-[12px] font-extrabold text-slate-900 truncate">
                {cutoffValidation.isValid ? 'Cutoff Rules Validated' : 'Timing Conflict Detected'}
              </span>
              <span className="text-[11px] text-slate-500 truncate">
                {cutoffValidation.message}
              </span>
            </div>
          </div>
          <span
            className={`text-[11px] font-bold px-2 py-0.5 rounded-md flex-shrink-0 border ${
              cutoffValidation.isValid
                ? 'bg-emerald-50 text-emerald-700 border-emerald-100'
                : 'bg-rose-50 text-rose-700 border-rose-200'
            }`}
          >
            {cutoffValidation.isValid ? 'No conflicts' : 'Review needed'}
          </span>
        </section>

        {/* Multi-Reminder "YOUR REMINDER PLAN" Pipeline Section */}
        <section ref={pipelineRef} className="flex flex-col gap-3 pb-6">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-2">
              <span className="text-[14px] font-bold text-orange-500">🔔</span>
              <h3 className="text-[17px] font-extrabold text-slate-900 tracking-tight">
                Your Reminder Plan
              </h3>
              <span className="px-2.5 py-0.5 rounded-full bg-orange-100 text-orange-600 text-[11px] font-extrabold">
                {pipeline.length} Reminders
              </span>
            </div>
            {pipeline.length > 0 && (
              <button
                type="button"
                onClick={handleClearAll}
                className="text-[12px] font-bold text-slate-400 hover:text-rose-500 transition-colors cursor-pointer"
              >
                Clear All
              </button>
            )}
          </div>

          {/* Connected Vertical Chrono Rail */}
          <div className="relative pl-6 flex flex-col gap-3 mt-1">
            {/* Continuous vertical track line */}
            <div className="absolute left-2.5 top-4 bottom-4 w-0.5 bg-slate-200 pointer-events-none"></div>

            {pipeline.length === 0 ? (
              <div className="p-5 rounded-2xl bg-white border border-slate-100 text-center text-slate-400">
                <p className="text-[13px] font-medium">No alerts in pipeline yet.</p>
                <p className="text-[11px] text-slate-400 mt-1">
                  Use the offset builder above to add your first reminder.
                </p>
              </div>
            ) : (
              pipeline.map((node, index) => {
                const isImminent = node.severity === 'critical';
                const isWarn = node.severity === 'warn';
                const dotColor = isImminent
                  ? 'border-red-500 bg-red-500'
                  : isWarn
                  ? 'border-amber-500 bg-amber-500'
                  : index === 0
                  ? 'border-orange-500 bg-orange-500'
                  : 'border-slate-400 bg-slate-400';

                return (
                  <div key={node.id} className="relative flex items-start gap-3 group">
                    {/* Rail Node Dot */}
                    <div
                      className={`absolute -left-6 top-3 w-5 h-5 rounded-full bg-white border-2 flex items-center justify-center shadow-xs z-10 ${
                        isImminent ? 'border-red-500' : isWarn ? 'border-amber-500' : index === 0 ? 'border-orange-500' : 'border-slate-400'
                      }`}
                    >
                      <div
                        className={`w-2 h-2 rounded-full ${
                          isImminent ? 'bg-red-500 animate-pulse' : isWarn ? 'bg-amber-500' : index === 0 ? 'bg-orange-500' : 'bg-slate-400'
                        }`}
                      ></div>
                    </div>

                    {/* Node Card */}
                    <div
                      className={`flex-1 rounded-2xl bg-white border p-3.5 flex flex-col gap-1 shadow-card transition-all ${
                        isImminent
                          ? 'border-l-4 border-l-red-500 border-slate-100'
                          : 'border-slate-100'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span
                          className={`text-[11px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded-md ${
                            isImminent
                              ? 'bg-red-50 text-red-700'
                              : isWarn
                              ? 'bg-amber-50 text-amber-700'
                              : index === 0
                              ? 'bg-orange-50 text-orange-600'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {node.label}
                        </span>

                        <div className="flex items-center gap-1.5 text-slate-400">
                          {node.channels.includes('sms') && (
                            <span className="material-symbols-outlined text-[15px]">sms</span>
                          )}
                          {node.channels.includes('loud_alarm') && (
                            <span className="material-symbols-outlined text-[16px] text-red-500">
                              campaign
                            </span>
                          )}
                          {node.channels.includes('email') && (
                            <span className="material-symbols-outlined text-[15px]">mail</span>
                          )}
                          {node.channels.includes('notification') && (
                            <span className="material-symbols-outlined text-[15px]">
                              notifications
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex items-baseline justify-between mt-1">
                        <span className="text-[14px] font-bold text-slate-900">
                          {node.triggerDate}
                        </span>
                        <button
                          type="button"
                          aria-label="Delete node"
                          onClick={() => handleRemoveNode(node.id)}
                          className="text-slate-400 hover:text-red-500 transition-colors p-1 cursor-pointer"
                        >
                          <span className="material-symbols-outlined text-[17px]">close</span>
                        </button>
                      </div>

                      <span className="text-[12px] text-slate-500">{node.note}</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </section>
      </div>

      {/* Bottom Fixed Action Bar matching Image 3 */}
      <footer className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-100 shadow-[0_-4px_16px_rgba(0,0,0,0.04)]">
        <div className="max-w-md mx-auto h-20 px-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-orange-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-orange-500"></span>
            </span>
            <div className="flex flex-col">
              <span className="text-[13px] font-extrabold text-slate-900">
                Auto-Save Active
              </span>
              <span className="text-[11px] font-semibold text-slate-400">
                Stored safely on device
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={handleSaveSchedule}
            className="h-12 px-6 rounded-2xl bg-orange-500 hover:bg-orange-600 text-white font-extrabold text-[14px] flex items-center justify-center gap-2 active:scale-95 transition-all shadow-lg shadow-orange-500/35 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span>Save Schedule</span>
          </button>
        </div>
      </footer>

      {/* Floating Save Toast */}
      {showSavedToast && (
        <div className="fixed bottom-24 left-1/2 -translate-x-1/2 z-50 bg-slate-900 text-white px-4 py-2.5 rounded-2xl shadow-xl flex items-center gap-2 text-[13px] font-bold animate-in fade-in duration-200">
          <span className="material-symbols-outlined text-[18px] text-emerald-400">
            check_circle
          </span>
          <span>Reminder Schedule Saved Successfully!</span>
        </div>
      )}
    </div>
  );
};
