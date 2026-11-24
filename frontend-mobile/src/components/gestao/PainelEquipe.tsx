import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, Text, View } from 'react-native';

import { Banner, Button, Cabecalho, Card, Titulo } from '../ui';
import { useAuth } from '../../context/AuthContext';
import { gestaoApi } from '../../services/gestao';
import { mensagemDeErro } from '../../services/http';
import type { StatusOcorrencia } from '../../services/types';
import { colors, radius } from '../../theme';
import { COR_STATUS, ROTULO_STATUS } from '../../utils/status';

type Totais = Record<StatusOcorrencia | 'TOTAL', number>;
const ORDEM: StatusOcorrencia[] = ['PENDENTE', 'PUBLICADA', 'OCULTA', 'REJEITADA'];

interface Atalho {
  label: string;
  icone: keyof typeof Ionicons.glyphMap;
  rota: string;
}

/** Painel inicial de Gestor/Administrador: contadores reais da fila + atalhos. */
export default function PainelEquipe({ base, atalhos }: { base: '/gestor' | '/admin'; atalhos: Atalho[] }) {
  const { usuario } = useAuth();
  const [totais, setTotais] = useState<Totais | null>(null);
  const [erro, setErro] = useState('');

  const carregar = useCallback(async () => {
    try {
      const [total, ...porStatus] = await Promise.all([gestaoApi.total(), ...ORDEM.map((s) => gestaoApi.total(s))]);
      const t = { TOTAL: total } as Totais;
      ORDEM.forEach((s, i) => (t[s] = porStatus[i]));
      setTotais(t);
      setErro('');
    } catch (e) {
      setErro(mensagemDeErro(e));
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      carregar();
    }, [carregar]),
  );

  const abrirStatus = (s: StatusOcorrencia) =>
    s === 'PENDENTE' && base === '/gestor'
      ? router.navigate('/gestor/fila')
      : router.navigate({ pathname: `${base}/ocorrencias`, params: { status: s } });

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <Cabecalho
        titulo={base === '/admin' ? 'Painel do administrador' : 'Painel do gestor'}
        subtitulo={`Olá, ${usuario?.nome ?? ''}`}
        icone={base === '/admin' ? 'speedometer' : 'shield-checkmark'}
      />
      <ScrollView
        contentContainerStyle={{ padding: 16, paddingBottom: 40 }}
        refreshControl={<RefreshControl refreshing={false} onRefresh={carregar} tintColor={colors.primary} />}
      >
        {!!erro && <Banner tipo="erro" texto={erro} />}

        {totais && totais.PENDENTE > 0 && (
          <Banner
            tipo="info"
            texto={`${totais.PENDENTE} ocorrência${totais.PENDENTE === 1 ? '' : 's'} aguardando moderação.`}
          />
        )}

        <Titulo>Ocorrências por situação</Titulo>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' }}>
          {ORDEM.map((s) => (
            <Pressable
              key={s}
              onPress={() => abrirStatus(s)}
              style={{
                width: '48.5%', backgroundColor: '#fff', borderRadius: radius.md, borderWidth: 1,
                borderColor: colors.border, padding: 14, marginBottom: 10,
              }}
            >
              <Text style={{ fontSize: 28, fontWeight: '800', color: COR_STATUS[s].fg }}>{totais ? totais[s] : '…'}</Text>
              <Text style={{ color: colors.muted, fontWeight: '600' }}>{ROTULO_STATUS[s]}</Text>
            </Pressable>
          ))}
        </View>
        <Text style={{ color: colors.muted, fontSize: 12, marginBottom: 16 }}>
          Total de ocorrências não excluídas: {totais ? totais.TOTAL : '…'}
        </Text>

        <Titulo>Atalhos</Titulo>
        <Card style={{ padding: 0, overflow: 'hidden' }}>
          {atalhos.map((a, i) => (
            <Pressable
              key={a.label}
              onPress={() => router.navigate(a.rota)}
              style={{
                flexDirection: 'row', alignItems: 'center', padding: 16,
                borderTopWidth: i === 0 ? 0 : 1, borderTopColor: colors.border,
              }}
            >
              <Ionicons name={a.icone} size={20} color={colors.primary} />
              <Text style={{ color: colors.text, fontWeight: '600', flex: 1, marginLeft: 12 }}>{a.label}</Text>
              <Ionicons name="chevron-forward" size={18} color={colors.text} />
            </Pressable>
          ))}
        </Card>
        <Button title="Atualizar números" variant="outline" onPress={carregar} />
      </ScrollView>
    </View>
  );
}
