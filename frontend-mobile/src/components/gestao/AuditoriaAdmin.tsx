import { router, useFocusEffect } from 'expo-router';
import React, { useCallback, useRef, useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, Text, View } from 'react-native';

import { Banner, Cabecalho, Card, Vazio } from '../ui';
import { adminApi } from '../../services/admin';
import { mensagemDeErro } from '../../services/http';
import type { RegistroAuditoria } from '../../services/types';
import { colors } from '../../theme';
import { dataHora } from '../../utils/formatar';
import { BackLink } from '../ui';

const LIMITE = 20;

/** Trilha de auditoria administrativa (somente leitura, somente ADMINISTRADOR). */
export default function AuditoriaAdmin() {
  const [itens, setItens] = useState<RegistroAuditoria[]>([]);
  const [pagina, setPagina] = useState(1);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [carregando, setCarregando] = useState(true);
  const [maisCarregando, setMaisCarregando] = useState(false);
  const [erro, setErro] = useState('');
  const reqId = useRef(0);

  const buscar = useCallback(async (p: number, anexar: boolean) => {
    const id = ++reqId.current;
    try {
      const r = await adminApi.auditoria(p, LIMITE);
      if (id !== reqId.current) return;
      setItens((a) => (anexar ? [...a, ...r.itens] : r.itens));
      setPagina(r.pagina);
      setTotalPaginas(r.totalPaginas);
      setErro('');
    } catch (e) {
      if (id === reqId.current) setErro(mensagemDeErro(e));
    } finally {
      if (id === reqId.current) {
        setCarregando(false);
        setMaisCarregando(false);
      }
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
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
      <Cabecalho titulo="Auditoria" subtitulo="Ações administrativas registradas" icone="document-text" />
      <View style={{ paddingHorizontal: 16, paddingTop: 8 }}>
        <BackLink label="Voltar ao painel" onPress={() => router.navigate('/admin')} />
        {!!erro && <Banner tipo="erro" texto={erro} />}
      </View>
      {carregando ? (
        <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} />
      ) : (
        <FlatList
          data={itens}
          keyExtractor={(a) => String(a.id)}
          contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 30 }}
          refreshControl={<RefreshControl refreshing={false} onRefresh={() => buscar(1, false)} tintColor={colors.primary} />}
          onEndReached={maisItens}
          onEndReachedThreshold={0.4}
          ListEmptyComponent={<Vazio texto="Nenhum registro de auditoria." />}
          ListFooterComponent={maisCarregando ? <ActivityIndicator color={colors.primary} style={{ margin: 12 }} /> : undefined}
          renderItem={({ item: a }) => (
            <Card style={{ marginBottom: 10 }}>
              <Text style={{ fontWeight: '800', color: colors.text }}>{a.acao.replace(/_/g, ' ')}</Text>
              <Text style={{ color: colors.muted, fontSize: 12, marginTop: 2 }}>
                {a.entidade}{a.entidadeId != null ? ` #${a.entidadeId}` : ''} · por usuário {a.usuarioId != null ? `#${a.usuarioId}` : '—'} · {dataHora(a.dataEvento)}
              </Text>
              {!!a.detalhes && <Text style={{ color: colors.text, marginTop: 4, fontSize: 13 }}>{a.detalhes}</Text>}
            </Card>
          )}
        />
      )}
    </View>
  );
}
