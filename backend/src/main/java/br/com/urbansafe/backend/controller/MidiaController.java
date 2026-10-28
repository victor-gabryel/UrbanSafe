package br.com.urbansafe.backend.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.urbansafe.backend.config.OpenApiConfig;
import br.com.urbansafe.backend.dto.MidiaResponse;
import br.com.urbansafe.backend.entity.MidiaOcorrencia;
import br.com.urbansafe.backend.entity.TipoMidia;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.MidiaService;

@RestController
@RequestMapping("/api/ocorrencias/{ocorrenciaId}/midias")
@Tag(name = "Mídias das ocorrências", description = "Formatos aceitos (detectados pelo conteúdo, não pela extensão): "
        + "JPEG, PNG, WebP (até 10 MB), MP4 (até 50 MB), MP3 e WAV (até 10 MB), PDF (até 10 MB); até 10 mídias por "
        + "ocorrência. Limites configuráveis por variáveis de ambiente.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class MidiaController {
    private final MidiaService midiaService;

    public MidiaController(MidiaService midiaService) {
        this.midiaService = midiaService;
    }

    @Operation(summary = "Listar mídias da ocorrência", description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR, conforme a visibilidade da ocorrência. Mídias removidas não aparecem.")
    @ApiResponse(responseCode = "200", description = "Mídias")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada (ou não visível)")
    @GetMapping
    public ResponseEntity<List<MidiaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer ocorrenciaId) {
        return ResponseEntity.ok(midiaService.listar(principal, ocorrenciaId));
    }

    @Operation(summary = "Adicionar mídia (multipart/form-data)", description = "Perfil: somente o autor da ocorrência, enquanto PENDENTE. Parte 'arquivo' (obrigatória) e campo "
            + "opcional 'descricao'. O tipo real do arquivo é verificado pelo conteúdo.")
    @ApiResponse(responseCode = "201", description = "Mídia adicionada")
    @ApiResponse(responseCode = "400", description = "Arquivo ausente/vazio ou descrição inválida")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Somente o autor pode adicionar")
    @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada (ou não visível)")
    @ApiResponse(responseCode = "409", description = "Ocorrência não está PENDENTE ou limite de mídias atingido")
    @ApiResponse(responseCode = "413", description = "Arquivo acima do limite do tipo")
    @ApiResponse(responseCode = "415", description = "Formato não permitido")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MidiaResponse> adicionar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer ocorrenciaId,
            @RequestPart("arquivo") MultipartFile arquivo,
            @Parameter(description = "Descrição opcional (até 500 caracteres)") @RequestParam(name = "descricao", required = false) String descricao) {
        MidiaResponse criada = midiaService.adicionar(principal, ocorrenciaId, arquivo, descricao);
        return ResponseEntity
                .created(URI.create("/api/ocorrencias/" + ocorrenciaId + "/midias/" + criada.id()))
                .body(criada);
    }

    @Operation(summary = "Remover mídia (remoção lógica)", description = "Perfis: autor enquanto a ocorrência está PENDENTE; GESTOR e ADMINISTRADOR em qualquer status.")
    @ApiResponse(responseCode = "204", description = "Mídia removida")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @ApiResponse(responseCode = "404", description = "Ocorrência ou mídia não encontrada")
    @ApiResponse(responseCode = "409", description = "Ocorrência não está PENDENTE (autor)")
    @DeleteMapping("/{midiaId}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer ocorrenciaId,
            @PathVariable Integer midiaId) {
        midiaService.remover(principal, ocorrenciaId, midiaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Baixar o arquivo da mídia", description = "Perfis: conforme a visibilidade da ocorrência. Acesso controlado (JWT): os arquivos não são "
            + "servidos por URL pública. Resposta com X-Content-Type-Options: nosniff.")
    @ApiResponse(responseCode = "200", description = "Conteúdo do arquivo")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "404", description = "Ocorrência, mídia ou arquivo não encontrado")
    @GetMapping("/{midiaId}/arquivo")
    public ResponseEntity<Resource> baixar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer ocorrenciaId,
            @PathVariable Integer midiaId) {
        MidiaService.ArquivoMidia arquivo = midiaService.baixar(principal, ocorrenciaId, midiaId);
        MidiaOcorrencia midia = arquivo.midia();

        String chave = midia.getChaveArmazenamento();
        String extensao = chave.substring(chave.lastIndexOf('.') + 1);
        ContentDisposition disposicao = (midia.getTipo() == TipoMidia.DOCUMENTO
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename("midia-" + midia.getId() + "." + extensao)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(midia.getTipoConteudo()))
                .contentLength(midia.getTamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(arquivo.recurso());
    }
}
