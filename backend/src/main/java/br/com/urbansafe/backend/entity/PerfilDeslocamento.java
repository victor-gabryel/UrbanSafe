package br.com.urbansafe.backend.entity;

/**
 * Modo de deslocamento aceito no cálculo de rotas. O público do MVP são pedestres e ciclistas
 * (seção 9 do documento); outros modos não são oferecidos.
 */
public enum PerfilDeslocamento {

    A_PE,
    BICICLETA
}
