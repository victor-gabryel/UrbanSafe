package br.com.urbansafe.backend.security;

/** Identidade autenticada disponível via @AuthenticationPrincipal. Não carrega senha nem token. */
public record UsuarioPrincipal(Integer id, String email, String perfil) {

    public boolean isAdministrador() {
        return "ADMINISTRADOR".equals(perfil);
    }
}
