package br.com.urbansafe.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resposta paginada. {@code pagina} começa em 1. */
@Schema(description = "Resultado paginado")
public record PaginaResponse<T>(
        List<T> itens,
        @Schema(description = "Página atual (começa em 1)", example = "1") int pagina,
        @Schema(description = "Tamanho máximo da página", example = "20") int limite,
        @Schema(description = "Total de itens que atendem aos filtros", example = "57") long totalItens,
        @Schema(description = "Total de páginas", example = "3") int totalPaginas) {

    public static <T> PaginaResponse<T> de(List<T> itens, int pagina, int limite, long totalItens) {
        int totalPaginas = limite <= 0 ? 0 : (int) ((totalItens + limite - 1) / limite);
        return new PaginaResponse<>(itens, pagina, limite, totalItens, totalPaginas);
    }
}
