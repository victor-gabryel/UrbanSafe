export type Perfil = 'USUARIO' | 'GESTOR' | 'ADMINISTRADOR';
export type StatusOcorrencia = 'PENDENTE' | 'PUBLICADA' | 'REJEITADA' | 'OCULTA';

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
