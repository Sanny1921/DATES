import React, { useState } from 'react';
import { NeverMissItem, CategoryType } from '../types';
import { getCategoryColor } from '../storage';
import { sound } from '../sound';

interface HistoryScreenProps {
  items: NeverMissItem[];
  onRestoreItem: (id: string) => void;
  onDeleteItem: (id: string) => void;
  onSelectItem: (item: NeverMissItem) => void;
}

export const HistoryScreen: React.FC<HistoryScreenProps> = ({
  items,
  onRestoreItem,
  onDeleteItem,
  onSelectItem,
}) => {
  const [selectedCat, setSelectedCat] = useState<CategoryType | 'ALL'>('ALL');

  const completedList = items
    .filter((i) => i.status === 'completed')
    .filter((i) => (selectedCat === 'ALL' ? true : i.category === selectedCat));

  const totalCompleted = items.filter((i) => i.status === 'completed').length;

  return (
    <div className="flex-1 flex flex-col w-full px-4 pt-3 pb-28 gap-4 max-w-md mx-auto animate-in fade-in duration-150">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <span className="text-[11px] font-extrabold uppercase tracking-wider text-emerald-600">
            COMPLETION ARCHIVE
          </span>
          <h2 className="text-[22px] font-extrabold text-slate-900 tracking-tight">
            Past Accomplishments
          </h2>
        </div>
        <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-[12px] font-extrabold flex items-center gap-1 shadow-xs">
          <span className="material-symbols-outlined text-[15px]">verified</span>
          <span>{totalCompleted} Done</span>
        </span>
      </div>

      {/* Stats summary card */}
      <section className="bg-white rounded-3xl p-5 shadow-card border border-slate-100 grid grid-cols-3 gap-2 text-center">
        <div className="p-2.5 rounded-2xl bg-emerald-50/70 border border-emerald-100">
          <span className="text-[22px] font-extrabold text-emerald-700 block">
            {totalCompleted}
          </span>
          <span className="text-[11px] font-semibold text-emerald-800">
            Resolved
          </span>
        </div>
        <div className="p-2.5 rounded-2xl bg-orange-50/70 border border-orange-100">
          <span className="text-[22px] font-extrabold text-orange-700 block">
            100%
          </span>
          <span className="text-[11px] font-semibold text-orange-800">
            Local Safe
          </span>
        </div>
        <div className="p-2.5 rounded-2xl bg-blue-50/70 border border-blue-100">
          <span className="text-[22px] font-extrabold text-blue-700 block">
            0
          </span>
          <span className="text-[11px] font-semibold text-blue-800">
            Missed
          </span>
        </div>
      </section>

      {/* Category filter */}
      <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5">
        {(['ALL', 'Payment', 'Work', 'Health', 'Renewal', 'Birthday'] as (CategoryType | 'ALL')[]).map((c) => (
          <button
            key={c}
            type="button"
            onClick={() => {
              sound.playClick();
              setSelectedCat(c);
            }}
            className={`px-3 py-1 rounded-full text-[12px] font-bold flex-shrink-0 transition-all cursor-pointer ${
              selectedCat === c
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {c === 'ALL' ? 'All Archived' : c}
          </button>
        ))}
      </div>

      {/* Completed items list */}
      <div className="space-y-3">
        {completedList.length === 0 ? (
          <div className="bg-white rounded-2xl p-8 text-center border border-slate-100 text-slate-400">
            <span className="material-symbols-outlined text-[40px] text-slate-300 mb-2">
              task_alt
            </span>
            <p className="text-[14px] font-medium">No completed items in this filter.</p>
          </div>
        ) : (
          completedList.map((item) => {
            const categoryCfg = getCategoryColor(item.category);
            return (
              <div
                key={item.id}
                onClick={() => onSelectItem(item)}
                className="bg-white rounded-2xl p-4 border border-slate-100 shadow-soft flex flex-col gap-2 relative overflow-hidden transition-all hover:border-emerald-200 cursor-pointer"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 text-[10px] font-extrabold uppercase">
                      COMPLETED
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 text-[10px] font-extrabold uppercase">
                      {item.category}
                    </span>
                  </div>
                  <span className="text-[12px] font-medium text-slate-400">
                    {item.targetDate}
                  </span>
                </div>

                <h4 className="text-[17px] font-extrabold text-slate-900 line-through opacity-80 leading-tight">
                  {item.title}
                </h4>

                {item.notes && (
                  <p className="text-[12px] text-slate-500 line-clamp-1">
                    {item.notes}
                  </p>
                )}

                <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      sound.playClick();
                      onRestoreItem(item.id);
                    }}
                    className="text-[12px] font-bold text-orange-600 hover:text-orange-700 flex items-center gap-1 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-[16px]">undo</span>
                    <span>Restore to Active</span>
                  </button>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      if (confirm(`Permanently remove "${item.title}"?`)) {
                        onDeleteItem(item.id);
                      }
                    }}
                    className="text-slate-400 hover:text-rose-600 transition-colors p-1 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
