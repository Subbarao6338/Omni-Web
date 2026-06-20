export type Theme = 'earth' | 'forest' | 'water' | 'sand' | 'dark' | 'sun' | 'lava' | 'arctic' | 'moss' | 'midnight' | 'bloom' | 'savanna';

export interface Bookmark {
  id: string;
  title: string;
  url: string;
  icon?: string;
  folder?: string;
  tags?: string[];
}

export interface HistoryItem {
  id: string;
  title: string;
  url: string;
  timestamp: number;
}

export interface DownloadItem {
  id: string;
  filename: string;
  url: string;
  progress: number;
  status: 'downloading' | 'completed' | 'failed';
  size: string;
  mimeType: string;
}

export interface BrowserTab {
  id: string;
  title: string;
  url: string;
  isLoading: boolean;
  favicon?: string;
  isReaderMode?: boolean;
  groupId?: string;
}

export interface TabGroup {
  id: string;
  name: string;
  color: string;
}

export interface BrowserSettings {
  theme: Theme;
  fontSize: 'small' | 'medium' | 'large';
  layout: 'top' | 'bottom';
  interfaceMode: 'light' | 'dark' | 'system';
  privacyMode: boolean;
  trackerBlocking: boolean;
  vpnEnabled: boolean;
  autoDiscardTabs: boolean;
  showNewsFeed: boolean;
  searchEngine: 'google' | 'duckduckgo' | 'bing' | 'ecosia';
  startPageWidgets: string[];
  tabGroups: TabGroup[];
  zenModeEnabled: boolean;
  gardenLevel: number;
}
