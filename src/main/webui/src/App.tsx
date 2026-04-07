import { useState, useEffect, useRef } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { NavBar } from './components/NavBar';
import { CalendarPage } from './pages/CalendarPage';
import { ProfilePage } from './pages/ProfilePage';
import { AdminPage } from './pages/AdminPage';
import { StatsPage } from './pages/StatsPage';
import { MyStatsPage } from './pages/MyStatsPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { ActivatePage } from './pages/ActivatePage';
import { HelpPage } from './pages/HelpPage';
import { PalanqueePage } from './pages/PalanqueePage';
import { adminService } from './services/adminService';
import type { AppConfig } from './types';
import './App.css';

function AppContent() {
  const { isAuthenticated, user, hasRole } = useAuth();

  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');
  const isResetPage = window.location.pathname === '/reset-password' && token;
  const isActivatePage = window.location.pathname === '/activate' && token;

  const [currentPage, setCurrentPage] = useState<string>('calendar');
  const [appConfig, setAppConfig] = useState<AppConfig | null>(null);
  // Contexte de retour vers le calendrier (date + vue + slotId) après navigation palanquées
  const calendarReturnRef = useRef<{ date: string; viewMode: string; slotId?: number } | null>(null);
  const calendarViewModeRef = useRef<string>('day');

  useEffect(() => {
    adminService.getConfig().then(setAppConfig).catch(() => {});
  }, []);

  const navigate = (page: string) => {
    if (page === 'admin' && user?.role !== 'ADMIN') return;
    if (page === 'stats' && user?.role !== 'ADMIN') return;
    if (page === 'my-stats' && !hasRole('DIVE_DIRECTOR')) return;
    if (page === 'profile' && !isAuthenticated) return;
    if (page.startsWith('palanquee-') && !hasRole('ADMIN') && !hasRole('DIVE_DIRECTOR')) return;
    // Mémoriser le viewMode encodé dans la navigation palanquée (palanquee-{id}-{viewMode})
    if (page.startsWith('palanquee-')) {
      const parts = page.split('-');
      // parts: ['palanquee', slotId, viewMode?]
      if (parts.length >= 3) {
        calendarViewModeRef.current = parts.slice(2).join('-'); // 'day' ou 'week'
      }
    }
    setCurrentPage(page);
  };

  if (isResetPage) {
    return <ResetPasswordPage token={token!} />;
  }

  if (isActivatePage) {
    return <ActivatePage token={token!} />;
  }

  // Accès public désactivé : afficher un écran de connexion si non connecté
  const publicAccess = appConfig === null || appConfig.publicAccess;
  const selfRegistration = appConfig === null || appConfig.selfRegistration;

  if (!publicAccess && !isAuthenticated) {
    return (
      <div className="app">
        <div className="center-page" style={{ flexDirection: 'column', gap: 16, textAlign: 'center', padding: 32 }}>
          <div style={{ fontSize: 48 }}>🔒</div>
          <h2 style={{ fontSize: 22, fontWeight: 700 }}>Accès réservé aux membres</h2>
          <p style={{ color: '#6b7280' }}>Connectez-vous pour accéder au calendrier de réservation.</p>
          <NavBar onNavigate={navigate} currentPage={currentPage} selfRegistration={selfRegistration} />
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      <NavBar onNavigate={navigate} currentPage={currentPage} selfRegistration={selfRegistration} />
      <div className="app-content">
        {currentPage === 'calendar' && (
          <CalendarPage
            onNavigate={navigate}
            returnContext={calendarReturnRef.current}
            onReturnConsumed={() => { calendarReturnRef.current = null; }}
          />
        )}
        {currentPage === 'profile' && isAuthenticated && <ProfilePage />}
        {currentPage === 'admin' && user?.role === 'ADMIN' && <AdminPage />}
        {currentPage === 'stats' && user?.role === 'ADMIN' && <StatsPage />}
        {currentPage === 'my-stats' && hasRole('DIVE_DIRECTOR') && <MyStatsPage />}
        {currentPage === 'help' && <HelpPage />}
        {currentPage.startsWith('palanquee-') && (hasRole('ADMIN') || hasRole('DIVE_DIRECTOR')) && (
          <PalanqueePage
            slotId={parseInt(currentPage.split('-')[1], 10)}
            onBack={(slotDate, slotId) => {
              if (slotDate) {
                calendarReturnRef.current = { date: slotDate, viewMode: calendarViewModeRef.current, slotId };
              }
              navigate('calendar');
            }}
          />
        )}
      </div>
      <footer className="app-footer">
        <p>🌊 Santalina — Système de réservation © {new Date().getFullYear()} · v{__APP_VERSION__}</p>
      </footer>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
