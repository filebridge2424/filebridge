import { useState } from 'react';
import {
  Download, Trash2, File, FileImage, FileText, FileVideo,
  FileAudio, FileArchive, FileCode, FileSpreadsheet,
  ChevronUp, ChevronDown, Eye, Edit2, FolderInput, MoreVertical, Share2
} from 'lucide-react';
import { formatBytes, formatRelativeDate, getFileTypeInfo, sortFiles } from '../utils/format.js';
import { filesApi } from '../api/filesApi.js';
import ShareModal from './ShareModal.jsx';

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

export default function FileList({ files, onDelete, onRename, onMove, onPreview, onShare }) {
  const [sortField, setSortField] = useState('uploadedAt');
  const [sortDir, setSortDir] = useState('desc');
  const [deletingId, setDeletingId] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [activeMenuId, setActiveMenuId] = useState(null);
  const [sharingFile, setSharingFile] = useState(null);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('asc');
    }
  };

  const handleDelete = async (file) => {
    if (!window.confirm(`Move "${file.originalFilename}" to Trash?\n\nYou can restore it later from Trash.`)) return;
    setDeletingId(file.id);
    try {
      await filesApi.delete(file.id, false);
      onDelete?.(file.id);
    } catch (err) {
      alert(`Failed to delete "${file.originalFilename}": ${err.message}`);
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
      {/* Table */}
      <div className="card overflow-visible">
        {/* Header */}
        <div className="hidden sm:grid grid-cols-12 gap-4 px-4 py-3 border-b border-slate-100 bg-slate-50/70 rounded-t-xl">
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
          <div className="col-span-1 text-right">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">Actions</span>
          </div>
        </div>

        {/* Rows */}
        <ul className="divide-y divide-slate-100">
          {sorted.map((file) => {
            const { color, label } = getFileTypeInfo(file.contentType);
            const isDeleting = deletingId === file.id;
            const isDownloading = downloadingId === file.id;
            const isMenuOpen = activeMenuId === file.id;

            return (
              <li
                key={file.id}
                className={`group flex sm:grid sm:grid-cols-12 sm:gap-4 items-center px-4 py-3 hover:bg-slate-50/80 transition-colors duration-100 ${
                  isDeleting ? 'opacity-50 pointer-events-none' : ''
                }`}
              >
                {/* Name + icon */}
                <div
                  className="col-span-5 flex items-center gap-3 min-w-0 flex-1 cursor-pointer"
                  onClick={() => onPreview?.(file)}
                  title="Click to preview file"
                >
                  <div className={`flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center ${color}`}>
                    <FileTypeIcon contentType={file.contentType} size={18} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-800 truncate group-hover:text-teal-600 transition-colors">
                      {file.originalFilename}
                    </p>
                    {/* Mobile secondary info */}
                    <p className="sm:hidden text-xs text-slate-500">
                      {formatBytes(file.sizeBytes)} • {formatRelativeDate(file.uploadedAt)}
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
                <div className="col-span-1 flex items-center justify-end gap-1 flex-shrink-0 ml-auto sm:ml-0 relative">
                  {/* Quick Preview Button */}
                  <button
                    onClick={() => onPreview?.(file)}
                    title="Preview file"
                    className="p-1.5 rounded-lg text-slate-400 hover:text-teal-600 hover:bg-teal-50 transition-colors hidden md:inline-flex"
                  >
                    <Eye size={16} />
                  </button>

                  {/* Quick Download Button */}
                  <button
                    onClick={() => handleDownload(file)}
                    disabled={isDownloading}
                    title="Download"
                    className="p-1.5 rounded-lg text-slate-400 hover:text-teal-600 hover:bg-teal-50 transition-colors disabled:opacity-50"
                  >
                    {isDownloading ? (
                      <span className="inline-block w-4 h-4 border-2 border-teal-400 border-t-transparent rounded-full animate-spin" />
                    ) : (
                      <Download size={16} />
                    )}
                  </button>

                  {/* Context menu for Rename, Move, Delete */}
                  <div className="relative">
                    <button
                      onClick={() => setActiveMenuId(isMenuOpen ? null : file.id)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                      title="More actions"
                    >
                      <MoreVertical size={16} />
                    </button>

                    {isMenuOpen && (
                      <>
                        <div
                          className="fixed inset-0 z-20"
                          onClick={() => setActiveMenuId(null)}
                        />
                        <div className="absolute right-0 top-7 z-30 w-40 bg-white rounded-xl shadow-lg border border-slate-100 py-1 animate-scale-up text-xs">
                          <button
                            onClick={() => {
                              setActiveMenuId(null);
                              onPreview?.(file);
                            }}
                            className="w-full flex items-center gap-2 px-3 py-2 text-slate-700 hover:bg-slate-50 hover:text-teal-600 text-left"
                          >
                            <Eye size={14} />
                            <span>Preview</span>
                          </button>
                          <button
                            onClick={() => {
                              setActiveMenuId(null);
                              if (onShare) {
                                onShare(file);
                              } else {
                                setSharingFile(file);
                              }
                            }}
                            className="w-full flex items-center gap-2 px-3 py-2 text-slate-700 hover:bg-slate-50 hover:text-teal-600 text-left"
                          >
                            <Share2 size={14} />
                            <span>Share</span>
                          </button>
                          <button
                            onClick={() => {
                              setActiveMenuId(null);
                              onRename?.(file);
                            }}
                            className="w-full flex items-center gap-2 px-3 py-2 text-slate-700 hover:bg-slate-50 hover:text-teal-600 text-left"
                          >
                            <Edit2 size={14} />
                            <span>Rename</span>
                          </button>
                          <button
                            onClick={() => {
                              setActiveMenuId(null);
                              onMove?.(file);
                            }}
                            className="w-full flex items-center gap-2 px-3 py-2 text-slate-700 hover:bg-slate-50 hover:text-teal-600 text-left"
                          >
                            <FolderInput size={14} />
                            <span>Move to Folder</span>
                          </button>
                          <div className="border-t border-slate-100 my-1" />
                          <button
                            onClick={() => {
                              setActiveMenuId(null);
                              handleDelete(file);
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
              </li>
            );
          })}
        </ul>
      </div>

      {sharingFile && (
        <ShareModal
          file={sharingFile}
          onClose={() => setSharingFile(null)}
        />
      )}
    </div>
  );
}
