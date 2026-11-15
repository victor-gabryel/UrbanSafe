import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';

import { API_TIMEOUT_MS, API_URL } from '../config/api';
import { tokenStorage } from './tokenStorage';
import type { LoginResponse } from './types';

export const http = axios.create({
  baseURL: API_URL,
  timeout: API_TIMEOUT_MS,
  headers: { Accept: 'application/json' },
});

/** Chamado quando a sessão não pode mais ser renovada (refresh inválido/expirado). */
let aoExpirarSessao: (() => void) | null = null;
/** Chamado após renovar tokens com sucesso, para manter o usuário em memória atualizado. */
let aoRenovar: ((r: LoginResponse) => void) | null = null;

export function registrarCallbacksDeSessao(cbs: {
  expirou: () => void;
  renovou: (r: LoginResponse) => void;
}) {
  aoExpirarSessao = cbs.expirou;
  aoRenovar = cbs.renovou;
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.getAccess();
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let renovacaoEmAndamento: Promise<string | null> | null = null;

async function renovarTokens(): Promise<string | null> {
  const refresh = tokenStorage.getRefresh();
  if (!refresh) return null;
  try {
    // Cliente separado: evita recursão nos interceptors.
    const r = await axios.post<LoginResponse>(
      `${API_URL}/api/auth/refresh`,
      { refreshToken: refresh },
      { timeout: API_TIMEOUT_MS },
    );
    await tokenStorage.save(r.data.accessToken, r.data.refreshToken);
    aoRenovar?.(r.data);
    return r.data.accessToken;
  } catch {
    return null;
  }
}

const ROTAS_SEM_RENOVACAO = ['/api/auth/login', '/api/auth/refresh', '/api/auth/cadastro', '/api/auth/logout'];

http.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as (AxiosRequestConfig & { _tentou?: boolean }) | undefined;
    const status = error.response?.status;
    const url = original?.url ?? '';

    if (
      status === 401 &&
      original &&
      !original._tentou &&
      tokenStorage.getRefresh() &&
      !ROTAS_SEM_RENOVACAO.some((r) => url.includes(r))
    ) {
      original._tentou = true;
      // Uma única renovação para todas as requisições que falharam ao mesmo tempo.
      renovacaoEmAndamento = renovacaoEmAndamento ?? renovarTokens().finally(() => (renovacaoEmAndamento = null));
      const novo = await renovacaoEmAndamento;
      if (novo) {
        original.headers = { ...(original.headers ?? {}), Authorization: `Bearer ${novo}` };
        return http(original);
      }
      await tokenStorage.clear();
      aoExpirarSessao?.();
    }
    return Promise.reject(error);
  },
);

interface ErroApi {
  mensagem?: string;
  campos?: { campo: string; mensagem: string }[];
}

/** Converte qualquer erro em texto amigável em português. */
export function mensagemDeErro(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const data = e.response?.data as ErroApi | undefined;
    if (data?.campos?.length) return data.campos.map((c) => c.mensagem).join('\n');
    if (data?.mensagem) return data.mensagem;
    if (!e.response) {
      if (e.code === 'ECONNABORTED') return `Tempo esgotado ao falar com o servidor (${API_URL}).`;
      return `Não foi possível conectar ao servidor (${API_URL}). Verifique se o backend está ligado, se o celular está no mesmo Wi-Fi do PC e se o firewall libera a porta 8080.`;
    }
    if (e.response.status === 429) return 'Muitas tentativas. Aguarde um pouco e tente de novo.';
    return `Erro ${e.response.status} ao processar a requisição.`;
  }
  return e instanceof Error ? e.message : 'Erro inesperado.';
}
