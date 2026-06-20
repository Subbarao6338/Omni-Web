import React from 'react';
import { motion } from 'motion/react';
import { 
  X, 
  Moon, 
  Sun, 
  Type, 
  Shield, 
  EyeOff, 
  Globe, 
  Monitor,
  Check,
  ChevronRight,
  Download,
  Share2,
  Trash2,
  Key,
  Users
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { Theme } from '../../types';

import { auth, loginWithGoogle } from '../../firebase';

export function SettingsPanel({ onClose, browser }: { onClose: () => void; browser: any }) {
  const { settings, setSettings, user } = browser;

  const themes: { id: Theme; label: string; bg: string }[] = [
    { id: 'earth', label: 'Earthy Clay', bg: 'bg-[#8c7a65]' },
    { id: 'forest', label: 'Deep Forest', bg: 'bg-[#2d5a27]' },
    { id: 'water', label: 'Ocean Mist', bg: 'bg-[#3b82f6]' },
    { id: 'sand', label: 'Desert Sand', bg: 'bg-[#d2c8b8]' },
    { id: 'sun', label: 'Golden Sun', bg: 'bg-[#f59e0b]' },
    { id: 'lava', label: 'Volcanic Lava', bg: 'bg-[#ef4444]' },
    { id: 'arctic', label: 'Arctic Ice', bg: 'bg-[#0ea5e9]' },
    { id: 'moss', label: 'Highland Moss', bg: 'bg-[#65a30d]' },
    { id: 'midnight', label: 'Midnight Blue', bg: 'bg-[#6366f1]' },
    { id: 'bloom', label: 'Nature Bloom', bg: 'bg-[#ec4899]' },
    { id: 'savanna', label: 'Wild Savanna', bg: 'bg-[#f97316]' },
    { id: 'dark', label: 'Night Nature', bg: 'bg-[#1a1a1a]' },
  ];

  const updateSetting = (key: string, value: any) => {
    setSettings((prev: any) => ({ ...prev, [key]: value }));
  };

  const handleFirefoxSync = () => {
    alert("Redirecting to Firefox Accounts... \nNature Browser will sync your Firefox bookmarks and history via GeckoView Sync.");
    // In a real app we'd initiate gecko-sync
  };

  return (
    <motion.div 
      initial={{ x: '100%' }}
      animate={{ x: 0 }}
      exit={{ x: '100%' }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      className="absolute top-0 right-0 bottom-0 w-[420px] bg-white shadow-2xl z-[100] border-l border-nature-200 flex flex-col"
    >
      <div className="p-8 border-b border-nature-100 flex items-center justify-between">
        <h2 className="font-display text-2xl font-bold text-nature-900">Browser Settings</h2>
        <button onClick={onClose} className="p-2 hover:bg-nature-100 rounded-full text-nature-500 transition-colors">
          <X size={24} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-8 no-scrollbar space-y-12">
        {/* Profile Section */}
        <section>
          <div className="flex flex-col gap-3">
            {user ? (
              <div className="flex items-center gap-4 p-4 bg-nature-50 rounded-3xl border border-nature-100">
                <div className="w-14 h-14 rounded-2xl bg-forest-500 flex items-center justify-center text-white overflow-hidden">
                  {user.photoURL ? <img src={user.photoURL} alt={user.displayName || ""} className="w-full h-full object-cover" /> : <Users size={32} />}
                </div>
                <div className="flex-1">
                  <h3 className="font-bold text-nature-900 truncate">{user.displayName || "Nature User"}</h3>
                  <p className="text-xs text-nature-500 truncate">{user.email}</p>
                </div>
                <button 
                  onClick={() => auth.signOut()}
                  className="text-xs font-bold text-red-500 p-2 hover:bg-red-50 rounded-lg"
                >
                  Sign Out
                </button>
              </div>
            ) : (
              <div className="p-6 bg-nature-900 rounded-3xl text-white">
                <h3 className="font-display text-xl font-bold mb-2">Sync Your Nature</h3>
                <p className="text-xs opacity-75 mb-6">Sign in to sync your bookmarks, history and settings across all your devices.</p>
                <div className="flex flex-col gap-2">
                  <button 
                    onClick={() => loginWithGoogle()}
                    className="w-full py-3 bg-white text-nature-900 rounded-2xl font-bold hover:bg-nature-100 transition-colors flex items-center justify-center gap-2"
                  >
                    <Globe size={16} /> Sign in with Google
                  </button>
                  <button 
                    onClick={handleFirefoxSync}
                    className="w-full py-3 bg-orange-600 text-white rounded-2xl font-bold hover:bg-orange-700 transition-colors flex items-center justify-center gap-2"
                  >
                    <svg viewBox="0 0 24 24" className="w-4 h-4 fill-current" xmlns="http://www.w3.org/2000/svg">
                      <path d="M22.05 12.01c0 5.42-4.47 9.81-9.98 9.81A9.914 9.914 0 012.3 14.16c.36.08.77.12 1.25.12 1.62 0 3.33-.87 3.33-2.67 0-.58-.16-1.15-.46-1.63-.3-.48-.73-.85-1.24-1.06-.5-.21-1.07-.27-1.61-.17a3.29 3.29 0 00-2.31 2.31c-.1.54-.04 1.11.17 1.61.21.51.58.94 1.06 1.24s1.05.46 1.63.46c1.8 0 2.67-1.71 2.67-3.33 0-.48-.04-.89-.12-1.25a9.914 9.914 0 017.66-9.77c.33.02.66.02.99.02 5.51 0 9.98 4.39 9.98 9.81z"/>
                    </svg>
                    Sync with Firefox Account
                  </button>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Customization */}
        <section>
          <h3 className="text-xs font-bold text-nature-400 uppercase tracking-widest mb-6 px-1">Visual Theme</h3>
          <div className="grid grid-cols-5 gap-3">
            {themes.map((t) => (
              <button
                key={t.id}
                onClick={() => updateSetting('theme', t.id)}
                className={cn(
                  "flex flex-col items-center gap-2 group transition-transform hover:scale-105"
                )}
              >
                <div className={cn(
                  "w-full aspect-square rounded-2xl border-4 transition-all shadow-md",
                  t.bg,
                  settings.theme === t.id ? "border-forest-500 scale-110" : "border-transparent"
                )} />
                <span className="text-[10px] font-bold text-nature-600 truncate w-full text-center">{t.label}</span>
              </button>
            ))}
          </div>
        </section>

        {/* Interface Mode */}
        <section>
          <h3 className="text-xs font-bold text-nature-400 uppercase tracking-widest mb-6 px-1">Interface Mode</h3>
          <div className="flex bg-nature-100 p-1.5 rounded-2xl">
            {(['light', 'dark', 'system'] as const).map((mode) => (
              <button
                key={mode}
                onClick={() => updateSetting('interfaceMode', mode)}
                className={cn(
                  "flex-1 py-3 rounded-xl text-xs font-bold transition-all capitalize flex items-center justify-center gap-2",
                  settings.interfaceMode === mode ? "bg-white text-forest-600 shadow-sm" : "text-nature-500 hover:text-nature-700"
                )}
              >
                {mode === 'light' && <Sun size={14} />}
                {mode === 'dark' && <Moon size={14} />}
                {mode === 'system' && <Monitor size={14} />}
                {mode}
              </button>
            ))}
          </div>
        </section>

        {/* Font Size */}
        <section>
          <h3 className="text-xs font-bold text-nature-400 uppercase tracking-widest mb-6 px-1">Typography</h3>
          <div className="flex bg-nature-100 p-1.5 rounded-2xl">
            {(['small', 'medium', 'large'] as const).map((size) => (
              <button
                key={size}
                onClick={() => updateSetting('fontSize', size)}
                className={cn(
                  "flex-1 py-3 rounded-xl text-xs font-bold transition-all capitalize",
                  settings.fontSize === size ? "bg-white text-forest-600 shadow-sm" : "text-nature-500 hover:text-nature-700"
                )}
              >
                {size}
              </button>
            ))}
          </div>
        </section>

        {/* Layout & Position */}
        <section>
          <h3 className="text-xs font-bold text-nature-400 uppercase tracking-widest mb-6 px-1">Layout & Interface</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-white border border-nature-200 rounded-3xl">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-nature-50 text-nature-600 rounded-2xl"><Share2 size={20} className="rotate-90" /></div>
                <div>
                  <h4 className="text-sm font-bold text-nature-900">Address Bar</h4>
                  <p className="text-[10px] text-nature-500">Choose where to place the navigation.</p>
                </div>
              </div>
              <div className="flex bg-nature-100 p-1 rounded-xl">
                {['top', 'bottom'].map((pos) => (
                  <button
                    key={pos}
                    onClick={() => updateSetting('layout', pos)}
                    className={cn(
                      "px-4 py-1.5 rounded-lg text-xs font-bold transition-all capitalize",
                      settings.layout === pos ? "bg-white text-forest-600 shadow-sm" : "text-nature-500 hover:text-nature-700"
                    )}
                  >
                    {pos}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Privacy & Security */}
        <section>
          <h3 className="text-xs font-bold text-nature-400 uppercase tracking-widest mb-6 px-1">Privacy & Security</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-white border border-nature-200 rounded-3xl hover:border-forest-200 transition-colors">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-red-50 text-red-600 rounded-2xl"><Shield size={20} /></div>
                <div>
                  <h4 className="text-sm font-bold text-nature-900">Tracker Blocking</h4>
                  <p className="text-[10px] text-nature-500">Block 3rd party advert trackers.</p>
                </div>
              </div>
              <button 
                onClick={() => updateSetting('trackerBlocking', !settings.trackerBlocking)}
                className={cn(
                  "w-12 h-6 rounded-full transition-colors relative",
                  settings.trackerBlocking ? "bg-red-500" : "bg-nature-300"
                )}
              >
                <div className={cn(
                  "absolute top-1 w-4 h-4 bg-white rounded-full transition-all",
                  settings.trackerBlocking ? "right-1" : "left-1"
                )} />
              </button>
            </div>

            <div className="flex items-center justify-between p-4 bg-white border border-nature-200 rounded-3xl hover:border-forest-200 transition-colors">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-forest-50 text-forest-600 rounded-2xl"><Globe size={20} /></div>
                <div>
                  <h4 className="text-sm font-bold text-nature-900">Built-in VPN</h4>
                  <p className="text-[10px] text-nature-500">Encrypt browsing traffic.</p>
                </div>
              </div>
              <button 
                onClick={() => updateSetting('vpnEnabled', !settings.vpnEnabled)}
                className={cn(
                  "w-12 h-6 rounded-full transition-colors relative",
                  settings.vpnEnabled ? "bg-forest-500" : "bg-nature-300"
                )}
              >
                <div className={cn(
                  "absolute top-1 w-4 h-4 bg-white rounded-full transition-all",
                  settings.vpnEnabled ? "right-1" : "left-1"
                )} />
              </button>
            </div>

            <div className="flex items-center justify-between p-4 bg-white border border-nature-200 rounded-3xl hover:border-forest-200 transition-colors">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-amber-50 text-amber-600 rounded-2xl"><Key size={20} /></div>
                <div>
                  <h4 className="text-sm font-bold text-nature-900">Password Manager</h4>
                  <p className="text-[10px] text-nature-500">Secure storage & auto-fill.</p>
                </div>
              </div>
              <ChevronRight size={20} className="text-nature-400" />
            </div>
          </div>
        </section>

        {/* Maintenance */}
        <section>
          <div className="flex gap-4">
            <button className="flex-1 flex flex-col items-center gap-2 p-4 rounded-3xl bg-nature-50 border border-nature-200 hover:bg-nature-100 transition-all font-bold text-nature-600 text-xs text-center">
              <Download size={20} className="mb-2" />
              Export Data
            </button>
            <button className="flex-1 flex flex-col items-center gap-2 p-4 rounded-3xl bg-nature-50 border border-nature-200 hover:bg-nature-100 transition-all font-bold text-nature-600 text-xs text-center">
              <Trash2 size={20} className="mb-2 text-red-500" />
              Reset All
            </button>
          </div>
        </section>
      </div>

      <div className="p-8 bg-nature-100 border-t border-nature-200 text-center">
        <p className="text-[10px] font-bold text-nature-400 uppercase tracking-widest mb-1">Nature Browser v1.0.4</p>
        <p className="text-[10px] text-nature-500">Based on Firefox Engine • Privacy by Design</p>
      </div>
    </motion.div>
  );
}
