import { Tabs } from 'expo-router';
import React from 'react';

import { icone, RequerPerfil, useOpcoesDaBarra } from '../../components/navegacao';

/** Área do GESTOR (moderação de ocorrências). */
export default function GestorLayout() {
  const opcoes = useOpcoesDaBarra();
  return (
    <RequerPerfil perfis={['GESTOR']}>
      <Tabs screenOptions={opcoes}>
        <Tabs.Screen name="index" options={{ title: 'Painel', tabBarIcon: icone('speedometer', 'speedometer-outline') }} />
        <Tabs.Screen name="fila" options={{ title: 'Fila', tabBarIcon: icone('shield-checkmark', 'shield-checkmark-outline') }} />
        <Tabs.Screen name="ocorrencias" options={{ title: 'Ocorrências', tabBarIcon: icone('list', 'list-outline') }} />
        <Tabs.Screen name="mapa" options={{ title: 'Mapa', tabBarIcon: icone('map', 'map-outline') }} />
        <Tabs.Screen name="perfil" options={{ title: 'Perfil', tabBarIcon: icone('person', 'person-outline') }} />
      </Tabs>
    </RequerPerfil>
  );
}
