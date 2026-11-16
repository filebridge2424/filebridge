import { useState, useEffect } from 'react';
import { X, Download, FileText, AlertCircle, Loader2, EyeOff } from 'lucide-react';
import { formatBytes, formatRelativeDate, getFileTypeInfo } from '../utils/format.js';
import { filesApi } from '../api/filesApi.js';

export default function FilePreviewModal({ file, onClose }) {
  const [blobUrl, setBlobUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const contentType = file?.contentType || '';
  const isImage = contentType.startsWith('image/');
  const isPdf = contentType === 'application/pdf';
  const isSupported = isImage || isPdf;

  useEffect(() => {
    if (!file || !isSupported) {
      setLoading(false);
      return;
    }

    let active = true;
    let createdUrl = null;

    async function loadPreview() {
      setLoading(true);
      setError(null);
      try {
        const blob = await filesApi.getPreviewBlob(file.id);
        if (active) {
          createdUrl = URL.createObjectURL(blob);
          setBlobUrl(createdUrl);
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Failed to load file preview.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadPreview();

    return () => {
      active = false;
      if (createdUrl) {
        URL.revokeObjectURL(createdUrl);
      }
    };
  }, [file, isSupported]);

  if (!file) return null;

  const canDownload = file.canDownload !== false;

  const handleDownload = () => {
    if (!canDownload) return;
    filesApi.download(file.id, file.originalFilename);
  };

  const { color, label } = getFileTypeInfo(file.contentType);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-navy-950/70 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl border border-slate-200/80 w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3 min-w-0">
            <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${color}`}>
              {label}
            </span>
            <h2 className="text-base font-semibold text-slate-800 truncate" title={file.originalFilename}>
              {file.originalFilename}
            </h2>
            <span className="text-xs text-slate-400 hidden sm:inline">
              ({formatBytes(file.sizeBytes)})
            </span>
          </div>

          <div className="flex items-center gap-2 flex-shrink-0 ml-4">
            {canDownload ? (
              <button
                onClick={handleDownload}
                className="btn-secondary text-xs py-1.5 px-3 flex items-center gap-1.5"
                title="Download file"
              >
                <Download size={14} />
                <span className="hidden sm:inline">Download</span>
              </button>
            ) : (
              <span className="text-[11px] font-medium text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-md">
                View Only
              </span>
            )}
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
              title="Close preview"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Preview Content */}
        <div className="flex-1 overflow-auto p-4 flex items-center justify-center bg-slate-100/50 min-h-[300px]">
          {loading && (
            <div className="flex flex-col items-center gap-3 text-slate-500 py-12">
              <Loader2 size={32} className="animate-spin text-teal-600" />
              <p className="text-sm font-medium">Loading preview...</p>
            </div>
          )}

          {!loading && error && (
            <div className="flex flex-col items-center gap-3 text-red-600 py-12 text-center max-w-md">
              <AlertCircle size={36} />
              <p className="text-sm font-semibold">Unable to load preview</p>
              <p className="text-xs text-slate-500">{error}</p>
              <button onClick={handleDownload} className="btn-primary mt-3 text-xs">
                <Download size={14} />
                Download File
              </button>
            </div>
          )}

          {!loading && !error && isImage && blobUrl && (
            <div className="flex items-center justify-center max-h-[70vh] w-full">
              <img
                src={blobUrl}
                alt={file.originalFilename}
                className="max-h-[70vh] max-w-full object-contain rounded-lg shadow-sm"
              />
            </div>
          )}

          {!loading && !error && isPdf && blobUrl && (
            <div className="w-full h-[72vh] rounded-lg overflow-hidden border border-slate-200 bg-white">
              <iframe
                src={blobUrl}
                title={file.originalFilename}
                className="w-full h-full border-0"
              />
            </div>
          )}

          {!loading && !isSupported && (
            <div className="card max-w-md p-8 text-center flex flex-col items-center gap-4 bg-white shadow-sm border border-slate-200">
              <div className="w-16 h-16 rounded-2xl bg-amber-50 border border-amber-200/60 flex items-center justify-center text-amber-600">
                <EyeOff size={30} />
              </div>

              <div>
                <h3 className="text-base font-semibold text-slate-800">Preview Not Available</h3>
                <p className="text-sm text-slate-500 mt-1">
                  Preview is only supported for image files (PNG, JPG, SVG, WebP) and PDF documents.
                </p>
              </div>

              <div className="w-full bg-slate-50 rounded-xl p-3 text-left space-y-1.5 text-xs text-slate-600 border border-slate-100">
                <div className="flex justify-between">
                  <span className="text-slate-400">File Name:</span>
                  <span className="font-medium text-slate-700 truncate max-w-[200px]">{file.originalFilename}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">File Size:</span>
                  <span className="font-medium text-slate-700">{formatBytes(file.sizeBytes)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Type:</span>
                  <span className="font-medium text-slate-700">{file.contentType || 'Unknown'}</span>
                </div>
                {file.uploadedAt && (
                  <div className="flex justify-between">
                    <span className="text-slate-400">Uploaded:</span>
                    <span className="font-medium text-slate-700">{formatRelativeDate(file.uploadedAt)}</span>
                  </div>
                )}
              </div>

              <button onClick={handleDownload} className="btn-primary w-full justify-center">
                <Download size={16} />
                Download to View
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
