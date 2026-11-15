import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BackLink, Banner, Button, Field } from '../components/ui';
import { authApi } from '../services/auth';
import { mensagemDeErro } from '../services/http';
import { colors } from '../theme';
import { validarCadastro } from '../utils/validacao';

export default function Cadastro() {
  const [f, setF] = useState({ nome: '', email: '', senha: '', confirmar: '' });
  const [aceite, setAceite] = useState(false);
  const [erros, setErros] = useState<Record<string, string>>({});
  const [erroApi, setErroApi] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [loading, setLoading] = useState(false);

  const set = (k: keyof typeof f) => (v: string) => setF((o) => ({ ...o, [k]: v }));

  async function enviar() {
    setErroApi('');
    const e = validarCadastro({ ...f, aceite });
    setErros(e);
    if (Object.keys(e).length) return;
    setLoading(true);
    try {
      const r = await authApi.cadastro(f.nome.trim(), f.email.trim(), f.senha);
      setSucesso(`Conta criada para ${r.email}. Enviamos um e-mail com o código de verificação; confirme-o antes de entrar.`);
    } catch (err) {
      setErroApi(mensagemDeErro(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ paddingBottom: 40 }}>
          <View style={{ backgroundColor: '#fff', padding: 20, paddingBottom: 14 }}>
            <BackLink label="Login" onPress={() => (router.canGoBack() ? router.back() : router.replace('/login'))} />
            <Text style={{ fontSize: 24, fontWeight: '800', color: colors.text, marginTop: 6 }}>Criar conta</Text>
            <Text style={{ color: colors.primary, marginTop: 2 }}>Junte-se à rede de segurança urbana</Text>
          </View>

          <View style={{ padding: 20 }}>
            {!!erroApi && <Banner tipo="erro" texto={erroApi} />}
            {sucesso ? (
              <>
                <Banner tipo="ok" texto={sucesso} />
                <Button title="Informar código de verificação" onPress={() => router.replace('/verificar-email')} />
                <Button title="Ir para o login" variant="link" onPress={() => router.replace('/login')} />
              </>
            ) : (
              <>
                <Field label="Nome completo" placeholder="João da Silva" value={f.nome} onChangeText={set('nome')} error={erros.nome} autoComplete="name" />
                <Field label="E-mail" placeholder="seu@email.com" autoCapitalize="none" keyboardType="email-address" value={f.email} onChangeText={set('email')} error={erros.email} />
                <Field label="Senha" placeholder="Mínimo de 8 caracteres" secureTextEntry value={f.senha} onChangeText={set('senha')} error={erros.senha} />
                <Field label="Confirmar senha" placeholder="Repita a senha" secureTextEntry value={f.confirmar} onChangeText={set('confirmar')} error={erros.confirmar} />

                <Pressable onPress={() => setAceite((a) => !a)} style={{ flexDirection: 'row', alignItems: 'flex-start', marginBottom: 6 }}>
                  <Ionicons name={aceite ? 'checkbox' : 'square-outline'} size={24} color={aceite ? colors.primary : colors.muted} />
                  <Text style={{ flex: 1, marginLeft: 8, color: colors.muted, lineHeight: 20 }}>
                    Aceito os Termos de Uso e a Política de Privacidade.
                  </Text>
                </Pressable>
                {!!erros.aceite && <Text style={{ color: colors.danger, fontSize: 12, marginBottom: 10 }}>{erros.aceite}</Text>}

                <Button title="Criar conta" onPress={enviar} loading={loading} style={{ marginTop: 10 }} />
                <View style={{ flexDirection: 'row', justifyContent: 'center', marginTop: 16 }}>
                  <Text style={{ color: colors.muted }}>Já tenho cadastro. </Text>
                  <Pressable onPress={() => router.replace('/login')}>
                    <Text style={{ color: colors.primary, fontWeight: '700' }}>Entrar</Text>
                  </Pressable>
                </View>
              </>
            )}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
