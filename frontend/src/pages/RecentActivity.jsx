import { Clock, History } from 'lucide-react';
import { useFiles } from '../hooks/useFiles.js';
import { formatBytes, formatRelativeDate, getFileTypeInfo } from '../utils/format.js';
import { Link } from 'react-router-dom';

export default function RecentActivity() {
  const { files, loading } = useFiles();

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Recent Activity</h1>
        <p className="text-sm text-slate-500 mt-0.5">Timeline of recent uploads and actions in your workspace</p>
      </div>

      {loading && (
        <div className="card divide-y divide-slate-100 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="p-4 flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-slate-100" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-slate-100 rounded w-1/3" />
                <div className="h-3 bg-slate-100 rounded w-1/4" />
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && files.length === 0 && (
        <div className="card flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mb-4">
            <Clock size={28} className="text-slate-400" />
          </div>
          <h3 className="text-base font-semibold text-slate-700">No activity yet</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm">
            Activity updates will appear here whenever files are uploaded or modified.
          </p>
          <Link to="/files" className="btn-secondary mt-5">
            Go to My Files
          </Link>
        </div>
      )}

      {!loading && files.length > 0 && (
        <div className="card divide-y divide-slate-100">
          {files.map((file) => {
            const { color } = getFileTypeInfo(file.contentType);
            return (
              <div key={file.id} className="p-4 flex items-center gap-4 hover:bg-slate-50 transition-colors">
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 ${color}`}>
                  <History size={18} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-800 truncate">
                    Uploaded <span className="font-semibold">{file.originalFilename}</span>
                  </p>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {formatBytes(file.sizeBytes)} · {formatRelativeDate(file.uploadedAt)}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
