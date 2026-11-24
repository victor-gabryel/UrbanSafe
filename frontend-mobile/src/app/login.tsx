import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Banner, Button, Field } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { mensagemDeErro } from '../services/http';
import { colors } from '../theme';
import { rotaInicial } from '../utils/perfil';
import { emailValido } from '../utils/validacao';

export default function Login() {
  const { entrar } = useAuth();
  const { aviso } = useLocalSearchParams<{ aviso?: string }>();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);

  async function enviar() {
    setErro('');
    if (!emailValido(email)) return setErro('Informe um e-mail válido.');
    if (!senha) return setErro('Informe a senha.');
    setLoading(true);
    try {
      const u = await entrar(email, senha);
      router.replace(rotaInicial(u.perfil));
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ flexGrow: 1 }}>
          <SafeAreaView edges={['top']} style={{ backgroundColor: colors.primary, alignItems: 'center', paddingBottom: 36, paddingTop: 28 }}>
            <View
              style={{
                width: 72, height: 72, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.18)',
                alignItems: 'center', justifyContent: 'center',
              }}
            >
              <Ionicons name="shield-half" size={38} color="#fff" />
            </View>
            <Text style={{ color: '#fff', fontSize: 30, fontWeight: '800', marginTop: 12 }}>UrbanSafe</Text>
            <Text style={{ color: '#D7E5FB', marginTop: 4 }}>Segurança urbana inteligente</Text>
          </SafeAreaView>

          <View style={{ padding: 20, backgroundColor: colors.bg, flex: 1 }}>
            <Text style={{ fontSize: 22, fontWeight: '800', color: colors.text }}>Entrar na conta</Text>
            <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 18 }}>
              Consulte ocorrências relatadas e compare trajetos urbanos
            </Text>

            {!!aviso && <Banner tipo="info" texto={aviso} />}
            {!!erro && <Banner tipo="erro" texto={erro} />}

            <Field
              label="E-mail"
              placeholder="seu@email.com"
              autoCapitalize="none"
              keyboardType="email-address"
              autoComplete="email"
              value={email}
              onChangeText={setEmail}
            />
            <Field label="Senha" placeholder="••••••••" secureTextEntry value={senha} onChangeText={setSenha} onSubmitEditing={enviar} />

            <Button title="Entrar" onPress={enviar} loading={loading} />
            <Button title="Esqueci minha senha" variant="link" onPress={() => router.push('/esqueci-senha')} />

            <View style={{ flexDirection: 'row', alignItems: 'center', marginVertical: 6 }}>
              <View style={{ flex: 1, height: 1, backgroundColor: colors.border }} />
              <Text style={{ marginHorizontal: 10, color: colors.muted, fontSize: 12 }}>ou</Text>
              <View style={{ flex: 1, height: 1, backgroundColor: colors.border }} />
            </View>

            <Button title="Criar conta" variant="outline" onPress={() => router.push('/cadastro')} style={{ marginTop: 8 }} />
            <Button title="Continuar como visitante" variant="link" onPress={() => router.replace('/(tabs)')} />
            <Pressable onPress={() => router.push('/verificar-email')} style={{ alignItems: 'center', marginTop: 4 }}>
              <Text style={{ color: colors.muted, fontSize: 13 }}>Tenho um código de verificação de e-mail</Text>
            </Pressable>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}
