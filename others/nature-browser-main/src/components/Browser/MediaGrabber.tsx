import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { 
  X, 
  Download, 
  Video, 
  Image as ImageIcon, 
  Music, 
  FileText,
  Zap,
  CheckCircle2,
  Package,
  ArrowDownToLine
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface SniffedMedia {
  id: string;
  type: 'video' | 'image' | 'audio' | 'document';
  title: string;
  size: string;
  url: string;
  thumbnail: string;
}

export function MediaGrabber({ onClose, browser }: { onClose: () => void; browser: any }) {
  const [isSniffing, setIsSniffing] = useState(true);
  const [mediaList, setMediaList] = useState<SniffedMedia[]>([]);

  useEffect(() => {
    // Simulate sniffing
    const timer = setTimeout(() => {
      setIsSniffing(false);
      setMediaList([
        { id: '1', type: 'video', title: 'Summer Vibes.mp4', size: '42 MB', url: '#', thumbnail: 'https://picsum.photos/seed/nature1/200/200' },
        { id: '2', type: 'video', title: 'Nature Loop.mp4', size: '12 MB', url: '#', thumbnail: 'https://picsum.photos/seed/nature2/200/200' },
        { id: '3', type: 'image', title: 'Profile_Photo_01.jpg', size: '1.2 MB', url: '#', thumbnail: 'https://picsum.photos/seed/profile1/200/200' },
        { id: '4', type: 'image', title: 'Beach_Sunset.jpg', size: '4.5 MB', url: '#', thumbnail: 'https://picsum.photos/seed/beach/200/200' },
        { id: '5', type: 'audio', title: 'Wind_Bells.mp3', size: '3.1 MB', url: '#', thumbnail: 'https://picsum.photos/seed/audio1/200/200' },
      ]);
    }, 1500);
    return () => clearTimeout(timer);
  }, []);

  const handleSocialGrab = () => {
    setIsSniffing(true);
    setTimeout(() => {
      const socialMedia: SniffedMedia[] = [
        ...mediaList,
        { id: 's1', type: 'image', title: 'Profile_Banner.jpg', size: '2.4 MB', url: '#', thumbnail: 'https://picsum.photos/seed/banner/200/200' },
        { id: 's2', type: 'video', title: 'Latest_Story.mp4', size: '18 MB', url: '#', thumbnail: 'https://picsum.photos/seed/story/200/200' },
        { id: 's3', type: 'image', title: 'Post_001.jpg', size: '1.1 MB', url: '#', thumbnail: 'https://picsum.photos/seed/p1/200/200' },
      ];
      setMediaList(socialMedia);
      setIsSniffing(false);
      socialMedia.forEach(m => browser.downloadMedia(m.url, m.title));
    }, 2000);
  };

  return (
    <motion.div 
      initial={{ x: '100%' }}
      animate={{ x: 0 }}
      exit={{ x: '100%' }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      className="absolute top-0 right-0 bottom-0 w-96 bg-white shadow-2xl z-[100] border-l border-nature-200 flex flex-col"
    >
      <div className="p-6 border-b border-nature-100 flex items-center justify-between bg-nature-50">
        <div className="flex items-center gap-3 text-forest-600">
          <Zap size={24} className="fill-forest-500" />
          <h2 className="font-display text-xl font-bold">Media Sniffer</h2>
        </div>
        <button onClick={onClose} className="p-2 hover:bg-nature-200 rounded-full text-nature-500 transition-colors">
          <X size={20} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-6 no-scrollbar">
        {isSniffing ? (
          <div className="h-full flex flex-col items-center justify-center text-center">
            <motion.div 
              animate={{ rotate: 360 }}
              transition={{ repeat: Infinity, duration: 2, ease: "linear" }}
              className="mb-6 text-forest-500"
            >
              <Zap size={48} />
            </motion.div>
            <h3 className="text-xl font-display font-medium text-nature-900 mb-2">Sniffing Media...</h3>
            <p className="text-nature-500 text-sm">Identifying videos, images and audio from current page.</p>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-bold text-nature-400 uppercase tracking-widest">{mediaList.length} Items Found</span>
              <button 
                onClick={() => mediaList.forEach(m => browser.downloadMedia(m.url, m.title))}
                className="flex items-center gap-2 px-4 py-2 bg-forest-500 text-white rounded-xl text-xs font-bold hover:bg-forest-600 transition-colors shadow-lg shadow-forest-500/20"
              >
                <Package size={14} /> Download All
              </button>
            </div>

            {mediaList.map((media) => (
              <motion.div 
                key={media.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="group flex gap-3 p-3 rounded-2xl border border-nature-100 hover:border-forest-200 hover:bg-forest-50/30 transition-all"
              >
                <div className="w-16 h-16 rounded-xl overflow-hidden bg-nature-100 shrink-0 border border-nature-200">
                  <img src={media.thumbnail} alt={media.title} className="w-full h-full object-cover" />
                </div>
                <div className="flex-1 min-w-0 flex flex-col justify-center">
                  <h4 className="text-xs font-bold text-nature-900 truncate mb-1">{media.title}</h4>
                  <div className="flex items-center gap-3 text-[10px] font-semibold text-nature-500">
                    <span className="flex items-center gap-1">
                      {media.type === 'video' && <Video size={10} />}
                      {media.type === 'image' && <ImageIcon size={10} />}
                      {media.type === 'audio' && <Music size={10} />}
                      {media.type.toUpperCase()}
                    </span>
                    <span>•</span>
                    <span>{media.size}</span>
                  </div>
                </div>
                <button 
                  onClick={() => browser.downloadMedia(media.url, media.title)}
                  className="p-2 h-fit my-auto text-nature-400 hover:text-forest-500 hover:bg-forest-100 rounded-lg transition-all"
                >
                  <Download size={18} />
                </button>
              </motion.div>
            ))}
          </div>
        )}
      </div>

      <div className="p-6 bg-nature-50 border-t border-nature-200">
        <div className="flex items-center gap-3 p-4 bg-white border border-nature-200 rounded-2xl shadow-sm">
          <div className="w-10 h-10 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center">
            <Zap size={20} className={cn(isSniffing && "animate-pulse")} />
          </div>
          <div className="flex-1">
            <h4 className="text-xs font-bold text-nature-900">One-Tap Social Grabber</h4>
            <p className="text-[10px] text-nature-500">Download all profile media automatically.</p>
          </div>
          <button 
            onClick={handleSocialGrab}
            disabled={isSniffing}
            className="px-3 py-1.5 bg-nature-900 text-white rounded-lg text-xs font-bold hover:bg-black transition-colors disabled:opacity-50"
          >
            {isSniffing ? 'Sniffing...' : 'Grab All'}
          </button>
        </div>
      </div>
    </motion.div>
  );
}
