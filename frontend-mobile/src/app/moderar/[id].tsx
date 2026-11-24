import { useLocalSearchParams } from 'expo-router';
import React from 'react';

import DetalheModeracao from '../../components/gestao/DetalheModeracao';
import { RequerPerfil } from '../../components/navegacao';

/** Detalhe/moderação de uma ocorrência (compartilhado por Gestor e Administrador). */
export default function Moderar() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const n = Number(id);
  return (
    <RequerPerfil perfis={['GESTOR', 'ADMINISTRADOR']}>
      <DetalheModeracao id={n} />
    </RequerPerfil>
  );
}
