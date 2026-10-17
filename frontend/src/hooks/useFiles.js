import { useState, useEffect, useCallback } from 'react';
import { filesApi } from '../api/filesApi.js';

/**
 * Hook to manage file list state with loading, error, and refresh.
 */
export function useFiles() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchFiles = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await filesApi.list();
      setFiles(data);
    } catch (err) {
      setError(err.message || 'Failed to load files');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFiles();
  }, [fetchFiles]);

  return { files, loading, error, refresh: fetchFiles };
}

/**
 * Hook to manage storage usage state.
 */
export function useStorageUsage(refreshTrigger) {
  const [usage, setUsage] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchUsage = useCallback(async () => {
    setLoading(true);
    try {
      const data = await filesApi.storageUsage();
      setUsage(data);
    } catch {
      // Non-critical — don't show error for usage failures
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsage();
  }, [fetchUsage, refreshTrigger]);

  return { usage, loading, refresh: fetchUsage };
}
