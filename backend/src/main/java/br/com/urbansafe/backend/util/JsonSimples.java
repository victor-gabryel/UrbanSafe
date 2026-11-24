package br.com.urbansafe.backend.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor mínimo de JSON usado apenas para interpretar a resposta do serviço de roteamento (OSRM).
 * Não depende de nenhuma versão do Jackson, então o código de integração não quebra se a biblioteca
 * de JSON do Spring mudar. Mapeamento: objeto -> {@code Map<String, Object>}, vetor -> {@code List<Object>},
 * número -> {@code Double}, texto -> {@code String}, booleano -> {@code Boolean}, nulo -> {@code null}.
 */
public final class JsonSimples {

    private static final int PROFUNDIDADE_MAXIMA = 64;

    private final String texto;
    private int posicao;

    private JsonSimples(String texto) {
        this.texto = texto;
    }

    /** @throws IllegalArgumentException se o texto não for um JSON válido */
    public static Object ler(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("JSON ausente.");
        }
        JsonSimples leitor = new JsonSimples(texto);
        leitor.pularEspacos();
        Object valor = leitor.lerValor(0);
        leitor.pularEspacos();
        if (leitor.posicao != texto.length()) {
            throw leitor.erro("conteúdo após o fim do JSON");
        }
        return valor;
    }

    private Object lerValor(int profundidade) {
        if (profundidade > PROFUNDIDADE_MAXIMA) {
            throw erro("JSON aninhado demais");
        }
        if (posicao >= texto.length()) {
            throw erro("fim inesperado");
        }
        char c = texto.charAt(posicao);
        return switch (c) {
            case '{' -> lerObjeto(profundidade);
            case '[' -> lerVetor(profundidade);
            case '"' -> lerTexto();
            case 't' -> lerLiteral("true", Boolean.TRUE);
            case 'f' -> lerLiteral("false", Boolean.FALSE);
            case 'n' -> lerLiteral("null", null);
            default -> lerNumero();
        };
    }

    private Map<String, Object> lerObjeto(int profundidade) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        posicao++; // {
        pularEspacos();
        if (consumir('}')) {
            return mapa;
        }
        while (true) {
            pularEspacos();
            if (posicao >= texto.length() || texto.charAt(posicao) != '"') {
                throw erro("chave de objeto esperada");
            }
            String chave = lerTexto();
            pularEspacos();
            if (!consumir(':')) {
                throw erro("':' esperado");
            }
            pularEspacos();
            mapa.put(chave, lerValor(profundidade + 1));
            pularEspacos();
            if (consumir(',')) {
                continue;
            }
            if (consumir('}')) {
                return mapa;
            }
            throw erro("',' ou '}' esperado");
        }
    }

    private List<Object> lerVetor(int profundidade) {
        List<Object> lista = new ArrayList<>();
        posicao++; // [
        pularEspacos();
        if (consumir(']')) {
            return lista;
        }
        while (true) {
            pularEspacos();
            lista.add(lerValor(profundidade + 1));
            pularEspacos();
            if (consumir(',')) {
                continue;
            }
            if (consumir(']')) {
                return lista;
            }
            throw erro("',' ou ']' esperado");
        }
    }

    private String lerTexto() {
        StringBuilder sb = new StringBuilder();
        posicao++; // aspas de abertura
        while (posicao < texto.length()) {
            char c = texto.charAt(posicao++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (posicao >= texto.length()) {
                break;
            }
            char e = texto.charAt(posicao++);
            switch (e) {
                case '"', '\\', '/' -> sb.append(e);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (posicao + 4 > texto.length()) {
                        throw erro("escape unicode incompleto");
                    }
                    try {
                        sb.append((char) Integer.parseInt(texto.substring(posicao, posicao + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw erro("escape unicode inválido");
                    }
                    posicao += 4;
                }
                default -> throw erro("escape inválido");
            }
        }
        throw erro("texto sem aspas de fechamento");
    }

    private Object lerLiteral(String esperado, Object valor) {
        if (!texto.startsWith(esperado, posicao)) {
            throw erro("valor inválido");
        }
        posicao += esperado.length();
        return valor;
    }

    private Double lerNumero() {
        int inicio = posicao;
        while (posicao < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(posicao)) >= 0) {
            posicao++;
        }
        if (inicio == posicao) {
            throw erro("valor inválido");
        }
        try {
            return Double.valueOf(texto.substring(inicio, posicao));
        } catch (NumberFormatException e) {
            throw erro("número inválido");
        }
    }

    private boolean consumir(char esperado) {
        if (posicao < texto.length() && texto.charAt(posicao) == esperado) {
            posicao++;
            return true;
        }
        return false;
    }

    private void pularEspacos() {
        while (posicao < texto.length() && Character.isWhitespace(texto.charAt(posicao))) {
            posicao++;
        }
    }

    private IllegalArgumentException erro(String motivo) {
        return new IllegalArgumentException("JSON inválido na posição " + posicao + ": " + motivo + ".");
    }
}
