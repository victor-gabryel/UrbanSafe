import { useLocalSearchParams } from 'expo-router';
import React from 'react';

import MapaScreen from '../../components/MapaScreen';

export default function Inicio() {
  const { lista } = useLocalSearchParams<{ lista?: string }>();
  return <MapaScreen listaInicial={lista === '1'} />;
}
