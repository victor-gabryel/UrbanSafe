import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import React, { useRef, useState } from 'react';
import { Linking, ScrollView, StyleSheet, Text, View } from 'react-native';
import MapView, { Marker, Polyline } from 'react-native-maps';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Banner, Button, Field } from '../../components/ui';
import { REGIAO_PADRAO } from '../../config/api';
import { mensagemDeErro } from '../../services/http';
import { rotasApi } from '../../services/rotas';
import type { PerfilDeslocamento, PontoRota, RotaCalculada } from '../../services/types';
import { colors, radius } from '../../theme';
import { formatarDistancia, formatarDuracao } from '../../utils/formatar';

const MODOS: { id: PerfilDeslocamento; label: string; mapas: 'walking' | 'bicycling' }[] = [
  { id: 'A_PE', label: 'A pé', mapas: 'walking' },
  { id: 'BICICLETA', label: 'Bicicleta', mapas: 'bicycling' },
];

async function geocodificar(texto: string): Promise<PontoRota | null> {
  const r = await Location.geocodeAsync(texto.trim());
  return r.length === 0 ? null : { latitude: r[0].latitude, longitude: r[0].longitude, rotulo: texto.trim() };
}

async function minhaLocalizacao(): Promise<PontoRota> {
  let perm = await Location.getForegroundPermissionsAsync();
  if (!perm.granted) perm = await Location.requestForegroundPermissionsAsync();
  if (!perm.granted) throw new Error('Permissão de localização negada. Informe a origem manualmente.');
  const pos = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
  return { latitude: pos.coords.latitude, longitude: pos.coords.longitude, rotulo: 'Minha localização' };
}

export default function Rotas() {
  const mapRef = useRef<MapView | null>(null);
  const [origemTexto, setOrigemTexto] = useState('');
  const [destinoTexto, setDestinoTexto] = useState('');
  const [modo, setModo] = useState<PerfilDeslocamento>('A_PE');
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);
  const [rota, setRota] = useState<RotaCalculada | null>(null);

  async function calcular() {
    setErro('');
    if (!destinoTexto.trim()) return setErro('Informe o endereço de destino.');
    setLoading(true);
    try {
      const origem = origemTexto.trim() ? await geocodificar(origemTexto) : await minhaLocalizacao();
      if (!origem) return setErro('Origem não encontrada. Tente incluir bairro e cidade.');
      const destino = await geocodificar(destinoTexto);
      if (!destino) return setErro('Destino não encontrado. Tente incluir bairro e cidade.');

      const r = await rotasApi.calcular(origem, destino, modo);
      setRota(r);
      const pontos = r.geometria.coordenadas.map(([lon, lat]) => ({ latitude: lat, longitude: lon }));
      setTimeout(
        () => mapRef.current?.fitToCoordinates(pontos, { edgePadding: { top: 40, right: 40, bottom: 40, left: 40 }, animated: true }),
        300,
      );
    } catch (e) {
      setRota(null);
      setErro(mensagemDeErro(e));
    } finally {
      setLoading(false);
    }
  }

  function abrirNoApp() {
    if (!rota) return;
    const m = MODOS.find((x) => x.id === modo)?.mapas ?? 'walking';
    const url =
      `https://www.google.com/maps/dir/?api=1&origin=${rota.origem.latitude},${rota.origem.longitude}` +
      `&destination=${rota.destino.latitude},${rota.destino.longitude}&travelmode=${m}`;
    Linking.openURL(url).catch(() => setErro('Não foi possível abrir o app de mapas.'));
  }

  const linha = rota?.geometria.coordenadas.map(([lon, lat]) => ({ latitude: lat, longitude: lon })) ?? [];
  const exp = rota?.exposicao;

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 20 }}>
        <Text style={{ fontSize: 24, fontWeight: '800', color: colors.text }}>Calcular rota</Text>
        <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 16 }}>
          Trace o trajeto e veja o indicador informativo de exposição a ocorrências registradas.
        </Text>

        {!!erro && <Banner tipo="erro" texto={erro} />}

        <Field label="Origem" placeholder="Vazio = minha localização atual" value={origemTexto} onChangeText={setOrigemTexto} />
        <Field label="Destino" placeholder="Rua, bairro ou ponto de referência" value={destinoTexto} onChangeText={setDestinoTexto} />

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
        <Button title="Calcular rota" onPress={calcular} loading={loading} />

        {rota && exp && (
          <View style={{ marginTop: 18 }}>
            <View style={s.mapa}>
              <MapView ref={mapRef} style={StyleSheet.absoluteFill} initialRegion={REGIAO_PADRAO}>
                <Polyline coordinates={linha} strokeColor={colors.primary} strokeWidth={5} />
                <Marker coordinate={{ latitude: rota.origem.latitude, longitude: rota.origem.longitude }} title="Origem" pinColor="green" />
                <Marker coordinate={{ latitude: rota.destino.latitude, longitude: rota.destino.longitude }} title="Destino" />
              </MapView>
            </View>

            <View style={s.card}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-around' }}>
                <Info icon="walk-outline" rotulo="Distância" valor={formatarDistancia(rota.distanciaMetros)} />
                <Info icon="time-outline" rotulo="Tempo estimado" valor={formatarDuracao(rota.duracaoSegundos)} />
                <Info icon="alert-circle-outline" rotulo="Exposição" valor={exp.indice === null ? '—' : `${Math.round(exp.indice)}/100`} />
              </View>
              <Text style={{ color: colors.text, marginTop: 12, fontSize: 13, lineHeight: 18 }}>
                {exp.indice === null
                  ? exp.observacao ?? 'Indicador não calculado.'
                  : `${exp.ocorrenciasProximas} ocorrência(s) publicada(s) a até ${exp.distanciaBuscaMetros} m do percurso nos últimos ${exp.periodoDias} dias.`}
              </Text>
              <Text style={{ color: colors.muted, marginTop: 8, fontSize: 11, lineHeight: 16 }}>{exp.aviso}</Text>
            </View>
            <Button title="Abrir no app de mapas" variant="outline" onPress={abrirNoApp} style={{ marginTop: 12 }} />
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

function Info({ icon, rotulo, valor }: { icon: keyof typeof Ionicons.glyphMap; rotulo: string; valor: string }) {
  return (
    <View style={{ alignItems: 'center' }}>
      <Ionicons name={icon} size={20} color={colors.primary} />
      <Text style={{ fontWeight: '800', color: colors.text, marginTop: 2 }}>{valor}</Text>
      <Text style={{ fontSize: 11, color: colors.muted }}>{rotulo}</Text>
    </View>
  );
}

const s = StyleSheet.create({
  mapa: { height: 260, borderRadius: radius.md, overflow: 'hidden', borderWidth: 1, borderColor: colors.border },
  card: { backgroundColor: '#fff', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, padding: 14, marginTop: 12 },
});
