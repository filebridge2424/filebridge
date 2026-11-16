import { useState, useEffect, useMemo, useCallback } from 'react';
import {
  FolderPlus, Upload, RefreshCw, AlertTriangle, CheckCircle,
  ChevronRight, HardDrive, Folder, Files, ArrowLeft, ArrowUpDown
} from 'lucide-react';
import { filesApi, foldersApi } from '../api/filesApi.js';
import { useStorageUsage } from '../hooks/useFiles.js';
import FileList from '../components/FileList.jsx';
import FolderList from '../components/FolderList.jsx';
import UploadZone from '../components/UploadZone.jsx';
import StorageUsageBar from '../components/StorageUsageBar.jsx';
import SearchBar from '../components/SearchBar.jsx';
import FilePreviewModal from '../components/FilePreviewModal.jsx';
import CreateFolderModal from '../components/CreateFolderModal.jsx';
import RenameModal from '../components/RenameModal.jsx';
import MoveFileModal from '../components/MoveFileModal.jsx';
import RecentFilesSection from '../components/RecentFilesSection.jsx';

export default function MyFiles() {
  const [currentFolder, setCurrentFolder] = useState(null); // null represents root
  const [breadcrumbs, setBreadcrumbs] = useState([]);
  const [folders, setFolders] = useState([]);
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Search & Sort
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('uploadedAt');
  const [sortDir, setSortDir] = useState('desc');

  // Modals & Panels
  const [showUpload, setShowUpload] = useState(false);
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const [previewFile, setPreviewFile] = useState(null);
  const [renamingItem, setRenamingItem] = useState(null); // { type: 'file' | 'folder', item }
  const [movingFile, setMovingFile] = useState(null);
  const [feedback, setFeedback] = useState(null);

  const [refreshKey, setRefreshKey] = useState(0);
  const { usage, loading: usageLoading, refresh: refreshUsage } = useStorageUsage(refreshKey);

  const showNotification = (type, message) => {
    setFeedback({ type, message });
    setTimeout(() => {
      setFeedback((cur) => (cur?.message === message ? null : cur));
    }, 4000);
  };

  // Fetch folders and files for current directory
  const loadDirectoryData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const folderId = currentFolder ? currentFolder.id : null;
      const [foldersData, filesData] = await Promise.all([
        foldersApi.list(folderId),
        filesApi.list(folderId, '', sortBy, sortDir)
      ]);
      setFolders(foldersData);
      setFiles(filesData);

      // Load lineage if in a subfolder
      if (folderId) {
        try {
          const lineage = await foldersApi.lineage(folderId);
          setBreadcrumbs(lineage);
        } catch {
          setBreadcrumbs(currentFolder ? [currentFolder] : []);
        }
      } else {
        setBreadcrumbs([]);
      }
    } catch (err) {
      setError(err.message || 'Failed to load folder contents.');
    } finally {
      setLoading(false);
    }
  }, [currentFolder, sortBy, sortDir]);

  useEffect(() => {
    loadDirectoryData();
  }, [loadDirectoryData, refreshKey]);

  // Refresh trigger
  const triggerRefresh = useCallback(() => {
    setRefreshKey((k) => k + 1);
    refreshUsage();
  }, [refreshUsage]);

  // Navigation handlers
  const handleOpenFolder = (folder) => {
    setCurrentFolder(folder);
    setSearchQuery('');
  };

  const handleNavigateToRoot = () => {
    setCurrentFolder(null);
    setBreadcrumbs([]);
    setSearchQuery('');
  };

  const handleNavigateBreadcrumb = (folder, index) => {
    if (index === breadcrumbs.length - 1) return; // already here
    setCurrentFolder(folder);
    setSearchQuery('');
  };

  // Folder Actions
  const handleFolderCreated = (newFolder) => {
    showNotification('success', `Folder "${newFolder.name}" created.`);
    triggerRefresh();
  };

  const handleRenameFolder = async (newName) => {
    if (!renamingItem || renamingItem.type !== 'folder') return;
    const folder = renamingItem.item;
    await foldersApi.rename(folder.id, newName);
    showNotification('success', `Folder renamed to "${newName}".`);
    triggerRefresh();
  };

  const handleDeleteFolder = async (folder) => {
    if (!window.confirm(`Move folder "${folder.name}" and all its contents to Trash?\n\nYou can restore it later from Trash.`)) return;
    try {
      await foldersApi.delete(folder.id);
      showNotification('success', `Folder "${folder.name}" moved to Trash.`);
      triggerRefresh();
    } catch (err) {
      showNotification('error', `Failed to delete folder: ${err.message}`);
    }
  };

  // File Actions
  const handleRenameFile = async (newName) => {
    if (!renamingItem || renamingItem.type !== 'file') return;
    const file = renamingItem.item;
    await filesApi.rename(file.id, newName);
    showNotification('success', `File renamed to "${newName}".`);
    triggerRefresh();
  };

  const handleMoveFile = async (fileId, targetFolderId) => {
    await filesApi.move(fileId, targetFolderId);
    showNotification('success', 'File moved successfully.');
    triggerRefresh();
  };

  const handleDeleteFile = (fileId) => {
    showNotification('success', 'File moved to Trash.');
    triggerRefresh();
  };

  const handleUploadComplete = () => {
    showNotification('success', 'File uploaded successfully.');
    triggerRefresh();
  };

  // Search filtering
  const filteredFiles = useMemo(() => {
    if (!searchQuery.trim()) return files;
    const q = searchQuery.toLowerCase();
    return files.filter((f) =>
      f.originalFilename?.toLowerCase().includes(q) ||
      f.contentType?.toLowerCase().includes(q)
    );
  }, [files, searchQuery]);

  const filteredFolders = useMemo(() => {
    if (!searchQuery.trim()) return folders;
    const q = searchQuery.toLowerCase();
    return folders.filter((f) => f.name?.toLowerCase().includes(q));
  }, [folders, searchQuery]);

  const totalCurrentItems = filteredFolders.length + filteredFiles.length;
  const isDirectoryEmpty = !loading && folders.length === 0 && files.length === 0;

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      {/* Header & Primary Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            {currentFolder ? currentFolder.name : 'My Files'}
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {loading ? 'Loading...' : `${totalCurrentItems} item${totalCurrentItems !== 1 ? 's' : ''}`}
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <button
            onClick={triggerRefresh}
            disabled={loading}
            className="btn-secondary"
            title="Refresh"
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refresh</span>
          </button>

          <button
            onClick={() => setShowCreateFolder(true)}
            className="btn-secondary"
          >
            <FolderPlus size={16} className="text-amber-500" />
            <span>New Folder</span>
          </button>

          <button
            onClick={() => setShowUpload((v) => !v)}
            className="btn-primary"
          >
            <Upload size={16} />
            <span>{showUpload ? 'Hide Upload' : 'Upload Files'}</span>
          </button>
        </div>
      </div>

      {/* Storage usage */}
      <StorageUsageBar usage={usage} loading={usageLoading} />

      {/* Feedback Toast */}
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

      {/* Upload Zone (with folderId support) */}
      {showUpload && (
        <div className="card p-4">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-slate-700">
              Upload to {currentFolder ? `"${currentFolder.name}"` : 'Root'}
            </h2>
            <button
              onClick={() => setShowUpload(false)}
              className="text-xs text-slate-400 hover:text-slate-600"
            >
              Close
            </button>
          </div>
          <UploadZone
            folderId={currentFolder ? currentFolder.id : null}
            onUploadComplete={handleUploadComplete}
          />
        </div>
      )}

      {/* Recent Files widget (only visible at root) */}
      {!currentFolder && !searchQuery && (
        <RecentFilesSection
          onPreview={(file) => setPreviewFile(file)}
          refreshTrigger={refreshKey}
        />
      )}

      {/* Breadcrumb Navigation */}
      <div className="flex items-center gap-1.5 text-xs text-slate-600 bg-white px-3.5 py-2.5 rounded-xl border border-slate-200/80 overflow-x-auto shadow-sm">
        <button
          onClick={handleNavigateToRoot}
          className={`flex items-center gap-1.5 font-medium transition-colors hover:text-teal-600 ${
            !currentFolder ? 'text-teal-700 font-semibold' : 'text-slate-500'
          }`}
        >
          <HardDrive size={14} className={!currentFolder ? 'text-teal-600' : 'text-slate-400'} />
          <span>My Files</span>
        </button>

        {breadcrumbs.map((crumb, idx) => (
          <div key={crumb.id} className="flex items-center gap-1.5 flex-shrink-0">
            <ChevronRight size={14} className="text-slate-300" />
            <button
              onClick={() => handleNavigateBreadcrumb(crumb, idx)}
              className={`hover:text-teal-600 truncate max-w-[150px] ${
                idx === breadcrumbs.length - 1
                  ? 'text-teal-700 font-semibold'
                  : 'text-slate-500 font-medium'
              }`}
              title={crumb.name}
            >
              {crumb.name}
            </button>
          </div>
        ))}

        {currentFolder && (
          <button
            onClick={() => {
              if (breadcrumbs.length <= 1) {
                handleNavigateToRoot();
              } else {
                handleNavigateBreadcrumb(breadcrumbs[breadcrumbs.length - 2], breadcrumbs.length - 2);
              }
            }}
            className="ml-auto flex items-center gap-1 text-slate-400 hover:text-slate-600 text-[11px] font-medium"
            title="Go up one folder"
          >
            <ArrowLeft size={13} />
            <span>Up</span>
          </button>
        )}
      </div>

      {/* Error state */}
      {error && (
        <div className="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700">
          <AlertTriangle size={20} className="flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm font-medium">Failed to load items</p>
            <p className="text-xs mt-0.5">{error}</p>
          </div>
          <button onClick={triggerRefresh} className="btn-secondary text-xs">
            Try again
          </button>
        </div>
      )}

      {/* Search and Filters */}
      {!error && (
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          <div className="flex-1 max-w-md">
            <SearchBar
              value={searchQuery}
              onChange={setSearchQuery}
              placeholder={currentFolder ? `Search in ${currentFolder.name}...` : 'Search files and folders...'}
            />
          </div>

          <div className="flex items-center gap-2 flex-shrink-0">
            <div className="flex items-center gap-1 text-xs text-slate-500 bg-white border border-slate-200 rounded-xl px-2.5 py-1.5 shadow-sm">
              <ArrowUpDown size={14} className="text-slate-400" />
              <span className="hidden sm:inline font-medium">Sort:</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="bg-transparent text-xs text-slate-700 font-medium focus:outline-none cursor-pointer"
              >
                <option value="uploadedAt">Date Uploaded</option>
                <option value="originalFilename">Name</option>
                <option value="sizeBytes">File Size</option>
              </select>
              <button
                onClick={() => setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))}
                className="ml-1 text-[11px] font-bold uppercase text-teal-600 hover:text-teal-700"
                title={`Sort ${sortDir === 'asc' ? 'Descending' : 'Ascending'}`}
              >
                {sortDir}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 animate-pulse">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-16 bg-slate-100 rounded-xl" />
            ))}
          </div>
          <div className="card overflow-hidden animate-pulse">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-14 border-b border-slate-100 last:border-0" />
            ))}
          </div>
        </div>
      )}

      {/* Empty State: Directory is completely empty */}
      {isDirectoryEmpty && !searchQuery && (
        <div className="card flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mb-4 text-slate-400">
            {currentFolder ? <Folder size={28} /> : <Files size={28} />}
          </div>
          <h3 className="text-base font-semibold text-slate-700">
            {currentFolder ? 'This folder is empty' : 'No files or folders yet'}
          </h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm">
            {currentFolder
              ? 'Upload files or create subfolders inside this folder.'
              : 'Upload your first file or create a folder to organize your workspace.'}
          </p>
          <div className="flex items-center gap-3 mt-5">
            <button
              onClick={() => setShowCreateFolder(true)}
              className="btn-secondary"
            >
              <FolderPlus size={16} className="text-amber-500" />
              <span>Create Folder</span>
            </button>
            <button
              onClick={() => setShowUpload(true)}
              className="btn-primary"
            >
              <Upload size={16} />
              <span>Upload Files</span>
            </button>
          </div>
        </div>
      )}

      {/* Empty State: No search results */}
      {!loading && !isDirectoryEmpty && totalCurrentItems === 0 && searchQuery && (
        <div className="card flex flex-col items-center justify-center py-12 text-center">
          <p className="text-base font-medium text-slate-600">No items match "{searchQuery}"</p>
          <p className="text-sm text-slate-500 mt-1">Try a different search term or clear the search.</p>
          <button onClick={() => setSearchQuery('')} className="btn-secondary mt-3 text-sm">
            Clear search
          </button>
        </div>
      )}

      {/* Folders & Files Views */}
      {!loading && totalCurrentItems > 0 && (
        <div className="space-y-6">
          {/* Folders Grid */}
          <FolderList
            folders={filteredFolders}
            onOpenFolder={handleOpenFolder}
            onRenameFolder={(folder) => setRenamingItem({ type: 'folder', item: folder })}
            onDeleteFolder={handleDeleteFolder}
          />

          {/* Files List */}
          {filteredFiles.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Files ({filteredFiles.length})
                </h2>
              </div>
              <FileList
                files={filteredFiles}
                onDelete={handleDeleteFile}
                onRename={(file) => setRenamingItem({ type: 'file', item: file })}
                onMove={(file) => setMovingFile(file)}
                onPreview={(file) => setPreviewFile(file)}
              />
            </div>
          )}
        </div>
      )}

      {/* Modals */}
      {showCreateFolder && (
        <CreateFolderModal
          parentId={currentFolder ? currentFolder.id : null}
          onClose={() => setShowCreateFolder(false)}
          onCreated={handleFolderCreated}
        />
      )}

      {previewFile && (
        <FilePreviewModal
          file={previewFile}
          onClose={() => setPreviewFile(null)}
        />
      )}

      {renamingItem && (
        <RenameModal
          title={renamingItem.type === 'folder' ? 'Rename Folder' : 'Rename File'}
          currentName={
            renamingItem.type === 'folder'
              ? renamingItem.item.name
              : renamingItem.item.originalFilename
          }
          onClose={() => setRenamingItem(null)}
          onRename={
            renamingItem.type === 'folder' ? handleRenameFolder : handleRenameFile
          }
        />
      )}

      {movingFile && (
        <MoveFileModal
          file={movingFile}
          currentFolderId={currentFolder ? currentFolder.id : null}
          onClose={() => setMovingFile(null)}
          onMoved={handleMoveFile}
        />
      )}
    </div>
  );
}
