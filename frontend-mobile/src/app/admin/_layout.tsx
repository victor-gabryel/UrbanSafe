import { Tabs } from 'expo-router';
import React from 'react';

import { icone, RequerPerfil, useOpcoesDaBarra } from '../../components/navegacao';

/** Área do ADMINISTRADOR (moderação + usuários, tipos e auditoria). */
export default function AdminLayout() {
  const opcoes = useOpcoesDaBarra();
  return (
    <RequerPerfil perfis={['ADMINISTRADOR']}>
      <Tabs screenOptions={opcoes}>
        <Tabs.Screen name="index" options={{ title: 'Painel', tabBarIcon: icone('speedometer', 'speedometer-outline') }} />
        <Tabs.Screen name="ocorrencias" options={{ title: 'Ocorrências', tabBarIcon: icone('list', 'list-outline') }} />
        <Tabs.Screen name="usuarios" options={{ title: 'Usuários', tabBarIcon: icone('people', 'people-outline') }} />
        <Tabs.Screen name="tipos" options={{ title: 'Tipos', tabBarIcon: icone('pricetags', 'pricetags-outline') }} />
        <Tabs.Screen name="perfil" options={{ title: 'Perfil', tabBarIcon: icone('person', 'person-outline') }} />
        {/* Telas acessadas pelo painel (sem aba própria) */}
        <Tabs.Screen name="auditoria" options={{ href: null }} />
        <Tabs.Screen name="mapa" options={{ href: null }} />
      </Tabs>
    </RequerPerfil>
  );
}
