import React, { useCallback, useRef, useState } from 'react';
import { useFocusEffect } from 'expo-router';
import { ActivityIndicator, Alert, FlatList, Pressable, RefreshControl, Text, View } from 'react-native';

import { Banner, Button, Cabecalho, Chip, Vazio } from '../ui';
import { useAuth } from '../../context/AuthContext';
import { adminApi } from '../../services/admin';
import { mensagemDeErro } from '../../services/http';
import type { Perfil, UsuarioAdmin } from '../../services/types';
import { colors, radius } from '../../theme';
import { dataCurta } from '../../utils/formatar';
import { ROTULO_PERFIL } from '../../utils/perfil';

const PERFIS: Perfil[] = ['USUARIO', 'GESTOR', 'ADMINISTRADOR'];
const LIMITE = 20;
const perfilDe = (p: string): Perfil => (p === 'USUARIO_COMUM' ? 'USUARIO' : (p as Perfil));

/** Administração de usuários: listar, bloquear/reativar e alterar perfil (somente ADMINISTRADOR). */
export default function UsuariosAdmin() {
  const { usuario: eu } = useAuth();
  const [itens, setItens] = useState<UsuarioAdmin[]>([]);
  const [pagina, setPagina] = useState(1);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [total, setTotal] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [maisCarregando, setMaisCarregando] = useState(false);
  const [aberto, setAberto] = useState<number | null>(null);
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');
  const reqId = useRef(0);

  const buscar = useCallback(async (p: number, anexar: boolean) => {
    const id = ++reqId.current;
    try {
      const r = await adminApi.usuarios(p, LIMITE);
      if (id !== reqId.current) return;
      setItens((a) => (anexar ? [...a, ...r.itens] : r.itens));
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

  function substituir(u: UsuarioAdmin) {
    setItens((a) => a.map((x) => (x.id === u.id ? u : x)));
  }

  function confirmar(titulo: string, msg: string, executar: () => Promise<void>) {
    Alert.alert(titulo, msg, [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Confirmar', onPress: () => void executar() },
    ]);
  }

  function alternarStatus(u: UsuarioAdmin) {
    const bloquear = u.ativo;
    confirmar(
      bloquear ? 'Bloquear conta' : 'Reativar conta',
      bloquear
        ? `${u.nome} perderá o acesso e as sessões abertas serão encerradas.`
        : `${u.nome} poderá entrar novamente no aplicativo.`,
      async () => {
        setOcupado(u.id);
        setErro('');
        setOk('');
        try {
          substituir(await adminApi.alterarStatusUsuario(u.id, !u.ativo));
          setOk(bloquear ? 'Conta bloqueada.' : 'Conta reativada.');
        } catch (e) {
          setErro(mensagemDeErro(e));
        } finally {
          setOcupado(null);
        }
      },
    );
  }

  function alterarPerfil(u: UsuarioAdmin, novo: Perfil) {
    if (perfilDe(u.perfil) === novo) return;
    confirmar('Alterar perfil', `Definir ${u.nome} como ${ROTULO_PERFIL[novo]}? A ação fica registrada na auditoria.`, async () => {
      setOcupado(u.id);
      setErro('');
      setOk('');
      try {
        const r = await adminApi.alterarPerfilUsuario(u.id, novo);
        substituir({ ...u, perfil: r.perfil });
        setOk('Perfil alterado. O novo acesso vale a partir do próximo login do usuário.');
      } catch (e) {
        setErro(mensagemDeErro(e));
      } finally {
        setOcupado(null);
      }
    });
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <Cabecalho titulo="Usuários" subtitulo="Contas, perfis e bloqueios" icone="people" />
      <View style={{ paddingHorizontal: 16, paddingTop: 12 }}>
        {!!erro && <Banner tipo="erro" texto={erro} />}
        {!!ok && <Banner tipo="ok" texto={ok} />}
        {!carregando && <Text style={{ color: colors.muted, fontSize: 12, marginBottom: 8 }}>{total} usuário{total === 1 ? '' : 's'}</Text>}
      </View>
      {carregando ? (
        <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} />
      ) : (
        <FlatList
          data={itens}
          keyExtractor={(u) => String(u.id)}
          contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 30 }}
          refreshControl={<RefreshControl refreshing={false} onRefresh={() => buscar(1, false)} tintColor={colors.primary} />}
          onEndReached={maisItens}
          onEndReachedThreshold={0.4}
          ListEmptyComponent={<Vazio texto="Nenhum usuário encontrado." />}
          ListFooterComponent={maisCarregando ? <ActivityIndicator color={colors.primary} style={{ margin: 12 }} /> : undefined}
          renderItem={({ item: u }) => {
            const proprio = u.id === eu?.id;
            const expandido = aberto === u.id;
            return (
              <View style={{ backgroundColor: '#fff', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, marginBottom: 10 }}>
                <Pressable onPress={() => setAberto(expandido ? null : u.id)} style={{ padding: 14 }}>
                  <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                    <Text style={{ flex: 1, fontWeight: '700', color: colors.text }} numberOfLines={1}>
                      {u.nome}{proprio ? ' (você)' : ''}
                    </Text>
                    <Text style={{ fontSize: 11, fontWeight: '800', color: u.ativo ? colors.success : colors.danger }}>
                      {u.ativo ? 'ATIVA' : 'BLOQUEADA'}
                    </Text>
                  </View>
                  <Text style={{ color: colors.muted, fontSize: 12, marginTop: 2 }} numberOfLines={1}>{u.email}</Text>
                  <Text style={{ color: colors.muted, fontSize: 12 }}>
                    {ROTULO_PERFIL[perfilDe(u.perfil)] ?? u.perfil} · {u.emailVerificado ? 'e-mail verificado' : 'e-mail não verificado'} · desde {dataCurta(u.dataCadastro)}
                  </Text>
                </Pressable>
                {expandido && (
                  <View style={{ padding: 14, paddingTop: 0 }}>
                    {proprio ? (
                      <Text style={{ color: colors.muted, fontSize: 12 }}>Você não pode alterar o próprio perfil nem bloquear a própria conta.</Text>
                    ) : (
                      <>
                        <Text style={{ fontSize: 11, fontWeight: '700', color: colors.muted, marginBottom: 8 }}>PERFIL</Text>
                        <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
                          {PERFIS.map((p) => (
                            <Chip key={p} label={ROTULO_PERFIL[p]} ativo={perfilDe(u.perfil) === p} onPress={() => alterarPerfil(u, p)} />
                          ))}
                        </View>
                        <Button
                          title={u.ativo ? 'Bloquear conta' : 'Reativar conta'}
                          variant={u.ativo ? 'danger' : 'primary'}
                          loading={ocupado === u.id}
                          onPress={() => alternarStatus(u)}
                        />
                      </>
                    )}
                  </View>
                )}
              </View>
            );
          }}
        />
      )}
    </View>
  );
}
