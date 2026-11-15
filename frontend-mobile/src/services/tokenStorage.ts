import * as SecureStore from 'expo-secure-store';

const K_ACCESS = 'urbansafe.accessToken';
const K_REFRESH = 'urbansafe.refreshToken';

let accessCache: string | null = null;
let refreshCache: string | null = null;

export const tokenStorage = {
  async load(): Promise<{ access: string | null; refresh: string | null }> {
    try {
      accessCache = await SecureStore.getItemAsync(K_ACCESS);
      refreshCache = await SecureStore.getItemAsync(K_REFRESH);
    } catch {
      accessCache = null;
      refreshCache = null;
    }
    return { access: accessCache, refresh: refreshCache };
  },
  getAccess: () => accessCache,
  getRefresh: () => refreshCache,
  async save(access: string, refresh: string): Promise<void> {
    accessCache = access;
    refreshCache = refresh;
    await SecureStore.setItemAsync(K_ACCESS, access);
    await SecureStore.setItemAsync(K_REFRESH, refresh);
  },
  async clear(): Promise<void> {
    accessCache = null;
    refreshCache = null;
    try {
      await SecureStore.deleteItemAsync(K_ACCESS);
      await SecureStore.deleteItemAsync(K_REFRESH);
    } catch {
      /* nada a fazer */
    }
  },
};
