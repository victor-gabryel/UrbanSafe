package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.urbansafe.backend.dto.AtualizarPerfilRequest;
import br.com.urbansafe.backend.dto.CadastroRequest;
import br.com.urbansafe.backend.dto.CadastroResponse;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.PerfilRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.support.Fixtures;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private PerfilRepository perfilRepository;
    private VerificacaoEmailNotificador notificador;
    private TokenVerificacaoEmailService tokenService;
    private RefreshTokenService refreshTokenService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        perfilRepository = mock(PerfilRepository.class);
        notificador = mock(VerificacaoEmailNotificador.class);
        tokenService = mock(TokenVerificacaoEmailService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        service = new UsuarioService(usuarioRepository, perfilRepository, encoder,
                notificador, tokenService, refreshTokenService);
    }

    private void prepararCadastroPossivel() {
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(perfilRepository.findByNome("USUARIO")).thenReturn(Optional.of(Fixtures.perfil("USUARIO")));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenService.gerarToken(any(Usuario.class))).thenReturn("token-original");
    }

    @Test
    void cadastroValidoGravaBcryptPerfilUsuarioEEmailNormalizado() {
        prepararCadastroPossivel();

        CadastroResponse resposta = service.cadastrar(
                new CadastroRequest("  Maria  ", "  Maria@Exemplo.COM ", "SenhaForte123"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();

        assertEquals("maria@exemplo.com", salvo.getEmail());
        assertEquals("Maria", salvo.getNome());
        assertEquals("USUARIO", salvo.getPerfil().getNome());
        assertTrue(salvo.isAtivo());
        assertFalse(salvo.isEmailVerificado());
        assertNotEquals("SenhaForte123", salvo.getSenhaHash());
        assertTrue(salvo.getSenhaHash().startsWith("$2"));
        assertTrue(encoder.matches("SenhaForte123", salvo.getSenhaHash()));
        assertEquals("maria@exemplo.com", resposta.email());
        verify(notificador).enviarAposCommit(any(), eq("maria@exemplo.com"), eq("token-original"));
    }

    @Test
    void cadastroComEmailDuplicadoRetorna409() {
        when(usuarioRepository.existsByEmailIgnoreCase("maria@exemplo.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.cadastrar(
                new CadastroRequest("Maria", "maria@exemplo.com", "SenhaForte123")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastroComSenhaAcimaDe72BytesERejeitado() {
        String senhaLonga = "é".repeat(40); // 40 caracteres, 80 bytes

        ApiException ex = assertThrows(ApiException.class, () -> service.cadastrar(
                new CadastroRequest("Maria", "maria@exemplo.com", senhaLonga)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void cadastroEntregaOTokenAoNotificadorETokenNaoVaiNaResposta() {
        prepararCadastroPossivel();

        CadastroResponse resposta = service.cadastrar(
                new CadastroRequest("Maria", "maria@exemplo.com", "SenhaForte123"));

        assertFalse(resposta.toString().contains("token-original"));
        verify(tokenService).gerarToken(any(Usuario.class));
    }

    @Test
    void atualizarPerfilAlteraSomenteONome() {
        Usuario usuario = Fixtures.usuario(1, "a@b.com", "USUARIO");
        when(usuarioRepository.findComPerfilById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.atualizarPerfil(1, new AtualizarPerfilRequest("  Novo Nome "));

        assertEquals("Novo Nome", resposta.nome());
        assertEquals("USUARIO", resposta.perfil());
        assertEquals("a@b.com", resposta.email());
    }

    @Test
    void desativarContaMarcaInativoERevogaSessoes() {
        Usuario usuario = Fixtures.usuario(1, "a@b.com", "USUARIO");
        when(usuarioRepository.findComPerfilById(1)).thenReturn(Optional.of(usuario));

        service.desativarConta(1);

        assertFalse(usuario.isAtivo());
        verify(usuarioRepository).save(usuario);
        verify(refreshTokenService).revogarTodos(1);
    }

    @Test
    void usuarioComumNaoConsultaOutroUsuario() {
        UsuarioPrincipal comum = new UsuarioPrincipal(1, "a@b.com", "USUARIO");

        ApiException ex = assertThrows(ApiException.class, () -> service.buscarPorId(comum, 2));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(usuarioRepository, never()).findComPerfilById(any());
    }

    @Test
    void gestorTambemNaoConsultaOutroUsuario() {
        UsuarioPrincipal gestor = new UsuarioPrincipal(1, "g@b.com", "GESTOR");

        assertThrows(ApiException.class, () -> service.buscarPorId(gestor, 2));
    }

    @Test
    void usuarioConsultaASiMesmoPeloId() {
        when(usuarioRepository.findComPerfilById(1))
                .thenReturn(Optional.of(Fixtures.usuario(1, "a@b.com", "USUARIO")));

        var resposta = service.buscarPorId(new UsuarioPrincipal(1, "a@b.com", "USUARIO"), 1);

        assertEquals(1, resposta.id());
    }

    @Test
    void administradorConsultaQualquerUsuario() {
        when(usuarioRepository.findComPerfilById(5))
                .thenReturn(Optional.of(Fixtures.usuario(5, "x@b.com", "USUARIO")));

        var resposta = service.buscarPorId(new UsuarioPrincipal(1, "adm@b.com", "ADMINISTRADOR"), 5);

        assertEquals(5, resposta.id());
    }

    @Test
    void administradorRecebe404ParaUsuarioInexistente() {
        when(usuarioRepository.findComPerfilById(99)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.buscarPorId(new UsuarioPrincipal(1, "adm@b.com", "ADMINISTRADOR"), 99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
