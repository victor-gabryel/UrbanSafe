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

import { colors, radius } from '../theme';

interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'outline' | 'link';
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
            variant === 'primary' ? { color: '#fff' } : { color: colors.primary },
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

const styles = StyleSheet.create({
  btn: { height: 50, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16 },
  btnPrimary: { backgroundColor: colors.primary },
  btnOutline: { borderWidth: 1.5, borderColor: colors.primary, backgroundColor: '#fff' },
  btnLink: { height: 40 },
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
