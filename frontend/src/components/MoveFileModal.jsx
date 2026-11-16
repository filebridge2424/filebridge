import { useState, useEffect } from 'react';
import { Folder, HardDrive, X, ArrowRight, Loader2 } from 'lucide-react';
import { foldersApi } from '../api/filesApi.js';

export default function MoveFileModal({ file, currentFolderId, onClose, onMoved }) {
  const [folders, setFolders] = useState([]);
  const [selectedFolderId, setSelectedFolderId] = useState(currentFolderId || '');
  const [loading, setLoading] = useState(true);
  const [moving, setMoving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadAllFolders() {
      setLoading(true);
      setError(null);
      try {
        // Fetch all active folders (root level)
        const data = await foldersApi.list(null);
        setFolders(data);
      } catch (err) {
        setError(err.message || 'Failed to load folders.');
      } finally {
        setLoading(false);
      }
    }
    loadAllFolders();
  }, []);

  const handleMove = async () => {
    const target = selectedFolderId === '' ? null : selectedFolderId;
    if (target === currentFolderId) {
      onClose();
      return;
    }
    setMoving(true);
    setError(null);
    try {
      await onMoved(file.id, target);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to move file.');
      setMoving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-navy-950/70 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl border border-slate-200/80 w-full max-w-md overflow-hidden animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-teal-50 border border-teal-200/50 flex items-center justify-center text-teal-600">
              <Folder size={16} />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-800">Move File</h3>
              <p className="text-xs text-slate-400 truncate max-w-[260px]">{file.originalFilename}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
          >
            <X size={16} />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600">
              {error}
            </div>
          )}

          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            Select Destination Folder
          </p>

          {loading ? (
            <div className="flex items-center justify-center py-8 text-slate-400">
              <Loader2 size={24} className="animate-spin text-teal-600" />
            </div>
          ) : (
            <div className="space-y-1.5 max-h-56 overflow-y-auto pr-1">
              {/* Root Directory Option */}
              <button
                type="button"
                onClick={() => setSelectedFolderId('')}
                className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-left text-sm transition-all ${
                  selectedFolderId === ''
                    ? 'bg-teal-50 text-teal-900 border border-teal-300 font-medium'
                    : 'hover:bg-slate-50 text-slate-700 border border-transparent'
                }`}
              >
                <HardDrive size={18} className={selectedFolderId === '' ? 'text-teal-600' : 'text-slate-400'} />
                <span className="flex-1">Root (My Files)</span>
                {currentFolderId === null && (
                  <span className="text-[11px] text-slate-400 italic">Current</span>
                )}
              </button>

              {/* Folder list */}
              {folders.map((f) => {
                const isCurrent = currentFolderId === f.id;
                const isSelected = selectedFolderId === f.id;
                return (
                  <button
                    key={f.id}
                    type="button"
                    onClick={() => setSelectedFolderId(f.id)}
                    className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-left text-sm transition-all ${
                      isSelected
                        ? 'bg-teal-50 text-teal-900 border border-teal-300 font-medium'
                        : 'hover:bg-slate-50 text-slate-700 border border-transparent'
                    }`}
                  >
                    <Folder size={18} className={isSelected ? 'text-teal-600' : 'text-amber-500'} />
                    <span className="flex-1 truncate">{f.name}</span>
                    {isCurrent && (
                      <span className="text-[11px] text-slate-400 italic">Current</span>
                    )}
                  </button>
                );
              })}

              {folders.length === 0 && (
                <p className="text-xs text-slate-400 text-center py-3 italic">
                  No custom folders yet. You can move files to Root.
                </p>
              )}
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50/50">
          <button
            type="button"
            onClick={onClose}
            disabled={moving}
            className="btn-secondary text-xs"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleMove}
            disabled={moving || loading || (selectedFolderId === (currentFolderId || ''))}
            className="btn-primary text-xs"
          >
            {moving ? (
              <>
                <Loader2 size={14} className="animate-spin" />
                <span>Moving...</span>
              </>
            ) : (
              <>
                <ArrowRight size={14} />
                <span>Move Here</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
