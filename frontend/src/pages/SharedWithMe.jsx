import { useState, useEffect } from 'react';
import {
  Users, Eye, Download, ShieldCheck, ShieldAlert,
  File, FileText, FileImage, FileVideo, FileAudio, FileArchive,
  Loader2, ArrowRight
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { sharesApi } from '../api/filesApi.js';
import { formatBytes, formatRelativeDate, getFileTypeInfo } from '../utils/format.js';
import FilePreviewModal from '../components/FilePreviewModal.jsx';

function FileTypeIcon({ contentType, size = 16 }) {
  if (!contentType) return <File size={size} />;
  const t = contentType.toLowerCase();
  if (t.startsWith('image/')) return <FileImage size={size} />;
  if (t.startsWith('video/')) return <FileVideo size={size} />;
  if (t.startsWith('audio/')) return <FileAudio size={size} />;
  if (t === 'application/pdf') return <FileText size={size} />;
  if (t.includes('zip') || t.includes('tar') || t.includes('gzip') || t.includes('rar')) {
    return <FileArchive size={size} />;
  }
  return <FileText size={size} />;
}

export default function SharedWithMe() {
  const [sharedFiles, setSharedFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [previewFile, setPreviewFile] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [error, setError] = useState(null);

  const fetchShared = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await sharesApi.getSharedWithMe();
      setSharedFiles(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load shared files.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShared();
  }, []);

  const handleDownload = async (share) => {
    if (share.permission !== 'DOWNLOAD') return;
    try {
      setDownloadingId(share.shareId);
      await sharesApi.downloadShared(share.shareId, share.filename);
    } catch (err) {
      alert(err.message || 'Download failed.');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Shared with Me</h1>
          <p className="text-sm text-slate-500 mt-0.5">Files other registered users have shared with you</p>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-xs text-red-600">
          {error}
        </div>
      )}

      {loading && (
        <div className="card divide-y divide-slate-100 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="p-4 flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-slate-100" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-slate-100 rounded w-1/4" />
                <div className="h-3 bg-slate-100 rounded w-1/3" />
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && sharedFiles.length === 0 && (
        <div className="card flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mb-4">
            <Users size={28} className="text-slate-400" />
          </div>
          <h3 className="text-base font-semibold text-slate-700">No shared files yet</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm">
            When someone shares a file with your email address, it will automatically appear here.
          </p>
          <Link to="/files" className="btn-secondary mt-5">
            Go to My Files
          </Link>
        </div>
      )}

      {!loading && sharedFiles.length > 0 && (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-100 text-slate-500 font-medium">
                <tr>
                  <th className="py-3 px-4">File Name</th>
                  <th className="py-3 px-4">Shared By</th>
                  <th className="py-3 px-4">Permission</th>
                  <th className="py-3 px-4">Size</th>
                  <th className="py-3 px-4">Shared On</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {sharedFiles.map((share) => {
                  const { color } = getFileTypeInfo(share.contentType);
                  const canDownload = share.permission === 'DOWNLOAD';

                  // Map to preview object structure
                  const fileForPreview = {
                    id: share.fileId,
                    originalFilename: share.filename,
                    contentType: share.contentType,
                    sizeBytes: share.sizeBytes,
                    uploadedAt: share.sharedAt,
                    canDownload: share.permission === 'DOWNLOAD'
                  };

                  return (
                    <tr key={share.shareId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3 px-4 font-medium text-slate-900">
                        <div className="flex items-center gap-3">
                          <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 ${color}`}>
                            <FileTypeIcon contentType={share.contentType} size={16} />
                          </div>
                          <span className="truncate max-w-xs">{share.filename}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-slate-600">
                        <div>
                          <p className="font-medium text-slate-800">{share.ownerName || 'User'}</p>
                          <p className="text-[11px] text-slate-400">{share.ownerEmail}</p>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        {canDownload ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <ShieldCheck size={12} />
                            View & Download
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                            <ShieldAlert size={12} />
                            View Only
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-slate-500">{formatBytes(share.sizeBytes)}</td>
                      <td className="py-3 px-4 text-slate-500">{formatRelativeDate(share.sharedAt)}</td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => setPreviewFile(fileForPreview)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-teal-600 hover:bg-teal-50 transition-colors"
                            title="Preview file"
                          >
                            <Eye size={16} />
                          </button>
                          {canDownload ? (
                            <button
                              type="button"
                              onClick={() => handleDownload(share)}
                              disabled={downloadingId === share.shareId}
                              className="p-1.5 rounded-lg text-slate-500 hover:text-teal-600 hover:bg-teal-50 transition-colors disabled:opacity-50"
                              title="Download file"
                            >
                              {downloadingId === share.shareId ? (
                                <Loader2 size={16} className="animate-spin text-teal-600" />
                              ) : (
                                <Download size={16} />
                              )}
                            </button>
                          ) : (
                            <span
                              className="p-1.5 text-slate-300 cursor-not-allowed"
                              title="Download restricted by owner (View Only)"
                            >
                              <Download size={16} />
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {previewFile && (
        <FilePreviewModal
          file={previewFile}
          onClose={() => setPreviewFile(null)}
        />
      )}
    </div>
  );
}
