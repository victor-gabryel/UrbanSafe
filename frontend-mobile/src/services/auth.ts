import { http } from './http';
import type { CadastroResponse, LoginResponse, Usuario } from './types';

export const authApi = {
  login: (email: string, senha: string) =>
    http.post<LoginResponse>('/api/auth/login', { email, senha }).then((r) => r.data),
  cadastro: (nome: string, email: string, senha: string) =>
    http.post<CadastroResponse>('/api/auth/cadastro', { nome, email, senha }).then((r) => r.data),
  verificarEmail: (token: string) =>
    http.post<string>('/api/auth/verificar-email', { token }, { responseType: 'text' }).then((r) => r.data),
  reenviarVerificacao: (email: string) =>
    http.post<{ mensagem: string }>('/api/auth/reenviar-verificacao', { email }).then((r) => r.data),
  recuperarSenha: (email: string) =>
    http.post<{ mensagem: string }>('/api/auth/recuperar-senha', { email }).then((r) => r.data),
  redefinirSenha: (token: string, novaSenha: string) =>
    http.post<{ mensagem: string }>('/api/auth/redefinir-senha', { token, novaSenha }).then((r) => r.data),
  logout: (refreshToken: string) => http.post('/api/auth/logout', { refreshToken }),
  me: () => http.get<Usuario>('/api/auth/me').then((r) => r.data),
  atualizarNome: (nome: string) => http.patch<Usuario>('/api/usuarios/me', { nome }).then((r) => r.data),
};
