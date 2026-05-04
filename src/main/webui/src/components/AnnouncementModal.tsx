import React from 'react';

interface AnnouncementModalProps {
  message: string;
  onClose: () => void;
}

const AnnouncementModal: React.FC<AnnouncementModalProps> = ({ message, onClose }) => {
  return (
    <div className="modal-overlay">
      <div className="modal" style={{ maxWidth: 520 }} onClick={e => e.stopPropagation()}>
        <h2 className="modal-title">📢 Information</h2>
        <div
          style={{ background: '#e8f4fd', border: '1px solid #90cdf4', color: '#1a365d', borderRadius: 6, padding: '12px 16px', marginBottom: 20, lineHeight: 1.6, fontSize: 14 }}
          dangerouslySetInnerHTML={{ __html: message }}
        />
        <button className="btn btn-primary" onClick={onClose} style={{ width: '100%' }}>
          J'ai compris
        </button>
      </div>
    </div>
  );
};

export default AnnouncementModal;
