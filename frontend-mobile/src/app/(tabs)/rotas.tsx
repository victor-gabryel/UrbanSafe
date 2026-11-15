import * as Location from 'expo-location';
import { Linking } from 'react-native';
import React, { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Banner, Button, Field } from '../../components/ui';
import { colors, radius } from '../../theme';

type Modo = 'walking' | 'driving' | 'transit' | 'bicycling';
const MODOS: { id: Modo; label: string }[] = [
  { id: 'walking', label: 'A pé' },
  { id: 'driving', label: 'Carro' },
  { id: 'transit', label: 'Transporte' },
  { id: 'bicycling', label: 'Bicicleta' },
];

export default function Rotas() {
  const [destino, setDestino] = useState('');
  const [modo, setModo] = useState<Modo>('walking');
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);

  async function abrir() {
    setErro('');
    if (!destino.trim()) return setErro('Informe o endereço de destino.');
    setLoading(true);
    try {
      const r = await Location.geocodeAsync(destino.trim());
      if (r.length === 0) return setErro('Destino não encontrado. Tente incluir bairro e cidade.');
      const url = `https://www.google.com/maps/dir/?api=1&destination=${r[0].latitude},${r[0].longitude}&travelmode=${modo}`;
      await Linking.openURL(url);
    } catch {
      setErro('Não foi possível abrir a rota agora.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 20 }}>
        <Text style={{ fontSize: 24, fontWeight: '800', color: colors.text }}>Calcular rota</Text>
        <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 16 }}>Trace o trajeto da sua localização até o destino.</Text>

        <Banner
          tipo="info"
          texto="O backend do UrbanSafe ainda não possui cálculo de rotas seguras. Este atalho abre o trajeto no app de mapas do seu celular (origem: sua localização atual)."
        />
        {!!erro && <Banner tipo="erro" texto={erro} />}

        <Field label="Destino" placeholder="Rua, bairro ou ponto de referência" value={destino} onChangeText={setDestino} />
        <Text style={{ fontSize: 11, fontWeight: '700', color: colors.muted, marginBottom: 8 }}>MODO</Text>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 14 }}>
          {MODOS.map((m) => (
            <Text
              key={m.id}
              onPress={() => setModo(m.id)}
              style={{
                paddingHorizontal: 14, paddingVertical: 8, borderRadius: radius.pill, marginRight: 8, marginBottom: 8, overflow: 'hidden',
                backgroundColor: modo === m.id ? colors.primary : '#fff', color: modo === m.id ? '#fff' : colors.text,
                borderWidth: 1, borderColor: modo === m.id ? colors.primary : colors.border, fontWeight: '600',
              }}
            >
              {m.label}
            </Text>
          ))}
        </View>
        <Button title="Abrir rota no app de mapas" onPress={abrir} loading={loading} />
      </ScrollView>
    </SafeAreaView>
  );
}
