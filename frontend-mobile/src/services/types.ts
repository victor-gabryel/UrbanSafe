export type Perfil = 'USUARIO' | 'GESTOR' | 'ADMINISTRADOR';
export type StatusOcorrencia = 'PENDENTE' | 'PUBLICADA' | 'REJEITADA' | 'OCULTA';
export type Relevancia = 'BAIXA' | 'MEDIA' | 'ALTA';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  emailVerificado: boolean;
  dataCadastro: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  refreshToken: string;
  usuario: Usuario;
}

export interface CadastroResponse {
  id: number;
  nome: string;
  email: string;
  emailVerificado: boolean;
  dataCadastro: string;
}

export interface TipoOcorrencia {
  id: number;
  nome: string;
  descricao: string | null;
  categoria: string | null;
  ativo: boolean;
  exigeRevisao: boolean;
}

export interface MarcadorMapa {
  id: number;
  tipoId: number;
  tipoNome: string;
  titulo: string;
  latitude: number;
  longitude: number;
  status: StatusOcorrencia;
  relevancia: string | null;
  dataOcorrencia: string | null;
}

export interface MapaOcorrencias {
  itens: MarcadorMapa[];
  limite: number;
  truncado: boolean;
}

export interface PontoCalor {
  latitude: number;
  longitude: number;
  quantidade: number;
  intensidade: number;
}

export interface MapaCalor {
  celulaGraus: number;
  totalOcorrencias: number;
  pontos: PontoCalor[];
  truncado: boolean;
  aviso: string;
}

export interface Ocorrencia {
  id: number;
  tipo: { id: number; nome: string; categoria: string | null };
  titulo: string;
  descricao: string;
  latitude: number;
  longitude: number;
  endereco: string | null;
  dataOcorrencia: string | null;
  dataCadastro: string;
  status: StatusOcorrencia;
  anonimato: boolean;
  /** Preenchido pelo backend só para o autor, GESTOR e ADMINISTRADOR. */
  autorId?: number | null;
  relevancia?: Relevancia | null;
}

export interface Pagina<T> {
  itens: T[];
  pagina: number;
  limite: number;
  totalItens: number;
  totalPaginas: number;
}

export interface CriarOcorrenciaPayload {
  tipoOcorrenciaId: number;
  titulo: string;
  descricao: string;
  latitude: number;
  longitude: number;
  endereco?: string;
  dataOcorrencia?: string;
  anonimato?: boolean;
}

export interface Bbox {
  minLon: number;
  minLat: number;
  maxLon: number;
  maxLat: number;
}

export interface FiltrosMapa {
  tipoId?: number;
  dataInicio?: string;
}

export type PerfilDeslocamento = 'A_PE' | 'BICICLETA';

export interface PontoRota {
  latitude: number;
  longitude: number;
  rotulo?: string | null;
}

export interface ExposicaoRota {
  indice: number | null;
  ocorrenciasProximas: number;
  distanciaBuscaMetros: number;
  periodoDias: number;
  dadosAtualizadosEm: string | null;
  observacao: string | null;
  aviso: string;
}

export interface RotaCalculada {
  id: number | null;
  perfil: PerfilDeslocamento;
  origem: PontoRota;
  destino: PontoRota;
  distanciaMetros: number;
  duracaoSegundos: number;
  /** Coordenadas no formato GeoJSON: [longitude, latitude]. */
  geometria: { tipo: string; coordenadas: [number, number][] };
  exposicao: ExposicaoRota;
  calculadaEm: string;
}

export interface OcorrenciaDetalhe extends Ocorrencia {
  relevancia?: 'BAIXA' | 'MEDIA' | 'ALTA';
}

// ---------------------------------------------------------------------------
// Gestor / Administrador (endpoints restritos do backend)
// ---------------------------------------------------------------------------

export type AcaoModeracao = 'APROVAR' | 'REJEITAR' | 'SOLICITAR_CORRECAO' | 'OCULTAR_TEMPORARIAMENTE';

export interface Moderacao {
  id: number;
  ocorrenciaId: number;
  moderadorId: number;
  acao: AcaoModeracao;
  statusAnterior: StatusOcorrencia;
  statusNovo: StatusOcorrencia;
  justificativa: string | null;
  dataModeracao: string;
}

export interface EventoHistorico {
  id: number;
  acao: string;
  statusAnterior: StatusOcorrencia | null;
  statusNovo: StatusOcorrencia | null;
  descricao: string | null;
  dataEvento: string;
  usuarioId: number | null;
}

export interface Midia {
  id: number;
  ocorrenciaId: number;
  tipo: string;
  tipoConteudo: string;
  nomeArquivo: string;
  descricao: string | null;
  tamanhoBytes: number;
  dataEnvio: string;
  /** Rota autenticada, relativa ao backend (ex.: /api/ocorrencias/42/midias/7/arquivo). */
  arquivoUrl: string;
}

export interface FiltrosGestao {
  status?: StatusOcorrencia;
  tipoId?: number;
}

export interface UsuarioAdmin {
  id: number;
  nome: string;
  email: string;
  perfil: string;
  ativo: boolean;
  emailVerificado: boolean;
  dataCadastro: string;
}

export interface RegistroAuditoria {
  id: number;
  usuarioId: number | null;
  acao: string;
  entidade: string;
  entidadeId: number | null;
  detalhes: string | null;
  dataEvento: string;
}

export interface NovoTipoPayload {
  nome: string;
  descricao?: string;
  categoria?: string;
  exigeRevisao?: boolean;
}

export interface AtualizarTipoPayload {
  nome?: string;
  descricao?: string;
  categoria?: string;
  ativo?: boolean;
  exigeRevisao?: boolean;
}
