import React, { useState } from 'react';
import { motion } from 'motion/react';
import { 
  X, 
  BarChart3, 
  FileJson, 
  FileText, 
  FileCode, 
  Share2, 
  Layout, 
  Zap, 
  Gauge, 
  Eye, 
  Scissors,
  Download,
  Printer,
  Copy,
  Plus,
  MessageCircle,
  Send,
  Sparkles
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { analyzePage, AnalysisResult } from '../../services/analysisService';
import { exportToMarkdown, simulateDownload } from '../../services/exportService';
import { chatWithPage } from '../../services/geminiService';

interface PageToolsSidebarProps {
  url: string;
  onClose: () => void;
  onAddDownload: (item: any) => void;
  browser: any;
}

export function PageToolsSidebar({ url, onClose, onAddDownload, browser }: PageToolsSidebarProps) {
  const [activeTab, setActiveTab] = useState<'analysis' | 'export' | 'tools' | 'chat'>('analysis');
  const [analysis] = useState<AnalysisResult>(analyzePage(url));
  const [chatHistory, setChatHistory] = useState<{ role: 'user' | 'assistant'; content: string }[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || isTyping) return;
    
    const userMsg = inputMessage;
    setInputMessage('');
    setChatHistory(prev => [...prev, { role: 'user', content: userMsg }]);
    setIsTyping(true);

    const response = await chatWithPage(url, chatHistory, userMsg);
    setChatHistory(prev => [...prev, { role: 'assistant', content: response }]);
    setIsTyping(false);
  };

  const handleExport = (type: 'md' | 'pdf' | 'mhtml') => {
    const filename = `${url.replace(/https?:\/\//, '').replace(/\//g, '_')}.${type}`;
    const mdContent = exportToMarkdown(url, browser.activeTab.title);
    
    // Simulate internal download
    const download = simulateDownload(filename, mdContent, type === 'md' ? 'text/markdown' : 'application/pdf');
    onAddDownload(download);
    alert(`Exporting page as ${type.toUpperCase()}... Check downloads.`);
  };

  return (
    <motion.div
      initial={{ x: '100%' }}
      animate={{ x: 0 }}
      exit={{ x: '100%' }}
      className="absolute top-0 right-0 w-80 h-full bg-white border-l border-nature-300 shadow-2xl z-50 flex flex-col font-sans"
    >
      <div className="p-4 border-b border-nature-300 flex items-center justify-between bg-nature-100">
        <h2 className="font-display font-bold text-lg text-nature-900 flex items-center gap-2">
          <BarChart3 size={20} className="text-forest-500" />
          Page Intelligence
        </h2>
        <button onClick={onClose} className="p-1 hover:bg-nature-300 rounded-lg">
          <X size={20} />
        </button>
      </div>

      <div className="flex border-b border-nature-300">
        {(['analysis', 'chat', 'export', 'tools'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={cn(
              "flex-1 py-3 text-[10px] font-bold uppercase tracking-wider transition-all",
              activeTab === tab ? "bg-white text-forest-500 border-b-2 border-forest-500" : "bg-nature-50 text-nature-500 hover:bg-nature-100"
            )}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-4 custom-scrollbar flex flex-col">
        {activeTab === 'chat' && (
          <div className="flex-1 flex flex-col min-h-0 bg-nature-50 rounded-2xl overflow-hidden border border-nature-200">
            <div className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
              {chatHistory.length === 0 && (
                <div className="flex flex-col items-center justify-center h-full text-center space-y-4 opacity-50 px-4">
                  <Sparkles size={32} className="text-forest-400" />
                  <p className="text-xs font-bold leading-relaxed">
                    Ask me anything about this page. I can summarize, explain concepts, or find specific details.
                  </p>
                </div>
              )}
              {chatHistory.map((msg, i) => (
                <div key={i} className={cn(
                  "flex flex-col max-w-[85%] space-y-1",
                  msg.role === 'user' ? "ml-auto items-end" : "items-start"
                )}>
                  <div className={cn(
                    "p-3 rounded-2xl text-xs font-medium shadow-sm",
                    msg.role === 'user' ? "bg-forest-500 text-white" : "bg-white text-nature-800 border border-nature-200"
                  )}>
                    {msg.content}
                  </div>
                </div>
              ))}
              {isTyping && (
                <div className="flex items-center gap-2 text-forest-400">
                  <motion.div 
                    animate={{ scale: [1, 1.2, 1] }} 
                    transition={{ repeat: Infinity, duration: 1 }}
                    className="w-1.5 h-1.5 bg-current rounded-full" 
                  />
                  <span className="text-[10px] font-bold uppercase italic">Analyzing...</span>
                </div>
              )}
            </div>
            <div className="p-3 bg-white border-t border-nature-200 flex gap-2">
              <input 
                type="text" 
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                placeholder="Ask about this page..."
                className="flex-1 bg-nature-100 rounded-xl px-4 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-forest-500"
              />
              <button 
                onClick={handleSendMessage}
                disabled={!inputMessage.trim() || isTyping}
                className="p-2 bg-forest-500 text-white rounded-xl hover:bg-forest-600 disabled:opacity-50 transition-colors"
              >
                <Send size={16} />
              </button>
            </div>
          </div>
        )}

        {activeTab === 'analysis' && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-3">
              <div className="p-3 bg-nature-50 border border-nature-200 rounded-xl text-center">
                <div className="text-xs font-bold text-nature-500 uppercase mb-1">SEO</div>
                <div className={cn("text-2xl font-black", analysis.seo.score > 80 ? "text-forest-500" : "text-amber-500")}>
                  {analysis.seo.score}
                </div>
              </div>
              <div className="p-3 bg-nature-50 border border-nature-200 rounded-xl text-center">
                <div className="text-xs font-bold text-nature-500 uppercase mb-1">Speed</div>
                <div className="text-2xl font-black text-forest-500">
                  {analysis.performance.score}
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <section>
                <h3 className="text-[10px] font-bold text-nature-400 uppercase tracking-widest mb-2 flex items-center gap-2">
                  <Eye size={12} /> Optimization Insights
                </h3>
                <div className="space-y-2">
                  {analysis.seo.issues.map((issue, i) => (
                    <div key={i} className="flex gap-2 text-xs text-nature-700 bg-nature-50 p-2 rounded-lg border-l-2 border-amber-400">
                      <span className="font-bold shrink-0">•</span>
                      <span>{issue}</span>
                    </div>
                  ))}
                </div>
              </section>

              <section>
                <h3 className="text-[10px] font-bold text-nature-400 uppercase tracking-widest mb-2 flex items-center gap-2">
                  <Gauge size={12} /> Core Web Vitals
                </h3>
                <div className="space-y-1">
                  {analysis.performance.metrics.map((m, i) => (
                    <div key={i} className="flex items-center justify-between text-xs py-1 border-b border-nature-100">
                      <span className="text-nature-600">{m.label}</span>
                      <span className="font-mono font-bold text-forest-600">{m.value}</span>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          </div>
        )}

        {activeTab === 'export' && (
          <div className="space-y-4">
            <div className="p-4 bg-forest-50 border border-forest-100 rounded-2xl mb-4">
              <p className="text-xs text-forest-700 leading-relaxed italic">
                "Archive the current page into various formats. Nature Pro preserves layouts and media for offline reference."
              </p>
            </div>
            
            <button 
              onClick={() => handleExport('pdf')}
              className="w-full flex items-center justify-between p-3 bg-white border border-nature-200 rounded-xl hover:bg-nature-50 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <div className="p-2 bg-red-50 text-red-500 rounded-lg group-hover:bg-red-500 group-hover:text-white transition-colors">
                  <Printer size={18} />
                </div>
                <div className="text-left">
                  <div className="text-sm font-bold">Save as PDF</div>
                  <div className="text-[10px] text-nature-500">Universal document format</div>
                </div>
              </div>
            </button>

            <button 
              onClick={() => handleExport('md')}
              className="w-full flex items-center justify-between p-3 bg-white border border-nature-200 rounded-xl hover:bg-nature-50 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <div className="p-2 bg-blue-50 text-blue-500 rounded-lg group-hover:bg-blue-500 group-hover:text-white transition-colors">
                  <FileCode size={18} />
                </div>
                <div className="text-left">
                  <div className="text-sm font-bold">Export to Markdown</div>
                  <div className="text-[10px] text-nature-500">Preserve text & media structure</div>
                </div>
              </div>
            </button>

            <button 
              onClick={() => handleExport('mhtml')}
              className="w-full flex items-center justify-between p-3 bg-white border border-nature-200 rounded-xl hover:bg-nature-50 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <div className="p-2 bg-amber-50 text-amber-500 rounded-lg group-hover:bg-amber-500 group-hover:text-white transition-colors">
                  <Layout size={18} />
                </div>
                <div className="text-left">
                  <div className="text-sm font-bold">Save as Web Bundle</div>
                  <div className="text-[10px] text-nature-500">Full page capture (MHTML)</div>
                </div>
              </div>
            </button>
          </div>
        )}

        {activeTab === 'tools' && (
          <div className="space-y-3">
            <h3 className="text-[10px] font-bold text-nature-400 uppercase tracking-widest mb-2">Utility Stack</h3>
            <div className="grid grid-cols-2 gap-2">
              <button className="flex flex-col items-center gap-2 p-4 bg-nature-50 rounded-xl hover:bg-nature-100 transition-all border border-transparent hover:border-nature-300">
                <Scissors size={20} className="text-nature-600" />
                <span className="text-[10px] font-bold uppercase">Screenshot</span>
              </button>
              <button 
                onClick={() => alert("Simulating Network Inspection... \n- Latency: 45ms \n- Packet Loss: 0% \n- Origin: Cloudflare edge")}
                className="flex flex-col items-center gap-2 p-4 bg-nature-50 rounded-xl hover:bg-nature-100 transition-all border border-transparent hover:border-nature-300"
              >
                <Zap size={20} className="text-nature-600" />
                <span className="text-[10px] font-bold uppercase">Net Inspect</span>
              </button>
              <button className="flex flex-col items-center gap-2 p-4 bg-nature-50 rounded-xl hover:bg-nature-100 transition-all border border-transparent hover:border-nature-300">
                <FileJson size={20} className="text-nature-600" />
                <span className="text-[10px] font-bold uppercase">JSON Fetch</span>
              </button>
              <button className="flex flex-col items-center gap-2 p-4 bg-nature-50 rounded-xl hover:bg-nature-100 transition-all border border-transparent hover:border-nature-300">
                <Share2 size={20} className="text-nature-600" />
                <span className="text-[10px] font-bold uppercase">Cast View</span>
              </button>
              <button 
                onClick={() => alert("Color Picker Active: Select any pixel on page.")}
                className="flex flex-col items-center gap-2 p-4 bg-nature-50 rounded-xl hover:bg-nature-100 transition-all border border-transparent hover:border-nature-300"
              >
                <div className="w-5 h-5 rounded-full bg-gradient-to-tr from-red-500 via-green-500 to-blue-500" />
                <span className="text-[10px] font-bold uppercase">Color Pick</span>
              </button>
              <button 
                onClick={() => alert("Extracting all text from images using OCR...")}
                className="flex flex-col items-center gap-2 p-4 bg-nature-50 rounded-xl hover:bg-nature-100 transition-all border border-transparent hover:border-nature-300"
              >
                <FileText size={20} className="text-nature-600" />
                <span className="text-[10px] font-bold uppercase">Text OCR</span>
              </button>
              <button 
                onClick={() => browser.setSettings({ ...browser.settings, zenModeEnabled: true })}
                className="flex flex-col items-center gap-2 p-4 bg-nature-50 rounded-xl hover:bg-nature-100 transition-all border border-transparent hover:border-nature-300"
              >
                <Eye size={20} className="text-nature-600" />
                <span className="text-[10px] font-bold uppercase">Zen Bloom</span>
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="p-4 bg-nature-900 text-white flex items-center justify-between">
        <div className="text-[10px] font-bold tracking-tighter uppercase opacity-60">Nature Ultimate Build</div>
        <div className="flex items-center gap-1">
          <Zap size={10} className="text-forest-400 fill-forest-400" />
          <span className="text-[10px] font-bold">V 2.4.0</span>
        </div>
      </div>
    </motion.div>
  );
}
