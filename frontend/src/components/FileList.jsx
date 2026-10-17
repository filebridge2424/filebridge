import { useState } from 'react';
import {
  Download, Trash2, File, FileImage, FileText, FileVideo,
  FileAudio, FileArchive, FileCode, FileSpreadsheet,
  MoreVertical, ChevronUp, ChevronDown
} from 'lucide-react';
import { formatBytes, formatRelativeDate, getFileTypeInfo, sortFiles } from '../utils/format.js';
import { filesApi } from '../api/filesApi.js';

function FileTypeIcon({ contentType, size = 20 }) {
  if (!contentType) return <File size={size} />;
  const t = contentType.toLowerCase();
  if (t.startsWith('image/'))     return <FileImage size={size} />;
  if (t.startsWith('video/'))     return <FileVideo size={size} />;
  if (t.startsWith('audio/'))     return <FileAudio size={size} />;
  if (t === 'application/pdf')    return <FileText size={size} />;
  if (t.includes('zip') || t.includes('tar') || t.includes('gzip') || t.includes('rar'))
                                  return <FileArchive size={size} />;
  if (t.includes('spreadsheet') || t.includes('excel') || t === 'text/csv')
                                  return <FileSpreadsheet size={size} />;
  if (t.startsWith('text/'))      return <FileText size={size} />;
  if (t.includes('json') || t.includes('javascript') || t.includes('typescript'))
                                  return <FileCode size={size} />;
  return <File size={size} />;
}

function SortHeader({ field, label, sortField, sortDir, onSort }) {
  const active = sortField === field;
  return (
    <button
      onClick={() => onSort(field)}
      className={`flex items-center gap-1 text-xs font-semibold uppercase tracking-wide transition-colors ${
        active ? 'text-teal-600' : 'text-slate-400 hover:text-slate-600'
      }`}
    >
      {label}
      <span className={`transition-opacity ${active ? 'opacity-100' : 'opacity-0'}`}>
        {sortDir === 'asc' ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
      </span>
    </button>
  );
}

export default function FileList({ files, onDelete, onDownload }) {
  const [sortField, setSortField] = useState('uploadedAt');
  const [sortDir, setSortDir] = useState('desc');
  const [deletingId, setDeletingId] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDir((d) => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDir('asc');
    }
  };

  const handleDelete = async (file) => {
    if (!window.confirm(`Delete "${file.originalFilename}"?\n\nThis action cannot be undone.`)) return;
    setDeletingId(file.id);
    setDeleteError(null);
    try {
      await filesApi.delete(file.id);
      onDelete?.(file.id);
    } catch (err) {
      setDeleteError(`Failed to delete "${file.originalFilename}": ${err.message}`);
    } finally {
      setDeletingId(null);
    }
  };

  const handleDownload = async (file) => {
    setDownloadingId(file.id);
    try {
      await filesApi.download(file.id, file.originalFilename);
    } catch (err) {
      alert(`Download failed: ${err.message}`);
    } finally {
      setDownloadingId(null);
    }
  };

  const sorted = sortFiles(files, sortField, sortDir);

  return (
    <div className="space-y-2">
      {/* Delete error banner */}
      {deleteError && (
        <div className="flex items-center gap-2 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          <span className="flex-1">{deleteError}</span>
          <button onClick={() => setDeleteError(null)} className="text-red-400 hover:text-red-600">✕</button>
        </div>
      )}

      {/* Table */}
      <div className="card overflow-hidden">
        {/* Header */}
        <div className="hidden sm:grid grid-cols-12 gap-4 px-4 py-3 border-b border-slate-100 bg-slate-50">
          <div className="col-span-5">
            <SortHeader field="originalFilename" label="Name" sortField={sortField} sortDir={sortDir} onSort={handleSort} />
          </div>
          <div className="col-span-2">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">Type</span>
          </div>
          <div className="col-span-2">
            <SortHeader field="sizeBytes" label="Size" sortField={sortField} sortDir={sortDir} onSort={handleSort} />
          </div>
          <div className="col-span-2">
            <SortHeader field="uploadedAt" label="Uploaded" sortField={sortField} sortDir={sortDir} onSort={handleSort} />
          </div>
          <div className="col-span-1" />
        </div>

        {/* Rows */}
        <ul className="divide-y divide-slate-100">
          {sorted.map((file) => {
            const { color, label } = getFileTypeInfo(file.contentType);
            const isDeleting = deletingId === file.id;
            const isDownloading = downloadingId === file.id;

            return (
              <li
                key={file.id}
                className={`group flex sm:grid sm:grid-cols-12 sm:gap-4 items-center px-4 py-3 hover:bg-slate-50 transition-colors duration-100 ${
                  isDeleting ? 'opacity-50 pointer-events-none' : ''
                }`}
              >
                {/* Name + icon */}
                <div className="col-span-5 flex items-center gap-3 min-w-0 flex-1">
                  <div className={`flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center ${color}`}>
                    <FileTypeIcon contentType={file.contentType} size={18} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-800 truncate" title={file.originalFilename}>
                      {file.originalFilename}
                    </p>
                    {/* Mobile secondary info */}
                    <p className="sm:hidden text-xs text-slate-500">
                      {formatBytes(file.sizeBytes)} · {formatRelativeDate(file.uploadedAt)}
                    </p>
                  </div>
                </div>

                {/* Type badge */}
                <div className="col-span-2 hidden sm:flex">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium ${color}`}>
                    {label}
                  </span>
                </div>

                {/* Size */}
                <div className="col-span-2 hidden sm:block">
                  <span className="text-sm text-slate-600">{formatBytes(file.sizeBytes)}</span>
                </div>

                {/* Date */}
                <div className="col-span-2 hidden sm:block">
                  <span className="text-sm text-slate-500">{formatRelativeDate(file.uploadedAt)}</span>
                </div>

                {/* Actions */}
                <div className="col-span-1 flex items-center gap-1 flex-shrink-0 ml-auto sm:ml-0">
                  <button
                    onClick={() => handleDownload(file)}
                    disabled={isDownloading}
                    title="Download"
                    className="p-2 rounded-lg text-slate-400 hover:text-teal-600 hover:bg-teal-50 transition-colors disabled:opacity-50"
                  >
                    {isDownloading ? (
                      <span className="inline-block w-4 h-4 border-2 border-teal-400 border-t-transparent rounded-full animate-spin" />
                    ) : (
                      <Download size={16} />
                    )}
                  </button>
                  <button
                    onClick={() => handleDelete(file)}
                    disabled={isDeleting}
                    title="Delete"
                    className="p-2 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50"
                  >
                    {isDeleting ? (
                      <span className="inline-block w-4 h-4 border-2 border-red-400 border-t-transparent rounded-full animate-spin" />
                    ) : (
                      <Trash2 size={16} />
                    )}
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
