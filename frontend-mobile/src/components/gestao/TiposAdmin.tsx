import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, KeyboardAvoidingView, Platform, RefreshControl, ScrollView, Switch, Text, View } from 'react-native';

import { Banner, Button, Cabecalho, Card, Field, Titulo, Vazio } from '../ui';
import { adminApi } from '../../services/admin';
import { mensagemDeErro } from '../../services/http';
import type { TipoOcorrencia } from '../../services/types';
import { colors } from '../../theme';

interface Form {
  id: number | null;
  nome: string;
  categoria: string;
  descricao: string;
  exigeRevisao: boolean;
}
const VAZIO: Form = { id: null, nome: '', categoria: '', descricao: '', exigeRevisao: true };

/** Catálogo de tipos de ocorrência: criar, editar e desativar (somente ADMINISTRADOR). */
export default function TiposAdmin() {
  const [tipos, setTipos] = useState<TipoOcorrencia[] | null>(null);
  // O backend só lista tipos ativos; guardamos os desativados nesta sessão para permitir reativar.
  const [desativados, setDesativados] = useState<TipoOcorrencia[]>([]);
  const [form, setForm] = useState<Form | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');

  const carregar = useCallback(async () => {
    try {
      setTipos(await adminApi.tiposAtivos());
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

  async function salvar() {
    if (!form) return;
    setErro('');
    setOk('');
    if (!form.nome.trim()) return setErro('Informe o nome do tipo.');
    setSalvando(true);
    try {
      if (form.id === null) {
        await adminApi.criarTipo({
          nome: form.nome.trim(),
          categoria: form.categoria.trim() || undefined,
          descricao: form.descricao.trim() || undefined,
          exigeRevisao: form.exigeRevisao,
        });
        setOk('Tipo criado.');
      } else {
        // Texto vazio limpa o campo no backend; campo ausente não é alterado.
        await adminApi.atualizarTipo(form.id, {
          nome: form.nome.trim(),
          categoria: form.categoria.trim(),
          descricao: form.descricao.trim(),
          exigeRevisao: form.exigeRevisao,
        });
        setOk('Tipo atualizado.');
      }
      setForm(null);
      await carregar();
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setSalvando(false);
    }
  }

  function desativar(t: TipoOcorrencia) {
    Alert.alert('Desativar tipo', `"${t.nome}" deixará de aparecer para novas ocorrências. As já registradas não são afetadas.`, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Desativar',
        style: 'destructive',
        onPress: async () => {
          setErro('');
          setOk('');
          try {
            await adminApi.desativarTipo(t.id);
            setDesativados((d) => [...d, { ...t, ativo: false }]);
            setOk('Tipo desativado.');
            await carregar();
          } catch (e) {
            setErro(mensagemDeErro(e));
          }
        },
      },
    ]);
  }

  async function reativar(t: TipoOcorrencia) {
    setErro('');
    setOk('');
    try {
      await adminApi.atualizarTipo(t.id, { ativo: true });
      setDesativados((d) => d.filter((x) => x.id !== t.id));
      setOk('Tipo reativado.');
      await carregar();
    } catch (e) {
      setErro(mensagemDeErro(e));
    }
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <Cabecalho titulo="Tipos de ocorrência" subtitulo="Catálogo usado nas denúncias" icone="pricetags" />
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={{ padding: 16, paddingBottom: 40 }}
          refreshControl={<RefreshControl refreshing={false} onRefresh={carregar} tintColor={colors.primary} />}
        >
          {!!erro && <Banner tipo="erro" texto={erro} />}
          {!!ok && <Banner tipo="ok" texto={ok} />}

          {form ? (
            <Card>
              <Text style={{ fontWeight: '800', color: colors.text, fontSize: 16, marginBottom: 10 }}>
                {form.id === null ? 'Novo tipo' : 'Editar tipo'}
              </Text>
              <Field label="Nome" value={form.nome} onChangeText={(v) => setForm({ ...form, nome: v })} maxLength={100} />
              <Field label="Categoria (opcional)" value={form.categoria} onChangeText={(v) => setForm({ ...form, categoria: v })} maxLength={80} />
              <Field
                label="Descrição (opcional)"
                value={form.descricao}
                onChangeText={(v) => setForm({ ...form, descricao: v })}
                multiline
                maxLength={1000}
                style={{ height: 90, paddingTop: 12, textAlignVertical: 'top' }}
              />
              <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 16 }}>
                <Text style={{ flex: 1, color: colors.text }}>Exigir moderação antes de publicar</Text>
                <Switch value={form.exigeRevisao} onValueChange={(v) => setForm({ ...form, exigeRevisao: v })} trackColor={{ true: colors.primary }} />
              </View>
              <View style={{ flexDirection: 'row', gap: 10 }}>
                <Button title="Salvar" onPress={salvar} loading={salvando} style={{ flex: 1 }} />
                <Button title="Cancelar" variant="outline" onPress={() => setForm(null)} style={{ flex: 1 }} />
              </View>
            </Card>
          ) : (
            <Button title="Novo tipo" onPress={() => { setOk(''); setForm(VAZIO); }} style={{ marginBottom: 14 }} />
          )}

          <Titulo>Tipos ativos</Titulo>
          {tipos === null && !erro && <ActivityIndicator color={colors.primary} style={{ marginTop: 16 }} />}
          {tipos?.length === 0 && <Vazio texto="Nenhum tipo ativo." />}
          {tipos?.map((t) => (
            <Card key={t.id}>
              <Text style={{ fontWeight: '700', color: colors.text }}>{t.nome}</Text>
              <Text style={{ color: colors.muted, fontSize: 12, marginTop: 2 }}>
                {t.categoria ?? 'Sem categoria'} · {t.exigeRevisao ? 'passa por moderação' : 'publica direto'}
              </Text>
              {!!t.descricao && <Text style={{ color: colors.text, fontSize: 13, marginTop: 4 }}>{t.descricao}</Text>}
              <View style={{ flexDirection: 'row', gap: 10, marginTop: 10 }}>
                <Button
                  title="Editar"
                  variant="outline"
                  style={{ flex: 1, height: 42 }}
                  onPress={() => { setOk(''); setForm({ id: t.id, nome: t.nome, categoria: t.categoria ?? '', descricao: t.descricao ?? '', exigeRevisao: t.exigeRevisao }); }}
                />
                <Button title="Desativar" variant="danger" style={{ flex: 1, height: 42 }} onPress={() => desativar(t)} />
              </View>
            </Card>
          ))}

          {desativados.length > 0 && (
            <>
              <Titulo>Desativados nesta sessão</Titulo>
              {desativados.map((t) => (
                <Card key={t.id}>
                  <Text style={{ fontWeight: '700', color: colors.muted }}>{t.nome}</Text>
                  <Button title="Reativar" variant="outline" style={{ marginTop: 10, height: 42 }} onPress={() => reativar(t)} />
                </Card>
              ))}
            </>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}
