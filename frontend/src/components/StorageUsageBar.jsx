import { useState } from 'react';
import { HardDrive, Trash2, Files, Crown, Sparkles, ArrowUpRight } from 'lucide-react';
import { formatBytes } from '../utils/format.js';
import UpgradeModal from './UpgradeModal.jsx';

export default function StorageUsageBar({ usage, loading, onRefresh }) {
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);

  if (loading || !usage) {
    return (
      <div className="card p-4 animate-pulse">
        <div className="h-4 bg-slate-100 rounded w-32 mb-3" />
        <div className="h-2 bg-slate-100 rounded-full" />
        <div className="h-3 bg-slate-100 rounded w-48 mt-2" />
      </div>
    );
  }

  const {
    usedBytes = 0,
    limitBytes = 104857600,
    usedPercent = 0,
    remainingBytes,
    fileCount,
    trashBytes,
    plan = 'FREE'
  } = usage;

  const isPremium = plan === 'PREMIUM';
  const computedRemaining = remainingBytes !== undefined ? remainingBytes : Math.max(0, limitBytes - usedBytes);
  const isNearLimit = usedPercent >= 80;
  const isCritical = usedPercent >= 95;

  const barColor = isCritical
    ? 'bg-red-500'
    : isNearLimit
    ? 'bg-amber-500'
    : isPremium
    ? 'bg-amber-500'
    : 'bg-teal-500';

  return (
    <>
      <div className="card p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <div className={`w-7 h-7 rounded-lg border flex items-center justify-center ${
              isPremium
                ? 'bg-amber-50 border-amber-200/50 text-amber-600'
                : 'bg-teal-50 border-teal-200/50 text-teal-600'
            }`}>
              {isPremium ? <Crown size={15} /> : <HardDrive size={15} />}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-slate-800">Storage Usage</span>
                <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                  isPremium
                    ? 'bg-gradient-to-r from-amber-500 to-amber-600 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600'
                }`}>
                  {isPremium && <Crown size={10} />}
                  {plan}
                </span>
              </div>
              <span className="text-xs text-slate-400">
                ({formatBytes(computedRemaining)} available)
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {!isPremium && (
              <button
                type="button"
                onClick={() => setShowUpgradeModal(true)}
                className="btn-primary text-xs py-1 px-2.5 flex items-center gap-1 shadow-xs"
              >
                <Sparkles size={12} />
                Upgrade
                <ArrowUpRight size={12} />
              </button>
            )}

            {fileCount !== undefined && (
              <span className="hidden sm:inline-flex items-center gap-1 text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                <Files size={12} />
                {fileCount} active file{fileCount !== 1 ? 's' : ''}
              </span>
            )}
            {trashBytes > 0 && (
              <span className="inline-flex items-center gap-1 text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full" title="Trash storage">
                <Trash2 size={12} className="text-slate-400" />
                {formatBytes(trashBytes)} in trash
              </span>
            )}
            {isCritical && (
              <span className="text-xs font-medium text-red-600 bg-red-50 px-2 py-0.5 rounded-full">
                Almost full
              </span>
            )}
            {isNearLimit && !isCritical && (
              <span className="text-xs font-medium text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
                {usedPercent.toFixed(0)}% used
              </span>
            )}
          </div>
        </div>

        <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
          <div
            className={`h-2 rounded-full transition-all duration-500 ${barColor}`}
            style={{ width: `${Math.min(usedPercent, 100)}%` }}
          />
        </div>

        <div className="flex items-center justify-between mt-2.5">
          <span className="text-xs font-medium text-slate-700">
            {formatBytes(usedBytes)} <span className="text-slate-400 font-normal">used</span>
          </span>
          <span className="text-xs text-slate-400">
            {formatBytes(limitBytes)} limit
          </span>
        </div>
      </div>

      {showUpgradeModal && (
        <UpgradeModal
          currentPlan={plan}
          onClose={() => setShowUpgradeModal(false)}
          onUpgraded={() => {
            onRefresh?.();
          }}
        />
      )}
    </>
  );
}
