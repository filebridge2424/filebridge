import { useState, useEffect } from 'react';
import { Clock, Eye, Download, FileText, Image, File } from 'lucide-react';
import { filesApi } from '../api/filesApi.js';
import { formatBytes, formatRelativeDate, getFileTypeInfo } from '../utils/format.js';

export default function RecentFilesSection({ onPreview, refreshTrigger }) {
  const [recentFiles, setRecentFiles] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    async function loadRecent() {
      try {
        const files = await filesApi.recent(5);
        if (active) setRecentFiles(files);
      } catch {
        // silent fail for non-critical widget
      } finally {
        if (active) setLoading(false);
      }
    }
    loadRecent();
    return () => { active = false; };
  }, [refreshTrigger]);

  if (loading || recentFiles.length === 0) return null;

  return (
    <div className="space-y-2.5">
      <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-slate-500">
        <Clock size={13} className="text-teal-600" />
        <span>Recent Uploads</span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
        {recentFiles.map((file) => {
          const { color } = getFileTypeInfo(file.contentType);
          return (
            <div
              key={file.id}
              onClick={() => onPreview?.(file)}
              className="group flex flex-col justify-between p-3 bg-white rounded-xl border border-slate-200/80 hover:border-teal-400 hover:shadow-sm transition-all duration-150 cursor-pointer"
            >
              <div className="flex items-center justify-between mb-2">
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs ${color}`}>
                  {file.contentType?.startsWith('image/') ? <Image size={16} /> : <FileText size={16} />}
                </div>
                <span className="text-[10px] text-slate-400">
                  {formatRelativeDate(file.uploadedAt)}
                </span>
              </div>

              <div>
                <p className="text-xs font-medium text-slate-800 truncate group-hover:text-teal-600 transition-colors" title={file.originalFilename}>
                  {file.originalFilename}
                </p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  {formatBytes(file.sizeBytes)}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
