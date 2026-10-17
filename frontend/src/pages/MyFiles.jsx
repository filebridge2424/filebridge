import { useState, useMemo, useCallback } from 'react';
import { Files, Upload, RefreshCw, AlertTriangle } from 'lucide-react';
import { useFiles, useStorageUsage } from '../hooks/useFiles.js';
import FileList from '../components/FileList.jsx';
import UploadZone from '../components/UploadZone.jsx';
import StorageUsageBar from '../components/StorageUsageBar.jsx';
import SearchBar from '../components/SearchBar.jsx';

export default function MyFiles() {
  const [showUpload, setShowUpload] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);

  const { files, loading, error, refresh } = useFiles();
  const { usage, loading: usageLoading } = useStorageUsage(refreshKey);

  const handleUploadComplete = useCallback(() => {
    refresh();
    setRefreshKey((k) => k + 1);
  }, [refresh]);

  const handleDelete = useCallback((deletedId) => {
    refresh();
    setRefreshKey((k) => k + 1);
  }, [refresh]);

  // Client-side search filtering
  const filteredFiles = useMemo(() => {
    if (!searchQuery.trim()) return files;
    const q = searchQuery.toLowerCase();
    return files.filter((f) =>
      f.originalFilename?.toLowerCase().includes(q) ||
      f.contentType?.toLowerCase().includes(q)
    );
  }, [files, searchQuery]);

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">My Files</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {loading ? 'Loading...' : `${files.length} file${files.length !== 1 ? 's' : ''}`}
          </p>
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          <button
            onClick={refresh}
            disabled={loading}
            className="btn-secondary"
            title="Refresh"
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refresh</span>
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

      {/* Upload zone */}
      {showUpload && (
        <div className="card p-4">
          <h2 className="text-sm font-semibold text-slate-700 mb-3">Upload Files</h2>
          <UploadZone onUploadComplete={handleUploadComplete} />
        </div>
      )}

      {/* Error state */}
      {error && (
        <div className="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700">
          <AlertTriangle size={20} className="flex-shrink-0" />
          <div>
            <p className="text-sm font-medium">Failed to load files</p>
            <p className="text-xs mt-0.5">{error}</p>
          </div>
          <button onClick={refresh} className="ml-auto btn-secondary text-xs">
            Try again
          </button>
        </div>
      )}

      {/* File list section */}
      {!error && (
        <div className="space-y-3">
          {/* Search and count */}
          <div className="flex items-center gap-3">
            <div className="flex-1 max-w-sm">
              <SearchBar
                value={searchQuery}
                onChange={setSearchQuery}
                placeholder="Search files by name or type..."
              />
            </div>
            {searchQuery && (
              <p className="text-sm text-slate-500 flex-shrink-0">
                {filteredFiles.length} result{filteredFiles.length !== 1 ? 's' : ''}
              </p>
            )}
          </div>

          {/* Loading skeleton */}
          {loading && (
            <div className="card overflow-hidden">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="flex items-center gap-3 px-4 py-3 border-b border-slate-100 last:border-0 animate-pulse">
                  <div className="w-9 h-9 rounded-lg bg-slate-100" />
                  <div className="flex-1 space-y-1.5">
                    <div className="h-3.5 bg-slate-100 rounded w-48" />
                    <div className="h-3 bg-slate-100 rounded w-24" />
                  </div>
                  <div className="h-3 bg-slate-100 rounded w-16" />
                  <div className="h-3 bg-slate-100 rounded w-20" />
                </div>
              ))}
            </div>
          )}

          {/* Empty state — no files at all */}
          {!loading && files.length === 0 && (
            <div className="card flex flex-col items-center justify-center py-16 text-center">
              <div className="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mb-4">
                <Files size={28} className="text-slate-400" />
              </div>
              <h3 className="text-base font-semibold text-slate-700">No files yet</h3>
              <p className="text-sm text-slate-500 mt-1 max-w-xs">
                Upload your first file using the button above to get started.
              </p>
              <button
                onClick={() => setShowUpload(true)}
                className="btn-primary mt-4"
              >
                <Upload size={16} />
                Upload Files
              </button>
            </div>
          )}

          {/* Empty search results */}
          {!loading && files.length > 0 && filteredFiles.length === 0 && (
            <div className="card flex flex-col items-center justify-center py-12 text-center">
              <p className="text-base font-medium text-slate-600">No files match "{searchQuery}"</p>
              <p className="text-sm text-slate-500 mt-1">Try a different search term</p>
              <button onClick={() => setSearchQuery('')} className="btn-secondary mt-3 text-sm">
                Clear search
              </button>
            </div>
          )}

          {/* File list */}
          {!loading && filteredFiles.length > 0 && (
            <FileList
              files={filteredFiles}
              onDelete={handleDelete}
            />
          )}
        </div>
      )}
    </div>
  );
}
