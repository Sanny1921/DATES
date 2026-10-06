import React, { useState } from 'react';
import { NeverMissItem, CategoryType } from '../types';
import { getCategoryColor } from '../storage';
import { sound } from '../sound';

interface CalendarScreenProps {
  items: NeverMissItem[];
  onSelectItem: (item: NeverMissItem) => void;
  onOpenSchedule: (item: NeverMissItem) => void;
  onOpenAddDate: (selectedDateStr?: string) => void;
}

export const CalendarScreen: React.FC<CalendarScreenProps> = ({
  items,
  onSelectItem,
  onOpenSchedule,
  onOpenAddDate,
}) => {
  const [currentYear, setCurrentYear] = useState(2026);
  const [currentMonth, setCurrentMonth] = useState(9); // 0-indexed: 9 = October
  const [selectedDay, setSelectedDay] = useState(15);
  const [categoryFilter, setCategoryFilter] = useState<CategoryType | 'ALL'>('ALL');

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
  const firstDayIndex = new Date(currentYear, currentMonth, 1).getDay();

  // Create date lookup map: "YYYY-MM-DD" -> items
  const itemsByDate: Record<string, NeverMissItem[]> = {};
  items.forEach((item) => {
    if (!itemsByDate[item.targetDate]) {
      itemsByDate[item.targetDate] = [];
    }
    itemsByDate[item.targetDate].push(item);
  });

  const selectedDateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(
    selectedDay
  ).padStart(2, '0')}`;

  const selectedDayItems = (itemsByDate[selectedDateStr] || []).filter((item) => {
    if (categoryFilter === 'ALL') return true;
    return item.category === categoryFilter;
  });

  const handlePrevMonth = () => {
    sound.playClick();
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear((y) => y - 1);
    } else {
      setCurrentMonth((m) => m - 1);
    }
  };

  const handleNextMonth = () => {
    sound.playClick();
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear((y) => y + 1);
    } else {
      setCurrentMonth((m) => m + 1);
    }
  };

  return (
    <div className="flex-1 flex flex-col w-full px-4 pt-3 pb-28 gap-4 max-w-md mx-auto animate-in fade-in duration-150">
      {/* Calendar Header */}
      <div className="flex items-center justify-between">
        <div>
          <span className="text-[11px] font-extrabold uppercase tracking-wider text-orange-500">
            OFFLINE AGENDA
          </span>
          <h2 className="text-[22px] font-extrabold text-slate-900 tracking-tight">
            {monthNames[currentMonth]} {currentYear}
          </h2>
        </div>

        <div className="flex items-center gap-1 bg-white border border-slate-200 rounded-2xl p-1 shadow-xs">
          <button
            type="button"
            onClick={handlePrevMonth}
            className="w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-700 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">chevron_left</span>
          </button>
          <button
            type="button"
            onClick={handleNextMonth}
            className="w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-700 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">chevron_right</span>
          </button>
        </div>
      </div>

      {/* Monthly Grid Card */}
      <section className="bg-white rounded-3xl p-4 sm:p-5 shadow-card border border-slate-100">
        {/* Day-of-week headers */}
        <div className="grid grid-cols-7 text-center mb-2">
          {['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'].map((d, i) => (
            <span
              key={i}
              className="text-[12px] font-extrabold text-slate-400 py-1"
            >
              {d}
            </span>
          ))}
        </div>

        {/* Days grid */}
        <div className="grid grid-cols-7 gap-1">
          {Array.from({ length: firstDayIndex }).map((_, i) => (
            <div key={`empty-${i}`} className="h-10"></div>
          ))}

          {Array.from({ length: daysInMonth }).map((_, i) => {
            const dayNum = i + 1;
            const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(
              dayNum
            ).padStart(2, '0')}`;
            const dayItems = itemsByDate[dateStr] || [];
            const isSelected = selectedDay === dayNum;
            const hasEvents = dayItems.length > 0;
            const hasAttention = dayItems.some((it) => it.status === 'attention');

            return (
              <button
                key={dayNum}
                type="button"
                onClick={() => {
                  sound.playClick();
                  setSelectedDay(dayNum);
                }}
                className={`h-11 rounded-2xl flex flex-col items-center justify-center relative transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-orange-500 text-white font-extrabold shadow-sm scale-105'
                    : hasEvents
                    ? 'hover:bg-orange-50/70 text-slate-900 font-bold'
                    : 'text-slate-600 hover:bg-slate-100 font-medium'
                }`}
              >
                <span className="text-[13px]">{dayNum}</span>

                {/* Event Indicator Dots */}
                {hasEvents && (
                  <div className="flex items-center gap-0.5 mt-0.5">
                    {hasAttention ? (
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          isSelected ? 'bg-white' : 'bg-rose-500'
                        }`}
                      ></span>
                    ) : (
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          isSelected ? 'bg-white' : 'bg-orange-500'
                        }`}
                      ></span>
                    )}
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </section>

      {/* Selected Day Agenda Header & Add Action */}
      <div className="flex items-center justify-between pt-1">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-orange-500"></span>
          <h3 className="text-[16px] font-extrabold text-slate-900">
            Agenda for {selectedDay} {monthNames[currentMonth]}
          </h3>
        </div>
        <button
          type="button"
          onClick={() => onOpenAddDate(selectedDateStr)}
          className="text-[12px] font-bold text-orange-600 bg-orange-50 hover:bg-orange-100 px-2.5 py-1 rounded-xl flex items-center gap-1 cursor-pointer transition-all"
        >
          <span className="material-symbols-outlined text-[16px]">add</span>
          <span>Add Date</span>
        </button>
      </div>

      {/* Items list for the selected day */}
      <div className="space-y-3">
        {selectedDayItems.length === 0 ? (
          <div className="bg-white rounded-2xl p-6 text-center border border-slate-100 shadow-soft text-slate-400">
            <span className="material-symbols-outlined text-[36px] text-slate-300 mb-1">
              event_available
            </span>
            <p className="text-[13px] font-medium">No deadlines scheduled on this day.</p>
            <button
              type="button"
              onClick={() => onOpenAddDate(selectedDateStr)}
              className="mt-3 px-4 py-2 rounded-xl bg-orange-50 text-orange-600 hover:bg-orange-100 text-[12px] font-bold inline-flex items-center gap-1.5 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-[16px]">add_circle</span>
              <span>Schedule Event Here</span>
            </button>
          </div>
        ) : (
          selectedDayItems.map((item) => {
            const categoryCfg = getCategoryColor(item.category);
            return (
              <div
                key={item.id}
                onClick={() => onSelectItem(item)}
                className="bg-white rounded-2xl p-4 border border-slate-100 shadow-card hover:border-orange-200 transition-all cursor-pointer flex flex-col gap-2.5"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <span
                      className={`px-2.5 py-0.5 rounded-md text-[10px] font-extrabold uppercase tracking-wider ${categoryCfg.bg} ${categoryCfg.text}`}
                    >
                      {item.category}
                    </span>
                    <span className="text-[12px] font-semibold text-slate-500">
                      {item.targetTime}
                    </span>
                  </div>
                  <span className="w-8 h-8 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center">
                    <span className="material-symbols-outlined text-[18px]">
                      {categoryCfg.icon}
                    </span>
                  </span>
                </div>

                <h4 className="text-[17px] font-extrabold text-slate-900 leading-tight">
                  {item.title}
                </h4>

                <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-[12px]">
                  <span className="text-slate-500 font-medium">
                    {item.reminders.length} reminder alerts active
                  </span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onOpenSchedule(item);
                    }}
                    className="font-bold text-orange-600 hover:text-orange-700 flex items-center gap-1"
                  >
                    <span>View Schedule</span>
                    <span className="material-symbols-outlined text-[15px]">arrow_forward</span>
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
