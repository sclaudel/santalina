import { useEffect, useRef, type ReactNode } from 'react';

interface RichTextEditorProps {
  /** HTML initial (lu une seule fois au montage) */
  initialValue: string;
  onChange: (html: string) => void;
  placeholder?: string;
  minHeight?: number;
}

/**
 * Éditeur WYSIWYG minimal basé sur contentEditable.
 * Barre d'outils : Gras, Italique, Souligné, H2, H3, Paragraphe, Liste puces, Liste numérotée.
 * Pas de dépendance externe.
 */
export function RichTextEditor({ initialValue, onChange, placeholder, minHeight = 320 }: RichTextEditorProps) {
  const editorRef = useRef<HTMLDivElement>(null);

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
        {tbBtn('↺', 'Annuler (Ctrl+Z)', () => exec('undo'))}
        {tbBtn('↻', 'Rétablir (Ctrl+Y)', () => exec('redo'))}
      </div>

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
