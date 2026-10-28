package br.com.urbansafe.backend.security;

/** Identidade autenticada disponível via @AuthenticationPrincipal. Não carrega senha nem token. */
public record UsuarioPrincipal(Integer id, String email, String perfil) {

    public boolean isAdministrador() {
        return "ADMINISTRADOR".equals(perfil);
    }

    public boolean isGestor() {
        return "GESTOR".equals(perfil);
    }

    /** GESTOR ou ADMINISTRADOR: perfis que analisam e moderam ocorrências. */
    public boolean isEquipe() {
        return isGestor() || isAdministrador();
    }
}
