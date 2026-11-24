import { Ionicons } from '@expo/vector-icons';
import { Redirect } from 'expo-router';
import React from 'react';
import { ColorValue } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '../context/AuthContext';
import type { Perfil } from '../services/types';
import { colors } from '../theme';
import { rotaInicial } from '../utils/perfil';

export type Icone = keyof typeof Ionicons.glyphMap;

/** Ícone de aba (cheio quando ativa, contorno quando inativa) — mesmo padrão das abas do Usuário. */
export function icone(ativo: Icone, inativo: Icone) {
  return ({ focused, color }: { focused: boolean; color: ColorValue }) => (
    <Ionicons name={focused ? ativo : inativo} size={22} color={color as string} />
  );
}

/** Opções visuais da barra de abas, compartilhadas por Usuário, Gestor e Administrador. */
export function useOpcoesDaBarra() {
  const insets = useSafeAreaInsets();
  return {
    headerShown: false,
    tabBarActiveTintColor: colors.primary,
    tabBarInactiveTintColor: '#8A97AD',
    tabBarLabelStyle: { fontSize: 10, fontWeight: '600' as const },
    tabBarStyle: { height: 62 + insets.bottom, paddingBottom: insets.bottom + 4, paddingTop: 6, borderTopColor: colors.border },
  };
}

/**
 * Guarda de rota por perfil. Sem login vai para /login; com perfil não autorizado
 * volta para a área do próprio perfil. (A autorização real continua no backend.)
 */
export function RequerPerfil({ perfis, children }: { perfis: Perfil[]; children: React.ReactNode }) {
  const { usuario } = useAuth();
  if (!usuario) return <Redirect href="/login" />;
  if (!perfis.includes(usuario.perfil)) return <Redirect href={rotaInicial(usuario.perfil)} />;
  return <>{children}</>;
}
