import { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { NavBar } from './components/NavBar';
import { CalendarPage } from './pages/CalendarPage';
import { ProfilePage } from './pages/ProfilePage';
import { AdminPage } from './pages/AdminPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { HelpPage } from './pages/HelpPage';
import { adminService } from './services/adminService';
import type { AppConfig } from './types';
import './App.css';

function AppContent() {
  const { isAuthenticated, user } = useAuth();

  const urlParams = new URLSearchParams(window.location.search);
  const resetToken = urlParams.get('token');
  const isResetPage = window.location.pathname === '/reset-password' && resetToken;

  const [currentPage, setCurrentPage] = useState<string>('calendar');
  const [appConfig, setAppConfig] = useState<AppConfig | null>(null);

  useEffect(() => {
    adminService.getConfig().then(setAppConfig).catch(() => {});
  }, []);

  const navigate = (page: string) => {
    if (page === 'admin' && user?.role !== 'ADMIN') return;
    if (page === 'profile' && !isAuthenticated) return;
    setCurrentPage(page);
  };

  if (isResetPage) {
    return <ResetPasswordPage token={resetToken!} />;
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
        {currentPage === 'calendar' && <CalendarPage />}
        {currentPage === 'profile' && isAuthenticated && <ProfilePage />}
        {currentPage === 'admin' && user?.role === 'ADMIN' && <AdminPage />}
        {currentPage === 'help' && <HelpPage />}
      </div>
      <footer className="app-footer">
        <p>🌊 Lac Plongée — Système de réservation © {new Date().getFullYear()}</p>
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
