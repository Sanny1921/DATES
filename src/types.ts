export type CategoryType =
  | 'Payment'
  | 'Birthday'
  | 'Anniversary'
  | 'Health'
  | 'Work'
  | 'Travel'
  | 'Renewal'
  | 'Education'
  | 'Event'
  | 'Personal'
  | 'Other';

export type PriorityType = 'low' | 'medium' | 'high' | 'critical';

export interface ReminderAlert {
  id: string;
  daysBefore: number;
  hoursBefore: number;
  minutesBefore: number;
  exactTime?: string; // e.g. "09:00"
  label: string; // e.g. "10 Days Before", "1 Hour Before (Imminent)"
  triggerDate: string; // ISO date string or formatted
  note: string; // description / channel note
  channels: ('notification' | 'sms' | 'email' | 'loud_alarm')[];
  severity: 'normal' | 'warn' | 'critical';
}

export interface NeverMissItem {
  id: string;
  title: string;
  category: CategoryType;
  targetDate: string; // YYYY-MM-DD
  targetTime: string; // HH:mm 24-hr or 12-hr
  priority: PriorityType;
  notes?: string;
  status: 'pending' | 'completed' | 'attention';
  completedAt?: string;
  createdAt: string;
  reminders: ReminderAlert[];
}

export type ScreenId = 'onboarding' | 'home' | 'calendar' | 'add_date' | 'schedule' | 'history';
