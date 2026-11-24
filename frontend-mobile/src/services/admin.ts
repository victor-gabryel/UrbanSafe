import { http } from './http';
import type {
  AtualizarTipoPayload,
  NovoTipoPayload,
  Pagina,
  RegistroAuditoria,
  TipoOcorrencia,
  Usuario,
  UsuarioAdmin,
} from './types';

/** Administração do sistema — somente perfil ADMINISTRADOR (endpoints reais do backend). */
export const adminApi = {
  /** GET /api/admin/usuarios */
  usuarios: (page = 1, limit = 20) =>
    http.get<Pagina<UsuarioAdmin>>('/api/admin/usuarios', { params: { page, limit } }).then((r) => r.data),

  /** PATCH /api/admin/usuarios/{id}/status — bloqueia (false) ou reativa (true). */
  alterarStatusUsuario: (id: number, ativo: boolean) =>
    http.patch<UsuarioAdmin>(`/api/admin/usuarios/${id}/status`, { ativo }).then((r) => r.data),

  /** PATCH /api/admin/usuarios/{id}/perfil — USUARIO, GESTOR ou ADMINISTRADOR. */
  alterarPerfilUsuario: (id: number, perfil: 'USUARIO' | 'GESTOR' | 'ADMINISTRADOR') =>
    http.patch<Usuario>(`/api/admin/usuarios/${id}/perfil`, { perfil }).then((r) => r.data),

  /** GET /api/admin/auditoria */
  auditoria: (page = 1, limit = 20) =>
    http.get<Pagina<RegistroAuditoria>>('/api/admin/auditoria', { params: { page, limit } }).then((r) => r.data),

  /** GET /api/tipos-ocorrencia — o backend lista somente tipos ATIVOS. */
  tiposAtivos: () => http.get<TipoOcorrencia[]>('/api/tipos-ocorrencia').then((r) => r.data),

  /** POST /api/tipos-ocorrencia */
  criarTipo: (p: NovoTipoPayload) => http.post<TipoOcorrencia>('/api/tipos-ocorrencia', p).then((r) => r.data),

  /** PATCH /api/tipos-ocorrencia/{id} — campos ausentes não são alterados. */
  atualizarTipo: (id: number, p: AtualizarTipoPayload) =>
    http.patch<TipoOcorrencia>(`/api/tipos-ocorrencia/${id}`, p).then((r) => r.data),

  /** DELETE /api/tipos-ocorrencia/{id} — exclusão lógica (desativa). */
  desativarTipo: (id: number) => http.delete(`/api/tipos-ocorrencia/${id}`),
};
