/** Couleurs par préfixe de type de créneau — partagé entre SlotBlock et MonthView */

interface SlotTypeStyle {
  bg: string;
  color: string;
  border: string;
  tagBg: string;
  tagColor: string;
}

const SLOT_TYPE_STYLES: { match: RegExp; style: SlotTypeStyle }[] = [
  {
    match: /^club/i,
    style: { bg: '#eff6ff', color: '#1e40af', border: '#1e40af', tagBg: '#dbeafe', tagColor: '#1e40af' },
  },
  {
    match: /^codep/i,
    style: { bg: '#f0fdf4', color: '#15803d', border: '#15803d', tagBg: '#dcfce7', tagColor: '#15803d' },
  },
  {
    match: /^externe/i,
    style: { bg: '#fff7ed', color: '#c2410c', border: '#c2410c', tagBg: '#fed7aa', tagColor: '#c2410c' },
  },
];

const DEFAULT_STYLE: SlotTypeStyle = {
  bg: '#f9fafb', color: '#374151', border: '#6b7280', tagBg: '#f3f4f6', tagColor: '#374151',
};

export function getSlotTypeStyle(slotType: string | null | undefined): SlotTypeStyle {
  if (!slotType) return DEFAULT_STYLE;
  return SLOT_TYPE_STYLES.find(s => s.match.test(slotType))?.style ?? DEFAULT_STYLE;
}

