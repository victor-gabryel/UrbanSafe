import { Ionicons } from '@expo/vector-icons';
import { Redirect, Tabs } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { icone, useOpcoesDaBarra } from '../../components/navegacao';
import { useAuth } from '../../context/AuthContext';
import { colors, shadow } from '../../theme';
import { ehEquipe, rotaInicial } from '../../utils/perfil';

export default function TabsLayout() {
  const opcoes = useOpcoesDaBarra();
  const { usuario } = useAuth();
  // Gestor e Administrador têm área própria; Usuário e visitante seguem nestas abas.
  if (ehEquipe(usuario)) return <Redirect href={rotaInicial(usuario!.perfil)} />;
  return (
    <Tabs screenOptions={opcoes}>
      <Tabs.Screen name="index" options={{ title: 'Início', tabBarIcon: icone('home', 'home-outline') }} />
      <Tabs.Screen name="mapa" options={{ title: 'Mapa', tabBarIcon: icone('map', 'map-outline') }} />
      <Tabs.Screen
        name="denunciar"
        options={{
          title: 'Denunciar',
          tabBarLabel: ({ focused }) => (
            <Text style={{ fontSize: 10, fontWeight: '700', color: colors.primary, opacity: focused ? 1 : 0.9 }}>Denunciar</Text>
          ),
          tabBarButton: (props) => (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Denunciar"
              onPress={props.onPress ?? undefined}
              style={styles.centro}
            >
              <View style={styles.fab}>
                <Ionicons name="add" size={32} color="#fff" />
              </View>
              <Text style={{ fontSize: 10, fontWeight: '700', color: colors.primary, marginTop: 2 }}>Denunciar</Text>
            </Pressable>
          ),
        }}
      />
      <Tabs.Screen name="rotas" options={{ title: 'Rotas', tabBarIcon: icone('git-compare', 'git-compare-outline') }} />
      <Tabs.Screen name="perfil" options={{ title: 'Perfil', tabBarIcon: icone('person', 'person-outline') }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  centro: { flex: 1, alignItems: 'center', justifyContent: 'flex-start' },
  fab: {
    width: 54, height: 54, borderRadius: 18, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center',
    marginTop: -22, ...shadow,
  },
});
