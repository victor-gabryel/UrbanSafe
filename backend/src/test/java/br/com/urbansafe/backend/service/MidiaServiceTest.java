package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.urbansafe.backend.dto.MidiaResponse;
import br.com.urbansafe.backend.entity.MidiaOcorrencia;
import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.MidiaOcorrenciaRepository;
import br.com.urbansafe.backend.repository.OcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

class MidiaServiceTest {

    private static final UsuarioPrincipal AUTOR = new UsuarioPrincipal(10, "a@x.com", "USUARIO");
    private static final UsuarioPrincipal OUTRO = new UsuarioPrincipal(11, "b@x.com", "USUARIO");
    private static final UsuarioPrincipal GESTOR = new UsuarioPrincipal(20, "g@x.com", "GESTOR");

    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

    private OcorrenciaRepository ocorrenciaRepository;
    private MidiaOcorrenciaRepository midiaRepository;
    private ArmazenamentoArquivos armazenamento;
    private MidiaService service;
    private Ocorrencia ocorrencia;

    @BeforeEach
    void preparar() throws IOException {
        ocorrenciaRepository = mock(OcorrenciaRepository.class);
        midiaRepository = mock(MidiaOcorrenciaRepository.class);
        armazenamento = mock(ArmazenamentoArquivos.class);
        service = new MidiaService(new OcorrenciaAcesso(ocorrenciaRepository), midiaRepository, armazenamento,
                new MidiaValidador(), mock(HistoricoService.class), 2, 1024, 4096, 1024, 1024);

        ocorrencia = new Ocorrencia();
        ReflectionTestUtils.setField(ocorrencia, "id", 1);
        ocorrencia.setUsuarioId(10);
        ocorrencia.setStatus(StatusOcorrencia.PENDENTE);
        when(ocorrenciaRepository.findByIdAndExcluidaEmIsNull(1)).thenReturn(Optional.of(ocorrencia));

        when(armazenamento.salvar(any(), anyString(), anyLong()))
                .thenReturn(new ArmazenamentoArquivos.Armazenado("abc.png", PNG.length));
        when(midiaRepository.saveAndFlush(any(MidiaOcorrencia.class))).thenAnswer(i -> i.getArgument(0));
    }

    private MockMultipartFile arquivo(String nome, byte[] conteudo) {
        // Content-Type "image/png" informado de propósito: o servidor deve ignorá-lo e olhar os bytes.
        return new MockMultipartFile("arquivo", nome, "image/png", conteudo);
    }

    @Test
    void autorAdicionaImagemValidaEMetadadosVemDoConteudo() {
        MidiaResponse r = service.adicionar(AUTOR, 1, arquivo("../../foto minha.png", PNG), " legenda ");

        assertEquals("image/png", r.tipoConteudo());
        assertEquals("foto minha.png", r.nomeArquivo()); // sem diretórios
        assertEquals("legenda", r.descricao());
    }

    @Test
    void rejeitaArquivoDisfarcadoDeImagem() throws IOException {
        byte[] script = "<?php system($_GET['c']); ?>".getBytes();
        ApiException e = assertThrows(ApiException.class,
                () -> service.adicionar(AUTOR, 1, arquivo("foto.png", script), null));

        assertEquals(HttpStatus.valueOf(415), e.getStatus());
        verify(armazenamento, never()).salvar(any(), anyString(), anyLong());
    }

    @Test
    void rejeitaArquivoVazioEArquivoAcimaDoLimite() throws IOException {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.adicionar(AUTOR, 1, arquivo("a.png", new byte[0]), null)).getStatus());

        byte[] grande = new byte[2000];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertEquals(HttpStatus.valueOf(413), assertThrows(ApiException.class,
                () -> service.adicionar(AUTOR, 1, arquivo("a.png", grande), null)).getStatus());
        verify(armazenamento, never()).salvar(any(), anyString(), anyLong());
    }

    @Test
    void somenteOAutorAdicionaEApenasEnquantoPendente() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
                () -> service.adicionar(OUTRO, 1, arquivo("a.png", PNG), null)).getStatus()); // PENDENTE: invisível

        ocorrencia.setStatus(StatusOcorrencia.PUBLICADA);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> service.adicionar(OUTRO, 1, arquivo("a.png", PNG), null)).getStatus());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class,
                () -> service.adicionar(AUTOR, 1, arquivo("a.png", PNG), null)).getStatus());
    }

    @Test
    void respeitaLimiteDeMidiasPorOcorrencia() {
        when(midiaRepository.countByOcorrenciaIdAndRemovidaEmIsNull(1)).thenReturn(2L);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class,
                () -> service.adicionar(AUTOR, 1, arquivo("a.png", PNG), null)).getStatus());
    }

    @Test
    void remocaoEhLogicaEAutorizada() {
        MidiaOcorrencia midia = new MidiaOcorrencia();
        ReflectionTestUtils.setField(midia, "id", 7);
        midia.setOcorrenciaId(1);
        midia.setUsuarioId(10);
        when(midiaRepository.findByIdAndOcorrenciaIdAndRemovidaEmIsNull(7, 1)).thenReturn(Optional.of(midia));

        ocorrencia.setStatus(StatusOcorrencia.PUBLICADA);
        // autor não remove depois de publicada; usuário comum alheio não remove; gestor remove
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> service.remover(AUTOR, 1, 7)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> service.remover(OUTRO, 1, 7)).getStatus());
        assertNull(midia.getRemovidaEm());

        service.remover(GESTOR, 1, 7);
        assertEquals(20, midia.getRemovidaPor());
        verify(armazenamento, never()).remover(anyString()); // arquivo mantido (retenção/auditoria)
    }

    @Test
    void sanitizaNomeDeArquivo() {
        assertEquals("a.png", MidiaService.sanitizarNome("C:\\x\\y\\a.png"));
        assertEquals("a_b.png", MidiaService.sanitizarNome("/tmp/a$b.png"));
        assertNull(MidiaService.sanitizarNome("   "));
        assertNull(MidiaService.sanitizarNome(null));
    }
}
