import { useEffect, useRef, useState, type ReactNode } from 'react';

interface RichTextEditorProps {
  /** HTML initial (lu une seule fois au montage) */
  initialValue: string;
  onChange: (html: string) => void;
  placeholder?: string;
  minHeight?: number;
}

/**
 * Éditeur WYSIWYG minimal basé sur contentEditable.
 * Barre d'outils : Gras, Italique, Souligné, H2, H3, Paragraphe, Liste puces, Liste numérotée, Lien.
 * Pas de dépendance externe.
 */
export function RichTextEditor({ initialValue, onChange, placeholder, minHeight = 320 }: RichTextEditorProps) {
  const editorRef = useRef<HTMLDivElement>(null);
  const savedRangeRef = useRef<Range | null>(null);
  const [linkPopup, setLinkPopup] = useState(false);
  const [linkUrl, setLinkUrl] = useState('');
  const [linkText, setLinkText] = useState('');

  // Initialise le contenu une seule fois
  useEffect(() => {
    if (editorRef.current) {
      editorRef.current.innerHTML = initialValue;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const exec = (cmd: string, arg?: string) => {
    editorRef.current?.focus();
    // execCommand est déprécié mais reste fonctionnel dans tous les navigateurs actuels
    // eslint-disable-next-line @typescript-eslint/no-deprecated
    document.execCommand(cmd, false, arg ?? undefined);
  };

  /** Sauvegarde la sélection courante avant d'ouvrir la popup */
  const openLinkPopup = () => {
    const sel = window.getSelection();
    const selectedText = sel && sel.rangeCount > 0 ? sel.toString() : '';
    savedRangeRef.current = sel && sel.rangeCount > 0 ? sel.getRangeAt(0).cloneRange() : null;
    setLinkText(selectedText);
    setLinkUrl('https://');
    setLinkPopup(true);
  };

  /** Insère le lien en restaurant la sélection sauvegardée */
  const insertLink = () => {
    const url = linkUrl.trim();
    if (!url || url === 'https://') { setLinkPopup(false); return; }

    editorRef.current?.focus();

    const sel = window.getSelection();
    if (sel && savedRangeRef.current) {
      sel.removeAllRanges();
      sel.addRange(savedRangeRef.current);
    }

    const currentSelected = sel ? sel.toString() : '';
    if (currentSelected) {
      // Il y a du texte sélectionné : on crée le lien dessus
      // eslint-disable-next-line @typescript-eslint/no-deprecated
      document.execCommand('createLink', false, url);
      // Rendre le lien cliquable dans un nouvel onglet
      const anchor = editorRef.current?.querySelector(`a[href="${CSS.escape(url)}"]`);
      if (anchor instanceof HTMLAnchorElement) {
        anchor.target = '_blank';
        anchor.rel = 'noopener noreferrer';
      }
    } else {
      // Pas de sélection : insérer le lien avec le texte fourni
      const text = linkText.trim() || url;
      const safeUrl = url.replace(/"/g, '&quot;');
      const safeText = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      // eslint-disable-next-line @typescript-eslint/no-deprecated
      document.execCommand('insertHTML', false,
        `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer">${safeText}</a>`);
    }

    if (editorRef.current) onChange(editorRef.current.innerHTML);
    setLinkPopup(false);
  };

  const removeLink = () => {
    editorRef.current?.focus();
    // eslint-disable-next-line @typescript-eslint/no-deprecated
    document.execCommand('unlink', false, undefined);
    if (editorRef.current) onChange(editorRef.current.innerHTML);
  };

  const tbBtn = (label: ReactNode, title: string, handler: () => void) => (
    <button
      key={title}
      type="button"
      className="rte-btn"
      title={title}
      onMouseDown={(e) => { e.preventDefault(); handler(); }}
    >
      {label}
    </button>
  );

  return (
    <div className="rte">
      {/* Barre d'outils */}
      <div className="rte-toolbar">
        {tbBtn(<><b>G</b></>, 'Gras (Ctrl+B)', () => exec('bold'))}
        {tbBtn(<><i>I</i></>, 'Italique (Ctrl+I)', () => exec('italic'))}
        {tbBtn(<><u>S</u></>, 'Souligné (Ctrl+U)', () => exec('underline'))}
        <span className="rte-sep" />
        {tbBtn('H2', 'Titre 2', () => exec('formatBlock', 'h2'))}
        {tbBtn('H3', 'Titre 3', () => exec('formatBlock', 'h3'))}
        {tbBtn('¶', 'Paragraphe', () => exec('formatBlock', 'p'))}
        <span className="rte-sep" />
        {tbBtn('• Liste', 'Liste à puces', () => exec('insertUnorderedList'))}
        {tbBtn('1. Liste', 'Liste numérotée', () => exec('insertOrderedList'))}
        <span className="rte-sep" />
        {tbBtn('🔗', 'Insérer un lien (https:// ou mailto:)', openLinkPopup)}
        {tbBtn('🔗✕', 'Supprimer le lien', removeLink)}
        <span className="rte-sep" />
        {tbBtn('↺', 'Annuler (Ctrl+Z)', () => exec('undo'))}
        {tbBtn('↻', 'Rétablir (Ctrl+Y)', () => exec('redo'))}
      </div>

      {/* Popup d'insertion de lien */}
      {linkPopup && (
        <div className="rte-link-popup">
          <div className="rte-link-popup-inner">
            <div className="form-group" style={{ marginBottom: 8 }}>
              <label style={{ fontSize: 13, display: 'block', marginBottom: 4 }}>URL du lien</label>
              <input
                type="text"
                value={linkUrl}
                onChange={e => setLinkUrl(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); insertLink(); } if (e.key === 'Escape') setLinkPopup(false); }}
                placeholder="https://example.com ou mailto:contact@example.com"
                autoFocus
                style={{ width: '100%', fontSize: 13 }}
              />
              <span style={{ fontSize: 11, color: '#6b7280', marginTop: 4, display: 'block' }}>
                Exemples&nbsp;: <code>https://monautre.fr</code> ou <code>mailto:contact@club.fr</code>
              </span>
            </div>
            {!linkText && (
              <div className="form-group" style={{ marginBottom: 8 }}>
                <label style={{ fontSize: 13, display: 'block', marginBottom: 4 }}>Texte affiché</label>
                <input
                  type="text"
                  value={linkText}
                  onChange={e => setLinkText(e.target.value)}
                  placeholder="Texte du lien (laisser vide pour utiliser l'URL)"
                  style={{ width: '100%', fontSize: 13 }}
                />
              </div>
            )}
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <button type="button" className="btn btn-secondary" style={{ fontSize: 13 }} onClick={() => setLinkPopup(false)}>Annuler</button>
              <button type="button" className="btn btn-primary" style={{ fontSize: 13 }} onClick={insertLink}>Insérer</button>
            </div>
          </div>
        </div>
      )}

      {/* Zone éditable */}
      <div
        ref={editorRef}
        contentEditable
        suppressContentEditableWarning
        className="rte-content"
        data-placeholder={placeholder ?? 'Rédigez votre message ici...'}
        style={{ minHeight }}
        onInput={() => {
          if (editorRef.current) onChange(editorRef.current.innerHTML);
        }}
      />
    </div>
  );
}
