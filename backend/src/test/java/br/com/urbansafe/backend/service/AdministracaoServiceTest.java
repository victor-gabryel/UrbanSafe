package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.com.urbansafe.backend.dto.AlterarPerfilRequest;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.PerfilRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.support.Fixtures;

class AdministracaoServiceTest {

    private static final UsuarioPrincipal ADMIN = new UsuarioPrincipal(1, "adm@x.com", "ADMINISTRADOR");
    private static final UsuarioPrincipal GESTOR = new UsuarioPrincipal(2, "g@x.com", "GESTOR");
    private static final UsuarioPrincipal USUARIO = new UsuarioPrincipal(3, "u@x.com", "USUARIO");

    private UsuarioRepository usuarioRepository;
    private PerfilRepository perfilRepository;
    private AuditoriaService auditoria;
    private AdministracaoService service;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        perfilRepository = mock(PerfilRepository.class);
        auditoria = mock(AuditoriaService.class);
        service = new AdministracaoService(usuarioRepository, perfilRepository, auditoria);

        when(perfilRepository.findByNome("GESTOR")).thenReturn(Optional.of(Fixtures.perfil("GESTOR")));
        when(perfilRepository.findByNome("USUARIO")).thenReturn(Optional.of(Fixtures.perfil("USUARIO")));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void alvo(int id, String perfil) {
        when(usuarioRepository.findComPerfilById(id))
                .thenReturn(Optional.of(Fixtures.usuario(id, "alvo@x.com", perfil)));
    }

    @Test
    void administradorPromoveUsuarioAGestorERegistraAuditoria() {
        alvo(5, "USUARIO");

        UsuarioResponse r = service.alterarPerfil(ADMIN, 5, new AlterarPerfilRequest("gestor"));

        assertEquals("GESTOR", r.perfil());
        verify(auditoria).registrar(eq(1), eq("ALTERAR_PERFIL_USUARIO"), eq("usuarios"), eq(5),
                eq("USUARIO -> GESTOR"));
    }

    @Test
    void usuarioComumEGestorNaoAlteramPerfis() {
        alvo(5, "USUARIO");
        for (UsuarioPrincipal p : new UsuarioPrincipal[] { USUARIO, GESTOR }) {
            ApiException e = assertThrows(ApiException.class,
                    () -> service.alterarPerfil(p, 5, new AlterarPerfilRequest("ADMINISTRADOR")));
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void ninguemAlteraOProprioPerfil() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.alterarPerfil(ADMIN, 1, new AlterarPerfilRequest("USUARIO")));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void rejeitaPerfilForaDosTresPermitidos() {
        alvo(5, "USUARIO");
        for (String invalido : new String[] { "SUPER", "MODERADOR", "", "ROLE_ADMIN" }) {
            ApiException e = assertThrows(ApiException.class,
                    () -> service.alterarPerfil(ADMIN, 5, new AlterarPerfilRequest(invalido)));
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    void usuarioComumEhSinonimoDeUsuarioEMesmoPerfilNaoGeraAuditoria() {
        alvo(5, "USUARIO");
        UsuarioResponse r = service.alterarPerfil(ADMIN, 5, new AlterarPerfilRequest("USUARIO_COMUM"));

        assertEquals("USUARIO", r.perfil());
        verify(auditoria, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void usuarioInexistenteRetorna404() {
        when(usuarioRepository.findComPerfilById(77)).thenReturn(Optional.empty());
        ApiException e = assertThrows(ApiException.class,
                () -> service.alterarPerfil(ADMIN, 77, new AlterarPerfilRequest("GESTOR")));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
