import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.omni.browser',
  appName: 'Omni Browser',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  }
};

export default config;
