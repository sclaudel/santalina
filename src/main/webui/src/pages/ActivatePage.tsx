import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

interface Props {
  token: string;
}

export function ActivatePage({ token }: Props) {
  const { activateAccount } = useAuth();
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
      await activateAccount(token, password);
      setMsg('Compte activé avec succès ! Vous êtes maintenant connecté.');
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
        <h2>🎉 Activation de votre compte</h2>
        <p style={{ color: '#6b7280', marginBottom: 16 }}>Choisissez un mot de passe pour finaliser la création de votre compte.</p>
        {msg && <div className="alert alert-success">{msg}</div>}
        {error && <div className="alert alert-error">{error}</div>}
        {!done ? (
          <form onSubmit={handleSubmit} className="form">
            <div className="form-group">
              <label>Mot de passe</label>
              <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={6} />
            </div>
            <div className="form-group">
              <label>Confirmer le mot de passe</label>
              <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)} required minLength={6} />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Activation...' : 'Activer mon compte'}
            </button>
          </form>
        ) : (
          <p><a href="/">← Aller au calendrier</a></p>
        )}
      </div>
    </div>
  );
}
