package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.urbansafe.backend.dto.AtualizarTipoOcorrenciaRequest;
import br.com.urbansafe.backend.dto.CriarTipoOcorrenciaRequest;
import br.com.urbansafe.backend.dto.TipoOcorrenciaResponse;
import br.com.urbansafe.backend.entity.TipoOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.TipoOcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

class TipoOcorrenciaServiceTest {

    private static final UsuarioPrincipal ADMIN = new UsuarioPrincipal(1, "a@x.com", "ADMINISTRADOR");
    private static final UsuarioPrincipal GESTOR = new UsuarioPrincipal(2, "g@x.com", "GESTOR");
    private static final UsuarioPrincipal USUARIO = new UsuarioPrincipal(3, "u@x.com", "USUARIO");

    private TipoOcorrenciaRepository repository;
    private AuditoriaService auditoria;
    private TipoOcorrenciaService service;
    private TipoOcorrencia existente;

    @BeforeEach
    void preparar() {
        repository = mock(TipoOcorrenciaRepository.class);
        auditoria = mock(AuditoriaService.class);
        service = new TipoOcorrenciaService(repository, auditoria);

        existente = new TipoOcorrencia();
        ReflectionTestUtils.setField(existente, "id", 4);
        existente.setNome("Furto");
        existente.setDescricao("desc");
        existente.setCategoria("Patrimônio");
        when(repository.findById(4)).thenReturn(Optional.of(existente));
        when(repository.save(any(TipoOcorrencia.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void somenteAdministradorCriaEditaEDesativa() {
        var criar = new CriarTipoOcorrenciaRequest("Novo", null, null, null);
        var editar = new AtualizarTipoOcorrenciaRequest("X", null, null, null, null);

        for (UsuarioPrincipal p : new UsuarioPrincipal[] { USUARIO, GESTOR }) {
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> service.criar(p, criar)).getStatus());
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> service.atualizar(p, 4, editar)).getStatus());
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> service.desativar(p, 4)).getStatus());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void criaComPadroesEEvitaNomeDuplicadoSemDiferenciarCaixa() {
        TipoOcorrenciaResponse r = service.criar(ADMIN, new CriarTipoOcorrenciaRequest(" Vandalismo ", " ", null, null));
        assertEquals("Vandalismo", r.nome());
        assertTrue(r.ativo());
        assertTrue(r.exigeRevisao());
        assertEquals(null, r.descricao());

        when(repository.existsByNomeIgnoreCase("FURTO")).thenReturn(true);
        ApiException e = assertThrows(ApiException.class,
                () -> service.criar(ADMIN, new CriarTipoOcorrenciaRequest("FURTO", null, null, null)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void patchAlteraSomenteOsCamposEnviados() {
        TipoOcorrenciaResponse r = service.atualizar(ADMIN, 4,
                new AtualizarTipoOcorrenciaRequest(null, null, "Outra", null, false));

        assertEquals("Furto", r.nome());
        assertEquals("desc", r.descricao());
        assertEquals("Outra", r.categoria());
        assertFalse(r.exigeRevisao());
    }

    @Test
    void patchVazioOuNomeDuplicado() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.atualizar(ADMIN, 4, new AtualizarTipoOcorrenciaRequest(null, null, null, null, null)))
                .getStatus());

        when(repository.existsByNomeIgnoreCaseAndIdNot("Roubo", 4)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class,
                () -> service.atualizar(ADMIN, 4, new AtualizarTipoOcorrenciaRequest("Roubo", null, null, null, null)))
                .getStatus());
    }

    @Test
    void desativarEhLogicoEIdempotente() {
        service.desativar(ADMIN, 4);
        assertFalse(existente.isAtivo());
        verify(repository).save(existente);

        service.desativar(ADMIN, 4); // já inativo: não salva nem audita de novo
        verify(auditoria).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void tipoInativoSoApareceParaAdministrador() {
        existente.setAtivo(false);

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.buscar(USUARIO, 4)).getStatus());
        assertFalse(service.buscar(ADMIN, 4).ativo());
    }
}
