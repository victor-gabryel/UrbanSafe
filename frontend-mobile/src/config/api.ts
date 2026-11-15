import Constants from 'expo-constants';

/**
 * Endereço do backend, visto PELO CELULAR.
 *
 * Ordem de prioridade:
 *  1. EXPO_PUBLIC_API_URL (arquivo .env do frontend-mobile)  -> ex.: http://192.168.0.100:8080
 *  2. Detecção automática: o Expo Go carrega o app do PC, então o IP do Metro (hostUri) é o IP do PC;
 *     o backend roda nesse mesmo PC na porta 8080.
 *
 * Para trocar o endereço, edite o .env e reinicie com: npx expo start --clear
 */
const BACKEND_PORT = 8080;

function detectarPeloExpo(): string | null {
  const hostUri = Constants.expoConfig?.hostUri; // "192.168.0.100:8081"
  if (!hostUri) return null;
  const host = hostUri.split(':')[0];
  if (!host || host === 'localhost' || host === '127.0.0.1') return null;
  return `http://${host}:${BACKEND_PORT}`;
}

function normalizar(url: string): string {
  return url.trim().replace(/\/+$/, '');
}

const doEnv = process.env.EXPO_PUBLIC_API_URL;

export const API_URL: string = normalizar(
  doEnv && doEnv.trim().length > 0 ? doEnv : detectarPeloExpo() ?? `http://192.168.0.100:${BACKEND_PORT}`,
);

export const API_TIMEOUT_MS = 15000;

/** Região inicial do mapa (Recife-PE), usada quando a localização não está disponível. */
export const REGIAO_PADRAO = {
  latitude: -8.0476,
  longitude: -34.877,
  latitudeDelta: 0.05,
  longitudeDelta: 0.05,
};
