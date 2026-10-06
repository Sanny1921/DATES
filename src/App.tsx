import { useState, useEffect } from 'react';
import { ScreenId, NeverMissItem } from './types';
import { loadItems, saveItems, hasUserOnboarded, setUserOnboarded, resetToSeedData } from './storage';
import { ScreenSwitcher } from './components/ScreenSwitcher';
import { Header } from './components/Header';
import { BottomNav } from './components/BottomNav';
import { NotificationDrawer } from './components/NotificationDrawer';
import { ProfileModal } from './components/ProfileModal';

import { OnboardingScreen } from './screens/OnboardingScreen';
import { HomeScreen } from './screens/HomeScreen';
import { AddDateScreen } from './screens/AddDateScreen';
import { ReminderScheduleScreen } from './screens/ReminderScheduleScreen';
import { CalendarScreen } from './screens/CalendarScreen';
import { HistoryScreen } from './screens/HistoryScreen';
import { sound } from './sound';

export default function App() {
  const [items, setItems] = useState<NeverMissItem[]>(() => loadItems());
  const [currentScreen, setCurrentScreen] = useState<ScreenId>(() =>
    hasUserOnboarded() ? 'home' : 'onboarding'
  );
  const [activeItemForSchedule, setActiveItemForSchedule] = useState<NeverMissItem | null>(() => {
    const list = loadItems();
    return list.find((i) => i.id === 'item-electricity-bill') || list[0] || null;
  });
  const [editingItem, setEditingItem] = useState<NeverMissItem | null>(null);

  // UI state
  const [isDeviceFrame, setIsDeviceFrame] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);

  // Sync to local storage whenever items change
  useEffect(() => {
    saveItems(items);
  }, [items]);

  const handleCompleteItem = (id: string) => {
    setItems((prev) =>
      prev.map((item) => {
        if (item.id === id) {
          const isDone = item.status === 'completed';
          return {
            ...item,
            status: isDone ? 'pending' : 'completed',
            completedAt: isDone ? undefined : new Date().toISOString(),
          };
        }
        return item;
      })
    );
  };

  const handleDeleteItem = (id: string) => {
    sound.playClick();
    setItems((prev) => prev.filter((i) => i.id !== id));
  };

  const handleSaveItem = (savedItem: NeverMissItem, proceedToSchedule = false) => {
    setItems((prev) => {
      const idx = prev.findIndex((i) => i.id === savedItem.id);
      if (idx >= 0) {
        const copy = [...prev];
        copy[idx] = savedItem;
        return copy;
      }
      return [savedItem, ...prev];
    });

    if (proceedToSchedule) {
      setActiveItemForSchedule(savedItem);
      setCurrentScreen('schedule');
    } else {
      setCurrentScreen('home');
    }
  };

  const handleSaveSchedule = (updatedItem: NeverMissItem) => {
    setItems((prev) =>
      prev.map((item) => (item.id === updatedItem.id ? updatedItem : item))
    );
    setActiveItemForSchedule(updatedItem);
  };

  const handleOpenScheduleForItem = (item: NeverMissItem) => {
    sound.playClick();
    setActiveItemForSchedule(item);
    setCurrentScreen('schedule');
  };

  const handleSelectItemToEdit = (item: NeverMissItem) => {
    sound.playClick();
    setEditingItem(item);
    setCurrentScreen('add_date');
  };

  const handleOpenAddDate = (selectedDateStr?: string) => {
    sound.playClick();
    setEditingItem(
      selectedDateStr
        ? {
            id: `item-${Date.now()}`,
            title: '',
            category: 'Personal',
            targetDate: selectedDateStr,
            targetTime: '12:00',
            priority: 'medium',
            status: 'pending',
            createdAt: new Date().toISOString(),
            reminders: [],
          }
        : null
    );
    setCurrentScreen('add_date');
  };

  const handleRestoreItem = (id: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, status: 'pending', completedAt: undefined } : item
      )
    );
  };

  const handleResetSeedData = () => {
    const fresh = resetToSeedData();
    setItems(fresh);
    const electricity = fresh.find((i) => i.id === 'item-electricity-bill') || fresh[0];
    setActiveItemForSchedule(electricity);
  };

  const handleImportItems = (newItems: NeverMissItem[]) => {
    setItems(newItems);
    saveItems(newItems);
  };

  // Determine header properties
  const isBackHeader = currentScreen === 'schedule' || currentScreen === 'add_date';
  const headerTitle =
    currentScreen === 'schedule'
      ? 'Reminder Schedule'
      : currentScreen === 'add_date'
      ? editingItem?.id ? 'Edit Date' : 'Add Date'
      : undefined;

  const headerSubtitleTag =
    currentScreen === 'schedule'
      ? (activeItemForSchedule?.title || 'Electricity Bill').toUpperCase()
      : undefined;

  const handleBack = () => {
    sound.playClick();
    if (currentScreen === 'schedule') {
      setCurrentScreen('home');
    } else if (currentScreen === 'add_date') {
      setCurrentScreen('home');
    } else {
      setCurrentScreen('home');
    }
  };

  // Render the current screen content
  const renderScreenContent = () => {
    switch (currentScreen) {
      case 'onboarding':
        return (
          <OnboardingScreen
            onComplete={() => {
              setUserOnboarded(true);
              setCurrentScreen('home');
            }}
          />
        );
      case 'home':
        return (
          <HomeScreen
            items={items}
            onSelectItem={handleSelectItemToEdit}
            onOpenSchedule={handleOpenScheduleForItem}
            onCompleteItem={handleCompleteItem}
            onDeleteItem={handleDeleteItem}
            onOpenAdd={() => handleOpenAddDate()}
          />
        );
      case 'add_date':
        return (
          <AddDateScreen
            initialItem={editingItem}
            onSave={handleSaveItem}
            onCancel={() => {
              sound.playClick();
              setCurrentScreen('home');
            }}
          />
        );
      case 'schedule':
        return activeItemForSchedule ? (
          <ReminderScheduleScreen
            item={activeItemForSchedule}
            onSaveSchedule={handleSaveSchedule}
            onBack={handleBack}
          />
        ) : (
          <div className="p-8 text-center text-slate-500">
            No item selected for schedule.
            <button
              onClick={() => setCurrentScreen('home')}
              className="mt-4 px-4 py-2 bg-orange-500 text-white rounded-xl font-bold block mx-auto"
            >
              Go to Home
            </button>
          </div>
        );
      case 'calendar':
        return (
          <CalendarScreen
            items={items}
            onSelectItem={handleSelectItemToEdit}
            onOpenSchedule={handleOpenScheduleForItem}
            onOpenAddDate={handleOpenAddDate}
          />
        );
      case 'history':
        return (
          <HistoryScreen
            items={items}
            onRestoreItem={handleRestoreItem}
            onDeleteItem={handleDeleteItem}
            onSelectItem={handleSelectItemToEdit}
          />
        );
      default:
        return null;
    }
  };

  const showBottomNav =
    currentScreen === 'home' || currentScreen === 'calendar' || currentScreen === 'history';

  return (
    <div className="min-h-screen bg-[#f5f7fb] flex flex-col items-center">
      {/* Prototype Step Navigator & Device Frame Switcher (From Image 2 top bar) */}
      <ScreenSwitcher
        currentScreen={currentScreen}
        onSelectScreen={(screen) => {
          sound.playClick();
          if (screen === 'add_date') {
            setEditingItem(null);
          }
          setCurrentScreen(screen);
        }}
        isDeviceFrame={isDeviceFrame}
        onToggleDeviceFrame={() => setIsDeviceFrame(!isDeviceFrame)}
      />

      {/* Main View Area: Wrapped in Device Shell or Full View */}
      <div
        className={`w-full flex-1 flex flex-col transition-all duration-300 ${
          isDeviceFrame
            ? 'max-w-[430px] my-4 rounded-[42px] border-[10px] border-slate-900 shadow-2xl overflow-hidden bg-white min-h-[880px]'
            : 'max-w-2xl bg-transparent min-h-screen'
        }`}
      >
        {/* App Header (except during full onboarding experience) */}
        {currentScreen !== 'onboarding' && (
          <Header
            title={headerTitle}
            subtitleTag={headerSubtitleTag}
            showBack={isBackHeader}
            onBack={handleBack}
            onOpenNotifications={() => setIsNotificationOpen(true)}
            onOpenProfile={() => setIsProfileOpen(true)}
            unreadCount={items.filter((i) => i.status === 'attention').length}
          />
        )}

        {/* Dynamic Screen Component */}
        <main className="flex-1 flex flex-col relative w-full overflow-y-auto">
          {renderScreenContent()}
        </main>

        {/* Bottom Navigation */}
        {showBottomNav && (
          <BottomNav
            currentScreen={currentScreen}
            onNavigate={(screen) => {
              sound.playClick();
              setCurrentScreen(screen);
            }}
            onOpenAdd={() => handleOpenAddDate()}
          />
        )}
      </div>

      {/* Offline Notifications Drawer Modal */}
      <NotificationDrawer
        isOpen={isNotificationOpen}
        onClose={() => setIsNotificationOpen(false)}
        items={items}
        onSelectItem={(item) => {
          setActiveItemForSchedule(item);
          setCurrentScreen('schedule');
        }}
        onCompleteItem={handleCompleteItem}
      />

      {/* Offline Storage & Profile Modal */}
      <ProfileModal
        isOpen={isProfileOpen}
        onClose={() => setIsProfileOpen(false)}
        items={items}
        onResetSeedData={handleResetSeedData}
        onImportItems={handleImportItems}
      />
    </div>
  );
}
