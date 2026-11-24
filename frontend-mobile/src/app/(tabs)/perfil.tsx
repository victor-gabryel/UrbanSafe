import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import PerfilConta from '../../components/PerfilConta';
import { Button } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { colors, radius } from '../../theme';

export default function Perfil() {
  const { logado } = useAuth();
  return logado ? <PerfilConta /> : <PerfilVisitante />;
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
