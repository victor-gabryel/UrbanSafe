const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export const emailValido = (v: string) => EMAIL.test(v.trim());

export function validarCadastro(f: {
  nome: string;
  email: string;
  senha: string;
  confirmar: string;
  aceite: boolean;
}): Record<string, string> {
  const e: Record<string, string> = {};
  if (f.nome.trim().length < 3) e.nome = 'Informe seu nome completo.';
  if (f.nome.trim().length > 150) e.nome = 'O nome deve ter no máximo 150 caracteres.';
  if (!emailValido(f.email)) e.email = 'Informe um e-mail válido.';
  if (f.senha.length < 8) e.senha = 'A senha deve ter no mínimo 8 caracteres.';
  else if (new TextEncoder().encode(f.senha).length > 72) e.senha = 'A senha deve ter no máximo 72 bytes.';
  if (f.confirmar !== f.senha) e.confirmar = 'As senhas não conferem.';
  if (!f.aceite) e.aceite = 'É necessário aceitar os Termos de Uso e a Política de Privacidade.';
  return e;
}
