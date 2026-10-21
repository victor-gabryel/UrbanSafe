package br.com.urbansafe.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.exception.GlobalExceptionHandler;
import br.com.urbansafe.backend.service.AuthService;
import br.com.urbansafe.backend.service.RecuperacaoSenhaService;
import br.com.urbansafe.backend.service.TokenVerificacaoEmailService;
import br.com.urbansafe.backend.service.UsuarioService;

/** Testa validação (Bean Validation) e o formato dos erros, sem subir banco nem a cadeia de segurança. */
class AuthControllerTest {

    private UsuarioService usuarioService;
    private AuthService authService;
    private RecuperacaoSenhaService recuperacaoSenhaService;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        usuarioService = mock(UsuarioService.class);
        authService = mock(AuthService.class);
        recuperacaoSenhaService = mock(RecuperacaoSenhaService.class);

        AuthController controller = new AuthController(usuarioService,
                mock(TokenVerificacaoEmailService.class), authService, recuperacaoSenhaService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void cadastroComDadosInvalidosRetorna400ComCampos() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isArray());

        verify(usuarioService, never()).cadastrar(any());
    }

    @Test
    void cadastroIgnoraTentativaDeEnviarPerfil() throws Exception {
        // O DTO não tem campo "perfil": a propriedade extra é descartada e o serviço decide o papel.
        when(usuarioService.cadastrar(any())).thenReturn(
                new br.com.urbansafe.backend.dto.CadastroResponse(1, "Maria", "maria@exemplo.com", false, null));

        mockMvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@exemplo.com\",\"senha\":\"SenhaForte123\","
                        + "\"perfil\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").doesNotExist());
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        when(usuarioService.cadastrar(any()))
                .thenThrow(ApiException.conflict("Este e-mail já está cadastrado."));

        mockMvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@exemplo.com\",\"senha\":\"SenhaForte123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Este e-mail já está cadastrado."));
    }

    @Test
    void loginComCredenciaisInvalidasRetorna401() throws Exception {
        when(authService.login(any()))
                .thenThrow(ApiException.unauthorized("E-mail ou senha inválidos."));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@exemplo.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginSemCorpoRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recuperarSenhaSempreResponde202ComMensagemGenerica() throws Exception {
        mockMvc.perform(post("/api/auth/recuperar-senha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"qualquer@exemplo.com\"}"))
                .andExpect(status().isAccepted());

        verify(recuperacaoSenhaService).solicitar("qualquer@exemplo.com");
    }

    @Test
    void excecaoInesperadaVira500GenericoSemVazarDetalhes() throws Exception {
        when(authService.login(any())).thenThrow(new IllegalStateException("detalhe interno secreto"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@exemplo.com\",\"senha\":\"x\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Erro interno. Tente novamente mais tarde."));
    }
}
