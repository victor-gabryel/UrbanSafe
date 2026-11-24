import { http } from './http';
import type {
  AcaoModeracao,
  EventoHistorico,
  FiltrosGestao,
  Midia,
  Moderacao,
  Ocorrencia,
  OcorrenciaDetalhe,
  Pagina,
  Relevancia,
} from './types';

/**
 * Moderação e gestão de ocorrências — perfis GESTOR e ADMINISTRADOR.
 * Endpoints reais do backend (ver backend/docs e SecurityConfig).
 */
export const gestaoApi = {
  /** GET /api/admin/ocorrencias/pendentes */
  pendentes: (page = 1, limit = 20, tipoId?: number) =>
    http
      .get<Pagina<Ocorrencia>>('/api/admin/ocorrencias/pendentes', {
        params: { page, limit, ...(tipoId ? { tipo: tipoId } : {}) },
      })
      .then((r) => r.data),

  /** GET /api/admin/ocorrencias (todas as não excluídas, com filtros opcionais) */
  listar: (f: FiltrosGestao, page = 1, limit = 20) =>
    http
      .get<Pagina<Ocorrencia>>('/api/admin/ocorrencias', {
        params: { page, limit, ...(f.status ? { status: f.status } : {}), ...(f.tipoId ? { tipo: f.tipoId } : {}) },
      })
      .then((r) => r.data),

  /** Apenas o total (limit=1) — usado nos contadores do painel. */
  total: (status?: Ocorrencia['status']) =>
    http
      .get<Pagina<Ocorrencia>>('/api/admin/ocorrencias', { params: { page: 1, limit: 1, ...(status ? { status } : {}) } })
      .then((r) => r.data.totalItens),

  /** GET /api/ocorrencias/{id} — para GESTOR/ADMIN inclui autorId e qualquer status. */
  detalhe: (id: number) => http.get<OcorrenciaDetalhe>(`/api/ocorrencias/${id}`).then((r) => r.data),

  /** GET /api/ocorrencias/{id}/midias */
  midias: (id: number) => http.get<Midia[]>(`/api/ocorrencias/${id}/midias`).then((r) => r.data),

  /** GET /api/ocorrencias/{id}/historico */
  historico: (id: number) => http.get<EventoHistorico[]>(`/api/ocorrencias/${id}/historico`).then((r) => r.data),

  /** GET /api/ocorrencias/{id}/moderacao */
  moderacoes: (id: number) => http.get<Moderacao[]>(`/api/ocorrencias/${id}/moderacao`).then((r) => r.data),

  /** POST /api/ocorrencias/{id}/moderacao — a justificativa é obrigatória, exceto em APROVAR. */
  moderar: (id: number, acao: AcaoModeracao, justificativa?: string) =>
    http
      .post<Moderacao>(`/api/ocorrencias/${id}/moderacao`, {
        acao,
        ...(justificativa?.trim() ? { justificativa: justificativa.trim() } : {}),
      })
      .then((r) => r.data),

  /** PATCH /api/ocorrencias/{id} — gestor/admin só podem alterar tipo e relevância. */
  atualizar: (id: number, campos: { relevancia?: Relevancia; tipoOcorrenciaId?: number }) =>
    http.patch<Ocorrencia>(`/api/ocorrencias/${id}`, campos).then((r) => r.data),
};
