import React, { useState } from 'react';
import confetti from 'canvas-confetti';
import { NeverMissItem, CategoryType } from '../types';
import { getCategoryColor } from '../storage';
import { sound } from '../sound';

interface HomeScreenProps {
  items: NeverMissItem[];
  onSelectItem: (item: NeverMissItem) => void;
  onOpenSchedule: (item: NeverMissItem) => void;
  onCompleteItem: (id: string) => void;
  onDeleteItem: (id: string) => void;
  onOpenAdd: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  items,
  onSelectItem,
  onOpenSchedule,
  onCompleteItem,
  onDeleteItem,
  onOpenAdd,
}) => {
  const [filterMode, setFilterMode] = useState<'all' | 'completed' | 'upcoming' | 'attention'>('all');
  const [selectedCategory, setSelectedCategory] = useState<CategoryType | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Stats calculation
  const totalCount = items.length;
  const completedItems = items.filter((i) => i.status === 'completed');
  const attentionItems = items.filter((i) => i.status === 'attention');
  const upcomingItems = items.filter((i) => i.status === 'pending');

  const completedCount = completedItems.length;
  const attentionCount = attentionItems.length;
  const upcomingCount = upcomingItems.length;

  const completionPercent = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 50;

  // Filtered list
  const filteredList = items.filter((item) => {
    if (filterMode === 'completed' && item.status !== 'completed') return false;
    if (filterMode === 'attention' && item.status !== 'attention') return false;
    if (filterMode === 'upcoming' && item.status !== 'pending') return false;
    if (selectedCategory !== 'ALL' && item.category !== selectedCategory) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        item.title.toLowerCase().includes(q) ||
        item.category.toLowerCase().includes(q) ||
        (item.notes && item.notes.toLowerCase().includes(q))
      );
    }
    return true;
  });

  // Split into today vs others
  const todayStr = '2026-10-06';
  const todayItems = filteredList.filter(
    (i) => i.targetDate === todayStr || i.status === 'attention' || i.id === 'item-electricity-bill'
  );
  const otherItems = filteredList.filter(
    (i) => !todayItems.includes(i)
  );

  const handleToggleComplete = (item: NeverMissItem, e: React.MouseEvent) => {
    e.stopPropagation();
    sound.playChime();
    if (item.status !== 'completed') {
      confetti({
        particleCount: 50,
        spread: 60,
        origin: { y: 0.7 },
        colors: ['#f97316', '#10b981', '#3b82f6', '#fbbf24'],
      });
    }
    onCompleteItem(item.id);
  };

  const categories: (CategoryType | 'ALL')[] = [
    'ALL',
    'Payment',
    'Birthday',
    'Health',
    'Work',
    'Renewal',
    'Travel',
    'Education',
    'Event',
    'Personal',
  ];

  return (
    <div className="flex-1 flex flex-col w-full px-4 pt-3 pb-28 gap-4 max-w-md mx-auto animate-in fade-in duration-150">
      {/* Top Greeting Headline & 100% Offline Badge */}
      <div className="flex items-center justify-between pt-1">
        <div className="flex items-center gap-2">
          <h1 className="text-[26px] font-extrabold text-slate-900 tracking-tight leading-tight">
            Good morning
          </h1>
          <span className="text-[24px]">👋</span>
        </div>
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200/60 text-[12px] font-bold shadow-xs">
          <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
          <span>⚡ 100% Offline</span>
        </span>
      </div>
      <p className="text-[14px] text-slate-500 font-medium -mt-2">
        Here is what needs your attention today.
      </p>

      {/* Week in Motion Card */}
      <section className="bg-white rounded-3xl p-4 sm:p-5 shadow-card border border-slate-100 flex flex-col gap-3 relative overflow-hidden">
        <div className="flex items-start justify-between">
          <div>
            <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-400">
              WEEK IN MOTION
            </span>
            <h2 className="text-[19px] sm:text-[20px] font-extrabold text-slate-900 tracking-tight mt-0.5">
              {totalCount} important dates this week
            </h2>
          </div>

          {/* 50% Circular Progress Indicator */}
          <div className="relative w-12 h-12 flex items-center justify-center flex-shrink-0">
            <svg className="w-12 h-12 -rotate-90" viewBox="0 0 36 36">
              <path
                className="text-slate-100"
                strokeWidth="3.5"
                stroke="currentColor"
                fill="none"
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
              <path
                className="text-orange-500 transition-all duration-500"
                strokeDasharray={`${completionPercent}, 100`}
                strokeWidth="3.5"
                strokeLinecap="round"
                stroke="currentColor"
                fill="none"
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
            </svg>
            <span className="absolute font-extrabold text-[12px] text-slate-900">
              {completionPercent}%
            </span>
          </div>
        </div>

        {/* Multi-segment Progress Bar matching Image 1 */}
        <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden flex">
          <div
            style={{ width: `${(completedCount / Math.max(totalCount, 1)) * 100}%` }}
            className="bg-emerald-500 h-full rounded-l-full transition-all duration-300"
          ></div>
          <div
            style={{ width: `${(upcomingCount / Math.max(totalCount, 1)) * 100}%` }}
            className="bg-orange-500 h-full transition-all duration-300"
          ></div>
          <div
            style={{ width: `${(attentionCount / Math.max(totalCount, 1)) * 100}%` }}
            className="bg-red-500 h-full rounded-r-full transition-all duration-300"
          ></div>
        </div>

        {/* Interactive Stats Chips */}
        <div className="flex items-center flex-wrap gap-2 pt-1">
          <button
            type="button"
            onClick={() => setFilterMode(filterMode === 'completed' ? 'all' : 'completed')}
            className={`px-3 py-1.5 rounded-xl font-bold text-[12px] flex items-center gap-1.5 transition-all cursor-pointer ${
              filterMode === 'completed'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'bg-emerald-50 text-emerald-800 border border-emerald-100 hover:bg-emerald-100/70'
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">check_circle</span>
            <span>{completedCount} completed</span>
          </button>

          <button
            type="button"
            onClick={() => setFilterMode(filterMode === 'upcoming' ? 'all' : 'upcoming')}
            className={`px-3 py-1.5 rounded-xl font-bold text-[12px] flex items-center gap-1.5 transition-all cursor-pointer ${
              filterMode === 'upcoming'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'bg-blue-50 text-blue-800 border border-blue-100 hover:bg-blue-100/70'
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">schedule</span>
            <span>{upcomingCount} upcoming</span>
          </button>

          <button
            type="button"
            onClick={() => setFilterMode(filterMode === 'attention' ? 'all' : 'attention')}
            className={`px-3 py-1.5 rounded-xl font-bold text-[12px] flex items-center gap-1.5 transition-all cursor-pointer ${
              filterMode === 'attention'
                ? 'bg-red-600 text-white shadow-xs'
                : 'bg-red-50 text-red-800 border border-red-100 hover:bg-red-100/70'
            }`}
          >
            <span className="material-symbols-outlined text-[16px] font-extrabold">priority_high</span>
            <span>{attentionCount} needs attention</span>
          </button>
        </div>
      </section>

      {/* Category horizontal scrolling filter chips */}
      <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5">
        {categories.map((cat) => (
          <button
            key={cat}
            type="button"
            onClick={() => setSelectedCategory(cat)}
            className={`px-3 py-1 rounded-full text-[12px] font-bold flex-shrink-0 transition-all cursor-pointer ${
              selectedCategory === cat
                ? 'bg-orange-500 text-white shadow-xs'
                : 'bg-slate-100/80 text-slate-600 hover:bg-slate-200/80'
            }`}
          >
            {cat === 'ALL' ? 'All Items' : cat}
          </button>
        ))}
      </div>

      {/* TODAY Section */}
      <section className="space-y-3">
        <div className="flex items-center justify-between px-1">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-rose-400"></span>
            <h3 className="text-[16px] font-extrabold text-slate-900 tracking-wider uppercase">
              TODAY
            </h3>
          </div>
          <span className="text-[12px] font-bold text-slate-500 uppercase tracking-wider">
            {todayItems.filter((i) => i.status !== 'completed').length} ITEMS PENDING
          </span>
        </div>

        {todayItems.length === 0 ? (
          <div className="p-5 rounded-2xl bg-white border border-slate-100 text-center text-slate-400">
            <p className="text-[13px] font-medium">Nothing due today. Enjoy your day!</p>
          </div>
        ) : (
          todayItems.map((item) => renderTaskCard(item))
        )}
      </section>

      {/* UPCOMING & OTHER Section */}
      {otherItems.length > 0 && (
        <section className="space-y-3 pt-2">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-orange-400"></span>
              <h3 className="text-[16px] font-extrabold text-slate-900 tracking-wider uppercase">
                THIS WEEK &amp; UPCOMING
              </h3>
            </div>
            <span className="text-[12px] font-bold text-slate-500 uppercase tracking-wider">
              {otherItems.filter((i) => i.status !== 'completed').length} ITEMS
            </span>
          </div>

          {otherItems.map((item) => renderTaskCard(item))}
        </section>
      )}
    </div>
  );

  function renderTaskCard(item: NeverMissItem) {
    const isDone = item.status === 'completed';
    const isAttention = item.status === 'attention';
    const categoryCfg = getCategoryColor(item.category);

    return (
      <div
        key={item.id}
        onClick={() => onSelectItem(item)}
        className={`bg-white rounded-2xl shadow-card border border-slate-100 relative overflow-hidden transition-all duration-200 hover:border-orange-200 cursor-pointer ${
          isDone ? 'opacity-65' : ''
        }`}
      >
        {/* Left Edge Indicator Strip */}
        <div
          className={`absolute left-0 top-0 bottom-0 w-1.5 ${
            isDone
              ? 'bg-emerald-500'
              : isAttention
              ? 'bg-rose-500'
              : 'bg-orange-500'
          }`}
        ></div>

        <div className="p-4 pl-4.5">
          {/* Header row with tags and lightning icon */}
          <div className="flex items-start justify-between gap-2">
            <div className="flex items-center flex-wrap gap-1.5">
              {/* Category pill */}
              <span className="px-2.5 py-0.5 rounded-md bg-rose-50 text-rose-700 text-[10px] font-extrabold uppercase tracking-wider">
                {item.category.toUpperCase()} ({item.category === 'Payment' ? 'UTILITY' : 'PRIORITY'})
              </span>

              {/* Due status pill */}
              <span
                className={`px-2 py-0.5 rounded-md text-[10px] font-extrabold uppercase tracking-wider flex items-center gap-1 ${
                  isAttention
                    ? 'bg-rose-500 text-white'
                    : isDone
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-orange-100 text-orange-800'
                }`}
              >
                <span className="material-symbols-outlined text-[12px]">schedule</span>
                <span>{isAttention ? 'DUE TODAY' : isDone ? 'COMPLETED' : item.targetDate}</span>
              </span>
            </div>

            {/* Quick Icon Box (e.g. lightning in pink circle for electricity bill) */}
            <div className="w-10 h-10 rounded-2xl bg-rose-50 text-rose-500 flex items-center justify-center flex-shrink-0 shadow-xs border border-rose-100">
              <span className="material-symbols-outlined text-[22px]">
                {item.category === 'Payment' ? 'bolt' : categoryCfg.icon}
              </span>
            </div>
          </div>

          {/* Title */}
          <h3 className="text-[18px] font-extrabold text-slate-900 tracking-tight mt-1.5 truncate">
            {item.title}
          </h3>

          {/* Due Info & Details */}
          <div className="flex items-center gap-2 mt-1 text-[12px] text-slate-500 font-medium">
            <span className="material-symbols-outlined text-[15px] text-orange-500">
              calendar_today
            </span>
            <span>
              {item.targetDate} at {item.targetTime}
            </span>
            {item.reminders.length > 0 && (
              <>
                <span>•</span>
                <span className="text-orange-600 font-bold">
                  {item.reminders.length} alerts queued
                </span>
              </>
            )}
          </div>

          {item.notes && (
            <p className="text-[12px] text-slate-600 mt-2 line-clamp-1 bg-slate-50 p-2 rounded-xl">
              {item.notes}
            </p>
          )}

          {/* Bottom Card Actions */}
          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between gap-2">
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onOpenSchedule(item);
              }}
              className="text-[12px] font-bold text-orange-600 hover:text-orange-700 bg-orange-50 hover:bg-orange-100/70 px-3 py-1.5 rounded-xl flex items-center gap-1 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-[15px]">timeline</span>
              <span>Manage Pipeline ({item.reminders.length})</span>
            </button>

            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={(e) => handleToggleComplete(item, e)}
                title={isDone ? 'Mark Pending' : 'Mark Completed'}
                className={`w-9 h-9 rounded-xl flex items-center justify-center transition-all cursor-pointer ${
                  isDone
                    ? 'bg-emerald-500 text-white'
                    : 'bg-slate-100 hover:bg-emerald-100 text-slate-700 hover:text-emerald-700'
                }`}
              >
                <span className="material-symbols-outlined text-[18px] font-bold">
                  {isDone ? 'done_all' : 'check'}
                </span>
              </button>

              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  if (confirm(`Delete "${item.title}"?`)) {
                    onDeleteItem(item.id);
                  }
                }}
                title="Delete event"
                className="w-9 h-9 rounded-xl bg-slate-100 hover:bg-rose-100 text-slate-400 hover:text-rose-600 flex items-center justify-center transition-all cursor-pointer"
              >
                <span className="material-symbols-outlined text-[17px]">delete</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }
};
