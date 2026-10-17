import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import DashboardLayout from './layouts/DashboardLayout.jsx';
import MyFiles from './pages/MyFiles.jsx';
import SharedWithMe from './pages/SharedWithMe.jsx';
import RecentActivity from './pages/RecentActivity.jsx';
import Trash from './pages/Trash.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public authentication routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<DashboardLayout />}>
              <Route path="/" element={<Navigate to="/files" replace />} />
              <Route path="/files" element={<MyFiles />} />
              <Route path="/shared" element={<SharedWithMe />} />
              <Route path="/activity" element={<RecentActivity />} />
              <Route path="/trash" element={<Trash />} />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/files" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
