import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

/**
 * Intercepte les erreurs de rendu React et affiche un message de repli
 * au lieu de planter l'application entière.
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary] Erreur non gérée :', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', minHeight: '60vh', padding: '2rem', textAlign: 'center',
        }}>
          <h2 style={{ color: '#c0392b', marginBottom: '1rem' }}>Une erreur est survenue</h2>
          <p style={{ color: '#555', marginBottom: '1.5rem' }}>
            Une erreur inattendue s'est produite. Veuillez recharger la page.
          </p>
          <button
            onClick={() => { this.setState({ hasError: false, error: null }); window.location.reload(); }}
            style={{
              padding: '0.6rem 1.4rem', backgroundColor: '#2980b9', color: 'white',
              border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '1rem',
            }}
          >
            Recharger la page
          </button>
          {this.state.error && (
            <details style={{ marginTop: '1rem', fontSize: '0.8rem', color: '#888' }}>
              <summary>Détails techniques</summary>
              <pre style={{ textAlign: 'left', whiteSpace: 'pre-wrap' }}>{this.state.error.message}</pre>
            </details>
          )}
        </div>
      );
    }
    return this.props.children;
  }
}
