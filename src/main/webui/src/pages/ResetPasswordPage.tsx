import { useState } from 'react';
import { authService } from '../services/authService';

interface Props {
  token: string;
}

export function ResetPasswordPage({ token }: Props) {
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirm) { setError('Les mots de passe ne correspondent pas'); return; }
    setError(''); setMsg(''); setLoading(true);
    try {
      await authService.confirmPasswordReset(token, password);
      setMsg('Mot de passe réinitialisé avec succès !');
      setDone(true);
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(m || 'Lien invalide ou expiré');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page center-page">
      <div className="reset-card">
        <h2>🔒 Réinitialisation du mot de passe</h2>
        {msg && <div className="alert alert-success">{msg}</div>}
        {error && <div className="alert alert-error">{error}</div>}
        {!done ? (
          <form onSubmit={handleSubmit} className="form">
            <div className="form-group">
              <label>Nouveau mot de passe</label>
              <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={6} />
            </div>
            <div className="form-group">
              <label>Confirmer le mot de passe</label>
              <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)} required minLength={6} />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Réinitialisation...' : 'Réinitialiser'}
            </button>
          </form>
        ) : (
          <p><a href="/">← Retour à l'accueil</a></p>
        )}
      </div>
    </div>
  );
}

