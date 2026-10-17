import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  Files, Share2, Clock, Trash2,
  HardDrive, Menu, X, LogOut, User as UserIcon
} from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '../context/AuthContext.jsx';

const NAV_ITEMS = [
  { to: '/files',    icon: Files,    label: 'My Files' },
  { to: '/shared',  icon: Share2,   label: 'Shared with Me' },
  { to: '/activity',icon: Clock,    label: 'Recent Activity' },
  { to: '/trash',   icon: Trash2,   label: 'Trash' },
];

export default function DashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">
      {/* Mobile sidebar overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/40 backdrop-blur-sm lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
          fixed inset-y-0 left-0 z-30 flex flex-col w-64 bg-navy-950 text-white
          transform transition-transform duration-200 ease-in-out
          lg:relative lg:translate-x-0
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 px-5 py-5 border-b border-white/10">
          <div className="flex items-center justify-center w-8 h-8 bg-teal-500 rounded-lg">
            <HardDrive size={18} className="text-white" />
          </div>
          <span className="text-lg font-semibold tracking-tight">FileBridge</span>
          <button
            className="ml-auto lg:hidden text-white/60 hover:text-white"
            onClick={() => setSidebarOpen(false)}
          >
            <X size={18} />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150 ${
                  isActive
                    ? 'bg-teal-600/20 text-teal-300 border border-teal-600/30'
                    : 'text-slate-300 hover:bg-white/5 hover:text-white'
                }`
              }
            >
              <Icon size={18} />
              <span className="flex-1">{label}</span>
            </NavLink>
          ))}
        </nav>

        {/* User Profile & Logout section in footer */}
        <div className="p-3 border-t border-white/10 space-y-2">
          {user && (
            <div className="flex items-center gap-2.5 px-2.5 py-2 rounded-lg bg-white/5">
              <div className="w-8 h-8 rounded-full bg-teal-600/30 border border-teal-500/40 flex items-center justify-center flex-shrink-0 text-teal-300">
                <UserIcon size={15} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold text-white truncate">{user.name}</p>
                <p className="text-[11px] text-slate-400 truncate">{user.email}</p>
              </div>
            </div>
          )}

          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2 px-3 py-2 text-xs font-medium text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
          >
            <LogOut size={15} />
            <span>Sign out</span>
          </button>

          <p className="text-[10px] text-slate-600 text-center pt-1">FileBridge v1.0</p>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        {/* Top bar — only on mobile */}
        <div className="flex items-center justify-between px-4 py-3 bg-white border-b border-slate-200 lg:hidden">
          <div className="flex items-center gap-2.5">
            <button
              onClick={() => setSidebarOpen(true)}
              className="p-1.5 rounded-md text-slate-500 hover:bg-slate-100"
            >
              <Menu size={20} />
            </button>
            <div className="flex items-center gap-2">
              <div className="flex items-center justify-center w-6 h-6 bg-teal-500 rounded">
                <HardDrive size={14} className="text-white" />
              </div>
              <span className="font-semibold text-slate-900 text-sm">FileBridge</span>
            </div>
          </div>

          {user && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-600 font-medium hidden sm:inline">{user.name}</span>
              <button
                onClick={handleLogout}
                className="p-1.5 text-slate-400 hover:text-red-600 rounded-md"
                title="Sign out"
              >
                <LogOut size={16} />
              </button>
            </div>
          )}
        </div>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
