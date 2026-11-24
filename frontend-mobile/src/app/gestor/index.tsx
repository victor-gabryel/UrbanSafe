import React from 'react';

import PainelEquipe from '../../components/gestao/PainelEquipe';

export default function GestorPainel() {
  return (
    <PainelEquipe
      base="/gestor"
      atalhos={[
        { label: 'Fila de moderação', icone: 'shield-checkmark-outline', rota: '/gestor/fila' },
        { label: 'Todas as ocorrências', icone: 'list-outline', rota: '/gestor/ocorrencias' },
        { label: 'Mapa e mapa de calor', icone: 'map-outline', rota: '/gestor/mapa' },
      ]}
    />
  );
}
