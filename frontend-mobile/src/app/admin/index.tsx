import React from 'react';

import PainelEquipe from '../../components/gestao/PainelEquipe';

export default function AdminPainel() {
  return (
    <PainelEquipe
      base="/admin"
      atalhos={[
        { label: 'Moderar ocorrências pendentes', icone: 'shield-checkmark-outline', rota: '/admin/ocorrencias?status=PENDENTE' },
        { label: 'Usuários e perfis', icone: 'people-outline', rota: '/admin/usuarios' },
        { label: 'Tipos de ocorrência', icone: 'pricetags-outline', rota: '/admin/tipos' },
        { label: 'Auditoria administrativa', icone: 'document-text-outline', rota: '/admin/auditoria' },
        { label: 'Mapa e mapa de calor', icone: 'map-outline', rota: '/admin/mapa' },
      ]}
    />
  );
}
