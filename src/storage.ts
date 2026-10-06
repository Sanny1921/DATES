import { NeverMissItem, CategoryType } from './types';

export const INITIAL_ITEMS: NeverMissItem[] = [
  {
    id: 'item-electricity-bill',
    title: 'Electricity Bill',
    category: 'Payment',
    targetDate: '2026-10-15',
    targetTime: '23:59',
    priority: 'high',
    notes: 'Power & Utility Account #4928-11. Pay before 11:59 PM to avoid disconnection penalty.',
    status: 'attention',
    createdAt: new Date().toISOString(),
    reminders: [
      {
        id: 'rem-1',
        daysBefore: 10,
        hoursBefore: 0,
        minutesBefore: 0,
        exactTime: '10:00',
        label: '10 Days Before',
        triggerDate: 'Oct 05, 2026 • 10:00 AM',
        note: 'Early planning brief & logistics ping',
        channels: ['notification', 'sms'],
        severity: 'normal',
      },
      {
        id: 'rem-2',
        daysBefore: 5,
        hoursBefore: 0,
        minutesBefore: 0,
        exactTime: '10:00',
        label: '5 Days Before',
        triggerDate: 'Oct 10, 2026 • 10:00 AM',
        note: 'Keynote dry-run recap check',
        channels: ['notification'],
        severity: 'normal',
      },
      {
        id: 'rem-3',
        daysBefore: 1,
        hoursBefore: 6,
        minutesBefore: 0,
        exactTime: '20:00',
        label: '1 Day, 6 Hours Before',
        triggerDate: 'Oct 14, 2026 • 08:00 PM',
        note: 'Urgent VIP briefing & slide freeze reminder',
        channels: ['email', 'notification'],
        severity: 'warn',
      },
      {
        id: 'rem-4',
        daysBefore: 0,
        hoursBefore: 1,
        minutesBefore: 0,
        exactTime: '22:59',
        label: '! 1 Hour Before (Imminent)',
        triggerDate: 'Oct 15, 2026 • 10:59 PM',
        note: 'High-priority loud chime & live link',
        channels: ['loud_alarm', 'notification'],
        severity: 'critical',
      },
    ],
  },
  {
    id: 'item-health-checkup',
    title: 'Annual Health Screening',
    category: 'Health',
    targetDate: '2026-10-06',
    targetTime: '14:30',
    priority: 'medium',
    notes: 'Fasting required 8 hours prior. Bring previous lab results.',
    status: 'pending',
    createdAt: new Date().toISOString(),
    reminders: [
      {
        id: 'rem-hc-1',
        daysBefore: 2,
        hoursBefore: 0,
        minutesBefore: 0,
        label: '2 Days Before',
        triggerDate: 'Oct 04, 2026 • 02:30 PM',
        note: 'Fast from midnight reminder',
        channels: ['notification'],
        severity: 'normal',
      },
      {
        id: 'rem-hc-2',
        daysBefore: 0,
        hoursBefore: 2,
        minutesBefore: 0,
        label: '2 Hours Before',
        triggerDate: 'Oct 06, 2026 • 12:30 PM',
        note: 'Traffic buffer departure alert',
        channels: ['notification'],
        severity: 'warn',
      },
    ],
  },
  {
    id: 'item-passport-renewal',
    title: 'Passport Expiration & Renewal',
    category: 'Renewal',
    targetDate: '2026-11-20',
    targetTime: '17:00',
    priority: 'high',
    notes: 'Book embassy slot. Prepare 2 standard biometric photos.',
    status: 'pending',
    createdAt: new Date().toISOString(),
    reminders: [
      {
        id: 'rem-pr-1',
        daysBefore: 30,
        hoursBefore: 0,
        minutesBefore: 0,
        label: '30 Days Before',
        triggerDate: 'Oct 21, 2026 • 05:00 PM',
        note: 'First appointment reminder window',
        channels: ['notification', 'email'],
        severity: 'normal',
      },
      {
        id: 'rem-pr-2',
        daysBefore: 7,
        hoursBefore: 0,
        minutesBefore: 0,
        label: '7 Days Before',
        triggerDate: 'Nov 13, 2026 • 05:00 PM',
        note: 'Ensure documents printed',
        channels: ['notification'],
        severity: 'warn',
      },
    ],
  },
  {
    id: 'item-moms-birthday',
    title: "Mom's 60th Birthday Celebration",
    category: 'Birthday',
    targetDate: '2026-10-18',
    targetTime: '19:00',
    priority: 'high',
    notes: 'Dinner reservation confirmed at Osteria. Flower delivery arrives at 3 PM.',
    status: 'pending',
    createdAt: new Date().toISOString(),
    reminders: [
      {
        id: 'rem-mb-1',
        daysBefore: 7,
        hoursBefore: 0,
        minutesBefore: 0,
        label: '7 Days Before',
        triggerDate: 'Oct 11, 2026 • 07:00 PM',
        note: 'Order floral arrangement & gift wrapping',
        channels: ['notification'],
        severity: 'normal',
      },
      {
        id: 'rem-mb-2',
        daysBefore: 1,
        hoursBefore: 0,
        minutesBefore: 0,
        label: '1 Day Before',
        triggerDate: 'Oct 17, 2026 • 07:00 PM',
        note: 'Pick up bakery cake box',
        channels: ['notification', 'sms'],
        severity: 'warn',
      },
    ],
  },
  {
    id: 'item-car-insurance',
    title: 'Vehicle Insurance Policy Renewal',
    category: 'Payment',
    targetDate: '2026-10-09',
    targetTime: '18:00',
    priority: 'medium',
    notes: 'Policy #GEI-992384. Auto-debit backup enabled.',
    status: 'pending',
    createdAt: new Date().toISOString(),
    reminders: [
      {
        id: 'rem-ci-1',
        daysBefore: 3,
        hoursBefore: 0,
        minutesBefore: 0,
        label: '3 Days Before',
        triggerDate: 'Oct 06, 2026 • 06:00 PM',
        note: 'Review coverage terms & premium rate',
        channels: ['notification'],
        severity: 'normal',
      },
    ],
  },
  {
    id: 'item-cloud-server',
    title: 'Cloud Production Cluster Renewal',
    category: 'Work',
    targetDate: '2026-10-06',
    targetTime: '10:00',
    priority: 'high',
    notes: 'Verify corporate credit card balance before automatic cycle run.',
    status: 'completed',
    completedAt: '2026-10-06T09:15:00',
    createdAt: new Date().toISOString(),
    reminders: [],
  },
  {
    id: 'item-dental-cleaning',
    title: 'Dental Hygiene & Checkup',
    category: 'Health',
    targetDate: '2026-10-03',
    targetTime: '11:00',
    priority: 'low',
    notes: 'Completed six-month routine cleaning.',
    status: 'completed',
    completedAt: '2026-10-03T11:45:00',
    createdAt: new Date().toISOString(),
    reminders: [],
  },
  {
    id: 'item-project-kickoff',
    title: 'Q4 Product Roadmap Sync',
    category: 'Work',
    targetDate: '2026-10-04',
    targetTime: '15:00',
    priority: 'high',
    notes: 'All stakeholders attended and OKRs approved.',
    status: 'completed',
    completedAt: '2026-10-04T16:00:00',
    createdAt: new Date().toISOString(),
    reminders: [],
  },
  {
    id: 'item-gym-membership',
    title: 'Gym Annual Pass Renewal',
    category: 'Renewal',
    targetDate: '2026-10-01',
    targetTime: '12:00',
    priority: 'low',
    notes: 'Paid via direct debit.',
    status: 'completed',
    completedAt: '2026-10-01T10:00:00',
    createdAt: new Date().toISOString(),
    reminders: [],
  },
  {
    id: 'item-apartment-lease',
    title: 'Apartment Lease Renewal Notice',
    category: 'Personal',
    targetDate: '2026-10-02',
    targetTime: '17:00',
    priority: 'medium',
    notes: 'Sent confirmation letter to landlord.',
    status: 'completed',
    completedAt: '2026-10-02T14:20:00',
    createdAt: new Date().toISOString(),
    reminders: [],
  },
  {
    id: 'item-domain-renewal',
    title: 'Domain Name Hosting Renewal',
    category: 'Work',
    targetDate: '2026-10-05',
    targetTime: '08:00',
    priority: 'medium',
    notes: 'Auto-renewed for 2 years.',
    status: 'completed',
    completedAt: '2026-10-05T08:05:00',
    createdAt: new Date().toISOString(),
    reminders: [],
  },
];

const STORAGE_KEY = 'nevermiss_events_v2';
const ONBOARDED_KEY = 'nevermiss_has_onboarded_v2';

export function loadItems(): NeverMissItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      saveItems(INITIAL_ITEMS);
      return INITIAL_ITEMS;
    }
    return JSON.parse(raw);
  } catch {
    return INITIAL_ITEMS;
  }
}

export function saveItems(items: NeverMissItem[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch (err) {
    console.error('Failed to write to localStorage', err);
  }
}

export function hasUserOnboarded(): boolean {
  try {
    return localStorage.getItem(ONBOARDED_KEY) === 'true';
  } catch {
    return false;
  }
}

export function setUserOnboarded(val: boolean): void {
  try {
    localStorage.setItem(ONBOARDED_KEY, val ? 'true' : 'false');
  } catch {}
}

export function resetToSeedData(): NeverMissItem[] {
  saveItems(INITIAL_ITEMS);
  return INITIAL_ITEMS;
}

export function getCategoryColor(category: CategoryType): {
  bg: string;
  text: string;
  border: string;
  icon: string;
} {
  switch (category) {
    case 'Payment':
      return {
        bg: 'bg-orange-50',
        text: 'text-orange-700',
        border: 'border-orange-200',
        icon: 'payments',
      };
    case 'Birthday':
      return {
        bg: 'bg-pink-50',
        text: 'text-pink-700',
        border: 'border-pink-200',
        icon: 'cake',
      };
    case 'Anniversary':
      return {
        bg: 'bg-rose-50',
        text: 'text-rose-700',
        border: 'border-rose-200',
        icon: 'favorite',
      };
    case 'Health':
      return {
        bg: 'bg-emerald-50',
        text: 'text-emerald-700',
        border: 'border-emerald-200',
        icon: 'health_and_safety',
      };
    case 'Work':
      return {
        bg: 'bg-blue-50',
        text: 'text-blue-700',
        border: 'border-blue-200',
        icon: 'work',
      };
    case 'Travel':
      return {
        bg: 'bg-cyan-50',
        text: 'text-cyan-700',
        border: 'border-cyan-200',
        icon: 'flight',
      };
    case 'Renewal':
      return {
        bg: 'bg-amber-50',
        text: 'text-amber-700',
        border: 'border-amber-200',
        icon: 'autorenew',
      };
    case 'Education':
      return {
        bg: 'bg-indigo-50',
        text: 'text-indigo-700',
        border: 'border-indigo-200',
        icon: 'school',
      };
    case 'Event':
      return {
        bg: 'bg-purple-50',
        text: 'text-purple-700',
        border: 'border-purple-200',
        icon: 'event',
      };
    case 'Personal':
      return {
        bg: 'bg-teal-50',
        text: 'text-teal-700',
        border: 'border-teal-200',
        icon: 'person',
      };
    default:
      return {
        bg: 'bg-slate-100',
        text: 'text-slate-700',
        border: 'border-slate-200',
        icon: 'bookmark',
      };
  }
}
