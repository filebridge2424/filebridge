import { HardDrive } from 'lucide-react';
import { formatBytes } from '../utils/format.js';

export default function StorageUsageBar({ usage, loading }) {
  if (loading || !usage) {
    return (
      <div className="card p-4 animate-pulse">
        <div className="h-4 bg-slate-100 rounded w-32 mb-3" />
        <div className="h-2 bg-slate-100 rounded-full" />
        <div className="h-3 bg-slate-100 rounded w-24 mt-2" />
      </div>
    );
  }

  const { usedBytes, limitBytes, usedPercent } = usage;
  const isNearLimit = usedPercent >= 80;
  const isCritical = usedPercent >= 95;

  const barColor = isCritical
    ? 'bg-red-500'
    : isNearLimit
    ? 'bg-amber-500'
    : 'bg-teal-500';

  return (
    <div className="card p-4">
      <div className="flex items-center gap-2 mb-3">
        <HardDrive size={16} className="text-slate-500" />
        <span className="text-sm font-medium text-slate-700">Storage</span>
        {isCritical && (
          <span className="ml-auto text-xs font-medium text-red-600 bg-red-50 px-2 py-0.5 rounded-full">
            Almost full
          </span>
        )}
        {isNearLimit && !isCritical && (
          <span className="ml-auto text-xs font-medium text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
            {usedPercent.toFixed(0)}% used
          </span>
        )}
      </div>

      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
        <div
          className={`h-2 rounded-full transition-all duration-500 ${barColor}`}
          style={{ width: `${Math.min(usedPercent, 100)}%` }}
        />
      </div>

      <div className="flex items-center justify-between mt-2">
        <span className="text-xs text-slate-500">
          {formatBytes(usedBytes)} used
        </span>
        <span className="text-xs text-slate-400">
          {formatBytes(limitBytes)} total
        </span>
      </div>
    </div>
  );
}
