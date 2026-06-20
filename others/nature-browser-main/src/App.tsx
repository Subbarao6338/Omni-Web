/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Shield, 
  ShieldAlert,
  Search, 
  Plus, 
  X, 
  ChevronLeft, 
  ChevronRight, 
  RotateCw, 
  Settings, 
  Download, 
  Bookmark as BookmarkIcon,
  Globe,
  Lock,
  Menu,
  MoreVertical,
  History,
  LayoutDashboard,
  Cloud,
  Zap,
  BarChart3,
  BookOpen,
  Activity,
  ShieldCheck,
  Layout,
  MessageSquare,
  EyeOff,
  Leaf
} from 'lucide-react';
import { useBrowser } from './hooks/useBrowser';
import { cn } from './lib/utils';
import { BrowserTab, TabGroup } from './types';

// Sub-components
import { StartPage } from './components/Browser/StartPage';
import { SettingsPanel } from './components/Browser/SettingsPanel';
import { MediaGrabber } from './components/Browser/MediaGrabber';
import { DownloadsPanel } from './components/Browser/DownloadsPanel';
import { PageToolsSidebar } from './components/Browser/PageToolsSidebar';
import { getWebSummary } from './services/geminiService';

export default function App() {
  const browser = useBrowser();
  const [showSettings, setShowSettings] = useState(false);
  const [showDownloads, setShowDownloads] = useState(false);
  const [showMediaGrabber, setShowMediaGrabber] = useState(false);
  const [showPageTools, setShowPageTools] = useState(false);
  const [showPrivacyDashboard, setShowPrivacyDashboard] = useState(false);
  const [showTabGroupMenu, setShowTabGroupMenu] = useState(false);
  const [urlInput, setUrlInput] = useState(browser.activeTab.url);
  const [summary, setSummary] = useState<string | null>(null);
  const [isSummarizing, setIsSummarizing] = useState(false);

  useEffect(() => {
    setUrlInput(browser.activeTab.url);
    setSummary(null);
  }, [browser.activeTab.url]);

  const handleSummarize = async () => {
    setIsSummarizing(true);
    const text = await getWebSummary(browser.activeTab.url);
    setSummary(text || "No summary available.");
    setIsSummarizing(false);
  };

  const handleUrlSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    browser.navigate(urlInput);
  };

  const isStartPage = browser.activeTab.url === 'nature://start';

  if (browser.settings.zenModeEnabled) {
    return (
      <div 
        onMouseMove={() => browser.setSettings({ ...browser.settings, zenModeEnabled: false })}
        className={cn(
          "fixed inset-0 flex flex-col items-center justify-center p-12 transition-all duration-1000",
          browser.settings.interfaceMode === 'dark' ? "bg-stone-950" : "bg-nature-50"
        )}
      >
        <motion.div 
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="max-w-4xl w-full bg-white p-16 rounded-[4rem] shadow-2xl space-y-8 text-center"
        >
          <div className="flex justify-center"><Leaf size={48} className="text-forest-400" /></div>
          <h1 className="text-5xl font-display font-black text-nature-900 leading-tight">
            You are in Zen Mode.
          </h1>
          <p className="text-xl text-nature-500 font-medium">
            Focus on your thoughts. All distractions are hidden. Move your mouse to return.
          </p>
          <div className="pt-12">
            <div className="w-16 h-1 bg-gradient-to-r from-transparent via-forest-500 to-transparent mx-auto rounded-full" />
          </div>
        </motion.div>
      </div>
    );
  }

  const urlBar = (
    <div className={cn(
      "z-50 px-2 py-1 border-nature-300",
      browser.settings.layout === 'bottom' ? "border-t" : "border-b",
      browser.isPrivate ? "bg-stone-800 border-stone-700" : "bg-nature-200 border-nature-300"
    )}>
      <div className="flex items-center gap-3 px-2 py-1">
        <div className="flex items-center gap-1">
          <button className="p-2 hover:bg-nature-300/50 rounded-lg"><ChevronLeft size={20} /></button>
          <button className="p-2 hover:bg-nature-300/50 rounded-lg"><ChevronRight size={20} /></button>
          <button className="p-2 hover:bg-nature-300/50 rounded-lg"><RotateCw size={18} /></button>
        </div>

        <form onSubmit={handleUrlSubmit} className="flex-1">
          <div className={cn(
            "flex items-center gap-2 px-4 py-2 border rounded-full transition-all group focus-within:ring-2 relative",
            browser.isPrivate 
              ? "bg-stone-700 border-stone-600 focus-within:ring-amber-500/30" 
              : "bg-white border-nature-300 focus-within:ring-forest-500/30 shadow-inner"
          )}>
            <button 
              type="button"
              onClick={() => setShowPrivacyDashboard(!showPrivacyDashboard)}
              className="p-1 hover:bg-nature-300/50 rounded-full transition-colors"
            >
              {browser.isPrivate ? <Lock size={16} className="text-amber-500" /> : <ShieldCheck size={16} className="text-forest-500" />}
            </button>
            <input
              type="text"
              value={urlInput}
              onChange={(e) => setUrlInput(e.target.value)}
              className="flex-1 bg-transparent border-none outline-none text-sm font-medium"
              placeholder="Search or enter URL"
            />
            <button type="submit" className="p-1 hover:bg-nature-300/50 rounded-full">
              <Search size={16} />
            </button>

            {/* Privacy Dashboard Dropdown */}
            <AnimatePresence>
              {showPrivacyDashboard && (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 10 }}
                  className="absolute top-full left-0 mt-2 w-72 bg-white border border-nature-200 shadow-2xl rounded-3xl z-[60] p-6 overflow-hidden"
                >
                  <div className="flex items-center justify-between mb-6">
                    <h3 className="font-display font-bold text-lg text-nature-900">Privacy Health</h3>
                    <div className="p-2 bg-forest-50 text-forest-500 rounded-xl"><ShieldCheck size={20} /></div>
                  </div>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="p-2 bg-red-50 text-red-500 rounded-lg"><EyeOff size={14} /></div>
                        <span className="text-xs font-bold text-nature-600">Trackers Blocked</span>
                      </div>
                      <span className="text-sm font-black text-nature-900">4,129</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="p-2 bg-blue-50 text-blue-500 rounded-lg"><Activity size={14} /></div>
                        <span className="text-xs font-bold text-nature-600">Data Encrypted</span>
                      </div>
                      <span className="text-sm font-black text-nature-900">100%</span>
                    </div>
                    <div className="pt-4 border-t border-nature-100 flex flex-col gap-2">
                      <div className="text-[10px] font-bold text-nature-400 uppercase tracking-widest">Active Protections</div>
                      <div className="flex flex-wrap gap-2">
                        {['Fingerprinting', 'Cryptomining', 'Cross-site Tracking'].map(p => (
                          <span key={p} className="px-2 py-1 bg-nature-100 text-nature-600 text-[10px] font-bold rounded-lg">{p}</span>
                        ))}
                      </div>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </form>

        <div className="flex items-center gap-1">
          <button 
            onClick={() => setShowPageTools(!showPageTools)}
            className={cn(
              "p-2 rounded-lg transition-colors",
              showPageTools ? "bg-forest-500 text-white" : "hover:bg-nature-300/50"
            )}
            title="Page Intelligence"
          >
            <BarChart3 size={20} />
          </button>
          <button 
            onClick={() => browser.toggleReaderMode()}
            className={cn(
              "p-2 rounded-lg transition-colors",
              browser.activeTab.isReaderMode ? "bg-forest-500 text-white" : "hover:bg-nature-300/50"
            )}
            title="Toggle Reader Mode"
          >
            <BookOpen size={20} />
          </button>
          <button 
            onClick={() => setShowMediaGrabber(true)}
            className={cn(
              "p-2 rounded-lg transition-colors",
              "bg-forest-500/10 text-forest-600 hover:bg-forest-500/20"
            )}
            title="Media Grabber"
          >
            <Zap size={20} />
          </button>
          <button onClick={() => setShowDownloads(true)} className="p-2 hover:bg-nature-300/50 rounded-lg"><Download size={20} /></button>
          <button onClick={() => setShowSettings(true)} className="p-2 hover:bg-nature-300/50 rounded-lg"><Settings size={20} /></button>
        </div>
      </div>
    </div>
  );

  return (
    <div className={cn(
      "fixed inset-0 flex flex-col transition-colors duration-500 overflow-hidden",
      browser.isPrivate ? "bg-stone-900 text-stone-100" : "bg-nature-100 text-nature-900",
      browser.settings.interfaceMode === 'dark' && "dark",
      `theme-${browser.settings.theme}`,
      `text-size-${browser.settings.fontSize}`
    )}>
      {/* Tabs Bar (Always Top) */}
      <header className={cn(
        "z-50 px-2 pt-2 border-b shadow-sm",
        browser.isPrivate ? "bg-stone-800 border-stone-700" : "bg-nature-200 border-nature-300"
      )}>
        <div className="flex items-center gap-1 overflow-x-auto no-scrollbar">
          {browser.tabs.map((tab: any) => (
            <motion.div
              layoutId={tab.id}
              key={tab.id}
              onClick={() => browser.setActiveTabId(tab.id)}
              onContextMenu={(e) => {
                e.preventDefault();
                setShowTabGroupMenu(tab.id);
              }}
              className={cn(
                "flex items-center gap-2 px-3 py-2 rounded-t-lg cursor-pointer min-w-[140px] max-w-[200px] transition-all relative",
                browser.activeTabId === tab.id 
                  ? (browser.isPrivate ? "bg-stone-900 text-amber-500" : "bg-nature-100 text-nature-900 border-b-2 border-forest-500 shadow-[0_-4px_10px_rgba(0,0,0,0.05)]") 
                  : "hover:bg-nature-300/50 text-nature-600"
              )}
            >
              {tab.groupId && (
                <div className="absolute top-0 left-0 right-0 h-1 bg-forest-500 rounded-t-lg" />
              )}
              <Globe size={14} className="shrink-0" />
              <span className="text-xs truncate font-medium">{tab.title === 'nature://start' ? 'Nature Start' : tab.title}</span>
              <button 
                onClick={(e) => { e.stopPropagation(); browser.closeTab(tab.id); }}
                className="ml-auto p-0.5 hover:bg-black/10 rounded-full"
              >
                <X size={12} />
              </button>

              <AnimatePresence>
                {showTabGroupMenu === tab.id && (
                  <motion.div
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    className="absolute top-full left-0 mt-1 w-48 bg-white border border-nature-200 shadow-2xl rounded-2xl z-[70] p-2"
                  >
                    <div className="text-[10px] font-bold text-nature-400 uppercase tracking-widest px-2 py-1">Groups</div>
                    <button 
                      onClick={(e) => {
                        e.stopPropagation();
                        browser.setTabs((prev: any) => prev.map((t: any) => t.id === tab.id ? { ...t, groupId: t.groupId ? null : '1' } : t));
                        setShowTabGroupMenu(false);
                      }}
                      className="w-full text-left px-3 py-2 text-xs font-bold hover:bg-nature-50 rounded-xl flex items-center gap-2"
                    >
                      <Layout size={14} className="text-forest-500" />
                      {tab.groupId ? 'Remove from Group' : 'Add to Nature Group'}
                    </button>
                    <button 
                      onClick={() => setShowTabGroupMenu(false)}
                      className="w-full text-left px-3 py-2 text-[10px] uppercase font-bold text-nature-400 hover:text-nature-600"
                    >
                      Cancel
                    </button>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
          <button 
            onClick={() => browser.addTab()}
            className="p-2 hover:bg-nature-300/50 rounded-lg text-nature-600"
          >
            <Plus size={18} />
          </button>
        </div>
      </header>

      {browser.settings.layout === 'top' && urlBar}

      {/* Main Content Area */}
      <main className="flex-1 relative bg-white overflow-hidden">
        <AnimatePresence mode="wait">
          {isStartPage ? (
            <motion.div
              key="start-page"
              initial={{ opacity: 0, scale: 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 1.02 }}
              className="absolute inset-0"
            >
              <StartPage browser={browser} />
            </motion.div>
          ) : (
            <motion.div
              key="viewport"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 flex flex-col items-center justify-center p-8 text-center bg-nature-50"
            >
              <div className={cn(
                "max-w-3xl w-full transition-all duration-700",
                browser.activeTab.isReaderMode ? "scale-105" : ""
              )}>
                {browser.activeTab.isReaderMode ? (
                  <motion.div 
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-left bg-white p-12 rounded-3xl shadow-2xl space-y-6"
                  >
                    <h1 className="text-4xl font-display font-black text-nature-900 border-b pb-6">
                      {browser.activeTab.title}
                    </h1>
                    <div className="flex items-center gap-4 text-xs font-bold text-nature-400 uppercase tracking-widest">
                      <span>Nature Reader Mode</span>
                      <span>•</span>
                      <span>12 Min Read</span>
                      <span>•</span>
                      <span>{browser.activeTab.url}</span>
                    </div>
                    <div className="prose prose-nature max-w-none text-nature-700 leading-relaxed text-lg">
                      <p className="first-letter:text-5xl first-letter:font-black first-letter:mr-3 first-letter:float-left">
                        This is the simplified reader view. All advertisements, tracking scripts, and navigational clutter have been removed for your concentration. 
                        Nature Browser uses an advanced heuristic to extract the primary content body and present it in our signature organic typography.
                      </p>
                      <img 
                        src="https://picsum.photos/seed/nature/1200/600" 
                        alt="Hero" 
                        className="rounded-2xl my-8 w-full h-80 object-cover"
                        referrerPolicy="no-referrer"
                      />
                      <p>
                        In this ultimate build, we prioritize your focus. Whether you are conducting academic research or reading a long-form article, 
                        the tools at your disposal—from AI summarization to Markdown export—are designed to work seamlessly with this unified content view.
                      </p>
                    </div>
                    <div className="pt-12 flex justify-center">
                      <button 
                        onClick={() => browser.toggleReaderMode()}
                        className="text-xs font-bold uppercase tracking-widest text-forest-500 hover:text-forest-700 transition-colors"
                      >
                        Exit Reader View
                      </button>
                    </div>
                  </motion.div>
                ) : (
                  <>
                    <Globe size={64} className="mx-auto mb-4 text-forest-300" />
                    <h2 className="text-2xl font-display text-nature-900 mb-2">Simulated Viewport</h2>
                    <p className="text-nature-600 mb-6">You are browsing: <span className="font-mono text-xs p-1 bg-nature-200 rounded">{browser.activeTab.url}</span></p>
                    
                    <div className="flex flex-col gap-4">
                      <div className="p-6 border-2 border-dashed border-nature-300 rounded-2xl bg-white shadow-sm italic text-nature-500">
                        "In a real browser, this would be an IFrame rendering the website. For security and demo purposes, we simulate the browsing experience here."
                      </div>

                      {/* AI Summary Section */}
                      <div className="p-6 bg-forest-50 border border-forest-100 rounded-2xl text-left">
                        <div className="flex items-center justify-between mb-4">
                          <div className="flex items-center gap-2 text-forest-600">
                            <Zap size={18} className="fill-forest-500" />
                            <span className="text-sm font-bold">Nature AI Insights</span>
                          </div>
                          <button 
                            onClick={handleSummarize}
                            disabled={isSummarizing}
                            className="text-xs font-bold text-forest-500 hover:text-forest-700 disabled:opacity-50"
                          >
                            {isSummarizing ? "Summarizing..." : (summary ? "Regenerate" : "Summarize Page")}
                          </button>
                        </div>
                        {summary ? (
                          <p className="text-sm text-nature-700 leading-relaxed font-medium">
                            {summary}
                          </p>
                        ) : (
                          <div className="h-20 flex items-center justify-center border-2 border-dashed border-forest-200 rounded-xl text-forest-300 text-xs font-bold italic">
                            {isSummarizing ? "Analyzing coordinates..." : "Click summarize to get AI page insights"}
                          </div>
                        )}
                      </div>
                    </div>

                    <button 
                      onClick={() => browser.navigate('nature://start')}
                      className="mt-8 px-6 py-2 bg-forest-500 text-white rounded-full font-medium hover:bg-forest-600 transition-colors shadow-lg"
                    >
                      Return to Nature Start
                    </button>
                  </>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Drawers / Overlays */}
        <AnimatePresence>
          {showSettings && (
            <SettingsPanel 
              onClose={() => setShowSettings(false)} 
              browser={browser} 
            />
          )}
          {showDownloads && (
            <DownloadsPanel 
              onClose={() => setShowDownloads(false)} 
              browser={browser} 
            />
          )}
          {showMediaGrabber && (
            <MediaGrabber 
              onClose={() => setShowMediaGrabber(false)} 
              browser={browser} 
            />
          )}
          {showPageTools && (
            <PageToolsSidebar
              url={browser.activeTab.url}
              browser={browser}
              onClose={() => setShowPageTools(false)}
              onAddDownload={(item) => browser.setDownloads([item, ...browser.downloads])}
            />
          )}
        </AnimatePresence>
      </main>

      {browser.settings.layout === 'bottom' && urlBar}

      {/* Status Bar */}
      <footer className={cn(
        "h-6 px-4 flex items-center justify-between text-[10px] font-medium border-t",
        browser.isPrivate ? "bg-stone-800 border-stone-700 text-stone-400" : "bg-nature-200 border-nature-300 text-nature-500"
      )}>
        <div className="flex gap-4">
          <span>Speed: 1.2 GB/s</span>
          <span>Privacy: Robust</span>
          {browser.settings.vpnEnabled && <span className="text-forest-500 flex items-center gap-1"><Shield size={10} /> VPN Active</span>}
        </div>
        <div className="flex gap-4 items-center">
          <button onClick={() => browser.togglePrivate()} className={cn(
            "px-2 py-0.5 rounded transition-colors",
            browser.isPrivate ? "bg-amber-500 text-black" : "bg-nature-300 hover:bg-nature-400"
          )}>
            {browser.isPrivate ? "Disable Private Mode" : "Private Session"}
          </button>
          <span>© 2026 Nature Browser</span>
        </div>
      </footer>
    </div>
  );
}
