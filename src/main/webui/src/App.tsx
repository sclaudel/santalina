import { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { NavBar } from './components/NavBar';
import { CalendarPage } from './pages/CalendarPage';
import { ProfilePage } from './pages/ProfilePage';
import { AdminPage } from './pages/AdminPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import './App.css';

function AppContent() {
  const { isAuthenticated, user } = useAuth();

  const urlParams = new URLSearchParams(window.location.search);
  const resetToken = urlParams.get('token');
  const isResetPage = window.location.pathname === '/reset-password' && resetToken;

  const [currentPage, setCurrentPage] = useState<string>('calendar');

  const navigate = (page: string) => {
    if (page === 'admin' && user?.role !== 'ADMIN') return;
    if (page === 'profile' && !isAuthenticated) return;
    setCurrentPage(page);
  };

  if (isResetPage) {
    return <ResetPasswordPage token={resetToken!} />;
  }

  return (
    <div className="app">
      <NavBar onNavigate={navigate} currentPage={currentPage} />
      <div className="app-content">
        {currentPage === 'calendar' && <CalendarPage />}
        {currentPage === 'profile' && isAuthenticated && <ProfilePage />}
        {currentPage === 'admin' && user?.role === 'ADMIN' && <AdminPage />}
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
