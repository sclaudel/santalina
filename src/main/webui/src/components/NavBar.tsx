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
  maintenanceMode?: boolean;
}

export function NavBar({ onNavigate, currentPage, selfRegistration = true, maintenanceMode = false }: Props) {
  const { user, isAuthenticated, logout, hasRole } = useAuth();
  const [showLogin, setShowLogin] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [siteName, setSiteName] = useState('Carrière de Saint-Lin');

  useEffect(() => {
    adminService.getConfig()
      .then(c => { if (c.siteName) setSiteName(c.siteName); })
      .catch(() => {});
  }, []);

  const closeMobileMenu = () => setShowMobileMenu(false);

  return (
    <nav className="navbar">
      <div className="navbar-brand" onClick={() => { onNavigate('calendar'); closeMobileMenu(); }}>
        🌊 <span>{siteName}</span>
      </div>

      {/* Navigation desktop */}
      <div className="navbar-nav">
        <button
          className={`nav-link ${currentPage === 'calendar' ? 'active' : ''}`}
          onClick={() => onNavigate('calendar')}
        >
          📅 Calendrier
        </button>
      </div>

      {/* Menu utilisateur desktop */}
      <div className="navbar-auth navbar-auth-desktop">
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
                <button onClick={() => { onNavigate('help'); setShowUserMenu(false); }}>
                  📖 Aide
                </button>
                {user?.role === 'ADMIN' && (
                  <>
                    <div className="user-dropdown-separator" />
                    <button onClick={() => { onNavigate('admin'); setShowUserMenu(false); }}>
                      ⚙️ Administration
                    </button>
                    <button onClick={() => { onNavigate('stats'); setShowUserMenu(false); }}>
                      📊 Statistiques
                    </button>
                  </>
                )}
                {hasRole('DIVE_DIRECTOR') && (
                  <button onClick={() => { onNavigate('my-stats'); setShowUserMenu(false); }}>
                    📊 Mes statistiques
                  </button>
                )}
                <div className="user-dropdown-separator" />
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

      {showLogin && <LoginModal onClose={() => setShowLogin(false)} selfRegistration={selfRegistration} maintenanceMode={maintenanceMode} />}
      {/* Bouton hamburger (mobile uniquement) */}
      <button
        className="navbar-hamburger"
        onClick={() => setShowMobileMenu(v => !v)}
        aria-label="Menu"
        aria-expanded={showMobileMenu}
      >
        {showMobileMenu ? '✕' : '☰'}
      </button>

      {/* Menu déroulant mobile */}
      {showMobileMenu && (
        <div className="navbar-mobile-menu">
          <button
            className={currentPage === 'calendar' ? 'active' : ''}
            onClick={() => { onNavigate('calendar'); closeMobileMenu(); }}
          >
            📅 Calendrier
          </button>
          {isAuthenticated && user?.role === 'ADMIN' && (
            <button
              className={currentPage === 'admin' ? 'active' : ''}
              onClick={() => { onNavigate('admin'); closeMobileMenu(); }}
            >
              ⚙️ Administration
            </button>
          )}
          {isAuthenticated && user?.role === 'ADMIN' && (
            <button
              className={currentPage === 'stats' ? 'active' : ''}
              onClick={() => { onNavigate('stats'); closeMobileMenu(); }}
            >
              📊 Statistiques
            </button>
          )}
          {isAuthenticated && hasRole('DIVE_DIRECTOR') && (
            <button
              className={currentPage === 'my-stats' ? 'active' : ''}
              onClick={() => { onNavigate('my-stats'); closeMobileMenu(); }}
            >
              📊 Mes statistiques
            </button>
          )}
          <button
            className={currentPage === 'help' ? 'active' : ''}
            onClick={() => { onNavigate('help'); closeMobileMenu(); }}
          >
            📖 Aide
          </button>
          {isAuthenticated ? (
            <>
              <button
                className={currentPage === 'profile' ? 'active' : ''}
                onClick={() => { onNavigate('profile'); closeMobileMenu(); }}
              >
                👤 Mon profil {user ? `— ${user.name}` : ''}
              </button>
              <button className="mobile-menu-danger" onClick={() => { logout(); closeMobileMenu(); }}>
                🚪 Déconnexion
              </button>
            </>
          ) : (
            <button onClick={() => { setShowLogin(true); closeMobileMenu(); }}>
              🔐 Connexion
            </button>
          )}
        </div>
      )}

      {showLogin && <LoginModal onClose={() => setShowLogin(false)} selfRegistration={selfRegistration} maintenanceMode={maintenanceMode} />}
    </nav>
  );
}
