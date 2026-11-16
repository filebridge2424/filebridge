const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// Token storage key
const TOKEN_KEY = 'filebridge_auth_token';

export const tokenStorage = {
  get: () => {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set: (token) => {
    try {
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
    } catch {
      // ignore
    }
  },
  clear: () => {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      // ignore
    }
  }
};

let onAuthErrorCallback = null;
export function setAuthErrorHandler(callback) {
  onAuthErrorCallback = callback;
}

async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`;

  const headers = {
    ...(options.headers || {})
  };

  const token = tokenStorage.get();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // If body is plain object / string and Content-Type not set, default to JSON
  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, {
    ...options,
    headers
  });

  if (response.status === 401) {
    tokenStorage.clear();
    if (onAuthErrorCallback) {
      onAuthErrorCallback();
    }
    let errorMessage = 'Session expired or unauthorized. Please log in.';
    try {
      const err = await response.json();
      if (err.error) errorMessage = err.error;
    } catch {
      // ignore
    }
    throw new Error(errorMessage);
  }

  if (!response.ok) {
    let errorMessage = `Request failed with status ${response.status}`;
    try {
      const errorData = await response.json();
      errorMessage = errorData.error || errorMessage;
    } catch {
      // ignore JSON parse failure
    }
    throw new Error(errorMessage);
  }

  return response;
}

export const authApi = {
  register: async (name, email, password) => {
    const res = await request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password })
    });
    return res.json();
  },

  login: async (email, password) => {
    const res = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    return res.json();
  },

  me: async () => {
    const res = await request('/auth/me');
    return res.json();
  }
};

export const filesApi = {
  /**
   * List files for current user with optional folderId, search, and sorting
   */
  list: async (folderId = null, search = '', sortBy = 'uploadedAt', sortDir = 'desc') => {
    const params = new URLSearchParams();
    if (folderId) params.append('folderId', folderId);
    if (search) params.append('search', search);
    if (sortBy) params.append('sortBy', sortBy);
    if (sortDir) params.append('sortDir', sortDir);

    const queryString = params.toString() ? `?${params.toString()}` : '';
    const res = await request(`/files${queryString}`);
    return res.json();
  },

  /**
   * Get recently uploaded files
   */
  recent: async (limit = 10) => {
    const res = await request(`/files/recent?limit=${limit}`);
    return res.json();
  },

  /**
   * Upload a file using multipart/form-data with progress and optional folderId
   */
  upload: async (file, folderId = null, onProgress = null) => {
    return new Promise((resolve, reject) => {
      const formData = new FormData();
      formData.append('file', file);
      if (folderId) {
        formData.append('folderId', folderId);
      }

      const xhr = new XMLHttpRequest();
      xhr.open('POST', `${BASE_URL}/files`);

      const token = tokenStorage.get();
      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      }

      if (onProgress) {
        xhr.upload.addEventListener('progress', (e) => {
          if (e.lengthComputable) {
            onProgress(Math.round((e.loaded / e.total) * 100));
          }
        });
      }

      xhr.onload = () => {
        if (xhr.status === 201) {
          try {
            resolve(JSON.parse(xhr.responseText));
          } catch {
            reject(new Error('Invalid response from server'));
          }
        } else if (xhr.status === 401) {
          tokenStorage.clear();
          if (onAuthErrorCallback) onAuthErrorCallback();
          reject(new Error('Session expired or unauthorized.'));
        } else {
          try {
            const err = JSON.parse(xhr.responseText);
            reject(new Error(err.error || `Upload failed with status ${xhr.status}`));
          } catch {
            reject(new Error(`Upload failed with status ${xhr.status}`));
          }
        }
      };

      xhr.onerror = () => reject(new Error('Network error during upload'));
      xhr.send(formData);
    });
  },

  /**
   * Rename a file
   */
  rename: async (id, newName) => {
    const res = await request(`/files/${id}/rename`, {
      method: 'PATCH',
      body: JSON.stringify({ newName })
    });
    return res.json();
  },

  /**
   * Move a file to another folder (or null for root)
   */
  move: async (id, targetFolderId) => {
    const res = await request(`/files/${id}/move`, {
      method: 'PATCH',
      body: JSON.stringify({ targetFolderId: targetFolderId || null })
    });
    return res.json();
  },

  /**
   * Delete a file (soft-delete to trash by default, or permanent)
   */
  delete: async (id, permanent = false) => {
    await request(`/files/${id}?permanent=${permanent}`, { method: 'DELETE' });
  },

  /**
   * Get storage usage stats for current user
   */
  storageUsage: async () => {
    const res = await request('/files/storage-usage');
    return res.json();
  },

  /**
   * Get single file metadata
   */
  getMetadata: async (id) => {
    const res = await request(`/files/${id}`);
    return res.json();
  },

  /**
   * Fetch authenticated preview blob
   */
  getPreviewBlob: async (id) => {
    const res = await request(`/files/${id}/preview`);
    return res.blob();
  },

  /**
   * Trigger file download via browser with Authorization header
   */
  download: async (id, filename) => {
    const res = await request(`/files/${id}/download`);
    const blob = await res.blob();
    const blobUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(blobUrl);
  },

  /**
   * Check API health
   */
  health: async () => {
    const res = await request('/health');
    return res.json();
  },
};

export const foldersApi = {
  /**
   * Create a folder under parentId (or root if parentId is null)
   */
  create: async (name, parentId = null) => {
    const res = await request('/folders', {
      method: 'POST',
      body: JSON.stringify({ name, parentId: parentId || null })
    });
    return res.json();
  },

  /**
   * List folders under parentId
   */
  list: async (parentId = null) => {
    const query = parentId ? `?parentId=${encodeURIComponent(parentId)}` : '';
    const res = await request(`/folders${query}`);
    return res.json();
  },

  /**
   * Get folder details by ID
   */
  get: async (id) => {
    const res = await request(`/folders/${id}`);
    return res.json();
  },

  /**
   * Get breadcrumbs lineage for a folder
   */
  lineage: async (id) => {
    const res = await request(`/folders/${id}/lineage`);
    return res.json();
  },

  /**
   * Rename a folder
   */
  rename: async (id, newName) => {
    const res = await request(`/folders/${id}/rename`, {
      method: 'PATCH',
      body: JSON.stringify({ newName })
    });
    return res.json();
  },

  /**
   * Soft-delete folder to trash
   */
  delete: async (id) => {
    await request(`/folders/${id}`, { method: 'DELETE' });
  }
};

export const trashApi = {
  /**
   * Get all items in trash (folders and files)
   */
  get: async () => {
    const res = await request('/trash');
    return res.json();
  },

  /**
   * Restore a file from trash
   */
  restoreFile: async (id) => {
    await request(`/trash/files/${id}/restore`, { method: 'PUT' });
  },

  /**
   * Permanently delete a file from trash
   */
  permanentDeleteFile: async (id) => {
    await request(`/trash/files/${id}`, { method: 'DELETE' });
  },

  /**
   * Restore a folder from trash
   */
  restoreFolder: async (id) => {
    await request(`/trash/folders/${id}/restore`, { method: 'PUT' });
  },

  /**
   * Permanently delete a folder from trash
   */
  permanentDeleteFolder: async (id) => {
    await request(`/trash/folders/${id}`, { method: 'DELETE' });
  },

  /**
   * Empty all trash for user
   */
  empty: async () => {
    await request('/trash/empty', { method: 'DELETE' });
  }
};

export const sharesApi = {
  /**
   * Share file with user by email or generate public link
   * @param {{ fileId: string, recipientEmail?: string, permission: 'VIEW'|'DOWNLOAD', linkShare?: boolean, expiresAt?: string }} data
   */
  share: async (data) => {
    const res = await request('/shares', {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return res.json();
  },

  /**
   * List all active shares for a specific file (owner only)
   */
  listForFile: async (fileId) => {
    const res = await request(`/shares/file/${fileId}`);
    return res.json();
  },

  /**
   * Revoke an active share
   */
  revoke: async (shareId) => {
    await request(`/shares/${shareId}`, { method: 'DELETE' });
  },

  /**
   * List files shared with authenticated user
   */
  getSharedWithMe: async () => {
    const res = await request('/shares/shared-with-me');
    return res.json();
  },

  /**
   * Download a shared file (authenticated recipient)
   */
  downloadShared: async (shareId, filename) => {
    const res = await request(`/shares/${shareId}/download`);
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename || 'shared-file';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },

  /**
   * Get preview blob URL for authenticated shared file
   */
  getSharedPreviewBlob: async (shareId) => {
    const res = await request(`/shares/${shareId}/preview`);
    const blob = await res.blob();
    return window.URL.createObjectURL(blob);
  },

  /**
   * Get metadata for a public link share (unauthenticated)
   */
  getPublicShare: async (token) => {
    const res = await fetch(`${BASE_URL}/shares/public/${token}`);
    if (!res.ok) {
      let err = 'Invalid, expired, or revoked share link.';
      try {
        const body = await res.json();
        if (body.error) err = body.error;
      } catch {}
      throw new Error(err);
    }
    return res.json();
  },

  /**
   * Download a public shared file (unauthenticated)
   */
  downloadPublic: async (token, filename) => {
    const res = await fetch(`${BASE_URL}/shares/public/${token}/download`);
    if (!res.ok) {
      let err = 'Download failed or restricted.';
      try {
        const body = await res.json();
        if (body.error) err = body.error;
      } catch {}
      throw new Error(err);
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename || 'downloaded-file';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },

  /**
   * Preview public shared file (unauthenticated)
   */
  getPublicPreviewBlob: async (token) => {
    const res = await fetch(`${BASE_URL}/shares/public/${token}/preview`);
    if (!res.ok) {
      let err = 'Preview failed or restricted.';
      try {
        const body = await res.json();
        if (body.error) err = body.error;
      } catch {}
      throw new Error(err);
    }
    const blob = await res.blob();
    return window.URL.createObjectURL(blob);
  }
};

export const paymentsApi = {
  /**
   * Get payment configuration & pricing
   */
  getConfig: async () => {
    const res = await request('/payments/config');
    return res.json();
  },

  /**
   * Create Razorpay order
   */
  createOrder: async () => {
    const res = await request('/payments/create-order', {
      method: 'POST'
    });
    return res.json();
  },

  /**
   * Verify signature and complete upgrade
   */
  verifyPayment: async (data) => {
    const res = await request('/payments/verify', {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return res.json();
  }
};

export const activityApi = {
  /**
   * Get user activities timeline
   */
  list: async (page = 0, size = 30) => {
    const res = await request(`/activity?page=${page}&size=${size}`);
    return res.json();
  }
};

