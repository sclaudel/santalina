import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { LoginModal } from './LoginModal';
import { adminService } from '../services/adminService';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '🔑 Administrateur',
  DIVE_DIRECTOR: '🤿 Directeur de plongée',
  DIVER: '🏊 Plongeur',
};
const ROLE_COLORS: Record<string, string> = {
  ADMIN: 'badge-admin',
  DIVE_DIRECTOR: 'badge-director',
  DIVER: 'badge-guest',
};

interface Props {
  onNavigate: (page: string) => void;
  currentPage: string;
  selfRegistration?: boolean;
}

export function NavBar({ onNavigate, currentPage, selfRegistration = true }: Props) {
  const { user, isAuthenticated, logout } = useAuth();
  const [showLogin, setShowLogin] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [siteName, setSiteName] = useState('Carrière de Saint-Lin');

  useEffect(() => {
    adminService.getConfig()
      .then(c => { if (c.siteName) setSiteName(c.siteName); })
      .catch(() => {});
  }, []);

  return (
    <nav className="navbar">
      <div className="navbar-brand" onClick={() => onNavigate('calendar')}>
        🌊 <span>{siteName}</span>
      </div>

      <div className="navbar-nav">
        <button
          className={`nav-link ${currentPage === 'calendar' ? 'active' : ''}`}
          onClick={() => onNavigate('calendar')}
        >
          📅 Calendrier
        </button>
        {isAuthenticated && user?.role === 'ADMIN' && (
          <button
            className={`nav-link ${currentPage === 'admin' ? 'active' : ''}`}
            onClick={() => onNavigate('admin')}
          >
            ⚙️ Administration
          </button>
        )}
      </div>

      <div className="navbar-auth">
        {isAuthenticated && user ? (
          <div className="user-menu-wrapper">
            <button className="user-btn" onClick={() => setShowUserMenu(!showUserMenu)}>
              <span className={`role-badge ${ROLE_COLORS[user.role]}`}>
                {ROLE_LABELS[user.role]}
              </span>
              <span className="user-name">{user.name}</span>
              <span>▾</span>
            </button>
            {showUserMenu && (
              <div className="user-dropdown">
                <button onClick={() => { onNavigate('profile'); setShowUserMenu(false); }}>
                  👤 Mon profil
                </button>
                <button onClick={() => { logout(); setShowUserMenu(false); }}>
                  🚪 Déconnexion
                </button>
              </div>
            )}
          </div>
        ) : (
          <button className="btn btn-primary" onClick={() => setShowLogin(true)}>
            🔐 Connexion
          </button>
        )}
      </div>

      {showLogin && <LoginModal onClose={() => setShowLogin(false)} selfRegistration={selfRegistration} />}
    </nav>
  );
}
