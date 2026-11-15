import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Banner, Button, Field } from '../../components/ui';
import { API_URL } from '../../config/api';
import { useAuth } from '../../context/AuthContext';
import { authApi } from '../../services/auth';
import { mensagemDeErro } from '../../services/http';
import { ocorrenciasApi } from '../../services/ocorrencias';
import type { Ocorrencia } from '../../services/types';
import { colors, radius } from '../../theme';
import { dataCurta } from '../../utils/formatar';

export default function Perfil() {
  const { logado } = useAuth();
  return logado ? <PerfilLogado /> : <PerfilVisitante />;
}

function PerfilVisitante() {
  const itens: { label: string; ir: () => void }[] = [
    { label: 'Mapa de ocorrências', ir: () => router.navigate('/(tabs)') },
    { label: 'Mapa de calor', ir: () => router.navigate('/(tabs)/mapa') },
    { label: 'Calcular rota', ir: () => router.navigate('/(tabs)/rotas') },
    { label: 'Ocorrências publicadas', ir: () => router.navigate({ pathname: '/(tabs)', params: { lista: '1' } }) },
  ];
  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <SafeAreaView edges={['top']} style={{ backgroundColor: colors.primary }}>
        <View style={{ padding: 18, paddingBottom: 22 }}>
          <Pressable onPress={() => router.navigate('/(tabs)')} style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 14 }}>
            <Ionicons name="chevron-back" size={18} color="#fff" />
            <Text style={{ color: '#fff', fontWeight: '600' }}>Voltar</Text>
          </Pressable>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View style={{ width: 56, height: 56, borderRadius: 16, backgroundColor: 'rgba(255,255,255,0.2)', alignItems: 'center', justifyContent: 'center' }}>
              <Ionicons name="person-outline" size={26} color="#fff" />
            </View>
            <View style={{ marginLeft: 14 }}>
              <Text style={{ color: '#fff', fontSize: 22, fontWeight: '800' }}>Perfil de visitante</Text>
              <Text style={{ color: '#D7E5FB' }}>Navegação pública e anônima</Text>
            </View>
          </View>
        </View>
      </SafeAreaView>

      <ScrollView contentContainerStyle={{ padding: 16 }}>
        <View style={{ backgroundColor: colors.primaryLight, borderRadius: radius.md, borderWidth: 1, borderColor: '#C9DAF5', padding: 14, marginBottom: 14 }}>
          <Text style={{ color: colors.primary, fontWeight: '800', fontSize: 15 }}>Você está utilizando o UrbanSafe como visitante.</Text>
          <Text style={{ color: colors.muted, marginTop: 4, lineHeight: 19 }}>
            Consulte relatos publicados, regiões, mapa de calor e rotas sem criar uma conta.
          </Text>
        </View>

        <View style={{ flexDirection: 'row', gap: 10, marginBottom: 16 }}>
          <Button title="Entrar" onPress={() => router.push('/login')} style={{ flex: 1 }} />
          <Button title="Criar conta" variant="outline" onPress={() => router.push('/cadastro')} style={{ flex: 1 }} />
        </View>

        <View style={{ backgroundColor: '#fff', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, overflow: 'hidden' }}>
          {itens.map((it, i) => (
            <Pressable
              key={it.label}
              onPress={it.ir}
              style={{
                flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16,
                borderTopWidth: i === 0 ? 0 : 1, borderTopColor: colors.border,
              }}
            >
              <Text style={{ color: colors.text, fontWeight: '600' }}>{it.label}</Text>
              <Ionicons name="chevron-forward" size={18} color={colors.text} />
            </Pressable>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

function PerfilLogado() {
  const { usuario, sair, atualizarUsuario } = useAuth();
  const [nome, setNome] = useState(usuario?.nome ?? '');
  const [editando, setEditando] = useState(false);
  const [msg, setMsg] = useState<{ tipo: 'erro' | 'ok'; texto: string } | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [minhas, setMinhas] = useState<Ocorrencia[] | null>(null);

  const carregarMinhas = useCallback(async () => {
    try {
      setMinhas((await ocorrenciasApi.minhas()).itens);
    } catch (e) {
      setMsg({ tipo: 'erro', texto: mensagemDeErro(e) });
    }
  }, []);

  useEffect(() => {
    carregarMinhas();
  }, [carregarMinhas]);

  async function salvar() {
    setMsg(null);
    if (nome.trim().length < 3) return setMsg({ tipo: 'erro', texto: 'Informe um nome válido.' });
    setSalvando(true);
    try {
      atualizarUsuario(await authApi.atualizarNome(nome.trim()));
      setEditando(false);
      setMsg({ tipo: 'ok', texto: 'Nome atualizado.' });
    } catch (e) {
      setMsg({ tipo: 'erro', texto: mensagemDeErro(e) });
    } finally {
      setSalvando(false);
    }
  }

  async function sairAgora() {
    await sair();
    router.replace('/login');
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <SafeAreaView edges={['top']} style={{ backgroundColor: colors.primary }}>
        <View style={{ padding: 18, paddingBottom: 22, flexDirection: 'row', alignItems: 'center' }}>
          <View style={{ width: 56, height: 56, borderRadius: 16, backgroundColor: 'rgba(255,255,255,0.2)', alignItems: 'center', justifyContent: 'center' }}>
            <Ionicons name="person" size={26} color="#fff" />
          </View>
          <View style={{ marginLeft: 14, flex: 1 }}>
            <Text style={{ color: '#fff', fontSize: 20, fontWeight: '800' }} numberOfLines={1}>{usuario?.nome}</Text>
            <Text style={{ color: '#D7E5FB' }} numberOfLines={1}>{usuario?.email} · {usuario?.perfil}</Text>
          </View>
        </View>
      </SafeAreaView>

      <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
        {msg && <Banner tipo={msg.tipo} texto={msg.texto} />}

        <View style={{ backgroundColor: '#fff', borderRadius: radius.md, padding: 16, borderWidth: 1, borderColor: colors.border, marginBottom: 14 }}>
          {editando ? (
            <>
              <Field label="Nome" value={nome} onChangeText={setNome} />
              <View style={{ flexDirection: 'row', gap: 10 }}>
                <Button title="Salvar" onPress={salvar} loading={salvando} style={{ flex: 1 }} />
                <Button title="Cancelar" variant="outline" onPress={() => { setEditando(false); setNome(usuario?.nome ?? ''); }} style={{ flex: 1 }} />
              </View>
            </>
          ) : (
            <Button title="Editar nome" variant="outline" onPress={() => setEditando(true)} />
          )}
        </View>

        <Text style={{ fontWeight: '800', color: colors.text, fontSize: 16, marginBottom: 8 }}>Minhas ocorrências</Text>
        <View style={{ backgroundColor: '#fff', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, marginBottom: 14 }}>
          {minhas === null && <Text style={{ padding: 16, color: colors.muted }}>Carregando…</Text>}
          {minhas?.length === 0 && <Text style={{ padding: 16, color: colors.muted }}>Você ainda não registrou ocorrências.</Text>}
          {minhas?.map((o, i) => (
            <View key={o.id} style={{ padding: 14, borderTopWidth: i === 0 ? 0 : 1, borderTopColor: colors.border }}>
              <Text style={{ fontWeight: '700', color: colors.text }}>{o.titulo}</Text>
              <Text style={{ color: colors.muted, fontSize: 12, marginTop: 2 }}>
                {o.tipo.nome} · {dataCurta(o.dataOcorrencia ?? o.dataCadastro)} · {o.status}
              </Text>
            </View>
          ))}
        </View>

        <Button title="Sair da conta" variant="outline" onPress={sairAgora} />
        <Text style={{ color: colors.muted, fontSize: 11, textAlign: 'center', marginTop: 16 }}>Servidor: {API_URL}</Text>
      </ScrollView>
    </View>
  );
}
