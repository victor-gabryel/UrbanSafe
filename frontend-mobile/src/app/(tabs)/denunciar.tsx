import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import * as Location from 'expo-location';
import { router } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, Switch, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Banner, Button, Field } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { mensagemDeErro } from '../../services/http';
import { ocorrenciasApi } from '../../services/ocorrencias';
import type { TipoOcorrencia } from '../../services/types';
import { colors, radius } from '../../theme';

export default function Denunciar() {
  const { logado } = useAuth();
  const [tipos, setTipos] = useState<TipoOcorrencia[]>([]);
  const [tipoId, setTipoId] = useState<number | null>(null);
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [endereco, setEndereco] = useState('');
  const [coord, setCoord] = useState<{ latitude: number; longitude: number } | null>(null);
  const [anonimo, setAnonimo] = useState(false);
  const [foto, setFoto] = useState<ImagePicker.ImagePickerAsset | null>(null);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [loading, setLoading] = useState(false);
  const [locando, setLocando] = useState(false);

  useEffect(() => {
    ocorrenciasApi.tipos().then(setTipos).catch((e) => setErro(mensagemDeErro(e)));
  }, []);

  if (!logado) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg, padding: 20, justifyContent: 'center' }}>
        <View style={{ backgroundColor: '#fff', borderRadius: radius.lg, padding: 22, alignItems: 'center' }}>
          <Ionicons name="lock-closed" size={36} color={colors.primary} />
          <Text style={{ fontSize: 20, fontWeight: '800', color: colors.text, marginTop: 10 }}>Entre para denunciar</Text>
          <Text style={{ color: colors.muted, textAlign: 'center', marginVertical: 10, lineHeight: 20 }}>
            Para registrar uma ocorrência é necessário ter uma conta. Você pode continuar consultando o mapa como visitante.
          </Text>
          <Button title="Entrar" onPress={() => router.push('/login')} style={{ alignSelf: 'stretch' }} />
          <Button title="Criar conta" variant="outline" onPress={() => router.push('/cadastro')} style={{ alignSelf: 'stretch', marginTop: 10 }} />
        </View>
      </SafeAreaView>
    );
  }

  async function usarLocalizacao() {
    setErro('');
    setLocando(true);
    try {
      const perm = await Location.requestForegroundPermissionsAsync();
      if (!perm.granted) return setErro('Permissão de localização negada.');
      const pos = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
      setCoord({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
      try {
        const [a] = await Location.reverseGeocodeAsync(pos.coords);
        if (a && !endereco) setEndereco([a.street, a.district ?? a.subregion, a.city].filter(Boolean).join(', '));
      } catch {
        /* endereço é opcional */
      }
    } catch {
      setErro('Não foi possível obter sua localização.');
    } finally {
      setLocando(false);
    }
  }

  async function escolherFoto() {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) return setErro('Permissão para acessar fotos negada.');
    const r = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.7 });
    if (!r.canceled) setFoto(r.assets[0]);
  }

  async function enviar() {
    setErro('');
    if (!tipoId) return setErro('Escolha o tipo de ocorrência.');
    if (titulo.trim().length < 3) return setErro('Informe um título.');
    if (!descricao.trim()) return setErro('Descreva o que aconteceu.');
    if (!coord) return setErro('Informe o local usando "Usar minha localização atual".');

    setLoading(true);
    try {
      const criada = await ocorrenciasApi.criar({
        tipoOcorrenciaId: tipoId,
        titulo: titulo.trim(),
        descricao: descricao.trim(),
        latitude: coord.latitude,
        longitude: coord.longitude,
        endereco: endereco.trim() || undefined,
        dataOcorrencia: new Date().toISOString(),
        anonimato: anonimo,
      });

      let extra = '';
      if (foto) {
        try {
          await ocorrenciasApi.enviarMidia(criada.id, {
            uri: foto.uri,
            name: foto.fileName ?? `foto-${criada.id}.jpg`,
            type: foto.mimeType ?? 'image/jpeg',
          });
        } catch (e) {
          extra = ` A foto não pôde ser anexada: ${mensagemDeErro(e)}`;
        }
      }
      setSucesso(
        (criada.status === 'PENDENTE'
          ? 'Ocorrência enviada e aguardando moderação antes de aparecer no mapa.'
          : 'Ocorrência registrada e publicada.') + extra,
      );
      setTitulo('');
      setDescricao('');
      setEndereco('');
      setCoord(null);
      setFoto(null);
      setTipoId(null);
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 20, paddingBottom: 40 }}>
          <Text style={{ fontSize: 24, fontWeight: '800', color: colors.text }}>Denunciar ocorrência</Text>
          <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 16 }}>Ajude a comunidade relatando o que aconteceu.</Text>

          {!!erro && <Banner tipo="erro" texto={erro} />}
          {!!sucesso && <Banner tipo="ok" texto={sucesso} />}

          <Text style={{ fontSize: 11, fontWeight: '700', color: colors.muted, marginBottom: 8 }}>TIPO</Text>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 10 }}>
            {tipos.map((t) => (
              <Pressable
                key={t.id}
                onPress={() => setTipoId(t.id)}
                style={{
                  paddingHorizontal: 12, paddingVertical: 8, borderRadius: radius.pill, marginRight: 8, marginBottom: 8,
                  backgroundColor: tipoId === t.id ? colors.primary : '#fff', borderWidth: 1,
                  borderColor: tipoId === t.id ? colors.primary : colors.border,
                }}
              >
                <Text style={{ color: tipoId === t.id ? '#fff' : colors.text, fontWeight: '600', fontSize: 13 }}>{t.nome}</Text>
              </Pressable>
            ))}
          </View>

          <Field label="Título" placeholder="Ex.: Celular furtado no ponto de ônibus" value={titulo} onChangeText={setTitulo} maxLength={150} />
          <Field label="Descrição" placeholder="Descreva de forma objetiva" value={descricao} onChangeText={setDescricao} multiline style={{ height: 100, paddingTop: 12, textAlignVertical: 'top' }} maxLength={5000} />

          <Button
            title={coord ? `Local definido (${coord.latitude.toFixed(4)}, ${coord.longitude.toFixed(4)})` : 'Usar minha localização atual'}
            variant="outline"
            onPress={usarLocalizacao}
            loading={locando}
            style={{ marginBottom: 14 }}
          />
          <Field label="Endereço (opcional)" placeholder="Rua, bairro" value={endereco} onChangeText={setEndereco} maxLength={500} />

          <Pressable onPress={escolherFoto} style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 14 }}>
            <Ionicons name="image-outline" size={22} color={colors.primary} />
            <Text style={{ color: colors.primary, fontWeight: '600', marginLeft: 8 }}>{foto ? 'Trocar foto' : 'Anexar foto (opcional)'}</Text>
          </Pressable>
          {foto && <Image source={{ uri: foto.uri }} style={{ width: '100%', height: 160, borderRadius: radius.md, marginBottom: 14 }} />}

          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18 }}>
            <Text style={{ color: colors.text, flex: 1 }}>Ocultar meu nome para outros usuários</Text>
            <Switch value={anonimo} onValueChange={setAnonimo} trackColor={{ true: colors.primary }} />
          </View>

          <Button title="Enviar denúncia" onPress={enviar} loading={loading} />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
