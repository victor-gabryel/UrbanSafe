import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { authApi } from '../services/auth';
import { registrarCallbacksDeSessao } from '../services/http';
import { tokenStorage } from '../services/tokenStorage';
import type { Usuario } from '../services/types';

interface AuthState {
  carregando: boolean;
  usuario: Usuario | null;
  logado: boolean;
  entrar: (email: string, senha: string) => Promise<Usuario>;
  sair: () => Promise<void>;
  atualizarUsuario: (u: Usuario) => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [carregando, setCarregando] = useState(true);
  const [usuario, setUsuario] = useState<Usuario | null>(null);

  useEffect(() => {
    registrarCallbacksDeSessao({
      expirou: () => setUsuario(null),
      renovou: (r) => setUsuario(r.usuario),
    });

    (async () => {
      const { access } = await tokenStorage.load();
      if (access) {
        try {
          // 401 aqui dispara a renovação automática pelo interceptor.
          setUsuario(await authApi.me());
        } catch {
          // Sem rede ou sessão inválida: segue como visitante (tokens inválidos já foram limpos).
        }
      }
      setCarregando(false);
    })();
  }, []);

  const entrar = useCallback(async (email: string, senha: string) => {
    const r = await authApi.login(email.trim(), senha);
    await tokenStorage.save(r.accessToken, r.refreshToken);
    setUsuario(r.usuario);
    return r.usuario;
  }, []);

  const sair = useCallback(async () => {
    const refresh = tokenStorage.getRefresh();
    try {
      if (refresh) await authApi.logout(refresh);
    } catch {
      /* logout no servidor é "melhor esforço" */
    }
    await tokenStorage.clear();
    setUsuario(null);
  }, []);

  const value = useMemo<AuthState>(
    () => ({ carregando, usuario, logado: !!usuario, entrar, sair, atualizarUsuario: setUsuario }),
    [carregando, usuario, entrar, sair],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider');
  return ctx;
}
