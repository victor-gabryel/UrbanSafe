import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { router } from 'expo-router';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import MapView, { Circle, Marker, Region } from 'react-native-maps';
import { SafeAreaView } from 'react-native-safe-area-context';

import { REGIAO_PADRAO } from '../config/api';
import { useAuth } from '../context/AuthContext';
import { mensagemDeErro } from '../services/http';
import { ocorrenciasApi } from '../services/ocorrencias';
import type { Bbox, MapaCalor, MarcadorMapa, OcorrenciaDetalhe, TipoOcorrencia } from '../services/types';
import { colors, corDoTipo, radius, shadow } from '../theme';
import { dataCurta } from '../utils/formatar';
import { ehEquipe } from '../utils/perfil';

const MAX_LADO_BBOX = 4.5; // o backend aceita até 5 graus

function bboxDe(r: Region): Bbox {
  return {
    minLon: r.longitude - r.longitudeDelta / 2,
    minLat: r.latitude - r.latitudeDelta / 2,
    maxLon: r.longitude + r.longitudeDelta / 2,
    maxLat: r.latitude + r.latitudeDelta / 2,
  };
}

function corCalor(i: number): string {
  // amarelo -> laranja -> vermelho
  const g = Math.round(210 - 170 * i);
  return `rgb(230, ${g}, 40)`;
}

const PERIODOS: { label: string; dias: number | null }[] = [
  { label: 'Todo o período', dias: null },
  { label: '7 dias', dias: 7 },
  { label: '30 dias', dias: 30 },
  { label: '90 dias', dias: 90 },
];

export default function MapaScreen({ calorInicial = false, listaInicial = false }: { calorInicial?: boolean; listaInicial?: boolean }) {
  const { logado, usuario } = useAuth();
  const equipe = ehEquipe(usuario); // Gestor/Admin usam o mapa para consulta; denunciar fica na área do Usuário
  const mapRef = useRef<MapView | null>(null);
  const regiaoRef = useRef<Region>(REGIAO_PADRAO);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reqId = useRef(0);

  const [marcadores, setMarcadores] = useState<MarcadorMapa[]>([]);
  const [calor, setCalor] = useState<MapaCalor | null>(null);
  const [tipos, setTipos] = useState<TipoOcorrencia[]>([]);
  const [busca, setBusca] = useState('');
  const [tipoId, setTipoId] = useState<number | undefined>();
  const [dias, setDias] = useState<number | null>(null);
  const [mostrarLista, setMostrarLista] = useState(listaInicial);
  const [mostrarCalor, setMostrarCalor] = useState(calorInicial);
  const [legenda, setLegenda] = useState(false);
  const [filtroAberto, setFiltroAberto] = useState(false);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState('');
  const [aviso, setAviso] = useState('');
  const [permLocal, setPermLocal] = useState(false);
  const [rastrear, setRastrear] = useState(true);
  const [detalhe, setDetalhe] = useState<OcorrenciaDetalhe | null>(null);
  const [detalheCarregando, setDetalheCarregando] = useState(false);
  const [detalheAberto, setDetalheAberto] = useState(false);

  const filtros = useMemo(
    () => ({
      tipoId,
      dataInicio: dias ? new Date(Date.now() - dias * 86400000).toISOString() : undefined,
    }),
    [tipoId, dias],
  );

  const carregar = useCallback(
    async (r: Region) => {
      const id = ++reqId.current;
      if (r.latitudeDelta > MAX_LADO_BBOX || r.longitudeDelta > MAX_LADO_BBOX) {
        setAviso('Aproxime o mapa para carregar as ocorrências desta área.');
        setMarcadores([]);
        setCalor(null);
        return;
      }
      setAviso('');
      setCarregando(true);
      setErro('');
      try {
        const bbox = bboxDe(r);
        const celula = Math.min(0.05, Math.max(0.0005, r.latitudeDelta / 25));
        const [m, c] = await Promise.all([
          ocorrenciasApi.mapa(bbox, filtros),
          ocorrenciasApi.calor(bbox, filtros, Number(celula.toFixed(5))),
        ]);
        if (id !== reqId.current) return;
        setMarcadores(m.itens);
        setCalor(c);
        setRastrear(true);
        setTimeout(() => setRastrear(false), 800);
      } catch (e) {
        if (id === reqId.current) setErro(mensagemDeErro(e));
      } finally {
        if (id === reqId.current) setCarregando(false);
      }
    },
    [filtros],
  );

  const agendar = useCallback(
    (r: Region) => {
      regiaoRef.current = r;
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(() => carregar(r), 450);
    },
    [carregar],
  );

  // Permite que outras telas abram o mapa já na visão de lista.
  useEffect(() => {
    if (listaInicial) setMostrarLista(true);
  }, [listaInicial]);

  // Recarrega quando os filtros mudam.
  useEffect(() => {
    carregar(regiaoRef.current);
  }, [carregar]);

  useEffect(() => {
    ocorrenciasApi.tipos().then(setTipos).catch(() => undefined);
    (async () => {
      const p = await Location.getForegroundPermissionsAsync();
      if (p.granted) {
        setPermLocal(true);
        irParaMinhaLocalizacao(false);
      }
    })();
    return () => {
      if (timer.current) clearTimeout(timer.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function irParaMinhaLocalizacao(pedir = true) {
    try {
      let perm = await Location.getForegroundPermissionsAsync();
      if (!perm.granted && pedir) perm = await Location.requestForegroundPermissionsAsync();
      if (!perm.granted) {
        if (pedir) setErro('Permissão de localização negada. Ative-a nas configurações do celular.');
        return;
      }
      setPermLocal(true);
      const pos = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      mapRef.current?.animateToRegion(
        { latitude: pos.coords.latitude, longitude: pos.coords.longitude, latitudeDelta: 0.02, longitudeDelta: 0.02 },
        600,
      );
    } catch {
      if (pedir) setErro('Não foi possível obter sua localização.');
    }
  }

  const termo = busca.trim().toLowerCase();
  const itens = useMemo(
    () =>
      termo
        ? marcadores.filter((m) => m.titulo.toLowerCase().includes(termo) || m.tipoNome.toLowerCase().includes(termo))
        : marcadores,
    [marcadores, termo],
  );

  async function submeterBusca() {
    if (!termo) return;
    // 1) tipo de ocorrência com esse nome -> aplica como filtro
    const tipo = tipos.find((t) => t.nome.toLowerCase().includes(termo));
    if (tipo) {
      setTipoId(tipo.id);
      setBusca('');
      return;
    }
    // 2) já existem ocorrências com esse texto na área -> nada a fazer
    if (itens.length > 0) return;
    // 3) endereço/região -> geocodificação do sistema
    try {
      const r = await Location.geocodeAsync(busca.trim());
      if (r.length > 0) {
        mapRef.current?.animateToRegion(
          { latitude: r[0].latitude, longitude: r[0].longitude, latitudeDelta: 0.03, longitudeDelta: 0.03 },
          700,
        );
        setBusca('');
        setErro('');
      } else {
        setErro('Nenhum endereço ou região encontrado para essa busca.');
      }
    } catch {
      setErro('Não foi possível buscar este endereço agora.');
    }
  }

  async function abrirDetalhe(id: number) {
    setDetalhe(null);
    setDetalheAberto(true);
    setDetalheCarregando(true);
    try {
      setDetalhe(await ocorrenciasApi.detalhe(id));
    } catch (e) {
      setDetalheAberto(false);
      setErro(mensagemDeErro(e));
    } finally {
      setDetalheCarregando(false);
    }
  }

  function focar(m: MarcadorMapa) {
    mapRef.current?.animateToRegion(
      { latitude: m.latitude, longitude: m.longitude, latitudeDelta: 0.01, longitudeDelta: 0.01 },
      500,
    );
  }

  const filtrosAtivos = (tipoId ? 1 : 0) + (dias ? 1 : 0);
  const tiposNoMapa = useMemo(() => {
    const mapa = new Map<number, string>();
    marcadores.forEach((m) => mapa.set(m.tipoId, m.tipoNome));
    return Array.from(mapa, ([id, nome]) => ({ id, nome }));
  }, [marcadores]);

  return (
    <View style={{ flex: 1, backgroundColor: '#fff' }}>
      <SafeAreaView edges={['top']} style={{ backgroundColor: '#fff' }}>
        <View style={s.header}>
          <Text style={s.titulo}>Mapa de{'\n'}ocorrências</Text>
          <HeaderBtn icon="list" label="Lista" ativo={mostrarLista} onPress={() => setMostrarLista((v) => !v)} />
          <HeaderBtn icon="filter" label={filtrosAtivos ? `Filtrar (${filtrosAtivos})` : 'Filtrar'} ativo={filtrosAtivos > 0} onPress={() => setFiltroAberto(true)} />
          <HeaderBtn icon="flame" label={'Mapa de\nCalor'} ativo={mostrarCalor} onPress={() => setMostrarCalor((v) => !v)} />
        </View>

        <View style={s.buscaLinha}>
          <View style={s.buscaBox}>
            <Ionicons name="search" size={18} color={colors.muted} />
            <TextInput
              style={{ flex: 1, marginLeft: 8, color: colors.text, paddingVertical: 0 }}
              placeholder="Endereço, região ou tipo"
              placeholderTextColor="#98A4B8"
              value={busca}
              onChangeText={setBusca}
              onSubmitEditing={submeterBusca}
              returnKeyType="search"
            />
            {!!busca && (
              <Pressable onPress={() => setBusca('')} hitSlop={8}>
                <Ionicons name="close-circle" size={18} color={colors.muted} />
              </Pressable>
            )}
            <View style={s.contador}>
              <Text style={s.contadorTxt}>{itens.length} ocorrências</Text>
            </View>
          </View>
          <Pressable style={s.iconBtn} onPress={() => irParaMinhaLocalizacao(true)} accessibilityLabel="Minha localização">
            <Ionicons name="navigate" size={20} color={colors.primary} />
          </Pressable>
          {!equipe && (
            <Pressable style={[s.iconBtn, { backgroundColor: colors.primary }]} onPress={() => router.push('/(tabs)/denunciar')} accessibilityLabel="Denunciar">
              <Ionicons name="add" size={26} color="#fff" />
            </Pressable>
          )}
        </View>

        {!logado && (
          <Text style={s.visitante}>Navegando como visitante. Recursos públicos permanecem disponíveis.</Text>
        )}
        {!!erro && <Text style={s.erro}>{erro}</Text>}
        {!!aviso && <Text style={s.aviso}>{aviso}</Text>}
      </SafeAreaView>

      <View style={{ flex: 1 }}>
        <MapView
          ref={mapRef}
          style={StyleSheet.absoluteFill}
          initialRegion={REGIAO_PADRAO}
          showsUserLocation={permLocal}
          showsMyLocationButton={false}
          onRegionChangeComplete={agendar}
        >
          {mostrarCalor &&
            calor?.pontos.map((p, i) => (
              <Circle
                key={`c${i}`}
                center={{ latitude: p.latitude, longitude: p.longitude }}
                radius={(calor.celulaGraus * 111320) / 1.4}
                strokeWidth={0}
                fillColor={corCalor(p.intensidade).replace('rgb', 'rgba').replace(')', `, ${0.25 + 0.4 * p.intensidade})`)}
              />
            ))}
          {itens.map((m) => (
            <Marker
              key={m.id}
              coordinate={{ latitude: m.latitude, longitude: m.longitude }}
              title={m.titulo}
              description={`${m.tipoNome} · ${dataCurta(m.dataOcorrencia)}`}
              tracksViewChanges={rastrear}
              onCalloutPress={() => abrirDetalhe(m.id)}
            >
              <View style={[s.pino, { backgroundColor: corDoTipo(m.tipoId, m.tipoNome) }]}>
                <Text style={s.pinoTxt}>{m.tipoNome.charAt(0).toUpperCase()}</Text>
              </View>
            </Marker>
          ))}
        </MapView>

        {carregando && (
          <View style={s.loading}>
            <ActivityIndicator color={colors.primary} />
          </View>
        )}

        {/* Legenda recolhível */}
        <View style={s.legendaWrap} pointerEvents="box-none">
          {legenda && (
            <View style={s.legendaCard}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                <Text style={{ fontWeight: '700', color: colors.text }}>Legenda</Text>
                <Pressable onPress={() => setLegenda(false)} hitSlop={8}>
                  <Ionicons name="close" size={18} color={colors.muted} />
                </Pressable>
              </View>
              <ScrollView style={{ maxHeight: 170 }}>
                {tiposNoMapa.length === 0 && <Text style={{ color: colors.muted, fontSize: 12 }}>Nenhuma ocorrência na área.</Text>}
                {tiposNoMapa.map((t) => (
                  <View key={t.id} style={{ flexDirection: 'row', alignItems: 'center', marginVertical: 3 }}>
                    <View style={[s.pinoMini, { backgroundColor: corDoTipo(t.id, t.nome) }]}>
                      <Text style={{ color: '#fff', fontSize: 9, fontWeight: '700' }}>{t.nome.charAt(0).toUpperCase()}</Text>
                    </View>
                    <Text style={{ marginLeft: 8, color: colors.text, fontSize: 12 }}>{t.nome}</Text>
                  </View>
                ))}
              </ScrollView>
              <View style={{ marginTop: 8 }}>
                <View style={{ flexDirection: 'row', height: 8, borderRadius: 4, overflow: 'hidden' }}>
                  {[0, 0.25, 0.5, 0.75, 1].map((i) => (
                    <View key={i} style={{ flex: 1, backgroundColor: corCalor(i) }} />
                  ))}
                </View>
                <Text style={{ fontSize: 10, color: colors.muted, marginTop: 3 }}>Menos relatos → mais relatos (mapa de calor)</Text>
              </View>
            </View>
          )}
          <Pressable style={s.legendaBtn} onPress={() => setLegenda((v) => !v)}>
            <Ionicons name={legenda ? 'close-circle-outline' : 'information-circle-outline'} size={16} color={colors.primary} />
            <Text style={{ color: colors.primary, fontWeight: '700', fontSize: 12, marginLeft: 6 }}>{legenda ? 'Fechar legenda' : 'Abrir legenda'}</Text>
          </Pressable>
        </View>
      </View>

      {mostrarLista && (
        <View style={s.lista}>
          <Text style={{ fontWeight: '700', color: colors.text, marginBottom: 6 }}>Ocorrências publicadas na área ({itens.length})</Text>
          <FlatList
            data={itens}
            keyExtractor={(m) => String(m.id)}
            ListEmptyComponent={<Text style={{ color: colors.muted, paddingVertical: 12 }}>Nenhuma ocorrência encontrada nesta área e filtros.</Text>}
            renderItem={({ item }) => (
              <Pressable style={s.itemLista} onPress={() => { focar(item); abrirDetalhe(item.id); }}>
                <View style={[s.pinoMini, { backgroundColor: corDoTipo(item.tipoId, item.tipoNome), width: 26, height: 26, borderRadius: 13 }]}>
                  <Text style={{ color: '#fff', fontWeight: '700', fontSize: 11 }}>{item.tipoNome.charAt(0).toUpperCase()}</Text>
                </View>
                <View style={{ flex: 1, marginLeft: 10 }}>
                  <Text style={{ color: colors.text, fontWeight: '600' }} numberOfLines={1}>{item.titulo}</Text>
                  <Text style={{ color: colors.muted, fontSize: 12 }}>{item.tipoNome} · {dataCurta(item.dataOcorrencia)}</Text>
                </View>
                <Ionicons name="locate" size={18} color={colors.primary} />
              </Pressable>
            )}
          />
        </View>
      )}

      {/* Detalhes da ocorrência (consulta pública: somente PUBLICADAS) */}
      <Modal visible={detalheAberto} transparent animationType="slide" onRequestClose={() => setDetalheAberto(false)}>
        <Pressable style={{ flex: 1, backgroundColor: 'rgba(0,0,0,0.35)' }} onPress={() => setDetalheAberto(false)} />
        <View style={s.modal}>
          {detalheCarregando && <ActivityIndicator color={colors.primary} />}
          {detalhe && (
            <ScrollView style={{ maxHeight: 380 }}>
              <Text style={{ fontSize: 18, fontWeight: '800', color: colors.text }}>{detalhe.titulo}</Text>
              <Text style={{ color: colors.muted, marginTop: 4, fontSize: 12 }}>
                {detalhe.tipo.nome} · {dataCurta(detalhe.dataOcorrencia ?? detalhe.dataCadastro)}
              </Text>
              <Text style={{ color: colors.text, marginTop: 12, lineHeight: 20 }}>{detalhe.descricao}</Text>
              {!!detalhe.endereco && <Text style={{ color: colors.muted, marginTop: 10, fontSize: 12 }}>Local: {detalhe.endereco}</Text>}
              <Text style={{ color: colors.muted, marginTop: 10, fontSize: 11 }}>
                Relato registrado por usuário; a publicação não equivale a confirmação oficial do fato.
              </Text>
            </ScrollView>
          )}
          <Pressable style={[s.modalBtn, { backgroundColor: colors.primary, marginTop: 16 }]} onPress={() => setDetalheAberto(false)}>
            <Text style={{ color: '#fff', fontWeight: '700' }}>Fechar</Text>
          </Pressable>
        </View>
      </Modal>

      {/* Modal de filtros */}
      <Modal visible={filtroAberto} transparent animationType="slide" onRequestClose={() => setFiltroAberto(false)}>
        <Pressable style={{ flex: 1, backgroundColor: 'rgba(0,0,0,0.35)' }} onPress={() => setFiltroAberto(false)} />
        <View style={s.modal}>
          <Text style={{ fontSize: 18, fontWeight: '800', color: colors.text, marginBottom: 12 }}>Filtros</Text>
          <Text style={s.secao}>TIPO DE OCORRÊNCIA</Text>
          <ScrollView style={{ maxHeight: 220 }}>
            <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
              <Chip label="Todos" ativo={!tipoId} onPress={() => setTipoId(undefined)} />
              {tipos.map((t) => (
                <Chip key={t.id} label={t.nome} ativo={tipoId === t.id} onPress={() => setTipoId(t.id)} />
              ))}
            </View>
          </ScrollView>
          <Text style={[s.secao, { marginTop: 14 }]}>PERÍODO</Text>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
            {PERIODOS.map((p) => (
              <Chip key={p.label} label={p.label} ativo={dias === p.dias} onPress={() => setDias(p.dias)} />
            ))}
          </View>
          <View style={{ flexDirection: 'row', marginTop: 20, gap: 10 }}>
            <Pressable
              style={[s.modalBtn, { borderWidth: 1.5, borderColor: colors.primary }]}
              onPress={() => {
                setTipoId(undefined);
                setDias(null);
              }}
            >
              <Text style={{ color: colors.primary, fontWeight: '700' }}>Limpar</Text>
            </Pressable>
            <Pressable style={[s.modalBtn, { backgroundColor: colors.primary }]} onPress={() => setFiltroAberto(false)}>
              <Text style={{ color: '#fff', fontWeight: '700' }}>Aplicar</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </View>
  );
}

function HeaderBtn({ icon, label, ativo, onPress }: { icon: keyof typeof Ionicons.glyphMap; label: string; ativo: boolean; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={[s.hBtn, ativo && { backgroundColor: colors.primaryLight, borderColor: colors.primary }]}>
      <Ionicons name={icon} size={18} color={ativo ? colors.primary : colors.text} />
      <Text style={{ fontSize: 10, color: ativo ? colors.primary : colors.text, textAlign: 'center', marginTop: 2, fontWeight: '600' }}>{label}</Text>
    </Pressable>
  );
}

function Chip({ label, ativo, onPress }: { label: string; ativo: boolean; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      style={{
        paddingHorizontal: 12, paddingVertical: 8, borderRadius: radius.pill, marginRight: 8, marginBottom: 8,
        backgroundColor: ativo ? colors.primary : colors.bg, borderWidth: 1, borderColor: ativo ? colors.primary : colors.border,
      }}
    >
      <Text style={{ color: ativo ? '#fff' : colors.text, fontSize: 13, fontWeight: '600' }}>{label}</Text>
    </Pressable>
  );
}

const s = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingTop: 8, gap: 6 },
  titulo: { flex: 1, fontSize: 18, fontWeight: '800', color: colors.text, lineHeight: 24 },
  hBtn: {
    width: 62, height: 52, borderRadius: radius.md, borderWidth: 1, borderColor: colors.border,
    alignItems: 'center', justifyContent: 'center', backgroundColor: '#fff',
  },
  buscaLinha: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingTop: 10, gap: 8 },
  buscaBox: {
    flex: 1, height: 44, borderRadius: radius.md, backgroundColor: colors.bg, borderWidth: 1, borderColor: colors.border,
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 10,
  },
  contador: { position: 'absolute', left: 6, bottom: -14, backgroundColor: colors.primary, borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 3 },
  contadorTxt: { color: '#fff', fontSize: 11, fontWeight: '700' },
  iconBtn: { width: 44, height: 44, borderRadius: radius.md, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center' },
  visitante: { color: colors.primary, fontSize: 12, paddingHorizontal: 14, paddingTop: 20, paddingBottom: 8, backgroundColor: '#fff' },
  erro: { color: colors.danger, fontSize: 12, paddingHorizontal: 14, paddingTop: 6, paddingBottom: 6 },
  aviso: { color: colors.warning, fontSize: 12, paddingHorizontal: 14, paddingTop: 6, paddingBottom: 6 },
  pino: { width: 30, height: 30, borderRadius: 15, alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderColor: '#fff' },
  pinoTxt: { color: '#fff', fontWeight: '800', fontSize: 12 },
  pinoMini: { width: 20, height: 20, borderRadius: 10, alignItems: 'center', justifyContent: 'center' },
  loading: { position: 'absolute', top: 10, alignSelf: 'center', backgroundColor: '#fff', borderRadius: radius.pill, padding: 8, ...shadow },
  legendaWrap: { position: 'absolute', left: 10, bottom: 14 },
  legendaCard: { backgroundColor: '#fff', borderRadius: radius.md, padding: 12, width: 220, marginBottom: 8, ...shadow },
  legendaBtn: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', backgroundColor: '#fff', borderRadius: radius.pill, paddingHorizontal: 12, paddingVertical: 8, ...shadow },
  lista: { maxHeight: '40%', backgroundColor: '#fff', paddingHorizontal: 14, paddingTop: 10, borderTopWidth: 1, borderTopColor: colors.border },
  itemLista: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: colors.border },
  modal: { backgroundColor: '#fff', padding: 20, borderTopLeftRadius: 22, borderTopRightRadius: 22 },
  secao: { fontSize: 11, fontWeight: '700', color: colors.muted, marginBottom: 8, letterSpacing: 0.5 },
  modalBtn: { flex: 1, height: 48, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
});
