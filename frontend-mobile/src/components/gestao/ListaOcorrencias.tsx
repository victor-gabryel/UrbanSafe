import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, RefreshControl, ScrollView, Text, View } from 'react-native';

import { Banner, Cabecalho, Chip, StatusBadge, Vazio } from '../ui';
import { gestaoApi } from '../../services/gestao';
import { mensagemDeErro } from '../../services/http';
import { ocorrenciasApi } from '../../services/ocorrencias';
import type { Ocorrencia, StatusOcorrencia, TipoOcorrencia } from '../../services/types';
import { colors, radius } from '../../theme';
import { dataCurta } from '../../utils/formatar';
import { ROTULO_STATUS } from '../../utils/status';

const STATUS: StatusOcorrencia[] = ['PENDENTE', 'PUBLICADA', 'OCULTA', 'REJEITADA'];
const LIMITE = 20;

interface Props {
  /** fila = só pendentes (GET /pendentes); gestao = todas, com filtro de status. */
  modo: 'fila' | 'gestao';
  statusInicial?: StatusOcorrencia;
}

export default function ListaOcorrencias({ modo, statusInicial }: Props) {
  const [status, setStatus] = useState<StatusOcorrencia | undefined>(statusInicial);
  const [tipoId, setTipoId] = useState<number | undefined>();
  const [tipos, setTipos] = useState<TipoOcorrencia[]>([]);
  const [itens, setItens] = useState<Ocorrencia[]>([]);
  const [pagina, setPagina] = useState(1);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [total, setTotal] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [maisCarregando, setMaisCarregando] = useState(false);
  const [erro, setErro] = useState('');
  const reqId = useRef(0);

  // Quando o painel navega para esta aba com outro status, acompanha o parâmetro.
  useEffect(() => setStatus(statusInicial), [statusInicial]);
  useEffect(() => {
    ocorrenciasApi.tipos().then(setTipos).catch(() => undefined);
  }, []);

  const buscar = useCallback(
    async (p: number, anexar: boolean) => {
      const id = ++reqId.current;
      try {
        const r = modo === 'fila' ? await gestaoApi.pendentes(p, LIMITE, tipoId) : await gestaoApi.listar({ status, tipoId }, p, LIMITE);
        if (id !== reqId.current) return;
        setItens((atual) => (anexar ? [...atual, ...r.itens] : r.itens));
        setPagina(r.pagina);
        setTotalPaginas(r.totalPaginas);
        setTotal(r.totalItens);
        setErro('');
      } catch (e) {
        if (id === reqId.current) setErro(mensagemDeErro(e));
      } finally {
        if (id === reqId.current) {
          setCarregando(false);
          setMaisCarregando(false);
        }
      }
    },
    [modo, status, tipoId],
  );

  // Recarrega sempre que a tela ganha foco (ex.: voltando da moderação) ou os filtros mudam.
  useFocusEffect(
    useCallback(() => {
      setCarregando(true);
      buscar(1, false);
    }, [buscar]),
  );

  function maisItens() {
    if (carregando || maisCarregando || pagina >= totalPaginas) return;
    setMaisCarregando(true);
    buscar(pagina + 1, true);
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <Cabecalho
        titulo={modo === 'fila' ? 'Fila de moderação' : 'Ocorrências'}
        subtitulo={modo === 'fila' ? 'Relatos aguardando análise' : 'Todos os relatos não excluídos'}
        icone={modo === 'fila' ? 'shield-checkmark' : 'list'}
      />
      <View style={{ paddingHorizontal: 16, paddingTop: 12 }}>
        {modo === 'gestao' && (
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <Chip label="Todas" ativo={!status} onPress={() => setStatus(undefined)} />
            {STATUS.map((s) => (
              <Chip key={s} label={ROTULO_STATUS[s]} ativo={status === s} onPress={() => setStatus(s)} />
            ))}
          </ScrollView>
        )}
        {tipos.length > 0 && (
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <Chip label="Todos os tipos" ativo={!tipoId} onPress={() => setTipoId(undefined)} />
            {tipos.map((t) => (
              <Chip key={t.id} label={t.nome} ativo={tipoId === t.id} onPress={() => setTipoId(t.id)} />
            ))}
          </ScrollView>
        )}
        {!!erro && <Banner tipo="erro" texto={erro} />}
        {!carregando && !erro && (
          <Text style={{ color: colors.muted, fontSize: 12, marginBottom: 8 }}>
            {total} ocorrência{total === 1 ? '' : 's'}
          </Text>
        )}
      </View>

      {carregando ? (
        <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} />
      ) : (
        <FlatList
          data={itens}
          keyExtractor={(o) => String(o.id)}
          contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 30 }}
          refreshControl={<RefreshControl refreshing={false} onRefresh={() => buscar(1, false)} tintColor={colors.primary} />}
          onEndReached={maisItens}
          onEndReachedThreshold={0.4}
          ListEmptyComponent={
            <Vazio texto={modo === 'fila' ? 'Nenhuma ocorrência aguardando moderação.' : 'Nenhuma ocorrência encontrada com estes filtros.'} />
          }
          ListFooterComponent={maisCarregando ? <ActivityIndicator color={colors.primary} style={{ margin: 12 }} /> : undefined}
          renderItem={({ item }) => <ItemOcorrencia o={item} />}
        />
      )}
    </View>
  );
}

function ItemOcorrencia({ o }: { o: Ocorrencia }) {
  return (
    <Pressable
      onPress={() => router.push(`/moderar/${o.id}`)}
      style={({ pressed }) => ({
        backgroundColor: '#fff', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border,
        padding: 14, marginBottom: 10, opacity: pressed ? 0.85 : 1, flexDirection: 'row', alignItems: 'center',
      })}
    >
      <View style={{ flex: 1 }}>
        <StatusBadge status={o.status} />
        <Text style={{ fontWeight: '700', color: colors.text, marginTop: 6 }} numberOfLines={2}>{o.titulo}</Text>
        <Text style={{ color: colors.muted, fontSize: 12, marginTop: 2 }} numberOfLines={1}>
          {o.tipo.nome} · {dataCurta(o.dataOcorrencia ?? o.dataCadastro)}
          {o.endereco ? ` · ${o.endereco}` : ''}
        </Text>
      </View>
      <Ionicons name="chevron-forward" size={18} color={colors.text} />
    </Pressable>
  );
}
