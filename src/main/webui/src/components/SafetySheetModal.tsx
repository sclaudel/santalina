import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import type { SafetySheetFile } from '../types';
import { slotSafetySheetService } from '../services/slotSafetySheetService';
import { useAuth } from '../context/AuthContext';

const MAX_FILES      = 4;
const MAX_SIZE_BYTES = 3 * 1024 * 1024; // 3 Mo
const ALLOWED_TYPES  = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
const ALLOWED_EXT    = ['.jpg', '.jpeg', '.png', '.webp', '.pdf'];

interface Props {
  slotId:    number;
  slotDate:  string;   // YYYY-MM-DD
  isDP:      boolean;  // true si l'utilisateur peut uploader
  onClose:   () => void;
  onUploaded?: () => void;  // appelé après upload réussi
}

function formatSize(bytes: number): string {
  if (bytes < 1024)        return bytes + ' o';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
  return (bytes / (1024 * 1024)).toFixed(2) + ' Mo';
}

export function SafetySheetModal({ slotId, slotDate, isDP, onClose, onUploaded }: Props) {
  const [sheets, setSheets]             = useState<SafetySheetFile[]>([]);
  const [loading, setLoading]           = useState(true);
  const [uploading, setUploading]       = useState(false);
  const [error, setError]               = useState('');
  const [success, setSuccess]           = useState('');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [downloadingZip, setDownloadingZip] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { user } = useAuth();

  useEffect(() => {
    load();
  }, [slotId]);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await slotSafetySheetService.list(slotId);
      setSheets(data);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Impossible de charger les fiches de sécurité.');
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    validateAndSetFiles(files);
    // Réinitialiser l'input pour permettre de re-sélectionner les mêmes fichiers
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const validateAndSetFiles = (newFiles: File[]) => {
    setError('');
    const total = sheets.length + selectedFiles.length + newFiles.length;
    if (total > MAX_FILES) {
      setError(`Maximum ${MAX_FILES} fichiers par créneau (déjà ${sheets.length} déposé(s), ${selectedFiles.length} en attente).`);
      return;
    }
    for (const f of newFiles) {
      if (!ALLOWED_TYPES.includes(f.type)) {
        setError(`Le fichier "${f.name}" n'est pas autorisé. Types acceptés : JPG, PNG, WEBP, PDF.`);
        return;
      }
      if (f.size > MAX_SIZE_BYTES) {
        setError(`Le fichier "${f.name}" dépasse 3 Mo (${formatSize(f.size)}).`);
        return;
      }
    }
    setSelectedFiles(prev => [...prev, ...newFiles]);
  };

  const removeSelected = (idx: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== idx));
    setError('');
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) return;
    setUploading(true);
    setError('');
    setSuccess('');
    try {
      const updated = await slotSafetySheetService.upload(slotId, selectedFiles);
      setSheets(updated);
      setSelectedFiles([]);
      setSuccess(`${selectedFiles.length} fichier(s) déposé(s) avec succès.`);
      onUploaded?.();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Erreur lors de l\'upload.');
    } finally {
      setUploading(false);
    }
  };

  const handleDownloadZip = async () => {
    setDownloadingZip(true);
    try {
      const blob = await slotSafetySheetService.downloadZip(slotId);
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href     = url;
      a.download = `fiches_securite_${slotDate}.zip`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e: any) {
      setError('Impossible de télécharger le ZIP.');
    } finally {
      setDownloadingZip(false);
    }
  };

  const handleDelete = async (fileId: number, name: string) => {
    if (!window.confirm(`Supprimer "${name}" ? Cette action est irréversible.`)) return;
    try {
      await slotSafetySheetService.delete(slotId, fileId);
      setSheets(prev => prev.filter(s => s.id !== fileId));
    } catch (e: any) {
      setError('Impossible de supprimer le fichier.');
    }
  };

  const canUpload = isDP && sheets.length + selectedFiles.length < MAX_FILES;

  return createPortal(
    <div
      className="modal-overlay"
      style={{ zIndex: 200000 }}
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
      role="dialog"
      aria-modal="true"
      aria-label="Fiches de sécurité"
    >
      <div className="modal" style={{ maxWidth: 560, width: '95vw', padding: 24 }}>
        <div className="modal-header">
          <h2 className="modal-title">📋 Fiches de sécurité</h2>
          <button className="modal-close" onClick={onClose} aria-label="Fermer">✕</button>
        </div>

        <div style={{ padding: '0 0 12px' }}>
          <p style={{ color: '#6b7280', fontSize: 13, margin: '0 0 16px' }}>
            Créneau du <strong>{new Date(slotDate + 'T00:00:00').toLocaleDateString('fr-FR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</strong>
          </p>

          {error   && <div className="alert alert-error"   style={{ marginBottom: 12 }}>{error}</div>}
          {success && <div className="alert alert-success" style={{ marginBottom: 12 }}>{success}</div>}

          {/* ── Fichiers déjà déposés ── */}
          <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>
            Fichiers déposés ({sheets.length}/{MAX_FILES})
          </h3>
          {loading ? (
            <p style={{ color: '#9ca3af', fontSize: 13 }}>Chargement…</p>
          ) : sheets.length === 0 ? (
            <p style={{ color: '#9ca3af', fontSize: 13, fontStyle: 'italic' }}>Aucune fiche déposée pour ce créneau.</p>
          ) : (
            <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 16px' }}>
              {sheets.map(sheet => (
                <li key={sheet.id} style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  padding: '6px 10px', background: '#f9fafb',
                  borderRadius: 6, marginBottom: 6,
                  border: '1px solid #e5e7eb',
                }}>
                  <span style={{ flex: 1, fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {sheet.contentType === 'application/pdf' ? '📄' : '🖼️'} {sheet.originalName}
                  </span>
                  <span style={{ fontSize: 11, color: '#9ca3af', whiteSpace: 'nowrap' }}>
                    {formatSize(sheet.fileSize)}
                  </span>
                  {user?.role === 'ADMIN' && (
                    <button
                      onClick={() => handleDelete(sheet.id, sheet.originalName)}
                      className="btn-icon btn-delete"
                      title="Supprimer"
                      style={{ flexShrink: 0 }}
                    >
                      🗑️
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}

          {/* ── Téléchargement ZIP ── */}
          {sheets.length > 0 && (
            <button
              className="btn btn-outline btn-small"
              onClick={handleDownloadZip}
              disabled={downloadingZip}
              style={{ marginBottom: 20 }}
            >
              {downloadingZip ? '…' : '⬇️ Télécharger tout (.zip)'}
            </button>
          )}

          {/* ── Zone d'upload (DP uniquement) ── */}
          {isDP && (
            <>
              <hr style={{ border: '1px solid #e5e7eb', marginBottom: 16 }} />
              <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>
                Ajouter des fichiers
              </h3>
              <p style={{ fontSize: 12, color: '#6b7280', marginBottom: 10 }}>
                Formats acceptés : JPG, PNG, WEBP, PDF — 3 Mo max par fichier — {MAX_FILES} fichiers max par créneau.
              </p>

              {canUpload && (
                <>
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept={ALLOWED_EXT.join(',')}
                    onChange={handleFileChange}
                    style={{ display: 'none' }}
                    id="safety-sheet-input"
                  />
                  <label
                    htmlFor="safety-sheet-input"
                    className="btn btn-outline btn-small"
                    style={{ cursor: 'pointer', display: 'inline-block', marginBottom: 12 }}
                  >
                    📎 Sélectionner des fichiers
                  </label>
                </>
              )}

              {selectedFiles.length > 0 && (
                <>
                  <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 12px' }}>
                    {selectedFiles.map((f, i) => (
                      <li key={i} style={{
                        display: 'flex', alignItems: 'center', gap: 8,
                        padding: '5px 10px', background: '#eff6ff',
                        borderRadius: 6, marginBottom: 5,
                        border: '1px solid #bfdbfe',
                      }}>
                        <span style={{ flex: 1, fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {f.type === 'application/pdf' ? '📄' : '🖼️'} {f.name}
                        </span>
                        <span style={{ fontSize: 11, color: '#9ca3af', whiteSpace: 'nowrap' }}>
                          {formatSize(f.size)}
                        </span>
                        <button
                          onClick={() => removeSelected(i)}
                          className="btn-icon"
                          title="Retirer"
                          style={{ color: '#6b7280' }}
                        >
                          ✕
                        </button>
                      </li>
                    ))}
                  </ul>
                  <button
                    className="btn btn-primary btn-small"
                    onClick={handleUpload}
                    disabled={uploading}
                  >
                    {uploading ? 'Envoi en cours…' : `📤 Envoyer ${selectedFiles.length} fichier(s)`}
                  </button>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
