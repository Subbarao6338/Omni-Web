import { useState, useEffect, useCallback, useRef } from 'react';
import { 
  onAuthStateChanged,
  User as FirebaseUser
} from 'firebase/auth';
import {
  doc,
  collection,
  onSnapshot,
  setDoc,
  deleteDoc,
  query,
  orderBy,
  limit,
  serverTimestamp
} from 'firebase/firestore';
import { auth, db, handleFirestoreError, OperationType } from '../firebase';
import { 
  BrowserTab, 
  BrowserSettings, 
  Bookmark, 
  HistoryItem, 
  DownloadItem,
  Theme
} from '../types';

const INITIAL_SETTINGS: BrowserSettings = {
  theme: 'earth',
  fontSize: 'medium',
  layout: 'top',
  interfaceMode: 'light',
  privacyMode: false,
  trackerBlocking: true,
  vpnEnabled: false,
  autoDiscardTabs: false,
  showNewsFeed: true,
  searchEngine: 'google',
  startPageWidgets: ['weather', 'productivity', 'garden'],
  tabGroups: [],
  zenModeEnabled: false,
  gardenLevel: 1
};

export function useBrowser() {
  const [user, setUser] = useState<FirebaseUser | null>(null);
  const [isAuthReady, setIsAuthReady] = useState(false);
  const [tabs, setTabs] = useState<BrowserTab[]>([
    { id: '1', title: 'New Tab', url: 'nature://start', isLoading: false }
  ]);
  const [activeTabId, setActiveTabId] = useState<string>('1');
  const [settings, setSettings] = useState<BrowserSettings>(INITIAL_SETTINGS);
  const [bookmarks, setBookmarks] = useState<Bookmark[]>([]);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [downloads, setDownloads] = useState<DownloadItem[]>([]);
  const [isPrivate, setIsPrivate] = useState(false);

  // Track initialization
  const isInitialized = useRef(false);

  // Auth Listener
  useEffect(() => {
    return onAuthStateChanged(auth, (u) => {
      setUser(u);
      setIsAuthReady(true);
    });
  }, []);

  // Sync from Firestore
  useEffect(() => {
    if (!isAuthReady || !user) return;

    const userDocRef = doc(db, 'users', user.uid);
    const unsubSettings = onSnapshot(userDocRef, (snap) => {
      if (snap.exists()) {
        setSettings(snap.data() as BrowserSettings);
      } else {
        // Init remote settings with defaults
        setDoc(userDocRef, INITIAL_SETTINGS).catch(e => handleFirestoreError(e, OperationType.WRITE, `users/${user.uid}`));
      }
    }, (e) => handleFirestoreError(e, OperationType.GET, `users/${user.uid}`));

    const bookmarksRef = collection(db, 'users', user.uid, 'bookmarks');
    const unsubBookmarks = onSnapshot(bookmarksRef, (snap) => {
      const bmarks: Bookmark[] = [];
      snap.forEach(d => bmarks.push(d.data() as Bookmark));
      setBookmarks(bmarks);
    }, (e) => handleFirestoreError(e, OperationType.LIST, `users/${user.uid}/bookmarks`));

    const historyRef = query(collection(db, 'users', user.uid, 'history'), orderBy('timestamp', 'desc'), limit(50));
    const unsubHistory = onSnapshot(historyRef, (snap) => {
      const hist: HistoryItem[] = [];
      snap.forEach(d => hist.push(d.data() as HistoryItem));
      setHistory(hist);
    }, (e) => handleFirestoreError(e, OperationType.LIST, `users/${user.uid}/history`));

    return () => {
      unsubSettings();
      unsubBookmarks();
      unsubHistory();
    };
  }, [user, isAuthReady]);

  // Fallback to LocalStorage if not logged in
  useEffect(() => {
    if (user || !isAuthReady) return;
    
    const saved = localStorage.getItem('nature_browser_data');
    if (saved) {
      try {
        const data = JSON.parse(saved);
        if (data.settings) setSettings(data.settings);
        if (data.bookmarks) setBookmarks(data.bookmarks);
        if (data.history) setHistory(data.history);
        if (data.downloads) setDownloads(data.downloads);
      } catch (e) {
        console.error('Failed to load browser data', e);
      }
    }
  }, [user, isAuthReady]);

  // Sync settings TO firestore
  const updateSettings = useCallback((newSettings: BrowserSettings) => {
    setSettings(newSettings);
    if (user) {
      setDoc(doc(db, 'users', user.uid), newSettings)
        .catch(e => handleFirestoreError(e, OperationType.WRITE, `users/${user.uid}`));
    } else {
      const data = { settings: newSettings, bookmarks, history, downloads };
      localStorage.setItem('nature_browser_data', JSON.stringify(data));
    }
  }, [user, bookmarks, history, downloads]);

  const activeTab = tabs.find(t => t.id === activeTabId) || tabs[0];

  const addTab = useCallback((url: string = 'nature://start') => {
    const newId = Math.random().toString(36).substr(2, 9);
    const newTab: BrowserTab = { id: newId, title: 'New Tab', url, isLoading: false };
    setTabs(prev => [...prev, newTab]);
    setActiveTabId(newId);
  }, []);

  const closeTab = useCallback((id: string) => {
    if (tabs.length === 1) return;
    setTabs(prev => prev.filter(t => t.id !== id));
    if (activeTabId === id) {
      const idx = tabs.findIndex(t => t.id === id);
      setActiveTabId(tabs[idx === 0 ? 1 : idx - 1].id);
    }
  }, [tabs, activeTabId]);

  const navigate = useCallback((url: string) => {
    let finalUrl = url;
    if (!url.startsWith('nature://') && !url.startsWith('http')) {
      finalUrl = `https://www.google.com/search?q=${encodeURIComponent(url)}`;
    }

    setTabs(prev => prev.map(t => t.id === activeTabId ? { ...t, url: finalUrl, title: finalUrl } : t));
    
    if (!isPrivate) {
      const historyItem: HistoryItem = {
        id: Math.random().toString(36).substr(2, 9),
        title: finalUrl,
        url: finalUrl,
        timestamp: Date.now()
      };
      
      if (user) {
        setDoc(doc(db, 'users', user.uid, 'history', historyItem.id), historyItem)
          .catch(e => handleFirestoreError(e, OperationType.WRITE, `users/${user.uid}/history/${historyItem.id}`));
      } else {
        setHistory(prev => [historyItem, ...prev].slice(0, 100));
      }
    }
  }, [activeTabId, isPrivate, user]);

  const togglePrivate = useCallback(() => {
    setIsPrivate(prev => !prev);
    if (!isPrivate) {
      addTab('nature://start');
    }
  }, [isPrivate, addTab]);

  const toggleReaderMode = useCallback(() => {
    setTabs(prev => prev.map(t => 
      t.id === activeTabId ? { ...t, isReaderMode: !t.isReaderMode } : t
    ));
  }, [activeTabId]);

  const syncDownloads = useCallback((items: DownloadItem[]) => {
    setDownloads(items);
    if (user) {
      items.forEach(d => {
        setDoc(doc(db, 'users', user.uid, 'downloads', d.id), d)
          .catch(e => handleFirestoreError(e, OperationType.WRITE, `users/${user.uid}/downloads/${d.id}`));
      });
    }
  }, [user]);

  const startSimulation = useCallback((id: string) => {
    const intervalId = setInterval(() => {
      setDownloads(current => {
        const item = current.find(d => d.id === id);
        if (!item || item.status !== 'downloading') {
          clearInterval(intervalId);
          return current;
        }

        const newProgress = Math.min(100, item.progress + Math.random() * 15);
        const newItem: DownloadItem = { 
          ...item, 
          progress: newProgress, 
          status: newProgress === 100 ? 'completed' : 'downloading' 
        };

        if (newProgress === 100) {
          clearInterval(intervalId);
        }

        if (user) {
          setDoc(doc(db, 'users', user.uid, 'downloads', newItem.id), newItem);
        }
        
        return current.map(d => d.id === id ? newItem : d);
      });
    }, 1000);
  }, [user]);

  const downloadMedia = useCallback((url: string, filename: string) => {
    const newId = Math.random().toString(36).substr(2, 9);
    const newDownload: DownloadItem = {
      id: newId,
      filename,
      url,
      progress: 0,
      status: 'downloading',
      size: 'Unknown',
      mimeType: filename.endsWith('.mp4') ? 'video/mp4' : (filename.endsWith('.jpg') ? 'image/jpeg' : 'application/octet-stream')
    };
    setDownloads(prev => [newDownload, ...prev]);

    if (user) {
      setDoc(doc(db, 'users', user.uid, 'downloads', newDownload.id), newDownload)
        .catch(e => handleFirestoreError(e, OperationType.WRITE, `users/${user.uid}/downloads/${newDownload.id}`));
    }

    startSimulation(newId);
  }, [user, startSimulation]);

  const toggleDownload = useCallback((id: string) => {
    setDownloads(prev => {
      const item = prev.find(d => d.id === id);
      if (!item) return prev;

      const newStatus = item.status === 'downloading' ? 'failed' : 'downloading';
      const updated = prev.map(d => d.id === id ? { ...d, status: newStatus as any } : d);

      if (newStatus === 'downloading') {
        startSimulation(id);
      }

      return updated;
    });
  }, [startSimulation]);

  return {
    user,
    isAuthReady,
    tabs,
    setTabs,
    activeTab,
    activeTabId,
    setActiveTabId,
    addTab,
    closeTab,
    navigate,
    settings,
    setSettings: updateSettings,
    bookmarks,
    setBookmarks,
    history,
    downloads,
    setDownloads,
    isPrivate,
    togglePrivate,
    toggleReaderMode,
    downloadMedia,
    toggleDownload
  };
}
