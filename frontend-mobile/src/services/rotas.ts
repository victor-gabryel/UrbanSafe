import { http } from './http';
import type { PerfilDeslocamento, PontoRota, RotaCalculada } from './types';

export const rotasApi = {
  /** Público: funciona sem login; com login a consulta é gravada no histórico. */
  calcular: (origem: PontoRota, destino: PontoRota, perfil: PerfilDeslocamento) =>
    http
      .post<RotaCalculada>(
        '/api/rotas/calcular',
        { origem, destino, perfil, preferencias: { considerarOcorrencias: true, considerarDistanciaDasOcorrencias: true } },
        { timeout: 30000 },
      )
      .then((r) => r.data),
};
