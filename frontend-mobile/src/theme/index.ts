export const colors = {
  primary: '#1457B8',
  primaryDark: '#0F4596',
  primaryLight: '#E8F0FC',
  bg: '#F6F8FB',
  card: '#FFFFFF',
  border: '#DDE4EE',
  text: '#0F1B2D',
  muted: '#5F6E85',
  danger: '#D32F2F',
  success: '#2E7D32',
  warning: '#ED8F00',
};

export const radius = { sm: 10, md: 14, lg: 20, pill: 999 };

export const shadow = {
  shadowColor: '#0B1F3F',
  shadowOpacity: 0.12,
  shadowRadius: 10,
  shadowOffset: { width: 0, height: 4 },
  elevation: 4,
};

/** Cores dos marcadores por tipo. Tipos não mapeados usam a paleta por id. */
const POR_NOME: Record<string, string> = {
  roubo: '#D62828',
  furto: '#E07A00',
};
const PALETA = ['#0E8A7D', '#7B3FE4', '#3D4B63', '#C2185B', '#2E7D32', '#0277BD', '#8D6E63'];

function normalizar(s: string): string {
  return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

export function corDoTipo(tipoId: number, nome: string): string {
  const n = normalizar(nome);
  if (POR_NOME[n]) return POR_NOME[n];
  return PALETA[Math.abs(tipoId) % PALETA.length];
}
