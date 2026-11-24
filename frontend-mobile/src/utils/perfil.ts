import type { Perfil, Usuario } from '../services/types';

export const ehGestor = (u: Pick<Usuario, 'perfil'> | null | undefined) => u?.perfil === 'GESTOR';
export const ehAdministrador = (u: Pick<Usuario, 'perfil'> | null | undefined) => u?.perfil === 'ADMINISTRADOR';
export const ehEquipe = (u: Pick<Usuario, 'perfil'> | null | undefined) => ehGestor(u) || ehAdministrador(u);

/** Rota inicial de cada perfil após o login / ao abrir o app já autenticado. */
export function rotaInicial(perfil: Perfil | undefined | null): '/gestor' | '/admin' | '/(tabs)' {
  if (perfil === 'ADMINISTRADOR') return '/admin';
  if (perfil === 'GESTOR') return '/gestor';
  return '/(tabs)';
}

export const ROTULO_PERFIL: Record<Perfil, string> = {
  USUARIO: 'Usuário',
  GESTOR: 'Gestor',
  ADMINISTRADOR: 'Administrador',
};
