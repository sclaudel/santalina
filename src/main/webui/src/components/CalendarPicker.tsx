import { useState } from 'react';
import dayjs from 'dayjs';
import 'dayjs/locale/fr';
dayjs.locale('fr');

interface Props {
  selectedDate: string; // YYYY-MM-DD
  onDateChange: (date: string) => void;
}

export function CalendarPicker({ selectedDate, onDateChange }: Props) {
  const [viewMonth, setViewMonth] = useState(dayjs(selectedDate).startOf('month'));

  const startOfMonth = viewMonth.startOf('month');
  const daysInMonth = viewMonth.daysInMonth();
  const startDow = startOfMonth.day(); // 0=dim, 1=lun...
  const offset = startDow === 0 ? 6 : startDow - 1; // Lundi = 0

  const days: (dayjs.Dayjs | null)[] = [
    ...Array(offset).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => startOfMonth.add(i, 'day')),
  ];

  return (
    <div className="calendar-picker">
      <div className="calendar-picker-header">
        <button onClick={() => setViewMonth(m => m.subtract(1, 'month'))}>‹</button>
        <span>{viewMonth.format('MMMM YYYY')}</span>
        <button onClick={() => setViewMonth(m => m.add(1, 'month'))}>›</button>
      </div>
      <div className="calendar-picker-grid">
        {['L', 'M', 'M', 'J', 'V', 'S', 'D'].map((d, i) => (
          <div key={i} className="calendar-picker-dow">{d}</div>
        ))}
        {days.map((day, i) => (
          <div
            key={i}
            className={`calendar-picker-day ${
              !day ? 'empty' :
              day.format('YYYY-MM-DD') === selectedDate ? 'selected' :
              day.format('YYYY-MM-DD') === dayjs().format('YYYY-MM-DD') ? 'today' : ''
            }`}
            onClick={() => day && onDateChange(day.format('YYYY-MM-DD'))}
          >
            {day ? day.date() : ''}
          </div>
        ))}
      </div>
      <button
        className="btn btn-small btn-outline"
        onClick={() => {
          const today = dayjs().format('YYYY-MM-DD');
          onDateChange(today);
          setViewMonth(dayjs().startOf('month'));
        }}
      >
        Aujourd'hui
      </button>
    </div>
  );
}

