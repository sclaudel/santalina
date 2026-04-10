import { useState } from 'react';
import { createPortal } from 'react-dom';
import type { DiveSlot } from '../types';
import { DIVER_LEVELS, DP_LEVELS, PREPARED_LEVELS } from '../types';
import { waitingListService } from '../services/waitingListService';
import { authService } from '../services/authService';
import { getErrorMessage } from '../utils/errorUtils';

interface Props {
  slot: DiveSlot;
  onClose: () => void;
  onSuccess: (newEmail?: string) => void;
}

export function SelfRegistrationModal({ slot, onClose, onSuccess }: Props) {
  const storedUser = authService.getStoredUser();
  const isDP = storedUser?.role === 'DIVE_DIRECTOR';
  const availableLevels: readonly string[] = isDP ? DP_LEVELS : DIVER_LEVELS;

  const originalEmail = storedUser?.email ?? '';
  const [email, setEmail]               = useState(originalEmail);
  const [emailConfirm, setEmailConfirm] = useState(originalEmail);
  const [level, setLevel]               = useState('');
  const [preparedLevel, setPreparedLevel] = useState('Aucun');
  const [comment, setComment]           = useState('');
  const [medicalCertDate, setMedicalCertDate] = useState('');
  const [licenseConfirmed, setLicenseConfirmed] = useState(false);
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState<string | null>(null);

  const today = new Date().toISOString().split('T')[0];

  const userName = storedUser ? `${storedUser.firstName} ${storedUser.lastName}` : '';

  const slotDate = new Date(slot.slotDate).toLocaleDateString('fr-FR', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
  });

  const emailChanged = email.trim().toLowerCase() !== originalEmail.toLowerCase();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const trimmedEmail   = email.trim().toLowerCase();
    const trimmedConfirm = emailConfirm.trim().toLowerCase();

    if (trimmedEmail !== trimmedConfirm) {
      setError('Les deux adresses e-mail ne correspondent pas.');
      return;
    }
    if (!level) {
      setError('Veuillez sélectionner votre niveau.');
      return;
    }
    if (!medicalCertDate) {
      setError('La date de début de votre certificat médical est obligatoire.');
      return;
    }
    if (!licenseConfirmed) {
      setError('Vous devez confirmer la validité de votre licence FFESSM.');
      return;
    }

    setLoading(true);
    try {
      if (emailChanged) {
        await authService.updateEmail(trimmedEmail);
      }
      await waitingListService.register(slot.id, {
        firstName: storedUser?.firstName ?? '',
        lastName:  storedUser?.lastName  ?? '',
        email:     trimmedEmail,
        emailConfirm: trimmedConfirm,
        level,
        preparedLevel: preparedLevel === 'Aucun' ? undefined : preparedLevel,
        comment: comment.trim() || undefined,
        medicalCertDate,
        licenseConfirmed,
      });
      onSuccess(emailChanged ? trimmedEmail : undefined);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return createPortal(
    <div className="modal-overlay" style={{ zIndex: 200000 }} onClick={onClose}>
      <div
        className="modal"
        style={{ maxWidth: 480 }}
        onClick={e => e.stopPropagation()}
      >
        <button className="modal-close btn-icon" onClick={onClose} title="Fermer">✕</button>

        <div className="modal-title">S'inscrire sur le créneau</div>

        <p style={{ color: '#6b7280', fontSize: 13, margin: '0 0 14px' }}>
          📅 {slotDate} · {slot.startTime} – {slot.endTime}
          {slot.title && <> — <strong>{slot.title}</strong></>}
        </p>

        <div style={{
          background: '#f1f5f9', borderRadius: 8, padding: '10px 14px',
          marginBottom: 16, fontSize: 14,
        }}>
          <div style={{ fontWeight: 600 }}>{userName}</div>
        </div>

        {error && (
          <div className="alert alert-error" style={{ marginBottom: 12 }}>{error}</div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label>E-mail de contact <span style={{ color: '#ef4444' }}>*</span></label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group" style={{ marginTop: 10 }}>
            <label>Confirmer l'e-mail <span style={{ color: '#ef4444' }}>*</span></label>
            <input
              type="email"
              value={emailConfirm}
              onChange={e => setEmailConfirm(e.target.value)}
              required
              autoComplete="off"
            />
          </div>

          {emailChanged && (
            <p style={{ fontSize: 12, color: '#92400e', background: '#fef3c7', borderRadius: 6, padding: '6px 10px', margin: '6px 0 0' }}>
              ⚠️ Votre profil sera mis à jour avec ce nouvel e-mail.
            </p>
          )}

          <div className="form-row" style={{ marginTop: 12 }}>
            <div className="form-group">
              <label>Votre niveau <span style={{ color: '#ef4444' }}>*</span></label>
              <select value={level} onChange={e => setLevel(e.target.value)} required>
                <option value="">— Sélectionner —</option>
                {availableLevels.map(l => (
                  <option key={l} value={l}>{l}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Niveau en préparation</label>
              <select value={preparedLevel} onChange={e => setPreparedLevel(e.target.value)}>
                {PREPARED_LEVELS.map(l => (
                  <option key={l} value={l}>{l}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-group" style={{ marginTop: 12 }}>
            <label>Message pour le DP (optionnel)</label>
            <textarea
              value={comment}
              onChange={e => setComment(e.target.value)}
              rows={2}
              placeholder="Ce que vous souhaitez travailler ou faire durant la plongée…"
              style={{ resize: 'vertical' }}
            />
          </div>

          <div className="form-group" style={{ marginTop: 12 }}>
            <label>Date de début de mon certificat médical <span style={{ color: '#ef4444' }}>*</span></label>
            <input
              type="date"
              value={medicalCertDate}
              onChange={e => setMedicalCertDate(e.target.value)}
              max={today}
              required
            />
            <p style={{ fontSize: 12, color: '#6b7280', margin: '4px 0 0' }}>
              J'ai un certificat médical en cours de validité. Sa date de début est le {medicalCertDate ? new Date(medicalCertDate).toLocaleDateString('fr-FR') : '[champ date]'}.
            </p>
          </div>

          <div className="form-group" style={{ marginTop: 12 }}>
            <label style={{ display: 'flex', alignItems: 'flex-start', gap: 8, cursor: 'pointer', fontWeight: 'normal' }}>
              <input
                type="checkbox"
                checked={licenseConfirmed}
                onChange={e => setLicenseConfirmed(e.target.checked)}
                style={{ marginTop: 3, flexShrink: 0 }}
              />
              <span>
                Je confirme avoir vérifié sur le site de la FFESSM la validité de ma licence : <strong>OUI</strong>
                <span style={{ color: '#ef4444' }}> *</span>
              </span>
            </label>
          </div>

          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
            <button type="button" className="btn btn-outline" onClick={onClose}>
              Annuler
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Envoi…' : "S'inscrire"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

// ── Confirmation d'annulation d'une inscription validée ─────────────────────

interface CancelConfirmDialogProps {
  onConfirm: () => void;
  onCancel: () => void;
}

export function CancelConfirmDialog({ onConfirm, onCancel }: CancelConfirmDialogProps) {
  return createPortal(
    <div className="modal-overlay" style={{ zIndex: 200000 }} onClick={onCancel}>
      <div
        className="modal"
        style={{ maxWidth: 420 }}
        onClick={e => e.stopPropagation()}
      >
        <div className="modal-title" style={{ color: '#dc2626' }}>⚠️ Confirmer l'annulation</div>
        <p>Vous êtes sur le point d'annuler votre participation à cette sortie.</p>
        <p style={{ color: '#6b7280', fontSize: 13 }}>
          Cette action peut perturber l'organisation de la palanquée.
          Le directeur de plongée en sera informé par e-mail.
        </p>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
          <button className="btn btn-outline" onClick={onCancel}>
            Non, garder ma place
          </button>
          <button
            className="btn btn-primary"
            style={{ background: '#dc2626', borderColor: '#dc2626' }}
            onClick={onConfirm}
          >
            Oui, annuler ma participation
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
