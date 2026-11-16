import { useState, useEffect, useCallback } from 'react';
import {
  Trash2, RotateCcw, AlertTriangle, CheckCircle, Loader2,
  Folder, File, HardDrive, RefreshCw
} from 'lucide-react';
import { trashApi } from '../api/filesApi.js';
import { formatBytes, formatRelativeDate, getFileTypeInfo } from '../utils/format.js';
import { Link } from 'react-router-dom';

export default function Trash() {
  const [trashData, setTrashData] = useState({ folders: [], files: [], totalTrashBytes: 0 });
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [emptyLoading, setEmptyLoading] = useState(false);
  const [feedback, setFeedback] = useState(null); // { type: 'success' | 'error', message }

  const loadTrash = useCallback(async () => {
    setLoading(true);
    try {
      const data = await trashApi.get();
      setTrashData(data);
    } catch (err) {
      setFeedback({ type: 'error', message: err.message || 'Failed to load trash items.' });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTrash();
  }, [loadTrash]);

  const showNotification = (type, message) => {
    setFeedback({ type, message });
    setTimeout(() => {
      setFeedback((cur) => (cur?.message === message ? null : cur));
    }, 4000);
  };

  const handleRestoreFile = async (file) => {
    setActionLoadingId(`file-restore-${file.id}`);
    try {
      await trashApi.restoreFile(file.id);
      showNotification('success', `Restored "${file.originalFilename}".`);
      loadTrash();
    } catch (err) {
      showNotification('error', `Failed to restore "${file.originalFilename}": ${err.message}`);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handlePermanentDeleteFile = async (file) => {
    if (!window.confirm(`Permanently delete "${file.originalFilename}"?\n\nThis action CANNOT be undone and storage will be freed.`)) return;
    setActionLoadingId(`file-delete-${file.id}`);
    try {
      await trashApi.permanentDeleteFile(file.id);
      showNotification('success', `Permanently deleted "${file.originalFilename}".`);
      loadTrash();
    } catch (err) {
      showNotification('error', `Failed to permanently delete "${file.originalFilename}": ${err.message}`);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleRestoreFolder = async (folder) => {
    setActionLoadingId(`folder-restore-${folder.id}`);
    try {
      await trashApi.restoreFolder(folder.id);
      showNotification('success', `Restored folder "${folder.name}" and its contents.`);
      loadTrash();
    } catch (err) {
      showNotification('error', `Failed to restore folder "${folder.name}": ${err.message}`);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handlePermanentDeleteFolder = async (folder) => {
    if (!window.confirm(`Permanently delete folder "${folder.name}" and all files inside it?\n\nThis action CANNOT be undone.`)) return;
    setActionLoadingId(`folder-delete-${folder.id}`);
    try {
      await trashApi.permanentDeleteFolder(folder.id);
      showNotification('success', `Permanently deleted folder "${folder.name}".`);
      loadTrash();
    } catch (err) {
      showNotification('error', `Failed to permanently delete folder "${folder.name}": ${err.message}`);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleEmptyTrash = async () => {
    if (!window.confirm("Empty entire trash?\n\nAll items in Trash will be permanently deleted and cannot be recovered.")) return;
    setEmptyLoading(true);
    try {
      await trashApi.empty();
      showNotification('success', 'Trash emptied successfully.');
      loadTrash();
    } catch (err) {
      showNotification('error', `Failed to empty trash: ${err.message}`);
    } finally {
      setEmptyLoading(false);
    }
  };

  const totalItems = trashData.folders.length + trashData.files.length;

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Trash</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {loading ? 'Loading...' : `${totalItems} item${totalItems !== 1 ? 's' : ''} in trash (${formatBytes(trashData.totalTrashBytes)})`}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={loadTrash}
            disabled={loading}
            className="btn-secondary"
            title="Refresh Trash"
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refresh</span>
          </button>

          {totalItems > 0 && (
            <button
              onClick={handleEmptyTrash}
              disabled={emptyLoading || loading}
              className="btn-danger flex items-center gap-2"
            >
              {emptyLoading ? (
                <Loader2 size={16} className="animate-spin" />
              ) : (
                <Trash2 size={16} />
              )}
              <span>Empty Trash</span>
            </button>
          )}
        </div>
      </div>

      {/* Notification Toast */}
      {feedback && (
        <div
          className={`flex items-center gap-3 p-4 rounded-xl border animate-fade-in ${
            feedback.type === 'success'
              ? 'bg-teal-50 border-teal-200 text-teal-800'
              : 'bg-red-50 border-red-200 text-red-700'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle size={18} className="text-teal-600 flex-shrink-0" />
          ) : (
            <AlertTriangle size={18} className="text-red-500 flex-shrink-0" />
          )}
          <p className="text-sm font-medium flex-1">{feedback.message}</p>
          <button
            onClick={() => setFeedback(null)}
            className="text-xs font-semibold opacity-70 hover:opacity-100"
          >
            ✕
          </button>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading && (
        <div className="card divide-y divide-slate-100 animate-pulse">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="p-4 flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-slate-100" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-slate-100 rounded w-1/3" />
                <div className="h-3 bg-slate-100 rounded w-1/4" />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!loading && totalItems === 0 && (
        <div className="card flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mb-4">
            <Trash2 size={28} className="text-slate-400" />
          </div>
          <h3 className="text-base font-semibold text-slate-700">Trash is empty</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm">
            Deleted files and folders will appear here. You can restore or permanently delete them at any time.
          </p>
          <Link to="/files" className="btn-secondary mt-5">
            Back to My Files
          </Link>
        </div>
      )}

      {/* Content */}
      {!loading && totalItems > 0 && (
        <div className="space-y-6">
          {/* Deleted Folders */}
          {trashData.folders.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Deleted Folders ({trashData.folders.length})
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {trashData.folders.map((folder) => {
                  const isRestoring = actionLoadingId === `folder-restore-${folder.id}`;
                  const isDeleting = actionLoadingId === `folder-delete-${folder.id}`;

                  return (
                    <div
                      key={folder.id}
                      className="card p-4 flex items-center justify-between gap-3 hover:border-slate-300 transition-colors"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-10 h-10 rounded-xl bg-amber-50 border border-amber-200/50 flex items-center justify-center text-amber-500 flex-shrink-0">
                          <Folder size={20} />
                        </div>
                        <div className="min-w-0">
                          <h4 className="text-sm font-medium text-slate-800 truncate" title={folder.name}>
                            {folder.name}
                          </h4>
                          <p className="text-xs text-slate-400 mt-0.5">
                            Deleted {formatRelativeDate(folder.deletedAt)}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-1 flex-shrink-0">
                        <button
                          onClick={() => handleRestoreFolder(folder)}
                          disabled={isRestoring || isDeleting}
                          title="Restore folder"
                          className="p-2 rounded-lg text-slate-500 hover:text-teal-600 hover:bg-teal-50 transition-colors"
                        >
                          {isRestoring ? <Loader2 size={16} className="animate-spin text-teal-600" /> : <RotateCcw size={16} />}
                        </button>
                        <button
                          onClick={() => handlePermanentDeleteFolder(folder)}
                          disabled={isRestoring || isDeleting}
                          title="Delete permanently"
                          className="p-2 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                        >
                          {isDeleting ? <Loader2 size={16} className="animate-spin text-red-500" /> : <Trash2 size={16} />}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Deleted Files */}
          {trashData.files.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Deleted Files ({trashData.files.length})
              </h2>

              <div className="card overflow-hidden">
                <ul className="divide-y divide-slate-100">
                  {trashData.files.map((file) => {
                    const { color } = getFileTypeInfo(file.contentType);
                    const isRestoring = actionLoadingId === `file-restore-${file.id}`;
                    const isDeleting = actionLoadingId === `file-delete-${file.id}`;

                    return (
                      <li
                        key={file.id}
                        className="flex items-center justify-between px-4 py-3 hover:bg-slate-50 transition-colors"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className={`w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 ${color}`}>
                            <File size={18} />
                          </div>
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-slate-800 truncate" title={file.originalFilename}>
                              {file.originalFilename}
                            </p>
                            <p className="text-xs text-slate-400 mt-0.5">
                              {formatBytes(file.sizeBytes)} • Deleted {formatRelativeDate(file.deletedAt)}
                            </p>
                          </div>
                        </div>

                        <div className="flex items-center gap-1 flex-shrink-0 ml-4">
                          <button
                            onClick={() => handleRestoreFile(file)}
                            disabled={isRestoring || isDeleting}
                            className="btn-secondary text-xs py-1.5 px-2.5 flex items-center gap-1.5 text-teal-700 hover:text-teal-800"
                            title="Restore file"
                          >
                            {isRestoring ? <Loader2 size={14} className="animate-spin" /> : <RotateCcw size={14} />}
                            <span className="hidden sm:inline">Restore</span>
                          </button>
                          <button
                            onClick={() => handlePermanentDeleteFile(file)}
                            disabled={isRestoring || isDeleting}
                            className="p-2 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                            title="Delete permanently"
                          >
                            {isDeleting ? <Loader2 size={14} className="animate-spin text-red-500" /> : <Trash2 size={16} />}
                          </button>
                        </div>
                      </li>
                    );
                  })}
                </ul>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
