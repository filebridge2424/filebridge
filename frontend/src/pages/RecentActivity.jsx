import { useState, useEffect } from 'react';
import {
  Clock,
  UploadCloud,
  Download,
  Edit3,
  FolderInput,
  Trash2,
  RotateCcw,
  AlertTriangle,
  Share2,
  UserX,
  Crown,
  History
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { activityApi } from '../api/filesApi.js';
import { formatRelativeDate } from '../utils/format.js';

export default function RecentActivity() {
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchActivities = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await activityApi.list(0, 50);
      setActivities(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load activity timeline.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchActivities();
  }, []);

  const getEventBadge = (type) => {
    switch (type) {
      case 'FILE_UPLOADED':
        return {
          icon: UploadCloud,
          bg: 'bg-emerald-50 text-emerald-600 border-emerald-200/60',
          label: 'Uploaded'
        };
      case 'FILE_DOWNLOADED':
        return {
          icon: Download,
          bg: 'bg-blue-50 text-blue-600 border-blue-200/60',
          label: 'Downloaded'
        };
      case 'FILE_RENAMED':
        return {
          icon: Edit3,
          bg: 'bg-amber-50 text-amber-600 border-amber-200/60',
          label: 'Renamed'
        };
      case 'FILE_MOVED':
        return {
          icon: FolderInput,
          bg: 'bg-purple-50 text-purple-600 border-purple-200/60',
          label: 'Moved'
        };
      case 'FILE_DELETED':
        return {
          icon: Trash2,
          bg: 'bg-orange-50 text-orange-600 border-orange-200/60',
          label: 'Moved to Trash'
        };
      case 'FILE_RESTORED':
        return {
          icon: RotateCcw,
          bg: 'bg-teal-50 text-teal-600 border-teal-200/60',
          label: 'Restored'
        };
      case 'FILE_PERMANENTLY_DELETED':
        return {
          icon: AlertTriangle,
          bg: 'bg-red-50 text-red-600 border-red-200/60',
          label: 'Permanently Deleted'
        };
      case 'FILE_SHARED':
        return {
          icon: Share2,
          bg: 'bg-indigo-50 text-indigo-600 border-indigo-200/60',
          label: 'Shared'
        };
      case 'SHARE_REVOKED':
        return {
          icon: UserX,
          bg: 'bg-rose-50 text-rose-600 border-rose-200/60',
          label: 'Access Revoked'
        };
      case 'PLAN_UPGRADED':
        return {
          icon: Crown,
          bg: 'bg-yellow-50 text-amber-600 border-amber-200/60',
          label: 'Plan Upgraded'
        };
      default:
        return {
          icon: History,
          bg: 'bg-slate-50 text-slate-600 border-slate-200/60',
          label: 'Activity'
        };
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Recent Activity</h1>
        <p className="text-sm text-slate-500 mt-0.5">Live timeline of file actions, shares and subscription events</p>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-xs text-red-600">
          {error}
        </div>
      )}

      {loading && (
        <div className="card divide-y divide-slate-100 animate-pulse">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="p-4 flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-slate-100" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-slate-100 rounded w-1/3" />
                <div className="h-3 bg-slate-100 rounded w-1/4" />
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && activities.length === 0 && (
        <div className="card flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mb-4">
            <Clock size={28} className="text-slate-400" />
          </div>
          <h3 className="text-base font-semibold text-slate-700">No activity yet</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm">
            Activity events will appear here when you upload, rename, move, delete, or share files.
          </p>
          <Link to="/files" className="btn-secondary mt-5">
            Go to My Files
          </Link>
        </div>
      )}

      {!loading && activities.length > 0 && (
        <div className="card divide-y divide-slate-100">
          {activities.map((act) => {
            const badge = getEventBadge(act.eventType);
            const Icon = badge.icon;

            return (
              <div
                key={act.id}
                className="p-4 flex items-start gap-4 hover:bg-slate-50/70 transition-colors"
              >
                <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 border ${badge.bg}`}>
                  <Icon size={18} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-semibold text-slate-800">
                      {act.description}
                    </span>
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${badge.bg}`}>
                      {badge.label}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-slate-400 mt-1">
                    {act.fileName && (
                      <span className="text-slate-500 font-medium truncate max-w-xs">
                        {act.fileName}
                      </span>
                    )}
                    {act.fileName && <span>•</span>}
                    <span>{formatRelativeDate(act.timestamp)}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
