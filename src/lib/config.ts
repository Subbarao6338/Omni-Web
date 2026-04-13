import { Capacitor } from '@capacitor/core';

// This URL should be the production URL where the backend is hosted.
// If the user hasn't deployed it yet, they will need to update this.
// For now, we'll try to use a reasonable default or allow it to be configured via env.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (Capacitor.isNativePlatform() ? 'https://omni-web.vercel.app' : '');

export const isNative = Capacitor.isNativePlatform();

/**
 * Gets the absolute URL for a given relative API path.
 */
export function getAbsoluteApiUrl(path: string): string {
  if (path.startsWith('http')) return path;
  const cleanPath = path.startsWith('/') ? path : `/${path}`;

  // If we are in native and have a base URL, use it.
  if (isNative && API_BASE_URL) {
    return `${API_BASE_URL}${cleanPath}`;
  }

  // Otherwise return the relative path (works in web/dev)
  return cleanPath;
}
