import { Users, FileQuestion } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function SharedWithMe() {
  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Shared with Me</h1>
        <p className="text-sm text-slate-500 mt-0.5">Files and folders other people have shared with you</p>
      </div>

      <div className="card flex flex-col items-center justify-center py-16 text-center">
        <div className="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mb-4">
          <Users size={28} className="text-slate-400" />
        </div>
        <h3 className="text-base font-semibold text-slate-700">No shared files yet</h3>
        <p className="text-sm text-slate-500 mt-1 max-w-sm">
          When someone shares a file or folder with you, it will show up here.
        </p>
        <Link to="/files" className="btn-secondary mt-5">
          Go to My Files
        </Link>
      </div>
    </div>
  );
}
