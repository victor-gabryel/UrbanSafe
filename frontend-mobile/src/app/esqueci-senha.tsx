import { router } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BackLink, Banner, Button, Field } from '../components/ui';
import { authApi } from '../services/auth';
import { mensagemDeErro } from '../services/http';
import { colors } from '../theme';
import { emailValido } from '../utils/validacao';

export default function EsqueciSenha() {
  const [etapa, setEtapa] = useState<1 | 2>(1);
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [senha, setSenha] = useState('');
  const [msg, setMsg] = useState<{ tipo: 'erro' | 'ok'; texto: string } | null>(null);
  const [loading, setLoading] = useState(false);

  async function pedir() {
    setMsg(null);
    if (!emailValido(email)) return setMsg({ tipo: 'erro', texto: 'Informe um e-mail válido.' });
    setLoading(true);
    try {
      const r = await authApi.recuperarSenha(email.trim());
      setMsg({ tipo: 'ok', texto: r.mensagem });
      setEtapa(2);
    } catch (e) {
      setMsg({ tipo: 'erro', texto: mensagemDeErro(e) });
    } finally {
      setLoading(false);
    }
  }

  async function redefinir() {
    setMsg(null);
    if (!token.trim()) return setMsg({ tipo: 'erro', texto: 'Cole o código recebido por e-mail.' });
    if (senha.length < 8) return setMsg({ tipo: 'erro', texto: 'A nova senha deve ter no mínimo 8 caracteres.' });
    setLoading(true);
    try {
      await authApi.redefinirSenha(token.trim(), senha);
      router.replace({ pathname: '/login', params: { aviso: 'Senha redefinida. Entre com a nova senha.' } });
    } catch (e) {
      setMsg({ tipo: 'erro', texto: mensagemDeErro(e) });
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 20 }}>
          <BackLink label="Login" onPress={() => (router.canGoBack() ? router.back() : router.replace('/login'))} />
          <Text style={{ fontSize: 24, fontWeight: '800', color: colors.text, marginTop: 6 }}>Esqueci minha senha</Text>
          <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 18, lineHeight: 20 }}>
            {etapa === 1
              ? 'Informe seu e-mail para receber as instruções de redefinição.'
              : 'Cole o código (parte após ?token=) do e-mail e escolha a nova senha.'}
          </Text>
          {msg && <Banner tipo={msg.tipo} texto={msg.texto} />}
          {etapa === 1 ? (
            <>
              <Field label="E-mail" autoCapitalize="none" keyboardType="email-address" value={email} onChangeText={setEmail} />
              <Button title="Enviar instruções" onPress={pedir} loading={loading} />
              <Button title="Já tenho o código" variant="link" onPress={() => setEtapa(2)} />
            </>
          ) : (
            <>
              <Field label="Código" autoCapitalize="none" autoCorrect={false} value={token} onChangeText={setToken} />
              <Field label="Nova senha" secureTextEntry value={senha} onChangeText={setSenha} />
              <Button title="Redefinir senha" onPress={redefinir} loading={loading} />
            </>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
