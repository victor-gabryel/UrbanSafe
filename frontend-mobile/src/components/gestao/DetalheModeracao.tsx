import { router } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Image, KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { API_URL } from '../../config/api';
import { gestaoApi } from '../../services/gestao';
import { mensagemDeErro } from '../../services/http';
import { tokenStorage } from '../../services/tokenStorage';
import type { AcaoModeracao, EventoHistorico, Midia, Moderacao, OcorrenciaDetalhe, Relevancia } from '../../services/types';
import { colors, radius } from '../../theme';
import { dataHora } from '../../utils/formatar';
import {
  ACAO_EXIGE_JUSTIFICATIVA,
  ACOES_POR_STATUS,
  ROTULO_ACAO,
  ROTULO_BOTAO_ACAO,
  ROTULO_RELEVANCIA,
  ROTULO_STATUS,
} from '../../utils/status';
import { BackLink, Banner, Button, Card, Chip, Field, StatusBadge, Titulo } from '../ui';

const RELEVANCIAS: Relevancia[] = ['BAIXA', 'MEDIA', 'ALTA'];

/** Detalhe da ocorrência com as ações de moderação (GESTOR e ADMINISTRADOR). */
export default function DetalheModeracao({ id }: { id: number }) {
  const [o, setO] = useState<OcorrenciaDetalhe | null>(null);
  const [midias, setMidias] = useState<Midia[]>([]);
  const [historico, setHistorico] = useState<EventoHistorico[]>([]);
  const [moderacoes, setModeracoes] = useState<Moderacao[]>([]);
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');
  const [acao, setAcao] = useState<AcaoModeracao | null>(null);
  const [justificativa, setJustificativa] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [salvandoRel, setSalvandoRel] = useState(false);

  const carregar = useCallback(async () => {
    try {
      const det = await gestaoApi.detalhe(id);
      setO(det);
      // Mídias, histórico e moderações são complementares: falha neles não impede a moderação.
      const [m, h, mo] = await Promise.allSettled([gestaoApi.midias(id), gestaoApi.historico(id), gestaoApi.moderacoes(id)]);
      if (m.status === 'fulfilled') setMidias(m.value);
      if (h.status === 'fulfilled') setHistorico(h.value);
      if (mo.status === 'fulfilled') setModeracoes(mo.value);
      setErro('');
    } catch (e) {
      setErro(mensagemDeErro(e));
    }
  }, [id]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  async function confirmar() {
    if (!acao) return;
    setErro('');
    setOk('');
    if (ACAO_EXIGE_JUSTIFICATIVA[acao] && !justificativa.trim()) {
      return setErro('Informe a justificativa para esta ação.');
    }
    setEnviando(true);
    try {
      await gestaoApi.moderar(id, acao, justificativa);
      setOk('Decisão registrada.');
      setAcao(null);
      setJustificativa('');
      await carregar();
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  async function definirRelevancia(r: Relevancia) {
    if (!o || o.relevancia === r) return;
    setSalvandoRel(true);
    setErro('');
    try {
      await gestaoApi.atualizar(id, { relevancia: r });
      setOk('Relevância atualizada.');
      await carregar();
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setSalvandoRel(false);
    }
  }

  const acoes = o ? ACOES_POR_STATUS[o.status] : [];
  // A rota de arquivo exige JWT; o <Image> envia o cabeçalho Authorization.
  const headersMidia = { Authorization: `Bearer ${tokenStorage.getAccess() ?? ''}` };

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 20, paddingBottom: 50 }}>
          <BackLink label="Voltar" onPress={() => (router.canGoBack() ? router.back() : router.replace('/'))} />

          {!!erro && <Banner tipo="erro" texto={erro} />}
          {!!ok && <Banner tipo="ok" texto={ok} />}
          {!o && !erro && <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} />}

          {o && (
            <>
              <StatusBadge status={o.status} />
              <Text style={{ fontSize: 22, fontWeight: '800', color: colors.text, marginTop: 8 }}>{o.titulo}</Text>
              <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 14 }}>
                {o.tipo.nome}
                {o.tipo.categoria ? ` · ${o.tipo.categoria}` : ''}
              </Text>

              <Card>
                <Text style={{ color: colors.text, lineHeight: 20 }}>{o.descricao}</Text>
                <Linha rotulo="Local" valor={o.endereco ?? `${o.latitude.toFixed(5)}, ${o.longitude.toFixed(5)}`} />
                <Linha rotulo="Ocorrido em" valor={dataHora(o.dataOcorrencia)} />
                <Linha rotulo="Registrado em" valor={dataHora(o.dataCadastro)} />
                <Linha rotulo="Autor (id)" valor={o.autorId != null ? `#${o.autorId}${o.anonimato ? ' · pediu anonimato' : ''}` : '—'} />
              </Card>

              {midias.length > 0 && (
                <>
                  <Titulo>Evidências</Titulo>
                  {midias.map((m) =>
                    m.tipoConteudo.startsWith('image/') ? (
                      <Image
                        key={m.id}
                        source={{ uri: `${API_URL}${m.arquivoUrl}`, headers: headersMidia }}
                        style={{ width: '100%', height: 200, borderRadius: radius.md, marginBottom: 10, backgroundColor: '#E5EAF2' }}
                        resizeMode="cover"
                      />
                    ) : (
                      <Card key={m.id}>
                        <Text style={{ color: colors.text, fontWeight: '600' }}>{m.nomeArquivo}</Text>
                        <Text style={{ color: colors.muted, fontSize: 12 }}>
                          {m.tipo} · {(m.tamanhoBytes / 1024).toFixed(0)} KB (visualização apenas de imagens no app)
                        </Text>
                      </Card>
                    ),
                  )}
                </>
              )}

              <Titulo>Relevância temporal</Titulo>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center' }}>
                {RELEVANCIAS.map((r) => (
                  <Chip key={r} label={ROTULO_RELEVANCIA[r]} ativo={o.relevancia === r} onPress={() => definirRelevancia(r)} />
                ))}
                {salvandoRel && <ActivityIndicator color={colors.primary} style={{ marginBottom: 8 }} />}
              </View>

              <Titulo>Moderação</Titulo>
              {acao ? (
                <Card>
                  <Text style={{ fontWeight: '800', color: colors.text, marginBottom: 8 }}>{ROTULO_BOTAO_ACAO[acao]}</Text>
                  <Field
                    label={ACAO_EXIGE_JUSTIFICATIVA[acao] ? 'Justificativa (obrigatória)' : 'Justificativa (opcional)'}
                    placeholder="Explique a decisão"
                    value={justificativa}
                    onChangeText={setJustificativa}
                    multiline
                    maxLength={1000}
                    style={{ height: 100, paddingTop: 12, textAlignVertical: 'top' }}
                  />
                  <View style={{ flexDirection: 'row', gap: 10 }}>
                    <Button title="Confirmar" onPress={confirmar} loading={enviando} style={{ flex: 1 }} />
                    <Button title="Cancelar" variant="outline" onPress={() => { setAcao(null); setJustificativa(''); }} style={{ flex: 1 }} />
                  </View>
                </Card>
              ) : (
                <View style={{ gap: 10, marginBottom: 14 }}>
                  {acoes.map((a) => (
                    <Button
                      key={a}
                      title={ROTULO_BOTAO_ACAO[a]}
                      variant={a === 'APROVAR' ? 'primary' : a === 'REJEITAR' ? 'danger' : 'outline'}
                      onPress={() => { setErro(''); setOk(''); setAcao(a); }}
                    />
                  ))}
                  {acoes.length === 0 && <Text style={{ color: colors.muted }}>Nenhuma ação disponível para o status {ROTULO_STATUS[o.status]}.</Text>}
                </View>
              )}

              <Titulo>Decisões de moderação</Titulo>
              <Card>
                {moderacoes.length === 0 && <Text style={{ color: colors.muted }}>Nenhuma decisão registrada ainda.</Text>}
                {moderacoes.map((m, i) => (
                  <View key={m.id} style={{ paddingTop: i === 0 ? 0 : 10, marginTop: i === 0 ? 0 : 10, borderTopWidth: i === 0 ? 0 : 1, borderTopColor: colors.border }}>
                    <Text style={{ fontWeight: '700', color: colors.text }}>{ROTULO_ACAO[m.acao]}</Text>
                    <Text style={{ color: colors.muted, fontSize: 12 }}>
                      {ROTULO_STATUS[m.statusAnterior]} → {ROTULO_STATUS[m.statusNovo]} · moderador #{m.moderadorId} · {dataHora(m.dataModeracao)}
                    </Text>
                    {!!m.justificativa && <Text style={{ color: colors.text, marginTop: 4 }}>{m.justificativa}</Text>}
                  </View>
                ))}
              </Card>

              <Titulo>Histórico</Titulo>
              <Card>
                {historico.length === 0 && <Text style={{ color: colors.muted }}>Sem eventos.</Text>}
                {historico.map((h, i) => (
                  <View key={h.id} style={{ paddingTop: i === 0 ? 0 : 8, marginTop: i === 0 ? 0 : 8, borderTopWidth: i === 0 ? 0 : 1, borderTopColor: colors.border }}>
                    <Text style={{ color: colors.text, fontWeight: '600' }}>{h.acao.replace(/_/g, ' ')}</Text>
                    <Text style={{ color: colors.muted, fontSize: 12 }}>{dataHora(h.dataEvento)}</Text>
                    {!!h.descricao && <Text style={{ color: colors.text, fontSize: 13, marginTop: 2 }}>{h.descricao}</Text>}
                  </View>
                ))}
              </Card>
            </>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Linha({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <View style={{ flexDirection: 'row', marginTop: 8 }}>
      <Text style={{ width: 100, color: colors.muted, fontSize: 12, fontWeight: '700' }}>{rotulo.toUpperCase()}</Text>
      <Text style={{ flex: 1, color: colors.text }}>{valor}</Text>
    </View>
  );
}
