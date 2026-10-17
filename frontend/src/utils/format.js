/**
 * Format bytes into human-readable string.
 * @param {number} bytes
 * @param {number} decimals
 */
export function formatBytes(bytes, decimals = 1) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

/**
 * Format ISO timestamp to readable locale string.
 * @param {string} isoString
 */
export function formatDate(isoString) {
  if (!isoString) return '-';
  const date = new Date(isoString);
  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

/**
 * Format ISO timestamp to relative time or date.
 * @param {string} isoString
 */
export function formatRelativeDate(isoString) {
  if (!isoString) return '-';
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now - date;
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return 'just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHour < 24) return `${diffHour}h ago`;
  if (diffDay < 7) return `${diffDay}d ago`;
  return formatDate(isoString);
}

/**
 * Get a color class and label for a content type.
 * @param {string} contentType
 */
export function getFileTypeInfo(contentType) {
  if (!contentType) return { color: 'bg-slate-100 text-slate-600', label: 'File' };
  const type = contentType.toLowerCase();

  if (type.startsWith('image/'))     return { color: 'bg-purple-100 text-purple-700', label: 'Image' };
  if (type.startsWith('video/'))     return { color: 'bg-pink-100 text-pink-700',   label: 'Video' };
  if (type.startsWith('audio/'))     return { color: 'bg-yellow-100 text-yellow-700', label: 'Audio' };
  if (type === 'application/pdf')    return { color: 'bg-red-100 text-red-700',     label: 'PDF' };
  if (type.includes('zip') || type.includes('tar') || type.includes('gzip') || type.includes('rar'))
                                     return { color: 'bg-orange-100 text-orange-700', label: 'Archive' };
  if (type.includes('spreadsheet') || type.includes('excel') || type === 'text/csv')
                                     return { color: 'bg-green-100 text-green-700',  label: 'Spreadsheet' };
  if (type.includes('presentation') || type.includes('powerpoint'))
                                     return { color: 'bg-orange-100 text-orange-700', label: 'Slides' };
  if (type.includes('word') || type.includes('document'))
                                     return { color: 'bg-blue-100 text-blue-700',   label: 'Document' };
  if (type.startsWith('text/'))      return { color: 'bg-slate-100 text-slate-700', label: 'Text' };
  if (type.includes('json') || type.includes('javascript') || type.includes('typescript') || type.includes('html'))
                                     return { color: 'bg-indigo-100 text-indigo-700', label: 'Code' };

  return { color: 'bg-slate-100 text-slate-600', label: 'File' };
}

/**
 * Sort an array of file objects by a given field and direction.
 */
export function sortFiles(files, sortField, sortDir) {
  return [...files].sort((a, b) => {
    let aVal = a[sortField];
    let bVal = b[sortField];

    if (sortField === 'originalFilename') {
      aVal = aVal?.toLowerCase() ?? '';
      bVal = bVal?.toLowerCase() ?? '';
    }
    if (sortField === 'uploadedAt') {
      aVal = new Date(aVal).getTime();
      bVal = new Date(bVal).getTime();
    }

    if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
    return 0;
  });
}
