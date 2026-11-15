import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, shadow } from '../../theme';

type Icone = keyof typeof Ionicons.glyphMap;

function icone(ativo: Icone, inativo: Icone) {
  return ({ focused, color }: { focused: boolean; color: string }) => (
    <Ionicons name={focused ? ativo : inativo} size={22} color={color} />
  );
}

export default function TabsLayout() {
  const insets = useSafeAreaInsets();
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: '#8A97AD',
        tabBarLabelStyle: { fontSize: 10, fontWeight: '600' },
        tabBarStyle: { height: 62 + insets.bottom, paddingBottom: insets.bottom + 4, paddingTop: 6, borderTopColor: colors.border },
      }}
    >
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
