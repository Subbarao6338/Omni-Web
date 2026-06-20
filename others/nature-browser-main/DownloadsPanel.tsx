import React from 'react';
import { motion } from 'motion/react';
import { 
  X, 
  Pause, 
  Play, 
  Trash2, 
  FolderOpen, 
  CheckCircle2, 
  Clock,
  ArrowDownToLine,
  FileCode,
  FileImage,
  FileVideo,
  Music
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { DownloadItem } from '../../types';

export function DownloadsPanel({ onClose, browser }: { onClose: () => void; browser: any }) {
  const { downloads, setDownloads } = browser;

  const toggleStatus = (id: string) => {
    browser.toggleDownload(id);
  };

  const removeDownload = (id: string) => {
    setDownloads((prev: DownloadItem[]) => prev.filter(d => d.id !== id));
  };

  const getIcon = (item: DownloadItem) => {
    const mime = item.mimeType.toLowerCase();
    if (mime.includes('image')) return <FileImage size={24} className="text-blue-500" />;
    if (mime.includes('video')) return <FileVideo size={24} className="text-purple-500" />;
    if (mime.includes('audio')) return <Music size={24} className="text-pink-500" />;
    return <FileCode size={24} className="text-amber-500" />;
  };

  return (
    <motion.div 
      initial={{ x: '100%' }}
      animate={{ x: 0 }}
      exit={{ x: '100%' }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      className="absolute top-0 right-0 bottom-0 w-[420px] bg-white shadow-2xl z-[100] border-l border-nature-200 flex flex-col"
    >
      <div className="p-8 border-b border-nature-100 flex items-center justify-between bg-nature-50">
        <div>
          <h2 className="font-display text-2xl font-bold text-nature-900">Downloads</h2>
          <p className="text-[10px] font-bold text-nature-400 uppercase tracking-widest mt-1">
            {downloads.filter((d: any) => d.status === 'downloading').length} Active • {downloads.length} Total
          </p>
        </div>
        <button onClick={onClose} className="p-2 hover:bg-nature-200 rounded-full text-nature-500 transition-colors">
          <X size={24} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-6 no-scrollbar space-y-4">
        {downloads.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center opacity-40">
            <ArrowDownToLine size={64} className="text-nature-300 mb-4" />
            <p className="font-display text-xl text-nature-900">No downloads yet</p>
            <p className="text-sm text-nature-500">Files you grab will appear here.</p>
          </div>
        ) : (
          downloads.map((item: DownloadItem) => (
            <div key={item.id} className="p-4 rounded-3xl border border-nature-100 hover:border-forest-200 hover:shadow-lg hover:shadow-forest-500/5 transition-all bg-white relative overflow-hidden group">
              <div className="flex gap-4 items-start relative z-10">
                <div className="w-12 h-12 rounded-2xl bg-nature-50 flex items-center justify-center shrink-0">
                  {getIcon(item)}
                </div>
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm font-bold text-nature-900 truncate mb-1">{item.filename}</h4>
                  <p className="text-[10px] text-nature-500 mb-3">{item.url}</p>
                  
                  {/* Progress Bar */}
                  <div className="w-full h-1.5 bg-nature-100 rounded-full overflow-hidden mb-2">
                    <motion.div 
                      initial={{ width: 0 }}
                      animate={{ width: `${item.progress}%` }}
                      className={cn(
                        "h-full transition-colors",
                        item.status === 'completed' ? "bg-forest-500" : (item.status === 'failed' ? "bg-amber-400" : "bg-blue-500")
                      )}
                    />
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold text-nature-400">
                      {item.status === 'completed' ? 'Completed' : (item.status === 'failed' ? 'Paused' : `${Math.round(item.progress)}%`)}
                    </span>
                    <div className="flex items-center gap-1">
                      {item.status !== 'completed' && (
                        <button 
                          onClick={() => toggleStatus(item.id)}
                          className="p-1.5 hover:bg-nature-100 rounded-lg text-nature-600"
                        >
                          {item.status === 'downloading' ? <Pause size={14} /> : <Play size={14} />}
                        </button>
                      )}
                      <button 
                        onClick={() => removeDownload(item.id)}
                        className="p-1.5 hover:bg-red-50 rounded-lg text-red-400"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      <div className="p-6 bg-nature-100 border-t border-nature-200">
        <button className="w-full py-4 bg-nature-900 text-white rounded-2xl font-bold hover:bg-black transition-colors flex items-center justify-center gap-2">
          <FolderOpen size={18} /> Open Downloads Folder
        </button>
      </div>
    </motion.div>
  );
}
