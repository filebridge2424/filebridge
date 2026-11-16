import { useState, useEffect } from 'react';
import { Share2, X, Copy, Check, Trash2, Globe, Mail, ShieldCheck, Loader2 } from 'lucide-react';
import { sharesApi } from '../api/filesApi.js';

export default function ShareModal({ file, onClose }) {
  const [activeTab, setActiveTab] = useState('email'); // 'email' | 'link'
  const [recipientEmail, setRecipientEmail] = useState('');
  const [permission, setPermission] = useState('VIEW');
  const [loading, setLoading] = useState(false);
  const [shares, setShares] = useState([]);
  const [loadingShares, setLoadingShares] = useState(true);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const fetchShares = async () => {
    try {
      setLoadingShares(true);
      const data = await sharesApi.listForFile(file.id);
      setShares(data || []);
    } catch (err) {
      console.error('Failed to load shares:', err);
    } finally {
      setLoadingShares(false);
    }
  };

  useEffect(() => {
    if (file?.id) {
      fetchShares();
    }
  }, [file?.id]);

  const activeLinkShare = shares.find((s) => s.linkShare && !s.revoked);

  const handleShareWithEmail = async (e) => {
    e.preventDefault();
    if (!recipientEmail.trim()) {
      setError('Please enter a valid recipient email address.');
      return;
    }
    setLoading(true);
    setError(null);
    setSuccessMsg(null);
    try {
      await sharesApi.share({
        fileId: file.id,
        recipientEmail: recipientEmail.trim(),
        permission,
        linkShare: false
      });
      setRecipientEmail('');
      setSuccessMsg(`Shared successfully with ${recipientEmail.trim()}`);
      await fetchShares();
    } catch (err) {
      setError(err.message || 'Failed to share file.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateOrUpdateLink = async (targetPermission) => {
    setLoading(true);
    setError(null);
    setSuccessMsg(null);
    try {
      await sharesApi.share({
        fileId: file.id,
        permission: targetPermission,
        linkShare: true
      });
      setSuccessMsg('Share link updated.');
      await fetchShares();
    } catch (err) {
      setError(err.message || 'Failed to generate link.');
    } finally {
      setLoading(false);
    }
  };

  const handleRevoke = async (shareId) => {
    try {
      await sharesApi.revoke(shareId);
      await fetchShares();
      setSuccessMsg('Share revoked successfully.');
    } catch (err) {
      setError(err.message || 'Failed to revoke share.');
    }
  };

  const publicUrl = activeLinkShare
    ? `${window.location.origin}/share/${activeLinkShare.shareToken}`
    : '';

  const handleCopyLink = () => {
    if (!publicUrl) return;
    navigator.clipboard.writeText(publicUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-navy-950/70 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl border border-slate-200/80 w-full max-w-lg overflow-hidden animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-teal-50 border border-teal-200/50 flex items-center justify-center text-teal-600">
              <Share2 size={16} />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-800">Share "{file?.originalFilename}"</h3>
              <p className="text-xs text-slate-500">Manage file access and permissions</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
          >
            <X size={16} />
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-slate-100 px-6 pt-2">
          <button
            onClick={() => { setActiveTab('email'); setError(null); setSuccessMsg(null); }}
            className={`pb-3 px-3 text-xs font-medium border-b-2 transition-colors flex items-center gap-1.5 ${
              activeTab === 'email'
                ? 'border-teal-600 text-teal-700 font-semibold'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            <Mail size={14} />
            Share with People
          </button>
          <button
            onClick={() => { setActiveTab('link'); setError(null); setSuccessMsg(null); }}
            className={`pb-3 px-3 text-xs font-medium border-b-2 transition-colors flex items-center gap-1.5 ${
              activeTab === 'link'
                ? 'border-teal-600 text-teal-700 font-semibold'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            <Globe size={14} />
            Shareable Link
          </button>
        </div>

        <div className="p-6 space-y-5">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600">
              {error}
            </div>
          )}

          {successMsg && (
            <div className="p-3 bg-teal-50 border border-teal-200 rounded-lg text-xs text-teal-700">
              {successMsg}
            </div>
          )}

          {/* Tab 1: Share by Email */}
          {activeTab === 'email' && (
            <form onSubmit={handleShareWithEmail} className="space-y-4">
              <div className="flex gap-2">
                <input
                  type="email"
                  value={recipientEmail}
                  onChange={(e) => setRecipientEmail(e.target.value)}
                  placeholder="Enter registered user's email"
                  className="input flex-1 text-xs"
                  required
                />
                <select
                  value={permission}
                  onChange={(e) => setPermission(e.target.value)}
                  className="input text-xs w-32"
                >
                  <option value="VIEW">Can View</option>
                  <option value="DOWNLOAD">Can Download</option>
                </select>
                <button
                  type="submit"
                  disabled={loading}
                  className="btn-primary text-xs flex items-center gap-1.5 px-4"
                >
                  {loading ? <Loader2 size={14} className="animate-spin" /> : 'Share'}
                </button>
              </div>
            </form>
          )}

          {/* Tab 2: Shareable Link */}
          {activeTab === 'link' && (
            <div className="space-y-4">
              {activeLinkShare ? (
                <div className="space-y-3">
                  <div className="flex gap-2">
                    <input
                      type="text"
                      readOnly
                      value={publicUrl}
                      className="input flex-1 text-xs bg-slate-50 text-slate-600 select-all"
                    />
                    <button
                      type="button"
                      onClick={handleCopyLink}
                      className="btn-secondary text-xs flex items-center gap-1.5"
                    >
                      {copied ? <Check size={14} className="text-teal-600" /> : <Copy size={14} />}
                      {copied ? 'Copied' : 'Copy'}
                    </button>
                  </div>
                  <div className="flex items-center justify-between text-xs text-slate-500 pt-1">
                    <div className="flex items-center gap-2">
                      <span>Permission:</span>
                      <select
                        value={activeLinkShare.permission}
                        onChange={(e) => handleCreateOrUpdateLink(e.target.value)}
                        className="text-xs border rounded px-2 py-1 bg-white"
                        disabled={loading}
                      >
                        <option value="VIEW">View Only</option>
                        <option value="DOWNLOAD">View & Download</option>
                      </select>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRevoke(activeLinkShare.id)}
                      className="text-red-500 hover:text-red-700 text-xs font-medium"
                    >
                      Disable Link
                    </button>
                  </div>
                </div>
              ) : (
                <div className="text-center py-4 space-y-3">
                  <p className="text-xs text-slate-500">
                    Create a public link anyone with the link can use to access this file.
                  </p>
                  <div className="flex justify-center gap-2">
                    <button
                      type="button"
                      onClick={() => handleCreateOrUpdateLink('VIEW')}
                      disabled={loading}
                      className="btn-secondary text-xs"
                    >
                      Create View-Only Link
                    </button>
                    <button
                      type="button"
                      onClick={() => handleCreateOrUpdateLink('DOWNLOAD')}
                      disabled={loading}
                      className="btn-primary text-xs"
                    >
                      Create Download Link
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* People with Access List */}
          <div className="border-t border-slate-100 pt-4 space-y-2">
            <h4 className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              Who has access
            </h4>

            {loadingShares ? (
              <div className="py-4 text-center text-xs text-slate-400">Loading access list...</div>
            ) : shares.length === 0 ? (
              <p className="text-xs text-slate-400 py-2">Only you have access to this file.</p>
            ) : (
              <div className="max-h-40 overflow-y-auto divide-y divide-slate-100">
                {shares.map((share) => (
                  <div key={share.id} className="py-2.5 flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 text-[10px] font-bold">
                        {share.linkShare ? <Globe size={12} /> : (share.sharedWithEmail?.[0] || 'U').toUpperCase()}
                      </div>
                      <div>
                        <p className="font-medium text-slate-700">
                          {share.linkShare ? 'Anyone with link' : share.sharedWithEmail}
                        </p>
                        <p className="text-[10px] text-slate-400">
                          {share.permission === 'DOWNLOAD' ? 'Can download' : 'Can view'}
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRevoke(share.id)}
                      className="p-1 text-slate-400 hover:text-red-500 rounded transition-colors"
                      title="Revoke access"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="px-6 py-3 border-t border-slate-100 bg-slate-50/50 flex justify-end">
          <button onClick={onClose} className="btn-secondary text-xs">
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
