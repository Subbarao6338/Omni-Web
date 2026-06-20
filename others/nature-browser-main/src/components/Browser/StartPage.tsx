import React, { useState } from 'react';
import { motion } from 'motion/react';
import { 
  Cloud, 
  Search, 
  MapPin, 
  User, 
  Calendar,
  Zap,
  Leaf,
  Wind,
  Droplets,
  ArrowRight,
  Plus,
  Shield
} from 'lucide-react';
import { cn } from '../../lib/utils';

export function StartPage({ browser }: { browser: any }) {
  const [searchValue, setSearchValue] = useState('');
  const [isRearranging, setIsRearranging] = useState(false);
  const { settings, setSettings } = browser;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchValue.trim()) {
      browser.navigate(searchValue);
    }
  };

  const quickLinks = [
    { name: 'Google', url: 'https://google.com', icon: 'G' },
    { name: 'GitHub', url: 'https://github.com', icon: 'GH' },
    { name: 'YouTube', url: 'https://youtube.com', icon: 'YT' },
    { name: 'Nature', url: 'https://nationalgeographic.com', icon: 'Nat' },
  ];

  const moveWidget = (from: number, to: number) => {
    const newWidgets = [...settings.startPageWidgets];
    const [moved] = newWidgets.splice(from, 1);
    newWidgets.splice(to, 0, moved);
    setSettings({ ...settings, startPageWidgets: newWidgets });
  };

  const renderWidget = (type: string, index: number) => {
    switch (type) {
      case 'garden':
        return (
          <motion.div 
            key={type}
            layout
            whileHover={isRearranging ? {} : { y: -5 }}
            className={cn(
              "p-6 rounded-3xl bg-gradient-to-br from-forest-500 to-emerald-700 text-white shadow-xl shadow-emerald-500/20 relative group",
              isRearranging && "ring-2 ring-forest-500 cursor-move opacity-80"
            )}
          >
            {isRearranging && (
              <div className="absolute top-2 right-2 flex gap-1">
                <button onClick={() => moveWidget(index, Math.max(0, index - 1))} className="p-1 bg-white/20 rounded hover:bg-white/40"><Plus size={12} className="rotate-45" /></button>
                <button onClick={() => moveWidget(index, Math.min(settings.startPageWidgets.length - 1, index + 1))} className="p-1 bg-white/20 rounded hover:bg-white/40"><Plus size={12} /></button>
              </div>
            )}
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-bold opacity-90">Zen Garden</h3>
                <p className="text-xs opacity-75">Level {settings.gardenLevel || 1} Oasis</p>
              </div>
              <Leaf size={32} className="text-emerald-300" />
            </div>
            
            <div className="flex items-center gap-2 mb-4 overflow-hidden">
              {[...Array(settings.gardenLevel || 1)].map((_, i) => (
                <motion.div 
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  key={i} 
                  className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center border border-white/30"
                >
                  <Leaf size={16} className="text-emerald-200" />
                </motion.div>
              ))}
              <div className="w-10 h-10 border-2 border-dashed border-white/30 rounded-xl flex items-center justify-center opacity-40">
                <Plus size={16} />
              </div>
            </div>

            <p className="text-[10px] font-bold uppercase tracking-widest opacity-80">
              Grow by browsing responsibly
            </p>
          </motion.div>
        );
      case 'weather':
        return (
          <motion.div 
            key={type}
            layout
            whileHover={isRearranging ? {} : { y: -5 }}
            className={cn(
              "p-6 rounded-3xl bg-gradient-to-br from-water-500 to-blue-600 text-white shadow-xl shadow-blue-500/20 relative group",
              isRearranging && "ring-2 ring-forest-500 cursor-move opacity-80"
            )}
          >
            {isRearranging && (
              <div className="absolute top-2 right-2 flex gap-1">
                <button onClick={() => moveWidget(index, Math.max(0, index - 1))} className="p-1 bg-white/20 rounded hover:bg-white/40"><Plus size={12} className="rotate-45" /></button>
                <button onClick={() => moveWidget(index, Math.min(settings.startPageWidgets.length - 1, index + 1))} className="p-1 bg-white/20 rounded hover:bg-white/40"><Plus size={12} /></button>
              </div>
            )}
            <div className="flex justify-between items-start mb-6">
              <div>
                <h3 className="text-lg font-bold opacity-90">Rainy Valley</h3>
                <p className="text-xs opacity-75">10:20 AM • Rainy</p>
              </div>
              <Droplets size={32} />
            </div>
            <div className="text-4xl font-display font-medium">18°C</div>
            <div className="mt-4 flex items-center gap-2 text-xs font-semibold bg-white/20 px-3 py-1.5 rounded-full w-fit">
              <Shield size={12} /> Air Quality: Good
            </div>
          </motion.div>
        );
      case 'focus': // renamed from 'news' or productivity for clarity
      case 'productivity':
        return (
          <motion.div 
            key={type}
            layout
            whileHover={isRearranging ? {} : { y: -5 }}
            className={cn(
              "p-6 rounded-3xl bg-white border border-nature-200 shadow-lg shadow-nature-300/10 relative",
              isRearranging && "ring-2 ring-forest-500 cursor-move opacity-80"
            )}
          >
            {isRearranging && (
              <div className="absolute top-2 right-2 flex gap-1 text-nature-400">
                <button onClick={() => moveWidget(index, Math.max(0, index - 1))} className="p-1 bg-nature-100 rounded hover:bg-nature-200"><Plus size={12} className="rotate-45" /></button>
                <button onClick={() => moveWidget(index, Math.min(settings.startPageWidgets.length - 1, index + 1))} className="p-1 bg-nature-100 rounded hover:bg-nature-200"><Plus size={12} /></button>
              </div>
            )}
            <div className="flex items-center gap-3 mb-4 text-forest-500">
              <Zap size={20} />
              <h3 className="font-bold text-nature-900">Today's Focus</h3>
            </div>
            <div className="space-y-4">
              <div className="flex items-center gap-3 p-3 bg-nature-50 rounded-2xl border border-nature-100">
                <div className="w-2 h-2 rounded-full bg-forest-500" />
                <span className="text-sm text-nature-700 font-medium">Research Firefox API</span>
              </div>
            </div>
            <button className="w-full mt-4 flex items-center justify-center gap-2 text-xs font-bold text-forest-500 hover:text-forest-700">
              View All <ArrowRight size={12} />
            </button>
          </motion.div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="h-full overflow-y-auto bg-nature-100 flex flex-col items-center py-20 px-6 no-scrollbar">
      {/* Search Section */}
      <motion.div 
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="w-full max-w-2xl text-center mb-16"
      >
        <div className="flex items-center justify-center gap-3 mb-8">
          <Leaf className="text-forest-500" size={40} />
          <h1 className="text-6xl font-display font-bold text-nature-900 tracking-tight">Nature</h1>
        </div>

        <form onSubmit={handleSearch} className="relative group">
          <div className="absolute inset-y-0 left-6 flex items-center pointer-events-none text-nature-400 group-focus-within:text-forest-500 transition-colors">
            <Search size={24} />
          </div>
          <input
            type="text"
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            className="w-full pl-16 pr-24 py-5 rounded-3xl bg-white border border-nature-200 shadow-xl shadow-nature-300/20 outline-none focus:ring-4 focus:ring-forest-500/10 focus:border-forest-500 transition-all text-lg font-medium"
            placeholder="Explore the web..."
          />
          <button 
            type="submit"
            className="absolute right-3 top-3 bottom-3 px-6 bg-forest-500 text-white rounded-2xl font-bold hover:bg-forest-600 transition-colors shadow-lg shadow-forest-500/20"
          >
            Go
          </button>
        </form>
      </motion.div>

      {/* Grid Content */}
      <div className="w-full max-w-5xl grid grid-cols-1 md:grid-cols-12 gap-6">
        {/* Quick Links */}
        <div className="md:col-span-8 grid grid-cols-4 gap-4">
          {quickLinks.map((link, i) => (
            <motion.button
              key={link.url}
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: i * 0.1 }}
              onClick={() => browser.navigate(link.url)}
              className="flex flex-col items-center gap-2 p-6 rounded-3xl bg-white border border-nature-200 hover:border-forest-500 hover:shadow-xl hover:shadow-forest-500/10 transition-all group"
            >
              <div className="w-14 h-14 rounded-full bg-nature-100 flex items-center justify-center text-xl font-bold text-forest-500 group-hover:bg-forest-50 group-hover:scale-110 transition-all">
                {link.icon}
              </div>
              <span className="text-sm font-semibold text-nature-600 group-hover:text-forest-600">{link.name}</span>
            </motion.button>
          ))}
          <motion.button
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.4 }}
            className="flex flex-col items-center gap-2 p-6 rounded-3xl bg-nature-200/50 border-2 border-dashed border-nature-300 hover:border-forest-300 text-nature-400 transition-all"
          >
            <div className="w-14 h-14 rounded-full border-2 border-dashed border-nature-300 flex items-center justify-center">
              <Plus size={24} />
            </div>
            <span className="text-sm font-semibold">Add</span>
          </motion.button>
        </div>

        {/* Widgets Column */}
        <div className="md:col-span-4 flex flex-col gap-6">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-xs font-bold text-nature-400 uppercase tracking-widest">Widgets</h4>
            <button 
              onClick={() => setIsRearranging(!isRearranging)}
              className={cn(
                "text-xs font-bold px-3 py-1 rounded-full transition-all",
                isRearranging ? "bg-forest-500 text-white" : "bg-nature-200 text-nature-500 hover:bg-nature-300"
              )}
            >
              {isRearranging ? 'Done' : 'Rearrange'}
            </button>
          </div>
          {settings.startPageWidgets.map((type: string, i: number) => renderWidget(type, i))}
          
          <motion.button
            whileHover={{ scale: 1.02 }}
            className="w-full p-4 rounded-2xl border-2 border-dashed border-nature-300 flex items-center justify-center gap-2 text-nature-400 hover:border-forest-300 hover:text-forest-500 transition-all font-bold text-sm"
          >
            <Plus size={16} /> Add Widget
          </motion.button>
        </div>
      </div>
    </div>
  );
}
