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
        <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>Le mot de passe doit contenir au moins <strong>8 caractères</strong>, une <strong>majuscule</strong>, un <strong>chiffre</strong> et un <strong>caractère spécial</strong>.</p>
        {msg && <div className="alert alert-success">{msg}</div>}
        {error && <div className="alert alert-error">{error}</div>}
        {!done ? (
          <form onSubmit={handleSubmit} className="form">
            <div className="form-group">
              <label>Nouveau mot de passe</label>
              <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={8}
                pattern="^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,100}$"
                title="Au moins 8 caractères, une majuscule, un chiffre et un caractère spécial" />
            </div>
            <div className="form-group">
              <label>Confirmer le mot de passe</label>
              <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)} required minLength={8} />
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

