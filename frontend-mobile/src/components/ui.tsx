import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleProp,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
  ViewStyle,
} from 'react-native';

import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, radius } from '../theme';
import type { StatusOcorrencia } from '../services/types';
import { COR_STATUS, ROTULO_STATUS } from '../utils/status';

interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'outline' | 'link' | 'danger';
  loading?: boolean;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
}

export function Button({ title, onPress, variant = 'primary', loading, disabled, style }: ButtonProps) {
  const inativo = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={inativo}
      style={({ pressed }) => [
        styles.btn,
        variant === 'primary' && styles.btnPrimary,
        variant === 'outline' && styles.btnOutline,
        variant === 'link' && styles.btnLink,
        variant === 'danger' && styles.btnDanger,
        inativo && { opacity: 0.6 },
        pressed && { opacity: 0.85 },
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={variant === 'primary' ? '#fff' : colors.primary} />
      ) : (
        <Text
          style={[
            styles.btnText,
            variant === 'primary' ? { color: '#fff' } : variant === 'danger' ? { color: colors.danger } : { color: colors.primary },
            variant === 'link' && { fontWeight: '500' },
          ]}
        >
          {title}
        </Text>
      )}
    </Pressable>
  );
}

interface FieldProps extends TextInputProps {
  label: string;
  error?: string;
}

export function Field({ label, error, style, ...rest }: FieldProps) {
  return (
    <View style={{ marginBottom: 14 }}>
      <Text style={styles.label}>{label.toUpperCase()}</Text>
      <TextInput
        placeholderTextColor="#98A4B8"
        style={[styles.input, !!error && { borderColor: colors.danger }, style]}
        {...rest}
      />
      {!!error && <Text style={styles.error}>{error}</Text>}
    </View>
  );
}

export function Banner({ tipo, texto }: { tipo: 'erro' | 'ok' | 'info'; texto: string }) {
  const cor = tipo === 'erro' ? colors.danger : tipo === 'ok' ? colors.success : colors.primary;
  const fundo = tipo === 'erro' ? '#FDECEC' : tipo === 'ok' ? '#E8F5E9' : colors.primaryLight;
  return (
    <View style={[styles.banner, { backgroundColor: fundo, borderColor: cor }]}>
      <Text style={{ color: cor, fontSize: 13, lineHeight: 18 }}>{texto}</Text>
    </View>
  );
}

export function BackLink({ label, onPress, color = colors.primary }: { label: string; onPress: () => void; color?: string }) {
  return (
    <Pressable onPress={onPress} style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 6 }} hitSlop={8}>
      <Ionicons name="chevron-back" size={18} color={color} />
      <Text style={{ color, fontWeight: '600', marginLeft: 2 }}>{label}</Text>
    </Pressable>
  );
}

/** Cabeçalho azul das telas de gestão (mesmo padrão do cabeçalho do Perfil). */
export function Cabecalho({ titulo, subtitulo, icone, children }: {
  titulo: string; subtitulo?: string; icone?: keyof typeof Ionicons.glyphMap; children?: React.ReactNode;
}) {
  return (
    <SafeAreaView edges={['top']} style={{ backgroundColor: colors.primary }}>
      <View style={{ padding: 18, paddingBottom: 20 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          {icone && (
            <View style={{ width: 48, height: 48, borderRadius: 14, backgroundColor: 'rgba(255,255,255,0.2)', alignItems: 'center', justifyContent: 'center', marginRight: 12 }}>
              <Ionicons name={icone} size={24} color="#fff" />
            </View>
          )}
          <View style={{ flex: 1 }}>
            <Text style={{ color: '#fff', fontSize: 22, fontWeight: '800' }} numberOfLines={1}>{titulo}</Text>
            {!!subtitulo && <Text style={{ color: '#D7E5FB' }} numberOfLines={2}>{subtitulo}</Text>}
          </View>
        </View>
        {children}
      </View>
    </SafeAreaView>
  );
}

export function Chip({ label, ativo, onPress }: { label: string; ativo: boolean; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      style={{
        paddingHorizontal: 12, paddingVertical: 8, borderRadius: radius.pill, marginRight: 8, marginBottom: 8,
        backgroundColor: ativo ? colors.primary : '#fff', borderWidth: 1, borderColor: ativo ? colors.primary : colors.border,
      }}
    >
      <Text style={{ color: ativo ? '#fff' : colors.text, fontWeight: '600', fontSize: 13 }}>{label}</Text>
    </Pressable>
  );
}

export function StatusBadge({ status }: { status: StatusOcorrencia }) {
  const c = COR_STATUS[status];
  return (
    <View style={{ backgroundColor: c.bg, borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 3, alignSelf: 'flex-start' }}>
      <Text style={{ color: c.fg, fontSize: 11, fontWeight: '800' }}>{ROTULO_STATUS[status].toUpperCase()}</Text>
    </View>
  );
}

export function Card({ children, style }: { children: React.ReactNode; style?: StyleProp<ViewStyle> }) {
  return (
    <View style={[{ backgroundColor: '#fff', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, padding: 14, marginBottom: 12 }, style]}>
      {children}
    </View>
  );
}

export function Vazio({ texto }: { texto: string }) {
  return <Text style={{ color: colors.muted, textAlign: 'center', padding: 24 }}>{texto}</Text>;
}

export function Titulo({ children }: { children: React.ReactNode }) {
  return <Text style={{ fontWeight: '800', color: colors.text, fontSize: 16, marginBottom: 8 }}>{children}</Text>;
}

const styles = StyleSheet.create({
  btn: { height: 50, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16 },
  btnPrimary: { backgroundColor: colors.primary },
  btnOutline: { borderWidth: 1.5, borderColor: colors.primary, backgroundColor: '#fff' },
  btnLink: { height: 40 },
  btnDanger: { borderWidth: 1.5, borderColor: colors.danger, backgroundColor: '#fff' },
  btnText: { fontSize: 15, fontWeight: '700' },
  label: { fontSize: 11, fontWeight: '700', color: colors.muted, marginBottom: 6, letterSpacing: 0.5 },
  input: {
    height: 48,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: '#fff',
    paddingHorizontal: 14,
    fontSize: 15,
    color: colors.text,
  },
  error: { color: colors.danger, fontSize: 12, marginTop: 4 },
  banner: { borderWidth: 1, borderRadius: radius.sm, padding: 12, marginBottom: 14 },
});
