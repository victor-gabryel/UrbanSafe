import { useLocalSearchParams } from 'expo-router';
import React from 'react';

import type { StatusOcorrencia } from '../../services/types';
import ListaOcorrencias from './ListaOcorrencias';

const VALIDOS: StatusOcorrencia[] = ['PENDENTE', 'PUBLICADA', 'OCULTA', 'REJEITADA'];

/** Tela "Ocorrências" de Gestor/Admin; aceita ?status= vindo dos cartões do painel. */
export default function OcorrenciasTela() {
  const { status } = useLocalSearchParams<{ status?: string }>();
  const inicial = VALIDOS.find((s) => s === status);
  return <ListaOcorrencias modo="gestao" statusInicial={inicial} />;
}
