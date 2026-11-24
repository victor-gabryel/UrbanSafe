import type { AcaoModeracao, Relevancia, StatusOcorrencia } from '../services/types';
import { colors } from '../theme';

export const ROTULO_STATUS: Record<StatusOcorrencia, string> = {
  PENDENTE: 'Pendente',
  PUBLICADA: 'Publicada',
  REJEITADA: 'Rejeitada',
  OCULTA: 'Oculta',
};

export const COR_STATUS: Record<StatusOcorrencia, { fg: string; bg: string }> = {
  PENDENTE: { fg: colors.warning, bg: '#FFF4E0' },
  PUBLICADA: { fg: colors.success, bg: '#E8F5E9' },
  REJEITADA: { fg: colors.danger, bg: '#FDECEC' },
  OCULTA: { fg: colors.muted, bg: '#EEF1F6' },
};

export const ROTULO_ACAO: Record<AcaoModeracao, string> = {
  APROVAR: 'Aprovada / publicada',
  REJEITAR: 'Rejeitada',
  SOLICITAR_CORRECAO: 'Correção solicitada',
  OCULTAR_TEMPORARIAMENTE: 'Ocultada',
};

export const ROTULO_RELEVANCIA: Record<Relevancia, string> = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta' };

/** Ações permitidas por status atual — espelha AcaoModeracao.java do backend. */
export const ACOES_POR_STATUS: Record<StatusOcorrencia, AcaoModeracao[]> = {
  PENDENTE: ['APROVAR', 'SOLICITAR_CORRECAO', 'REJEITAR', 'OCULTAR_TEMPORARIAMENTE'],
  PUBLICADA: ['SOLICITAR_CORRECAO', 'OCULTAR_TEMPORARIAMENTE', 'REJEITAR'],
  OCULTA: ['APROVAR', 'REJEITAR'],
  REJEITADA: ['APROVAR'],
};

export const ACAO_EXIGE_JUSTIFICATIVA: Record<AcaoModeracao, boolean> = {
  APROVAR: false,
  REJEITAR: true,
  SOLICITAR_CORRECAO: true,
  OCULTAR_TEMPORARIAMENTE: true,
};

export const ROTULO_BOTAO_ACAO: Record<AcaoModeracao, string> = {
  APROVAR: 'Aprovar e publicar',
  REJEITAR: 'Rejeitar',
  SOLICITAR_CORRECAO: 'Solicitar correção',
  OCULTAR_TEMPORARIAMENTE: 'Ocultar temporariamente',
};
