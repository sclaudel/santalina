import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';

interface Props {
  onClose: () => void;
  selfRegistration?: boolean;
}

export function LoginModal({ onClose, selfRegistration = true }: Props) {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register' | 'forgot'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [gdprAccepted, setGdprAccepted] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [registerDone, setRegisterDone] = useState(false);

  // Captcha
  const [captchaId, setCaptchaId] = useState('');
  const [captchaImage, setCaptchaImage] = useState('');
  const [captchaAnswer, setCaptchaAnswer] = useState('');
  const [captchaLoading, setCaptchaLoading] = useState(false);

  const loadCaptcha = useCallback(async () => {
    setCaptchaLoading(true);
    setCaptchaAnswer('');
    try {
      const data = await authService.getCaptcha();
      setCaptchaId(data.id);
      setCaptchaImage(data.image);
    } catch {
      // silently ignore, user can retry
    } finally {
      setCaptchaLoading(false);
    }
  }, []);

  useEffect(() => {
    if (mode === 'register') {
      setFirstName(''); setLastName('');
      setGdprAccepted(false);
      setRegisterDone(false);
      loadCaptcha();
    }
  }, [mode, loadCaptcha]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setSuccess(''); setLoading(true);
    try {
      if (mode === 'login') {
        await login(email, password);
        onClose();
      } else if (mode === 'register') {
        const msg = await register(email, firstName, lastName, phone, captchaId, captchaAnswer);
        setRegisterDone(true);
        setSuccess(msg);
      } else {
        const res = await fetch('/api/auth/password-reset/request', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email }),
        });
        if (res.ok) setSuccess('Si cet email existe, un lien de réinitialisation a été envoyé.');
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg || 'Une erreur est survenue');
      if (mode === 'register') loadCaptcha();
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (m: 'login' | 'register' | 'forgot') => {
    setMode(m); setError(''); setSuccess(''); setRegisterDone(false);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2 className="modal-title">
          {mode === 'login' ? '🔐 Connexion' : mode === 'register' ? '📝 Inscription' : '📧 Mot de passe oublié'}
        </h2>

        {error   && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        {/* Succès inscription */}
        {registerDone ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>📧</div>
            <p style={{ fontWeight: 600, fontSize: 16, marginBottom: 8 }}>Vérifiez votre boite email !</p>
            <p style={{ color: '#4b5563', fontSize: 14 }}>{success}</p>
            <button className="btn btn-outline" style={{ marginTop: 20 }} onClick={onClose}>Fermer</button>
          </div>
        ) : (
        <form onSubmit={handleSubmit} className="form">
          {mode === 'register' && (
            <div className="form-row">
              <div className="form-group" style={{ flex: 1 }}>
                <label>Prénom *</label>
                <input type="text" value={firstName} onChange={e => setFirstName(e.target.value)}
                  required placeholder="Jean" minLength={2} />
              </div>
              <div className="form-group" style={{ flex: 1 }}>
                <label>Nom *</label>
                <input type="text" value={lastName} onChange={e => setLastName(e.target.value)}
                  required placeholder="Dupont" minLength={2} />
              </div>
            </div>
          )}

          <div className="form-group">
            <label>Email *</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)}
              required placeholder="email@exemple.com" />
          </div>

          {mode === 'register' && (
            <div className="form-group">
              <label>Téléphone *</label>
              <input type="tel" value={phone} onChange={e => setPhone(e.target.value)}
                required placeholder="+33 6 12 34 56 78"
                pattern="^[+]?[0-9 .\-()]{7,20}$"
                title="Numéro de téléphone valide (ex: +33 6 12 34 56 78)" />
            </div>
          )}

          {mode === 'login' && (
            <div className="form-group">
              <label>Mot de passe *</label>
              <input type="password" value={password} onChange={e => setPassword(e.target.value)}
                required minLength={6} placeholder="••••••" />
            </div>
          )}

          {/* Captcha image */}
          {mode === 'register' && (
            <div className="form-group">
              <label>Vérification *</label>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                {captchaImage
                  ? <img src={captchaImage} alt="Code de vérification" style={{ border: '1px solid #ccd5de', borderRadius: '4px', height: '60px' }} />
                  : <div style={{ width: '200px', height: '60px', background: '#eef2f7', borderRadius: '4px' }} />}
                <button type="button" onClick={loadCaptcha} disabled={captchaLoading}
                  title="Nouveau code" style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px', padding: '4px' }}>
                  ↺
                </button>
              </div>
              <input type="text" value={captchaAnswer} onChange={e => setCaptchaAnswer(e.target.value)}
                required placeholder="Tapez les caractères affichés" maxLength={10}
                autoComplete="off" autoCorrect="off" autoCapitalize="characters" spellCheck={false} />
            </div>
          )}

          {/* Consentement RGPD */}
          {mode === 'register' && (
            <div className="gdpr-consent">
              <input id="gdpr-check" type="checkbox" required checked={gdprAccepted}
                onChange={e => setGdprAccepted(e.target.checked)} />
              <label htmlFor="gdpr-check">
                J'accepte que mes données personnelles (prénom, nom, email, téléphone) soient
                enregistrées pour la gestion des réservations de plongée, conformément au RGPD.
                Ces données ne seront pas transmises à des tiers.
              </label>
            </div>
          )}

          <button type="submit" className="btn btn-primary" disabled={loading} style={{ width: '100%' }}>
            {loading ? 'Chargement...' : mode === 'login' ? 'Se connecter' : mode === 'register' ? "S'inscrire" : 'Envoyer le lien'}
          </button>
        </form>
        )}

        <div className="modal-links">
          {mode === 'login' && (<>
            {selfRegistration && (
              <button onClick={() => switchMode('register')}>Pas encore de compte ? S'inscrire</button>
            )}
            <button onClick={() => switchMode('forgot')}>Mot de passe oublié ?</button>
          </>)}
          {mode !== 'login' && (
            <button onClick={() => switchMode('login')}>← Retour à la connexion</button>
          )}
        </div>
      </div>
    </div>
  );
}
