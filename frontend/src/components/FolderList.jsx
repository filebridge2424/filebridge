import { useState } from 'react';
import { Folder, MoreVertical, Edit2, Trash2, ArrowRight } from 'lucide-react';

export default function FolderList({ folders, onOpenFolder, onRenameFolder, onDeleteFolder }) {
  const [activeMenuId, setActiveMenuId] = useState(null);

  if (!folders || folders.length === 0) {
    return null;
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
          Folders ({folders.length})
        </h2>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
        {folders.map((folder) => {
          const isMenuOpen = activeMenuId === folder.id;
          const totalItems = (folder.fileCount || 0) + (folder.subfolderCount || 0);

          return (
            <div
              key={folder.id}
              onClick={() => onOpenFolder(folder)}
              className="group relative flex items-center gap-3 p-3.5 bg-white rounded-xl border border-slate-200/80 hover:border-teal-400 hover:shadow-md transition-all duration-150 cursor-pointer select-none"
            >
              <div className="w-10 h-10 rounded-xl bg-amber-50 group-hover:bg-teal-50 border border-amber-200/50 group-hover:border-teal-200/60 flex items-center justify-center text-amber-500 group-hover:text-teal-600 transition-colors flex-shrink-0">
                <Folder size={20} className="transition-transform group-hover:scale-105" />
              </div>

              <div className="flex-1 min-w-0 pr-6">
                <h3 className="text-sm font-medium text-slate-800 truncate group-hover:text-teal-700 transition-colors" title={folder.name}>
                  {folder.name}
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  {totalItems} item{totalItems !== 1 ? 's' : ''}
                </p>
              </div>

              {/* Action Menu button */}
              <div
                className="absolute right-2 top-3"
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  type="button"
                  onClick={() => setActiveMenuId(isMenuOpen ? null : folder.id)}
                  className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors opacity-80 group-hover:opacity-100"
                  title="Folder options"
                >
                  <MoreVertical size={16} />
                </button>

                {isMenuOpen && (
                  <>
                    <div
                      className="fixed inset-0 z-20"
                      onClick={() => setActiveMenuId(null)}
                    />
                    <div className="absolute right-0 top-7 z-30 w-36 bg-white rounded-xl shadow-lg border border-slate-100 py-1 animate-scale-up text-xs">
                      <button
                        type="button"
                        onClick={() => {
                          setActiveMenuId(null);
                          onOpenFolder(folder);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-slate-700 hover:bg-slate-50 hover:text-teal-600 text-left"
                      >
                        <ArrowRight size={14} />
                        <span>Open</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setActiveMenuId(null);
                          onRenameFolder(folder);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-slate-700 hover:bg-slate-50 hover:text-teal-600 text-left"
                      >
                        <Edit2 size={14} />
                        <span>Rename</span>
                      </button>
                      <div className="border-t border-slate-100 my-1" />
                      <button
                        type="button"
                        onClick={() => {
                          setActiveMenuId(null);
                          onDeleteFolder(folder);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-red-600 hover:bg-red-50 text-left"
                      >
                        <Trash2 size={14} />
                        <span>Move to Trash</span>
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
