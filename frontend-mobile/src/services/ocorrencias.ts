import { http } from './http';
import type {
  Bbox,
  CriarOcorrenciaPayload,
  FiltrosMapa,
  MapaCalor,
  MapaOcorrencias,
  Ocorrencia,
  Pagina,
  TipoOcorrencia,
} from './types';

export const bboxParaParam = (b: Bbox) => `${b.minLon},${b.minLat},${b.maxLon},${b.maxLat}`;

function paramsMapa(b: Bbox, f: FiltrosMapa) {
  return {
    bbox: bboxParaParam(b),
    ...(f.tipoId ? { tipo: f.tipoId } : {}),
    ...(f.dataInicio ? { dataInicio: f.dataInicio } : {}),
  };
}

export const ocorrenciasApi = {
  tipos: () => http.get<TipoOcorrencia[]>('/api/tipos-ocorrencia').then((r) => r.data),

  mapa: (b: Bbox, f: FiltrosMapa, limit = 200) =>
    http.get<MapaOcorrencias>('/api/mapa/ocorrencias', { params: { ...paramsMapa(b, f), limit } }).then((r) => r.data),

  calor: (b: Bbox, f: FiltrosMapa, celula: number) =>
    http.get<MapaCalor>('/api/mapa/calor', { params: { ...paramsMapa(b, f), celula } }).then((r) => r.data),

  criar: (p: CriarOcorrenciaPayload) => http.post<Ocorrencia>('/api/ocorrencias', p).then((r) => r.data),

  minhas: (page = 1, limit = 20) =>
    http
      .get<Pagina<Ocorrencia>>('/api/ocorrencias', { params: { minhas: true, page, limit } })
      .then((r) => r.data),

  enviarMidia: (ocorrenciaId: number, arquivo: { uri: string; name: string; type: string }) => {
    const form = new FormData();
    // React Native aceita este formato de objeto no FormData.
    form.append('arquivo', arquivo as unknown as Blob);
    return http
      .post(`/api/ocorrencias/${ocorrenciaId}/midias`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000,
        transformRequest: (d) => d,
      })
      .then((r) => r.data);
  },
};
