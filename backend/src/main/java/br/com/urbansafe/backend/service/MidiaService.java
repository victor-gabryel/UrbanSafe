package br.com.urbansafe.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.urbansafe.backend.dto.MidiaResponse;
import br.com.urbansafe.backend.entity.AcaoHistorico;
import br.com.urbansafe.backend.entity.MidiaOcorrencia;
import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.entity.TipoMidia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.MidiaOcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

@Service
public class MidiaService {

    private static final int DESCRICAO_MAX = 500;

    public record ArquivoMidia(MidiaOcorrencia midia, Resource recurso) {
    }

    private final OcorrenciaAcesso acesso;
    private final MidiaOcorrenciaRepository midiaRepository;
    private final ArmazenamentoArquivos armazenamento;
    private final MidiaValidador validador;
    private final HistoricoService historicoService;
    private final int maxPorOcorrencia;
    private final long maxBytesImagem;
    private final long maxBytesVideo;
    private final long maxBytesAudio;
    private final long maxBytesDocumento;

    public MidiaService(
            OcorrenciaAcesso acesso,
            MidiaOcorrenciaRepository midiaRepository,
            ArmazenamentoArquivos armazenamento,
            MidiaValidador validador,
            HistoricoService historicoService,
            @Value("${app.midias.max-por-ocorrencia:10}") int maxPorOcorrencia,
            @Value("${app.midias.max-bytes-imagem:10485760}") long maxBytesImagem,
            @Value("${app.midias.max-bytes-video:52428800}") long maxBytesVideo,
            @Value("${app.midias.max-bytes-audio:10485760}") long maxBytesAudio,
            @Value("${app.midias.max-bytes-documento:10485760}") long maxBytesDocumento) {

        this.acesso = acesso;
        this.midiaRepository = midiaRepository;
        this.armazenamento = armazenamento;
        this.validador = validador;
        this.historicoService = historicoService;
        this.maxPorOcorrencia = maxPorOcorrencia;
        this.maxBytesImagem = maxBytesImagem;
        this.maxBytesVideo = maxBytesVideo;
        this.maxBytesAudio = maxBytesAudio;
        this.maxBytesDocumento = maxBytesDocumento;
    }

    @Transactional(readOnly = true)
    public List<MidiaResponse> listar(UsuarioPrincipal principal, Integer ocorrenciaId) {
        acesso.carregarVisivel(principal, ocorrenciaId);
        return midiaRepository.findByOcorrenciaIdAndRemovidaEmIsNullOrderByIdAsc(ocorrenciaId).stream()
                .map(MidiaResponse::de)
                .toList();
    }

    /** Somente o AUTOR adiciona mídias, e apenas enquanto a ocorrência está PENDENTE. */
    @Transactional
    public MidiaResponse adicionar(UsuarioPrincipal principal, Integer ocorrenciaId, MultipartFile arquivo,
            String descricao) {

        Ocorrencia ocorrencia = acesso.carregarVisivel(principal, ocorrenciaId);

        if (!acesso.ehAutor(principal, ocorrencia)) {
            throw ApiException.forbidden("Somente o autor pode adicionar mídias à ocorrência.");
        }
        if (ocorrencia.getStatus() != StatusOcorrencia.PENDENTE) {
            throw ApiException.conflict("Mídias só podem ser adicionadas enquanto a ocorrência está PENDENTE.");
        }
        if (arquivo == null || arquivo.isEmpty()) {
            throw ApiException.badRequest("O arquivo é obrigatório e não pode estar vazio.");
        }
        if (descricao != null && descricao.length() > DESCRICAO_MAX) {
            throw ApiException.badRequest("A descrição deve ter no máximo " + DESCRICAO_MAX + " caracteres.");
        }
        if (midiaRepository.countByOcorrenciaIdAndRemovidaEmIsNull(ocorrenciaId) >= maxPorOcorrencia) {
            throw ApiException.conflict("Limite de " + maxPorOcorrencia + " mídias por ocorrência atingido.");
        }

        // Tipo REAL pelos primeiros bytes; extensão e Content-Type enviados pelo cliente não são confiáveis.
        byte[] cabecalho;
        try (InputStream in = arquivo.getInputStream()) {
            cabecalho = in.readNBytes(MidiaValidador.BYTES_CABECALHO);
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível ler o arquivo enviado.");
        }

        MidiaValidador.Detectado detectado = validador.detectar(cabecalho)
                .orElseThrow(() -> ApiException.unsupportedMediaType(
                        "Formato não permitido. Aceitos: JPEG, PNG, WebP, MP4, MP3, WAV e PDF."));

        long limite = limiteBytes(detectado.tipo());
        if (arquivo.getSize() > limite) {
            throw ApiException.payloadTooLarge(
                    "O arquivo excede o limite de " + (limite / (1024 * 1024)) + " MB para " + detectado.tipo() + ".");
        }

        ArmazenamentoArquivos.Armazenado armazenado;
        try (InputStream in = arquivo.getInputStream()) {
            // O limite é reaplicado sobre os bytes realmente gravados (não confia apenas em getSize()).
            armazenado = armazenamento.salvar(in, detectado.extensao(), limite);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível armazenar o arquivo.");
        }

        MidiaOcorrencia midia = new MidiaOcorrencia();
        midia.setOcorrenciaId(ocorrenciaId);
        midia.setUsuarioId(principal.id());
        midia.setTipo(detectado.tipo());
        midia.setChaveArmazenamento(armazenado.chave());
        midia.setNomeArquivo(sanitizarNome(arquivo.getOriginalFilename()));
        midia.setDescricao(descricao == null || descricao.isBlank() ? null : descricao.trim());
        midia.setTipoConteudo(detectado.mime());
        midia.setTamanhoBytes(armazenado.bytes());

        MidiaOcorrencia salva;
        try {
            salva = midiaRepository.saveAndFlush(midia);
            historicoService.registrar(ocorrenciaId, principal.id(), AcaoHistorico.MIDIA_ADICIONADA, null, null,
                    "Mídia adicionada (" + detectado.tipo() + ").");
        } catch (RuntimeException e) {
            armazenamento.remover(armazenado.chave()); // não deixa arquivo órfão se o banco falhar
            throw e;
        }

        return MidiaResponse.de(salva);
    }

    /**
     * Remoção lógica. O autor remove enquanto a ocorrência está PENDENTE; GESTOR/ADMINISTRADOR podem remover
     * em qualquer status (ex.: conteúdo sensível). O arquivo é mantido para fins de auditoria/retenção.
     */
    @Transactional
    public void remover(UsuarioPrincipal principal, Integer ocorrenciaId, Integer midiaId) {
        Ocorrencia ocorrencia = acesso.carregarVisivel(principal, ocorrenciaId);
        MidiaOcorrencia midia = midiaRepository.findByIdAndOcorrenciaIdAndRemovidaEmIsNull(midiaId, ocorrenciaId)
                .orElseThrow(() -> ApiException.notFound("Mídia não encontrada."));

        boolean autor = acesso.ehAutor(principal, ocorrencia);
        if (!principal.isEquipe()) {
            if (!autor) {
                throw ApiException.forbidden("Você não tem permissão para remover esta mídia.");
            }
            if (ocorrencia.getStatus() != StatusOcorrencia.PENDENTE) {
                throw ApiException.conflict("O autor só pode remover mídias enquanto a ocorrência está PENDENTE.");
            }
        }

        midia.remover(principal.id());
        midiaRepository.saveAndFlush(midia);

        historicoService.registrar(ocorrenciaId, principal.id(), AcaoHistorico.MIDIA_REMOVIDA, null, null,
                "Mídia removida (" + midia.getTipo() + ").");
    }

    /** Download protegido: mesma visibilidade da ocorrência. */
    @Transactional(readOnly = true)
    public ArquivoMidia baixar(UsuarioPrincipal principal, Integer ocorrenciaId, Integer midiaId) {
        acesso.carregarVisivel(principal, ocorrenciaId);
        MidiaOcorrencia midia = midiaRepository.findByIdAndOcorrenciaIdAndRemovidaEmIsNull(midiaId, ocorrenciaId)
                .orElseThrow(() -> ApiException.notFound("Mídia não encontrada."));
        return new ArquivoMidia(midia, armazenamento.carregar(midia.getChaveArmazenamento()));
    }

    private long limiteBytes(TipoMidia tipo) {
        return switch (tipo) {
            case IMAGEM -> maxBytesImagem;
            case VIDEO -> maxBytesVideo;
            case AUDIO -> maxBytesAudio;
            case DOCUMENTO -> maxBytesDocumento;
        };
    }

    /** Mantém apenas o nome-base (sem diretórios), remove caracteres de controle/estranhos e limita o tamanho. */
    static String sanitizarNome(String original) {
        if (original == null) {
            return null;
        }
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1);
        nome = nome.replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();
        if (nome.length() > 255) {
            nome = nome.substring(0, 255);
        }
        return nome.isBlank() ? null : nome;
    }
}
