import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Download,
  ShieldAlert,
  File,
  FileText,
  FileImage,
  FileVideo,
  FileAudio,
  FileArchive,
  AlertCircle,
  Loader2
} from 'lucide-react';
import { sharesApi } from '../api/filesApi.js';
import { formatBytes, getFileTypeInfo } from '../utils/format.js';

function FileTypeIcon({ contentType, size = 24 }) {
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

export default function PublicShareView() {
  const { token } = useParams();
  const [share, setShare] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [downloading, setDownloading] = useState(false);
  const [previewBlobUrl, setPreviewBlobUrl] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  useEffect(() => {
    if (!token) return;
    sharesApi.getPublicShare(token)
      .then((data) => {
        setShare(data);
        // Automatically attempt preview if supported
        loadPreview(token);
      })
      .catch((err) => {
        setError(err.message || 'Share link is invalid, expired, or has been revoked.');
      })
      .finally(() => setLoading(false));
  }, [token]);

  const loadPreview = async (shareToken) => {
    try {
      setPreviewLoading(true);
      const url = await sharesApi.getPublicPreviewBlob(shareToken);
      setPreviewBlobUrl(url);
    } catch {
      // Inline preview not available or not supported format
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!share || share.permission !== 'DOWNLOAD') return;
    try {
      setDownloading(true);
      await sharesApi.downloadPublic(token, share.fileName);
    } catch (err) {
      alert(err.message || 'Download failed.');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="text-center space-y-3">
          <Loader2 size={32} className="animate-spin text-teal-600 mx-auto" />
          <p className="text-xs text-slate-500 font-medium">Loading shared file...</p>
        </div>
      </div>
    );
  }

  if (error || !share) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl shadow-xl border border-slate-200 p-8 max-w-md w-full text-center space-y-4">
          <div className="w-14 h-14 bg-red-50 text-red-500 rounded-2xl flex items-center justify-center mx-auto">
            <AlertCircle size={28} />
          </div>
          <h2 className="text-lg font-bold text-slate-800">File Unavailable</h2>
          <p className="text-xs text-slate-500">{error || 'This link is invalid or has expired.'}</p>
          <Link to="/login" className="btn-primary inline-flex text-xs px-5 py-2.5 mt-2">
            Go to FileBridge
          </Link>
        </div>
      </div>
    );
  }

  const { color } = getFileTypeInfo(share.fileContentType);
  const isImage = (share.fileContentType || '').toLowerCase().startsWith('image/');
  const isPdf = (share.fileContentType || '').toLowerCase() === 'application/pdf';
  const canDownload = share.permission === 'DOWNLOAD';

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-4 sm:p-8 flex flex-col items-center">
      {/* Top Navbar */}
      <div className="w-full max-w-4xl flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-teal-600 flex items-center justify-center text-white font-black text-sm shadow-md shadow-teal-600/20">
            FB
          </div>
          <span className="font-bold text-slate-800 text-sm tracking-tight">FileBridge</span>
        </div>
        <Link to="/login" className="btn-secondary text-xs">
          Sign In
        </Link>
      </div>

      {/* Main Container */}
      <div className="bg-white rounded-3xl shadow-xl border border-slate-200/80 w-full max-w-4xl overflow-hidden">
        {/* Header bar */}
        <div className="p-6 sm:p-8 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-50/50">
          <div className="flex items-center gap-4">
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center flex-shrink-0 ${color}`}>
              <FileTypeIcon contentType={share.fileContentType} size={24} />
            </div>
            <div>
              <h1 className="text-base sm:text-lg font-bold text-slate-900 break-all">
                {share.fileName}
              </h1>
              <p className="text-xs text-slate-500 mt-0.5">
                Shared by <span className="font-semibold text-slate-700">{share.ownerName || 'User'}</span> ({share.ownerEmail}) • {formatBytes(share.fileSize)}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {canDownload ? (
              <button
                onClick={handleDownload}
                disabled={downloading}
                className="btn-primary text-xs flex items-center gap-2 px-5 py-2.5 shadow-md shadow-teal-600/10"
              >
                {downloading ? <Loader2 size={16} className="animate-spin" /> : <Download size={16} />}
                Download File
              </button>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-50 text-amber-700 border border-amber-200 text-xs font-semibold">
                <ShieldAlert size={14} />
                View Only Access
              </span>
            )}
          </div>
        </div>

        {/* Preview Area */}
        <div className="p-6 sm:p-8 bg-slate-900/5 min-h-[400px] flex items-center justify-center">
          {previewLoading ? (
            <div className="text-center space-y-2">
              <Loader2 size={24} className="animate-spin text-teal-600 mx-auto" />
              <p className="text-xs text-slate-500">Preparing preview...</p>
            </div>
          ) : previewBlobUrl && isImage ? (
            <img
              src={previewBlobUrl}
              alt={share.fileName}
              className="max-h-[600px] max-w-full rounded-2xl shadow-lg object-contain bg-white"
            />
          ) : previewBlobUrl && isPdf ? (
            <iframe
              src={previewBlobUrl}
              title={share.fileName}
              className="w-full h-[650px] rounded-2xl shadow-lg border border-slate-200 bg-white"
            />
          ) : (
            <div className="text-center py-12 space-y-3">
              <div className={`w-16 h-16 rounded-2xl flex items-center justify-center mx-auto ${color}`}>
                <FileTypeIcon contentType={share.fileContentType} size={32} />
              </div>
              <h3 className="text-sm font-semibold text-slate-800">Preview not available</h3>
              <p className="text-xs text-slate-500 max-w-xs mx-auto">
                {canDownload
                  ? 'Click "Download File" above to save and view this document on your device.'
                  : 'This file format cannot be displayed directly in the browser.'}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
