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
   * List files for current user
   */
  list: async () => {
    const res = await request('/files');
    return res.json();
  },

  /**
   * Upload a file using multipart/form-data with progress
   */
  upload: async (file, onProgress) => {
    return new Promise((resolve, reject) => {
      const formData = new FormData();
      formData.append('file', file);

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
   * Delete a file by ID
   */
  delete: async (id) => {
    await request(`/files/${id}`, { method: 'DELETE' });
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
