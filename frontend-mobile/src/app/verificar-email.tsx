import { router } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BackLink, Banner, Button, Field } from '../components/ui';
import { authApi } from '../services/auth';
import { mensagemDeErro } from '../services/http';
import { colors } from '../theme';
import { emailValido } from '../utils/validacao';

export default function VerificarEmail() {
  const [token, setToken] = useState('');
  const [email, setEmail] = useState('');
  const [msg, setMsg] = useState<{ tipo: 'erro' | 'ok'; texto: string } | null>(null);
  const [loading, setLoading] = useState<'v' | 'r' | null>(null);

  async function verificar() {
    setMsg(null);
    if (!token.trim()) return setMsg({ tipo: 'erro', texto: 'Cole o código recebido por e-mail.' });
    setLoading('v');
    try {
      await authApi.verificarEmail(token.trim());
      router.replace({ pathname: '/login', params: { aviso: 'E-mail verificado! Agora você já pode entrar.' } });
    } catch (e) {
      setMsg({ tipo: 'erro', texto: mensagemDeErro(e) });
    } finally {
      setLoading(null);
    }
  }

  async function reenviar() {
    setMsg(null);
    if (!emailValido(email)) return setMsg({ tipo: 'erro', texto: 'Informe o e-mail cadastrado para reenviar.' });
    setLoading('r');
    try {
      const r = await authApi.reenviarVerificacao(email.trim());
      setMsg({ tipo: 'ok', texto: r.mensagem });
    } catch (e) {
      setMsg({ tipo: 'erro', texto: mensagemDeErro(e) });
    } finally {
      setLoading(null);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 20 }}>
          <BackLink label="Login" onPress={() => (router.canGoBack() ? router.back() : router.replace('/login'))} />
          <Text style={{ fontSize: 24, fontWeight: '800', color: colors.text, marginTop: 6 }}>Verificar e-mail</Text>
          <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 18, lineHeight: 20 }}>
            O e-mail de verificação traz um link terminando em ?token=CÓDIGO. Cole aqui apenas o CÓDIGO.
          </Text>
          {msg && <Banner tipo={msg.tipo} texto={msg.texto} />}
          <Field label="Código de verificação" autoCapitalize="none" autoCorrect={false} value={token} onChangeText={setToken} />
          <Button title="Verificar" onPress={verificar} loading={loading === 'v'} />

          <View style={{ height: 1, backgroundColor: colors.border, marginVertical: 24 }} />
          <Text style={{ fontWeight: '700', color: colors.text, marginBottom: 10 }}>Não recebeu o e-mail?</Text>
          <Field label="E-mail cadastrado" autoCapitalize="none" keyboardType="email-address" value={email} onChangeText={setEmail} />
          <Button title="Reenviar verificação" variant="outline" onPress={reenviar} loading={loading === 'r'} />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
