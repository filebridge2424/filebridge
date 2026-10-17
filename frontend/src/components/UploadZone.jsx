import { useCallback, useRef, useState } from 'react';
import { Upload, X, File, AlertCircle, CheckCircle } from 'lucide-react';
import { formatBytes } from '../utils/format.js';
import { filesApi } from '../api/filesApi.js';

const MAX_FILE_SIZE_DISPLAY = 100 * 1024 * 1024; // 100 MB label (matches server config)

export default function UploadZone({ onUploadComplete }) {
  const [isDragging, setIsDragging] = useState(false);
  const [uploads, setUploads] = useState([]); // [{ file, progress, status, error }]
  const inputRef = useRef();

  const handleFiles = useCallback((files) => {
    const fileList = Array.from(files);
    if (!fileList.length) return;

    const newUploads = fileList.map((file) => ({
      id: `${file.name}-${Date.now()}-${Math.random()}`,
      file,
      progress: 0,
      status: 'pending', // pending | uploading | done | error
      error: null,
    }));

    setUploads((prev) => [...prev, ...newUploads]);

    // Upload each file
    newUploads.forEach((uploadItem) => {
      uploadSingle(uploadItem);
    });
  }, []);

  const uploadSingle = async (uploadItem) => {
    const { id, file } = uploadItem;

    if (file.size === 0) {
      setUploads((prev) =>
        prev.map((u) => u.id === id
          ? { ...u, status: 'error', error: 'File is empty.' }
          : u
        )
      );
      return;
    }

    setUploads((prev) =>
      prev.map((u) => u.id === id ? { ...u, status: 'uploading' } : u)
    );

    try {
      await filesApi.upload(file, (progress) => {
        setUploads((prev) =>
          prev.map((u) => u.id === id ? { ...u, progress } : u)
        );
      });

      setUploads((prev) =>
        prev.map((u) => u.id === id ? { ...u, status: 'done', progress: 100 } : u)
      );

      onUploadComplete?.();
    } catch (err) {
      setUploads((prev) =>
        prev.map((u) => u.id === id
          ? { ...u, status: 'error', error: err.message }
          : u
        )
      );
    }
  };

  const removeUpload = (id) => {
    setUploads((prev) => prev.filter((u) => u.id !== id));
  };

  const clearDone = () => {
    setUploads((prev) => prev.filter((u) => u.status !== 'done'));
  };

  const hasDone = uploads.some((u) => u.status === 'done');

  // Drag events
  const onDragOver = useCallback((e) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const onDragLeave = useCallback((e) => {
    if (!e.currentTarget.contains(e.relatedTarget)) {
      setIsDragging(false);
    }
  }, []);

  const onDrop = useCallback((e) => {
    e.preventDefault();
    setIsDragging(false);
    handleFiles(e.dataTransfer.files);
  }, [handleFiles]);

  return (
    <div className="space-y-3">
      {/* Drop zone */}
      <div
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        className={`
          relative flex flex-col items-center justify-center gap-2 p-6 rounded-xl border-2 border-dashed
          cursor-pointer transition-all duration-150 select-none
          ${isDragging
            ? 'border-teal-500 bg-teal-50 drop-active'
            : 'border-slate-300 hover:border-teal-400 hover:bg-teal-50/30'
          }
        `}
      >
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => handleFiles(e.target.files)}
          onClick={(e) => e.stopPropagation()}
        />
        <div className={`p-3 rounded-full transition-colors ${isDragging ? 'bg-teal-100' : 'bg-slate-100'}`}>
          <Upload size={22} className={isDragging ? 'text-teal-600' : 'text-slate-500'} />
        </div>
        <div className="text-center">
          <p className="text-sm font-medium text-slate-700">
            {isDragging ? 'Drop files here' : 'Drop files here or click to browse'}
          </p>
          <p className="text-xs text-slate-500 mt-0.5">Up to {formatBytes(MAX_FILE_SIZE_DISPLAY)} per file</p>
        </div>
      </div>

      {/* Upload queue */}
      {uploads.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">Upload queue</p>
            {hasDone && (
              <button
                onClick={clearDone}
                className="text-xs text-teal-600 hover:text-teal-700 font-medium"
              >
                Clear completed
              </button>
            )}
          </div>

          {uploads.map((u) => (
            <div key={u.id} className="flex items-center gap-3 p-3 bg-white rounded-lg border border-slate-200">
              <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center">
                <File size={16} className="text-slate-500" />
              </div>

              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-slate-800 truncate">{u.file.name}</p>
                <p className="text-xs text-slate-500">{formatBytes(u.file.size)}</p>

                {u.status === 'uploading' && (
                  <div className="mt-1.5 w-full bg-slate-100 rounded-full h-1.5">
                    <div
                      className="h-1.5 bg-teal-500 rounded-full transition-all duration-100"
                      style={{ width: `${u.progress}%` }}
                    />
                  </div>
                )}

                {u.status === 'error' && (
                  <p className="text-xs text-red-600 mt-0.5 flex items-center gap-1">
                    <AlertCircle size={11} />
                    {u.error}
                  </p>
                )}
              </div>

              <div className="flex items-center gap-2 flex-shrink-0">
                {u.status === 'uploading' && (
                  <span className="text-xs text-slate-500 tabular-nums">{u.progress}%</span>
                )}
                {u.status === 'done' && (
                  <CheckCircle size={18} className="text-teal-500" />
                )}
                {u.status === 'error' && (
                  <AlertCircle size={18} className="text-red-500" />
                )}
                {u.status !== 'uploading' && (
                  <button
                    onClick={() => removeUpload(u.id)}
                    className="p-1 text-slate-400 hover:text-slate-600 rounded"
                  >
                    <X size={14} />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
