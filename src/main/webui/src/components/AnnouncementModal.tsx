import React from 'react';

interface AnnouncementBannerProps {
  message: string;
  onClose: () => void;
}

const AnnouncementModal: React.FC<AnnouncementBannerProps> = ({ message, onClose }) => {
  return (
    <div
      style={{
        background: '#e8f4fd',
        border: '1px solid #90cdf4',
        borderRadius: 6,
        padding: '10px 16px',
        display: 'flex',
        alignItems: 'flex-start',
        gap: 10,
        margin: '12px 16px 0',
        position: 'relative',
      }}
    >
      <span style={{ fontSize: 18, lineHeight: 1.4 }}>📢</span>
      <span
        style={{ flex: 1, fontSize: 14, color: '#1a365d', lineHeight: 1.6 }}
        dangerouslySetInnerHTML={{ __html: message }}
      />
      <button
        onClick={onClose}
        style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 16, color: '#4a5568', padding: '0 4px', lineHeight: 1 }}
        title="Fermer"
      >
        ✕
      </button>
    </div>
  );
};

export default AnnouncementModal;
